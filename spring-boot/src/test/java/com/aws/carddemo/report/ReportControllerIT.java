package com.aws.carddemo.report;

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
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.customer.CustomerRepository;
import com.aws.carddemo.transaction.TransactionRecord;
import com.aws.carddemo.transaction.TransactionRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIT {

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
    private TransactionRecordRepository transactionRecordRepository;

    @BeforeEach
    void setUp() {
        transactionRecordRepository.deleteAll();
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setAddressLine1("456 Oak Ave");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipCode("60601");
        customer.setCountryCode("US");
        customer.setSsn("987654321");
        customer = customerRepository.save(customer);

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountStatus("A");
        account.setCreditLimit(new BigDecimal("15000.00"));
        account.setCurrentBalance(new BigDecimal("3000.00"));
        account.setCashCreditLimit(new BigDecimal("5000.00"));
        account.setOpenDate(LocalDate.of(2024, 1, 1));
        account.setExpirationDate(LocalDate.of(2028, 12, 31));
        account = accountRepository.save(account);

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber("4111111111111111");
        card.setCardStatus("A");
        card.setEmbossedName("JANE SMITH");
        card.setCvvCode("123");
        card.setIssuedDate(LocalDate.of(2024, 1, 1));
        card.setExpiryDate(LocalDate.of(2028, 12, 31));
        card = cardRepository.save(card);

        TransactionRecord txn = new TransactionRecord();
        txn.setCard(card);
        txn.setTransactionType("SA");
        txn.setTransactionCategory("5001");
        txn.setTransactionSource("ONLINE");
        txn.setAmount(new BigDecimal("250.00"));
        txn.setTimestamp(LocalDateTime.of(2025, 6, 15, 10, 30));
        txn.setDescription("Grocery purchase");
        txn.setMerchantName("Fresh Mart");
        txn.setMerchantCity("Chicago");
        transactionRecordRepository.save(txn);
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void submitReportReturnsJobId() throws Exception {
        String json = """
                {"reportType": "MONTHLY", "month": 6, "year": 2025}
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void submitAndCheckReportStatus() throws Exception {
        String json = """
                {"reportType": "MONTHLY", "month": 6, "year": 2025}
                """;

        MvcResult result = mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString()).get("jobId").asText();

        Thread.sleep(500);

        mockMvc.perform(get("/api/reports/" + jobId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void submitAndDownloadReport() throws Exception {
        String json = """
                {"reportType": "MONTHLY", "month": 6, "year": 2025}
                """;

        MvcResult result = mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString()).get("jobId").asText();

        Thread.sleep(1000);

        mockMvc.perform(get("/api/reports/" + jobId + "/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType").value("MONTHLY"))
                .andExpect(jsonPath("$.grandTotal").isNumber())
                .andExpect(jsonPath("$.cardGroups").isArray());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void submitAndDownloadReportAsCsv() throws Exception {
        String json = """
                {"reportType": "MONTHLY", "month": 6, "year": 2025}
                """;

        MvcResult result = mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString()).get("jobId").asText();

        Thread.sleep(1000);

        mockMvc.perform(get("/api/reports/" + jobId + "/download")
                        .param("format", "csv"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void submitReportInvalidTypeReturns400() throws Exception {
        String json = """
                {"reportType": "MONTHLY"}
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getStatusNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/reports/nonexistent/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitReportUnauthenticatedReturns401() throws Exception {
        String json = """
                {"reportType": "MONTHLY", "month": 6, "year": 2025}
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void submitYearlyReport() throws Exception {
        String json = """
                {"reportType": "YEARLY", "year": 2025}
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void submitCustomReport() throws Exception {
        String json = """
                {"reportType": "CUSTOM", "startDate": "2025-06-01", "endDate": "2025-06-30"}
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty());
    }
}
