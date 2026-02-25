package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.DisclosureGroup;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.DisclosureGroupRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
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
import java.util.List;

/**
 * Spring Batch job for interest calculation - migrated from JCL INTCALC / CBACT04C.
 * Original COBOL CBACT04C:
 * 1. Read account master file
 * 2. For each account, look up disclosure group by account group ID
 * 3. For each transaction category balance, calculate interest = balance * rate / 12
 * 4. Add interest to account balance
 */
@Configuration
public class InterestCalculationJob {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationJob.class);

    private final AccountRepository accountRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final TransactionCategoryBalanceRepository catBalRepository;

    public InterestCalculationJob(AccountRepository accountRepository,
                                  DisclosureGroupRepository disclosureGroupRepository,
                                  TransactionCategoryBalanceRepository catBalRepository) {
        this.accountRepository = accountRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.catBalRepository = catBalRepository;
    }

    @Bean
    public Job interestCalcBatchJob(JobRepository jobRepository, Step interestCalcStep) {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(interestCalcStep)
                .build();
    }

    @Bean
    public Step interestCalcStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("interestCalcStep", jobRepository)
                .tasklet(interestCalcTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet interestCalcTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting interest calculation...");

            List<Account> accounts = accountRepository.findAll();
            int processed = 0;

            for (Account account : accounts) {
                if (!"Y".equalsIgnoreCase(account.getActiveStatus())) {
                    continue;
                }

                String groupId = account.getGroupId();
                if (groupId == null) {
                    continue;
                }

                // Get category balances for this account
                List<TransactionCategoryBalance> catBalances =
                        catBalRepository.findByAcctId(account.getAcctId());

                BigDecimal totalInterest = BigDecimal.ZERO;

                for (TransactionCategoryBalance catBal : catBalances) {
                    // Look up interest rate from disclosure group
                    List<DisclosureGroup> groups = disclosureGroupRepository
                            .findByAcctGroupIdAndTranTypeCd(groupId, catBal.getTypeCd());

                    if (!groups.isEmpty()) {
                        BigDecimal rate = groups.get(0).getInterestRate();
                        if (rate != null && catBal.getBalance() != null
                                && catBal.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                            // Monthly interest = balance * (annual rate / 100) / 12
                            BigDecimal monthlyInterest = catBal.getBalance()
                                    .multiply(rate)
                                    .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
                            totalInterest = totalInterest.add(monthlyInterest);
                        }
                    }
                }

                if (totalInterest.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentBalance = account.getCurrentBalance() != null
                            ? account.getCurrentBalance() : BigDecimal.ZERO;
                    account.setCurrentBalance(currentBalance.add(totalInterest));
                    accountRepository.save(account);
                    log.debug("Account {}: interest charged = {}", account.getAcctId(), totalInterest);
                }

                processed++;
            }

            log.info("Interest calculation complete. Processed {} accounts", processed);
            return RepeatStatus.FINISHED;
        };
    }
}
