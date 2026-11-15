package com.carddemo.interest.codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * How fixed-length records lie in a file.
 * Recorded fixture format (FI-2): one record per line, trailing spaces lost; a line gets them back
 * and an output record loses them again (tools/parity/record.py:51, 104). Host format: the records
 * follow each other with no separator. Nothing is cut here: a line or a last block of the wrong
 * length is handed on as it is and {@link RecordCodec} rejects it, naming file and record.
 */
public final class RecordImage {

    private RecordImage() {
    }

    public static byte[] fromLine(String line, int recordLength, CodePage codePage) {
        byte[] bytes = line.getBytes(codePage.charset());
        if (bytes.length >= recordLength) {
            return bytes;
        }
        byte[] record = Arrays.copyOf(bytes, recordLength);
        Arrays.fill(record, bytes.length, recordLength, codePage.space());
        return record;
    }

    public static String toLine(byte[] record, CodePage codePage) {
        int end = record.length;
        while (end > 0 && record[end - 1] == codePage.space()) {
            end--;
        }
        return new String(record, 0, end, codePage.charset());
    }

    public static List<byte[]> fromBlock(byte[] content, int recordLength) {
        List<byte[]> records = new ArrayList<>();
        for (int start = 0; start < content.length; start += recordLength) {
            records.add(Arrays.copyOfRange(content, start, Math.min(start + recordLength, content.length)));
        }
        return records;
    }
}
