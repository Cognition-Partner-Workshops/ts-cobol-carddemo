package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * Encoding rules E1-E4 of the data map as tables, in the fixture code page and in cp037.
 * Expected bytes are written by hand: fixture bytes as text, host bytes in hex.
 */
class RecordCodecTest {

    private static final Charset CP037 = Charset.forName("IBM037");
    private static final RecordOrigin ORIGIN = new RecordOrigin("TEST.PS.txt", 7);

    /** One field of each rule: E1 9(04), E2 S9(09)V99, E3 X(10), E4 FILLER X(3). */
    private static final RecordLayout LAYOUT = RecordLayout.of("TESTCPY")
            .unsigned("CODE", 4)
            .signed("AMOUNT", 9, 2)
            .text("SOURCE", 10)
            .filler(3)
            .build();
    private static final RecordCodec FIXTURE = new RecordCodec(LAYOUT, CodePage.fixture());
    private static final RecordCodec HOST = new RecordCodec(LAYOUT, CodePage.host());

    private record SignedCase(String fixtureBytes, String hostHex, String value) {
    }

    /** { A-I are +0 to +9 (X'C0'-X'C9'), } J-R are -0 to -9 (X'D0'-X'D9') in the last byte. */
    private static final List<SignedCase> E2_ROUND_TRIP = List.of(
            new SignedCase("0000010000{", "F0F0F0F0F0F1F0F0F0F0C0", "1000.00"),
            new SignedCase("0000000300}", "F0F0F0F0F0F0F0F3F0F0D0", "-30.00"),
            new SignedCase("0000010003C", "F0F0F0F0F0F1F0F0F0F3C3", "1000.33"),
            new SignedCase("0000002500P", "F0F0F0F0F0F0F2F5F0F0D7", "-250.07"),
            new SignedCase("0000000001}", "F0F0F0F0F0F0F0F0F0F1D0", "-0.10"),
            new SignedCase("0000000000{", "F0F0F0F0F0F0F0F0F0F0C0", "0.00"),
            new SignedCase("9999999999I", "F9F9F9F9F9F9F9F9F9F9C9", "999999999.99"),
            new SignedCase("9999999999R", "F9F9F9F9F9F9F9F9F9F9D9", "-999999999.99"));

    private static byte[] fixtureRecord(String code, String amount, String source, String filler) {
        return (code + amount + source + filler).getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] hostRecord(String codeHex, String amountHex, String source, String filler) {
        return HexFormat.of().parseHex(codeHex + amountHex
                + HexFormat.of().formatHex(source.getBytes(CP037)) + HexFormat.of().formatHex(filler.getBytes(CP037)));
    }

    @Test
    void e2_signedZonedRoundTripInBothCodePages() {
        for (SignedCase row : E2_ROUND_TRIP) {
            byte[] fixture = fixtureRecord("0005", row.fixtureBytes(), "System    ", "   ");
            byte[] host = hostRecord("F0F0F0F5", row.hostHex(), "System    ", "   ");
            BigDecimal value = new BigDecimal(row.value());

            assertEquals(value, FIXTURE.decode(fixture, ORIGIN).signed("AMOUNT"), row.fixtureBytes());
            assertEquals(value, HOST.decode(host, ORIGIN).signed("AMOUNT"), row.hostHex());
            assertArrayEquals(fixture, FIXTURE.encode(FIXTURE.decode(fixture, ORIGIN), ORIGIN), row.fixtureBytes());
            assertArrayEquals(host, HOST.encode(HOST.decode(host, ORIGIN), ORIGIN), row.hostHex());
            // the values of one code page give the bytes of the other; FILLER (the last 3 bytes) is raw, not translated
            assertArrayEquals(java.util.Arrays.copyOf(host, 25),
                    java.util.Arrays.copyOf(HOST.encode(FIXTURE.decode(fixture, ORIGIN), ORIGIN), 25), row.hostHex());
        }
    }

