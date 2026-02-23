package com.carddemo.core.exception;

/**
 * Exception thrown when a business rule validation fails.
 * Replaces COBOL WS-RETURN-MSG error handling patterns.
 */
public class BusinessValidationException extends RuntimeException {

    private final String errorCode;

    public BusinessValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
    }

    public BusinessValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
