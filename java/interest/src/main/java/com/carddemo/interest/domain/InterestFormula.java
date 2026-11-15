package com.carddemo.interest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * INT-R05: one month of interest is balance x yearly rate in percent / 1200 (CBACT04C:464-465).
 * The product is exact; the one division goes straight to two decimals with the mode of INT-Q01.
 */
public final class InterestFormula {

    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");

    private InterestFormula() {
    }

    public static BigDecimal monthlyInterest(BigDecimal balance, BigDecimal yearlyRate, RoundingMode intQ01Mode) {
        return balance.multiply(yearlyRate).divide(TWELVE_HUNDRED, 2, intQ01Mode);
    }
}
