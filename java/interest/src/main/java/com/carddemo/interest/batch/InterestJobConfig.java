package com.carddemo.interest.batch;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.carddemo.interest.record.TranCatBalRecord;

/** Job INTCALC, step STEP15 (CBACT04C). */
@Configuration
public class InterestJobConfig {

    @Bean
    public Job intcalcJob(JobRepository jobRepository, Step step15) {
        return new JobBuilder("INTCALC", jobRepository).start(step15).build();
    }

    @Bean
    public Step step15(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                       JdbcCursorItemReader<TranCatBalRecord> tranCatBalReader, InterestCalculator interestCalculator,
                       PostingWriter postingWriter, @Value("${carddemo.interest.chunk-size}") int chunkSize) {
        return new StepBuilder("STEP15", jobRepository)
                .<TranCatBalRecord, StepOutput>chunk(chunkSize, transactionManager)
                .reader(tranCatBalReader)
                .processor(interestCalculator)
                .writer(postingWriter)
                .listener(postingWriter)
                .allowStartIfComplete(true)
                .build();
    }

    /** TCATBALF read sequentially in KSDS key order (byte order of the 17-byte key). */
    @Bean
    @StepScope
    public JdbcCursorItemReader<TranCatBalRecord> tranCatBalReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<TranCatBalRecord>()
                .name("tranCatBalReader")
                .dataSource(dataSource)
                .sql("SELECT acct_id, tran_type_cd, tran_cat_cd, tran_cat_bal, filler FROM tran_cat_balance"
                        + " ORDER BY acct_id, tran_type_cd COLLATE \"C\", tran_cat_cd")
                .rowMapper((rs, n) -> new TranCatBalRecord(rs.getLong(1), rs.getString(2), rs.getInt(3),
                        rs.getBigDecimal(4), rs.getString(5)))
                .build();
    }

    @Bean
    @StepScope
    public InterestCalculator interestCalculator(JdbcTemplate jdbcTemplate, Sysout sysout,
                                                 @Value("#{jobParameters['parm']}") String parm,
                                                 Supplier<LocalDateTime> carddemoClock) {
        return new InterestCalculator(jdbcTemplate, sysout, parm, carddemoClock);
    }

    @Bean
    @StepScope
    public PostingWriter postingWriter(JdbcTemplate jdbcTemplate) {
        return new PostingWriter(jdbcTemplate);
    }
}
