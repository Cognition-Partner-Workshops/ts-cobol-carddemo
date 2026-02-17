package com.aws.carddemo.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.billing.dto.BalanceResponse;
import com.aws.carddemo.billing.dto.BillPaymentRequest;
import com.aws.carddemo.billing.dto.BillPaymentResponse;
import com.aws.carddemo.card.Card;
import com.aws.carddemo.card.CardRepository;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;
import com.aws.carddemo.transaction.TransactionRecord;
import com.aws.carddemo.transaction.TransactionRecordRepository;

@ExtendWith(MockitoExtension.class)
class BillPaymentServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    @InjectMocks
    private BillPaymentService billPaymentService;

    private Account testAccount;
    private Card testCard;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAddressLine1("123 Main St");
        customer.setCity("Springfield");
        customer.setState("IL");
        customer.setZipCode("62701");
        customer.setCountryCode("US");
        customer.setSsn("123456789");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setCustomer(customer);
        testAccount.setAccountStatus("A");
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCurrentBalance(new BigDecimal("1500.00"));
        testAccount.setCashCreditLimit(new BigDecimal("2000.00"));
        testAccount.setOpenDate(LocalDate.of(2024, 1, 1));
        testAccount.setExpirationDate(LocalDate.of(2027, 12, 31));
        testAccount.setVersion(0L);

        testCard = new Card();
        testCard.setId(1L);
        testCard.setAccount(testAccount);
        testCard.setCardNumber("4111111111111111");
        testCard.setCardStatus("A");
        testCard.setEmbossedName("JOHN DOE");
        testCard.setCvvCode("123");
        testCard.setIssuedDate(LocalDate.of(2024, 1, 1));
        testCard.setExpiryDate(LocalDate.of(2027, 12, 31));
    }

    @Test
    void getBalanceReturnsResponse() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        BalanceResponse response = billPaymentService.getBalance(1L);

        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.currentBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(response.accountStatus()).isEqualTo("A");
    }

    @Test
    void getBalanceAccountNotFoundThrows() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billPaymentService.getBalance(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void processPaymentSuccessful() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(cardRepository.findByAccountId(1L)).thenReturn(List.of(testCard));

        TransactionRecord savedTransaction = new TransactionRecord();
        savedTransaction.setId(100L);
        savedTransaction.setCard(testCard);
        savedTransaction.setAmount(new BigDecimal("-1500.00"));
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenReturn(savedTransaction);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        BillPaymentRequest request = new BillPaymentRequest(1L, true);
        BillPaymentResponse response = billPaymentService.processPayment(request);

        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.amountPaid()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(response.newBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.transactionId()).isEqualTo(100L);
        assertThat(response.timestamp()).isNotNull();

        verify(transactionRecordRepository).save(any(TransactionRecord.class));
        verify(accountRepository).save(testAccount);
        assertThat(testAccount.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void processPaymentZeroBalanceThrows() {
        testAccount.setCurrentBalance(BigDecimal.ZERO);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        BillPaymentRequest request = new BillPaymentRequest(1L, true);

        assertThatThrownBy(() -> billPaymentService.processPayment(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("zero balance");

        verify(transactionRecordRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void processPaymentInactiveAccountThrows() {
        testAccount.setAccountStatus("C");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        BillPaymentRequest request = new BillPaymentRequest(1L, true);

        assertThatThrownBy(() -> billPaymentService.processPayment(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not active");

        verify(transactionRecordRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void processPaymentNotConfirmedThrows() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        BillPaymentRequest request = new BillPaymentRequest(1L, false);

        assertThatThrownBy(() -> billPaymentService.processPayment(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("confirmed");

        verify(transactionRecordRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void processPaymentAccountNotFoundThrows() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        BillPaymentRequest request = new BillPaymentRequest(99L, true);

        assertThatThrownBy(() -> billPaymentService.processPayment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void processPaymentOptimisticLockConflict() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(cardRepository.findByAccountId(1L)).thenReturn(List.of(testCard));

        TransactionRecord savedTransaction = new TransactionRecord();
        savedTransaction.setId(100L);
        savedTransaction.setCard(testCard);
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenReturn(savedTransaction);
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class.getName(), 1L));

        BillPaymentRequest request = new BillPaymentRequest(1L, true);

        assertThatThrownBy(() -> billPaymentService.processPayment(request))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void processPaymentNoCardForAccountThrows() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(cardRepository.findByAccountId(1L)).thenReturn(List.of());

        BillPaymentRequest request = new BillPaymentRequest(1L, true);

        assertThatThrownBy(() -> billPaymentService.processPayment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No card found");
    }
}
