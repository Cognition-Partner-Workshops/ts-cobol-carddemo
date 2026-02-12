package com.aws.carddemo.card.dto;

import com.aws.carddemo.card.CardXref;

public record CardXrefResponse(
        String cardNumber,
        Long accountId,
        String accountStatus,
        Long customerId,
        String customerFirstName,
        String customerLastName
) {
    public static CardXrefResponse from(CardXref xref) {
        return new CardXrefResponse(
                xref.getCardNumber(),
                xref.getAccount().getId(),
                xref.getAccount().getAccountStatus(),
                xref.getCustomer().getId(),
                xref.getCustomer().getFirstName(),
                xref.getCustomer().getLastName()
        );
    }
}
