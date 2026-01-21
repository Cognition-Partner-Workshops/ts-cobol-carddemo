package com.carddemo.dto.response;

import com.carddemo.entity.Card;

import java.time.LocalDate;

/**
 * Response DTO for Card entity.
 * Used for returning card data in API responses.
 * Card number is masked for security.
 */
public class CardResponse {

    private String cardNumber;
    private String maskedCardNumber;
    private Long accountId;
    private Long customerId;
    private String cardholderName;
    private LocalDate expirationDate;
    private String activeStatus;

    public CardResponse() {
    }

    public static CardResponse fromEntity(Card card) {
        CardResponse response = new CardResponse();
        response.setCardNumber(card.getCardNumber());
        response.setMaskedCardNumber(maskCardNumber(card.getCardNumber()));
        response.setAccountId(card.getAccountId());
        response.setCustomerId(card.getCustomerId());
        response.setCardholderName(card.getCardholderName());
        response.setExpirationDate(card.getExpirationDate());
        response.setActiveStatus(card.getActiveStatus());
        return response;
    }

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }
}
