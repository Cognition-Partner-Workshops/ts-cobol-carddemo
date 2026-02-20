package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StatementGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(StatementGenerationJob.class);

    private final AccountRepository accountRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public StatementGenerationJob(AccountRepository accountRepository,
                                  CardCrossReferenceRepository cardCrossReferenceRepository,
                                  CustomerRepository customerRepository,
                                  TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.cardCrossReferenceRepository = cardCrossReferenceRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Map<String, Object>> execute(LocalDate startDate, LocalDate endDate) {
        log.info("START OF STATEMENT GENERATION JOB");
        log.info("Date range: {} to {}", startDate, endDate);

        List<Account> allAccounts = accountRepository.findAll();
        List<Map<String, Object>> statements = new ArrayList<>();

        for (Account account : allAccounts) {
            Map<String, Object> statement = generateStatement(account, startDate, endDate);
            if (statement != null) {
                statements.add(statement);
            }
        }

        log.info("Statements generated: {}", statements.size());
        log.info("END OF STATEMENT GENERATION JOB");
        return statements;
    }

    private Map<String, Object> generateStatement(Account account,
                                                   LocalDate startDate,
                                                   LocalDate endDate) {
        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findByAcctId(account.getAcctId());
        if (xrefs.isEmpty()) {
            return null;
        }

        Optional<Customer> customer = customerRepository.findById(xrefs.get(0).getCustId());

        List<String> cardNums = xrefs.stream().map(CardCrossReference::getCardNum).toList();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findByDateRange(start, end).stream()
                .filter(t -> cardNums.contains(t.getCardNum()))
                .toList();

        if (transactions.isEmpty()) {
            return null;
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Transaction txn : transactions) {
            if (txn.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalDebits = totalDebits.add(txn.getAmount());
            } else {
                totalCredits = totalCredits.add(txn.getAmount().abs());
            }
        }

        Map<String, Object> statement = new LinkedHashMap<>();
        statement.put("accountId", account.getAcctId());
        statement.put("statementDate", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        statement.put("periodStart", startDate.format(DateTimeFormatter.ISO_DATE));
        statement.put("periodEnd", endDate.format(DateTimeFormatter.ISO_DATE));

        if (customer.isPresent()) {
            Customer cust = customer.get();
            statement.put("customerName", (cust.getFirstName() + " " + cust.getLastName()).trim());
            statement.put("address", cust.getAddrLine1());
        }

        statement.put("currentBalance", account.getCurrBal());
        statement.put("creditLimit", account.getCreditLimit());
        statement.put("totalDebits", totalDebits);
        statement.put("totalCredits", totalCredits);
        statement.put("transactionCount", transactions.size());
        statement.put("transactions", transactions);

        return statement;
    }
}
