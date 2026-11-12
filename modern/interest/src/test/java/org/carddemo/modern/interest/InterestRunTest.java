package org.carddemo.modern.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.carddemo.modern.codec.Layouts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class InterestRunTest {
    @TempDir Path temporary;
    private InterestRun run;
    private Path input;
    private JdbcTemplate jdbc;
    private String parmDate;

    @BeforeEach
    void setUp() throws Exception {
        var datasource = new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
        new JdbcTemplate(datasource).execute("CREATE SCHEMA IF NOT EXISTS interest_test");
        datasource.setUrl("jdbc:postgresql://db:5432/carddemo?currentSchema=interest_test");
        jdbc = new JdbcTemplate(datasource);
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("schema.sql")).execute(datasource);
        run = new InterestRun(jdbc);
        var jcl = Files.readString(Path.of("../../app/jcl/INTCALC.jcl"));
        var parm = java.util.regex.Pattern.compile("PARM='([^']*)'").matcher(jcl);
        assertTrue(parm.find());
        parmDate = parm.group(1);
        input = Files.createDirectory(temporary.resolve("input"));
        try (var paths = Files.list(Path.of("../../fixtures/interest/i01-baseline/input"))) {
            for (var path : paths.toList()) Files.copy(path, input.resolve(path.getFileName()));
        }
    }

    @Test
    void truncatesEachMonthlyAmountTowardZeroBeforeAccumulating() {
        assertEquals(new BigDecimal("0.00"), InterestRun.monthlyInterest(new BigDecimal("1"), new BigDecimal("6")));
        assertEquals(new BigDecimal("0.01"), InterestRun.monthlyInterest(new BigDecimal("3"), new BigDecimal("6")));
        assertEquals(new BigDecimal("-0.01"), InterestRun.monthlyInterest(new BigDecimal("-3"), new BigDecimal("6")));
        assertEquals(new BigDecimal("0.00"), InterestRun.monthlyInterest(new BigDecimal("1"), new BigDecimal("6"))
                .add(InterestRun.monthlyInterest(new BigDecimal("1"), new BigDecimal("6"))));
    }

    @Test
    void tieFixtureTruncatesTransactionAndPostedBalance() throws Exception {
        useFixture("i02-tie-half-cent");
        var output = temporary.resolve("output");
        assertEquals(0, run.run(input, output, parmDate));
        var transaction = Layouts.TRAN.decode(Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).getFirst());
        assertEquals(new BigDecimal("12.35"), transaction.get("TRAN-AMT"));
        var account = Layouts.ACCOUNT.decode(Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt")).getFirst());
        assertEquals(new BigDecimal("1247.85"), account.get("ACCT-CURR-BAL"));
    }

    @Test
    void negativeFixtureStillWritesNegativeThreeDollars() throws Exception {
        useFixture("i06-negative-balance");
        var output = temporary.resolve("output");
        assertEquals(0, run.run(input, output, parmDate));
        var transaction = Layouts.TRAN.decode(Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).getFirst());
        assertEquals(new BigDecimal("-3.00"), transaction.get("TRAN-AMT"));
    }

    private void useFixture(String fixture) throws Exception {
        try (var paths = Files.list(Path.of("../../fixtures/interest/" + fixture + "/input"))) {
            for (var path : paths.toList()) Files.copy(path, input.resolve(path.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    void baselineUpdatesOnlyTheAccountClosedByABreakAndExportsAllTransactions() throws Exception {
        var output = temporary.resolve("output");
        assertEquals(0, run.run(input, output, parmDate));
        var accounts = Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt"));
        assertEquals(2, accounts.size());
        for (var line : accounts) {
            var account = Layouts.ACCOUNT.decode(line);
            assertEquals(new BigDecimal("0.00"), account.get("ACCT-CURR-CYC-DEBIT"));
        }
        var first = Layouts.ACCOUNT.decode(accounts.getFirst());
        assertEquals(new BigDecimal("1010.00"), first.get("ACCT-CURR-BAL"));
        assertEquals(new BigDecimal("0.00"), first.get("ACCT-CURR-CYC-CREDIT"));
        var last = Layouts.ACCOUNT.decode(accounts.getLast());
        assertEquals(new BigDecimal("2400.00"), last.get("ACCT-CURR-BAL"));
        assertEquals(new BigDecimal("2400.00"), last.get("ACCT-CURR-CYC-CREDIT"));
        assertEquals(2, Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).size());
        assertTrue(Files.readString(output.resolve("sysout/INTCALC.STEP15.txt")).endsWith("END OF EXECUTION OF PROGRAM CBACT04C\n"));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"maxrc\": 0"));
        var sysout = Files.readAllLines(output.resolve("sysout/INTCALC.STEP15.txt"));
        var balances = Files.readAllLines(input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt"));
        assertEquals(4, sysout.size());
        assertEquals(Layouts.CATEGORY_BALANCE.encode(Layouts.CATEGORY_BALANCE.decode(balances.getFirst())), sysout.get(1));
        assertEquals(Layouts.CATEGORY_BALANCE.encode(Layouts.CATEGORY_BALANCE.decode(balances.getLast())), sysout.get(2));
    }

    @Test
    void singleAccountWithMultipleCategoriesIsNeverRewritten() throws Exception {
        useFixture("i05-multi-category");
        var output = temporary.resolve("output");
        assertEquals(0, run.run(input, output, parmDate));
        var account = Layouts.ACCOUNT.decode(Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt")).getFirst());
        assertEquals(new BigDecimal("1500.00"), account.get("ACCT-CURR-BAL"));
        assertEquals(new BigDecimal("1500.00"), account.get("ACCT-CURR-CYC-CREDIT"));
        assertEquals(2, Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).size());
    }

    @Test
    void accountBreakPostsTheSumOfAllPrecedingCategories() throws Exception {
        useFixture("i05-multi-category");
        var accountFile = input.resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt");
        var account = Layouts.ACCOUNT.decode(Files.readAllLines(accountFile).getFirst());
        account.put("ACCT-ID", new BigDecimal("2008"));
        Files.writeString(accountFile, Layouts.ACCOUNT.encodeLine(account) + "\n", java.nio.file.StandardOpenOption.APPEND);
        var xrefFile = input.resolve("AWS.M2.CARDDEMO.CARDXREF.PS.txt");
        var xref = Layouts.XREF.decode(Files.readAllLines(xrefFile).getFirst());
        xref.put("XREF-ACCT-ID", new BigDecimal("2008"));
        xref.put("XREF-CARD-NUM", "4000000000002008");
        Files.writeString(xrefFile, Layouts.XREF.encodeLine(xref) + "\n", java.nio.file.StandardOpenOption.APPEND);
        var balanceFile = input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt");
        var balance = Layouts.CATEGORY_BALANCE.decode(Files.readAllLines(balanceFile).getFirst());
        balance.put("TRANCAT-ACCT-ID", new BigDecimal("2008"));
        Files.writeString(balanceFile, Layouts.CATEGORY_BALANCE.encodeLine(balance) + "\n", java.nio.file.StandardOpenOption.APPEND);
        var output = temporary.resolve("output");
        assertEquals(0, run.run(input, output, parmDate));
        var accounts = Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt"));
        var posted = Layouts.ACCOUNT.decode(accounts.getFirst());
        assertEquals(new BigDecimal("1517.50"), posted.get("ACCT-CURR-BAL"));
        assertEquals(new BigDecimal("0.00"), posted.get("ACCT-CURR-CYC-CREDIT"));
        var last = Layouts.ACCOUNT.decode(accounts.getLast());
        assertEquals(new BigDecimal("1500.00"), last.get("ACCT-CURR-BAL"));
        assertEquals(new BigDecimal("1500.00"), last.get("ACCT-CURR-CYC-CREDIT"));
        assertEquals(3, Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).size());
    }

    @Test
    void emptyBalancesProduceNoTransactions() throws Exception {
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt"), "");
        var output = temporary.resolve("output");
        assertEquals(0, run.run(input, output, parmDate));
        assertEquals(0, Files.size(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")));
        assertEquals(2, Files.readAllLines(output.resolve("sysout/INTCALC.STEP15.txt")).size());
    }

    @Test
    void missingDisclosureRecordsTheLegacyAbend() throws Exception {
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt"), "");
        assertAbend(java.util.List.of("DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE",
                "ERROR READING DEFAULT DISCLOSURE GROUP"));
    }

    @Test
    void missingAccountRecordsTheLegacyAbend() throws Exception {
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt"), "");
        assertAbend(java.util.List.of("ACCOUNT NOT FOUND: 00000002001", "ERROR READING ACCOUNT FILE"));
    }

    @Test
    void missingXrefRecordsTheLegacyAbend() throws Exception {
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.CARDXREF.PS.txt"), "");
        assertAbend(java.util.List.of("ACCOUNT NOT FOUND: 00000002001", "ERROR READING XREF FILE"));
    }

    private void assertAbend(java.util.List<String> diagnostic) throws Exception {
        var output = temporary.resolve("output");
        assertEquals(12, run.run(input, output, parmDate));
        var sysout = Files.readAllLines(output.resolve("sysout/INTCALC.STEP15.txt"));
        assertEquals(diagnostic, sysout.subList(2, 2 + diagnostic.size()));
        assertEquals(java.util.List.of("FILE STATUS IS: NNNN0023", "ABENDING PROGRAM",
                "CEE3ABD: USER ABEND U+0000000999"), sysout.subList(sysout.size() - 3, sysout.size()));
        assertTrue(!sysout.contains("END OF EXECUTION OF PROGRAM CBACT04C"));
        var rc = Files.readString(output.resolve("rc.json"));
        assertTrue(rc.contains("\"maxrc\": 12"));
        assertTrue(rc.contains("\"STEP15\": 12"));
        assertEquals(0, Files.size(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")));
    }

    @Test
    void transactionPrefixComesFromTheJobParameter() throws Exception {
        var output = temporary.resolve("output");
        assertEquals(0, run.run(input, output, "2026020100"));
        var transaction = Layouts.TRAN.decode(Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).getFirst());
        assertEquals("2026020100000001", transaction.get("TRAN-ID"));
        assertEquals("2026020100000001", jdbc.queryForObject("SELECT tran_id FROM \"transaction\" ORDER BY tran_id LIMIT 1", String.class));
        assertThrows(IllegalArgumentException.class, () -> run.run(input, output, "invalid"));
    }

    @Test
    void databaseColumnsHaveTheCopybookNumericPrecisionAndScale() {
        for (var table : InterestTables.ALL) {
            for (var field : table.layout().fields()) {
                if (field.kind() != org.carddemo.modern.codec.Field.Kind.ZONED) continue;
                var column = field.name().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
                var metadata = jdbc.queryForMap("SELECT numeric_precision, numeric_scale FROM information_schema.columns "
                        + "WHERE table_schema = 'interest_test' AND table_name = ? AND column_name = ?",
                        table.name().replace("\"", ""), column);
                assertEquals(field.length(), ((Number) metadata.get("numeric_precision")).intValue());
                assertEquals(field.scale(), ((Number) metadata.get("numeric_scale")).intValue());
            }
        }
    }
}
