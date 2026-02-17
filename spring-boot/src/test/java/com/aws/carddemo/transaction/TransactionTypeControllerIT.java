package com.aws.carddemo.transaction;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionTypeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    @BeforeEach
    void setUp() {
        transactionCategoryRepository.deleteAll();
        transactionTypeRepository.deleteAll();

        TransactionType saleType = new TransactionType();
        saleType.setTypeCd("SA");
        saleType.setTypeDesc("Sale");
        transactionTypeRepository.save(saleType);

        TransactionType returnType = new TransactionType();
        returnType.setTypeCd("RT");
        returnType.setTypeDesc("Return");
        transactionTypeRepository.save(returnType);
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listTypes_returnsAll() throws Exception {
        mockMvc.perform(get("/api/transaction-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].typeCd").exists())
                .andExpect(jsonPath("$[0].typeDesc").exists());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getType_returnsDetails() throws Exception {
        mockMvc.perform(get("/api/transaction-types/SA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeCd").value("SA"))
                .andExpect(jsonPath("$.typeDesc").value("Sale"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getType_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/transaction-types/XX"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listTypes_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/transaction-types"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getType_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/transaction-types/SA"))
                .andExpect(status().isForbidden());
    }
}
