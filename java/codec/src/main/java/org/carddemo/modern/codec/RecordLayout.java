package org.carddemo.modern.codec;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A fixed-length record layout: decode a line into named values and encode them back. */
public final class RecordLayout {

    private final String copybook;
    private final List<Field> fields;
    private final int length;

    public RecordLayout(String copybook, List<Field> fields) {
        this.copybook = copybook;
        this.fields = List.copyOf(fields);
        this.length = fields.stream().mapToInt(Field::end).max().orElse(0);
    }

    public String copybook() {
        return copybook;
    }

    public List<Field> fields() {
        return fields;
    }

    public int length() {
        return length;
    }

    public Map<String, Object> decode(String line) {
        String record = pad(line, length);
        Map<String, Object> values = new LinkedHashMap<>();
        for (Field f : fields) {
            String raw = record.substring(f.offset(), f.end());
            switch (f.kind()) {
                case TEXT -> values.put(f.name(), raw);
                case ZONED -> values.put(f.name(), Zoned.decode(raw, f.scale(), f.signed()));
                case FILLER -> { }
            }
        }
        return values;
    }

    public String encode(Map<String, ?> values) {
        char[] out = " ".repeat(length).toCharArray();
        for (Field f : fields) {
            String text = switch (f.kind()) {
                case TEXT -> pad(Objects.toString(values.get(f.name()), ""), f.length());
                case ZONED -> Zoned.encode(asDecimal(values.get(f.name())), f.length(), f.scale(), f.signed());
                case FILLER -> " ".repeat(f.length());
            };
            text.getChars(0, f.length(), out, f.offset());
        }
        return new String(out);
    }

    /** The line as the estate's recorder writes it: trailing spaces stripped. */
    public String encodeLine(Map<String, ?> values) {
        return encode(values).stripTrailing();
    }

    private static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal d) {
            return d;
        }
        return new BigDecimal(value.toString().trim());
    }

    static String pad(String s, int len) {
        return s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
    }
}
