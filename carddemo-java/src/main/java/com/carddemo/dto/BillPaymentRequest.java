package com.carddemo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Bill Payment request DTO - replaces COBIL00C bill payment screen input.
 */
public class BillPaymentRequest {

    @NotNull
    private Long acctId;

    @NotNull
    @Positive
    private BigDecimal paymentAmount;

    public BillPaymentRequest() {}

    public BillPaymentRequest(Long acctId, BigDecimal paymentAmount) {
        this.acctId = acctId;
        this.paymentAmount = paymentAmount;
    }

    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
}
