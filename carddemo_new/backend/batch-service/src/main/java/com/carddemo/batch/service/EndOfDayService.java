package com.carddemo.batch.service;

import com.carddemo.batch.dto.BatchJobResponse;
import com.carddemo.batch.dto.InterestCalculationResult;
import com.carddemo.batch.dto.TransactionPostingResult;
import com.carddemo.batch.entity.BatchJobLog;
import com.carddemo.batch.repository.BatchJobLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndOfDayService {
    
    private final TransactionPostingService transactionPostingService;
    private final InterestCalculationService interestCalculationService;
    private final BatchJobLogRepository batchJobLogRepository;
    
    private static final String JOB_TYPE_EOD = "END_OF_DAY";
    private static final String JOB_NAME_EOD = "Daily End of Day Processing";
    
    @Transactional
    public BatchJobResponse runEndOfDayProcessing() {
        log.info("Starting End of Day processing");
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchJobLog jobLog = new BatchJobLog();
        jobLog.setJobName(JOB_NAME_EOD);
        jobLog.setJobType(JOB_TYPE_EOD);
        jobLog.setStartTime(startTime);
        jobLog.setStatus("RUNNING");
        batchJobLogRepository.save(jobLog);
        
        int totalProcessed = 0;
        int totalSuccess = 0;
        int totalFailed = 0;
        StringBuilder errorMessages = new StringBuilder();
        
        try {
            // Step 1: Post pending transactions
            log.info("EOD Step 1: Posting pending transactions");
            List<TransactionPostingResult> postingResults = transactionPostingService.postPendingTransactions();
            for (TransactionPostingResult result : postingResults) {
                totalProcessed++;
                if ("POSTED".equals(result.getNewStatus())) {
                    totalSuccess++;
                } else {
                    totalFailed++;
                    if (result.getErrorMessage() != null) {
                        errorMessages.append("Txn ").append(result.getTransactionId())
                            .append(": ").append(result.getErrorMessage()).append("; ");
                    }
                }
            }
            
            // Step 2: Calculate daily interest
            log.info("EOD Step 2: Calculating daily interest");
            List<InterestCalculationResult> interestResults = interestCalculationService.calculateDailyInterest();
            for (InterestCalculationResult result : interestResults) {
                totalProcessed++;
                if ("SUCCESS".equals(result.getStatus())) {
                    totalSuccess++;
                } else if ("FAILED".equals(result.getStatus())) {
                    totalFailed++;
                    if (result.getErrorMessage() != null) {
                        errorMessages.append("Interest ").append(result.getAccountId())
                            .append(": ").append(result.getErrorMessage()).append("; ");
                    }
                }
            }
            
            // Update job log
            LocalDateTime endTime = LocalDateTime.now();
            jobLog.setEndTime(endTime);
            jobLog.setRecordsProcessed(totalProcessed);
            jobLog.setRecordsSuccess(totalSuccess);
            jobLog.setRecordsFailed(totalFailed);
            jobLog.setStatus(totalFailed == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
            if (errorMessages.length() > 0) {
                jobLog.setErrorMessage(errorMessages.toString().substring(0, Math.min(500, errorMessages.length())));
            }
            batchJobLogRepository.save(jobLog);
            
            log.info("End of Day processing completed. Processed: {}, Success: {}, Failed: {}", 
                totalProcessed, totalSuccess, totalFailed);
            
            return BatchJobResponse.builder()
                .jobId(jobLog.getId())
                .jobName(JOB_NAME_EOD)
                .jobType(JOB_TYPE_EOD)
                .startTime(startTime)
                .endTime(endTime)
                .status(jobLog.getStatus())
                .recordsProcessed(totalProcessed)
                .recordsSuccess(totalSuccess)
                .recordsFailed(totalFailed)
                .errorMessage(jobLog.getErrorMessage())
                .build();
                
        } catch (Exception e) {
            log.error("End of Day processing failed: {}", e.getMessage(), e);
            
            LocalDateTime endTime = LocalDateTime.now();
            jobLog.setEndTime(endTime);
            jobLog.setRecordsProcessed(totalProcessed);
            jobLog.setRecordsSuccess(totalSuccess);
            jobLog.setRecordsFailed(totalFailed);
            jobLog.setStatus("FAILED");
            jobLog.setErrorMessage(e.getMessage());
            batchJobLogRepository.save(jobLog);
            
            return BatchJobResponse.builder()
                .jobId(jobLog.getId())
                .jobName(JOB_NAME_EOD)
                .jobType(JOB_TYPE_EOD)
                .startTime(startTime)
                .endTime(endTime)
                .status("FAILED")
                .recordsProcessed(totalProcessed)
                .recordsSuccess(totalSuccess)
                .recordsFailed(totalFailed)
                .errorMessage(e.getMessage())
                .build();
        }
    }
}
