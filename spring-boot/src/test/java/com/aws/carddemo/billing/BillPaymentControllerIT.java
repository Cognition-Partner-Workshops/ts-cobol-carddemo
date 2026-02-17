package com.aws.carddemo.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.billing.dto.BillPaymentRequest;
import com.aws.carddemo.card.Card;
import com.aws.carddemo.card.CardRepository;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.customer.CustomerRepository;
import com.aws.carddemo.transaction.TransactionRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillPaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    private Long accountId;

    @BeforeEach
    void setUp() {
        transactionRecordRepository.deleteAll();
        cardRepository.deleteAll();
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

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber("4111111111111111");
        card.setCardStatus("A");
        card.setEmbossedName("TEST CUSTOMER");
        card.setCvvCode("123");
        card.setIssuedDate(LocalDate.of(2024, 1, 15));
        card.setExpiryDate(LocalDate.of(2028, 1, 15));
        cardRepository.save(card);
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getBalanceReturnsAccountBalance() throws Exception {
        mockMvc.perform(get("/api/billing/balance")
                        .param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.currentBalance").value(2500.00))
                .andExpect(jsonPath("$.accountStatus").value("A"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getBalanceAccountNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/billing/balance")
                        .param("accountId", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBalanceUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/billing/balance")
                        .param("accountId", accountId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void processPaymentFullFlow() throws Exception {
        BillPaymentRequest request = new BillPaymentRequest(accountId, true);

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.amountPaid").value(2500.00))
                .andExpect(jsonPath("$.newBalance").value(0.00))
                .andExpect(jsonPath("$.transactionId").isNumber())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        mockMvc.perform(get("/api/billing/balance")
                        .param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(0.00));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void processPaymentNotConfirmedReturns400() throws Exception {
        BillPaymentRequest request = new BillPaymentRequest(accountId, false);

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment must be confirmed"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void processPaymentZeroBalanceReturns400() throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setCurrentBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        BillPaymentRequest request = new BillPaymentRequest(accountId, true);

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account has zero balance; nothing to pay"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void processPaymentInactiveAccountReturns400() throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setAccountStatus("C");
        accountRepository.save(account);

        BillPaymentRequest request = new BillPaymentRequest(accountId, true);

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account is not active"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void processPaymentAccountNotFoundReturns404() throws Exception {
        BillPaymentRequest request = new BillPaymentRequest(999999L, true);

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void processPaymentMissingAccountIdReturns400() throws Exception {
        String json = "{\"confirmed\": true}";

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPaymentUnauthenticatedReturns401() throws Exception {
        BillPaymentRequest request = new BillPaymentRequest(accountId, true);

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void processPaymentTwiceSecondReturnsZeroBalance() throws Exception {
        BillPaymentRequest request = new BillPaymentRequest(accountId, true);

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaid").value(2500.00));

        mockMvc.perform(post("/api/billing/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account has zero balance; nothing to pay"));
    }
}
