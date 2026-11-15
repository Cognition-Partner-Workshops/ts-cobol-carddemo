package com.carddemo.interest.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * What the calculation reads of an account: current balance (12,2), disclosure group and, where the
 * adapter supplies it, whether the active flag is Y. The flag is read only by the SKIP option of
 * INT-H04; as coded it is never looked at.
 */
public record Account(long accountId, BigDecimal currentBalance, String groupId, Optional<Boolean> active) {

    /** An account whose active flag the adapter does not supply. */
    public Account(long accountId, BigDecimal currentBalance, String groupId) {
        this(accountId, currentBalance, groupId, Optional.empty());
    }
}
