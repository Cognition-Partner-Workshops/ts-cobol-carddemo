package com.cardemo.batch;

import com.cardemo.entity.AuthSummary;
import com.cardemo.repository.AuthDetailRepository;
import com.cardemo.repository.AuthSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Purge Expired Authorizations batch job configuration (Optional module).
 * Migrated from JCL job CBPAUP0J / COBOL program CBPAUP0C.
 *
 * COBOL flow:
 * 1. Open IMS DB (DBPAUTP0)
 * 2. For each authorization summary (root segment):
 *    a. Check if last authorization timestamp is older than retention period (90 days)
 *    b. If expired: DLET (Delete) the root segment and all child detail segments
 *    c. Track counts of purged records
 * 3. Close IMS DB and write summary report
 *
 * Retention period: 90 days (configurable)
 */
@Configuration
public class PurgeExpiredAuthorizationsJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PurgeExpiredAuthorizationsJobConfig.class);
    private static final int RETENTION_DAYS = 90;
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final AuthSummaryRepository authSummaryRepository;
    private final AuthDetailRepository authDetailRepository;

    public PurgeExpiredAuthorizationsJobConfig(AuthSummaryRepository authSummaryRepository,
                                               AuthDetailRepository authDetailRepository) {
        this.authSummaryRepository = authSummaryRepository;
        this.authDetailRepository = authDetailRepository;
    }

    @Bean
    public Job purgeExpiredAuthorizationsJob(JobRepository jobRepository,
                                             Step purgeExpiredAuthorizationsStep) {
        return new JobBuilder("purgeExpiredAuthorizationsJob", jobRepository)
                .start(purgeExpiredAuthorizationsStep)
                .build();
    }

    @Bean
    public Step purgeExpiredAuthorizationsStep(JobRepository jobRepository,
                                               PlatformTransactionManager transactionManager) {
        return new StepBuilder("purgeExpiredAuthorizationsStep", jobRepository)
                .tasklet(purgeExpiredAuthorizationsTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet to purge expired authorization records.
     * Migrated from CBPAUP0C main processing loop.
     * COBOL: GU/GN loop over root segments, check date, DLET if expired.
     */
    @Bean
    public Tasklet purgeExpiredAuthorizationsTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate cutoffDate = LocalDate.now().minusDays(RETENTION_DAYS);
            String cutoffTs = cutoffDate.atStartOfDay().format(TS_FORMAT);

            log.info("CBPAUP0J: Purging authorizations older than {} (cutoff: {})",
                    RETENTION_DAYS + " days", cutoffTs);

            List<AuthSummary> allSummaries = authSummaryRepository.findAll();
            int purgedSummaries = 0;
            int purgedDetails = 0;
            int retained = 0;

            for (AuthSummary summary : allSummaries) {
                String lastAuthTs = summary.getLastAuthTs();

                // Check if expired - COBOL: IF PAUTSUM0-LAST-AUTH-TS < WS-CUTOFF-TS
                if (lastAuthTs != null && lastAuthTs.compareTo(cutoffTs) < 0) {
                    // Delete child detail records first (referential integrity)
                    // COBOL: DLET root segment cascades to children in IMS
                    var details = authDetailRepository.findByAuthSummaryId(summary.getId());
                    purgedDetails += details.size();
                    authDetailRepository.deleteAll(details);

                    // Delete summary record
                    authSummaryRepository.delete(summary);
                    purgedSummaries++;

                    log.debug("CBPAUP0J: Purged auth summary id={} card={} lastAuth={}",
                            summary.getId(), summary.getCardNum(), lastAuthTs);
                } else {
                    retained++;
                }
            }

            log.info("CBPAUP0J: Purge complete. Purged summaries={}, details={}, retained={}",
                    purgedSummaries, purgedDetails, retained);

            return RepeatStatus.FINISHED;
        };
    }
}
