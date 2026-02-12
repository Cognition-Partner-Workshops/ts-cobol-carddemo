package com.aws.carddemo.card.dto;

import java.time.LocalDate;

import com.aws.carddemo.card.Card;

public record CardDetailResponse(
        Long id,
        String cardNumber,
        Long accountId,
        String cardStatus,
        String embossedName,
        String maskedCvv,
        LocalDate issuedDate,
        LocalDate expiryDate,
        AccountSummary accountSummary
) {
    public static CardDetailResponse from(Card card) {
        return new CardDetailResponse(
                card.getId(),
                card.getCardNumber(),
                card.getAccount().getId(),
                card.getCardStatus(),
                card.getEmbossedName(),
                maskCvv(card.getCvvCode()),
                card.getIssuedDate(),
                card.getExpiryDate(),
                AccountSummary.from(card.getAccount())
        );
    }

    private static String maskCvv(String cvv) {
        if (cvv == null) {
            return null;
        }
        return "*".repeat(cvv.length());
    }
}
