package com.carddemo.interest.codec;

/**
 * One elementary field of a copybook: its name, encoding rule, zero-based offset, length in bytes
 * and, for numbers, the count of implied decimals. {@code refusal} says why a REFUSED field is not handled.
 */
public record LayoutField(String name, FieldKind kind, int offset, int length, int scale, String refusal) {

    public int end() {
        return offset + length;
    }

    boolean numeric() {
        return kind == FieldKind.UNSIGNED_ZONED || kind == FieldKind.SIGNED_ZONED;
    }
}
