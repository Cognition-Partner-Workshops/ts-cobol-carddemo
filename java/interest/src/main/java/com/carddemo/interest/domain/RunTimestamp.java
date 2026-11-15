package com.carddemo.interest.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * INT-T05: the 26-character timestamp YYYY-MM-DD-HH.MM.SS.hh0000 (CBACT04C:140-165, 613-624).
 * Hundredths are cut from the clock, not rounded, and followed by the literal 0000.
 */
public final class RunTimestamp {

    private static final DateTimeFormatter TO_THE_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss");

    private static final int CYCLE_DATE_LENGTH = 8;
    private static final String MIDNIGHT = "-00.00.00.000000";

    private RunTimestamp() {
    }

    public static String format(LocalDateTime moment) {
        int hundredths = moment.getNano() / 10_000_000;
        return TO_THE_SECOND.format(moment) + String.format(".%02d0000", hundredths);
    }

    /**
     * INT-Q03 option B: the cycle date of the PARM at 00.00.00. The PARM stays text (standards S3.8):
     * its first eight characters are YYYYMMDD and are copied, not parsed as a date.
     */
    public static String ofCycleDate(String parmDate) {
        String cycleDate = parmDate.substring(0, CYCLE_DATE_LENGTH);
        if (!cycleDate.chars().allMatch(character -> character >= '0' && character <= '9')) {
            throw new InterestRunStoppedException(StopReason.CYCLE_DATE_UNREADABLE,
                    "the PARM does not start with the eight digits of a cycle date");
        }
        return cycleDate.substring(0, 4) + "-" + cycleDate.substring(4, 6) + "-" + cycleDate.substring(6)
                + MIDNIGHT;
    }
}
