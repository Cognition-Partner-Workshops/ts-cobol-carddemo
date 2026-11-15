package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** A line is EQUAL only for two figures that are both there and the same (ticket 10, Errors). */
class ReconLineTest {

    @Test
    void amountsOfTheSameValueAreEqualWhateverTheScale() {
        ReconLine line = ReconLine.ofAmounts("RC2", "sum", "file", Optional.of(new BigDecimal("4003")), "run",
                Optional.of(new BigDecimal("4003.00")));

        assertEquals(ReconStatus.EQUAL, line.status());
        assertEquals("4003", line.leftText());
        assertEquals("4003.00", line.rightText());
    }

    @Test
    void amountsOneCentApartAreDifferentAndBothAreShown() {
        ReconLine line = ReconLine.ofAmounts("RC5", "sum", "table", Optional.of(new BigDecimal("47.00")), "run",
                Optional.of(new BigDecimal("47.01")));

        assertEquals(ReconStatus.DIFFERENT, line.status());
        assertEquals("47.00", line.leftText());
        assertEquals("47.01", line.rightText());
    }

    @Test
    void aMissingFigureIsNotAvailableOnEitherSide() {
        assertEquals(ReconStatus.NOT_AVAILABLE,
                ReconLine.ofCounts("RC1", "rows", "file", Optional.empty(), "table", Optional.of(2L)).status());
        assertEquals(ReconStatus.NOT_AVAILABLE,
                ReconLine.ofCounts("RC1", "rows", "file", Optional.of(2L), "table", Optional.empty()).status());
        assertEquals(ReconStatus.NOT_AVAILABLE,
                ReconLine.ofAmounts("RC2", "sum", "file", Optional.empty(), "run", Optional.empty()).status());
        assertEquals(ReconStatus.NOT_AVAILABLE,
                ReconLine.ofKeys("RC4", "key", "file", Optional.empty(), "table", Optional.empty()).status());
    }

    @Test
    void twoMissingFiguresAreNeverEqual_theTextSaysNotAvailable() {
        ReconLine line = ReconLine.ofCounts("RC5", "rows", "table", Optional.empty(), "run", Optional.empty());

        assertEquals("NOT AVAILABLE", line.status().label());
        assertEquals("NOT AVAILABLE", line.leftText());
        assertEquals("NOT AVAILABLE", line.rightText());
    }

    @Test
    void countsAndKeys() {
        assertEquals(ReconStatus.EQUAL, ReconLine.ofCounts("RC1", "rows", "file", Optional.of(15L), "table", Optional.of(15L)).status());
        assertEquals(ReconStatus.DIFFERENT, ReconLine.ofCounts("RC1", "rows", "file", Optional.of(15L), "table", Optional.of(14L)).status());
        assertEquals(ReconStatus.EQUAL, ReconLine.ofKeys("RC4", "key", "file", Optional.of("00000002001"), "table", Optional.of("00000002001")).status());
        assertEquals(ReconStatus.DIFFERENT, ReconLine.ofKeys("RC4", "key", "file", Optional.of("00000002001"), "table", Optional.of("00000002002")).status());
    }
}
