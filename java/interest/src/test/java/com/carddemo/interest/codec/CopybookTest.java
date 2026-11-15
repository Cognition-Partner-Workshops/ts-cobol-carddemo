package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The copybook reader against the five copybooks of the estate (FI-1) and against clauses it must
 * refuse (E5). Expected offsets and lengths are written by hand from
 * modernization/design/data-map-interest.md (its offsets count from 1, these from 0).
 */
class CopybookTest {

    private static final Path COPYBOOKS = Path.of("..", "..", "app", "cpy");

    static RecordLayout estateCopybook(String name) {
        try {
            return Copybook.parse(name, Files.readString(COPYBOOKS.resolve(name + ".cpy"), StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static LayoutField unsigned(String name, int offset, int length) {
        return new LayoutField(name, FieldKind.UNSIGNED_ZONED, offset, length, 0, null);
    }

    private static LayoutField signed(String name, int offset, int length) {
        return new LayoutField(name, FieldKind.SIGNED_ZONED, offset, length, 2, null);
    }

    private static LayoutField text(String name, int offset, int length) {
        return new LayoutField(name, FieldKind.TEXT, offset, length, 0, null);
    }

    private static LayoutField filler(int offset, int length) {
        return new LayoutField("FILLER", FieldKind.FILLER, offset, length, 0, null);
    }

    static Stream<Arguments> fiveLayouts() {
        return Stream.of(
                Arguments.of("CVACT01Y", CardDemoLayouts.ACCOUNT, "ACCOUNT-RECORD", 300, List.of(
                        unsigned("ACCT-ID", 0, 11),
                        text("ACCT-ACTIVE-STATUS", 11, 1),
                        signed("ACCT-CURR-BAL", 12, 12),
                        signed("ACCT-CREDIT-LIMIT", 24, 12),
                        signed("ACCT-CASH-CREDIT-LIMIT", 36, 12),
                        text("ACCT-OPEN-DATE", 48, 10),
                        text("ACCT-EXPIRAION-DATE", 58, 10),
                        text("ACCT-REISSUE-DATE", 68, 10),
                        signed("ACCT-CURR-CYC-CREDIT", 78, 12),
                        signed("ACCT-CURR-CYC-DEBIT", 90, 12),
                        text("ACCT-ADDR-ZIP", 102, 10),
                        text("ACCT-GROUP-ID", 112, 10),
                        filler(122, 178))),
                Arguments.of("CVACT03Y", CardDemoLayouts.CARD_XREF, "CARD-XREF-RECORD", 50, List.of(
                        text("XREF-CARD-NUM", 0, 16),
                        unsigned("XREF-CUST-ID", 16, 9),
                        unsigned("XREF-ACCT-ID", 25, 11),
                        filler(36, 14))),
                Arguments.of("CVTRA01Y", CardDemoLayouts.CATEGORY_BALANCE, "TRAN-CAT-BAL-RECORD", 50, List.of(
                        unsigned("TRANCAT-ACCT-ID", 0, 11),
                        text("TRANCAT-TYPE-CD", 11, 2),
                        unsigned("TRANCAT-CD", 13, 4),
                        signed("TRAN-CAT-BAL", 17, 11),
                        filler(28, 22))),
                Arguments.of("CVTRA02Y", CardDemoLayouts.DISCLOSURE_GROUP, "DIS-GROUP-RECORD", 50, List.of(
                        text("DIS-ACCT-GROUP-ID", 0, 10),
                        text("DIS-TRAN-TYPE-CD", 10, 2),
                        unsigned("DIS-TRAN-CAT-CD", 12, 4),
                        signed("DIS-INT-RATE", 16, 6),
                        filler(22, 28))),
                Arguments.of("CVTRA05Y", CardDemoLayouts.TRANSACTION, "TRAN-RECORD", 350, List.of(
                        text("TRAN-ID", 0, 16),
                        text("TRAN-TYPE-CD", 16, 2),
                        unsigned("TRAN-CAT-CD", 18, 4),
                        text("TRAN-SOURCE", 22, 10),
                        text("TRAN-DESC", 32, 100),
                        signed("TRAN-AMT", 132, 11),
                        unsigned("TRAN-MERCHANT-ID", 143, 9),
                        text("TRAN-MERCHANT-NAME", 152, 50),
                        text("TRAN-MERCHANT-CITY", 202, 50),
                        text("TRAN-MERCHANT-ZIP", 252, 10),
                        text("TRAN-CARD-NUM", 262, 16),
                        text("TRAN-ORIG-TS", 278, 26),
                        text("TRAN-PROC-TS", 304, 26),
                        filler(330, 20))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fiveLayouts")
    void fi1_copybookOfTheEstateGivesTheFieldsOfTheDataMap(String name, RecordLayout carried, String recordName,
            int recordLength, List<LayoutField> expected) {
        RecordLayout fromEstate = estateCopybook(name);

        assertEquals(expected, fromEstate.fields());
        assertEquals(recordName, fromEstate.recordName());
        assertEquals(recordLength, fromEstate.length());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fiveLayouts")
    void fi1_layoutCarriedInTheJarEqualsTheCopybookOfTheEstate(String name, RecordLayout carried, String recordName,
            int recordLength, List<LayoutField> expected) {
        assertEquals(estateCopybook(name).fields(), carried.fields());
        assertEquals(recordName, carried.recordName());
        assertEquals(recordLength, carried.length());
    }

    @Test
    void e5_theFiveLayoutsHold41ElementaryFieldsAllDisplay() {
        // data map E5: "none of the 43 fields is COMP-3 or COMP" counts the two key groups TRAN-CAT-KEY and
        // DIS-GROUP-KEY; the elementary fields are 13 + 4 + 5 + 5 + 14 = 41 (36 named and 5 FILLER)
        List<LayoutField> all = fiveLayouts().flatMap(layout -> ((RecordLayout) layout.get()[1]).fields().stream()).toList();

        assertEquals(41, all.size());
        assertTrue(all.stream().noneMatch(field -> field.kind() == FieldKind.REFUSED));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "PIC S9(7)V99 COMP-3              | USAGE COMP-3 is not DISPLAY (E5)",
            "PIC S9(04) COMP                  | USAGE COMP is not DISPLAY (E5)",
            "PIC S9(09) USAGE IS BINARY       | USAGE BINARY is not DISPLAY (E5)",
            "PIC S9(9)V99 PACKED-DECIMAL      | USAGE PACKED-DECIMAL is not DISPLAY (E5)",
            "PIC S9(4) COMPUTATIONAL-3        | USAGE COMPUTATIONAL-3 is not DISPLAY (E5)",
            "COMP-1                           | USAGE COMP-1 is not DISPLAY (E5)",
            "PIC X(10) OCCURS 3 TIMES         | OCCURS is not handled by this codec",
            "PIC S9(5) SIGN IS LEADING        | LEADING is not handled by this codec",
            "PIC S9(5) SIGN TRAILING SEPARATE | SEPARATE is not handled by this codec",
            "PIC ZZ,ZZ9.99                    | PIC ZZ,ZZ9.99 is not handled by this codec",
            "PIC +9(5)                        | PIC +9(5) is not handled by this codec",
            "PIC X(8) BLANK WHEN ZERO         | clause 'BLANK WHEN ZERO' is not handled by this codec"})
    void e5_fieldThatIsNotPlainDisplayIsRefusedNotGuessed(String clauses, String refusal) {
        RecordLayout layout = Copybook.parse("TESTCPY", """
                       01  TEST-RECORD.
                           05  TEST-KEY                PIC X(04).
                           05  TEST-ODD                %s.
                           05  TEST-AFTER              PIC 9(02).
                """.formatted(clauses));

        LayoutField odd = layout.field("TEST-ODD");
        assertEquals(FieldKind.REFUSED, odd.kind());
        assertEquals(refusal, odd.refusal());
    }

    @Test
    void readsFixedFormat_sequenceColumnsCommentsTabsContinuedEntriesAndPictureForms() {
        // columns 1-6 and 73-80 are not data; * in column 7 is a comment; an entry ends at its period
        RecordLayout layout = Copybook.parse("TESTCPY", String.join("\n",
                "000100* A comment with PIC X(99). in it",
                "000200 01  TEST-RECORD.                                                 00000200",
                "000300     05  TEST-GROUP.                                              COMP-3  ",
                "\t\t10 TEST-CODE PIC XX.",
                "           10 TEST-COUNT",
                "              PICTURE IS 999.",
                "       05 TEST-RATE pic s9v9(3) usage display.",
                "           88 TEST-RATE-ZERO VALUE 0.",
                "       05 FILLER PIC X(02).",
                "       05 FILLER PIC 9(03)."));

        assertEquals(List.of(
                new LayoutField("TEST-CODE", FieldKind.TEXT, 0, 2, 0, null),
                new LayoutField("TEST-COUNT", FieldKind.UNSIGNED_ZONED, 2, 3, 0, null),
                new LayoutField("TEST-RATE", FieldKind.SIGNED_ZONED, 5, 4, 3, null),
                new LayoutField("FILLER", FieldKind.FILLER, 9, 2, 0, null),
                new LayoutField("FILLER-2", FieldKind.FILLER, 11, 3, 0, null)), layout.fields());
        assertEquals(14, layout.length());
    }

    @Test
    void entryCutAtColumn72IsRefusedNotMergedSilently() {
        // the period of TEST-LONG sits in column 73, so the entry runs into the next one
        RecordLayout layout = Copybook.parse("TESTCPY", String.join("\n",
                "       01  TEST-RECORD.",
                "           05  TEST-LONG                                   PIC S9(09)V99.",
                "           05  TEST-NEXT                                   PIC X(02)."));

        assertEquals(FieldKind.REFUSED, layout.field("TEST-LONG").kind());
        assertEquals("clause '05 TEST-NEXT' is not handled by this codec", layout.field("TEST-LONG").refusal());
    }

    @Test
    void valueClauseDoesNotChangeTheField() {
        RecordLayout layout = Copybook.parse("TESTCPY", String.join("\n",
                "       01  TEST-RECORD.",
                "           05  TEST-NAME     PIC X(06) VALUE 'System'.",
                "           05  TEST-COUNT    PIC 9(02) USAGE IS DISPLAY VALUE ZERO."));

        assertEquals(List.of(
                new LayoutField("TEST-NAME", FieldKind.TEXT, 0, 6, 0, null),
                new LayoutField("TEST-COUNT", FieldKind.UNSIGNED_ZONED, 6, 2, 0, null)), layout.fields());
    }

    @Test
    void textWithoutARecordDescriptionIsNotALayout() {
        assertThrows(IllegalArgumentException.class, () -> Copybook.parse("EMPTY", "      * nothing here\n"));
    }
}
