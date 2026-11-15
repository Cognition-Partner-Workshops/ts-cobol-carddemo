package com.carddemo.interest.load;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The reconciliation figures of one file or of its table (strategy RC1, RC2): the record count and
 * the sum of each summed column, by column name. Sums have the scale of their column.
 */
public record LoadFigures(long count, Map<String, BigDecimal> sums) {

    public LoadFigures {
        sums = Map.copyOf(sums);
    }

    public BigDecimal sum(String column) {
        return sums.get(column);
    }

    /** BigDecimal.equals compares the scale too, and the two sides of RC2 may differ in scale only. */
    public boolean sameAs(LoadFigures other) {
        return count == other.count && sums.keySet().equals(other.sums.keySet())
                && sums.entrySet().stream().allMatch(sum -> sum.getValue().compareTo(other.sums.get(sum.getKey())) == 0);
    }
}
