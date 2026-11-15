package com.carddemo.interest.recon;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.ExtractFormat;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The file side of the reconciliation: the figures of one unloaded file, computed again from the
 * file itself with the record codec (the loader's own figures are not reused, so the report is a
 * second count). A file that cannot be read, or holds a record the codec refuses, has no figures;
 * the report then shows NOT AVAILABLE for every line that needs them.
 */
final class ExtractSide {

    /** The figures of one file. Keys are the key fields as they stand in the record. */
    record Figures(long records, Map<String, BigDecimal> sums, String firstKey, String lastKey, long g02Differences,
            long g03Differences, long nonBlankFillers, List<RecordValues> rows, List<byte[]> encodedRows) {
    }

    private final Path inputDir;
    private final ExtractFormat format;
    private final Map<ExtractFile, String> unavailable = new LinkedHashMap<>();

    ExtractSide(Path inputDir, ExtractFormat format) {
        this.inputDir = inputDir;
        this.format = format;
    }

    /** Why a file gave no figures, per file, for the report. */
    Map<ExtractFile, String> unavailable() {
        return unavailable;
    }

    Optional<Figures> figures(ExtractFile file) {
        String fileName = file.fileName(format);
        List<byte[]> records;
        try {
            records = records(file, Files.readAllBytes(inputDir.resolve(fileName)));
        } catch (IOException e) {
            unavailable.put(file, "file " + fileName + " cannot be read");
            return Optional.empty();
        }
        CodePage codePage = format.codePage();
        RecordCodec codec = new RecordCodec(file.layout(), codePage);
        List<RecordValues> rows = new ArrayList<>();
        List<byte[]> encodedRows = new ArrayList<>();
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
            } catch (InterestRunStoppedException rejected) {
                unavailable.put(file, "file " + fileName + " record " + (i + 1) + " cannot be decoded");
                return Optional.empty();
            }
            for (FieldSpec field : file.layout().fields()) {
                if (Arrays.equals(record, field.offset(), field.end(), encoded, field.offset(), field.end())) {
                    if (field.kind() == FieldKind.FILLER && !isBlank(record, field, codePage)) {
                        nonBlankFillers++;
                    }
                } else if (field.kind() != FieldKind.SIGNED_ZONED) {
                    unavailable.put(file, "file " + fileName + " record " + (i + 1) + " field " + field.name()
                            + " does not survive the round trip");
                    return Optional.empty();
                } else if (values.signed(field).signum() == 0) {
                    // The value is zero and only its sign byte changed: minus zero (G03).
                    g03Differences++;
                } else {
                    // Same value, other sign byte: an F-signed (unsigned) positive (G02).
                    g02Differences++;
                }
            }
            rows.add(values);
            encodedRows.add(encoded);
        }
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        file.summedFields().forEach(field -> sums.put(file.column(field),
                rows.stream().map(row -> row.signed(field)).reduce(BigDecimal.ZERO, BigDecimal::add)));
        file.hashTotalField().ifPresent(field -> sums.put(file.column(field),
                rows.stream().map(row -> BigDecimal.valueOf(row.unsigned(field))).reduce(BigDecimal.ZERO, BigDecimal::add)));
        String firstKey = rows.isEmpty() ? "" : keyOf(file, rows.get(0));
        String lastKey = rows.isEmpty() ? "" : keyOf(file, rows.get(rows.size() - 1));
        return Optional.of(new Figures(rows.size(), sums, firstKey, lastKey, g02Differences, g03Differences,
                nonBlankFillers, rows, encodedRows));
    }

    private List<byte[]> records(ExtractFile file, byte[] content) {
        int recordLength = file.layout().length();
        if (format == ExtractFormat.HOST) {
            return RecordImage.fromBlock(content, recordLength);
        }
        return new String(content, format.codePage().charset()).lines()
                .map(line -> RecordImage.fromLine(line, recordLength, format.codePage()))
                .toList();
    }

    private static boolean isBlank(byte[] record, FieldSpec field, CodePage codePage) {
        for (int i = field.offset(); i < field.end(); i++) {
            if (record[i] != codePage.space()) {
                return false;
            }
        }
        return true;
    }

    /** The key of a row as text: its key fields in record order, numbers with their leading zeros. */
    static String keyOf(ExtractFile file, RecordValues row) {
        StringBuilder key = new StringBuilder();
        for (FieldSpec field : file.keyFields()) {
            if (key.length() > 0) {
                key.append('/');
            }
            key.append(field.kind() == FieldKind.UNSIGNED_ZONED
                    ? String.format("%0" + field.length() + "d", row.unsigned(field))
                    : row.text(field));
        }
        return key.toString();
    }
}
