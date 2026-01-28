package com.carddemo.account.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    @Pattern(regexp = "^[YN]$", message = "Active status must be 'Y' or 'N'")
    private String activeStatus;

    @DecimalMin(value = "0.00", message = "Credit limit must be positive")
    private BigDecimal creditLimit;

    @DecimalMin(value = "0.00", message = "Cash credit limit must be positive")
    private BigDecimal cashCreditLimit;

    private LocalDate expirationDate;

    private LocalDate reissueDate;

    @Size(max = 10, message = "Address zip must be at most 10 characters")
    private String addressZip;

    @Size(max = 10, message = "Group ID must be at most 10 characters")
    private String groupId;
}
