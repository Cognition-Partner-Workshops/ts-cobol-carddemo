package org.carddemo.modern.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecordLayoutTest {

    private static final String DALYTRAN_LINE =
            RecordLayout.pad("0000000000000002010001POS TERM  Card unknown to the estate", 132)
            + "0000000999I100000002"
            + RecordLayout.pad("Nordlicht Buchhandel", 50)
            + RecordLayout.pad("Hamburg", 50)
            + RecordLayout.pad("20095", 10)
            + "4000999999999999"
            + "2022-07-04-09.15.00.000000";

    @Test
    void zonedOverpunchRoundTrips() {
        assertEquals("0000000999I", Zoned.encode(new BigDecimal("99.99"), 11, 2, true));
        assertEquals("0000000999R", Zoned.encode(new BigDecimal("-99.99"), 11, 2, true));
        assertEquals(new BigDecimal("-12.30"), Zoned.decode("0000000123}", 2, true));
        assertEquals(new BigDecimal("0.00"), Zoned.decode("           ", 2, true));
    }

    @Test
    void layoutLengthsMatchTheDatasets() {
        assertEquals(53, Layouts.TRANTYPE_INPUT.length());
        assertEquals(350, Layouts.DALYTRAN.length());
        assertEquals(350, Layouts.TRAN.length());
        assertEquals(430, Layouts.REJTRAN.length());
        assertEquals(300, Layouts.ACCOUNT.length());
        assertEquals(50, Layouts.CARD_XREF.length());
        assertEquals(50, Layouts.TRAN_CAT_BAL.length());
        assertEquals(50, Layouts.DIS_GROUP.length());
        assertEquals(60, Layouts.TRAN_TYPE.length());
        assertEquals(60, Layouts.TRAN_CATEGORY.length());
        assertEquals(53, Layouts.TRAN_TYPE_MAINT.length());
    }

    @Test
    void dailyTransactionDecodesAndEncodesBack() {
        Map<String, Object> values = Layouts.DALYTRAN.decode(DALYTRAN_LINE);
        assertEquals(new BigDecimal("99.99"), values.get("DALYTRAN-AMT"));
        assertEquals("4000999999999999", values.get("DALYTRAN-CARD-NUM"));
        assertEquals(DALYTRAN_LINE, Layouts.DALYTRAN.encodeLine(values));
    }

    @Test
    void rejectLineKeepsTheTransactionAndTheDescription() {
        String line = new RejectRecord(DALYTRAN_LINE, 100, "INVALID CARD NUMBER FOUND").toLine();
        assertEquals(DALYTRAN_LINE, line.substring(0, DALYTRAN_LINE.length()));
        assertTrue(line.endsWith("INVALID CARD NUMBER FOUND"));
    }

    @Test
    void rejectLineCopiesTheTransactionBytesUnchanged() {
        String blankAmount = DALYTRAN_LINE.substring(0, 132) + " ".repeat(11) + DALYTRAN_LINE.substring(143);
        String line = new RejectRecord(blankAmount, 101, "ACCOUNT RECORD NOT FOUND").toLine();
        assertEquals(blankAmount, line.substring(0, blankAmount.length()));
    }

    @Test
    void categoryBalanceDecodesNegativeOverpunchAndEncodesBack() {
        String line = "00000002008010001" + "0000002500}";
        Map<String, Object> values = Layouts.TRAN_CAT_BAL.decode(line);
        assertEquals(new BigDecimal("-250.00"), values.get("TRAN-CAT-BAL"));
        assertEquals(new BigDecimal("1"), values.get("TRANCAT-CD"));
        assertEquals(line, Layouts.TRAN_CAT_BAL.encodeLine(values));
    }

    @Test
    void tranTypeMaintenanceRecordKeepsPaddedText() {
        Map<String, Object> values = Layouts.TRAN_TYPE_MAINT.decode("A04Authorization");
        assertEquals("A", values.get("INPUT-REC-TYPE"));
        assertEquals("04", values.get("INPUT-REC-NUMBER"));
        assertEquals(RecordLayout.pad("Authorization", 50), values.get("INPUT-REC-DESC"));
        assertEquals("A04Authorization", Layouts.TRAN_TYPE_MAINT.encodeLine(values));
    }

    @Test
    void reasonOutsideTheValidationRangeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new RejectRecord(DALYTRAN_LINE, 0, ""));
    }

    @org.junit.jupiter.api.Test
    void fillerIsKeptOnlyWhenAskedFor() {
        String line = "000000020010100010000010000{" + "KEEP ME".concat(" ".repeat(15));
        org.junit.jupiter.api.Assertions.assertFalse(Layouts.TRAN_CAT_BAL.decode(line).containsKey("FILLER@28"));
        Map<String, Object> withFiller = Layouts.TRAN_CAT_BAL.decode(line, true);
        org.junit.jupiter.api.Assertions.assertEquals("KEEP ME" + " ".repeat(15), withFiller.get("FILLER@28"));
        org.junit.jupiter.api.Assertions.assertEquals(line.stripTrailing(), Layouts.TRAN_CAT_BAL.encodeLine(withFiller));
        org.junit.jupiter.api.Assertions.assertEquals("000000020010100010000010000{",
                Layouts.TRAN_CAT_BAL.encodeLine(Layouts.TRAN_CAT_BAL.decode(line)));
    }
}
