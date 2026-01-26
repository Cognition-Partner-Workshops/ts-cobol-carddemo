package com.aws.carddemo.batch;

import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.BatchJobLog;
import com.aws.carddemo.entity.Transaction;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.BatchJobLogRepository;
import com.aws.carddemo.repository.TransactionRepository;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class StatementGenerationBatchJob {

    private static final Logger log = LoggerFactory.getLogger(StatementGenerationBatchJob.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BatchJobLogRepository batchJobLogRepository;

    public StatementGenerationBatchJob(AccountRepository accountRepository,
                                        TransactionRepository transactionRepository,
                                        BatchJobLogRepository batchJobLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.batchJobLogRepository = batchJobLogRepository;
    }

    @Bean
    public Job statementGenerationJob(JobRepository jobRepository, Step statementGenerationStep) {
        return new JobBuilder("statementGenerationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(statementGenerationStep)
                .build();
    }

    @Bean
    public Step statementGenerationStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         ItemReader<Account> statementAccountReader,
                                         ItemProcessor<Account, Map<String, Object>> statementProcessor,
                                         ItemWriter<Map<String, Object>> statementWriter) {
        return new StepBuilder("statementGenerationStep", jobRepository)
                .<Account, Map<String, Object>>chunk(100, transactionManager)
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
                .methodName("findAll")
                .sorts(Collections.singletonMap("acctId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Account, Map<String, Object>> statementProcessor() {
        return account -> {
            if (!account.isActive()) {
                return null;
            }
            
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1);
            
            BigDecimal totalTransactions = transactionRepository.sumTransactionsByAccount(account.getAcctId());
            
            Map<String, Object> statement = new HashMap<>();
            statement.put("acctId", account.getAcctId());
            statement.put("statementDate", LocalDateTime.now());
            statement.put("openingBalance", account.getAcctCurrBal().subtract(
                    totalTransactions != null ? totalTransactions : BigDecimal.ZERO));
            statement.put("closingBalance", account.getAcctCurrBal());
            statement.put("creditLimit", account.getAcctCreditLimit());
            statement.put("availableCredit", account.getAvailableCredit());
            statement.put("totalCredits", account.getAcctCurrCycCredit());
            statement.put("totalDebits", account.getAcctCurrCycDebit());
            statement.put("minimumPayment", calculateMinimumPayment(account));
            statement.put("dueDate", LocalDateTime.now().plusDays(25));
            
            log.info("Generated statement for account: {}", account.getAcctId());
            
            return statement;
        };
    }

    private BigDecimal calculateMinimumPayment(Account account) {
        BigDecimal balance = account.getAcctCurrBal();
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal percentagePayment = balance.multiply(new BigDecimal("0.02"));
        BigDecimal minimumFixed = new BigDecimal("25.00");
        
        return percentagePayment.max(minimumFixed).min(balance);
    }

    @Bean
    public ItemWriter<Map<String, Object>> statementWriter() {
        return statements -> {
            long count = 0;
            for (Map<String, Object> statement : statements) {
                log.debug("Statement generated: {}", statement);
                count++;
            }
            
            BatchJobLog jobLog = BatchJobLog.builder()
                    .jobName("STMTGEN")
                    .stepName("GENERATE_STATEMENTS")
                    .status("COMPLETED")
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .recordsProcessed(count)
                    .recordsRejected(0L)
                    .build();
            batchJobLogRepository.save(jobLog);
        };
    }
}
