package com.aws.carddemo.statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.card.Card;
import com.aws.carddemo.card.CardXref;
import com.aws.carddemo.card.CardXrefRepository;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;
import com.aws.carddemo.statement.dto.StatementRequest;
import com.aws.carddemo.statement.dto.StatementResponse;
import com.aws.carddemo.transaction.TransactionRecord;
import com.aws.carddemo.transaction.TransactionRecordRepository;

@ExtendWith(MockitoExtension.class)
class StatementServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    private StatementService statementService;

    private Account testAccount;
    private Customer testCustomer;
    private Card testCard;
    private CardXref testCardXref;

    @BeforeEach
    void setUp() {
        statementService = new StatementService(accountRepository, cardXrefRepository, transactionRecordRepository);

        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setFirstName("John");
        testCustomer.setMiddleName("M");
        testCustomer.setLastName("Doe");
        testCustomer.setAddressLine1("123 Main St");
        testCustomer.setAddressLine2("Apt 4B");
        testCustomer.setCity("Springfield");
        testCustomer.setState("IL");
        testCustomer.setZipCode("62701");
        testCustomer.setCountryCode("US");
        testCustomer.setSsn("123456789");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setCustomer(testCustomer);
        testAccount.setAccountStatus("A");
        testAccount.setCreditLimit(new BigDecimal("10000.00"));
        testAccount.setCurrentBalance(new BigDecimal("2500.00"));
        testAccount.setCashCreditLimit(new BigDecimal("3000.00"));
        testAccount.setOpenDate(LocalDate.of(2024, 1, 1));
        testAccount.setExpirationDate(LocalDate.of(2028, 12, 31));
        testAccount.setCards(new ArrayList<>());

        testCard = new Card();
        testCard.setCardNumber("4111111111111111");

        testCardXref = new CardXref();
        testCardXref.setCardNumber("4111111111111111");
        testCardXref.setAccount(testAccount);
        testCardXref.setCustomer(testCustomer);
    }

    @Test
    void generateStatementIncludesAccountSummary() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testCardXref));

        TransactionRecord txn1 = createTransaction(1L, testCard, "SA", new BigDecimal("500.00"),
                LocalDateTime.of(2025, 6, 10, 12, 0));
        TransactionRecord txn2 = createTransaction(2L, testCard, "CR", new BigDecimal("-200.00"),
                LocalDateTime.of(2025, 6, 15, 14, 0));

        when(transactionRecordRepository.findByCardCardNumberInAndTimestampBetween(
                eq(List.of("4111111111111111")), any(), any()))
                .thenReturn(List.of(txn1, txn2));

        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        StatementResponse response = statementService.generateStatement(request);

        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.customerName()).isEqualTo("John M Doe");
        assertThat(response.customerAddress()).contains("123 Main St", "Apt 4B", "Springfield", "IL", "62701");
        assertThat(response.totalDebits()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.totalCredits()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.closingBalance()).isEqualByComparingTo(new BigDecimal("2500.00"));
    }

    @Test
    void generateStatementCalculatesOpeningBalance() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testCardXref));

        TransactionRecord txn1 = createTransaction(1L, testCard, "SA", new BigDecimal("500.00"),
                LocalDateTime.of(2025, 6, 10, 12, 0));
        TransactionRecord txn2 = createTransaction(2L, testCard, "CR", new BigDecimal("-200.00"),
                LocalDateTime.of(2025, 6, 15, 14, 0));

        when(transactionRecordRepository.findByCardCardNumberInAndTimestampBetween(
                eq(List.of("4111111111111111")), any(), any()))
                .thenReturn(List.of(txn1, txn2));

        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        StatementResponse response = statementService.generateStatement(request);

        BigDecimal expectedOpening= new BigDecimal("2500.00")
                .subtract(new BigDecimal("500.00"))
                .add(new BigDecimal("200.00"));
        assertThat(response.openingBalance()).isEqualByComparingTo(expectedOpening);
    }

    @Test
    void generateStatementFiltersTransactionsByPeriod() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testCardXref));

        TransactionRecord inRange = createTransaction(1L, testCard, "SA", new BigDecimal("100.00"),
                LocalDateTime.of(2025, 6, 15, 12, 0));
        TransactionRecord beforeRange = createTransaction(2L, testCard, "SA", new BigDecimal("999.00"),
                LocalDateTime.of(2025, 5, 31, 23, 59));
        TransactionRecord afterRange = createTransaction(3L, testCard, "SA", new BigDecimal("888.00"),
                LocalDateTime.of(2025, 7, 1, 0, 1));

        when(transactionRecordRepository.findByCardCardNumberInAndTimestampBetween(
                eq(List.of("4111111111111111")), any(), any()))
                .thenReturn(List.of(inRange));

        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        StatementResponse response = statementService.generateStatement(request);

        assertThat(response.transactions()).hasSize(1);
        assertThat(response.transactions().get(0).amount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void generateStatementCategoryBreakdown() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testCardXref));

        TransactionRecord txn1 = createTransaction(1L, testCard, "SA", new BigDecimal("100.00"),
                LocalDateTime.of(2025, 6, 10, 12, 0));
        txn1.setTransactionCategory("FOOD");

        TransactionRecord txn2 = createTransaction(2L, testCard, "SA", new BigDecimal("200.00"),
                LocalDateTime.of(2025, 6, 15, 12, 0));
        txn2.setTransactionCategory("FOOD");

        TransactionRecord txn3 = createTransaction(3L, testCard, "SA", new BigDecimal("50.00"),
                LocalDateTime.of(2025, 6, 20, 12, 0));
        txn3.setTransactionCategory("GAS");

        when(transactionRecordRepository.findByCardCardNumberInAndTimestampBetween(
                eq(List.of("4111111111111111")), any(), any()))
                .thenReturn(List.of(txn1, txn2, txn3));

        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        StatementResponse response = statementService.generateStatement(request);

        assertThat(response.categoryBreakdown()).containsEntry("FOOD", new BigDecimal("300.00"));
        assertThat(response.categoryBreakdown()).containsEntry("GAS", new BigDecimal("50.00"));
    }

    @Test
    void generateStatementAccountNotFoundThrows() {
        when(accountRepository.findByIdWithCustomer(999L)).thenReturn(Optional.empty());

        StatementRequest request = new StatementRequest(999L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        assertThatThrownBy(() -> statementService.generateStatement(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void generateStatementInvalidPeriodThrows() {
        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 1));
        assertThatThrownBy(() -> statementService.generateStatement(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Period start date must be before");
    }

    @Test
    void getStatementNotFoundThrows() {
        assertThatThrownBy(() -> statementService.getStatement("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Statement not found");
    }

    @Test
    void generateStatementSortsTransactionsByDate() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testCardXref));

        TransactionRecord txn1 = createTransaction(1L, testCard, "SA", new BigDecimal("100.00"),
                LocalDateTime.of(2025, 6, 20, 12, 0));
        TransactionRecord txn2 = createTransaction(2L, testCard, "SA", new BigDecimal("50.00"),
                LocalDateTime.of(2025, 6, 5, 12, 0));

        when(transactionRecordRepository.findByCardCardNumberInAndTimestampBetween(
                eq(List.of("4111111111111111")), any(), any()))
                .thenReturn(List.of(txn1, txn2));

        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        StatementResponse response = statementService.generateStatement(request);

        assertThat(response.transactions()).hasSize(2);
        assertThat(response.transactions().get(0).timestamp())
                .isBefore(response.transactions().get(1).timestamp());
    }

    @Test
    void generateStatementWithNoTransactions() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testCardXref));
        when(transactionRecordRepository.findByCardCardNumberInAndTimestampBetween(
                eq(List.of("4111111111111111")), any(), any()))
                .thenReturn(List.of());

        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        StatementResponse response = statementService.generateStatement(request);

        assertThat(response.transactions()).isEmpty();
        assertThat(response.totalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalDebits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.openingBalance()).isEqualByComparingTo(response.closingBalance());
    }

    @Test
    void generateAndRetrieveStatement() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testCardXref));
        when(transactionRecordRepository.findByCardCardNumberInAndTimestampBetween(
                eq(List.of("4111111111111111")), any(), any()))
                .thenReturn(List.of());

        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        StatementResponse generated = statementService.generateStatement(request);

        StatementResponse retrieved = statementService.getStatement(generated.statementId());
        assertThat(retrieved.statementId()).isEqualTo(generated.statementId());
        assertThat(retrieved.accountId()).isEqualTo(generated.accountId());
    }

    @Test
    void customerNameWithoutMiddleName() {
        testCustomer.setMiddleName(null);
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testCardXref));
        when(transactionRecordRepository.findByCardCardNumberInAndTimestampBetween(
                eq(List.of("4111111111111111")), any(), any()))
                .thenReturn(List.of());

        StatementRequest request = new StatementRequest(1L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));
        StatementResponse response = statementService.generateStatement(request);

        assertThat(response.customerName()).isEqualTo("John Doe");
    }

    private TransactionRecord createTransaction(Long id, Card card, String type, BigDecimal amount,
                                                  LocalDateTime timestamp) {
        TransactionRecord txn = new TransactionRecord();
        txn.setId(id);
        txn.setCard(card);
        txn.setTransactionType(type);
        txn.setTransactionSource("ONLINE");
        txn.setAmount(amount);
        txn.setTimestamp(timestamp);
        txn.setDescription("Test transaction");
        return txn;
    }
}
