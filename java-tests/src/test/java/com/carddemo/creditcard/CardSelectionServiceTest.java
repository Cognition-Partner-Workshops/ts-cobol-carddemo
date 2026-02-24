package com.carddemo.creditcard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for {@link CardSelectionService}, encoding the business rules
 * extracted from COCRDSLC.cbl (Credit Card Selection/Detail).
 *
 * <p>Each test documents which COBOL paragraph and line range it validates.</p>
 */
class CardSelectionServiceTest {

    private CardSelectionService service;

    @BeforeEach
    void setUp() {
        service = new CardSelectionService();
    }

    // =========================================================================
    // Test 1: Valid search criteria passes
    // =========================================================================
    @Test
    @DisplayName("COCRDSLC: Valid 11-digit account + 16-digit card passes "
            + "(2210/2220-EDIT, lines 647-724)")
    void testValidSearchCriteriaPasses() {
        CardSelectionService.ValidationResult result =
                service.validateSearchCriteria("12345678901", "4111111111111111");
        assertTrue(result.isValid());
    }

    // =========================================================================
    // Test 2: Both fields blank - "No input received"
    // =========================================================================
    @Test
    @DisplayName("COCRDSLC: Both account and card blank produces "
            + "'No input received' (COBOL lines 637-640)")
    void testBothFieldsBlankProducesNoInput() {
        // COBOL: IF FLG-ACCTFILTER-BLANK AND FLG-CARDFILTER-BLANK
        //            SET NO-SEARCH-CRITERIA-RECEIVED TO TRUE
        CardSelectionService.ValidationResult result =
                service.validateSearchCriteria(null, null);
        assertFalse(result.isValid());
        assertEquals("No input received", result.getErrorMessage());
    }

    @Test
    @DisplayName("COCRDSLC: Both fields all zeros treated as blank")
    void testBothFieldsAllZerosTreatedAsBlank() {
        CardSelectionService.ValidationResult result =
                service.validateSearchCriteria("00000000000", "0000000000000000");
        assertFalse(result.isValid());
        assertEquals("No input received", result.getErrorMessage());
    }

    // =========================================================================
    // Test 3: Account ID validation - non-numeric rejected
    // =========================================================================
    @Nested
    @DisplayName("COCRDSLC: Account ID validation "
            + "(2210-EDIT-ACCOUNT, lines 647-683)")
    class AccountIdValidation {

        @Test
        @DisplayName("Non-numeric account ID rejected")
        void testNonNumericAccountRejected() {
            // COBOL: IF CC-ACCT-ID IS NOT NUMERIC
            CardSelectionService.ValidationResult result =
                    service.validateAccountId("1234567890A");
            assertFalse(result.isValid());
            assertEquals(
                    "ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER",
                    result.getErrorMessage());
        }

        @Test
        @DisplayName("Account ID shorter than 11 digits rejected")
        void testShortAccountRejected() {
            CardSelectionService.ValidationResult result =
                    service.validateAccountId("12345");
            assertFalse(result.isValid());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "00000000000"})
        @DisplayName("Blank/null/all-zeros account ID rejected")
        void testBlankAccountRejected(String accountId) {
            CardSelectionService.ValidationResult result =
                    service.validateAccountId(accountId);
            assertFalse(result.isValid());
            assertEquals("Account number not provided",
                    result.getErrorMessage());
        }
    }

    // =========================================================================
    // Test 4: Card number validation - non-numeric rejected
    // =========================================================================
    @Nested
    @DisplayName("COCRDSLC: Card number validation "
            + "(2220-EDIT-CARD, lines 685-724)")
    class CardNumberValidation {

        @Test
        @DisplayName("Non-numeric card number rejected")
        void testNonNumericCardRejected() {
            CardSelectionService.ValidationResult result =
                    service.validateCardNumber("411111111111111X");
            assertFalse(result.isValid());
            assertEquals(
                    "CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER",
                    result.getErrorMessage());
        }

        @Test
        @DisplayName("Card number shorter than 16 digits rejected")
        void testShortCardRejected() {
            CardSelectionService.ValidationResult result =
                    service.validateCardNumber("41111111");
            assertFalse(result.isValid());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "0000000000000000"})
        @DisplayName("Blank/null/all-zeros card number rejected")
        void testBlankCardRejected(String cardNumber) {
            CardSelectionService.ValidationResult result =
                    service.validateCardNumber(cardNumber);
            assertFalse(result.isValid());
            assertEquals("Card number not provided",
                    result.getErrorMessage());
        }
    }

    // =========================================================================
    // Test 5: Input normalization - asterisk and spaces
    // =========================================================================
    @Test
    @DisplayName("COCRDSLC: Asterisk '*' and spaces normalized to null "
            + "(COBOL lines 614-627)")
    void testInputNormalization() {
        // COBOL: IF field = '*' OR SPACES -> MOVE LOW-VALUES TO field
        assertNull(service.normalizeInput("*"));
        assertNull(service.normalizeInput("   "));
        assertNull(service.normalizeInput(null));
        assertEquals("12345678901", service.normalizeInput("12345678901"));
    }

    // =========================================================================
    // Test 6: Search fields protected when coming from list screen
    // =========================================================================
    @Test
    @DisplayName("COCRDSLC: Fields are protected when coming from COCRDLIC "
            + "(1300-SETUP-SCREEN-ATTRS, lines 505-512)")
    void testFieldsProtectedFromListScreen() {
        // COBOL: IF CDEMO-LAST-MAPSET EQUAL LIT-CCLISTMAPSET
        //        AND CDEMO-FROM-PROGRAM EQUAL LIT-CCLISTPGM
        //            MOVE DFHBMPRF TO fields
        assertTrue(service.areSearchFieldsProtected("COCRDLI", "COCRDLIC"));
    }

    @Test
    @DisplayName("COCRDSLC: Fields are editable when not from list screen")
    void testFieldsEditableFromOtherContext() {
        assertFalse(service.areSearchFieldsProtected("COMEN01", "COMEN01C"));
        assertFalse(service.areSearchFieldsProtected(null, null));
        assertFalse(service.areSearchFieldsProtected("COCRDLI", "COMEN01C"));
    }

    // =========================================================================
    // Test 7: Valid account ID at exact boundary (11 digits, non-zero)
    // =========================================================================
    @Test
    @DisplayName("COCRDSLC: Account ID at max boundary 99999999999 passes")
    void testMaxAccountIdPasses() {
        CardSelectionService.ValidationResult result =
                service.validateAccountId("99999999999");
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("COCRDSLC: Account ID at min boundary 00000000001 passes")
    void testMinAccountIdPasses() {
        CardSelectionService.ValidationResult result =
                service.validateAccountId("00000000001");
        assertTrue(result.isValid());
    }

    // =========================================================================
    // Test 8: Card number at exact boundary
    // =========================================================================
    @Test
    @DisplayName("COCRDSLC: Card number at max boundary 9999999999999999 passes")
    void testMaxCardNumberPasses() {
        CardSelectionService.ValidationResult result =
                service.validateCardNumber("9999999999999999");
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("COCRDSLC: Card number at min boundary 0000000000000001 passes")
    void testMinCardNumberPasses() {
        CardSelectionService.ValidationResult result =
                service.validateCardNumber("0000000000000001");
        assertTrue(result.isValid());
    }
}
