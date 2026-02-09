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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    public Job statementGenerationBatchJob(JobRepository jobRepository,
                                           Step generateStatementsStep) {
        return new JobBuilder("StatementGenerationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(generateStatementsStep)
                .build();
    }

    @Bean
    public Step generateStatementsStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("START OF EXECUTION OF STATEMENT GENERATION JOB");

            List<Account> accounts = accountRepository.findByActiveStatus("Y");
            int statementsGenerated = 0;

            for (Account account : accounts) {
                List<CardAccountXref> xrefs = xrefRepository.findByAcctId(account.getAcctId());
                if (xrefs.isEmpty()) {
                    continue;
                }

                CardAccountXref xref = xrefs.get(0);
                Optional<Customer> custOpt = customerRepository.findById(xref.getCustId());
                if (custOpt.isEmpty()) {
                    continue;
                }

                Customer customer = custOpt.get();

                log.info("=== STATEMENT FOR ACCOUNT: {} ===", account.getAcctId());
                log.info("Customer: {} {}", customer.getFirstName(), customer.getLastName());
                log.info("Address: {}", customer.getAddrLine1());
                log.info("Current Balance: {}", account.getCurrBal());
                log.info("Credit Limit: {}", account.getCreditLimit());
                log.info("Available Credit: {}",
                        account.getCreditLimit().subtract(account.getCurrBal()));

                List<Transaction> transactions = transactionRepository
                        .findByAccountId(account.getAcctId(), PageRequest.of(0, 100))
                        .getContent();

                BigDecimal totalCharges = BigDecimal.ZERO;
                BigDecimal totalPayments = BigDecimal.ZERO;

                for (Transaction tran : transactions) {
                    log.info("  {} | {} | {} | {}",
                            tran.getOrigTs(), tran.getDescription(),
                            tran.getMerchantName(), tran.getAmount());
                    if (tran.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        totalCharges = totalCharges.add(tran.getAmount());
                    } else {
                        totalPayments = totalPayments.add(tran.getAmount().abs());
                    }
                }

                log.info("Total Charges: {}", totalCharges);
                log.info("Total Payments: {}", totalPayments);
                log.info("=== END STATEMENT ===");

                statementsGenerated++;
            }

            log.info("STATEMENTS GENERATED: {}", statementsGenerated);
            log.info("END OF EXECUTION OF STATEMENT GENERATION JOB");

            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("generateStatementsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
