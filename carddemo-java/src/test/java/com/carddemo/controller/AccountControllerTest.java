package com.carddemo.controller;

import com.carddemo.dto.AccountViewDto;
import com.carddemo.dto.CardDto;
import com.carddemo.entity.Account;
import com.carddemo.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @Test
    void viewAccount_existingAccount_returnsAccountView() {
        AccountViewDto dto = new AccountViewDto();
        dto.setAcctId(1000000001L);
        dto.setActiveStatus("Y");
        dto.setCurrBal(new BigDecimal("1500.00"));
        dto.setCreditLimit(new BigDecimal("5000.00"));
        dto.setCashCreditLimit(new BigDecimal("1500.00"));
        dto.setCustId(100001L);
        dto.setCustFirstName("John");
        dto.setCustLastName("Doe");
        dto.setCards(List.of(new CardDto()));

        when(accountService.getAccountView(1000000001L)).thenReturn(dto);

        ResponseEntity<AccountViewDto> response = accountController.viewAccount(1000000001L);

        assertNotNull(response.getBody());
        assertEquals(1000000001L, response.getBody().getAcctId());
        assertEquals("John", response.getBody().getCustFirstName());
        assertEquals(1, response.getBody().getCards().size());
    }

    @Test
    void listAccounts_returnsPagedResults() {
        Account account = new Account();
        account.setAcctId(1000000001L);
        Page<Account> page = new PageImpl<>(List.of(account));
        when(accountService.listAccounts(any())).thenReturn(page);

        ResponseEntity<Page<Account>> response = accountController.listAccounts(PageRequest.of(0, 10));

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
    }
}
