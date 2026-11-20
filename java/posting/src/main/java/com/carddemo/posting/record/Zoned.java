package com.carddemo.posting.record;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * COBOL USAGE DISPLAY numerics as the estate compiles them ({@code -fsign=EBCDIC}): the sign
 * of a signed field is overpunched on the last digit ({@code {A-I} positive, {@code }J-R}
 * negative). Arithmetic results are truncated on the left like an ADD/COMPUTE without
 * ON SIZE ERROR and on the right like a COMPUTE without ROUNDED.
 */
public final class Zoned {

    private static final String POSITIVE = "{ABCDEFGHI";
    private static final String NEGATIVE = "}JKLMNOPQR";
    private static final String EDIT_MASK = "ZZZ,ZZZ,ZZZ.ZZ";

    private Zoned() {
    }

    public static BigDecimal decode(String text, int scale) {
        boolean negative = false;
        StringBuilder digits = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i == text.length() - 1) {
                int positive = POSITIVE.indexOf(c);
                int negativeDigit = NEGATIVE.indexOf(c);
                if (positive >= 0) {
                    digits.append((char) ('0' + positive));
                    continue;
                }
                if (negativeDigit >= 0) {
                    digits.append((char) ('0' + negativeDigit));
                    negative = true;
                    continue;
                }
            }
            digits.append(c >= '0' && c <= '9' ? c : '0');
        }
        BigDecimal value = new BigDecimal(new BigInteger(digits.toString()), scale);
        return negative ? value.negate() : value;
    }

    /** Value as it ends up in a PIC S9(intDigits)V9(scale) receiving field. */
    public static BigDecimal fit(BigDecimal value, int intDigits, int scale) {
        BigInteger unscaled = value.setScale(scale, RoundingMode.DOWN).unscaledValue();
        BigInteger kept = unscaled.abs().mod(BigInteger.TEN.pow(intDigits + scale));
        return new BigDecimal(unscaled.signum() < 0 ? kept.negate() : kept, scale);
    }

    public static String encode(BigDecimal value, int intDigits, int scale, boolean signed) {
        BigDecimal fitted = fit(value, intDigits, scale);
        String digits = digits(fitted, intDigits + scale);
        if (!signed) {
            return digits;
        }
        int last = digits.charAt(digits.length() - 1) - '0';
        char sign = fitted.signum() < 0 ? NEGATIVE.charAt(last) : POSITIVE.charAt(last);
        return digits.substring(0, digits.length() - 1) + sign;
    }

    /** {@code DISPLAY} of a signed DISPLAY item under GnuCOBOL: digits then a trailing sign. */
    public static String display(BigDecimal value, int intDigits, int scale) {
        BigDecimal fitted = fit(value, intDigits, scale);
        return digits(fitted, intDigits + scale) + (fitted.signum() < 0 ? '-' : '+');
    }

    /**
     * MOVE to {@code -ZZZ,ZZZ,ZZZ.ZZ} ({@code plusSign=false}) or {@code +ZZZ,ZZZ,ZZZ.ZZ}
     * ({@code plusSign=true}). Every digit position is Z, so a zero value edits to spaces.
     */
    public static String edit(BigDecimal value, boolean plusSign) {
        BigDecimal fitted = fit(value, 9, 2);
        if (fitted.signum() == 0) {
            return " ".repeat(EDIT_MASK.length() + 1);
        }
        String digits = digits(fitted, 11);
        StringBuilder out = new StringBuilder(EDIT_MASK.length() + 1);
        out.append(fitted.signum() < 0 ? '-' : (plusSign ? '+' : ' '));
        boolean significant = false;
        int next = 0;
        for (char mask : EDIT_MASK.toCharArray()) {
            if (mask == 'Z') {
                char digit = digits.charAt(next++);
                significant |= digit != '0';
                out.append(significant ? digit : ' ');
            } else if (mask == ',') {
                out.append(significant ? ',' : ' ');
            } else {
                out.append('.');
                significant = true;
            }
        }
        return out.toString();
    }

    private static String digits(BigDecimal fitted, int width) {
        String digits = fitted.unscaledValue().abs().toString();
        return "0".repeat(width - digits.length()) + digits;
    }
}
