package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalanceId;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
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
import java.util.List;
import java.util.Optional;

@Configuration
public class TransactionPostingJob {

    private static final Logger log = LoggerFactory.getLogger(TransactionPostingJob.class);

    private final TransactionRepository transactionRepository;
    private final CardAccountXrefRepository xrefRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository tcatBalRepository;

    public TransactionPostingJob(TransactionRepository transactionRepository,
                                 CardAccountXrefRepository xrefRepository,
                                 AccountRepository accountRepository,
                                 TransactionCategoryBalanceRepository tcatBalRepository) {
        this.transactionRepository = transactionRepository;
        this.xrefRepository = xrefRepository;
        this.accountRepository = accountRepository;
        this.tcatBalRepository = tcatBalRepository;
    }

    @Bean
    public Job transactionPostingBatchJob(JobRepository jobRepository, Step postTransactionsStep) {
        return new JobBuilder("TransactionPostingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(postTransactionsStep)
                .build();
    }

    @Bean
    public Step postTransactionsStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("START OF EXECUTION OF TRANSACTION POSTING JOB");

            List<Transaction> unprocessed = transactionRepository.findAll();
            int processed = 0;
            int rejected = 0;

            for (Transaction tran : unprocessed) {
                if (tran.getProcTs() != null && !tran.getProcTs().isEmpty()) {
                    continue;
                }

                Optional<CardAccountXref> xrefOpt = xrefRepository.findById(tran.getCardNum());
                if (xrefOpt.isEmpty()) {
                    log.warn("Card not found in xref: {}", tran.getCardNum());
                    rejected++;
                    continue;
                }

                CardAccountXref xref = xrefOpt.get();
                Optional<Account> acctOpt = accountRepository.findById(xref.getAcctId());
                if (acctOpt.isEmpty()) {
                    log.warn("Account not found: {}", xref.getAcctId());
                    rejected++;
                    continue;
                }

                Account account = acctOpt.get();
                account.setCurrBal(account.getCurrBal().add(tran.getAmount()));

                if (tran.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal cycDebit = account.getCurrCycDebit() != null
                            ? account.getCurrCycDebit() : BigDecimal.ZERO;
                    account.setCurrCycDebit(cycDebit.add(tran.getAmount()));
                } else {
                    BigDecimal cycCredit = account.getCurrCycCredit() != null
                            ? account.getCurrCycCredit() : BigDecimal.ZERO;
                    account.setCurrCycCredit(cycCredit.add(tran.getAmount().abs()));
                }
                accountRepository.save(account);

                TransactionCategoryBalanceId balId = new TransactionCategoryBalanceId(
                        xref.getAcctId(), tran.getTypeCd(), tran.getCatCd());
                TransactionCategoryBalance balance = tcatBalRepository.findById(balId)
                        .orElseGet(() -> {
                            TransactionCategoryBalance newBal = new TransactionCategoryBalance();
                            newBal.setAcctId(xref.getAcctId());
                            newBal.setTypeCd(tran.getTypeCd());
                            newBal.setCatCd(tran.getCatCd());
                            newBal.setBalance(BigDecimal.ZERO);
                            return newBal;
                        });
                balance.setBalance(balance.getBalance().add(tran.getAmount()));
                tcatBalRepository.save(balance);

                processed++;
            }

            log.info("TRANSACTIONS PROCESSED: {}", processed);
            log.info("TRANSACTIONS REJECTED: {}", rejected);
            log.info("END OF EXECUTION OF TRANSACTION POSTING JOB");

            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("postTransactionsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
