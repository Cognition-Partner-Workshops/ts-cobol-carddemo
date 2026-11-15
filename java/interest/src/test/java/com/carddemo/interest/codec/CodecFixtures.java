package com.carddemo.interest.codec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.carddemo.interest.Fixtures;

/** The recorded files of fixtures/interest and the copybook each one is laid out by (tools/runjcl/chains.json). */
final class CodecFixtures {

    static final Path COPYBOOKS = Path.of("..", "..", "app", "cpy");
    static final Charset CP037 = Charset.forName("IBM037");

    /** Data set name part -> copybook. CVTRA03Y and CVTRA04Y are not among the five layouts; INTCALC does not read them. */
    private static final Map<String, String> COPYBOOK_OF = Map.of(
            "ACCTDATA", "CVACT01Y",
            "CARDXREF", "CVACT03Y",
            "TCATBALF", "CVTRA01Y",
            "DISCGRP", "CVTRA02Y",
            "SYSTRAN", "CVTRA05Y",
            "TRANTYPE", "CVTRA03Y",
            "TRANCATG", "CVTRA04Y");

    private CodecFixtures() {
    }

    /** One recorded file: its path under fixtures/interest and its layout, read from the copybook source. */
    record FixtureFile(Path path, RecordLayout layout) {

        String name() {
            return Fixtures.INTEREST.relativize(path).toString();
        }

        List<String> lines() {
            return Fixtures.lines(path);
        }
    }

    static RecordLayout copybook(String name) {
        try {
            return CopybookParser.parse(name, Files.readString(COPYBOOKS.resolve(name + ".cpy"), StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every file under fixtures/interest/&lt;case&gt;/input and /expected/datasets, in path order. */
    static List<FixtureFile> allFiles() {
        List<FixtureFile> files = new ArrayList<>();
        try (Stream<Path> cases = Files.list(Fixtures.INTEREST)) {
            for (Path caseDir : cases.filter(Files::isDirectory).sorted().toList()) {
                for (Path dir : List.of(caseDir.resolve("input"), caseDir.resolve("expected").resolve("datasets"))) {
                    try (Stream<Path> entries = Files.list(dir)) {
                        for (Path file : entries.sorted().toList()) {
                            files.add(new FixtureFile(file, copybook(copybookOf(file))));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    private static String copybookOf(Path file) {
        String name = file.getFileName().toString();
        return COPYBOOK_OF.entrySet().stream()
                .filter(entry -> name.contains("." + entry.getKey() + "."))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no copybook known for fixture file " + file));
    }

    static String hex(byte[] bytes) {
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    static byte[] latin1(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    /** The decoded values in field order; a FILLER is shown as the text its bytes are in the code page. */
    static List<Object> values(RecordValues values, CodePage codePage) {
        List<Object> list = new ArrayList<>();
        for (FieldSpec field : values.layout().fields()) {
            list.add(switch (field.kind()) {
                case UNSIGNED_ZONED -> values.unsigned(field);
                case SIGNED_ZONED -> values.signed(field);
                case TEXT -> values.text(field);
                case FILLER -> codePage.display(values.filler(field));
                case NOT_DISPLAY -> throw new IllegalStateException(field.name());
            });
        }
        return list;
    }
}
