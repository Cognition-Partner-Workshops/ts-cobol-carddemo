package com.aws.carddemo.service;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.entity.Account;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.mapper.AccountMapper;
import com.aws.carddemo.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private AccountDto testAccountDto;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAcctId(12345678901L);
        testAccount.setAcctActiveStatus("Y");
        testAccount.setAcctCurrBal(new BigDecimal("1500.00"));
        testAccount.setAcctCreditLimit(new BigDecimal("5000.00"));
        testAccount.setAcctExpirationDate(LocalDate.now().plusYears(1));
        testAccount.setAcctCurrCycCredit(new BigDecimal("500.00"));
        testAccount.setAcctCurrCycDebit(new BigDecimal("200.00"));
        testAccount.setAcctGroupId("GRP001");

        testAccountDto = AccountDto.builder()
                .acctId(12345678901L)
                .acctActiveStatus("Y")
                .acctCurrBal(new BigDecimal("1500.00"))
                .acctCreditLimit(new BigDecimal("5000.00"))
                .acctExpirationDate(LocalDate.now().plusYears(1))
                .availableCredit(new BigDecimal("3500.00"))
                .active(true)
                .expired(false)
                .build();
    }

    @Nested
    @DisplayName("Account Retrieval Tests")
    class AccountRetrievalTests {

        @Test
        @DisplayName("Get account by ID returns account DTO")
        void getAccount_ExistingId_ReturnsAccountDto() {
            when(accountRepository.findById(12345678901L)).thenReturn(Optional.of(testAccount));
            when(accountMapper.toDto(testAccount)).thenReturn(testAccountDto);

            AccountDto result = accountService.getAccount(12345678901L);

            assertNotNull(result);
            assertEquals(12345678901L, result.getAcctId());
            assertEquals("Y", result.getAcctActiveStatus());
        }

        @Test
        @DisplayName("Get account by non-existent ID throws ResourceNotFoundException")
        void getAccount_NonExistentId_ThrowsException() {
            when(accountRepository.findById(99999999999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> accountService.getAccount(99999999999L));
        }

        @Test
        @DisplayName("Get all accounts returns paginated results")
        void getAllAccounts_ReturnsPagedResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> accountPage = new PageImpl<>(List.of(testAccount));
            
            when(accountRepository.findAll(pageable)).thenReturn(accountPage);
            when(accountMapper.toDto(testAccount)).thenReturn(testAccountDto);

            Page<AccountDto> result = accountService.getAllAccounts(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Get active accounts returns only active accounts")
        void getActiveAccounts_ReturnsOnlyActiveAccounts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> accountPage = new PageImpl<>(List.of(testAccount));
            
            when(accountRepository.findByAcctActiveStatus("Y", pageable)).thenReturn(accountPage);
            when(accountMapper.toDto(testAccount)).thenReturn(testAccountDto);

            Page<AccountDto> result = accountService.getActiveAccounts(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Get accounts by group ID returns matching accounts")
        void getAccountsByGroupId_ReturnsMatchingAccounts() {
            when(accountRepository.findByAcctGroupId("GRP001")).thenReturn(List.of(testAccount));
            when(accountMapper.toDtoList(any())).thenReturn(List.of(testAccountDto));

            List<AccountDto> result = accountService.getAccountsByGroupId("GRP001");

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Get expired accounts returns accounts past expiration")
        void getExpiredAccounts_ReturnsExpiredAccounts() {
            Account expiredAccount = new Account();
            expiredAccount.setAcctExpirationDate(LocalDate.now().minusDays(1));
            
            when(accountRepository.findExpiredAccounts(any(LocalDate.class)))
                    .thenReturn(List.of(expiredAccount));
            when(accountMapper.toDtoList(any())).thenReturn(List.of(testAccountDto));

            List<AccountDto> result = accountService.getExpiredAccounts();

            assertNotNull(result);
            verify(accountRepository).findExpiredAccounts(any(LocalDate.class));
        }

        @Test
        @DisplayName("Get overlimit accounts returns accounts over credit limit")
        void getOverlimitAccounts_ReturnsOverlimitAccounts() {
            when(accountRepository.findOverlimitAccounts()).thenReturn(List.of(testAccount));
            when(accountMapper.toDtoList(any())).thenReturn(List.of(testAccountDto));

            List<AccountDto> result = accountService.getOverlimitAccounts();

            assertNotNull(result);
            verify(accountRepository).findOverlimitAccounts();
        }
    }

    @Nested
    @DisplayName("Account Balance Update Tests")
    class AccountBalanceUpdateTests {

        @Test
        @DisplayName("Update account balance with positive amount increases balance")
        void updateAccountBalance_PositiveAmount_IncreasesBalance() {
            when(accountRepository.findById(12345678901L)).thenReturn(Optional.of(testAccount));

            accountService.updateAccountBalance(12345678901L, new BigDecimal("100.00"));

            verify(accountRepository).save(testAccount);
            assertEquals(new BigDecimal("1600.00"), testAccount.getAcctCurrBal());
        }

        @Test
        @DisplayName("Update account balance with negative amount decreases balance")
        void updateAccountBalance_NegativeAmount_DecreasesBalance() {
            when(accountRepository.findById(12345678901L)).thenReturn(Optional.of(testAccount));

            accountService.updateAccountBalance(12345678901L, new BigDecimal("-100.00"));

            verify(accountRepository).save(testAccount);
            assertEquals(new BigDecimal("1400.00"), testAccount.getAcctCurrBal());
        }

        @Test
        @DisplayName("Update balance on non-existent account throws exception")
        void updateAccountBalance_NonExistentAccount_ThrowsException() {
            when(accountRepository.findById(99999999999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> accountService.updateAccountBalance(99999999999L, new BigDecimal("100.00")));
        }
    }

    @Nested
    @DisplayName("Account Deactivation Tests")
    class AccountDeactivationTests {

        @Test
        @DisplayName("Deactivate account sets status to N")
        void deactivateAccount_SetsStatusToN() {
            when(accountRepository.findById(12345678901L)).thenReturn(Optional.of(testAccount));

            accountService.deactivateAccount(12345678901L);

            verify(accountRepository).save(testAccount);
            assertEquals("N", testAccount.getAcctActiveStatus());
        }

        @Test
        @DisplayName("Deactivate non-existent account throws exception")
        void deactivateAccount_NonExistentAccount_ThrowsException() {
            when(accountRepository.findById(99999999999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> accountService.deactivateAccount(99999999999L));
        }
    }
}
