package org.carddemo.modern.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PostingLayoutsTest {
    @Test
    void everyCalledCopybookFieldHasTheRightNameOffsetPicAndSign() throws Exception {
        var pattern = Pattern.compile("\\s+\\d+\\s+([A-Z0-9-]+)\\s+PIC\\s+(S?)([X9])\\((\\d+)\\)(V99)?\\.");
        for (var layout : List.of(Layouts.ACCOUNT, Layouts.XREF, Layouts.CATEGORY_BALANCE, Layouts.DISCLOSURE,
                Layouts.DALYTRAN, Layouts.TRAN, Layouts.TRAN_TYPE, Layouts.TRAN_CATEGORY, Layouts.REJTRAN)) {
            var path = Path.of(layout == Layouts.REJTRAN ? "../../tools/parity/layouts/REJTRAN.cpy"
                    : "../../app/cpy/" + layout.copybook() + ".cpy");
            int offset = 0;
            int index = 0;
            for (var line : Files.readAllLines(path)) {
                var match = pattern.matcher(line);
                if (!match.find()) continue;
                int scale = match.group(5) == null ? 0 : 2;
                int length = Integer.parseInt(match.group(4)) + scale;
                var field = layout.fields().get(index++);
                assertEquals(offset, field.offset(), match.group(1));
                assertEquals(length, field.length(), match.group(1));
                assertEquals(scale, field.scale(), match.group(1));
                assertEquals(!match.group(2).isEmpty(), field.signed(), match.group(1));
                if (match.group(1).equals("FILLER")) assertEquals(Field.Kind.FILLER, field.kind());
                else {
                    assertEquals(match.group(1), field.name());
                    assertEquals(match.group(3).equals("X") ? Field.Kind.TEXT : Field.Kind.ZONED, field.kind());
                }
                offset += length;
            }
            assertEquals(layout.fields().size(), index, layout.copybook());
            assertEquals(offset, layout.length(), layout.copybook());
        }
    }

    @Test
    void rejectReasonIsEncodedAndDecodedRatherThanDiscardedAsFiller() {
        var encoded = new RejectRecord(Layouts.DALYTRAN.encode(Map.of()), 103, "TRANSACTION RECEIVED AFTER ACCT EXPIRATION").toLine();
        assertEquals("0103", encoded.substring(350, 354));
        assertEquals(new BigDecimal("103"), Layouts.REJTRAN.decode(encoded).get("REJ-FAIL-REASON"));
        assertEquals(encoded, Layouts.REJTRAN.encodeLine(Layouts.REJTRAN.decode(encoded)));
    }

    @Test
    void datesAndReportMatchTheProgramDefinedRecords() throws Exception {
        var source = Files.readString(Path.of("../../app/cbl/CBTRN03C.cbl"));
        assertTrue(source.contains("01 FD-REPTFILE-REC       PIC X(133)."));
        assertTrue(source.contains("01 FD-DATEPARM-REC       PIC X(80)."));
        assertTrue(source.contains("05 WS-START-DATE      PIC X(10)."));
        assertTrue(source.contains("05 WS-END-DATE        PIC X(10)."));
        var dates = Layouts.DATE_PARAMETERS.decode("2022-01-01 2022-07-06");
        assertEquals("2022-01-01", dates.get("WS-START-DATE"));
        assertEquals("2022-07-06", dates.get("WS-END-DATE"));
        assertEquals(80, Layouts.DATE_PARAMETERS.length());
        assertEquals(133, Layouts.REPORT.length());
        assertEquals("hello", Layouts.REPORT.encodeLine(Map.of("FD-REPTFILE-REC", "hello")));
    }
}
