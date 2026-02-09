package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.DisclosureGroup;
import com.carddemo.entity.DisclosureGroupId;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.DisclosureGroupRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Configuration
public class InterestCalculationJob {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationJob.class);
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final TransactionCategoryBalanceRepository tcatBalRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public InterestCalculationJob(TransactionCategoryBalanceRepository tcatBalRepository,
                                  DisclosureGroupRepository disclosureGroupRepository,
                                  AccountRepository accountRepository,
                                  TransactionRepository transactionRepository) {
        this.tcatBalRepository = tcatBalRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Bean
    public Job interestCalculationBatchJob(JobRepository jobRepository,
                                           Step calculateInterestStep) {
        return new JobBuilder("InterestCalculationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(calculateInterestStep)
                .build();
    }

    @Bean
    public Step calculateInterestStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("START OF EXECUTION OF INTEREST CALCULATION JOB");

            List<TransactionCategoryBalance> balances = tcatBalRepository.findAll();
            Map<Long, BigDecimal> accountInterestTotals = new HashMap<>();
            int recordCount = 0;

            Long lastAcctId = null;

            for (TransactionCategoryBalance catBal : balances) {
                recordCount++;

                if (lastAcctId != null && !lastAcctId.equals(catBal.getAcctId())) {
                    updateAccountWithInterest(lastAcctId, accountInterestTotals.getOrDefault(
                            lastAcctId, BigDecimal.ZERO));
                }
                lastAcctId = catBal.getAcctId();

                Optional<Account> acctOpt = accountRepository.findById(catBal.getAcctId());
                if (acctOpt.isEmpty()) {
                    continue;
                }
                Account account = acctOpt.get();

                DisclosureGroupId dgId = new DisclosureGroupId(
                        account.getGroupId(), catBal.getTypeCd(), catBal.getCatCd());
                Optional<DisclosureGroup> dgOpt = disclosureGroupRepository.findById(dgId);

                if (dgOpt.isPresent() && dgOpt.get().getIntRate().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal annualRate = dgOpt.get().getIntRate();
                    BigDecimal monthlyRate = annualRate.divide(MONTHS_PER_YEAR, 10, RoundingMode.HALF_UP)
                            .divide(HUNDRED, 10, RoundingMode.HALF_UP);
                    BigDecimal interest = catBal.getBalance().multiply(monthlyRate)
                            .setScale(2, RoundingMode.HALF_UP);

                    accountInterestTotals.merge(catBal.getAcctId(), interest, BigDecimal::add);

                    if (interest.compareTo(BigDecimal.ZERO) != 0) {
                        createInterestTransaction(catBal, interest, account);
                    }
                }
            }

            if (lastAcctId != null) {
                updateAccountWithInterest(lastAcctId, accountInterestTotals.getOrDefault(
                        lastAcctId, BigDecimal.ZERO));
            }

            log.info("RECORDS PROCESSED: {}", recordCount);
            log.info("END OF EXECUTION OF INTEREST CALCULATION JOB");

            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("calculateInterestStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    private void updateAccountWithInterest(Long acctId, BigDecimal totalInterest) {
        accountRepository.findById(acctId).ifPresent(account -> {
            account.setCurrBal(account.getCurrBal().add(totalInterest));
            account.setCurrCycCredit(BigDecimal.ZERO);
            account.setCurrCycDebit(BigDecimal.ZERO);
            accountRepository.save(account);
        });
    }

    private void createInterestTransaction(TransactionCategoryBalance catBal,
                                           BigDecimal interest, Account account) {
        String tranId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String now = LocalDateTime.now().format(TS_FORMAT);

        Transaction tran = new Transaction();
        tran.setTranId(tranId);
        tran.setTypeCd(catBal.getTypeCd());
        tran.setCatCd(catBal.getCatCd());
        tran.setSource("INTEREST");
        tran.setDescription("Monthly Interest Charge");
        tran.setAmount(interest);
        tran.setCardNum("");
        tran.setOrigTs(now);
        tran.setProcTs(now);
        transactionRepository.save(tran);
    }
}
