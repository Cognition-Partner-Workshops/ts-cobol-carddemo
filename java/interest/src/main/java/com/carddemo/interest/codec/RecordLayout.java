package com.carddemo.interest.codec;

import java.util.ArrayList;
import java.util.List;

/**
 * The elementary fields of one copybook record in declaration order (group items add no bytes).
 * Built either by {@link CopybookParser} from the copybook source or by {@link Builder} with the
 * PICTURE strings of the copybook; {@code CopybookLayoutTest} proves the two agree for the five
 * layouts of the stream (FI-1).
 */
public final class RecordLayout {

    private final String copybook;
    private final List<FieldSpec> fields;
    private final int length;

    private RecordLayout(String copybook, List<FieldSpec> fields) {
        this.copybook = copybook;
        this.fields = List.copyOf(fields);
        this.length = fields.isEmpty() ? 0 : fields.get(fields.size() - 1).end();
    }

    public static Builder builder(String copybook) {
        return new Builder(copybook);
    }

    public String copybook() {
        return copybook;
    }

    public List<FieldSpec> fields() {
        return fields;
    }

    /** Record length in bytes; meaningful only when every field is USAGE DISPLAY. */
    public int length() {
        return length;
    }

    boolean owns(FieldSpec field) {
        return field.index() < fields.size() && fields.get(field.index()).equals(field);
    }

    /** Declares the fields in copybook order; each call returns the field for the mapping that uses it. */
    public static final class Builder {

        private final String copybook;
        private final List<FieldSpec> fields = new ArrayList<>();
        private int offset;

        private Builder(String copybook) {
            this.copybook = copybook;
        }

        public FieldSpec field(String name, String picture) {
            return field(name, picture, FieldSpec.DISPLAY);
        }

        public FieldSpec field(String name, String picture, String usage) {
            FieldSpec field = FieldSpec.of(name, fields.size(), offset, picture, usage);
            fields.add(field);
            offset = field.end();
            return field;
        }

        public FieldSpec filler(String picture) {
            return field(FieldSpec.FILLER, picture);
        }

        public RecordLayout build() {
            return new RecordLayout(copybook, fields);
        }
    }
}
