package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.jdbc.InterestTables;
import com.carddemo.interest.load.LoadExtractsStepConfig;

/**
 * The three steps joined into interestCalcJob (ticket 09, architecture.md section 4) under profile
 * {@code tables}, on PostgreSQL 15 in a container with the schema of FI-4 (standards S7.3). The
 * job reads the unloaded files of a fixture case and the files it writes are compared with the
 * recorded legacy run (standards S7.4).
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@ActiveProfiles("tables")
@Testcontainers
class LoadCalcExportJobIT {

    private static final List<String> RECORDED_FILES = List.of(
            "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt",
            "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt",
            "sysout/INTCALC.STEP15.txt",
            "rc.json");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = InterestTables.postgres();

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    @BeforeEach
    void emptyTables() {
        InterestTables.clear(jdbc);
    }

    private JobExecution launch(Path inputDir, Path outputDir) throws Exception {
        return jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, inputDir.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
    }

    private static List<String> steps(JobExecution execution) {
        return execution.getStepExecutions().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(step -> step.getStepName() + ":" + step.getStatus())
                .toList();
    }

    private static long records(Path file) {
        return Fixtures.lines(file).size();
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped",
            "i04-default-group-fallback", "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void loadCalcExport_everyCaseEqualsTheRecordedLegacyRun(String caseName) throws Exception {
        Path output = temp.resolve("actual");

        JobExecution execution = launch(Fixtures.input(caseName), output);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus(), caseName);
        assertEquals(List.of("loadExtractsStep:COMPLETED", "calcInterestStep:COMPLETED", "exportDatasetsStep:COMPLETED"),
                steps(execution));
        for (String file : RECORDED_FILES) {
            assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(output.resolve(file)),
                    caseName + " " + file);
        }
        try (Stream<Path> written = Files.walk(output)) {
            assertEquals(RECORDED_FILES.size(), written.filter(Files::isRegularFile).count(), "no other file is exported");
        }

        // the three steps worked on one interest_run row: the one the loader created
        long run = execution.getExecutionContext().getLong(LoadExtractsStepConfig.RUN_ID_KEY);
        assertEquals(run, execution.getExecutionContext().getLong(CalcInterestTableRun.INTEREST_RUN_ID));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_run", Long.class));
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM carddemo.interest_run WHERE run_id = ?", run);
        assertEquals("EXPORTED", row.get("status"));
        // RC1: rows loaded = records of the unloaded files, counted here from the case's input
        Path input = Fixtures.input(caseName);
        assertEquals(records(input.resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")), row.get("loaded_account_count"));
        assertEquals(records(input.resolve("AWS.M2.CARDDEMO.CARDXREF.PS.txt")), row.get("loaded_card_xref_count"));
        assertEquals(records(input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt")), row.get("loaded_tran_cat_balance_count"));
        assertEquals(records(input.resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt")), row.get("loaded_disclosure_group_count"));
        // RC5: transactions inserted = records of the recorded SYSTRAN generation = records exported
        long recorded = records(Fixtures.expected(caseName).resolve(RECORDED_FILES.get(0)));
        assertEquals(recorded, row.get("written_transaction_count"));
        assertEquals(recorded, row.get("written_systran_count"));
        assertEquals(row.get("written_tran_amt_sum"), row.get("written_systran_tran_amt_sum"));
    }

    @Test
    void intR08_missingExtractStopsTheJobBeforeCalculationAndExport() throws Exception {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        try (Stream<Path> files = Files.list(Fixtures.input("i01-baseline"))) {
            for (Path file : files.toList()) {
                Files.copy(file, input.resolve(file.getFileName()));
            }
        }
        Files.delete(input.resolve("AWS.M2.CARDDEMO.CARDXREF.PS.txt"));
        Path output = temp.resolve("actual");

        JobExecution execution = launch(input, output);

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(List.of("loadExtractsStep:FAILED"), steps(execution));
        assertFalse(Files.exists(output), "a stopped run leaves no output directory (standards S6.4)");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction", Long.class));
        assertEquals(List.of("FAILED"), jdbc.queryForList("SELECT status FROM carddemo.interest_run", String.class));
    }

    @Test
    void intR08_stoppedCalculationIsNotExported() throws Exception {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        try (Stream<Path> files = Files.list(Fixtures.input("i01-baseline"))) {
            for (Path file : files.toList()) {
                Files.copy(file, input.resolve(file.getFileName()));
            }
        }
        // no rate at all: neither the group of the account nor DEFAULT is found (CBACT04C:444-459)
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt"), "");
        Path output = temp.resolve("actual");

        JobExecution execution = launch(input, output);

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(List.of("loadExtractsStep:COMPLETED", "calcInterestStep:FAILED"), steps(execution));
        assertFalse(Files.exists(output), "a stopped run leaves no output directory (standards S6.4)");
        assertEquals(List.of("FAILED"), jdbc.queryForList("SELECT status FROM carddemo.interest_run", String.class));
    }

}
