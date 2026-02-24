package com.carddemo.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for {@link BillPaymentService}, encoding the business rules
 * extracted from COBIL00C.cbl (Bill Payment).
 *
 * <p>Each test documents which COBOL paragraph and line range it validates.</p>
 */
class BillPaymentServiceTest {

    private BillPaymentService service;

    @BeforeEach
    void setUp() {
        service = new BillPaymentService();
    }

    // =========================================================================
    // Test 1: Normal flow - successful full-balance payment
    // =========================================================================
    @Test
    @DisplayName("COBIL00C: Successful payment zeroes out the account balance "
            + "(COBOL lines 218-234)")
    void testSuccessfulPaymentZeroesBalance() {
        // COBOL rule: COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT
        // where TRAN-AMT = ACCT-CURR-BAL, so new balance = 0
        AccountRecord account = new AccountRecord();
        account.setAccountId("12345678901");
        account.setCurrentBalance(new BigDecimal("1500.50"));

        BillPaymentService.BillPaymentResult result = service.processPayment(
                account, "4111111111111111", "0000000000000100",
                "2024-01-15 10:30:00.000000");

        assertTrue(result.isSuccess(), "Payment should succeed for positive balance");
        assertNotNull(result.getTransaction());
        assertEquals(BigDecimal.ZERO, result.getNewBalance().stripTrailingZeros(),
                "New balance must be zero after full payment");
        assertEquals(new BigDecimal("1500.50"),
                result.getTransaction().getTransactionAmount(),
                "Transaction amount must equal the original balance");
    }

    // =========================================================================
    // Test 2: Transaction record fields are populated correctly
    // =========================================================================
    @Test
    @DisplayName("COBIL00C: Transaction record fields match COBOL constants "
            + "(COBOL lines 220-229)")
    void testTransactionRecordFieldsMatchCobolConstants() {
        // COBOL: TRAN-TYPE-CD = '02', TRAN-CAT-CD = 2, TRAN-SOURCE = 'POS TERM',
        // TRAN-DESC = 'BILL PAYMENT - ONLINE', TRAN-MERCHANT-ID = 999999999,
        // TRAN-MERCHANT-NAME = 'BILL PAYMENT', TRAN-MERCHANT-CITY = 'N/A',
        // TRAN-MERCHANT-ZIP = 'N/A'
        AccountRecord account = new AccountRecord();
        account.setAccountId("12345678901");
        account.setCurrentBalance(new BigDecimal("250.00"));

        String timestamp = "2024-06-15 14:22:00.000000";
        BillPaymentService.BillPaymentResult result = service.processPayment(
                account, "4111111111111111", "0000000000000050", timestamp);

        assertTrue(result.isSuccess());
        TransactionRecord txn = result.getTransaction();

        assertEquals("02", txn.getTransactionTypeCode());
        assertEquals(2, txn.getTransactionCategoryCode());
        assertEquals("POS TERM", txn.getTransactionSource());
        assertEquals("BILL PAYMENT - ONLINE", txn.getTransactionDescription());
        assertEquals(999999999L, txn.getMerchantId());
        assertEquals("BILL PAYMENT", txn.getMerchantName());
        assertEquals("N/A", txn.getMerchantCity());
        assertEquals("N/A", txn.getMerchantZip());
        assertEquals("4111111111111111", txn.getCardNumber());
        assertEquals("0000000000000050", txn.getTransactionId());
        assertEquals(timestamp, txn.getOriginTimestamp());
        assertEquals(timestamp, txn.getProcessTimestamp());
    }

    // =========================================================================
    // Test 3: Empty account ID rejected
    // =========================================================================
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("COBIL00C: Empty/blank account ID produces error "
            + "(COBOL lines 159-167)")
    void testEmptyAccountIdRejected(String accountId) {
        // COBOL: WHEN ACTIDINI = SPACES OR LOW-VALUES -> error
        AccountRecord account = new AccountRecord();
        account.setAccountId(accountId);
        account.setCurrentBalance(new BigDecimal("100.00"));

        BillPaymentService.BillPaymentResult result = service.processPayment(
                account, "4111111111111111", "1", "2024-01-01 00:00:00.000000");

        assertFalse(result.isSuccess());
        assertEquals("Acct ID can NOT be empty...", result.getErrorMessage());
    }

    // =========================================================================
    // Test 4: Zero balance - nothing to pay
    // =========================================================================
    @Test
    @DisplayName("COBIL00C: Zero balance produces 'nothing to pay' error "
            + "(COBOL lines 198-205)")
    void testZeroBalanceRejected() {
        // COBOL: IF ACCT-CURR-BAL <= ZEROS -> 'You have nothing to pay...'
        AccountRecord account = new AccountRecord();
        account.setAccountId("12345678901");
        account.setCurrentBalance(BigDecimal.ZERO);

        BillPaymentService.BillPaymentResult result = service.processPayment(
                account, "4111111111111111", "1", "2024-01-01 00:00:00.000000");

        assertFalse(result.isSuccess());
        assertEquals("You have nothing to pay...", result.getErrorMessage());
    }

