package com.carddemo.interest.load;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carddemo.interest.codec.CodePage.SignedDigit;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.InterestRunStoppedException;

/**
 * Decodes one unloaded file with the record codec (standards S3.5: the encoding rules stay there)
 * and proves what the load relies on before a table is touched: every record decodes (TL3), every
 * row encoded again is its source record except for the sign byte of gaps G02 and G03 (RC3), and
 * no key occurs twice (RC4). What fails is added to the reject list; nothing is substituted.
 */
public final class ExtractDecoder {

    /** The codec names the field in the text of its exception and nowhere else. */
    private static final Pattern FIELD_OF_REJECTION = Pattern.compile(" field (\\S+): (.*)$");
    private static final Pattern RECORD_REJECTION = Pattern.compile(" record \\d+ (record .*)$");

    private final RecordFormat format;

    public ExtractDecoder(RecordFormat format) {
        this.format = format;
    }

    /** The decoded file, or empty when a record was added to the reject list. */
    public Optional<DecodedExtract> decode(ExtractFile file, byte[] content, List<RejectedRecord> rejects) {
        List<byte[]> records;
        try {
            records = format.records(content, file.layout(), file.fileName());
        } catch (InterestRunStoppedException e) {
            rejects.add(rejected(file, recordNumberOf(e), e));
            return Optional.empty();
        }
        RecordCodec codec = format.codec(file.layout());
        int rejectsBefore = rejects.size();
        List<RecordValues> rows = new ArrayList<>();
        Set<List<Object>> keys = new HashSet<>();
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        for (FieldLayout field : summedFields(file)) {
            sums.put(file.column(field), BigDecimal.ZERO.setScale(field.scale()));
        }
        long fSigned = 0;
        long minusZero = 0;
        for (int number = 1; number <= records.size(); number++) {
            byte[] record = records.get(number - 1);
            RecordOrigin origin = new RecordOrigin(file.fileName(), number);
            RecordValues values;
            byte[] encodedAgain;
            try {
                values = codec.decode(record, origin);
                encodedAgain = codec.encode(values, origin);
            } catch (InterestRunStoppedException e) {
                rejects.add(rejected(file, number, e));
                continue;
            }
            boolean isRoundTrip = true;
            for (FieldLayout field : file.layout().fields()) {
                for (int at = field.offset(); at < field.end(); at++) {
                    if (record[at] == encodedAgain[at]) {
                        continue;
                    }
                    Optional<SignedDigit> source = field.kind() == Kind.SIGNED && at == field.end() - 1
                            ? codec.codePage().signedDigitOf(record[at]) : Optional.empty();
                    if (source.isPresent() && source.get().negative()) {
                        minusZero++;
                    } else if (source.isPresent()) {
                        fSigned++;
                    } else {
                        rejects.add(new RejectedRecord(file.fileName(), number, field.name(),
                                "RC3: the row encoded again is not the source record"));
                        isRoundTrip = false;
                    }
                    break;
                }
            }
            if (!isRoundTrip) {
                continue;
            }
            if (!keys.add(key(file, values))) {
                rejects.add(new RejectedRecord(file.fileName(), number, String.join("+", file.keyFields()),
                        "RC4: the key is in an earlier record of the file"));
                continue;
            }
            for (FieldLayout field : summedFields(file)) {
                // sums of values of one scale: exact, no rounding (standards S3.3)
                sums.merge(file.column(field), amount(values, field), BigDecimal::add);
            }
            rows.add(values);
        }
        if (rejects.size() > rejectsBefore) {
            return Optional.empty();
        }
        return Optional.of(new DecodedExtract(file, rows, new LoadFigures(rows.size(), sums), fSigned, minusZero));
    }

    /** RC2: every amount of the file and the account id that is its hash total. */
    static List<FieldLayout> summedFields(ExtractFile file) {
        return file.layout().fields().stream()
                .filter(field -> field.kind() == Kind.SIGNED || field.name().equals(file.hashTotalField()))
                .toList();
    }

    static List<Object> key(ExtractFile file, RecordValues values) {
        return file.keyFields().stream()
                .<Object>map(name -> file.layout().field(name).kind() == Kind.UNSIGNED
                        ? (Object) values.unsigned(name) : values.text(name))
                .toList();
    }

    private static BigDecimal amount(RecordValues values, FieldLayout field) {
        return field.kind() == Kind.SIGNED ? values.signed(field.name()) : BigDecimal.valueOf(values.unsigned(field.name()));
    }

    private static RejectedRecord rejected(ExtractFile file, int recordNumber, InterestRunStoppedException e) {
        Matcher field = FIELD_OF_REJECTION.matcher(e.getMessage());
        if (field.find()) {
            return new RejectedRecord(file.fileName(), recordNumber, field.group(1), field.group(2));
        }
        Matcher record = RECORD_REJECTION.matcher(e.getMessage());
        return new RejectedRecord(file.fileName(), recordNumber, RejectedRecord.WHOLE_RECORD,
                record.find() ? record.group(1) : e.reason().name());
    }

    private static int recordNumberOf(InterestRunStoppedException e) {
        Matcher number = Pattern.compile(" record (\\d+) ").matcher(e.getMessage());
        return number.find() ? Integer.parseInt(number.group(1)) : 0;
    }
}
