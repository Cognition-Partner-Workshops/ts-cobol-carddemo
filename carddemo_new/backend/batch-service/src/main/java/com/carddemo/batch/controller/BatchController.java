package com.carddemo.batch.controller;

import com.carddemo.batch.dto.*;
import com.carddemo.batch.entity.BatchJobLog;
import com.carddemo.batch.entity.Statement;
import com.carddemo.batch.repository.BatchJobLogRepository;
import com.carddemo.batch.repository.StatementRepository;
import com.carddemo.batch.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {
    
    private final InterestCalculationService interestCalculationService;
    private final TransactionPostingService transactionPostingService;
    private final StatementGenerationService statementGenerationService;
    private final EndOfDayService endOfDayService;
    private final FileMaintenanceService fileMaintenanceService;
    private final BatchJobLogRepository batchJobLogRepository;
    private final StatementRepository statementRepository;
    
    // EPIC-008 Feature 1: Daily Interest Calculation
    @PostMapping("/interest/calculate")
    public ResponseEntity<Map<String, Object>> calculateDailyInterest() {
        log.info("API: Starting daily interest calculation");
        List<InterestCalculationResult> results = interestCalculationService.calculateDailyInterest();
        
        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failedCount = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "COMPLETED");
        response.put("totalProcessed", results.size());
        response.put("successCount", successCount);
        response.put("failedCount", failedCount);
        response.put("results", results);
        
        return ResponseEntity.ok(response);
    }
    
    // EPIC-008 Feature 2: End of Day Processing
    @PostMapping("/eod/run")
    public ResponseEntity<BatchJobResponse> runEndOfDay() {
        log.info("API: Starting End of Day processing");
        BatchJobResponse response = endOfDayService.runEndOfDayProcessing();
        return ResponseEntity.ok(response);
    }
    
    // EPIC-008 Feature 3: Monthly Statement Generation
    @PostMapping("/statements/generate")
    public ResponseEntity<Map<String, Object>> generateStatements() {
        log.info("API: Starting monthly statement generation");
        List<StatementGenerationResult> results = statementGenerationService.generateMonthlyStatements();
        
        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failedCount = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "COMPLETED");
        response.put("totalProcessed", results.size());
        response.put("successCount", successCount);
        response.put("failedCount", failedCount);
        response.put("results", results);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statements/account/{accountId}")
    public ResponseEntity<List<Statement>> getStatementsByAccount(@PathVariable String accountId) {
        List<Statement> statements = statementRepository.findByAccountIdOrderByDateDesc(accountId);
        return ResponseEntity.ok(statements);
    }
    
    @GetMapping("/statements/{statementId}")
    public ResponseEntity<Statement> getStatement(@PathVariable String statementId) {
        return statementRepository.findByStatementId(statementId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // EPIC-008 Feature 4: Transaction Posting
    @PostMapping("/transactions/post")
    public ResponseEntity<Map<String, Object>> postTransactions() {
        log.info("API: Starting transaction posting");
        List<TransactionPostingResult> results = transactionPostingService.postPendingTransactions();
        
        long successCount = results.stream().filter(r -> "POSTED".equals(r.getNewStatus())).count();
        long failedCount = results.stream().filter(r -> "FAILED".equals(r.getNewStatus())).count();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "COMPLETED");
        response.put("totalProcessed", results.size());
        response.put("successCount", successCount);
        response.put("failedCount", failedCount);
        response.put("results", results);
        
        return ResponseEntity.ok(response);
    }
    
    // EPIC-008 Feature 5: File Maintenance
    @PostMapping("/maintenance/accounts")
    public ResponseEntity<BatchJobResponse> runAccountMaintenance() {
        log.info("API: Starting account maintenance");
        BatchJobResponse response = fileMaintenanceService.runAccountMaintenance();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/maintenance/archive")
    public ResponseEntity<BatchJobResponse> archiveTransactions() {
        log.info("API: Starting transaction archival");
        BatchJobResponse response = fileMaintenanceService.archiveOldTransactions();
        return ResponseEntity.ok(response);
    }
    
    // Job History
    @GetMapping("/jobs")
    public ResponseEntity<List<BatchJobLog>> getJobHistory() {
        List<BatchJobLog> jobs = batchJobLogRepository.findAllOrderByStartTimeDesc();
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/jobs/recent")
    public ResponseEntity<List<BatchJobLog>> getRecentJobs() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<BatchJobLog> jobs = batchJobLogRepository.findJobsSince(since);
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<BatchJobLog> getJob(@PathVariable Long jobId) {
        return batchJobLogRepository.findById(jobId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/jobs/type/{jobType}")
    public ResponseEntity<List<BatchJobLog>> getJobsByType(@PathVariable String jobType) {
        List<BatchJobLog> jobs = batchJobLogRepository.findByJobType(jobType);
        return ResponseEntity.ok(jobs);
    }
}
