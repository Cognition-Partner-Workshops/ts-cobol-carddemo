package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * Encoding rules E1 and E2 of the data map (INT-T06), table-driven, in the fixture code page and in
 * cp037. Every expected value and every byte is written by hand: fixture bytes as text, host bytes
 * as hexadecimal (digits X'F0'-X'F9', plus X'C0'-X'C9', minus X'D0'-X'D9').
 */
class ZonedDecimalTest {

    private static final RecordSource SOURCE = new RecordSource("TEST.PS", 7);

    /** One field at offset 0, so that a record is the bytes of the field. */
    private static RecordLayout oneField(String picture) {
        return Copybook.parse("TESTCPY", "       01  TEST-RECORD.\n           05  TEST-FIELD  PIC " + picture + ".\n");
    }

    private static BigDecimal decode(String picture, byte[] bytes, CodePage codePage) {
        return oneField(picture).decode(bytes, codePage, SOURCE).number("TEST-FIELD");
    }

    private static byte[] encode(String picture, String value, CodePage codePage) {
        RecordLayout layout = oneField(picture);
        return layout.encode(layout.blank(codePage).with("TEST-FIELD", new BigDecimal(value)), codePage, SOURCE);
    }

    @ParameterizedTest(name = "{0} {1} = {3}")
    @CsvSource({
            // picture,     fixture bytes,  cp037 bytes,              value
            "S9(09)V99,     0000010000{,    F0F0F0F0F0F1F0F0F0F0C0,   1000.00",
            "S9(09)V99,     0000000300},    F0F0F0F0F0F0F0F3F0F0D0,   -30.00",
            "S9(09)V99,     0000010003C,    F0F0F0F0F0F1F0F0F0F3C3,   1000.33",
            "S9(09)V99,     0000002500P,    F0F0F0F0F0F0F2F5F0F0D7,   -250.07",
            "S9(09)V99,     0000007777G,    F0F0F0F0F0F0F7F7F7F7C7,   777.77",
            "S9(09)V99,     9999999999I,    F9F9F9F9F9F9F9F9F9F9C9,   999999999.99",
            "S9(09)V99,     9999999999R,    F9F9F9F9F9F9F9F9F9F9D9,   -999999999.99",
            "S9(09)V99,     0000000000{,    F0F0F0F0F0F0F0F0F0F0C0,   0.00",
            "S9(09)V99,     0000000000A,    F0F0F0F0F0F0F0F0F0F0C1,   0.01",
            "S9(09)V99,     0000000000J,    F0F0F0F0F0F0F0F0F0F0D1,   -0.01",
            "S9(10)V99,     00000024000{,   F0F0F0F0F0F0F2F4F0F0F0C0, 2400.00",
            "S9(10)V99,     99999999999R,   F9F9F9F9F9F9F9F9F9F9F9D9, -9999999999.99",
            "S9(04)V99,     00120{,         F0F0F1F2F0C0,             12.00",
            "S9(04)V99,     00000{,         F0F0F0F0F0C0,             0.00",
            "S9(04)V99,     99999I,         F9F9F9F9F9C9,             9999.99",
            "S9(04)V99,     00150},         F0F0F1F5F0D0,             -15.00",
            "9(11),         00000002001,    F0F0F0F0F0F0F0F2F0F0F1,   2001",
            "9(11),         99999999999,    F9F9F9F9F9F9F9F9F9F9F9,   99999999999",
            "9(09),         000000000,      F0F0F0F0F0F0F0F0F0,       0",
            "9(04),         0005,           F0F0F0F5,                 5",
            "9(04),         9999,           F9F9F9F9,                 9999"})
    void e1e2_decodesAndEncodesTheSameBytesInBothCodePages(String picture, String fixtureBytes, String hostHex, String value) {
        BigDecimal expected = new BigDecimal(value);

        assertEquals(expected, decode(picture, Bytes.fixture(fixtureBytes), CodePage.FIXTURE));
        assertEquals(expected, decode(picture, Bytes.hex(hostHex), CodePage.host()));
        assertArrayEquals(Bytes.fixture(fixtureBytes), encode(picture, value, CodePage.FIXTURE));
        assertEquals(hostHex, Bytes.hex(encode(picture, value, CodePage.host())));
    }

