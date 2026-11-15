package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * One figure of a report: a count, a sum or a key, or the reason why it could not be computed.
 * A figure that is not available is never equal to anything (ticket INT-T10, Acceptance: Errors).
 */
public record Figure(Object value, String whyNot) {

    public static Figure of(long count) {
        return new Figure(BigDecimal.valueOf(count), null);
    }

    public static Figure of(BigDecimal amount) {
        return amount == null ? notAvailable("no value") : new Figure(amount, null);
    }

    public static Figure of(String text) {
        return text == null ? notAvailable("no value") : new Figure(text, null);
    }

    public static Figure notAvailable(String whyNot) {
        return new Figure(null, whyNot);
    }

    /** The figure, or NOT AVAILABLE with the first line of the failure when it cannot be read. */
    public static Figure attempt(Supplier<Figure> source) {
        try {
            return source.get();
        } catch (RuntimeException e) {
            return notAvailable(firstLine(e));
        }
    }

    static String firstLine(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        return message.lines().findFirst().orElse(cause.getClass().getSimpleName()).strip();
    }

    public boolean isAvailable() {
        return value != null;
    }

    /** Numbers are compared by value (3400 and 3400.00 are the same figure), texts by their characters. */
    public boolean sameAs(Figure other) {
        if (!isAvailable() || !other.isAvailable()) {
            return false;
        }
        if (value instanceof BigDecimal number && other.value instanceof BigDecimal otherNumber) {
            return number.compareTo(otherNumber) == 0;
        }
        return value.equals(other.value);
    }

    /** Sum of two figures of one scale: exact, nothing is rounded (standards S3.3). */
    public Figure plus(Figure other) {
        return combined(other, false);
    }

    public Figure minus(Figure other) {
        return combined(other, true);
    }

    private Figure combined(Figure other, boolean isDifference) {
        if (!isAvailable()) {
            return this;
        }
        if (!other.isAvailable()) {
            return other;
        }
        if (!(value instanceof BigDecimal number) || !(other.value instanceof BigDecimal otherNumber)) {
            return notAvailable("not a number");
        }
        return of(isDifference ? number.subtract(otherNumber) : number.add(otherNumber));
    }

    public String text() {
        if (!isAvailable()) {
            return Verdict.NOT_AVAILABLE.label() + " (" + whyNot + ")";
        }
        return value instanceof BigDecimal number ? number.toPlainString() : value.toString();
    }
}
