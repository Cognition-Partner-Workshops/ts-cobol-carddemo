package com.carddemo.interest.codec;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * How the records of a dataset lie in a file, and in which code page.
 * Fixture format (FI-2, tools/parity/record.py): one record per line, trailing spaces of the record
 * not written, ISO-8859-1. Host format: fixed-length records one after the other, no separator,
 * EBCDIC. A record longer than its layout, or a host file that is not a whole number of records,
 * is rejected; nothing is cut.
 */
public final class RecordFormat {

    private final CodePage codePage;
    private final boolean lines;

    private RecordFormat(CodePage codePage, boolean lines) {
        this.codePage = codePage;
        this.lines = lines;
    }

    public static RecordFormat fixture() {
        return new RecordFormat(CodePage.fixture(), true);
    }

    /** Host format in the unconfirmed default code page cp037 (REL-08). */
    public static RecordFormat host() {
        return new RecordFormat(CodePage.host(), false);
    }

    public static RecordFormat host(Charset ebcdicCharset) {
        return new RecordFormat(CodePage.host(ebcdicCharset), false);
    }

    public CodePage codePage() {
        return codePage;
    }

    public RecordCodec codec(RecordLayout layout) {
        return new RecordCodec(layout, codePage);
    }

    /** The records of a file, each of the length of the layout. */
    public List<byte[]> records(byte[] content, RecordLayout layout, String file) {
        return lines ? fromLines(content, layout, file) : fromBlocks(content, layout, file);
    }

    /** The content of a file holding the records. */
    public byte[] content(List<byte[]> records) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        for (byte[] record : records) {
            int end = record.length;
            while (lines && end > 0 && record[end - 1] == codePage.space()) {
                end--;
            }
            content.write(record, 0, end);
            if (lines) {
                content.write('\n');
            }
        }
        return content.toByteArray();
    }

    private List<byte[]> fromLines(byte[] content, RecordLayout layout, String file) {
        List<byte[]> records = new ArrayList<>();
        int start = 0;
        while (start < content.length) {
            int end = start;
            while (end < content.length && content[end] != '\n') {
                end++;
            }
            int lineEnd = end > start && content[end - 1] == '\r' ? end - 1 : end;
            int length = lineEnd - start;
            if (length > layout.length()) {
                throw wrongLength(file, records.size() + 1, layout, "the record is " + length + " bytes");
            }
            byte[] record = new byte[layout.length()];
            java.util.Arrays.fill(record, codePage.space());
            System.arraycopy(content, start, record, 0, length);
            records.add(record);
            start = end + 1;
        }
        return records;
    }

    private List<byte[]> fromBlocks(byte[] content, RecordLayout layout, String file) {
        int length = layout.length();
        List<byte[]> records = new ArrayList<>();
        for (int start = 0; start + length <= content.length; start += length) {
            records.add(java.util.Arrays.copyOfRange(content, start, start + length));
        }
        if (content.length % length != 0) {
            throw wrongLength(file, records.size() + 1, layout, "the record is " + content.length % length + " bytes");
        }
        return records;
    }

    private static InterestRunStoppedException wrongLength(String file, int recordNumber, RecordLayout layout, String found) {
        return new InterestRunStoppedException(StopReason.RECORD_REJECTED, new RecordOrigin(file, recordNumber)
                + " record " + layout.copybook() + ": " + found + ", the layout is " + layout.length());
    }
}
