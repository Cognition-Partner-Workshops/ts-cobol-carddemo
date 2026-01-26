package com.aws.carddemo.exception;

public class TransactionValidationException extends CardDemoException {

    public static final int INVALID_CARD_NUMBER = 100;
    public static final int ACCOUNT_NOT_FOUND = 101;
    public static final int OVERLIMIT_TRANSACTION = 102;
    public static final int ACCOUNT_EXPIRED = 103;

    public static final String INVALID_CARD_NUMBER_DESC = "INVALID CARD NUMBER FOUND";
    public static final String ACCOUNT_NOT_FOUND_DESC = "ACCOUNT RECORD NOT FOUND";
    public static final String OVERLIMIT_TRANSACTION_DESC = "OVERLIMIT TRANSACTION";
    public static final String ACCOUNT_EXPIRED_DESC = "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";

    public TransactionValidationException(int errorCode, String message) {
        super(message, errorCode);
    }

    public static TransactionValidationException invalidCardNumber() {
        return new TransactionValidationException(INVALID_CARD_NUMBER, INVALID_CARD_NUMBER_DESC);
    }

    public static TransactionValidationException accountNotFound() {
        return new TransactionValidationException(ACCOUNT_NOT_FOUND, ACCOUNT_NOT_FOUND_DESC);
    }

    public static TransactionValidationException overlimitTransaction() {
        return new TransactionValidationException(OVERLIMIT_TRANSACTION, OVERLIMIT_TRANSACTION_DESC);
    }

    public static TransactionValidationException accountExpired() {
        return new TransactionValidationException(ACCOUNT_EXPIRED, ACCOUNT_EXPIRED_DESC);
    }
}
