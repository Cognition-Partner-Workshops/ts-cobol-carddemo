package com.carddemo.batch.service;

import com.carddemo.batch.dto.BatchJobResponse;
import com.carddemo.batch.entity.Account;
import com.carddemo.batch.entity.BatchJobLog;
import com.carddemo.batch.entity.Transaction;
import com.carddemo.batch.repository.AccountRepository;
import com.carddemo.batch.repository.BatchJobLogRepository;
import com.carddemo.batch.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileMaintenanceService {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BatchJobLogRepository batchJobLogRepository;
    
    private static final String JOB_TYPE_MAINTENANCE = "FILE_MAINTENANCE";
    private static final int ARCHIVE_DAYS = 90;
    
    @Transactional
    public BatchJobResponse runAccountMaintenance() {
        log.info("Starting account maintenance");
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchJobLog jobLog = new BatchJobLog();
        jobLog.setJobName("Account File Maintenance");
        jobLog.setJobType(JOB_TYPE_MAINTENANCE);
        jobLog.setStartTime(startTime);
        jobLog.setStatus("RUNNING");
        batchJobLogRepository.save(jobLog);
        
        int totalProcessed = 0;
        int totalSuccess = 0;
        int totalFailed = 0;
        StringBuilder errorMessages = new StringBuilder();
        
        try {
            // Check for expired accounts
            List<Account> allAccounts = accountRepository.findAll();
            LocalDate today = LocalDate.now();
            
            for (Account account : allAccounts) {
                totalProcessed++;
                try {
                    boolean updated = false;
                    
                    // Check if account has expired
                    if (account.getExpirationDate() != null && 
                        account.getExpirationDate().isBefore(today) &&
                        "Y".equals(account.getActiveStatus())) {
                        
                        // Check if reissue date is set and in the future
                        if (account.getReissueDate() != null && 
                            account.getReissueDate().isAfter(today)) {
                            // Account will be reissued, keep active
                            log.info("Account {} expired but pending reissue on {}", 
                                account.getAccountId(), account.getReissueDate());
                        } else {
                            // Deactivate expired account
                            account.setActiveStatus("N");
                            updated = true;
                            log.info("Deactivated expired account {}", account.getAccountId());
                        }
                    }
                    
                    if (updated) {
                        accountRepository.save(account);
                    }
                    totalSuccess++;
                    
                } catch (Exception e) {
                    totalFailed++;
                    errorMessages.append("Account ").append(account.getAccountId())
                        .append(": ").append(e.getMessage()).append("; ");
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
            
            log.info("Account maintenance completed. Processed: {}, Success: {}, Failed: {}", 
                totalProcessed, totalSuccess, totalFailed);
            
            return BatchJobResponse.builder()
                .jobId(jobLog.getId())
                .jobName("Account File Maintenance")
                .jobType(JOB_TYPE_MAINTENANCE)
                .startTime(startTime)
                .endTime(endTime)
                .status(jobLog.getStatus())
                .recordsProcessed(totalProcessed)
                .recordsSuccess(totalSuccess)
                .recordsFailed(totalFailed)
                .errorMessage(jobLog.getErrorMessage())
                .build();
                
        } catch (Exception e) {
            log.error("Account maintenance failed: {}", e.getMessage(), e);
            
            LocalDateTime endTime = LocalDateTime.now();
            jobLog.setEndTime(endTime);
            jobLog.setStatus("FAILED");
            jobLog.setErrorMessage(e.getMessage());
            batchJobLogRepository.save(jobLog);
            
            return BatchJobResponse.builder()
                .jobId(jobLog.getId())
                .jobName("Account File Maintenance")
                .jobType(JOB_TYPE_MAINTENANCE)
                .startTime(startTime)
                .endTime(endTime)
                .status("FAILED")
                .errorMessage(e.getMessage())
                .build();
        }
    }
    
    @Transactional
    public BatchJobResponse archiveOldTransactions() {
        log.info("Starting transaction archival");
        LocalDateTime startTime = LocalDateTime.now();
        
        BatchJobLog jobLog = new BatchJobLog();
        jobLog.setJobName("Transaction Archival");
        jobLog.setJobType(JOB_TYPE_MAINTENANCE);
        jobLog.setStartTime(startTime);
        jobLog.setStatus("RUNNING");
        jobLog.setParameters("archiveDays=" + ARCHIVE_DAYS);
        batchJobLogRepository.save(jobLog);
        
        int totalProcessed = 0;
        int totalSuccess = 0;
        int totalFailed = 0;
        
        try {
            LocalDateTime archiveDate = LocalDateTime.now().minusDays(ARCHIVE_DAYS);
            List<Transaction> oldTransactions = transactionRepository.findPostedTransactionsSince(archiveDate);
            
            // In a real implementation, this would move transactions to an archive table
            // For now, we just log the count
            totalProcessed = oldTransactions.size();
            totalSuccess = totalProcessed;
            
            log.info("Found {} transactions older than {} days for archival", totalProcessed, ARCHIVE_DAYS);
            
            // Update job log
            LocalDateTime endTime = LocalDateTime.now();
            jobLog.setEndTime(endTime);
            jobLog.setRecordsProcessed(totalProcessed);
            jobLog.setRecordsSuccess(totalSuccess);
            jobLog.setRecordsFailed(totalFailed);
            jobLog.setStatus("COMPLETED");
            batchJobLogRepository.save(jobLog);
            
            return BatchJobResponse.builder()
                .jobId(jobLog.getId())
                .jobName("Transaction Archival")
                .jobType(JOB_TYPE_MAINTENANCE)
                .startTime(startTime)
                .endTime(endTime)
                .status("COMPLETED")
                .recordsProcessed(totalProcessed)
                .recordsSuccess(totalSuccess)
                .recordsFailed(totalFailed)
                .build();
                
        } catch (Exception e) {
            log.error("Transaction archival failed: {}", e.getMessage(), e);
            
            LocalDateTime endTime = LocalDateTime.now();
            jobLog.setEndTime(endTime);
            jobLog.setStatus("FAILED");
            jobLog.setErrorMessage(e.getMessage());
            batchJobLogRepository.save(jobLog);
            
            return BatchJobResponse.builder()
                .jobId(jobLog.getId())
                .jobName("Transaction Archival")
                .jobType(JOB_TYPE_MAINTENANCE)
                .startTime(startTime)
                .endTime(endTime)
                .status("FAILED")
                .errorMessage(e.getMessage())
                .build();
        }
    }
}
