package com.carddemo.modern.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** CBACT04C: COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200, no ROUNDED. */
class InterestCalculatorTest {

    private static BigDecimal monthly(String balance, String rate) {
        return CobolNumeric.store(InterestCalculator.monthlyInterest(new BigDecimal(balance), new BigDecimal(rate)), 9, 2);
    }

    @Test
    void halfCentTieIsTruncatedNotRoundedToEven() {
        // i02-tie-half-cent: 12.355 -> COBOL 12.35, HALF_EVEN would give 12.36
        assertEquals(new BigDecimal("12.35"), monthly("1235.50", "12.00"));
    }

    @Test
    void negativeBalanceTruncatesTowardZero() {
        assertEquals(new BigDecimal("-12.35"), monthly("-1235.50", "12.00"));
    }

    @Test
    void storeDropsHighOrderDigitsWithoutSizeError() {
        assertEquals(new BigDecimal("23.45"), CobolNumeric.store(new BigDecimal("1000000023.45"), 9, 2));
    }
}