    @Test
    void e2_fSignedDigitIsPositiveAndComesBackCSigned() {
        // G02: 0.15 with an unsigned last digit (fixture 5, host X'F5') comes back as E / X'C5'
        byte[] fixture = fixtureRecord("0005", "00000000015", "System    ", "   ");
        byte[] host = hostRecord("F0F0F0F5", "F0F0F0F0F0F0F0F0F0F1F5", "System    ", "   ");

        assertEquals(new BigDecimal("0.15"), FIXTURE.decode(fixture, ORIGIN).signed("AMOUNT"));
        assertEquals(new BigDecimal("0.15"), HOST.decode(host, ORIGIN).signed("AMOUNT"));
        assertArrayEquals(fixtureRecord("0005", "0000000001E", "System    ", "   "),
                FIXTURE.encode(FIXTURE.decode(fixture, ORIGIN), ORIGIN));
        assertArrayEquals(hostRecord("F0F0F0F5", "F0F0F0F0F0F0F0F0F0F1C5", "System    ", "   "),
                HOST.encode(HOST.decode(host, ORIGIN), ORIGIN));
    }

    @Test
    void e2_minusZeroComesBackPlusZero() {
        // G03: } / X'D0' in the last byte of an all-zero field
        byte[] fixture = fixtureRecord("0005", "0000000000}", "System    ", "   ");
        byte[] host = hostRecord("F0F0F0F5", "F0F0F0F0F0F0F0F0F0F0D0", "System    ", "   ");

        assertEquals(0, FIXTURE.decode(fixture, ORIGIN).signed("AMOUNT").signum());
        assertArrayEquals(fixtureRecord("0005", "0000000000{", "System    ", "   "),
                FIXTURE.encode(FIXTURE.decode(fixture, ORIGIN), ORIGIN));
        assertArrayEquals(hostRecord("F0F0F0F5", "F0F0F0F0F0F0F0F0F0F0C0", "System    ", "   "),
                HOST.encode(HOST.decode(host, ORIGIN), ORIGIN));
    }

    @Test
    void e2_valueThatDoesNotFitIsRefusedNotCutOrRounded() {
        RecordValues values = RecordValues.blank(LAYOUT);

        // S9(09)V99 holds at most 999999999.99 and two decimals
        assertRejected("AMOUNT", () -> FIXTURE.encode(values.with("AMOUNT", new BigDecimal("1000000000.00")), ORIGIN));
        assertRejected("AMOUNT", () -> FIXTURE.encode(values.with("AMOUNT", new BigDecimal("0.005")), ORIGIN));
        // a value with fewer decimals is the same number
        assertArrayEquals(fixtureRecord("0000", "0000000015{", "          ", "   "),
                FIXTURE.encode(values.with("AMOUNT", new BigDecimal("1.5")), ORIGIN));
    }

    @Test
    void e1_nonDigitRejectsTheRecordNamingFileRecordAndField() {
        // fixture: a letter, a space, a sign byte in a PIC 9 field; a letter or a space in a PIC S9 field
        for (String code : List.of("00A1", "  01", "000{", "000A")) {
            assertRejected("CODE", () -> FIXTURE.decode(fixtureRecord(code, "0000010000{", "System    ", "   "), ORIGIN));
        }
        for (String amount : List.of("00000A0000{", "0000010000 ", "0000010000a", " 000010000{")) {
            assertRejected("AMOUNT", () -> FIXTURE.decode(fixtureRecord("0005", amount, "System    ", "   "), ORIGIN));
        }
        // host: X'C1' (A) and X'40' (space) in a PIC 9 field; a fixture digit X'31' is not a host digit
        for (String codeHex : List.of("F0F0C1F1", "4040F0F1", "F0F0F031")) {
            assertRejected("CODE", () -> HOST.decode(
                    hostRecord(codeHex, "F0F0F0F0F0F1F0F0F0F0C0", "System    ", "   "), ORIGIN));
        }
        assertRejected("AMOUNT", () -> HOST.decode(
                hostRecord("F0F0F0F5", "F0F0F0F0F0F1F0F0F0F040", "System    ", "   "), ORIGIN));
        assertRejected("AMOUNT", () -> HOST.decode(
                hostRecord("F0F0F0F5", "F0F0F0F0F0F1F0F0F0F0E0", "System    ", "   "), ORIGIN));
    }

