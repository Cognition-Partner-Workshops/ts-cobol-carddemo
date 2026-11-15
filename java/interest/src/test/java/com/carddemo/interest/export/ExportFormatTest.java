package com.carddemo.interest.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ExportFormatTest {

    @Test
    void format_fixtureAndHostAreTheTwoParameterValues() {
        assertEquals(ExportFormat.FIXTURE, ExportFormat.ofParameter("fixture"));
        assertEquals(ExportFormat.HOST, ExportFormat.ofParameter("host"));
    }

    @Test
    void format_anotherValueIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ExportFormat.ofParameter("FIXTURE"));
        assertThrows(IllegalArgumentException.class, () -> ExportFormat.ofParameter(null));
    }

    @Test
    void format_fixtureFileIsDsnDotTxtAndHostFileIsTheDsn() {
        assertEquals("AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt", ExportFormat.FIXTURE.datasetFile("AWS.M2.CARDDEMO.SYSTRAN.G0001V00"));
        assertEquals("AWS.M2.CARDDEMO.SYSTRAN.G0001V00", ExportFormat.HOST.datasetFile("AWS.M2.CARDDEMO.SYSTRAN.G0001V00"));
    }

    /** FI-2: one record per line with the trailing spaces cut (tools/parity/record.py). */
    @Test
    void format_fixtureRecordIsALineWithoutTrailingSpaces() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExportFormat.FIXTURE.write(out, "AB  ".getBytes(StandardCharsets.ISO_8859_1));
        ExportFormat.FIXTURE.write(out, "C   ".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("AB\nC\n", out.toString(StandardCharsets.ISO_8859_1));
    }

    /** Host: the full record, nothing between records. */
    @Test
    void format_hostRecordKeepsItsLengthAndHasNoSeparator() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] first = {(byte) 0xC1, 0x40, 0x40};
        byte[] second = {(byte) 0xC2, 0x40, 0x40};
        ExportFormat.HOST.write(out, first);
        ExportFormat.HOST.write(out, second);
        assertArrayEquals(new byte[] {(byte) 0xC1, 0x40, 0x40, (byte) 0xC2, 0x40, 0x40}, out.toByteArray());
    }
}
