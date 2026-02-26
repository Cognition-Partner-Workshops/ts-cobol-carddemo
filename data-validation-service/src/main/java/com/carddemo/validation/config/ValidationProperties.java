package com.carddemo.validation.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Strongly-typed configuration binding for {@code validation.*} YAML keys.
 *
 * <p>Each entry in {@link #tablePairs} describes a source/target table pair
 * together with the validation rules that should be applied during a
 * validation run.</p>
 */
@ConfigurationProperties(prefix = "validation")
public class ValidationProperties {

    /** Global toggle – set to {@code false} to skip all validations. */
    private boolean enabled = true;

    /** Maximum number of sample records to include in diff reports. */
    private int maxSampleRecords = 10;

    /** Pairs of source/target tables and their validation rules. */
    @Valid
    @NotEmpty(message = "At least one table pair must be configured")
    private List<TablePairConfig> tablePairs = new ArrayList<>();

    /** Optional mainframe-to-API comparison configs. */
    @Valid
    private List<MainframeApiConfig> mainframeApiComparisons = new ArrayList<>();

    // ---- Getters / Setters ------------------------------------------------

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxSampleRecords() {
        return maxSampleRecords;
    }

    public void setMaxSampleRecords(int maxSampleRecords) {
        this.maxSampleRecords = maxSampleRecords;
    }

    public List<TablePairConfig> getTablePairs() {
        return tablePairs;
    }

    public void setTablePairs(List<TablePairConfig> tablePairs) {
        this.tablePairs = tablePairs;
    }

    public List<MainframeApiConfig> getMainframeApiComparisons() {
        return mainframeApiComparisons;
    }

    public void setMainframeApiComparisons(List<MainframeApiConfig> mainframeApiComparisons) {
        this.mainframeApiComparisons = mainframeApiComparisons;
    }

    // ====================================================================
    // Inner configuration classes
    // ====================================================================

    /**
     * Describes a source (DB2 / legacy) and target (Postgres) table pair
     * along with which validation rules to execute.
     */
    public static class TablePairConfig {

        @NotBlank
        private String name;

        /** Source datasource key (references a named Spring datasource). */
        @NotBlank
        private String sourceDatasource;

        /** Source table (fully-qualified if needed). */
        @NotBlank
        private String sourceTable;

        /** Target datasource key. */
        @NotBlank
        private String targetDatasource;

        /** Target table (fully-qualified if needed). */
        @NotBlank
        private String targetTable;

        /** Primary-key columns used for record-level comparison. */
        private List<String> primaryKeyColumns = new ArrayList<>();

        /** Columns to include in checksum computation (empty = all). */
        private List<String> checksumColumns = new ArrayList<>();

        /** Optional path to a mainframe flat-file for file-vs-DB comparison. */
        private String mainframeFilePath;

        /** Validation rules to apply. */
        @Valid
        private ValidationRules rules = new ValidationRules();

        // -- Getters / Setters --

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSourceDatasource() {
            return sourceDatasource;
        }

        public void setSourceDatasource(String sourceDatasource) {
            this.sourceDatasource = sourceDatasource;
        }

        public String getSourceTable() {
            return sourceTable;
        }

        public void setSourceTable(String sourceTable) {
            this.sourceTable = sourceTable;
        }

        public String getTargetDatasource() {
            return targetDatasource;
        }

        public void setTargetDatasource(String targetDatasource) {
            this.targetDatasource = targetDatasource;
        }

        public String getTargetTable() {
            return targetTable;
        }

        public void setTargetTable(String targetTable) {
            this.targetTable = targetTable;
        }

        public List<String> getPrimaryKeyColumns() {
            return primaryKeyColumns;
        }

        public void setPrimaryKeyColumns(List<String> primaryKeyColumns) {
            this.primaryKeyColumns = primaryKeyColumns;
        }

        public List<String> getChecksumColumns() {
            return checksumColumns;
        }

        public void setChecksumColumns(List<String> checksumColumns) {
            this.checksumColumns = checksumColumns;
        }

        public String getMainframeFilePath() {
            return mainframeFilePath;
        }

        public void setMainframeFilePath(String mainframeFilePath) {
            this.mainframeFilePath = mainframeFilePath;
        }

        public ValidationRules getRules() {
            return rules;
        }

        public void setRules(ValidationRules rules) {
            this.rules = rules;
        }
    }

    /**
     * Toggles for individual validation checks.
     */
    public static class ValidationRules {

        private boolean rowCount = true;
        private boolean checksum = true;
        private boolean sampleRecordDiff = true;
        private boolean mainframeFileComparison = false;

        public boolean isRowCount() {
            return rowCount;
        }

        public void setRowCount(boolean rowCount) {
            this.rowCount = rowCount;
        }

        public boolean isChecksum() {
            return checksum;
        }

        public void setChecksum(boolean checksum) {
            this.checksum = checksum;
        }

        public boolean isSampleRecordDiff() {
            return sampleRecordDiff;
        }

        public void setSampleRecordDiff(boolean sampleRecordDiff) {
            this.sampleRecordDiff = sampleRecordDiff;
        }

        public boolean isMainframeFileComparison() {
            return mainframeFileComparison;
        }

        public void setMainframeFileComparison(boolean mainframeFileComparison) {
            this.mainframeFileComparison = mainframeFileComparison;
        }
    }

    /**
     * Configuration for comparing a mainframe output file against a
     * microservice API endpoint.
     */
    public static class MainframeApiConfig {

        @NotBlank
        private String name;

        /** Path to the mainframe output flat-file. */
        @NotBlank
        private String mainframeFilePath;

        /** URL of the microservice API to compare against. */
        @NotBlank
        private String apiUrl;

        /** HTTP method (GET / POST). */
        private String httpMethod = "GET";

        /** Optional request body (for POST). */
        private String requestBody;

        /** Primary-key / identity fields used for record matching. */
        private List<String> keyFields = new ArrayList<>();

        // -- Getters / Setters --

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getRequestBody() {
            return requestBody;
        }

        public void setRequestBody(String requestBody) {
            this.requestBody = requestBody;
        }

        public List<String> getKeyFields() {
            return keyFields;
        }

        public void setKeyFields(List<String> keyFields) {
            this.keyFields = keyFields;
        }
    }
}
