package com.aws.carddemo.exception;

public class CardDemoException extends RuntimeException {

    private final int errorCode;

    public CardDemoException(String message) {
        super(message);
        this.errorCode = 0;
    }

    public CardDemoException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CardDemoException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 0;
    }

    public CardDemoException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
