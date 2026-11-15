package com.carddemo.interest.export;

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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.domain.StopReason;

/** The export without a database: the rows come from a list. Expected values are written by hand (S7.4). */
class DatasetExportTest {

    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    private static final String TIMESTAMP = "2022-07-05-10.00.00.000000";

    @TempDir
    Path temp;

    /** A run of one account and one transaction, kept in memory. */
    private static final class Rows implements ExportSource {
        RunToExport run = new RunToExport(7, CALCULATED, 1L, new BigDecimal("-3.00"));
        final List<TransactionRow> transactions = new ArrayList<>(List.of(new TransactionRow(1, "2022071800000001",
                "01", 5, "System", "Int. for a/c 00000002008", new BigDecimal("-3.00"), 0, "", "", "",
                "4000000000002008", TIMESTAMP, TIMESTAMP, spaces(20))));
        final List<AccountRow> accounts = new ArrayList<>(List.of(new AccountRow(2008, "Y", new BigDecimal("-300.00"),
                new BigDecimal("5000.00"), new BigDecimal("2500.00"), "2019-03-15", "2026-12-31", "2026-12-31",
                new BigDecimal("0.00"), new BigDecimal("300.00"), "10115", "GOLD", spaces(178))));
        boolean stillCalculated = true;
        int marked;

        @Override
        public Optional<RunToExport> run(long runId) {
            return runId == run.runId() ? Optional.of(run) : Optional.empty();
        }

        @Override
        public List<TransactionRow> transactions(long runId) {
            return transactions;
        }

        @Override
        public List<AccountRow> accounts() {
            return accounts;
        }

        @Override
        public boolean markExported(long runId) {
            marked++;
            return stillCalculated;
        }
    }

