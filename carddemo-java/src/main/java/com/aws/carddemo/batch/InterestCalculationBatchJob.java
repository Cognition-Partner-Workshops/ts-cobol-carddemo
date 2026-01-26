package com.aws.carddemo.batch;

import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.InterestCalculation;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.InterestCalculationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;

@Configuration
public class InterestCalculationBatchJob {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationBatchJob.class);
    
    private static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.1999");
    private static final BigDecimal MONTHLY_INTEREST_RATE = ANNUAL_INTEREST_RATE.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);

    private final AccountRepository accountRepository;
    private final InterestCalculationRepository interestCalculationRepository;

    public InterestCalculationBatchJob(AccountRepository accountRepository,
                                        InterestCalculationRepository interestCalculationRepository) {
        this.accountRepository = accountRepository;
        this.interestCalculationRepository = interestCalculationRepository;
    }

    @Bean
    public Job interestCalculationJob(JobRepository jobRepository, Step interestCalculationStep) {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(interestCalculationStep)
                .build();
    }

    @Bean
    public Step interestCalculationStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         ItemReader<Account> accountReaderForInterest,
                                         ItemProcessor<Account, InterestCalculation> interestProcessor,
                                         ItemWriter<InterestCalculation> interestWriter) {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .<Account, InterestCalculation>chunk(100, transactionManager)
                .reader(accountReaderForInterest)
                .processor(interestProcessor)
                .writer(interestWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> accountReaderForInterest() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("accountReaderForInterest")
                .repository(accountRepository)
                .methodName("findAll")
                .sorts(Collections.singletonMap("acctId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Account, InterestCalculation> interestProcessor() {
        return account -> {
            if (!account.isActive()) {
                log.debug("Skipping inactive account: {}", account.getAcctId());
                return null;
            }
            
            BigDecimal balance = account.getAcctCurrBal();
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Skipping account with zero or negative balance: {}", account.getAcctId());
                return null;
            }
            
            BigDecimal interestAmount = balance.multiply(MONTHLY_INTEREST_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            
            account.setAcctCurrBal(balance.add(interestAmount));
            accountRepository.save(account);
            
            InterestCalculation calculation = InterestCalculation.builder()
                    .acctId(account.getAcctId())
                    .calcDate(LocalDate.now())
                    .principalBalance(balance)
                    .interestRate(ANNUAL_INTEREST_RATE)
                    .interestAmount(interestAmount)
                    .newBalance(account.getAcctCurrBal())
                    .build();
            
            log.info("Calculated interest for account {}: {} at rate {}", 
                    account.getAcctId(), interestAmount, ANNUAL_INTEREST_RATE);
            
            return calculation;
        };
    }

    @Bean
    public ItemWriter<InterestCalculation> interestWriter() {
        return calculations -> {
            for (InterestCalculation calculation : calculations) {
                interestCalculationRepository.save(calculation);
            }
        };
    }
}
