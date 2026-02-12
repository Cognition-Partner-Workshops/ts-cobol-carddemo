package com.aws.carddemo.card;

import static org.hamcrest.Matchers.hasSize;
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

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.card.dto.CardUpdateRequest;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.customer.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardControllerIT {

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

    private Long accountId;
    private String cardNumber;

    @BeforeEach
    void setUp() {
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
        accountId = account.getId();

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber("4111111111111111");
        card.setCardStatus("A");
        card.setEmbossedName("JOHN DOE");
        card.setCvvCode("123");
        card.setIssuedDate(LocalDate.of(2023, 1, 1));
        card.setExpiryDate(LocalDate.of(2028, 12, 31));
        card = cardRepository.save(card);
        cardNumber = card.getCardNumber();

        Card card2 = new Card();
        card2.setAccount(account);
        card2.setCardNumber("5222222222222222");
        card2.setCardStatus("A");
        card2.setEmbossedName("JOHN DOE");
        card2.setCvvCode("456");
        card2.setIssuedDate(LocalDate.of(2023, 6, 1));
        card2.setExpiryDate(LocalDate.of(2029, 6, 30));
        cardRepository.save(card2);

        CardXref xref = new CardXref();
        xref.setCardNumber("4111111111111111");
        xref.setAccount(account);
        xref.setCustomer(customer);
        cardXrefRepository.save(xref);

        CardXref xref2 = new CardXref();
        xref2.setCardNumber("5222222222222222");
        xref2.setAccount(account);
        xref2.setCustomer(customer);
        cardXrefRepository.save(xref2);
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listCards_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .param("accountId", accountId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("************1111"))
                .andExpect(jsonPath("$.content[0].cardStatus").value("A"))
                .andExpect(jsonPath("$.content[0].embossedName").value("JOHN DOE"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listCards_paginationWorks() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .param("accountId", accountId.toString())
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listCards_emptyForUnknownAccount() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .param("accountId", "99999")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCard_returnsFullDetails() throws Exception {
        mockMvc.perform(get("/api/cards/" + cardNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.cardStatus").value("A"))
                .andExpect(jsonPath("$.embossedName").value("JOHN DOE"))
                .andExpect(jsonPath("$.maskedCvv").value("***"))
                .andExpect(jsonPath("$.issuedDate").value("2023-01-01"))
                .andExpect(jsonPath("$.expiryDate").value("2028-12-31"))
                .andExpect(jsonPath("$.accountSummary.accountId").value(accountId))
                .andExpect(jsonPath("$.accountSummary.creditLimit").value(10000.00))
                .andExpect(jsonPath("$.accountSummary.currentBalance").value(2500.00));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCard_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/cards/9999999999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCard_invalidFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/cards/123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCard_updatesStatus() throws Exception {
        CardUpdateRequest request = new CardUpdateRequest("C", null, null);

        mockMvc.perform(put("/api/cards/" + cardNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("C"))
                .andExpect(jsonPath("$.embossedName").value("JOHN DOE"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCard_updatesEmbossedName() throws Exception {
        CardUpdateRequest request = new CardUpdateRequest(null, "JANE DOE", null);

        mockMvc.perform(put("/api/cards/" + cardNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embossedName").value("JANE DOE"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCard_updatesExpiryDate() throws Exception {
        String futureDate = LocalDate.now().plusYears(5).toString();
        String json = "{\"expiryDate\":\"" + futureDate + "\"}";

        mockMvc.perform(put("/api/cards/" + cardNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value(futureDate));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCard_cannotReactivateCancelled() throws Exception {
        CardUpdateRequest cancelRequest = new CardUpdateRequest("C", null, null);
        mockMvc.perform(put("/api/cards/" + cardNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk());

        CardUpdateRequest reactivateRequest = new CardUpdateRequest("A", null, null);
        mockMvc.perform(put("/api/cards/" + cardNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reactivateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCard_notFound_returns404() throws Exception {
        CardUpdateRequest request = new CardUpdateRequest("C", null, null);

        mockMvc.perform(put("/api/cards/9999999999999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCardXref_returnsLinkedData() throws Exception {
        mockMvc.perform(get("/api/cards/xref")
                        .param("cardNumber", cardNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.accountStatus").value("A"))
                .andExpect(jsonPath("$.customerFirstName").value("John"))
                .andExpect(jsonPath("$.customerLastName").value("Doe"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCardXref_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/cards/xref")
                        .param("cardNumber", "9999999999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listCards_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .param("accountId", accountId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/cards/" + cardNumber))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCard_unauthenticated_returns401() throws Exception {
        CardUpdateRequest request = new CardUpdateRequest("C", null, null);

        mockMvc.perform(put("/api/cards/" + cardNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCardXref_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/cards/xref")
                        .param("cardNumber", cardNumber))
                .andExpect(status().isUnauthorized());
    }
}
