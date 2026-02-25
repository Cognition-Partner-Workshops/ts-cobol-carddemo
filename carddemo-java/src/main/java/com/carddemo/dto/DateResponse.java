package com.carddemo.dto;

/**
 * Date Response MQ message - migrated from CODATE01 (Phase 5c).
 * RESPONSE-TYPE  PIC X(4) = 'DATE'
 * RESPONSE-ID    PIC X(8)
 * SYSTEM-DATE    PIC X(10)
 */
public record DateResponse(String responseType, String responseId, String systemDate) {

    public String toMessage() {
        return String.format("%-4s%-8s%-10s", responseType, responseId, systemDate);
    }
}
