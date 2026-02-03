package com.carddemo.service;

import com.carddemo.dto.BillPaymentRequest;
import com.carddemo.dto.TransactionRequest;
import com.carddemo.exception.BadRequestException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.model.Account;
import com.carddemo.model.CardXref;
import com.carddemo.model.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllByOrderByTransactionIdDesc(pageable);
    }
    
    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "transactionId", transactionId));
    }
    
    public List<Transaction> getTransactionsByCardNumber(String cardNumber) {
        return transactionRepository.findByCardNumber(cardNumber);
    }
    
    public Page<Transaction> getTransactionsByCardNumber(String cardNumber, Pageable pageable) {
        return transactionRepository.findByCardNumber(cardNumber, pageable);
    }
    
    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        String cardNumber = request.getCardNumber();
        
        if (!cardXrefRepository.existsByCardNumber(cardNumber)) {
            throw new ResourceNotFoundException("Card", "cardNumber", cardNumber);
        }
        
        String transactionId = generateTransactionId();
        
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .typeCode(request.getTypeCode())
                .categoryCode(request.getCategoryCode())
                .source(request.getSource())
                .description(request.getDescription())
                .amount(request.getAmount())
                .merchantId(request.getMerchantId())
                .merchantName(request.getMerchantName())
                .merchantCity(request.getMerchantCity())
                .merchantZip(request.getMerchantZip())
                .cardNumber(cardNumber)
                .originTimestamp(LocalDateTime.now())
                .processTimestamp(LocalDateTime.now())
                .build();
        
        return transactionRepository.save(transaction);
    }
    
    @Transactional
    public Transaction processBillPayment(BillPaymentRequest request) {
        String accountId = request.getAccountId();
        
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));
        
        if (account.getCurrentBalance() == null || account.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("You have nothing to pay");
        }
        
        CardXref cardXref = cardXrefRepository.findByAccountId(accountId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CardXref", "accountId", accountId));
        
        BigDecimal paymentAmount = account.getCurrentBalance();
        String transactionId = generateTransactionId();
        
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .typeCode("02")
                .categoryCode(2)
                .source("POS TERM")
                .description("BILL PAYMENT - ONLINE")
                .amount(paymentAmount)
                .merchantId("999999999")
                .merchantName("BILL PAYMENT")
                .merchantCity("N/A")
                .merchantZip("N/A")
                .cardNumber(cardXref.getCardNumber())
                .originTimestamp(LocalDateTime.now())
                .processTimestamp(LocalDateTime.now())
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        account.setCurrentBalance(BigDecimal.ZERO);
        accountRepository.save(account);
        
        return savedTransaction;
    }
    
    private String generateTransactionId() {
        return transactionRepository.findTopByOrderByTransactionIdDesc()
                .map(t -> {
                    try {
                        long lastId = Long.parseLong(t.getTransactionId());
                        return String.format("%016d", lastId + 1);
                    } catch (NumberFormatException e) {
                        return String.format("%016d", System.currentTimeMillis());
                    }
                })
                .orElse(String.format("%016d", 1L));
    }
}
