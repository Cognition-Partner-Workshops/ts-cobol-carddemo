package com.carddemo.common.exception;

public class AccountInactiveException extends RuntimeException {
    
    public AccountInactiveException(Long accountId) {
        super(String.format("Account %d is inactive", accountId));
    }
}
