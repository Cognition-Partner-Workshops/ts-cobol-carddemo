package com.aws.cardemo.migration.importer;

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
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AccountDataImportJobConfig {

    private final AccountRepository accountRepository;

    @Bean
    public Job accountImportJob(JobRepository jobRepository, Step importAccountsStep) {
        return new JobBuilder("accountImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(importAccountsStep)
                .build();
    }

    @Bean
    public Step importAccountsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Account> accountFileReader,
            ItemProcessor<Account, Account> accountImportProcessor,
            ItemWriter<Account> accountImportWriter) {
        return new StepBuilder("importAccountsStep", jobRepository)
                .<Account, Account>chunk(100, transactionManager)
                .reader(accountFileReader)
                .processor(accountImportProcessor)
                .writer(accountImportWriter)
                .build();
    }

    @Bean
    public FlatFileItemReader<Account> accountFileReader() {
        return new FlatFileItemReaderBuilder<Account>()
                .name("accountFileReader")
                .resource(new ClassPathResource("data/accounts.csv"))
                .delimited()
                .names("accountId", "accountStatus", "currentBalance", "creditLimit", 
                       "openDate", "expirationDate", "groupId")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Account.class);
                }})
                .linesToSkip(1)
                .build();
    }

    @Bean
    public ItemProcessor<Account, Account> accountImportProcessor() {
        return account -> {
            log.info("Processing account for import: {}", account.getAccountId());
            return account;
        };
    }

    @Bean
    public ItemWriter<Account> accountImportWriter() {
        return accounts -> {
            for (Account account : accounts) {
                log.info("Importing account: {}", account.getAccountId());
                accountRepository.save(account);
            }
        };
    }
}
