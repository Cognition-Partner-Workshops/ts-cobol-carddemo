package com.carddemo.common.exception;

public class CardExpiredException extends RuntimeException {
    
    public CardExpiredException(String cardNumber) {
        super(String.format("Card ending in %s has expired", cardNumber.substring(cardNumber.length() - 4)));
    }
}
