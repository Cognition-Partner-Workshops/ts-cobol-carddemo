package com.carddemo.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * JPA entity for the card_cross_reference table.
 * Replaces CCXREF VSAM KSDS + CXACAIX Alternate Index (CVACT03Y.cpy, 50 bytes).
 * Enables bidirectional Account ID <-> Card Number resolution (BR-AT-04, BR-AT-05).
 */
@Entity
@Table(name = "card_cross_reference")
public class CardCrossReference {

    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    @Column(name = "customer_id", precision = 9, scale = 0, nullable = false)
    private BigDecimal customerId;

    @Column(name = "account_id", precision = 11, scale = 0, nullable = false)
    private BigDecimal accountId;

    public CardCrossReference() {
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public BigDecimal getCustomerId() {
        return customerId;
    }

    public void setCustomerId(BigDecimal customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(BigDecimal accountId) {
        this.accountId = accountId;
    }
}
