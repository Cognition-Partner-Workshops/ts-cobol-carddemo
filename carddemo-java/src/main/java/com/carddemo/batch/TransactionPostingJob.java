package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalanceId;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class TransactionPostingJob {

    private static final Logger log = LoggerFactory.getLogger(TransactionPostingJob.class);

    private final TransactionRepository transactionRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository tranCatBalRepository;

    public TransactionPostingJob(TransactionRepository transactionRepository,
                                 CardCrossReferenceRepository cardCrossReferenceRepository,
                                 AccountRepository accountRepository,
                                 TransactionCategoryBalanceRepository tranCatBalRepository) {
        this.transactionRepository = transactionRepository;
        this.cardCrossReferenceRepository = cardCrossReferenceRepository;
        this.accountRepository = accountRepository;
        this.tranCatBalRepository = tranCatBalRepository;
    }

    @Transactional
    public BatchResult execute(List<Transaction> dailyTransactions) {
        log.info("START OF EXECUTION OF TRANSACTION POSTING JOB");

        int processedCount = 0;
        int rejectedCount = 0;

        for (Transaction tran : dailyTransactions) {
            try {
                boolean valid = validateTransaction(tran);
                if (valid) {
                    postTransaction(tran);
                    processedCount++;
                } else {
                    rejectedCount++;
                    log.warn("Transaction rejected: {}", tran.getTranId());
                }
            } catch (Exception e) {
                rejectedCount++;
                log.error("Error processing transaction {}: {}", tran.getTranId(), e.getMessage());
            }
        }

        log.info("TRANSACTIONS PROCESSED: {}", processedCount);
        log.info("TRANSACTIONS REJECTED: {}", rejectedCount);
        log.info("END OF EXECUTION OF TRANSACTION POSTING JOB");

        return new BatchResult(processedCount, rejectedCount);
    }

    private boolean validateTransaction(Transaction tran) {
        if (tran.getCardNum() == null || tran.getCardNum().isBlank()) {
            return false;
        }

        Optional<CardCrossReference> xref = cardCrossReferenceRepository.findById(tran.getCardNum());
        if (xref.isEmpty()) {
            return false;
        }

        Optional<Account> account = accountRepository.findById(xref.get().getAcctId());
        return account.isPresent();
    }

    private void postTransaction(Transaction tran) {
        tran.setProcTs(LocalDateTime.now());
        transactionRepository.save(tran);

        CardCrossReference xref = cardCrossReferenceRepository.findById(tran.getCardNum()).orElseThrow();
        Account account = accountRepository.findById(xref.getAcctId()).orElseThrow();

        account.setCurrBal(account.getCurrBal().add(tran.getAmount()));

        if (tran.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debit = account.getCurrCycDebit() != null ? account.getCurrCycDebit() : BigDecimal.ZERO;
            account.setCurrCycDebit(debit.add(tran.getAmount()));
        } else {
            BigDecimal credit = account.getCurrCycCredit() != null ? account.getCurrCycCredit() : BigDecimal.ZERO;
            account.setCurrCycCredit(credit.add(tran.getAmount().abs()));
        }

        accountRepository.save(account);

        TransactionCategoryBalanceId catBalId = new TransactionCategoryBalanceId(
                xref.getAcctId(), tran.getTypeCd(), tran.getCatCd());
        Optional<TransactionCategoryBalance> existingBal = tranCatBalRepository.findById(catBalId);

        if (existingBal.isPresent()) {
            TransactionCategoryBalance bal = existingBal.get();
            bal.setBalance(bal.getBalance().add(tran.getAmount()));
            tranCatBalRepository.save(bal);
        } else {
            TransactionCategoryBalance newBal = new TransactionCategoryBalance();
            newBal.setAcctId(xref.getAcctId());
            newBal.setTypeCd(tran.getTypeCd());
            newBal.setCatCd(tran.getCatCd());
            newBal.setBalance(tran.getAmount());
            tranCatBalRepository.save(newBal);
        }
    }

    public record BatchResult(int processedCount, int rejectedCount) {
    }
}
