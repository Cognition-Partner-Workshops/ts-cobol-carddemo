package com.carddemo.validation.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.carddemo.validation.config.ValidationProperties.TablePairConfig;
import com.carddemo.validation.model.TableValidationResult;

/**
 * Compares row counts between a source and target table.
 */
@Component
public class RowCountValidator {

    private static final Logger log = LoggerFactory.getLogger(RowCountValidator.class);

    private final Map<String, JdbcTemplate> jdbcTemplateRegistry;

    public RowCountValidator(Map<String, JdbcTemplate> jdbcTemplateRegistry) {
        this.jdbcTemplateRegistry = jdbcTemplateRegistry;
    }

    /**
     * Execute a row-count comparison for the given table pair and populate
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
            String sourceCountSql = "SELECT COUNT(*) FROM " + pair.getSourceTable();
            String targetCountSql = "SELECT COUNT(*) FROM " + pair.getTargetTable();

            Long sourceCount = sourceJdbc.queryForObject(sourceCountSql, Long.class);
            Long targetCount = targetJdbc.queryForObject(targetCountSql, Long.class);

            result.setSourceRowCount(sourceCount);
            result.setTargetRowCount(targetCount);

            boolean match = sourceCount != null && sourceCount.equals(targetCount);
            result.setRowCountMatch(match);

            if (!match) {
                result.markFailed();
                log.warn("Row count mismatch for {}: source={} target={}",
                        pair.getName(), sourceCount, targetCount);
            } else {
                log.info("Row count match for {}: {}", pair.getName(), sourceCount);
            }
        } catch (Exception e) {
            result.addError("Row count validation failed: " + e.getMessage());
            log.error("Row count validation error for {}", pair.getName(), e);
        }
    }
}
