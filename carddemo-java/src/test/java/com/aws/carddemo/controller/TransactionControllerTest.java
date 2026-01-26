package com.aws.carddemo.controller;

import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.exception.TransactionValidationException;
import com.aws.carddemo.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionDto createTestTransactionDto() {
        return TransactionDto.builder()
                .tranId("TRN0000000000001")
                .tranCardNum("4111111111111111")
                .tranAmt(new BigDecimal("100.00"))
                .tranTypeCd("PU")
                .tranCatCd(1)
                .tranOrigTs(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/transactions - Returns paginated transactions")
    @WithMockUser
    void getAllTransactions_ReturnsPaginatedResults() throws Exception {
        TransactionDto transactionDto = createTestTransactionDto();
        Page<TransactionDto> page = new PageImpl<>(List.of(transactionDto));
        when(transactionService.getAllTransactions(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tranId").value("TRN0000000000001"));
    }

    @Test
    @DisplayName("POST /api/transactions/post - Valid transaction posts successfully")
    @WithMockUser
    void postTransaction_ValidTransaction_ReturnsCreated() throws Exception {
        TransactionDto transactionDto = createTestTransactionDto();
        when(transactionService.postTransaction(any(TransactionDto.class))).thenReturn(transactionDto);

        mockMvc.perform(post("/api/transactions/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tranId").value("TRN0000000000001"));
    }

    @Test
    @DisplayName("POST /api/transactions/post - Invalid card returns error code 100")
    @WithMockUser
    void postTransaction_InvalidCard_ReturnsValidationError() throws Exception {
        TransactionDto transactionDto = createTestTransactionDto();
        when(transactionService.postTransaction(any(TransactionDto.class)))
                .thenThrow(TransactionValidationException.invalidCardNumber());

        mockMvc.perform(post("/api/transactions/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(100))
                .andExpect(jsonPath("$.message").value("INVALID CARD NUMBER FOUND"));
    }

    @Test
    @DisplayName("POST /api/transactions/post - Account not found returns error code 101")
    @WithMockUser
    void postTransaction_AccountNotFound_ReturnsValidationError() throws Exception {
        TransactionDto transactionDto = createTestTransactionDto();
        when(transactionService.postTransaction(any(TransactionDto.class)))
                .thenThrow(TransactionValidationException.accountNotFound());

        mockMvc.perform(post("/api/transactions/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(101))
                .andExpect(jsonPath("$.message").value("ACCOUNT RECORD NOT FOUND"));
    }

    @Test
    @DisplayName("POST /api/transactions/post - Overlimit returns error code 102")
    @WithMockUser
    void postTransaction_Overlimit_ReturnsValidationError() throws Exception {
        TransactionDto transactionDto = createTestTransactionDto();
        when(transactionService.postTransaction(any(TransactionDto.class)))
                .thenThrow(TransactionValidationException.overlimitTransaction());

        mockMvc.perform(post("/api/transactions/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(102))
                .andExpect(jsonPath("$.message").value("OVERLIMIT TRANSACTION"));
    }

    @Test
    @DisplayName("POST /api/transactions/post - Expired account returns error code 103")
    @WithMockUser
    void postTransaction_ExpiredAccount_ReturnsValidationError() throws Exception {
        TransactionDto transactionDto = createTestTransactionDto();
        when(transactionService.postTransaction(any(TransactionDto.class)))
                .thenThrow(TransactionValidationException.accountExpired());

        mockMvc.perform(post("/api/transactions/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(103))
                .andExpect(jsonPath("$.message").value("TRANSACTION RECEIVED AFTER ACCT EXPIRATION"));
    }

    @Test
    @DisplayName("POST /api/transactions/validate - Valid transaction returns OK")
    @WithMockUser
    void validateTransaction_ValidTransaction_ReturnsOk() throws Exception {
        TransactionDto transactionDto = createTestTransactionDto();

        mockMvc.perform(post("/api/transactions/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/transactions/validate - Invalid transaction returns error")
    @WithMockUser
    void validateTransaction_InvalidTransaction_ReturnsError() throws Exception {
        TransactionDto transactionDto = createTestTransactionDto();
        doThrow(TransactionValidationException.invalidCardNumber())
                .when(transactionService).validateTransaction(any(TransactionDto.class));

        mockMvc.perform(post("/api/transactions/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(100));
    }

    @Test
    @DisplayName("GET /api/transactions/card/{cardNum}/sum - Returns sum")
    @WithMockUser
    void sumTransactionsByCard_ReturnsSum() throws Exception {
        when(transactionService.sumTransactionsByCard("4111111111111111"))
                .thenReturn(new BigDecimal("1500.00"));

        mockMvc.perform(get("/api/transactions/card/4111111111111111/sum"))
                .andExpect(status().isOk())
                .andExpect(content().string("1500.00"));
    }
}
