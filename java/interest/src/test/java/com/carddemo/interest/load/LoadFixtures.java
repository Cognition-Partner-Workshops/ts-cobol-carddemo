package com.carddemo.interest.load;

import com.carddemo.interest.Fixtures;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Synthetic variants of a recorded input directory, written to a temporary directory (standards S7.2, S8.5). */
final class LoadFixtures {

    static final List<String> CASES = List.of("i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped",
            "i04-default-group-fallback", "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding");

    private LoadFixtures() {
    }

    static String fileName(ExtractFile file) {
        return file.fileName(ExtractFormat.FIXTURE);
    }

    static List<String> lines(String caseName, ExtractFile file) {
        return Fixtures.lines(Fixtures.input(caseName).resolve(fileName(file)));
    }

    /** A copy of the four files of a case in {@code target}, with one line of one file changed. */
    static Path copyWithLine(String caseName, Path target, ExtractFile changedFile, int recordNumber, UnaryOperator<String> change) {
        try {
            Files.createDirectories(target);
            for (ExtractFile file : ExtractFile.values()) {
                List<String> lines = new ArrayList<>(lines(caseName, file));
                if (file == changedFile) {
                    lines.set(recordNumber - 1, change.apply(lines.get(recordNumber - 1)));
                }
                Files.write(target.resolve(fileName(file)), lines, StandardCharsets.ISO_8859_1);
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A copy with one more line at the end of one file. */
    static Path copyWithExtraLine(String caseName, Path target, ExtractFile changedFile, String extraLine) {
        Path directory = copyWithLine(caseName, target, changedFile, 1, line -> line);
        try {
            List<String> lines = new ArrayList<>(lines(caseName, changedFile));
            lines.add(extraLine);
            Files.write(directory.resolve(fileName(changedFile)), lines, StandardCharsets.ISO_8859_1);
            return directory;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String withCharAt(String line, int offset, char replacement) {
        return line.substring(0, offset) + replacement + line.substring(offset + 1);
    }
}
