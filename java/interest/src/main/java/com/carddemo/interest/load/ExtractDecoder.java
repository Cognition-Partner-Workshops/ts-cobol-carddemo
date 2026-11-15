package com.carddemo.interest.load;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * Reads one unloaded file and decodes every record with the record-codec (standards S3.5). A
 * record that breaks an encoding rule, has the wrong length or repeats a key becomes a line of the
 * reject list; nothing is substituted and no record is left out silently (strategy item TL3).
 */
final class ExtractDecoder {

    private static final String WHOLE_RECORD = "(record)";

    private ExtractDecoder() {
    }

    static DecodedExtract decode(Path inputDir, ExtractFile file, ExtractFormat format) {
        String fileName = file.fileName(format);
        RecordLayout layout = file.table().layout();
        CodePage codePage = format.codePage();
        List<Reject> rejects = new ArrayList<>();
        List<byte[]> sources = read(inputDir.resolve(fileName), fileName, layout, format, rejects);
        List<DecodedRecord> records = new ArrayList<>(sources.size());
        Map<String, Long> firstRecordOfKey = new HashMap<>();
        for (int i = 0; i < sources.size(); i++) {
            byte[] record = sources.get(i);
            RecordSource source = new RecordSource(fileName, i + 1L);
            int before = rejects.size();
            for (LayoutField field : layout.fields()) {
                try {
                    layout.read(record, field.name(), codePage, source);
                } catch (InterestRunStoppedException e) {
                    rejects.add(new Reject(fileName, source.recordNumber(), field.name(), reason(e, field.name())));
                }
            }
            if (rejects.size() > before) {
                continue;
            }
            records.add(layout.decode(record, codePage, source));
            Long first = firstRecordOfKey.putIfAbsent(RoundTrip.key(file.table(), record), source.recordNumber());
            if (first != null) {
                rejects.add(new Reject(fileName, source.recordNumber(), String.join("+", file.table().keyFields()),
                        "key already in record " + first + " (RC4)"));
            }
        }
        return new DecodedExtract(file, fileName, sources, rejects.isEmpty() ? records : List.of(), rejects);
    }

    /** The full-length records of the file, in file order. A record of the wrong length is a reject. */
    private static List<byte[]> read(Path path, String fileName, RecordLayout layout, ExtractFormat format, List<Reject> rejects) {
        byte[] content;
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read input file " + fileName, e);
        }
        List<byte[]> records = new ArrayList<>();
        if (format == ExtractFormat.HOST) {
            int whole = content.length / layout.length();
            for (int i = 0; i < whole; i++) {
                records.add(Arrays.copyOfRange(content, i * layout.length(), (i + 1) * layout.length()));
            }
            if (content.length % layout.length() != 0) {
                rejects.add(wrongLength(fileName, whole + 1L, content.length % layout.length(), layout));
            }
            return records;
        }
        String[] lines = RecordImage.image(content).split("\n", -1);
        int count = lines.length > 0 && lines[lines.length - 1].isEmpty() ? lines.length - 1 : lines.length;
        for (int i = 0; i < count; i++) {
            if (lines[i].length() > layout.length()) {
                rejects.add(wrongLength(fileName, i + 1L, lines[i].length(), layout));
            } else {
                records.add(RecordImage.bytes(RecordImage.fromLine(lines[i], layout, new RecordSource(fileName, i + 1L))));
            }
        }
        return records;
    }

    private static Reject wrongLength(String fileName, long recordNumber, int length, RecordLayout layout) {
        return new Reject(fileName, recordNumber, WHOLE_RECORD,
                "record is " + length + " bytes, " + layout.copybook() + " is " + layout.length());
    }

    /** The codec's reason without its "file, record, field" prefix, which the reject line already holds. */
    private static String reason(InterestRunStoppedException e, String fieldName) {
        String marker = "field " + fieldName + ": ";
        int at = e.getMessage().indexOf(marker);
        return at < 0 ? e.getMessage() : e.getMessage().substring(at + marker.length());
    }
}
