package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.codec.CodecFixtures.FixtureFile;

/**
 * Acceptance lines 1 and 2 of ticket 03 over every record of every file under
 * fixtures/interest/&lt;case&gt;/input and /expected/datasets. The count per file is printed
 * (lines starting with ROUNDTRIP) as the final-revision evidence the ticket asks for.
 */
class FixtureRoundTripTest {

    private static final int FILES = 56;
    private static final int RECORDS = 228;

    @Test
    void everyFixtureRecordDecodesAndEncodesToTheSameBytes() {
        CodePage fixture = CodePage.fixture();
        List<String> gaps = new ArrayList<>();
        int files = 0;
        int records = 0;
        for (FixtureFile file : CodecFixtures.allFiles()) {
            RecordCodec codec = new RecordCodec(file.layout(), fixture);
            int number = 0;
            for (String line : file.lines()) {
                RecordSource source = new RecordSource(file.name(), ++number);
                byte[] record = RecordImage.fromLine(line, file.layout().length(), fixture);

                byte[] encoded = codec.encode(codec.decode(record, source), source);

                if (!Arrays.equals(record, encoded) || !line.equals(RecordImage.toLine(encoded, fixture))) {
                    gaps.add(source + ": read " + CodecFixtures.hex(record) + " wrote " + CodecFixtures.hex(encoded));
                }
            }
            System.out.printf("ROUNDTRIP fixture %-12s %3d records %s%n", file.layout().copybook(), number, file.name());
            files++;
            records += number;
        }
        System.out.printf("ROUNDTRIP fixture total %d files, %d records, %d not identical%n", files, records, gaps.size());

        assertEquals(List.of(), gaps);
        assertEquals(FILES, files);
        assertEquals(RECORDS, records);
    }

    @Test
    void theSameRecordsInCp037DecodeToTheSameValues() {
        CodePage fixture = CodePage.fixture();
        CodePage host = CodePage.host(CodecFixtures.CP037);
        Set<String> signBytesSeen = new TreeSet<>();
        int records = 0;
        for (FixtureFile file : CodecFixtures.allFiles()) {
            RecordCodec fixtureCodec = new RecordCodec(file.layout(), fixture);
            RecordCodec hostCodec = new RecordCodec(file.layout(), host);
            int number = 0;
            for (String line : file.lines()) {
                RecordSource source = new RecordSource(file.name(), ++number);
                byte[] record = RecordImage.fromLine(line, file.layout().length(), fixture);
                byte[] hostRecord = toCp037(record, file.layout(), signBytesSeen);

                RecordValues fromFixture = fixtureCodec.decode(record, source);
                RecordValues fromHost = hostCodec.decode(hostRecord, source);

                assertEquals(CodecFixtures.values(fromFixture, fixture), CodecFixtures.values(fromHost, host), source.toString());
                assertArrayEquals(hostRecord, hostCodec.encode(fromHost, source), source.toString());
            }
            System.out.printf("ROUNDTRIP cp037   %-12s %3d records %s%n", file.layout().copybook(), number, file.name());
            records += number;
        }
        System.out.printf("ROUNDTRIP cp037   total %d records, sign bytes seen %s%n", records, signBytesSeen);

        assertEquals(RECORDS, records);
        // both signs occur in the fixtures: i06-negative-balance holds the D-signed amounts
        assertTrue(signBytesSeen.contains("C0"), signBytesSeen.toString());
        assertTrue(signBytesSeen.stream().anyMatch(sign -> sign.startsWith("D")), signBytesSeen.toString());
    }

    /**
     * The host image of a fixture record, built field by field and not with the codec under test:
     * text and FILLER through the cp037 table, digits as X'F0'-X'F9', the sign byte as X'C0'-X'C9'
     * (plus) or X'D0'-X'D9' (minus).
     */
    private static byte[] toCp037(byte[] record, RecordLayout layout, Set<String> signBytesSeen) {
        byte[] host = new byte[record.length];
        for (FieldSpec field : layout.fields()) {
            for (int i = field.offset(); i < field.end(); i++) {
                char c = (char) (record[i] & 0xFF);
                boolean signByte = field.kind() == FieldKind.SIGNED_ZONED && i == field.end() - 1;
                if (signByte) {
                    int plus = "{ABCDEFGHI".indexOf(c);
                    int minus = "}JKLMNOPQR".indexOf(c);
                    assertTrue(plus >= 0 || minus >= 0, "fixture sign byte " + c + " of " + field.name());
                    host[i] = (byte) (plus >= 0 ? 0xC0 + plus : 0xD0 + minus);
                    signBytesSeen.add(String.format("%02X", host[i] & 0xFF));
                } else if (field.kind() == FieldKind.TEXT || field.kind() == FieldKind.FILLER) {
                    host[i] = String.valueOf(c).getBytes(CodecFixtures.CP037)[0];
                } else {
                    assertTrue(c >= '0' && c <= '9', "fixture digit " + c + " of " + field.name());
                    host[i] = (byte) (0xF0 + c - '0');
                }
            }
        }
        return host;
    }
}
