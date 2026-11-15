package com.carddemo.interest.load;

/**
 * What strategy item TL4 asks the load to profile, as counts only: accounts with several cards
 * (G08, INT-H03), accounts whose balances mix letter and digit type codes (G06), minus-zero amounts
 * (G03), F-signed amounts (G02), fillers that are not blank, accounts that are not active
 * (INT-H04), balances with neither a rate of their group nor a DEFAULT rate (INT-R08).
 */
public record ExtractProfile(long accountsWithSeveralCards, long accountsWithMixedTypeCodes, long minusZeroAmounts,
        long fSignedAmounts, long nonBlankFillers, long inactiveAccounts, long balancesWithoutRate) {

    public long findings() {
        return accountsWithSeveralCards + accountsWithMixedTypeCodes + minusZeroAmounts + fSignedAmounts
                + nonBlankFillers + inactiveAccounts + balancesWithoutRate;
    }
}
