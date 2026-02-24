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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for {@link CardUpdateService}, encoding the business rules
 * extracted from COCRDUPC.cbl (Credit Card Update).
 *
 * <p>Each test documents which COBOL paragraph and line range it validates.</p>
 */
class CardUpdateServiceTest {

    private CardUpdateService service;

    @BeforeEach
    void setUp() {
        service = new CardUpdateService();
    }

    // =========================================================================
    // Test 1: Valid account ID passes validation
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: Valid 11-digit numeric account ID passes "
            + "(1210-EDIT-ACCOUNT, lines 721-756)")
    void testValidAccountId() {
        CardUpdateService.ValidationResult result =
                service.validateAccountId("12345678901");
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    // =========================================================================
    // Test 2: Invalid account IDs - blank, non-numeric, wrong length
    // =========================================================================
    @Nested
    @DisplayName("COCRDUPC: Account ID validation edge cases "
            + "(1210-EDIT-ACCOUNT, lines 721-756)")
    class AccountIdValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "00000000000"})
        @DisplayName("Blank/null/all-zeros account ID rejected")
        void testBlankAccountIdRejected(String accountId) {
            CardUpdateService.ValidationResult result =
                    service.validateAccountId(accountId);
            assertFalse(result.isValid());
            assertNotNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Non-numeric account ID rejected")
        void testNonNumericAccountIdRejected() {
            // COBOL: IF CC-ACCT-ID IS NOT NUMERIC
            CardUpdateService.ValidationResult result =
                    service.validateAccountId("1234567890A");
            assertFalse(result.isValid());
            assertEquals("Account number must be a non zero 11 digit number",
                    result.getErrorMessage());
        }

        @Test
        @DisplayName("Account ID with wrong length rejected")
        void testWrongLengthAccountIdRejected() {
            CardUpdateService.ValidationResult result =
                    service.validateAccountId("12345");
            assertFalse(result.isValid());
        }
    }

    // =========================================================================
    // Test 3: Card number validation
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: Valid 16-digit numeric card number passes "
            + "(1220-EDIT-CARD, lines 762-800)")
    void testValidCardNumber() {
        CardUpdateService.ValidationResult result =
                service.validateCardNumber("4111111111111111");
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("COCRDUPC: Non-numeric card number rejected "
            + "(1220-EDIT-CARD, lines 784-794)")
    void testNonNumericCardNumberRejected() {
        CardUpdateService.ValidationResult result =
                service.validateCardNumber("411111111111111X");
        assertFalse(result.isValid());
        assertEquals("Card number if supplied must be a 16 digit number",
                result.getErrorMessage());
    }

    // =========================================================================
    // Test 4: Card name validation - alphabets and spaces only
    // =========================================================================
    @Nested
    @DisplayName("COCRDUPC: Card name validation "
            + "(1230-EDIT-NAME, lines 806-843)")
    class CardNameValidation {

        @Test
        @DisplayName("Valid alphabetic name with spaces passes")
        void testValidCardName() {
            // COBOL: INSPECT CONVERTING all-alpha TO all-spaces, then TRIM check
            CardUpdateService.ValidationResult result =
                    service.validateCardName("John Doe Smith");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Name with digits rejected")
        void testNameWithDigitsRejected() {
            CardUpdateService.ValidationResult result =
                    service.validateCardName("John123");
            assertFalse(result.isValid());
            assertEquals("Card name can only contain alphabets and spaces",
                    result.getErrorMessage());
        }

        @Test
        @DisplayName("Name with special characters rejected")
        void testNameWithSpecialCharsRejected() {
            CardUpdateService.ValidationResult result =
                    service.validateCardName("O'Brien");
            assertFalse(result.isValid());
            assertEquals("Card name can only contain alphabets and spaces",
                    result.getErrorMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Blank/null name rejected")
        void testBlankNameRejected(String name) {
            CardUpdateService.ValidationResult result =
                    service.validateCardName(name);
            assertFalse(result.isValid());
            assertEquals("Card name not provided", result.getErrorMessage());
        }
    }

    // =========================================================================
    // Test 5: Card status validation - must be Y or N
    // =========================================================================
    @Nested
    @DisplayName("COCRDUPC: Card active status validation "
            + "(1240-EDIT-CARDSTATUS, lines 845-876)")
    class CardStatusValidation {

        @ParameterizedTest
        @ValueSource(strings = {"Y", "N"})
        @DisplayName("Y and N are the only valid statuses")
        void testValidStatuses(String status) {
            // COBOL: 88 FLG-YES-NO-VALID VALUES 'Y', 'N'.
            CardUpdateService.ValidationResult result =
                    service.validateCardStatus(status);
            assertTrue(result.isValid());
        }

        @ParameterizedTest
        @ValueSource(strings = {"y", "n", "X", "1", "YES", "NO"})
        @DisplayName("Lowercase, other chars, and multi-char values rejected")
        void testInvalidStatuses(String status) {
            // Note: COBOL FLG-YES-NO-CHECK is PIC X(1) VALUES 'Y', 'N'
            // lowercase y/n do NOT match the 88-level condition
            CardUpdateService.ValidationResult result =
                    service.validateCardStatus(status);
            assertFalse(result.isValid());
            assertEquals("Card Active Status must be Y or N",
                    result.getErrorMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Blank/null status rejected")
        void testBlankStatusRejected(String status) {
            CardUpdateService.ValidationResult result =
                    service.validateCardStatus(status);
            assertFalse(result.isValid());
        }
    }

    // =========================================================================
    // Test 6: Expiry month boundary validation (1-12)
    // =========================================================================
    @Nested
    @DisplayName("COCRDUPC: Expiry month validation "
            + "(1250-EDIT-EXPIRY-MON, lines 877-912)")
    class ExpiryMonthValidation {

        @Test
        @DisplayName("Month 1 (January) is valid - lower boundary")
        void testMonth1Valid() {
            // COBOL: 88 VALID-MONTH VALUES 1 THRU 12.
            assertTrue(service.validateExpiryMonth("01").isValid());
        }

        @Test
        @DisplayName("Month 12 (December) is valid - upper boundary")
        void testMonth12Valid() {
            assertTrue(service.validateExpiryMonth("12").isValid());
        }

        @Test
        @DisplayName("Month 0 rejected - below lower boundary")
        void testMonth0Rejected() {
            // COBOL: CCUP-NEW-EXPMON EQUAL ZEROS -> rejected before range check
            CardUpdateService.ValidationResult result =
                    service.validateExpiryMonth("00");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Month 13 rejected - above upper boundary")
        void testMonth13Rejected() {
            CardUpdateService.ValidationResult result =
                    service.validateExpiryMonth("13");
            assertFalse(result.isValid());
            assertEquals("Card expiry month must be between 1 and 12",
                    result.getErrorMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Blank/null month rejected")
        void testBlankMonthRejected(String month) {
            assertFalse(service.validateExpiryMonth(month).isValid());
        }
    }

    // =========================================================================
    // Test 7: Expiry year boundary validation (1950-2099)
    // =========================================================================
    @Nested
    @DisplayName("COCRDUPC: Expiry year validation "
            + "(1260-EDIT-EXPIRY-YEAR, lines 913-947)")
    class ExpiryYearValidation {

        @Test
        @DisplayName("Year 1950 is valid - lower boundary")
        void testYear1950Valid() {
            // COBOL: 88 VALID-YEAR VALUES 1950 THRU 2099.
            assertTrue(service.validateExpiryYear("1950").isValid());
        }

        @Test
        @DisplayName("Year 2099 is valid - upper boundary")
        void testYear2099Valid() {
            assertTrue(service.validateExpiryYear("2099").isValid());
        }

        @Test
        @DisplayName("Year 1949 rejected - below lower boundary")
        void testYear1949Rejected() {
            CardUpdateService.ValidationResult result =
                    service.validateExpiryYear("1949");
            assertFalse(result.isValid());
            assertEquals("Invalid card expiry year", result.getErrorMessage());
        }

        @Test
        @DisplayName("Year 2100 rejected - above upper boundary")
        void testYear2100Rejected() {
            CardUpdateService.ValidationResult result =
                    service.validateExpiryYear("2100");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Year 0000 (all zeros) rejected")
        void testYear0000Rejected() {
            // COBOL: CCUP-NEW-EXPYEAR EQUAL ZEROS -> rejected
            assertFalse(service.validateExpiryYear("0000").isValid());
        }
    }

    // =========================================================================
    // Test 8: Input normalization - asterisk treated as blank
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: Asterisk '*' input normalized to null "
            + "(COBOL lines 589-635)")
    void testAsteriskNormalizedToNull() {
        // COBOL: IF field = '*' OR SPACES -> MOVE LOW-VALUES TO field
        assertNull(service.normalizeInput("*"));
        assertNull(service.normalizeInput("   "));
        assertNull(service.normalizeInput(null));
        assertEquals("JOHN DOE", service.normalizeInput("JOHN DOE"));
    }

    // =========================================================================
    // Test 9: Change detection - case-insensitive comparison
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: No changes detected when data differs only by case "
            + "(COBOL lines 680-683)")
    void testNoChangeDetectedCaseInsensitive() {
        // COBOL: IF FUNCTION UPPER-CASE(NEW) EQUAL FUNCTION UPPER-CASE(OLD)
        CardRecord oldCard = new CardRecord(
                "4111111111111111", "12345678901", "123",
                "JOHN DOE", "2025-06-15", "Y");

        boolean changed = service.hasChanges(oldCard,
                "john doe", "2025", "06", "15", "Y");
        assertFalse(changed, "Case-only differences should not be detected as changes");
    }

    @Test
    @DisplayName("COCRDUPC: Changes detected when status differs")
    void testChangeDetectedWhenStatusDiffers() {
        CardRecord oldCard = new CardRecord(
                "4111111111111111", "12345678901", "123",
                "JOHN DOE", "2025-06-15", "Y");

        boolean changed = service.hasChanges(oldCard,
                "JOHN DOE", "2025", "06", "15", "N");
        assertTrue(changed, "Status change should be detected");
    }

    // =========================================================================
    // Test 10: Optimistic locking - concurrent modification detection
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: Optimistic locking detects concurrent modification "
            + "(9300-CHECK-CHANGE-IN-REC, lines 1498-1519)")
    void testOptimisticLockingDetectsConcurrentChange() {
        // COBOL: Compares CVV, name, expiry, status fields
        CardRecord originalCard = new CardRecord(
                "4111111111111111", "12345678901", "123",
                "JOHN DOE", "2025-06-15", "Y");

        // Another user changed the status from Y to N
        CardRecord currentDbCard = new CardRecord(
                "4111111111111111", "12345678901", "123",
                "JOHN DOE", "2025-06-15", "N");

        assertTrue(service.isRecordChangedByAnotherUser(originalCard, currentDbCard),
                "Status change by another user should be detected");
    }

    @Test
    @DisplayName("COCRDUPC: Optimistic locking passes when record unchanged")
    void testOptimisticLockingPassesWhenUnchanged() {
        CardRecord originalCard = new CardRecord(
                "4111111111111111", "12345678901", "123",
                "JOHN DOE", "2025-06-15", "Y");

        CardRecord currentDbCard = new CardRecord(
                "4111111111111111", "12345678901", "123",
                "JOHN DOE", "2025-06-15", "Y");

        assertFalse(service.isRecordChangedByAnotherUser(originalCard, currentDbCard),
                "Identical records should not trigger conflict");
    }

    // =========================================================================
    // Test 11: Expiration date formatting
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: Expiration date built as YYYY-MM-DD "
            + "(COBOL lines 1467-1474)")
    void testExpirationDateFormatting() {
        // COBOL: STRING year '-' month '-' day DELIMITED BY SIZE INTO date
        String date = service.buildExpirationDate("2025", "06", "15");
        assertEquals("2025-06-15", date);
    }

    // =========================================================================
    // Test 12: Full card field validation - all fields valid
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: All card fields valid passes full validation "
            + "(edit paragraphs 1230-1260)")
    void testAllFieldsValidPassesFullValidation() {
        CardUpdateService.ValidationResult result = service.validateAllCardFields(
                "John Doe", "Y", "06", "2025");
        assertTrue(result.isValid());
    }

    // =========================================================================
    // Test 13: Full card field validation - first error returned
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: Full validation returns first error encountered")
    void testFullValidationReturnsFirstError() {
        // Name is invalid (has digits), status is also invalid
        // Should return name error first since COBOL checks name before status
        CardUpdateService.ValidationResult result = service.validateAllCardFields(
                "John123", "X", "13", "1900");
        assertFalse(result.isValid());
        assertEquals("Card name can only contain alphabets and spaces",
                result.getErrorMessage());
    }

    // =========================================================================
    // Test 14: Optimistic locking - CVV change detected
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: Optimistic locking detects CVV change "
            + "(9300-CHECK-CHANGE-IN-REC, lines 1503)")
    void testOptimisticLockingDetectsCvvChange() {
        CardRecord originalCard = new CardRecord(
                "4111111111111111", "12345678901", "123",
                "JOHN DOE", "2025-06-15", "Y");

        CardRecord currentDbCard = new CardRecord(
                "4111111111111111", "12345678901", "456",
                "JOHN DOE", "2025-06-15", "Y");

        assertTrue(service.isRecordChangedByAnotherUser(originalCard, currentDbCard),
                "CVV change by another user should be detected");
    }

    // =========================================================================
    // Test 15: Update state enum covers all COBOL states
    // =========================================================================
    @Test
    @DisplayName("COCRDUPC: UpdateState enum covers all COBOL CCUP-CHANGE-ACTION values "
            + "(lines 276-290)")
    void testUpdateStateEnumCompleteness() {
        // Verify all 7 states from COBOL are represented
        CardUpdateService.UpdateState[] states = CardUpdateService.UpdateState.values();
        assertEquals(7, states.length, "Must have exactly 7 states matching COBOL");
        assertNotNull(CardUpdateService.UpdateState.NOT_FETCHED);
        assertNotNull(CardUpdateService.UpdateState.SHOW_DETAILS);
        assertNotNull(CardUpdateService.UpdateState.CHANGES_NOT_OK);
        assertNotNull(CardUpdateService.UpdateState.CHANGES_OK_NOT_CONFIRMED);
        assertNotNull(CardUpdateService.UpdateState.CHANGES_OKAYED_AND_DONE);
        assertNotNull(CardUpdateService.UpdateState.CHANGES_OKAYED_LOCK_ERROR);
        assertNotNull(CardUpdateService.UpdateState.CHANGES_OKAYED_BUT_FAILED);
    }
}
