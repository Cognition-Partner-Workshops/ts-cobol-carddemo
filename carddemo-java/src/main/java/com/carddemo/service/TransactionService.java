package com.carddemo.service;

import com.carddemo.dto.TransactionAddRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalanceId;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction service - migrated from:
 *   COTRN00C (CT00 - Transaction List)
 *   COTRN01C (CT01 - Transaction View)
 *   COTRN02C (CT02 - Transaction Add)
 *
 * COTRN00C logic: Browse TRANSACT by card number AIX, paginate forward/backward.
 * COTRN01C logic: Read single transaction by TRAN-ID.
 * COTRN02C logic: Validate input, generate TRAN-ID, WRITE to TRANSACT,
 *                 update account balance (current cycle debit/credit),
 *                 update transaction category balance.
 */
@Service
public class TransactionService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CardAccountXrefRepository xrefRepository;
    private final TransactionCategoryBalanceRepository catBalRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CardRepository cardRepository,
                              AccountRepository accountRepository,
                              CardAccountXrefRepository xrefRepository,
                              TransactionCategoryBalanceRepository catBalRepository) {
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.xrefRepository = xrefRepository;
        this.catBalRepository = catBalRepository;
    }

    /**
     * List transactions by card number - migrated from COTRN00C browse logic.
     */
    public Page<Transaction> listByCardNum(String cardNum, Pageable pageable) {
        return transactionRepository.findByCardNum(cardNum, pageable);
    }

    /**
     * View a single transaction - migrated from COTRN01C.
     */
    public Optional<Transaction> viewTransaction(String tranId) {
        return transactionRepository.findById(tranId);
    }

    /**
     * Add a new transaction - migrated from COTRN02C.
     * Generates a unique transaction ID, validates card exists,
     * updates account balance and category balance.
     */
    @Transactional
    public Transaction addTransaction(TransactionAddRequest request) {
        // Validate card exists (equivalent to COTRN02C card validation)
        Card card = cardRepository.findById(request.getCardNum())
                .orElseThrow(() -> new IllegalArgumentException("Card number not found"));

        if (!"Y".equalsIgnoreCase(card.getActiveStatus())) {
            throw new IllegalArgumentException("Card is not active");
        }

        // Find account via xref
        List<CardAccountXref> xrefs = xrefRepository.findByCardNum(request.getCardNum());
        if (xrefs.isEmpty()) {
            throw new IllegalArgumentException("Card not linked to any account");
        }
        Long acctId = xrefs.get(0).getAcctId();

        // Generate unique transaction ID (replaces COBOL EIBTASKN-based generation)
        String tranId = generateTransactionId();
        String now = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setTranId(tranId);
        transaction.setTranTypeCd(request.getTranTypeCd());
        transaction.setTranCatCd(request.getTranCatCd());
        transaction.setTranSource(request.getTranSource() != null ? request.getTranSource() : "ONLINE");
        transaction.setTranDesc(request.getTranDesc());
        transaction.setTranAmt(request.getTranAmt());
        transaction.setMerchantId(request.getMerchantId());
        transaction.setMerchantName(request.getMerchantName());
        transaction.setMerchantCity(request.getMerchantCity());
        transaction.setMerchantZip(request.getMerchantZip());
        transaction.setCardNum(request.getCardNum());
        transaction.setOrigTimestamp(now);
        transaction.setProcTimestamp(now);

        Transaction saved = transactionRepository.save(transaction);

        // Update account balance (migrated from COTRN02C balance update logic)
        updateAccountBalance(acctId, request.getTranAmt());

        // Update transaction category balance
        updateCategoryBalance(acctId, request.getTranTypeCd(), request.getTranCatCd(), request.getTranAmt());

        return saved;
    }

    /**
     * List all transactions with pagination.
     */
    public Page<Transaction> listAll(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    private String generateTransactionId() {
        // Generate a 16-character unique ID (replaces COBOL EIBTASKN + timestamp approach)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private void updateAccountBalance(Long acctId, BigDecimal amount) {
        accountRepository.findById(acctId).ifPresent(account -> {
            BigDecimal currentBalance = account.getCurrentBalance() != null
                    ? account.getCurrentBalance() : BigDecimal.ZERO;
            account.setCurrentBalance(currentBalance.add(amount));

            if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                BigDecimal cycleDebit = account.getCurrentCycleDebit() != null
                        ? account.getCurrentCycleDebit() : BigDecimal.ZERO;
                account.setCurrentCycleDebit(cycleDebit.add(amount));
            } else {
                BigDecimal cycleCredit = account.getCurrentCycleCredit() != null
                        ? account.getCurrentCycleCredit() : BigDecimal.ZERO;
                account.setCurrentCycleCredit(cycleCredit.add(amount.abs()));
            }

            accountRepository.save(account);
        });
    }

    private void updateCategoryBalance(Long acctId, String typeCd, Integer catCd, BigDecimal amount) {
        if (typeCd == null || catCd == null) {
            return;
        }
        TransactionCategoryBalanceId id = new TransactionCategoryBalanceId(acctId, typeCd, catCd);
        TransactionCategoryBalance catBal = catBalRepository.findById(id)
                .orElseGet(() -> {
                    TransactionCategoryBalance newBal = new TransactionCategoryBalance();
                    newBal.setAcctId(acctId);
                    newBal.setTypeCd(typeCd);
                    newBal.setCatCd(catCd);
                    newBal.setBalance(BigDecimal.ZERO);
                    return newBal;
                });
        catBal.setBalance(catBal.getBalance().add(amount));
        catBalRepository.save(catBal);
    }
}
