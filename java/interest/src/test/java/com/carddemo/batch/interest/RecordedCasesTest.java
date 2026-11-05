package com.carddemo.batch.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs the Java job on every recorded INTCALC case and requires every output file (datasets,
 * SYSOUT, rc.json, manifest) to be byte-identical to what the legacy COBOL produced:
 * the seven golden cases under fixtures/interest and the characterization cases recorded from
 * the same COBOL under java/interest/characterization/cases/interest.
 */
class RecordedCasesTest {
    private static final Path ROOT = Path.of(System.getProperty("carddemo.root", "../..")).toAbsolutePath().normalize();

    static Stream<Path> cases() throws IOException {
        List<Path> cases = new ArrayList<>();
        for (Path dir : List.of(ROOT.resolve("fixtures/interest"),
                                ROOT.resolve("java/interest/characterization/cases/interest"))) {
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(p -> Files.isDirectory(p.resolve("expected"))).sorted().forEach(cases::add);
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void outputMatchesLegacyRecording(Path caseDir, @TempDir Path out) throws IOException {
        InterestJob.Result result = InterestJob.run(InterestJob.readInputs(caseDir.resolve("input")),
            InterestJob.DEFAULT_PARM, "2022-07-05 10:00:00.00");
        InterestJob.write(result, out);
        Path expected = caseDir.resolve("expected");
        List<Path> files;
        try (Stream<Path> s = Files.walk(expected)) {
            files = s.filter(Files::isRegularFile).sorted().toList();
        }
        assertFalse(files.isEmpty());
        for (Path want : files) {
            Path rel = expected.relativize(want);
            Path got = out.resolve(rel);
            assertEquals(read(want), Files.exists(got) ? read(got) : "<missing>", caseDir.getFileName() + "/" + rel);
        }
    }

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.ISO_8859_1);
    }
}
