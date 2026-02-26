package com.carddemo.validation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Validation results for a single source/target table pair.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableValidationResult {

    private String tablePairName;
    private String sourceTable;
    private String targetTable;
    private ValidationReport.OverallStatus status = ValidationReport.OverallStatus.PASSED;

    // Row-count comparison
    private Long sourceRowCount;
    private Long targetRowCount;
    private Boolean rowCountMatch;

    // Checksum comparison
    private String sourceChecksum;
    private String targetChecksum;
    private Boolean checksumMatch;

    // Sample record diffs
    private List<RecordDiff> sampleDiffs = new ArrayList<>();

    // Mainframe file comparison
    private MainframeFileComparisonResult mainframeFileComparison;

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

    public ValidationReport.OverallStatus getStatus() {
        return status;
    }

    public void setStatus(ValidationReport.OverallStatus status) {
        this.status = status;
    }

    public Long getSourceRowCount() {
        return sourceRowCount;
    }

    public void setSourceRowCount(Long sourceRowCount) {
        this.sourceRowCount = sourceRowCount;
    }

    public Long getTargetRowCount() {
        return targetRowCount;
    }

    public void setTargetRowCount(Long targetRowCount) {
        this.targetRowCount = targetRowCount;
    }

    public Boolean getRowCountMatch() {
        return rowCountMatch;
    }

    public void setRowCountMatch(Boolean rowCountMatch) {
        this.rowCountMatch = rowCountMatch;
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

    public Boolean getChecksumMatch() {
        return checksumMatch;
    }

    public void setChecksumMatch(Boolean checksumMatch) {
        this.checksumMatch = checksumMatch;
    }

    public List<RecordDiff> getSampleDiffs() {
        return sampleDiffs;
    }

    public void setSampleDiffs(List<RecordDiff> sampleDiffs) {
        this.sampleDiffs = sampleDiffs;
    }

    public MainframeFileComparisonResult getMainframeFileComparison() {
        return mainframeFileComparison;
    }

    public void setMainframeFileComparison(MainframeFileComparisonResult mainframeFileComparison) {
        this.mainframeFileComparison = mainframeFileComparison;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    // =========================================================================
    // Inner types
    // =========================================================================

    /**
     * Represents a single differing record between source and target.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecordDiff {
        private Map<String, Object> primaryKey;
        private Map<String, FieldDiff> fieldDiffs;

        public Map<String, Object> getPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(Map<String, Object> primaryKey) {
            this.primaryKey = primaryKey;
        }

        public Map<String, FieldDiff> getFieldDiffs() {
            return fieldDiffs;
        }

        public void setFieldDiffs(Map<String, FieldDiff> fieldDiffs) {
            this.fieldDiffs = fieldDiffs;
        }
    }

    /**
     * A single field-level difference between source and target values.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldDiff {
        private Object sourceValue;
        private Object targetValue;

        public FieldDiff() {
        }

        public FieldDiff(Object sourceValue, Object targetValue) {
            this.sourceValue = sourceValue;
            this.targetValue = targetValue;
        }

        public Object getSourceValue() {
            return sourceValue;
        }

        public void setSourceValue(Object sourceValue) {
            this.sourceValue = sourceValue;
        }

        public Object getTargetValue() {
            return targetValue;
        }

        public void setTargetValue(Object targetValue) {
            this.targetValue = targetValue;
        }
    }

    /**
     * Result of comparing mainframe flat-file data against Postgres data.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MainframeFileComparisonResult {
        private long fileRecordCount;
        private long dbRecordCount;
        private boolean recordCountMatch;
        private List<RecordDiff> sampleDiffs = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public long getFileRecordCount() {
            return fileRecordCount;
        }

        public void setFileRecordCount(long fileRecordCount) {
            this.fileRecordCount = fileRecordCount;
        }

        public long getDbRecordCount() {
            return dbRecordCount;
        }

        public void setDbRecordCount(long dbRecordCount) {
            this.dbRecordCount = dbRecordCount;
        }

        public boolean isRecordCountMatch() {
            return recordCountMatch;
        }

        public void setRecordCountMatch(boolean recordCountMatch) {
            this.recordCountMatch = recordCountMatch;
        }

        public List<RecordDiff> getSampleDiffs() {
            return sampleDiffs;
        }

        public void setSampleDiffs(List<RecordDiff> sampleDiffs) {
            this.sampleDiffs = sampleDiffs;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}
