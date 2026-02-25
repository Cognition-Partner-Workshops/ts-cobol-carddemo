package com.cardemo.service;

import com.cardemo.dto.TransactionRequest;
import com.cardemo.entity.Account;
import com.cardemo.entity.Card;
import com.cardemo.entity.CardAccountXref;
import com.cardemo.entity.Transaction;
import com.cardemo.exception.CardDemoException;
import com.cardemo.repository.AccountRepository;
import com.cardemo.repository.CardAccountXrefRepository;
import com.cardemo.repository.CardRepository;
import com.cardemo.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Transaction management service.
 * Migrated from COTRN00C (CT00 - list), COTRN01C (CT01 - detail), COTRN02C (CT02 - add).
 * Also covers CORPT00C (CR00 - report) for transaction reporting.
 */
@Service
public class TransactionService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CardAccountXrefRepository xrefRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CardRepository cardRepository,
                              AccountRepository accountRepository,
                              CardAccountXrefRepository xrefRepository) {
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.xrefRepository = xrefRepository;
    }

    /**
     * List transactions by card number - migrated from COTRN00C (CT00 transaction).
     * COBOL: EXEC CICS STARTBR DATASET(WS-TRANSACT-FILE) RIDFLD(WS-TRAN-ID)
     * Uses alternate index (AIX) on card number for lookup.
     */
    public List<Transaction> getTransactionsByCardNum(String cardNum) {
        return transactionRepository.findByTranCardNumOrderByTranOrigTsDesc(cardNum);
    }

    /**
     * List transactions with pagination - for REST API cursor-based paging.
     */
    public Page<Transaction> getTransactionsByCardNum(String cardNum, Pageable pageable) {
        return transactionRepository.findByTranCardNum(cardNum, pageable);
    }

    /**
     * Get transaction detail - migrated from COTRN01C (CT01 transaction).
     * COBOL: EXEC CICS READ DATASET(WS-TRANSACT-FILE) INTO(TRAN-RECORD)
     */
    public Transaction getTransaction(String tranId) {
        return transactionRepository.findById(tranId)
                .orElseThrow(() -> CardDemoException.notFound("Transaction not found: " + tranId));
    }

    /**
     * Create transaction - migrated from COTRN02C (CT02 transaction).
     * COBOL: PROCESS-ENTER-KEY -> WRITE-TRANSACTION-TO-FILE
     * Validates: card exists and is active, account exists and has sufficient credit.
     * Updates account balance after transaction.
     */
    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        // Validate card exists and is active
        Card card = cardRepository.findById(request.getCardNum())
                .orElseThrow(() -> CardDemoException.notFound("Card not found: " + request.getCardNum()));

        if (!"Y".equalsIgnoreCase(card.getCardActiveStatus())) {
            throw CardDemoException.badRequest("Card is not active: " + request.getCardNum());
        }

        // Look up account via cross-reference - COBOL: READ XREFFILE
        CardAccountXref xref = xrefRepository.findById(request.getCardNum())
                .orElseThrow(() -> CardDemoException.notFound("Card cross-reference not found"));

        Account account = accountRepository.findById(xref.getXrefAcctId())
                .orElseThrow(() -> CardDemoException.notFound("Account not found for card"));

        // Validate account is active
        if (!"Y".equalsIgnoreCase(account.getAcctActiveStatus())) {
            throw CardDemoException.badRequest("Account is not active");
        }

        // Validate credit limit - COBOL: IF WS-TRAN-AMT > ACCT-CREDIT-LIMIT - ACCT-CURR-BAL
        BigDecimal availableCredit = account.getAcctCreditLimit().subtract(account.getAcctCurrBal());
        if (request.getTranAmt().compareTo(availableCredit) > 0) {
            throw CardDemoException.badRequest("Transaction exceeds available credit limit");
        }

        // Generate transaction ID - COBOL: uses timestamp-based ID
        String tranId = generateTransactionId();
        String now = LocalDateTime.now().format(TS_FORMAT);

        Transaction transaction = new Transaction();
        transaction.setTranId(tranId);
        transaction.setTranTypeCd(request.getTranTypeCd());
        transaction.setTranCatCd(request.getTranCatCd());
        transaction.setTranSource(request.getTranSource() != null ? request.getTranSource() : "ONLINE");
        transaction.setTranDesc(request.getTranDesc());
        transaction.setTranAmt(request.getTranAmt());
        transaction.setTranMerchantId(request.getTranMerchantId());
        transaction.setTranMerchantName(request.getTranMerchantName());
        transaction.setTranMerchantCity(request.getTranMerchantCity());
        transaction.setTranMerchantZip(request.getTranMerchantZip());
        transaction.setTranCardNum(request.getCardNum());
        transaction.setTranOrigTs(now);
        transaction.setTranProcTs(now);

        Transaction saved = transactionRepository.save(transaction);

        // Update account balance - COBOL: COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL + WS-TRAN-AMT
        account.setAcctCurrBal(account.getAcctCurrBal().add(request.getTranAmt()));
        account.setAcctCurrCycDebit(account.getAcctCurrCycDebit().add(request.getTranAmt()));
        accountRepository.save(account);

        return saved;
    }

    /**
     * Get all transactions for reporting - migrated from CORPT00C (CR00 transaction).
     * COBOL: STARTBR/READNEXT loop over TRANSACT file.
     */
    public Page<Transaction> getTransactionReport(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    /**
     * Generate a unique transaction ID using timestamp.
     * COBOL used EIBTASKN + EIBTIME + EIBDATE for unique IDs.
     */
    private String generateTransactionId() {
        long ts = System.currentTimeMillis();
        return String.format("%016d", ts);
    }
}
