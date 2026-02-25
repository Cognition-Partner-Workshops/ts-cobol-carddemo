package com.carddemo.validation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation results for a single source/target table pair.
 */
public class TableValidationResult {

    private String tablePairName;
    private String sourceTable;
    private String targetTable;

    // DB-to-DB validation
    private ValidationStatus rowCountStatus;
    private long sourceRowCount;
    private long targetRowCount;

    private ValidationStatus checksumStatus;
    private String sourceChecksum;
    private String targetChecksum;

    private ValidationStatus sampleDiffStatus;
    private List<RecordDiff> sampleDiffs = new ArrayList<>();

    // File-to-DB validation
    private ValidationStatus fileToDbStatus;
    private long fileRowCount;
    private String fileToDbDetail;

    // File-to-API validation
    private ValidationStatus fileToApiStatus;
    private long apiRecordCount;
    private String fileToApiDetail;

    // Error information
    private List<String> errors = new ArrayList<>();

    public TableValidationResult() {
    }

    public TableValidationResult(String tablePairName, String sourceTable, String targetTable) {
        this.tablePairName = tablePairName;
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
    }

    /**
     * Returns the overall status for this table pair by aggregating all
     * individual check statuses.
     */
    public ValidationStatus getOverallStatus() {
        List<ValidationStatus> statuses = List.of(
                rowCountStatus != null ? rowCountStatus : ValidationStatus.SKIPPED,
                checksumStatus != null ? checksumStatus : ValidationStatus.SKIPPED,
                sampleDiffStatus != null ? sampleDiffStatus : ValidationStatus.SKIPPED,
                fileToDbStatus != null ? fileToDbStatus : ValidationStatus.SKIPPED,
                fileToApiStatus != null ? fileToApiStatus : ValidationStatus.SKIPPED
        );
        if (statuses.stream().anyMatch(s -> s == ValidationStatus.ERROR)) {
            return ValidationStatus.ERROR;
        }
        if (statuses.stream().anyMatch(s -> s == ValidationStatus.FAIL)) {
            return ValidationStatus.FAIL;
        }
        if (statuses.stream().allMatch(s -> s == ValidationStatus.SKIPPED)) {
            return ValidationStatus.SKIPPED;
        }
        return ValidationStatus.PASS;
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public String getTablePairName() {
        return tablePairName;
    }

    public void setTablePairName(String tablePairName) {
        this.tablePairName = tablePairName;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public ValidationStatus getRowCountStatus() {
        return rowCountStatus;
    }

    public void setRowCountStatus(ValidationStatus rowCountStatus) {
        this.rowCountStatus = rowCountStatus;
    }

    public long getSourceRowCount() {
        return sourceRowCount;
    }

    public void setSourceRowCount(long sourceRowCount) {
        this.sourceRowCount = sourceRowCount;
    }

    public long getTargetRowCount() {
        return targetRowCount;
    }

    public void setTargetRowCount(long targetRowCount) {
        this.targetRowCount = targetRowCount;
    }

    public ValidationStatus getChecksumStatus() {
        return checksumStatus;
    }

    public void setChecksumStatus(ValidationStatus checksumStatus) {
        this.checksumStatus = checksumStatus;
    }

    public String getSourceChecksum() {
        return sourceChecksum;
    }

    public void setSourceChecksum(String sourceChecksum) {
        this.sourceChecksum = sourceChecksum;
    }

    public String getTargetChecksum() {
        return targetChecksum;
    }

    public void setTargetChecksum(String targetChecksum) {
        this.targetChecksum = targetChecksum;
    }

    public ValidationStatus getSampleDiffStatus() {
        return sampleDiffStatus;
    }

    public void setSampleDiffStatus(ValidationStatus sampleDiffStatus) {
        this.sampleDiffStatus = sampleDiffStatus;
    }

    public List<RecordDiff> getSampleDiffs() {
        return sampleDiffs;
    }

    public void setSampleDiffs(List<RecordDiff> sampleDiffs) {
        this.sampleDiffs = sampleDiffs;
    }

    public ValidationStatus getFileToDbStatus() {
        return fileToDbStatus;
    }

    public void setFileToDbStatus(ValidationStatus fileToDbStatus) {
        this.fileToDbStatus = fileToDbStatus;
    }

    public long getFileRowCount() {
        return fileRowCount;
    }

    public void setFileRowCount(long fileRowCount) {
        this.fileRowCount = fileRowCount;
    }

    public String getFileToDbDetail() {
        return fileToDbDetail;
    }

    public void setFileToDbDetail(String fileToDbDetail) {
        this.fileToDbDetail = fileToDbDetail;
    }

    public ValidationStatus getFileToApiStatus() {
        return fileToApiStatus;
    }

    public void setFileToApiStatus(ValidationStatus fileToApiStatus) {
        this.fileToApiStatus = fileToApiStatus;
    }

    public long getApiRecordCount() {
        return apiRecordCount;
    }

    public void setApiRecordCount(long apiRecordCount) {
        this.apiRecordCount = apiRecordCount;
    }

    public String getFileToApiDetail() {
        return fileToApiDetail;
    }

    public void setFileToApiDetail(String fileToApiDetail) {
        this.fileToApiDetail = fileToApiDetail;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }
}
