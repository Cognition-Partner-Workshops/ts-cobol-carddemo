package com.carddemo.controller;

import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.batch.TransactionPostingJob;
import com.carddemo.batch.TransactionReportJob;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final TransactionPostingJob transactionPostingJob;
    private final InterestCalculationJob interestCalculationJob;
    private final StatementGenerationJob statementGenerationJob;
    private final TransactionReportJob transactionReportJob;

    public BatchController(TransactionPostingJob transactionPostingJob,
                           InterestCalculationJob interestCalculationJob,
                           StatementGenerationJob statementGenerationJob,
                           TransactionReportJob transactionReportJob) {
        this.transactionPostingJob = transactionPostingJob;
        this.interestCalculationJob = interestCalculationJob;
        this.statementGenerationJob = statementGenerationJob;
        this.transactionReportJob = transactionReportJob;
    }

    @PostMapping("/post-transactions")
    public ResponseEntity<Map<String, Object>> postTransactions(
            @RequestBody List<Transaction> transactions) {
        TransactionPostingJob.BatchResult result = transactionPostingJob.execute(transactions);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("processed", result.processedCount());
        response.put("rejected", result.rejectedCount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate-interest")
    public ResponseEntity<Map<String, Object>> calculateInterest(
            @RequestParam(required = false) String date) {
        LocalDate calcDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        int count = interestCalculationJob.execute(calcDate);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("recordsProcessed", count);
        response.put("calculationDate", calcDate.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/generate-statements")
    public ResponseEntity<List<Map<String, Object>>> generateStatements(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ResponseEntity.ok(statementGenerationJob.execute(start, end));
    }

    @GetMapping("/transaction-report")
    public ResponseEntity<Map<String, Object>> transactionReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ResponseEntity.ok(transactionReportJob.execute(start, end));
    }
}
