package com.carddemo.interest.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * The fields of one fixed-length record and the codec of the data-map rules E1-E5 on its bytes.
 * A value that breaks a rule rejects the record; nothing is substituted, padded over or cut. Every
 * rejection names the file, the record number and the field.
 */
public final class RecordLayout {

    private final String copybook;
    private final String recordName;
    private final List<LayoutField> fields;
    private final int length;

    RecordLayout(String copybook, String recordName, List<LayoutField> fields) {
        this.copybook = copybook;
        this.recordName = recordName;
        this.fields = List.copyOf(fields);
        this.length = fields.get(fields.size() - 1).end();
    }

    public String copybook() {
        return copybook;
    }

    public String recordName() {
        return recordName;
    }

    public List<LayoutField> fields() {
        return fields;
    }

    /** Record length in bytes: the sum of the field lengths. */
    public int length() {
        return length;
    }

    public LayoutField field(String name) {
        for (LayoutField field : fields) {
            if (field.name().equals(name)) {
                return field;
            }
        }
        throw new IllegalArgumentException("copybook " + copybook + " has no field " + name);
    }

    /** Decodes every field of the record. */
    public DecodedRecord decode(byte[] record, CodePage codePage, RecordSource source) {
        check(record, source);
        List<Object> values = new ArrayList<>(fields.size());
        for (LayoutField field : fields) {
            values.add(decodeField(record, field, codePage, source));
        }
        return new DecodedRecord(this, values);
    }

    /** Encodes every field; the result has exactly {@link #length()} bytes. */
    public byte[] encode(DecodedRecord decoded, CodePage codePage, RecordSource source) {
        byte[] record = new byte[length];
        for (LayoutField field : fields) {
            requireHandled(field, source);
            encodeField(record, field, decoded.value(field.name()), codePage, source);
        }
        return record;
    }

    /** A new record: numbers zero, text and FILLER spaces of the code page (E4, G05). */
    public DecodedRecord blank(CodePage codePage) {
        List<Object> values = new ArrayList<>(fields.size());
        for (LayoutField field : fields) {
            if (field.numeric()) {
                values.add(BigDecimal.ZERO.setScale(field.scale()));
            } else if (field.kind() == FieldKind.FILLER) {
                byte[] spaces = new byte[field.length()];
                Arrays.fill(spaces, codePage.space());
                values.add(spaces);
            } else {
                values.add("");
            }
        }
        return new DecodedRecord(this, values);
    }

    /** Decodes one field; the other bytes of the record are not looked at. */
    public Object read(byte[] record, String fieldName, CodePage codePage, RecordSource source) {
        check(record, source);
        return decodeField(record, field(fieldName), codePage, source);
    }

    /** A copy of the record with the bytes of one field replaced; every other byte stays as it is (INT-T06). */
    public byte[] replace(byte[] record, String fieldName, Object value, CodePage codePage, RecordSource source) {
        check(record, source);
        byte[] updated = record.clone();
        encodeField(updated, field(fieldName), value, codePage, source);
        return updated;
    }

    private void check(byte[] record, RecordSource source) {
        for (LayoutField field : fields) {
            requireHandled(field, source);
        }
        if (record.length != length) {
            throw rejected(source, "(record)", "record is " + record.length + " bytes, " + copybook + " is " + length);
        }
    }

    private void requireHandled(LayoutField field, RecordSource source) {
        if (field.kind() == FieldKind.REFUSED) {
            throw rejected(source, field.name(), field.refusal());
        }
    }

    private Object decodeField(byte[] record, LayoutField field, CodePage codePage, RecordSource source) {
        switch (field.kind()) {
            case FILLER:
                return Arrays.copyOfRange(record, field.offset(), field.end());
            case TEXT:
                String text = codePage.text(record, field.offset(), field.length());
                if (text == null) {
                    throw rejected(source, field.name(), "a byte is not a character of the code page " + codePage + " (E3, G07)");
                }
                return text;
            case UNSIGNED_ZONED:
            case SIGNED_ZONED:
                return decodeNumber(record, field, codePage, source);
            default:
                throw rejected(source, field.name(), field.refusal());
        }
    }

