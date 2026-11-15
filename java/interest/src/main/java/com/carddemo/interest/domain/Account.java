package com.carddemo.interest.domain;

import java.math.BigDecimal;

/** What the calculation reads of an account: current balance (12,2) and disclosure group. */
public record Account(long accountId, BigDecimal currentBalance, String groupId) {
}
