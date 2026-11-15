package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * Acceptance line 4 of ticket 03: each of the four errors stops the run with a message naming
 * file, record number and field, and never a field value (standards S6.5).
 */
class RecordRejectionTest {

    private static final String FILE = "AWS.M2.CARDDEMO.TCATBALF.PS.txt";
    private static final RecordSource SOURCE = new RecordSource(FILE, 7);
    private static final String PREFIX = "RECORD_REJECTED (INT-R08): file " + FILE + " record 7 field ";
    /** i01-baseline, first balance record: account 2001, type 01, category 1, 1000.00. */
    private static final String BALANCE = "000000020010100010000010000{";

    private static String rejection(Executable call) {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class, call);
        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        return stop.getMessage();
    }

    private static byte[] balance(CodePage codePage) {
        return RecordImage.fromLine(BALANCE, CategoryBalanceRecord.LAYOUT.length(), codePage);
    }

    @ParameterizedTest(name = "non-digit at byte {0} is reported for {1}")
    @CsvSource({
            "0, TRANCAT-ACCT-ID",
            "10, TRANCAT-ACCT-ID",
            "13, TRANCAT-CD",
            "16, TRANCAT-CD"})
    void e1_nonDigitInAPic9FieldNamesFileRecordAndField(int offset, String field) {
        CategoryBalanceRecord layout = new CategoryBalanceRecord(CodePage.fixture());
        byte[] record = balance(CodePage.fixture());
        record[offset] = 'X';

        String message = rejection(() -> layout.decode(record, SOURCE));

        assertEquals(PREFIX + field + ": not numeric (E1)", message);
    }

    @ParameterizedTest(name = "{0}: byte X''{1}'' at byte {2} is reported for {3}")
    @CsvSource({
            // a space, a letter, the other code page's digit
            "fixture, 20, 3, TRANCAT-ACCT-ID, E1",
            "fixture, F1, 3, TRANCAT-ACCT-ID, E1",
            "host, 31, 3, TRANCAT-ACCT-ID, E1",
            "host, 40, 14, TRANCAT-CD, E1",
            // digits before the sign byte, and a sign byte that is neither sign nor digit
            "fixture, 2D, 20, TRAN-CAT-BAL, E2",
            "fixture, 53, 27, TRAN-CAT-BAL, E2",
            "fixture, C0, 27, TRAN-CAT-BAL, E2",
            "host, 7B, 27, TRAN-CAT-BAL, E2",
            "host, E2, 27, TRAN-CAT-BAL, E2"})
    void e1e2_bytesThatAreNoDigitOfTheCodePage(String codePageName, String hex, int offset, String field, String rule) {
        CodePage codePage = codePageName.equals("host") ? CodePage.host() : CodePage.fixture();
        byte[] record = codePageName.equals("host") ? BALANCE.concat(" ".repeat(22)).getBytes(CodecFixtures.CP037) : balance(codePage);
        record[offset] = (byte) Integer.parseInt(hex, 16);

        String message = rejection(() -> new CategoryBalanceRecord(codePage).decode(record, SOURCE));

        assertEquals(PREFIX + field + ": not numeric (" + rule + ")", message);
    }

    @ParameterizedTest(name = "{0}: byte X''{1}'' in a PIC X field is outside the code page")
    @CsvSource({
            // control characters of ISO-8859-1 (C0 and C1 ranges) and of cp037, and a byte cp037 maps to a control
            "fixture, 00, ISO-8859-1",
            "fixture, 1F, ISO-8859-1",
            "fixture, 7F, ISO-8859-1",
            "fixture, 85, ISO-8859-1",
            "host, 00, IBM037",
            "host, 3F, IBM037",
            "host, FF, IBM037"})
    void e3_g07_byteOutsideTheCodePageInAPicXFieldNamesFileRecordAndField(String codePageName, String hex, String charset) {
        CodePage codePage = codePageName.equals("host") ? CodePage.host() : CodePage.fixture();
        byte[] record = codePageName.equals("host") ? BALANCE.concat(" ".repeat(22)).getBytes(CodecFixtures.CP037) : balance(codePage);
        record[12] = (byte) Integer.parseInt(hex, 16);

        String message = rejection(() -> new CategoryBalanceRecord(codePage).decode(record, SOURCE));

        assertEquals(PREFIX + "TRANCAT-TYPE-CD: a byte is not a character of code page " + charset + " (E3, G07)", message);
    }

    @Test
    void e3_g07_aByteWithNoCharacterInTheConfiguredCodePage() {
        // the code page is the configured one: X'81' is 'a' in IBM1047, X'04' is a control
        RecordLayout.Builder builder = RecordLayout.builder("SYNTH");
        builder.field("SYNTH-TEXT", "X(03)");
        RecordCodec codec = new RecordCodec(builder.build(), CodePage.host(Charset.forName("IBM1047")));

        assertEquals("abc", codec.decode(new byte[] {(byte) 0x81, (byte) 0x82, (byte) 0x83}, SOURCE).text(codec.layout().fields().get(0)));
        assertEquals(PREFIX + "SYNTH-TEXT: a byte is not a character of code page IBM1047 (E3, G07)",
                rejection(() -> codec.decode(new byte[] {(byte) 0x81, 0x04, (byte) 0x83}, SOURCE)));
    }

    @ParameterizedTest(name = "a record of {0} bytes is reported for {1}")
    @CsvSource({
            "0, TRANCAT-ACCT-ID, ends before the end of the field",
            "10, TRANCAT-ACCT-ID, ends before the end of the field",
            "12, TRANCAT-TYPE-CD, ends before the end of the field",
            "27, TRAN-CAT-BAL, ends before the end of the field",
            "49, FILLER, ends before the end of the field",
            "51, FILLER, goes on after the last field",
            "300, FILLER, goes on after the last field"})
    void wrongRecordLengthNamesFileRecordAndField(int length, String field, String what) {
        byte[] record = Arrays.copyOf(balance(CodePage.fixture()), length);
        Arrays.fill(record, Math.min(length, 50), length, (byte) ' ');

        String message = rejection(() -> new CategoryBalanceRecord(CodePage.fixture()).decode(record, SOURCE));

        assertEquals(PREFIX + field + ": record of " + length + " bytes " + what + "; CVTRA01Y records have 50 bytes", message);
    }

    @Test
    void aLineLongerThanTheRecordIsNotCut() {
        // 51 characters in a 50-byte file: the pilot cut the line, the codec rejects the record
        byte[] record = RecordImage.fromLine(BALANCE + " ".repeat(22) + "X", 50, CodePage.fixture());

        assertEquals(51, record.length);
        assertEquals(PREFIX + "FILLER: record of 51 bytes goes on after the last field; CVTRA01Y records have 50 bytes",
                rejection(() -> new CategoryBalanceRecord(CodePage.fixture()).decode(record, SOURCE)));
    }

    @Test
    void aHostFileWhoseLastRecordIsShortIsNotPadded() {
        byte[] content = new byte[120];

        assertEquals(List.of(50, 50, 20), RecordImage.fromBlock(content, 50).stream().map(r -> r.length).toList());
    }

    @ParameterizedTest(name = "USAGE {0} is refused")
    @CsvSource({"COMP-3", "COMP", "BINARY", "PACKED-DECIMAL", "COMP-5"})
    void e5_usageOtherThanDisplayNamesFileRecordAndField(String usage) {
        RecordLayout layout = CopybookParser.parse("SYNTH", String.join("\n",
                "       01  SYNTH-RECORD.",
                "           05  SYNTH-ID             PIC 9(11).",
                "           05  SYNTH-AMT            PIC S9(7)V99 " + usage + ".",
                "           05  FILLER               PIC X(04)."));
        RecordCodec codec = new RecordCodec(layout, CodePage.fixture());
        String expected = PREFIX + "SYNTH-AMT: USAGE " + usage + " is not DISPLAY; the field is refused, not guessed (E5)";

        assertEquals(expected, rejection(() -> codec.decode(new byte[20], SOURCE)));
        assertEquals(expected, rejection(() -> codec.encode(RecordValues.of(layout), SOURCE)));
        assertEquals(expected, rejection(() -> codec.recordLength(SOURCE)));
    }

    @Test
    void s6_5_theRejectionHoldsNoFieldValue() {
        byte[] record = balance(CodePage.fixture());
        record[16] = 'X';

        String message = rejection(() -> new CategoryBalanceRecord(CodePage.fixture()).decode(record, SOURCE));

        assertFalse(message.contains("2001"), message);
        assertFalse(message.contains("10000"), message);
    }
}
