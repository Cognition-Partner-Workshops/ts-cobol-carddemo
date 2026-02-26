package com.carddemo.validation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carddemo.validation.model.ValidationReport;
import com.carddemo.validation.service.ValidationOrchestrator;

/**
 * REST controller exposing the {@code /validate} endpoint.
 *
 * <p>Triggers a full validation run comparing DB2/legacy data against
 * Postgres targets, mainframe output files against Postgres data, and
 * mainframe output files against microservice API responses.</p>
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
     * Execute all configured validations and return a comprehensive report.
     *
     * @return 200 OK with the {@link ValidationReport} body
     */
    @GetMapping
    public ResponseEntity<ValidationReport> validate() {
        log.info("Received validation request");
        ValidationReport report = orchestrator.runValidation();
        log.info("Validation completed with status: {}", report.getOverallStatus());
        return ResponseEntity.ok(report);
    }
}
