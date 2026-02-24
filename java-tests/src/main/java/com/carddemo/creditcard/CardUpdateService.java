package com.carddemo.creditcard;

/**
 * Java equivalent of COCRDUPC.cbl - Credit Card Update processing.
 *
 * <h2>Business Rules from COBOL (COCRDUPC.cbl)</h2>
 *
 * <h3>Validation Rules</h3>
 * <ul>
 *   <li><b>Account ID (1210-EDIT-ACCOUNT):</b> Must be numeric, 11 digits, non-zero.
 *       Blank/spaces/low-values/zeros rejected with "Account number not provided".</li>
 *   <li><b>Card Number (1220-EDIT-CARD):</b> Must be numeric, 16 digits, non-zero.
 *       Blank/spaces/low-values/zeros rejected with "Card number not provided".</li>
 *   <li><b>Card Name (1230-EDIT-NAME):</b> Must not be blank; must contain only
 *       alphabets (A-Z, a-z) and spaces. Non-alpha characters are rejected.</li>
 *   <li><b>Card Status (1240-EDIT-CARDSTATUS):</b> Must be 'Y' or 'N' only.
 *       Blank/spaces/zeros and other values rejected.</li>
 *   <li><b>Expiry Month (1250-EDIT-EXPIRY-MON):</b> Must be numeric, 1-12.
 *       Blank/spaces/zeros rejected.</li>
 *   <li><b>Expiry Year (1260-EDIT-EXPIRY-YEAR):</b> Must be numeric, 1950-2099.
 *       Blank/spaces/zeros rejected.</li>
 * </ul>
 *
 * <h3>State Machine (CCUP-CHANGE-ACTION)</h3>
 * <ul>
 *   <li><b>NOT_FETCHED (LOW-VALUES/SPACES):</b> No card details loaded yet.</li>
 *   <li><b>SHOW_DETAILS ('S'):</b> Card details displayed to user.</li>
 *   <li><b>CHANGES_NOT_OK ('E'):</b> User edited but validation failed.</li>
 *   <li><b>CHANGES_OK_NOT_CONFIRMED ('N'):</b> Edits validated, awaiting F5 confirm.</li>
 *   <li><b>CHANGES_OKAYED_AND_DONE ('C'):</b> Update committed to database.</li>
 *   <li><b>CHANGES_OKAYED_LOCK_ERROR ('L'):</b> Could not lock record for update.</li>
 *   <li><b>CHANGES_OKAYED_BUT_FAILED ('F'):</b> Lock acquired but rewrite failed.</li>
 * </ul>
 *
 * <h3>Optimistic Locking (9300-CHECK-CHANGE-IN-REC)</h3>
 * <p>Before writing, the system re-reads the record and compares CVV, embossed name,
 * expiry year/month/day, and active status against the originally fetched values.
 * If any field differs, the update is aborted with
 * "Record changed by some one else. Please review".</p>
 *
 * <h3>Name Normalization</h3>
 * <p>On read, the embossed name is converted to uppercase using
 * INSPECT CONVERTING lowercase TO uppercase (COBOL lines 1356-1358).
 * Change detection uses FUNCTION UPPER-CASE for case-insensitive comparison
 * (COBOL lines 680-681).</p>
 *
 * <h3>Edge Cases</h3>
 * <ul>
 *   <li>Asterisk ('*') input treated as blank/empty (lines 589-635).</li>
 *   <li>No changes detected: "No change detected" message, no write performed.</li>
 *   <li>Cross-field: both account and card blank = "No input received".</li>
 *   <li>Card not found: "Did not find cards for this search condition".</li>
 *   <li>Record lock failure: "Could not lock record for update".</li>
 *   <li>Rewrite failure after lock: "Update of record failed".</li>
 * </ul>
 */
public class CardUpdateService {

    /** Minimum valid expiry year per COBOL line 99: VALUES 1950 THRU 2099. */
    public static final int MIN_EXPIRY_YEAR = 1950;

