package com.aws.carddemo.batch;

import com.aws.carddemo.entity.BatchJobLog;
import com.aws.carddemo.repository.BatchJobLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Configuration
public class DailyProcessingBatchJob {

    private static final Logger log = LoggerFactory.getLogger(DailyProcessingBatchJob.class);

    private final BatchJobLogRepository batchJobLogRepository;

    public DailyProcessingBatchJob(BatchJobLogRepository batchJobLogRepository) {
        this.batchJobLogRepository = batchJobLogRepository;
    }

    @Bean
    public Job dailyProcessingJob(JobRepository jobRepository,
                                   Step closeFilesStep,
                                   Step transactionBackupStep,
                                   Step waitStep,
                                   Step openFilesStep) {
        return new JobBuilder("dailyProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(closeFilesStep)
                .next(transactionBackupStep)
                .next(waitStep)
                .next(openFilesStep)
                .build();
    }

    @Bean
    public Step closeFilesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("closeFilesStep", jobRepository)
                .tasklet(closeFilesTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet closeFilesTasklet() {
        return (contribution, chunkContext) -> {
            log.info("CLOSEFIL: Closing files for batch processing");
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("CLOSEFIL")
                    .stepName("CLOSE_FILES")
                    .status("COMPLETED")
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .recordsProcessed(0L)
                    .recordsRejected(0L)
                    .build();
            batchJobLogRepository.save(jobLog);
            
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step transactionBackupStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("transactionBackupStep", jobRepository)
                .tasklet(transactionBackupTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet transactionBackupTasklet() {
        return (contribution, chunkContext) -> {
            log.info("TRANBKP: Creating transaction database backup");
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("TRANBKP")
                    .stepName("BACKUP_TRANSACTIONS")
                    .status("COMPLETED")
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .recordsProcessed(0L)
                    .recordsRejected(0L)
                    .build();
            batchJobLogRepository.save(jobLog);
            
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step waitStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("waitStep", jobRepository)
                .tasklet(waitTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet waitTasklet() {
        return (contribution, chunkContext) -> {
            log.info("WAITSTEP: System stability wait");
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("WAITSTEP")
                    .stepName("WAIT")
                    .status("COMPLETED")
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .recordsProcessed(0L)
                    .recordsRejected(0L)
                    .build();
            batchJobLogRepository.save(jobLog);
            
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step openFilesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("openFilesStep", jobRepository)
                .tasklet(openFilesTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet openFilesTasklet() {
        return (contribution, chunkContext) -> {
            log.info("OPENFIL: Reopening files after batch processing");
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("OPENFIL")
                    .stepName("OPEN_FILES")
                    .status("COMPLETED")
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .recordsProcessed(0L)
                    .recordsRejected(0L)
                    .build();
            batchJobLogRepository.save(jobLog);
            
            return RepeatStatus.FINISHED;
        };
    }
}
