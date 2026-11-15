package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;

/**
 * Ticket 03 acceptance: every record of every dataset file under fixtures/interest (input and
 * expected/datasets of every case) decodes and encodes to the identical bytes, and the same record
 * in cp037 decodes to the same values. The count of records per file is printed (ROUND-TRIP lines).
 */
class FixtureRoundTripTest {

    private static final Charset CP037 = Charset.forName("IBM037");

    /** Dataset name to layout: tools/runjcl/chains.json (interest: inputs, outputs) and the DD names of CBACT04C. */
    private static final Map<String, RecordLayout> LAYOUTS = Map.of(
            "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", AccountRecord.LAYOUT,
            "AWS.M2.CARDDEMO.CARDXREF.PS.txt", CardXrefRecord.LAYOUT,
            "AWS.M2.CARDDEMO.TCATBALF.PS.txt", CategoryBalanceRecord.LAYOUT,
            "AWS.M2.CARDDEMO.DISCGRP.PS.txt", DisclosureGroupRecord.LAYOUT,
            "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt", AccountRecord.LAYOUT,
            "AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt", TransactionRecord.LAYOUT);

    /** Files of other layouts than the five: not read by INTCALC (chains.json lists four inputs). */
    private static final List<String> NOT_OF_THE_FIVE_LAYOUTS =
            List.of("AWS.M2.CARDDEMO.TRANCATG.PS.txt", "AWS.M2.CARDDEMO.TRANTYPE.PS.txt");

