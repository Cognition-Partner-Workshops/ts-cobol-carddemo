package com.carddemo.batch;

import com.carddemo.entity.Transaction;
import com.carddemo.repository.TransactionRepository;
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

import java.util.List;

@Configuration
public class TransactionBackupJob {

    private static final Logger log = LoggerFactory.getLogger(TransactionBackupJob.class);

    private final TransactionRepository transactionRepository;

    public TransactionBackupJob(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Bean
    public Job transactionBackupBatchJob(JobRepository jobRepository,
                                         Step backupTransactionsStep) {
        return new JobBuilder("TransactionBackupJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(backupTransactionsStep)
                .build();
    }

    @Bean
    public Step backupTransactionsStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("START OF TRANSACTION BACKUP JOB");

            List<Transaction> transactions = transactionRepository.findAll();
            log.info("Total transactions to backup: {}", transactions.size());

            for (Transaction tran : transactions) {
                log.debug("Backing up transaction: {} | {} | {} | {}",
                        tran.getTranId(), tran.getCardNum(),
                        tran.getAmount(), tran.getOrigTs());
            }

            log.info("TRANSACTION BACKUP COMPLETE. Records: {}", transactions.size());
            log.info("END OF TRANSACTION BACKUP JOB");

            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("backupTransactionsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
