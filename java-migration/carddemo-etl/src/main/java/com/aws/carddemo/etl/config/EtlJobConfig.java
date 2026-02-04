package com.aws.carddemo.etl.config;

import com.aws.carddemo.domain.entity.*;
import com.aws.carddemo.domain.repository.*;
import com.aws.carddemo.etl.reader.VsamExportRecordReader;
import com.aws.carddemo.etl.reader.VsamExportRecordReader.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * ETL Job Configuration - migrated from CBIMPORT.cbl
 * Imports VSAM export data into PostgreSQL database
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class EtlJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    @Value("${carddemo.etl.input.directory:/tmp/carddemo/import}")
    private String inputDirectory;

    @Bean
    public Job dataImportJob(Step importCustomersStep, Step importAccountsStep, 
                              Step importCardsStep, Step importTransactionsStep) {
        return new JobBuilder("dataImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(importCustomersStep)
                .next(importAccountsStep)
                .next(importCardsStep)
                .next(importTransactionsStep)
                .build();
    }

    @Bean
    public Step importCustomersStep() {
        return new StepBuilder("importCustomersStep", jobRepository)
                .<ExportRecord, Customer>chunk(100, transactionManager)
                .reader(customerExportReader())
                .processor(customerProcessor())
                .writer(customerWriter())
                .build();
    }

    @Bean
    public Step importAccountsStep() {
        return new StepBuilder("importAccountsStep", jobRepository)
                .<ExportRecord, Account>chunk(100, transactionManager)
                .reader(accountExportReader())
                .processor(accountProcessor())
                .writer(accountWriter())
                .build();
    }

    @Bean
    public Step importCardsStep() {
        return new StepBuilder("importCardsStep", jobRepository)
                .<ExportRecord, Card>chunk(100, transactionManager)
                .reader(cardExportReader())
                .processor(cardProcessor())
                .writer(cardWriter())
                .build();
    }

    @Bean
    public Step importTransactionsStep() {
        return new StepBuilder("importTransactionsStep", jobRepository)
                .<ExportRecord, Transaction>chunk(100, transactionManager)
                .reader(transactionExportReader())
                .processor(transactionProcessor())
                .writer(transactionWriter())
                .build();
    }

    @Bean
    public FlatFileItemReader<ExportRecord> customerExportReader() {
        return VsamExportRecordReader.createReader(
                new FileSystemResource(inputDirectory + "/customers.dat"));
    }

    @Bean
    public FlatFileItemReader<ExportRecord> accountExportReader() {
        return VsamExportRecordReader.createReader(
                new FileSystemResource(inputDirectory + "/accounts.dat"));
    }

    @Bean
    public FlatFileItemReader<ExportRecord> cardExportReader() {
        return VsamExportRecordReader.createReader(
                new FileSystemResource(inputDirectory + "/cards.dat"));
    }

    @Bean
    public FlatFileItemReader<ExportRecord> transactionExportReader() {
        return VsamExportRecordReader.createReader(
                new FileSystemResource(inputDirectory + "/transactions.dat"));
    }

    @Bean
    public ItemProcessor<ExportRecord, Customer> customerProcessor() {
        return record -> {
            if (!"CUSTOMER".equals(record.getRecordType())) {
                return null;
            }
            CustomerRecord parsed = VsamExportRecordReader.parseCustomerRecord(record.getRecordData());
            
            return Customer.builder()
                    .customerId(parsed.getCustomerId())
                    .firstName(parsed.getFirstName())
                    .middleName(parsed.getMiddleName())
                    .lastName(parsed.getLastName())
                    .addressLine1(parsed.getAddressLine1())
                    .addressLine2(parsed.getAddressLine2())
                    .addressLine3(parsed.getAddressLine3())
                    .stateCode(parsed.getStateCode())
                    .countryCode(parsed.getCountryCode())
                    .zipCode(parsed.getZipCode())
                    .phoneNumber1(parsed.getPhoneNumber1())
                    .phoneNumber2(parsed.getPhoneNumber2())
                    .ssn(parsed.getSsn())
                    .govtIssuedId(parsed.getGovtIssuedId())
                    .dateOfBirth(parseDate(parsed.getDateOfBirth()))
                    .eftAccountId(parsed.getEftAccountId())
                    .primaryCardHolder("Y".equalsIgnoreCase(parsed.getPrimaryCardHolder()) || "1".equals(parsed.getPrimaryCardHolder()))
                    .ficoCreditScore(parsed.getFicoCreditScore())
                    .build();
        };
    }

    @Bean
    public ItemProcessor<ExportRecord, Account> accountProcessor() {
        return record -> {
            if (!"ACCOUNT".equals(record.getRecordType())) {
                return null;
            }
            AccountRecord parsed = VsamExportRecordReader.parseAccountRecord(record.getRecordData());
            
            return Account.builder()
                    .accountId(parsed.getAccountId())
                    .activeStatus(parsed.getActiveStatus())
                    .currentBalance(parsed.getCurrentBalance())
                    .creditLimit(parsed.getCreditLimit())
                    .cashCreditLimit(parsed.getCashCreditLimit())
                    .openDate(parseDate(parsed.getOpenDate()))
                    .expirationDate(parseDate(parsed.getExpirationDate()))
                    .reissueDate(parseDate(parsed.getReissueDate()))
                    .currentCycleCredit(parsed.getCurrentCycleCredit())
                    .currentCycleDebit(parsed.getCurrentCycleDebit())
                    .zipCode(parsed.getZipCode())
                    .groupId(parsed.getGroupId())
                    .build();
        };
    }

    @Bean
    public ItemProcessor<ExportRecord, Card> cardProcessor() {
        return record -> {
            if (!"CARD".equals(record.getRecordType())) {
                return null;
            }
            CardRecord parsed = VsamExportRecordReader.parseCardRecord(record.getRecordData());
            
            Account account = accountRepository.findById(parsed.getAccountId()).orElse(null);
            
            return Card.builder()
                    .cardNumber(parsed.getCardNumber())
                    .account(account)
                    .cvvCode(parsed.getCvvCode())
                    .embossedName(parsed.getEmbossedName())
                    .expirationDate(parseDate(parsed.getExpirationDate()))
                    .activeStatus(parsed.getActiveStatus())
                    .build();
        };
    }

    @Bean
    public ItemProcessor<ExportRecord, Transaction> transactionProcessor() {
        return record -> {
            if (!"TRANSACT".equals(record.getRecordType())) {
                return null;
            }
            TransactionRecord parsed = VsamExportRecordReader.parseTransactionRecord(record.getRecordData());
            
            return Transaction.builder()
                    .transactionId(parsed.getTransactionId())
                    .transactionTypeCode(parsed.getTransactionTypeCode())
                    .transactionCategoryCode(parsed.getTransactionCategoryCode())
                    .transactionSource(parsed.getTransactionSource())
                    .description(parsed.getDescription())
                    .amount(parsed.getAmount())
                    .merchantId(parsed.getMerchantId())
                    .merchantName(parsed.getMerchantName())
                    .merchantCity(parsed.getMerchantCity())
                    .merchantZip(parsed.getMerchantZip())
                    .cardNumber(parsed.getCardNumber())
                    .originTimestamp(parseDateTime(parsed.getOriginTimestamp()))
                    .processTimestamp(parseDateTime(parsed.getProcessTimestamp()))
                    .build();
        };
    }

    @Bean
    public ItemWriter<Customer> customerWriter() {
        return customers -> {
            for (Customer customer : customers) {
                customerRepository.save(customer);
            }
            log.info("Imported {} customers", customers.size());
        };
    }

    @Bean
    public ItemWriter<Account> accountWriter() {
        return accounts -> {
            for (Account account : accounts) {
                accountRepository.save(account);
            }
            log.info("Imported {} accounts", accounts.size());
        };
    }

    @Bean
    public ItemWriter<Card> cardWriter() {
        return cards -> {
            for (Card card : cards) {
                cardRepository.save(card);
            }
            log.info("Imported {} cards", cards.size());
        };
    }

    @Bean
    public ItemWriter<Transaction> transactionWriter() {
        return transactions -> {
            for (Transaction transaction : transactions) {
                transactionRepository.save(transaction);
            }
            log.info("Imported {} transactions", transactions.size());
        };
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (DateTimeParseException e2) {
                log.warn("Unable to parse date: {}", dateStr);
                return null;
            }
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            } catch (DateTimeParseException e2) {
                log.warn("Unable to parse datetime: {}", dateTimeStr);
                return null;
            }
        }
    }
}
