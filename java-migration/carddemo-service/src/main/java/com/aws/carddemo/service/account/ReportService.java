package com.aws.carddemo.service.account;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Transaction;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.CardRepository;
import com.aws.carddemo.domain.repository.CustomerRepository;
import com.aws.carddemo.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Report Service - migrated from CORPT00C.cbl
 * Generates various reports for the CardDemo application
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Generate account summary report
     */
    @Transactional(readOnly = true)
    public AccountSummaryReport generateAccountSummaryReport() {
        log.info("Generating account summary report");

        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countActiveAccounts();
        BigDecimal totalBalance = accountRepository.getTotalActiveBalance();
        BigDecimal totalCreditLimit = accountRepository.getTotalCreditLimit();
        List<Account> overLimitAccounts = accountRepository.findOverLimitAccounts();

        BigDecimal utilizationRate = BigDecimal.ZERO;
        if (totalCreditLimit != null && totalCreditLimit.compareTo(BigDecimal.ZERO) > 0) {
            utilizationRate = totalBalance.divide(totalCreditLimit, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return AccountSummaryReport.builder()
                .reportDate(LocalDate.now())
                .totalAccounts(totalAccounts)
                .activeAccounts(activeAccounts)
                .inactiveAccounts(totalAccounts - activeAccounts)
                .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                .totalCreditLimit(totalCreditLimit != null ? totalCreditLimit : BigDecimal.ZERO)
                .creditUtilizationRate(utilizationRate)
                .overLimitAccountCount(overLimitAccounts.size())
                .build();
    }

    /**
     * Generate transaction summary report
     */
    @Transactional(readOnly = true)
    public TransactionSummaryReport generateTransactionSummaryReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating transaction summary report from {} to {}", startDate, endDate);

        List<Transaction> transactions = transactionRepository.findByDateRange(startDate, endDate);
        
        long totalTransactions = transactions.size();
        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDebits = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = BigDecimal.ZERO;
        if (totalTransactions > 0) {
            averageAmount = totalAmount.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);
        }

        List<Object[]> summaryByType = transactionRepository.getTransactionSummaryByType(startDate, endDate);

        return TransactionSummaryReport.builder()
                .reportDate(LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .totalTransactions(totalTransactions)
                .totalAmount(totalAmount)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .averageTransactionAmount(averageAmount)
                .transactionsByType(summaryByType.stream()
                        .map(row -> TransactionTypeSummary.builder()
                                .typeCode((String) row[0])
                                .count((Long) row[1])
                                .totalAmount((BigDecimal) row[2])
                                .build())
                        .toList())
                .build();
    }

    /**
     * Generate card status report
     */
    @Transactional(readOnly = true)
    public CardStatusReport generateCardStatusReport() {
        log.info("Generating card status report");

        long totalCards = cardRepository.count();
        long activeCards = cardRepository.countActiveCards();
        long expiredCards = cardRepository.findExpiredActiveCards().size();
        
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        long expiringCards = cardRepository.findExpiringCards(thirtyDaysFromNow).size();

        return CardStatusReport.builder()
                .reportDate(LocalDate.now())
                .totalCards(totalCards)
                .activeCards(activeCards)
                .inactiveCards(totalCards - activeCards)
                .expiredActiveCards(expiredCards)
                .expiringWithin30Days(expiringCards)
                .build();
    }

    /**
     * Generate customer statistics report
     */
    @Transactional(readOnly = true)
    public CustomerStatisticsReport generateCustomerStatisticsReport() {
        log.info("Generating customer statistics report");

        long totalCustomers = customerRepository.count();
        long primaryCardHolders = customerRepository.countPrimaryCardHolders();

        return CustomerStatisticsReport.builder()
                .reportDate(LocalDate.now())
                .totalCustomers(totalCustomers)
                .primaryCardHolders(primaryCardHolders)
                .build();
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AccountSummaryReport {
        private LocalDate reportDate;
        private long totalAccounts;
        private long activeAccounts;
        private long inactiveAccounts;
        private BigDecimal totalBalance;
        private BigDecimal totalCreditLimit;
        private BigDecimal creditUtilizationRate;
        private int overLimitAccountCount;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransactionSummaryReport {
        private LocalDate reportDate;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private long totalTransactions;
        private BigDecimal totalAmount;
        private BigDecimal totalCredits;
        private BigDecimal totalDebits;
        private BigDecimal averageTransactionAmount;
        private List<TransactionTypeSummary> transactionsByType;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransactionTypeSummary {
        private String typeCode;
        private Long count;
        private BigDecimal totalAmount;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class CardStatusReport {
        private LocalDate reportDate;
        private long totalCards;
        private long activeCards;
        private long inactiveCards;
        private long expiredActiveCards;
        private long expiringWithin30Days;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class CustomerStatisticsReport {
        private LocalDate reportDate;
        private long totalCustomers;
        private long primaryCardHolders;
    }
}