    @Test
    void intT06_observableValuesOfTheTicket() {
        // 0000010000{ is 1000.00 as S9(09)V99; 0000000300} is -30.00 and comes back as the same 11 bytes
        assertEquals(new BigDecimal("1000.00"), decode("S9(09)V99", Bytes.fixture("0000010000{"), CodePage.FIXTURE));
        assertEquals(new BigDecimal("-30.00"), decode("S9(09)V99", Bytes.fixture("0000000300}"), CodePage.FIXTURE));
        assertEquals("0000000300}", new String(encode("S9(09)V99", "-30.00", CodePage.FIXTURE), FixtureCodePage.CHARSET));
        assertEquals(11, encode("S9(09)V99", "-30.00", CodePage.FIXTURE).length);
    }

    @ParameterizedTest(name = "{0} {1} -> {3}")
    @CsvSource({
            // G02: an F-signed (unsigned) last digit is plus and is written back C-signed
            "S9(09)V99, 00000000015, F0F0F0F0F0F0F0F0F0F1F5, 0000000001E, F0F0F0F0F0F0F0F0F0F1C5, 0.15",
            "S9(09)V99, 00000100000, F0F0F0F0F0F1F0F0F0F0F0, 0000010000{, F0F0F0F0F0F1F0F0F0F0C0, 1000.00",
            "S9(04)V99, 001200,      F0F0F1F2F0F0,           00120{,      F0F0F1F2F0C0,           12.00",
            // G03: minus zero is zero and is written back as plus zero
            "S9(09)V99, 0000000000}, F0F0F0F0F0F0F0F0F0F0D0, 0000000000{, F0F0F0F0F0F0F0F0F0F0C0, 0.00"})
    void g02g03_fSignedAndMinusZeroComeBackCSigned(String picture, String fixtureIn, String hostIn,
            String fixtureOut, String hostOut, String value) {
        RecordLayout layout = oneField(picture);

        DecodedRecord fromFixture = layout.decode(Bytes.fixture(fixtureIn), CodePage.FIXTURE, SOURCE);
        DecodedRecord fromHost = layout.decode(Bytes.hex(hostIn), CodePage.host(), SOURCE);

        assertEquals(new BigDecimal(value), fromFixture.number("TEST-FIELD"));
        assertEquals(new BigDecimal(value), fromHost.number("TEST-FIELD"));
        assertArrayEquals(Bytes.fixture(fixtureOut), layout.encode(fromFixture, CodePage.FIXTURE, SOURCE));
        assertEquals(hostOut, Bytes.hex(layout.encode(fromHost, CodePage.host(), SOURCE)));
    }

