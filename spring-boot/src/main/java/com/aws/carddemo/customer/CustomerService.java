package com.aws.carddemo.customer;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.customer.dto.CustomerResponse;
import com.aws.carddemo.customer.dto.CustomerUpdateRequest;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;

@Service
@Transactional
public class CustomerService {

    private static final Set<String> VALID_STATE_CODES = Set.of(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
            "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
            "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
            "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
            "DC", "PR", "VI", "GU", "AS", "MP"
    );

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(String lastName, Pageable pageable) {
        Page<Customer> customers;
        if (lastName != null && !lastName.isBlank()) {
            customers = customerRepository.findByLastNameContainingIgnoreCase(lastName, pageable);
        } else {
            customers = customerRepository.findAll(pageable);
        }
        return customers.map(CustomerResponse::from);
    }

    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (request.firstName() != null) {
            customer.setFirstName(request.firstName());
        }
        if (request.middleName() != null) {
            customer.setMiddleName(request.middleName());
        }
        if (request.lastName() != null) {
            customer.setLastName(request.lastName());
        }
        if (request.addressLine1() != null) {
            customer.setAddressLine1(request.addressLine1());
        }
        if (request.addressLine2() != null) {
            customer.setAddressLine2(request.addressLine2());
        }
        if (request.city() != null) {
            customer.setCity(request.city());
        }
        if (request.state() != null) {
            validateStateCode(request.state());
            customer.setState(request.state());
        }
        if (request.zipCode() != null) {
            validateZipCode(request.zipCode());
            customer.setZipCode(request.zipCode());
        }
        if (request.countryCode() != null) {
            customer.setCountryCode(request.countryCode());
        }
        if (request.phoneNumber() != null) {
            customer.setPhoneNumber(request.phoneNumber());
        }

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.from(saved);
    }

    private void validateStateCode(String stateCode) {
        if (!VALID_STATE_CODES.contains(stateCode)) {
            throw new ValidationException("Invalid state code: " + stateCode);
        }
    }

    private void validateZipCode(String zipCode) {
        if (!zipCode.matches("\\d{5}(-\\d{4})?")) {
            throw new ValidationException("Invalid ZIP code format: " + zipCode);
        }
    }
}
