package com.carddemo.batch;

import com.carddemo.entity.DailyTransaction;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.DailyTransactionRepository;
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

/**
 * Spring Batch job to backup transaction data - migrated from JCL TRANBKP job.
 * Original JCL: REPRO from TRANSACT VSAM to sequential backup file.
 * Java equivalent: Copy transactions to daily_transactions backup table.
 */
@Configuration
public class BackupTransactionJob {

    private static final Logger log = LoggerFactory.getLogger(BackupTransactionJob.class);

    private final TransactionRepository transactionRepository;
    private final DailyTransactionRepository dailyTransactionRepository;

    public BackupTransactionJob(TransactionRepository transactionRepository,
                                DailyTransactionRepository dailyTransactionRepository) {
        this.transactionRepository = transactionRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
    }

    @Bean
    public Job backupTransactionBatchJob(JobRepository jobRepository, Step backupTransactionStep) {
        return new JobBuilder("backupTransactionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(backupTransactionStep)
                .build();
    }

    @Bean
    public Step backupTransactionStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("backupTransactionStep", jobRepository)
                .tasklet(backupTransactionTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet backupTransactionTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting transaction backup...");

            List<Transaction> transactions = transactionRepository.findAll();
            int count = 0;
            for (Transaction t : transactions) {
                DailyTransaction daily = new DailyTransaction();
                daily.setTranId(t.getTranId());
                daily.setTranTypeCd(t.getTranTypeCd());
                daily.setTranCatCd(t.getTranCatCd());
                daily.setTranSource(t.getTranSource());
                daily.setTranDesc(t.getTranDesc());
                daily.setTranAmt(t.getTranAmt());
                daily.setMerchantId(t.getMerchantId());
                daily.setMerchantName(t.getMerchantName());
                daily.setMerchantCity(t.getMerchantCity());
                daily.setMerchantZip(t.getMerchantZip());
                daily.setCardNum(t.getCardNum());
                daily.setOrigTimestamp(t.getOrigTimestamp());
                daily.setProcTimestamp(t.getProcTimestamp());
                dailyTransactionRepository.save(daily);
                count++;
            }

            log.info("Backed up {} transaction records", count);
            return RepeatStatus.FINISHED;
        };
    }
}
