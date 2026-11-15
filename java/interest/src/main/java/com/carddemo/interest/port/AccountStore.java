package com.carddemo.interest.port;

import java.math.BigDecimal;
import java.util.Optional;

import com.carddemo.interest.domain.Account;

/** FI-3. Accounts by account id (CBACT04C:41-45, 372-391) and the update of the account break (350-356). */
public interface AccountStore {

    Optional<Account> find(long accountId);

    /** INT-R07: sets the current balance and zeroes cycle credit and cycle debit; nothing else changes. */
    void postInterest(long accountId, BigDecimal newBalance);
}