    /** Maximum valid expiry year per COBOL line 99: VALUES 1950 THRU 2099. */
    public static final int MAX_EXPIRY_YEAR = 2099;

    /** Minimum valid expiry month per COBOL line 95: VALUES 1 THRU 12. */
    public static final int MIN_EXPIRY_MONTH = 1;

    /** Maximum valid expiry month per COBOL line 95: VALUES 1 THRU 12. */
    public static final int MAX_EXPIRY_MONTH = 12;

    /**
     * Represents the state of a card update workflow.
     * Maps to CCUP-CHANGE-ACTION in COBOL (lines 276-290).
     */
    public enum UpdateState {
        /** No card details fetched yet. COBOL: LOW-VALUES/SPACES. */
        NOT_FETCHED,
        /** Card details shown to user. COBOL: 'S'. */
        SHOW_DETAILS,
        /** Changes made but validation failed. COBOL: 'E'. */
        CHANGES_NOT_OK,
        /** Changes validated, awaiting confirmation. COBOL: 'N'. */
        CHANGES_OK_NOT_CONFIRMED,
        /** Changes confirmed and committed. COBOL: 'C'. */
        CHANGES_OKAYED_AND_DONE,
        /** Could not lock record for update. COBOL: 'L'. */
        CHANGES_OKAYED_LOCK_ERROR,
        /** Lock acquired but rewrite failed. COBOL: 'F'. */
        CHANGES_OKAYED_BUT_FAILED
    }

