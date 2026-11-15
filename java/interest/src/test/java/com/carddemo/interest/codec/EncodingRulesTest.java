package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * Encoding rules E1-E4 of the data map as tables: bytes in, value out, bytes back.
 * Fixture bytes are written as text, host (cp037) bytes as hex.
 */
class EncodingRulesTest {

    private static final RecordSource SOURCE = new RecordSource("SYNTH.PS", 1);

    private static CodePage codePage(String name) {
        return name.equals("host") ? CodePage.host(CodecFixtures.CP037) : CodePage.fixture();
    }

    private static byte[] bytes(String codePage, String image) {
        return codePage.equals("host") ? HexFormat.of().parseHex(image) : CodecFixtures.latin1(image);
    }

    private record OneField(RecordCodec codec, FieldSpec field) {

        static OneField of(String picture, String codePage) {
            RecordLayout.Builder builder = RecordLayout.builder("SYNTH");
            FieldSpec field = builder.field("SYNTH-FIELD", picture);
            return new OneField(new RecordCodec(builder.build(), codePage(codePage)), field);
        }
    }

    @ParameterizedTest(name = "E1 {0} {1} [{2}] = {3}")
    @CsvSource({
            // CVTRA01Y/TRANCAT-ACCT-ID, CVTRA01Y/TRANCAT-CD, CVACT03Y/XREF-CUST-ID, CVTRA05Y/TRAN-MERCHANT-ID
            "fixture, 9(11), 00000002001, 2001",
            "fixture, 9(04), 0001, 1",
            "fixture, 9(04), 9999, 9999",
            "fixture, 9(09), 000002001, 2001",
            "fixture, 9(09), 000000000, 0",
            "host, 9(11), F0F0F0F0F0F0F0F2F0F0F1, 2001",
            "host, 9(04), F0F0F0F1, 1",
            "host, 9(04), F9F9F9F9, 9999",
            "host, 9(09), F0F0F0F0F0F0F0F0F0, 0"})
    void e1_unsignedZoned(String codePage, String picture, String image, long value) {
        OneField one = OneField.of(picture, codePage);
        byte[] record = bytes(codePage, image);

        RecordValues decoded = one.codec().decode(record, SOURCE);

        assertEquals(value, decoded.unsigned(one.field()));
        assertArrayEquals(record, one.codec().encode(decoded, SOURCE));
    }

