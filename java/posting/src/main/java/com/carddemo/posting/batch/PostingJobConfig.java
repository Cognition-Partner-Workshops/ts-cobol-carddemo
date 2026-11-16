package com.carddemo.posting.batch;

import com.carddemo.posting.batch.PostTransactionsStep.Daily;
import com.carddemo.posting.batch.PostTransactionsStep.Outcome;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The posting chain as one Spring Batch job. Step names follow the legacy jobs:
 * setup jobs -> POSTTRAN STEP15 (CBTRN02C) -> TRANREPT STEP05R/STEP10R (SORT + CBTRN03C) -> unload.
 */
@Configuration
public class PostingJobConfig {

    static final int CHUNK_SIZE = 100;

    @Bean
    public Job postingJob(JobRepository jobRepository, Step loadClusters, Step postTransactions,
                          Step transactionReport, Step unloadOutputs) {
        return new JobBuilder("posting", jobRepository)
                .start(loadClusters)
                .next(postTransactions)
                .next(transactionReport)
                .next(unloadOutputs)
                .build();
    }

    @Bean
    public Step loadClusters(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                             LoadClustersTasklet tasklet) {
        return new StepBuilder("setup.loadClusters", jobRepository).tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step postTransactions(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                 PostTransactionsStep posting) {
        return new StepBuilder("POSTTRAN.STEP15", jobRepository)
                .<Daily, Outcome>chunk(CHUNK_SIZE, transactionManager)
                .reader(posting)
                .processor(posting)
                .writer(posting)
                .listener(posting)
                .build();
    }

    @Bean
    public Step transactionReport(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                  TransactionReportTasklet tasklet) {
        return new StepBuilder("TRANREPT.STEP10R", jobRepository).tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step unloadOutputs(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                              UnloadTasklet tasklet) {
        return new StepBuilder("unload", jobRepository).tasklet(tasklet, transactionManager).build();
    }
}
