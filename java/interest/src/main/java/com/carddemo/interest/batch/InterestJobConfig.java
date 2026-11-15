package com.carddemo.interest.batch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
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
 * calcInterestStep, on one of two sets of adapters of the ports (FI-3):
 * <ul>
 * <li>{@code interest.calc.adapters=file} (default): the file adapters of the pilot; PostgreSQL
 * holds the job repository only. FI-6 ({@code make parity-java}) runs this.</li>
 * <li>{@code interest.calc.adapters=jdbc}: the tables of schema carddemo (ticket 06), with the
 * commit and restart boundaries of architecture.md sections 5 and 6.</li>
 * </ul>
 * The default stays {@code file} until loadExtractsStep (ticket 05) and exportDatasetsStep
 * (ticket 07) are steps of this job: without them the tables are not filled and no dataset is written.
 */
@Configuration
@EnableConfigurationProperties(InterestPolicy.class)
public class InterestJobConfig {

    public static final String JOB_NAME = "interestCalcJob";
    public static final String CALC_STEP_NAME = "calcInterestStep";
    public static final String FILE_ADAPTERS = "file";
    public static final String JDBC_ADAPTERS = "jdbc";

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
    CalcInterestTableRun calcInterestTableRun(JdbcTemplate jdbc, InterestPolicy policy, Clock runClock) {
        return new CalcInterestTableRun(jdbc, policy, runClock);
    }

    /**
     * A chunk is {@code interest.calc.commitInterval} whole accounts (architecture.md section 5).
     * The default of 100 is a guess until the volumes are known (gap G11).
     */
    @Bean
    Step calcInterestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            CalcInterestRun fileRun, CalcInterestTableRun tableRun,
            @Value("${interest.calc.adapters:" + FILE_ADAPTERS + "}") String adapters,
            @Value("${interest.calc.commitInterval:100}") int commitInterval) {
        // No skip, no retry: one stop exception fails the step (standards S6.2).
        var step = new StepBuilder(CALC_STEP_NAME, jobRepository)
                .<AccountBalances, AccountInterest>chunk(commitInterval, transactionManager);
        return switch (adapters) {
            case FILE_ADAPTERS -> step
                    .reader(fileRun::read)
                    .processor(fileRun::process)
                    .writer(fileRun::write)
                    .listener(fileRun)
                    .build();
            case JDBC_ADAPTERS -> step
                    .reader(tableRun::read)
                    .processor(tableRun::process)
                    .writer(tableRun::write)
                    .stream(tableRun)
                    .listener((StepExecutionListener) tableRun)
                    .build();
            default -> throw new IllegalStateException(
                    "interest.calc.adapters must be " + FILE_ADAPTERS + " or " + JDBC_ADAPTERS);
        };
    }

    @Bean
    Job interestCalcJob(JobRepository jobRepository, Step calcInterestStep, CalcInterestTableRun tableRun,
            @Value("${interest.calc.adapters:" + FILE_ADAPTERS + "}") String adapters) {
        var job = new JobBuilder(JOB_NAME, jobRepository).validator(new InterestJobParameters());
        if (JDBC_ADAPTERS.equals(adapters)) {
            job.listener((JobExecutionListener) tableRun);
        }
        return job.start(calcInterestStep).build();
    }
}
