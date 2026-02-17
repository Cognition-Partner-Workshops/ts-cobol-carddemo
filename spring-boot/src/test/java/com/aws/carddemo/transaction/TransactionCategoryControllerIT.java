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
class TransactionCategoryControllerIT {

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

        TransactionCategory retailCategory = new TransactionCategory();
        retailCategory.setCatCd("0001");
        retailCategory.setCatDesc("Retail Purchase");
        retailCategory.setTransactionType(saleType);
        transactionCategoryRepository.save(retailCategory);

        TransactionCategory onlineCategory = new TransactionCategory();
        onlineCategory.setCatCd("0002");
        onlineCategory.setCatDesc("Online Purchase");
        onlineCategory.setTransactionType(saleType);
        transactionCategoryRepository.save(onlineCategory);
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void listCategories_returnsAll() throws Exception {
        mockMvc.perform(get("/api/transaction-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].catCd").exists())
                .andExpect(jsonPath("$[0].catDesc").exists())
                .andExpect(jsonPath("$[0].catTypeCd").exists());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCategory_returnsDetails() throws Exception {
        mockMvc.perform(get("/api/transaction-categories/0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catCd").value("0001"))
                .andExpect(jsonPath("$.catDesc").value("Retail Purchase"))
                .andExpect(jsonPath("$.catTypeCd").value("SA"));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = {"USER"})
    void getCategory_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/transaction-categories/XXXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listCategories_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/transaction-categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCategory_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/transaction-categories/0001"))
                .andExpect(status().isForbidden());
    }
}
