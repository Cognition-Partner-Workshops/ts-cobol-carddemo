package com.carddemo.modern.interest;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** CBACT04C 1300-COMPUTE-INTEREST. */
public final class InterestCalculator {

    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);

    private InterestCalculator() {
    }

    /** {@code COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200}. */
    public static BigDecimal monthlyInterest(BigDecimal balance, BigDecimal annualRatePercent) {
        return balance.multiply(annualRatePercent).divide(TWELVE_HUNDRED, 2, RoundingMode.DOWN);
    }
}
