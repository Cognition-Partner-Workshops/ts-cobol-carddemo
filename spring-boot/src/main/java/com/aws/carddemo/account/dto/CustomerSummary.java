package com.aws.carddemo.account.dto;

import com.aws.carddemo.customer.Customer;

public record CustomerSummary(
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
        String phoneNumber
) {
    public static CustomerSummary from(Customer c) {
        return new CustomerSummary(
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
                c.getPhoneNumber()
        );
    }
}
