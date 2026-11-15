package com.carddemo.interest.batch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.RunLog;

/**
 * The run log of calcInterestStep on the tables: the legacy SYSOUT, byte for byte (INT-T02,
 * standards S6.5), kept in a work file beside the output directory while the step runs and moved to
 * {@code sysout/INTCALC.STEP15.txt} when the step completes.
 *
 * <p>Restart (architecture.md section 6): the lines of a chunk reach the file when the chunk
 * commits, and the length of the file at that moment is part of the step's restart state. A
 * restart cuts the file back to that length, so no line is missing and none is written twice.
 */
final class RunLogFile implements RunLog {

    static final String RUN_LOG_FILE = "sysout/INTCALC.STEP15.txt";
    private static final byte[] LINE_END = {'\n'};

    private final Path workFile;
    private final Path completedFile;
    private final Charset charset;
    private final ByteArrayOutputStream uncommitted = new ByteArrayOutputStream();
    private FileChannel channel;

    RunLogFile(Path outputDir, String runId, Charset charset) {
        Path output = outputDir.toAbsolutePath().normalize();
        this.workFile = output.resolveSibling(output.getFileName() + ".calc-runlog-" + runId);
        this.completedFile = output.resolve(RUN_LOG_FILE);
        this.charset = charset;
    }

    /** A new run: an empty log; what an abandoned run with the same runId left is replaced. */
    void startEmpty() {
        try {
            Files.createDirectories(workFile.getParent());
            channel = FileChannel.open(workFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot create the run log", e);
        }
    }

    /** A restart: the log is cut back to the length saved with the last commit. */
    void resumeAt(long committedLength) {
        try {
            channel = FileChannel.open(workFile, StandardOpenOption.WRITE);
            if (channel.size() < committedLength) {
                channel.close();
                channel = null;
                throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                        "the run log of the run to restart is shorter than its committed length");
            }
            channel.truncate(committedLength);
            channel.position(committedLength);
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot reopen the run log of the run to restart", e);
        }
    }

    @Override
    public void line(String text) {
        uncommitted.writeBytes(text.getBytes(charset));
        uncommitted.writeBytes(LINE_END);
    }

    @Override
    public void echo(String recordImage) {
        line(recordImage);
    }

    /** Writes the lines since the last call to the file and returns its length: the value to save with the commit. */
    long commitLength() {
        try {
            ByteBuffer lines = ByteBuffer.wrap(uncommitted.toByteArray());
            while (lines.hasRemaining()) {
                channel.write(lines);
            }
            uncommitted.reset();
            channel.force(false);
            return channel.position();
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot write the run log", e);
        }
    }

    /** Normal end: the work file becomes sysout/INTCALC.STEP15.txt of the output directory. */
    void complete() {
        commitLength();
        close();
        try {
            Files.createDirectories(completedFile.getParent());
            Files.move(workFile, completedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot move the run log into place", e);
        }
    }

    /** The work file stays: a failed run needs it for its restart. */
    void close() {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
            channel = null;
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot close the run log", e);
        }
    }
}
