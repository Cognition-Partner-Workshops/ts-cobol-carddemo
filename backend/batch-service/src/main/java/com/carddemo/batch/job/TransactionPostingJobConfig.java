package com.carddemo.batch.job;

import com.carddemo.common.entity.Transaction;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Configuration
public class TransactionPostingJobConfig {

    @Bean
    public Job transactionPostingJob(JobRepository jobRepository, Step transactionPostingStep) {
        return new JobBuilder("transactionPostingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transactionPostingStep)
                .build();
    }

    @Bean
    public Step transactionPostingStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       JpaPagingItemReader<Transaction> pendingTransactionReader,
                                       ItemProcessor<Transaction, Transaction> transactionPostingProcessor,
                                       JpaItemWriter<Transaction> transactionWriter) {
        return new StepBuilder("transactionPostingStep", jobRepository)
                .<Transaction, Transaction>chunk(100, transactionManager)
                .reader(pendingTransactionReader)
                .processor(transactionPostingProcessor)
                .writer(transactionWriter)
                .build();
    }

    @Bean
    public JpaPagingItemReader<Transaction> pendingTransactionReader(EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<Transaction>()
                .name("pendingTransactionReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT t FROM Transaction t WHERE t.processingTimestamp IS NULL")
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> transactionPostingProcessor() {
        return transaction -> {
            transaction.setProcessingTimestamp(LocalDateTime.now());
            return transaction;
        };
    }

    @Bean
    public JpaItemWriter<Transaction> transactionWriter(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Transaction>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
