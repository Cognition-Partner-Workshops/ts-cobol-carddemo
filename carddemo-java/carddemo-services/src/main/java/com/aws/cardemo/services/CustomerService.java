package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.Customer;
import com.aws.cardemo.persistence.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(String customerId) {
        return customerRepository.findById(customerId);
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public void deleteCustomer(String customerId) {
        customerRepository.deleteById(customerId);
    }

    public List<Customer> getCustomersByLastName(String lastName) {
        return customerRepository.findByLastName(lastName);
    }

    public List<Customer> getCustomersByState(String stateCode) {
        return customerRepository.findByStateCode(stateCode);
    }

    public Optional<Customer> getCustomerBySsn(String ssn) {
        return customerRepository.findBySsn(ssn);
    }

    public List<Customer> searchCustomersByName(String name) {
        return customerRepository.searchByName(name);
    }

    public List<Customer> getCustomersByMinimumCreditScore(Integer minScore) {
        return customerRepository.findByMinimumCreditScore(minScore);
    }
}
