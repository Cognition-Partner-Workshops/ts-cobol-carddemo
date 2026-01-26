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
public class MonthlyProcessingBatchJob {

    private static final Logger log = LoggerFactory.getLogger(MonthlyProcessingBatchJob.class);

    private final BatchJobLogRepository batchJobLogRepository;

    public MonthlyProcessingBatchJob(BatchJobLogRepository batchJobLogRepository) {
        this.batchJobLogRepository = batchJobLogRepository;
    }

    @Bean
    public Job monthlyProcessingJob(JobRepository jobRepository,
                                     Step monthlyCloseFilesStep,
                                     Step interestCalcStep,
                                     Step combineTransactionsStep,
                                     Step monthlyWaitStep,
                                     Step monthlyOpenFilesStep) {
        return new JobBuilder("monthlyProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(monthlyCloseFilesStep)
                .next(interestCalcStep)
                .next(combineTransactionsStep)
                .next(monthlyWaitStep)
                .next(monthlyOpenFilesStep)
                .build();
    }

    @Bean
    public Step monthlyCloseFilesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("monthlyCloseFilesStep", jobRepository)
                .tasklet(monthlyCloseFilesTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet monthlyCloseFilesTasklet() {
        return (contribution, chunkContext) -> {
            log.info("CLOSEFIL (Monthly): Closing files for monthly batch processing");
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("CLOSEFIL_MONTHLY")
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
    public Step interestCalcStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("interestCalcStep", jobRepository)
                .tasklet(interestCalcTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet interestCalcTasklet() {
        return (contribution, chunkContext) -> {
            log.info("INTCALC: Processing transaction balance file and computing interest (CBACT04C)");
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("INTCALC")
                    .stepName("CALCULATE_INTEREST")
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
    public Step combineTransactionsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("combineTransactionsStep", jobRepository)
                .tasklet(combineTransactionsTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet combineTransactionsTasklet() {
        return (contribution, chunkContext) -> {
            log.info("COMBTRAN: Combining system transactions with daily transactions");
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("COMBTRAN")
                    .stepName("COMBINE_TRANSACTIONS")
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
    public Step monthlyWaitStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("monthlyWaitStep", jobRepository)
                .tasklet(monthlyWaitTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet monthlyWaitTasklet() {
        return (contribution, chunkContext) -> {
            log.info("WAITSTEP (Monthly): System stability wait");
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("WAITSTEP_MONTHLY")
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
    public Step monthlyOpenFilesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("monthlyOpenFilesStep", jobRepository)
                .tasklet(monthlyOpenFilesTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet monthlyOpenFilesTasklet() {
        return (contribution, chunkContext) -> {
            log.info("OPENFIL (Monthly): Reopening files after monthly batch processing");
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("OPENFIL_MONTHLY")
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
