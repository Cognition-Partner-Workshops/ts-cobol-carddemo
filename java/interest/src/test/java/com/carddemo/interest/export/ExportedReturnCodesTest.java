package com.carddemo.interest.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.carddemo.interest.Fixtures;

class ExportedReturnCodesTest {

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void returnCodeZero_isTheRecordedFileOfEveryCase(String caseName) {
        assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve("rc.json")), ExportedReturnCodes.json(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 12})
    void anotherReturnCode_isStep15AndMaxrcOnly(int returnCode) {
        String json = ExportedReturnCodes.json(returnCode);

        assertTrue(json.contains("\"maxrc\": " + returnCode + ","));
        assertTrue(json.contains("\"STEP15\": " + returnCode + ","));
        assertTrue(json.contains("\"STEP05\": 0,"));
        assertTrue(json.contains("\"STEP30\": 0\n"));
    }
}
