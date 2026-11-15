package com.carddemo.interest.codec;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One elementary field of a record layout, derived from its PICTURE and USAGE as written in the
 * copybook. Offset and length are in bytes. A field whose USAGE is not DISPLAY is kept with kind
 * {@link FieldKind#NOT_DISPLAY} and length 0: its size is not guessed (rule E5).
 */
public record FieldSpec(String name, int index, FieldKind kind, int offset, int length, int scale, String usage) {

    public static final String FILLER = "FILLER";
    public static final String DISPLAY = "DISPLAY";
    /** A long holds 18 digits; none of the five layouts has a longer field. */
    private static final int MAX_DIGITS = 18;

    private static final Pattern TEXT_PICTURE = Pattern.compile("(X(\\(\\d+\\))?)+");
    private static final Pattern NUMERIC_PICTURE = Pattern.compile("(S?)((?:9(?:\\(\\d+\\))?)+)(?:V((?:9(?:\\(\\d+\\))?)+))?");
    private static final Pattern SYMBOL = Pattern.compile("[X9](?:\\((\\d+)\\))?");

    public int end() {
        return offset + length;
    }

    static FieldSpec of(String name, int index, int offset, String picture, String usage) {
        String pic = picture.toUpperCase(Locale.ROOT);
        boolean filler = FILLER.equals(name);
        if (!DISPLAY.equals(usage)) {
            return new FieldSpec(name, index, FieldKind.NOT_DISPLAY, offset, 0, 0, usage);
        }
        if (TEXT_PICTURE.matcher(pic).matches()) {
            return new FieldSpec(name, index, filler ? FieldKind.FILLER : FieldKind.TEXT, offset, count(pic), 0, usage);
        }
        Matcher numeric = NUMERIC_PICTURE.matcher(pic);
        if (!numeric.matches() || filler) {
            throw new IllegalArgumentException("field " + name + ": PICTURE " + picture + " is not supported");
        }
        boolean signed = !numeric.group(1).isEmpty();
        int scale = numeric.group(3) == null ? 0 : count(numeric.group(3));
        int digits = count(numeric.group(2)) + scale;
        if (digits > MAX_DIGITS || (!signed && scale > 0)) {
            throw new IllegalArgumentException("field " + name + ": PICTURE " + picture + " is not supported");
        }
        return new FieldSpec(name, index, signed ? FieldKind.SIGNED_ZONED : FieldKind.UNSIGNED_ZONED, offset, digits, scale, usage);
    }

    private static int count(String symbols) {
        int total = 0;
        Matcher symbol = SYMBOL.matcher(symbols);
        while (symbol.find()) {
            total += symbol.group(1) == null ? 1 : Integer.parseInt(symbol.group(1));
        }
        return total;
    }
}
