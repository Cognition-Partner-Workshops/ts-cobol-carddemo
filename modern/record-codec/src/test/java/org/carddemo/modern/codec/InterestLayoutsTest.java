package org.carddemo.modern.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class InterestLayoutsTest {
    @Test
    void everyElementaryFieldMatchesItsCopybook() throws Exception {
        var pattern = Pattern.compile("\\s+\\d+\\s+([A-Z0-9-]+)\\s+PIC\\s+(S?)([X9])\\((\\d+)\\)(V99)?\\.");
        for (var layout : List.of(Layouts.ACCOUNT, Layouts.XREF, Layouts.CATEGORY_BALANCE, Layouts.DISCLOSURE, Layouts.TRAN)) {
            int offset = 0;
            int index = 0;
            for (var line : Files.readAllLines(Path.of("../../app/cpy/" + layout.copybook() + ".cpy"))) {
                var match = pattern.matcher(line);
                if (!match.find()) continue;
                int scale = match.group(5) == null ? 0 : 2;
                int length = Integer.parseInt(match.group(4)) + scale;
                var field = layout.fields().get(index++);
                assertEquals(offset, field.offset(), match.group(1));
                assertEquals(length, field.length(), match.group(1));
                assertEquals(scale, field.scale(), match.group(1));
                assertEquals(!match.group(2).isEmpty(), field.signed(), match.group(1));
                if (match.group(1).equals("FILLER")) assertEquals(Field.Kind.FILLER, field.kind());
                else {
                    assertEquals(match.group(1), field.name());
                    assertEquals(match.group(3).equals("X") ? Field.Kind.TEXT : Field.Kind.ZONED, field.kind());
                }
                offset += length;
            }
            assertEquals(layout.fields().size(), index);
            assertEquals(offset, layout.length());
            var encoded = layout.encode(java.util.Map.of());
            assertEquals(encoded, layout.encode(layout.decode(encoded)));
        }
    }
}
