package com.carddemo.transaction.controller;

import com.carddemo.transaction.dto.CrossReferenceResponse;
import com.carddemo.transaction.exception.ResourceNotFoundException;
import com.carddemo.transaction.service.CrossReferenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller layer tests for CrossReferenceController.
 * Tests the /api/v1/cross-references/resolve endpoint (BR-AT-04, BR-AT-05).
 */
@WebMvcTest(CrossReferenceController.class)
class CrossReferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CrossReferenceService crossReferenceService;

    @Nested
    @DisplayName("Path A: Account ID -> Card Number")
    class PathAEndpointTests {

        @Test
        @DisplayName("Valid account ID returns 200 with resolved cross-reference")
        void testResolve_ByAccountId_Returns200() throws Exception {
            CrossReferenceResponse response = new CrossReferenceResponse();
            response.setCardNumber("4111111111111111");
            response.setAccountId("00000000001");
            response.setCustomerId(100000001L);

            when(crossReferenceService.resolve("1", null))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/cross-references/resolve")
                            .param("accountId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                    .andExpect(jsonPath("$.accountId").value("00000000001"))
                    .andExpect(jsonPath("$.customerId").value(100000001));
        }

        @Test
        @DisplayName("Account not found returns 404")
        void testResolve_AccountNotFound_Returns404() throws Exception {
            when(crossReferenceService.resolve("99999", null))
                    .thenThrow(new ResourceNotFoundException(
                            "Account ID NOT found...", "accountId", "BR-AT-04"));

            mockMvc.perform(get("/api/v1/cross-references/resolve")
                            .param("accountId", "99999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Account ID NOT found..."));
        }
    }

    @Nested
    @DisplayName("Path B: Card Number -> Account ID")
    class PathBEndpointTests {

        @Test
        @DisplayName("Valid card number returns 200 with resolved cross-reference")
        void testResolve_ByCardNumber_Returns200() throws Exception {
            CrossReferenceResponse response = new CrossReferenceResponse();
            response.setCardNumber("4111111111111111");
            response.setAccountId("00000000001");
            response.setCustomerId(100000001L);

            when(crossReferenceService.resolve(null, "4111111111111111"))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/cross-references/resolve")
                            .param("cardNumber", "4111111111111111"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                    .andExpect(jsonPath("$.accountId").value("00000000001"));
        }

        @Test
        @DisplayName("Card not found returns 404")
        void testResolve_CardNotFound_Returns404() throws Exception {
            when(crossReferenceService.resolve(null, "9999999999999999"))
                    .thenThrow(new ResourceNotFoundException(
                            "Card Number NOT found...", "cardNumber", "BR-AT-04"));

            mockMvc.perform(get("/api/v1/cross-references/resolve")
                            .param("cardNumber", "9999999999999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Card Number NOT found..."));
        }
    }

    @Nested
    @DisplayName("Input Validation at Controller Level")
    class InputValidationEndpointTests {

        @Test
        @DisplayName("Both parameters provided returns 400")
        void testResolve_BothProvided_Returns400() throws Exception {
            when(crossReferenceService.resolve("1", "4111111111111111"))
                    .thenThrow(new IllegalArgumentException(
                            "Provide either accountId or cardNumber, not both"));

            mockMvc.perform(get("/api/v1/cross-references/resolve")
                            .param("accountId", "1")
                            .param("cardNumber", "4111111111111111"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Provide either accountId or cardNumber, not both"));
        }

        @Test
        @DisplayName("No parameters provided returns 400")
        void testResolve_NoParams_Returns400() throws Exception {
            when(crossReferenceService.resolve(null, null))
                    .thenThrow(new IllegalArgumentException(
                            "Either accountId or cardNumber must be provided"));

            mockMvc.perform(get("/api/v1/cross-references/resolve"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Either accountId or cardNumber must be provided"));
        }
    }
}
