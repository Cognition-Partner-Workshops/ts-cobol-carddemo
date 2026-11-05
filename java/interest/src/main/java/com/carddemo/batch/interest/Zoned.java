package com.carddemo.batch.interest;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * DISPLAY numeric fields as written by {@code cobc -std=ibm -fsign=EBCDIC}: unsigned fields are
 * plain digits, signed fields carry the sign as an overpunch on the last digit
 * ({@code {A..I} = +0..+9}, {@code }J..R} = -0..-9}).
 */
final class Zoned {
    private static final String POSITIVE = "{ABCDEFGHI";
    private static final String NEGATIVE = "}JKLMNOPQR";

    private Zoned() {
    }

    static BigDecimal decode(String raw, int scale) {
        StringBuilder digits = new StringBuilder(raw.length());
        int sign = 1;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            int pos = POSITIVE.indexOf(c);
            int neg = NEGATIVE.indexOf(c);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else if (pos >= 0) {
                digits.append((char) ('0' + pos));
            } else if (neg >= 0) {
                digits.append((char) ('0' + neg));
                sign = -1;
            } else {
                digits.append('0');
            }
        }
        BigInteger unscaled = new BigInteger(digits.toString());
        return new BigDecimal(sign < 0 ? unscaled.negate() : unscaled, scale);
    }

    /**
     * Stores {@code value} the way a COBOL MOVE/COMPUTE without ROUNDED or ON SIZE ERROR does:
     * excess decimals are truncated toward zero, excess high-order integer digits are dropped,
     * and a zero result is stored as positive zero.
     */
    static String encode(BigDecimal value, int digits, int scale, boolean signed) {
        BigInteger unscaled = truncate(value, digits, scale).unscaledValue();
        String text = String.format("%0" + digits + "d", unscaled.abs());
        if (!signed) {
            return text;
        }
        int last = text.charAt(digits - 1) - '0';
        char sign = unscaled.signum() < 0 ? NEGATIVE.charAt(last) : POSITIVE.charAt(last);
        return text.substring(0, digits - 1) + sign;
    }

    /** Truncates to {@code scale} decimals toward zero and to {@code digits} total digits. */
    static BigDecimal truncate(BigDecimal value, int digits, int scale) {
        BigInteger unscaled = value.setScale(scale, java.math.RoundingMode.DOWN).unscaledValue();
        BigInteger limit = BigInteger.TEN.pow(digits);
        return new BigDecimal(unscaled.remainder(limit), scale);
    }
}
