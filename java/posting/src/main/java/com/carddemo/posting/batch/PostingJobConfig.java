package com.carddemo.posting.batch;

import com.carddemo.posting.batch.PostTransactionsStep.Daily;
import com.carddemo.posting.batch.PostTransactionsStep.Outcome;
import com.carddemo.posting.store.PostingStore;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * One Spring Batch job per migrated program. The JCL around them (setup jobs, POSTTRAN's other
 * steps, TRANREPT's REPRO and SORT) still runs on the JCL runner.
 */
@Configuration
public class PostingJobConfig {

    static final int CHUNK_SIZE = 100;

    /** POSTTRAN STEP15. */
    @Bean
    public Job cbtrn02cJob(JobRepository jobRepository, PlatformTransactionManager tm, PostingStore store,
                           PostTransactionsStep posting, UnloadClustersTasklet unload) {
        Step load = new StepBuilder("CBTRN02C.load", jobRepository)
                .tasklet(new LoadClustersTasklet(store, "XREFFILE", "ACCTFILE", "TCATBALF"), tm).build();
        Step post = new StepBuilder("CBTRN02C.post", jobRepository)
                .<Daily, Outcome>chunk(CHUNK_SIZE, tm)
                .reader(posting)
                .processor(posting)
                .writer(posting)
                .listener(posting)
                .build();
        Step unloadStep = new StepBuilder("CBTRN02C.unload", jobRepository).tasklet(unload, tm).build();
        return new JobBuilder("CBTRN02C", jobRepository).start(load).next(post).next(unloadStep).build();
    }

    /** TRANREPT STEP10R. */
    @Bean
    public Job cbtrn03cJob(JobRepository jobRepository, PlatformTransactionManager tm, PostingStore store,
                           TransactionReportTasklet report) {
        Step load = new StepBuilder("CBTRN03C.load", jobRepository)
                .tasklet(new LoadClustersTasklet(store, "CARDXREF", "TRANTYPE", "TRANCATG"), tm).build();
        Step reportStep = new StepBuilder("CBTRN03C.report", jobRepository).tasklet(report, tm).build();
        return new JobBuilder("CBTRN03C", jobRepository).start(load).next(reportStep).build();
    }
}
