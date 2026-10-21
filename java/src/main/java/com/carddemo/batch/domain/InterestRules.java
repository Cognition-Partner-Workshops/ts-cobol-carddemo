package com.carddemo.batch.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Interest rules of CBACT04C (INTCALC). */
public final class InterestRules {

    public static final String DEFAULT_GROUP = "DEFAULT";
    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);

    private InterestRules() {
    }

    /** (balance * annual rate) / 1200, cut to the cent. COBOL COMPUTE without ROUNDED truncates. */
    public static BigDecimal monthlyInterest(BigDecimal balance, BigDecimal annualRatePercent, RoundingMode mode) {
        return balance.multiply(annualRatePercent).divide(MONTHS_TIMES_PERCENT, 2, mode);
    }

    public static String interestTransactionId(String parmDate, int suffix) {
        return parmDate + String.format("%06d", suffix);
    }

    public static String interestDescription(long accountId) {
        return "Int. for a/c " + String.format("%011d", accountId);
    }

    /** Interest is added to the balance and the cycle totals restart at zero. */
    public static Account applyToAccount(Account account, BigDecimal totalInterest) {
        return account.withBalances(account.currentBalance().add(totalInterest), BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2));
    }
}
