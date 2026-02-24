package com.carddemo.transaction.dto;

import java.time.Instant;

/**
 * Standard error response format matching the legacy error message catalog (BRE Section 8.2).
 * Used for all error conditions across the API.
 */
public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String field;
    private String businessRule;

    public ErrorResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ErrorResponse(int status, String error, String message, String field, String businessRule) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.field = field;
        this.businessRule = businessRule;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getBusinessRule() {
        return businessRule;
    }

    public void setBusinessRule(String businessRule) {
        this.businessRule = businessRule;
    }
}
