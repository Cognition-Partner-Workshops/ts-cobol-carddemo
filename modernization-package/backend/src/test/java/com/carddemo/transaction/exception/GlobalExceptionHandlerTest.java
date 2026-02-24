package com.carddemo.transaction.exception;

import com.carddemo.transaction.dto.ErrorResponse;
import com.carddemo.transaction.dto.ValidationErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler.
 * Verifies that each exception type is mapped to the correct HTTP status and response body.
 * Covers error message catalog from BRE Section 8.2.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("ValidationException Handling")
    class ValidationExceptionTests {

        @Test
        @DisplayName("400 validation error maps correctly with phase info")
        void testValidationException_400() {
            ValidationException ex = new ValidationException(
                    "Account ID must be Numeric...", 1, "accountId", "BR-AT-02", 400);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidationException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Account ID must be Numeric...", response.getBody().getMessage());
            assertEquals("Validation Failed", response.getBody().getError());
            assertEquals("accountId", response.getBody().getField());
            assertEquals("BR-AT-02", response.getBody().getBusinessRule());
            assertEquals(1, response.getBody().getPhase());
        }

        @Test
        @DisplayName("404 validation error maps correctly")
        void testValidationException_404() {
            ValidationException ex = new ValidationException(
                    "Account ID NOT found...", 1, "accountId", "BR-AT-04", 404);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidationException(ex);

            assertEquals(404, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Not Found", response.getBody().getError());
            assertEquals("Account ID NOT found...", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Phase 2 mandatory field empty error")
        void testValidationException_Phase2() {
            ValidationException ex = new ValidationException(
                    "Type CD can NOT be empty...", 2, "typeCode", "BR-AT-06", 400);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidationException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().getPhase());
            assertEquals("Type CD can NOT be empty...", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Phase 3 numeric check error")
        void testValidationException_Phase3() {
            ValidationException ex = new ValidationException(
                    "Type CD must be Numeric...", 3, "typeCode", "BR-AT-07", 400);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidationException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(3, response.getBody().getPhase());
        }

        @Test
        @DisplayName("Phase 4 amount format error")
        void testValidationException_Phase4() {
            ValidationException ex = new ValidationException(
                    "Amount should be in format -99999999.99", 4, "amount", "BR-AT-08", 400);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidationException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(4, response.getBody().getPhase());
        }

        @Test
        @DisplayName("Phase 5 date format error")
        void testValidationException_Phase5_DateFormat() {
            ValidationException ex = new ValidationException(
                    "Orig Date - Date format must be YYYY-MM-DD...", 5, "originationDate", "BR-AT-09", 400);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidationException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(5, response.getBody().getPhase());
            assertEquals("Orig Date - Date format must be YYYY-MM-DD...", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Phase 5 date validity error")
        void testValidationException_Phase5_DateValidity() {
            ValidationException ex = new ValidationException(
                    "Orig Date - Not a valid date...", 5, "originationDate", "BR-AT-10", 400);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidationException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Orig Date - Not a valid date...", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Phase 6 merchant ID numeric error")
        void testValidationException_Phase6() {
            ValidationException ex = new ValidationException(
                    "Merchant ID must be Numeric...", 6, "merchantId", "BR-AT-11", 400);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidationException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(6, response.getBody().getPhase());
            assertEquals("Merchant ID must be Numeric...", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("ResourceNotFoundException Handling")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("Empty transaction ID error")
        void testResourceNotFound_EmptyId() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "Tran ID can NOT be empty...", "transactionId", "BR-VT-01");

            ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex);

            assertEquals(404, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Not Found", response.getBody().getError());
            assertEquals("Tran ID can NOT be empty...", response.getBody().getMessage());
            assertEquals("transactionId", response.getBody().getField());
            assertEquals("BR-VT-01", response.getBody().getBusinessRule());
        }

        @Test
        @DisplayName("Transaction not found error")
        void testResourceNotFound_TransactionNotFound() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "Transaction ID NOT found...", "transactionId", "BR-VT-01");

            ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex);

            assertEquals(404, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Transaction ID NOT found...", response.getBody().getMessage());
        }

        @Test
        @DisplayName("No transactions found error (latest)")
        void testResourceNotFound_NoTransactions() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "No transactions found", "transactionId", null);

            ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex);

            assertEquals(404, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("No transactions found", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("DuplicateTransactionException Handling")
    class DuplicateTests {

        @Test
        @DisplayName("BR-AT-14: Duplicate ID returns 409 with exact message")
        void testDuplicateTransaction() {
            DuplicateTransactionException ex = new DuplicateTransactionException(
                    "Tran ID already exist...");

            ResponseEntity<ErrorResponse> response = handler.handleDuplicateTransactionException(ex);

            assertEquals(409, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Conflict", response.getBody().getError());
            assertEquals("Tran ID already exist...", response.getBody().getMessage());
            assertEquals("transactionId", response.getBody().getField());
            assertEquals("BR-AT-14", response.getBody().getBusinessRule());
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException Handling")
    class IllegalArgumentTests {

        @Test
        @DisplayName("Non-numeric filter error")
        void testIllegalArgument_NonNumericFilter() {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "Tran ID must be Numeric ...");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Bad Request", response.getBody().getError());
            assertEquals("Tran ID must be Numeric ...", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Both params provided error")
        void testIllegalArgument_BothParams() {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "Provide either accountId or cardNumber, not both");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex);

            assertEquals(400, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Provide either accountId or cardNumber, not both",
                    response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Generic Exception Handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("Unexpected error returns 500")
        void testGenericException() {
            Exception ex = new RuntimeException("Something went wrong");

            ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

            assertEquals(500, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Internal Server Error", response.getBody().getError());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        @DisplayName("File/lookup error returns 500")
        void testGenericException_FileError() {
            Exception ex = new RuntimeException("Unable to lookup transaction...");

            ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

            assertEquals(500, response.getStatusCode().value());
        }
    }
}
