package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/** The run log of the table step and its restart boundary (INT-T02, architecture.md section 6). */
class RunLogFileTest {

    @TempDir
    Path temp;

    private Path output() {
        return temp.resolve("actual");
    }

    private Path workFile() {
        return temp.resolve("actual.calc-runlog-run1");
    }

    private RunLogFile open() {
        return new RunLogFile(output(), "run1", StandardCharsets.ISO_8859_1);
    }

    private String text(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.ISO_8859_1);
    }

    @Test
    void intT02_linesReachTheFileWithTheCommitAndTheLengthIsItsBytes() throws IOException {
        RunLogFile log = open();
        log.startEmpty();
        log.line("START");
        log.echo("000000020010100010000010000{                      ");

        assertEquals("", text(workFile()));
        // 5 + 1 and 50 + 1 bytes: every line ends with one line feed
        assertEquals(57L, log.commitLength());
        assertEquals("START\n000000020010100010000010000{                      \n", text(workFile()));
        log.close();
    }

    @Test
    void intT02_restartCutsTheLogBackToTheCommittedLength() throws IOException {
        RunLogFile first = open();
        first.startEmpty();
        first.line("START");
        first.line("account 1");
        long committed = first.commitLength();
        first.line("account 2, chunk that fails after its lines reached the file");
        first.commitLength();
        first.close();
        assertEquals(16L, committed);

        RunLogFile restarted = open();
        restarted.resumeAt(committed);
        restarted.line("account 2");
        restarted.line("END");
        restarted.complete();

        // no line missing, none written twice, and the line of the failed chunk is gone
        assertEquals("START\naccount 1\naccount 2\nEND\n", text(output().resolve("sysout/INTCALC.STEP15.txt")));
        assertFalse(Files.exists(workFile()));
    }

    @Test
    void intT02_uncommittedLinesOfAFailedChunkAreNotInTheFile() throws IOException {
        RunLogFile log = open();
        log.startEmpty();
        log.line("START");
        log.commitLength();
        log.line("line of a chunk that never commits");
        log.close();

        assertEquals("START\n", text(workFile()));
        assertFalse(Files.exists(output()));
    }

    @Test
    void intT02_completedLogReplacesTheOneOfAnEarlierRunInTheOutputDirectory() throws IOException {
        Files.createDirectories(output().resolve("sysout"));
        Files.writeString(output().resolve("sysout/INTCALC.STEP15.txt"), "earlier run\n");
        RunLogFile log = open();
        log.startEmpty();
        log.line("START");
        log.complete();

        assertEquals("START\n", text(output().resolve("sysout/INTCALC.STEP15.txt")));
    }

    @Test
    void intR08_restartWithoutTheRunLogStopsTheRun() {
        InterestRunStoppedException missing = assertThrows(InterestRunStoppedException.class, () -> open().resumeAt(6L));
        assertEquals(StopReason.WRITE_OUTPUT, missing.reason());
    }

    @Test
    void intR08_restartWithAShorterRunLogThanCommittedStopsTheRun() {
        RunLogFile first = open();
        first.startEmpty();
        first.line("START");
        first.commitLength();
        first.close();

        InterestRunStoppedException tooShort = assertThrows(InterestRunStoppedException.class, () -> open().resumeAt(60L));
        assertEquals(StopReason.WRITE_OUTPUT, tooShort.reason());
        assertTrue(tooShort.getMessage().contains("shorter"));
    }
}
