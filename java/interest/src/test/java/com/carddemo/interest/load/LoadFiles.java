package com.carddemo.interest.load;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;

/** Builds input directories for the loader tests from the recorded inputs; the fixtures are only read. */
final class LoadFiles {

    private LoadFiles() {
    }

    /** A copy of a case's four input files in the fixture format. */
    static Path copyOf(String caseName, Path target) {
        try {
            Files.createDirectories(target);
            for (ExtractFile file : ExtractFile.values()) {
                String name = file.fileName(ExtractFormat.FIXTURE);
                Files.copy(Fixtures.input(caseName).resolve(name), target.resolve(name));
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Rewrites the lines of one file of a directory made by {@link #copyOf}. */
    static void edit(Path dir, ExtractFile file, UnaryOperator<List<String>> change) {
        Path path = dir.resolve(file.fileName(ExtractFormat.FIXTURE));
        List<String> lines = change.apply(new ArrayList<>(Fixtures.lines(path)));
        try {
            Files.writeString(path, String.join("\n", lines) + "\n", StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The line with the characters from offset on replaced by the text. */
    static String overlay(String line, int offset, String text) {
        String padded = line + " ".repeat(Math.max(0, offset + text.length() - line.length()));
        return padded.substring(0, offset) + text + padded.substring(offset + text.length());
    }

    /** A case's four input files in the host format: fixed-length cp037 records, C/D signs, no separator. */
    static Path hostCopyOf(String caseName, Path target) {
        try {
            Files.createDirectories(target);
            for (ExtractFile file : ExtractFile.values()) {
                RecordLayout layout = file.table().layout();
                List<String> lines = Fixtures.lines(Fixtures.input(caseName).resolve(file.fileName(ExtractFormat.FIXTURE)));
                byte[] content = new byte[lines.size() * layout.length()];
                for (int i = 0; i < lines.size(); i++) {
                    RecordSource source = new RecordSource(file.fileName(ExtractFormat.FIXTURE), i + 1L);
                    byte[] fixture = RecordImage.bytes(RecordImage.fromLine(lines.get(i), layout, source));
                    byte[] host = layout.encode(layout.decode(fixture, CodePage.FIXTURE, source), CodePage.host(), source);
                    System.arraycopy(host, 0, content, i * layout.length(), layout.length());
                }
                Files.write(target.resolve(file.fileName(ExtractFormat.HOST)), content);
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
