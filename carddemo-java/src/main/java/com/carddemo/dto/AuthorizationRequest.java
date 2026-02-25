package com.carddemo.dto;

import java.math.BigDecimal;

/**
 * Authorization Request MQ message POJO - migrated from COPAUA0C MQ message format.
 * CSV format: AUTH-DATE, AUTH-TIME, CARD-NUM, AUTH-TYPE, CARD-EXPIRY-DATE,
 *   MESSAGE-TYPE, MESSAGE-SOURCE, PROCESSING-CODE, TRANSACTION-AMT,
 *   MERCHANT-CATEGORY-CODE, ACQR-COUNTRY-CODE, POS-ENTRY-MODE,
 *   MERCHANT-ID, MERCHANT-NAME, MERCHANT-CITY, MERCHANT-STATE,
 *   MERCHANT-ZIP, TRANSACTION-ID
 */
public record AuthorizationRequest(
    String authDate,
    String authTime,
    String cardNum,
    String authType,
    String cardExpiryDate,
    String messageType,
    String messageSource,
    String processingCode,
    BigDecimal transactionAmt,
    String merchantCategoryCode,
    String acqrCountryCode,
    int posEntryMode,
    String merchantId,
    String merchantName,
    String merchantCity,
    String merchantState,
    String merchantZip,
    String transactionId
) {

    /**
     * Parse a CSV-formatted authorization request message.
     */
    public static AuthorizationRequest fromCsv(String csv) {
        String[] fields = csv.split(",", -1);
        if (fields.length < 18) {
            throw new IllegalArgumentException("Invalid authorization request CSV: expected 18 fields, got " + fields.length);
        }
        return new AuthorizationRequest(
            fields[0].trim(),
            fields[1].trim(),
            fields[2].trim(),
            fields[3].trim(),
            fields[4].trim(),
            fields[5].trim(),
            fields[6].trim(),
            fields[7].trim(),
            new BigDecimal(fields[8].trim()),
            fields[9].trim(),
            fields[10].trim(),
            Integer.parseInt(fields[11].trim()),
            fields[12].trim(),
            fields[13].trim(),
            fields[14].trim(),
            fields[15].trim(),
            fields[16].trim(),
            fields[17].trim()
        );
    }
}
