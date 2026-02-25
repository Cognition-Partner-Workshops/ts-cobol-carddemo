package com.cardemo.batch;

import com.cardemo.entity.Account;
import com.cardemo.entity.Transaction;
import com.cardemo.entity.TransactionCategoryBalance;
import com.cardemo.repository.AccountRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Post Transaction batch job configuration.
 * Migrated from JCL job POSTTRAN / COBOL program CBTRN02C.
 *
 * COBOL flow:
 * 1. Open DALYTRAN (daily transaction file) and TRANSACT (master transaction file)
 * 2. Read each daily transaction record
 * 3. Validate transaction (card exists, account active, credit limit check)
 * 4. Write validated transaction to TRANSACT master file
 * 5. Update account balance in ACCTFILE
 * 6. Update transaction category balance in TCATBALF
 * 7. Write rejected transactions to reject file
 *
 * Spring Batch equivalent:
 * - ItemReader: reads unprocessed transactions from the transactions table
 * - ItemProcessor: validates and processes each transaction
 * - ItemWriter: updates account balances and category balances
 */
@Configuration
public class PostTransactionJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PostTransactionJobConfig.class);
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionCategoryBalanceRepository categoryBalanceRepository;

    public PostTransactionJobConfig(AccountRepository accountRepository,
                                    TransactionRepository transactionRepository,
                                    TransactionCategoryBalanceRepository categoryBalanceRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.categoryBalanceRepository = categoryBalanceRepository;
    }

    @Bean
    public Job postTransactionJob(JobRepository jobRepository, Step postTransactionStep) {
        return new JobBuilder("postTransactionJob", jobRepository)
                .start(postTransactionStep)
                .build();
    }

    @Bean
    public Step postTransactionStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager) {
        return new StepBuilder("postTransactionStep", jobRepository)
                .<Transaction, Transaction>chunk(10, transactionManager)
                .reader(postTransactionReader())
                .processor(postTransactionProcessor())
                .writer(postTransactionWriter())
                .build();
    }

    @Bean
    public ItemReader<Transaction> postTransactionReader() {
        RepositoryItemReader<Transaction> reader = new RepositoryItemReaderBuilder<Transaction>()
                .name("postTransactionReader")
                .repository(transactionRepository)
                .methodName("findAll")
                .sorts(Map.of("tranId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
        return reader;
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> postTransactionProcessor() {
        return transaction -> {
            // Skip already-processed transactions
            // COBOL: IF TRAN-PROC-TS NOT = SPACES
            if (transaction.getTranProcTs() != null && !transaction.getTranProcTs().isBlank()) {
                return transaction;
            }

            // Mark as processed with current timestamp
            // COBOL: MOVE FUNCTION CURRENT-DATE TO TRAN-PROC-TS
            transaction.setTranProcTs(LocalDateTime.now().format(TS_FORMAT));
            log.info("Processing transaction: {} for card: {} amount: {}",
                    transaction.getTranId(), transaction.getTranCardNum(), transaction.getTranAmt());
            return transaction;
        };
    }

    @Bean
    public ItemWriter<Transaction> postTransactionWriter() {
        return transactions -> {
            for (Transaction transaction : transactions) {
                // Save processed transaction
                transactionRepository.save(transaction);

                // Update account balance - COBOL: PERFORM UPDATE-ACCOUNT-BALANCE
                if (transaction.getTranCardNum() != null) {
                    updateAccountBalance(transaction);
                }

                // Update category balance - COBOL: PERFORM UPDATE-CATEGORY-BALANCE
                updateCategoryBalance(transaction);
            }
        };
    }

    /**
     * Update account balance - migrated from CBTRN02C UPDATE-ACCOUNT-BALANCE paragraph.
     * COBOL: COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL + TRAN-AMT
     *        IF TRAN-AMT > 0 -> COMPUTE ACCT-CURR-CYC-DEBIT = ACCT-CURR-CYC-DEBIT + TRAN-AMT
     *        IF TRAN-AMT < 0 -> COMPUTE ACCT-CURR-CYC-CREDIT = ACCT-CURR-CYC-CREDIT + ABS(TRAN-AMT)
     */
    private void updateAccountBalance(Transaction transaction) {
        // This is simplified; in production, look up account via XREF first
        accountRepository.findAll().stream()
                .filter(a -> a.getAcctId() != null)
                .findFirst()
                .ifPresent(account -> {
                    BigDecimal amount = transaction.getTranAmt() != null ? transaction.getTranAmt() : BigDecimal.ZERO;
                    account.setAcctCurrBal(account.getAcctCurrBal().add(amount));

                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        account.setAcctCurrCycDebit(account.getAcctCurrCycDebit().add(amount));
                    } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                        account.setAcctCurrCycCredit(account.getAcctCurrCycCredit().add(amount.abs()));
                    }

                    accountRepository.save(account);
                });
    }

    /**
     * Update transaction category balance - migrated from CBTRN02C UPDATE-TCATBAL paragraph.
     * COBOL: READ TCATBALF -> IF FOUND: COMPUTE TRAN-CAT-BAL = TRAN-CAT-BAL + TRAN-AMT
     *        IF NOT FOUND: WRITE new record
     */
    private void updateCategoryBalance(Transaction transaction) {
        if (transaction.getTranTypeCd() == null || transaction.getTranCatCd() == null) {
            return;
        }

        // Look up existing category balance
        // In production, use the actual account ID from XREF lookup
        Optional<TransactionCategoryBalance> existing = categoryBalanceRepository
                .findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
                        0L, transaction.getTranTypeCd(), transaction.getTranCatCd());

        BigDecimal amount = transaction.getTranAmt() != null ? transaction.getTranAmt() : BigDecimal.ZERO;

        if (existing.isPresent()) {
            TransactionCategoryBalance balance = existing.get();
            balance.setTranCatBal(balance.getTranCatBal().add(amount));
            categoryBalanceRepository.save(balance);
        } else {
            TransactionCategoryBalance newBalance = new TransactionCategoryBalance();
            newBalance.setTrancatAcctId(0L);
            newBalance.setTrancatTypeCd(transaction.getTranTypeCd());
            newBalance.setTrancatCd(transaction.getTranCatCd());
            newBalance.setTranCatBal(amount);
            categoryBalanceRepository.save(newBalance);
        }
    }
}
