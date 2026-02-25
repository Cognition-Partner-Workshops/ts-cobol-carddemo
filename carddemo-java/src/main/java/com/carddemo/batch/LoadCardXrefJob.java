package com.carddemo.batch;

import com.carddemo.entity.CardAccountXref;
import com.carddemo.repository.CardAccountXrefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job to load card/account cross-reference data - migrated from JCL XREFFILE job.
 * Original JCL: DEFINE CLUSTER for XREFFILE VSAM KSDS, REPRO from flat file.
 */
@Configuration
public class LoadCardXrefJob {

    private static final Logger log = LoggerFactory.getLogger(LoadCardXrefJob.class);

    private final CardAccountXrefRepository xrefRepository;

    public LoadCardXrefJob(CardAccountXrefRepository xrefRepository) {
        this.xrefRepository = xrefRepository;
    }

    @Bean
    public Job loadXrefJob(JobRepository jobRepository, Step loadXrefStep) {
        return new JobBuilder("loadXrefJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(loadXrefStep)
                .build();
    }

    @Bean
    public Step loadXrefStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
        return new StepBuilder("loadXrefStep", jobRepository)
                .tasklet(loadXrefTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet loadXrefTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting card/account xref data load...");

            if (xrefRepository.count() == 0) {
                CardAccountXref xref1 = new CardAccountXref();
                xref1.setCardNum("4111111111111111");
                xref1.setCustId(100001L);
                xref1.setAcctId(1000000001L);
                xrefRepository.save(xref1);

                CardAccountXref xref2 = new CardAccountXref();
                xref2.setCardNum("4222222222222222");
                xref2.setCustId(100002L);
                xref2.setAcctId(1000000002L);
                xrefRepository.save(xref2);

                log.info("Loaded {} xref records", xrefRepository.count());
            } else {
                log.info("Xref data already loaded, skipping. Count: {}", xrefRepository.count());
            }

            return RepeatStatus.FINISHED;
        };
    }
}
