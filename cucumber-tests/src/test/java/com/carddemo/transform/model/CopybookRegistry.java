package com.carddemo.transform.model;

import com.carddemo.transform.model.CobolFieldDefinition.PicType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of COBOL copybook field definitions for each entity type.
 * Mirrors the exact layout from CVACT01Y.cpy, CVCUS01Y.cpy, and CVTRA05Y.cpy.
 */
public final class CopybookRegistry {

    private CopybookRegistry() {
        // utility class
    }

    // ── CVACT01Y.cpy  Account Record  RECLN 300 ──────────────────────

    private static final Map<String, CobolFieldDefinition> ACCOUNT_FIELDS;

    static {
        Map<String, CobolFieldDefinition> m = new LinkedHashMap<>();
        m.put("ACCT-ID", new CobolFieldDefinition("ACCT-ID", PicType.NUMERIC_DISPLAY, 11));
        m.put("ACCT-ACTIVE-STATUS", new CobolFieldDefinition("ACCT-ACTIVE-STATUS", PicType.ALPHANUMERIC, 1));
        m.put("ACCT-CURR-BAL", new CobolFieldDefinition("ACCT-CURR-BAL", PicType.SIGNED_DECIMAL, 12, 2));
        m.put("ACCT-CREDIT-LIMIT", new CobolFieldDefinition("ACCT-CREDIT-LIMIT", PicType.SIGNED_DECIMAL, 12, 2));
        m.put("ACCT-CASH-CREDIT-LIMIT", new CobolFieldDefinition("ACCT-CASH-CREDIT-LIMIT", PicType.SIGNED_DECIMAL, 12, 2));
        m.put("ACCT-OPEN-DATE", new CobolFieldDefinition("ACCT-OPEN-DATE", PicType.ALPHANUMERIC, 10));
        m.put("ACCT-EXPIRAION-DATE", new CobolFieldDefinition("ACCT-EXPIRAION-DATE", PicType.ALPHANUMERIC, 10));
        m.put("ACCT-REISSUE-DATE", new CobolFieldDefinition("ACCT-REISSUE-DATE", PicType.ALPHANUMERIC, 10));
        m.put("ACCT-CURR-CYC-CREDIT", new CobolFieldDefinition("ACCT-CURR-CYC-CREDIT", PicType.SIGNED_DECIMAL, 12, 2));
        m.put("ACCT-CURR-CYC-DEBIT", new CobolFieldDefinition("ACCT-CURR-CYC-DEBIT", PicType.SIGNED_DECIMAL, 12, 2));
        m.put("ACCT-ADDR-ZIP", new CobolFieldDefinition("ACCT-ADDR-ZIP", PicType.ALPHANUMERIC, 10));
        m.put("ACCT-GROUP-ID", new CobolFieldDefinition("ACCT-GROUP-ID", PicType.ALPHANUMERIC, 10));
        ACCOUNT_FIELDS = Collections.unmodifiableMap(m);
    }

    public static final int ACCOUNT_RECORD_LENGTH = 300;

    // ── CVCUS01Y.cpy  Customer Record  RECLN 500 ─────────────────────

    private static final Map<String, CobolFieldDefinition> CUSTOMER_FIELDS;

