package com.carddemo.interest.codec;

import java.math.BigDecimal;
import java.util.Arrays;

import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;

/**
 * The values of the fields of one record, in the order of its layout: a long for PIC 9 (E1),
 * a BigDecimal with the scale of the picture for PIC S9 (E2), a String of the full field width for
 * PIC X (E3) and the raw bytes for FILLER (E4). Immutable; {@link #with} gives a changed copy.
 */
public final class RecordValues {

    private final RecordLayout layout;
    private final Object[] values;

    RecordValues(RecordLayout layout, Object[] values) {
        this.layout = layout;
        this.values = values;
    }

    /** A new record: zero in every numeric field, spaces in every text field and FILLER (G05). */
    public static RecordValues blank(RecordLayout layout) {
        Object[] values = new Object[layout.fields().size()];
        for (int i = 0; i < values.length; i++) {
            FieldLayout field = layout.fields().get(i);
            values[i] = switch (field.kind()) {
                case UNSIGNED -> 0L;
                case SIGNED -> BigDecimal.ZERO.setScale(field.scale());
                case TEXT -> " ".repeat(field.length());
                // null: the codec writes the spaces of its own code page
                case FILLER, REFUSED -> null;
            };
        }
        return new RecordValues(layout, values);
    }

    public RecordLayout layout() {
        return layout;
    }

    Object value(int index) {
        Object value = values[index];
        return value instanceof byte[] bytes ? bytes.clone() : value;
    }

    /** E1. */
    public long unsigned(String field) {
        return (Long) typed(field, Kind.UNSIGNED);
    }

    /** E2: the value with the scale of the picture. */
    public BigDecimal signed(String field) {
        return (BigDecimal) typed(field, Kind.SIGNED);
    }

    /** E3: the text without its trailing padding. */
    public String text(String field) {
        return ((String) typed(field, Kind.TEXT)).stripTrailing();
    }

    /** E4: the raw bytes of a FILLER, by its position in the layout; null in a new record. */
    public byte[] filler(int index) {
        if (layout.fields().get(index).kind() != Kind.FILLER) {
            throw new IllegalArgumentException("field " + index + " of " + layout.copybook() + " is not a FILLER");
        }
        return (byte[]) value(index);
    }

    public RecordValues with(String field, long value) {
        return replaced(field, Kind.UNSIGNED, value);
    }

    public RecordValues with(String field, BigDecimal value) {
        return replaced(field, Kind.SIGNED, value);
    }

    public RecordValues with(String field, String value) {
        return replaced(field, Kind.TEXT, value);
    }

    private Object typed(String field, Kind kind) {
        int index = layout.indexOf(field);
        requireKind(index, kind);
        return values[index];
    }

    private RecordValues replaced(String field, Kind kind, Object value) {
        int index = layout.indexOf(field);
        requireKind(index, kind);
        Object[] changed = Arrays.copyOf(values, values.length);
        changed[index] = value;
        return new RecordValues(layout, changed);
    }

    private void requireKind(int index, Kind kind) {
        FieldLayout field = layout.fields().get(index);
        if (field.kind() != kind) {
            throw new IllegalArgumentException(
                    "field " + field.name() + " of " + layout.copybook() + " is " + field.kind() + ", not " + kind);
        }
    }
}
