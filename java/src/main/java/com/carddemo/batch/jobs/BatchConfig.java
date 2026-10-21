package com.carddemo.batch.jobs;

import com.carddemo.batch.RunProperties;
import com.carddemo.batch.store.BatchRepository;
import com.carddemo.batch.store.Datasets;
import com.carddemo.batch.store.JsonlStore;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** One Spring Batch job: load the decoded inputs, run the batch path jobs in order, export the outputs. */
@Configuration
public class BatchConfig {

    @Bean
    public Job carddemo(JobRepository jobs, PlatformTransactionManager tx, JsonlStore store,
                        BatchRepository repo, RunProperties run) {
        Step load = new StepBuilder("load", jobs).tasklet((c, ctx) -> {
            for (Datasets d : Datasets.INPUTS) {
                store.importFile(d, run.inputDir().resolve(d.fileName()));
            }
            return RepeatStatus.FINISHED;
        }, tx).build();
        Step export = new StepBuilder("export", jobs).tasklet((c, ctx) -> {
            for (Datasets d : Datasets.OUTPUTS) {
                store.exportFile(d, run.outputDir().resolve(d.fileName()));
            }
            return RepeatStatus.FINISHED;
        }, tx).build();
        SimpleJobBuilder job = new JobBuilder("carddemo", jobs).incrementer(new RunIdIncrementer()).start(load);
        for (String name : run.jobs()) {
            job = job.next(switch (name.trim()) {
                case "POSTTRAN" -> new StepBuilder("POSTTRAN", jobs).tasklet(new PostTranTasklet(repo, run), tx).build();
                case "INTCALC" -> new StepBuilder("INTCALC", jobs).tasklet(new IntCalcTasklet(repo, run), tx).build();
                default -> throw new IllegalArgumentException("unknown batch path job " + name);
            });
        }
        return job.next(export).build();
    }
}
