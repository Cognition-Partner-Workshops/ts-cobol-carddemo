package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Ticket INT-T10, Acceptance "Errors": a figure that cannot be computed is NOT AVAILABLE, never equal. */
class FigureTest {

    @Test
    void notAvailable_isNeverEqual_notEvenToItself() {
        Figure missing = Figure.notAvailable("no file");

        assertFalse(missing.sameAs(missing));
        assertFalse(missing.sameAs(Figure.of(0)));
        assertFalse(Figure.of(0).sameAs(missing));
        assertEquals("NOT AVAILABLE (no file)", missing.text());
    }

    @Test
    void notAvailable_lineIsNotAvailable_notEqualAndNotDifferent() {
        CheckLine line = CheckLine.compare("RC1", "records and rows", "file", Figure.notAvailable("no file"),
                "table", Figure.of(2), "");

        assertEquals(Verdict.NOT_AVAILABLE, line.verdict());
    }

    @Test
    void numbers_areComparedByValue_notByScale() {
        // the hash total of the file is 4003, interest_run holds it as NUMERIC(18,2): 4003.00
        assertTrue(Figure.of(4003).sameAs(Figure.of(new BigDecimal("4003.00"))));
        assertFalse(Figure.of(new BigDecimal("47.00")).sameAs(Figure.of(new BigDecimal("47.01"))));
        assertEquals("47.00", Figure.of(new BigDecimal("47.00")).text());
    }

    @Test
    void arithmetic_isExact_andKeepsTheReasonOfAMissingFigure() {
        // i01-baseline: 3410.00 - 3400.00 = 10.00; 10.00 + 37.00 = 47.00
        Figure change = Figure.of(new BigDecimal("3410.00")).minus(Figure.of(new BigDecimal("3400.00")));

        assertEquals("10.00", change.text());
        assertEquals("47.00", change.plus(Figure.of(new BigDecimal("37.00"))).text());
        assertEquals("NOT AVAILABLE (no run)", change.plus(Figure.notAvailable("no run")).text());
    }

    @Test
    void differentFigures_giveDifferent_andABrokenConditionBesideEqualFiguresToo() {
        assertEquals(Verdict.DIFFERENT, CheckLine.compare("RC1", "x", "a", Figure.of(2), "b", Figure.of(3), "").verdict());
        assertEquals(Verdict.EQUAL, CheckLine.compare("RC3", "x", "a", Figure.of(2), "b", Figure.of(2), true, "").verdict());
        assertEquals(Verdict.DIFFERENT, CheckLine.compare("RC3", "x", "a", Figure.of(2), "b", Figure.of(2), false, "").verdict());
    }

    @Test
    void verdictOfSeveral_differentWins_thenNotAvailable_andNothingCheckedIsNotAvailable() {
        assertEquals(Verdict.DIFFERENT, Verdict.of(List.of(Verdict.EQUAL, Verdict.NOT_AVAILABLE, Verdict.DIFFERENT)));
        assertEquals(Verdict.NOT_AVAILABLE, Verdict.of(List.of(Verdict.EQUAL, Verdict.NOT_AVAILABLE)));
        assertEquals(Verdict.EQUAL, Verdict.of(List.of(Verdict.EQUAL, Verdict.EQUAL)));
        assertEquals(Verdict.NOT_AVAILABLE, Verdict.of(List.of()));
    }
}
