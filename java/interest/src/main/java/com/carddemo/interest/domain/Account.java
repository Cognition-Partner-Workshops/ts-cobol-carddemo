package com.carddemo.interest.domain;

import java.math.BigDecimal;

/**
 * What the calculation reads of an account: the active flag as stored (INT-H04), the current
 * balance (12,2) and the disclosure group.
 */
public record Account(long accountId, String activeStatus, BigDecimal currentBalance, String groupId) {
}
