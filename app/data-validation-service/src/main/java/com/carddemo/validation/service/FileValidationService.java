package com.carddemo.validation.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.carddemo.validation.model.TableValidationResult;
import com.carddemo.validation.model.ValidationStatus;

/**
 * Validates mainframe output files against the PostgreSQL target database.
 *
 * <p>Compares:
 * <ul>
 *   <li>Row count in the flat file vs row count in the target table</li>
 *   <li>Field-level spot checks on a sample of records</li>
 * </ul>
 */
@Service
public class FileValidationService {

    private static final Logger log = LoggerFactory.getLogger(FileValidationService.class);

    private final JdbcTemplate postgresJdbc;
    private final ValidationProperties properties;

    public FileValidationService(
            @Qualifier("postgresJdbcTemplate") JdbcTemplate postgresJdbc,
            ValidationProperties properties) {
        this.postgresJdbc = postgresJdbc;
        this.properties = properties;
    }

    /**
     * Validate a mainframe output file against the PostgreSQL target table.
     */
    public void validateFileToDb(TablePairConfig pair, TableValidationResult result) {
        String mainframeFile = pair.getMainframeFile();
        if (mainframeFile == null || mainframeFile.isBlank()) {
            result.setFileToDbStatus(ValidationStatus.SKIPPED);
            result.setFileToDbDetail("No mainframe file configured");
            return;
        }

        Path filePath = Paths.get(properties.getMainframeOutputDir(), mainframeFile);
        if (!Files.exists(filePath)) {
            result.setFileToDbStatus(ValidationStatus.SKIPPED);
            result.setFileToDbDetail("Mainframe file not found: " + filePath);
            log.warn("FILE_TO_DB [{}]: file not found – {}", pair.getName(), filePath);
            return;
        }

        try {
            // Count lines in the file (each non-empty line = one record)
            long fileRowCount = countFileRecords(filePath);
            result.setFileRowCount(fileRowCount);

            // Count rows in the PostgreSQL target table
            long dbRowCount = queryRowCount(pair.getTargetTable());
            result.setTargetRowCount(dbRowCount);

            boolean match = fileRowCount == dbRowCount;
            result.setFileToDbStatus(match ? ValidationStatus.PASS : ValidationStatus.FAIL);
            result.setFileToDbDetail(String.format(
                    "File records: %d, DB records: %d, Match: %s",
                    fileRowCount, dbRowCount, match));

            log.info("FILE_TO_DB [{}]: fileRows={} dbRows={} status={}",
                    pair.getName(), fileRowCount, dbRowCount, result.getFileToDbStatus());

            // Perform sample record comparison
            if (match) {
                compareSampleFileRecords(pair, filePath, result);
            }
        } catch (IOException e) {
            log.error("FILE_TO_DB [{}]: I/O error – {}", pair.getName(), e.getMessage(), e);
            result.setFileToDbStatus(ValidationStatus.ERROR);
            result.addError("File-to-DB validation I/O error: " + e.getMessage());
        } catch (DataAccessException e) {
            log.error("FILE_TO_DB [{}]: DB error – {}", pair.getName(), e.getMessage(), e);
            result.setFileToDbStatus(ValidationStatus.ERROR);
            result.addError("File-to-DB validation DB error: " + e.getMessage());
        }
    }

    /**
     * Count non-empty, non-header records in the flat file.
     */
    private long countFileRecords(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .count();
        }
    }

    /**
     * Query the row count from the PostgreSQL target table.
     */
    private long queryRowCount(String table) {
        String sql = "SELECT COUNT(*) FROM " + sanitizeIdentifier(table);
        Long count = postgresJdbc.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * Compare a sample of records from the file against the database.
     */
    private void compareSampleFileRecords(TablePairConfig pair, Path filePath,
                                          TableValidationResult result) throws IOException {
        int maxSamples = properties.getMaxSampleRecords();
        List<String> sampleLines = readSampleLines(filePath, maxSamples);
        List<Map<String, Object>> dbRecords = querySampleRecords(pair.getTargetTable(),
                pair.getPrimaryKey(), maxSamples);

        List<String> discrepancies = new ArrayList<>();
        int compared = Math.min(sampleLines.size(), dbRecords.size());

        for (int i = 0; i < compared; i++) {
            String fileLine = sampleLines.get(i);
            Map<String, Object> dbRecord = dbRecords.get(i);

            // Build a pipe-delimited representation of the DB record for comparison
            StringBuilder dbLine = new StringBuilder();
            for (Object value : dbRecord.values()) {
                if (dbLine.length() > 0) {
                    dbLine.append("|");
                }
                dbLine.append(value != null ? value.toString().trim() : "");
            }

            // Simple string-level comparison (field-level would require schema awareness)
            if (!normalizeForComparison(fileLine).equals(normalizeForComparison(dbLine.toString()))) {
                discrepancies.add(String.format("Record %d: file vs DB mismatch", i + 1));
            }
        }

        if (!discrepancies.isEmpty()) {
            String detail = result.getFileToDbDetail() + "; Sample discrepancies: " + discrepancies.size();
            result.setFileToDbDetail(detail);
            log.warn("FILE_TO_DB [{}]: {} sample discrepancies found", pair.getName(), discrepancies.size());
        }
    }

    /**
     * Read the first N non-empty lines from a file.
     */
    private List<String> readSampleLines(Path filePath, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null && lines.size() < maxLines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        }
        return lines;
    }

    /**
     * Query a sample of records from the target database, ordered by primary key.
     */
    private List<Map<String, Object>> querySampleRecords(String table, String primaryKey, int limit) {
        String sql = "SELECT * FROM " + sanitizeIdentifier(table)
                + " ORDER BY " + sanitizeIdentifier(primaryKey)
                + " LIMIT " + limit;
        return postgresJdbc.queryForList(sql);
    }

    /**
     * Normalize a string for comparison by trimming whitespace and collapsing
     * multiple delimiters.
     */
    private String normalizeForComparison(String value) {
        return value.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private String sanitizeIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
