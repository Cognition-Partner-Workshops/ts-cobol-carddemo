package com.carddemo.interest.batch;

import org.springframework.batch.core.BatchStatus;
import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Maps the outcome of the launch to the process exit code: 0 for a COMPLETED job, the one stop
 * code for everything else, a refused launch included (standards S6.2).
 */
@Component
public class InterestExitCodes implements ExitCodeGenerator, ExitCodeExceptionMapper {

    private volatile int exitCode = InterestExitCode.COMPLETED;

    @EventListener
    public void onJobExecution(JobExecutionEvent event) {
        if (event.getJobExecution().getStatus() != BatchStatus.COMPLETED) {
            exitCode = InterestExitCode.STOPPED;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public int getExitCode(Throwable exception) {
        return InterestExitCode.STOPPED;
    }
}
