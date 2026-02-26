package com.carddemo.validation.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Validation result for comparing a mainframe output file against a
 * microservice API endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MainframeApiValidationResult {

    private String comparisonName;
    private String mainframeFilePath;
    private String apiUrl;
    private ValidationReport.OverallStatus status = ValidationReport.OverallStatus.PASSED;
    private long fileRecordCount;
    private long apiRecordCount;
    private boolean recordCountMatch;
    private List<TableValidationResult.RecordDiff> sampleDiffs = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    // -- Convenience ----------------------------------------------------------

    public void addError(String error) {
        errors.add(error);
        status = ValidationReport.OverallStatus.FAILED;
    }

    public void markFailed() {
        status = ValidationReport.OverallStatus.FAILED;
    }

    // -- Getters / Setters ----------------------------------------------------

    public String getComparisonName() {
        return comparisonName;
    }

    public void setComparisonName(String comparisonName) {
        this.comparisonName = comparisonName;
    }

    public String getMainframeFilePath() {
        return mainframeFilePath;
    }

    public void setMainframeFilePath(String mainframeFilePath) {
        this.mainframeFilePath = mainframeFilePath;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public ValidationReport.OverallStatus getStatus() {
        return status;
    }

    public void setStatus(ValidationReport.OverallStatus status) {
        this.status = status;
    }

    public long getFileRecordCount() {
        return fileRecordCount;
    }

    public void setFileRecordCount(long fileRecordCount) {
        this.fileRecordCount = fileRecordCount;
    }

    public long getApiRecordCount() {
        return apiRecordCount;
    }

    public void setApiRecordCount(long apiRecordCount) {
        this.apiRecordCount = apiRecordCount;
    }

    public boolean isRecordCountMatch() {
        return recordCountMatch;
    }

    public void setRecordCountMatch(boolean recordCountMatch) {
        this.recordCountMatch = recordCountMatch;
    }

    public List<TableValidationResult.RecordDiff> getSampleDiffs() {
        return sampleDiffs;
    }

    public void setSampleDiffs(List<TableValidationResult.RecordDiff> sampleDiffs) {
        this.sampleDiffs = sampleDiffs;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
