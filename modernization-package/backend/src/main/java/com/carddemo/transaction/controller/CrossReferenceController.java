package com.carddemo.transaction.controller;

import com.carddemo.transaction.dto.CrossReferenceResponse;
import com.carddemo.transaction.service.CrossReferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Cross-Reference Resolution endpoint.
 * Replaces legacy CXACAIX (Alternate Index) and CCXREF (KSDS) VSAM lookups.
 * Business Rules: BR-AT-04, BR-AT-05
 */
@RestController
@RequestMapping("/api/v1")
public class CrossReferenceController {

    private final CrossReferenceService crossReferenceService;

    public CrossReferenceController(CrossReferenceService crossReferenceService) {
        this.crossReferenceService = crossReferenceService;
    }

    /**
     * Bidirectional resolution of Account ID to Card Number (and vice versa).
     *
     * Path A: Provide accountId -> returns associated Card Number
     * Path B: Provide cardNumber -> returns associated Account ID
     *
     * Exactly one of accountId or cardNumber must be provided.
     */
    @GetMapping("/cross-references/resolve")
    public ResponseEntity<CrossReferenceResponse> resolveCrossReference(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String cardNumber) {
        CrossReferenceResponse response = crossReferenceService.resolve(accountId, cardNumber);
        return ResponseEntity.ok(response);
    }
}
