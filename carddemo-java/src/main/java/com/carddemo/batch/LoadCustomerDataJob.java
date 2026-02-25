package com.carddemo.batch;

import com.carddemo.entity.Customer;
import com.carddemo.repository.CustomerRepository;
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
 * Spring Batch job to load customer data - migrated from JCL CUSTFILE job.
 * Original JCL: DEFINE CLUSTER for CUSTFILE VSAM KSDS, REPRO from flat file.
 */
@Configuration
public class LoadCustomerDataJob {

    private static final Logger log = LoggerFactory.getLogger(LoadCustomerDataJob.class);

    private final CustomerRepository customerRepository;

    public LoadCustomerDataJob(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Bean
    public Job loadCustomerJob(JobRepository jobRepository, Step loadCustomerStep) {
        return new JobBuilder("loadCustomerJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(loadCustomerStep)
                .build();
    }

    @Bean
    public Step loadCustomerStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("loadCustomerStep", jobRepository)
                .tasklet(loadCustomerTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet loadCustomerTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting customer data load...");

            if (customerRepository.count() == 0) {
                Customer cust1 = new Customer();
                cust1.setCustId(100001L);
                cust1.setFirstName("JOHN");
                cust1.setMiddleName("M");
                cust1.setLastName("DOE");
                cust1.setAddrLine1("123 MAIN ST");
                cust1.setAddrStateCd("NY");
                cust1.setAddrCountryCd("US");
                cust1.setAddrZip("10001");
                cust1.setPhoneNum1("2125551234");
                cust1.setSsn(123456789L);
                cust1.setDobYyyyMmDd("19800115");
                cust1.setFicoCreditScore(750);
                customerRepository.save(cust1);

                Customer cust2 = new Customer();
                cust2.setCustId(100002L);
                cust2.setFirstName("JANE");
                cust2.setMiddleName("A");
                cust2.setLastName("SMITH");
                cust2.setAddrLine1("456 OAK AVE");
                cust2.setAddrStateCd("CA");
                cust2.setAddrCountryCd("US");
                cust2.setAddrZip("90210");
                cust2.setPhoneNum1("3105559876");
                cust2.setSsn(987654321L);
                cust2.setDobYyyyMmDd("19850620");
                cust2.setFicoCreditScore(800);
                customerRepository.save(cust2);

                log.info("Loaded {} customer records", customerRepository.count());
            } else {
                log.info("Customer data already loaded, skipping. Count: {}", customerRepository.count());
            }

            return RepeatStatus.FINISHED;
        };
    }
}
