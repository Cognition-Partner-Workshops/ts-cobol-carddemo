package com.carddemo.batch.job;

import com.carddemo.common.entity.Account;
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

import java.math.BigDecimal;
import java.math.RoundingMode;

@Configuration
public class InterestCalculationJobConfig {

    private static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.015");

    @Bean
    public Job interestCalculationJob(JobRepository jobRepository, Step interestCalculationStep) {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(interestCalculationStep)
                .build();
    }

    @Bean
    public Step interestCalculationStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        JpaPagingItemReader<Account> accountReader,
                                        ItemProcessor<Account, Account> interestProcessor,
                                        JpaItemWriter<Account> accountWriter) {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .<Account, Account>chunk(100, transactionManager)
                .reader(accountReader)
                .processor(interestProcessor)
                .writer(accountWriter)
                .build();
    }

    @Bean
    public JpaPagingItemReader<Account> accountReader(EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<Account>()
                .name("accountReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT a FROM Account a WHERE a.activeStatus = 'Y' AND a.currentBalance > 0")
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Account, Account> interestProcessor() {
        return account -> {
            BigDecimal interest = account.getCurrentBalance()
                    .multiply(MONTHLY_INTEREST_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            account.setCurrentBalance(account.getCurrentBalance().add(interest));
            account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(interest));
            return account;
        };
    }

    @Bean
    public JpaItemWriter<Account> accountWriter(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Account>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
