package com.carddemo.batch.domain;

import java.math.BigDecimal;

/** DALYTRAN-RECORD, app/cpy/CVTRA06Y.cpy. */
public record DailyTransaction(long seq, String id, String typeCode, int categoryCode, String source,
                               String description, BigDecimal amount, long merchantId, String merchantName,
                               String merchantCity, String merchantZip, String cardNumber, String originTimestamp,
                               String processTimestamp) {

    public String originDate() {
        return originTimestamp.length() >= 10 ? originTimestamp.substring(0, 10) : originTimestamp;
    }
}
