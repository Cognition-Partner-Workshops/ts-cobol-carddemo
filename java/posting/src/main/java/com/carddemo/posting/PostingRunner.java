package com.carddemo.posting;

import com.carddemo.posting.batch.FailureOutcome;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * {@code java -jar posting.jar <case-dir> <out-dir>}: runs the posting chain on a fixture case
 * and writes outputs in the recorder's layout. Exit code 0 when the job completes (the legacy
 * RC 4 for rejects is reported in rc.json, as the legacy harness does), 12 otherwise.
 */
@Component
public class PostingRunner implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(PostingRunner.class);

    private final JobLauncher jobLauncher;
    private final Job postingJob;
    private final ChainRun run;
    private int exitCode;

    public PostingRunner(JobLauncher jobLauncher, Job postingJob, ChainRun run) {
        this.jobLauncher = jobLauncher;
        this.postingJob = postingJob;
        this.run = run;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> paths = args.getNonOptionArgs();
        if (paths.size() != 2) {
            LOG.error("usage: posting.jar <fixtures/posting/<case>> <out-dir>");
            exitCode = 2;
            return;
        }
        Path caseDir = Path.of(paths.get(0));
        Path outDir = Path.of(paths.get(1));
        recreate(outDir);
        run.start(caseDir, outDir);
        JobExecution execution = jobLauncher.run(postingJob, new JobParametersBuilder()
                .addString("case", caseDir.toString())
                .addString("out", outDir.toString())
                .addLong("launchedAt", System.currentTimeMillis())
                .toJobParameters());
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            LOG.info("posting {} -> {}: {}", caseDir, outDir, run.returnCodes());
            exitCode = 0;
        } else {
            FailureOutcome.write(run);
            execution.getAllFailureExceptions().forEach(e -> LOG.error("posting failed", e));
            exitCode = 12;
        }
    }

    private static void recreate(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(dir);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
