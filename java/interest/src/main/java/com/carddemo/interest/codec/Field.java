package com.carddemo.interest.codec;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * One elementary USAGE DISPLAY field of a fixed-length record, in the fixture code page, and the
 * encoding rules of the data map: E1 unsigned zoned, E2 signed zoned with trailing overpunch,
 * E3 text. FILLER (E4) is never a Field: it is carried as part of the record image.
 * A value that breaks a rule rejects the record; nothing is substituted, padded over or cut.
 */
public record Field(String name, int offset, int length) {

    private static final String POSITIVE_OVERPUNCH = "{ABCDEFGHI";
    private static final String NEGATIVE_OVERPUNCH = "}JKLMNOPQR";

    public int end() {
        return offset + length;
    }

    private String slice(String image) {
        return image.substring(offset, end());
    }

    /** E3: text without its trailing padding. */
    public String text(String image) {
        String raw = slice(image);
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) < ' ') {
                throw rejected("not a character of the code page");
            }
        }
        return raw.stripTrailing();
    }

    /** E1: n digit characters. */
    public long unsigned(String image) {
        String raw = slice(image);
        for (int i = 0; i < raw.length(); i++) {
            if (!isDigit(raw.charAt(i))) {
                throw rejected("not numeric");
            }
        }
        return Long.parseLong(raw);
    }

    /** E2: sign and last digit share the last byte; scale is the number of implied decimals. */
    public BigDecimal signed(String image, int scale) {
        String raw = slice(image);
        char last = raw.charAt(length - 1);
        int positive = POSITIVE_OVERPUNCH.indexOf(last);
        int negative = NEGATIVE_OVERPUNCH.indexOf(last);
        int lastDigit;
        if (positive >= 0) {
            lastDigit = positive;
        } else if (negative >= 0) {
            lastDigit = negative;
        } else if (isDigit(last)) {
            lastDigit = last - '0';
        } else {
            throw rejected("not numeric");
        }
        long unscaled = 0;
        for (int i = 0; i < length - 1; i++) {
            char c = raw.charAt(i);
            if (!isDigit(c)) {
                throw rejected("not numeric");
            }
            unscaled = unscaled * 10 + (c - '0');
        }
        unscaled = unscaled * 10 + lastDigit;
        return BigDecimal.valueOf(negative >= 0 ? -unscaled : unscaled, scale);
    }

    /** E3: padded with spaces to the field width. */
    public String encodeText(String value) {
        if (value.length() > length) {
            throw rejected("value longer than the field");
        }
        return value + " ".repeat(length - value.length());
    }

    /** E1: zero-padded to the field width. */
    public String encodeUnsigned(long value) {
        String digits = Long.toString(value);
        if (value < 0 || digits.length() > length) {
            throw rejected("value does not fit the field");
        }
        return "0".repeat(length - digits.length()) + digits;
    }

    /** E2: C-signed for zero or positive, D-signed for negative (minus zero comes back plus, G03). */
    public String encodeSigned(BigDecimal value, int scale) {
        String digits = value.setScale(scale, RoundingMode.UNNECESSARY).unscaledValue().abs().toString();
        if (digits.length() > length) {
            throw rejected("value does not fit the field");
        }
        String padded = "0".repeat(length - digits.length()) + digits;
        int lastDigit = padded.charAt(length - 1) - '0';
        String overpunch = value.signum() < 0 ? NEGATIVE_OVERPUNCH : POSITIVE_OVERPUNCH;
        return padded.substring(0, length - 1) + overpunch.charAt(lastDigit);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private InterestRunStoppedException rejected(String why) {
        return new InterestRunStoppedException(StopReason.RECORD_REJECTED, "field " + name + ": " + why);
    }
}
