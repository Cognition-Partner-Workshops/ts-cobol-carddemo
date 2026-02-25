package com.carddemo.validation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carddemo.validation.model.ValidationReport;
import com.carddemo.validation.model.ValidationStatus;
import com.carddemo.validation.service.ValidationOrchestrator;

/**
 * REST controller that exposes the {@code /validate} endpoint.
 *
 * <p>Usage examples:
 * <pre>
 *   GET /validate              – validate all configured table pairs
 *   GET /validate?table=Card   – validate a single table pair by name
 * </pre>
 *
 * <p>Returns a JSON {@link ValidationReport} with:
 * <ul>
 *   <li>Overall status (PASS / FAIL / ERROR / SKIPPED)</li>
 *   <li>Summary counts</li>
 *   <li>Per-table results including row counts, checksums, sample diffs</li>
 *   <li>Duration in milliseconds</li>
 * </ul>
 */
@RestController
@RequestMapping("/validate")
public class ValidationController {

    private static final Logger log = LoggerFactory.getLogger(ValidationController.class);

    private final ValidationOrchestrator orchestrator;

    public ValidationController(ValidationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Run validation for all table pairs or a single table pair.
     *
     * @param table optional table pair name to validate individually
     * @return a {@link ValidationReport} with HTTP 200 (pass/skipped) or 409 (fail/error)
     */
    @GetMapping
    public ResponseEntity<ValidationReport> validate(
            @RequestParam(required = false) String table) {

        log.info("Validation requested{}", table != null ? " for table: " + table : " (all tables)");

        ValidationReport report;
        try {
            if (table != null && !table.isBlank()) {
                report = orchestrator.runValidationForTable(table);
            } else {
                report = orchestrator.runFullValidation();
            }
        } catch (Exception e) {
            log.error("Unexpected error during validation", e);
            report = new ValidationReport();
            report.addGlobalError("Unexpected error: " + e.getMessage());
            report.computeSummary();
        }

        // Use HTTP 200 for PASS/SKIPPED, 409 CONFLICT for FAIL/ERROR
        if (report.getOverallStatus() == ValidationStatus.FAIL
                || report.getOverallStatus() == ValidationStatus.ERROR) {
            return ResponseEntity.status(409).body(report);
        }
        return ResponseEntity.ok(report);
    }

    /**
     * Health check endpoint that returns the service status.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Data Validation Service is running");
    }
}
