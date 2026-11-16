package com.carddemo.posting.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ZonedTest {

    @Test
    void decodesEbcdicOverpunch() {
        assertEquals(new BigDecimal("125.00"), Zoned.decode("0000001250{", 2));
        assertEquals(new BigDecimal("-15.75"), Zoned.decode("0000000157N", 2));
        assertEquals(new BigDecimal("1001"), Zoned.decode("00000001001", 0));
    }

    @Test
    void encodesWithTrailingOverpunchAndLeftTruncation() {
        assertEquals("0000001250{", Zoned.encode(new BigDecimal("125.00"), 9, 2, true));
        assertEquals("0000000157N", Zoned.encode(new BigDecimal("-15.75"), 9, 2, true));
        assertEquals("00000000000{", Zoned.encode(new BigDecimal("-0.00"), 10, 2, true));
        assertEquals(new BigDecimal("-1.00"), Zoned.fit(new BigDecimal("-1000000001.00"), 9, 2));
    }

    @Test
    void displaysWithTrailingSign() {
        assertEquals("00000001575-", Zoned.display(new BigDecimal("-15.75"), 9, 2));
        assertEquals("00000002000+", Zoned.display(new BigDecimal("20.00"), 9, 2));
    }

    @Test
    void editsLikeZeroSuppressedPictures() {
        assertEquals("         125.00", Zoned.edit(new BigDecimal("125.00"), false));
        assertEquals("-         15.75", Zoned.edit(new BigDecimal("-15.75"), false));
        assertEquals("+  1,234,567.05", Zoned.edit(new BigDecimal("1234567.05"), true));
        assertEquals("            .05", Zoned.edit(new BigDecimal("0.05"), false));
        assertEquals("               ", Zoned.edit(BigDecimal.ZERO, true));
    }
}
