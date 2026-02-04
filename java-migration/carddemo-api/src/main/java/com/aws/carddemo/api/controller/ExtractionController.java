package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.extraction.AccountExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/extractions")
@RequiredArgsConstructor
@Tag(name = "Extractions", description = "Account extraction endpoints - migrated from CODATE01 (CDRD) and COACCT01 (CDRA)")
public class ExtractionController {

    private final AccountExtractionService extractionService;

    @GetMapping("/system/date")
    @Operation(summary = "Get system date - migrated from CODATE01 (CDRD)")
    public ResponseEntity<AccountExtractionService.SystemDateResponse> getSystemDate() {
        return ResponseEntity.ok(extractionService.getSystemDate());
    }

    @GetMapping("/accounts/{accountId}")
    @Operation(summary = "Account inquiry - migrated from COACCT01 (CDRA)")
    public ResponseEntity<AccountExtractionService.AccountInquiryResponse> getAccountInquiry(
            @PathVariable Long accountId) {
        return extractionService.getAccountInquiry(accountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/accounts")
    @Operation(summary = "Extract accounts with criteria")
    public ResponseEntity<Page<AccountExtractionService.AccountExtract>> extractAccounts(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) Boolean activeOnly,
            Pageable pageable) {
        AccountExtractionService.AccountExtractionCriteria criteria = 
                AccountExtractionService.AccountExtractionCriteria.builder()
                        .groupId(groupId)
                        .activeOnly(activeOnly)
                        .build();
        return ResponseEntity.ok(extractionService.extractAccounts(criteria, pageable));
    }

    @GetMapping("/accounts/over-limit")
    @Operation(summary = "Extract over-limit accounts")
    public ResponseEntity<List<AccountExtractionService.AccountExtract>> extractOverLimitAccounts() {
        return ResponseEntity.ok(extractionService.extractOverLimitAccounts());
    }

    @GetMapping("/accounts/expiring")
    @Operation(summary = "Extract accounts expiring within specified days")
    public ResponseEntity<List<AccountExtractionService.AccountExtract>> extractExpiringAccounts(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(extractionService.extractExpiringAccounts(days));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get extraction summary")
    public ResponseEntity<AccountExtractionService.ExtractionSummary> getExtractionSummary() {
        return ResponseEntity.ok(extractionService.getExtractionSummary());
    }
}
