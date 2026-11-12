package com.carddemo.modern.interest;

import com.carddemo.modern.interest.Records.TranCatBal;
import javax.sql.DataSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Job INTCALC, step STEP15 (PGM=CBACT04C): TCATBALF in key order, chunked. */
@Configuration
public class IntcalcJobConfig {

    @Bean
    public Job intcalcJob(JobRepository jobRepository, Step step15) {
        return new JobBuilder("INTCALC", jobRepository).start(step15).build();
    }

    @Bean
    public Step step15(JobRepository jobRepository, PlatformTransactionManager tx,
                       JdbcCursorItemReader<TranCatBal> tcatbalfReader, Cbact04cProcessor processor,
                       InterestRepository repository, Sysout sysout,
                       @Value("${interest.chunk-size}") int chunkSize) {
        return new StepBuilder("STEP15", jobRepository)
                .<TranCatBal, Step15Output>chunk(chunkSize, tx)
                .reader(tcatbalfReader)
                .processor(processor)
                .writer(new Step15Writer(repository))
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        sysout.display("START OF EXECUTION OF PROGRAM CBACT04C");
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        if (stepExecution.getFailureExceptions().isEmpty()) {
                            sysout.display("END OF EXECUTION OF PROGRAM CBACT04C");
                        }
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<TranCatBal> tcatbalfReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<TranCatBal>()
                .name("tcatbalf")
                .dataSource(dataSource)
                .sql(InterestRepository.TRAN_CAT_BAL_IN_KEY_ORDER)
                .rowMapper(InterestRepository.TRAN_CAT_BAL)
                .build();
    }

    @Bean
    @StepScope
    public Cbact04cProcessor cbact04cProcessor(InterestRepository repository, Sysout sysout, Estate estate,
                                               @Value("${interest.parm-date}") String parmDate) {
        return new Cbact04cProcessor(repository, sysout, estate.codec("CVTRA01Y"), parmDate, estate.clock());
    }
}
