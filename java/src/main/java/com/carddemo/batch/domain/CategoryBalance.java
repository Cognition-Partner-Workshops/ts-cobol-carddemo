package com.carddemo.batch.domain;

import java.math.BigDecimal;

/** TRAN-CAT-BAL-RECORD, app/cpy/CVTRA01Y.cpy. */
public record CategoryBalance(long accountId, String typeCode, int categoryCode, BigDecimal balance) {
}
