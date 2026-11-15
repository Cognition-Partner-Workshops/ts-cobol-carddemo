package com.carddemo.interest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** INT-T05 (CBACT04C:140-165, 613-624). */
class RunTimestampTest {

    @Test
    void intT05_timestampFormat() {
        // the frozen fixture clock 2022-07-05 10:00:00.00, as recorded in i01-baseline TRAN-ORIG-TS
        assertEquals("2022-07-05-10.00.00.000000", RunTimestamp.format(LocalDateTime.of(2022, 7, 5, 10, 0, 0, 0)));
    }

    @Test
    void intT05_hundredthsAreCutNotRounded() {
        // 23:59:58.129999999 -> hundredths 12, then the literal 0000
        assertEquals("2023-12-31-23.59.58.120000",
                RunTimestamp.format(LocalDateTime.of(2023, 12, 31, 23, 59, 58, 129_999_999)));
        assertEquals("2023-01-02-03.04.05.990000",
                RunTimestamp.format(LocalDateTime.of(2023, 1, 2, 3, 4, 5, 999_999_999)));
    }
}
