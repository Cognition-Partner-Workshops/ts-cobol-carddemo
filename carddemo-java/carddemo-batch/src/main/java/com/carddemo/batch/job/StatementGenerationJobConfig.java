package com.carddemo.batch.job;

import com.carddemo.core.domain.Account;
import com.carddemo.core.repository.AccountRepository;
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
 * Spring Batch job configuration for Statement Generation.
 * Replaces JCL job CREASTMT executing COBOL program CBSTM03A.
 *
 * JCL → Spring Batch mapping:
 *   //CREASTMT JOB ... → statementGenerationJob
 *   //STEP1    EXEC PGM=CBSTM03A → statementGenerationStep
 *   //ACCTFILE DD ... → RepositoryItemReader (reads ACCOUNT table)
 *   //TRFILE   DD ... → Reader (reads TRANSACTION table for each account)
 *   //STMTFILE DD ... → Writer (generates statement output)
 *
 * Business logic (Phase 3):
 *   1. Read each active account
 *   2. Gather all transactions for the billing cycle
 *   3. Calculate statement totals (debits, credits, balance)
 *   4. Format and output statement (PDF or structured data)
 */
@Configuration
@RequiredArgsConstructor
public class StatementGenerationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Bean
    public Job statementGenerationJob() {
        return new JobBuilder("statementGenerationJob", jobRepository)
                .start(statementGenerationStep())
                .build();
    }

    @Bean
    public Step statementGenerationStep() {
        return new StepBuilder("statementGenerationStep", jobRepository)
                .<Account, Account>chunk(50, transactionManager)
                .reader(statementAccountReader())
                .processor(statementProcessor())
                .writer(statementWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> statementAccountReader() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("statementAccountReader")
                .repository(accountRepository)
                .methodName("findAll")
                .sorts(Map.of("acctId", Sort.Direction.ASC))
                .pageSize(50)
                .build();
    }

    @Bean
    public ItemProcessor<Account, Account> statementProcessor() {
        // TODO Phase 3: Implement CBSTM03A statement generation logic
        // - Query transactions for current billing cycle
        // - Calculate statement summary (total debits, credits, fees, interest)
        // - Calculate minimum payment due
        // - Format statement data
        return account -> account;
    }

    @Bean
    public ItemWriter<Account> statementWriter() {
        // TODO Phase 3: Generate statement output (PDF/structured data)
        // - Write statement to file system or document store
        // - Update account with statement generation date
        return chunk -> {
            // Placeholder: statements would be generated here
        };
    }
}
