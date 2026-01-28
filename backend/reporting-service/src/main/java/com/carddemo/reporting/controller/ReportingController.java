package com.carddemo.reporting.controller;

import com.carddemo.common.dto.ApiResponse;
import com.carddemo.reporting.dto.AccountStatementDto;
import com.carddemo.reporting.dto.DashboardSummaryDto;
import com.carddemo.reporting.dto.TransactionReportDto;
import com.carddemo.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Reporting and analytics endpoints")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary", description = "Get summary statistics for the dashboard")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary() {
        DashboardSummaryDto summary = reportingService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/account/{accountId}/statement")
    @Operation(summary = "Generate account statement", description = "Generate a statement for an account")
    public ResponseEntity<ApiResponse<AccountStatementDto>> generateAccountStatement(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AccountStatementDto statement = reportingService.generateAccountStatement(accountId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statement));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Generate transaction report", description = "Generate a transaction report for a date range")
    public ResponseEntity<ApiResponse<TransactionReportDto>> generateTransactionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        TransactionReportDto report = reportingService.generateTransactionReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
