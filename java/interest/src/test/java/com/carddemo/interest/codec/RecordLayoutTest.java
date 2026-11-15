package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.DisclosureGroupRecord.Rate;
import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.CategoryBalance;
import com.carddemo.interest.domain.InterestTransaction;

/** The five layouts against the records of i01-baseline (input and recorded legacy output). */
class RecordLayoutTest {

    private static final String CASE = "i01-baseline";

    private static List<String> expectedDataset(String name) {
        return Fixtures.lines(Fixtures.expected(CASE).resolve("datasets").resolve(name));
    }

    @Test
    void cvtra01y_decodesTheBalanceOfI01() {
        // 00000002001 / 01 / 0001 / 0000010000{ = account 2001, type 01, category 1, 1000.00
        String image = RecordImage.fromLine("000000020010100010000010000{", CategoryBalanceRecord.LENGTH);

        assertEquals(new CategoryBalance(2001L, "01", 1, new BigDecimal("1000.00")), CategoryBalanceRecord.decode(image));
        assertEquals(50, image.length());
    }

    @Test
    void cvtra02y_decodesTheRateOfI01() {
        // "GOLD      " / 01 / 0001 / 00120{ = 12.00 %
        String image = RecordImage.fromLine("GOLD      01000100120{", DisclosureGroupRecord.LENGTH);

        assertEquals(new Rate("GOLD", "01", 1, new BigDecimal("12.00")), DisclosureGroupRecord.decode(image));
    }

    @Test
    void cvact03y_decodesTheCardOfI01() {
        String image = RecordImage.fromLine("400000000000200100000200100000002001", CardXrefRecord.LENGTH);

        assertEquals(new CardXrefRecord.CardOfAccount(2001L, "4000000000002001"), CardXrefRecord.decode(image));
    }

    @Test
    void cvact01y_decodesTheAccountOfI01() {
        String image = RecordImage.fromLine(
                Fixtures.lines(Fixtures.input(CASE).resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")).get(0), AccountRecord.LENGTH);

        assertEquals(new Account(2001L, "Y", new BigDecimal("1000.00"), "GOLD"), AccountRecord.decode(image));
    }

    @Test
    void intR07_accountBreakRewritesThreeFieldsOnly() {
        // input record of account 2001 with 1010.00 posted must equal the recorded legacy record, byte for byte
        String input = Fixtures.lines(Fixtures.input(CASE).resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")).get(0);
        String image = RecordImage.fromLine(input, AccountRecord.LENGTH);

        String updated = AccountRecord.withInterestPosted(image, new BigDecimal("1010.00"));

        assertEquals(expectedDataset("AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt").get(0), RecordImage.toLine(updated));
        assertEquals(300, updated.length());
    }

    @Test
    void e4_accountFillerRoundTrip() {
        // synthetic FILLER content: the 178 bytes after ACCT-GROUP-ID come back untouched
        String filler = "filler bytes <>{}[] 0123456789".repeat(6).substring(0, 178);
        String image = RecordImage.fromLine(
                Fixtures.lines(Fixtures.input(CASE).resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")).get(0), 122) + filler;

        String updated = AccountRecord.withInterestPosted(image, new BigDecimal("1010.00"));

        assertEquals(filler, updated.substring(122));
        assertEquals(image.substring(24, 78), updated.substring(24, 78));
        assertEquals(image.substring(102), updated.substring(102));
    }

    @Test
    void intR06_transactionFieldsFixedWidth() {
        // the first recorded SYSTRAN record of i01-baseline, field by field from CBACT04C:476-498
        InterestTransaction transaction = new InterestTransaction("2022071800000001", "01", 5, "System",
                "Int. for a/c 00000002001", new BigDecimal("10.00"), 0L, "4000000000002001",
                "2022-07-05-10.00.00.000000", "2022-07-05-10.00.00.000000");

        String image = TransactionRecord.encode(transaction);

        assertEquals(350, image.length());
        assertEquals(expectedDataset("AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt").get(0), RecordImage.toLine(image));
        // INT-T04, G05: the tail of TRAN-DESC and the FILLER are spaces
        assertEquals(" ".repeat(76), image.substring(56, 132));
        assertEquals(" ".repeat(20), image.substring(330));
    }

    @Test
    void intQ02_idLongerThanTheFieldIsRefused() {
        // counter 1000000 would make a 17-character TRAN-ID: refused, not cut (INT-H05 stays open)
        InterestTransaction transaction = new InterestTransaction("20220718001000000", "01", 5, "System",
                "Int. for a/c 00000002001", new BigDecimal("10.00"), 0L, "4000000000002001",
                "2022-07-05-10.00.00.000000", "2022-07-05-10.00.00.000000");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.carddemo.interest.domain.InterestRunStoppedException.class,
                () -> TransactionRecord.encode(transaction));
    }
}
