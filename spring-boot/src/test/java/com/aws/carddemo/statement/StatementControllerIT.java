package com.aws.carddemo.statement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.card.Card;
import com.aws.carddemo.card.CardRepository;
import com.aws.carddemo.card.CardXref;
import com.aws.carddemo.card.CardXrefRepository;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.customer.CustomerRepository;
import com.aws.carddemo.transaction.TransactionRecord;
import com.aws.carddemo.transaction.TransactionRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatementControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    private Long accountId;

    @BeforeEach
    void setUp() {
        transactionRecordRepository.deleteAll();
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = new Customer();
        customer.setFirstName("Alice");
        customer.setLastName("Johnson");
        customer.setAddressLine1("789 Elm St");
        customer.setCity("Boston");
        customer.setState("MA");
        customer.setZipCode("02101");
        customer.setCountryCode("US");
        customer.setSsn("555666777");
        customer = customerRepository.save(customer);

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountStatus("A");
        account.setCreditLimit(new BigDecimal("20000.00"));
        account.setCurrentBalance(new BigDecimal("5000.00"));
        account.setCashCreditLimit(new BigDecimal("8000.00"));
        account.setOpenDate(LocalDate.of(2024, 1, 1));
        account.setExpirationDate(LocalDate.of(2028, 12, 31));
        account = accountRepository.save(account);
        accountId = account.getId();

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber("5500000000000004");
        card.setCardStatus("A");
        card.setEmbossedName("ALICE JOHNSON");
        card.setCvvCode("456");
        card.setIssuedDate(LocalDate.of(2024, 1, 1));
        card.setExpiryDate(LocalDate.of(2028, 12, 31));
        card = cardRepository.save(card);

        CardXref xref = new CardXref();
        xref.setCardNumber("5500000000000004");
        xref.setAccount(account);
        xref.setCustomer(customer);
        cardXrefRepository.save(xref);

        TransactionRecord txn1 = new TransactionRecord();
        txn1.setCard(card);
        txn1.setTransactionType("SA");
        txn1.setTransactionCategory("5001");
        txn1.setTransactionSource("POS");
        txn1.setAmount(new BigDecimal("150.00"));
        txn1.setTimestamp(LocalDateTime.of(2025, 6, 10, 14, 30));
        txn1.setDescription("Electronics store");
        txn1.setMerchantName("Best Buy");
        txn1.setMerchantCity("Boston");
        transactionRecordRepository.save(txn1);

        TransactionRecord txn2 = new TransactionRecord();
        txn2.setCard(card);
        txn2.setTransactionType("CR");
        txn2.setTransactionSource("ONLINE");
        txn2.setAmount(new BigDecimal("-50.00"));
        txn2.setTimestamp(LocalDateTime.of(2025, 6, 20, 9, 0));
        txn2.setDescription("Refund");
        transactionRecordRepository.save(txn2);
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void generateStatementReturnsCreated() throws Exception {
        String json = String.format("""
                {"accountId": %d, "periodStartDate": "2025-06-01", "periodEndDate": "2025-06-30"}
                """, accountId);

        mockMvc.perform(post("/api/statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statementId").isNotEmpty())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$.openingBalance").isNumber())
                .andExpect(jsonPath("$.closingBalance").isNumber())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(2));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void generateAndRetrieveStatement() throws Exception {
        String json = String.format("""
                {"accountId": %d, "periodStartDate": "2025-06-01", "periodEndDate": "2025-06-30"}
                """, accountId);

        MvcResult result = mockMvc.perform(post("/api/statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        String statementId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("statementId").asText();

        mockMvc.perform(get("/api/statements/" + statementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value(statementId))
                .andExpect(jsonPath("$.accountId").value(accountId));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getStatementNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/statements/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateStatementUnauthenticatedReturns401() throws Exception {
        String json = String.format("""
                {"accountId": %d, "periodStartDate": "2025-06-01", "periodEndDate": "2025-06-30"}
                """, accountId != null ? accountId : 1);

        mockMvc.perform(post("/api/statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void generateStatementAccountNotFoundReturns404() throws Exception {
        String json = """
                {"accountId": 999999, "periodStartDate": "2025-06-01", "periodEndDate": "2025-06-30"}
                """;

        mockMvc.perform(post("/api/statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void generateStatementInvalidPeriodReturns400() throws Exception {
        String json = String.format("""
                {"accountId": %d, "periodStartDate": "2025-06-30", "periodEndDate": "2025-06-01"}
                """, accountId);

        mockMvc.perform(post("/api/statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void generateStatementIncludesCategoryBreakdown() throws Exception {
        String json = String.format("""
                {"accountId": %d, "periodStartDate": "2025-06-01", "periodEndDate": "2025-06-30"}
                """, accountId);

        mockMvc.perform(post("/api/statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryBreakdown").isMap());
    }
}
