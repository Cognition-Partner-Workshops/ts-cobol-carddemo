package com.aws.carddemo.batch;

import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.BatchJobLog;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.BatchJobLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

@Configuration
public class AccountExpirationBatchJob {

    private static final Logger log = LoggerFactory.getLogger(AccountExpirationBatchJob.class);

    private final AccountRepository accountRepository;
    private final BatchJobLogRepository batchJobLogRepository;

    public AccountExpirationBatchJob(AccountRepository accountRepository,
                                      BatchJobLogRepository batchJobLogRepository) {
        this.accountRepository = accountRepository;
        this.batchJobLogRepository = batchJobLogRepository;
    }

    @Bean
    public Job accountExpirationJob(JobRepository jobRepository, Step accountExpirationStep) {
        return new JobBuilder("accountExpirationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(accountExpirationStep)
                .build();
    }

    @Bean
    public Step accountExpirationStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       ItemReader<Account> expiredAccountReader,
                                       ItemProcessor<Account, Account> expirationProcessor,
                                       ItemWriter<Account> expirationWriter) {
        return new StepBuilder("accountExpirationStep", jobRepository)
                .<Account, Account>chunk(100, transactionManager)
                .reader(expiredAccountReader)
                .processor(expirationProcessor)
                .writer(expirationWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> expiredAccountReader() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("expiredAccountReader")
                .repository(accountRepository)
                .methodName("findExpiredAccounts")
                .arguments(LocalDate.now())
                .sorts(Collections.singletonMap("acctId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Account, Account> expirationProcessor() {
        return account -> {
            if (account.isActive()) {
                log.info("Deactivating expired account: {}", account.getAcctId());
                account.setAcctActiveStatus("N");
                return account;
            }
            return null;
        };
    }

    @Bean
    public ItemWriter<Account> expirationWriter() {
        return accounts -> {
            long count = 0;
            for (Account account : accounts) {
                accountRepository.save(account);
                count++;
            }
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("ACCTEXP")
                    .stepName("DEACTIVATE_EXPIRED")
                    .status("COMPLETED")
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .recordsProcessed(count)
                    .recordsRejected(0L)
                    .build();
            batchJobLogRepository.save(jobLog);
        };
    }
}
