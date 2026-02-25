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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Batch job for transaction report - migrated from JCL TRANREPT / CBTRN03C.
 * Original COBOL CBTRN03C:
 * 1. Read transaction master file sequentially
 * 2. Accumulate totals by transaction type
 * 3. Print summary report
 */
@Configuration
public class TransactionReportJob {

    private static final Logger log = LoggerFactory.getLogger(TransactionReportJob.class);

    private final TransactionRepository transactionRepository;

    public TransactionReportJob(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Bean
    public Job transactionReportBatchJob(JobRepository jobRepository, Step transactionReportStep) {
        return new JobBuilder("transactionReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transactionReportStep)
                .build();
    }

    @Bean
    public Step transactionReportStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("transactionReportStep", jobRepository)
                .tasklet(transactionReportTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet transactionReportTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting transaction report generation...");
            String reportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            List<Transaction> transactions = transactionRepository.findAll();

            Map<String, BigDecimal> totalsByType = new HashMap<>();
            Map<String, Integer> countByType = new HashMap<>();
            BigDecimal grandTotal = BigDecimal.ZERO;

            for (Transaction t : transactions) {
                String typeCd = t.getTranTypeCd() != null ? t.getTranTypeCd() : "??";
                BigDecimal amt = t.getTranAmt() != null ? t.getTranAmt() : BigDecimal.ZERO;

                totalsByType.merge(typeCd, amt, BigDecimal::add);
                countByType.merge(typeCd, 1, Integer::sum);
                grandTotal = grandTotal.add(amt);
            }

            // Print report (replaces COBOL WRITE to print file)
            log.info("=== TRANSACTION REPORT - {} ===", reportDate);
            log.info("Total transactions: {}", transactions.size());
            log.info("Grand total amount: {}", grandTotal);
            log.info("--- By Type ---");
            for (Map.Entry<String, BigDecimal> entry : totalsByType.entrySet()) {
                log.info("Type {}: Count={}, Total={}",
                        entry.getKey(), countByType.get(entry.getKey()), entry.getValue());
            }
            log.info("=== END REPORT ===");

            return RepeatStatus.FINISHED;
        };
    }
}
