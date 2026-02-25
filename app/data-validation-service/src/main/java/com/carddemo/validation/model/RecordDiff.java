package com.carddemo.validation.model;

import java.util.Map;

/**
 * Represents the difference for a single record identified by its primary-key value.
 */
public class RecordDiff {

    private String primaryKeyValue;
    private Map<String, Object> sourceRecord;
    private Map<String, Object> targetRecord;
    private Map<String, FieldDiff> fieldDiffs;

    public RecordDiff() {
    }

    public RecordDiff(String primaryKeyValue,
                      Map<String, Object> sourceRecord,
                      Map<String, Object> targetRecord,
                      Map<String, FieldDiff> fieldDiffs) {
        this.primaryKeyValue = primaryKeyValue;
        this.sourceRecord = sourceRecord;
        this.targetRecord = targetRecord;
        this.fieldDiffs = fieldDiffs;
    }

    public String getPrimaryKeyValue() {
        return primaryKeyValue;
    }

    public void setPrimaryKeyValue(String primaryKeyValue) {
        this.primaryKeyValue = primaryKeyValue;
    }

    public Map<String, Object> getSourceRecord() {
        return sourceRecord;
    }

    public void setSourceRecord(Map<String, Object> sourceRecord) {
        this.sourceRecord = sourceRecord;
    }

    public Map<String, Object> getTargetRecord() {
        return targetRecord;
    }

    public void setTargetRecord(Map<String, Object> targetRecord) {
        this.targetRecord = targetRecord;
    }

    public Map<String, FieldDiff> getFieldDiffs() {
        return fieldDiffs;
    }

    public void setFieldDiffs(Map<String, FieldDiff> fieldDiffs) {
        this.fieldDiffs = fieldDiffs;
    }

    /**
     * Represents the difference for a single field within a record.
     */
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
}
