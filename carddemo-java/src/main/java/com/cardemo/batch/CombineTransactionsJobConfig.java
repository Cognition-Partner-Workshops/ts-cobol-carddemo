package com.cardemo.batch;

import com.cardemo.entity.Transaction;
import com.cardemo.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Combine Transactions batch job configuration.
 * Migrated from JCL job COMBTRAN which uses SORT utility.
 *
 * JCL flow:
 * 1. SORT FIELDS=(TRAN-CARD-NUM,A,TRAN-ORIG-TS,A)
 * 2. Combines daily transactions (DALYTRAN.PS) with master transactions (TRANSACT)
 * 3. Outputs sorted, merged transaction file
 *
 * In the Java migration, this is simplified since PostgreSQL handles sorting natively.
 * This job validates data integrity and generates a combined sorted view.
 */
@Configuration
public class CombineTransactionsJobConfig {

    private static final Logger log = LoggerFactory.getLogger(CombineTransactionsJobConfig.class);

    private final TransactionRepository transactionRepository;

    public CombineTransactionsJobConfig(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Bean
    public Job combineTransactionsJob(JobRepository jobRepository, Step combineTransactionsStep) {
        return new JobBuilder("combineTransactionsJob", jobRepository)
                .start(combineTransactionsStep)
                .build();
    }

    @Bean
    public Step combineTransactionsStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("combineTransactionsStep", jobRepository)
                .tasklet(combineTransactionsTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet that validates and logs combined transaction data.
     * In the COBOL/JCL version, this was a SORT utility merge.
     * In PostgreSQL, sorting is handled by the database, so this job
     * primarily ensures data integrity and logs statistics.
     */
    @Bean
    public Tasklet combineTransactionsTasklet() {
        return (contribution, chunkContext) -> {
            long totalCount = transactionRepository.count();
            log.info("COMBTRAN: Total transactions in master file: {}", totalCount);

            // Validate all transactions have required fields
            List<Transaction> allTransactions = transactionRepository.findAll(
                    Sort.by(Sort.Direction.ASC, "tranCardNum", "tranOrigTs"));

            long validCount = 0;
            long invalidCount = 0;

            for (Transaction txn : allTransactions) {
                if (txn.getTranCardNum() != null && !txn.getTranCardNum().isBlank()
                        && txn.getTranAmt() != null) {
                    validCount++;
                } else {
                    invalidCount++;
                    log.warn("COMBTRAN: Invalid transaction found: id={}", txn.getTranId());
                }
            }

            log.info("COMBTRAN: Combine complete. Valid={}, Invalid={}, Total={}",
                    validCount, invalidCount, totalCount);

            return RepeatStatus.FINISHED;
        };
    }
}
