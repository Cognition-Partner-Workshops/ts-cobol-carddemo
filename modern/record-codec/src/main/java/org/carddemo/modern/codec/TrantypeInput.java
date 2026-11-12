package org.carddemo.modern.codec;

import java.util.List;

public final class TrantypeInput {
    private TrantypeInput() {}

    public static final RecordLayout LAYOUT = new RecordLayout("COBTUPDT WS-INPUT-REC", List.of(
            Field.text("INPUT-REC-TYPE", 0, 1),
            Field.text("INPUT-REC-NUMBER", 1, 2),
            Field.text("INPUT-REC-DESC", 3, 50)));
}
