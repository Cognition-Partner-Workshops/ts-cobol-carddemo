package com.carddemo.service;

import com.carddemo.dto.AccountViewDto;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.Customer;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CardAccountXrefRepository xrefRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAcctId(1000000001L);
        testAccount.setActiveStatus("Y");
        testAccount.setCurrBal(new BigDecimal("1500.00"));
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCashCreditLimit(new BigDecimal("1500.00"));
        testAccount.setOpenDate("2020-01-15");
        testAccount.setExpirationDate("2025-01-15");
        testAccount.setGroupId("GRP001");

        testCustomer = new Customer();
        testCustomer.setCustId(100001L);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
    }

    @Test
    void getAccountView_existingAccount_returnsDto() {
        CardAccountXref xref = new CardAccountXref();
        xref.setCardNum("4111111111111111");
        xref.setAcctId(1000000001L);
        xref.setCustId(100001L);

        Card card = new Card();
        card.setCardNum("4111111111111111");
        card.setAcctId(1000000001L);
        card.setActiveStatus("Y");

        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(testAccount));
        when(xrefRepository.findByAcctId(1000000001L)).thenReturn(List.of(xref));
        when(customerRepository.findById(100001L)).thenReturn(Optional.of(testCustomer));
        when(cardRepository.findByAcctId(1000000001L)).thenReturn(List.of(card));

        AccountViewDto dto = accountService.getAccountView(1000000001L);

        assertNotNull(dto);
        assertEquals(1000000001L, dto.getAcctId());
        assertEquals("John", dto.getCustFirstName());
        assertEquals("Doe", dto.getCustLastName());
        assertEquals(1, dto.getCards().size());
    }

    @Test
    void getAccountView_nonExistent_throwsResourceNotFoundException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.getAccountView(999L));
    }

    @Test
    void updateAccount_existingAccount_updatesFields() {
        Account updates = new Account();
        updates.setCreditLimit(new BigDecimal("10000.00"));
        updates.setActiveStatus("N");

        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.updateAccount(1000000001L, updates);

        assertEquals(new BigDecimal("10000.00"), result.getCreditLimit());
        assertEquals("N", result.getActiveStatus());
    }

    @Test
    void listAccounts_returnsPagedResults() {
        Page<Account> page = new PageImpl<>(List.of(testAccount));
        when(accountRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Account> result = accountService.listAccounts(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }
}
