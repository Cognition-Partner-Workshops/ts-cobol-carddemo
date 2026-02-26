package com.carddemo.validation.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.carddemo.validation.config.ValidationProperties.TablePairConfig;
import com.carddemo.validation.model.TableValidationResult;

/**
 * Computes and compares aggregate checksums for a source/target table pair.
 *
 * <p>The checksum is computed by hashing the concatenated string
 * representation of all rows (ordered by primary key). If no
 * {@code checksumColumns} are configured, all columns are included.</p>
 */
@Component
public class ChecksumValidator {

    private static final Logger log = LoggerFactory.getLogger(ChecksumValidator.class);

    private final Map<String, JdbcTemplate> jdbcTemplateRegistry;

    public ChecksumValidator(Map<String, JdbcTemplate> jdbcTemplateRegistry) {
        this.jdbcTemplateRegistry = jdbcTemplateRegistry;
    }

    /**
     * Execute a checksum comparison for the given table pair and populate
     * the result object.
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

        try {
            String columns = buildColumnList(pair);
            String orderBy = buildOrderByClause(pair);

            String sourceSql = "SELECT " + columns + " FROM " + pair.getSourceTable() + orderBy;
            String targetSql = "SELECT " + columns + " FROM " + pair.getTargetTable() + orderBy;

            String sourceChecksum = computeChecksum(sourceJdbc, sourceSql);
            String targetChecksum = computeChecksum(targetJdbc, targetSql);

            result.setSourceChecksum(sourceChecksum);
            result.setTargetChecksum(targetChecksum);

            boolean match = sourceChecksum.equals(targetChecksum);
            result.setChecksumMatch(match);

            if (!match) {
                result.markFailed();
                log.warn("Checksum mismatch for {}: source={} target={}",
                        pair.getName(), sourceChecksum, targetChecksum);
            } else {
                log.info("Checksum match for {}: {}", pair.getName(), sourceChecksum);
            }
        } catch (Exception e) {
            result.addError("Checksum validation failed: " + e.getMessage());
            log.error("Checksum validation error for {}", pair.getName(), e);
        }
    }

    // -- Private helpers ------------------------------------------------------

    private String buildColumnList(TablePairConfig pair) {
        List<String> cols = pair.getChecksumColumns();
        if (cols == null || cols.isEmpty()) {
            return "*";
        }
        return String.join(", ", cols);
    }

    private String buildOrderByClause(TablePairConfig pair) {
        List<String> pk = pair.getPrimaryKeyColumns();
        if (pk == null || pk.isEmpty()) {
            return "";
        }
        return " ORDER BY " + pk.stream().collect(Collectors.joining(", "));
    }

    /**
     * Execute the query, concatenate all result rows into a single string,
     * and return the SHA-256 hex digest.
     */
    private String computeChecksum(JdbcTemplate jdbc, String sql) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }

        for (Map<String, Object> row : rows) {
            StringBuilder sb = new StringBuilder();
            for (Object value : row.values()) {
                sb.append(value == null ? "NULL" : value.toString());
                sb.append('|');
            }
            digest.update(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        return HexFormat.of().formatHex(digest.digest());
    }
}
