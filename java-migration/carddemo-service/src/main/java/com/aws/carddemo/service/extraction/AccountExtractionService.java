package com.aws.carddemo.service.extraction;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Customer;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Account Extraction Service - migrated from app/app-vsam-mq/
 * Handles account data extraction operations
 * 
 * Original implementation used:
 * - CODATE01 (CDRD): System date inquiry
 * - COACCT01 (CDRA): Account inquiry
 * - MQ queues for asynchronous processing
 * 
 * Migrated to:
 * - REST APIs for synchronous access
 * - Kafka topics for asynchronous processing (optional)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountExtractionService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public SystemDateResponse getSystemDate() {
        log.info("System date inquiry");
        return SystemDateResponse.builder()
                .currentDate(LocalDate.now())
                .currentDateTime(LocalDateTime.now())
                .timezone("UTC")
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<AccountInquiryResponse> getAccountInquiry(Long accountId) {
        log.info("Account inquiry for: {}", accountId);
        
        return accountRepository.findById(accountId)
                .map(account -> {
                    Customer customer = customerRepository.findByAccountId(accountId)
                            .stream()
                            .findFirst()
                            .orElse(null);

                    return AccountInquiryResponse.builder()
                            .accountId(account.getAccountId())
                            .activeStatus(account.getActiveStatus())
                            .currentBalance(account.getCurrentBalance())
                            .creditLimit(account.getCreditLimit())
                            .cashCreditLimit(account.getCashCreditLimit())
                            .availableCredit(account.getAvailableCredit())
                            .availableCashCredit(account.getAvailableCashCredit())
                            .openDate(account.getOpenDate())
                            .expirationDate(account.getExpirationDate())
                            .currentCycleCredit(account.getCurrentCycleCredit())
                            .currentCycleDebit(account.getCurrentCycleDebit())
                            .groupId(account.getGroupId())
                            .customerName(customer != null ? customer.getFullName() : null)
                            .customerId(customer != null ? customer.getCustomerId() : null)
                            .inquiryTimestamp(LocalDateTime.now())
                            .build();
                });
    }

    @Transactional(readOnly = true)
    public Page<AccountExtract> extractAccounts(AccountExtractionCriteria criteria, Pageable pageable) {
        log.info("Extracting accounts with criteria: {}", criteria);

        Page<Account> accounts;
        
        if (criteria.getGroupId() != null) {
            accounts = accountRepository.findByGroupId(criteria.getGroupId(), pageable);
        } else if (criteria.getActiveOnly() != null && criteria.getActiveOnly()) {
            accounts = accountRepository.findByActiveStatus("Y", pageable);
        } else if (criteria.getMinBalance() != null && criteria.getMaxBalance() != null) {
            accounts = accountRepository.findByBalanceRange(criteria.getMinBalance(), criteria.getMaxBalance(), pageable);
        } else {
            accounts = accountRepository.findAll(pageable);
        }

        return accounts.map(this::mapToExtract);
    }

    @Transactional(readOnly = true)
    public List<AccountExtract> extractOverLimitAccounts() {
        log.info("Extracting over-limit accounts");
        return accountRepository.findOverLimitAccounts().stream()
                .map(this::mapToExtract)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountExtract> extractExpiringAccounts(int daysUntilExpiration) {
        log.info("Extracting accounts expiring within {} days", daysUntilExpiration);
        LocalDate expirationDate = LocalDate.now().plusDays(daysUntilExpiration);
        return accountRepository.findExpiringAccounts(expirationDate).stream()
                .map(this::mapToExtract)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExtractionSummary getExtractionSummary() {
        log.info("Generating extraction summary");
        
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countActiveAccounts();
        BigDecimal totalBalance = accountRepository.getTotalActiveBalance();
        BigDecimal totalCreditLimit = accountRepository.getTotalCreditLimit();
        int overLimitCount = accountRepository.findOverLimitAccounts().size();

        return ExtractionSummary.builder()
                .totalAccounts(totalAccounts)
                .activeAccounts(activeAccounts)
                .inactiveAccounts(totalAccounts - activeAccounts)
                .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                .totalCreditLimit(totalCreditLimit != null ? totalCreditLimit : BigDecimal.ZERO)
                .overLimitAccountCount(overLimitCount)
                .extractionTimestamp(LocalDateTime.now())
                .build();
    }

    private AccountExtract mapToExtract(Account account) {
        Customer customer = customerRepository.findByAccountId(account.getAccountId())
                .stream()
                .findFirst()
                .orElse(null);

        return AccountExtract.builder()
                .accountId(account.getAccountId())
                .activeStatus(account.getActiveStatus())
                .currentBalance(account.getCurrentBalance())
                .creditLimit(account.getCreditLimit())
                .availableCredit(account.getAvailableCredit())
                .openDate(account.getOpenDate())
                .expirationDate(account.getExpirationDate())
                .groupId(account.getGroupId())
                .customerId(customer != null ? customer.getCustomerId() : null)
                .customerName(customer != null ? customer.getFullName() : null)
                .overLimit(account.isOverLimit())
                .extractTimestamp(LocalDateTime.now())
                .build();
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class SystemDateResponse {
        private LocalDate currentDate;
        private LocalDateTime currentDateTime;
        private String timezone;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AccountInquiryResponse {
        private Long accountId;
        private String activeStatus;
        private BigDecimal currentBalance;
        private BigDecimal creditLimit;
        private BigDecimal cashCreditLimit;
        private BigDecimal availableCredit;
        private BigDecimal availableCashCredit;
        private LocalDate openDate;
        private LocalDate expirationDate;
        private BigDecimal currentCycleCredit;
        private BigDecimal currentCycleDebit;
        private String groupId;
        private String customerName;
        private Long customerId;
        private LocalDateTime inquiryTimestamp;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AccountExtractionCriteria {
        private String groupId;
        private Boolean activeOnly;
        private BigDecimal minBalance;
        private BigDecimal maxBalance;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AccountExtract {
        private Long accountId;
        private String activeStatus;
        private BigDecimal currentBalance;
        private BigDecimal creditLimit;
        private BigDecimal availableCredit;
        private LocalDate openDate;
        private LocalDate expirationDate;
        private String groupId;
        private Long customerId;
        private String customerName;
        private boolean overLimit;
        private LocalDateTime extractTimestamp;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ExtractionSummary {
        private long totalAccounts;
        private long activeAccounts;
        private long inactiveAccounts;
        private BigDecimal totalBalance;
        private BigDecimal totalCreditLimit;
        private int overLimitAccountCount;
        private LocalDateTime extractionTimestamp;
    }
}
