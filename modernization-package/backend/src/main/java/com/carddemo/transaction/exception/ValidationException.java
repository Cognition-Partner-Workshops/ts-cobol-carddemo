package com.carddemo.transaction.exception;

/**
 * Exception thrown during the 6-phase validation chain in CT02 Add Transaction.
 * Carries phase number, field name, business rule reference, and HTTP status code.
 */
public class ValidationException extends RuntimeException {

    private final int phase;
    private final String field;
    private final String businessRule;
    private final int httpStatus;

    public ValidationException(String message, int phase, String field, String businessRule, int httpStatus) {
        super(message);
        this.phase = phase;
        this.field = field;
        this.businessRule = businessRule;
        this.httpStatus = httpStatus;
    }

    public int getPhase() {
        return phase;
    }

    public String getField() {
        return field;
    }

    public String getBusinessRule() {
        return businessRule;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