    @Test
    void e1_unsignedIsZeroPaddedToTheFieldWidth() {
        RecordValues values = RecordValues.blank(LAYOUT).with("CODE", 5L);

        assertEquals(5L, FIXTURE.decode(fixtureRecord("0005", "0000000000{", "          ", "   "), ORIGIN).unsigned("CODE"));
        assertArrayEquals(fixtureRecord("0005", "0000000000{", "          ", "   "), FIXTURE.encode(values, ORIGIN));
        assertArrayEquals(hostRecord("F0F0F0F5", "F0F0F0F0F0F0F0F0F0F0C0", "          ", "   "), HOST.encode(values, ORIGIN));
        assertRejected("CODE", () -> FIXTURE.encode(values.with("CODE", 10000L), ORIGIN));
        assertRejected("CODE", () -> FIXTURE.encode(values.with("CODE", -1L), ORIGIN));
    }

    @Test
    void e3_textIsPaddedAndNeverCut() {
        RecordValues values = RecordValues.blank(LAYOUT).with("SOURCE", "System");

        assertEquals("System", FIXTURE.decode(fixtureRecord("0005", "0000000000{", "System    ", "   "), ORIGIN).text("SOURCE"));
        assertEquals("System", HOST.decode(
                hostRecord("F0F0F0F5", "F0F0F0F0F0F0F0F0F0F0C0", "System    ", "   "), ORIGIN).text("SOURCE"));
        assertArrayEquals(fixtureRecord("0000", "0000000000{", "System    ", "   "), FIXTURE.encode(values, ORIGIN));
        // System in cp037 is E2 A8 A2 A3 85 94, the padding X'40'
        assertEquals("f0f0f0f0" + "f0f0f0f0f0f0f0f0f0f0c0" + "e2a8a2a38594" + "40404040" + "404040",
                HexFormat.of().formatHex(HOST.encode(values, ORIGIN)));
        assertRejected("SOURCE", () -> FIXTURE.encode(values.with("SOURCE", "System text"), ORIGIN));
        // leading and inner spaces are part of the text and come back
        byte[] spaced = fixtureRecord("0005", "0000000000{", " a  b     ", "   ");
        assertArrayEquals(spaced, FIXTURE.encode(FIXTURE.decode(spaced, ORIGIN), ORIGIN));
    }

    @Test
    void e3_byteThatIsNotACharacterRejectsTheRecord() {
        // G07: low-values (X'00') in both code pages; X'9F' is a control in ISO-8859-1, X'3F' in cp037
        byte[] fixture = fixtureRecord("0005", "0000000000{", "Sys\u0000em    ", "   ");
        byte[] fixtureControl = fixtureRecord("0005", "0000000000{", "Sys\u009fem    ", "   ");
        byte[] host = hostRecord("F0F0F0F5", "F0F0F0F0F0F0F0F0F0F0C0", "System    ", "   ");
        byte[] hostLowValue = host.clone();
        hostLowValue[17] = 0x00;
        byte[] hostControl = host.clone();
        hostControl[17] = 0x3F;

        assertRejected("SOURCE", () -> FIXTURE.decode(fixture, ORIGIN));
        assertRejected("SOURCE", () -> FIXTURE.decode(fixtureControl, ORIGIN));
        assertRejected("SOURCE", () -> HOST.decode(hostLowValue, ORIGIN));
        assertRejected("SOURCE", () -> HOST.decode(hostControl, ORIGIN));
        // a character that the code page does not have cannot be written
        assertRejected("SOURCE", () -> FIXTURE.encode(RecordValues.blank(LAYOUT).with("SOURCE", "\u20ac"), ORIGIN));
        assertRejected("SOURCE", () -> HOST.encode(RecordValues.blank(LAYOUT).with("SOURCE", "\u20ac"), ORIGIN));
    }

