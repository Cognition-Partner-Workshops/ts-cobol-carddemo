package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.CustomerRepository;
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
import java.util.List;

/**
 * Spring Batch job for statement generation - migrated from JCL CREASTMT / CBSTM03A.
 * Original COBOL CBSTM03A:
 * 1. Read account master sequentially
 * 2. For each account, read customer data
 * 3. Read all transactions for account's cards
 * 4. Generate statement report (print file)
 * Java equivalent: Generate statement data and log it (could be extended to PDF/email).
 */
@Configuration
public class StatementGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(StatementGenerationJob.class);

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardAccountXrefRepository xrefRepository;
    private final TransactionRepository transactionRepository;

    public StatementGenerationJob(AccountRepository accountRepository,
                                  CustomerRepository customerRepository,
                                  CardAccountXrefRepository xrefRepository,
                                  TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.xrefRepository = xrefRepository;
        this.transactionRepository = transactionRepository;
    }

    @Bean
    public Job statementGenBatchJob(JobRepository jobRepository, Step statementGenStep) {
        return new JobBuilder("statementGenerationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(statementGenStep)
                .build();
    }

    @Bean
    public Step statementGenStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("statementGenStep", jobRepository)
                .tasklet(statementGenTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet statementGenTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting statement generation...");
            String stmtDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            List<Account> accounts = accountRepository.findAll();
            int statementsGenerated = 0;

            for (Account account : accounts) {
                if (!"Y".equalsIgnoreCase(account.getActiveStatus())) {
                    continue;
                }

                // Find customer via xref
                List<CardAccountXref> xrefs = xrefRepository.findByAcctId(account.getAcctId());
                if (xrefs.isEmpty()) {
                    continue;
                }

                Customer customer = customerRepository.findById(xrefs.get(0).getCustId()).orElse(null);

                // Get all transactions for cards linked to this account
                BigDecimal totalCharges = BigDecimal.ZERO;
                BigDecimal totalCredits = BigDecimal.ZERO;
                int transactionCount = 0;

                for (CardAccountXref xref : xrefs) {
                    List<Transaction> transactions = transactionRepository
                            .findByCardNumOrderByOrigTimestampDesc(xref.getCardNum());
                    for (Transaction t : transactions) {
                        if (t.getTranAmt() != null) {
                            if (t.getTranAmt().compareTo(BigDecimal.ZERO) >= 0) {
                                totalCharges = totalCharges.add(t.getTranAmt());
                            } else {
                                totalCredits = totalCredits.add(t.getTranAmt().abs());
                            }
                            transactionCount++;
                        }
                    }
                }

                // Log statement summary (in production, this would generate a PDF or email)
                log.info("Statement for Account {}: Customer={} {}, " +
                                "Balance={}, Charges={}, Credits={}, Transactions={}",
                        account.getAcctId(),
                        customer != null ? customer.getFirstName() : "N/A",
                        customer != null ? customer.getLastName() : "N/A",
                        account.getCurrentBalance(),
                        totalCharges, totalCredits, transactionCount);

                statementsGenerated++;
            }

            log.info("Statement generation complete. Generated {} statements for date {}",
                    statementsGenerated, stmtDate);
            return RepeatStatus.FINISHED;
        };
    }
}
