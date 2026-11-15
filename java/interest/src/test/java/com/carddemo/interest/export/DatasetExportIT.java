package com.carddemo.interest.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * The export on PostgreSQL 15 in a container with the schema of FI-4 (standards S7.3). The tables
 * hold a finished run inserted by SQL (src/test/resources/export); the expected files are the
 * recorded legacy output (standards S7.4). The run log is written by the calculation step of
 * interestCalcJob, launched on the same case, before the export.
 *
 * <p>The i01-baseline and i06-negative-balance exports are also left under
 * {@code target/export-parity/<case>/actual} for tools/parity/compare.py.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class DatasetExportIT {

    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final String RETURN_CODES = "rc.json";
    private static final Path PARITY_DIR = Path.of("target", "export-parity");
    private static final long RUN = 1;
    private static final List<String> TABLES = List.of("account", "card_xref", "tran_cat_balance", "disclosure_group",
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
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;

    @TempDir
    Path temp;

    @BeforeEach
    void emptyTables() {
        jdbc.execute("TRUNCATE carddemo.interest_transaction, carddemo.interest_run, carddemo.account, carddemo.card_xref, "
                + "carddemo.tran_cat_balance, carddemo.disclosure_group RESTART IDENTITY");
    }

    private void finishedRun(String name) {
        new ResourceDatabasePopulator(new ClassPathResource("export/" + name + "-finished-run.sql")).execute(dataSource);
    }

    /** The calculation step (file adapters of ticket 01) writes its run log into the output directory. */
    private void calculationStepWrites(String caseName, Path outputDir) throws Exception {
        JobExecution execution = jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString("parmDate", Fixtures.PARM_DATE)
                .addString("runId", "export-it-" + UUID.randomUUID())
                .addString("inputDir", Fixtures.input(caseName).toString())
                .addString("outputDir", outputDir.toString())
                .addString("format", "fixture")
                .toJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertTrue(Files.isRegularFile(outputDir.resolve(RUN_LOG)));
    }

    /** A run log as the calculation step leaves it, for the tests on synthetic rows. */
    private static void runLogIn(Path outputDir) throws IOException {
        Files.createDirectories(outputDir.resolve("sysout"));
        Files.writeString(outputDir.resolve(RUN_LOG),
                "START OF EXECUTION OF PROGRAM CBACT04C\nEND OF EXECUTION OF PROGRAM CBACT04C\n");
    }

    private static Path parityDir(String caseName) throws IOException {
        Path dir = PARITY_DIR.resolve(caseName).resolve("actual");
        deleteTree(dir);
        return dir;
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static List<String> filesUnder(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile).map(path -> dir.relativize(path).toString()).sorted().toList();
        }
    }

    private static void assertSameBytes(Path expected, Path actual) throws IOException {
        assertArrayEquals(Files.readAllBytes(expected), Files.readAllBytes(actual), actual.toString());
    }

    /** One fingerprint per table over every column of every row. */
    private Map<String, String> tableFingerprints() {
        Map<String, String> fingerprints = new LinkedHashMap<>();
        for (String table : TABLES) {
            fingerprints.put(table, jdbc.queryForObject(
                    "SELECT count(*) || ':' || md5(coalesce(string_agg(t::text, '|' ORDER BY t::text), '')) FROM carddemo." + table + " t",
                    String.class));
        }
        return fingerprints;
    }

    private Map<String, Object> runRow(long runId) {
        return jdbc.queryForMap("SELECT * FROM carddemo.interest_run WHERE run_id = ?", runId);
    }

    private void assertExportEqualsRecorded(String caseName, Path output) throws IOException {
        Path expected = Fixtures.expected(caseName);
        assertEquals(List.of(ACCOUNTS + ".txt", TRANSACTIONS + ".txt", RETURN_CODES, RUN_LOG), filesUnder(output));
        assertSameBytes(expected.resolve(TRANSACTIONS + ".txt"), output.resolve(TRANSACTIONS + ".txt"));
        assertSameBytes(expected.resolve(ACCOUNTS + ".txt"), output.resolve(ACCOUNTS + ".txt"));
        assertSameBytes(expected.resolve(RETURN_CODES), output.resolve(RETURN_CODES));
        assertSameBytes(expected.resolve(RUN_LOG), output.resolve(RUN_LOG));
    }

    // ---- Acceptance: Input / Result ---------------------------------------------------------------

    /**
     * INT-R06: type 01, category 0005, source System, the description, no merchant and the card number
     * come out as the recorded SYSTRAN records; the ACCTDATA image and rc.json equal the recorded files
     * and the run log of the calculation step is beside them.
     */
    @Test
    void intR06_i01BaselineExportEqualsRecorded() throws Exception {
        Path output = parityDir("i01-baseline");
        calculationStepWrites("i01-baseline", output);
        finishedRun("i01-baseline");

        ExportTotals totals = exporter.export(RUN, output, ExportFormat.FIXTURE);

        assertExportEqualsRecorded("i01-baseline", output);
        assertEquals(2, totals.transactionCount());
        assertEquals(new BigDecimal("47.00"), totals.transactionAmountSum()); // 10.00 + 37.00
    }

    /** Rule E2 on the way out: -3.00 is written D-signed (0000000030}) and -300.00 as 00000003000} (INT-Q05). */
    @Test
    void e2_i06NegativeAmountsAreDSignedAsRecorded() throws Exception {
        Path output = parityDir("i06-negative-balance");
        calculationStepWrites("i06-negative-balance", output);
        finishedRun("i06-negative-balance");

        ExportTotals totals = exporter.export(RUN, output, ExportFormat.FIXTURE);

        assertExportEqualsRecorded("i06-negative-balance", output);
        String transaction = Fixtures.lines(output.resolve(TRANSACTIONS + ".txt")).get(0);
        assertEquals("0000000030}", transaction.substring(132, 143));
        assertEquals(new BigDecimal("-3.00"), totals.transactionAmountSum());
    }

    /** INT-T04, gap G05: bytes 25-100 of TRAN-DESC and the 20-byte FILLER are spaces; every record is 350 bytes. */
    @Test
    void intT04_descriptionTailAndFillerAreSpaces() throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        finishedRun("i01-baseline");

        exporter.export(RUN, output, ExportFormat.HOST);

        List<byte[]> records = RecordImage.fromBlock(Files.readAllBytes(output.resolve(TRANSACTIONS)), 350);
        byte ebcdicSpace = 0x40;
        for (byte[] record : records) {
            assertEquals(350, record.length);
            for (int i = 32 + 24; i < 132; i++) {
                assertEquals(ebcdicSpace, record[i], "TRAN-DESC byte " + (i - 32 + 1));
            }
            for (int i = 330; i < 350; i++) {
                assertEquals(ebcdicSpace, record[i], "FILLER byte " + (i - 330 + 1));
            }
        }
    }

    /** INT-T05: both timestamps leave as YYYY-MM-DD-HH.MM.SS.hh0000, 26 characters, as stored. */
    @Test
    void intT05_timestampsAreExportedAsStored() throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        finishedRun("i01-baseline");

        exporter.export(RUN, output, ExportFormat.FIXTURE);

        for (String record : Fixtures.lines(output.resolve(TRANSACTIONS + ".txt"))) {
            assertEquals("2022-07-05-10.00.00.000000", record.substring(278, 304));
            assertEquals("2022-07-05-10.00.00.000000", record.substring(304, 330));
        }
    }

    // ---- Acceptance: Host format --------------------------------------------------------------------

    /**
     * The same run as fixed-length files: 350-byte and 300-byte records, no separator, and, decoded
     * with the codec, the field values of the recorded files.
     */
    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i06-negative-balance"})
    void host_fixedLengthFilesDecodeToTheRecordedFieldValues(String caseName) throws Exception {
        Path output = temp.resolve("actual");
        calculationStepWrites(caseName, output);
        finishedRun(caseName);

        exporter.export(RUN, output, ExportFormat.HOST);

        assertEquals(List.of(ACCOUNTS, TRANSACTIONS, RETURN_CODES, RUN_LOG), filesUnder(output));
        assertHostFileDecodesToRecorded(caseName, TRANSACTIONS, 350,
                new TransactionRecord(CodePage.host()).codec(), new TransactionRecord(CodePage.fixture()).codec());
        assertHostFileDecodesToRecorded(caseName, ACCOUNTS, 300,
                new AccountRecord(CodePage.host()).codec(), new AccountRecord(CodePage.fixture()).codec());
    }

    private void assertHostFileDecodesToRecorded(String caseName, String dataset, int recordLength,
            RecordCodec hostCodec, RecordCodec fixtureCodec) throws IOException {
        byte[] content = Files.readAllBytes(temp.resolve("actual").resolve(dataset));
        List<String> recorded = Fixtures.lines(Fixtures.expected(caseName).resolve(dataset + ".txt"));
        assertEquals(recorded.size() * recordLength, content.length, dataset + " length");
        assertEquals(recordLength, hostCodec.layout().length());
        List<byte[]> records = RecordImage.fromBlock(content, recordLength);
        for (int i = 0; i < recorded.size(); i++) {
            RecordSource source = new RecordSource(dataset, i + 1);
            RecordValues host = hostCodec.decode(records.get(i), source);
            RecordValues fixture = fixtureCodec.decode(
                    RecordImage.fromLine(recorded.get(i), recordLength, CodePage.fixture()), source);
            for (FieldSpec field : hostCodec.layout().fields()) {
                String where = dataset + " record " + (i + 1) + " " + field.name();
                if (field.kind() == FieldKind.TEXT) {
                    assertEquals(fixture.text(field), host.text(field), where);
                } else if (field.kind() == FieldKind.UNSIGNED_ZONED) {
                    assertEquals(fixture.unsigned(field), host.unsigned(field), where);
                } else if (field.kind() == FieldKind.SIGNED_ZONED) {
                    assertEquals(fixture.signed(field), host.signed(field), where);
                }
            }
        }
    }

    /** Rule E4: the 178-byte account FILLER leaves as the raw bytes of the table, also in the host format. */
    @Test
    void e4_accountFillerIsWrittenBackByteForByte() throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        finishedRun("i01-baseline");
        byte[] filler = new byte[178];
        for (int i = 0; i < filler.length; i++) {
            filler[i] = (byte) (i + 1); // X'01' to X'B2': bytes no code page translation would keep
        }
        jdbc.update("UPDATE carddemo.account SET acct_filler = ? WHERE acct_id = 2001", (Object) filler);

        exporter.export(RUN, output, ExportFormat.HOST);

        List<byte[]> records = RecordImage.fromBlock(Files.readAllBytes(output.resolve(ACCOUNTS)), 300);
        assertArrayEquals(filler, Arrays.copyOfRange(records.get(0), 122, 300));
    }

    // ---- Acceptance: Order --------------------------------------------------------------------------

    /** SYSTRAN in seq_no order (not id order, not insert order, only this run); accounts in acct_id order. */
    @Test
    void order_transactionsBySeqNoAndAccountsByAcctId() throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        finishedRun("order");

        ExportTotals totals = exporter.export(RUN, output, ExportFormat.FIXTURE);

        List<String> transactionIds = Fixtures.lines(output.resolve(TRANSACTIONS + ".txt")).stream()
                .map(line -> line.substring(0, 16)).toList();
        assertEquals(List.of("2022071800000009", "2022071800000008", "2022071800000007"), transactionIds);
        List<String> accountIds = Fixtures.lines(output.resolve(ACCOUNTS + ".txt")).stream()
                .map(line -> line.substring(0, 11)).toList();
        assertEquals(List.of("00000002010", "00000002020", "00000002030"), accountIds);
        assertEquals(3, totals.transactionCount());
        assertEquals(new BigDecimal("6.00"), totals.transactionAmountSum()); // 1.00 + 2.00 + 3.00, run 2 left out
    }

    // ---- Acceptance: Errors -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"LOADED", "EXPORTED", "FAILED", "ABANDONED"})
    void errors_runThatIsNotCalculatedIsRefused(String status) throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        Files.writeString(output.resolve(RETURN_CODES), "earlier export");
        finishedRun("i01-baseline");
        jdbc.update("UPDATE carddemo.interest_run SET status = ? WHERE run_id = ?", status, RUN);
        Map<String, String> before = tableFingerprints();

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> exporter.export(RUN, output, ExportFormat.FIXTURE));

        assertEquals(StopReason.WRITE_OUTPUT, stopped.reason());
        assertTrue(stopped.getMessage().contains("has status " + status + ", not CALCULATED"), stopped.getMessage());
        assertEquals(List.of("actual/" + RETURN_CODES, "actual/" + RUN_LOG), filesUnder(temp));
        assertEquals("earlier export", Files.readString(output.resolve(RETURN_CODES)));
        assertEquals(before, tableFingerprints());
    }

    @Test
    void errors_unknownRunIsRefused() throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        finishedRun("i01-baseline");
        Map<String, String> before = tableFingerprints();

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> exporter.export(99, output, ExportFormat.FIXTURE));

        assertTrue(stopped.getMessage().contains("run 99 is not in interest_run"), stopped.getMessage());
        assertEquals(List.of("actual/" + RUN_LOG), filesUnder(temp));
        assertEquals(before, tableFingerprints());
    }

    /** An earlier export's files do not stay beside the new ones; only the run log is carried over. */
    @Test
    void errors_existingOutputDirectoryIsReplacedAsAWhole() throws Exception {
        Path output = temp.resolve("actual");
        calculationStepWrites("i01-baseline", output);
        Files.writeString(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0002V00.txt"), "generation of an earlier export\n");
        Files.writeString(output.resolve("datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS"), "host image of an earlier export");
        Files.writeString(output.resolve("notes.txt"), "left by somebody");
        finishedRun("i01-baseline");

        exporter.export(RUN, output, ExportFormat.FIXTURE);

        assertExportEqualsRecorded("i01-baseline", output);
        assertEquals(List.of("actual"), siblingsOfOutput());
    }

    /**
     * A record that cannot be encoded stops the export part-way (rule E3: the character is not in the
     * code page). The directory keeps every file it had, none of the new ones, and the run stays CALCULATED.
     */
    @Test
    void errors_failedExportLeavesTheOldDirectoryAndTheRunCalculated() throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        Files.createDirectories(output.resolve("datasets"));
        Files.writeString(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt"), "earlier export\n");
        finishedRun("i01-baseline");
        jdbc.update("UPDATE carddemo.interest_transaction SET tran_desc = ? WHERE run_id = ? AND seq_no = 2", "Int. \u20ac", RUN);
        Map<String, String> before = tableFingerprints();

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> exporter.export(RUN, output, ExportFormat.FIXTURE));

        assertEquals(StopReason.RECORD_REJECTED, stopped.reason());
        assertEquals(List.of("actual/" + TRANSACTIONS + ".txt", "actual/" + RUN_LOG), filesUnder(temp));
        assertEquals("earlier export\n", Files.readString(output.resolve(TRANSACTIONS + ".txt")));
        assertEquals(before, tableFingerprints());
        assertEquals("CALCULATED", runRow(RUN).get("status"));
    }

    /** Without the run log of the calculation step the directory would be incomplete: refused, nothing written. */
    @Test
    void errors_missingRunLogOfTheCalculationStepIsRefused() throws Exception {
        Path output = temp.resolve("actual");
        finishedRun("i01-baseline");
        Map<String, String> before = tableFingerprints();

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> exporter.export(RUN, output, ExportFormat.FIXTURE));

        assertEquals(StopReason.WRITE_OUTPUT, stopped.reason());
        assertEquals(List.of(), filesUnder(temp));
        assertEquals(before, tableFingerprints());
    }

    private List<String> siblingsOfOutput() throws IOException {
        try (Stream<Path> paths = Files.list(temp)) {
            return paths.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    // ---- Acceptance: Database assertions --------------------------------------------------------------

    /**
     * RC5: the exported count and sum equal the rows of interest_transaction (i01-baseline: 2 and
     * 47.00, strategy-interest.md). The export changes interest_run only: the status and the
     * written_systran_* and written_acct* columns of this run's row.
     */
    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i06-negative-balance"})
    void rc5_exportChangesOnlyTheRunRow(String caseName) throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        finishedRun(caseName);
        boolean baseline = caseName.equals("i01-baseline");
        Map<String, String> before = tableFingerprints();
        Map<String, Object> runBefore = runRow(RUN);

        exporter.export(RUN, output, ExportFormat.FIXTURE);

        Map<String, String> after = tableFingerprints();
        for (String table : TABLES) {
            if (table.equals("interest_run")) {
                assertFalse(before.get(table).equals(after.get(table)), table);
            } else {
                assertEquals(before.get(table), after.get(table), table);
            }
        }
        Map<String, Object> run = runRow(RUN);
        assertEquals("EXPORTED", run.get("status"));
        // RC5, from the recorded SYSTRAN file: i01 two records 10.00 + 37.00, i06 one record -3.00
        assertEquals(baseline ? 2L : 1L, run.get("written_systran_count"));
        assertEquals(new BigDecimal(baseline ? "47.00" : "-3.00"), run.get("written_systran_tran_amt_sum"));
        assertEquals(run.get("written_transaction_count"), run.get("written_systran_count"));
        assertEquals(run.get("written_tran_amt_sum"), run.get("written_systran_tran_amt_sum"));
        // RC6, from the recorded ACCTDATA file: i01 1010.00 + 2400.00, limits 5000.00 + 6000.00 and
        // 2500.00 + 3000.00, cycle credit 0.00 + 2400.00, cycle debit 0.00; i06 one account
        assertEquals(baseline ? 2L : 1L, run.get("written_acctdata_count"));
        assertEquals(new BigDecimal(baseline ? "3410.00" : "-300.00"), run.get("written_acct_curr_bal_sum"));
        assertEquals(new BigDecimal(baseline ? "11000.00" : "5000.00"), run.get("written_acct_credit_limit_sum"));
        assertEquals(new BigDecimal(baseline ? "5500.00" : "2500.00"), run.get("written_acct_cash_credit_limit_sum"));
        assertEquals(new BigDecimal(baseline ? "2400.00" : "0.00"), run.get("written_acct_curr_cyc_credit_sum"));
        assertEquals(new BigDecimal(baseline ? "0.00" : "300.00"), run.get("written_acct_curr_cyc_debit_sum"));
        List<String> changed = new ArrayList<>();
        for (String column : run.keySet()) {
            if (!java.util.Objects.equals(runBefore.get(column), run.get(column))) {
                changed.add(column);
            }
        }
        assertEquals(List.of("status", "written_systran_count", "written_systran_tran_amt_sum", "written_acctdata_count",
                "written_acct_curr_bal_sum", "written_acct_credit_limit_sum", "written_acct_cash_credit_limit_sum",
                "written_acct_curr_cyc_credit_sum", "written_acct_curr_cyc_debit_sum"), changed);
    }

    /** Another run's row is not touched when a run is exported. */
    @Test
    void rc5_otherRunsKeepTheirRow() throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        finishedRun("order");
        Map<String, Object> otherBefore = runRow(2);

        exporter.export(RUN, output, ExportFormat.FIXTURE);

        assertEquals(otherBefore, runRow(2));
        assertEquals("EXPORTED", runRow(RUN).get("status"));
    }

    // ---- exportDatasetsStep ---------------------------------------------------------------------------

    private static ChunkContext stepOfJob(JobParameters parameters, Long interestRunId) {
        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution("interestCalcJob", 1L, 1L, parameters);
        if (interestRunId != null) {
            jobExecution.getExecutionContext().putLong(ExportDatasetsTasklet.INTEREST_RUN_ID, interestRunId);
        }
        StepExecution stepExecution = jobExecution.createStepExecution("exportDatasetsStep");
        return new ChunkContext(new StepContext(stepExecution));
    }

    @Test
    void step_exportsTheRunOfTheJobInTheFormatOfTheJobParameter() throws Exception {
        Path output = temp.resolve("actual");
        calculationStepWrites("i01-baseline", output);
        finishedRun("i01-baseline");
        ChunkContext context = stepOfJob(new JobParametersBuilder()
                .addString("outputDir", output.toString()).addString("format", "fixture").toJobParameters(), RUN);
        StepContribution contribution = new StepContribution(context.getStepContext().getStepExecution());

        new ExportDatasetsTasklet(exporter).execute(contribution, context);

        assertExportEqualsRecorded("i01-baseline", output);
        assertEquals(4, contribution.getWriteCount()); // 2 transactions + 2 accounts
        assertEquals("EXPORTED", runRow(RUN).get("status"));
    }

    @Test
    void step_withoutTheRunIdOfTheJobNothingIsExported() throws Exception {
        Path output = temp.resolve("actual");
        runLogIn(output);
        finishedRun("i01-baseline");
        ChunkContext context = stepOfJob(new JobParametersBuilder()
                .addString("outputDir", output.toString()).addString("format", "fixture").toJobParameters(), null);
        StepContribution contribution = new StepContribution(context.getStepContext().getStepExecution());

        assertThrows(InterestRunStoppedException.class, () -> new ExportDatasetsTasklet(exporter).execute(contribution, context));

        assertEquals(List.of("actual/" + RUN_LOG), filesUnder(temp));
        assertEquals("CALCULATED", runRow(RUN).get("status"));
    }
}
