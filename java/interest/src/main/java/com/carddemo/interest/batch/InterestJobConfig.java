package com.carddemo.interest.batch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

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
 * interestCalcJob replaces INTCALC.STEP15 (architecture.md section 4). It has one step,
 * calcInterestStep, with the same reader, processor and writer shape on either set of adapters of
 * the ports (FI-3): the file adapters of the pilot (interest.calc.store=file, the default while the
 * job has no load and export step) or the JDBC adapters on schema carddemo (interest.calc.store=jdbc,
 * ticket 06).
 */
@Configuration
@EnableConfigurationProperties(InterestPolicy.class)
public class InterestJobConfig {

    public static final String JOB_NAME = "interestCalcJob";
    public static final String CALC_STEP_NAME = "calcInterestStep";
    static final String STORE_FILE = "file";
    static final String STORE_JDBC = "jdbc";

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
    JdbcCalcInterestRun jdbcCalcInterestRun(DataSource dataSource, InterestPolicy policy, Clock runClock) {
        return new JdbcCalcInterestRun(dataSource, policy, runClock);
    }

    @Bean
    Step calcInterestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            CalcInterestRun run, JdbcCalcInterestRun jdbcRun,
            @Value("${interest.calc.store:file}") String store,
            @Value("${interest.calc.commitInterval:100}") int commitInterval) {
        // No skip, no retry: one stop exception fails the step (standards S6.2).
        if (STORE_JDBC.equals(store)) {
            return new StepBuilder(CALC_STEP_NAME, jobRepository)
                    .<AccountBalances, AccountInterest>chunk(commitInterval, transactionManager)
                    .reader(jdbcRun::read)
                    .processor(jdbcRun::process)
                    .writer(jdbcRun::write)
                    .stream(jdbcRun)
                    .listener(jdbcRun)
                    .build();
        }
        if (!STORE_FILE.equals(store)) {
            throw new IllegalArgumentException("interest.calc.store must be file or jdbc");
        }
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
