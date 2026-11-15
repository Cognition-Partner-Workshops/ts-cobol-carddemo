package com.carddemo.interest.domain;

/** The 11 digits of an account id inside a text (ACCT-ID 9(11); standards S3.6). */
final class AccountIdText {

    private AccountIdText() {
    }

    static String elevenDigits(long accountId) {
        return String.format("%011d", accountId);
    }
}
