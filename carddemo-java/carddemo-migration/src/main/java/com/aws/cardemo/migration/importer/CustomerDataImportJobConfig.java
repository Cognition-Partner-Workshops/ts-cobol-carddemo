package com.aws.cardemo.migration.importer;

import com.aws.cardemo.domain.entity.Customer;
import com.aws.cardemo.persistence.repository.CustomerRepository;
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
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CustomerDataImportJobConfig {

    private final CustomerRepository customerRepository;

    @Bean
    public Job customerImportJob(JobRepository jobRepository, Step importCustomersStep) {
        return new JobBuilder("customerImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(importCustomersStep)
                .build();
    }

    @Bean
    public Step importCustomersStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Customer> customerFileReader,
            ItemProcessor<Customer, Customer> customerImportProcessor,
            ItemWriter<Customer> customerImportWriter) {
        return new StepBuilder("importCustomersStep", jobRepository)
                .<Customer, Customer>chunk(100, transactionManager)
                .reader(customerFileReader)
                .processor(customerImportProcessor)
                .writer(customerImportWriter)
                .build();
    }

    @Bean
    public FlatFileItemReader<Customer> customerFileReader() {
        return new FlatFileItemReaderBuilder<Customer>()
                .name("customerFileReader")
                .resource(new ClassPathResource("data/customers.csv"))
                .delimited()
                .names("customerId", "firstName", "middleName", "lastName", 
                       "addressLine1", "addressLine2", "stateCode", "postalCode", 
                       "phoneNumber1", "ssn", "dateOfBirth", "ficoCreditScore")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Customer.class);
                }})
                .linesToSkip(1)
                .build();
    }

    @Bean
    public ItemProcessor<Customer, Customer> customerImportProcessor() {
        return customer -> {
            log.info("Processing customer for import: {}", customer.getCustomerId());
            return customer;
        };
    }

    @Bean
    public ItemWriter<Customer> customerImportWriter() {
        return customers -> {
            for (Customer customer : customers) {
                log.info("Importing customer: {}", customer.getCustomerId());
                customerRepository.save(customer);
            }
        };
    }
}
