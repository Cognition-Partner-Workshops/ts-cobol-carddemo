package com.carddemo.batch.interest;

import java.util.ArrayList;
import java.util.List;

final class Text {
    private Text() {
    }

    /** Left-justifies and space-fills or truncates to {@code length}, like a COBOL alphanumeric MOVE. */
    static String pad(String value, int length) {
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }

    static String rstrip(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(0, end);
    }

    /** Splits like Python's str.splitlines() for \n, \r\n and \r terminators. */
    static List<String> lines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                lines.add(text.substring(start, i));
                i += (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') ? 2 : 1;
                start = i;
            } else {
                i++;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
        }
        return lines;
    }
}
