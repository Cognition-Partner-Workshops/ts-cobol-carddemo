package com.carddemo.modern.trantype;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

/**
 * {@code SELECT TR-RECORD ASSIGN TO INPFILE}, a fixed-length sequential dataset (RECFM F).
 * The fixture's {@code input/INPFILE.txt} holds one record per line; like
 * {@code tools/parity/record.py materialise_inputs} each line is space-padded or cut to the LRECL.
 *
 * <ul>
 *   <li>{@link #open} is 0001-OPEN-FILES ({@code OPEN INPUT TR-RECORD}, DISPLAY 'OPEN FILE OK');</li>
 *   <li>{@link #read} is the READ of 1002-READ-RECORDS ({@code AT END MOVE 'Y' TO LASTREC});</li>
 *   <li>{@link #close} is 2001-CLOSE-STOP.</li>
 * </ul>
 */
public class InpfileReader implements ItemStreamReader<InputRecord> {

    private final Path dataset;
    private final int lrecl;
    private final Sysout sysout;
    private Iterator<String> records;

    public InpfileReader(Path dataset, int lrecl, Sysout sysout) {
        this.dataset = dataset;
        this.lrecl = lrecl;
        this.sysout = sysout;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        List<String> fixed = new ArrayList<>();
        try {
            if (Files.exists(dataset)) {
                for (String line : splitlines(Files.readString(dataset, StandardCharsets.ISO_8859_1))) {
                    fixed.add(line.length() >= lrecl ? line.substring(0, lrecl) : line + " ".repeat(lrecl - line.length()));
                }
            }
        } catch (IOException e) {
            sysout.display("OPEN FILE NOT OK");
            throw new UncheckedIOException(e);
        }
        records = fixed.iterator();
        sysout.display("OPEN FILE OK");
    }

    @Override
    public InputRecord read() {
        return records.hasNext() ? InputRecord.of(records.next()) : null;
    }

    @Override
    public void close() {
        records = null;
    }

    /** Python {@code str.splitlines()} for Latin-1 text, as record.py splits the fixture input. */
    static List<String> splitlines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r' || c == '\u000b' || c == '\u000c' || c == '\u001c' || c == '\u001d'
                    || c == '\u001e' || c == '\u0085') {
                lines.add(text.substring(start, i));
                i += (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') ? 2 : 1;
                start = i;
            } else {
                i++;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
        }
        return lines;
    }
}
