package com.carddemo.transaction.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Used for Transaction ID not found (BR-VT-01/BR-VT-02),
 * Account ID not found (BR-AT-04), Card Number not found (BR-AT-04).
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String field;
    private final String businessRule;

    public ResourceNotFoundException(String message, String field, String businessRule) {
        super(message);
        this.field = field;
        this.businessRule = businessRule;
    }

    public String getField() {
        return field;
    }

    public String getBusinessRule() {
        return businessRule;
    }
}
