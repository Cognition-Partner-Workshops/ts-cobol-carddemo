package com.carddemo.batch;

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

@Configuration
public class DataLoadJob {

    private static final Logger log = LoggerFactory.getLogger(DataLoadJob.class);

    @Bean
    public Job accountLoadBatchJob(JobRepository jobRepository, Step accountLoadStep) {
        return new JobBuilder("AccountLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(accountLoadStep)
                .build();
    }

    @Bean
    public Step accountLoadStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return createLoadStep("accountLoadStep", "Account", jobRepository, transactionManager);
    }

    @Bean
    public Job cardLoadBatchJob(JobRepository jobRepository, Step cardLoadStep) {
        return new JobBuilder("CardLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cardLoadStep)
                .build();
    }

    @Bean
    public Step cardLoadStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
        return createLoadStep("cardLoadStep", "Card", jobRepository, transactionManager);
    }

    @Bean
    public Job xrefLoadBatchJob(JobRepository jobRepository, Step xrefLoadStep) {
        return new JobBuilder("XrefLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(xrefLoadStep)
                .build();
    }

    @Bean
    public Step xrefLoadStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
        return createLoadStep("xrefLoadStep", "Cross Reference", jobRepository, transactionManager);
    }

    @Bean
    public Job customerLoadBatchJob(JobRepository jobRepository, Step customerLoadStep) {
        return new JobBuilder("CustomerLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerLoadStep)
                .build();
    }

    @Bean
    public Step customerLoadStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        return createLoadStep("customerLoadStep", "Customer", jobRepository, transactionManager);
    }

    @Bean
    public Job transactionCategoryLoadBatchJob(JobRepository jobRepository,
                                               Step transactionCategoryLoadStep) {
        return new JobBuilder("TransactionCategoryLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transactionCategoryLoadStep)
                .build();
    }

    @Bean
    public Step transactionCategoryLoadStep(JobRepository jobRepository,
                                            PlatformTransactionManager transactionManager) {
        return createLoadStep("transactionCategoryLoadStep", "Transaction Category",
                jobRepository, transactionManager);
    }

    @Bean
    public Job transactionTypeLoadBatchJob(JobRepository jobRepository,
                                           Step transactionTypeLoadStep) {
        return new JobBuilder("TransactionTypeLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transactionTypeLoadStep)
                .build();
    }

    @Bean
    public Step transactionTypeLoadStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return createLoadStep("transactionTypeLoadStep", "Transaction Type",
                jobRepository, transactionManager);
    }

    @Bean
    public Job disclosureGroupLoadBatchJob(JobRepository jobRepository,
                                           Step disclosureGroupLoadStep) {
        return new JobBuilder("DisclosureGroupLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(disclosureGroupLoadStep)
                .build();
    }

    @Bean
    public Step disclosureGroupLoadStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return createLoadStep("disclosureGroupLoadStep", "Disclosure Group",
                jobRepository, transactionManager);
    }

    @Bean
    public Job transactionCategoryBalanceLoadBatchJob(JobRepository jobRepository,
                                                      Step transactionCategoryBalanceLoadStep) {
        return new JobBuilder("TransactionCategoryBalanceLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transactionCategoryBalanceLoadStep)
                .build();
    }

    @Bean
    public Step transactionCategoryBalanceLoadStep(JobRepository jobRepository,
                                                   PlatformTransactionManager transactionManager) {
        return createLoadStep("transactionCategoryBalanceLoadStep", "Transaction Category Balance",
                jobRepository, transactionManager);
    }

    @Bean
    public Job userSecurityLoadBatchJob(JobRepository jobRepository,
                                        Step userSecurityLoadStep) {
        return new JobBuilder("UserSecurityLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(userSecurityLoadStep)
                .build();
    }

    @Bean
    public Step userSecurityLoadStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        return createLoadStep("userSecurityLoadStep", "User Security",
                jobRepository, transactionManager);
    }

    private Step createLoadStep(String stepName, String dataType,
                                JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("START OF {} DATA LOAD", dataType.toUpperCase());
            log.info("{} data load completed via Flyway migration scripts", dataType);
            log.info("END OF {} DATA LOAD", dataType.toUpperCase());
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder(stepName, jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
