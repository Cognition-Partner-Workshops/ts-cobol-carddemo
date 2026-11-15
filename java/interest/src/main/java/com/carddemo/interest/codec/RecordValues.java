package com.carddemo.interest.codec;

import java.math.BigDecimal;

/**
 * The decoded values of one record, by field: {@code long} for E1, {@code BigDecimal} at the scale
 * of the PICTURE for E2, text without trailing spaces for E3, the raw bytes for a FILLER (E4).
 * Immutable: {@code with...} returns a copy. A field never set encodes as zeros (E1, E2) or as
 * spaces of the output code page (E3, E4 - gap G05).
 */
public final class RecordValues {

    private final RecordLayout layout;
    private final Object[] values;

    private RecordValues(RecordLayout layout, Object[] values) {
        this.layout = layout;
        this.values = values;
    }

    public static RecordValues of(RecordLayout layout) {
        return new RecordValues(layout, new Object[layout.fields().size()]);
    }

    public RecordLayout layout() {
        return layout;
    }

    public long unsigned(FieldSpec field) {
        Object value = value(field, FieldKind.UNSIGNED_ZONED);
        return value == null ? 0L : (Long) value;
    }

    public BigDecimal signed(FieldSpec field) {
        Object value = value(field, FieldKind.SIGNED_ZONED);
        return value == null ? BigDecimal.ZERO.setScale(field.scale()) : (BigDecimal) value;
    }

    public String text(FieldSpec field) {
        Object value = value(field, FieldKind.TEXT);
        return value == null ? "" : (String) value;
    }

    /** The raw bytes of a FILLER, or null when it was never set. */
    public byte[] filler(FieldSpec field) {
        Object value = value(field, FieldKind.FILLER);
        return value == null ? null : ((byte[]) value).clone();
    }

    public RecordValues withUnsigned(FieldSpec field, long value) {
        return with(field, FieldKind.UNSIGNED_ZONED, value);
    }

    public RecordValues withSigned(FieldSpec field, BigDecimal value) {
        return with(field, FieldKind.SIGNED_ZONED, value);
    }

    public RecordValues withText(FieldSpec field, String value) {
        return with(field, FieldKind.TEXT, value);
    }

    public RecordValues withFiller(FieldSpec field, byte[] value) {
        return with(field, FieldKind.FILLER, value.clone());
    }

    private Object value(FieldSpec field, FieldKind kind) {
        check(field, kind);
        return values[field.index()];
    }

    private RecordValues with(FieldSpec field, FieldKind kind, Object value) {
        check(field, kind);
        Object[] copy = values.clone();
        copy[field.index()] = value;
        return new RecordValues(layout, copy);
    }

    private void check(FieldSpec field, FieldKind kind) {
        if (!layout.owns(field) || field.kind() != kind) {
            throw new IllegalArgumentException("field " + field.name() + " is not a " + kind + " field of " + layout.copybook());
        }
    }
}
