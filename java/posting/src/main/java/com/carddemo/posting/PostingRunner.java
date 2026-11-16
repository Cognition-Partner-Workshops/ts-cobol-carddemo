package com.carddemo.posting;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * {@code java -jar posting.jar CBTRN02C|CBTRN03C}: the program behind {@code EXEC PGM=} in
 * POSTTRAN STEP15 / TRANREPT STEP10R, with the step contract described in {@link StepFiles}.
 */
@Component
public class PostingRunner implements ApplicationRunner, ExitCodeGenerator {

    private final JobLauncher jobLauncher;
    private final Job cbtrn02c;
    private final Job cbtrn03c;
    private final StepFiles files;
    private int exitCode;

    public PostingRunner(JobLauncher jobLauncher, @Qualifier("cbtrn02cJob") Job cbtrn02c,
                         @Qualifier("cbtrn03cJob") Job cbtrn03c, StepFiles files) {
        this.jobLauncher = jobLauncher;
        this.cbtrn02c = cbtrn02c;
        this.cbtrn03c = cbtrn03c;
        this.files = files;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String program = args.getNonOptionArgs().isEmpty() ? "" : args.getNonOptionArgs().get(0).toUpperCase();
        Job job = switch (program) {
            case "CBTRN02C" -> cbtrn02c;
            case "CBTRN03C" -> cbtrn03c;
            default -> throw new IllegalArgumentException("usage: posting.jar CBTRN02C|CBTRN03C, got '" + program + "'");
        };
        try {
            JobExecution execution = jobLauncher.run(job, new JobParametersBuilder()
                    .addString("program", program)
                    .addLong("launchedAt", System.currentTimeMillis())
                    .toJobParameters());
            if (!files.abended() && execution.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException(program + " ended " + execution.getStatus() + ": "
                        + execution.getAllFailureExceptions());
            }
            exitCode = files.returnCode();
        } finally {
            files.sysout().forEach(System.out::println);
            System.out.flush();
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
