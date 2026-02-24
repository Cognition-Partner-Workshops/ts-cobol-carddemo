package com.carddemo.billing;

import java.math.BigDecimal;

/**
 * Java equivalent of COBIL00C.cbl - Bill Payment processing.
 *
 * <h2>Business Rules from COBOL (COBIL00C.cbl)</h2>
 *
 * <h3>Calculation Formulas</h3>
 * <ul>
 *   <li><b>Payment Amount:</b> Always the full current balance (ACCT-CURR-BAL).
 *       There is no partial payment option; TRAN-AMT = ACCT-CURR-BAL.</li>
 *   <li><b>New Balance:</b> COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT.
 *       Since TRAN-AMT equals the full balance, the result is always zero.</li>
 *   <li><b>Transaction ID:</b> Highest existing TRAN-ID + 1 (auto-increment).</li>
 * </ul>
 *
 * <h3>Decision Logic</h3>
 * <ul>
 *   <li>Account ID must not be empty/spaces/low-values.</li>
 *   <li>Account must exist in the ACCTDAT file.</li>
 *   <li>Current balance must be greater than zero (nothing to pay otherwise).</li>
 *   <li>Confirmation flag must be 'Y' or 'y' to proceed with payment.</li>
 *   <li>'N' or 'n' cancels and clears the screen.</li>
 *   <li>Any other confirmation value is rejected as invalid.</li>
 *   <li>Blank/spaces confirmation shows the account details without paying.</li>
 * </ul>
 *
 * <h3>Transaction Record Fields</h3>
 * <ul>
 *   <li>TRAN-TYPE-CD = '02' (bill payment type)</li>
 *   <li>TRAN-CAT-CD = 2</li>
 *   <li>TRAN-SOURCE = 'POS TERM'</li>
 *   <li>TRAN-DESC = 'BILL PAYMENT - ONLINE'</li>
 *   <li>TRAN-MERCHANT-ID = 999999999</li>
 *   <li>TRAN-MERCHANT-NAME = 'BILL PAYMENT'</li>
 *   <li>TRAN-MERCHANT-CITY = 'N/A'</li>
 *   <li>TRAN-MERCHANT-ZIP = 'N/A'</li>
 * </ul>
 *
 * <h3>Edge Cases</h3>
 * <ul>
 *   <li>Zero or negative balance: "You have nothing to pay" error.</li>
 *   <li>Empty account ID: Error before any file lookup.</li>
 *   <li>Account not found in file: "Account ID NOT found" error.</li>
 *   <li>Cross-reference (CXACAIX) not found: Same "Account ID NOT found" error.</li>
 *   <li>Duplicate transaction ID on write: "Tran ID already exist" error.</li>
 *   <li>File I/O errors: Generic error messages with RESP/RESP2 codes.</li>
 * </ul>
 */
public class BillPaymentService {

    /** Transaction type code for bill payments, per COBOL line 220. */
    public static final String TRAN_TYPE_BILL_PAYMENT = "02";

    /** Transaction category code for bill payments, per COBOL line 221. */
    public static final int TRAN_CATEGORY_BILL_PAYMENT = 2;

    /** Fixed transaction source, per COBOL line 222. */
    public static final String TRAN_SOURCE = "POS TERM";

    /** Fixed transaction description, per COBOL line 223. */
    public static final String TRAN_DESCRIPTION = "BILL PAYMENT - ONLINE";

    /** Fixed merchant ID for bill pay transactions, per COBOL line 226. */
    public static final long MERCHANT_ID = 999999999L;

    /** Fixed merchant name, per COBOL line 227. */
    public static final String MERCHANT_NAME = "BILL PAYMENT";

    /** Fixed merchant city, per COBOL line 228. */
    public static final String MERCHANT_CITY = "N/A";

    /** Fixed merchant zip, per COBOL line 229. */
    public static final String MERCHANT_ZIP = "N/A";

    /**
     * Result object encapsulating the outcome of a bill payment operation.
     */
    public static class BillPaymentResult {
        private final boolean success;
        private final String errorMessage;
        private final TransactionRecord transaction;
        private final BigDecimal newBalance;

        private BillPaymentResult(boolean success, String errorMessage,
                                  TransactionRecord transaction, BigDecimal newBalance) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.transaction = transaction;
            this.newBalance = newBalance;
        }

        public static BillPaymentResult error(String message) {
            return new BillPaymentResult(false, message, null, null);
        }

        public static BillPaymentResult success(TransactionRecord transaction,
                                                 BigDecimal newBalance) {
            return new BillPaymentResult(true, null, transaction, newBalance);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public TransactionRecord getTransaction() {
            return transaction;
        }

        public BigDecimal getNewBalance() {
            return newBalance;
        }
    }

