package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.carddemo.interest.jdbc.ReplicaTable;

/**
 * What the report read from the database, in one read-only transaction: the run's row, the rows of
 * the four tables encoded as records in key order, and the run's interest transactions. A null
 * {@code run} means there is no such row; {@code failure} means the database could not be read.
 */
public record DatabaseSide(RunRow run, Map<ReplicaTable, List<byte[]>> tableRecords, Long transactionRows,
        BigDecimal transactionAmountSum, String failure) {

    public static DatabaseSide unavailable(String why) {
        return new DatabaseSide(null, Map.of(), null, null, why);
    }

    /** Why a figure of the load is missing, or null when the tables hold a load. */
    String whyNoLoad() {
        if (failure != null) {
            return failure;
        }
        if (run == null) {
            return "no row in interest_run";
        }
        return run.loadedRun() ? null : "interest_run " + run.runId() + " is " + run.status() + ": the tables do not hold its load";
    }

    /** Why a figure of the calculation is missing, or null when the run is calculated. */
    String whyNoCalculation() {
        String noLoad = whyNoLoad();
        if (noLoad != null) {
            return noLoad;
        }
        return run.calculated() ? null : "interest_run " + run.runId() + " is " + run.status() + ": not calculated on the tables";
    }
}
