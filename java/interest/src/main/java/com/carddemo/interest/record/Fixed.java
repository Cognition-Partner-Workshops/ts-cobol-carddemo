package com.carddemo.interest.record;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Fixed-width field codec for the copybook layouts, matching {@code cobc -fsign=EBCDIC}
 * on an ASCII host: signed DISPLAY numerics carry the sign as an overpunch on the last digit.
 */
public final class Fixed {

    private static final String POSITIVE = "{ABCDEFGHI";
    private static final String NEGATIVE = "}JKLMNOPQR";

    private Fixed() {
    }

    public static String text(String record, int offset, int length) {
        return record.substring(offset, offset + length);
    }

    public static String pad(String value, int length) {
        String padded = value.length() >= length ? value : value + " ".repeat(length - value.length());
        return padded.substring(0, length);
    }

    /** PIC 9(n). */
    public static long unsigned(String record, int offset, int length) {
        return Long.parseLong(text(record, offset, length));
    }

    public static String unsigned(long value, int length) {
        return String.format("%0" + length + "d", value);
    }

    /** PIC S9(i)V9(s) zoned with an overpunch sign. */
    public static BigDecimal zoned(String record, int offset, int length, int scale) {
        String raw = text(record, offset, length);
        char last = raw.charAt(length - 1);
        int sign = 1;
        char digit = last;
        if (POSITIVE.indexOf(last) >= 0) {
            digit = (char) ('0' + POSITIVE.indexOf(last));
        } else if (NEGATIVE.indexOf(last) >= 0) {
            digit = (char) ('0' + NEGATIVE.indexOf(last));
            sign = -1;
        }
        BigInteger unscaled = new BigInteger(raw.substring(0, length - 1) + digit);
        return new BigDecimal(sign < 0 ? unscaled.negate() : unscaled, scale);
    }

    public static String zoned(BigDecimal value, int length, int scale) {
        BigDecimal fitted = fit(value, length - scale, scale);
        String digits = unsigned(fitted.unscaledValue().abs().longValueExact(), length);
        int last = digits.charAt(length - 1) - '0';
        char punch = fitted.signum() < 0 ? NEGATIVE.charAt(last) : POSITIVE.charAt(last);
        return digits.substring(0, length - 1) + punch;
    }

    /**
     * Store a value into PIC S9(integerDigits)V9(scale) the way COBOL does without
     * ROUNDED or ON SIZE ERROR: excess decimals are truncated toward zero and excess
     * high-order digits are dropped.
     */
    public static BigDecimal fit(BigDecimal value, int integerDigits, int scale) {
        BigInteger unscaled = value.setScale(scale, RoundingMode.DOWN).unscaledValue();
        BigInteger modulus = BigInteger.TEN.pow(integerDigits + scale);
        return new BigDecimal(unscaled.abs().mod(modulus).multiply(BigInteger.valueOf(unscaled.signum() < 0 ? -1 : 1)), scale);
    }
}
