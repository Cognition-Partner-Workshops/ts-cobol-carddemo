package com.carddemo.posting.record;

/** PIC X helpers: fixed-width slices of a record and space padding back to copybook width. */
public final class Text {

    private Text() {
    }

    public static String pad(String value, int width) {
        String text = value == null ? "" : value;
        return text.length() >= width ? text.substring(0, width) : text + " ".repeat(width - text.length());
    }

    public static String slice(String record, int offset, int length) {
        return pad(record, offset + length).substring(offset, offset + length);
    }

    public static String rtrim(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(0, end);
    }

    /** PIC X(width) field value as stored in PostgreSQL (trailing spaces dropped). */
    public static String field(String record, int offset, int width) {
        return rtrim(slice(record, offset, width));
    }

    public static long unsigned(String record, int offset, int width) {
        return Zoned.decode(slice(record, offset, width), 0).longValueExact();
    }

    public static String digits(long value, int width) {
        String digits = Long.toString(Math.abs(value));
        digits = digits.length() > width ? digits.substring(digits.length() - width) : digits;
        return "0".repeat(width - digits.length()) + digits;
    }
}
