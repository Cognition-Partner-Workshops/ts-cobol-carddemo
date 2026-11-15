package com.carddemo.interest.recon;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.load.ExtractFormat;

/**
 * The records of one file, as bytes and decoded, or the reason the file could not be read. The kit
 * only reads: a record that does not decode makes every figure of the file NOT AVAILABLE.
 */
public record RecordFiles(String fileName, RecordLayout layout, List<byte[]> sources, List<DecodedRecord> records, String failure) {

    public static RecordFiles read(Path file, RecordLayout layout, ExtractFormat format) {
        String name = file.getFileName().toString();
        if (!Files.isRegularFile(file)) {
            return failed(name, layout, "no file " + name);
        }
        try {
            List<byte[]> sources = new ArrayList<>();
            if (format == ExtractFormat.FIXTURE) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
                for (int i = 0; i < lines.size(); i++) {
                    sources.add(RecordImage.bytes(RecordImage.fromLine(lines.get(i), layout, new RecordSource(name, i + 1L))));
                }
            } else {
                sources.addAll(List.of(RecordImage.hostRecords(Files.readAllBytes(file), layout, name)));
            }
            CodePage codePage = format.codePage();
            List<DecodedRecord> records = new ArrayList<>();
            for (int i = 0; i < sources.size(); i++) {
                records.add(layout.decode(sources.get(i), codePage, new RecordSource(name, i + 1L)));
            }
            return new RecordFiles(name, layout, List.copyOf(sources), List.copyOf(records), null);
        } catch (IOException | RuntimeException e) {
            return failed(name, layout, name + " cannot be read: " + e.getMessage());
        }
    }

    static RecordFiles failed(String name, RecordLayout layout, String why) {
        return new RecordFiles(name, layout, List.of(), List.of(), why);
    }

    public boolean available() {
        return failure == null;
    }

    public Figure count() {
        return available() ? Figure.of((long) records.size()) : Figure.missing(failure);
    }

    public Figure sum(String fieldName) {
        return available() ? Figure.of(total(fieldName)) : Figure.missing(failure);
    }

    BigDecimal total(String fieldName) {
        BigDecimal sum = BigDecimal.ZERO.setScale(layout.field(fieldName).scale());
        for (DecodedRecord record : records) {
            sum = sum.add(record.number(fieldName));
        }
        return sum;
    }
}
