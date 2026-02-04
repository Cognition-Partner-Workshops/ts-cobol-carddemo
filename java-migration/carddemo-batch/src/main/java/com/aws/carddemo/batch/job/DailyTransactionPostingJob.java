package com.aws.carddemo.batch.job;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Transaction;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
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

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Daily Transaction Posting Job - migrated from CBTRN02C (POSTTRAN)
 * Part of the daily batch cycle: CLOSEFIL → CBPAUP0J → POSTTRAN → WAITSTEP → OPENFIL
 * 
 * This job processes unprocessed transactions and marks them as posted.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DailyTransactionPostingJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Bean
    public Job postTransactionsJob(Step postTransactionsStep) {
        return new JobBuilder("postTransactionsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(postTransactionsStep)
                .build();
    }

    @Bean
    public Step postTransactionsStep(ItemReader<Transaction> unprocessedTransactionReader,
                                      ItemProcessor<Transaction, Transaction> transactionProcessor,
                                      ItemWriter<Transaction> transactionWriter) {
        return new StepBuilder("postTransactionsStep", jobRepository)
                .<Transaction, Transaction>chunk(100, transactionManager)
                .reader(unprocessedTransactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Transaction> unprocessedTransactionReader() {
        return new RepositoryItemReaderBuilder<Transaction>()
                .name("unprocessedTransactionReader")
                .repository(transactionRepository)
                .methodName("findUnprocessedTransactions")
                .sorts(Collections.singletonMap("transactionId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        return transaction -> {
            log.debug("Processing transaction: {}", transaction.getTransactionId());
            transaction.setProcessTimestamp(LocalDateTime.now());
            return transaction;
        };
    }

    @Bean
    public ItemWriter<Transaction> transactionWriter() {
        return transactions -> {
            for (Transaction transaction : transactions) {
                transactionRepository.save(transaction);
                log.debug("Posted transaction: {}", transaction.getTransactionId());
            }
            log.info("Posted {} transactions", transactions.size());
        };
    }
}
