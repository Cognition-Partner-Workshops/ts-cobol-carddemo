package com.carddemo.validation.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.carddemo.validation.config.ValidationProperties;
import com.carddemo.validation.config.ValidationProperties.TablePairConfig;
import com.carddemo.validation.model.TableValidationResult;
import com.carddemo.validation.model.TableValidationResult.FieldDiff;
import com.carddemo.validation.model.TableValidationResult.RecordDiff;

/**
 * Compares sample records between source and target tables, reporting
 * field-level differences keyed by primary key.
 */
@Component
public class SampleRecordDiffValidator {

    private static final Logger log = LoggerFactory.getLogger(SampleRecordDiffValidator.class);

    private final Map<String, JdbcTemplate> jdbcTemplateRegistry;
    private final ValidationProperties properties;

    public SampleRecordDiffValidator(Map<String, JdbcTemplate> jdbcTemplateRegistry,
                                     ValidationProperties properties) {
        this.jdbcTemplateRegistry = jdbcTemplateRegistry;
        this.properties = properties;
    }

    /**
     * Fetch a limited number of records from both source and target,
     * join them by primary key, and report field-level differences.
     */
    public void validate(TablePairConfig pair, TableValidationResult result) {
        JdbcTemplate sourceJdbc = jdbcTemplateRegistry.get(pair.getSourceDatasource());
        JdbcTemplate targetJdbc = jdbcTemplateRegistry.get(pair.getTargetDatasource());

        if (sourceJdbc == null) {
            result.addError("Source datasource not found: " + pair.getSourceDatasource());
            return;
        }
        if (targetJdbc == null) {
            result.addError("Target datasource not found: " + pair.getTargetDatasource());
            return;
        }

        List<String> pkColumns = pair.getPrimaryKeyColumns();
        if (pkColumns == null || pkColumns.isEmpty()) {
            result.addError("Primary key columns not configured – cannot perform sample diff");
            return;
        }

        try {
            int limit = properties.getMaxSampleRecords();
            String orderBy = " ORDER BY " + pkColumns.stream().collect(Collectors.joining(", "));
            String limitClause = " FETCH FIRST " + limit + " ROWS ONLY";

            String sourceSql = "SELECT * FROM " + pair.getSourceTable() + orderBy + limitClause;
            String targetSql = "SELECT * FROM " + pair.getTargetTable() + orderBy + limitClause;

            List<Map<String, Object>> sourceRows = sourceJdbc.queryForList(sourceSql);
            List<Map<String, Object>> targetRows = targetJdbc.queryForList(targetSql);

            // Index target rows by PK for efficient lookup
            Map<String, Map<String, Object>> targetIndex = indexByPk(targetRows, pkColumns);

            List<RecordDiff> diffs = new ArrayList<>();
            for (Map<String, Object> sourceRow : sourceRows) {
                String pkKey = extractPkKey(sourceRow, pkColumns);
                Map<String, Object> targetRow = targetIndex.get(pkKey);

                if (targetRow == null) {
                    RecordDiff diff = new RecordDiff();
                    diff.setPrimaryKey(extractPkMap(sourceRow, pkColumns));
                    Map<String, FieldDiff> fieldDiffs = new LinkedHashMap<>();
                    fieldDiffs.put("_record", new FieldDiff("EXISTS", "MISSING"));
                    diff.setFieldDiffs(fieldDiffs);
                    diffs.add(diff);
                    continue;
                }

                Map<String, FieldDiff> fieldDiffs = compareRows(sourceRow, targetRow, pkColumns);
                if (!fieldDiffs.isEmpty()) {
                    RecordDiff diff = new RecordDiff();
                    diff.setPrimaryKey(extractPkMap(sourceRow, pkColumns));
                    diff.setFieldDiffs(fieldDiffs);
                    diffs.add(diff);
                }
            }

            result.setSampleDiffs(diffs);
            if (!diffs.isEmpty()) {
                result.markFailed();
                log.warn("Sample record diffs found for {}: {} differences", pair.getName(), diffs.size());
            } else {
                log.info("No sample record diffs for {}", pair.getName());
            }
        } catch (Exception e) {
            result.addError("Sample record diff failed: " + e.getMessage());
            log.error("Sample record diff error for {}", pair.getName(), e);
        }
    }

    // -- Private helpers ------------------------------------------------------

    private Map<String, Map<String, Object>> indexByPk(
            List<Map<String, Object>> rows, List<String> pkColumns) {
        Map<String, Map<String, Object>> index = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            index.put(extractPkKey(row, pkColumns), row);
        }
        return index;
    }

    private String extractPkKey(Map<String, Object> row, List<String> pkColumns) {
        StringBuilder sb = new StringBuilder();
        for (String col : pkColumns) {
            Object val = row.get(col);
            if (val == null) {
                // Try case-insensitive lookup
                val = row.entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase(col))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            }
            sb.append(val == null ? "NULL" : val.toString()).append("|");
        }
        return sb.toString();
    }

    private Map<String, Object> extractPkMap(Map<String, Object> row, List<String> pkColumns) {
        Map<String, Object> pk = new LinkedHashMap<>();
        for (String col : pkColumns) {
            Object val = row.get(col);
            if (val == null) {
                val = row.entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase(col))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            }
            pk.put(col, val);
        }
        return pk;
    }

    private Map<String, FieldDiff> compareRows(
            Map<String, Object> sourceRow,
            Map<String, Object> targetRow,
            List<String> pkColumns) {

        Map<String, FieldDiff> diffs = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : sourceRow.entrySet()) {
            String col = entry.getKey();
            // Skip PK columns – they were used for matching
            if (pkColumns.stream().anyMatch(pk -> pk.equalsIgnoreCase(col))) {
                continue;
            }
            Object sourceVal = entry.getValue();
            Object targetVal = findValueCaseInsensitive(targetRow, col);

            if (!Objects.equals(asString(sourceVal), asString(targetVal))) {
                diffs.put(col, new FieldDiff(sourceVal, targetVal));
            }
        }
        return diffs;
    }

    private Object findValueCaseInsensitive(Map<String, Object> row, String key) {
        Object val = row.get(key);
        if (val != null) {
            return val;
        }
        return row.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String asString(Object obj) {
        return obj == null ? null : obj.toString();
    }
}
