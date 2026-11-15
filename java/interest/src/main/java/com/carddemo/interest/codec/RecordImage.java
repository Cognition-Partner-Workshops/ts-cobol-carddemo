package com.carddemo.interest.codec;

/**
 * Fixed record length in the recorded fixture format (FI-2): a line of an input file has lost its
 * trailing spaces and gets them back; an output record loses them again (tools/parity/record.py:193, 246).
 */
public final class RecordImage {

    private RecordImage() {
    }

    public static String fromLine(String line, int recordLength) {
        if (line.length() >= recordLength) {
            return line.substring(0, recordLength);
        }
        return line + " ".repeat(recordLength - line.length());
    }

    public static String toLine(String image) {
        int end = image.length();
        while (end > 0 && image.charAt(end - 1) == ' ') {
            end--;
        }
        return image.substring(0, end);
    }
}
