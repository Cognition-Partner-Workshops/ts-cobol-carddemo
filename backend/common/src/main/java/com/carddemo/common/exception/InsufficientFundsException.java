package com.carddemo.common.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    
    private final BigDecimal requestedAmount;
    private final BigDecimal availableAmount;

    public InsufficientFundsException(String message) {
        super(message);
        this.requestedAmount = null;
        this.availableAmount = null;
    }

    public InsufficientFundsException(BigDecimal requestedAmount, BigDecimal availableAmount) {
        super(String.format("Insufficient funds. Requested: %s, Available: %s", requestedAmount, availableAmount));
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }
}
