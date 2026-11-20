package com.carddemo.posting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The batch step contract the JCL runner (tools/runjcl/candidate.py) gives a migrated program:
 * datasets as DD_&lt;ddname&gt; paths holding fixed-length unblocked records (KSDS clusters arrive
 * as flat images and are reloaded when the step changes them), STDOUT as SYSOUT, the exit
 * status as the step return code, and GnuCOBOL's frozen clock in COB_CURRENT_DATE.
 */
@Component
public class StepFiles {

    public static final String ABEND_MESSAGE = "CEE3ABD: USER ABEND U+0000000999";
    public static final int ABEND_RETURN_CODE = 12;

    private final String clock;
    private final List<String> sysout = new ArrayList<>();
    private int returnCode;
    private boolean abended;

    public StepFiles(@Value("${carddemo.clock:}") String clock) {
        this.clock = clock;
    }

    public static Path dd(String ddname) {
        String path = System.getenv("DD_" + ddname);
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("DD " + ddname + " is not allocated");
        }
        return Path.of(path);
    }

    public static List<String> readFixed(Path file, int lrecl) {
        List<String> records = new ArrayList<>();
        if (!Files.exists(file)) {
            return records;
        }
        try {
            String data = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
            for (int i = 0; i + lrecl <= data.length(); i += lrecl) {
                records.add(data.substring(i, i + lrecl));
            }
            return records;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeFixed(Path file, List<String> records, int lrecl) {
        StringBuilder data = new StringBuilder(records.size() * lrecl);
        records.forEach(record -> data.append(com.carddemo.posting.record.Text.pad(record, lrecl)));
        try {
            Files.write(file, data.toString().getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Z-GET-DB2-FORMAT-TIMESTAMP: FUNCTION CURRENT-DATE reshaped to
     * {@code YYYY-MM-DD-HH.MM.SS.hh0000} (hundredths, then a literal '0000').
     */
    public String db2Timestamp() {
        String now = clock.isBlank()
                ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"))
                : clock;
        return now.substring(0, 10) + "-" + now.substring(11, 22).replace(':', '.') + "0000";
    }

    public synchronized void display(String line) {
        sysout.add(line);
    }

    public synchronized List<String> sysout() {
        return List.copyOf(sysout);
    }

    public void returnCode(int rc) {
        returnCode = rc;
    }

    public int returnCode() {
        return abended ? ABEND_RETURN_CODE : returnCode;
    }

    /** 9910-DISPLAY-IO-STATUS + 9999-ABEND-PROGRAM (CALL 'CEE3ABD' with ABCODE 999). */
    public LegacyAbend abend(String message, String ioStatus) {
        display(message);
        display("FILE STATUS IS: NNNN00" + ioStatus);
        display("ABENDING PROGRAM");
        display(ABEND_MESSAGE);
        abended = true;
        return new LegacyAbend(message);
    }

    public boolean abended() {
        return abended;
    }
}
