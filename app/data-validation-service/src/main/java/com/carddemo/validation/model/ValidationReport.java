package com.carddemo.validation.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level validation report returned by the {@code /validate} endpoint.
 */
public class ValidationReport {

    private Instant timestamp;
    private ValidationStatus overallStatus;
    private int totalTablePairs;
    private int passedCount;
    private int failedCount;
    private int skippedCount;
    private int errorCount;
    private long durationMillis;
    private List<TableValidationResult> tableResults = new ArrayList<>();
    private List<String> globalErrors = new ArrayList<>();

    public ValidationReport() {
        this.timestamp = Instant.now();
    }

    /**
     * Recompute summary counts from the list of table results.
     */
    public void computeSummary() {
        this.totalTablePairs = tableResults.size();
        this.passedCount = 0;
        this.failedCount = 0;
        this.skippedCount = 0;
        this.errorCount = 0;

        for (TableValidationResult result : tableResults) {
            switch (result.getOverallStatus()) {
                case PASS -> this.passedCount++;
                case FAIL -> this.failedCount++;
                case SKIPPED -> this.skippedCount++;
                case ERROR -> this.errorCount++;
            }
        }

        if (this.errorCount > 0 || !globalErrors.isEmpty()) {
            this.overallStatus = ValidationStatus.ERROR;
        } else if (this.failedCount > 0) {
            this.overallStatus = ValidationStatus.FAIL;
        } else if (this.passedCount > 0) {
            this.overallStatus = ValidationStatus.PASS;
        } else {
            this.overallStatus = ValidationStatus.SKIPPED;
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public ValidationStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(ValidationStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public int getTotalTablePairs() {
        return totalTablePairs;
    }

    public void setTotalTablePairs(int totalTablePairs) {
        this.totalTablePairs = totalTablePairs;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(int passedCount) {
        this.passedCount = passedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public List<TableValidationResult> getTableResults() {
        return tableResults;
    }

    public void setTableResults(List<TableValidationResult> tableResults) {
        this.tableResults = tableResults;
    }

    public List<String> getGlobalErrors() {
        return globalErrors;
    }

    public void setGlobalErrors(List<String> globalErrors) {
        this.globalErrors = globalErrors;
    }

    public void addGlobalError(String error) {
        this.globalErrors.add(error);
    }

    public void addTableResult(TableValidationResult result) {
        this.tableResults.add(result);
    }
}
