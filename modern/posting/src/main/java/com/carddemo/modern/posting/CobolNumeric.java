package com.carddemo.modern.posting;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Storing a result into a COBOL numeric item. Without ROUNDED, COBOL drops excess decimal
 * places (truncation toward zero); without ON SIZE ERROR, excess high-order integer digits
 * are lost too. Both apply to COMPUTE, ADD and MOVE into {@code PIC S9(n)V9(m)}.
 */
final class CobolNumeric {

    private CobolNumeric() {
    }

    /** Value as stored in {@code PIC S9(integerDigits)V9(scale)} without ROUNDED / ON SIZE ERROR. */
    static BigDecimal store(BigDecimal value, int integerDigits, int scale) {
        BigDecimal truncated = value.setScale(scale, RoundingMode.DOWN);
        BigDecimal modulus = BigDecimal.TEN.pow(integerDigits);
        return truncated.remainder(modulus).setScale(scale, RoundingMode.UNNECESSARY);
    }
}
