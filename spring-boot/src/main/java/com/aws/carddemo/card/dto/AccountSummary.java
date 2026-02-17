package com.aws.carddemo.card.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.aws.carddemo.account.Account;

public record AccountSummary(
        Long accountId,
        String accountStatus,
        BigDecimal creditLimit,
        BigDecimal currentBalance,
        LocalDate openDate,
        LocalDate expirationDate
) {
    public static AccountSummary from(Account account) {
        return new AccountSummary(
                account.getId(),
                account.getAccountStatus(),
                account.getCreditLimit(),
                account.getCurrentBalance(),
                account.getOpenDate(),
                account.getExpirationDate()
        );
    }
}
