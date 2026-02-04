package com.aws.carddemo.service.account;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Transaction;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.CardRepository;
import com.aws.carddemo.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bill Payment Service - migrated from COBIL00C.cbl
 * Handles bill payment operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillPaymentService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Process bill payment - migrated from COBIL00C.cbl
     */
    @Transactional
    public BillPaymentResult processPayment(BillPaymentRequest request) {
        log.info("Processing bill payment for account: {}", request.getAccountId());

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountId()));

        if (!"Y".equalsIgnoreCase(account.getActiveStatus())) {
            throw new AccountNotActiveException("Account is not active: " + request.getAccountId());
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentAmountException("Payment amount must be positive");
        }

        String cardNumber = cardRepository.findByAccountAccountId(request.getAccountId())
                .stream()
                .filter(card -> "Y".equalsIgnoreCase(card.getActiveStatus()))
                .findFirst()
                .map(card -> card.getCardNumber())
                .orElseThrow(() -> new NoActiveCardException("No active card found for account: " + request.getAccountId()));

        BigDecimal newBalance = account.getCurrentBalance().subtract(request.getAmount());
        account.setCurrentBalance(newBalance);
        account.setCurrentCycleCredit(account.getCurrentCycleCredit().add(request.getAmount()));
        accountRepository.save(account);

        String transactionId = generateTransactionId();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionTypeCode("PM")
                .transactionCategoryCode(9999)
                .transactionSource("BILLPAY")
                .description("Bill Payment - " + (request.getDescription() != null ? request.getDescription() : "Payment"))
                .amount(request.getAmount())
                .cardNumber(cardNumber)
                .originTimestamp(LocalDateTime.now())
                .processTimestamp(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        log.info("Bill payment processed successfully. Transaction ID: {}", transactionId);

        return BillPaymentResult.builder()
                .transactionId(transactionId)
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .newBalance(newBalance)
                .processedAt(LocalDateTime.now())
                .status("SUCCESS")
                .build();
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class BillPaymentRequest {
        private Long accountId;
        private BigDecimal amount;
        private String description;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class BillPaymentResult {
        private String transactionId;
        private Long accountId;
        private BigDecimal amount;
        private BigDecimal newBalance;
        private LocalDateTime processedAt;
        private String status;
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String message) {
            super(message);
        }
    }

    public static class AccountNotActiveException extends RuntimeException {
        public AccountNotActiveException(String message) {
            super(message);
        }
    }

    public static class InvalidPaymentAmountException extends RuntimeException {
        public InvalidPaymentAmountException(String message) {
            super(message);
        }
    }

    public static class NoActiveCardException extends RuntimeException {
        public NoActiveCardException(String message) {
            super(message);
        }
    }
}
