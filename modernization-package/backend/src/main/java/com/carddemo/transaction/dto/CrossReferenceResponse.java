package com.carddemo.transaction.dto;

/**
 * Result of bidirectional cross-reference resolution (BR-AT-04, BR-AT-05).
 * Contains all three fields from the CARD-XREF-RECORD (CVACT03Y.cpy).
 */
public class CrossReferenceResponse {

    private String cardNumber;
    private String accountId;
    private long customerId;

    public CrossReferenceResponse() {
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }
}
