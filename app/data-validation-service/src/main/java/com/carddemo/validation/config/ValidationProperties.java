package com.carddemo.validation.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code validation.*} properties from {@code application.yml}
 * into a strongly-typed configuration object.
 */
@Component
@ConfigurationProperties(prefix = "validation")
public class ValidationProperties {

    private int maxSampleRecords = 10;
    private String mainframeOutputDir;
    private String apiBaseUrl;
    private List<TablePairConfig> tablePairs = new ArrayList<>();

    // ── Getters / Setters ────────────────────────────────────────────

    public int getMaxSampleRecords() {
        return maxSampleRecords;
    }

    public void setMaxSampleRecords(int maxSampleRecords) {
        this.maxSampleRecords = maxSampleRecords;
    }

    public String getMainframeOutputDir() {
        return mainframeOutputDir;
    }

    public void setMainframeOutputDir(String mainframeOutputDir) {
        this.mainframeOutputDir = mainframeOutputDir;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public List<TablePairConfig> getTablePairs() {
        return tablePairs;
    }

    public void setTablePairs(List<TablePairConfig> tablePairs) {
        this.tablePairs = tablePairs;
    }

    // ── Nested: table-pair configuration ─────────────────────────────

    public static class TablePairConfig {

        private String name;
        private String sourceTable;
        private String targetTable;
        private String sourceType;
        private String primaryKey;
        private List<String> rules = new ArrayList<>();
        private String mainframeFile;
        private String apiEndpoint;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(String primaryKey) {
            this.primaryKey = primaryKey;
        }

        public List<String> getRules() {
            return rules;
        }

        public void setRules(List<String> rules) {
            this.rules = rules;
        }

        public String getMainframeFile() {
            return mainframeFile;
        }

        public void setMainframeFile(String mainframeFile) {
            this.mainframeFile = mainframeFile;
        }

        public String getApiEndpoint() {
            return apiEndpoint;
        }

        public void setApiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
        }
    }
}
