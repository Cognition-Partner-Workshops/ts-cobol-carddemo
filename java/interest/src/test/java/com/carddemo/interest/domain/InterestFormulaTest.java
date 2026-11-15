package com.carddemo.interest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;

/** INT-R05 and INT-Q01 (CBACT04C:464-465). Expected values are worked by hand, never by the formula class. */
class InterestFormulaTest {

    private static InterestPolicy rounding(Rounding rounding) {
        return new InterestPolicy(rounding, IdPrefix.PARM, TimestampSource.RUN_CLOCK, false);
    }

    private static BigDecimal interest(String balance, String rate, Rounding rounding) {
        return InterestFormula.monthlyInterest(new BigDecimal(balance), new BigDecimal(rate),
                rounding(rounding).intQ01InterestRoundingMode());
    }

    @Test
    void intR05_formulaExactProductThenDivide() {
        // i01-baseline: 1000.00 * 12.00 / 1200 = 10.00 and 2400.00 * 18.50 / 1200 = 37.00, both exact
        assertEquals(new BigDecimal("10.00"), interest("1000.00", "12.00", Rounding.TRUNCATE));
        assertEquals(new BigDecimal("37.00"), interest("2400.00", "18.50", Rounding.TRUNCATE));
        // i07-nontie-rounding: 1000.33 * 18.50 = 18506.1050, / 1200 = 15.42175...; 777.77 * 21.00 / 1200 = 13.610975
        assertEquals(new BigDecimal("15.42"), interest("1000.33", "18.50", Rounding.TRUNCATE));
        assertEquals(new BigDecimal("13.61"), interest("777.77", "21.00", Rounding.TRUNCATE));
    }

    @Test
    void intR05_productIsNotRoundedBeforeTheDivision() {
        // 0.99 * 0.99 = 0.9801; * 1200 / 1200 would survive an early cut to 0.98, the real quotient does not:
        // 1234.56 * 0.99 = 1222.2144, / 1200 = 1.018512 -> 1.01
        assertEquals(new BigDecimal("1.01"), interest("1234.56", "0.99", Rounding.TRUNCATE));
        // 999999999.99 * 9999.99 / 1200 = 8333324999.916658... : the 17-digit product keeps every digit
        assertEquals(new BigDecimal("8333324999.91"), interest("999999999.99", "9999.99", Rounding.TRUNCATE));
    }

    @Test
    void intQ01_truncatesHalfCent() {
        // i02-tie-half-cent: 1235.50 * 12.00 / 1200 = 12.355 -> 12.35; 1234.50 * 12.00 / 1200 = 12.345 -> 12.34
        assertEquals(new BigDecimal("12.35"), interest("1235.50", "12.00", Rounding.TRUNCATE));
        assertEquals(new BigDecimal("12.34"), interest("1234.50", "12.00", Rounding.TRUNCATE));
    }

    @Test
    void intQ01_truncatesTowardsZeroForNegativeAmounts() {
        // -1235.50 * 12.00 / 1200 = -12.355 -> -12.35 (towards zero, not down to -12.36)
        assertEquals(new BigDecimal("-12.35"), interest("-1235.50", "12.00", Rounding.TRUNCATE));
    }

    @Test
    void intQ01_nonTieUnchanged() {
        // i07-nontie-rounding: 15.42175 and 13.610975 give 15.42 and 13.61 under every mode
        for (Rounding rounding : Rounding.values()) {
            assertEquals(new BigDecimal("15.42"), interest("1000.33", "18.50", rounding));
            assertEquals(new BigDecimal("13.61"), interest("777.77", "21.00", rounding));
        }
    }

    @Test
    void intQ01_halfUpWhenSwitched() {
        // 12.355 -> 12.36 and 12.345 -> 12.35
        assertEquals(new BigDecimal("12.36"), interest("1235.50", "12.00", Rounding.HALF_UP));
        assertEquals(new BigDecimal("12.35"), interest("1234.50", "12.00", Rounding.HALF_UP));
    }

    @Test
    void intQ01_halfEvenWhenSwitched() {
        // 12.355 -> 12.36 (6 is even) and 12.345 -> 12.34 (4 is even)
        assertEquals(new BigDecimal("12.36"), interest("1235.50", "12.00", Rounding.HALF_EVEN));
        assertEquals(new BigDecimal("12.34"), interest("1234.50", "12.00", Rounding.HALF_EVEN));
    }

    @Test
    void intQ01_defaultIsTruncate() {
        assertEquals(Rounding.TRUNCATE, new InterestPolicy(null, null, null, false).rounding());
        assertEquals(Rounding.TRUNCATE, InterestPolicy.legacy().rounding());
    }
}
