package com.carddemo.batch.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behaviour pinned from GnuCOBOL runs of the original CBACT04C (golden cases i01-i07 and the
 * characterization cases c01-c10 under java/interest/characterization). Each test names the
 * run it was observed in.
 */
class Cbact04cQuirksTest {
    private static final String CLOCK = "2022-07-05 10:00:00.00";
    private static final List<String> DISCGRP = List.of(
        disc("DEFAULT", "01", 1, "21.00"), disc("DEFAULT", "01", 2, "27.00"), disc("DEFAULT", "02", 1, "0"),
        disc("GOLD", "01", 1, "12.00"), disc("GOLD", "01", 2, "18.00"), disc("GOLD", "02", 1, "0"));

    @Test
    void interestIsTruncatedTowardZeroNotRounded() {
        // c01: 250.50 * 18.00 / 1200 = 3.7575 -> 3.75; c02: -1235.50 * 12.00 / 1200 = -12.355 -> -12.35
        assertEquals(new BigDecimal("3.75"), Cbact04c.monthlyInterest(new BigDecimal("250.50"), new BigDecimal("18.00")));
        assertEquals(new BigDecimal("-12.35"), Cbact04c.monthlyInterest(new BigDecimal("-1235.50"), new BigDecimal("12.00")));
        // c02: -0.33 * 12.00 / 1200 = -0.0033 -> 0.00, still written because the rate is not zero
        assertEquals(0, Cbact04c.monthlyInterest(new BigDecimal("-0.33"), new BigDecimal("12.00")).signum());
    }

    @Test
    void interestOverflowDropsHighOrderDigits() {
        // c04: 999999999.99 * 9999.99 / 1200 = 8333324999.9167 -> S9(9)V99 keeps 333324999.91
        assertEquals(new BigDecimal("333324999.91"),
            Cbact04c.monthlyInterest(new BigDecimal("999999999.99"), new BigDecimal("9999.99")));
    }

    @Test
    void lastAccountOnTcatbalfIsNeverRewritten() {
        // c01: 3001 and 3002 are rewritten (interest added, cycle amounts zeroed); 3003 is not
        InterestJob.Result r = run(
            List.of(acct(3001, "1000.00", "GOLD", "120.00", "45.50"), acct(3002, "500.00", "GOLD", "7.00", "8.00")),
            List.of(xref(3001), xref(3002)),
            List.of(tcat(3001, "01", 1, "1000.00"), tcat(3002, "01", 1, "500.00")));
        assertEquals(0, r.returnCode());
        assertEquals(Text.rstrip(acct(3001, "1010.00", "GOLD", "0", "0")), r.accounts().get(0));
        assertEquals(Text.rstrip(acct(3002, "500.00", "GOLD", "7.00", "8.00")), r.accounts().get(1));
        assertEquals(2, r.transactions().size());
    }

    @Test
    void zeroRateWritesNoTransactionButAccountIsStillRewritten() {
        // c03: 3201's only category is type 02 (rate 0): no SYSTRAN record, cycle amounts still zeroed
        InterestJob.Result r = run(
            List.of(acct(3201, "800.00", "GOLD", "55.00", "66.00"), acct(3202, "900.00", "GOLD", "0", "0")),
            List.of(xref(3201), xref(3202)),
            List.of(tcat(3201, "02", 1, "800.00"), tcat(3202, "01", 1, "900.00")));
        assertEquals(Text.rstrip(acct(3201, "800.00", "GOLD", "0", "0")), r.accounts().get(0));
        assertEquals(1, r.transactions().size());
        assertTrue(r.transactions().get(0).contains("Int. for a/c 00000003202"));
    }

