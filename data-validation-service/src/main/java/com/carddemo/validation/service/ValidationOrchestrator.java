package com.carddemo.validation.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.carddemo.validation.config.ValidationProperties;
import com.carddemo.validation.config.ValidationProperties.MainframeApiConfig;
import com.carddemo.validation.config.ValidationProperties.TablePairConfig;
import com.carddemo.validation.config.ValidationProperties.ValidationRules;
import com.carddemo.validation.model.MainframeApiValidationResult;
import com.carddemo.validation.model.TableValidationResult;
import com.carddemo.validation.model.ValidationReport;

/**
 * Orchestrates a full validation run across all configured table pairs and
 * mainframe-API comparisons.
 *
 * <p>For each table pair the following checks are executed based on the
 * rules configuration:</p>
 * <ol>
 *   <li>Row-count comparison (DB2/legacy vs Postgres)</li>
 *   <li>Aggregate checksum comparison</li>
 *   <li>Sample record field-level diff</li>
 *   <li>Mainframe flat-file vs Postgres comparison</li>
 * </ol>
 *
 * <p>Additionally, any configured mainframe-to-API comparisons are
 * executed.</p>
 */
@Service
public class ValidationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ValidationOrchestrator.class);

    private final ValidationProperties properties;
    private final RowCountValidator rowCountValidator;
    private final ChecksumValidator checksumValidator;
    private final SampleRecordDiffValidator sampleRecordDiffValidator;
    private final MainframeFileValidator mainframeFileValidator;
    private final MainframeApiValidator mainframeApiValidator;

    public ValidationOrchestrator(ValidationProperties properties,
                                   RowCountValidator rowCountValidator,
                                   ChecksumValidator checksumValidator,
                                   SampleRecordDiffValidator sampleRecordDiffValidator,
                                   MainframeFileValidator mainframeFileValidator,
                                   MainframeApiValidator mainframeApiValidator) {
        this.properties = properties;
        this.rowCountValidator = rowCountValidator;
        this.checksumValidator = checksumValidator;
        this.sampleRecordDiffValidator = sampleRecordDiffValidator;
        this.mainframeFileValidator = mainframeFileValidator;
        this.mainframeApiValidator = mainframeApiValidator;
    }

    /**
     * Run all configured validations and return a consolidated report.
     *
     * @return the complete {@link ValidationReport}
     */
    public ValidationReport runValidation() {
        ValidationReport report = new ValidationReport();

        if (!properties.isEnabled()) {
            log.info("Validation is disabled – returning empty report");
            report.setOverallStatus(ValidationReport.OverallStatus.SKIPPED);
            return report;
        }

        // -- Table-pair validations -------------------------------------------
        List<TablePairConfig> tablePairs = properties.getTablePairs();
        log.info("Starting validation for {} table pair(s)", tablePairs.size());

        for (TablePairConfig pair : tablePairs) {
            TableValidationResult tableResult = validateTablePair(pair);
            report.addTableResult(tableResult);
        }

        // -- Mainframe-to-API comparisons ------------------------------------
        List<MainframeApiConfig> apiComparisons = properties.getMainframeApiComparisons();
        if (apiComparisons != null && !apiComparisons.isEmpty()) {
            log.info("Starting {} mainframe-API comparison(s)", apiComparisons.size());
            for (MainframeApiConfig config : apiComparisons) {
                try {
                    MainframeApiValidationResult apiResult = mainframeApiValidator.validate(config);
                    report.addMainframeApiResult(apiResult);
                } catch (Exception e) {
                    report.addError("Mainframe-API comparison '" + config.getName()
                            + "' failed: " + e.getMessage());
                    log.error("Mainframe-API comparison error for '{}'", config.getName(), e);
                }
            }
        }

        log.info("Validation complete. Overall status: {}", report.getOverallStatus());
        return report;
    }

    /**
     * Validate a single table pair according to its configured rules.
     */
    private TableValidationResult validateTablePair(TablePairConfig pair) {
        TableValidationResult result = new TableValidationResult();
        result.setTablePairName(pair.getName());
        result.setSourceTable(pair.getSourceTable());
        result.setTargetTable(pair.getTargetTable());

        ValidationRules rules = pair.getRules();
        log.info("Validating table pair '{}' [rowCount={}, checksum={}, sampleDiff={}, mfFile={}]",
                pair.getName(), rules.isRowCount(), rules.isChecksum(),
                rules.isSampleRecordDiff(), rules.isMainframeFileComparison());

        try {
            if (rules.isRowCount()) {
                rowCountValidator.validate(pair, result);
            }
            if (rules.isChecksum()) {
                checksumValidator.validate(pair, result);
            }
            if (rules.isSampleRecordDiff()) {
                sampleRecordDiffValidator.validate(pair, result);
            }
            if (rules.isMainframeFileComparison()) {
                mainframeFileValidator.validate(pair, result);
            }
        } catch (Exception e) {
            result.addError("Unexpected error validating '" + pair.getName() + "': " + e.getMessage());
            log.error("Unexpected validation error for '{}'", pair.getName(), e);
        }

        return result;
    }
}
