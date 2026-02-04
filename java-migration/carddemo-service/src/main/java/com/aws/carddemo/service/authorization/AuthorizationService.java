package com.aws.carddemo.service.authorization;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Card;
import com.aws.carddemo.domain.entity.Transaction;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.CardRepository;
import com.aws.carddemo.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authorization Service - migrated from COPAUA0C (CP00)
 * Handles real-time credit card authorization with fraud detection
 * 
 * Original implementation used:
 * - IMS DB for hierarchical data storage
 * - DB2 for relational data
 * - MQ for request/response patterns
 * - Two-phase commits across IMS DB and DB2
 * 
 * Migrated to:
 * - PostgreSQL with JSONB for hierarchical data
 * - Spring @Transactional for atomic operations
 * - REST APIs or Kafka for messaging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private static final BigDecimal FRAUD_THRESHOLD_SINGLE = new BigDecimal("5000.00");
    private static final BigDecimal FRAUD_THRESHOLD_DAILY = new BigDecimal("10000.00");
    private static final int MAX_DAILY_TRANSACTIONS = 50;

    @Transactional
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        log.info("Processing authorization request for card: {}", maskCardNumber(request.getCardNumber()));

        Card card = cardRepository.findById(request.getCardNumber())
                .orElse(null);

        if (card == null) {
            return buildDeclineResponse(request, "CARD_NOT_FOUND", "Card not found");
        }

        if (!"Y".equalsIgnoreCase(card.getActiveStatus())) {
            return buildDeclineResponse(request, "CARD_INACTIVE", "Card is not active");
        }

        if (card.isExpired()) {
            return buildDeclineResponse(request, "CARD_EXPIRED", "Card has expired");
        }

        if (!validateCvv(card, request.getCvv())) {
            return buildDeclineResponse(request, "CVV_MISMATCH", "CVV validation failed");
        }

        Account account = card.getAccount();
        if (account == null || !"Y".equalsIgnoreCase(account.getActiveStatus())) {
            return buildDeclineResponse(request, "ACCOUNT_INACTIVE", "Account is not active");
        }

        FraudCheckResult fraudCheck = performFraudCheck(card, request);
        if (!fraudCheck.isPassed()) {
            log.warn("Fraud check failed for card {}: {}", maskCardNumber(request.getCardNumber()), fraudCheck.getReason());
            return buildDeclineResponse(request, "FRAUD_SUSPECTED", fraudCheck.getReason());
        }

        BigDecimal availableCredit = account.getAvailableCredit();
        if (request.getAmount().compareTo(availableCredit) > 0) {
            return buildDeclineResponse(request, "INSUFFICIENT_CREDIT", "Insufficient credit available");
        }

        String authCode = generateAuthorizationCode();
        
        BigDecimal newBalance = account.getCurrentBalance().add(request.getAmount());
        account.setCurrentBalance(newBalance);
        account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(request.getAmount()));
        accountRepository.save(account);

        String transactionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionTypeCode(request.getTransactionType())
                .transactionCategoryCode(request.getCategoryCode())
                .transactionSource("AUTH")
                .description(request.getDescription())
                .amount(request.getAmount().negate())
                .merchantId(request.getMerchantId())
                .merchantName(request.getMerchantName())
                .merchantCity(request.getMerchantCity())
                .merchantZip(request.getMerchantZip())
                .cardNumber(request.getCardNumber())
                .originTimestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        log.info("Authorization approved for card {}: authCode={}, amount={}", 
                maskCardNumber(request.getCardNumber()), authCode, request.getAmount());

        return AuthorizationResponse.builder()
                .authorizationCode(authCode)
                .transactionId(transactionId)
                .status("APPROVED")
                .responseCode("00")
                .responseMessage("Approved")
                .availableCredit(account.getAvailableCredit())
                .processedAt(LocalDateTime.now())
                .build();
    }

    private FraudCheckResult performFraudCheck(Card card, AuthorizationRequest request) {
        if (request.getAmount().compareTo(FRAUD_THRESHOLD_SINGLE) > 0) {
            return FraudCheckResult.builder()
                    .passed(false)
                    .reason("Transaction amount exceeds single transaction limit")
                    .build();
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        
        BigDecimal dailyTotal = transactionRepository.sumAmountByCardNumberAndDateRange(
                card.getCardNumber(), startOfDay, endOfDay);
        
        if (dailyTotal != null) {
            BigDecimal projectedTotal = dailyTotal.abs().add(request.getAmount());
            if (projectedTotal.compareTo(FRAUD_THRESHOLD_DAILY) > 0) {
                return FraudCheckResult.builder()
                        .passed(false)
                        .reason("Daily transaction limit exceeded")
                        .build();
            }
        }

        long dailyCount = transactionRepository.countByCardNumber(card.getCardNumber());
        if (dailyCount >= MAX_DAILY_TRANSACTIONS) {
            return FraudCheckResult.builder()
                    .passed(false)
                    .reason("Maximum daily transaction count exceeded")
                    .build();
        }

        return FraudCheckResult.builder()
                .passed(true)
                .reason("All fraud checks passed")
                .build();
    }

    private boolean validateCvv(Card card, String cvv) {
        if (cvv == null || cvv.isEmpty()) {
            return true;
        }
        return card.getCvvCode() != null && card.getCvvCode().equals(cvv);
    }

    private String generateAuthorizationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    private AuthorizationResponse buildDeclineResponse(AuthorizationRequest request, String code, String message) {
        return AuthorizationResponse.builder()
                .authorizationCode(null)
                .transactionId(null)
                .status("DECLINED")
                .responseCode(code)
                .responseMessage(message)
                .availableCredit(null)
                .processedAt(LocalDateTime.now())
                .build();
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AuthorizationRequest {
        private String cardNumber;
        private String cvv;
        private BigDecimal amount;
        private String transactionType;
        private Integer categoryCode;
        private String description;
        private Long merchantId;
        private String merchantName;
        private String merchantCity;
        private String merchantZip;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AuthorizationResponse {
        private String authorizationCode;
        private String transactionId;
        private String status;
        private String responseCode;
        private String responseMessage;
        private BigDecimal availableCredit;
        private LocalDateTime processedAt;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    private static class FraudCheckResult {
        private boolean passed;
        private String reason;
    }
}
