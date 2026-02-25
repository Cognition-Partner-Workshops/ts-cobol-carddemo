package com.carddemo.batch;

import com.carddemo.service.AuthorizationService;
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

/**
 * Spring Batch job to purge expired authorizations - migrated from Phase 5b CBPAUP0C / CBPAUP0J.
 * Original COBOL CBPAUP0C:
 * 1. Read IMS authorization summary/detail segments
 * 2. Find unmatched (pending) authorizations older than expiry threshold
 * 3. Delete expired authorizations from IMS
 * 4. Adjust available credit on the account when unmatched authorizations are removed
 * 5. Update DB2 tables
 *
 * The original two-phase commit across IMS DB + DB2 is replaced with @Transactional.
 */
@Configuration
public class PurgeExpiredAuthorizationsJob {

    private static final Logger log = LoggerFactory.getLogger(PurgeExpiredAuthorizationsJob.class);
    private static final int DEFAULT_EXPIRY_DAYS = 30;

    private final AuthorizationService authorizationService;

    public PurgeExpiredAuthorizationsJob(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Bean
    public Job purgeExpiredAuthBatchJob(JobRepository jobRepository, Step purgeExpiredAuthStep) {
        return new JobBuilder("purgeExpiredAuthorizationsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(purgeExpiredAuthStep)
                .build();
    }

    @Bean
    public Step purgeExpiredAuthStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("purgeExpiredAuthStep", jobRepository)
                .tasklet(purgeExpiredAuthTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet purgeExpiredAuthTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting purge of expired authorizations (expiry: {} days)...", DEFAULT_EXPIRY_DAYS);
            int purged = authorizationService.purgeExpiredAuthorizations(DEFAULT_EXPIRY_DAYS);
            log.info("Purge complete. Removed {} expired authorizations", purged);
            return RepeatStatus.FINISHED;
        };
    }
}
