package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/** Encoding rules E1-E3 of the data map, fixture code page. Expected bytes are written by hand. */
class FieldTest {

    private static final Field AMOUNT = new Field("TRAN-CAT-BAL", 0, 11);
    private static final Field DIGITS = new Field("TRANCAT-CD", 0, 4);
    private static final Field TEXT = new Field("TRAN-SOURCE", 0, 10);

    @Test
    void e2_overpunchRoundTrip() {
        // { A-I are +0 to +9, } J-R are -0 to -9 in the last byte
        assertEquals(new BigDecimal("1000.00"), AMOUNT.signed("0000010000{", 2));
        assertEquals(new BigDecimal("1000.33"), AMOUNT.signed("0000010003C", 2));
        assertEquals(new BigDecimal("-250.07"), AMOUNT.signed("0000002500P", 2));
        assertEquals("0000010000{", AMOUNT.encodeSigned(new BigDecimal("1000.00"), 2));
        assertEquals("0000010003C", AMOUNT.encodeSigned(new BigDecimal("1000.33"), 2));
        assertEquals("0000002500P", AMOUNT.encodeSigned(new BigDecimal("-250.07"), 2));
        assertEquals("0000000001}", AMOUNT.encodeSigned(new BigDecimal("-0.10"), 2));
    }

    @Test
    void e2_unsignedLastDigitIsPositiveAndComesBackCSigned() {
        // G02: an F-signed source digit is accepted as positive and written C-signed
        assertEquals(new BigDecimal("0.15"), AMOUNT.signed("00000000015", 2));
        assertEquals("0000000001E", AMOUNT.encodeSigned(new BigDecimal("0.15"), 2));
    }

    @Test
    void e2_minusZeroComesBackPlusZero() {
        // G03
        assertEquals(0, AMOUNT.signed("0000000000}", 2).signum());
        assertEquals("0000000000{", AMOUNT.encodeSigned(AMOUNT.signed("0000000000}", 2), 2));
    }

    @Test
    void e2_valueThatDoesNotFitIsRefusedNotCut() {
        // S9(09)V99 holds at most 999999999.99
        assertRejected(() -> AMOUNT.encodeSigned(new BigDecimal("1000000000.00"), 2));
    }

    @Test
    void e1_nonDigitRejectsRecord() {
        assertRejected(() -> DIGITS.unsigned("00A1"));
        assertRejected(() -> DIGITS.unsigned("  01"));
        assertRejected(() -> AMOUNT.signed("00000A0000{", 2));
        assertRejected(() -> AMOUNT.signed("0000010000 ", 2));
    }

    @Test
    void e1_zeroPaddedToTheFieldWidth() {
        assertEquals(5L, DIGITS.unsigned("0005"));
        assertEquals("0005", DIGITS.encodeUnsigned(5));
        assertRejected(() -> DIGITS.encodeUnsigned(10000));
    }

    @Test
    void e3_textIsPaddedAndNeverCut() {
        assertEquals("System", TEXT.text("System    "));
        assertEquals("System    ", TEXT.encodeText("System"));
        assertRejected(() -> TEXT.encodeText("System text"));
        assertRejected(() -> TEXT.text("Sys\u0000em    "));
    }

    private static void assertRejected(Runnable action) {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class, action::run);
        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
    }
}
