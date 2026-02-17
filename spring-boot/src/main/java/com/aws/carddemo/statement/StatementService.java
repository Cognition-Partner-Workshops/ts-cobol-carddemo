package com.aws.carddemo.statement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.card.CardXref;
import com.aws.carddemo.card.CardXrefRepository;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;
import com.aws.carddemo.statement.dto.StatementRequest;
import com.aws.carddemo.statement.dto.StatementResponse;
import com.aws.carddemo.statement.dto.StatementTransaction;
import com.aws.carddemo.transaction.TransactionRecord;
import com.aws.carddemo.transaction.TransactionRecordRepository;

@Service
@Transactional(readOnly = true)
public class StatementService {

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final ConcurrentHashMap<String, StatementResponse> statementStore = new ConcurrentHashMap<>();

    public StatementService(AccountRepository accountRepository,
                            CardXrefRepository cardXrefRepository,
                            TransactionRecordRepository transactionRecordRepository) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    public StatementResponse generateStatement(StatementRequest request) {
        if (request.periodStartDate().isAfter(request.periodEndDate())) {
            throw new ValidationException("Period start date must be before or equal to period end date");
        }

        Account account = accountRepository.findByIdWithCustomer(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + request.accountId()));

        Customer customer = account.getCustomer();
        String customerName = buildCustomerName(customer);
        String customerAddress = buildCustomerAddress(customer);

        List<CardXref> cardXrefs = cardXrefRepository.findByAccountId(request.accountId());
        List<String> cardNumbers = cardXrefs.stream()
                .map(CardXref::getCardNumber)
                .toList();

        LocalDateTime periodStart = request.periodStartDate().atStartOfDay();
        LocalDateTime periodEnd = request.periodEndDate().atTime(LocalTime.MAX);

        List<TransactionRecord> periodTransactions = new ArrayList<>();
        for (String cardNumber : cardNumbers) {
            List<TransactionRecord> cardTxns = transactionRecordRepository.findByCardCardNumber(cardNumber);
            for (TransactionRecord txn : cardTxns) {
                if (!txn.getTimestamp().isBefore(periodStart) && !txn.getTimestamp().isAfter(periodEnd)) {
                    periodTransactions.add(txn);
                }
            }
        }

        periodTransactions.sort(Comparator.comparing(TransactionRecord::getTimestamp));

        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryBreakdown = new LinkedHashMap<>();
        List<StatementTransaction> statementTransactions = new ArrayList<>();

        for (TransactionRecord txn : periodTransactions) {
            BigDecimal amount = txn.getAmount();
            if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                totalDebits = totalDebits.add(amount);
            } else {
                totalCredits = totalCredits.add(amount.abs());
            }

            String category = txn.getTransactionCategory() != null ? txn.getTransactionCategory() : "Uncategorized";
            categoryBreakdown.merge(category, amount, BigDecimal::add);

            statementTransactions.add(new StatementTransaction(
                    txn.getId(),
                    txn.getTimestamp(),
                    txn.getCard().getCardNumber(),
                    txn.getTransactionType(),
                    txn.getDescription(),
                    txn.getAmount()
            ));
        }

        BigDecimal closingBalance = account.getCurrentBalance();
        BigDecimal openingBalance = closingBalance.subtract(totalDebits).add(totalCredits);

        String statementId = UUID.randomUUID().toString();

        StatementResponse response = new StatementResponse(
                statementId,
                account.getId(),
                customerName,
                customerAddress,
                request.periodStartDate(),
                request.periodEndDate(),
                openingBalance,
                totalCredits,
                totalDebits,
                closingBalance,
                statementTransactions,
                categoryBreakdown
        );

        statementStore.put(statementId, response);
        return response;
    }

    public StatementResponse getStatement(String statementId) {
        StatementResponse statement = statementStore.get(statementId);
        if (statement == null) {
            throw new ResourceNotFoundException("Statement not found with id: " + statementId);
        }
        return statement;
    }

    private String buildCustomerName(Customer customer) {
        StringBuilder name = new StringBuilder();
        name.append(customer.getFirstName());
        if (customer.getMiddleName() != null && !customer.getMiddleName().isBlank()) {
            name.append(" ").append(customer.getMiddleName());
        }
        name.append(" ").append(customer.getLastName());
        return name.toString();
    }

    private String buildCustomerAddress(Customer customer) {
        StringBuilder address = new StringBuilder();
        address.append(customer.getAddressLine1());
        if (customer.getAddressLine2() != null && !customer.getAddressLine2().isBlank()) {
            address.append(", ").append(customer.getAddressLine2());
        }
        address.append(", ").append(customer.getCity());
        address.append(", ").append(customer.getState());
        address.append(" ").append(customer.getZipCode());
        return address.toString();
    }

    ConcurrentHashMap<String, StatementResponse> getStatementStore() {
        return statementStore;
    }
}
