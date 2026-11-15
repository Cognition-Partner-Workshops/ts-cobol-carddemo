package com.carddemo.interest.domain;

import java.math.BigDecimal;

/** One interest transaction (INT-R06); amount is (11,2). */
public record InterestTransaction(
        String id,
        String typeCode,
        int categoryCode,
        String source,
        String description,
        BigDecimal amount,
        long merchantId,
        String cardNumber,
        String originTimestamp,
        String processedTimestamp) {
}
