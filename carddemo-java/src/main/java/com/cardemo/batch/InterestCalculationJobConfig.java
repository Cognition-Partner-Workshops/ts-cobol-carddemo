package com.cardemo.batch;

import com.cardemo.entity.Account;
import com.cardemo.entity.DisclosureGroup;
import com.cardemo.entity.Transaction;
import com.cardemo.entity.TransactionCategoryBalance;
import com.cardemo.repository.AccountRepository;
import com.cardemo.repository.DisclosureGroupRepository;
import com.cardemo.repository.TransactionCategoryBalanceRepository;
import com.cardemo.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interest Calculation batch job configuration.
 * Migrated from JCL job INTCALC / COBOL program CBACT04C.
 *
 * COBOL flow:
 * 1. Open ACCTFILE (accounts), TCATBALF (category balances), DISCGRP (disclosure/interest rates)
 * 2. For each account:
 *    a. Read category balances for the account
 *    b. For each category balance:
 *       - Look up interest rate from DISCGRP using account group + tran type + tran category
 *       - COMPUTE interest = balance * rate / 12 / 100 (monthly interest)
 *       - Create interest transaction
 *       - Update account balance
 * 3. Close files
 *
 * Key COBOL computations:
 * COMPUTE WS-INTEREST-AMT = (TRAN-CAT-BAL * DIS-INT-RATE) / 12 / 100
 */
@Configuration
public class InterestCalculationJobConfig {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationJobConfig.class);
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");
    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository categoryBalanceRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final TransactionRepository transactionRepository;

    public InterestCalculationJobConfig(AccountRepository accountRepository,
                                        TransactionCategoryBalanceRepository categoryBalanceRepository,
                                        DisclosureGroupRepository disclosureGroupRepository,
                                        TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.categoryBalanceRepository = categoryBalanceRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.transactionRepository = transactionRepository;
    }

    @Bean
    public Job interestCalculationJob(JobRepository jobRepository, Step interestCalculationStep) {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .start(interestCalculationStep)
                .build();
    }

    @Bean
    public Step interestCalculationStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .<Account, Account>chunk(10, transactionManager)
                .reader(interestCalcAccountReader())
                .processor(interestCalcProcessor())
                .writer(interestCalcWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> interestCalcAccountReader() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("interestCalcAccountReader")
                .repository(accountRepository)
                .methodName("findAll")
                .sorts(Map.of("acctId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Account, Account> interestCalcProcessor() {
        return account -> {
            // Skip inactive accounts - COBOL: IF ACCT-ACTIVE-STATUS NOT = 'Y'
            if (!"Y".equalsIgnoreCase(account.getAcctActiveStatus())) {
                return null; // skip
            }

            // Get category balances for this account
            List<TransactionCategoryBalance> balances =
                    categoryBalanceRepository.findByTrancatAcctId(account.getAcctId());

            BigDecimal totalInterest = BigDecimal.ZERO;

            for (TransactionCategoryBalance catBal : balances) {
                BigDecimal balance = catBal.getTranCatBal();
                if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Look up interest rate from disclosure group
                String groupId = account.getAcctGroupId() != null ? account.getAcctGroupId() : "DEFAULT";
                Optional<DisclosureGroup> discOpt = disclosureGroupRepository
                        .findByDisAcctGroupIdAndDisTranTypeCdAndDisTranCatCd(
                                groupId, catBal.getTrancatTypeCd(), catBal.getTrancatCd());

                if (discOpt.isEmpty()) {
                    continue;
                }

                BigDecimal rate = discOpt.get().getDisIntRate();
                if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                // COBOL: COMPUTE WS-INTEREST-AMT = (TRAN-CAT-BAL * DIS-INT-RATE) / 12 / 100
                BigDecimal interest = balance.multiply(rate)
                        .divide(MONTHS_IN_YEAR, 10, RoundingMode.HALF_UP)
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP);

                totalInterest = totalInterest.add(interest);

                log.info("Interest calc: acct={} type={} cat={} bal={} rate={} interest={}",
                        account.getAcctId(), catBal.getTrancatTypeCd(), catBal.getTrancatCd(),
                        balance, rate, interest);
            }

            // Update account balance with interest
            if (totalInterest.compareTo(BigDecimal.ZERO) > 0) {
                account.setAcctCurrBal(account.getAcctCurrBal().add(totalInterest));
            }

            return account;
        };
    }

    @Bean
    public ItemWriter<Account> interestCalcWriter() {
        return accounts -> {
            for (Account account : accounts) {
                accountRepository.save(account);

                // Create interest transaction record
                BigDecimal interestAmt = account.getAcctCurrBal(); // simplified
                String tranId = String.format("INT%013d", System.currentTimeMillis());
                String now = LocalDateTime.now().format(TS_FORMAT);

                Transaction interestTxn = new Transaction();
                interestTxn.setTranId(tranId);
                interestTxn.setTranTypeCd("01");
                interestTxn.setTranCatCd(5); // Interest charge category
                interestTxn.setTranSource("BATCH");
                interestTxn.setTranDesc("Monthly Interest Charge");
                interestTxn.setTranAmt(BigDecimal.ZERO); // Interest amount handled in processor
                interestTxn.setTranOrigTs(now);
                interestTxn.setTranProcTs(now);

                transactionRepository.save(interestTxn);
            }
        };
    }
}
