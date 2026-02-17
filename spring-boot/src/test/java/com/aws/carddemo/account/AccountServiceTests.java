package com.aws.carddemo.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.aws.carddemo.account.dto.AccountResponse;
import com.aws.carddemo.account.dto.AccountUpdateRequest;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setAddressLine1("123 Main St");
        testCustomer.setCity("Springfield");
        testCustomer.setState("IL");
        testCustomer.setZipCode("62701");
        testCustomer.setCountryCode("US");
        testCustomer.setSsn("123456789");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setCustomer(testCustomer);
        testAccount.setAccountStatus("A");
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCurrentBalance(new BigDecimal("1000.00"));
        testAccount.setCashCreditLimit(new BigDecimal("2000.00"));
        testAccount.setOpenDate(LocalDate.of(2024, 1, 1));
        testAccount.setExpirationDate(LocalDate.of(2027, 12, 31));
        testAccount.setGroupId("GRP001");
        testAccount.setCards(new ArrayList<>());
    }

    @Test
    void getAccountReturnsResponse() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));

        AccountResponse response = accountService.getAccount(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.accountStatus()).isEqualTo("A");
        assertThat(response.creditLimit()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(response.customer().firstName()).isEqualTo("John");
        assertThat(response.cardCount()).isZero();
    }

    @Test
    void getAccountNotFoundThrows() {
        when(accountRepository.findByIdWithCustomer(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void updateAccountStatusActiveToSuspended() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountUpdateRequest request = new AccountUpdateRequest("S", null, null, null, null, null);
        accountService.updateAccount(1L, request);

        assertThat(testAccount.getAccountStatus()).isEqualTo("S");
    }

    @Test
    void updateAccountStatusActiveToClosed() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountUpdateRequest request = new AccountUpdateRequest("C", null, null, null, null, null);
        accountService.updateAccount(1L, request);

        assertThat(testAccount.getAccountStatus()).isEqualTo("C");
    }

    @Test
    void updateAccountStatusClosedToActiveThrows() {
        testAccount.setAccountStatus("C");
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));

        AccountUpdateRequest request = new AccountUpdateRequest("A", null, null, null, null, null);
        assertThatThrownBy(() -> accountService.updateAccount(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid status transition");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateAccountStatusSuspendedToActive() {
        testAccount.setAccountStatus("S");
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountUpdateRequest request = new AccountUpdateRequest("A", null, null, null, null, null);
        accountService.updateAccount(1L, request);

        assertThat(testAccount.getAccountStatus()).isEqualTo("A");
    }

    @Test
    void updateAccountSameStatusIsNoOp() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountUpdateRequest request = new AccountUpdateRequest("A", null, null, null, null, null);
        accountService.updateAccount(1L, request);

        assertThat(testAccount.getAccountStatus()).isEqualTo("A");
    }

    @Test
    void updateCreditLimitBelowBalanceThrows() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));

        AccountUpdateRequest request = new AccountUpdateRequest(null, new BigDecimal("500.00"), null, null, null, null);
        assertThatThrownBy(() -> accountService.updateAccount(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Credit limit cannot be less than current balance");
    }

    @Test
    void updateCreditLimitValid() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountUpdateRequest request = new AccountUpdateRequest(null, new BigDecimal("8000.00"), null, null, null, null);
        accountService.updateAccount(1L, request);

        assertThat(testAccount.getCreditLimit()).isEqualByComparingTo(new BigDecimal("8000.00"));
    }

    @Test
    void updateCashCreditLimitExceedsCreditLimitThrows() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));

        AccountUpdateRequest request = new AccountUpdateRequest(null, null, new BigDecimal("10000.00"), null, null, null);
        assertThatThrownBy(() -> accountService.updateAccount(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cash credit limit cannot exceed credit limit");
    }

    @Test
    void updateExpirationDateInPastThrows() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));

        AccountUpdateRequest request = new AccountUpdateRequest(null, null, null, LocalDate.of(2020, 1, 1), null, null);
        assertThatThrownBy(() -> accountService.updateAccount(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Expiration date cannot be in the past");
    }

    @Test
    void updateMultipleFieldsSucceeds() {
        when(accountRepository.findByIdWithCustomer(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        LocalDate newExpiry = LocalDate.now().plusYears(2);
        AccountUpdateRequest request = new AccountUpdateRequest(
                "S", new BigDecimal("10000.00"), new BigDecimal("3000.00"),
                newExpiry, LocalDate.now(), "NEWGRP");
        accountService.updateAccount(1L, request);

        assertThat(testAccount.getAccountStatus()).isEqualTo("S");
        assertThat(testAccount.getCreditLimit()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(testAccount.getCashCreditLimit()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(testAccount.getExpirationDate()).isEqualTo(newExpiry);
        assertThat(testAccount.getGroupId()).isEqualTo("NEWGRP");
    }

    @Test
    void listAccountsByCustomerReturnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> page = new PageImpl<>(java.util.List.of(testAccount), pageable, 1);
        when(accountRepository.findByCustomerId(eq(1L), eq(pageable))).thenReturn(page);

        Page<AccountResponse> result = accountService.listAccountsByCustomer(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    void listAllReturnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> page = new PageImpl<>(java.util.List.of(testAccount), pageable, 1);
        when(accountRepository.findAll(pageable)).thenReturn(page);

        Page<AccountResponse> result = accountService.listAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
