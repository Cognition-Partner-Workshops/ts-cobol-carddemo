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

import java.util.Comparator;
import java.util.List;

/**
 * Spring Batch job to combine/merge transaction files - migrated from JCL COMBTRAN.
 * Original JCL: Uses SORT utility to merge daily transaction file with transaction master.
 * Java equivalent: Merge daily_transactions into transactions table, sorted by timestamp.
 * Replaces DFSORT/SYNCSORT with Java Comparator sorting.
 */
@Configuration
public class CombineTransactionJob {

    private static final Logger log = LoggerFactory.getLogger(CombineTransactionJob.class);

    private final DailyTransactionRepository dailyTransactionRepository;
    private final TransactionRepository transactionRepository;

    public CombineTransactionJob(DailyTransactionRepository dailyTransactionRepository,
                                 TransactionRepository transactionRepository) {
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.transactionRepository = transactionRepository;
    }

    @Bean
    public Job combineTransactionBatchJob(JobRepository jobRepository, Step combineTransactionStep) {
        return new JobBuilder("combineTransactionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(combineTransactionStep)
                .build();
    }

    @Bean
    public Step combineTransactionStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("combineTransactionStep", jobRepository)
                .tasklet(combineTransactionTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet combineTransactionTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting transaction combine/merge...");

            List<DailyTransaction> dailyTransactions = dailyTransactionRepository.findAll();

            // Sort by timestamp (replaces DFSORT FIELDS=(ORIG-TIMESTAMP,A))
            dailyTransactions.sort(Comparator.comparing(
                    dt -> dt.getOrigTimestamp() != null ? dt.getOrigTimestamp() : "",
                    Comparator.naturalOrder()));

            int merged = 0;
            for (DailyTransaction dt : dailyTransactions) {
                if (!transactionRepository.existsById(dt.getTranId())) {
                    Transaction transaction = new Transaction();
                    transaction.setTranId(dt.getTranId());
                    transaction.setTranTypeCd(dt.getTranTypeCd());
                    transaction.setTranCatCd(dt.getTranCatCd());
                    transaction.setTranSource(dt.getTranSource());
                    transaction.setTranDesc(dt.getTranDesc());
                    transaction.setTranAmt(dt.getTranAmt());
                    transaction.setMerchantId(dt.getMerchantId());
                    transaction.setMerchantName(dt.getMerchantName());
                    transaction.setMerchantCity(dt.getMerchantCity());
                    transaction.setMerchantZip(dt.getMerchantZip());
                    transaction.setCardNum(dt.getCardNum());
                    transaction.setOrigTimestamp(dt.getOrigTimestamp());
                    transaction.setProcTimestamp(dt.getProcTimestamp());
                    transactionRepository.save(transaction);
                    merged++;
                }
            }

            log.info("Transaction combine complete. Merged {} new records", merged);
            return RepeatStatus.FINISHED;
        };
    }
}
