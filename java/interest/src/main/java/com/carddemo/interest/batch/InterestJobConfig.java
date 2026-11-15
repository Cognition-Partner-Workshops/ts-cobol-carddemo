package com.carddemo.interest.batch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.policy.InterestPolicy;

/**
 * interestCalcJob replaces INTCALC.STEP15 (architecture.md section 4). In the pilot it has one
 * step, calcInterestStep, on file adapters; PostgreSQL holds the job repository only.
 */
@Configuration
@EnableConfigurationProperties(InterestPolicy.class)
public class InterestJobConfig {

    public static final String JOB_NAME = "interestCalcJob";
    public static final String CALC_STEP_NAME = "calcInterestStep";

    private static final DateTimeFormatter FROZEN_CLOCK_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS");

    /** The run clock (INT-Q03). A non-empty interest.clock.frozen freezes it, as the harness does. */
    @Bean
    Clock runClock(@Value("${interest.clock.frozen:}") String frozen) {
        ZoneId zone = ZoneId.systemDefault();
        if (frozen.isEmpty()) {
            return Clock.system(zone);
        }
        return Clock.fixed(LocalDateTime.parse(frozen, FROZEN_CLOCK_FORMAT).atZone(zone).toInstant(), zone);
    }

    @Bean
    CalcInterestRun calcInterestRun(InterestPolicy policy, Clock runClock) {
        return new CalcInterestRun(policy, runClock);
    }

    @Bean
    Step calcInterestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            CalcInterestRun run, @Value("${interest.calc.commitInterval:100}") int commitInterval) {
        // No skip, no retry: one stop exception fails the step (standards S6.2).
        return new StepBuilder(CALC_STEP_NAME, jobRepository)
                .<AccountBalances, AccountInterest>chunk(commitInterval, transactionManager)
                .reader(run::read)
                .processor(run::process)
                .writer(run::write)
                .listener(run)
                .build();
    }

    @Bean
    Job interestCalcJob(JobRepository jobRepository, Step calcInterestStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(new InterestJobParameters())
                .start(calcInterestStep)
                .build();
    }
}
