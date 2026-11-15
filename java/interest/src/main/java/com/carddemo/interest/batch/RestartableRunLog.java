package com.carddemo.interest.batch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.carddemo.interest.codec.FixtureCodePage;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.RunLog;

/**
 * The run log (legacy SYSOUT, INT-T02) as a file that is written while the step runs. Its length
 * is saved with every commit; a restart cuts the file back to that length, so no line of a
 * rolled-back chunk stays and none is written twice (architecture.md section 6, standards S6.3).
 */
public final class RestartableRunLog implements RunLog, AutoCloseable {

    private final FileChannel channel;

    private RestartableRunLog(FileChannel channel) {
        this.channel = channel;
    }

    /** A new, empty run log; a file left by an earlier launch into the same directory is replaced. */
    public static RestartableRunLog create(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            return new RestartableRunLog(FileChannel.open(file, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE));
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot create the run log", e);
        }
    }

    /** The run log of a restarted step, cut back to the length the last commit saved. */
    public static RestartableRunLog resume(Path file, long committedLength) {
        try {
            FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE);
            if (channel.size() < committedLength) {
                channel.close();
                throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                        "the run log is shorter than the last commit left it; the run cannot be restarted");
            }
            channel.truncate(committedLength);
            channel.position(committedLength);
            return new RestartableRunLog(channel);
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot reopen the run log", e);
        }
    }

    @Override
    public void line(String text) {
        try {
            channel.write(ByteBuffer.wrap((text + "\n").getBytes(FixtureCodePage.CHARSET)));
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot write the run log", e);
        }
    }

    @Override
    public void echo(String recordImage) {
        line(recordImage);
    }

    /** The length to save with a commit; everything up to it is on disk. */
    public long committedLength() {
        try {
            channel.force(false);
            return channel.position();
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot write the run log", e);
        }
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot close the run log", e);
        }
    }
}
