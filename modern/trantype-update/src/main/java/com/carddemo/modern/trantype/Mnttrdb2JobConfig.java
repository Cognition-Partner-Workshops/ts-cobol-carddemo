package com.carddemo.modern.trantype;

import java.nio.file.Path;
import java.util.List;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Job MNTTRDB2, step STEP1 (PGM=IKJEFT01 -> COBTUPDT): INPFILE in record order, chunked. */
@Configuration
public class Mnttrdb2JobConfig {

    static final String INPFILE = "INPFILE";

    @Bean
    public Job mnttrdb2Job(JobRepository jobRepository, Step step1) {
        return new JobBuilder("MNTTRDB2", jobRepository).start(step1).build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager tx, InpfileReader inpfileReader,
                      CobtupdtProcessor processor, Sysout sysout, Db2Attach db2,
                      @Value("${trantype.program}") String program,
                      @Value("${trantype.chunk-size}") int chunkSize) {
        return new StepBuilder("STEP1", jobRepository)
                .<InputRecord, List<String>>chunk(chunkSize, tx)
                .reader(inpfileReader)
                .processor(processor)
                .writer(new SysoutWriter(sysout))
                .listener(processor)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        db2.attach(program);
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        db2.commitAtEndOfTask();
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    public InpfileReader inpfileReader(@Value("#{jobParameters['case']}") String caseDir, Estate estate, Sysout sysout) {
        return new InpfileReader(Path.of(caseDir).resolve("input").resolve(INPFILE + ".txt"), sysout);
    }

    @Bean
    @StepScope
    public CobtupdtProcessor cobtupdtProcessor(Db2Attach db2) {
        return new CobtupdtProcessor(db2);
    }
}