    @Test
    void e4_fillerIsRawBytesInBothCodePages() {
        // bytes that are no characters and no digits: carried, not translated, not checked
        byte[] fixture = fixtureRecord("0005", "0000000000{", "System    ", "\u0000\u00ff\u0001");
        byte[] host = hostRecord("F0F0F0F5", "F0F0F0F0F0F0F0F0F0F0C0", "System    ", "   ");
        host[25] = 0x00;
        host[26] = (byte) 0xFF;
        host[27] = 0x0C;

        assertArrayEquals(new byte[] {0x00, (byte) 0xFF, 0x01}, FIXTURE.decode(fixture, ORIGIN).filler(3));
        assertArrayEquals(fixture, FIXTURE.encode(FIXTURE.decode(fixture, ORIGIN), ORIGIN));
        assertArrayEquals(host, HOST.encode(HOST.decode(host, ORIGIN), ORIGIN));
    }

    @Test
    void e4_fillerOfANewRecordIsSpacesOfTheOutputCodePage() {
        // G05
        byte[] fixture = FIXTURE.encode(RecordValues.blank(LAYOUT), ORIGIN);
        byte[] host = HOST.encode(RecordValues.blank(LAYOUT), ORIGIN);

        assertEquals("0000" + "0000000000{" + " ".repeat(13), new String(fixture, StandardCharsets.ISO_8859_1));
        assertEquals("f0f0f0f0" + "f0f0f0f0f0f0f0f0f0f0c0" + "40".repeat(13), HexFormat.of().formatHex(host));
    }

    @Test
    void s36_withFieldReplacesOneFieldAndNoOtherByte() {
        // CODE, SOURCE and the raw FILLER stay as they are: only the bytes of AMOUNT are rewritten
        byte[] record = fixtureRecord("0005", "00000000015", "System    ", "\u0000\u00ff\u0001");

        byte[] changed = FIXTURE.withField(record, "AMOUNT", new BigDecimal("-2.50"), ORIGIN);

        assertArrayEquals(fixtureRecord("0005", "0000000025}", "System    ", "\u0000\u00ff\u0001"), changed);
        assertRejected("AMOUNT", () -> FIXTURE.withField(record, "AMOUNT", new BigDecimal("1000000000.00"), ORIGIN));
    }

    @Test
    void wrongRecordLengthRejectsTheRecordNamingFileAndRecord() {
        byte[] record = fixtureRecord("0005", "0000000000{", "System    ", "   ");

        for (int length : List.of(0, record.length - 1, record.length + 1)) {
            InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                    () -> FIXTURE.decode(java.util.Arrays.copyOf(record, length), ORIGIN));
            assertEquals(StopReason.RECORD_REJECTED, stop.reason());
            assertTrue(stop.getMessage().contains("file TEST.PS.txt record 7 record TESTCPY: the record is "
                    + length + " bytes, the layout is 28"), stop.getMessage());
        }
    }

    @Test
    void e5_usageOtherThanDisplayIsRefusedNamingFileRecordAndField() {
        RecordLayout packed = Copybook.parse("PACKED", """
                       01  PACKED-RECORD.
                           05  PACKED-ID            PIC 9(04).
                           05  PACKED-AMT           PIC S9(09)V99 COMP-3.
                           05  FILLER               PIC X(10).
                """);
        RecordCodec codec = new RecordCodec(packed, CodePage.fixture());

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> codec.decode(new byte[20], ORIGIN));

        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS.txt record 7 field PACKED-AMT: "
                + "USAGE COMP-3 is not DISPLAY (rule E5)", stop.getMessage());
        assertRejected("PACKED-AMT", () -> codec.encode(RecordValues.blank(packed), ORIGIN));
    }

    private static void assertRejected(String field, Runnable action) {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class, action::run);
        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        assertTrue(stop.getMessage().contains("file TEST.PS.txt record 7 field " + field + ": "), stop.getMessage());
    }
}
