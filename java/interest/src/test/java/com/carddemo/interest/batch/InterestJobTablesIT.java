package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
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
import com.carddemo.interest.jdbc.calc.ReplicaTables;

/**
 * interestCalcJob with its three steps (profile tables: interest.job.steps=load-calc-export on
 * interest.calc.store=jdbc), PostgreSQL 15 with the schema of FI-4 (standards S7.3). Nothing but
 * the job touches the tables: loadExtractsStep fills them from the case's input directory,
 * calcInterestStep calculates on them, exportDatasetsStep writes the four files the comparator
 * reads. Every expectation is the recorded legacy run (standards S7.4) or a figure of the RC table
 * of modernization/design/strategy-interest.md, written here by hand. Each case leaves
 * target/parity-job/&lt;case&gt;/actual for tools/parity/compare.py.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@ActiveProfiles("tables")
@Testcontainers
class InterestJobTablesIT {

    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final String RETURN_CODES = "rc.json";
    private static final Path PARITY_OUTPUT = Path.of("target", "parity-job");
    private static final List<String> INPUT_FILES = List.of("AWS.M2.CARDDEMO.ACCTDATA.PS.txt",
            "AWS.M2.CARDDEMO.CARDXREF.PS.txt", "AWS.M2.CARDDEMO.TCATBALF.PS.txt", "AWS.M2.CARDDEMO.DISCGRP.PS.txt");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = ReplicaTables.postgres();

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    private static JobParameters parametersOfANewRun(Path inputDir, Path outputDir) {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, inputDir.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters();
    }

    private List<String> stepsOf(JobExecution execution) {
        return jdbc.queryForList("SELECT step_name || ':' || status FROM batch.batch_step_execution"
                + " WHERE job_execution_id = ? ORDER BY step_execution_id", String.class, execution.getId());
    }

    private Map<String, Object> runOf(JobExecution execution) {
        return jdbc.queryForMap("SELECT * FROM carddemo.interest_run WHERE run_id = ?",
                execution.getExecutionContext().getLong("interestRunId"));
    }

    private long rows(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM carddemo." + table, Long.class);
    }

    private static String recorded(String caseName, String file) {
        return Fixtures.text(Fixtures.expected(caseName).resolve(file));
    }

    private static Path copyOfInput(String caseName, Path target) throws IOException {
        Files.createDirectories(target);
        for (String file : INPUT_FILES) {
            Files.copy(Fixtures.input(caseName).resolve(file), target.resolve(file));
        }
        return target;
    }

    private static void deleteTree(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    /**
     * The seven recorded cases through the database. The figures are the RC table of
     * strategy-interest.md: accounts, cards, balances (the rates are 15 with sum 120.50 in every
     * case), sum of ACCT-CURR-BAL before, sum of TRAN-CAT-BAL, transactions written, sum of
     * TRAN-AMT, sum of ACCT-CURR-BAL after, interest of the account that ends the input, which
     * is not added to its balance (INT-Q04).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "i01-baseline,               2, 2, 2, 3400.00, 3400.00, 2, 47.00, 3410.00, 37.00",
            "i02-tie-half-cent,          2, 2, 2, 2470.00, 2470.00, 2, 24.69, 2482.35, 12.34",
            "i03-zero-rate-skipped,      1, 1, 2,  700.00,  700.00, 1, 10.00,  700.00, 10.00",
            "i04-default-group-fallback, 1, 1, 1, 1000.00, 1000.00, 1, 17.50, 1000.00, 17.50",
            "i05-multi-category,         1, 1, 2, 1500.00, 1500.00, 2, 17.50, 1500.00, 17.50",
            "i06-negative-balance,       1, 1, 1, -300.00, -300.00, 1, -3.00, -300.00, -3.00",
            "i07-nontie-rounding,        2, 2, 2, 1778.10, 1778.10, 2, 29.03, 1793.52, 13.61"})
    void fixtureCase_loadCalcExportGiveTheRecordedFiles(String caseName, long accounts, long cards, long balances,
            BigDecimal balanceSumBefore, BigDecimal categoryBalanceSum, long transactions, BigDecimal amountSum,
            BigDecimal balanceSumAfter, BigDecimal interestOfLastAccount) throws Exception {
        Path output = PARITY_OUTPUT.resolve(caseName).resolve("actual");
        deleteTree(output);

        JobExecution execution = jobLauncher.run(interestCalcJob, parametersOfANewRun(Fixtures.input(caseName), output));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(List.of("loadExtractsStep:COMPLETED", "calcInterestStep:COMPLETED", "exportDatasetsStep:COMPLETED"),
                stepsOf(execution));
        // FI-2: the four files the comparator reads, byte for byte, and nothing else
        for (String file : List.of(TRANSACTIONS, ACCOUNTS, RUN_LOG, RETURN_CODES)) {
            assertEquals(recorded(caseName, file), Fixtures.text(output.resolve(file)), file);
        }
        try (Stream<Path> files = Files.walk(output)) {
            assertEquals(4, files.filter(Files::isRegularFile).count(), "files under the output directory");
        }
        // the export step was handed the run the load opened and the calculation continued
        assertEquals(execution.getExecutionContext().getLong("interestRunId"),
                execution.getExecutionContext().getLong("interest.run.id"));
        Map<String, Object> run = runOf(execution);
        assertEquals("EXPORTED", run.get("status"));
        // RC1: row counts of the four tables as loaded
        assertEquals(accounts, run.get("loaded_account_count"));
        assertEquals(cards, run.get("loaded_card_xref_count"));
        assertEquals(balances, run.get("loaded_tran_cat_balance_count"));
        assertEquals(15L, run.get("loaded_disclosure_group_count"));
        assertEquals(accounts, rows("account"));
        assertEquals(cards, rows("card_xref"));
        assertEquals(balances, rows("tran_cat_balance"));
        assertEquals(15L, rows("disclosure_group"));
        // RC2: sums as loaded
        assertEquals(balanceSumBefore, run.get("loaded_acct_curr_bal_sum"));
        assertEquals(categoryBalanceSum, run.get("loaded_tran_cat_bal_sum"));
        assertEquals(new BigDecimal("120.50"), run.get("loaded_dis_int_rate_sum"));
        // RC5: transactions written = records exported; sum of TRAN-AMT = balance change + interest of the last account
        assertEquals(transactions, run.get("written_transaction_count"));
        assertEquals(transactions, (long) Fixtures.lines(output.resolve(TRANSACTIONS)).size());
        assertEquals(amountSum, run.get("written_tran_amt_sum"));
        assertEquals(balanceSumAfter, run.get("written_acct_curr_bal_sum"));
        assertEquals(amountSum, balanceSumAfter.subtract(balanceSumBefore).add(interestOfLastAccount));
        assertEquals(balanceSumAfter, jdbc.queryForObject("SELECT sum(acct_curr_bal) FROM carddemo.account",
                BigDecimal.class));
    }

    @Test
    void intR08_rejectedRecordStopsTheJobInTheLoad() throws Exception {
        // DIS-TRAN-CAT-CD of the second rate is not numeric: the load rejects the record and nothing follows
        Path input = copyOfInput("i01-baseline", temp.resolve("input"));
        Path rates = input.resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt");
        List<String> lines = Files.readAllLines(rates, StandardCharsets.ISO_8859_1);
        lines.set(1, lines.get(1).substring(0, 12) + "X" + lines.get(1).substring(13));
        Files.write(rates, lines, StandardCharsets.ISO_8859_1);
        Path output = temp.resolve("actual");

        JobExecution execution = jobLauncher.run(interestCalcJob, parametersOfANewRun(input, output));

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(List.of("loadExtractsStep:FAILED"), stepsOf(execution));
        assertFalse(Files.exists(output.resolve("datasets")), "no dataset of a stopped run");
        assertFalse(Files.exists(output.resolve(RETURN_CODES)), "no rc.json of a stopped run");
    }

    @Test
    void intR08_missingAccountStopsTheJobInTheCalculation() throws Exception {
        // i01-baseline without the record of account 2002: its balance has no account (CBACT04C:373-390).
        // Account 2001 ends no input, so its 10.00 would be posted, but the chunk of both accounts is rolled back.
        Path input = copyOfInput("i01-baseline", temp.resolve("input"));
        Path accounts = input.resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt");
        List<String> lines = Files.readAllLines(accounts, StandardCharsets.ISO_8859_1);
        assertTrue(lines.get(1).startsWith("00000002002"));
        Files.write(accounts, lines.subList(0, 1), StandardCharsets.ISO_8859_1);
        Path output = temp.resolve("actual");

        JobExecution execution = jobLauncher.run(interestCalcJob, parametersOfANewRun(input, output));

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(List.of("loadExtractsStep:COMPLETED", "calcInterestStep:FAILED"), stepsOf(execution));
        assertEquals("FAILED", runOf(execution).get("status"));
        // S6.4: no SYSTRAN generation and no rc.json of a stopped run; the run log keeps the legacy lines
        assertFalse(Files.exists(output.resolve("datasets")), "no dataset of a stopped run");
        assertFalse(Files.exists(output.resolve(RETURN_CODES)), "no rc.json of a stopped run");
        assertEquals(List.of(
                "START OF EXECUTION OF PROGRAM CBACT04C",
                "000000020010100010000010000{                      ",
                "000000020020100010000024000{                      ",
                "ACCOUNT NOT FOUND: 00000002002",
                "ERROR READING ACCOUNT FILE",
                "FILE STATUS IS: NNNN0023",
                "ABENDING PROGRAM"), Fixtures.lines(output.resolve(RUN_LOG)));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction WHERE run_id = ?",
                Integer.class, execution.getExecutionContext().getLong("interestRunId")));
    }
}
