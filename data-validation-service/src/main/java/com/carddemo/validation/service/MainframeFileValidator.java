package com.carddemo.validation.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.carddemo.validation.config.ValidationProperties;
import com.carddemo.validation.config.ValidationProperties.TablePairConfig;
import com.carddemo.validation.model.TableValidationResult;
import com.carddemo.validation.model.TableValidationResult.FieldDiff;
import com.carddemo.validation.model.TableValidationResult.MainframeFileComparisonResult;
import com.carddemo.validation.model.TableValidationResult.RecordDiff;

/**
 * Compares records from a mainframe flat-file against the target Postgres
 * table.
 *
 * <p>The flat-file is assumed to be pipe-delimited ({@code |}) with the
 * first line as a header row. Override the delimiter via the
 * {@code MAINFRAME_FILE_DELIMITER} environment variable if needed.</p>
 */
@Component
public class MainframeFileValidator {

    private static final Logger log = LoggerFactory.getLogger(MainframeFileValidator.class);
    private static final String DEFAULT_DELIMITER = "\\|";

    private final Map<String, JdbcTemplate> jdbcTemplateRegistry;
    private final ValidationProperties properties;

    public MainframeFileValidator(Map<String, JdbcTemplate> jdbcTemplateRegistry,
                                  ValidationProperties properties) {
        this.jdbcTemplateRegistry = jdbcTemplateRegistry;
        this.properties = properties;
    }

    /**
     * Compare the mainframe output file against the target database table.
     */
    public void validate(TablePairConfig pair, TableValidationResult result) {
        String filePath = pair.getMainframeFilePath();
        if (filePath == null || filePath.isBlank()) {
            result.addError("Mainframe file path not configured for " + pair.getName());
            return;
        }

        JdbcTemplate targetJdbc = jdbcTemplateRegistry.get(pair.getTargetDatasource());
        if (targetJdbc == null) {
            result.addError("Target datasource not found: " + pair.getTargetDatasource());
            return;
        }

        try {
            MainframeFileComparisonResult comparison = new MainframeFileComparisonResult();
            List<Map<String, String>> fileRecords = readFlatFile(Paths.get(filePath));
            comparison.setFileRecordCount(fileRecords.size());

            String countSql = "SELECT COUNT(*) FROM " + pair.getTargetTable();
            Long dbCount = targetJdbc.queryForObject(countSql, Long.class);
            comparison.setDbRecordCount(dbCount != null ? dbCount : 0);
            comparison.setRecordCountMatch(fileRecords.size() == (dbCount != null ? dbCount : 0));

            // Sample diffs – compare first N records
            List<String> pkColumns = pair.getPrimaryKeyColumns();
            if (pkColumns != null && !pkColumns.isEmpty()) {
                int limit = properties.getMaxSampleRecords();
                String sql = "SELECT * FROM " + pair.getTargetTable() +
                        " ORDER BY " + String.join(", ", pkColumns) +
                        " FETCH FIRST " + limit + " ROWS ONLY";
                List<Map<String, Object>> dbRows = targetJdbc.queryForList(sql);

                List<RecordDiff> diffs = compareFileToDb(
                        fileRecords.subList(0, Math.min(limit, fileRecords.size())),
                        dbRows,
                        pkColumns);
                comparison.setSampleDiffs(diffs);
            }

            if (!comparison.isRecordCountMatch() || !comparison.getSampleDiffs().isEmpty()) {
                result.markFailed();
            }

            result.setMainframeFileComparison(comparison);
            log.info("Mainframe file comparison for {}: file={} db={} match={}",
                    pair.getName(), fileRecords.size(), dbCount,
                    comparison.isRecordCountMatch());
        } catch (Exception e) {
            result.addError("Mainframe file comparison failed: " + e.getMessage());
            log.error("Mainframe file comparison error for {}", pair.getName(), e);
        }
    }

    // -- Private helpers ------------------------------------------------------

    /**
     * Read a pipe-delimited flat file with a header row.
     */
    private List<Map<String, String>> readFlatFile(Path path) throws IOException {
        String delimiter = System.getenv("MAINFRAME_FILE_DELIMITER");
        if (delimiter == null || delimiter.isBlank()) {
            delimiter = DEFAULT_DELIMITER;
        }

        List<Map<String, String>> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return records;
            }
            String[] headers = headerLine.split(delimiter);
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(delimiter, -1);
                Map<String, String> record = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    record.put(headers[i], values[i].trim());
                }
                records.add(record);
            }
        }
        return records;
    }

    private List<RecordDiff> compareFileToDb(
            List<Map<String, String>> fileRecords,
            List<Map<String, Object>> dbRows,
            List<String> pkColumns) {

        // Index DB rows by PK
        Map<String, Map<String, Object>> dbIndex = new LinkedHashMap<>();
        for (Map<String, Object> row : dbRows) {
            String key = buildPkKey(row, pkColumns);
            dbIndex.put(key, row);
        }

        List<RecordDiff> diffs = new ArrayList<>();
        for (Map<String, String> fileRec : fileRecords) {
            String key = buildPkKeyFromStrings(fileRec, pkColumns);
            Map<String, Object> dbRow = dbIndex.get(key);

            if (dbRow == null) {
                RecordDiff diff = new RecordDiff();
                diff.setPrimaryKey(buildPkMapFromStrings(fileRec, pkColumns));
                Map<String, FieldDiff> fieldDiffs = new LinkedHashMap<>();
                fieldDiffs.put("_record", new FieldDiff("EXISTS_IN_FILE", "MISSING_IN_DB"));
                diff.setFieldDiffs(fieldDiffs);
                diffs.add(diff);
                continue;
            }

            Map<String, FieldDiff> fieldDiffs = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : fileRec.entrySet()) {
                if (pkColumns.stream().anyMatch(pk -> pk.equalsIgnoreCase(entry.getKey()))) {
                    continue;
                }
                String fileVal = entry.getValue();
                Object dbVal = findCaseInsensitive(dbRow, entry.getKey());
                String dbStr = dbVal == null ? null : dbVal.toString();
                if (!Objects.equals(fileVal, dbStr)) {
                    fieldDiffs.put(entry.getKey(), new FieldDiff(fileVal, dbStr));
                }
            }
            if (!fieldDiffs.isEmpty()) {
                RecordDiff diff = new RecordDiff();
                diff.setPrimaryKey(buildPkMapFromStrings(fileRec, pkColumns));
                diff.setFieldDiffs(fieldDiffs);
                diffs.add(diff);
            }
        }
        return diffs;
    }

    private String buildPkKey(Map<String, Object> row, List<String> pkColumns) {
        StringBuilder sb = new StringBuilder();
        for (String col : pkColumns) {
            Object val = findCaseInsensitive(row, col);
            sb.append(val == null ? "NULL" : val.toString()).append("|");
        }
        return sb.toString();
    }

    private String buildPkKeyFromStrings(Map<String, String> row, List<String> pkColumns) {
        StringBuilder sb = new StringBuilder();
        for (String col : pkColumns) {
            String val = findCaseInsensitiveStr(row, col);
            sb.append(val == null ? "NULL" : val).append("|");
        }
        return sb.toString();
    }

    private Map<String, Object> buildPkMapFromStrings(Map<String, String> row, List<String> pkColumns) {
        Map<String, Object> pk = new LinkedHashMap<>();
        for (String col : pkColumns) {
            pk.put(col, findCaseInsensitiveStr(row, col));
        }
        return pk;
    }

    private Object findCaseInsensitive(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val != null) {
            return val;
        }
        return map.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String findCaseInsensitiveStr(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val != null) {
            return val;
        }
        return map.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