    private static byte[] spaces(int length) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) 0x20);
        return bytes;
    }

    private static List<String> entries(Path dir) throws IOException {
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void i06NegativeBalance_rows_giveTheRecordedFiles() {
        Rows rows = new Rows();
        Path output = temp.resolve("actual");

        new DatasetExport(rows).export(7, output, ExportFormat.FIXTURE);

        Path expected = Fixtures.expected("i06-negative-balance");
        for (String file : List.of(TRANSACTIONS, ACCOUNTS, "rc.json")) {
            assertEquals(Fixtures.text(expected.resolve(file)), Fixtures.text(output.resolve(file)), file);
        }
        assertEquals(1, rows.marked);
    }

    @Test
    void existingDirectory_isReplacedAsAWhole_andTheRunLogOfTheCalculationIsKept() throws IOException {
        Path output = temp.resolve("actual");
        Files.createDirectories(output.resolve("datasets"));
        Files.createDirectories(output.resolve("sysout"));
        Files.writeString(output.resolve("datasets/OLD.GENERATION.txt"), "old");
        Files.writeString(output.resolve("old-rc.json"), "{}");
        Files.writeString(output.resolve("sysout/INTCALC.STEP15.txt"), "START OF EXECUTION OF PROGRAM CBACT04C\n");

        new DatasetExport(new Rows()).export(7, output, ExportFormat.FIXTURE);

        assertEquals(List.of("datasets", "rc.json", "sysout"), entries(output));
        assertEquals(List.of("AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt", "AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt"),
                entries(output.resolve("datasets")));
        assertEquals("START OF EXECUTION OF PROGRAM CBACT04C\n",
                Fixtures.text(output.resolve("sysout/INTCALC.STEP15.txt")));
        assertEquals(List.of("actual"), entries(temp));
    }

    @Test
    void runWithoutTransactions_writesAnEmptyGeneration() throws IOException {
        Rows rows = new Rows();
        rows.transactions.clear();
        rows.run = new ExportSource.RunToExport(7, ExportSource.CALCULATED, 0L, new BigDecimal("0.00"));
        Path output = temp.resolve("actual");

        DatasetExport.Exported exported = new DatasetExport(rows).export(7, output, ExportFormat.FIXTURE);

        assertEquals(0, Files.size(output.resolve(TRANSACTIONS)));
        assertEquals(new BigDecimal("0.00"), exported.amountSum());
    }

    private void assertRefused(Rows rows, String expectedInMessage) throws IOException {
        Path output = temp.resolve("actual");
        Files.createDirectories(output);
        Files.writeString(output.resolve("kept.txt"), "kept");

        ExportRefusedException refused = assertThrows(ExportRefusedException.class,
                () -> new DatasetExport(rows).export(7, output, ExportFormat.FIXTURE));

        assertEquals(StopReason.WRITE_OUTPUT, refused.reason());
        assertTrue(refused.getMessage().contains(expectedInMessage), refused.getMessage());
        assertEquals(List.of("kept.txt"), entries(output));
        assertEquals(List.of("actual"), entries(temp));
        assertEquals(0, rows.marked);
    }

    @Test
    void statusOtherThanCalculated_isRefused() throws IOException {
        Rows rows = new Rows();
        rows.run = new ExportSource.RunToExport(7, "LOADED", 1L, new BigDecimal("-3.00"));
        assertRefused(rows, "status LOADED");
    }

    @Test
    void transactionCountThatDiffersFromRc5_isRefused() throws IOException {
        Rows rows = new Rows();
        rows.run = new ExportSource.RunToExport(7, ExportSource.CALCULATED, 2L, new BigDecimal("-3.00"));
        assertRefused(rows, "RC5");
    }

    @Test
    void amountSumThatDiffersFromRc5_isRefused() throws IOException {
        Rows rows = new Rows();
        rows.run = new ExportSource.RunToExport(7, ExportSource.CALCULATED, 1L, new BigDecimal("3.00"));
        assertRefused(rows, "RC5");
    }

    @Test
    void runWithoutRc5Figures_isRefused() throws IOException {
        Rows rows = new Rows();
        rows.run = new ExportSource.RunToExport(7, ExportSource.CALCULATED, null, null);
        assertRefused(rows, "RC5");
    }

    @Test
    void fillerOfTheWrongLength_isRefused() throws IOException {
        Rows rows = new Rows();
        ExportSource.AccountRow account = rows.accounts.get(0);
        rows.accounts.set(0, new ExportSource.AccountRow(account.accountId(), account.activeStatus(),
                account.currentBalance(), account.creditLimit(), account.cashCreditLimit(), account.openDate(),
                account.expirationDate(), account.reissueDate(), account.cycleCredit(), account.cycleDebit(),
                account.zip(), account.groupId(), spaces(177)));
        assertRefused(rows, "FILLER");
    }

    @Test
    void runThatLeftCalculatedDuringTheExport_isReportedAsRefused() {
        Rows rows = new Rows();
        rows.stillCalculated = false;

        assertThrows(ExportRefusedException.class,
                () -> new DatasetExport(rows).export(7, temp.resolve("actual"), ExportFormat.FIXTURE));
    }

    @Test
    void fillerBytes_areWrittenBackByteForByte() {
        Rows rows = new Rows();
        byte[] filler = spaces(178);
        filler[0] = 'Z';
        filler[177] = (byte) 0xE9;
        ExportSource.AccountRow account = rows.accounts.get(0);
        rows.accounts.set(0, new ExportSource.AccountRow(account.accountId(), account.activeStatus(),
                account.currentBalance(), account.creditLimit(), account.cashCreditLimit(), account.openDate(),
                account.expirationDate(), account.reissueDate(), account.cycleCredit(), account.cycleDebit(),
                account.zip(), account.groupId(), filler));
        Path output = temp.resolve("actual");

        new DatasetExport(rows).export(7, output, ExportFormat.FIXTURE);

        String line = Fixtures.lines(output.resolve(ACCOUNTS)).get(0);
        assertEquals(300, line.length());
        assertEquals('Z', line.charAt(122));
        assertEquals('\u00e9', line.charAt(299));
    }

    @Test
    void format_isFixtureOrHost() {
        assertEquals(ExportFormat.FIXTURE, ExportFormat.of("fixture"));
        assertEquals(ExportFormat.HOST, ExportFormat.of("host"));
        assertThrows(ExportRefusedException.class, () -> ExportFormat.of("csv"));
        assertThrows(ExportRefusedException.class, () -> ExportFormat.of(null));
        assertFalse(ExportFormat.HOST.fileOf("A.B").endsWith(".txt"));
    }

    @Test
    void returnCodes_haveTheRecordedShape() {
        assertEquals(Fixtures.text(Fixtures.expected("i01-baseline").resolve("rc.json")), RecordedReturnCodes.json(0));
    }
}
