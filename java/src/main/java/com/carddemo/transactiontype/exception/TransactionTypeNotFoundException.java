package com.carddemo.transactiontype.exception;

public class TransactionTypeNotFoundException extends RuntimeException {

    public TransactionTypeNotFoundException(String typeCode) {
        super("Transaction type not found with code: " + typeCode);
    }
}
