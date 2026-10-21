package com.carddemo.batch.domain;

import java.math.BigDecimal;

/** TRAN-RECORD, app/cpy/CVTRA05Y.cpy. */
public record Transaction(String id, String typeCode, int categoryCode, String source, String description,
                          BigDecimal amount, long merchantId, String merchantName, String merchantCity,
                          String merchantZip, String cardNumber, String originTimestamp, String processTimestamp) {
}
