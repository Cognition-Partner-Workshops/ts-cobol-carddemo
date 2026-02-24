package com.carddemo.transaction.dto;

/**
 * Extended error response for the 6-phase validation chain in CT02.
 * Includes the validation phase number for diagnostics and traceability.
 */
public class ValidationErrorResponse extends ErrorResponse {

    private int phase;

    public ValidationErrorResponse() {
        super();
    }

    public ValidationErrorResponse(int status, String error, String message, String field,
                                   String businessRule, int phase) {
        super(status, error, message, field, businessRule);
        this.phase = phase;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }
}