    @ParameterizedTest(name = "E2 {0} {1} [{2}] = {3} -> [{4}]")
    @CsvSource({
            // ticket 03 observable values
            "fixture, S9(09)V99, 0000010000{, 1000.00, 0000010000{",
            "fixture, S9(09)V99, 0000000300}, -30.00, 0000000300}",
            // every sign byte of tools/parity/record.py:51
            "fixture, S9(09)V99, 0000000000A, 0.01, 0000000000A",
            "fixture, S9(09)V99, 0000000000I, 0.09, 0000000000I",
            "fixture, S9(09)V99, 0000000000J, -0.01, 0000000000J",
            "fixture, S9(09)V99, 0000000000R, -0.09, 0000000000R",
            "fixture, S9(09)V99, 9999999999I, 999999999.99, 9999999999I",
            "fixture, S9(09)V99, 9999999999R, -999999999.99, 9999999999R",
            // CVTRA02Y/DIS-INT-RATE and CVACT01Y/ACCT-CURR-BAL
            "fixture, S9(04)V99, 00120{, 12.00, 00120{",
            "fixture, S9(04)V99, 00185{, 18.50, 00185{",
            "fixture, S9(10)V99, 00000010100{, 1010.00, 00000010100{",
            // G02: a plain (F-signed) last digit is positive and is written with a C sign
            "fixture, S9(09)V99, 00000100000, 1000.00, 0000010000{",
            "fixture, S9(09)V99, 00000100005, 1000.05, 0000010000E",
            // G03: minus zero is written as plus zero
            "fixture, S9(09)V99, 0000000000}, 0.00, 0000000000{",
            "host, S9(09)V99, F0F0F0F0F0F1F0F0F0F0C0, 1000.00, F0F0F0F0F0F1F0F0F0F0C0",
            "host, S9(09)V99, F0F0F0F0F0F0F0F3F0F0D0, -30.00, F0F0F0F0F0F0F0F3F0F0D0",
            "host, S9(09)V99, F0F0F0F0F0F0F0F0F0F0C1, 0.01, F0F0F0F0F0F0F0F0F0F0C1",
            "host, S9(09)V99, F0F0F0F0F0F0F0F0F0F0C9, 0.09, F0F0F0F0F0F0F0F0F0F0C9",
            "host, S9(09)V99, F0F0F0F0F0F0F0F0F0F0D1, -0.01, F0F0F0F0F0F0F0F0F0F0D1",
            "host, S9(09)V99, F0F0F0F0F0F0F0F0F0F0D9, -0.09, F0F0F0F0F0F0F0F0F0F0D9",
            "host, S9(04)V99, F0F0F1F2F0C0, 12.00, F0F0F1F2F0C0",
            // G02 on the host: X'F0' as the sign byte
            "host, S9(09)V99, F0F0F0F0F0F1F0F0F0F0F0, 1000.00, F0F0F0F0F0F1F0F0F0F0C0",
            "host, S9(09)V99, F0F0F0F0F0F1F0F0F0F0F5, 1000.05, F0F0F0F0F0F1F0F0F0F0C5",
            // G03 on the host
            "host, S9(09)V99, F0F0F0F0F0F0F0F0F0F0D0, 0.00, F0F0F0F0F0F0F0F0F0F0C0"})
    void e2_signedZoned(String codePage, String picture, String image, String value, String encoded) {
        OneField one = OneField.of(picture, codePage);

        RecordValues decoded = one.codec().decode(bytes(codePage, image), SOURCE);

        // equals, not compareTo: the scale is the one of the PICTURE
        assertEquals(new BigDecimal(value), decoded.signed(one.field()));
        assertArrayEquals(bytes(codePage, encoded), one.codec().encode(decoded, SOURCE));
    }

    @ParameterizedTest(name = "E3 {0} {1} [{2}] = [{3}]")
    @CsvSource({
            // CVTRA02Y/DIS-ACCT-GROUP-ID, CVTRA01Y/TRANCAT-TYPE-CD, CVTRA05Y/TRAN-SOURCE
            "fixture, X(10), 'GOLD      ', GOLD",
            "fixture, X(10), 'DEFAULT   ', DEFAULT",
            "fixture, X(02), 01, 01",
            "fixture, X(10), 'System    ', System",
            "fixture, X(10), '          ', ''",
            "fixture, X(10), '  A B     ', '  A B'",
            "fixture, X(10), 'a/c {}[]. ', 'a/c {}[].'",
            "host, X(10), C7D6D3C4404040404040, GOLD",
            "host, X(02), F0F1, 01",
            "host, X(10), E2A8A2A3859440404040, System",
            "host, X(10), 40404040404040404040, ''",
            "host, X(10), 4040C140C24040404040, '  A B'"})
    void e3_text(String codePage, String picture, String image, String value) {
        OneField one = OneField.of(picture, codePage);
        byte[] record = bytes(codePage, image);

        RecordValues decoded = one.codec().decode(record, SOURCE);

        assertEquals(value, decoded.text(one.field()));
        assertArrayEquals(record, one.codec().encode(decoded, SOURCE));
    }

