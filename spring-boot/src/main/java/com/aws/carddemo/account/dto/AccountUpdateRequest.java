package com.aws.carddemo.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
        @Pattern(regexp = "[ACS]", message = "Account status must be A (Active), C (Closed), or S (Suspended)")
        String accountStatus,

        @DecimalMin(value = "0.00", message = "Credit limit must be non-negative")
        BigDecimal creditLimit,

        @DecimalMin(value = "0.00", message = "Cash credit limit must be non-negative")
        BigDecimal cashCreditLimit,

        LocalDate expirationDate,

        LocalDate reissueDate,

        @Size(max = 10, message = "Group ID must be at most 10 characters")
        String groupId
) {}
