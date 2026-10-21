package com.carddemo.batch.domain;

import java.math.BigDecimal;

/** ACCOUNT-RECORD, app/cpy/CVACT01Y.cpy. */
public record Account(long id, BigDecimal currentBalance, BigDecimal creditLimit, String expirationDate,
                      BigDecimal cycleCredit, BigDecimal cycleDebit, String groupId) {

    public Account withBalances(BigDecimal balance, BigDecimal credit, BigDecimal debit) {
        return new Account(id, balance, creditLimit, expirationDate, credit, debit, groupId);
    }
}
