package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.DisclosureGroupRecord.Rate;
import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.CategoryBalance;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.InterestTransaction;

/** The five layouts against the records of i01-baseline (input and recorded legacy output). */
class RecordLayoutTest {

    private static final String CASE = "i01-baseline";

    private static List<String> expectedDataset(String name) {
        return Fixtures.lines(Fixtures.expected(CASE).resolve("datasets").resolve(name));
    }

    private static final CodePage FIXTURE = CodePage.fixture();
    private static final RecordSource SOURCE = new RecordSource("i01-baseline", 1);

    private static byte[] accountOfI01() {
        return RecordImage.fromLine(
                Fixtures.lines(Fixtures.input(CASE).resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")).get(0),
                AccountRecord.LAYOUT.length(), FIXTURE);
    }

    @Test
    void cvtra01y_decodesTheBalanceOfI01() {
        // 00000002001 / 01 / 0001 / 0000010000{ = account 2001, type 01, category 1, 1000.00
        byte[] record = RecordImage.fromLine("000000020010100010000010000{", CategoryBalanceRecord.LAYOUT.length(), FIXTURE);

        assertEquals(new CategoryBalance(2001L, "01", 1, new BigDecimal("1000.00")),
                new CategoryBalanceRecord(FIXTURE).decode(record, SOURCE));
        assertEquals(50, record.length);
    }

    @Test
    void cvtra01y_decodesTheSameBalanceFromCp037() {
        CodePage host = CodePage.host();
        byte[] record = ("000000020010100010000010000{" + " ".repeat(22)).getBytes(host.charset());

        assertEquals((byte) 0xC0, record[27]);
        assertEquals(new CategoryBalance(2001L, "01", 1, new BigDecimal("1000.00")),
                new CategoryBalanceRecord(host).decode(record, SOURCE));
    }

    @Test
    void cvtra02y_decodesTheRateOfI01() {
        // "GOLD      " / 01 / 0001 / 00120{ = 12.00 %
        byte[] record = RecordImage.fromLine("GOLD      01000100120{", DisclosureGroupRecord.LAYOUT.length(), FIXTURE);

        assertEquals(new Rate("GOLD", "01", 1, new BigDecimal("12.00")), new DisclosureGroupRecord(FIXTURE).decode(record, SOURCE));
    }

    @Test
    void cvact03y_decodesTheCardOfI01() {
        byte[] record = RecordImage.fromLine("400000000000200100000200100000002001", CardXrefRecord.LAYOUT.length(), FIXTURE);

        assertEquals(new CardXrefRecord.CardOfAccount(2001L, "4000000000002001"), new CardXrefRecord(FIXTURE).decode(record, SOURCE));
    }

    @Test
    void cvact01y_decodesTheAccountOfI01() {
        assertEquals(new Account(2001L, "Y", new BigDecimal("1000.00"), "GOLD"), new AccountRecord(FIXTURE).decode(accountOfI01(), SOURCE));
    }

    @Test
    void intR07_accountBreakRewritesThreeFieldsOnly() {
        // input record of account 2001 with 1010.00 posted must equal the recorded legacy record, byte for byte
        byte[] updated = new AccountRecord(FIXTURE).withInterestPosted(accountOfI01(), new BigDecimal("1010.00"), SOURCE);

        assertEquals(expectedDataset("AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt").get(0), RecordImage.toLine(updated, FIXTURE));
        assertEquals(300, updated.length);
    }

    @Test
    void e4_accountFillerRoundTrip() {
        // synthetic FILLER content, bytes that are no character included: the 178 bytes after ACCT-GROUP-ID come back untouched
        byte[] record = accountOfI01();
        for (int i = 122; i < 300; i++) {
            record[i] = (byte) i;
        }

        byte[] updated = new AccountRecord(FIXTURE).withInterestPosted(record, new BigDecimal("1010.00"), SOURCE);

        assertArrayEquals(Arrays.copyOfRange(record, 122, 300), Arrays.copyOfRange(updated, 122, 300));
        assertArrayEquals(Arrays.copyOfRange(record, 24, 78), Arrays.copyOfRange(updated, 24, 78));
        assertArrayEquals(Arrays.copyOfRange(record, 102, 300), Arrays.copyOfRange(updated, 102, 300));
    }

    @Test
    void intR06_transactionFieldsFixedWidth() {
        // the first recorded SYSTRAN record of i01-baseline, field by field from CBACT04C:476-498
        InterestTransaction transaction = new InterestTransaction("2022071800000001", "01", 5, "System",
                "Int. for a/c 00000002001", new BigDecimal("10.00"), 0L, "4000000000002001",
                "2022-07-05-10.00.00.000000", "2022-07-05-10.00.00.000000");

        byte[] record = new TransactionRecord(FIXTURE).encode(transaction, SOURCE);
        String image = FIXTURE.display(record);

        assertEquals(350, record.length);
        assertEquals(expectedDataset("AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt").get(0), RecordImage.toLine(record, FIXTURE));
        // INT-T04, G05: the tail of TRAN-DESC and the FILLER are spaces
        assertEquals(" ".repeat(76), image.substring(56, 132));
        assertEquals(" ".repeat(20), image.substring(330));
    }

    @Test
    void intT04_g05_theSameTransactionInCp037() {
        CodePage host = CodePage.host();
        InterestTransaction transaction = new InterestTransaction("2022071800000001", "01", 5, "System",
                "Int. for a/c 00000002001", new BigDecimal("10.00"), 0L, "4000000000002001",
                "2022-07-05-10.00.00.000000", "2022-07-05-10.00.00.000000");

        byte[] record = new TransactionRecord(host).encode(transaction, SOURCE);

        // the host image is the fixture image through the cp037 table: C-signed amount, FILLER X'40'
        String fixtureImage = FIXTURE.display(new TransactionRecord(FIXTURE).encode(transaction, SOURCE));
        assertArrayEquals(fixtureImage.getBytes(host.charset()), record);
        assertEquals((byte) 0xC0, record[142]);
        assertEquals((byte) 0x40, record[349]);
    }

    @Test
    void intQ02_idLongerThanTheFieldIsRefused() {
        // counter 1000000 would make a 17-character TRAN-ID: refused, not cut (INT-H05 stays open)
        InterestTransaction transaction = new InterestTransaction("20220718001000000", "01", 5, "System",
                "Int. for a/c 00000002001", new BigDecimal("10.00"), 0L, "4000000000002001",
                "2022-07-05-10.00.00.000000", "2022-07-05-10.00.00.000000");

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> new TransactionRecord(FIXTURE).encode(transaction, new RecordSource("SYSTRAN", 3)));

        assertEquals("RECORD_REJECTED (INT-R08): file SYSTRAN record 3 field TRAN-ID: value longer than the field (E3)",
                stop.getMessage());
    }
}
