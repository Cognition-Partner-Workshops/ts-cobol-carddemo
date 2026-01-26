package com.aws.carddemo.batch;

import com.aws.carddemo.entity.*;
import com.aws.carddemo.exception.TransactionValidationException;
import com.aws.carddemo.repository.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

@Configuration
public class TransactionPostingBatchJob {

    private static final Logger log = LoggerFactory.getLogger(TransactionPostingBatchJob.class);

    private final TransactionRepository transactionRepository;
    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final RejectedTransactionRepository rejectedTransactionRepository;
    private final BatchJobLogRepository batchJobLogRepository;

    public TransactionPostingBatchJob(TransactionRepository transactionRepository,
                                       CardXrefRepository cardXrefRepository,
                                       AccountRepository accountRepository,
                                       RejectedTransactionRepository rejectedTransactionRepository,
                                       BatchJobLogRepository batchJobLogRepository) {
        this.transactionRepository = transactionRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.rejectedTransactionRepository = rejectedTransactionRepository;
        this.batchJobLogRepository = batchJobLogRepository;
    }

    @Bean
    public Job transactionPostingJob(JobRepository jobRepository, Step transactionPostingStep) {
        return new JobBuilder("transactionPostingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transactionPostingStep)
                .build();
    }

    @Bean
    public Step transactionPostingStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        ItemReader<Transaction> transactionReader,
                                        ItemProcessor<Transaction, Transaction> transactionProcessor,
                                        ItemWriter<Transaction> transactionWriter) {
        return new StepBuilder("transactionPostingStep", jobRepository)
                .<Transaction, Transaction>chunk(100, transactionManager)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .faultTolerant()
                .skip(TransactionValidationException.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    public RepositoryItemReader<Transaction> transactionReader() {
        return new RepositoryItemReaderBuilder<Transaction>()
                .name("transactionReader")
                .repository(transactionRepository)
                .methodName("findAll")
                .sorts(Collections.singletonMap("tranId", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        return transaction -> {
            try {
                validateTransaction(transaction);
                processTransaction(transaction);
                return transaction;
            } catch (TransactionValidationException e) {
                rejectTransaction(transaction, e.getErrorCode(), e.getMessage());
                throw e;
            }
        };
    }

    private void validateTransaction(Transaction transaction) {
        String cardNum = transaction.getCard().getCardNum();
        
        CardXref xref = cardXrefRepository.findByCardNumWithDetails(cardNum)
                .orElseThrow(TransactionValidationException::invalidCardNumber);
        
        Account account = xref.getAccount();
        if (account == null) {
            throw TransactionValidationException.accountNotFound();
        }
        
        BigDecimal projectedBalance = account.getProjectedBalance(transaction.getTranAmt());
        if (account.getAcctCreditLimit().compareTo(projectedBalance) < 0) {
            throw TransactionValidationException.overlimitTransaction();
        }
        
        LocalDate tranDate = transaction.getTranOrigTs().toLocalDate();
        if (account.getAcctExpirationDate().isBefore(tranDate)) {
            throw TransactionValidationException.accountExpired();
        }
    }

    private void processTransaction(Transaction transaction) {
        String cardNum = transaction.getCard().getCardNum();
        CardXref xref = cardXrefRepository.findByCardNumWithDetails(cardNum).orElseThrow();
        Account account = xref.getAccount();
        
        account.setAcctCurrBal(account.getAcctCurrBal().add(transaction.getTranAmt()));
        
        if (transaction.isCredit()) {
            account.setAcctCurrCycCredit(account.getAcctCurrCycCredit().add(transaction.getTranAmt()));
        } else {
            account.setAcctCurrCycDebit(account.getAcctCurrCycDebit().add(transaction.getTranAmt()));
        }
        
        accountRepository.save(account);
        
        transaction.setTranProcTs(LocalDateTime.now());
    }

    private void rejectTransaction(Transaction transaction, int errorCode, String errorMessage) {
        RejectedTransaction rejected = RejectedTransaction.builder()
                .tranId(transaction.getTranId())
                .cardNum(transaction.getCard().getCardNum())
                .tranAmt(transaction.getTranAmt())
                .tranTypeCode(transaction.getTranTypeCd())
                .tranOrigTs(transaction.getTranOrigTs())
                .rejectionCode(errorCode)
                .rejectionReason(errorMessage)
                .rejectedAt(LocalDateTime.now())
                .build();
        
        rejectedTransactionRepository.save(rejected);
        log.warn("Transaction {} rejected with code {}: {}", transaction.getTranId(), errorCode, errorMessage);
    }

    @Bean
    public ItemWriter<Transaction> transactionWriter() {
        return transactions -> {
            for (Transaction transaction : transactions) {
                transactionRepository.save(transaction);
            }
        };
    }
}
