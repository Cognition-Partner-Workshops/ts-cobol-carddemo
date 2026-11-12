package org.carddemo.modern.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

/**
 * DISPLAY zoned decimal with the EBCDIC overpunch convention that
 * {@code cobc -fsign=EBCDIC} writes: the last digit carries the sign,
 * {@code {ABCDEFGHI} for 0-9 positive and {@code }JKLMNOPQR} for 0-9 negative.
 */
public final class Zoned {

    private static final String POSITIVE = "{ABCDEFGHI";
    private static final String NEGATIVE = "}JKLMNOPQR";

    private Zoned() {
    }

    public static BigDecimal decode(String raw, int scale, boolean signed) {
        if (raw.isBlank()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        String digits = raw;
        int sign = 1;
        char last = raw.charAt(raw.length() - 1);
        int pos = POSITIVE.indexOf(last);
        int neg = NEGATIVE.indexOf(last);
        if (pos >= 0) {
            digits = raw.substring(0, raw.length() - 1) + pos;
        } else if (neg >= 0) {
            digits = raw.substring(0, raw.length() - 1) + neg;
            sign = -1;
        }
        BigInteger units = new BigInteger(digits.replace(' ', '0'));
        BigDecimal value = new BigDecimal(units, scale);
        return sign < 0 ? value.negate() : value;
    }

    public static String encode(BigDecimal value, int length, int scale, boolean signed) {
        BigInteger units = value.abs().movePointRight(scale).toBigInteger();
        String text = String.format(Locale.ROOT, "%0" + length + "d", units);
        text = text.substring(text.length() - length);
        if (signed) {
            int lastDigit = text.charAt(length - 1) - '0';
            char zone = value.signum() < 0 ? NEGATIVE.charAt(lastDigit) : POSITIVE.charAt(lastDigit);
            text = text.substring(0, length - 1) + zone;
        }
        return text;
    }
}