    @ParameterizedTest(name = "{0} [{1}]")
    @CsvSource(delimiter = '|', value = {
            // E1: anything but a digit rejects the record
            "9(04)     | 00A1        | not numeric: byte X'41' at position 3 of the field (E1)",
            "9(04)     | '  01'      | not numeric: byte X'20' at position 1 of the field (E1)",
            "9(04)     | 000{        | not numeric: byte X'7B' at position 4 of the field (E1)",
            "9(04)     | 12.5        | not numeric: byte X'2E' at position 3 of the field (E1)",
            // E2: digits, and a C-, D- or F-signed digit in the last byte only
            "S9(09)V99 | 00000A0000{ | not numeric: byte X'41' at position 6 of the field (E2)",
            "S9(09)V99 | '0000010000 ' | not numeric: byte X'20' at position 11 of the field (E2)",
            "S9(09)V99 | 0000010000S | not numeric: byte X'53' at position 11 of the field (E2)",
            "S9(09)V99 | -0000010000 | not numeric: byte X'2D' at position 1 of the field (E2)",
            "S9(09)V99 | 000001000{0 | not numeric: byte X'7B' at position 10 of the field (E2)"})
    void e1e2_nonDigitInTheFixtureCodePageRejectsTheRecord(String picture, String fixtureBytes, String why) {
        byte[] bytes = Bytes.fixture(fixtureBytes);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> decode(picture, bytes, CodePage.FIXTURE));

        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS, record 7, field TEST-FIELD: " + why, stop.getMessage());
    }

    @ParameterizedTest(name = "{0} X''{1}''")
    @CsvSource(delimiter = '|', value = {
            "9(04)     | F0F0C1F1               | not numeric: byte X'C1' at position 3 of the field (E1)",
            "9(04)     | 40F0F0F1               | not numeric: byte X'40' at position 1 of the field (E1)",
            "9(04)     | 30303031               | not numeric: byte X'30' at position 1 of the field (E1)",
            "S9(09)V99 | F0F0F0F0F0F1F0F0F0F0E0 | not numeric: byte X'E0' at position 11 of the field (E2)",
            "S9(09)V99 | F0F0F0F0F0F1F0F0F0F0CA | not numeric: byte X'CA' at position 11 of the field (E2)",
            "S9(09)V99 | F0F0F0F0F0F1F0F0F0F040 | not numeric: byte X'40' at position 11 of the field (E2)",
            "S9(09)V99 | F0F0F0F0F0C1F0F0F0F0C0 | not numeric: byte X'C1' at position 6 of the field (E2)"})
    void e1e2_nonDigitInCp037RejectsTheRecord(String picture, String hostHex, String why) {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> decode(picture, Bytes.hex(hostHex), CodePage.host()));

        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS, record 7, field TEST-FIELD: " + why, stop.getMessage());
    }

    @ParameterizedTest(name = "{0} <- {1}")
    @CsvSource(delimiter = '|', value = {
            // a value that does not fit is refused, not cut (standards S3.4; INT-H05 stays open)
            "S9(09)V99 | 1000000000.00  | value does not fit the field",
            "S9(09)V99 | -1000000000.00 | value does not fit the field",
            "S9(04)V99 | 10000.00       | value does not fit the field",
            "9(04)     | 10000          | value does not fit the field",
            "9(04)     | -1             | value does not fit the field",
            // a value with more decimals than the field is refused, not rounded
            "S9(09)V99 | 10.005         | value has more than 2 decimals",
            "9(04)     | 1.5            | value has more than 0 decimals"})
    void e1e2_valueThatDoesNotFitIsRefusedNotCutOrRounded(String picture, String value, String why) {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> encode(picture, value, CodePage.FIXTURE));

        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS, record 7, field TEST-FIELD: " + why, stop.getMessage());
    }

    @Test
    void e2_valueWithFewerDecimalsIsWrittenWithTheScaleOfTheField() {
        assertEquals("0000000100{", new String(encode("S9(09)V99", "10", CodePage.FIXTURE), FixtureCodePage.CHARSET));
        assertEquals("0000000105{", new String(encode("S9(09)V99", "10.5", CodePage.FIXTURE), FixtureCodePage.CHARSET));
    }

    @Test
    void rel08_hostCodePageIsAParameter_zonedBytesDoNotDependOnIt() {
        // cp273 (German) has { at X'43' and } at X'DC'; the sign zones stay X'C0' and X'D0'
        CodePage german = CodePage.host("IBM273");

        assertEquals(new BigDecimal("-30.00"), decode("S9(09)V99", Bytes.hex("F0F0F0F0F0F0F0F3F0F0D0"), german));
        assertEquals("F0F0F0F0F0F1F0F0F0F0C0", Bytes.hex(encode("S9(09)V99", "1000.00", german)));
        assertThrows(IllegalArgumentException.class, () -> CodePage.host("ISO-8859-1"));
    }
}
