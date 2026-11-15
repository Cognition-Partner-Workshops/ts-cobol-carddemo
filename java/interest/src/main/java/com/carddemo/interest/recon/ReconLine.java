package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * One line of the reconciliation report: a check (RC1-RC6 of strategy-interest.md), what is
 * compared, the two figures with the side each one comes from, and the result. A figure that is
 * missing on either side makes the line NOT AVAILABLE; equality is only ever stated for two
 * figures that are both there.
 */
public record ReconLine(String check, String item, String leftSource, Optional<String> left, String rightSource,
        Optional<String> right, ReconStatus status) {

    static final String MISSING = ReconStatus.NOT_AVAILABLE.label();

    /** Two amounts or hash totals: equal by value, whatever the scale. */
    public static ReconLine ofAmounts(String check, String item, String leftSource, Optional<BigDecimal> left,
            String rightSource, Optional<BigDecimal> right) {
        return new ReconLine(check, item, leftSource, left.map(BigDecimal::toPlainString), rightSource,
                right.map(BigDecimal::toPlainString),
                status(left.isPresent() && right.isPresent(), () -> left.get().compareTo(right.get()) == 0));
    }

    /** Two counts. */
    public static ReconLine ofCounts(String check, String item, String leftSource, Optional<Long> left, String rightSource,
            Optional<Long> right) {
        return new ReconLine(check, item, leftSource, left.map(String::valueOf), rightSource, right.map(String::valueOf),
                status(left.isPresent() && right.isPresent(), () -> left.get().equals(right.get())));
    }

    /** Two keys, as text. */
    public static ReconLine ofKeys(String check, String item, String leftSource, Optional<String> left, String rightSource,
            Optional<String> right) {
        return new ReconLine(check, item, leftSource, left, rightSource, right,
                status(left.isPresent() && right.isPresent(), () -> left.get().equals(right.get())));
    }

    /** A check that cannot be made here at all; the reason stands where the missing figure would. */
    public static ReconLine notAvailable(String check, String item, String leftSource, String rightSource,
            Optional<String> right) {
        return new ReconLine(check, item, leftSource, Optional.empty(), rightSource, right, ReconStatus.NOT_AVAILABLE);
    }

    private static ReconStatus status(boolean bothThere, java.util.function.BooleanSupplier equal) {
        if (!bothThere) {
            return ReconStatus.NOT_AVAILABLE;
        }
        return equal.getAsBoolean() ? ReconStatus.EQUAL : ReconStatus.DIFFERENT;
    }

    public String leftText() {
        return left.orElse(MISSING);
    }

    public String rightText() {
        return right.orElse(MISSING);
    }
}
