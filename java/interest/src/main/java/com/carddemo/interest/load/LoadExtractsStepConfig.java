package com.carddemo.interest.load;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.jdbc.load.InterestRunLoadJournal;
import com.carddemo.interest.jdbc.load.ReplicaTableWriter;

/**
 * loadExtractsStep (architecture.md section 2): a tasklet, because one table is one transaction
 * and a restart repeats the whole load (section 6). The step itself holds no database transaction;
 * the loader opens one per table. The job parameters are those of interestCalcJob.
 */
@Configuration
public class LoadExtractsStepConfig {

    public static final String STEP_NAME = "loadExtractsStep";
    /** Key of the job execution context: the run_id (Long) of the carddemo.interest_run row of this load. */
    public static final String RUN_ID_KEY = "interestRunId";
    /** The reject list of a failed load, under the output directory: file, record number, field, reason. */
    public static final String REJECT_LIST = "load/rejects.tsv";

    private static final Logger LOG = LoggerFactory.getLogger(LoadExtractsStepConfig.class);

    @Bean
    ExtractLoader extractLoader(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, Clock runClock) {
        return new ExtractLoader(new ReplicaTableWriter(jdbc), new InterestRunLoadJournal(jdbc),
                new TransactionTemplate(transactionManager), runClock);
    }

    @Bean
    Step loadExtractsStep(JobRepository jobRepository, ExtractLoader loader) {
        Tasklet load = (contribution, chunk) -> {
            var parameters = chunk.getStepContext().getStepExecution().getJobParameters();
            LoadOutcome outcome = loader.load(Path.of(parameters.getString("inputDir")),
                    format(parameters.getString("format")), parameters.getString("parmDate"));
            chunk.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                    .putLong(RUN_ID_KEY, outcome.runId());
            if (!outcome.isLoaded()) {
                Path rejectList = Path.of(parameters.getString("outputDir")).resolve(REJECT_LIST);
                write(rejectList, outcome.rejects());
                outcome.rejects().forEach(reject -> LOG.error("loadExtractsStep reject: file {} record {} field {}: {}",
                        reject.file(), reject.recordNumber(), reject.field(), reject.reason()));
                // No skip, no retry: one reject fails the step (strategy TL3, standards S6.2).
                throw new InterestRunStoppedException(StopReason.RECORD_REJECTED, outcome.rejects().size()
                        + " records rejected by the load, no table was changed; reject list " + REJECT_LIST);
            }
            contribution.incrementWriteCount(outcome.tables().stream().mapToLong(table -> table.figures().count()).sum());
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(load, new ResourcelessTransactionManager())
                .allowStartIfComplete(true)
                .build();
    }

    /** REL-08: host is the proposed cp037 with C and D signs until the data owner states the code page. */
    static RecordFormat format(String name) {
        return switch (name) {
            case "fixture" -> RecordFormat.fixture();
            case "host" -> RecordFormat.host();
            default -> throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "unknown input format " + name);
        };
    }

    private static void write(Path rejectList, List<RejectedRecord> rejects) {
        try {
            Files.createDirectories(rejectList.getParent());
            Files.write(rejectList, rejects.stream().map(RejectedRecord::line).toList(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
