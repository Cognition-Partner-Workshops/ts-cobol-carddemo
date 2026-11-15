package com.carddemo.interest.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.carddemo.interest.Fixtures;

class RecordedReturnCodesTest {

    /** INT-T03: a normal end is return code 0; the file has the recorded shape (pitfall P4) and no final newline. */
    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intT03_rcJsonMatchesRecorded(String caseName) {
        String recorded = Fixtures.text(Fixtures.expected(caseName).resolve("rc.json"));
        String written = RecordedReturnCodes.json(RecordedReturnCodes.NORMAL_END);
        assertEquals(recorded, written);
        assertFalse(written.endsWith("\n"));
    }
}
