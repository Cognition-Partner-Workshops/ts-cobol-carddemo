package com.cardemo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO for payment processing.
 * Migrated from COBIL00C (CB00 transaction) bill payment.
 */
public class PaymentRequest {

    @NotNull(message = "Account ID is required")
    private Long acctId;

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    private BigDecimal amount;

    private String cardNum;
    private String description;

    public PaymentRequest() {
    }

    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
