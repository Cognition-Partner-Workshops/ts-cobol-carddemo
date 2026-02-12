package com.carddemo.service;

import com.carddemo.repository.CustomerRepository;
import org.springframework.stereotype.Service;

/**
 * Business logic for customer management.
 * Will contain migrated logic from:
 * - COCUSTIC.cbl (Customer Inquiry)
 * - COCUSTPC.cbl (Customer Update)
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
}
