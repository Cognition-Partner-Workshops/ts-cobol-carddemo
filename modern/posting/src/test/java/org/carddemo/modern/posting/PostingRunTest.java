package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PostingRunTest {
    @TempDir Path temporary;
    private JdbcTemplate jdbc;
    private PostingRun run;

    @BeforeEach
    void setUp() {
        var source = new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
        new JdbcTemplate(source).execute("CREATE SCHEMA IF NOT EXISTS posting_test");
        source.setUrl("jdbc:postgresql://db:5432/carddemo?currentSchema=posting_test");
        jdbc = new JdbcTemplate(source);
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("schema.sql")).execute(source);
        run = new PostingRun(jdbc);
    }

    @ParameterizedTest
    @ValueSource(strings = {"p01-baseline-mixed", "p02-invalid-card", "p03-account-missing", "p04-overlimit-reject",
            "p05-expired-account", "p06-credits-new-category", "p07-empty-day"})
    void recordedDatasetsSysoutAndReturnCodes(String fixture) throws Exception {
        var root = Path.of("../../fixtures/posting", fixture);
        var output = temporary.resolve("output");
        assertEquals(fixture.startsWith("p01") || fixture.startsWith("p06") || fixture.startsWith("p07") ? 0 : 4,
                run.run(root.resolve("input"), output));
        try (var paths = Files.walk(root.resolve("expected"))) {
            for (var path : paths.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals("manifest.json")).toList()) {
                var relative = root.resolve("expected").relativize(path);
                if (relative.toString().equals("rc.json")) {
                    assertEquals(Files.readString(path).replaceAll("\\s", ""),
                            Files.readString(output.resolve(relative)).replaceAll("\\s", ""));
                } else assertArrayEquals(Files.readAllBytes(path), Files.readAllBytes(output.resolve(relative)), relative.toString());
            }
        }
    }

    private Path input() throws Exception {
        var input = Files.createDirectory(temporary.resolve("input"));
        try (var paths = Files.list(Path.of("../../fixtures/posting/p01-baseline-mixed/input"))) {
            for (var path : paths.toList()) Files.copy(path, input.resolve(path.getFileName()));
        }
        return input;
    }

    @Test
    void expirationOverridesOverlimitAndCycleAmountsIgnoreCurrentBalance() {
        var daily = Layouts.DALYTRAN.decode(Layouts.DALYTRAN.encode(Map.of("DALYTRAN-AMT", new BigDecimal("50.00"),
                "DALYTRAN-ORIG-TS", "2022-07-04-00.00.00.000000")));
        var account = Layouts.ACCOUNT.decode(Layouts.ACCOUNT.encode(Map.of("ACCT-CREDIT-LIMIT", new BigDecimal("1000"),
                "ACCT-CURR-CYC-CREDIT", new BigDecimal("950"), "ACCT-CURR-BAL", new BigDecimal("9999"),
                "ACCT-EXPIRAION-DATE", "2022-07-04")));
        assertEquals(0, PostingRun.validation(daily, Map.of(), account));
        daily.put("DALYTRAN-AMT", new BigDecimal("50.01"));
        assertEquals(102, PostingRun.validation(daily, Map.of(), account));
        account.put("ACCT-EXPIRAION-DATE", "2022-07-03");
        assertEquals(103, PostingRun.validation(daily, Map.of(), account));
        assertEquals(100, PostingRun.validation(daily, null, account));
        assertEquals(101, PostingRun.validation(daily, Map.of(), null));
    }

    @Test
    void dateParmExcludesDetailsAndSuppressesEofTotals() throws Exception {
        var input = input();
        Files.writeString(input.resolve(PostingRun.PREFIX + "DATEPARM.txt"), "2022-07-06 2022-07-07\n");
        var output = temporary.resolve("output");
        assertEquals(0, run.run(input, output));
        assertEquals("", Files.readString(output.resolve("datasets/" + PostingRun.PREFIX + "TRANREPT.G0001V00.txt")));
        assertEquals("START OF EXECUTION OF PROGRAM CBTRN03C\nReporting from 2022-07-06 to 2022-07-07\nEND OF EXECUTION OF PROGRAM CBTRN03C\n",
                Files.readString(output.resolve("sysout/TRANREPT.STEP10R.txt")));
    }

    @Test
    void missingReportLookupAbendsButRetainsPostedTransactions() throws Exception {
        var input = input();
        Files.writeString(input.resolve(PostingRun.PREFIX + "TRANTYPE.PS.txt"), "");
        var output = temporary.resolve("output");
        assertEquals(12, run.run(input, output));
        assertEquals(4, PostingTables.TRANSACTION.read(jdbc).size());
        var log = Files.readString(output.resolve("sysout/TRANREPT.STEP10R.txt"));
        assertTrue(log.endsWith("INVALID TRANSACTION TYPE : 01\nFILE STATUS IS: NNNN0023\nABENDING PROGRAM\nCEE3ABD: USER ABEND U+0000000999\n"));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP10R\": 12"));
    }

    @Test
    void duplicateTransactionAbendsAfterBalanceUpdates() throws Exception {
        var input = input();
        var path = input.resolve(PostingRun.PREFIX + "DALYTRAN.PS.txt");
        var first = Files.readAllLines(path).getFirst();
        Files.write(path, List.of(first, first));
        var output = temporary.resolve("output");
        assertEquals(12, run.run(input, output));
        assertEquals(1, PostingTables.TRANSACTION.read(jdbc).size());
        assertTrue(Files.readString(output.resolve("sysout/POSTTRAN.STEP15.txt")).endsWith(
                "ERROR WRITING TO TRANSACTION FILE\nFILE STATUS IS: NNNN0022\nABENDING PROGRAM\nCEE3ABD: USER ABEND U+0000000999\n"));
    }

    @Test
    void reportPaginationStableOrderAndEofDoubleCount() throws Exception {
        var input = input();
        run.run(input, temporary.resolve("initial"));
        var row = PostingTables.TRANSACTION.read(jdbc).getFirst();
        row.put("TRAN-AMT", BigDecimal.ONE);
        var rows = new ArrayList<Map<String, Object>>();
        for (int i = 1; i <= 18; i++) {
            var copy = new LinkedHashMap<>(row);
            copy.put("TRAN-ID", String.format("%016d", i));
            rows.add(copy);
        }
        var output = Files.createDirectory(temporary.resolve("report"));
        Files.createDirectory(output.resolve("sysout"));
        Files.createDirectory(output.resolve("datasets"));
        assertEquals(0, new PostingReport(jdbc).run(input, output, rows));
        var lines = Files.readAllLines(output.resolve("datasets/" + PostingRun.PREFIX + "TRANREPT.G0001V00.txt"));
        assertEquals(2, lines.stream().filter(line -> line.startsWith("DALYREPT")).count());
        assertEquals("Page Total " + ".".repeat(86) + "+         16.00", lines.get(20));
        assertEquals("Grand Total" + ".".repeat(86) + "+         19.00", lines.getLast());
    }

    @Test
    void numericColumnsMirrorCodecPrecisionAndScale() {
        for (var table : PostingTables.ALL) {
            for (var field : table.layout().fields()) {
                if (field.kind() != org.carddemo.modern.codec.Field.Kind.ZONED) continue;
                var result = jdbc.queryForMap("SELECT numeric_precision, numeric_scale FROM information_schema.columns WHERE table_schema='posting_test' AND table_name=? AND column_name=?",
                        table.name().replace("\"", ""), field.name().toLowerCase(java.util.Locale.ROOT).replace('-', '_'));
                assertEquals(field.length(), ((Number) result.get("numeric_precision")).intValue());
                assertEquals(field.scale(), ((Number) result.get("numeric_scale")).intValue());
            }
        }
    }

    @Test
    void missingInputsAreEmptyAndDuplicateIndexedLoadsKeepFirst() throws Exception {
        var empty = Files.createDirectory(temporary.resolve("empty"));
        var output = temporary.resolve("empty-output");
        assertEquals(0, run.run(empty, output));
        assertEquals(List.of(), PostingTables.TRANSACTION.read(jdbc));
        assertEquals("START OF EXECUTION OF PROGRAM CBTRN03C\nEND OF EXECUTION OF PROGRAM CBTRN03C\n",
                Files.readString(output.resolve("sysout/TRANREPT.STEP10R.txt")));
        var input = input();
        var path = input.resolve(PostingRun.PREFIX + "ACCTDATA.PS.txt");
        var original = Files.readAllLines(path).getFirst();
        var duplicate = Layouts.ACCOUNT.decode(original);
        duplicate.put("ACCT-CURR-BAL", new BigDecimal("9999"));
        Files.writeString(path, Layouts.ACCOUNT.encodeLine(duplicate) + "\n", java.nio.file.StandardOpenOption.APPEND);
        output = temporary.resolve("duplicate-output");
        assertEquals(0, run.run(input, output));
        assertTrue(Files.readString(output.resolve("setup/ACCTFILE.STEP15.txt")).contains("IDC3302I DUPLICATE KEYS SKIPPED 000000001"));
        assertEquals("{\"STEP15\": 4}", Files.readString(output.resolve("setup/ACCTFILE.rc.json")));
        assertEquals(((BigDecimal) Layouts.ACCOUNT.decode(original).get("ACCT-CURR-BAL")).subtract(new BigDecimal("75.00")),
                PostingTables.ACCOUNT.read(jdbc).getFirst().get("ACCT-CURR-BAL"));
    }
}
