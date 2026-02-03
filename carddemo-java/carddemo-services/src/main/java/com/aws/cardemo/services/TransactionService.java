package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.Transaction;
import com.aws.cardemo.persistence.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    public Transaction createTransaction(Transaction transaction) {
        if (transaction.getTransactionTimestamp() == null) {
            transaction.setTransactionTimestamp(LocalDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(String transactionId) {
        transactionRepository.deleteById(transactionId);
    }

    public List<Transaction> getTransactionsByCardNumber(String cardNumber) {
        return transactionRepository.findByCardNumber(cardNumber);
    }

    public Page<Transaction> getTransactionsByCardNumber(String cardNumber, Pageable pageable) {
        return transactionRepository.findByCardNumber(cardNumber, pageable);
    }

    public List<Transaction> getTransactionsByTypeCode(String typeCode) {
        return transactionRepository.findByTransactionTypeCode(typeCode);
    }

    public List<Transaction> getTransactionsByCardNumberAndDateRange(
            String cardNumber, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByCardNumberAndDateRange(cardNumber, startDate, endDate);
    }

    public List<Transaction> getTransactionsByMerchantId(String merchantId) {
        return transactionRepository.findByMerchantId(merchantId);
    }

    public List<Transaction> getRecentTransactions(LocalDateTime since) {
        return transactionRepository.findRecentTransactions(since);
    }

    public List<Transaction> getTransactionsLast24Hours() {
        return transactionRepository.findRecentTransactions(LocalDateTime.now().minusHours(24));
    }
}
