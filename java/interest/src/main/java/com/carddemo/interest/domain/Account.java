package com.carddemo.interest.domain;

import java.math.BigDecimal;

/**
 * What the calculation reads of an account: current balance (12,2), disclosure group and the
 * active flag as it stands on the record (INT-H04).
 */
public record Account(long accountId, BigDecimal currentBalance, String groupId, String activeStatus) {

    /** INT-H04 option B speaks of "accounts whose active flag is not Y" (CVACT01Y/ACCT-ACTIVE-STATUS). */
    private static final String ACTIVE = "Y";

    public boolean isActive() {
        return ACTIVE.equals(activeStatus);
    }
}
