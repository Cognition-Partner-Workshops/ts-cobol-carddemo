package com.carddemo.interest.jdbc;

/** A CHAR(n) column comes back padded; the domain sees text without trailing spaces, as from the codec (rule E3). */
final class Texts {

    private Texts() {
    }

    static String withoutTrailingSpaces(String text) {
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == ' ') {
            end--;
        }
        return text.substring(0, end);
    }
}
