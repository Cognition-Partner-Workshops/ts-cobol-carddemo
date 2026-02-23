package com.carddemo.api.controller;

import com.carddemo.api.dto.ReportRequest;
import com.carddemo.api.dto.TransactionResponse;
import com.carddemo.api.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Report REST controller.
 * Replaces CICS transaction CR00 (CORPT00C - Transaction Reports).
 *
 * COBOL → Java mapping:
 *   CR00 → POST /api/reports/transactions (Generate Transaction Report)
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report generation (replaces CICS CR00)")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/transactions")
    @Operation(summary = "Generate transaction report",
            description = "Generates a transaction report for a date range (replaces CR00)")
    public ResponseEntity<List<TransactionResponse>> generateTransactionReport(
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.ok(reportService.generateTransactionReport(request));
    }
}
