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
        assertEquals(350, Layouts.DALYTRAN.length());
        assertEquals(350, Layouts.TRAN.length());
        assertEquals(430, Layouts.REJTRAN.length());
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
    void reasonOutsideTheValidationRangeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new RejectRecord(DALYTRAN_LINE, 0, ""));
    }
}
