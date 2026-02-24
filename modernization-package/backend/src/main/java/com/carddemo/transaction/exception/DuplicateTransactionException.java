package com.carddemo.transaction.exception;

/**
 * Exception thrown when a duplicate Transaction ID is detected (BR-AT-14).
 */
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }
}
