package com.carddemo.exception;

/**
 * Exception thrown when a request contains invalid data.
 * Replaces mainframe CICS RESP 16 (Invalid Request) condition.
 */
public class InvalidRequestException extends RuntimeException {

    private final String field;
    private final String reason;

    public InvalidRequestException(String message) {
        super(message);
        this.field = null;
        this.reason = message;
    }

    public InvalidRequestException(String field, String reason) {
        super(String.format("Invalid value for field '%s': %s", field, reason));
        this.field = field;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }
}
