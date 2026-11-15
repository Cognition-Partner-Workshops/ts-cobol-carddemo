package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * The four errors of ticket 03 on the layouts of the estate: each stops the run with
 * RECORD_REJECTED and a message naming file, record number and field. Records are synthetic.
 */
class RecordCodecErrorTest {

    private static final String BALANCES = "AWS.M2.CARDDEMO.TCATBALF.PS.txt";
    /** 00000002001 / 01 / 0001 / 1000.00, 22 spaces of FILLER. */
    private static final String BALANCE = "000000020010100010000010000{" + " ".repeat(22);

    private static String messageOf(Runnable action) {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class, action::run);
        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        return stop.getMessage();
    }

    @Test
    void e1_nonDigitInAPic9Field() {
        String image = BALANCE.substring(0, 13) + "00X1" + BALANCE.substring(17);

        assertEquals("RECORD_REJECTED (INT-R08): file " + BALANCES + ", record 4, field TRANCAT-CD: "
                + "not numeric: byte X'58' at position 3 of the field (E1)",
                messageOf(() -> CategoryBalanceRecord.decode(image, new RecordSource(BALANCES, 4))));
    }

    @Test
    void e2_nonDigitInASignedField() {
        String image = BALANCE.substring(0, 17) + "00000100 0{" + BALANCE.substring(28);

        assertEquals("RECORD_REJECTED (INT-R08): file " + BALANCES + ", record 2, field TRAN-CAT-BAL: "
                + "not numeric: byte X'20' at position 9 of the field (E2)",
                messageOf(() -> CategoryBalanceRecord.decode(image, new RecordSource(BALANCES, 2))));
    }

    @Test
    void g07_byteOutsideTheCodePageInAPicXField() {
        String image = BALANCE.substring(0, 11) + "0\u0000" + BALANCE.substring(13);

        assertEquals("RECORD_REJECTED (INT-R08): file " + BALANCES + ", record 12, field TRANCAT-TYPE-CD: "
                + "a byte is not a character of the code page fixture (ISO-8859-1) (E3, G07)",
                messageOf(() -> CategoryBalanceRecord.decode(image, new RecordSource(BALANCES, 12))));
    }

    @Test
    void recordOfTheWrongLength_bytes() {
        byte[] short49 = Bytes.fixture(BALANCE.substring(0, 49));
        byte[] long51 = Bytes.fixture(BALANCE + " ");

        assertEquals("RECORD_REJECTED (INT-R08): file " + BALANCES + ", record 5, field (record): record is 49 bytes, CVTRA01Y is 50",
                messageOf(() -> CardDemoLayouts.CATEGORY_BALANCE.decode(short49, CodePage.FIXTURE, new RecordSource(BALANCES, 5))));
        assertEquals("RECORD_REJECTED (INT-R08): file " + BALANCES + ", record 6, field (record): record is 51 bytes, CVTRA01Y is 50",
                messageOf(() -> CardDemoLayouts.CATEGORY_BALANCE.decode(long51, CodePage.FIXTURE, new RecordSource(BALANCES, 6))));
    }

    @Test
    void recordOfTheWrongLength_fixtureLineLongerThanTheRecord() {
        // a fixture line has lost its trailing spaces, so only a longer line shows a wrong length
        String line = BALANCE + "X";

        assertEquals("RECORD_REJECTED (INT-R08): file " + BALANCES + ", record 3, field (record): record is 51 bytes, CVTRA01Y is 50",
                messageOf(() -> RecordImage.fromLine(line, CardDemoLayouts.CATEGORY_BALANCE, new RecordSource(BALANCES, 3))));
        assertEquals(BALANCE, RecordImage.fromLine("000000020010100010000010000{", CardDemoLayouts.CATEGORY_BALANCE,
                new RecordSource(BALANCES, 3)));
    }

    @Test
    void recordOfTheWrongLength_hostFileThatDoesNotEndOnARecord() {
        // 2 records of 50 bytes and 20 bytes more: the third record is short
        byte[] content = new byte[120];

        assertEquals("RECORD_REJECTED (INT-R08): file TCATBALF.PS, record 3, field (record): record is 20 bytes, CVTRA01Y is 50",
                messageOf(() -> RecordImage.hostRecords(content, CardDemoLayouts.CATEGORY_BALANCE, "TCATBALF.PS")));
        assertEquals(2, RecordImage.hostRecords(new byte[100], CardDemoLayouts.CATEGORY_BALANCE, "TCATBALF.PS").length);
    }

    @Test
    void e5_fieldWithUsageOtherThanDisplay() {
        // CVTRA01Y with the balance packed, as a host file could be redefined: refused, not guessed
        RecordLayout packed = Copybook.parse("CVTRA01P", """
                       01  TRAN-CAT-BAL-RECORD.
                           05  TRAN-CAT-KEY.
                              10 TRANCAT-ACCT-ID         PIC 9(11).
                              10 TRANCAT-TYPE-CD         PIC X(02).
                              10 TRANCAT-CD              PIC 9(04).
                           05  TRAN-CAT-BAL              PIC S9(09)V99 COMP-3.
                           05  FILLER                    PIC X(27).
                """);
        byte[] record = Bytes.fixture(BALANCE);
        RecordSource source = new RecordSource(BALANCES, 9);
        String expected = "RECORD_REJECTED (INT-R08): file " + BALANCES + ", record 9, field TRAN-CAT-BAL: USAGE COMP-3 is not DISPLAY (E5)";

        assertEquals(expected, messageOf(() -> packed.decode(record, CodePage.FIXTURE, source)));
        assertEquals(expected, messageOf(() -> packed.read(record, "TRANCAT-ACCT-ID", CodePage.FIXTURE, source)));
        assertEquals(expected, messageOf(() -> packed.replace(record, "TRANCAT-CD", BigDecimal.ONE, CodePage.FIXTURE, source)));
    }

    @Test
    void recordBuiltByTheRunIsNamedAsNew() {
        RecordLayout layout = CardDemoLayouts.TRANSACTION;

        assertEquals("RECORD_REJECTED (INT-R08): file SYSTRAN, new record, field TRAN-ID: value of 17 characters is longer than the field",
                messageOf(() -> layout.encode(layout.blank(CodePage.FIXTURE).with("TRAN-ID", "20220718001000000"),
                        CodePage.FIXTURE, RecordSource.built("SYSTRAN"))));
    }
}
