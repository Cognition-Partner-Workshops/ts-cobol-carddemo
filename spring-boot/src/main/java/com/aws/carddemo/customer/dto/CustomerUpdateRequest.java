package com.aws.carddemo.customer.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(
        @Size(max = 25, message = "First name must be at most 25 characters")
        String firstName,

        @Size(max = 25, message = "Middle name must be at most 25 characters")
        String middleName,

        @Size(max = 25, message = "Last name must be at most 25 characters")
        String lastName,

        @Size(max = 50, message = "Address line 1 must be at most 50 characters")
        String addressLine1,

        @Size(max = 50, message = "Address line 2 must be at most 50 characters")
        String addressLine2,

        @Size(max = 30, message = "City must be at most 30 characters")
        String city,

        @Pattern(regexp = "[A-Z]{2}", message = "State must be a valid 2-letter US state code")
        String state,

        @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "ZIP code must be in format 99999 or 99999-9999")
        String zipCode,

        @Size(max = 3, message = "Country code must be at most 3 characters")
        String countryCode,

        @Pattern(regexp = "\\d{10,15}", message = "Phone number must contain 10-15 digits")
        String phoneNumber
) {}
