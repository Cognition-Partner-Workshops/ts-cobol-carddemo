package com.aws.cardemo.batch.job;

import com.aws.cardemo.domain.entity.Account;
import com.aws.cardemo.persistence.repository.AccountRepository;
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
public class AccountStatementJobConfig {

    private final AccountRepository accountRepository;

    @Bean
    public Job accountStatementJob(JobRepository jobRepository, Step generateStatementsStep) {
        return new JobBuilder("accountStatementJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(generateStatementsStep)
                .build();
    }

    @Bean
    public Step generateStatementsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Account> accountReader,
            ItemProcessor<Account, Account> statementProcessor,
            ItemWriter<Account> statementWriter) {
        return new StepBuilder("generateStatementsStep", jobRepository)
                .<Account, Account>chunk(50, transactionManager)
                .reader(accountReader)
                .processor(statementProcessor)
                .writer(statementWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> accountReader() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("accountReader")
                .repository(accountRepository)
                .methodName("findAll")
                .sorts(Map.of("accountId", Sort.Direction.ASC))
                .pageSize(50)
                .build();
    }

    @Bean
    public ItemProcessor<Account, Account> statementProcessor() {
        return account -> {
            log.info("Generating statement for account: {}", account.getAccountId());
            return account;
        };
    }

    @Bean
    public ItemWriter<Account> statementWriter() {
        return accounts -> {
            for (Account account : accounts) {
                log.info("Statement generated for account: {}", account.getAccountId());
            }
        };
    }
}
