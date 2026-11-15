package com.carddemo.interest.domain;

import java.math.BigDecimal;

/**
 * INT-H05 (standards S3.4): whether a value fits the PIC of the field it is stored in. The legacy
 * has no ON SIZE ERROR and would lose leading digits (CBACT04C:352, 464-467, 474); the target
 * checks before the store and never cuts.
 */
final class FieldRange {

    /** S9(09)V99: TRAN-AMT, WS-MONTHLY-INT, WS-TOTAL-INT (CVTRA05Y/TRAN-AMT; CBACT04C:168-169). */
    static final int INTEREST_INTEGER_DIGITS = 9;
    /** S9(10)V99: ACCT-CURR-BAL (CVACT01Y/ACCT-CURR-BAL). */
    static final int ACCOUNT_BALANCE_INTEGER_DIGITS = 10;
    /** 9(06): the counter of TRAN-ID (CBACT04C:173). */
    static final int HIGHEST_TRANSACTION_COUNTER = 999_999;

    private FieldRange() {
    }

    /** True when the amount, which has two decimals, has no more integer digits than the field. */
    static boolean fits(BigDecimal amount, int integerDigits) {
        return amount.abs().compareTo(BigDecimal.TEN.pow(integerDigits)) < 0;
    }
}
