package com.carddemo.interest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Read-only access to the recorded legacy runs under fixtures/interest (FI-2). Maven runs in java/interest. */
public final class Fixtures {

    public static final Path INTEREST = Path.of("..", "..", "fixtures", "interest");
    public static final String PARM_DATE = "2022071800";
    public static final String FROZEN_CLOCK = "2022-07-05 10:00:00.00";

    private Fixtures() {
    }

    public static Path input(String caseName) {
        return INTEREST.resolve(caseName).resolve("input");
    }

    public static Path expected(String caseName) {
        return INTEREST.resolve(caseName).resolve("expected");
    }

    public static String text(Path file) {
        try {
            return Files.readString(file, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<String> lines(Path file) {
        return text(file).lines().toList();
    }
}
