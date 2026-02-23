package com.carddemo.core.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when a payment or transaction exceeds available balance.
 * Replaces COBOL balance validation logic in COBIL00C and COTRN02C.
 */
public class InsufficientFundsException extends BusinessValidationException {

    private final BigDecimal requestedAmount;
    private final BigDecimal availableBalance;

    public InsufficientFundsException(BigDecimal requestedAmount, BigDecimal availableBalance) {
        super("INSUFFICIENT_FUNDS",
                String.format("Insufficient funds: requested %.2f, available %.2f",
                        requestedAmount, availableBalance));
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
}
