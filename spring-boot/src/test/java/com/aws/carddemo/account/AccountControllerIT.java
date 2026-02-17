package com.aws.carddemo.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.aws.carddemo.account.dto.AccountUpdateRequest;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.customer.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Long accountId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = new Customer();
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        customer.setAddressLine1("100 Test Blvd");
        customer.setCity("Testville");
        customer.setState("TX");
        customer.setZipCode("75001");
        customer.setCountryCode("US");
        customer.setSsn("111223333");
        customer = customerRepository.save(customer);
        customerId = customer.getId();

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountStatus("A");
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCurrentBalance(new BigDecimal("2500.00"));
        account.setCashCreditLimit(new BigDecimal("3000.00"));
        account.setOpenDate(LocalDate.of(2024, 1, 15));
        account.setExpirationDate(LocalDate.of(2028, 1, 15));
        account.setGroupId("GRP01");
        account = accountRepository.save(account);
        accountId = account.getId();
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getAccountReturnsDetails() throws Exception {
        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.accountStatus").value("A"))
                .andExpect(jsonPath("$.creditLimit").value(10000.00))
                .andExpect(jsonPath("$.customer.firstName").value("Test"))
                .andExpect(jsonPath("$.customer.lastName").value("Customer"))
                .andExpect(jsonPath("$.cardCount").value(0));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getAccountNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/accounts/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccountUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateAccountStatus() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest(
                "S", null, null, null, null, null);

        mockMvc.perform(put("/api/accounts/" + accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("S"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateAccountInvalidStatusTransitionReturns400() throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setAccountStatus("C");
        accountRepository.save(account);

        AccountUpdateRequest request = new AccountUpdateRequest(
                "A", null, null, null, null, null);

        mockMvc.perform(put("/api/accounts/" + accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition from 'C' to 'A'"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateAccountCreditLimit() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest(
                null, new BigDecimal("15000.00"), null, null, null, null);

        mockMvc.perform(put("/api/accounts/" + accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(15000.00));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateAccountCreditLimitBelowBalanceReturns400() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest(
                null, new BigDecimal("1000.00"), null, null, null, null);

        mockMvc.perform(put("/api/accounts/" + accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credit limit cannot be less than current balance"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateAccountInvalidStatusValueReturns400() throws Exception {
        String json = "{\"accountStatus\": \"X\"}";

        mockMvc.perform(put("/api/accounts/" + accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listAccountsByCustomer() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .param("customerId", customerId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(accountId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listAccountsByCustomerEmpty() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .param("customerId", "999999")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateAccountMultipleFields() throws Exception {
        LocalDate newExpiry = LocalDate.now().plusYears(3);
        AccountUpdateRequest request = new AccountUpdateRequest(
                null, new BigDecimal("20000.00"), new BigDecimal("5000.00"),
                newExpiry, null, "NEWGRP");

        mockMvc.perform(put("/api/accounts/" + accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(20000.00))
                .andExpect(jsonPath("$.cashCreditLimit").value(5000.00))
                .andExpect(jsonPath("$.groupId").value("NEWGRP"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void paginationWithSizeParameter() throws Exception {
        Account account2 = new Account();
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        account2.setCustomer(customer);
        account2.setAccountStatus("A");
        account2.setCreditLimit(new BigDecimal("5000.00"));
        account2.setCurrentBalance(BigDecimal.ZERO);
        account2.setCashCreditLimit(BigDecimal.ZERO);
        account2.setOpenDate(LocalDate.of(2024, 6, 1));
        account2.setExpirationDate(LocalDate.of(2028, 6, 1));
        accountRepository.save(account2);

        mockMvc.perform(get("/api/accounts")
                        .param("customerId", customerId.toString())
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
