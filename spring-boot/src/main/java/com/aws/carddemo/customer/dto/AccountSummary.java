package com.aws.carddemo.customer.dto;

import java.math.BigDecimal;

import com.aws.carddemo.account.Account;

public record AccountSummary(
        Long id,
        String accountStatus,
        BigDecimal creditLimit,
        BigDecimal currentBalance
) {
    public static AccountSummary from(Account a) {
        return new AccountSummary(
                a.getId(),
                a.getAccountStatus(),
                a.getCreditLimit(),
                a.getCurrentBalance()
        );
    }
}
