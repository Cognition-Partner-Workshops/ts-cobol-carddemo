package com.carddemo.interest.load;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.jdbc.ReplicaTable;

/**
 * Reconciliation RC3: a loaded row, encoded again, against its source record. A difference is
 * counted under the mapping gap that explains it - G02, a positive amount with an F sign comes back
 * C-signed; G03, minus zero comes back as plus zero - and any other difference fails the load.
 */
public record RoundTrip(long records, long equal, long fSigned, long minusZero, long other) {

    static final RoundTrip NONE = new RoundTrip(0, 0, 0, 0, 0);

    /** fSigned and minusZero count fields, records and equal count records, other counts unexplained fields or rows. */
    RoundTrip plus(RoundTrip more) {
        return new RoundTrip(records + more.records, equal + more.equal, fSigned + more.fSigned,
                minusZero + more.minusZero, other + more.other);
    }

    /** Compares one source record with its re-encoded row; a missing row is an unexplained difference. */
    static RoundTrip compare(RecordLayout layout, byte[] source, byte[] reencoded, CodePage codePage) {
        if (reencoded == null) {
            return new RoundTrip(1, 0, 0, 0, 1);
        }
        if (Arrays.equals(source, reencoded)) {
            return new RoundTrip(1, 1, 0, 0, 0);
        }
        byte[] digits = "0123456789".getBytes(codePage.charset());
        long fSigned = 0;
        long minusZero = 0;
        long other = 0;
        for (LayoutField field : layout.fields()) {
            int last = field.end() - 1;
            if (Arrays.equals(source, field.offset(), field.end(), reencoded, field.offset(), field.end())) {
                continue;
            }
            boolean onlySignDiffers = field.kind() == FieldKind.SIGNED_ZONED
                    && Arrays.equals(source, field.offset(), last, reencoded, field.offset(), last);
            if (!onlySignDiffers) {
                other++;
            } else if (indexOf(digits, source[last]) >= 0) {
                fSigned++;
            } else if (isZero(layout, reencoded, field, codePage)) {
                minusZero++;
            } else {
                other++;
            }
        }
        return new RoundTrip(1, 0, fSigned, minusZero, other);
    }

    /** The key bytes of a record as text, one character per byte: equal keys give equal text. */
    static String key(ReplicaTable table, byte[] record) {
        StringBuilder key = new StringBuilder();
        List<String> keyFields = table.keyFields();
        for (String name : keyFields) {
            LayoutField field = table.layout().field(name);
            key.append(RecordImage.image(Arrays.copyOfRange(record, field.offset(), field.end())));
        }
        return key.toString();
    }

    /** The re-encoded field is plus zero, so a source that differs in the sign byte only held minus zero. */
    private static boolean isZero(RecordLayout layout, byte[] reencoded, LayoutField field, CodePage codePage) {
        byte[] zero = layout.replace(reencoded, field.name(), BigDecimal.ZERO.setScale(field.scale()), codePage,
                RecordSource.built(layout.copybook()));
        return Arrays.equals(zero, reencoded);
    }

    private static int indexOf(byte[] bytes, byte value) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
