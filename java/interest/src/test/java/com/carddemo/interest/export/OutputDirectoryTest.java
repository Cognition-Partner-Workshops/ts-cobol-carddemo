package com.carddemo.interest.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutputDirectoryTest {

    @TempDir
    Path temp;

    private static List<String> filesUnder(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile).map(path -> dir.relativize(path).toString()).sorted().toList();
        }
    }

    @Test
    void export_existingDirectoryIsReplacedAsAWhole() throws IOException {
        Path output = temp.resolve("actual");
        Files.createDirectories(output.resolve("datasets"));
        Files.writeString(output.resolve("datasets/OLD.GENERATION.txt"), "old");
        Files.writeString(output.resolve("rc.json"), "old");
        OutputDirectory directory = new OutputDirectory(output, 7);

        Path staging = directory.openStaging();
        Files.writeString(staging.resolve("rc.json"), "new");
        directory.replaceWithStaging();

        assertEquals(List.of("rc.json"), filesUnder(output));
        assertEquals("new", Files.readString(output.resolve("rc.json")));
        assertEquals(List.of("actual/rc.json"), filesUnder(temp));
    }

    @Test
    void export_discardedStagingLeavesTheDirectoryAsItWas() throws IOException {
        Path output = temp.resolve("actual");
        Files.createDirectories(output);
        Files.writeString(output.resolve("rc.json"), "old");
        OutputDirectory directory = new OutputDirectory(output, 7);

        Path staging = directory.openStaging();
        Files.writeString(staging.resolve("rc.json"), "half written");
        directory.discardStaging();

        assertEquals(List.of("actual/rc.json"), filesUnder(temp));
        assertEquals("old", Files.readString(output.resolve("rc.json")));
        assertFalse(Files.exists(staging));
    }

    @Test
    void export_stagingBecomesTheDirectoryWhenThereWasNone() throws IOException {
        Path output = temp.resolve("actual");
        OutputDirectory directory = new OutputDirectory(output, 7);

        Path staging = directory.openStaging();
        Files.writeString(staging.resolve("rc.json"), "new");
        directory.replaceWithStaging();

        assertEquals(List.of("actual/rc.json"), filesUnder(temp));
    }

    @Test
    void export_stagingOfAnInterruptedExportIsEmptiedFirst() throws IOException {
        Path output = temp.resolve("actual");
        OutputDirectory directory = new OutputDirectory(output, 7);
        Files.writeString(directory.openStaging().resolve("left-over.txt"), "x");

        Path staging = directory.openStaging();

        assertEquals(List.of(), filesUnder(staging));
    }

    @Test
    void export_fileOfThePreviousStepIsFoundOnlyWhenItIsThere() throws IOException {
        Path output = temp.resolve("actual");
        Files.createDirectories(output.resolve("sysout"));
        Files.writeString(output.resolve("sysout/INTCALC.STEP15.txt"), "START\n");
        OutputDirectory directory = new OutputDirectory(output, 7);

        assertNotNull(directory.existing("sysout/INTCALC.STEP15.txt"));
        assertNull(directory.existing("sysout/OTHER.txt"));
    }
}
