package com.carddemo.interest.domain;

import java.util.List;

/**
 * INT-H01: the legacy announces fees and computes none (CBACT04C:215, 518-520). This is the one
 * place a fee rule from the business would go; until the decision is taken it charges nothing.
 */
final class FeeHook {

    private FeeHook() {
    }

    /** The fee transactions that follow the interest of a balance: none, as coded. */
    static List<InterestTransaction> intH01FeesAfterInterestOf(CategoryBalance balance) {
        return List.of();
    }
}
