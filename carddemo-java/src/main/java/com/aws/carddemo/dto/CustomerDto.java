package com.aws.carddemo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    @NotNull(message = "Customer ID is required")
    @Digits(integer = 9, fraction = 0, message = "Customer ID must be at most 9 digits")
    private Long custId;

    @NotBlank(message = "First name is required")
    @Size(max = 25, message = "First name must be at most 25 characters")
    private String custFirstName;

    @Size(max = 25, message = "Middle name must be at most 25 characters")
    private String custMiddleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 25, message = "Last name must be at most 25 characters")
    private String custLastName;

    @Size(max = 50, message = "Address line 1 must be at most 50 characters")
    private String custAddrLine1;

    @Size(max = 50, message = "Address line 2 must be at most 50 characters")
    private String custAddrLine2;

    @Size(max = 50, message = "Address line 3 must be at most 50 characters")
    private String custAddrLine3;

    @Size(max = 2, message = "State code must be at most 2 characters")
    private String custAddrStateCd;

    @Size(max = 3, message = "Country code must be at most 3 characters")
    private String custAddrCountryCd;

    @Size(max = 10, message = "ZIP code must be at most 10 characters")
    private String custAddrZip;

    @Size(max = 15, message = "Phone number 1 must be at most 15 characters")
    private String custPhoneNum1;

    @Size(max = 15, message = "Phone number 2 must be at most 15 characters")
    private String custPhoneNum2;

    @Size(max = 9, message = "SSN must be at most 9 characters")
    private String custSsn;

    @Size(max = 20, message = "Government ID must be at most 20 characters")
    private String custGovtIssuedId;

    private LocalDate custDob;

    @Size(max = 10, message = "EFT account ID must be at most 10 characters")
    private String custEftAccountId;

    @Size(max = 1, message = "Primary card holder indicator must be 1 character")
    private String custPriCardHolderInd;

    @Min(value = 0, message = "FICO score must be positive")
    @Max(value = 999, message = "FICO score must be at most 999")
    private Integer custFicoCreditScore;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String fullName;
    private boolean primaryCardHolder;
}
