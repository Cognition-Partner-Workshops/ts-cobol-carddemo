package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/** How records lie in a file: fixture lines (FI-2) and host fixed-length blocks. */
class RecordFormatTest {

    private static final RecordLayout LAYOUT = RecordLayout.of("TESTCPY").unsigned("CODE", 4).text("NAME", 4).build();

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void fixtureLine_trailingSpacesAreNotInTheFileAndComeBack() {
        List<byte[]> records = RecordFormat.fixture().records(ascii("0001AB\n0002\r\n0003ABCD"), LAYOUT, "TEST.PS.txt");

        assertEquals(List.of("0001AB  ", "0002    ", "0003ABCD"),
                records.stream().map(record -> new String(record, StandardCharsets.ISO_8859_1)).toList());
        assertArrayEquals(ascii("0001AB\n0002\n0003ABCD\n"), RecordFormat.fixture().content(records));
    }

    @Test
    void fixtureLine_longerThanTheLayoutIsRejectedNotCut() {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> RecordFormat.fixture().records(ascii("0001AB\n0002ABCDE\n"), LAYOUT, "TEST.PS.txt"));

        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS.txt record 2 record TESTCPY: "
                + "the record is 9 bytes, the layout is 8", stop.getMessage());
    }

    @Test
    void hostFile_fixedLengthRecordsWithoutSeparator() {
        // 0001AB and 0002 in cp037, padded with X'40'
        byte[] content = java.util.HexFormat.of().parseHex("F0F0F0F1C1C24040" + "F0F0F0F240404040");

        List<byte[]> records = RecordFormat.host().records(content, LAYOUT, "TEST.PS");

        assertEquals(2, records.size());
        assertEquals("AB", RecordFormat.host().codec(LAYOUT).decode(records.get(0), new RecordOrigin("TEST.PS", 1)).text("NAME"));
        assertArrayEquals(content, RecordFormat.host().content(records));
    }

    @Test
    void hostFile_thatIsNotAWholeNumberOfRecordsIsRejected() {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> RecordFormat.host().records(new byte[19], LAYOUT, "TEST.PS"));

        assertEquals("RECORD_REJECTED (INT-R08): file TEST.PS record 3 record TESTCPY: "
                + "the record is 3 bytes, the layout is 8", stop.getMessage());
    }
}
