package org.carddemo.modern.codec;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostingLayoutsTest {
    @Test void rejectTrailerKeepsEveryNamedFieldAtTheCopybookOffset() {
        for (int reason = 100; reason <= 103; reason++) {
            String daily = Layouts.DALYTRAN.encode(Map.of("DALYTRAN-ID", "transaction-1", "DALYTRAN-AMT", new BigDecimal("-25.50")));
            String line = new RejectRecord(daily, reason, "reason text").toLine();
            assertEquals(daily, line.substring(0, 350));
            assertEquals("0" + reason, line.substring(350, 354));
            var decoded = Layouts.REJTRAN.decode(line);
            assertEquals(BigDecimal.valueOf(reason), decoded.get("REJ-FAIL-REASON"));
            assertEquals(new BigDecimal("-25.50"), decoded.get("DALYTRAN-AMT"));
            assertEquals("reason text", decoded.get("REJ-FAIL-REASON-DESC").toString().stripTrailing());
        }
        assertEquals(430, Layouts.REJTRAN.length());
    }
    @Test void referenceAndDateLayoutsMatchCopybookLengthsAndOffsets() {
        assertEquals(60, PostingLayouts.TYPE.length()); assertEquals(60, PostingLayouts.CATEGORY.length());
        String type = PostingLayouts.TYPE.encode(Map.of("TRAN-TYPE", "01", "TRAN-TYPE-DESC", "Purchase"));
        assertEquals("01Purchase", type.substring(0, 10));
        String category = PostingLayouts.CATEGORY.encode(Map.of("TRAN-TYPE-CD", "02", "TRAN-CAT-CD", 12, "TRAN-CAT-TYPE-DESC", "Payments"));
        assertEquals("020012Payments", category.substring(0, 14));
        assertEquals("2022-07-06", PostingLayouts.DATE.decode("2022-01-01 2022-07-06").get("WS-END-DATE"));
        assertEquals(80, PostingLayouts.DATE.length()); assertEquals(133, PostingLayouts.REPORT.length());
    }
}
