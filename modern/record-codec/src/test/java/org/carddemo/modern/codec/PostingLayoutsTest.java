package org.carddemo.modern.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostingLayoutsTest {
    @Test
    void typeAndCategoryLayoutsRoundTripFixtureRecords() throws Exception {
        for (var entry : Map.of("TRANTYPE", Layouts.TRAN_TYPE, "TRANCATG", Layouts.TRAN_CATEGORY).entrySet()) {
            assertEquals(60, entry.getValue().length());
            for (var line : Files.readAllLines(Path.of("../../fixtures/posting/p01-baseline-mixed/input/AWS.M2.CARDDEMO."
                    + entry.getKey() + ".PS.txt"))) {
                assertEquals(line.stripTrailing(), entry.getValue().encodeLine(entry.getValue().decode(line)));
            }
        }
    }

    @Test
    void rejectReasonIsNumericAndPreservesTheExactDailyRecord() throws Exception {
        var line = Files.readAllLines(Path.of("../../fixtures/posting/p02-invalid-card/expected/datasets/AWS.M2.CARDDEMO.DALYREJS.G0001V00.txt")).getFirst();
        var decoded = Layouts.REJTRAN.decode(line);
        assertEquals(new BigDecimal("100"), decoded.get("REJ-FAIL-REASON"));
        assertEquals(line, new RejectRecord(line.substring(0, 350), 100, "INVALID CARD NUMBER FOUND").toLine());
        assertEquals(line, Layouts.REJTRAN.encodeLine(decoded));
    }

    @Test
    void emptyRecordsDefaultNumericFieldsToZeroAndRejectInvalidNumericInput() {
        assertEquals(BigDecimal.ZERO, Layouts.TRAN_CATEGORY.decode("").get("TRAN-CAT-CD"));
        assertEquals(BigDecimal.ZERO, Layouts.REJTRAN.decode("").get("REJ-FAIL-REASON"));
        assertThrows(IllegalArgumentException.class, () -> Layouts.TRAN_CATEGORY.decode("01oops"));
    }
}
