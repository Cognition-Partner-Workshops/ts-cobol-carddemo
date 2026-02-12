package com.aws.carddemo.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.aws.carddemo.account.Account;

public record AccountResponse(
        Long id,
        String accountStatus,
        BigDecimal creditLimit,
        BigDecimal currentBalance,
        BigDecimal cashCreditLimit,
        LocalDate openDate,
        LocalDate expirationDate,
        LocalDate reissueDate,
        String groupId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        CustomerSummary customer,
        int cardCount
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountStatus(),
                account.getCreditLimit(),
                account.getCurrentBalance(),
                account.getCashCreditLimit(),
                account.getOpenDate(),
                account.getExpirationDate(),
                account.getReissueDate(),
                account.getGroupId(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                CustomerSummary.from(account.getCustomer()),
                account.getCards() != null ? account.getCards().size() : 0
        );
    }
}
