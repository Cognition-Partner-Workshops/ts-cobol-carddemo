package org.carddemo.modern.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The trantype layouts are defined in the programs themselves, so they are checked against the COBOL source. */
class TrantypeLayoutsTest {
    private static final Pattern ITEM = Pattern.compile("^\\s+(\\d+)\\s+([A-Z0-9-]+)\\s+PIC\\s+X\\((\\d+)\\)");
    private static final Path DB2 = Path.of("../../app/app-transaction-type-db2/cbl");

    private static List<String[]> items(String program, String group) throws Exception {
        var found = new ArrayList<String[]>();
        var level01 = Pattern.compile("^\\s+01\\s+([A-Z0-9-]+)");
        boolean inside = false;
        for (var raw : Files.readAllLines(DB2.resolve(program))) {
            var line = raw.length() > 72 ? raw.substring(0, 72) : raw;
            var start = level01.matcher(line);
            if (start.find()) inside = start.group(1).equals(group);
            var match = ITEM.matcher(line);
            if (inside && match.find()) found.add(new String[] {match.group(2), match.group(3)});
        }
        return found;
    }

    private static void assertMatches(RecordLayout layout, List<String[]> items) {
        assertEquals(items.size(), layout.fields().size());
        int offset = 0;
        for (int i = 0; i < items.size(); i++) {
            var field = layout.fields().get(i);
            assertEquals(items.get(i)[0], field.name());
            assertEquals(Field.Kind.TEXT, field.kind());
            assertEquals(offset, field.offset(), field.name());
            assertEquals(Integer.parseInt(items.get(i)[1]), field.length(), field.name());
            offset += field.length();
        }
        assertEquals(offset, layout.length());
    }

    @Test
    void updateRecordMatchesCobtupdtWorkingStorage() throws Exception {
        assertMatches(Layouts.TRANTYPE_UPDATE, items("COBTUPDT.cbl", "WS-INPUT-REC"));
        assertEquals(53, Layouts.TRANTYPE_UPDATE.length());
        var record = Layouts.TRANTYPE_UPDATE.decode("A04Authorization");
        assertEquals("A", record.get("INPUT-REC-TYPE"));
        assertEquals("04", record.get("INPUT-REC-NUMBER"));
        assertEquals(String.format("%-50s", "Authorization"), record.get("INPUT-REC-DESC"));
        assertEquals("A04Authorization", Layouts.TRANTYPE_UPDATE.encodeLine(record));
    }

    @Test
    void dynsqlRecordMatchesCbtrndynFileSection() throws Exception {
        assertMatches(Layouts.DYNSQL, items("CBTRNDYN.cbl", "DYNSQL-REC"));
        assertEquals(80, Layouts.DYNSQL.decode("").get("DYNSQL-REC").toString().length());
    }
}
