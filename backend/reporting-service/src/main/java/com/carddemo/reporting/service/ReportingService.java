package com.carddemo.reporting.service;

import com.carddemo.common.dto.TransactionDto;
import com.carddemo.common.entity.*;
import com.carddemo.reporting.dto.AccountStatementDto;
import com.carddemo.reporting.dto.DashboardSummaryDto;
import com.carddemo.reporting.dto.TransactionReportDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    @PersistenceContext
    private EntityManager entityManager;

    public DashboardSummaryDto getDashboardSummary() {
        Long totalCustomers = countEntity(Customer.class);
        Long totalAccounts = countEntity(Account.class);
        Long totalCards = countEntity(Card.class);

        Long activeAccounts = countWithCondition("Account", "activeStatus = 'Y'");
        Long activeCards = countWithCondition("Card", "activeStatus = 'Y'");

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long transactionsToday = countTransactionsSince(startOfDay);
        Long transactionsThisMonth = countTransactionsSince(startOfMonth);

        BigDecimal totalBalance = sumAccountBalances();
        BigDecimal totalCreditLimit = sumCreditLimits();

        BigDecimal utilizationRate = BigDecimal.ZERO;
        if (totalCreditLimit.compareTo(BigDecimal.ZERO) > 0) {
            utilizationRate = totalBalance.divide(totalCreditLimit, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        Long overLimitAccounts = countOverLimitAccounts();
        Long expiringCards = countExpiringCards(30);

        return DashboardSummaryDto.builder()
                .totalCustomers(totalCustomers)
                .totalAccounts(totalAccounts)
                .activeAccounts(activeAccounts)
                .totalCards(totalCards)
                .activeCards(activeCards)
                .totalTransactionsToday(transactionsToday)
                .totalTransactionsThisMonth(transactionsThisMonth)
                .totalBalanceOutstanding(totalBalance)
                .totalCreditLimit(totalCreditLimit)
                .utilizationRate(utilizationRate)
                .overLimitAccounts(overLimitAccounts)
                .expiringCardsNext30Days(expiringCards)
                .build();
    }

    public AccountStatementDto generateAccountStatement(Long accountId, LocalDate startDate, LocalDate endDate) {
        Account account = entityManager.find(Account.class, accountId);
        if (account == null) {
            throw new RuntimeException("Account not found: " + accountId);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        TypedQuery<Transaction> query = entityManager.createQuery(
                "SELECT t FROM Transaction t JOIN Card c ON t.cardNumber = c.cardNumber " +
                "WHERE c.accountId = :accountId AND t.originationTimestamp BETWEEN :start AND :end " +
                "ORDER BY t.originationTimestamp", Transaction.class);
        query.setParameter("accountId", accountId);
        query.setParameter("start", startDateTime);
        query.setParameter("end", endDateTime);
        List<Transaction> transactions = query.getResultList();

        BigDecimal totalDebits = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal minimumPayment = account.getCurrentBalance()
                .multiply(BigDecimal.valueOf(0.02))
                .max(BigDecimal.valueOf(25));

        List<TransactionDto> transactionDtos = transactions.stream()
                .map(this::mapTransactionToDto)
                .collect(Collectors.toList());

        return AccountStatementDto.builder()
                .accountId(accountId)
                .statementStartDate(startDate)
                .statementEndDate(endDate)
                .openingBalance(account.getCurrentBalance().subtract(totalDebits).add(totalCredits))
                .closingBalance(account.getCurrentBalance())
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .minimumPaymentDue(minimumPayment)
                .paymentDueDate(endDate.plusDays(25))
                .creditLimit(account.getCreditLimit())
                .availableCredit(account.getAvailableCredit())
                .transactions(transactionDtos)
                .build();
    }

    public TransactionReportDto generateTransactionReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        TypedQuery<Transaction> query = entityManager.createQuery(
                "SELECT t FROM Transaction t WHERE t.originationTimestamp BETWEEN :start AND :end",
                Transaction.class);
        query.setParameter("start", startDateTime);
        query.setParameter("end", endDateTime);
        List<Transaction> transactions = query.getResultList();

        Long totalTransactions = (long) transactions.size();
        BigDecimal totalAmount = transactions.stream()
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalTransactions > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Long> transactionsByType = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionTypeCode, Collectors.counting()));

        Map<String, BigDecimal> amountByType = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getTransactionTypeCode,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)));

        Map<String, Long> transactionsByDay = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getOriginationTimestamp().toLocalDate().toString(),
                        Collectors.counting()));

        return TransactionReportDto.builder()
                .reportStartDate(startDate)
                .reportEndDate(endDate)
                .totalTransactions(totalTransactions)
                .totalAmount(totalAmount)
                .averageTransactionAmount(averageAmount)
                .transactionsByType(transactionsByType)
                .amountByType(amountByType)
                .transactionsByDay(transactionsByDay)
                .purchaseCount(transactionsByType.getOrDefault("PR", 0L))
                .paymentCount(transactionsByType.getOrDefault("PA", 0L))
                .refundCount(transactionsByType.getOrDefault("RF", 0L))
                .build();
    }

    private <T> Long countEntity(Class<T> entityClass) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    private Long countWithCondition(String entityName, String condition) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(e) FROM " + entityName + " e WHERE " + condition, Long.class);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    private Long countTransactionsSince(LocalDateTime since) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM Transaction t WHERE t.originationTimestamp >= :since", Long.class);
        query.setParameter("since", since);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    private BigDecimal sumAccountBalances() {
        TypedQuery<BigDecimal> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(a.currentBalance), 0) FROM Account a WHERE a.activeStatus = 'Y'", BigDecimal.class);
        BigDecimal result = query.getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    private BigDecimal sumCreditLimits() {
        TypedQuery<BigDecimal> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(a.creditLimit), 0) FROM Account a WHERE a.activeStatus = 'Y'", BigDecimal.class);
        BigDecimal result = query.getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    private Long countOverLimitAccounts() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(a) FROM Account a WHERE a.currentBalance > a.creditLimit", Long.class);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    private Long countExpiringCards(int daysAhead) {
        LocalDate endDate = LocalDate.now().plusDays(daysAhead);
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM Card c WHERE c.expirationDate BETWEEN :start AND :end AND c.activeStatus = 'Y'",
                Long.class);
        query.setParameter("start", LocalDate.now());
        query.setParameter("end", endDate);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    private TransactionDto mapTransactionToDto(Transaction transaction) {
        String maskedCardNumber = transaction.getCardNumber() != null && transaction.getCardNumber().length() >= 4
                ? "**** **** **** " + transaction.getCardNumber().substring(transaction.getCardNumber().length() - 4)
                : null;

        return TransactionDto.builder()
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
                .maskedCardNumber(maskedCardNumber)
                .originationTimestamp(transaction.getOriginationTimestamp())
                .processingTimestamp(transaction.getProcessingTimestamp())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
