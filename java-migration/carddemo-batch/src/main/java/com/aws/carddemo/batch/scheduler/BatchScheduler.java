package com.aws.carddemo.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Batch Scheduler - replaces Control-M/CA7 scheduler configurations
 * Schedules and orchestrates batch jobs according to the original mainframe schedule
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job postTransactionsJob;
    private final Job backupTransactionsJob;
    private final Job calculateInterestJob;
    private final Job generateStatementsJob;

    @Scheduled(cron = "${carddemo.batch.schedule.daily-posting:0 0 2 * * ?}")
    public void runDailyBatchCycle() {
        log.info("Starting daily batch cycle at {}", LocalDateTime.now());
        
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("cycle", "DAILY")
                    .toJobParameters();

            log.info("Step 1: Running transaction posting job (POSTTRAN)");
            jobLauncher.run(postTransactionsJob, params);

            log.info("Step 2: Running transaction backup job (TRANBKP)");
            jobLauncher.run(backupTransactionsJob, params);

            log.info("Daily batch cycle completed successfully at {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("Daily batch cycle failed", e);
        }
    }

    @Scheduled(cron = "${carddemo.batch.schedule.monthly-cycle:0 0 3 1 * ?}")
    public void runMonthlyBatchCycle() {
        log.info("Starting monthly batch cycle at {}", LocalDateTime.now());
        
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("cycle", "MONTHLY")
                    .toJobParameters();

            log.info("Step 1: Running interest calculation job (INTCALC)");
            jobLauncher.run(calculateInterestJob, params);

            log.info("Step 2: Running statement generation job (CREASTMT)");
            jobLauncher.run(generateStatementsJob, params);

            log.info("Monthly batch cycle completed successfully at {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("Monthly batch cycle failed", e);
        }
    }

    public void runJobManually(String jobName) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("trigger", "MANUAL")
                .toJobParameters();

        switch (jobName.toUpperCase()) {
            case "POSTTRAN" -> jobLauncher.run(postTransactionsJob, params);
            case "TRANBKP" -> jobLauncher.run(backupTransactionsJob, params);
            case "INTCALC" -> jobLauncher.run(calculateInterestJob, params);
            case "CREASTMT" -> jobLauncher.run(generateStatementsJob, params);
            default -> throw new IllegalArgumentException("Unknown job: " + jobName);
        }
    }
}