    /**
     * Encapsulates the result of a validation or update operation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Validates an account ID input.
     * <p>
     * COBOL reference (1210-EDIT-ACCOUNT, lines 721-756):
     * Must be non-blank, numeric, and represent a non-zero 11-digit number.
     *
     * @param accountId the account ID string
     * @return validation result
     */
    public ValidationResult validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank() || isAllZeros(accountId)) {
            return ValidationResult.error("Account number not provided");
        }
        if (!accountId.matches("\\d{11}")) {
            return ValidationResult.error(
                    "Account number must be a non zero 11 digit number");
        }
        if (Long.parseLong(accountId) == 0) {
            return ValidationResult.error(
                    "Account number must be a non zero 11 digit number");
        }
        return ValidationResult.ok();
    }

    /**
     * Validates a card number input.
     * <p>
     * COBOL reference (1220-EDIT-CARD, lines 762-800):
     * Must be non-blank, numeric, and represent a non-zero 16-digit number.
     *
     * @param cardNumber the card number string
     * @return validation result
     */
    public ValidationResult validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank() || isAllZeros(cardNumber)) {
            return ValidationResult.error("Card number not provided");
        }
        if (!cardNumber.matches("\\d{16}")) {
            return ValidationResult.error(
                    "Card number if supplied must be a 16 digit number");
        }
        if (Long.parseUnsignedLong(cardNumber) == 0) {
            return ValidationResult.error("Card number not provided");
        }
        return ValidationResult.ok();
    }

    /**
     * Validates the card embossed name.
     * <p>
     * COBOL reference (1230-EDIT-NAME, lines 806-843):
     * Must be non-blank and contain only alphabets (A-Z, a-z) and spaces.
     * The COBOL uses INSPECT CONVERTING all-alpha TO all-spaces, then checks
     * if the trimmed result is empty (meaning all characters were alphabetic).
     *
     * @param cardName the embossed name string
     * @return validation result
     */
    public ValidationResult validateCardName(String cardName) {
        if (cardName == null || cardName.isBlank()) {
            return ValidationResult.error("Card name not provided");
        }
        // Per COBOL logic: replace all alpha chars with spaces, then check if empty
        String stripped = cardName.replaceAll("[A-Za-z ]", "");
        if (!stripped.isEmpty()) {
            return ValidationResult.error(
                    "Card name can only contain alphabets and spaces");
        }
        return ValidationResult.ok();
    }

    /**
     * Validates the card active status.
     * <p>
     * COBOL reference (1240-EDIT-CARDSTATUS, lines 845-876):
     * Must be 'Y' or 'N'. Blank, spaces, zeros, and any other value rejected.
     *
     * @param status the status character
     * @return validation result
     */
    public ValidationResult validateCardStatus(String status) {
        if (status == null || status.isBlank()) {
            return ValidationResult.error("Card Active Status must be Y or N");
        }
        if (status.equals("Y") || status.equals("N")) {
            return ValidationResult.ok();
        }
        return ValidationResult.error("Card Active Status must be Y or N");
    }

    /**
     * Validates the card expiry month.
     * <p>
     * COBOL reference (1250-EDIT-EXPIRY-MON, lines 877-912):
     * Must be numeric, between 1 and 12 inclusive. Blank, spaces, zeros rejected.
     *
     * @param monthStr the month string (e.g., "01" to "12")
     * @return validation result
     */
    public ValidationResult validateExpiryMonth(String monthStr) {
        if (monthStr == null || monthStr.isBlank() || isAllZeros(monthStr)) {
            return ValidationResult.error(
                    "Card expiry month must be between 1 and 12");
        }
        try {
            int month = Integer.parseInt(monthStr.trim());
            if (month >= MIN_EXPIRY_MONTH && month <= MAX_EXPIRY_MONTH) {
                return ValidationResult.ok();
            }
        } catch (NumberFormatException ignored) {
            // Fall through to error
        }
        return ValidationResult.error(
                "Card expiry month must be between 1 and 12");
    }

    /**
     * Validates the card expiry year.
     * <p>
     * COBOL reference (1260-EDIT-EXPIRY-YEAR, lines 913-947):
     * Must be numeric, between 1950 and 2099 inclusive. Blank, spaces, zeros rejected.
     *
     * @param yearStr the year string (e.g., "2025")
     * @return validation result
     */
    public ValidationResult validateExpiryYear(String yearStr) {
        if (yearStr == null || yearStr.isBlank() || isAllZeros(yearStr)) {
            return ValidationResult.error("Invalid card expiry year");
        }
        try {
            int year = Integer.parseInt(yearStr.trim());
            if (year >= MIN_EXPIRY_YEAR && year <= MAX_EXPIRY_YEAR) {
                return ValidationResult.ok();
            }
        } catch (NumberFormatException ignored) {
            // Fall through to error
        }
        return ValidationResult.error("Invalid card expiry year");
    }

    /**
     * Normalizes input by treating '*' and spaces as blank, per COBOL lines 589-635.
     *
     * @param input the raw screen input
     * @return null if effectively blank, otherwise the original input
     */
    public String normalizeInput(String input) {
        if (input == null) {
            return null;
        }
        if (input.equals("*") || input.isBlank()) {
            return null;
        }
        return input;
    }

    /**
     * Detects whether the card data has changed (case-insensitive comparison).
     * <p>
     * COBOL reference (lines 680-683):
     * <pre>
     *   IF (FUNCTION UPPER-CASE(CCUP-NEW-CARDDATA) EQUAL
     *       FUNCTION UPPER-CASE(CCUP-OLD-CARDDATA))
     *       SET NO-CHANGES-DETECTED TO TRUE
     * </pre>
     * The "card data" compared includes: embossed name, expiry year, expiry month,
     * expiry day, and active status.
     *
     * @param oldCard the original card record
     * @param newName new embossed name
     * @param newExpiryYear new expiry year
     * @param newExpiryMonth new expiry month
     * @param newExpiryDay new expiry day
     * @param newStatus new active status
     * @return true if any data differs
     */
    public boolean hasChanges(CardRecord oldCard, String newName, String newExpiryYear,
                              String newExpiryMonth, String newExpiryDay,
                              String newStatus) {
        String oldData = buildCardDataString(
                oldCard.getEmbossedName(),
                oldCard.getExpiryYear(),
                oldCard.getExpiryMonth(),
                oldCard.getExpiryDay(),
                oldCard.getActiveStatus());

        String newData = buildCardDataString(
                newName, newExpiryYear, newExpiryMonth, newExpiryDay, newStatus);

        return !oldData.equalsIgnoreCase(newData);
    }

    /**
     * Checks for optimistic locking conflicts by comparing original card data
     * against the current database state.
     * <p>
     * COBOL reference (9300-CHECK-CHANGE-IN-REC, lines 1498-1519):
     * Compares CVV, embossed name (uppercased), expiry year/month/day, and status.
     *
     * @param originalCard the card data originally fetched
     * @param currentDbCard the card data currently in the database
     * @return true if the record was modified by another user
     */
    public boolean isRecordChangedByAnotherUser(CardRecord originalCard,
                                                 CardRecord currentDbCard) {
        // Per COBOL: name is uppercased before comparison
        String origName = originalCard.getEmbossedName() != null
                ? originalCard.getEmbossedName().toUpperCase() : "";
        String dbName = currentDbCard.getEmbossedName() != null
                ? currentDbCard.getEmbossedName().toUpperCase() : "";

        if (!nullSafeEquals(originalCard.getCvvCode(), currentDbCard.getCvvCode())) {
            return true;
        }
        if (!origName.equals(dbName)) {
            return true;
        }
        if (!nullSafeEquals(originalCard.getExpiryYear(),
                currentDbCard.getExpiryYear())) {
            return true;
        }
        if (!nullSafeEquals(originalCard.getExpiryMonth(),
                currentDbCard.getExpiryMonth())) {
            return true;
        }
        if (!nullSafeEquals(originalCard.getExpiryDay(),
                currentDbCard.getExpiryDay())) {
            return true;
        }
        if (!nullSafeEquals(originalCard.getActiveStatus(),
                currentDbCard.getActiveStatus())) {
            return true;
        }
        return false;
    }

    /**
     * Builds the expiration date in YYYY-MM-DD format from components.
     * <p>
     * COBOL reference (lines 1467-1474):
     * <pre>
     *   STRING CCUP-NEW-EXPYEAR '-' CCUP-NEW-EXPMON '-' CCUP-NEW-EXPDAY
     *          DELIMITED BY SIZE INTO CARD-UPDATE-EXPIRAION-DATE
     * </pre>
     *
     * @param year  the 4-digit year
     * @param month the 2-digit month
     * @param day   the 2-digit day
     * @return the formatted date string
     */
    public String buildExpirationDate(String year, String month, String day) {
        return year + "-" + month + "-" + day;
    }

    /**
     * Performs full validation of all editable card fields.
     * <p>
     * COBOL reference: calls 1230 through 1260 edit paragraphs sequentially.
     *
     * @param cardName    the embossed name
     * @param cardStatus  the active status
     * @param expiryMonth the expiry month
     * @param expiryYear  the expiry year
     * @return the first validation error, or ok if all pass
     */
    public ValidationResult validateAllCardFields(String cardName, String cardStatus,
                                                   String expiryMonth,
                                                   String expiryYear) {
        ValidationResult nameResult = validateCardName(cardName);
        if (!nameResult.isValid()) {
            return nameResult;
        }

        ValidationResult statusResult = validateCardStatus(cardStatus);
        if (!statusResult.isValid()) {
            return statusResult;
        }

        ValidationResult monthResult = validateExpiryMonth(expiryMonth);
        if (!monthResult.isValid()) {
            return monthResult;
        }

        ValidationResult yearResult = validateExpiryYear(expiryYear);
        if (!yearResult.isValid()) {
            return yearResult;
        }

        return ValidationResult.ok();
    }

    private String buildCardDataString(String name, String year, String month,
                                       String day, String status) {
        return safeStr(name) + safeStr(year) + safeStr(month)
                + safeStr(day) + safeStr(status);
    }

    private String safeStr(String s) {
        return s == null ? "" : s;
    }

    private boolean nullSafeEquals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private boolean isAllZeros(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (char c : s.toCharArray()) {
            if (c != '0') {
                return false;
            }
        }
        return true;
    }
}