    @Test
    void missingDisclosureGroupFallsBackToDefault() {
        // i04: group PLATINUM has no row -> two warning lines, DEFAULT 01/0001 rate 21.00 is used
        InterestJob.Result r = run(
            List.of(acct(2006, "100.00", "PLATINUM", "0", "0")), List.of(xref(2006)),
            List.of(tcat(2006, "01", 1, "1000.00")));
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", tcat(2006, "01", 1, "1000.00"),
            "DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE",
            "END OF EXECUTION OF PROGRAM CBACT04C"), List.of(r.sysout().split("\n")));
        assertEquals("0000000175{", r.transactions().get(0).substring(132, 143));
    }

    @Test
    void missingDefaultGroupAbendsWithRc12AndKeepsEarlierWork() {
        // c05: 3401 is rewritten when 3402 starts; 3402's PLATINUM 01/0009 and DEFAULT 01/0009 are missing
        InterestJob.Result r = run(
            List.of(acct(3401, "300.00", "GOLD", "3.00", "4.00"), acct(3402, "400.00", "PLATINUM", "0", "0")),
            List.of(xref(3401), xref(3402)),
            List.of(tcat(3401, "01", 1, "300.00"), tcat(3402, "01", 9, "400.00")));
        assertEquals(12, r.returnCode());
        assertEquals(Text.rstrip(acct(3401, "303.00", "GOLD", "0", "0")), r.accounts().get(0));
        assertEquals(1, r.transactions().size());
        assertTrue(r.sysout().contains("ERROR READING DEFAULT DISCLOSURE GROUP\nFILE STATUS IS: NNNN0023\n"
            + "ABENDING PROGRAM\nCEE3ABD: USER ABEND U+0000000999\n\nlibcob: warning: implicit CLOSE of TRANSACT-FILE"));
        assertTrue(r.rcJson().contains("\"maxrc\": 12"));
    }

    @Test
    void missingAccountOrCrossReferenceAbends() {
        // c06 / c07
        InterestJob.Result noAccount = run(List.of(), List.of(xref(3502)), List.of(tcat(3502, "01", 1, "1.00")));
        assertTrue(noAccount.sysout().contains("ACCOUNT NOT FOUND: 00000003502\nERROR READING ACCOUNT FILE\n"));
        assertEquals(12, noAccount.returnCode());
        InterestJob.Result noXref = run(List.of(acct(3602, "1.00", "GOLD", "0", "0")), List.of(),
            List.of(tcat(3602, "01", 1, "1.00")));
        assertTrue(noXref.sysout().contains("ACCOUNT NOT FOUND: 00000003602\nERROR READING XREF FILE\n"));
        assertEquals(12, noXref.returnCode());
    }

    @Test
    void transactionCarriesParmDateSequenceCardAndFrozenClock() {
        InterestJob.Result r = run(List.of(acct(2001, "100.00", "GOLD", "0", "0")), List.of(xref(2001)),
            List.of(tcat(2001, "01", 1, "1000.00"), tcat(2001, "01", 2, "100.00")));
        String second = r.transactions().get(1);
        assertEquals("2022071800000002", second.substring(0, 16));
        assertEquals("010005System    Int. for a/c 00000002001", second.substring(16, 56));
        assertEquals("0000000015{000000000", second.substring(132, 152));
        assertEquals(card(2001), second.substring(262, 278));
        assertEquals("2022-07-05-10.00.00.0000002022-07-05-10.00.00.000000", second.substring(278));
    }

    @Test
    void liveClockIsReadOncePerTransactionForBothTimestamps() {
        // 1300-B-WRITE-TX performs Z-GET-DB2-FORMAT-TIMESTAMP (FUNCTION CURRENT-DATE) for every
        // transaction and moves the one value to TRAN-ORIG-TS and TRAN-PROC-TS.
        Iterator<String> clock = List.of("2022-07-05-10.00.00.000000", "2022-07-05-10.00.02.000000").iterator();
        InterestJob.Result r = InterestJob.run(Map.of(
                InterestJob.ACCTDATA_PS, List.of(acct(2001, "100.00", "GOLD", "0", "0")),
                InterestJob.CARDXREF_PS, List.of(xref(2001)),
                InterestJob.TCATBALF_PS, List.of(tcat(2001, "01", 1, "1000.00"), tcat(2001, "01", 2, "100.00")),
                InterestJob.DISCGRP_PS, DISCGRP),
            "2022071800", clock::next);
        assertEquals("2022-07-05-10.00.00.0000002022-07-05-10.00.00.000000", r.transactions().get(0).substring(278));
        assertEquals("2022-07-05-10.00.02.0000002022-07-05-10.00.02.000000", r.transactions().get(1).substring(278));
        assertFalse(clock.hasNext());
    }

    @Test
    void loadSkipsDuplicateAndOutOfSequenceKeys() {
        // c08: the sequential KSDS load keeps 3701, 3703 and drops the duplicate 3701 and the late 3702
        IndexedFile f = IndexedFile.load(List.of("3701a", "3701b", "3703a", "3702a"), 0, 4);
        assertEquals(List.of("3701a", "3703a"), List.copyOf(f.inKeyOrder()));
        assertEquals(2, f.skipped());
    }

    @Test
    void accountWithTwoCardsUsesFirstCardInKeyOrder() {
        // c10
        IndexedFile xref = IndexedFile.load(List.of(xref(card(3801, 1), 3801), xref(card(3801, 9), 3801)), 0, 16)
            .withAlternateIndex(25, 11);
        assertEquals(card(3801, 1), xref.readAlternate("00000003801").substring(0, 16));
    }

    private static InterestJob.Result run(List<String> accounts, List<String> xrefs, List<String> tcats) {
        return InterestJob.run(Map.of(InterestJob.ACCTDATA_PS, accounts, InterestJob.CARDXREF_PS, xrefs,
            InterestJob.TCATBALF_PS, tcats, InterestJob.DISCGRP_PS, DISCGRP), "2022071800", CLOCK);
    }

    private static String money(String value, int digits) {
        return Zoned.encode(new BigDecimal(value), digits, 2, true);
    }

    static String acct(long id, String balance, String group, String cycCredit, String cycDebit) {
        return Text.pad(String.format("%011d", id) + "Y" + money(balance, 12) + money("5000.00", 12)
            + money("2500.00", 12) + "2019-03-152026-12-312026-12-31" + money(cycCredit, 12) + money(cycDebit, 12)
            + Text.pad("10115", 10) + Text.pad(group, 10), Cbact04c.ACCT_LRECL);
    }

    static String card(long acct) {
        return card(acct, 0);
    }

    static String card(long acct, int n) {
        return String.format("4000%04d%08d", n, acct);
    }

    static String xref(long acct) {
        return xref(card(acct), acct);
    }

    static String xref(String card, long acct) {
        return Text.pad(card + String.format("%09d%011d", acct, acct), Cbact04c.XREF_LRECL);
    }

    static String tcat(long acct, String type, int cat, String balance) {
        return Text.pad(String.format("%011d", acct) + type + String.format("%04d", cat) + money(balance, 11),
            Cbact04c.TCATBAL_LRECL);
    }

    static String disc(String group, String type, int cat, String rate) {
        return Text.pad(Text.pad(group, 10) + type + String.format("%04d", cat) + money(rate, 6), Cbact04c.DISCGRP_LRECL);
    }
}
