package com.carddemo.interest.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * INT-T05: the 26-character timestamp YYYY-MM-DD-HH.MM.SS.hh0000 (CBACT04C:140-165, 613-624).
 * Hundredths are cut from the clock, not rounded, and followed by the literal 0000.
 */
public final class RunTimestamp {

    private static final DateTimeFormatter TO_THE_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss");

    private RunTimestamp() {
    }

    public static String format(LocalDateTime moment) {
        int hundredths = moment.getNano() / 10_000_000;
        return TO_THE_SECOND.format(moment) + String.format(".%02d0000", hundredths);
    }
}
