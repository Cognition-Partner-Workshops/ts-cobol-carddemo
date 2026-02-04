package com.aws.carddemo.batch.job;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Customer;
import com.aws.carddemo.domain.entity.Transaction;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.CustomerRepository;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Statement Generation Job - migrated from CBSTM03A/B (CREASTMT)
 * Part of the monthly batch cycle: INTCALC → COMBTRAN → CREASTMT
 * 
 * This job generates monthly statements for all active accounts.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class StatementGenerationJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    @Bean
    public Job generateStatementsJob(Step generateStatementsStep) {
        return new JobBuilder("generateStatementsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(generateStatementsStep)
                .build();
    }

    @Bean
    public Step generateStatementsStep(ItemReader<Account> statementAccountReader,
                                        ItemProcessor<Account, StatementData> statementProcessor,
                                        ItemWriter<StatementData> statementWriter) {
        return new StepBuilder("generateStatementsStep", jobRepository)
                .<Account, StatementData>chunk(50, transactionManager)
                .reader(statementAccountReader)
                .processor(statementProcessor)
                .writer(statementWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> statementAccountReader() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("statementAccountReader")
                .repository(accountRepository)
                .methodName("findByActiveStatus")
                .arguments("Y")
                .sorts(Collections.singletonMap("accountId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Account, StatementData> statementProcessor() {
        return account -> {
            log.debug("Generating statement for account: {}", account.getAccountId());

            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
                    .atTime(23, 59, 59);

            Customer customer = customerRepository.findByAccountId(account.getAccountId())
                    .stream()
                    .findFirst()
                    .orElse(null);

            List<Transaction> transactions = transactionRepository.findByCardNumberAndDateRange(
                    account.getCards().isEmpty() ? null : account.getCards().get(0).getCardNumber(),
                    startOfMonth,
                    endOfMonth
            );

            BigDecimal totalCharges = transactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(Transaction::getAmount)
                    .map(BigDecimal::abs)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPayments = transactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal minimumPayment = calculateMinimumPayment(account.getCurrentBalance());

            return StatementData.builder()
                    .accountId(account.getAccountId())
                    .customerName(customer != null ? customer.getFullName() : "Unknown")
                    .statementDate(LocalDate.now())
                    .statementPeriodStart(startOfMonth.toLocalDate())
                    .statementPeriodEnd(endOfMonth.toLocalDate())
                    .previousBalance(account.getCurrentBalance().subtract(account.getCurrentCycleDebit()).add(account.getCurrentCycleCredit()))
                    .totalCharges(totalCharges)
                    .totalPayments(totalPayments)
                    .currentBalance(account.getCurrentBalance())
                    .creditLimit(account.getCreditLimit())
                    .availableCredit(account.getAvailableCredit())
                    .minimumPayment(minimumPayment)
                    .dueDate(LocalDate.now().plusDays(25))
                    .transactionCount(transactions.size())
                    .build();
        };
    }

    @Bean
    public ItemWriter<StatementData> statementWriter() {
        return statements -> {
            for (StatementData statement : statements) {
                log.info("Generated statement for account {}: Balance={}, MinPayment={}, DueDate={}",
                        statement.getAccountId(),
                        statement.getCurrentBalance(),
                        statement.getMinimumPayment(),
                        statement.getDueDate());
            }
            log.info("Generated {} statements", statements.size());
        };
    }

    private BigDecimal calculateMinimumPayment(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal percentagePayment = balance.multiply(new BigDecimal("0.02"));
        BigDecimal minimumFloor = new BigDecimal("25.00");
        
        if (balance.compareTo(minimumFloor) < 0) {
            return balance;
        }
        
        return percentagePayment.max(minimumFloor);
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class StatementData {
        private Long accountId;
        private String customerName;
        private LocalDate statementDate;
        private LocalDate statementPeriodStart;
        private LocalDate statementPeriodEnd;
        private BigDecimal previousBalance;
        private BigDecimal totalCharges;
        private BigDecimal totalPayments;
        private BigDecimal currentBalance;
        private BigDecimal creditLimit;
        private BigDecimal availableCredit;
        private BigDecimal minimumPayment;
        private LocalDate dueDate;
        private int transactionCount;
    }
}
