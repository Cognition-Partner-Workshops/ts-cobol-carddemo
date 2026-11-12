package com.carddemo.modern.dynamic;

import java.util.Iterator;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DynamicJob {
    @Bean
    Job maintenance(JobRepository repository, Step dynamic, Step calledProgram) {
        return new JobBuilder("MNTTRDYN", repository).start(dynamic).next(calledProgram).build();
    }

    @Bean
    Step dynamic(JobRepository repository, PlatformTransactionManager tx, RunUnit unit) {
        return new StepBuilder("CBTRNDYN", repository).tasklet((contribution, context) -> {
            unit.dynamicSql();
            return org.springframework.batch.repeat.RepeatStatus.FINISHED;
        }, tx).build();
    }

    @Bean(name = "COBTUPDT")
    @StepScope
    Cobtupdt cobtupdt(RunUnit unit) {
        return new Cobtupdt(unit);
    }

    /** Dynamic CALL registry: program selected from the JCL PARMS at run time. */
    @Bean
    @StepScope
    ItemReader<Map<String, Object>> input(RunUnit unit, Map<String, Cobtupdt> programs) throws Exception {
        if (!programs.containsKey(unit.program)) {
            throw new IllegalArgumentException("Unported dynamic CALL: " + unit.program);
        }
        unit.sysout.add("OPEN FILE OK");
        Iterator<String> records = unit.read("INPFILE").iterator();
        return () -> records.hasNext() ? Layouts.TRANTYPE_INPUT.decode(records.next()) : null;
    }

    /** 1001-READ-NEXT-RECORDS and 2001-CLOSE-STOP: STOP RUN suppresses CBTRNDYN: END. */
    @Bean
    Step calledProgram(JobRepository repository, PlatformTransactionManager tx,
                       ItemReader<Map<String, Object>> input, Map<String, Cobtupdt> programs, RunUnit unit) {
        return new StepBuilder("dynamic-CALL", repository)
                .<Map<String, Object>, Map<String, Object>>chunk(1, tx)
                .reader(input).processor(record -> programs.get(unit.program).process(record))
                .writer(chunk -> {
                    for (var record : chunk) programs.get(unit.program).write(record);
                }).build();
    }
}
