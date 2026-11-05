package com.carddemo.batch.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ZonedTest {
    @Test
    void decodesEbcdicOverpunchSigns() {
        assertEquals(new BigDecimal("1000.00"), Zoned.decode("00000010000{", 2));
        assertEquals(new BigDecimal("-1235.50"), Zoned.decode("00000012355}", 2));
        assertEquals(new BigDecimal("-0.33"), Zoned.decode("00000000003L", 2));
        assertEquals(new BigDecimal("9999.99"), Zoned.decode("99999I", 2));
        assertEquals(new BigDecimal("3001"), Zoned.decode("00000003001", 0));
    }

    @Test
    void encodesOverpunchAndUnsigned() {
        assertEquals("00000012478N", Zoned.encode(new BigDecimal("-1247.85"), 12, 2, true));
        assertEquals("00000010137E", Zoned.encode(new BigDecimal("1013.75"), 12, 2, true));
        assertEquals("000000000", Zoned.encode(BigDecimal.ZERO, 9, 0, false));
    }

    @Test
    void negativeZeroIsStoredAsPositiveZero() {
        // c02: -0.33 * 12.00 / 1200 = -0.0033 -> TRAN-AMT 0000000000{ under GnuCOBOL
        assertEquals("0000000000{", Zoned.encode(new BigDecimal("-0.0033"), 11, 2, true));
    }

    @Test
    void dropsHighOrderDigitsLikeAMoveWithoutSizeError() {
        // c04: ACCT-CURR-BAL 9999999999.99 + 666649999.82 -> 0666649999.81
        assertEquals("06666499998A", Zoned.encode(new BigDecimal("10666649999.81"), 12, 2, true));
    }
}
