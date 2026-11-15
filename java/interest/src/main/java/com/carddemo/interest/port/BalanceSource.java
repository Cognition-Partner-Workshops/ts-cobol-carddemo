package com.carddemo.interest.port;

import java.util.Optional;

import com.carddemo.interest.domain.AccountBalances;

/** FI-3. The category balances in key order, one account at a time (INT-R01, CBACT04C:28-32, 325-348). */
public interface BalanceSource {

    /** The next account with all its balances; empty at the end of the input. */
    Optional<AccountBalances> nextAccount();
}
