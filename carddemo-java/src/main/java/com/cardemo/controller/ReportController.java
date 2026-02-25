package com.cardemo.controller;

import com.cardemo.entity.Transaction;
import com.cardemo.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report controller.
 * Migrated from CR00 transaction / CORPT00C program.
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final TransactionService transactionService;

    public ReportController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * GET /reports/transactions - Migrated from CR00 (CORPT00C) transaction report.
     * COBOL: Reads all transactions and generates a report output.
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<Transaction>> getTransactionReport(Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionReport(pageable));
    }
}
