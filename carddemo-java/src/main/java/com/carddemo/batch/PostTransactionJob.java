package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.DailyTransaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.DailyTransactionRepository;
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
import java.util.List;

/**
 * Spring Batch job for core transaction processing - migrated from JCL POSTTRAN / CBTRN02C.
 * Original COBOL CBTRN02C: Reads daily transaction file, validates each transaction,
 * posts to account (updates balance), writes to transaction master file.
 */
@Configuration
public class PostTransactionJob {

    private static final Logger log = LoggerFactory.getLogger(PostTransactionJob.class);

    private final DailyTransactionRepository dailyTransactionRepository;
    private final AccountRepository accountRepository;
    private final CardAccountXrefRepository xrefRepository;

    public PostTransactionJob(DailyTransactionRepository dailyTransactionRepository,
                              AccountRepository accountRepository,
                              CardAccountXrefRepository xrefRepository) {
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.accountRepository = accountRepository;
        this.xrefRepository = xrefRepository;
    }

    @Bean
    public Job postTransactionBatchJob(JobRepository jobRepository, Step postTransactionStep) {
        return new JobBuilder("postTransactionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(postTransactionStep)
                .build();
    }

    @Bean
    public Step postTransactionStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager) {
        return new StepBuilder("postTransactionStep", jobRepository)
                .tasklet(postTransactionTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet postTransactionTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting transaction posting...");

            List<DailyTransaction> dailyTransactions = dailyTransactionRepository.findAll();
            int posted = 0;
            int errors = 0;

            for (DailyTransaction dt : dailyTransactions) {
                try {
                    // Find account for this card
                    List<CardAccountXref> xrefs = xrefRepository.findByCardNum(dt.getCardNum());
                    if (xrefs.isEmpty()) {
                        log.warn("No xref found for card: {}", dt.getCardNum());
                        errors++;
                        continue;
                    }

                    Long acctId = xrefs.get(0).getAcctId();
                    Account account = accountRepository.findById(acctId).orElse(null);
                    if (account == null) {
                        log.warn("Account not found: {}", acctId);
                        errors++;
                        continue;
                    }

                    // Post transaction to account balance
                    BigDecimal currentBalance = account.getCurrentBalance() != null
                            ? account.getCurrentBalance() : BigDecimal.ZERO;
                    BigDecimal tranAmt = dt.getTranAmt() != null ? dt.getTranAmt() : BigDecimal.ZERO;
                    account.setCurrentBalance(currentBalance.add(tranAmt));

                    // Update cycle debit/credit
                    if (tranAmt.compareTo(BigDecimal.ZERO) >= 0) {
                        BigDecimal cycleDebit = account.getCurrentCycleDebit() != null
                                ? account.getCurrentCycleDebit() : BigDecimal.ZERO;
                        account.setCurrentCycleDebit(cycleDebit.add(tranAmt));
                    } else {
                        BigDecimal cycleCredit = account.getCurrentCycleCredit() != null
                                ? account.getCurrentCycleCredit() : BigDecimal.ZERO;
                        account.setCurrentCycleCredit(cycleCredit.add(tranAmt.abs()));
                    }

                    accountRepository.save(account);
                    posted++;
                } catch (Exception e) {
                    log.error("Error posting transaction {}: {}", dt.getTranId(), e.getMessage());
                    errors++;
                }
            }

            log.info("Transaction posting complete. Posted: {}, Errors: {}", posted, errors);
            return RepeatStatus.FINISHED;
        };
    }
}
