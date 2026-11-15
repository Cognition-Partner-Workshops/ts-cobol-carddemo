package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** FI-1: the layouts the codec uses are the copybooks under app/cpy, field for field. */
class CopybookLayoutTest {

    private static RecordLayout layoutOf(String copybook) {
        return switch (copybook) {
            case "CVACT01Y" -> AccountRecord.LAYOUT;
            case "CVACT03Y" -> CardXrefRecord.LAYOUT;
            case "CVTRA01Y" -> CategoryBalanceRecord.LAYOUT;
            case "CVTRA02Y" -> DisclosureGroupRecord.LAYOUT;
            case "CVTRA05Y" -> TransactionRecord.LAYOUT;
            default -> throw new IllegalArgumentException(copybook);
        };
    }

    @ParameterizedTest(name = "{0}: {1} fields, {2} bytes")
    @CsvSource({
            "CVACT01Y, 13, 300",
            "CVACT03Y, 4, 50",
            "CVTRA01Y, 5, 50",
            "CVTRA02Y, 5, 50",
            "CVTRA05Y, 14, 350"})
    void fi1_layoutEqualsTheCopybookSource(String copybook, int fieldCount, int recordLength) {
        RecordLayout parsed = CodecFixtures.copybook(copybook);

        assertEquals(parsed.fields(), layoutOf(copybook).fields());
        assertEquals(fieldCount, parsed.fields().size());
        assertEquals(recordLength, parsed.length());
    }

    @Test
    void picturesAreReadFromTheCopybookText() {
        RecordLayout layout = CopybookParser.parse("SYNTH", String.join("\n",
                "      * comment line",
                "       01  SYNTH-RECORD.",
                "           05  SYNTH-KEY.",
                "              10  SYNTH-ID          PIC 9(11).",
                "              10  SYNTH-TYPE        PIC XX.",
                "           05  SYNTH-AMT            PIC S9(09)V99.",
                "           05  SYNTH-RATE           PIC S9(4)V9(2)",
                "                                    USAGE DISPLAY.",
                "           05  FILLER               PIC X(05)."));

        assertEquals(new FieldSpec("SYNTH-ID", 0, FieldKind.UNSIGNED_ZONED, 0, 11, 0, "DISPLAY"), layout.fields().get(0));
        assertEquals(new FieldSpec("SYNTH-TYPE", 1, FieldKind.TEXT, 11, 2, 0, "DISPLAY"), layout.fields().get(1));
        assertEquals(new FieldSpec("SYNTH-AMT", 2, FieldKind.SIGNED_ZONED, 13, 11, 2, "DISPLAY"), layout.fields().get(2));
        assertEquals(new FieldSpec("SYNTH-RATE", 3, FieldKind.SIGNED_ZONED, 24, 6, 2, "DISPLAY"), layout.fields().get(3));
        assertEquals(new FieldSpec("FILLER", 4, FieldKind.FILLER, 30, 5, 0, "DISPLAY"), layout.fields().get(4));
        assertEquals(35, layout.length());
    }

    @Test
    void e5_usageOtherThanDisplayIsKeptForTheRefusal() {
        RecordLayout layout = CopybookParser.parse("SYNTH", String.join("\n",
                "       01  SYNTH-RECORD.",
                "           05  SYNTH-ID             PIC 9(11).",
                "           05  SYNTH-PACKED         PIC S9(7)V99 COMP-3.",
                "           05  SYNTH-BINARY         PIC S9(4) USAGE IS COMP."));

        assertEquals(FieldKind.NOT_DISPLAY, layout.fields().get(1).kind());
        assertEquals("COMP-3", layout.fields().get(1).usage());
        assertEquals("COMP", layout.fields().get(2).usage());
    }

    @ParameterizedTest
    @CsvSource({
            "REDEFINES, '           05  SYNTH-B REDEFINES SYNTH-A PIC X(02).'",
            "OCCURS, '           05  SYNTH-B PIC X(02) OCCURS 3 TIMES.'",
            "SEPARATE, '           05  SYNTH-B PIC S9(02) SIGN IS TRAILING SEPARATE.'"})
    void clausesAbsentFromTheFiveLayoutsAreRefused(String clause, String line) {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> CopybookParser.parse("SYNTH", "       01  SYNTH-RECORD.\n           05  SYNTH-A PIC X(02).\n" + line));

        assertEquals("SYNTH: field SYNTH-B: " + clause + " is not supported", refused.getMessage());
    }
}
