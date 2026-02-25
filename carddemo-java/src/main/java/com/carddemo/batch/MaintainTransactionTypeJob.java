package com.carddemo.batch;

import com.carddemo.entity.TransactionType;
import com.carddemo.repository.TransactionTypeRepository;
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

/**
 * Spring Batch job for transaction type maintenance - migrated from Phase 5a COBTUPDT / MNTTRDB2.
 * Original COBOL COBTUPDT: Batch program that reads an input file of transaction type
 * updates and applies them to the DB2 TRANSACTION_TYPE table.
 */
@Configuration
public class MaintainTransactionTypeJob {

    private static final Logger log = LoggerFactory.getLogger(MaintainTransactionTypeJob.class);

    private final TransactionTypeRepository typeRepository;

    public MaintainTransactionTypeJob(TransactionTypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }

    @Bean
    public Job maintainTranTypeBatchJob(JobRepository jobRepository, Step maintainTranTypeStep) {
        return new JobBuilder("maintainTransactionTypeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(maintainTranTypeStep)
                .build();
    }

    @Bean
    public Step maintainTranTypeStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("maintainTranTypeStep", jobRepository)
                .tasklet(maintainTranTypeTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet maintainTranTypeTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting transaction type maintenance...");

            // Seed default transaction types if none exist
            if (typeRepository.count() == 0) {
                String[][] defaultTypes = {
                        {"01", "Purchase"},
                        {"02", "Cash Advance"},
                        {"03", "Balance Transfer"},
                        {"04", "Payment"},
                        {"05", "Fee"},
                        {"06", "Interest Charge"},
                        {"07", "Refund"},
                        {"08", "Adjustment"}
                };

                for (String[] typeData : defaultTypes) {
                    TransactionType type = new TransactionType();
                    type.setTypeCd(typeData[0]);
                    type.setTypeDesc(typeData[1]);
                    typeRepository.save(type);
                }

                log.info("Loaded {} default transaction types", typeRepository.count());
            } else {
                log.info("Transaction types already exist. Count: {}", typeRepository.count());
            }

            return RepeatStatus.FINISHED;
        };
    }
}
