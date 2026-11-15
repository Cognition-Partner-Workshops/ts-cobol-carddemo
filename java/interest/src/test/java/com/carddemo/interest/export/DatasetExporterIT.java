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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.batch.InterestJobParameters;
import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.load.ExtractFormat;
import com.carddemo.interest.load.ExtractLoader;

/**
 * INT-T07 on PostgreSQL 15 in a container (standards S7.3), with the init scripts of the db service.
 * A finished run is put into the tables by SQL from the recorded files of a case: the loader fills
 * the four replica tables from the case's input, then the test inserts the recorded SYSTRAN records
 * into interest_transaction, sets the account columns the calculation updates to the recorded
 * ACCTDATA values and marks the run CALCULATED. The calculation step itself is not involved.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class DatasetExporterIT {

    private static final String BASELINE = "i01-baseline";
    private static final String SYSTRAN = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    private static final String ACCTDATA = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final Path EXPORTED = Path.of("target", "exported");
    private static final List<String> ALL_TABLES = List.of("account", "card_xref", "tran_cat_balance", "disclosure_group",
            "interest_transaction", "interest_run");

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
    private DatasetExporter exporter;
    @Autowired
    private ExtractLoader loader;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    @Qualifier(ExportDatasetsStepConfig.STEP_NAME)
    private Step exportDatasetsStep;

    @TempDir
    Path temp;

    private static List<DecodedRecord> recorded(String caseName, String dataset, RecordLayout layout) {
        List<DecodedRecord> records = new ArrayList<>();
        RecordSource source = RecordSource.built(dataset);
        for (String line : Fixtures.lines(Fixtures.expected(caseName).resolve(dataset + ".txt"))) {
            records.add(layout.decode(RecordImage.bytes(RecordImage.fromLine(line, layout, source)), CodePage.FIXTURE, source));
        }
        return records;
    }

    private static String column(LayoutField field, String fillerColumn) {
        return field.kind() == FieldKind.FILLER ? fillerColumn : field.name().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static Object value(DecodedRecord record, LayoutField field) {
        return switch (field.kind()) {
            case FILLER -> record.filler(field.name());
            case TEXT -> record.text(field.name());
            default -> record.number(field.name());
        };
    }

    /** The tables after a finished run of the case, by SQL; the run is CALCULATED with its RC5 figures. */
    private long finishedRun(String caseName) {
        long runId = loader.load(Fixtures.input(caseName), ExtractFormat.FIXTURE, Fixtures.PARM_DATE,
                temp.resolve("rejects-" + UUID.randomUUID() + ".txt")).runId();
        RecordLayout layout = CardDemoLayouts.TRANSACTION;
        List<String> columns = layout.fields().stream().map(field -> column(field, "tran_filler")).toList();
        String insert = "INSERT INTO carddemo.interest_transaction (run_id, seq_no, " + String.join(", ", columns) + ") VALUES (?, ?"
                + ", ?".repeat(columns.size()) + ")";
        List<DecodedRecord> transactions = recorded(caseName, SYSTRAN, layout);
        for (int i = 0; i < transactions.size(); i++) {
            List<Object> values = new ArrayList<>(List.of(runId, i + 1));
            for (LayoutField field : layout.fields()) {
                values.add(value(transactions.get(i), field));
            }
            jdbc.update(insert, values.toArray());
        }
        for (DecodedRecord account : recorded(caseName, ACCTDATA, CardDemoLayouts.ACCOUNT)) {
            assertEquals(1, jdbc.update("""
                    UPDATE carddemo.account SET acct_curr_bal = ?, acct_curr_cyc_credit = ?, acct_curr_cyc_debit = ?
                     WHERE acct_id = ?""",
                    account.number("ACCT-CURR-BAL"), account.number("ACCT-CURR-CYC-CREDIT"),
                    account.number("ACCT-CURR-CYC-DEBIT"), account.number("ACCT-ID")));
        }
        jdbc.update("""
                UPDATE carddemo.interest_run
                   SET status = 'CALCULATED',
                       written_transaction_count = (SELECT COUNT(*) FROM carddemo.interest_transaction WHERE run_id = ?),
                       written_tran_amt_sum = (SELECT COALESCE(SUM(tran_amt), 0) FROM carddemo.interest_transaction WHERE run_id = ?),
                       written_account_count = 1,
                       written_acct_curr_bal_sum = (SELECT SUM(acct_curr_bal) FROM carddemo.account)
                 WHERE run_id = ?""", runId, runId, runId);
        return runId;
    }

    /** Every row of every table of the stream as text, in a fixed order; interest_run rows optionally left out. */
    private List<String> rows(boolean withInterestRun) {
        List<String> rows = new ArrayList<>();
        for (String table : ALL_TABLES) {
            if (withInterestRun || !table.equals("interest_run")) {
                rows.addAll(jdbc.queryForList("SELECT '" + table + " ' || t::text FROM carddemo." + table + " t ORDER BY 1", String.class));
            }
        }
        return rows;
    }

    private Map<String, Object> run(long runId) {
        return jdbc.queryForMap("SELECT * FROM carddemo.interest_run WHERE run_id = ?", runId);
    }

    private static List<String> filesOf(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile).map(path -> dir.relativize(path).toString().replace('\\', '/')).sorted().toList();
        }
    }

    private static void assertSameFile(String caseName, Path out, String file) {
        assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(out.resolve(file)), caseName + " " + file);
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void finishedRun_exportsTheRecordedDatasetsAndReturnCodes(String caseName) throws IOException {
        long runId = finishedRun(caseName);
        // Kept under target/ so that tools/parity/compare.py can be run on it (INT-T07 final-revision evidence).
        // The run log is the calculation step's file; the recorded one stands in for it here.
        Path out = EXPORTED.resolve(caseName).resolve("actual");
        Files.createDirectories(out.resolve("sysout"));
        Files.copy(Fixtures.expected(caseName).resolve(RUN_LOG), out.resolve(RUN_LOG), StandardCopyOption.REPLACE_EXISTING);

        ExportResult result = exporter.export(runId, out, ExtractFormat.FIXTURE);

        assertSameFile(caseName, out, SYSTRAN + ".txt");
        assertSameFile(caseName, out, ACCTDATA + ".txt");
        assertSameFile(caseName, out, "rc.json");
        assertSameFile(caseName, out, RUN_LOG);
        assertEquals(List.of(ACCTDATA + ".txt", SYSTRAN + ".txt", "rc.json", RUN_LOG), filesOf(out));
        System.out.printf("RC5 %s: transactions %d, SUM(tran_amt) %s, accounts %d, SUM(acct_curr_bal) %s%n", caseName,
                result.transactionCount(), result.tranAmtSum(), result.accountCount(), result.acctCurrBalSum());
    }

    @Test
    void runLogOfTheCalculationStep_staysInTheSameDirectory() throws IOException {
        long runId = finishedRun(BASELINE);
        Path out = temp.resolve("out");
        Files.createDirectories(out.resolve("sysout"));
        Files.copy(Fixtures.expected(BASELINE).resolve(RUN_LOG), out.resolve(RUN_LOG));

        exporter.export(runId, out, ExtractFormat.FIXTURE);

        assertSameFile(BASELINE, out, RUN_LOG);
        assertEquals(List.of(ACCTDATA + ".txt", SYSTRAN + ".txt", "rc.json", RUN_LOG), filesOf(out));
    }

    @Test
    void i01Baseline_rc5Figures() {
        ExportResult result = exporter.export(finishedRun(BASELINE), temp.resolve("out"), ExtractFormat.FIXTURE);

        assertEquals(2, result.transactionCount());
        assertEquals(new BigDecimal("47.00"), result.tranAmtSum());
        assertEquals(2, result.accountCount());
        assertEquals(new BigDecimal("3410.00"), result.acctCurrBalSum());
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i06-negative-balance"})
    void hostFormat_fixedLengthRecordsWithTheSameFieldValues(String caseName) throws IOException {
        long runId = finishedRun(caseName);
        Path out = temp.resolve("host-" + caseName);

        ExportResult result = exporter.export(runId, out, ExtractFormat.HOST);

        assertHostFile(out.resolve(SYSTRAN), CardDemoLayouts.TRANSACTION, 350, recorded(caseName, SYSTRAN, CardDemoLayouts.TRANSACTION));
        assertHostFile(out.resolve(ACCTDATA), CardDemoLayouts.ACCOUNT, 300, recorded(caseName, ACCTDATA, CardDemoLayouts.ACCOUNT));
        assertEquals(Files.size(out.resolve(SYSTRAN)), 350 * result.transactionCount());
        assertEquals(Files.size(out.resolve(ACCTDATA)), 300 * result.accountCount());
        assertEquals(List.of(ACCTDATA, SYSTRAN, "rc.json"), filesOf(out));
    }

    private static void assertHostFile(Path file, RecordLayout layout, int recordLength, List<DecodedRecord> expected) throws IOException {
        byte[] content = Files.readAllBytes(file);
        assertEquals(recordLength, layout.length());
        assertEquals(expected.size() * recordLength, content.length, file + " length");
        byte[][] records = RecordImage.hostRecords(content, layout, file.getFileName().toString());
        CodePage host = CodePage.host();
        for (int i = 0; i < records.length; i++) {
            DecodedRecord decoded = layout.decode(records[i], host, new RecordSource(file.getFileName().toString(), i + 1));
            for (LayoutField field : layout.fields()) {
                if (field.kind() == FieldKind.FILLER) {
                    assertArrayEquals(expected.get(i).filler(field.name()), decoded.filler(field.name()), field.name());
                } else {
                    assertEquals(value(expected.get(i), field), value(decoded, field), field.name() + " of record " + (i + 1));
                }
            }
        }
        // No record separator, and EBCDIC: the numeric key that starts an account record is X'F0'..X'F9'.
        assertFalse(new String(content, StandardCharsets.ISO_8859_1).contains("\n"));
        if (layout == CardDemoLayouts.ACCOUNT) {
            for (byte[] record : records) {
                assertTrue((record[0] & 0xF0) == 0xF0, "ACCT-ID is not EBCDIC zoned decimal");
            }
        }
    }

    @Test
    void order_transactionsBySeqNo_accountsByAcctId() throws IOException {
        long runId = finishedRun(BASELINE);
        // Swap the write order of the two transactions and renumber: seq_no decides, not tran_id or insert order.
        jdbc.update("UPDATE carddemo.interest_transaction SET seq_no = seq_no + 10 WHERE run_id = ?", runId);
        jdbc.update("UPDATE carddemo.interest_transaction SET seq_no = 13 - seq_no WHERE run_id = ?", runId);
        Path out = temp.resolve("out");

        exporter.export(runId, out, ExtractFormat.FIXTURE);

        List<String> recordedTransactions = Fixtures.lines(Fixtures.expected(BASELINE).resolve(SYSTRAN + ".txt"));
        List<String> exported = Fixtures.lines(out.resolve(SYSTRAN + ".txt"));
        assertEquals(2, exported.size());
        assertEquals(recordedTransactions.get(1), exported.get(0));
        assertEquals(recordedTransactions.get(0), exported.get(1));
        List<String> accountIds = Fixtures.lines(out.resolve(ACCTDATA + ".txt")).stream().map(line -> line.substring(0, 11)).toList();
        assertEquals(accountIds.stream().sorted().toList(), accountIds);
        assertEquals(2, accountIds.size());
    }

    @Test
    void transactionsOfAnotherRun_areNotExported() throws IOException {
        long earlier = finishedRun("i06-negative-balance");
        long runId = finishedRun(BASELINE);
        Path out = temp.resolve("out");

        exporter.export(runId, out, ExtractFormat.FIXTURE);

        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM carddemo.interest_transaction WHERE run_id = ?", Long.class, earlier) > 0);
        assertSameFile(BASELINE, out, SYSTRAN + ".txt");
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOADED", "EXPORTED", "FAILED", "ABANDONED"})
    void runNotCalculated_isRefused_nothingWritten(String status) throws IOException {
        long runId = finishedRun(BASELINE);
        jdbc.update("UPDATE carddemo.interest_run SET status = ? WHERE run_id = ?", status, runId);
        Path out = temp.resolve("out");
        Files.createDirectories(out);
        Files.writeString(out.resolve("earlier.txt"), "earlier export");
        List<String> before = rows(true);

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> exporter.export(runId, out, ExtractFormat.FIXTURE));

        assertEquals(StopReason.WRITE_OUTPUT, stopped.reason());
        assertTrue(stopped.getMessage().contains(status), stopped.getMessage());
        assertEquals(List.of("earlier.txt"), filesOf(out));
        assertEquals(before, rows(true));
        assertEquals(List.of(out.getFileName().toString()), filesOf(temp).stream()
                .filter(file -> !file.startsWith("rejects-")).map(file -> file.substring(0, file.indexOf('/'))).distinct().toList());
    }

    @Test
    void unknownRun_isRefused() {
        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> exporter.export(-1, temp.resolve("out"), ExtractFormat.FIXTURE));

        assertTrue(stopped.getMessage().contains("no such run"), stopped.getMessage());
        assertFalse(Files.exists(temp.resolve("out")));
    }

    @Test
    void rc5Mismatch_isRefused_outputDirectoryUntouched() throws IOException {
        long runId = finishedRun(BASELINE);
        jdbc.update("UPDATE carddemo.interest_run SET written_tran_amt_sum = 47.01 WHERE run_id = ?", runId);
        Path out = temp.resolve("out");
        Files.createDirectories(out);
        Files.writeString(out.resolve("earlier.txt"), "earlier export");

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> exporter.export(runId, out, ExtractFormat.FIXTURE));

        assertTrue(stopped.getMessage().contains("RC5"), stopped.getMessage());
        assertEquals(List.of("earlier.txt"), filesOf(out));
        assertEquals("CALCULATED", run(runId).get("status"));
    }

    @Test
    void existingOutputDirectory_isReplacedAsAWhole() throws IOException {
        long runId = finishedRun(BASELINE);
        Path out = temp.resolve("out");
        Files.createDirectories(out.resolve("datasets"));
        Files.writeString(out.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt"), "an older generation\n");
        Files.writeString(out.resolve("datasets/AWS.M2.CARDDEMO.OTHER.txt"), "a file of an older export\n");
        Files.writeString(out.resolve("stale.txt"), "stale");

        exporter.export(runId, out, ExtractFormat.FIXTURE);

        assertEquals(List.of(ACCTDATA + ".txt", SYSTRAN + ".txt", "rc.json"), filesOf(out));
        assertSameFile(BASELINE, out, SYSTRAN + ".txt");
        try (Stream<Path> siblings = Files.list(temp)) {
            assertEquals(List.of(), siblings.map(path -> path.getFileName().toString()).filter(name -> name.contains("staging")).toList());
        }
    }

    @Test
    void export_changesNoRowExceptInterestRun() {
        long runId = finishedRun(BASELINE);
        List<String> before = rows(false);
        Map<String, Object> runBefore = run(runId);
        List<String> otherRunsBefore = jdbc.queryForList(
                "SELECT t::text FROM carddemo.interest_run t WHERE run_id <> ? ORDER BY run_id", String.class, runId);

        ExportResult result = exporter.export(runId, temp.resolve("out"), ExtractFormat.FIXTURE);

        assertEquals(before, rows(false));
        assertEquals(otherRunsBefore, jdbc.queryForList(
                "SELECT t::text FROM carddemo.interest_run t WHERE run_id <> ? ORDER BY run_id", String.class, runId));
        Map<String, Object> runAfter = run(runId);
        assertEquals("EXPORTED", runAfter.get("status"));
        assertEquals(result.transactionCount(), runAfter.get("written_transaction_count"));
        assertEquals(result.tranAmtSum(), runAfter.get("written_tran_amt_sum"));
        assertEquals(result.acctCurrBalSum(), runAfter.get("written_acct_curr_bal_sum"));
        for (String column : runBefore.keySet()) {
            if (!List.of("status", "finished_at").contains(column)) {
                assertEquals(runBefore.get(column), runAfter.get(column), column);
            }
        }
    }

    @Test
    void rc5FiguresTheCalculationLeftEmpty_areTakenFromTheExport() {
        long runId = finishedRun(BASELINE);
        jdbc.update("""
                UPDATE carddemo.interest_run SET written_transaction_count = NULL, written_tran_amt_sum = NULL,
                       written_account_count = NULL, written_acct_curr_bal_sum = NULL WHERE run_id = ?""", runId);

        exporter.export(runId, temp.resolve("out"), ExtractFormat.FIXTURE);

        Map<String, Object> runAfter = run(runId);
        assertEquals("EXPORTED", runAfter.get("status"));
        assertEquals(2L, runAfter.get("written_transaction_count"));
        assertEquals(new BigDecimal("47.00"), runAfter.get("written_tran_amt_sum"));
        assertEquals(new BigDecimal("3410.00"), runAfter.get("written_acct_curr_bal_sum"));
        assertEquals(null, runAfter.get("written_account_count"));
    }

    @Test
    void exportingTwice_isRefusedTheSecondTime() {
        long runId = finishedRun(BASELINE);
        exporter.export(runId, temp.resolve("out"), ExtractFormat.FIXTURE);

        assertThrows(InterestRunStoppedException.class, () -> exporter.export(runId, temp.resolve("out"), ExtractFormat.FIXTURE));
    }

    private JobExecution launchStep(Path outputDir) throws Exception {
        Job job = new JobBuilder("exportDatasetsStepOnly", jobRepository).start(exportDatasetsStep).build();
        return jobLauncher.run(job, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, Fixtures.input(BASELINE).toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
    }

    @Test
    void exportDatasetsStep_exportsTheNewestCalculatedRun() throws Exception {
        long runId = finishedRun("i06-negative-balance");
        Path out = temp.resolve("out");

        JobExecution execution = launchStep(out);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertSameFile("i06-negative-balance", out, SYSTRAN + ".txt");
        assertSameFile("i06-negative-balance", out, ACCTDATA + ".txt");
        assertSameFile("i06-negative-balance", out, "rc.json");
        assertEquals("EXPORTED", run(runId).get("status"));
    }

    @Test
    void exportDatasetsStep_withoutACalculatedRun_fails() throws Exception {
        jdbc.update("UPDATE carddemo.interest_run SET status = 'ABANDONED' WHERE status = 'CALCULATED'");
        Path out = temp.resolve("out");

        JobExecution execution = launchStep(out);

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertFalse(Files.exists(out));
    }
}
