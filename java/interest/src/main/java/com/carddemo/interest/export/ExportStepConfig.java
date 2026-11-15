package com.carddemo.interest.export;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.carddemo.interest.jdbc.export.JdbcExportSource;

/**
 * The beans of exportDatasetsStep. The step is offered as a bean and is not part of
 * interestCalcJob yet: the job is assembled in package batch, by the tickets that own it.
 */
@Configuration
public class ExportStepConfig {

    @Bean
    ExportSource exportSource(JdbcTemplate jdbc) {
        return new JdbcExportSource(jdbc);
    }

    @Bean
    DatasetExport datasetExport(ExportSource exportSource) {
        return new DatasetExport(exportSource);
    }

    /** No skip, no retry: one stop exception fails the step (standards S6.2). */
    @Bean
    Step exportDatasetsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            DatasetExport datasetExport) {
        return new StepBuilder(ExportDatasetsTasklet.STEP_NAME, jobRepository)
                .tasklet(new ExportDatasetsTasklet(datasetExport), transactionManager)
                .allowStartIfComplete(true)
                .build();
    }
}
