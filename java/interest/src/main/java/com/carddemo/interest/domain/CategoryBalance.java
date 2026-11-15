package com.carddemo.interest.domain;

import java.math.BigDecimal;

/** One balance of an account for one transaction type and category; balance is (11,2). */
public record CategoryBalance(long accountId, String typeCode, int categoryCode, BigDecimal balance) {
}
