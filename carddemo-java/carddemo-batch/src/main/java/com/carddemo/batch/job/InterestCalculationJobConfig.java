package com.carddemo.batch.job;

import com.carddemo.core.domain.Account;
import com.carddemo.core.repository.AccountRepository;
import com.carddemo.core.repository.DisclosureGroupRepository;
import com.carddemo.core.repository.TransactionCategoryBalanceRepository;
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
 * Spring Batch job configuration for Interest Calculation.
 * Replaces JCL job INTCALC executing COBOL program CBACT04C.
 *
 * JCL → Spring Batch mapping:
 *   //INTCALC  JOB ... → interestCalculationJob
 *   //STEP1    EXEC PGM=CBACT04C → interestCalculationStep
 *   //ACCTFILE DD ... → RepositoryItemReader (reads from ACCOUNT table)
 *   //TCATBAL  DD ... → Reader (reads TRANSACTION_CATEGORY_BALANCE)
 *   //DISCGRP  DD ... → Reader (reads DISCLOSURE_GROUP for interest rates)
 *
 * Business logic (Phase 3):
 *   1. Read each active account
 *   2. Look up transaction category balances for the account
 *   3. Look up disclosure group interest rates for the account group
 *   4. Calculate interest = balance * rate / 365 * days
 *   5. Add interest transaction and update account balance
 */
@Configuration
@RequiredArgsConstructor
public class InterestCalculationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository tcbRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;

    @Bean
    public Job interestCalculationJob() {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .start(interestCalculationStep())
                .build();
    }

    @Bean
    public Step interestCalculationStep() {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .<Account, Account>chunk(100, transactionManager)
                .reader(accountReader())
                .processor(interestProcessor())
                .writer(accountWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> accountReader() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("accountReader")
                .repository(accountRepository)
                .methodName("findAll")
                .sorts(Map.of("acctId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Account, Account> interestProcessor() {
        // TODO Phase 3: Implement CBACT04C interest calculation logic
        // - Look up transaction category balances for account
        // - Look up disclosure group rates for account's group ID
        // - Calculate interest per category: balance * rate / 365 * days_in_period
        // - Create interest transaction records
        // - Update account balance with accrued interest
        return account -> account;
    }

    @Bean
    public ItemWriter<Account> accountWriter() {
        // TODO Phase 3: Persist updated account balances and interest transactions
        return chunk -> accountRepository.saveAll(chunk.getItems());
    }
}
