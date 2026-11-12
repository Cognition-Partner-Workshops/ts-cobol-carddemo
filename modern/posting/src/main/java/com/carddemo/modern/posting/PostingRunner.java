package com.carddemo.modern.posting;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.carddemo.modern.codec.Layouts;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Loads setup jobs, executes POSTTRAN/TRANREPT, and records the same five datasets and two SYSOUTs as record.py. */
@Component
public class PostingRunner implements ApplicationRunner {
    private final InputLoader loader;
    private final JobLauncher launcher;
    private final Job job;
    private final Job reportJob;
    private final PostingRepository repo;
    private final PostingOutput output;

    public PostingRunner(InputLoader loader, JobLauncher launcher, Job postingJob, Job reportingJob, PostingRepository repo, PostingOutput output) {
        this.loader = loader; this.launcher = launcher; this.job = postingJob; this.repo = repo; this.output = output;
        this.reportJob = reportingJob;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("case") || !args.containsOption("out"))
            throw new IllegalArgumentException("usage: --case=<fixtures/posting/case> --out=<dir> --estate.root=<repo>");
        Path dir = Path.of(args.getOptionValues("case").getFirst());
        loader.load(dir);
        var execution = launcher.run(job,new JobParametersBuilder().addString("case",dir.toString())
                .addLong("run",System.nanoTime()).toJobParameters());
        if (execution.getStatus() != BatchStatus.COMPLETED && output.postingRc != 12)
            throw new IllegalStateException("POSTING failed: " + execution.getAllFailureExceptions());
        var reportExecution = launcher.run(reportJob,new JobParametersBuilder().addString("case",dir.toString())
                .addLong("run",System.nanoTime()).toJobParameters());
        if (reportExecution.getStatus() != BatchStatus.COMPLETED && output.reportRc != 12)
            throw new IllegalStateException("TRANREPT failed: " + reportExecution.getAllFailureExceptions());
        var run = new RecordedRun(Path.of(args.getOptionValues("out").getFirst()));
        // record.py folds setup jobs into POSTTRAN's short step names; duplicate names overwrite.
        var steps = new TreeMap<String,Integer>();
        for (String name : new String[]{"CLCIFIL","OPCIFIL","STEP05","STEP10","STEP15","STEP20","STEP25","STEP30"}) steps.put(name,0);
        steps.put("STEP15",output.postingRc);
        run.returnCodes("POSTTRAN",output.postingRc,steps);
        run.returnCodes("TRANREPT",output.reportRc,Map.of("PRC001",0,"STEP05R",0,"STEP10R",output.reportRc));
        run.sysout("POSTTRAN","STEP15",output.postingSysout);
        run.sysout("TRANREPT","STEP10R",output.reportSysout);
        run.dataset(InputLoader.PREFIX + "TRANSACT.VSAM.KSDS", Layouts.TRAN,
                repo.records("transaction_master","tran_id").stream().map(s -> Layouts.TRAN.decode(s, true)).toList());
        run.dataset(InputLoader.PREFIX + "ACCTDATA.VSAM.KSDS", Layouts.ACCOUNT,
                repo.records("account","acct_id").stream().map(s -> Layouts.ACCOUNT.decode(s, true)).toList());
        run.dataset(InputLoader.PREFIX + "TCATBALF.VSAM.KSDS", Layouts.TRAN_CAT_BAL,
                repo.records("tran_cat_bal","acct_id,type_cd,cat_cd").stream().map(s -> Layouts.TRAN_CAT_BAL.decode(s, true)).toList());
        run.dataset(InputLoader.PREFIX + "DALYREJS.G0001V00", Layouts.REJTRAN,
                repo.records("reject","seq").stream().map(s -> Layouts.REJTRAN.decode(s, true)).toList());
        run.report(InputLoader.PREFIX + "TRANREPT.G0001V00","text",133,output.report);
    }
}