    private BigDecimal decodeNumber(byte[] record, LayoutField field, CodePage codePage, RecordSource source) {
        boolean signed = field.kind() == FieldKind.SIGNED_ZONED;
        BigInteger unscaled = BigInteger.ZERO;
        boolean minus = false;
        for (int i = field.offset(); i < field.end(); i++) {
            boolean signByte = signed && i == field.end() - 1;
            int digit = signByte ? codePage.signedDigit(record[i]) : codePage.digit(record[i]);
            if (digit < 0) {
                throw rejected(source, field.name(), String.format("not numeric: byte X'%02X' at position %d of the field (%s)",
                        record[i] & 0xFF, i - field.offset() + 1, signed ? "E2" : "E1"));
            }
            if (digit > 9) {
                minus = true;
                digit -= 10;
            }
            unscaled = unscaled.multiply(BigInteger.TEN).add(BigInteger.valueOf(digit));
        }
        return new BigDecimal(minus ? unscaled.negate() : unscaled, field.scale());
    }

    private void encodeField(byte[] record, LayoutField field, Object value, CodePage codePage, RecordSource source) {
        switch (field.kind()) {
            case FILLER:
                byte[] raw = (byte[]) value;
                if (raw.length != field.length()) {
                    throw rejected(source, field.name(), "FILLER is " + raw.length + " bytes, the field is " + field.length());
                }
                System.arraycopy(raw, 0, record, field.offset(), field.length());
                return;
            case TEXT:
                String text = (String) value;
                byte[] bytes = codePage.bytes(text);
                if (bytes == null) {
                    throw rejected(source, field.name(), "a character is not in the code page " + codePage + " (E3)");
                }
                if (bytes.length > field.length()) {
                    throw rejected(source, field.name(), "value of " + bytes.length + " characters is longer than the field");
                }
                Arrays.fill(record, field.offset(), field.end(), codePage.space());
                System.arraycopy(bytes, 0, record, field.offset(), bytes.length);
                return;
            case UNSIGNED_ZONED:
            case SIGNED_ZONED:
                encodeNumber(record, field, (BigDecimal) value, codePage, source);
                return;
            default:
                throw rejected(source, field.name(), field.refusal());
        }
    }

    private void encodeNumber(byte[] record, LayoutField field, BigDecimal value, CodePage codePage, RecordSource source) {
        boolean signed = field.kind() == FieldKind.SIGNED_ZONED;
        BigInteger unscaled;
        try {
            // exact: a value with more decimals than the field is refused, not rounded
            unscaled = value.setScale(field.scale(), java.math.RoundingMode.UNNECESSARY).unscaledValue();
        } catch (ArithmeticException e) {
            throw rejected(source, field.name(), "value has more than " + field.scale() + " decimals");
        }
        String digits = unscaled.abs().toString();
        if (digits.length() > field.length() || (!signed && unscaled.signum() < 0)) {
            throw rejected(source, field.name(), "value does not fit the field");
        }
        int last = field.end() - 1;
        Arrays.fill(record, field.offset(), field.end(), codePage.digitByte(0));
        for (int i = 0; i < digits.length(); i++) {
            record[last - i] = codePage.digitByte(digits.charAt(digits.length() - 1 - i) - '0');
        }
        if (signed) {
            // E2: C for zero or plus, D for minus; minus zero has signum 0 and comes back plus (G03)
            record[last] = codePage.signedDigitByte(codePage.digit(record[last]), unscaled.signum() < 0);
        }
    }

    static InterestRunStoppedException rejected(RecordSource source, String fieldName, String why) {
        return new InterestRunStoppedException(StopReason.RECORD_REJECTED,
                source.describe() + ", field " + fieldName + ": " + why);
    }
}
