package com.carddemo.interest.domain;

import java.util.List;

/**
 * One item of calcInterestStep: one account with all its balances in key order (INT-R01).
 * {@code endsInput} is decided once, by the reader: this is the account after which the input ends
 * (INT-Q04).
 */
public record AccountBalances(long accountId, List<BalanceRead> balances, boolean endsInput) {

    /** A balance and the record image it was read from; the image is echoed to the run log (standards S6.5). */
    public record BalanceRead(CategoryBalance balance, String recordImage) {
    }

    public AccountBalances {
        balances = List.copyOf(balances);
    }
}
