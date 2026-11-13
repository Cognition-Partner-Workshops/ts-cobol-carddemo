package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Field;
import org.carddemo.modern.codec.Layouts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PostingRunTest {
    @TempDir Path temporary;
    private JdbcTemplate jdbc;
    private PostingRun run;
    private Path input;
    private Path output;
    private String clock;
    private String start;
    private String end;

    @BeforeEach
    void setUp() throws Exception {
        var datasource = new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
        new JdbcTemplate(datasource).execute("CREATE SCHEMA IF NOT EXISTS posting_test");
        datasource.setUrl("jdbc:postgresql://db:5432/carddemo?currentSchema=posting_test");
        jdbc = new JdbcTemplate(datasource);
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("schema.sql")).execute(datasource);
        run = new PostingRun(jdbc);
        input = Files.createDirectory(temporary.resolve("input"));
        output = temporary.resolve("output");
        var chains = Files.readString(Path.of("../../tools/runjcl/chains.json"));
        var clockMatch = java.util.regex.Pattern.compile("\"clock\":\\s*\"([^\"]+)\"").matcher(chains);
        assertTrue(clockMatch.find());
        clock = clockMatch.group(1);
        var jcl = Files.readString(Path.of("../../app/jcl/TRANREPT.jcl"));
        start = parameter(jcl, "PARM-START-DATE");
        end = parameter(jcl, "PARM-END-DATE");
        useFixture("p01-baseline-mixed");
    }

    private static String parameter(String jcl, String name) {
        var match = java.util.regex.Pattern.compile(name + ",C'([^']+)'").matcher(jcl);
        assertTrue(match.find());
        return match.group(1);
    }

    private void useFixture(String name) throws Exception {
        try (var files = Files.list(input)) {
            for (var path : files.toList()) Files.delete(path);
        }
        try (var files = Files.list(Path.of("../../fixtures/posting/" + name + "/input"))) {
            for (var path : files.toList()) Files.copy(path, input.resolve(path.getFileName()));
        }
    }

    private int execute() throws Exception { return run.run(input, output, clock, start, end); }

    private List<String> dataset(String name) throws Exception {
        return Files.readAllLines(output.resolve("datasets/" + PostingRun.PREFIX + name + ".txt"), StandardCharsets.ISO_8859_1);
    }

    private void daily(List<Map<String, Object>> records) throws Exception {
        Files.write(input.resolve(PostingRun.PREFIX + "DALYTRAN.PS.txt"),
                records.stream().map(Layouts.DALYTRAN::encodeLine).toList(), StandardCharsets.ISO_8859_1);
    }

    private Map<String, Object> firstDaily() throws Exception {
        return Layouts.DALYTRAN.decode(Files.readAllLines(input.resolve(PostingRun.PREFIX + "DALYTRAN.PS.txt"),
                StandardCharsets.ISO_8859_1).getFirst());
    }

    @Test
    void mixedDayPostsTypedBalancesAndWritesReports() throws Exception {
        assertEquals(0, execute());
        assertEquals(4, dataset("TRANSACT.VSAM.KSDS").size());
        assertTrue(dataset("DALYREJS.G0001V00").isEmpty());
        var account = PostingTable.ACCOUNT.find(jdbc, 1001);
        assertEquals(new BigDecimal("425.00"), account.get("ACCT-CURR-BAL"));
        assertEquals(new BigDecimal("125.00"), account.get("ACCT-CURR-CYC-CREDIT"));
        assertEquals(new BigDecimal("-200.00"), account.get("ACCT-CURR-CYC-DEBIT"));
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM daily_transaction", Integer.class));
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM posted_transaction", Integer.class));
        assertTrue(dataset("TRANREPT.G0001V00").stream().anyMatch(line -> line.startsWith("0000000000000001 00000001001 01-Purchase")));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP10R\": 0"));
    }

    @Test
    void invalidCardIsRejectedWithItsNumericReasonAndStepRcFour() throws Exception {
        useFixture("p02-invalid-card");
        assertEquals(4, execute());
        var reject = Layouts.REJTRAN.decode(dataset("DALYREJS.G0001V00").getFirst());
        assertEquals(new BigDecimal("100"), reject.get("REJ-FAIL-REASON"));
        assertEquals("INVALID CARD NUMBER FOUND", reject.get("REJ-FAIL-REASON-DESC").toString().stripTrailing());
        assertEquals(1, dataset("TRANSACT.VSAM.KSDS").size());
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP15\": 4"));
    }

    @Test
    void missingAccountIsNotPosted() throws Exception {
        useFixture("p03-account-missing");
        assertEquals(4, execute());
        assertEquals(new BigDecimal("101"), Layouts.REJTRAN.decode(dataset("DALYREJS.G0001V00").getFirst()).get("REJ-FAIL-REASON"));
        var rejectedId = Layouts.REJTRAN.decode(dataset("DALYREJS.G0001V00").getFirst()).get("DALYTRAN-ID");
        assertTrue(dataset("TRANSACT.VSAM.KSDS").stream().noneMatch(line -> Layouts.TRAN.decode(line).get("TRAN-ID").equals(rejectedId)));
    }

    @Test
    void exactlyAtLimitIsAcceptedAndNextPurchaseIsRejected() throws Exception {
        useFixture("p04-overlimit-reject");
        assertEquals(4, execute());
        assertEquals(1, dataset("TRANSACT.VSAM.KSDS").size());
        assertEquals(new BigDecimal("102"), Layouts.REJTRAN.decode(dataset("DALYREJS.G0001V00").getFirst()).get("REJ-FAIL-REASON"));
    }

    @Test
    void expiryIsInclusiveAndOverridesTheLimitReason() throws Exception {
        useFixture("p05-expired-account");
        assertEquals(4, execute());
        assertEquals(new BigDecimal("103"), Layouts.REJTRAN.decode(dataset("DALYREJS.G0001V00").getFirst()).get("REJ-FAIL-REASON"));
        var daily = firstDaily();
        var xref = Layouts.XREF.decode("");
        var account = Layouts.ACCOUNT.decode("");
        account.put("ACCT-CURR-BAL", new BigDecimal("1000"));
        account.put("ACCT-CREDIT-LIMIT", BigDecimal.ZERO);
        account.put("ACCT-EXPIRAION-DATE", "2000-01-01");
        assertEquals(103, PostingRun.validationReason(daily, xref, account));
    }

    @Test
    void negativeCreditsCreateTheMissingCategoryAndUpdateDebit() throws Exception {
        useFixture("p06-credits-new-category");
        assertEquals(0, execute());
        assertTrue(PostingTable.BALANCE.read(jdbc).stream().anyMatch(row -> row.get("TRANCAT-TYPE-CD").equals("03")));
        assertTrue(PostingTable.ACCOUNT.read(jdbc).stream().anyMatch(row -> PostingRun.number(row, "ACCT-CURR-CYC-DEBIT").signum() < 0));
        assertTrue(Files.readString(output.resolve("sysout/POSTTRAN.STEP15.txt")).contains(".. Creating."));
    }

    @Test
    void emptyDayDoesNotRetainThePreviousRunOrInitialMaster() throws Exception {
        assertEquals(0, execute());
        useFixture("p07-empty-day");
        assertEquals(0, execute());
        assertTrue(dataset("TRANSACT.VSAM.KSDS").isEmpty());
        assertTrue(dataset("DALYREJS.G0001V00").isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM posted_transaction", Integer.class));
    }

    @Test
    void processingClockIsAParameterNotTheHostClock() throws Exception {
        clock = "2022-07-06 11:12:13.45";
        assertEquals(0, execute());
        assertEquals("2022-07-06-11.12.13.450000", Layouts.TRAN.decode(dataset("TRANSACT.VSAM.KSDS").getFirst()).get("TRAN-PROC-TS"));
        assertThrows(java.time.format.DateTimeParseException.class, () -> run.post(input, output, "invalid"));
    }

    @Test
    void missingReportTypeProducesDiagnosticsAndRealAbendRc() throws Exception {
        Files.writeString(input.resolve(PostingRun.PREFIX + "TRANTYPE.PS.txt"), "");
        assertEquals(12, execute());
        var sysout = Files.readAllLines(output.resolve("sysout/TRANREPT.STEP10R.txt"));
        assertTrue(sysout.contains("INVALID TRANSACTION TYPE : 01"));
        assertTrue(sysout.contains("FILE STATUS IS: NNNN0023"));
        assertEquals("CEE3ABD: USER ABEND U+0000000999", sysout.getLast());
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP10R\": 12"));
    }

    @Test
    void missingReportCategoryAbendsWithoutInventingDescriptions() throws Exception {
        Files.writeString(input.resolve(PostingRun.PREFIX + "TRANCATG.PS.txt"), "");
        assertEquals(12, execute());
        assertTrue(Files.readString(output.resolve("sysout/TRANREPT.STEP10R.txt")).contains("INVALID TRAN CATG KEY : 010001"));
    }

    @Test
    void duplicateDailyIdFailsAfterTheEarlierUpdates() throws Exception {
        var record = firstDaily();
        daily(List.of(record, record));
        assertEquals(12, execute());
        assertEquals(1, dataset("TRANSACT.VSAM.KSDS").size());
        assertEquals(new BigDecimal("750.00"), PostingTable.ACCOUNT.find(jdbc, 1001).get("ACCT-CURR-BAL"));
        assertTrue(Files.readString(output.resolve("sysout/POSTTRAN.STEP15.txt")).contains("FILE STATUS IS: NNNN0022"));
    }

    @Test
    void duplicateSetupKeysKeepFirstRecordAndProduceLoaderWarning() throws Exception {
        var path = input.resolve(PostingRun.PREFIX + "ACCTDATA.PS.txt");
        var lines = Files.readAllLines(path);
        Files.write(path, List.of(lines.getFirst(), lines.getFirst(), lines.getLast()));
        assertEquals(0, execute());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM account", Integer.class));
        assertTrue(Files.readString(output.resolve("setup/ACCTFILE.STEP15.txt")).contains("IDC3302I DUPLICATE KEYS SKIPPED 000000001"));
    }

    @Test
    void reportSortAndDateparmFiltersAreBothApplied() throws Exception {
        assertEquals(0, run.post(input, output, clock));
        assertEquals(0, run.report(output, "2030-01-01", "2030-12-31"));
        assertTrue(dataset("TRANREPT.G0001V00").stream().noneMatch(line -> line.startsWith("0000000000000001")));
        jdbc.update("UPDATE date_parameters SET ws_start_date='2030-01-01', ws_end_date='2030-12-31'");
        assertEquals(0, run.report(output, start, end));
        assertTrue(dataset("TRANREPT.G0001V00").stream().noneMatch(line -> line.startsWith("0000000000000001")));
    }

    @Test
    void dateparmEofLeavesAnEmptyReportAndCompletesNormally() throws Exception {
        Files.writeString(input.resolve(PostingRun.PREFIX + "DATEPARM.txt"), "");
        assertEquals(0, execute());
        assertTrue(dataset("TRANREPT.G0001V00").isEmpty());
    }

    @Test
    void everyNumericColumnMatchesItsCopybookAndAlternateIndexesAreNonunique() {
        for (var table : PostingTable.ALL) {
            for (var field : table.layout().fields()) {
                if (field.kind() != Field.Kind.ZONED) continue;
                var metadata = jdbc.queryForMap("SELECT numeric_precision, numeric_scale FROM information_schema.columns "
                        + "WHERE table_schema='posting_test' AND table_name=? AND column_name=?", table.name(), PostingTable.column(field.name()));
                assertEquals(field.length(), ((Number) metadata.get("numeric_precision")).intValue(), field.name());
                assertEquals(field.scale(), ((Number) metadata.get("numeric_scale")).intValue(), field.name());
            }
        }
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname='posting_test' "
                + "AND indexname IN ('card_xref_account', 'transaction_proc_ts') AND indexdef NOT LIKE '%UNIQUE%'", Integer.class));
    }
}
