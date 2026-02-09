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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class AuthorizationPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationPurgeJob.class);

    private final AuthorizationService authorizationService;

    public AuthorizationPurgeJob(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Bean
    public Job authorizationPurgeBatchJob(JobRepository jobRepository,
                                          Step purgeExpiredAuthorizationsStep) {
        return new JobBuilder("AuthorizationPurgeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(purgeExpiredAuthorizationsStep)
                .build();
    }

    @Bean
    public Step purgeExpiredAuthorizationsStep(JobRepository jobRepository,
                                               PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("START OF AUTHORIZATION PURGE JOB");

            String cutoffDate = LocalDate.now().minusDays(90)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            log.info("Purging authorizations older than: {}", cutoffDate);

            authorizationService.purgeExpiredAuthorizations(cutoffDate);

            log.info("END OF AUTHORIZATION PURGE JOB");
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("purgeExpiredAuthorizationsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
