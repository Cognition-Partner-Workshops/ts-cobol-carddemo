package com.carddemo.controller;

import com.carddemo.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Report controller - migrated from CORPT00C (CR00 - Transaction Reports).
 * Replaces CICS transaction CR00 with REST endpoint.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * GET /api/reports/transactions?cardNum=...&startDate=...&endDate=...
     * Generates a transaction report for a card within a date range.
     */
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> transactionReport(
            @RequestParam String cardNum,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(reportService.generateTransactionReport(cardNum, startDate, endDate));
    }

    /**
     * GET /api/reports/account-summary?acctId=...
     * Generates an account summary report.
     */
    @GetMapping("/account-summary")
    public ResponseEntity<Map<String, Object>> accountSummary(@RequestParam Long acctId) {
        return ResponseEntity.ok(reportService.generateAccountSummary(acctId));
    }
}
