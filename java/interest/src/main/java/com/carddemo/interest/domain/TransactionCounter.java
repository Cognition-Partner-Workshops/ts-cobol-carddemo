package com.carddemo.interest.domain;

/**
 * INT-Q02: the six-digit counter of the transaction id. It starts at 000001 in every run and runs
 * across accounts (CBACT04C:173, 474). A number is never cut to six digits (INT-H05). One instance belongs to one step execution.
 */
public final class TransactionCounter {

    /** INT-H05: the last number six digits hold. */
    private static final int LAST_OF_SIX_DIGITS = 999_999;

    private int lastIssued;

    /** INT-H05: true when the next number would need a seventh digit. */
    public boolean nextNeedsSevenDigits() {
        return lastIssued >= LAST_OF_SIX_DIGITS;
    }

    public String next() {
        lastIssued++;
        return String.format("%06d", lastIssued);
    }

    public int issued() {
        return lastIssued;
    }
}