    private static List<Path> datasetFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> cases = Files.list(Fixtures.INTEREST)) {
            for (Path caseDir : cases.filter(Files::isDirectory).sorted().toList()) {
                for (Path dir : List.of(caseDir.resolve("input"), caseDir.resolve("expected").resolve("datasets"))) {
                    try (Stream<Path> entries = Files.list(dir)) {
                        files.addAll(entries.sorted().toList());
                    }
                }
            }
        }
        return files;
    }

    private static String name(Path file) {
        return Fixtures.INTEREST.relativize(file).toString();
    }

    @Test
    void acceptance_everyFixtureRecordRoundTripsInTheFixtureCodePage() throws IOException {
        RecordFormat format = RecordFormat.fixture();
        TreeSet<String> withoutLayout = new TreeSet<>();
        TreeSet<String> cases = new TreeSet<>();
        List<String> gaps = new ArrayList<>();
        Map<String, Integer> recordsPerLayout = new TreeMap<>();
        int files = 0;
        int total = 0;
        for (Path file : datasetFiles()) {
            RecordLayout layout = LAYOUTS.get(file.getFileName().toString());
            if (layout == null) {
                withoutLayout.add(file.getFileName().toString());
                continue;
            }
            cases.add(Fixtures.INTEREST.relativize(file).getName(0).toString());
            byte[] content = Files.readAllBytes(file);
            List<byte[]> records = format.records(content, layout, name(file));
            RecordCodec codec = format.codec(layout);
            for (int i = 0; i < records.size(); i++) {
                RecordOrigin origin = new RecordOrigin(name(file), i + 1);
                byte[] encoded = codec.encode(codec.decode(records.get(i), origin), origin);
                if (!java.util.Arrays.equals(records.get(i), encoded)) {
                    gaps.add(origin + ": " + HexFormat.of().formatHex(records.get(i)) + " came back as "
                            + HexFormat.of().formatHex(encoded));
                }
            }
            // the file as a whole: line ends and the trailing spaces not written
            assertArrayEquals(content, format.content(records), name(file));
            System.out.println("ROUND-TRIP " + name(file) + " " + layout.copybook() + ": " + records.size() + " records");
            recordsPerLayout.merge(layout.copybook(), records.size(), Integer::sum);
            files++;
            total += records.size();
        }
        System.out.println("ROUND-TRIP total: " + total + " records in " + files + " files of " + cases.size()
                + " cases, per layout " + recordsPerLayout + ", " + gaps.size() + " not identical");

        assertEquals(List.of(), gaps);
        assertEquals(new TreeSet<>(NOT_OF_THE_FIVE_LAYOUTS), withoutLayout);
        assertEquals(7, cases.size());
        assertEquals(5, recordsPerLayout.size());
        assertTrue(total > 0);
    }

    @Test
    void acceptance_theSameRecordsInCp037DecodeToTheSameValues() throws IOException {
        RecordFormat fixture = RecordFormat.fixture();
        int total = 0;
        for (Path file : datasetFiles()) {
            RecordLayout layout = LAYOUTS.get(file.getFileName().toString());
            if (layout == null) {
                continue;
            }
            RecordCodec fixtureCodec = fixture.codec(layout);
            RecordCodec hostCodec = new RecordCodec(layout, CodePage.host(CP037));
            List<byte[]> records = fixture.records(Files.readAllBytes(file), layout, name(file));
            for (int i = 0; i < records.size(); i++) {
                RecordOrigin origin = new RecordOrigin(name(file), i + 1);
                byte[] hostRecord = toCp037(records.get(i), layout);
                RecordValues fixtureValues = fixtureCodec.decode(records.get(i), origin);
                RecordValues hostValues = hostCodec.decode(hostRecord, origin);

                for (int f = 0; f < layout.fields().size(); f++) {
                    FieldLayout field = layout.fields().get(f);
                    switch (field.kind()) {
                        case UNSIGNED -> assertEquals(fixtureValues.unsigned(field.name()), hostValues.unsigned(field.name()),
                                origin + " " + field.name());
                        case SIGNED -> assertEquals(fixtureValues.signed(field.name()), hostValues.signed(field.name()),
                                origin + " " + field.name());
                        case TEXT -> assertEquals(fixtureValues.text(field.name()), hostValues.text(field.name()),
                                origin + " " + field.name());
                        // E4: raw bytes, no translation; the host record holds the bytes it was given
                        case FILLER -> assertArrayEquals(
                                java.util.Arrays.copyOfRange(hostRecord, field.offset(), field.end()), hostValues.filler(f),
                                origin + " " + field.name());
                        case REFUSED -> throw new AssertionError(field.name());
                    }
                }
                assertArrayEquals(hostRecord, hostCodec.encode(hostValues, origin), origin.toString());
                // the fixture values written in the host code page, FILLER apart (raw bytes are not translated)
                int fillerStart = layout.fields().get(layout.fields().size() - 1).offset();
                assertArrayEquals(java.util.Arrays.copyOf(hostRecord, fillerStart),
                        java.util.Arrays.copyOf(hostCodec.encode(fixtureValues, origin), fillerStart), origin.toString());
                total++;
            }
        }
        System.out.println("CP037 total: " + total + " records decoded to the values of the fixture code page");
        assertTrue(total > 0);
    }

    /**
     * The record in cp037, made here without the codec: digits become X'F0'-X'F9', the sign byte
     * { A-I becomes X'C0'-X'C9' and } J-R becomes X'D0'-X'D9', text and FILLER are translated.
     */
    private static byte[] toCp037(byte[] record, RecordLayout layout) {
        byte[] host = new String(record, StandardCharsets.ISO_8859_1).getBytes(CP037);
        for (FieldLayout field : layout.fields()) {
            for (int i = field.offset(); i < field.end(); i++) {
                char c = (char) (record[i] & 0xFF);
                boolean isLastOfSigned = field.kind() == RecordLayout.Kind.SIGNED && i == field.end() - 1;
                if (isLastOfSigned && "{ABCDEFGHI".indexOf(c) >= 0) {
                    host[i] = (byte) (0xC0 + "{ABCDEFGHI".indexOf(c));
                } else if (isLastOfSigned && "}JKLMNOPQR".indexOf(c) >= 0) {
                    host[i] = (byte) (0xD0 + "}JKLMNOPQR".indexOf(c));
                } else if (field.kind() == RecordLayout.Kind.UNSIGNED || field.kind() == RecordLayout.Kind.SIGNED) {
                    host[i] = (byte) (0xF0 + (c - '0'));
                }
            }
        }
        return host;
    }
}
