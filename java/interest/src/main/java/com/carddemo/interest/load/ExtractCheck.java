package com.carddemo.interest.load;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.InterestRunStoppedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The boundary of the load (standards S3.5, strategy items TL3 and RC3): every record of a file is
 * decoded by the codec and encoded again. A record the codec refuses, a record whose image does not
 * come back byte for byte for a reason other than gaps G02 and G03, and a second record with a key
 * already seen go to the reject list; nothing is substituted. Encodings stay in the codec: the two
 * gap cases are told apart by asking the codec whether the sign byte reads as a plain digit.
 */
final class ExtractCheck {

    /** The codec names file, record number and field in this form (codec/RecordCodec, RecordSource). */
    private static final Pattern CODEC_REJECTION = Pattern.compile("file .+ record \\d+ field (\\S+): (.*)$", Pattern.DOTALL);
    private static final RecordLayout ONE_DIGIT = oneDigitLayout();

    private final CodePage codePage;
    private final RecordCodec oneDigit;

    ExtractCheck(CodePage codePage) {
        this.codePage = codePage;
        this.oneDigit = new RecordCodec(ONE_DIGIT, codePage);
    }

    private static RecordLayout oneDigitLayout() {
        RecordLayout.Builder builder = RecordLayout.builder("sign byte read as PIC 9(01)");
        builder.field("DIGIT", "9(01)");
        return builder.build();
    }

    CheckedExtract check(ExtractFile file, String fileName, List<byte[]> records) {
        RecordCodec codec = new RecordCodec(file.layout(), codePage);
        List<RecordValues> rows = new ArrayList<>();
        List<byte[]> encodedRows = new ArrayList<>();
        List<LoadReject> rejects = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        long g02Differences = 0;
        long g03Differences = 0;
        long nonBlankFillers = 0;
        for (int i = 0; i < records.size(); i++) {
            RecordSource source = new RecordSource(fileName, i + 1);
            byte[] record = records.get(i);
            RecordValues values;
            byte[] encoded;
            try {
                values = codec.decode(record, source);
                encoded = codec.encode(values, source);
            } catch (InterestRunStoppedException e) {
                rejects.add(codecReject(source, e));
                continue;
            }
            boolean roundTrips = true;
            for (FieldSpec field : file.layout().fields()) {
                if (sameBytes(record, encoded, field)) {
                    continue;
                }
                if (field.kind() != FieldKind.SIGNED_ZONED || !sameBytes(record, encoded, field.offset(), field.end() - 1)) {
                    rejects.add(new LoadReject(fileName, source.recordNumber(), field.name(),
                            "encoded again the field differs from the record (RC3)"));
                    roundTrips = false;
                } else if (isPlainDigit(record[field.end() - 1])) {
                    g02Differences++;
                } else {
                    g03Differences++;
                }
            }
            if (!roundTrips) {
                continue;
            }
            FieldSpec lastKeyField = file.keyFields().get(file.keyFields().size() - 1);
            if (!keys.add(HexFormat.of().formatHex(record, 0, lastKeyField.end()))) {
                rejects.add(new LoadReject(fileName, source.recordNumber(), file.keyFields().get(0).name(),
                        "the key is already in the file (RC4)"));
                continue;
            }
            nonBlankFillers += countNonBlankFillers(file, record);
            rows.add(values);
            encodedRows.add(encoded);
        }
        return new CheckedExtract(file, rows, encodedRows, g02Differences, g03Differences, nonBlankFillers, rejects);
    }

    /** G02: the last byte of a signed field is a digit without a sign. Otherwise the difference is the sign of a zero (G03). */
    private boolean isPlainDigit(byte signByte) {
        try {
            oneDigit.decode(new byte[] {signByte}, new RecordSource("sign byte", 1));
            return true;
        } catch (InterestRunStoppedException notADigit) {
            return false;
        }
    }

    private long countNonBlankFillers(ExtractFile file, byte[] record) {
        long count = 0;
        for (FieldSpec field : file.layout().fields()) {
            if (field.kind() != FieldKind.FILLER) {
                continue;
            }
            for (int i = field.offset(); i < field.end(); i++) {
                if (record[i] != codePage.space()) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static LoadReject codecReject(RecordSource source, InterestRunStoppedException rejection) {
        Matcher matcher = CODEC_REJECTION.matcher(rejection.getMessage());
        if (matcher.find()) {
            return new LoadReject(source.file(), source.recordNumber(), matcher.group(1), matcher.group(2));
        }
        return new LoadReject(source.file(), source.recordNumber(), "(record)", rejection.reason().name());
    }

    private static boolean sameBytes(byte[] a, byte[] b, FieldSpec field) {
        return sameBytes(a, b, field.offset(), field.end());
    }

    private static boolean sameBytes(byte[] a, byte[] b, int from, int to) {
        for (int i = from; i < to; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}
