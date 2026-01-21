package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * JPA entity representing a credit card.
 * Migrated from mainframe data structure based on carddata.txt and CVCRD01Y.cpy
 *
 * <p>This entity maps the VSAM card file structure to a relational database table.
 * Card numbers are stored as strings to preserve leading zeros and formatting.
 *
 * @see com.carddemo.repository.CardRepository
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Size(max = 16)
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @NotNull
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Size(max = 50)
    @Column(name = "cardholder_name", length = 50)
    private String cardholderName;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @NotNull
    @Size(max = 1)
    @Column(name = "active_status", length = 1, nullable = false)
    private String activeStatus;

    public Card() {
    }

    public Card(String cardNumber, Long accountId, Long customerId, String cardholderName) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.customerId = customerId;
        this.cardholderName = cardholderName;
        this.activeStatus = "Y";
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
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

    @Override
    public String toString() {
        return "Card{" +
                "cardNumber='" + cardNumber + '\'' +
                ", accountId=" + accountId +
                ", customerId=" + customerId +
                ", activeStatus='" + activeStatus + '\'' +
                '}';
    }
}
