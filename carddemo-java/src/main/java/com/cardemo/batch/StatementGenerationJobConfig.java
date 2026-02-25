package com.cardemo.batch;

import com.cardemo.entity.Account;
import com.cardemo.entity.Transaction;
import com.cardemo.repository.AccountRepository;
import com.cardemo.repository.CardAccountXrefRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statement Generation batch job configuration.
 * Migrated from JCL job CREASTMT / COBOL programs CBSTM03A and CBSTM03B.
 *
 * COBOL flow (CBSTM03A - Statement Header):
 * 1. Open ACCTFILE, CUSTFILE, XREFFILE
 * 2. For each account:
 *    a. Read customer info via XREFFILE
 *    b. Write statement header (account number, name, address, balances)
 *
 * COBOL flow (CBSTM03B - Statement Detail):
 * 1. Open TRANSACT file
 * 2. For each account's transactions:
 *    a. Write transaction detail lines
 *    b. Calculate subtotals by transaction type
 *    c. Write statement footer with totals
 *
 * In the Java migration, statements are generated as structured data
 * that can be rendered as PDF, HTML, or JSON.
 */
@Configuration
public class StatementGenerationJobConfig {

    private static final Logger log = LoggerFactory.getLogger(StatementGenerationJobConfig.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CardAccountXrefRepository xrefRepository;

    public StatementGenerationJobConfig(AccountRepository accountRepository,
                                        TransactionRepository transactionRepository,
                                        CardAccountXrefRepository xrefRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.xrefRepository = xrefRepository;
    }

    @Bean
    public Job statementGenerationJob(JobRepository jobRepository, Step statementGenerationStep) {
        return new JobBuilder("statementGenerationJob", jobRepository)
                .start(statementGenerationStep)
                .build();
    }

    @Bean
    public Step statementGenerationStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("statementGenerationStep", jobRepository)
                .<Account, Map<String, Object>>chunk(10, transactionManager)
                .reader(statementAccountReader())
                .processor(statementProcessor())
                .writer(statementWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Account> statementAccountReader() {
        return new RepositoryItemReaderBuilder<Account>()
                .name("statementAccountReader")
                .repository(accountRepository)
                .methodName("findAll")
                .sorts(Map.of("acctId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    /**
     * Process each account to generate statement data.
     * Migrated from CBSTM03A (header) and CBSTM03B (detail).
     */
    @Bean
    public ItemProcessor<Account, Map<String, Object>> statementProcessor() {
        return account -> {
            // Skip inactive accounts
            if (!"Y".equalsIgnoreCase(account.getAcctActiveStatus())) {
                return null;
            }

            Map<String, Object> statement = new LinkedHashMap<>();

            // Statement header - CBSTM03A
            statement.put("accountId", account.getAcctId());
            statement.put("currentBalance", account.getAcctCurrBal());
            statement.put("creditLimit", account.getAcctCreditLimit());
            statement.put("availableCredit",
                    account.getAcctCreditLimit().subtract(account.getAcctCurrBal()));
            statement.put("cycleCredits", account.getAcctCurrCycCredit());
            statement.put("cycleDebits", account.getAcctCurrCycDebit());

            // Get transactions for this account via xref
            // CBSTM03B: Read all transactions for cards linked to this account
            List<String> cardNums = xrefRepository.findByXrefAcctId(account.getAcctId())
                    .stream()
                    .map(xref -> xref.getXrefCardNum())
                    .toList();

            BigDecimal totalDebits = BigDecimal.ZERO;
            BigDecimal totalCredits = BigDecimal.ZERO;
            int transactionCount = 0;

            for (String cardNum : cardNums) {
                List<Transaction> transactions = transactionRepository.findByTranCardNum(cardNum);
                for (Transaction txn : transactions) {
                    transactionCount++;
                    BigDecimal amt = txn.getTranAmt() != null ? txn.getTranAmt() : BigDecimal.ZERO;
                    if (amt.compareTo(BigDecimal.ZERO) > 0) {
                        totalDebits = totalDebits.add(amt);
                    } else {
                        totalCredits = totalCredits.add(amt.abs());
                    }
                }
            }

            // Statement footer totals - CBSTM03B
            statement.put("transactionCount", transactionCount);
            statement.put("totalDebits", totalDebits);
            statement.put("totalCredits", totalCredits);
            statement.put("netActivity", totalDebits.subtract(totalCredits));

            return statement;
        };
    }

    @Bean
    public ItemWriter<Map<String, Object>> statementWriter() {
        return statements -> {
            for (Map<String, Object> statement : statements) {
                // In production, this would write to a file, send email, or store in DB
                log.info("CREASTMT: Generated statement for account {} - Balance: {}, Transactions: {}",
                        statement.get("accountId"),
                        statement.get("currentBalance"),
                        statement.get("transactionCount"));
            }
        };
    }
}
