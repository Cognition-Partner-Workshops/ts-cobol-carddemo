package com.carddemo.interest.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the data description of a copybook (FI-1) into a {@link RecordLayout}: the elementary
 * fields in order with their offsets. Understands what the five layouts of the interest stream
 * use - levels, group items, FILLER, PIC X(n), PIC 9(n), PIC S9(n)V9(m), USAGE DISPLAY - and marks
 * every other field REFUSED, so that a record of such a layout is rejected and never guessed (E5).
 */
public final class Copybook {

    private static final Pattern ENTRY = Pattern.compile("^(\\d{1,2})\\s+(\\S+)(?:\\s+(.*))?$", Pattern.DOTALL);
    private static final Pattern PICTURE = Pattern.compile("\\bPIC(?:TURE)?(?:\\s+IS)?\\s+(\\S+)");
    private static final Pattern PICTURE_PART = Pattern.compile("([SVX9])(?:\\((\\d+)\\))?");
    private static final Pattern NOT_DISPLAY = Pattern.compile(
            "\\b(COMP(?:UTATIONAL)?(?:-[1-5])?|BINARY|PACKED-DECIMAL|POINTER|INDEX|NATIONAL|DISPLAY-1)\\b");
    /** Clauses that do not change the bytes of a DISPLAY field: its picture, USAGE DISPLAY, and a VALUE up to the end of the entry. */
    private static final Pattern PLAIN_CLAUSES = Pattern.compile(
            "\\bPIC(?:TURE)?(?:\\s+IS)?\\s+\\S+|\\b(?:USAGE(?:\\s+IS)?\\s+)?DISPLAY\\b|\\bVALUES?\\b.*$");
    private static final Pattern NOT_IMPLEMENTED = Pattern.compile("\\b(OCCURS|REDEFINES|SEPARATE|LEADING|SYNC(?:HRONIZED)?|JUST(?:IFIED)?)\\b");

    private Copybook() {
    }

    /**
     * @param copybookName name used in messages, for example CVACT01Y
     * @param source the copybook text; fixed format, columns 1-6 and 73-80 are ignored, * in column 7 is a comment
     */
    public static RecordLayout parse(String copybookName, String source) {
        List<LayoutField> fields = new ArrayList<>();
        String recordName = null;
        int offset = 0;
        int fillers = 0;
        for (String entry : entries(source)) {
            Matcher matcher = ENTRY.matcher(entry);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("copybook " + copybookName + ": cannot read the entry '" + entry + "'");
            }
            int level = Integer.parseInt(matcher.group(1));
            String name = matcher.group(2);
            String clauses = matcher.group(3) == null ? "" : matcher.group(3);
            if (level == 88 || level == 66) {
                continue;
            }
            if (level == 1 && recordName == null) {
                recordName = name;
            }
            Matcher picture = PICTURE.matcher(clauses);
            Matcher notDisplay = NOT_DISPLAY.matcher(clauses);
            Matcher notImplemented = NOT_IMPLEMENTED.matcher(clauses);
            boolean elementary = picture.find();
            String otherClauses = PLAIN_CLAUSES.matcher(clauses).replaceAll("").strip();
            String refusal = notDisplay.find() ? "USAGE " + notDisplay.group(1) + " is not DISPLAY (E5)"
                    : notImplemented.find() ? notImplemented.group(1) + " is not handled by this codec"
                    : !otherClauses.isEmpty() ? "clause '" + otherClauses + "' is not handled by this codec" : null;
            if (!elementary && refusal == null) {
                continue;
            }
            boolean filler = name.equals("FILLER");
            String fieldName = filler ? (fillers++ == 0 ? "FILLER" : "FILLER-" + fillers) : name;
            LayoutField field = refusal != null
                    ? new LayoutField(fieldName, FieldKind.REFUSED, offset, 0, 0, refusal)
                    : fieldOf(copybookName, fieldName, filler, picture.group(1), offset);
            fields.add(field);
            offset = field.end();
        }
        if (recordName == null || fields.isEmpty()) {
            throw new IllegalArgumentException("copybook " + copybookName + ": no record description");
        }
        return new RecordLayout(copybookName, recordName, fields);
    }

    private static LayoutField fieldOf(String copybookName, String name, boolean filler, String pictureText, int offset) {
        Matcher parts = PICTURE_PART.matcher(pictureText);
        boolean signed = false;
        boolean text = false;
        boolean afterPoint = false;
        int digits = 0;
        int scale = 0;
        int characters = 0;
        int read = 0;
        while (parts.find() && parts.start() == read) {
            read = parts.end();
            int count = parts.group(2) == null ? 1 : Integer.parseInt(parts.group(2));
            switch (parts.group(1)) {
                case "S" -> signed = true;
                case "V" -> afterPoint = true;
                case "X" -> {
                    text = true;
                    characters += count;
                }
                default -> {
                    digits += count;
                    scale += afterPoint ? count : 0;
                }
            }
        }
        boolean mixed = text && (digits > 0 || signed || afterPoint);
        if (read != pictureText.length() || mixed || (digits == 0 && characters == 0)) {
            return new LayoutField(name, FieldKind.REFUSED, offset, 0, 0, "PIC " + pictureText + " is not handled by this codec");
        }
        if (filler) {
            return new LayoutField(name, FieldKind.FILLER, offset, characters + digits, 0, null);
        }
        if (text) {
            return new LayoutField(name, FieldKind.TEXT, offset, characters, 0, null);
        }
        return new LayoutField(name, signed ? FieldKind.SIGNED_ZONED : FieldKind.UNSIGNED_ZONED, offset, digits, scale, null);
    }

    /** The entries of the text: comments and sequence columns removed, each ending at its period. */
    private static List<String> entries(String source) {
        StringBuilder text = new StringBuilder();
        for (String rawLine : source.split("\\R")) {
            String line = rawLine.replace("\t", "        ");
            if (line.length() < 7 || line.charAt(6) == '*' || line.charAt(6) == '/') {
                continue;
            }
            text.append(line, 7, Math.min(line.length(), 72)).append(' ');
        }
        List<String> entries = new ArrayList<>();
        // a period ends an entry when white space follows it; the period inside 9(4).99 would not
        for (String entry : text.toString().toUpperCase(Locale.ROOT).split("\\.\\s+")) {
            String trimmed = entry.strip().replaceAll("\\s+", " ");
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries;
    }
}
