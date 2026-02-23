package com.carddemo.batch.job;

import com.carddemo.core.domain.Transaction;
import com.carddemo.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

/**
 * Spring Batch job configuration for Transaction Report.
 * Replaces JCL job TRANREPT executing COBOL program CBTRN03C.
 *
 * JCL → Spring Batch mapping:
 *   //TRANREPT JOB ... → transactionReportJob
 *   //STEP1    EXEC PGM=CBTRN03C → transactionReportStep
 *   //TRFILE   DD ... → RepositoryItemReader (reads TRANSACTION table)
 *   //RPTFILE  DD SYSOUT=* → Writer (report output)
 *
 * Business logic (Phase 3):
 *   1. Read all transactions for reporting period
 *   2. Group by transaction type/category
 *   3. Calculate subtotals and grand totals
 *   4. Format and output report
 */
@Configuration
@RequiredArgsConstructor
public class TransactionReportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionRepository transactionRepository;

    @Bean
    public Job transactionReportJob() {
        return new JobBuilder("transactionReportJob", jobRepository)
                .start(transactionReportStep())
                .build();
    }

    @Bean
    public Step transactionReportStep() {
        return new StepBuilder("transactionReportStep", jobRepository)
                .<Transaction, Transaction>chunk(200, transactionManager)
                .reader(reportTransactionReader())
                .processor(reportProcessor())
                .writer(reportWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Transaction> reportTransactionReader() {
        return new RepositoryItemReaderBuilder<Transaction>()
                .name("reportTransactionReader")
                .repository(transactionRepository)
                .methodName("findAll")
                .sorts(Map.of("origTimestamp", Sort.Direction.ASC))
                .pageSize(200)
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> reportProcessor() {
        // TODO Phase 3: Implement CBTRN03C report formatting logic
        // - Accumulate totals per transaction type
        // - Accumulate totals per transaction category
        // - Calculate percentages and averages
        return transaction -> transaction;
    }

    @Bean
    public ItemWriter<Transaction> reportWriter() {
        // TODO Phase 3: Generate report output
        // - Write formatted report to file or reporting service
        // - Include headers, detail lines, subtotals, grand totals
        return chunk -> {
            // Placeholder: report output would be generated here
        };
    }
}
