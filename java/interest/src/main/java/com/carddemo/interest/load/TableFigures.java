package com.carddemo.interest.load;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The reconciliation figures of one file or of one table (strategy items RC1, RC2, RC4): the row
 * count, the number of distinct keys, the sum of every amount and the hash total of the account
 * ids, each sum under the name of its column. Both sides produce one; they must be equal.
 */
public record TableFigures(long rowCount, long keyCount, Map<String, BigDecimal> sums) {

    public BigDecimal sum(String column) {
        return sums.get(column);
    }
}
