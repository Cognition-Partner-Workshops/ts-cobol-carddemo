package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;

/** The layouts of the five record classes against the copybooks they are mapped from (FI-1). */
class CopybookTest {

    private static final Path COPYBOOKS = Path.of("..", "..", "app", "cpy");

    private static RecordLayout parsed(String copybook) throws IOException {
        return Copybook.parse(copybook, Files.readString(COPYBOOKS.resolve(copybook + ".cpy"), StandardCharsets.ISO_8859_1));
    }

    @Test
    void fi1_theFiveLayoutsAreTheCopybooks() throws IOException {
        // record lengths from the copybook headers (RECLN) and tools/runjcl/chains.json
        assertEquals(parsed("CVACT01Y"), AccountRecord.LAYOUT);
        assertEquals(300, AccountRecord.LAYOUT.length());
        assertEquals(parsed("CVACT03Y"), CardXrefRecord.LAYOUT);
        assertEquals(50, CardXrefRecord.LAYOUT.length());
        assertEquals(parsed("CVTRA01Y"), CategoryBalanceRecord.LAYOUT);
        assertEquals(50, CategoryBalanceRecord.LAYOUT.length());
        assertEquals(parsed("CVTRA02Y"), DisclosureGroupRecord.LAYOUT);
        assertEquals(50, DisclosureGroupRecord.LAYOUT.length());
        assertEquals(parsed("CVTRA05Y"), TransactionRecord.LAYOUT);
        assertEquals(350, TransactionRecord.LAYOUT.length());
    }

    @Test
    void dataMap_offsetsAndLengthsOfCvtra01y() {
        // data-map rows CVTRA01Y: offsets from 0, group item TRAN-CAT-KEY is not a field
        assertEquals(List.of(
                new FieldLayout("TRANCAT-ACCT-ID", Kind.UNSIGNED, 0, 11, 0, ""),
                new FieldLayout("TRANCAT-TYPE-CD", Kind.TEXT, 11, 2, 0, ""),
                new FieldLayout("TRANCAT-CD", Kind.UNSIGNED, 13, 4, 0, ""),
                new FieldLayout("TRAN-CAT-BAL", Kind.SIGNED, 17, 11, 2, ""),
                new FieldLayout("FILLER", Kind.FILLER, 28, 22, 0, "")),
                CategoryBalanceRecord.LAYOUT.fields());
    }

    @Test
    void e5_whatTheCodecDoesNotKnowIsRefusedNotGuessed() {
        RecordLayout layout = Copybook.parse("REFUSED", """
              * a comment line
               01  REFUSED-RECORD.
                   05  R-ID                 PIC 9(04).
                   05  R-PACKED             PIC S9(09)V99 COMP-3.
                   05  R-BINARY             PIC S9(04) USAGE IS COMP.
                   05  R-GROUP              USAGE BINARY.
                       10  R-INHERITED      PIC 9(04).
                   05  R-EDITED             PIC ZZ9.99.
                   05  R-TABLE              PIC X(02) OCCURS 3 TIMES.
                   05  R-SEPARATE           PIC S9(03) SIGN IS LEADING SEPARATE.
                   05  R-REDEFINES          REDEFINES R-ID PIC X(04).
                   05  R-TEXT               PICTURE IS XX.
                   05  R-SHORT              PIC S99V9 USAGE DISPLAY.
            """);

        assertEquals(List.of(
                "R-ID UNSIGNED ",
                "R-PACKED REFUSED USAGE COMP-3 is not DISPLAY (rule E5)",
                "R-BINARY REFUSED USAGE COMP is not DISPLAY (rule E5)",
                "R-INHERITED REFUSED USAGE BINARY is not DISPLAY (rule E5)",
                "R-EDITED REFUSED PICTURE ZZ9.99 is not supported",
                "R-TABLE REFUSED clause OCCURS is not supported",
                "R-SEPARATE REFUSED clause SIGN is not supported",
                "R-REDEFINES REFUSED clause REDEFINES is not supported",
                "R-TEXT TEXT ",
                "R-SHORT SIGNED "),
                layout.fields().stream().map(f -> f.name() + " " + f.kind() + " " + f.refusal()).toList());
        assertEquals(2, layout.field("R-TEXT").length());
        assertEquals(new FieldLayout("R-SHORT", Kind.SIGNED, 6, 3, 1, ""), layout.field("R-SHORT"));
    }
}
