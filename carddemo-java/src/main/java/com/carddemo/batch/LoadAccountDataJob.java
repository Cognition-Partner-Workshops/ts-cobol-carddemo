package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;

/**
 * Spring Batch job to load account data - migrated from JCL ACCTFILE job.
 * Original JCL: DEFINE CLUSTER for ACCTFILE VSAM KSDS, REPRO from flat file.
 * Java equivalent: Read account data and load into accounts table.
 */
@Configuration
public class LoadAccountDataJob {

    private static final Logger log = LoggerFactory.getLogger(LoadAccountDataJob.class);

    private final AccountRepository accountRepository;

    public LoadAccountDataJob(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Bean
    public Job loadAccountJob(JobRepository jobRepository, Step loadAccountStep) {
        return new JobBuilder("loadAccountJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(loadAccountStep)
                .build();
    }

    @Bean
    public Step loadAccountStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("loadAccountStep", jobRepository)
                .tasklet(loadAccountTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet loadAccountTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting account data load...");

            // Sample seed data matching COBOL copybook CVACT01Y structure
            if (accountRepository.count() == 0) {
                Account acct1 = new Account();
                acct1.setAcctId(1000000001L);
                acct1.setActiveStatus("Y");
                acct1.setCurrentBalance(new BigDecimal("1500.00"));
                acct1.setCreditLimit(new BigDecimal("5000.00"));
                acct1.setCashCreditLimit(new BigDecimal("1500.00"));
                acct1.setOpenDate("2020-01-15");
                acct1.setExpirationDate("2026-01-15");
                acct1.setAddressZip("10001");
                acct1.setGroupId("GRP001");
                accountRepository.save(acct1);

                Account acct2 = new Account();
                acct2.setAcctId(1000000002L);
                acct2.setActiveStatus("Y");
                acct2.setCurrentBalance(new BigDecimal("3200.50"));
                acct2.setCreditLimit(new BigDecimal("10000.00"));
                acct2.setCashCreditLimit(new BigDecimal("3000.00"));
                acct2.setOpenDate("2019-06-20");
                acct2.setExpirationDate("2025-06-20");
                acct2.setAddressZip("90210");
                acct2.setGroupId("GRP002");
                accountRepository.save(acct2);

                log.info("Loaded {} account records", accountRepository.count());
            } else {
                log.info("Account data already loaded, skipping. Count: {}", accountRepository.count());
            }

            return RepeatStatus.FINISHED;
        };
    }
}
