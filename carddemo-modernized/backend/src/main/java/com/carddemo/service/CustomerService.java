package com.carddemo.service;

import com.carddemo.exception.BadRequestException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.model.Customer;
import com.carddemo.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    public Customer getCustomerById(String customerId) {
        return customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", customerId));
    }
    
    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsByCustomerId(customer.getCustomerId())) {
            throw new BadRequestException("Customer with ID " + customer.getCustomerId() + " already exists");
        }
        return customerRepository.save(customer);
    }
    
    public Customer updateCustomer(String customerId, Customer customerDetails) {
        Customer customer = getCustomerById(customerId);
        
        customer.setFirstName(customerDetails.getFirstName());
        customer.setMiddleName(customerDetails.getMiddleName());
        customer.setLastName(customerDetails.getLastName());
        customer.setAddressLine1(customerDetails.getAddressLine1());
        customer.setAddressLine2(customerDetails.getAddressLine2());
        customer.setAddressLine3(customerDetails.getAddressLine3());
        customer.setStateCode(customerDetails.getStateCode());
        customer.setCountryCode(customerDetails.getCountryCode());
        customer.setZipCode(customerDetails.getZipCode());
        customer.setPhoneNumber1(customerDetails.getPhoneNumber1());
        customer.setPhoneNumber2(customerDetails.getPhoneNumber2());
        customer.setSsn(customerDetails.getSsn());
        customer.setGovtIssuedId(customerDetails.getGovtIssuedId());
        customer.setDateOfBirth(customerDetails.getDateOfBirth());
        customer.setEftAccountId(customerDetails.getEftAccountId());
        customer.setPrimaryCardHolderInd(customerDetails.getPrimaryCardHolderInd());
        customer.setFicoCreditScore(customerDetails.getFicoCreditScore());
        
        return customerRepository.save(customer);
    }
    
    public void deleteCustomer(String customerId) {
        if (!customerRepository.existsByCustomerId(customerId)) {
            throw new ResourceNotFoundException("Customer", "customerId", customerId);
        }
        customerRepository.deleteByCustomerId(customerId);
    }
}
