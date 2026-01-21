package com.carddemo.repository;

import com.carddemo.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId(123456789L);
        testCustomer.setFirstName("John");
        testCustomer.setMiddleName("Michael");
        testCustomer.setLastName("Doe");
        testCustomer.setAddressLine1("123 Main St");
        testCustomer.setStateCode("NY");
        testCustomer.setCountryCode("USA");
        testCustomer.setZipCode("10001");
        testCustomer.setPhoneNumber1("555-123-4567");
        testCustomer.setSsn(123456789L);
        testCustomer.setDateOfBirth(LocalDate.of(1985, 6, 15));
        testCustomer.setPrimaryCardHolderIndicator("Y");
        testCustomer.setFicoCreditScore(750);
    }

    @Test
    void testSaveAndFindById() {
        Customer saved = customerRepository.save(testCustomer);
        
        Optional<Customer> found = customerRepository.findById(saved.getCustomerId());
        
        assertTrue(found.isPresent());
        assertEquals(testCustomer.getFirstName(), found.get().getFirstName());
        assertEquals(testCustomer.getLastName(), found.get().getLastName());
    }

    @Test
    void testFindBySsn() {
        customerRepository.save(testCustomer);
        
        Optional<Customer> found = customerRepository.findBySsn(123456789L);
        
        assertTrue(found.isPresent());
        assertEquals(testCustomer.getCustomerId(), found.get().getCustomerId());
    }

    @Test
    void testFindByLastName() {
        customerRepository.save(testCustomer);
        
        List<Customer> customers = customerRepository.findByLastName("Doe");
        
        assertEquals(1, customers.size());
        assertEquals(testCustomer.getFirstName(), customers.get(0).getFirstName());
    }

    @Test
    void testFindByLastNameWithPagination() {
        customerRepository.save(testCustomer);
        
        Page<Customer> page = customerRepository.findByLastName("Doe", PageRequest.of(0, 10));
        
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void testFindByStateCode() {
        customerRepository.save(testCustomer);
        
        List<Customer> customers = customerRepository.findByStateCode("NY");
        
        assertEquals(1, customers.size());
    }

    @Test
    void testFindByZipCode() {
        customerRepository.save(testCustomer);
        
        List<Customer> customers = customerRepository.findByZipCode("10001");
        
        assertEquals(1, customers.size());
    }

    @Test
    void testSearchByName() {
        customerRepository.save(testCustomer);
        
        List<Customer> customers = customerRepository.searchByName("John", "Doe");
        
        assertEquals(1, customers.size());
    }

    @Test
    void testFindByPrimaryCardHolderIndicator() {
        customerRepository.save(testCustomer);
        
        List<Customer> customers = customerRepository.findByPrimaryCardHolderIndicator("Y");
        
        assertEquals(1, customers.size());
    }

    @Test
    void testFindByFicoCreditScoreGreaterThanEqual() {
        customerRepository.save(testCustomer);
        
        List<Customer> customers = customerRepository.findByFicoCreditScoreGreaterThanEqual(700);
        
        assertEquals(1, customers.size());
    }

    @Test
    void testCountByStateCode() {
        customerRepository.save(testCustomer);
        
        long count = customerRepository.countByStateCode("NY");
        
        assertEquals(1, count);
    }
}
