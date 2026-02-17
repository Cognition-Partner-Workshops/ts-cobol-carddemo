package com.aws.carddemo.card.dto;

import java.time.LocalDate;

import com.aws.carddemo.card.Card;

public record CardListItemResponse(
        Long id,
        String maskedCardNumber,
        String cardStatus,
        String embossedName,
        LocalDate expiryDate
) {
    public static CardListItemResponse from(Card card) {
        return new CardListItemResponse(
                card.getId(),
                maskCardNumber(card.getCardNumber()),
                card.getCardStatus(),
                card.getEmbossedName(),
                card.getExpiryDate()
        );
    }

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        int visibleDigits = 4;
        int maskedLength = cardNumber.length() - visibleDigits;
        return "*".repeat(maskedLength) + cardNumber.substring(maskedLength);
    }
}
