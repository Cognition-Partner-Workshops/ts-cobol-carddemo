package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.account.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report generation endpoints - migrated from CORPT00C")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/account-summary")
    @Operation(summary = "Generate account summary report")
    public ResponseEntity<ReportService.AccountSummaryReport> generateAccountSummaryReport() {
        return ResponseEntity.ok(reportService.generateAccountSummaryReport());
    }

    @GetMapping("/transaction-summary")
    @Operation(summary = "Generate transaction summary report")
    public ResponseEntity<ReportService.TransactionSummaryReport> generateTransactionSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(reportService.generateTransactionSummaryReport(startDate, endDate));
    }

    @GetMapping("/card-status")
    @Operation(summary = "Generate card status report")
    public ResponseEntity<ReportService.CardStatusReport> generateCardStatusReport() {
        return ResponseEntity.ok(reportService.generateCardStatusReport());
    }

    @GetMapping("/customer-statistics")
    @Operation(summary = "Generate customer statistics report")
    public ResponseEntity<ReportService.CustomerStatisticsReport> generateCustomerStatisticsReport() {
        return ResponseEntity.ok(reportService.generateCustomerStatisticsReport());
    }
}
