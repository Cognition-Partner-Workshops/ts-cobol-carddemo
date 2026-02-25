package com.carddemo.controller;

import com.carddemo.entity.AuthorizationDetail;
import com.carddemo.entity.AuthorizationSummary;
import com.carddemo.service.AuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authorization controller - migrated from Phase 5b:
 *   COPAUS0C (CPVS - Authorization Summary view)
 *   COPAUS1C (CPVD - Authorization Details view)
 *   COPAUS2C (fraud marking)
 */
@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * GET /api/authorizations/summary?acctId=... - View authorization summary (CPVS).
     * Replaces COPAUS0C IMS/VSAM read with JPA query.
     */
    @GetMapping("/summary")
    public ResponseEntity<AuthorizationSummary> getSummary(@RequestParam Long acctId) {
        AuthorizationSummary summary = authorizationService.getSummary(acctId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No authorization summary found for account"));
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/authorizations/details?summaryId=... - View authorization details (CPVD).
     * Replaces COPAUS1C IMS child segment read with JPA query.
     */
    @GetMapping("/details")
    public ResponseEntity<Page<AuthorizationDetail>> getDetails(
            @RequestParam Long summaryId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(authorizationService.getDetails(summaryId, pageable));
    }

    /**
     * POST /api/authorizations/{detailId}/fraud - Mark authorization as fraud.
     * Replaces COPAUS2C fraud marking logic with DB2 insert.
     */
    @PostMapping("/{detailId}/fraud")
    public ResponseEntity<Void> markAsFraud(@PathVariable Long detailId) {
        authorizationService.markAsFraud(detailId);
        return ResponseEntity.ok().build();
    }
}