    // =========================================================================
    // Test 5: Negative balance - nothing to pay
    // =========================================================================
    @Test
    @DisplayName("COBIL00C: Negative balance (credit) produces 'nothing to pay' error "
            + "(COBOL lines 198-205)")
    void testNegativeBalanceRejected() {
        // COBOL: IF ACCT-CURR-BAL <= ZEROS (covers negative values too)
        AccountRecord account = new AccountRecord();
        account.setAccountId("12345678901");
        account.setCurrentBalance(new BigDecimal("-50.00"));

        BillPaymentService.BillPaymentResult result = service.processPayment(
                account, "4111111111111111", "1", "2024-01-01 00:00:00.000000");

        assertFalse(result.isSuccess());
        assertEquals("You have nothing to pay...", result.getErrorMessage());
    }

    // =========================================================================
    // Test 6: Confirmation flag validation
    // =========================================================================
    @Nested
    @DisplayName("COBIL00C: Confirmation flag validation (COBOL lines 173-191)")
    class ConfirmationValidation {

        @ParameterizedTest
        @ValueSource(strings = {"Y", "y"})
        @DisplayName("Y/y is a valid confirmation that means 'proceed'")
        void testValidConfirmationYes(String input) {
            assertNull(service.validateConfirmation(input));
            assertTrue(service.isPaymentConfirmed(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"N", "n"})
        @DisplayName("N/n is a valid confirmation that means 'cancel'")
        void testValidConfirmationNo(String input) {
            assertNull(service.validateConfirmation(input));
            assertFalse(service.isPaymentConfirmed(input));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Blank/null is valid - means 'show details without paying'")
        void testBlankConfirmationShowsDetails(String input) {
            assertNull(service.validateConfirmation(input));
            assertFalse(service.isPaymentConfirmed(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"X", "1", "yes", "no", "true"})
        @DisplayName("Invalid confirmation values produce error")
        void testInvalidConfirmation(String input) {
            String error = service.validateConfirmation(input);
            assertNotNull(error);
            assertEquals("Invalid value. Valid values are (Y/N)...", error);
        }
    }

    // =========================================================================
    // Test 7: Transaction ID generation
    // =========================================================================
    @Test
    @DisplayName("COBIL00C: Transaction ID is max existing + 1 "
            + "(COBOL lines 216-217)")
    void testTransactionIdIncrement() {
        // COBOL: MOVE TRAN-ID TO WS-TRAN-ID-NUM; ADD 1 TO WS-TRAN-ID-NUM
        assertEquals("100", service.generateNextTransactionId("99"));
        assertEquals("1", service.generateNextTransactionId("0"));
        assertEquals("1000000000000001",
                service.generateNextTransactionId("1000000000000000"));
    }

    @Test
    @DisplayName("COBIL00C: Transaction ID defaults to 1 for empty/invalid input")
    void testTransactionIdDefaultsToOne() {
        assertEquals("1", service.generateNextTransactionId(null));
        assertEquals("1", service.generateNextTransactionId(""));
        assertEquals("1", service.generateNextTransactionId("  "));
        assertEquals("1", service.generateNextTransactionId("abc"));
    }

    // =========================================================================
    // Test 8: Large balance boundary (COBOL PIC S9(10)V99 max = 9999999999.99)
    // =========================================================================
    @Test
    @DisplayName("COBIL00C: Large balance at COBOL field maximum boundary")
    void testLargeBalanceBoundary() {
        // COBOL PIC S9(10)V99 allows up to 9999999999.99
        AccountRecord account = new AccountRecord();
        account.setAccountId("99999999999");
        account.setCurrentBalance(new BigDecimal("9999999999.99"));

        BillPaymentService.BillPaymentResult result = service.processPayment(
                account, "9999999999999999", "9999999999999999",
                "2024-12-31 23:59:59.000000");

        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.ZERO, result.getNewBalance().stripTrailingZeros());
        assertEquals(new BigDecimal("9999999999.99"),
                result.getTransaction().getTransactionAmount());
    }

    // =========================================================================
    // Test 9: Smallest positive balance (0.01 penny)
    // =========================================================================
    @Test
    @DisplayName("COBIL00C: Smallest positive balance (1 cent) is payable")
    void testSmallestPositiveBalance() {
        // Boundary: smallest balance > 0 that passes the ACCT-CURR-BAL > ZEROS check
        AccountRecord account = new AccountRecord();
        account.setAccountId("00000000001");
        account.setCurrentBalance(new BigDecimal("0.01"));

        BillPaymentService.BillPaymentResult result = service.processPayment(
                account, "4111111111111111", "1", "2024-01-01 00:00:00.000000");

        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.ZERO, result.getNewBalance().stripTrailingZeros());
        assertEquals(new BigDecimal("0.01"),
                result.getTransaction().getTransactionAmount());
    }
}
