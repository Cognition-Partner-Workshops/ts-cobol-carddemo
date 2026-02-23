package com.carddemo.batch.job;

import com.carddemo.core.domain.Transaction;
import com.carddemo.core.repository.AccountRepository;
import com.carddemo.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

/**
 * Spring Batch job configuration for Transaction Posting.
 * Replaces JCL job POSTTRAN executing COBOL program CBTRN02C.
 *
 * JCL → Spring Batch mapping:
 *   //POSTTRAN JOB ... → transactionPostingJob
 *   //STEP1   EXEC PGM=CBTRN02C → transactionPostingStep
 *   //TRFILE  DD ... → RepositoryItemReader (reads from TRANSACTION table)
 *   //ACCTFILE DD ... → ItemWriter (updates ACCOUNT table)
 *
 * Business logic (Phase 3):
 *   1. Read unprocessed transactions
 *   2. Validate transaction against account limits
 *   3. Update account balances (debit/credit)
 *   4. Update transaction category balances
 *   5. Mark transaction as processed
 */
@Configuration
@RequiredArgsConstructor
public class TransactionPostingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Bean
    public Job transactionPostingJob() {
        return new JobBuilder("transactionPostingJob", jobRepository)
                .start(transactionPostingStep())
                .build();
    }

    @Bean
    public Step transactionPostingStep() {
        return new StepBuilder("transactionPostingStep", jobRepository)
                .<Transaction, Transaction>chunk(100, transactionManager)
                .reader(transactionReader())
                .processor(transactionProcessor())
                .writer(transactionWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Transaction> transactionReader() {
        return new RepositoryItemReaderBuilder<Transaction>()
                .name("transactionReader")
                .repository(transactionRepository)
                .methodName("findAll")
                .sorts(Map.of("origTimestamp", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        // TODO Phase 3: Implement CBTRN02C business logic
        // - Validate transaction against account credit limits
        // - Calculate fees based on transaction type/category
        // - Check for fraud indicators
        return transaction -> transaction;
    }

    @Bean
    public ItemWriter<Transaction> transactionWriter() {
        // TODO Phase 3: Implement account balance update logic
        // - Update ACCOUNT.CURRENT_BALANCE
        // - Update ACCOUNT.CURRENT_CYCLE_DEBIT or CURRENT_CYCLE_CREDIT
        // - Update TRANSACTION_CATEGORY_BALANCE
        return chunk -> transactionRepository.saveAll(chunk.getItems());
    }
}
