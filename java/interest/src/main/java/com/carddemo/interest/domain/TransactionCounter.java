package com.carddemo.interest.domain;

/**
 * INT-Q02: the six-digit counter of the transaction id. It starts at 000001 in every run and runs
 * across accounts (CBACT04C:173, 474). One instance belongs to one step execution; a restarted
 * execution continues from the count saved with the last commit.
 */
public final class TransactionCounter {

    private int lastIssued;

    public TransactionCounter() {
        this(0);
    }

    /** A counter that has already issued this many ids in the run. */
    public TransactionCounter(int issuedSoFar) {
        this.lastIssued = issuedSoFar;
    }

    public String next() {
        lastIssued++;
        return String.format("%06d", lastIssued);
    }

    public int issued() {
        return lastIssued;
    }

    /** INT-H05: the next id would need a seventh digit. */
    boolean isExhausted() {
        return lastIssued >= FieldRange.HIGHEST_TRANSACTION_COUNTER;
    }
}
