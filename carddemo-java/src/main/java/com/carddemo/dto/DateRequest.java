package com.carddemo.dto;

/**
 * Date Request MQ message - migrated from CODATE01 (Phase 5c).
 * REQUEST-TYPE  PIC X(4) = 'DATE'
 * REQUEST-ID    PIC X(8)
 */
public record DateRequest(String requestType, String requestId) {

    public static DateRequest parse(String message) {
        String type = message.substring(0, Math.min(4, message.length())).trim();
        String id = message.length() > 4 ? message.substring(4, Math.min(12, message.length())).trim() : "";
        return new DateRequest(type, id);
    }
}
