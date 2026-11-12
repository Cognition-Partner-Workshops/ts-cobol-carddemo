package com.carddemo.modern.posting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.carddemo.modern.codec.RecordLayout;

/**
 * Writes a candidate run in the layout {@code tools/parity/record.py} produces, so
 * {@code tools/parity/compare.py --actual <dir>} can compare it with the golden files:
 * {@code datasets/<DSN>.txt} (one record per line, trailing spaces stripped),
 * {@code manifest.json}, {@code sysout/<JOB>.<STEP>.txt} and {@code rc.json}.
 */
final class RecordedRun {

    private final Path out;
    private final Map<String, Map<String, Object>> manifest = new TreeMap<>();
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
            Files.createDirectories(out.resolve("datasets"));
            Files.createDirectories(out.resolve("sysout"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void dataset(String dsn, RecordLayout layout, List<? extends Map<String, ?>> records) {
        List<String> lines = new ArrayList<>(records.size());
        for (Map<String, ?> record : records) {
            lines.add(layout.encodeLine(record));
        }
        write(out.resolve("datasets").resolve(dsn + ".txt"), lines.isEmpty() ? "" : String.join("\n", lines) + "\n");
        Map<String, Object> meta = new TreeMap<>();
        meta.put("layout", layout.copybook());
        meta.put("lrecl", layout.length());
        meta.put("records", records.size());
        manifest.put(dsn, meta);
        write(out.resolve("manifest.json"), PythonJson.dumps(manifest));
    }

    void sysout(String job, String step, List<String> lines) {
        write(out.resolve("sysout").resolve(job + "." + step + ".txt"),
                lines.isEmpty() ? "" : String.join("\n", lines) + "\n");
    }

    void rawDataset(String dsn, String layout, int lrecl, List<String> records) {
        List<String> lines = records.stream().map(String::stripTrailing).toList();
        write(out.resolve("datasets").resolve(dsn + ".txt"), lines.isEmpty() ? "" : String.join("\n",lines) + "\n");
        Map<String, Object> meta = new TreeMap<>();
        meta.put("layout",layout);
        meta.put("lrecl",lrecl);
        meta.put("records",records.size());
        manifest.put(dsn,meta);
        write(out.resolve("manifest.json"),PythonJson.dumps(manifest));
    }

    /** Job return codes; {@code steps} uses the same short step names the JCL runner records. */
    void returnCodes(String job, int maxrc, Map<String, Integer> steps) {
        Map<String, Object> entry = new TreeMap<>();
        entry.put("maxrc", maxrc);
        entry.put("steps", new TreeMap<>(steps));
        rcs.put(job, entry);
        write(out.resolve("rc.json"), PythonJson.dumps(rcs));
    }

    private static void write(Path path, String content) {
        try {
            Files.writeString(path, content, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
