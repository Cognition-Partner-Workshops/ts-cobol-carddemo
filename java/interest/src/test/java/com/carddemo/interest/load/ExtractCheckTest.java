package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The boundary check of the load: rejects (TL3), round trip (RC3) and the gap counts G02 and G03. No database. */
class ExtractCheckTest {

    private static final CodePage FIXTURE = CodePage.fixture();
    private static final String CASE = "i01-baseline";

    private final ExtractCheck check = new ExtractCheck(FIXTURE);

    private static List<byte[]> records(ExtractFile file, List<String> lines) {
        return lines.stream().map(line -> RecordImage.fromLine(line, file.layout().length(), FIXTURE)).toList();
    }

    private CheckedExtract checked(ExtractFile file, List<String> lines) {
        return check.check(file, LoadFixtures.fileName(file), records(file, lines));
    }

    private static List<String> withLine(ExtractFile file, int recordNumber, int offset, char replacement) {
        List<String> lines = new ArrayList<>(LoadFixtures.lines(CASE, file));
        lines.set(recordNumber - 1, LoadFixtures.withCharAt(lines.get(recordNumber - 1), offset, replacement));
        return lines;
    }

    @Test
    void rc3_everyRecordOfTheSevenCasesEncodesBackToItsInput() {
        for (String caseName : LoadFixtures.CASES) {
            for (ExtractFile file : ExtractFile.values()) {
                List<String> lines = LoadFixtures.lines(caseName, file);
                CheckedExtract extract = checked(file, lines);
                assertEquals(List.of(), extract.rejects(), caseName + " " + file);
                assertEquals(lines.size(), extract.rows().size(), caseName + " " + file);
                assertEquals(0, extract.g02Differences() + extract.g03Differences(), caseName + " " + file);
                for (int i = 0; i < lines.size(); i++) {
                    assertEquals(lines.get(i), RecordImage.toLine(extract.encodedRows().get(i), FIXTURE), caseName + " " + file);
                }
            }
        }
    }

    @Test
    void tl3_nonNumericAmountIsRejectedWithFileRecordNumberAndField() {
        // TRAN-CAT-BAL is bytes 17-27 of the record (data map CVTRA01Y); offset 20 is one of its digits.
        CheckedExtract extract = checked(ExtractFile.TRAN_CAT_BALANCE, withLine(ExtractFile.TRAN_CAT_BALANCE, 2, 20, 'X'));

        assertEquals(List.of(new LoadReject("AWS.M2.CARDDEMO.TCATBALF.PS.txt", 2, "TRAN-CAT-BAL", "not numeric (E2)")),
                extract.rejects());
        assertEquals(1, extract.rows().size());
    }

    @Test
    void tl3_everyBadRecordIsListedNotOnlyTheFirst() {
        List<String> lines = withLine(ExtractFile.DISCLOSURE_GROUP, 4, 12, '?');
        lines.set(8, LoadFixtures.withCharAt(lines.get(8), 12, '?'));
        CheckedExtract extract = checked(ExtractFile.DISCLOSURE_GROUP, lines);

        assertEquals(List.of(4, 9), extract.rejects().stream().map(LoadReject::recordNumber).toList());
        assertEquals(List.of("DIS-TRAN-CAT-CD", "DIS-TRAN-CAT-CD"), extract.rejects().stream().map(LoadReject::field).toList());
    }

    @Test
    void g07_textByteThatIsNoCharacterIsRejected() {
        CheckedExtract extract = checked(ExtractFile.ACCOUNT, withLine(ExtractFile.ACCOUNT, 1, 11, '\u0000'));

        assertEquals(1, extract.rejects().size());
        assertEquals("ACCT-ACTIVE-STATUS", extract.rejects().get(0).field());
        assertEquals(1, extract.rejects().get(0).recordNumber());
    }

    @Test
    void tl3_recordOfTheWrongLengthIsRejected() {
        List<String> lines = new ArrayList<>(LoadFixtures.lines(CASE, ExtractFile.CARD_XREF));
        lines.set(0, lines.get(0) + " ".repeat(20) + "X");
        CheckedExtract extract = checked(ExtractFile.CARD_XREF, lines);

        assertEquals(1, extract.rejects().size());
        assertEquals(1, extract.rejects().get(0).recordNumber());
    }

    @Test
    void g02_fSignedAmountIsCountedAndComesBackCSigned() {
        // The last byte of TRAN-CAT-BAL (offset 27) is '{' = +0; a plain '0' is the F-signed form of the same value.
        List<String> lines = withLine(ExtractFile.TRAN_CAT_BALANCE, 1, 27, '0');
        CheckedExtract extract = checked(ExtractFile.TRAN_CAT_BALANCE, lines);

        assertEquals(List.of(), extract.rejects());
        assertEquals(1, extract.g02Differences());
        assertEquals(0, extract.g03Differences());
        assertArrayEquals(records(ExtractFile.TRAN_CAT_BALANCE, LoadFixtures.lines(CASE, ExtractFile.TRAN_CAT_BALANCE)).get(0),
                extract.encodedRows().get(0));
    }

    @Test
    void g03_minusZeroIsCountedAndComesBackPlusZero() {
        // Record 3 of DISCGRP has rate 0000.00 ("00000{"); '}' in the last byte (offset 21) makes it minus zero.
        List<String> lines = withLine(ExtractFile.DISCLOSURE_GROUP, 3, 21, '}');
        CheckedExtract extract = checked(ExtractFile.DISCLOSURE_GROUP, lines);

        assertEquals(List.of(), extract.rejects());
        assertEquals(0, extract.g02Differences());
        assertEquals(1, extract.g03Differences());
        assertEquals(Fixtures.lines(Fixtures.input(CASE).resolve(LoadFixtures.fileName(ExtractFile.DISCLOSURE_GROUP))).get(2),
                RecordImage.toLine(extract.encodedRows().get(2), FIXTURE));
    }

    @Test
    void rc4_secondRecordWithTheSameKeyIsRejected() {
        List<String> lines = new ArrayList<>(LoadFixtures.lines(CASE, ExtractFile.ACCOUNT));
        lines.add(lines.get(0));
        CheckedExtract extract = checked(ExtractFile.ACCOUNT, lines);

        assertEquals(List.of(new LoadReject("AWS.M2.CARDDEMO.ACCTDATA.PS.txt", 3, "ACCT-ID", "the key is already in the file (RC4)")),
                extract.rejects());
    }

    @Test
    void tl4_nonBlankFillerIsCountedNotRejected() {
        // The FILLER of CVACT03Y is bytes 36-49; the recorded line ends at byte 35 (trailing spaces are not recorded).
        List<String> lines = new ArrayList<>(LoadFixtures.lines(CASE, ExtractFile.CARD_XREF));
        assertEquals(36, lines.get(0).length());
        lines.set(0, lines.get(0) + "Z");
        CheckedExtract extract = checked(ExtractFile.CARD_XREF, lines);

        assertEquals(List.of(), extract.rejects());
        assertEquals(1, extract.nonBlankFillers());
    }
}
