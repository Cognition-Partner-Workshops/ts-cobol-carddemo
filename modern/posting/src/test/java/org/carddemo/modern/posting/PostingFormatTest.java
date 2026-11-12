package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PostingFormatTest {
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