    static {
        Map<String, CobolFieldDefinition> m = new LinkedHashMap<>();
        m.put("CUST-ID", new CobolFieldDefinition("CUST-ID", PicType.NUMERIC_DISPLAY, 9));
        m.put("CUST-FIRST-NAME", new CobolFieldDefinition("CUST-FIRST-NAME", PicType.ALPHANUMERIC, 25));
        m.put("CUST-MIDDLE-NAME", new CobolFieldDefinition("CUST-MIDDLE-NAME", PicType.ALPHANUMERIC, 25));
        m.put("CUST-LAST-NAME", new CobolFieldDefinition("CUST-LAST-NAME", PicType.ALPHANUMERIC, 25));
        m.put("CUST-ADDR-LINE-1", new CobolFieldDefinition("CUST-ADDR-LINE-1", PicType.ALPHANUMERIC, 50));
        m.put("CUST-ADDR-LINE-2", new CobolFieldDefinition("CUST-ADDR-LINE-2", PicType.ALPHANUMERIC, 50));
        m.put("CUST-ADDR-LINE-3", new CobolFieldDefinition("CUST-ADDR-LINE-3", PicType.ALPHANUMERIC, 50));
        m.put("CUST-ADDR-STATE-CD", new CobolFieldDefinition("CUST-ADDR-STATE-CD", PicType.ALPHANUMERIC, 2));
        m.put("CUST-ADDR-COUNTRY-CD", new CobolFieldDefinition("CUST-ADDR-COUNTRY-CD", PicType.ALPHANUMERIC, 3));
        m.put("CUST-ADDR-ZIP", new CobolFieldDefinition("CUST-ADDR-ZIP", PicType.ALPHANUMERIC, 10));
        m.put("CUST-PHONE-NUM-1", new CobolFieldDefinition("CUST-PHONE-NUM-1", PicType.ALPHANUMERIC, 15));
        m.put("CUST-PHONE-NUM-2", new CobolFieldDefinition("CUST-PHONE-NUM-2", PicType.ALPHANUMERIC, 15));
        m.put("CUST-SSN", new CobolFieldDefinition("CUST-SSN", PicType.NUMERIC_DISPLAY, 9));
        m.put("CUST-GOVT-ISSUED-ID", new CobolFieldDefinition("CUST-GOVT-ISSUED-ID", PicType.ALPHANUMERIC, 20));
        m.put("CUST-DOB-YYYY-MM-DD", new CobolFieldDefinition("CUST-DOB-YYYY-MM-DD", PicType.ALPHANUMERIC, 10));
        m.put("CUST-EFT-ACCOUNT-ID", new CobolFieldDefinition("CUST-EFT-ACCOUNT-ID", PicType.ALPHANUMERIC, 10));
        m.put("CUST-PRI-CARD-HOLDER-IND", new CobolFieldDefinition("CUST-PRI-CARD-HOLDER-IND", PicType.ALPHANUMERIC, 1));
        m.put("CUST-FICO-CREDIT-SCORE", new CobolFieldDefinition("CUST-FICO-CREDIT-SCORE", PicType.NUMERIC_DISPLAY, 3));
        CUSTOMER_FIELDS = Collections.unmodifiableMap(m);
    }

    public static final int CUSTOMER_RECORD_LENGTH = 500;

    // ── CVTRA05Y.cpy  Transaction Record  RECLN 350 ──────────────────

    private static final Map<String, CobolFieldDefinition> TRANSACTION_FIELDS;

    static {
        Map<String, CobolFieldDefinition> m = new LinkedHashMap<>();
        m.put("TRAN-ID", new CobolFieldDefinition("TRAN-ID", PicType.ALPHANUMERIC, 16));
        m.put("TRAN-TYPE-CD", new CobolFieldDefinition("TRAN-TYPE-CD", PicType.ALPHANUMERIC, 2));
        m.put("TRAN-CAT-CD", new CobolFieldDefinition("TRAN-CAT-CD", PicType.NUMERIC_DISPLAY, 4));
        m.put("TRAN-SOURCE", new CobolFieldDefinition("TRAN-SOURCE", PicType.ALPHANUMERIC, 10));
        m.put("TRAN-DESC", new CobolFieldDefinition("TRAN-DESC", PicType.ALPHANUMERIC, 100));
        m.put("TRAN-AMT", new CobolFieldDefinition("TRAN-AMT", PicType.SIGNED_DECIMAL, 11, 2));
        m.put("TRAN-MERCHANT-ID", new CobolFieldDefinition("TRAN-MERCHANT-ID", PicType.NUMERIC_DISPLAY, 9));
        m.put("TRAN-MERCHANT-NAME", new CobolFieldDefinition("TRAN-MERCHANT-NAME", PicType.ALPHANUMERIC, 50));
        m.put("TRAN-MERCHANT-CITY", new CobolFieldDefinition("TRAN-MERCHANT-CITY", PicType.ALPHANUMERIC, 50));
        m.put("TRAN-MERCHANT-ZIP", new CobolFieldDefinition("TRAN-MERCHANT-ZIP", PicType.ALPHANUMERIC, 10));
        m.put("TRAN-CARD-NUM", new CobolFieldDefinition("TRAN-CARD-NUM", PicType.ALPHANUMERIC, 16));
        m.put("TRAN-ORIG-TS", new CobolFieldDefinition("TRAN-ORIG-TS", PicType.ALPHANUMERIC, 26));
        m.put("TRAN-PROC-TS", new CobolFieldDefinition("TRAN-PROC-TS", PicType.ALPHANUMERIC, 26));
        TRANSACTION_FIELDS = Collections.unmodifiableMap(m);
    }

    public static final int TRANSACTION_RECORD_LENGTH = 350;

    // ── Accessors ─────────────────────────────────────────────────────

    public static Map<String, CobolFieldDefinition> getAccountFields() {
        return ACCOUNT_FIELDS;
    }

    public static Map<String, CobolFieldDefinition> getCustomerFields() {
        return CUSTOMER_FIELDS;
    }

    public static Map<String, CobolFieldDefinition> getTransactionFields() {
        return TRANSACTION_FIELDS;
    }
}
