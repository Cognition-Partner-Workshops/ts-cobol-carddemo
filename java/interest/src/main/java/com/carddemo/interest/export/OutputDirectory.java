package com.carddemo.interest.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * The output directory of one export: everything is written to a staging directory beside it and
 * the staging directory then takes its place as a whole (architecture.md section 5; standards S6.4).
 * A file of an earlier export never stays beside the new ones, and a failed export leaves the
 * directory as it was.
 */
final class OutputDirectory {

    private final Path outputDir;
    private final Path stagingDir;
    private final Path retiredDir;

    OutputDirectory(Path outputDir, long runId) {
        this.outputDir = outputDir.toAbsolutePath().normalize();
        String name = this.outputDir.getFileName().toString();
        this.stagingDir = this.outputDir.resolveSibling(name + ".export-staging-" + runId);
        this.retiredDir = this.outputDir.resolveSibling(name + ".export-retired-" + runId);
    }

    /** An empty staging directory; what an interrupted export of the same run left is removed. */
    Path openStaging() throws IOException {
        deleteTree(stagingDir);
        deleteTree(retiredDir);
        Files.createDirectories(stagingDir);
        return stagingDir;
    }

    /** A file the previous step left in the output directory, or null when it is not there. */
    Path existing(String relativeFile) {
        Path file = outputDir.resolve(relativeFile);
        return Files.isRegularFile(file) ? file : null;
    }

    /** The staging directory becomes the output directory; the old one goes as a whole. */
    void replaceWithStaging() throws IOException {
        boolean hadOutput = Files.exists(outputDir);
        if (hadOutput) {
            Files.move(outputDir, retiredDir, StandardCopyOption.ATOMIC_MOVE);
        }
        try {
            Files.move(stagingDir, outputDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            if (hadOutput) {
                Files.move(retiredDir, outputDir, StandardCopyOption.ATOMIC_MOVE);
            }
            throw e;
        }
        deleteTree(retiredDir);
    }

    /** After a failure: the staging directory goes, the output directory stays as it was. */
    void discardStaging() {
        try {
            deleteTree(stagingDir);
        } catch (IOException e) {
            // the failure that led here is the one reported
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
