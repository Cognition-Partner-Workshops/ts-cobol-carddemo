package com.carddemo.posting.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.carddemo.posting.StepFiles;
import com.carddemo.posting.batch.PostTransactionsStep.Daily;
import com.carddemo.posting.batch.PostTransactionsStep.Outcome;
import com.carddemo.posting.store.PostingStore;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

class PostTransactionsStepTest {

    private final StepFiles run = new StepFiles("2022-07-05 10:00:00.00");
    private final PostingStore store = mock(PostingStore.class);
    private final PostTransactionsStep step = new PostTransactionsStep(run, store);
    private final Outcome rejected = new Outcome(new Daily("X".repeat(350), null), null,
            100, "INVALID CARD NUMBER FOUND");

    @Test
    void writesFixedRejectAndHandlesEmptyChunk() throws Exception {
        StringWriter text = new StringWriter();
        ReflectionTestUtils.setField(step, "rejectFile", new BufferedWriter(text));
        step.write(new Chunk<>());
        assertEquals("", text.toString());
        step.write(new Chunk<>(List.of(rejected)));
        assertEquals(430, text.toString().length());
        assertEquals("0100", text.toString().substring(350, 354));
        assertFalse(run.abended());
    }

    @Test
    void writeFailureAbendsWithoutThrowingOrProcessingLaterRecords() throws Exception {
        BufferedWriter writer = mock(BufferedWriter.class);
        doThrow(new IOException("disk error")).when(writer).write(anyString());
        ReflectionTestUtils.setField(step, "rejectFile", writer);
        step.write(new Chunk<>(List.of(rejected)));
        assertAbend("30");
        assertNull(step.process(new Daily("not processed", null)));
        assertNull(step.read());
        verifyNoInteractions(store);
        assertEquals(1, PostingJobConfig.CHUNK_SIZE);
    }

    @Test
    void flushFailureAbendsAndCloseDoesNotMaskIt() throws Exception {
        BufferedWriter writer = mock(BufferedWriter.class);
        doThrow(new IOException("No space left on device")).when(writer).flush();
        doThrow(new IOException("No space left on device")).when(writer).close();
        ReflectionTestUtils.setField(step, "rejectFile", writer);
        step.write(new Chunk<>(List.of(rejected)));
        step.close();
        verify(writer).close();
        step.afterStep(mock(StepExecution.class));
        assertAbend("34");
    }

    private void assertAbend(String status) {
        assertTrue(run.abended());
        assertEquals(12, run.returnCode());
        assertEquals(List.of("ERROR WRITING TO REJECTS FILE", "FILE STATUS IS: NNNN00" + status,
                "ABENDING PROGRAM", StepFiles.ABEND_MESSAGE), run.sysout());
    }
}
