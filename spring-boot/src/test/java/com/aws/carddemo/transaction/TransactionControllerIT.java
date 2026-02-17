package com.aws.carddemo.transaction;

import static org.hamcrest.Matchers.hasSize;
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

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.billing.CategoryBalanceRepository;
import com.aws.carddemo.card.Card;
import com.aws.carddemo.card.CardRepository;
import com.aws.carddemo.card.CardXref;
import com.aws.carddemo.card.CardXrefRepository;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.customer.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIT {

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

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    @Autowired
    private CategoryBalanceRepository categoryBalanceRepository;

    private Long transactionId;

    @BeforeEach
    void setUp() {
        categoryBalanceRepository.deleteAll();
        transactionRecordRepository.deleteAll();
        transactionCategoryRepository.deleteAll();
        transactionTypeRepository.deleteAll();
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAddressLine1("123 Main St");
        customer.setCity("Springfield");
        customer.setState("IL");
        customer.setZipCode("62701");
        customer.setCountryCode("US");
        customer.setSsn("123-45-6789");
        customer.setFicoScore((short) 750);
        customer = customerRepository.save(customer);

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountStatus("A");
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCurrentBalance(new BigDecimal("2500.00"));
        account.setCashCreditLimit(new BigDecimal("2000.00"));
        account.setOpenDate(LocalDate.of(2020, 1, 15));
        account.setExpirationDate(LocalDate.of(2030, 12, 31));
        account = accountRepository.save(account);

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber("4111111111111111");
        card.setCardStatus("A");
        card.setEmbossedName("JOHN DOE");
        card.setCvvCode("123");
        card.setIssuedDate(LocalDate.of(2023, 1, 1));
        card.setExpiryDate(LocalDate.of(2028, 12, 31));
        cardRepository.save(card);

        CardXref xref = new CardXref();
        xref.setCardNumber("4111111111111111");
        xref.setAccount(account);
        xref.setCustomer(customer);
        cardXrefRepository.save(xref);

        TransactionType saleType = new TransactionType();
        saleType.setTypeCd("SA");
        saleType.setTypeDesc("Sale");
        transactionTypeRepository.save(saleType);

        TransactionType returnType = new TransactionType();
        returnType.setTypeCd("RT");
        returnType.setTypeDesc("Return");
        transactionTypeRepository.save(returnType);

        TransactionCategory retailCategory = new TransactionCategory();
        retailCategory.setCatCd("0001");
        retailCategory.setCatDesc("Retail Purchase");
        retailCategory.setTransactionType(saleType);
        transactionCategoryRepository.save(retailCategory);

        TransactionRecord record1 = new TransactionRecord();
        record1.setCard(card);
        record1.setTransactionType("SA");
        record1.setTransactionCategory("0001");
        record1.setTransactionSource("POS");
        record1.setDescription("Grocery Store");
        record1.setAmount(new BigDecimal("45.50"));
        record1.setTimestamp(LocalDateTime.of(2025, 6, 15, 10, 30));
        record1.setMerchantId("GROC001");
        record1.setMerchantName("Fresh Mart");
        record1.setMerchantCity("Springfield");
        record1.setMerchantZip("62701");
        record1 = transactionRecordRepository.save(record1);
        transactionId = record1.getId();

        TransactionRecord record2 = new TransactionRecord();
        record2.setCard(card);
        record2.setTransactionType("SA");
        record2.setTransactionCategory("0001");
        record2.setTransactionSource("ONLINE");
        record2.setDescription("Online Purchase");
        record2.setAmount(new BigDecimal("120.00"));
        record2.setTimestamp(LocalDateTime.of(2025, 7, 1, 14, 0));
        record2.setMerchantId("AMZN001");
        record2.setMerchantName("Amazon");
        record2.setMerchantCity("Seattle");
        record2.setMerchantZip("98101");
        transactionRecordRepository.save(record2);

        TransactionRecord record3 = new TransactionRecord();
        record3.setCard(card);
        record3.setTransactionType("RT");
        record3.setTransactionCategory("0001");
        record3.setTransactionSource("POS");
        record3.setDescription("Return Item");
        record3.setAmount(new BigDecimal("15.00"));
        record3.setTimestamp(LocalDateTime.of(2025, 7, 10, 9, 0));
        transactionRecordRepository.save(record3);
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listTransactions_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("cardNumber", "4111111111111111")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("************1111"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listTransactions_paginationWorks() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("cardNumber", "4111111111111111")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listTransactions_filterByDateRange() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("cardNumber", "4111111111111111")
                        .param("fromDate", "2025-07-01T00:00:00")
                        .param("toDate", "2025-07-31T23:59:59")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listTransactions_filterByType() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("cardNumber", "4111111111111111")
                        .param("transactionType", "RT")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].typeCode").value("RT"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listTransactions_filterByAmountRange() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("cardNumber", "4111111111111111")
                        .param("minAmount", "50.00")
                        .param("maxAmount", "200.00")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(120.00));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listTransactions_sortedByTimestampDesc() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("cardNumber", "4111111111111111")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Return Item"))
                .andExpect(jsonPath("$.content[2].description").value("Grocery Store"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getTransaction_returnsDetails() throws Exception {
        mockMvc.perform(get("/api/transactions/" + transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                .andExpect(jsonPath("$.transactionType").value("SA"))
                .andExpect(jsonPath("$.typeDescription").value("Sale"))
                .andExpect(jsonPath("$.transactionCategory").value("0001"))
                .andExpect(jsonPath("$.categoryDescription").value("Retail Purchase"))
                .andExpect(jsonPath("$.amount").value(45.50))
                .andExpect(jsonPath("$.merchantName").value("Fresh Mart"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getTransaction_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/transactions/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void createTransaction_success() throws Exception {
        String json = """
                {
                    "cardNumber": "4111111111111111",
                    "typeCode": "SA",
                    "categoryCode": "0001",
                    "source": "POS",
                    "description": "New purchase",
                    "amount": 99.99,
                    "merchantId": "NEW001",
                    "merchantName": "New Store",
                    "merchantCity": "Chicago",
                    "merchantZip": "60601",
                    "confirmed": true
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.typeDescription").value("Sale"))
                .andExpect(jsonPath("$.categoryDescription").value("Retail Purchase"))
                .andExpect(jsonPath("$.merchantName").value("New Store"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void createTransaction_notConfirmed_returns400() throws Exception {
        String json = """
                {
                    "cardNumber": "4111111111111111",
                    "typeCode": "SA",
                    "categoryCode": "0001",
                    "source": "POS",
                    "description": "Test",
                    "amount": 50.00,
                    "confirmed": false
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void createTransaction_invalidCard_returns400() throws Exception {
        String json = """
                {
                    "cardNumber": "9999999999999999",
                    "typeCode": "SA",
                    "categoryCode": "0001",
                    "source": "POS",
                    "description": "Test",
                    "amount": 50.00,
                    "confirmed": true
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void createTransaction_invalidTypeCode_returns400() throws Exception {
        String json = """
                {
                    "cardNumber": "4111111111111111",
                    "typeCode": "XX",
                    "categoryCode": "0001",
                    "source": "POS",
                    "description": "Test",
                    "amount": 50.00,
                    "confirmed": true
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void createTransaction_invalidCategoryCode_returns400() throws Exception {
        String json = """
                {
                    "cardNumber": "4111111111111111",
                    "typeCode": "SA",
                    "categoryCode": "XXXX",
                    "source": "POS",
                    "description": "Test",
                    "amount": 50.00,
                    "confirmed": true
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void createTransaction_missingRequiredFields_returns400() throws Exception {
        String json = """
                {
                    "cardNumber": "4111111111111111"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listTransactions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("cardNumber", "4111111111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransaction_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/transactions/" + transactionId))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_unauthenticated_returns403() throws Exception {
        String json = """
                {
                    "cardNumber": "4111111111111111",
                    "typeCode": "SA",
                    "categoryCode": "0001",
                    "source": "POS",
                    "amount": 50.00,
                    "confirmed": true
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}
