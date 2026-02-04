package com.aws.carddemo.service.transaction;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Card;
import com.aws.carddemo.domain.entity.Transaction;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.CardRepository;
import com.aws.carddemo.domain.repository.TransactionRepository;
import com.aws.carddemo.service.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction Service - migrated from COTRN00C.cbl, COTRN01C.cbl, COTRN02C.cbl
 * Handles transaction list, view, and add operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    /**
     * List transactions with pagination - migrated from COTRN00C.cbl
     */
    @Transactional(readOnly = true)
    public Page<TransactionDTO> listTransactions(Pageable pageable) {
        log.info("Listing all transactions");
        return transactionRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    /**
     * List transactions by card number - migrated from COTRN00C.cbl
     */
    @Transactional(readOnly = true)
    public Page<TransactionDTO> listTransactionsByCard(String cardNumber, Pageable pageable) {
        log.info("Listing transactions for card: {}", maskCardNumber(cardNumber));
        return transactionRepository.findByCardNumber(cardNumber, pageable)
                .map(this::mapToDTO);
    }

    /**
     * List transactions by date range
     */
    @Transactional(readOnly = true)
    public Page<TransactionDTO> listTransactionsByDateRange(LocalDateTime startDate, 
                                                             LocalDateTime endDate, 
                                                             Pageable pageable) {
        log.info("Listing transactions from {} to {}", startDate, endDate);
        return transactionRepository.findByDateRange(startDate, endDate, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get transaction details - migrated from COTRN01C.cbl
     */
    @Transactional(readOnly = true)
    public Optional<TransactionDTO> getTransaction(String transactionId) {
        log.info("Fetching transaction: {}", transactionId);
        return transactionRepository.findById(transactionId)
                .map(this::mapToDTO);
    }

    /**
     * Add new transaction - migrated from COTRN02C.cbl
     */
    @Transactional
    public TransactionDTO addTransaction(TransactionCreateRequest request) {
        log.info("Adding new transaction for card: {}", maskCardNumber(request.getCardNumber()));

        Card card = cardRepository.findById(request.getCardNumber())
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + maskCardNumber(request.getCardNumber())));

        if (!card.isActive()) {
            throw new CardNotActiveException("Card is not active: " + maskCardNumber(request.getCardNumber()));
        }

        if (card.isExpired()) {
            throw new CardExpiredException("Card is expired: " + maskCardNumber(request.getCardNumber()));
        }

        Account account = card.getAccount();
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal newBalance = account.getCurrentBalance().add(request.getAmount().abs());
            if (newBalance.compareTo(account.getCreditLimit()) > 0) {
                throw new CreditLimitExceededException("Transaction would exceed credit limit");
            }
        }

        String transactionId = generateTransactionId();
        
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionTypeCode(request.getTransactionTypeCode())
                .transactionCategoryCode(request.getTransactionCategoryCode())
                .transactionSource(request.getTransactionSource())
                .description(request.getDescription())
                .amount(request.getAmount())
                .merchantId(request.getMerchantId())
                .merchantName(request.getMerchantName())
                .merchantCity(request.getMerchantCity())
                .merchantZip(request.getMerchantZip())
                .cardNumber(request.getCardNumber())
                .originTimestamp(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            account.setCurrentCycleCredit(account.getCurrentCycleCredit().add(request.getAmount()));
        } else {
            account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(request.getAmount().abs()));
        }
        account.setCurrentBalance(account.getCurrentBalance().add(request.getAmount()));
        accountRepository.save(account);

        log.info("Transaction added successfully: {}", transactionId);
        return mapToDTO(savedTransaction);
    }

    /**
     * Get transaction summary by type
     */
    @Transactional(readOnly = true)
    public List<TransactionSummary> getTransactionSummaryByType(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = transactionRepository.getTransactionSummaryByType(startDate, endDate);
        return results.stream()
                .map(row -> TransactionSummary.builder()
                        .transactionTypeCode((String) row[0])
                        .count((Long) row[1])
                        .totalAmount((BigDecimal) row[2])
                        .build())
                .toList();
    }

    /**
     * Find large transactions for fraud detection
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> findLargeTransactions(BigDecimal threshold) {
        return transactionRepository.findLargeTransactions(threshold).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Find unprocessed transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> findUnprocessedTransactions() {
        return transactionRepository.findUnprocessedTransactions().stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Mark transaction as processed
     */
    @Transactional
    public TransactionDTO markAsProcessed(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));
        
        transaction.setProcessTimestamp(LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        return mapToDTO(savedTransaction);
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .transactionTypeCode(transaction.getTransactionTypeCode())
                .transactionCategoryCode(transaction.getTransactionCategoryCode())
                .transactionSource(transaction.getTransactionSource())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .merchantId(transaction.getMerchantId())
                .merchantName(transaction.getMerchantName())
                .merchantCity(transaction.getMerchantCity())
                .merchantZip(transaction.getMerchantZip())
                .cardNumber(transaction.getCardNumber())
                .maskedCardNumber(maskCardNumber(transaction.getCardNumber()))
                .originTimestamp(transaction.getOriginTimestamp())
                .processTimestamp(transaction.getProcessTimestamp())
                .build();
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) {
            return "****";
        }
        return "****" + cardNumber.substring(12);
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransactionCreateRequest {
        private String transactionTypeCode;
        private Integer transactionCategoryCode;
        private String transactionSource;
        private String description;
        private BigDecimal amount;
        private Long merchantId;
        private String merchantName;
        private String merchantCity;
        private String merchantZip;
        private String cardNumber;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransactionSummary {
        private String transactionTypeCode;
        private Long count;
        private BigDecimal totalAmount;
    }

    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String message) {
            super(message);
        }
    }

    public static class CardNotFoundException extends RuntimeException {
        public CardNotFoundException(String message) {
            super(message);
        }
    }

    public static class CardNotActiveException extends RuntimeException {
        public CardNotActiveException(String message) {
            super(message);
        }
    }

    public static class CardExpiredException extends RuntimeException {
        public CardExpiredException(String message) {
            super(message);
        }
    }

    public static class CreditLimitExceededException extends RuntimeException {
        public CreditLimitExceededException(String message) {
            super(message);
        }
    }
}
