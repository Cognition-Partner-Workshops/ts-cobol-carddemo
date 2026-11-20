package org.carddemo.modern.codec;

/** One elementary item of a copybook, flattened to its byte offset in the record. */
public record Field(String name, int offset, int length, Kind kind, int scale, boolean signed) {

    public enum Kind { TEXT, ZONED, FILLER }

    public static Field text(String name, int offset, int length) {
        return new Field(name, offset, length, Kind.TEXT, 0, false);
    }

    public static Field zoned(String name, int offset, int length, int scale, boolean signed) {
        return new Field(name, offset, length, Kind.ZONED, scale, signed);
    }

    public static Field filler(int offset, int length) {
        return new Field("FILLER@" + offset, offset, length, Kind.FILLER, 0, false);
    }

    public int end() {
        return offset + length;
    }
}
