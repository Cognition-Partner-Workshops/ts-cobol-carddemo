package com.carddemo.transaction.dto;

/**
 * Successful transaction creation response (CT02).
 * Includes the auto-generated Transaction ID (BR-AT-13).
 */
public class AddTransactionResponse {

    private String transactionId;
    private String message;
    private TransactionDetailResponse transaction;

    public AddTransactionResponse() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TransactionDetailResponse getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionDetailResponse transaction) {
        this.transaction = transaction;
    }
}
