package com.carddemo.validation.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Top-level validation report returned by the {@code /validate} endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationReport {

    private final Instant timestamp = Instant.now();
    private OverallStatus overallStatus = OverallStatus.PASSED;
    private final List<TableValidationResult> tableResults = new ArrayList<>();
    private final List<MainframeApiValidationResult> mainframeApiResults = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    // -- Convenience mutators ------------------------------------------------

    public void addTableResult(TableValidationResult result) {
        tableResults.add(result);
        if (result.getStatus() == OverallStatus.FAILED) {
            overallStatus = OverallStatus.FAILED;
        }
    }

    public void addMainframeApiResult(MainframeApiValidationResult result) {
        mainframeApiResults.add(result);
        if (result.getStatus() == OverallStatus.FAILED) {
            overallStatus = OverallStatus.FAILED;
        }
    }

    public void addError(String error) {
        errors.add(error);
        overallStatus = OverallStatus.FAILED;
    }

    // -- Getters / Setters ---------------------------------------------------

    public Instant getTimestamp() {
        return timestamp;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public List<TableValidationResult> getTableResults() {
        return tableResults;
    }

    public List<MainframeApiValidationResult> getMainframeApiResults() {
        return mainframeApiResults;
    }

    public List<String> getErrors() {
        return errors;
    }

    // -- Status enum ---------------------------------------------------------

    public enum OverallStatus {
        PASSED, FAILED, SKIPPED
    }
}
