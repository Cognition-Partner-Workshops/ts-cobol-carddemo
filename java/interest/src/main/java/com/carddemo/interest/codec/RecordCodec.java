package com.carddemo.interest.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

import com.carddemo.interest.codec.CodePage.SignedDigit;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * Decodes and encodes the records of one layout in one code page, by the encoding rules of the
 * data map: E1 unsigned zoned, E2 signed zoned with the sign in the last byte, E3 text, E4 FILLER
 * as raw bytes, E5 everything else refused. This is the only place these rules are written
 * (standards S3.5, S3.6). A value that breaks a rule rejects the record with an error naming the
 * file, the record number and the field; nothing is substituted, padded over or cut.
 */
public final class RecordCodec {

    private final RecordLayout layout;
    private final CodePage codePage;

    public RecordCodec(RecordLayout layout, CodePage codePage) {
        this.layout = layout;
        this.codePage = codePage;
    }

    public RecordLayout layout() {
        return layout;
    }

    public CodePage codePage() {
        return codePage;
    }

    public RecordValues decode(byte[] record, RecordOrigin origin) {
        check(record.length, origin);
        Object[] values = new Object[layout.fields().size()];
        for (int i = 0; i < values.length; i++) {
            FieldLayout field = layout.fields().get(i);
            values[i] = switch (field.kind()) {
                case UNSIGNED -> unsigned(record, field, origin);
                case SIGNED -> signed(record, field, origin);
                case TEXT -> codePage.text(record, field.offset(), field.length())
                        .orElseThrow(() -> rejected(origin, field,
                                "holds a byte that is not a character of the code page " + codePage.name()));
                case FILLER -> Arrays.copyOfRange(record, field.offset(), field.end());
                case REFUSED -> throw rejected(origin, field, field.refusal());
            };
        }
        return new RecordValues(layout, values);
    }

    public byte[] encode(RecordValues values, RecordOrigin origin) {
        if (!values.layout().equals(layout)) {
            throw new IllegalArgumentException(
                    "values of " + values.layout().copybook() + " given to the codec of " + layout.copybook());
        }
        check(layout.length(), origin);
        byte[] record = new byte[layout.length()];
        for (int i = 0; i < layout.fields().size(); i++) {
            put(record, layout.fields().get(i), values.value(i), origin);
        }
        return record;
    }

    /** A copy of the record with the bytes of one field replaced and every other byte untouched. */
    public byte[] withField(byte[] record, String fieldName, BigDecimal value, RecordOrigin origin) {
        check(record.length, origin);
        FieldLayout field = layout.field(fieldName);
        if (field.kind() != Kind.SIGNED) {
            throw new IllegalArgumentException("field " + fieldName + " is " + field.kind() + ", not SIGNED");
        }
        byte[] changed = record.clone();
        put(changed, field, value, origin);
        return changed;
    }

    /** E5 first, so that a refused field is named even when the record length cannot be known. */
    private void check(int recordLength, RecordOrigin origin) {
        for (FieldLayout field : layout.fields()) {
            if (field.kind() == Kind.REFUSED) {
                throw rejected(origin, field, field.refusal());
            }
        }
        if (recordLength != layout.length()) {
            throw new InterestRunStoppedException(StopReason.RECORD_REJECTED, origin + " record " + layout.copybook()
                    + ": the record is " + recordLength + " bytes, the layout is " + layout.length());
        }
    }

    private long unsigned(byte[] record, FieldLayout field, RecordOrigin origin) {
        long value = 0;
        for (int i = field.offset(); i < field.end(); i++) {
            int digit = codePage.digitOf(record[i]);
            if (digit < 0) {
                throw rejected(origin, field, "is not numeric");
            }
            value = value * 10 + digit;
        }
        return value;
    }

    private BigDecimal signed(byte[] record, FieldLayout field, RecordOrigin origin) {
        long unscaled = 0;
        for (int i = field.offset(); i < field.end() - 1; i++) {
            int digit = codePage.digitOf(record[i]);
            if (digit < 0) {
                throw rejected(origin, field, "is not numeric");
            }
            unscaled = unscaled * 10 + digit;
        }
        Optional<SignedDigit> last = codePage.signedDigitOf(record[field.end() - 1]);
        if (last.isEmpty()) {
            throw rejected(origin, field, "is not numeric");
        }
        unscaled = unscaled * 10 + last.get().digit();
        // G03: minus zero has no value of its own and is zero
        return BigDecimal.valueOf(last.get().negative() ? -unscaled : unscaled, field.scale());
    }

    private void put(byte[] record, FieldLayout field, Object value, RecordOrigin origin) {
        switch (field.kind()) {
            case UNSIGNED -> {
                long number = (Long) value;
                if (number < 0) {
                    throw rejected(origin, field, "is unsigned and the value is negative");
                }
                putDigits(record, field, BigInteger.valueOf(number), origin);
            }
            case SIGNED -> {
                BigDecimal number = (BigDecimal) value;
                BigDecimal scaled;
                try {
                    scaled = number.setScale(field.scale());
                } catch (ArithmeticException e) {
                    throw rejected(origin, field, "has " + field.scale() + " decimals and the value has more");
                }
                putDigits(record, field, scaled.unscaledValue().abs(), origin);
                int lastDigit = codePage.digitOf(record[field.end() - 1]);
                record[field.end() - 1] = codePage.signedDigit(lastDigit, scaled.signum() < 0);
            }
            case TEXT -> {
                String text = (String) value;
                byte[] bytes = codePage.bytes(text).orElseThrow(() -> rejected(origin, field,
                        "holds a character that is not in the code page " + codePage.name()));
                if (bytes.length > field.length()) {
                    throw rejected(origin, field, "is " + field.length() + " bytes and the value has " + bytes.length);
                }
                Arrays.fill(record, field.offset(), field.end(), codePage.space());
                System.arraycopy(bytes, 0, record, field.offset(), bytes.length);
            }
            case FILLER -> {
                byte[] raw = (byte[]) value;
                if (raw == null) {
                    Arrays.fill(record, field.offset(), field.end(), codePage.space());
                } else if (raw.length != field.length()) {
                    throw rejected(origin, field, "is " + field.length() + " bytes and the value has " + raw.length);
                } else {
                    System.arraycopy(raw, 0, record, field.offset(), raw.length);
                }
            }
            case REFUSED -> throw rejected(origin, field, field.refusal());
        }
    }

    private void putDigits(byte[] record, FieldLayout field, BigInteger magnitude, RecordOrigin origin) {
        String digits = magnitude.toString();
        if (digits.length() > field.length()) {
            throw rejected(origin, field, "is " + field.length() + " digits and the value does not fit");
        }
        Arrays.fill(record, field.offset(), field.end(), codePage.digit(0));
        int start = field.end() - digits.length();
        for (int i = 0; i < digits.length(); i++) {
            record[start + i] = codePage.digit(digits.charAt(i) - '0');
        }
    }

    private static InterestRunStoppedException rejected(RecordOrigin origin, FieldLayout field, String why) {
        return new InterestRunStoppedException(StopReason.RECORD_REJECTED,
                origin + " field " + field.name() + ": " + why);
    }
}
