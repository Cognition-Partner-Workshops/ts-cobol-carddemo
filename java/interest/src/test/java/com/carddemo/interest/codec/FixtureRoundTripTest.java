package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.Fixtures;

/**
 * Acceptance of ticket 03 on every record of every file under fixtures/interest/&#42;/input and
 * &#42;/expected/datasets (FI-2, read-only): decode then encode returns the identical bytes, and the
 * same records in cp037 decode to the same values.
 *
 * <p>The cp037 records are made here with the JDK's IBM037 charset on the whole record, not with
 * the codec. That is a valid host image because every byte of the fixtures is a printable ASCII
 * character and cp037 holds { A-I at X'C0'-X'C9', } J-R at X'D0'-X'D9' and 0-9 at X'F0'-X'F9'.
 * The record counts are written by hand from {@code wc -l} over the fixture files.
 */
class FixtureRoundTripTest {

    private static final Charset CP037 = Charset.forName("IBM037");

    /** File name -> layout. TRANCATG and TRANTYPE are not read by CBACT04C; their copybooks are read from app/cpy. */
    private static final Map<String, RecordLayout> LAYOUT_OF_FILE = Map.of(
            "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", CardDemoLayouts.ACCOUNT,
            "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt", CardDemoLayouts.ACCOUNT,
            "AWS.M2.CARDDEMO.CARDXREF.PS.txt", CardDemoLayouts.CARD_XREF,
            "AWS.M2.CARDDEMO.TCATBALF.PS.txt", CardDemoLayouts.CATEGORY_BALANCE,
            "AWS.M2.CARDDEMO.DISCGRP.PS.txt", CardDemoLayouts.DISCLOSURE_GROUP,
            "AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt", CardDemoLayouts.TRANSACTION,
            "AWS.M2.CARDDEMO.TRANCATG.PS.txt", CopybookTest.estateCopybook("CVTRA04Y"),
            "AWS.M2.CARDDEMO.TRANTYPE.PS.txt", CopybookTest.estateCopybook("CVTRA03Y"));

    /** wc -l over fixtures/interest/&#42;/input/&lt;file&gt; and &#42;/expected/datasets/&lt;file&gt;, 7 cases. */
    private static final Map<String, Integer> RECORDS_OF_FILE = Map.of(
            "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", 10,
            "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt", 10,
            "AWS.M2.CARDDEMO.CARDXREF.PS.txt", 10,
            "AWS.M2.CARDDEMO.TCATBALF.PS.txt", 12,
            "AWS.M2.CARDDEMO.DISCGRP.PS.txt", 105,
            "AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt", 11,
            "AWS.M2.CARDDEMO.TRANCATG.PS.txt", 35,
            "AWS.M2.CARDDEMO.TRANTYPE.PS.txt", 35);

