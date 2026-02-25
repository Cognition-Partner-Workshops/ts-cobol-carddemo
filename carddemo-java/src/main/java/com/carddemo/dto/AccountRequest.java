package com.carddemo.dto;

/**
 * Account Request MQ message - migrated from COACCT01 (Phase 5c).
 * REQUEST-TYPE    PIC X(4)  = 'ACCT'
 * REQUEST-ID      PIC X(8)
 * ACCOUNT-NUMBER  PIC X(11)
 */
public record AccountRequest(String requestType, String requestId, String accountNumber) {

    public static AccountRequest parse(String message) {
        String type = message.substring(0, Math.min(4, message.length())).trim();
        String id = message.length() > 4 ? message.substring(4, Math.min(12, message.length())).trim() : "";
        String acctNum = message.length() > 12 ? message.substring(12, Math.min(23, message.length())).trim() : "";
        return new AccountRequest(type, id, acctNum);
    }
}
