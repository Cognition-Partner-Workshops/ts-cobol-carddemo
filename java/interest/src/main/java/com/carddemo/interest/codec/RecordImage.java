package com.carddemo.interest.codec;

import java.util.Arrays;

/**
 * A record in the recorded fixture format (FI-2): one line per record, ISO-8859-1, trailing spaces
 * removed (tools/parity/record.py:193, 246). A line gets its spaces back up to the record length;
 * a line longer than the record is a record of the wrong length and is rejected.
 */
public final class RecordImage {

    private RecordImage() {
    }

    /** The full-length record of a fixture line, as text of the fixture code page (one character per byte). */
    public static String fromLine(String line, RecordLayout layout, RecordSource source) {
        if (line.length() > layout.length()) {
            throw RecordLayout.rejected(source, "(record)",
                    "record is " + line.length() + " bytes, " + layout.copybook() + " is " + layout.length());
        }
        return line + " ".repeat(layout.length() - line.length());
    }

    /** The fixture line of a full-length record: its trailing spaces removed. */
    public static String toLine(String image) {
        int end = image.length();
        while (end > 0 && image.charAt(end - 1) == ' ') {
            end--;
        }
        return image.substring(0, end);
    }

    public static byte[] bytes(String image) {
        return image.getBytes(FixtureCodePage.CHARSET);
    }

    public static String image(byte[] record) {
        return new String(record, FixtureCodePage.CHARSET);
    }

    /** The records of a host file: fixed length, no separator. A file that does not end on a record boundary is rejected. */
    public static byte[][] hostRecords(byte[] content, RecordLayout layout, String file) {
        int count = content.length / layout.length();
        if (content.length % layout.length() != 0) {
            throw RecordLayout.rejected(new RecordSource(file, count + 1), "(record)",
                    "record is " + content.length % layout.length() + " bytes, " + layout.copybook() + " is " + layout.length());
        }
        byte[][] records = new byte[count][];
        for (int i = 0; i < count; i++) {
            records[i] = Arrays.copyOfRange(content, i * layout.length(), (i + 1) * layout.length());
        }
        return records;
    }
}
