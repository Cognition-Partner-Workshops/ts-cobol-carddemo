package com.carddemo.interest.batch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.carddemo.interest.codec.FixtureCodePage;
import com.carddemo.interest.port.RunLog;

/**
 * FI-3: the run log as the file sysout/INTCALC.STEP15.txt of the output directory, in the recorded
 * format (FI-2): one line per DISPLAY, ISO-8859-1. Lines are appended as the calculation produces
 * them; the length of the file is part of the step's restart state, and a restart cuts the file
 * back to the length saved with the last commit (architecture.md section 6), so no line is missing
 * or written twice.
 */
final class RunLogFile implements RunLog, AutoCloseable {

    static final String RELATIVE_PATH = "sysout/INTCALC.STEP15.txt";
    private static final Charset CHARSET = FixtureCodePage.CHARSET;

    private final FileChannel file;

    private RunLogFile(FileChannel file) {
        this.file = file;
    }

    /** A new run: an empty log, replacing what an earlier run left at the same place. */
    static RunLogFile create(Path outputDir) {
        return open(outputDir, 0L);
    }

    /** A restart: the log of the failed execution, cut back to the length saved with its last commit. */
    static RunLogFile cutBackTo(Path outputDir, long committedLength) {
        return open(outputDir, committedLength);
    }

    private static RunLogFile open(Path outputDir, long length) {
        Path path = outputDir.resolve(RELATIVE_PATH);
        try {
            Files.createDirectories(path.getParent());
            FileChannel file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            file.truncate(length);
            file.position(length);
            return new RunLogFile(file);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot open the run log", e);
        }
    }

    @Override
    public void line(String text) {
        append(text);
    }

    @Override
    public void echo(String recordImage) {
        append(recordImage);
    }

    private void append(String text) {
        try {
            file.write(ByteBuffer.wrap((text + "\n").getBytes(CHARSET)));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the run log", e);
        }
    }

    /** The length to save with a commit: everything written so far, forced to disk first. */
    long committedLength() {
        try {
            file.force(true);
            return file.position();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the run log", e);
        }
    }

    @Override
    public void close() {
        try {
            file.close();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot close the run log", e);
        }
    }
}