    /**
     * Validates an account ID per COBOL rules.
     * <p>
     * COBOL reference (lines 159-167): Account ID cannot be spaces or low-values.
     *
     * @param accountId the account ID to validate
     * @return error message, or null if valid
     */
    public String validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "Acct ID can NOT be empty...";
        }
        return null;
    }

    /**
     * Validates the confirmation input per COBOL rules.
     * <p>
     * COBOL reference (lines 173-191): Only Y/y/N/n/spaces are valid.
     *
     * @param confirmInput the user's confirmation input
     * @return error message, or null if valid
     */
    public String validateConfirmation(String confirmInput) {
        if (confirmInput == null || confirmInput.isBlank()) {
            return null; // Blank is valid - means "show details without paying"
        }
        String trimmed = confirmInput.trim();
        if (trimmed.equals("Y") || trimmed.equals("y")
                || trimmed.equals("N") || trimmed.equals("n")) {
            return null;
        }
        return "Invalid value. Valid values are (Y/N)...";
    }

    /**
     * Checks whether the confirmation input means "proceed with payment".
     * <p>
     * COBOL reference (lines 174-176): Y or y means confirmed.
     *
     * @param confirmInput the user's confirmation input
     * @return true if confirmed
     */
    public boolean isPaymentConfirmed(String confirmInput) {
        if (confirmInput == null) {
            return false;
        }
        String trimmed = confirmInput.trim();
        return trimmed.equals("Y") || trimmed.equals("y");
    }

    /**
     * Validates whether the account balance allows payment.
     * <p>
     * COBOL reference (lines 198-205): Balance must be &gt; 0.
     *
     * @param currentBalance the account's current balance
     * @return error message, or null if payable
     */
    public String validateBalance(BigDecimal currentBalance) {
        if (currentBalance == null || currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return "You have nothing to pay...";
        }
        return null;
    }

    /**
     * Processes a bill payment, applying the full-balance-pay-off formula.
     * <p>
     * COBOL reference (lines 218-234):
     * <pre>
     *   MOVE ACCT-CURR-BAL TO TRAN-AMT
     *   COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT
     * </pre>
     *
     * @param account        the account record
     * @param cardNumber     the card number from the cross-reference
     * @param nextTransId    the next available transaction ID
     * @param timestamp      the current timestamp
     * @return the result of the payment operation
     */
    public BillPaymentResult processPayment(AccountRecord account, String cardNumber,
                                            String nextTransId, String timestamp) {
        // Validate account ID
        String accountIdError = validateAccountId(account.getAccountId());
        if (accountIdError != null) {
            return BillPaymentResult.error(accountIdError);
        }

        // Validate balance
        String balanceError = validateBalance(account.getCurrentBalance());
        if (balanceError != null) {
            return BillPaymentResult.error(balanceError);
        }

        // Build the transaction record per COBOL lines 218-233
        TransactionRecord transaction = new TransactionRecord();
        transaction.setTransactionId(nextTransId);
        transaction.setTransactionTypeCode(TRAN_TYPE_BILL_PAYMENT);
        transaction.setTransactionCategoryCode(TRAN_CATEGORY_BILL_PAYMENT);
        transaction.setTransactionSource(TRAN_SOURCE);
        transaction.setTransactionDescription(TRAN_DESCRIPTION);
        transaction.setTransactionAmount(account.getCurrentBalance());
        transaction.setCardNumber(cardNumber);
        transaction.setMerchantId(MERCHANT_ID);
        transaction.setMerchantName(MERCHANT_NAME);
        transaction.setMerchantCity(MERCHANT_CITY);
        transaction.setMerchantZip(MERCHANT_ZIP);
        transaction.setOriginTimestamp(timestamp);
        transaction.setProcessTimestamp(timestamp);

        // Apply the balance formula: new_balance = current_balance - tran_amt
        // COBOL line 234: COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT
        BigDecimal newBalance = account.getCurrentBalance()
                .subtract(transaction.getTransactionAmount());

        return BillPaymentResult.success(transaction, newBalance);
    }

    /**
     * Generates the next transaction ID by incrementing the maximum existing ID.
     * <p>
     * COBOL reference (lines 216-217):
     * <pre>
     *   MOVE TRAN-ID TO WS-TRAN-ID-NUM
     *   ADD 1 TO WS-TRAN-ID-NUM
     * </pre>
     *
     * @param currentMaxTransId the current highest transaction ID (numeric string)
     * @return the next transaction ID as a string
     */
    public String generateNextTransactionId(String currentMaxTransId) {
        if (currentMaxTransId == null || currentMaxTransId.isBlank()) {
            return "1";
        }
        try {
            long currentMax = Long.parseLong(currentMaxTransId.trim());
            return String.valueOf(currentMax + 1);
        } catch (NumberFormatException e) {
            return "1";
        }
    }
}
