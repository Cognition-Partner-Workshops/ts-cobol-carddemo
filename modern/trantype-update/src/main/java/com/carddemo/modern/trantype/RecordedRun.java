package com.carddemo.modern.trantype;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes a candidate run in the layout {@code tools/parity/record.py} produces, so
 * {@code tools/parity/compare.py --actual <dir>} can compare it with the golden files:
 * {@code sysout/<JOB>.<STEP>.txt}, {@code rc.json} and {@code db/<table>.csv}
 * (same pattern as modern/interest RecordedRun; this chain has no output datasets).
 */
final class RecordedRun {

    private final Path out;
    private final Map<String, Object> rcs = new TreeMap<>();

    RecordedRun(Path out) {
        this.out = out;
        try {
            if (Files.exists(out)) {
                try (var walk = Files.walk(out)) {
                    for (Path p : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                        Files.delete(p);
                    }
                }
            }
            Files.createDirectories(out.resolve("sysout"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void sysout(String job, String step, List<String> lines) {
        write(out.resolve("sysout").resolve(job + "." + step + ".txt"),
                (lines.isEmpty() ? "" : String.join("\n", lines) + "\n").getBytes(StandardCharsets.ISO_8859_1));
    }

    /** Job return codes; {@code steps} uses the same short step names the JCL runner records. */
    void returnCodes(String job, int maxrc, Map<String, Integer> steps) {
        Map<String, Object> entry = new TreeMap<>();
        entry.put("maxrc", maxrc);
        entry.put("steps", new TreeMap<>(steps));
        rcs.put(job, entry);
        write(out.resolve("rc.json"), PythonJson.dumps(rcs).getBytes(StandardCharsets.ISO_8859_1));
    }

    void db(String table, byte[] csv) {
        try {
            Files.createDirectories(out.resolve("db"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        write(out.resolve("db").resolve(table + ".csv"), csv);
    }

    private static void write(Path path, byte[] content) {
        try {
            Files.write(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
