package com.carddemo.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity for the card table.
 * Replaces CARDDAT VSAM KSDS (CVACT02Y.cpy, 150 bytes).
 */
@Entity
@Table(name = "card")
public class Card {

    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    @Column(name = "account_id", precision = 11, scale = 0, nullable = false)
    private BigDecimal accountId;

    @Column(name = "cvv_code", precision = 3, scale = 0, nullable = false)
    private BigDecimal cvvCode;

    @Column(name = "embossed_name", length = 50, nullable = false)
    private String embossedName;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "active_status", length = 1, nullable = false)
    private String activeStatus;

    public Card() {
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(BigDecimal accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getCvvCode() {
        return cvvCode;
    }

    public void setCvvCode(BigDecimal cvvCode) {
        this.cvvCode = cvvCode;
    }

    public String getEmbossedName() {
        return embossedName;
    }

    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
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
