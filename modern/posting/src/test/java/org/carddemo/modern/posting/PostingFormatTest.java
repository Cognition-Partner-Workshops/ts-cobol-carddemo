package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PostingFormatTest {
    @Test void frozenClockUsesTheCobolDb2SeparatorAndSixFractionalDigits() {
        assertEquals("2022-07-05-10.00.00.000000", PostingRun.timestamp("2022-07-05 10:00:00.00"));
        assertEquals("2025-12-31-23.59.58.120000", PostingRun.timestamp("2025-12-31 23:59:58.12"));
    }
}
