package com.carddemo.customer.service;

import com.carddemo.common.dto.PageResponse;
import com.carddemo.common.exception.ResourceNotFoundException;
import com.carddemo.customer.dto.CustomerDto;
import com.carddemo.customer.entity.Customer;
import com.carddemo.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerDto getCustomerById(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", customerId));
        return mapToCustomerDto(customer);
    }

    public PageResponse<CustomerDto> getAllCustomers(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        return buildPageResponse(customerPage);
    }

    public PageResponse<CustomerDto> searchByLastName(String lastName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        Page<Customer> customerPage = customerRepository.findByLastNameContainingIgnoreCase(lastName, pageable);
        return buildPageResponse(customerPage);
    }

    private PageResponse<CustomerDto> buildPageResponse(Page<Customer> customerPage) {
        List<CustomerDto> customers = customerPage.getContent().stream()
                .map(this::mapToCustomerDto)
                .collect(Collectors.toList());

        return PageResponse.<CustomerDto>builder()
                .content(customers)
                .pageNumber(customerPage.getNumber())
                .pageSize(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .first(customerPage.isFirst())
                .last(customerPage.isLast())
                .build();
    }

    private CustomerDto mapToCustomerDto(Customer customer) {
        String fullName = String.format("%s %s %s",
                customer.getFirstName() != null ? customer.getFirstName() : "",
                customer.getMiddleName() != null ? customer.getMiddleName() : "",
                customer.getLastName() != null ? customer.getLastName() : "").trim();

        return CustomerDto.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .middleName(customer.getMiddleName())
                .lastName(customer.getLastName())
                .fullName(fullName)
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .addressLine3(customer.getAddressLine3())
                .stateCode(customer.getStateCode())
                .countryCode(customer.getCountryCode())
                .zipCode(customer.getZipCode())
                .phoneNumber1(customer.getPhoneNumber1())
                .phoneNumber2(customer.getPhoneNumber2())
                .dateOfBirth(customer.getDateOfBirth())
                .primaryCardholderIndicator(customer.getPrimaryCardholderIndicator())
                .ficoScore(customer.getFicoScore())
                .build();
    }
}
