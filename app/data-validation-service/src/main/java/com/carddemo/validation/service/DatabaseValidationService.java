package com.carddemo.validation.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.carddemo.validation.config.ValidationProperties;
import com.carddemo.validation.config.ValidationProperties.TablePairConfig;
import com.carddemo.validation.model.RecordDiff;
import com.carddemo.validation.model.RecordDiff.FieldDiff;
import com.carddemo.validation.model.TableValidationResult;
import com.carddemo.validation.model.ValidationStatus;

/**
 * Performs database-to-database validation: compares legacy (DB2) source
 * tables against modernized PostgreSQL target tables.
 *
 * <p>Supports three validation rules:
 * <ul>
 *   <li>{@code ROW_COUNT} – total row count comparison</li>
 *   <li>{@code CHECKSUM}  – MD5 checksum of ordered concatenated rows</li>
 *   <li>{@code SAMPLE_DIFF} – field-by-field diff of a sample of records</li>
 * </ul>
 */
@Service
public class DatabaseValidationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseValidationService.class);

    private final JdbcTemplate postgresJdbc;
    private final JdbcTemplate legacyJdbc;
    private final ValidationProperties properties;

    public DatabaseValidationService(
            @Qualifier("postgresJdbcTemplate") JdbcTemplate postgresJdbc,
            @Qualifier("legacyJdbcTemplate") JdbcTemplate legacyJdbc,
            ValidationProperties properties) {
        this.postgresJdbc = postgresJdbc;
        this.legacyJdbc = legacyJdbc;
        this.properties = properties;
    }

    /**
     * Validate a single table pair using the rules specified in config.
     */
    public void validate(TablePairConfig pair, TableValidationResult result) {
        List<String> rules = pair.getRules();

        if (rules.contains("ROW_COUNT")) {
            validateRowCount(pair, result);
        }
        if (rules.contains("CHECKSUM")) {
            validateChecksum(pair, result);
        }
        if (rules.contains("SAMPLE_DIFF")) {
            validateSampleDiff(pair, result);
        }
    }

    // ── Row Count ────────────────────────────────────────────────────

    private void validateRowCount(TablePairConfig pair, TableValidationResult result) {
        try {
            long sourceCount = queryRowCount(legacyJdbc, pair.getSourceTable());
            long targetCount = queryRowCount(postgresJdbc, pair.getTargetTable());

            result.setSourceRowCount(sourceCount);
            result.setTargetRowCount(targetCount);
            result.setRowCountStatus(
                    sourceCount == targetCount ? ValidationStatus.PASS : ValidationStatus.FAIL
            );

            log.info("ROW_COUNT [{}]: source={} target={} status={}",
                    pair.getName(), sourceCount, targetCount, result.getRowCountStatus());
        } catch (DataAccessException e) {
            log.error("ROW_COUNT [{}]: error – {}", pair.getName(), e.getMessage(), e);
            result.setRowCountStatus(ValidationStatus.ERROR);
            result.addError("Row count error: " + e.getMessage());
        }
    }

    private long queryRowCount(JdbcTemplate jdbc, String table) {
        String sql = "SELECT COUNT(*) FROM " + sanitizeIdentifier(table);
        Long count = jdbc.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    // ── Checksum ─────────────────────────────────────────────────────

    private void validateChecksum(TablePairConfig pair, TableValidationResult result) {
        try {
            String sourceChecksum = computeTableChecksum(legacyJdbc, pair.getSourceTable(), pair.getPrimaryKey());
            String targetChecksum = computeTableChecksum(postgresJdbc, pair.getTargetTable(), pair.getPrimaryKey());

            result.setSourceChecksum(sourceChecksum);
            result.setTargetChecksum(targetChecksum);
            result.setChecksumStatus(
                    sourceChecksum.equals(targetChecksum) ? ValidationStatus.PASS : ValidationStatus.FAIL
            );

            log.info("CHECKSUM [{}]: source={} target={} status={}",
                    pair.getName(), sourceChecksum, targetChecksum, result.getChecksumStatus());
        } catch (DataAccessException e) {
            log.error("CHECKSUM [{}]: error – {}", pair.getName(), e.getMessage(), e);
            result.setChecksumStatus(ValidationStatus.ERROR);
            result.addError("Checksum error: " + e.getMessage());
        }
    }

    /**
     * Compute an MD5 checksum over all rows in the table, ordered by primary key.
     * Each row is represented as a pipe-delimited string of column values.
     */
    private String computeTableChecksum(JdbcTemplate jdbc, String table, String primaryKey) {
        String sql = "SELECT * FROM " + sanitizeIdentifier(table)
                + " ORDER BY " + sanitizeIdentifier(primaryKey);

        List<Map<String, Object>> rows = jdbc.queryForList(sql);

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (Map<String, Object> row : rows) {
                StringBuilder sb = new StringBuilder();
                for (Object value : row.values()) {
                    sb.append(value != null ? value.toString() : "NULL").append("|");
                }
                digest.update(sb.toString().getBytes());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    // ── Sample Diff ──────────────────────────────────────────────────

    private void validateSampleDiff(TablePairConfig pair, TableValidationResult result) {
        try {
            int limit = properties.getMaxSampleRecords();
            String pk = sanitizeIdentifier(pair.getPrimaryKey());

            String sourceSql = "SELECT * FROM " + sanitizeIdentifier(pair.getSourceTable())
                    + " ORDER BY " + pk + " LIMIT " + limit;
            String targetSql = "SELECT * FROM " + sanitizeIdentifier(pair.getTargetTable())
                    + " ORDER BY " + pk + " LIMIT " + limit;

            List<Map<String, Object>> sourceRows = legacyJdbc.queryForList(sourceSql);
            List<Map<String, Object>> targetRows = postgresJdbc.queryForList(targetSql);

            // Index target rows by primary key for efficient lookup
            Map<String, Map<String, Object>> targetIndex = new LinkedHashMap<>();
            for (Map<String, Object> row : targetRows) {
                Object pkValue = row.get(pair.getPrimaryKey());
                if (pkValue != null) {
                    targetIndex.put(pkValue.toString(), row);
                }
            }

            List<RecordDiff> diffs = new ArrayList<>();
            boolean allMatch = true;

            for (Map<String, Object> sourceRow : sourceRows) {
                Object pkValue = sourceRow.get(pair.getPrimaryKey());
                String pkStr = pkValue != null ? pkValue.toString() : "UNKNOWN";

                Map<String, Object> targetRow = targetIndex.get(pkStr);
                if (targetRow == null) {
                    // Record exists in source but not in target
                    RecordDiff diff = new RecordDiff(pkStr, sourceRow, null, Map.of());
                    diffs.add(diff);
                    allMatch = false;
                    continue;
                }

                // Compare field by field
                Map<String, FieldDiff> fieldDiffs = new HashMap<>();
                for (Map.Entry<String, Object> entry : sourceRow.entrySet()) {
                    String field = entry.getKey();
                    Object sourceVal = entry.getValue();
                    Object targetVal = targetRow.get(field);

                    String srcStr = sourceVal != null ? sourceVal.toString() : "NULL";
                    String tgtStr = targetVal != null ? targetVal.toString() : "NULL";

                    if (!srcStr.equals(tgtStr)) {
                        fieldDiffs.put(field, new FieldDiff(sourceVal, targetVal));
                    }
                }

                if (!fieldDiffs.isEmpty()) {
                    diffs.add(new RecordDiff(pkStr, sourceRow, targetRow, fieldDiffs));
                    allMatch = false;
                }
            }

            result.setSampleDiffs(diffs);
            result.setSampleDiffStatus(allMatch ? ValidationStatus.PASS : ValidationStatus.FAIL);

            log.info("SAMPLE_DIFF [{}]: compared={} diffs={} status={}",
                    pair.getName(), sourceRows.size(), diffs.size(), result.getSampleDiffStatus());
        } catch (DataAccessException e) {
            log.error("SAMPLE_DIFF [{}]: error – {}", pair.getName(), e.getMessage(), e);
            result.setSampleDiffStatus(ValidationStatus.ERROR);
            result.addError("Sample diff error: " + e.getMessage());
        }
    }

    // ── Utility ──────────────────────────────────────────────────────

    /**
     * Basic identifier sanitization – strips everything except alphanumeric
     * characters and underscores to prevent SQL injection.
     */
    private String sanitizeIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
