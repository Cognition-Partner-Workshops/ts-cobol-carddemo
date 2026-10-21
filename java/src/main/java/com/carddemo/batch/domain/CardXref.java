package com.carddemo.batch.domain;

/** CARD-XREF-RECORD, app/cpy/CVACT03Y.cpy. */
public record CardXref(String cardNumber, long customerId, long accountId) {
}
