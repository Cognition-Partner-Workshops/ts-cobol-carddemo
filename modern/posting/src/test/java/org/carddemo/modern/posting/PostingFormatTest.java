package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PostingFormatTest {
    @Test void limitUsesSignedCycleAmountsAndExpirationOverridesLimitReason() {
        var account = new java.util.HashMap<String, Object>();
        account.put("ACCT-CURR-BAL", new java.math.BigDecimal("9999"));
        account.put("ACCT-CREDIT-LIMIT", new java.math.BigDecimal("100"));
        account.put("ACCT-CURR-CYC-CREDIT", new java.math.BigDecimal("70"));
        account.put("ACCT-CURR-CYC-DEBIT", new java.math.BigDecimal("-20"));
        account.put("ACCT-EXPIRAION-DATE", "2022-07-05");
        var daily = new java.util.HashMap<String, Object>();
        daily.put("DALYTRAN-AMT", new java.math.BigDecimal("10")); daily.put("DALYTRAN-ORIG-TS", "2022-07-05 10:00:00.000000");
        assertEquals(0, PostingRun.validate(daily, account, true)); // equality is accepted, ignoring current balance
        daily.put("DALYTRAN-AMT", new java.math.BigDecimal("10.01"));
        assertEquals(102, PostingRun.validate(daily, account, true));
        daily.put("DALYTRAN-ORIG-TS", "2022-07-06 00:00:00.000000");
        assertEquals(103, PostingRun.validate(daily, account, true));
        assertEquals(101, PostingRun.validate(daily, null, true));
        assertEquals(100, PostingRun.validate(daily, account, false));
    }
    @Test void numericEditedPicturesHaveFixedSignsAndWidthFifteen() {
        assertEquals("-        200.00", PostingReport.edited(new java.math.BigDecimal("-200"), false));
        assertEquals("          42.00", PostingReport.edited(new java.math.BigDecimal("42"), false));
        assertEquals("+         84.00", PostingReport.edited(new java.math.BigDecimal("84"), true));
        assertEquals("+           .00", PostingReport.edited(java.math.BigDecimal.ZERO, true));
        assertEquals("-999,999,999.99", PostingReport.edited(new java.math.BigDecimal("-999999999.99"), true));
    }
    @Test void frozenClockUsesTheCobolDb2SeparatorAndSixFractionalDigits() {
        assertEquals("2022-07-05-10.00.00.000000", PostingRun.timestamp("2022-07-05 10:00:00.00"));
        assertEquals("2025-12-31-23.59.58.120000", PostingRun.timestamp("2025-12-31 23:59:58.12"));
    }
}
