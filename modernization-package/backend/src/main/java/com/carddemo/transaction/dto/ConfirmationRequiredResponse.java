package com.carddemo.transaction.dto;

/**
 * Returned when all validation passes but confirmation is not "Y" (BR-AT-12).
 * The client should display the message and re-submit with confirmation = "Y".
 */
public class ConfirmationRequiredResponse {

    private boolean confirmationRequired;
    private String message;
    private String resolvedAccountId;
    private String resolvedCardNumber;

    public ConfirmationRequiredResponse() {
        this.confirmationRequired = true;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResolvedAccountId() {
        return resolvedAccountId;
    }

    public void setResolvedAccountId(String resolvedAccountId) {
        this.resolvedAccountId = resolvedAccountId;
    }

    public String getResolvedCardNumber() {
        return resolvedCardNumber;
    }

    public void setResolvedCardNumber(String resolvedCardNumber) {
        this.resolvedCardNumber = resolvedCardNumber;
    }
}
