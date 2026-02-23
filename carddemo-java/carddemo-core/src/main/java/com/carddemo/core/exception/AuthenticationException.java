package com.carddemo.core.exception;

/**
 * Exception thrown when authentication fails.
 * Replaces COBOL signon validation logic in COSGN00C.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
