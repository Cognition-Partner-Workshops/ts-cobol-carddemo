package com.carddemo.interest.batch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.policy.InterestPolicy;

/**
 * interestCalcJob replaces INTCALC.STEP15 (architecture.md section 4). It has one step,
 * calcInterestStep. interest.calc.store chooses the adapters of its ports: file (the default, as
 * in the pilot: PostgreSQL holds the job repository only) or jdbc (the tables of schema carddemo,
 * with the commit and restart boundaries of architecture.md sections 5 and 6).
 */
@Configuration
@EnableConfigurationProperties(InterestPolicy.class)
public class InterestJobConfig {

    public static final String JOB_NAME = "interestCalcJob";
    public static final String CALC_STEP_NAME = "calcInterestStep";

    /** Where calcInterestStep reads and writes. */
    enum CalcStore { FILE, JDBC }

    /** Rows of tran_cat_balance read per query; no decision depends on it. */
    private static final int BALANCE_PAGE_ROWS = 1000;

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
    CalcInterestTableRun calcInterestTableRun(JdbcTemplate jdbcTemplate, InterestPolicy policy, Clock runClock) {
        return new CalcInterestTableRun(jdbcTemplate, policy, runClock, BALANCE_PAGE_ROWS);
    }

    /**
     * The commit interval is a number of whole accounts; the default 100 is a guess until the
     * volumes are known (gap G11).
     */
    @Bean
    Step calcInterestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            CalcInterestRun fileRun, CalcInterestTableRun tableRun,
            @Value("${interest.calc.store:file}") CalcStore store,
            @Value("${interest.calc.commitInterval:100}") int commitInterval) {
        // No skip, no retry: one stop exception fails the step (standards S6.2).
        SimpleStepBuilder<AccountBalances, AccountInterest> step = new StepBuilder(CALC_STEP_NAME, jobRepository)
                .<AccountBalances, AccountInterest>chunk(commitInterval, transactionManager);
        return switch (store) {
            case FILE -> step.reader(fileRun::read).processor(fileRun::process).writer(fileRun::write)
                    .listener(fileRun).build();
            case JDBC -> step.reader(tableRun::read).processor(tableRun::process).writer(tableRun::write)
                    .stream(tableRun).listener((StepExecutionListener) tableRun).build();
        };
    }

    @Bean
    Job interestCalcJob(JobRepository jobRepository, Step calcInterestStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(new InterestJobParameters())
                .start(calcInterestStep)
                .build();
    }
}
