package com.carddemo.interest;

import java.nio.file.Path;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import com.carddemo.interest.batch.Abend;
import com.carddemo.interest.batch.Sysout;
import com.carddemo.interest.batch.VsamDatasets;

/**
 * Entry point with CBACT04C's step contract: datasets arrive as DD_&lt;ddname&gt; paths, the first
 * program argument is the PARM text, STDOUT is SYSOUT and the exit status is the step return code.
 */
@Component
public class InterestRunner implements ApplicationRunner, ExitCodeGenerator {

    private final JobLauncher jobLauncher;
    private final Job intcalcJob;
    private final VsamDatasets datasets;
    private final Sysout sysout;
    private final String defaultParm;
    private int returnCode;

    public InterestRunner(JobLauncher jobLauncher, Job intcalcJob, VsamDatasets datasets, Sysout sysout,
                          @Value("${carddemo.parm}") String defaultParm) {
        this.jobLauncher = jobLauncher;
        this.intcalcJob = intcalcJob;
        this.datasets = datasets;
        this.sysout = sysout;
        this.defaultParm = defaultParm;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String parm = args.getNonOptionArgs().isEmpty() ? defaultParm : args.getNonOptionArgs().get(0);
        try {
            sysout.display("START OF EXECUTION OF PROGRAM CBACT04C");
            datasets.load(dd("ACCTFILE"), dd("XREFFILE"), dd("DISCGRP"), dd("TCATBALF"));
            JobExecution execution = jobLauncher.run(intcalcJob,
                    new JobParametersBuilder().addString("parm", parm).toJobParameters());
            datasets.unloadTransactions(dd("TRANSACT"));
            datasets.unloadAccounts(dd("ACCTFILE"));
            if (sysout.abended()) {
                returnCode = Abend.RETURN_CODE;
            } else if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException("INTCALC ended " + execution.getStatus() + ": "
                        + execution.getAllFailureExceptions());
            } else {
                sysout.display("END OF EXECUTION OF PROGRAM CBACT04C");
            }
        } finally {
            sysout.lines().forEach(System.out::println);
            System.out.flush();
        }
    }

    @Override
    public int getExitCode() {
        return returnCode;
    }

    private static Path dd(String name) {
        String path = System.getenv("DD_" + name);
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("DD " + name + " is not allocated");
        }
        return Path.of(path);
    }
}
