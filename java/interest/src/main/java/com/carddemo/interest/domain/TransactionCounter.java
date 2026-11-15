package com.carddemo.interest.domain;

/**
 * INT-Q02: the six-digit counter of the transaction id. It starts at 000001 in every run and runs
 * across accounts (CBACT04C:173, 474). One instance belongs to one step execution.
 */
public final class TransactionCounter {

    private int lastIssued;

    public String next() {
        lastIssued++;
        return String.format("%06d", lastIssued);
    }

    public int issued() {
        return lastIssued;
    }
}
