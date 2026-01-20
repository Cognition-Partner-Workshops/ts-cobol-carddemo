package com.carddemo.batch.config;

import com.carddemo.batch.service.EndOfDayService;
import com.carddemo.batch.service.FileMaintenanceService;
import com.carddemo.batch.service.StatementGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchSchedulerConfig {
    
    private final EndOfDayService endOfDayService;
    private final StatementGenerationService statementGenerationService;
    private final FileMaintenanceService fileMaintenanceService;
    
    /**
     * End of Day Processing - Runs daily at 11:00 PM
     * Posts pending transactions and calculates daily interest
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void scheduledEndOfDay() {
        log.info("Scheduled: Starting End of Day processing");
        try {
            endOfDayService.runEndOfDayProcessing();
            log.info("Scheduled: End of Day processing completed");
        } catch (Exception e) {
            log.error("Scheduled: End of Day processing failed", e);
        }
    }
    
    /**
     * Monthly Statement Generation - Runs on the 1st of each month at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void scheduledStatementGeneration() {
        log.info("Scheduled: Starting monthly statement generation");
        try {
            statementGenerationService.generateMonthlyStatements();
            log.info("Scheduled: Monthly statement generation completed");
        } catch (Exception e) {
            log.error("Scheduled: Monthly statement generation failed", e);
        }
    }
    
    /**
     * Account Maintenance - Runs daily at 3:00 AM
     * Checks for expired accounts and updates status
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledAccountMaintenance() {
        log.info("Scheduled: Starting account maintenance");
        try {
            fileMaintenanceService.runAccountMaintenance();
            log.info("Scheduled: Account maintenance completed");
        } catch (Exception e) {
            log.error("Scheduled: Account maintenance failed", e);
        }
    }
    
    /**
     * Transaction Archival - Runs weekly on Sunday at 4:00 AM
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    public void scheduledTransactionArchival() {
        log.info("Scheduled: Starting transaction archival");
        try {
            fileMaintenanceService.archiveOldTransactions();
            log.info("Scheduled: Transaction archival completed");
        } catch (Exception e) {
            log.error("Scheduled: Transaction archival failed", e);
        }
    }
}
