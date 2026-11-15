package com.carddemo.interest.recon;

import java.util.ArrayList;
import java.util.List;

/**
 * One difference found by tools/parity/compare.py: where (dataset, record and key, or SYSOUT,
 * rc.json), the field, the legacy value and the target value, already decoded by the comparator.
 */
public record Difference(String where, String field, String legacy, String target) {

    /** One line of the differences file: four columns separated by tabs, with \t, \n and \\ escaped. */
    public static Difference ofLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder column = new StringBuilder();
        for (int at = 0; at < line.length(); at++) {
            char c = line.charAt(at);
            if (c == '\t') {
                columns.add(column.toString());
                column.setLength(0);
            } else if (c == '\\' && at + 1 < line.length()) {
                char escaped = line.charAt(++at);
                column.append(escaped == 't' ? '\t' : escaped == 'n' ? '\n' : escaped);
            } else {
                column.append(c);
            }
        }
        columns.add(column.toString());
        if (columns.size() != 4) {
            throw new IllegalArgumentException("a difference line has four columns, found " + columns.size());
        }
        return new Difference(columns.get(0), columns.get(1), columns.get(2), columns.get(3));
    }
}
