package com.carddemo.interest.export;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * What an export wrote, for interest_run: the SYSTRAN generation (strategy item RC5) and the
 * ACCTDATA image (RC6: the count and the five sums of RC2, in the order of the account layout).
 */
public record ExportTotals(long transactionCount, BigDecimal transactionAmountSum, long accountCount,
        List<BigDecimal> accountAmountSums) {

    static final int MONEY_SCALE = 2;

    static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }
}
