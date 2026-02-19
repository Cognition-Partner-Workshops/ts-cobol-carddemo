package com.carddemo.transactiontype.exception;

public class TransactionTypeConflictException extends RuntimeException {

    public TransactionTypeConflictException(String message) {
        super(message);
    }
}
