package com.carddemo.report.controller;

import com.carddemo.common.dto.ApiResponse;
import com.carddemo.report.dto.AccountStatementReport;
import com.carddemo.report.dto.TransactionSummaryReport;
import com.carddemo.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reporting & Analytics", description = "Report generation operations - EPIC-006")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/transaction-summary/{accountId}")
    @Operation(summary = "Generate transaction summary", description = "Generate transaction summary report for an account (US-006-01-01)")
    public ResponseEntity<ApiResponse<TransactionSummaryReport>> getTransactionSummary(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        TransactionSummaryReport report = reportService.generateTransactionSummary(accountId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/statement/{accountId}")
    @Operation(summary = "Generate account statement", description = "Generate monthly account statement (US-006-01-02, US-006-02-01)")
    public ResponseEntity<ApiResponse<AccountStatementReport>> getAccountStatement(
            @PathVariable String accountId,
            @RequestParam int month,
            @RequestParam int year) {
        AccountStatementReport report = reportService.generateAccountStatement(accountId, month, year);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
