package com.aws.carddemo.controller;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.service.AccountService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private AccountDto createTestAccountDto() {
        return AccountDto.builder()
                .acctId(12345678901L)
                .acctActiveStatus("Y")
                .acctCurrBal(new BigDecimal("1500.00"))
                .acctCreditLimit(new BigDecimal("5000.00"))
                .acctExpirationDate(LocalDate.now().plusYears(1))
                .availableCredit(new BigDecimal("3500.00"))
                .active(true)
                .expired(false)
                .build();
    }

    @Test
    @DisplayName("GET /api/accounts/{acctId} - Returns account")
    @WithMockUser
    void getAccount_ExistingId_ReturnsAccount() throws Exception {
        AccountDto accountDto = createTestAccountDto();
        when(accountService.getAccount(12345678901L)).thenReturn(accountDto);

        mockMvc.perform(get("/api/accounts/12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acctId").value(12345678901L))
                .andExpect(jsonPath("$.acctActiveStatus").value("Y"));
    }

    @Test
    @DisplayName("GET /api/accounts/{acctId} - Not found returns 404")
    @WithMockUser
    void getAccount_NonExistentId_ReturnsNotFound() throws Exception {
        when(accountService.getAccount(99999999999L))
                .thenThrow(new ResourceNotFoundException("Account", "acctId", 99999999999L));

        mockMvc.perform(get("/api/accounts/99999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/accounts - Returns paginated accounts")
    @WithMockUser
    void getAllAccounts_ReturnsPaginatedResults() throws Exception {
        AccountDto accountDto = createTestAccountDto();
        Page<AccountDto> page = new PageImpl<>(List.of(accountDto));
        when(accountService.getAllAccounts(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].acctId").value(12345678901L));
    }

    @Test
    @DisplayName("GET /api/accounts/active - Returns active accounts")
    @WithMockUser
    void getActiveAccounts_ReturnsActiveAccounts() throws Exception {
        AccountDto accountDto = createTestAccountDto();
        Page<AccountDto> page = new PageImpl<>(List.of(accountDto));
        when(accountService.getActiveAccounts(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/accounts/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    @DisplayName("GET /api/accounts/expired - Returns expired accounts")
    @WithMockUser
    void getExpiredAccounts_ReturnsExpiredAccounts() throws Exception {
        when(accountService.getExpiredAccounts()).thenReturn(List.of(createTestAccountDto()));

        mockMvc.perform(get("/api/accounts/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acctId").value(12345678901L));
    }

    @Test
    @DisplayName("GET /api/accounts/overlimit - Returns overlimit accounts")
    @WithMockUser
    void getOverlimitAccounts_ReturnsOverlimitAccounts() throws Exception {
        when(accountService.getOverlimitAccounts()).thenReturn(List.of(createTestAccountDto()));

        mockMvc.perform(get("/api/accounts/overlimit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acctId").value(12345678901L));
    }

    @Test
    @DisplayName("POST /api/accounts - Admin can create account")
    @WithMockUser(roles = "ADMIN")
    void createAccount_AdminUser_CreatesAccount() throws Exception {
        AccountDto accountDto = createTestAccountDto();
        when(accountService.createAccount(any(AccountDto.class))).thenReturn(accountDto);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acctId").value(12345678901L));
    }

    @Test
    @DisplayName("POST /api/accounts - Regular user forbidden")
    @WithMockUser(roles = "USER")
    void createAccount_RegularUser_ReturnsForbidden() throws Exception {
        AccountDto accountDto = createTestAccountDto();

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/accounts/{acctId} - Admin can update account")
    @WithMockUser(roles = "ADMIN")
    void updateAccount_AdminUser_UpdatesAccount() throws Exception {
        AccountDto accountDto = createTestAccountDto();
        when(accountService.updateAccount(eq(12345678901L), any(AccountDto.class))).thenReturn(accountDto);

        mockMvc.perform(put("/api/accounts/12345678901")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acctId").value(12345678901L));
    }

    @Test
    @DisplayName("PATCH /api/accounts/{acctId}/deactivate - Admin can deactivate")
    @WithMockUser(roles = "ADMIN")
    void deactivateAccount_AdminUser_DeactivatesAccount() throws Exception {
        mockMvc.perform(patch("/api/accounts/12345678901/deactivate"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void getAccount_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/12345678901"))
                .andExpect(status().isUnauthorized());
    }
}
