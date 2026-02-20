package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Customer;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardCrossReferenceRepository cardCrossReferenceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private Customer testCustomer;
    private CardCrossReference testXref;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAcctId(1L);
        testAccount.setActiveStatus("Y");
        testAccount.setCurrBal(new BigDecimal("1940.00"));
        testAccount.setCreditLimit(new BigDecimal("20200.00"));

        testCustomer = new Customer();
        testCustomer.setCustId(1L);
        testCustomer.setFirstName("Immanuel");
        testCustomer.setLastName("Kessler");

        testXref = new CardCrossReference();
        testXref.setCardNum("9680294154603697");
        testXref.setCustId(1L);
        testXref.setAcctId(1L);
    }

    @Test
    void getAccountViewSuccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(cardCrossReferenceRepository.findByAcctId(1L)).thenReturn(List.of(testXref));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        Map<String, Object> result = accountService.getAccountView(1L);

        assertNotNull(result);
        assertEquals(testAccount, result.get("account"));
        assertEquals(testCustomer, result.get("customer"));
    }

    @Test
    void getAccountViewNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.getAccountView(999L));
    }

    @Test
    void updateAccountSuccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(testAccount)).thenReturn(testAccount);

        Account update = new Account();
        update.setCreditLimit(new BigDecimal("25000.00"));

        Account result = accountService.updateAccount(1L, update);

        assertNotNull(result);
        assertEquals(new BigDecimal("25000.00"), result.getCreditLimit());
    }
}
