package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.file.RecordedReturnCodesForTests;
import com.carddemo.interest.jdbc.calc.ReplicaTables;

/**
 * calcInterestStep on the tables (interest.calc.store=jdbc), PostgreSQL 15 with the schema of FI-4
 * (standards S7.3). Every expectation is the recorded legacy run of the case, byte for byte
 * (standards S7.4). The tables are filled and read back by test support that stands in for the
 * loader and the export; each case also leaves target/parity-tables/&lt;case&gt;/actual for
 * tools/parity/compare.py.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.store=jdbc",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class CalcInterestTablesIT {

    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final Path PARITY_OUTPUT = Path.of("target", "parity-tables");

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

    private JobExecution launch(String caseName, Path outputDir) throws Exception {
        return jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, Fixtures.input(caseName).toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
    }

    private String transactionsOfTheOnlyRun() {
        return ReplicaTables.fileContent(jdbc, ReplicaTables.TRANSACTION, "");
    }

    private static String recorded(String caseName, String file) {
        return Fixtures.text(Fixtures.expected(caseName).resolve(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void fixtureCase_tablesAndRunLogMatchRecorded(String caseName) throws Exception {
        ReplicaTables.load(jdbc, Fixtures.input(caseName));
        ReplicaTables.refuseUpdatesOfOtherAccountColumns(jdbc);
        String balancesBefore = ReplicaTables.rowHash(jdbc, ReplicaTables.BALANCE);
        String cardsBefore = ReplicaTables.rowHash(jdbc, ReplicaTables.CARD_XREF);
        String ratesBefore = ReplicaTables.rowHash(jdbc, ReplicaTables.RATE);
        String otherAccountColumnsBefore = ReplicaTables.accountHashWithoutPostedColumns(jdbc);
        Path output = PARITY_OUTPUT.resolve(caseName).resolve("actual");
        deleteTree(output);

        assertEquals(BatchStatus.COMPLETED, launch(caseName, output).getStatus());

        // interest_transaction in seq_no order equals the recorded SYSTRAN, account the recorded ACCTDATA
        String transactions = transactionsOfTheOnlyRun();
        String accounts = ReplicaTables.fileContent(jdbc, ReplicaTables.ACCOUNT, "");
        assertEquals(recorded(caseName, TRANSACTIONS), transactions, TRANSACTIONS);
        assertEquals(recorded(caseName, ACCOUNTS), accounts, ACCOUNTS);
        // INT-T02: the run log the step wrote is the recorded SYSOUT
        assertEquals(recorded(caseName, RUN_LOG), Fixtures.text(output.resolve(RUN_LOG)), RUN_LOG);
        // database assertions of the ticket
        assertEquals(balancesBefore, ReplicaTables.rowHash(jdbc, ReplicaTables.BALANCE), "tran_cat_balance");
        assertEquals(cardsBefore, ReplicaTables.rowHash(jdbc, ReplicaTables.CARD_XREF), "card_xref");
        assertEquals(ratesBefore, ReplicaTables.rowHash(jdbc, ReplicaTables.RATE), "disclosure_group");
        assertEquals(otherAccountColumnsBefore, ReplicaTables.accountHashWithoutPostedColumns(jdbc), "account");
        List<Integer> seqNos = jdbc.queryForList("SELECT seq_no FROM carddemo.interest_transaction ORDER BY seq_no",
                Integer.class);
        assertEquals(IntStream.rangeClosed(1, seqNos.size()).boxed().toList(), seqNos, "seq_no is 1..n");

        // for tools/parity/compare.py: the tables in the recorded file format, rc.json as the file adapters write it
        Files.createDirectories(output.resolve("datasets"));
        Files.writeString(output.resolve(TRANSACTIONS), transactions, StandardCharsets.ISO_8859_1);
        Files.writeString(output.resolve(ACCOUNTS), accounts, StandardCharsets.ISO_8859_1);
        Files.writeString(output.resolve("rc.json"), RecordedReturnCodesForTests.json(InterestExitCode.COMPLETED),
                StandardCharsets.ISO_8859_1);
    }

    @Test
    void intR07_runControlHoldsTheWrittenFigures() throws Exception {
        // i01-baseline: 10.00 for account 2001 and 37.00 for 2002; 2001 is updated to 1010.00, 2002 (INT-Q04)
        // stays 2400.00: 2 transactions, 47.00, 1 account, 3410.00
        ReplicaTables.load(jdbc, Fixtures.input("i01-baseline"));

        assertEquals(BatchStatus.COMPLETED, launch("i01-baseline", temp.resolve("actual")).getStatus());

        Map<String, Object> run = jdbc.queryForMap("SELECT * FROM carddemo.interest_run");
        assertEquals("CALCULATED", run.get("status"));
        assertEquals(Fixtures.PARM_DATE, run.get("parm_date"));
        assertEquals(2L, run.get("written_transaction_count"));
        assertEquals(new BigDecimal("47.00"), run.get("written_tran_amt_sum"));
        assertEquals(1L, run.get("written_account_count"));
        assertEquals(new BigDecimal("3410.00"), run.get("written_acct_curr_bal_sum"));
    }

    @Test
    void intH02_secondRunWithTheSameParmIsNotRefused() throws Exception {
        // rerunGuard stays OFF: a new runId is a new run on the tables as the first run left them. The category
        // balance of 2001 is still 1000.00, so it is charged 1000.00 * 12.00 / 1200 = 10.00 again: 1010.00 -> 1020.00
        ReplicaTables.load(jdbc, Fixtures.input("i01-baseline"));

        assertEquals(BatchStatus.COMPLETED, launch("i01-baseline", temp.resolve("first")).getStatus());
        assertEquals(BatchStatus.COMPLETED, launch("i01-baseline", temp.resolve("second")).getStatus());

        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_run WHERE status = 'CALCULATED'",
                Integer.class));
        assertEquals(List.of("2022071800000001", "2022071800000002", "2022071800000001", "2022071800000002"),
                jdbc.queryForList("SELECT tran_id FROM carddemo.interest_transaction ORDER BY run_id, seq_no", String.class));
        assertEquals(new BigDecimal("1020.00"),
                jdbc.queryForObject("SELECT acct_curr_bal FROM carddemo.account WHERE acct_id = 2001", BigDecimal.class));
    }

    @Test
    void emptyInput_writesStartAndEndOnly() throws Exception {
        // INT-T09 / behaviour m: no balance at all - no transaction, no update, the two fixed lines
        ReplicaTables.load(jdbc, Fixtures.input("i01-baseline"));
        jdbc.execute("TRUNCATE carddemo.tran_cat_balance");
        String accountsBefore = ReplicaTables.rowHash(jdbc, ReplicaTables.ACCOUNT);

        assertEquals(BatchStatus.COMPLETED, launch("i01-baseline", temp.resolve("actual")).getStatus());

        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "END OF EXECUTION OF PROGRAM CBACT04C"),
                Fixtures.lines(temp.resolve("actual").resolve(RUN_LOG)));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction", Integer.class));
        assertEquals(accountsBefore, ReplicaTables.rowHash(jdbc, ReplicaTables.ACCOUNT));
    }

    private static void deleteTree(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }
}
