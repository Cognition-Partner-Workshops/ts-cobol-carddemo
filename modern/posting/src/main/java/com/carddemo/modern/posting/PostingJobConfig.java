package com.carddemo.modern.posting;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** POSTTRAN STEP15 followed by TRANREPT PRC001 REPRO, STEP05R SORT and STEP10R CBTRN03C. */
@Configuration
public class PostingJobConfig {
    @Bean
    Job postingJob(JobRepository jobs, Step postTransactions) {
        return new JobBuilder("POSTTRAN",jobs).start(postTransactions).build();
    }
    @Bean
    Job reportingJob(JobRepository jobs, Step unloadAndSort, Step reportTransactions) {
        return new JobBuilder("TRANREPT",jobs).start(unloadAndSort).next(reportTransactions).build();
    }

    @Bean
    Step postTransactions(JobRepository jobs, PlatformTransactionManager tx, PostingRepository repo,
                          JdbcCursorItemReader<String> dailyReader, Cbtrn02cProcessor postingProcessor,
                          PostingOutput output) {
        // One record per chunk preserves WRITE order and stops immediately after a failed WRITE.
        return new StepBuilder("POSTTRAN.STEP15",jobs).<String,Cbtrn02cProcessor.Posted>chunk(1,tx)
                .reader(dailyReader).processor(postingProcessor).writer(new PostWriter(repo,postingProcessor,tx))
                .listener(new StepExecutionListener() {
                    @Override public void beforeStep(StepExecution step) { postingProcessor.open(); }
                    @Override public ExitStatus afterStep(StepExecution step) {
                        output.postingSysout = postingProcessor.sysout();
                        output.postingRc = postingProcessor.returnCode();
                        return step.getExitStatus();
                    }
                }).build();
    }

    @Bean
    Step unloadAndSort(JobRepository jobs, PlatformTransactionManager tx, PostingRepository repo) {
        return new StepBuilder("TRANREPT.REPRO-SORT",jobs).tasklet((contribution,context) -> {
            // REPRO is master key order; stable SORT ties retain that order. JCL dates are separate from DATEPARM.
            repo.jdbc().execute("INSERT INTO posting.report_input SELECT row_number() OVER (ORDER BY tran_id),"
                    + "card_num,record FROM posting.transaction_master WHERE proc_date >= '2022-01-01' AND proc_date <= '2022-07-06'");
            return org.springframework.batch.repeat.RepeatStatus.FINISHED;
        },tx).build();
    }

    @Bean
    Step reportTransactions(JobRepository jobs, PlatformTransactionManager tx, JdbcCursorItemReader<String> reportReader,
                            Cbtrn03cProcessor reportProcessor, PostingOutput output) {
        return new StepBuilder("TRANREPT.STEP10R",jobs).<String,List<String>>chunk(1,tx)
                .reader(reportReader).processor(reportProcessor).writer(chunk -> { })
                .listener(new StepExecutionListener() {
                    @Override public void beforeStep(StepExecution step) { reportProcessor.open(); }
                    @Override public ExitStatus afterStep(StepExecution step) {
                        reportProcessor.finish();
                        output.reportSysout = reportProcessor.sysout();
                        output.report = reportProcessor.lines();
                        output.reportRc = reportProcessor.returnCode();
                        return step.getExitStatus();
                    }
                }).build();
    }

    @Bean @StepScope
    JdbcCursorItemReader<String> dailyReader(DataSource ds) {
        return reader(ds,"daily","SELECT record FROM posting.daily_transaction ORDER BY seq");
    }
    @Bean @StepScope
    JdbcCursorItemReader<String> reportReader(DataSource ds) {
        return reader(ds,"report","SELECT record FROM posting.report_input ORDER BY card_num,seq");
    }
    private JdbcCursorItemReader<String> reader(DataSource ds, String name, String sql) {
        return new JdbcCursorItemReaderBuilder<String>().name(name).dataSource(ds).sql(sql)
                .rowMapper((rs,n) -> rs.getString(1)).build();
    }
    @Bean @StepScope
    Cbtrn02cProcessor postingProcessor(PostingRepository repo, Estate estate) { return new Cbtrn02cProcessor(repo,estate); }
    @Bean @StepScope
    Cbtrn03cProcessor reportProcessor(PostingRepository repo, InputLoader loader) {
        return new Cbtrn03cProcessor(repo,loader.startDate(),loader.endDate());
    }
}
