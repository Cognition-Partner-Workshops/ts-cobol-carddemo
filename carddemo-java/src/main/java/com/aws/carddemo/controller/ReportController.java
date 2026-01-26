package com.aws.carddemo.controller;

import com.aws.carddemo.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily-summary")
    public ResponseEntity<Map<String, Object>> getDailySummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.generateDailySummaryReport(date));
    }

    @GetMapping("/account-summary/{acctId}")
    public ResponseEntity<Map<String, Object>> getAccountSummaryReport(@PathVariable Long acctId) {
        return ResponseEntity.ok(reportService.generateAccountSummaryReport(acctId));
    }

    @GetMapping("/rejection-summary")
    public ResponseEntity<Map<String, Object>> getRejectionSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.generateRejectionSummaryReport(startDate, endDate));
    }

    @GetMapping("/system-statistics")
    public ResponseEntity<Map<String, Object>> getSystemStatisticsReport() {
        return ResponseEntity.ok(reportService.generateSystemStatisticsReport());
    }

    @GetMapping("/interest")
    public ResponseEntity<Map<String, Object>> getInterestReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate calcDate) {
        return ResponseEntity.ok(reportService.generateInterestReport(calcDate));
    }
}
