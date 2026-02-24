package com.carddemo.transaction.controller;

import com.carddemo.transaction.dto.*;
import com.carddemo.transaction.exception.DuplicateTransactionException;
import com.carddemo.transaction.exception.ResourceNotFoundException;
import com.carddemo.transaction.exception.ValidationException;
import com.carddemo.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller layer tests for TransactionController.
 * Tests HTTP status codes, request/response mapping, and endpoint routing.
 * Covers BR-CF-01, BR-CF-02, BR-CF-03, BR-LT-08, BR-VT-04, BR-VT-05.
 */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private TransactionListResponse sampleListResponse;
    private TransactionDetailResponse sampleDetailResponse;

    @BeforeEach
    void setUp() {
        TransactionSummaryDto summary = new TransactionSummaryDto();
        summary.setTransactionId("0000000000000001");
        summary.setTypeCode("01");
        summary.setCategoryCode(5001);
        summary.setSource("ONLINE");
        summary.setDescription("Monthly Subscription Service");
        summary.setAmount(new BigDecimal("-14.99"));
        summary.setCardNumber("4111111111111111");
        summary.setOriginationTimestamp(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        summary.setProcessingTimestamp(LocalDateTime.of(2024, 1, 1, 10, 0, 5));

        sampleListResponse = new TransactionListResponse();
        sampleListResponse.setContent(List.of(summary));
        sampleListResponse.setPage(0);
        sampleListResponse.setSize(10);
        sampleListResponse.setTotalElements(1);
        sampleListResponse.setTotalPages(1);
        sampleListResponse.setFirst(true);
        sampleListResponse.setLast(true);
        sampleListResponse.setHasNext(false);
        sampleListResponse.setHasPrevious(false);

        sampleDetailResponse = new TransactionDetailResponse();
        sampleDetailResponse.setTransactionId("0000000000000001");
        sampleDetailResponse.setCardNumber("4111111111111111");
        sampleDetailResponse.setAccountId("00000000001");
        sampleDetailResponse.setTypeCode("01");
        sampleDetailResponse.setCategoryCode(5001);
        sampleDetailResponse.setSource("ONLINE");
        sampleDetailResponse.setDescription("Monthly Subscription Service");
        sampleDetailResponse.setAmount(new BigDecimal("-14.99"));
        sampleDetailResponse.setMerchantId(100000001L);
        sampleDetailResponse.setMerchantName("StreamFlix");
        sampleDetailResponse.setMerchantCity("Los Angeles");
        sampleDetailResponse.setMerchantZip("90001");
        sampleDetailResponse.setOriginationTimestamp(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        sampleDetailResponse.setProcessingTimestamp(LocalDateTime.of(2024, 1, 1, 10, 0, 5));
    }

    @Nested
    @DisplayName("CT00 - List Transactions Endpoint")
    class ListEndpointTests {

        @Test
        @DisplayName("BR-CF-01/CF-02: GET /api/v1/transactions returns 200")
        void testListTransactions_ReturnsOk() throws Exception {
            when(transactionService.listTransactions(0, 10, null))
                    .thenReturn(sampleListResponse);

            mockMvc.perform(get("/api/v1/transactions")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("BR-LT-02: Non-numeric filter returns 400")
        void testListTransactions_NonNumericFilter_Returns400() throws Exception {
            when(transactionService.listTransactions(anyInt(), anyInt(), anyString()))
                    .thenThrow(new IllegalArgumentException("Tran ID must be Numeric ..."));

            mockMvc.perform(get("/api/v1/transactions")
                            .param("startTransactionId", "ABC"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Tran ID must be Numeric ..."));
        }

        @Test
        @DisplayName("BR-LT-04: Filter parameter is optional")
        void testListTransactions_NoFilter() throws Exception {
            when(transactionService.listTransactions(0, 10, null))
                    .thenReturn(sampleListResponse);

            mockMvc.perform(get("/api/v1/transactions"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("BR-LT-04: Numeric filter accepted")
        void testListTransactions_NumericFilter() throws Exception {
            when(transactionService.listTransactions(anyInt(), anyInt(), anyString()))
                    .thenReturn(sampleListResponse);

            mockMvc.perform(get("/api/v1/transactions")
                            .param("startTransactionId", "5"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Content type is application/json")
        void testListTransactions_ContentType() throws Exception {
            when(transactionService.listTransactions(0, 10, null))
                    .thenReturn(sampleListResponse);

            mockMvc.perform(get("/api/v1/transactions"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("CT01 - View Transaction Endpoint")
    class ViewEndpointTests {

        @Test
        @DisplayName("BR-LT-08/VT-03: GET /api/v1/transactions/{id} returns 200 with all fields")
        void testViewTransaction_ReturnsOk() throws Exception {
            when(transactionService.viewTransaction("0000000000000001"))
                    .thenReturn(sampleDetailResponse);

            mockMvc.perform(get("/api/v1/transactions/0000000000000001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transactionId").value("0000000000000001"))
                    .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                    .andExpect(jsonPath("$.accountId").value("00000000001"))
                    .andExpect(jsonPath("$.typeCode").value("01"))
                    .andExpect(jsonPath("$.categoryCode").value(5001))
                    .andExpect(jsonPath("$.source").value("ONLINE"))
                    .andExpect(jsonPath("$.description").value("Monthly Subscription Service"))
                    .andExpect(jsonPath("$.amount").value(-14.99))
                    .andExpect(jsonPath("$.merchantId").value(100000001))
                    .andExpect(jsonPath("$.merchantName").value("StreamFlix"))
                    .andExpect(jsonPath("$.merchantCity").value("Los Angeles"))
                    .andExpect(jsonPath("$.merchantZip").value("90001"))
                    .andExpect(jsonPath("$.originationTimestamp").exists())
                    .andExpect(jsonPath("$.processingTimestamp").exists());
        }

        @Test
        @DisplayName("BR-VT-01: Transaction not found returns 404")
        void testViewTransaction_NotFound_Returns404() throws Exception {
            when(transactionService.viewTransaction("9999999999999999"))
                    .thenThrow(new ResourceNotFoundException(
                            "Transaction ID NOT found...", "transactionId", "BR-VT-01"));

            mockMvc.perform(get("/api/v1/transactions/9999999999999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Transaction ID NOT found..."))
                    .andExpect(jsonPath("$.field").value("transactionId"))
                    .andExpect(jsonPath("$.businessRule").value("BR-VT-01"));
        }

        @Test
        @DisplayName("BR-VT-04: View endpoint is GET only (PUT returns error)")
        void testViewTransaction_PutNotAllowed() throws Exception {
            mockMvc.perform(put("/api/v1/transactions/0000000000000001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(result -> {
                        int statusCode = result.getResponse().getStatus();
                        assertTrue(statusCode == 405 || statusCode == 500,
                                "Expected 405 or 500 but got " + statusCode);
                    });
        }

        @Test
        @DisplayName("BR-VT-04: View endpoint is GET only (DELETE returns error)")
        void testViewTransaction_DeleteNotAllowed() throws Exception {
            mockMvc.perform(delete("/api/v1/transactions/0000000000000001"))
                    .andExpect(result -> {
                        int statusCode = result.getResponse().getStatus();
                        assertTrue(statusCode == 405 || statusCode == 500,
                                "Expected 405 or 500 but got " + statusCode);
                    });
        }
    }

    @Nested
    @DisplayName("PF5 - Latest Transaction Endpoint")
    class LatestEndpointTests {

        @Test
        @DisplayName("GET /api/v1/transactions/latest returns 200")
        void testGetLatestTransaction_ReturnsOk() throws Exception {
            LatestTransactionResponse latestResponse = new LatestTransactionResponse();
            latestResponse.setTransactionId("0000000000000015");
            latestResponse.setTypeCode("01");
            latestResponse.setCategoryCode(5411);
            latestResponse.setSource("POS");
            latestResponse.setDescription("Weekly Grocery Shopping");
            latestResponse.setAmount(new BigDecimal("-112.45"));
            latestResponse.setOriginationDate("2024-01-15");
            latestResponse.setProcessingDate("2024-01-15");
            latestResponse.setMerchantId(100000014L);
            latestResponse.setMerchantName("FreshMart");
            latestResponse.setMerchantCity("New York");
            latestResponse.setMerchantZip("10002");

            when(transactionService.getLatestTransaction())
                    .thenReturn(latestResponse);

            mockMvc.perform(get("/api/v1/transactions/latest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transactionId").value("0000000000000015"))
                    .andExpect(jsonPath("$.typeCode").value("01"));
        }

        @Test
        @DisplayName("No transactions returns 404")
        void testGetLatestTransaction_NoTransactions_Returns404() throws Exception {
            when(transactionService.getLatestTransaction())
                    .thenThrow(new ResourceNotFoundException(
                            "No transactions found", "transactionId", null));

            mockMvc.perform(get("/api/v1/transactions/latest"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No transactions found"));
        }
    }

    @Nested
    @DisplayName("CT02 - Add Transaction Endpoint")
    class AddEndpointTests {

        private AddTransactionRequest buildValidRequest() {
            AddTransactionRequest request = new AddTransactionRequest();
            request.setAccountId("1");
            request.setTypeCode("01");
            request.setCategoryCode("5001");
            request.setSource("ONLINE");
            request.setDescription("Test Transaction");
            request.setAmount("100.00");
            request.setOriginationDate("2024-01-15");
            request.setProcessingDate("2024-01-15");
            request.setMerchantId("100000001");
            request.setMerchantName("TestMerchant");
            request.setMerchantCity("TestCity");
            request.setMerchantZip("10001");
            request.setConfirmation("Y");
            return request;
        }

        @Test
        @DisplayName("Valid request with confirmation Y returns 201 Created")
        void testAddTransaction_Returns201() throws Exception {
            AddTransactionRequest request = buildValidRequest();
            AddTransactionResponse addResponse = new AddTransactionResponse();
            addResponse.setTransactionId("0000000000000016");
            addResponse.setMessage("Transaction added successfully. Your Tran ID is 0000000000000016.");
            addResponse.setTransaction(sampleDetailResponse);

            when(transactionService.addTransaction(any(AddTransactionRequest.class)))
                    .thenReturn(addResponse);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transactionId").value("0000000000000016"))
                    .andExpect(jsonPath("$.message").value(containsString("Transaction added successfully")));
        }

        @Test
        @DisplayName("Confirmation required returns 200 with confirmation prompt")
        void testAddTransaction_ConfirmationRequired_Returns200() throws Exception {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation(null);

            ConfirmationRequiredResponse confirmResp = new ConfirmationRequiredResponse();
            confirmResp.setMessage("Confirm to add this transaction...");
            confirmResp.setResolvedAccountId("00000000001");
            confirmResp.setResolvedCardNumber("4111111111111111");

            when(transactionService.addTransaction(any(AddTransactionRequest.class)))
                    .thenReturn(confirmResp);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.confirmationRequired").value(true))
                    .andExpect(jsonPath("$.message").value("Confirm to add this transaction..."))
                    .andExpect(jsonPath("$.resolvedAccountId").value("00000000001"))
                    .andExpect(jsonPath("$.resolvedCardNumber").value("4111111111111111"));
        }

        @Test
        @DisplayName("Validation error returns correct HTTP status with phase info")
        void testAddTransaction_ValidationError() throws Exception {
            AddTransactionRequest request = buildValidRequest();

            when(transactionService.addTransaction(any(AddTransactionRequest.class)))
                    .thenThrow(new ValidationException(
                            "Account ID must be Numeric...", 1, "accountId", "BR-AT-02", 400));

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Account ID must be Numeric..."))
                    .andExpect(jsonPath("$.phase").value(1))
                    .andExpect(jsonPath("$.field").value("accountId"))
                    .andExpect(jsonPath("$.businessRule").value("BR-AT-02"));
        }

        @Test
        @DisplayName("Cross-reference not found returns 404")
        void testAddTransaction_XrefNotFound_Returns404() throws Exception {
            AddTransactionRequest request = buildValidRequest();

            when(transactionService.addTransaction(any(AddTransactionRequest.class)))
                    .thenThrow(new ValidationException(
                            "Account ID NOT found...", 1, "accountId", "BR-AT-04", 404));

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Account ID NOT found..."));
        }

        @Test
        @DisplayName("BR-AT-14: Duplicate transaction ID returns 409 Conflict")
        void testAddTransaction_DuplicateId_Returns409() throws Exception {
            AddTransactionRequest request = buildValidRequest();

            when(transactionService.addTransaction(any(AddTransactionRequest.class)))
                    .thenThrow(new DuplicateTransactionException("Tran ID already exist..."));

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Tran ID already exist..."))
                    .andExpect(jsonPath("$.businessRule").value("BR-AT-14"));
        }
    }

    @Nested
    @DisplayName("BR-CF-03: Invalid Key / Endpoint Handling")
    class InvalidEndpointTests {

        @Test
        @DisplayName("Unknown endpoint returns error status")
        void testInvalidEndpoint_ReturnsError() throws Exception {
            mockMvc.perform(get("/api/v1/nonexistent"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 404 || status == 500,
                                "Expected 404 or 500 but got " + status);
                    });
        }

        @Test
        @DisplayName("PATCH method not allowed on transactions returns error")
        void testPatchNotAllowed_ReturnsError() throws Exception {
            mockMvc.perform(patch("/api/v1/transactions/0000000000000001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 405 || status == 500,
                                "Expected 405 or 500 but got " + status);
                    });
        }
    }
}
