package com.aws.carddemo.customer.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.aws.carddemo.customer.Customer;

public record CustomerResponse(
        Long id,
        String firstName,
        String middleName,
        String lastName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String zipCode,
        String countryCode,
        String phoneNumber,
        String ssn,
        Short ficoScore,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AccountSummary> accounts
) {
    public static CustomerResponse from(Customer c) {
        List<AccountSummary> accountSummaries = c.getAccounts() != null
                ? c.getAccounts().stream().map(AccountSummary::from).toList()
                : List.of();
        return new CustomerResponse(
                c.getId(),
                c.getFirstName(),
                c.getMiddleName(),
                c.getLastName(),
                c.getAddressLine1(),
                c.getAddressLine2(),
                c.getCity(),
                c.getState(),
                c.getZipCode(),
                c.getCountryCode(),
                c.getPhoneNumber(),
                c.getSsn(),
                c.getFicoScore(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                accountSummaries
        );
    }
}
