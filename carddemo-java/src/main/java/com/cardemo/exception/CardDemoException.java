package com.cardemo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class CardDemoException extends RuntimeException {

    private final HttpStatus status;

    public CardDemoException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static CardDemoException notFound(String message) {
        return new CardDemoException(message, HttpStatus.NOT_FOUND);
    }

    public static CardDemoException badRequest(String message) {
        return new CardDemoException(message, HttpStatus.BAD_REQUEST);
    }

    public static CardDemoException unauthorized(String message) {
        return new CardDemoException(message, HttpStatus.UNAUTHORIZED);
    }

    public static CardDemoException forbidden(String message) {
        return new CardDemoException(message, HttpStatus.FORBIDDEN);
    }

    public static CardDemoException conflict(String message) {
        return new CardDemoException(message, HttpStatus.CONFLICT);
    }
}
