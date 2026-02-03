package com.aws.cardemo.batch.job;

import com.aws.cardemo.domain.entity.Transaction;
import com.aws.cardemo.persistence.repository.TransactionRepository;
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

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessingJobConfig {

    private final TransactionRepository transactionRepository;

    @Bean
    public Job transactionProcessingJob(JobRepository jobRepository, Step processTransactionsStep) {
        return new JobBuilder("transactionProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(processTransactionsStep)
                .build();
    }

    @Bean
    public Step processTransactionsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Transaction> transactionReader,
            ItemProcessor<Transaction, Transaction> transactionProcessor,
            ItemWriter<Transaction> transactionWriter) {
        return new StepBuilder("processTransactionsStep", jobRepository)
                .<Transaction, Transaction>chunk(100, transactionManager)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Transaction> transactionReader() {
        return new RepositoryItemReaderBuilder<Transaction>()
                .name("transactionReader")
                .repository(transactionRepository)
                .methodName("findAll")
                .sorts(Map.of("transactionId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        return transaction -> {
            log.debug("Processing transaction: {}", transaction.getTransactionId());
            return transaction;
        };
    }

    @Bean
    public ItemWriter<Transaction> transactionWriter() {
        return transactions -> {
            for (Transaction transaction : transactions) {
                log.debug("Writing transaction: {}", transaction.getTransactionId());
                transactionRepository.save(transaction);
            }
        };
    }
}
