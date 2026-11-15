package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.carddemo.interest.domain.InterestRunStoppedException;

/** Encoding rules E3 (text, G07) and E4 (FILLER, G05) of the data map, table-driven, in both code pages. */
class TextAndFillerTest {

    private static final RecordSource SOURCE = new RecordSource("TEST.PS", 3);

    /** TEST-TEXT X(10) at 0, FILLER X(04) at 10. */
    private static final RecordLayout LAYOUT = Copybook.parse("TESTCPY", """
                   01  TEST-RECORD.
                       05  TEST-TEXT               PIC X(10).
                       05  FILLER                  PIC X(04).
            """);

    @ParameterizedTest(name = "[{0}]")
    @CsvSource(delimiter = '|', value = {
            // value        | fixture bytes | cp037 bytes
            "'System'       | 'System    '  | E2A8A2A3859440404040",
            "'DEFAULT'      | 'DEFAULT   '  | C4C5C6C1E4D3E3404040",
            "'A000000000'   | 'A000000000'  | C1F0F0F0F0F0F0F0F0F0",
            "''             | '          '  | 40404040404040404040",
            "'  lead'       | '  lead    '  | 40409385818440404040",
            "'Int. a/c'     | 'Int. a/c  '  | C995A34B408161834040"})
    void e3_textIsPaddedWithTheSpaceOfTheCodePage(String text, String fixtureBytes, String hostField) {
        byte[] fixtureRecord = Bytes.fixture(fixtureBytes + "abcd");
        byte[] hostRecord = Bytes.hex(hostField + "81828384");

        assertEquals(text, LAYOUT.decode(fixtureRecord, CodePage.FIXTURE, SOURCE).text("TEST-TEXT"));
        assertEquals(text, LAYOUT.decode(hostRecord, CodePage.host(), SOURCE).text("TEST-TEXT"));
        assertEquals(fixtureBytes, RecordImage.image(LAYOUT.encode(
                LAYOUT.blank(CodePage.FIXTURE).with("TEST-TEXT", text), CodePage.FIXTURE, SOURCE)).substring(0, 10));
        assertEquals(hostField, Bytes.hex(LAYOUT.encode(
                LAYOUT.blank(CodePage.host()).with("TEST-TEXT", text), CodePage.host(), SOURCE)).substring(0, 20));
    }

    @ParameterizedTest(name = "fixture X''{0}''")
    @CsvSource({
            // G07: low-values, a control character, DEL, a C1 control: not characters
            "53797300656D20202020", "5379730A656D20202020", "5379737F656D20202020", "53797385656D20202020"})
    void g07_byteThatIsNotACharacterRejectsTheRecord_fixtureCodePage(String fieldHex) {
        byte[] record = Bytes.hex(fieldHex + "20202020");

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> LAYOUT.decode(record, CodePage.FIXTURE, SOURCE));

        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS, record 3, field TEST-TEXT: "
                + "a byte is not a character of the code page fixture (ISO-8859-1) (E3, G07)", stop.getMessage());
    }

    @ParameterizedTest(name = "cp037 X''{0}''")
    @CsvSource({
            // G07: low-values X'00', a packed sign byte X'0C', X'15' (new line), X'FF'
            "E2A8A200859440404040", "E2A8A20C859440404040", "E2A8A215859440404040", "E2A8A2FF859440404040"})
    void g07_byteThatIsNotACharacterRejectsTheRecord_cp037(String fieldHex) {
        byte[] record = Bytes.hex(fieldHex + "40404040");

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> LAYOUT.decode(record, CodePage.host(), SOURCE));

        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS, record 3, field TEST-TEXT: "
                + "a byte is not a character of the code page host (IBM037) (E3, G07)", stop.getMessage());
    }

    @Test
    void e3_textLongerThanTheFieldIsRefusedNotCut() {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> LAYOUT.encode(LAYOUT.blank(CodePage.FIXTURE).with("TEST-TEXT", "System text"), CodePage.FIXTURE, SOURCE));

        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS, record 3, field TEST-TEXT: "
                + "value of 11 characters is longer than the field", stop.getMessage());
    }

    @Test
    void e3_characterOutsideTheCodePageIsRefusedNotReplaced() {
        // the euro sign is in neither ISO-8859-1 nor cp037
        for (CodePage codePage : new CodePage[] {CodePage.FIXTURE, CodePage.host()}) {
            InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                    () -> LAYOUT.encode(LAYOUT.blank(codePage).with("TEST-TEXT", "10 \u20AC"), codePage, SOURCE));

            assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS, record 3, field TEST-TEXT: "
                    + "a character is not in the code page " + codePage.name() + " (E3)", stop.getMessage());
        }
    }

    @Test
    void e4_fillerIsRawBytes_neverTranslatedOrChecked() {
        // low-values, a packed number and X'FF' in the FILLER come back byte for byte, in both code pages
        byte[] fixtureRecord = Bytes.hex("53797374656D20202020" + "00123CFF");
        byte[] hostRecord = Bytes.hex("E2A8A2A3859440404040" + "00123CFF");

        DecodedRecord fromFixture = LAYOUT.decode(fixtureRecord, CodePage.FIXTURE, SOURCE);
        DecodedRecord fromHost = LAYOUT.decode(hostRecord, CodePage.host(), SOURCE);

        assertArrayEquals(Bytes.hex("00123CFF"), fromFixture.filler("FILLER"));
        assertArrayEquals(Bytes.hex("00123CFF"), fromHost.filler("FILLER"));
        assertArrayEquals(fixtureRecord, LAYOUT.encode(fromFixture, CodePage.FIXTURE, SOURCE));
        assertArrayEquals(hostRecord, LAYOUT.encode(fromHost, CodePage.host(), SOURCE));
    }

    @Test
    void e4g05_fillerOfANewRecordIsSpacesOfTheOutputCodePage() {
        assertEquals("20202020", Bytes.hex(LAYOUT.blank(CodePage.FIXTURE).filler("FILLER")));
        assertEquals("40404040", Bytes.hex(LAYOUT.blank(CodePage.host()).filler("FILLER")));
    }

    @Test
    void valuesAreReadAndSetByTheKindOfTheField() {
        DecodedRecord blank = LAYOUT.blank(CodePage.FIXTURE);

        assertThrows(IllegalArgumentException.class, () -> blank.with("TEST-TEXT", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> blank.with("FILLER", "text"));
        assertThrows(IllegalArgumentException.class, () -> blank.with("NO-SUCH-FIELD", "text"));
    }
}
