package com.carddemo.interest.recon;

import java.math.BigDecimal;

/** One side of a check: a value, or the reason there is none. */
public record Figure(Object value, String whyMissing) {

    public static Figure of(BigDecimal amount) {
        return amount == null ? missing("not recorded") : new Figure(amount, null);
    }

    public static Figure of(Long count) {
        return count == null ? missing("not recorded") : new Figure(count, null);
    }

    public static Figure of(String text) {
        return new Figure(text, null);
    }

    public static Figure missing(String why) {
        return new Figure(null, why);
    }

    public boolean available() {
        return value != null;
    }

    /** Amounts are equal when their values are, whatever their scale; a missing figure equals nothing. */
    public boolean sameAs(Figure other) {
        if (!available() || !other.available()) {
            return false;
        }
        if (value instanceof BigDecimal mine && other.value instanceof BigDecimal theirs) {
            return mine.compareTo(theirs) == 0;
        }
        return value.toString().equals(other.value.toString());
    }

    public String text() {
        if (!available()) {
            return Verdict.NOT_AVAILABLE.text() + " (" + whyMissing + ")";
        }
        return value instanceof BigDecimal amount ? amount.toPlainString() : value.toString();
    }
}
