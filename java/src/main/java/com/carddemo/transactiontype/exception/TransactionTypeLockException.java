package com.carddemo.transactiontype.exception;

public class TransactionTypeLockException extends RuntimeException {

    public TransactionTypeLockException(String typeCode) {
        super("Could not lock record for update. Transaction type code: " + typeCode);
    }
}
