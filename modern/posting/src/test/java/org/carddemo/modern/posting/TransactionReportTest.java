package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.junit.jupiter.api.Test;

class TransactionReportTest {
    @Test
    void detailOffsetsAndWidthsFollowCvtra07y() throws Exception {
        var copybook = Files.readString(Path.of("../../app/cpy/CVTRA07Y.cpy"));
        assertTrue(copybook.contains("TRAN-REPORT-TYPE-DESC            PIC X(15)."));
        assertTrue(copybook.contains("TRAN-REPORT-CAT-DESC             PIC X(29)."));
        var report = new TransactionReport();
        report.headers("2022-01-01", "2022-07-06");
        var transaction = Layouts.TRAN.decode(Layouts.TRAN.encode(Map.of("TRAN-ID", "T1", "TRAN-TYPE-CD", "01",
                "TRAN-CAT-CD", 1234, "TRAN-AMT", new BigDecimal("-12.34"), "TRAN-SOURCE", "POS")));
        report.detail(transaction, Map.of("XREF-ACCT-ID", new BigDecimal("1001")),
                Map.of("TRAN-TYPE-DESC", "12345678901234567890"),
                Map.of("TRAN-CAT-TYPE-DESC", "C".repeat(50)), "2022-01-01", "2022-07-06");
        var line = Layouts.REPORT.encode(Map.of("FD-REPTFILE-REC", report.lines().getLast()));
        assertEquals(133, line.length());
        assertEquals("T1" + " ".repeat(14), line.substring(0, 16));
        assertEquals("00000001001", line.substring(17, 28));
        assertEquals("01-123456789012345", line.substring(29, 47));
        assertEquals("1234-" + "C".repeat(29), line.substring(48, 82));
        assertEquals("POS" + " ".repeat(7), line.substring(83, 93));
        assertEquals(TransactionReport.edited(new BigDecimal("-12.34"), false), line.substring(97, 112));
    }

    @Test
    void pageBreaksWriteHeadersAndTotalsWithoutLosingTheGrandTotal() {
        var report = new TransactionReport();
        report.headers("2022-01-01", "2022-07-06");
        for (int i = 0; i < 40; i++) {
            var transaction = Map.<String, Object>of("TRAN-ID", String.valueOf(i), "TRAN-TYPE-CD", "01",
                    "TRAN-CAT-CD", BigDecimal.ONE, "TRAN-SOURCE", "POS", "TRAN-AMT", BigDecimal.ONE);
            report.detail(transaction, Map.of("XREF-ACCT-ID", BigDecimal.ONE),
                    Map.of("TRAN-TYPE-DESC", "Purchase"), Map.of("TRAN-CAT-TYPE-DESC", "Retail"), "2022-01-01", "2022-07-06");
        }
        report.finish();
        assertTrue(report.lines().stream().filter(line -> line.startsWith("DALYREPT")).count() > 1);
        assertTrue(report.lines().getLast().endsWith(TransactionReport.edited(new BigDecimal("40.00"), true)));
    }

    @Test
    void numericEditingKeepsSignGroupingAndCents() {
        assertEquals("-" + " ".repeat(9) + "12.34", TransactionReport.edited(new BigDecimal("-12.34"), false));
        assertEquals("+" + " ".repeat(6) + "1,234.56", TransactionReport.edited(new BigDecimal("1234.56"), true));
        assertEquals(" ".repeat(12) + ".00", TransactionReport.edited(BigDecimal.ZERO, false));
    }
}
