package com.aws.carddemo.batch.job;

import com.aws.carddemo.domain.entity.Transaction;
import com.aws.carddemo.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * Transaction Backup Job - migrated from TRANBKP
 * Part of the daily batch cycle
 * 
 * This job backs up daily transactions to a file for archival purposes.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransactionBackupJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionRepository transactionRepository;

    @Value("${carddemo.batch.backup.directory:/tmp/carddemo/backup}")
    private String backupDirectory;

    @Bean
    public Job backupTransactionsJob(Step backupTransactionsStep) {
        return new JobBuilder("backupTransactionsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(backupTransactionsStep)
                .build();
    }

    @Bean
    public Step backupTransactionsStep(ItemReader<Transaction> transactionBackupReader,
                                        ItemWriter<Transaction> transactionBackupWriter) {
        return new StepBuilder("backupTransactionsStep", jobRepository)
                .<Transaction, Transaction>chunk(500, transactionManager)
                .reader(transactionBackupReader)
                .writer(transactionBackupWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Transaction> transactionBackupReader() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        return new RepositoryItemReaderBuilder<Transaction>()
                .name("transactionBackupReader")
                .repository(transactionRepository)
                .methodName("findByDateRange")
                .arguments(startOfDay, endOfDay)
                .sorts(Collections.singletonMap("transactionId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public FlatFileItemWriter<Transaction> transactionBackupWriter() {
        String fileName = String.format("%s/transactions_%s.csv", 
                backupDirectory, 
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));

        BeanWrapperFieldExtractor<Transaction> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{
                "transactionId", "transactionTypeCode", "transactionCategoryCode",
                "transactionSource", "description", "amount", "merchantId",
                "merchantName", "merchantCity", "merchantZip", "cardNumber",
                "originTimestamp", "processTimestamp"
        });

        DelimitedLineAggregator<Transaction> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(fieldExtractor);

        return new FlatFileItemWriterBuilder<Transaction>()
                .name("transactionBackupWriter")
                .resource(new FileSystemResource(fileName))
                .headerCallback(writer -> writer.write(
                        "transactionId,transactionTypeCode,transactionCategoryCode," +
                        "transactionSource,description,amount,merchantId," +
                        "merchantName,merchantCity,merchantZip,cardNumber," +
                        "originTimestamp,processTimestamp"))
                .lineAggregator(lineAggregator)
                .build();
    }
}
