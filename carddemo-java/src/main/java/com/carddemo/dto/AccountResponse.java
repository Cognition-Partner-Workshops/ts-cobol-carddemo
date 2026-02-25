package com.carddemo.dto;

/**
 * Account Response MQ message - migrated from COACCT01 (Phase 5c).
 * RESPONSE-TYPE  PIC X(4)  = 'ACCT'
 * RESPONSE-ID    PIC X(8)
 * ACCOUNT-DATA   PIC X(300)
 */
public record AccountResponse(String responseType, String responseId, String accountData) {

    public String toMessage() {
        return String.format("%-4s%-8s%-300s", responseType, responseId, accountData);
    }
}
