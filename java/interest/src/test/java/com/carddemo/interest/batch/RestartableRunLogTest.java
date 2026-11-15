package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.RunLogLines;

/** The run log and the restart state of calcInterestStep on the tables (architecture.md section 6, REL-11). */
class RestartableRunLogTest {

    private static final String ECHO = "000000020010100010000010000{                      ";

    @TempDir
    Path temp;

    private Path file() {
        return temp.resolve("sysout").resolve("INTCALC.STEP15.txt");
    }

    @Test
    void intT02_linesAndEchoesAreWrittenAsTheyAre() throws Exception {
        try (RestartableRunLog runLog = RestartableRunLog.create(file())) {
            runLog.line(RunLogLines.START);
            runLog.echo(ECHO);
            runLog.line(RunLogLines.END);
        }

        // the 50-byte record keeps its trailing spaces; every line ends with a line feed
        assertEquals("START OF EXECUTION OF PROGRAM CBACT04C\n" + ECHO + "\nEND OF EXECUTION OF PROGRAM CBACT04C\n",
                Files.readString(file(), StandardCharsets.ISO_8859_1));
    }

    @Test
    void rel11_resumeCutsTheLogBackToTheCommittedLength() throws Exception {
        long committed;
        try (RestartableRunLog runLog = RestartableRunLog.create(file())) {
            runLog.line(RunLogLines.START);
            runLog.echo(ECHO);
            committed = runLog.committedLength();
            runLog.echo("a record of the chunk that was rolled back");
        }
        // 38 characters + line feed, 50 + line feed
        assertEquals(39 + 51, committed);

        try (RestartableRunLog runLog = RestartableRunLog.resume(file(), committed)) {
            runLog.line(RunLogLines.END);
        }

        assertEquals("START OF EXECUTION OF PROGRAM CBACT04C\n" + ECHO + "\nEND OF EXECUTION OF PROGRAM CBACT04C\n",
                Files.readString(file(), StandardCharsets.ISO_8859_1));
    }

    @Test
    void rel11_createReplacesTheLogOfAnEarlierLaunch() throws Exception {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "left by another run\n");

        RestartableRunLog.create(file()).close();

        assertEquals(0, Files.size(file()));
    }

    @Test
    void rel11_aLogShorterThanTheCommitIsNotResumed() throws Exception {
        RestartableRunLog.create(file()).close();

        assertThrows(InterestRunStoppedException.class, () -> RestartableRunLog.resume(file(), 10));
        assertThrows(InterestRunStoppedException.class, () -> RestartableRunLog.resume(temp.resolve("missing"), 0));
    }

    @Test
    void rel11_restartStateSurvivesTheExecutionContext() {
        ExecutionContext context = new ExecutionContext();
        assertEquals(false, CalcRestartState.savedIn(context));
        CalcRestartState afterAChunk = new CalcRestartState(7, OptionalLong.of(2002), 2, 2, 2, 141);

        afterAChunk.saveIn(context);

        assertEquals(true, CalcRestartState.savedIn(context));
        assertEquals(afterAChunk, CalcRestartState.from(context));
    }

    @Test
    void rel11_stateBeforeTheFirstAccountHasNoLastAccount() {
        ExecutionContext context = new ExecutionContext();

        CalcRestartState.beforeTheFirstAccount(7, 39).saveIn(context);

        assertEquals(new CalcRestartState(7, OptionalLong.empty(), 0, 0, 0, 39), CalcRestartState.from(context));
    }
}
