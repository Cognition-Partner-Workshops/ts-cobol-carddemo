package com.carddemo.creditcard;

/**
 * Java equivalent of COCRDSLC.cbl - Credit Card Selection/Detail view.
 *
 * <h2>Business Rules from COBOL (COCRDSLC.cbl)</h2>
 *
 * <h3>Validation Rules</h3>
 * <ul>
 *   <li><b>Account ID (2210-EDIT-ACCOUNT):</b> Must be numeric, 11 digits, non-zero.
 *       Blank/spaces/zeros rejected with "Account number not provided".</li>
 *   <li><b>Card Number (2220-EDIT-CARD):</b> Must be numeric, 16 digits, non-zero.
 *       Blank/spaces/zeros rejected with "Card number not provided".</li>
 *   <li><b>Cross-field validation:</b> If BOTH account and card are blank,
 *       error is "No input received" (lines 637-640).</li>
 * </ul>
 *
 * <h3>Decision Logic / Screen Flow</h3>
 * <ul>
 *   <li><b>PF03:</b> Exit to calling program or main menu (lines 305-334).</li>
 *   <li><b>Coming from Credit Card List (COCRDLIC):</b> Input already validated;
 *       directly read data and display (lines 339-348).</li>
 *   <li><b>First entry (PGM-ENTER):</b> Show blank search form (lines 349-356).</li>
 *   <li><b>Re-entry (PGM-REENTER):</b> Validate inputs, then read and display
 *       (lines 357-371).</li>
 * </ul>
 *
 * <h3>Card Lookup</h3>
 * <ul>
 *   <li><b>By card number (primary key):</b> 9100-GETCARD-BYACCTCARD reads CARDDAT
 *       file using card number as RIDFLD (lines 736-773).</li>
 *   <li><b>By account ID (alternate index):</b> 9150-GETCARD-BYACCT reads CARDAIX
 *       file using account ID (lines 779-812).</li>
 * </ul>
 *
 * <h3>Input Normalization</h3>
 * <ul>
 *   <li>Asterisk ('*') and spaces are treated as blank/empty (lines 614-627).</li>
 * </ul>
 *
 * <h3>Screen Attribute Logic</h3>
 * <ul>
 *   <li>When coming from the list screen (COCRDLIC), account and card fields are
 *       protected (read-only) with DFHBMPRF (lines 505-512).</li>
 *   <li>Otherwise, fields are unprotected (editable) with DFHBMFSE.</li>
 *   <li>Invalid fields are highlighted in red (DFHRED).</li>
 *   <li>Blank fields on re-entry show asterisk ('*') in red.</li>
 * </ul>
 *
 * <h3>Edge Cases</h3>
 * <ul>
 *   <li>Account not found via alternate index: "Did not find this account in cards
 *       database" (line 799).</li>
 *   <li>Card not found by card number: "Did not find cards for this search condition"
 *       (line 760).</li>
 *   <li>File read error: Generic error with operation name, file name, RESP/RESP2
 *       codes (lines 767-771).</li>
 *   <li>Unexpected data scenario: Abend with code '0001' (lines 373-381).</li>
 *   <li>No EIBCALEN (first invocation): Initialize all areas (line 268).</li>
 * </ul>
 */
public class CardSelectionService {

    /**
     * Encapsulates a validation result.
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
     * Validates an account ID for the selection/search screen.
     * <p>
     * COBOL reference (2210-EDIT-ACCOUNT, lines 647-683):
     * Must be non-blank, numeric, 11 digits, and non-zero.
     *
     * @param accountId the account ID string
     * @return validation result
     */
    public ValidationResult validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank() || isAllZeros(accountId)) {
            return ValidationResult.error("Account number not provided");
        }
        if (!accountId.matches("\\d+")) {
            return ValidationResult.error(
                    "ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER");
        }
        if (accountId.length() != 11) {
            return ValidationResult.error(
                    "ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER");
        }
        return ValidationResult.ok();
    }

    /**
     * Validates a card number for the selection/search screen.
     * <p>
     * COBOL reference (2220-EDIT-CARD, lines 685-724):
     * Must be non-blank, numeric, 16 digits, and non-zero.
     *
     * @param cardNumber the card number string
     * @return validation result
     */
    public ValidationResult validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank() || isAllZeros(cardNumber)) {
            return ValidationResult.error("Card number not provided");
        }
        if (!cardNumber.matches("\\d+")) {
            return ValidationResult.error(
                    "CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER");
        }
        if (cardNumber.length() != 16) {
            return ValidationResult.error(
                    "CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER");
        }
        return ValidationResult.ok();
    }

    /**
     * Performs cross-field validation: both fields blank = no search criteria.
     * <p>
     * COBOL reference (lines 637-640):
     * <pre>
     *   IF FLG-ACCTFILTER-BLANK AND FLG-CARDFILTER-BLANK
     *       SET NO-SEARCH-CRITERIA-RECEIVED TO TRUE
     * </pre>
     *
     * @param accountId  the account ID (may be null/blank)
     * @param cardNumber the card number (may be null/blank)
     * @return validation result
     */
    public ValidationResult validateSearchCriteria(String accountId, String cardNumber) {
        boolean acctBlank = accountId == null || accountId.isBlank()
                || isAllZeros(accountId);
        boolean cardBlank = cardNumber == null || cardNumber.isBlank()
                || isAllZeros(cardNumber);

        if (acctBlank && cardBlank) {
            return ValidationResult.error("No input received");
        }

        // Validate individual fields
        ValidationResult acctResult = validateAccountId(accountId);
        if (!acctResult.isValid()) {
            return acctResult;
        }

        ValidationResult cardResult = validateCardNumber(cardNumber);
        if (!cardResult.isValid()) {
            return cardResult;
        }

        return ValidationResult.ok();
    }

    /**
     * Normalizes input by treating '*' and spaces as blank.
     * <p>
     * COBOL reference (lines 614-627): Asterisk and spaces map to LOW-VALUES.
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
     * Determines whether the search fields should be protected (read-only)
     * based on the navigation context.
     * <p>
     * COBOL reference (1300-SETUP-SCREEN-ATTRS, lines 505-512):
     * If coming from COCRDLIC (credit card list screen), fields are protected.
     *
     * @param lastMapset    the last mapset used
     * @param fromProgram   the program that invoked this one
     * @return true if the search fields should be read-only
     */
    public boolean areSearchFieldsProtected(String lastMapset, String fromProgram) {
        return "COCRDLI".equals(lastMapset) && "COCRDLIC".equals(fromProgram);
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
