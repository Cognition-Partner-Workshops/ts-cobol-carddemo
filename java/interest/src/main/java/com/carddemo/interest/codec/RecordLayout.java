package com.carddemo.interest.codec;

import java.util.ArrayList;
import java.util.List;

/**
 * The fields of one copybook record in storage order, with offsets from 0 and lengths in bytes as
 * in the data map. Group items are not fields. A layout is written once per copybook (the
 * {@code *Record} classes) or parsed from the copybook source ({@link Copybook}); the two are
 * compared by a test, so the copybook stays the authority (FI-1).
 */
public record RecordLayout(String copybook, List<FieldLayout> fields) {

    /** The encoding rule of the data map a field follows. */
    public enum Kind {
        /** E1: PIC 9(n). */
        UNSIGNED,
        /** E2: PIC S9(n)V9(m), sign in the last byte. */
        SIGNED,
        /** E3: PIC X(n). */
        TEXT,
        /** E4: FILLER, raw bytes. */
        FILLER,
        /** E5: USAGE other than DISPLAY, or a description this codec does not know. Refused, never guessed. */
        REFUSED
    }

    /** One field; {@code refusal} says why a REFUSED field is refused and is empty otherwise. */
    public record FieldLayout(String name, Kind kind, int offset, int length, int scale, String refusal) {

        public int end() {
            return offset + length;
        }
    }

    public RecordLayout {
        fields = List.copyOf(fields);
    }

    public static Builder of(String copybook) {
        return new Builder(copybook);
    }

    public int length() {
        return fields.isEmpty() ? 0 : fields.get(fields.size() - 1).end();
    }

    public int indexOf(String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).name().equals(fieldName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("layout " + copybook + " has no field " + fieldName);
    }

    public FieldLayout field(String fieldName) {
        return fields.get(indexOf(fieldName));
    }

    /** Adds fields one after the other; the offset of each is the end of the one before. */
    public static final class Builder {

        private final String copybook;
        private final List<FieldLayout> fields = new ArrayList<>();
        private int offset;

        private Builder(String copybook) {
            this.copybook = copybook;
        }

        /** PIC 9(digits). */
        public Builder unsigned(String name, int digits) {
            return add(name, Kind.UNSIGNED, digits, 0, "");
        }

        /** PIC S9(integerDigits)V9(scale). */
        public Builder signed(String name, int integerDigits, int scale) {
            return add(name, Kind.SIGNED, integerDigits + scale, scale, "");
        }

        /** PIC X(length). */
        public Builder text(String name, int length) {
            return add(name, Kind.TEXT, length, 0, "");
        }

        /** FILLER PIC X(length). */
        public Builder filler(int length) {
            return add(Copybook.FILLER, Kind.FILLER, length, 0, "");
        }

        Builder refused(String name, String refusal) {
            return add(name, Kind.REFUSED, 0, 0, refusal);
        }

        private Builder add(String name, Kind kind, int length, int scale, String refusal) {
            fields.add(new FieldLayout(name, kind, offset, length, scale, refusal));
            offset += length;
            return this;
        }

        public RecordLayout build() {
            return new RecordLayout(copybook, fields);
        }
    }
}
