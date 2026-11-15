package com.carddemo.interest.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The result for one account: its interest transactions and, unless the account ends the input
 * as coded (INT-Q04), the new current balance to post at the account break (INT-R07).
 */
public record AccountInterest(long accountId, List<InterestTransaction> transactions, Optional<BigDecimal> newBalance) {

    public AccountInterest {
        transactions = List.copyOf(transactions);
    }
}