    @ParameterizedTest(name = "E4 {0} FILLER [{1}]")
    @CsvSource({
            // spaces of either code page, low values, high values, bytes that are no character and no digit
            "fixture, 2020202020",
            "fixture, 0000000000",
            "fixture, FFFFFFFFFF",
            "fixture, 00017F80FF",
            "fixture, 4040404040",
            "host, 4040404040",
            "host, 0000000000",
            "host, FFFFFFFFFF",
            "host, 00017F80FF",
            "host, 2020202020"})
    void e4_fillerComesBackUnchanged(String codePage, String hex) {
        RecordLayout.Builder builder = RecordLayout.builder("SYNTH");
        builder.field("SYNTH-ID", "9(01)");
        FieldSpec filler = builder.filler("X(05)");
        RecordCodec codec = new RecordCodec(builder.build(), codePage(codePage));
        byte[] record = HexFormat.of().parseHex((codePage.equals("host") ? "F7" : "37") + hex);

        RecordValues decoded = codec.decode(record, SOURCE);

        assertArrayEquals(HexFormat.of().parseHex(hex), decoded.filler(filler));
        assertArrayEquals(record, codec.encode(decoded, SOURCE));
    }

    @ParameterizedTest(name = "{0}: a new record has spaces in text and FILLER, zeros in numbers [{1}]")
    @CsvSource({
            "fixture, 303030202030307B2020",
            "host, F0F0F04040F0F0C04040"})
    void e4_g05_fieldsNeverSetAreSpacesAndZeros(String codePage, String hex) {
        RecordLayout.Builder builder = RecordLayout.builder("SYNTH");
        builder.field("SYNTH-ID", "9(03)");
        builder.field("SYNTH-TEXT", "X(02)");
        builder.field("SYNTH-AMT", "S9(01)V99");
        builder.filler("X(02)");
        RecordLayout layout = builder.build();
        byte[] record = new RecordCodec(layout, codePage(codePage)).encode(RecordValues.of(layout), SOURCE);

        assertEquals(hex, CodecFixtures.hex(record));
    }

    @ParameterizedTest(name = "E1/E2/E3 {0}: {1} cannot hold {2}")
    @CsvSource({
            "unsigned, 9(04), 10000, value does not fit the field",
            "unsigned, 9(04), -1, value does not fit the field (E1)",
            "signed, S9(04)V99, 10000.00, value does not fit the field",
            "signed, S9(04)V99, 1.005, value has more decimals than the field (E2)",
            "text, X(04), ABCDE, 'value longer than the field (E3)'",
            "text, X(04), 'A\u20ACB', 'a character is not in code page ISO-8859-1 (E3, G07)'"})
    void valuesThatDoNotFitAreRefusedNotCut(String kind, String picture, String value, String why) {
        OneField one = OneField.of(picture, "fixture");
        RecordValues empty = RecordValues.of(one.codec().layout());
        RecordValues values = switch (kind) {
            case "unsigned" -> empty.withUnsigned(one.field(), Long.parseLong(value));
            case "signed" -> empty.withSigned(one.field(), new BigDecimal(value));
            default -> empty.withText(one.field(), value);
        };

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> one.codec().encode(values, SOURCE));

        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        assertEquals("RECORD_REJECTED (INT-R08): file SYNTH.PS record 1 field SYNTH-FIELD: " + why, stop.getMessage());
    }

    @Test
    void e6_keyOrderIsTheByteOrderOfTheCodePage() {
        // mixed keys: digits sort before letters in the fixture code page and after them in EBCDIC
        byte[] digitFixture = CodecFixtures.latin1("0000000200101");
        byte[] letterFixture = CodecFixtures.latin1("00000002001AB");
        byte[] digitHost = "0000000200101".getBytes(CodecFixtures.CP037);
        byte[] letterHost = "00000002001AB".getBytes(CodecFixtures.CP037);

        assertEquals(-1, Integer.signum(CategoryBalanceRecord.KEY_ORDER.compare(digitFixture, letterFixture)));
        assertEquals(1, Integer.signum(CategoryBalanceRecord.KEY_ORDER.compare(digitHost, letterHost)));
    }

    @Test
    void rel08_theHostCodePageIsAParameterWithCp037AsDefault() {
        assertEquals("IBM037", CodePage.host().charset().name());
        assertEquals("IBM01140", CodePage.host(Charset.forName("IBM01140")).charset().name());
        assertThrows(IllegalArgumentException.class, () -> CodePage.host(StandardCharsets.ISO_8859_1));
    }
}
