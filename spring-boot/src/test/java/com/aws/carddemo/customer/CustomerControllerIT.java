package com.aws.carddemo.customer;

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
import com.aws.carddemo.customer.dto.CustomerUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Long customerId;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = new Customer();
        customer.setFirstName("Alice");
        customer.setLastName("Wonderland");
        customer.setMiddleName("B");
        customer.setAddressLine1("1 Rabbit Ln");
        customer.setCity("Teaparty");
        customer.setState("CA");
        customer.setZipCode("90001");
        customer.setCountryCode("US");
        customer.setPhoneNumber("8005551234");
        customer.setSsn("999887766");
        customer.setFicoScore((short) 800);
        customer = customerRepository.save(customer);
        customerId = customer.getId();
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCustomerReturnsDetails() throws Exception {
        mockMvc.perform(get("/api/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Wonderland"))
                .andExpect(jsonPath("$.state").value("CA"))
                .andExpect(jsonPath("$.ficoScore").value(800))
                .andExpect(jsonPath("$.accounts").isArray());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCustomerWithLinkedAccounts() throws Exception {
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountStatus("A");
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCashCreditLimit(new BigDecimal("2000.00"));
        account.setOpenDate(LocalDate.of(2024, 1, 1));
        account.setExpirationDate(LocalDate.of(2028, 1, 1));
        accountRepository.save(account);

        mockMvc.perform(get("/api/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts[0].accountStatus").value("A"))
                .andExpect(jsonPath("$.accounts[0].creditLimit").value(5000.00));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCustomerNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/customers/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCustomerUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/customers/" + customerId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCustomerName() throws Exception {
        CustomerUpdateRequest request = new CustomerUpdateRequest(
                "Bob", null, "Builder", null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/customers/" + customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Bob"))
                .andExpect(jsonPath("$.lastName").value("Builder"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCustomerAddress() throws Exception {
        CustomerUpdateRequest request = new CustomerUpdateRequest(
                null, null, null, "500 New St", "Apt 10", "Denver", "CO", "80201", null, null);

        mockMvc.perform(put("/api/customers/" + customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine1").value("500 New St"))
                .andExpect(jsonPath("$.addressLine2").value("Apt 10"))
                .andExpect(jsonPath("$.city").value("Denver"))
                .andExpect(jsonPath("$.state").value("CO"))
                .andExpect(jsonPath("$.zipCode").value("80201"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCustomerInvalidStateReturns400() throws Exception {
        CustomerUpdateRequest request = new CustomerUpdateRequest(
                null, null, null, null, null, null, "XX", null, null, null);

        mockMvc.perform(put("/api/customers/" + customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCustomerInvalidZipReturns400() throws Exception {
        CustomerUpdateRequest request = new CustomerUpdateRequest(
                null, null, null, null, null, null, null, "ABCDE", null, null);

        mockMvc.perform(put("/api/customers/" + customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listCustomersWithLastNameFilter() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .param("lastName", "Wonder")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].lastName").value("Wonderland"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listCustomersWithNoMatchReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .param("lastName", "Nonexistent")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listAllCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void paginationWorks() throws Exception {
        Customer c2 = new Customer();
        c2.setFirstName("Charlie");
        c2.setLastName("Chaplin");
        c2.setAddressLine1("2 Film St");
        c2.setCity("Hollywood");
        c2.setState("CA");
        c2.setZipCode("90028");
        c2.setCountryCode("US");
        c2.setSsn("111222333");
        customerRepository.save(c2);

        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void updateCustomerNotFoundReturns404() throws Exception {
        CustomerUpdateRequest request = new CustomerUpdateRequest(
                "New", null, null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/customers/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
