package com.carddemo.batch;

import com.carddemo.entity.Card;
import com.carddemo.repository.CardRepository;
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
 * Spring Batch job to load card data - migrated from JCL CARDFILE job.
 * Original JCL: DEFINE CLUSTER for CARDFILE VSAM KSDS, REPRO from flat file.
 */
@Configuration
public class LoadCardDataJob {

    private static final Logger log = LoggerFactory.getLogger(LoadCardDataJob.class);

    private final CardRepository cardRepository;

    public LoadCardDataJob(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Bean
    public Job loadCardJob(JobRepository jobRepository, Step loadCardStep) {
        return new JobBuilder("loadCardJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(loadCardStep)
                .build();
    }

    @Bean
    public Step loadCardStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
        return new StepBuilder("loadCardStep", jobRepository)
                .tasklet(loadCardTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet loadCardTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting card data load...");

            if (cardRepository.count() == 0) {
                Card card1 = new Card();
                card1.setCardNum("4111111111111111");
                card1.setAcctId(1000000001L);
                card1.setCvvCode(123);
                card1.setEmbossedName("JOHN DOE");
                card1.setExpirationDate("2026-12-31");
                card1.setActiveStatus("Y");
                cardRepository.save(card1);

                Card card2 = new Card();
                card2.setCardNum("4222222222222222");
                card2.setAcctId(1000000002L);
                card2.setCvvCode(456);
                card2.setEmbossedName("JANE SMITH");
                card2.setExpirationDate("2025-06-30");
                card2.setActiveStatus("Y");
                cardRepository.save(card2);

                log.info("Loaded {} card records", cardRepository.count());
            } else {
                log.info("Card data already loaded, skipping. Count: {}", cardRepository.count());
            }

            return RepeatStatus.FINISHED;
        };
    }
}
