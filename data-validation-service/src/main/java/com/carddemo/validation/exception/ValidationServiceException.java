package com.carddemo.validation.exception;

/**
 * Thrown when an unrecoverable error occurs during a validation run.
 */
public class ValidationServiceException extends RuntimeException {

    public ValidationServiceException(String message) {
        super(message);
    }

    public ValidationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
