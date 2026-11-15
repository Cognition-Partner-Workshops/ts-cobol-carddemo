package com.carddemo.interest.codec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The values of one record, by copybook field name: a BigDecimal for a zoned number, the full-width
 * String for text, the raw bytes for FILLER. Immutable; {@link #with} returns a changed copy.
 */
public final class DecodedRecord {

    private final RecordLayout layout;
    private final List<Object> values;

    DecodedRecord(RecordLayout layout, List<Object> values) {
        this.layout = layout;
        this.values = values;
    }

    public RecordLayout layout() {
        return layout;
    }

    Object value(String fieldName) {
        return values.get(layout.fields().indexOf(layout.field(fieldName)));
    }

    /** E1, E2: the number with the scale of the field. */
    public BigDecimal number(String fieldName) {
        return (BigDecimal) value(fieldName);
    }

    /** E3: the text without its trailing padding, as a COBOL comparison sees it. */
    public String text(String fieldName) {
        return ((String) value(fieldName)).stripTrailing();
    }

    /** E4: a copy of the raw bytes. */
    public byte[] filler(String fieldName) {
        return ((byte[]) value(fieldName)).clone();
    }

    public DecodedRecord with(String fieldName, BigDecimal number) {
        return replaced(fieldName, number, layout.field(fieldName).numeric());
    }

    public DecodedRecord with(String fieldName, String text) {
        return replaced(fieldName, text, layout.field(fieldName).kind() == FieldKind.TEXT);
    }

    private DecodedRecord replaced(String fieldName, Object value, boolean rightKind) {
        if (!rightKind) {
            throw new IllegalArgumentException("field " + fieldName + " of " + layout.copybook() + " does not hold this kind of value");
        }
        List<Object> changed = new ArrayList<>(values);
        changed.set(layout.fields().indexOf(layout.field(fieldName)), value);
        return new DecodedRecord(layout, changed);
    }
}
