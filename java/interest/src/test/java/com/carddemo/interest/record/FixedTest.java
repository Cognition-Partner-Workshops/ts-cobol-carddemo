package com.carddemo.interest.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.batch.InterestCalculator;

class FixedTest {

    @Test
    void overpunchRoundTrips() {
        assertEquals("0000000030}", Fixed.zoned(new BigDecimal("-3.00"), 11, 2));
        assertEquals("0000000123E", Fixed.zoned(new BigDecimal("12.355"), 11, 2));
        assertEquals(new BigDecimal("-3.00"), Fixed.zoned("0000000030}", 0, 11, 2));
        assertEquals(new BigDecimal("1010.00"), Fixed.zoned("00000010100{", 0, 12, 2));
    }

    @Test
    void interestTruncatesLikeComputeWithoutRounded() {
        assertEquals(new BigDecimal("12.35"),
                InterestCalculator.monthlyInterest(new BigDecimal("741.30"), new BigDecimal("20.00")));
        assertEquals(new BigDecimal("-3.00"),
                InterestCalculator.monthlyInterest(new BigDecimal("-200.00"), new BigDecimal("18.00")));
    }

    @Test
    void storeDropsHighOrderDigits() {
        assertEquals(new BigDecimal("2.00"), Fixed.fit(new BigDecimal("1000000002.009"), 9, 2));
    }
}
