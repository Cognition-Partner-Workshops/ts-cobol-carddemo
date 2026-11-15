package com.carddemo.interest.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * Decodes and encodes the records of one layout in one code page by the encoding rules of the data
 * map (E1-E5). A byte or a value that breaks a rule rejects the record, naming file, record number
 * and field; nothing is substituted, padded over or cut (standards S3.5). The rejection never holds
 * a field value or a record image (standards S6.5).
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

    /** Record length of the layout; refuses a layout with a field that is not USAGE DISPLAY (E5). */
    public int recordLength(RecordSource source) {
        refuseNotDisplay(source);
        return layout.length();
    }

    public RecordValues decode(byte[] record, RecordSource source) {
        checkLength(record, source);
        RecordValues values = RecordValues.of(layout);
        for (FieldSpec field : layout.fields()) {
            values = switch (field.kind()) {
                case UNSIGNED_ZONED -> values.withUnsigned(field, decodeUnsigned(record, field, source));
                case SIGNED_ZONED -> values.withSigned(field, decodeSigned(record, field, source));
                case TEXT -> values.withText(field, decodeText(record, field, source));
                case FILLER -> values.withFiller(field, Arrays.copyOfRange(record, field.offset(), field.end()));
                case NOT_DISPLAY -> throw notDisplay(field, source);
            };
        }
        return values;
    }

    public byte[] encode(RecordValues values, RecordSource source) {
        refuseNotDisplay(source);
        if (values.layout() != layout) {
            throw new IllegalArgumentException("values of " + values.layout().copybook() + " given to the codec of " + layout.copybook());
        }
        byte[] record = new byte[layout.length()];
        for (FieldSpec field : layout.fields()) {
            switch (field.kind()) {
                case UNSIGNED_ZONED -> encodeUnsigned(record, field, values.unsigned(field), source);
                case SIGNED_ZONED -> encodeSigned(record, field, values.signed(field), source);
                case TEXT -> encodeText(record, field, values.text(field), source);
                case FILLER -> encodeFiller(record, field, values.filler(field), source);
                case NOT_DISPLAY -> throw notDisplay(field, source);
            }
        }
        return record;
    }

    /** A copy of the record with one signed field rewritten and every other byte untouched (standards S3.6). */
    public byte[] withSigned(byte[] record, FieldSpec field, BigDecimal value, RecordSource source) {
        checkLength(record, source);
        byte[] updated = record.clone();
        encodeSigned(updated, owned(field, FieldKind.SIGNED_ZONED), value, source);
        return updated;
    }

    /** One field of a record without decoding the others. */
    public long unsigned(byte[] record, FieldSpec field, RecordSource source) {
        checkLength(record, source);
        return decodeUnsigned(record, owned(field, FieldKind.UNSIGNED_ZONED), source);
    }

    private FieldSpec owned(FieldSpec field, FieldKind kind) {
        if (!layout.owns(field) || field.kind() != kind) {
            throw new IllegalArgumentException("field " + field.name() + " is not a " + kind + " field of " + layout.copybook());
        }
        return field;
    }

    private void refuseNotDisplay(RecordSource source) {
        for (FieldSpec field : layout.fields()) {
            if (field.kind() == FieldKind.NOT_DISPLAY) {
                throw notDisplay(field, source);
            }
        }
    }

    private InterestRunStoppedException notDisplay(FieldSpec field, RecordSource source) {
        return rejected(source, field, "USAGE " + field.usage() + " is not DISPLAY; the field is refused, not guessed (E5)");
    }

    private void checkLength(byte[] record, RecordSource source) {
        refuseNotDisplay(source);
        if (record.length == layout.length()) {
            return;
        }
        for (FieldSpec field : layout.fields()) {
            if (record.length < field.end()) {
                throw rejected(source, field, "record of " + record.length + " bytes ends before the end of the field; "
                        + layout.copybook() + " records have " + layout.length() + " bytes");
            }
        }
        FieldSpec last = layout.fields().get(layout.fields().size() - 1);
        throw rejected(source, last, "record of " + record.length + " bytes goes on after the last field; "
                + layout.copybook() + " records have " + layout.length() + " bytes");
    }

    /** E1: n digits. */
    private long decodeUnsigned(byte[] record, FieldSpec field, RecordSource source) {
        long value = 0;
        for (int i = field.offset(); i < field.end(); i++) {
            int digit = codePage.digit(record[i]);
            if (digit < 0) {
                throw rejected(source, field, "not numeric (E1)");
            }
            value = value * 10 + digit;
        }
        return value;
    }

    /** E2: sign and last digit share the last byte; a plain last digit is positive (G02). */
    private BigDecimal decodeSigned(byte[] record, FieldSpec field, RecordSource source) {
        long unscaled = 0;
        for (int i = field.offset(); i < field.end() - 1; i++) {
            int digit = codePage.digit(record[i]);
            if (digit < 0) {
                throw rejected(source, field, "not numeric (E2)");
            }
            unscaled = unscaled * 10 + digit;
        }
        CodePage.SignedDigit last = codePage.signedDigit(record[field.end() - 1])
                .orElseThrow(() -> rejected(source, field, "not numeric (E2)"));
        unscaled = unscaled * 10 + last.digit();
        return BigDecimal.valueOf(last.negative() ? -unscaled : unscaled, field.scale());
    }

    /** E3: the characters without the trailing padding. */
    private String decodeText(byte[] record, FieldSpec field, RecordSource source) {
        String text = codePage.text(record, field.offset(), field.length())
                .orElseThrow(() -> rejected(source, field, "a byte is not a character of code page "
                        + codePage.charset().name() + " (E3, G07)"));
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == ' ') {
            end--;
        }
        return text.substring(0, end);
    }

    /** E1: zero-padded to the field width. */
    private void encodeUnsigned(byte[] record, FieldSpec field, long value, RecordSource source) {
        if (value < 0) {
            throw rejected(source, field, "value does not fit the field (E1)");
        }
        putDigits(record, field, BigInteger.valueOf(value), false, false, source);
    }

    /** E2: C-signed for zero or positive, D-signed for negative; minus zero comes back plus (G03). */
    private void encodeSigned(byte[] record, FieldSpec field, BigDecimal value, RecordSource source) {
        BigInteger unscaled;
        try {
            unscaled = value.setScale(field.scale(), RoundingMode.UNNECESSARY).unscaledValue();
        } catch (ArithmeticException e) {
            throw rejected(source, field, "value has more decimals than the field (E2)");
        }
        putDigits(record, field, unscaled.abs(), true, unscaled.signum() < 0, source);
    }

    private void putDigits(byte[] record, FieldSpec field, BigInteger magnitude, boolean signed, boolean negative,
            RecordSource source) {
        String digits = magnitude.toString();
        if (digits.length() > field.length()) {
            throw rejected(source, field, "value does not fit the field");
        }
        int pad = field.length() - digits.length();
        for (int i = 0; i < field.length(); i++) {
            int digit = i < pad ? 0 : digits.charAt(i - pad) - '0';
            boolean last = i == field.length() - 1;
            record[field.offset() + i] = signed && last ? codePage.signedDigitByte(digit, negative) : codePage.digitByte(digit);
        }
    }

    /** E3: padded with spaces to the field width. */
    private void encodeText(byte[] record, FieldSpec field, String value, RecordSource source) {
        if (value.length() > field.length()) {
            throw rejected(source, field, "value longer than the field (E3)");
        }
        byte[] bytes = codePage.bytes(value)
                .orElseThrow(() -> rejected(source, field, "a character is not in code page "
                        + codePage.charset().name() + " (E3, G07)"));
        Arrays.fill(record, field.offset(), field.end(), codePage.space());
        System.arraycopy(bytes, 0, record, field.offset(), bytes.length);
    }

    /** E4: the raw bytes back; a FILLER never set is spaces of the output code page (G05). */
    private void encodeFiller(byte[] record, FieldSpec field, byte[] raw, RecordSource source) {
        if (raw == null) {
            Arrays.fill(record, field.offset(), field.end(), codePage.space());
        } else if (raw.length != field.length()) {
            throw rejected(source, field, "FILLER of " + raw.length + " bytes, not " + field.length() + " (E4)");
        } else {
            System.arraycopy(raw, 0, record, field.offset(), raw.length);
        }
    }

    private static InterestRunStoppedException rejected(RecordSource source, FieldSpec field, String why) {
        return new InterestRunStoppedException(StopReason.RECORD_REJECTED, source + " field " + field.name() + ": " + why);
    }
}
