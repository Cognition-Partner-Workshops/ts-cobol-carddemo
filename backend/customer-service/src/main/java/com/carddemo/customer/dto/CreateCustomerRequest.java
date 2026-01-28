package com.carddemo.customer.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 25, message = "First name must be at most 25 characters")
    private String firstName;

    @Size(max = 25, message = "Middle name must be at most 25 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 25, message = "Last name must be at most 25 characters")
    private String lastName;

    @Size(max = 50, message = "Address line 1 must be at most 50 characters")
    private String addressLine1;

    @Size(max = 50, message = "Address line 2 must be at most 50 characters")
    private String addressLine2;

    @Size(max = 50, message = "Address line 3 must be at most 50 characters")
    private String addressLine3;

    @Size(max = 2, message = "State code must be at most 2 characters")
    private String stateCode;

    @Size(max = 3, message = "Country code must be at most 3 characters")
    private String countryCode;

    @Size(max = 10, message = "Zip code must be at most 10 characters")
    private String zipCode;

    @Size(max = 15, message = "Phone number must be at most 15 characters")
    private String phoneNumber1;

    @Size(max = 15, message = "Phone number must be at most 15 characters")
    private String phoneNumber2;

    @NotBlank(message = "SSN is required")
    @Pattern(regexp = "^\\d{9}$", message = "SSN must be exactly 9 digits")
    private String ssn;

    @Size(max = 20, message = "Government issued ID must be at most 20 characters")
    private String govtIssuedId;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 10, message = "EFT account ID must be at most 10 characters")
    private String eftAccountId;

    @Pattern(regexp = "^[YN]$", message = "Primary cardholder indicator must be 'Y' or 'N'")
    private String primaryCardholderInd = "Y";

    @Min(value = 300, message = "FICO score must be at least 300")
    @Max(value = 850, message = "FICO score must be at most 850")
    private Integer ficoCreditScore;
}
