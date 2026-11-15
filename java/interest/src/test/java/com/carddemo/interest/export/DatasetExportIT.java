package com.carddemo.interest.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;

/**
 * Ticket 07 on PostgreSQL 15 with the schema of FI-4 (standards S7.3). The rows of a finished run
 * are inserted by SQL with values written by hand from the fixture READMEs and inputs; the
 * expected files are the recorded ones under fixtures/interest (S7.4). The exported directories
 * stay under target/exported/&lt;case&gt;/actual so that tools/parity/compare.py can be run on them.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class DatasetExportIT {

    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final String RUN_TIMESTAMP = "2022-07-05-10.00.00.000000";
    private static final byte FIXTURE_SPACE = 0x20;
    private static final byte HOST_SPACE = 0x40;
    private static final Path EXPORTED = Path.of("target", "exported");
    private static final List<String> BUSINESS_TABLES = List.of("account", "card_xref", "tran_cat_balance",
            "disclosure_group", "interest_transaction", "transaction_type");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("carddemo")
            .withUsername("carddemo")
            .withPassword("carddemo")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "db", "postgres", "01-transaction-type.sql")),
                    "/docker-entrypoint-initdb.d/01-transaction-type.sql")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "db", "postgres", "02-interest.sql")),
                    "/docker-entrypoint-initdb.d/02-interest.sql");

    @Autowired
    private DatasetExport export;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    @Qualifier("exportDatasetsStep")
    private Step exportDatasetsStep;

    @BeforeEach
    void emptyTables() {
        jdbc.execute("TRUNCATE carddemo.interest_transaction, carddemo.account, carddemo.interest_run");
    }

    // ---- rows of a finished run, by hand ----

    private long insertRun(String status, Long transactionCount, String amountSum, Long accountCount,
            String balanceSum) {
        return jdbc.queryForObject("""
                INSERT INTO carddemo.interest_run (parm_date, status, written_transaction_count,
                        written_tran_amt_sum, written_account_count, written_acct_curr_bal_sum)
                VALUES (?, ?, ?, ?::numeric, ?, ?::numeric) RETURNING run_id
                """, Long.class, Fixtures.PARM_DATE, status, transactionCount, amountSum, accountCount, balanceSum);
    }

    private void insertAccount(long id, String balance, String creditLimit, String cashLimit, String cycleCredit,
            String cycleDebit, String group, byte space) {
        jdbc.update("""
                INSERT INTO carddemo.account (acct_id, acct_active_status, acct_curr_bal, acct_credit_limit,
                        acct_cash_credit_limit, acct_open_date, acct_expiraion_date, acct_reissue_date,
                        acct_curr_cyc_credit, acct_curr_cyc_debit, acct_addr_zip, acct_group_id, acct_filler)
                VALUES (?, 'Y', ?::numeric, ?::numeric, ?::numeric, '2019-03-15', '2026-12-31', '2026-12-31',
                        ?::numeric, ?::numeric, '10115', ?, ?)
                """, id, balance, creditLimit, cashLimit, cycleCredit, cycleDebit, group, filled(178, space));
    }

    private void insertTransaction(long runId, int seqNo, String id, long accountId, String amount, byte space) {
        jdbc.update("""
                INSERT INTO carddemo.interest_transaction (run_id, seq_no, tran_id, tran_type_cd, tran_cat_cd,
                        tran_source, tran_desc, tran_amt, tran_merchant_id, tran_merchant_name, tran_merchant_city,
                        tran_merchant_zip, tran_card_num, tran_orig_ts, tran_proc_ts, tran_filler)
                VALUES (?, ?, ?, '01', 5, 'System', ?, ?::numeric, 0, '', '', '', ?, ?, ?, ?)
                """, runId, seqNo, id, "Int. for a/c 0000000" + accountId, amount, "400000000000" + accountId,
                RUN_TIMESTAMP, RUN_TIMESTAMP, filled(20, space));
    }

    private static byte[] filled(int length, byte value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, value);
        return bytes;
    }

    /**
     * i01-baseline after the calculation: accounts 2001 (GOLD, 1000.00 + 10.00) and 2002 (STANDARD,
     * the last account, not updated: INT-Q04); interest 10.00 and 37.00. Inserted against the
     * order of the files to prove the ORDER BY.
     */
    private long finishedBaseline(byte space) {
        long runId = insertRun("CALCULATED", 2L, "47.00", 1L, "3410.00");
        insertAccount(2002, "2400.00", "6000.00", "3000.00", "2400.00", "0.00", "STANDARD", space);
        insertAccount(2001, "1010.00", "5000.00", "2500.00", "0.00", "0.00", "GOLD", space);
        insertTransaction(runId, 2, "2022071800000002", 2002, "37.00", space);
        insertTransaction(runId, 1, "2022071800000001", 2001, "10.00", space);
        return runId;
    }

    /** i06-negative-balance after the calculation: one account at -300.00 and interest of -3.00. */
    private long finishedNegativeBalance() {
        long runId = insertRun("CALCULATED", 1L, "-3.00", 0L, "-300.00");
        insertAccount(2008, "-300.00", "5000.00", "2500.00", "0.00", "300.00", "GOLD", FIXTURE_SPACE);
        insertTransaction(runId, 1, "2022071800000001", 2008, "-3.00", FIXTURE_SPACE);
        return runId;
    }

    // ---- helpers ----

    /** An output directory as the calculation step leaves it: the run log, and here a stale file too. */
    private static Path outputOfCalculation(String caseName, String name) throws IOException {
        Path output = EXPORTED.resolve(name).resolve("actual");
        deleteTree(output);
        Files.createDirectories(output.resolve("sysout"));
        Files.copy(Fixtures.expected(caseName).resolve(RUN_LOG), output.resolve(RUN_LOG));
        Files.createDirectories(output.resolve("datasets"));
        Files.writeString(output.resolve("datasets/STALE.FROM.AN.EARLIER.EXPORT.txt"), "stale");
        return output;
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

    private static List<String> filesOf(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile).map(p -> dir.relativize(p).toString().replace('\\', '/'))
                    .sorted().toList();
        }
    }

    private static void assertSameAsRecorded(String caseName, Path output) throws IOException {
        for (String file : List.of(TRANSACTIONS + ".txt", ACCOUNTS + ".txt", "rc.json", RUN_LOG)) {
            assertArrayEquals(Files.readAllBytes(Fixtures.expected(caseName).resolve(file)),
                    Files.readAllBytes(output.resolve(file)), file);
        }
        assertEquals(List.of(ACCOUNTS + ".txt", TRANSACTIONS + ".txt", "rc.json", RUN_LOG), filesOf(output));
    }

    private Map<String, String> businessRows() {
        Map<String, String> rows = new LinkedHashMap<>();
        for (String table : BUSINESS_TABLES) {
            rows.put(table, jdbc.queryForObject("SELECT count(*) || ':' || coalesce(md5(string_agg(t::text, '|' "
                    + "ORDER BY t::text)), '') FROM carddemo." + table + " t", String.class));
        }
        return rows;
    }

    private String runWithoutStatus(long runId) {
        return jdbc.queryForObject(
                "SELECT (to_jsonb(r) - 'status')::text FROM carddemo.interest_run r WHERE run_id = ?", String.class,
                runId);
    }

    private String statusOf(long runId) {
        return jdbc.queryForObject("SELECT status FROM carddemo.interest_run WHERE run_id = ?", String.class, runId);
    }

    // ---- acceptance ----

    @Test
    void i01Baseline_fixtureFormat_equalsRecordedFiles_andReplacesTheDirectory() throws Exception {
        long runId = finishedBaseline(FIXTURE_SPACE);
        Path output = outputOfCalculation("i01-baseline", "i01-baseline");

        DatasetExport.Exported exported = export.export(runId, output, ExportFormat.FIXTURE);

        assertSameAsRecorded("i01-baseline", output);
        assertEquals(new DatasetExport.Exported(runId, 2, new BigDecimal("47.00"), 2), exported);
        assertFalse(Files.exists(output.resolveSibling("actual.export-staging-" + runId)));
    }

    @Test
    void i06NegativeBalance_fixtureFormat_equalsRecordedFiles() throws Exception {
        long runId = finishedNegativeBalance();
        Path output = outputOfCalculation("i06-negative-balance", "i06-negative-balance");

        export.export(runId, output, ExportFormat.FIXTURE);

        assertSameAsRecorded("i06-negative-balance", output);
    }

    @Test
    void i01Baseline_hostFormat_fixedLengthRecords_decodeToTheSameFieldValues() throws Exception {
        long runId = finishedBaseline(HOST_SPACE);
        Path output = outputOfCalculation("i01-baseline", "i01-baseline-host");

        export.export(runId, output, ExportFormat.HOST);

        assertEquals(List.of(ACCOUNTS, TRANSACTIONS, "rc.json", RUN_LOG), filesOf(output));
        byte[] transactions = Files.readAllBytes(output.resolve(TRANSACTIONS));
        byte[] accounts = Files.readAllBytes(output.resolve(ACCOUNTS));
        assertEquals(2 * 350, transactions.length);
        assertEquals(2 * 300, accounts.length);

        RecordValues first = RecordFormat.host().codec(TransactionRecord.LAYOUT)
                .decode(Arrays.copyOfRange(transactions, 0, 350), new RecordOrigin(TRANSACTIONS, 1));
        assertEquals("2022071800000001", first.text("TRAN-ID"));
        assertEquals("01", first.text("TRAN-TYPE-CD"));
        assertEquals(5, first.unsigned("TRAN-CAT-CD"));
        assertEquals("System", first.text("TRAN-SOURCE"));
        assertEquals("Int. for a/c 00000002001", first.text("TRAN-DESC"));
        assertEquals(new BigDecimal("10.00"), first.signed("TRAN-AMT"));
        assertEquals(0, first.unsigned("TRAN-MERCHANT-ID"));
        assertEquals("", first.text("TRAN-MERCHANT-NAME"));
        assertEquals("", first.text("TRAN-MERCHANT-CITY"));
        assertEquals("", first.text("TRAN-MERCHANT-ZIP"));
        assertEquals("4000000000002001", first.text("TRAN-CARD-NUM"));
        assertEquals(RUN_TIMESTAMP, first.text("TRAN-ORIG-TS"));
        assertEquals(RUN_TIMESTAMP, first.text("TRAN-PROC-TS"));
        assertArrayEquals(filled(20, HOST_SPACE), Arrays.copyOfRange(transactions, 330, 350));
        RecordValues second = RecordFormat.host().codec(TransactionRecord.LAYOUT)
                .decode(Arrays.copyOfRange(transactions, 350, 700), new RecordOrigin(TRANSACTIONS, 2));
        assertEquals("2022071800000002", second.text("TRAN-ID"));
        assertEquals(new BigDecimal("37.00"), second.signed("TRAN-AMT"));
        assertEquals("4000000000002002", second.text("TRAN-CARD-NUM"));

        RecordValues gold = RecordFormat.host().codec(AccountRecord.LAYOUT)
                .decode(Arrays.copyOfRange(accounts, 0, 300), new RecordOrigin(ACCOUNTS, 1));
        assertEquals(2001, gold.unsigned("ACCT-ID"));
        assertEquals("Y", gold.text("ACCT-ACTIVE-STATUS"));
        assertEquals(new BigDecimal("1010.00"), gold.signed("ACCT-CURR-BAL"));
        assertEquals(new BigDecimal("5000.00"), gold.signed("ACCT-CREDIT-LIMIT"));
        assertEquals(new BigDecimal("2500.00"), gold.signed("ACCT-CASH-CREDIT-LIMIT"));
        assertEquals("2019-03-15", gold.text("ACCT-OPEN-DATE"));
        assertEquals("2026-12-31", gold.text("ACCT-EXPIRAION-DATE"));
        assertEquals("2026-12-31", gold.text("ACCT-REISSUE-DATE"));
        assertEquals(new BigDecimal("0.00"), gold.signed("ACCT-CURR-CYC-CREDIT"));
        assertEquals(new BigDecimal("0.00"), gold.signed("ACCT-CURR-CYC-DEBIT"));
        assertEquals("10115", gold.text("ACCT-ADDR-ZIP"));
        assertEquals("GOLD", gold.text("ACCT-GROUP-ID"));
        assertArrayEquals(filled(178, HOST_SPACE), Arrays.copyOfRange(accounts, 122, 300));
        RecordValues standard = RecordFormat.host().codec(AccountRecord.LAYOUT)
                .decode(Arrays.copyOfRange(accounts, 300, 600), new RecordOrigin(ACCOUNTS, 2));
        assertEquals(2002, standard.unsigned("ACCT-ID"));
        assertEquals(new BigDecimal("2400.00"), standard.signed("ACCT-CURR-BAL"));
        assertEquals(new BigDecimal("2400.00"), standard.signed("ACCT-CURR-CYC-CREDIT"));
        assertEquals("STANDARD", standard.text("ACCT-GROUP-ID"));
        // E1/E2 in the host code page: zoned digits F0-F9, the sign in the zone of the last byte (C = +).
        assertEquals((byte) 0xF2, transactions[0]);
        assertEquals((byte) 0xC0, transactions[142]);
    }

    @Test
    void order_isSeqNoAndAcctId_whateverTheInsertOrder() throws Exception {
        long runId = finishedBaseline(FIXTURE_SPACE);
        Path output = outputOfCalculation("i01-baseline", "order");

        export.export(runId, output, ExportFormat.FIXTURE);

        assertEquals(List.of("2022071800000001", "2022071800000002"),
                Fixtures.lines(output.resolve(TRANSACTIONS + ".txt")).stream().map(l -> l.substring(0, 16)).toList());
        assertEquals(List.of("00000002001", "00000002002"),
                Fixtures.lines(output.resolve(ACCOUNTS + ".txt")).stream().map(l -> l.substring(0, 11)).toList());
    }

    @Test
    void transactionsOfAnotherRun_areNotExported() throws Exception {
        long otherRun = insertRun("EXPORTED", 1L, "9.99", 0L, "0.00");
        insertTransaction(otherRun, 1, "2022071800000001", 2002, "9.99", FIXTURE_SPACE);
        long runId = finishedBaseline(FIXTURE_SPACE);
        Path output = outputOfCalculation("i01-baseline", "other-run");

        export.export(runId, output, ExportFormat.FIXTURE);

        assertSameAsRecorded("i01-baseline", output);
    }

    @Test
    void runThatIsNotCalculated_isRefused_andNothingIsWrittenOrChanged() throws Exception {
        for (String status : List.of("LOADED", "EXPORTED", "FAILED", "ABANDONED")) {
            emptyTables();
            long runId = finishedBaseline(FIXTURE_SPACE);
            jdbc.update("UPDATE carddemo.interest_run SET status = ? WHERE run_id = ?", status, runId);
            Path output = outputOfCalculation("i01-baseline", "refused");
            List<String> filesBefore = filesOf(output);
            Map<String, String> rowsBefore = businessRows();

            ExportRefusedException refused = assertThrows(ExportRefusedException.class,
                    () -> export.export(runId, output, ExportFormat.FIXTURE));

            assertTrue(refused.getMessage().contains("status " + status), refused.getMessage());
            assertEquals(filesBefore, filesOf(output), "no SYSTRAN generation after a refusal (S6.4)");
            assertEquals(List.of("actual"), filesAndDirectoriesNextTo(output));
            assertEquals(status, statusOf(runId));
            assertEquals(rowsBefore, businessRows());
        }
    }

    private static List<String> filesAndDirectoriesNextTo(Path output) throws IOException {
        try (Stream<Path> paths = Files.list(output.getParent())) {
            return paths.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void runThatDoesNotExist_isRefused() throws Exception {
        Path output = outputOfCalculation("i01-baseline", "no-run");
        assertThrows(ExportRefusedException.class, () -> export.export(987654321L, output, ExportFormat.FIXTURE));
        assertFalse(Files.exists(output.resolve(TRANSACTIONS + ".txt")));
    }

    @Test
    void export_changesNoRowExceptTheStatusOfTheRun_andTheExportedFiguresEqualRc5() throws Exception {
        long runId = finishedBaseline(FIXTURE_SPACE);
        Path output = outputOfCalculation("i01-baseline", "database");
        Map<String, String> rowsBefore = businessRows();
        String runBefore = runWithoutStatus(runId);

        DatasetExport.Exported exported = export.export(runId, output, ExportFormat.FIXTURE);

        assertEquals(rowsBefore, businessRows());
        assertEquals(runBefore, runWithoutStatus(runId));
        assertEquals("EXPORTED", statusOf(runId));
        // RC5 on i01-baseline (strategy-interest.md): 2 transactions, sum of TRAN-AMT 47.00.
        assertEquals(2, exported.transactionCount());
        assertEquals(new BigDecimal("47.00"), exported.amountSum());
        assertEquals(2, Fixtures.lines(output.resolve(TRANSACTIONS + ".txt")).size());
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM carddemo.interest_run
                 WHERE run_id = ? AND written_transaction_count = 2 AND written_tran_amt_sum = 47.00
                """, Integer.class, runId));
    }

    @Test
    void runWhoseRowsDoNotAgreeWithRc5_isRefused() throws Exception {
        long runId = finishedBaseline(FIXTURE_SPACE);
        jdbc.update("DELETE FROM carddemo.interest_transaction WHERE run_id = ? AND seq_no = 2", runId);
        Path output = outputOfCalculation("i01-baseline", "rc5");

        ExportRefusedException refused = assertThrows(ExportRefusedException.class,
                () -> export.export(runId, output, ExportFormat.FIXTURE));

        assertTrue(refused.getMessage().contains("RC5"), refused.getMessage());
        assertFalse(Files.exists(output.resolve(TRANSACTIONS + ".txt")));
        assertEquals("CALCULATED", statusOf(runId));
    }

    @Test
    void exportDatasetsStep_exportsTheRunOfTheJobExecution_andARepeatIsRefused() throws Exception {
        long runId = finishedBaseline(FIXTURE_SPACE);
        Path output = outputOfCalculation("i01-baseline", "step");
        Step runCreated = new StepBuilder("runCreatedByAnEarlierStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    contribution.getStepExecution().getJobExecution().getExecutionContext()
                            .putLong(ExportDatasetsTasklet.RUN_ID_KEY, runId);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
        Job job = new JobBuilder("exportOnlyJob", jobRepository).start(runCreated).next(exportDatasetsStep).build();

        JobExecution first = launch(job, output);
        assertEquals(BatchStatus.COMPLETED, first.getStatus());
        assertSameAsRecorded("i01-baseline", output);
        assertEquals("EXPORTED", statusOf(runId));

        JobExecution second = launch(job, output);
        assertEquals(BatchStatus.FAILED, second.getStatus());
        assertSameAsRecorded("i01-baseline", output);
    }

    private JobExecution launch(Job job, Path output) throws Exception {
        return jobLauncher.run(job, new JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .addString("outputDir", output.toString())
                .addString("format", "fixture")
                .toJobParameters());
    }

    @Test
    void rcJson_isInTheFixtureCodePage_withoutFinalNewline() throws Exception {
        long runId = finishedNegativeBalance();
        Path output = outputOfCalculation("i06-negative-balance", "rc");

        export.export(runId, output, ExportFormat.HOST);

        String rc = Files.readString(output.resolve("rc.json"), StandardCharsets.ISO_8859_1);
        assertEquals(Fixtures.text(Fixtures.expected("i06-negative-balance").resolve("rc.json")), rc);
        assertFalse(rc.endsWith("\n"));
    }
}
