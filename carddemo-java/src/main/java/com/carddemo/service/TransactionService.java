package com.carddemo.service;

import com.carddemo.dto.PaymentRequest;
import com.carddemo.dto.TransactionDto;
import com.carddemo.entity.Account;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalanceId;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
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
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final TransactionRepository transactionRepository;
    private final CardAccountXrefRepository xrefRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository tcatBalRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CardAccountXrefRepository xrefRepository,
                              AccountRepository accountRepository,
                              TransactionCategoryBalanceRepository tcatBalRepository) {
        this.transactionRepository = transactionRepository;
        this.xrefRepository = xrefRepository;
        this.accountRepository = accountRepository;
        this.tcatBalRepository = tcatBalRepository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listTransactionsByCard(String cardNum, Pageable pageable) {
        return transactionRepository.findByCardNum(cardNum, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listTransactionsByAccount(Long acctId, Pageable pageable) {
        return transactionRepository.findByAccountId(acctId, pageable);
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(String tranId) {
        return transactionRepository.findById(tranId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + tranId));
    }

    public Transaction addTransaction(TransactionDto dto) {
        CardAccountXref xref = xrefRepository.findById(dto.getCardNum())
                .orElseThrow(() -> new ValidationException(
                        "Card not found in cross reference file: " + dto.getCardNum()));

        Account account = accountRepository.findById(xref.getAcctId())
                .orElseThrow(() -> new ValidationException(
                        "Account not found for card: " + dto.getCardNum()));

        if (!"Y".equals(account.getActiveStatus())) {
            throw new ValidationException("Account is not active");
        }

        BigDecimal newBal = account.getCurrBal().add(dto.getAmount());
        if (newBal.compareTo(account.getCreditLimit()) > 0) {
            throw new ValidationException("Transaction would exceed credit limit");
        }

        String tranId = generateTransactionId();
        String now = LocalDateTime.now().format(TS_FORMAT);

        Transaction transaction = new Transaction();
        transaction.setTranId(tranId);
        transaction.setTypeCd(dto.getTypeCd());
        transaction.setCatCd(dto.getCatCd());
        transaction.setSource(dto.getSource() != null ? dto.getSource() : "ONLINE");
        transaction.setDescription(dto.getDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setMerchantId(dto.getMerchantId());
        transaction.setMerchantName(dto.getMerchantName());
        transaction.setMerchantCity(dto.getMerchantCity());
        transaction.setMerchantZip(dto.getMerchantZip());
        transaction.setCardNum(dto.getCardNum());
        transaction.setOrigTs(now);
        transaction.setProcTs(now);

        Transaction saved = transactionRepository.save(transaction);

        account.setCurrBal(newBal);
        if (dto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cycDebit = account.getCurrCycDebit() != null
                    ? account.getCurrCycDebit() : BigDecimal.ZERO;
            account.setCurrCycDebit(cycDebit.add(dto.getAmount()));
        } else {
            BigDecimal cycCredit = account.getCurrCycCredit() != null
                    ? account.getCurrCycCredit() : BigDecimal.ZERO;
            account.setCurrCycCredit(cycCredit.add(dto.getAmount().abs()));
        }
        accountRepository.save(account);

        updateCategoryBalance(xref.getAcctId(), dto.getTypeCd(), dto.getCatCd(), dto.getAmount());

        return saved;
    }

    public Transaction processPayment(PaymentRequest request) {
        TransactionDto dto = new TransactionDto();
        dto.setTypeCd("05");
        dto.setCatCd(5020);
        dto.setSource("PAYMENT");
        dto.setDescription("Bill Payment");
        dto.setAmount(request.getAmount().negate());
        dto.setCardNum(request.getCardNum());
        return addTransaction(dto);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionReport(String cardNum, String startDate, String endDate) {
        return transactionRepository.findByCardNumAndDateRange(cardNum, startDate, endDate);
    }

    private void updateCategoryBalance(Long acctId, String typeCd, Integer catCd, BigDecimal amount) {
        TransactionCategoryBalanceId id = new TransactionCategoryBalanceId(acctId, typeCd, catCd);
        TransactionCategoryBalance balance = tcatBalRepository.findById(id).orElse(null);

        if (balance == null) {
            balance = new TransactionCategoryBalance();
            balance.setAcctId(acctId);
            balance.setTypeCd(typeCd);
            balance.setCatCd(catCd);
            balance.setBalance(amount);
        } else {
            balance.setBalance(balance.getBalance().add(amount));
        }

        tcatBalRepository.save(balance);
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