    /** Every dataset file of every case: input/*.txt and expected/datasets/*.txt. */
    private static List<Path> fixtureFiles() {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> cases = Files.list(Fixtures.INTEREST)) {
            for (Path caseDir : cases.filter(Files::isDirectory).sorted().toList()) {
                for (Path dir : List.of(caseDir.resolve("input"), caseDir.resolve("expected").resolve("datasets"))) {
                    try (Stream<Path> entries = Files.list(dir)) {
                        files.addAll(entries.filter(path -> path.getFileName().toString().endsWith(".txt")).sorted().toList());
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    /** The lines of a fixture file as bytes, split at the line feed only. */
    private static List<byte[]> lines(Path file) throws IOException {
        byte[] content = Files.readAllBytes(file);
        List<byte[]> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < content.length; i++) {
            if (content[i] == '\n') {
                lines.add(java.util.Arrays.copyOfRange(content, start, i));
                start = i + 1;
            }
        }
        assertEquals(content.length, start, file + " ends with a line feed");
        return lines;
    }

    @Test
    void everyFixtureRecordRoundTripsByteForByte_fixtureCodePage() throws IOException {
        Map<String, Integer> roundTripped = new TreeMap<>();
        List<String> different = new ArrayList<>();
        List<Path> files = fixtureFiles();
        for (Path file : files) {
            String name = file.getFileName().toString();
            RecordLayout layout = LAYOUT_OF_FILE.get(name);
            assertTrue(layout != null, "no layout for " + file);
            long recordNumber = 0;
            for (byte[] line : lines(file)) {
                RecordSource source = new RecordSource(file.toString(), ++recordNumber);
                String lineText = new String(line, StandardCharsets.ISO_8859_1);
                byte[] record = RecordImage.bytes(RecordImage.fromLine(lineText, layout, source));

                byte[] encoded = layout.encode(layout.decode(record, CodePage.FIXTURE, source), CodePage.FIXTURE, source);
                byte[] lineAgain = RecordImage.bytes(RecordImage.toLine(RecordImage.image(encoded)));

                if (!java.util.Arrays.equals(record, encoded) || !java.util.Arrays.equals(line, lineAgain)) {
                    different.add(source.describe() + ": " + Bytes.hex(record) + " came back as " + Bytes.hex(encoded));
                }
                roundTripped.merge(name, 1, Integer::sum);
            }
        }

        System.out.println("ticket 03 round trip, fixture code page: " + files.size() + " files under fixtures/interest");
        roundTripped.forEach((name, count) -> System.out.println("  round-tripped " + count + " records of " + name));
        System.out.println("  total " + roundTripped.values().stream().mapToInt(Integer::intValue).sum()
                + " records, " + different.size() + " differences");
        assertEquals(List.of(), different);
        assertEquals(new TreeMap<>(RECORDS_OF_FILE), roundTripped);
        // 7 cases, 6 input files and 2 recorded datasets each
        assertEquals(56, files.size());
    }

    @Test
    void everyFixtureRecordInCp037DecodesToTheSameValuesAndRoundTrips() throws IOException {
        CodePage host = CodePage.host();
        int records = 0;
        int numbers = 0;
        int signBytes = 0;
        for (Path file : fixtureFiles()) {
            RecordLayout layout = LAYOUT_OF_FILE.get(file.getFileName().toString());
            long recordNumber = 0;
            for (byte[] line : lines(file)) {
                RecordSource source = new RecordSource(file.toString(), ++recordNumber);
                String image = RecordImage.fromLine(new String(line, StandardCharsets.ISO_8859_1), layout, source);
                assertTrue(image.chars().allMatch(c -> c >= 0x20 && c <= 0x7E), "printable ASCII only: " + source.describe());
                byte[] hostRecord = image.getBytes(CP037);

                DecodedRecord fromFixture = layout.decode(RecordImage.bytes(image), CodePage.FIXTURE, source);
                DecodedRecord fromHost = layout.decode(hostRecord, host, source);

                for (LayoutField field : layout.fields()) {
                    String what = source.describe() + ", field " + field.name();
                    switch (field.kind()) {
                        case UNSIGNED_ZONED, SIGNED_ZONED -> {
                            assertEquals(fromFixture.number(field.name()), fromHost.number(field.name()), what);
                            numbers++;
                            if (field.kind() == FieldKind.SIGNED_ZONED) {
                                // the translated sign byte is in X'C0'-X'C9' or X'D0'-X'D9'
                                int sign = hostRecord[field.end() - 1] & 0xF0;
                                assertTrue(sign == 0xC0 || sign == 0xD0, what + " sign zone " + Integer.toHexString(sign));
                                assertTrue((hostRecord[field.end() - 1] & 0x0F) <= 9, what);
                                signBytes++;
                            }
                        }
                        case TEXT -> assertEquals(fromFixture.text(field.name()), fromHost.text(field.name()), what);
                        // FILLER is raw: the host bytes are the translated bytes, not the fixture's
                        default -> assertArrayEquals(
                                java.util.Arrays.copyOfRange(hostRecord, field.offset(), field.end()), fromHost.filler(field.name()), what);
                    }
                }
                assertArrayEquals(hostRecord, layout.encode(fromHost, host, source), source.describe());
                records++;
            }
        }
        System.out.println("ticket 03 cp037: " + records + " records, " + numbers + " numeric fields equal, "
                + signBytes + " sign bytes in X'C0'-X'C9' or X'D0'-X'D9', all re-encoded to the same cp037 bytes");
        assertEquals(228, records);
    }

    @Test
    void valuesOfOneRecordedAccountInBothCodePages() throws IOException {
        // i06-negative-balance, the recorded ACCTDATA record; the values are read off the file by hand
        // (S9(10)V99: the last two of the 12 digits are decimals, so 00000003000} is -300.00):
        // 00000002008 Y 00000003000} 00000050000{ 00000025000{ 2019-03-15 2026-12-31 2026-12-31 00000000000{ 00000003000{ 10115 GOLD
        Path file = Fixtures.expected("i06-negative-balance").resolve("datasets").resolve("AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt");
        RecordSource source = new RecordSource(file.getFileName().toString(), 1);
        String image = RecordImage.fromLine(new String(lines(file).get(0), StandardCharsets.ISO_8859_1), CardDemoLayouts.ACCOUNT, source);

        for (DecodedRecord account : List.of(
                CardDemoLayouts.ACCOUNT.decode(RecordImage.bytes(image), CodePage.FIXTURE, source),
                CardDemoLayouts.ACCOUNT.decode(image.getBytes(CP037), CodePage.host(), source))) {
            assertEquals(new BigDecimal("2008"), account.number("ACCT-ID"));
            assertEquals("Y", account.text("ACCT-ACTIVE-STATUS"));
            assertEquals(new BigDecimal("-300.00"), account.number("ACCT-CURR-BAL"));
            assertEquals(new BigDecimal("5000.00"), account.number("ACCT-CREDIT-LIMIT"));
            assertEquals(new BigDecimal("2500.00"), account.number("ACCT-CASH-CREDIT-LIMIT"));
            assertEquals("2019-03-15", account.text("ACCT-OPEN-DATE"));
            assertEquals("2026-12-31", account.text("ACCT-EXPIRAION-DATE"));
            assertEquals("2026-12-31", account.text("ACCT-REISSUE-DATE"));
            assertEquals(new BigDecimal("0.00"), account.number("ACCT-CURR-CYC-CREDIT"));
            assertEquals(new BigDecimal("300.00"), account.number("ACCT-CURR-CYC-DEBIT"));
            assertEquals("10115", account.text("ACCT-ADDR-ZIP"));
            assertEquals("GOLD", account.text("ACCT-GROUP-ID"));
            assertEquals(178, account.filler("FILLER").length);
        }
    }
}
