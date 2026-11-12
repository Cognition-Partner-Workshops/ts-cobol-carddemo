package org.carddemo.modern.codec;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrantypeInputTest {
    @Test
    void auditInlineCobolLayout() {
        var layout = TrantypeInput.LAYOUT;
        assertEquals(53, layout.length());
        assertEquals(java.util.List.of(Field.text("INPUT-REC-TYPE", 0, 1),
                Field.text("INPUT-REC-NUMBER", 1, 2), Field.text("INPUT-REC-DESC", 3, 50)), layout.fields());
        var encoded = layout.encode(Map.of("INPUT-REC-TYPE", "A", "INPUT-REC-NUMBER", "04", "INPUT-REC-DESC", "Fee"));
        assertEquals("A04Fee" + " ".repeat(47), encoded);
        assertEquals("04", layout.decode(encoded).get("INPUT-REC-NUMBER"));
        assertEquals("A04Fee", layout.encodeLine(layout.decode(encoded)));
    }

    @Test
    void emptyFieldsAreSpacesAndLongFieldsAreBounded() {
        assertEquals(" ".repeat(53), TrantypeInput.LAYOUT.encode(Map.of()));
        assertEquals(" ".repeat(50), TrantypeInput.LAYOUT.decode("").get("INPUT-REC-DESC"));
        assertEquals("X".repeat(50), TrantypeInput.LAYOUT.decode(TrantypeInput.LAYOUT.encode(
                Map.of("INPUT-REC-DESC", "X".repeat(51)))).get("INPUT-REC-DESC"));
    }

    @Test
    void nullInputIsRejected() {
        assertThrows(NullPointerException.class, () -> TrantypeInput.LAYOUT.decode(null));
    }
}
