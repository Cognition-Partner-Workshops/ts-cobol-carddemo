package com.carddemo.customer.service;

import com.carddemo.common.dto.CustomerDto;
import com.carddemo.common.dto.PagedResponse;
import com.carddemo.common.entity.Customer;
import com.carddemo.common.exception.BadRequestException;
import com.carddemo.common.exception.ResourceNotFoundException;
import com.carddemo.customer.dto.CreateCustomerRequest;
import com.carddemo.customer.dto.UpdateCustomerRequest;
import com.carddemo.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public PagedResponse<CustomerDto> getAllCustomers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Customer> customerPage = customerRepository.findAll(pageable);

        List<CustomerDto> customers = customerPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<CustomerDto>builder()
                .content(customers)
                .page(customerPage.getNumber())
                .size(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .first(customerPage.isFirst())
                .last(customerPage.isLast())
                .build();
    }

    public CustomerDto getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", customerId));
        return mapToDto(customer);
    }

    public PagedResponse<CustomerDto> searchCustomers(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customerPage = customerRepository.searchCustomers(searchTerm, pageable);

        List<CustomerDto> customers = customerPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<CustomerDto>builder()
                .content(customers)
                .page(customerPage.getNumber())
                .size(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .first(customerPage.isFirst())
                .last(customerPage.isLast())
                .build();
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsBySsn(request.getSsn())) {
            throw new BadRequestException("Customer with this SSN already exists");
        }

        Customer customer = Customer.builder()
                .customerId(generateCustomerId())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .addressLine3(request.getAddressLine3())
                .stateCode(request.getStateCode())
                .countryCode(request.getCountryCode())
                .zipCode(request.getZipCode())
                .phoneNumber1(request.getPhoneNumber1())
                .phoneNumber2(request.getPhoneNumber2())
                .ssn(request.getSsn())
                .govtIssuedId(request.getGovtIssuedId())
                .dateOfBirth(request.getDateOfBirth())
                .eftAccountId(request.getEftAccountId())
                .primaryCardholderInd(request.getPrimaryCardholderInd())
                .ficoCreditScore(request.getFicoCreditScore())
                .build();

        customer = customerRepository.save(customer);
        return mapToDto(customer);
    }

    @Transactional
    public CustomerDto updateCustomer(Long customerId, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", customerId));

        if (request.getFirstName() != null) customer.setFirstName(request.getFirstName());
        if (request.getMiddleName() != null) customer.setMiddleName(request.getMiddleName());
        if (request.getLastName() != null) customer.setLastName(request.getLastName());
        if (request.getAddressLine1() != null) customer.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) customer.setAddressLine2(request.getAddressLine2());
        if (request.getAddressLine3() != null) customer.setAddressLine3(request.getAddressLine3());
        if (request.getStateCode() != null) customer.setStateCode(request.getStateCode());
        if (request.getCountryCode() != null) customer.setCountryCode(request.getCountryCode());
        if (request.getZipCode() != null) customer.setZipCode(request.getZipCode());
        if (request.getPhoneNumber1() != null) customer.setPhoneNumber1(request.getPhoneNumber1());
        if (request.getPhoneNumber2() != null) customer.setPhoneNumber2(request.getPhoneNumber2());
        if (request.getGovtIssuedId() != null) customer.setGovtIssuedId(request.getGovtIssuedId());
        if (request.getDateOfBirth() != null) customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getEftAccountId() != null) customer.setEftAccountId(request.getEftAccountId());
        if (request.getPrimaryCardholderInd() != null) customer.setPrimaryCardholderInd(request.getPrimaryCardholderInd());
        if (request.getFicoCreditScore() != null) customer.setFicoCreditScore(request.getFicoCreditScore());

        customer = customerRepository.save(customer);
        return mapToDto(customer);
    }

    @Transactional
    public void deleteCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", "customerId", customerId);
        }
        customerRepository.deleteById(customerId);
    }

    public List<CustomerDto> getCustomersByState(String stateCode) {
        return customerRepository.findByStateCode(stateCode).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<CustomerDto> getCustomersByFicoRange(Integer minScore, Integer maxScore) {
        return customerRepository.findByFicoScoreRange(minScore, maxScore).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private Long generateCustomerId() {
        return ThreadLocalRandom.current().nextLong(100000000L, 999999999L);
    }

    private CustomerDto mapToDto(Customer customer) {
        String maskedSsn = customer.getSsn() != null && customer.getSsn().length() == 9
                ? "***-**-" + customer.getSsn().substring(5)
                : null;

        return CustomerDto.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .middleName(customer.getMiddleName())
                .lastName(customer.getLastName())
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .addressLine3(customer.getAddressLine3())
                .stateCode(customer.getStateCode())
                .countryCode(customer.getCountryCode())
                .zipCode(customer.getZipCode())
                .phoneNumber1(customer.getPhoneNumber1())
                .phoneNumber2(customer.getPhoneNumber2())
                .ssnMasked(maskedSsn)
                .govtIssuedId(customer.getGovtIssuedId())
                .dateOfBirth(customer.getDateOfBirth())
                .eftAccountId(customer.getEftAccountId())
                .primaryCardholderInd(customer.getPrimaryCardholderInd())
                .ficoCreditScore(customer.getFicoCreditScore())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
