package com.aws.carddemo.batch.job;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Transaction;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

/**
 * Interest Calculation Job - migrated from CBACT04C (INTCALC)
 * Part of the monthly batch cycle: INTCALC → COMBTRAN → CREASTMT
 * 
 * This job calculates monthly interest on account balances.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class InterestCalculationJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Value("${carddemo.batch.interest.annual-rate:0.1999}")
    private BigDecimal annualInterestRate;

    @Bean
    public Job calculateInterestJob(Step calculateInterestStep) {
        return new JobBuilder("calculateInterestJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(calculateInterestStep)
                .build();
    }

    @Bean
    public Step calculateInterestStep(ItemReader<Account> activeAccountReader,
                                       ItemProcessor<Account, InterestCalculationResult> interestProcessor,
                                       ItemWriter<InterestCalculationResult> interestWriter) {
        return new StepBuilder("calculateInterestStep", jobRepository)
                .<Account, InterestCalculationResult>chunk(100, transactionManager)
                .reader(activeAccountReader)
                .processor(interestProcessor)
                .writer(interestWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> activeAccountReader() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("activeAccountReader")
                .repository(accountRepository)
                .methodName("findByActiveStatus")
                .arguments("Y")
                .sorts(Collections.singletonMap("accountId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Account, InterestCalculationResult> interestProcessor() {
        return account -> {
            BigDecimal balance = account.getCurrentBalance();
            
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Skipping account {} - no balance to charge interest", account.getAccountId());
                return null;
            }

            BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

            log.debug("Calculated interest for account {}: {} (balance: {}, rate: {})", 
                    account.getAccountId(), interest, balance, monthlyRate);

            return InterestCalculationResult.builder()
                    .account(account)
                    .interestAmount(interest)
                    .calculatedAt(LocalDateTime.now())
                    .build();
        };
    }

    @Bean
    public ItemWriter<InterestCalculationResult> interestWriter() {
        return results -> {
            for (InterestCalculationResult result : results) {
                Account account = result.getAccount();
                BigDecimal newBalance = account.getCurrentBalance().add(result.getInterestAmount());
                account.setCurrentBalance(newBalance);
                account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(result.getInterestAmount()));
                accountRepository.save(account);

                String transactionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
                Transaction interestTransaction = Transaction.builder()
                        .transactionId(transactionId)
                        .transactionTypeCode("IN")
                        .transactionCategoryCode(9998)
                        .transactionSource("INTCALC")
                        .description("Monthly Interest Charge")
                        .amount(result.getInterestAmount().negate())
                        .originTimestamp(result.getCalculatedAt())
                        .processTimestamp(LocalDateTime.now())
                        .build();
                transactionRepository.save(interestTransaction);

                log.debug("Applied interest {} to account {}, new balance: {}", 
                        result.getInterestAmount(), account.getAccountId(), newBalance);
            }
            log.info("Processed interest for {} accounts", results.size());
        };
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class InterestCalculationResult {
        private Account account;
        private BigDecimal interestAmount;
        private LocalDateTime calculatedAt;
    }
}
