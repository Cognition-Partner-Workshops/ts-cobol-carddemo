package com.carddemo.validation.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.carddemo.validation.config.ValidationProperties;
import com.carddemo.validation.config.ValidationProperties.TablePairConfig;
import com.carddemo.validation.model.TableValidationResult;
import com.carddemo.validation.model.ValidationReport;

/**
 * Orchestrates the full validation process across all configured table pairs.
 *
 * <p>For each table pair, the orchestrator runs:
 * <ol>
 *   <li>Database-to-database validation (DB2 vs PostgreSQL)</li>
 *   <li>File-to-database validation (mainframe file vs PostgreSQL)</li>
 *   <li>File-to-API validation (mainframe file vs microservice API)</li>
 * </ol>
 *
 * Results are aggregated into a single {@link ValidationReport}.
 */
@Service
public class ValidationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ValidationOrchestrator.class);

    private final ValidationProperties properties;
    private final DatabaseValidationService databaseValidator;
    private final FileValidationService fileValidator;
    private final ApiValidationService apiValidator;

    public ValidationOrchestrator(ValidationProperties properties,
                                  DatabaseValidationService databaseValidator,
                                  FileValidationService fileValidator,
                                  ApiValidationService apiValidator) {
        this.properties = properties;
        this.databaseValidator = databaseValidator;
        this.fileValidator = fileValidator;
        this.apiValidator = apiValidator;
    }

    /**
     * Run the full validation suite for all configured table pairs.
     *
     * @return a complete {@link ValidationReport}
     */
    public ValidationReport runFullValidation() {
        Instant start = Instant.now();
        ValidationReport report = new ValidationReport();

        log.info("Starting full validation for {} table pair(s)",
                properties.getTablePairs().size());

        for (TablePairConfig pair : properties.getTablePairs()) {
            TableValidationResult result = validateTablePair(pair);
            report.addTableResult(result);
        }

        report.setDurationMillis(Duration.between(start, Instant.now()).toMillis());
        report.computeSummary();

        log.info("Validation complete: overall={} passed={} failed={} errors={} skipped={} duration={}ms",
                report.getOverallStatus(),
                report.getPassedCount(),
                report.getFailedCount(),
                report.getErrorCount(),
                report.getSkippedCount(),
                report.getDurationMillis());

        return report;
    }

    /**
     * Run validation for a single table pair identified by name.
     *
     * @param tablePairName the name of the table pair (as defined in config)
     * @return a {@link ValidationReport} containing the single table result
     */
    public ValidationReport runValidationForTable(String tablePairName) {
        Instant start = Instant.now();
        ValidationReport report = new ValidationReport();

        TablePairConfig pair = properties.getTablePairs().stream()
                .filter(p -> p.getName().equalsIgnoreCase(tablePairName))
                .findFirst()
                .orElse(null);

        if (pair == null) {
            report.addGlobalError("Table pair not found: " + tablePairName);
            report.computeSummary();
            return report;
        }

        TableValidationResult result = validateTablePair(pair);
        report.addTableResult(result);

        report.setDurationMillis(Duration.between(start, Instant.now()).toMillis());
        report.computeSummary();

        return report;
    }

    /**
     * Validate a single table pair through all three validation types.
     */
    private TableValidationResult validateTablePair(TablePairConfig pair) {
        log.info("Validating table pair: {} (source={} -> target={})",
                pair.getName(), pair.getSourceTable(), pair.getTargetTable());

        TableValidationResult result = new TableValidationResult(
                pair.getName(), pair.getSourceTable(), pair.getTargetTable());

        // 1. Database-to-database validation
        try {
            databaseValidator.validate(pair, result);
        } catch (Exception e) {
            log.error("DB validation failed for {}: {}", pair.getName(), e.getMessage(), e);
            result.addError("Database validation error: " + e.getMessage());
        }

        // 2. File-to-database validation
        try {
            fileValidator.validateFileToDb(pair, result);
        } catch (Exception e) {
            log.error("File-to-DB validation failed for {}: {}", pair.getName(), e.getMessage(), e);
            result.addError("File-to-DB validation error: " + e.getMessage());
        }

        // 3. File-to-API validation
        try {
            apiValidator.validateFileToApi(pair, result);
        } catch (Exception e) {
            log.error("File-to-API validation failed for {}: {}", pair.getName(), e.getMessage(), e);
            result.addError("File-to-API validation error: " + e.getMessage());
        }

        log.info("Table pair {} overall status: {}", pair.getName(), result.getOverallStatus());
        return result;
    }
}
