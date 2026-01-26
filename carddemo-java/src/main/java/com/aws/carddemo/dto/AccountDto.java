package com.aws.carddemo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    @NotNull(message = "Account ID is required")
    @Digits(integer = 11, fraction = 0, message = "Account ID must be at most 11 digits")
    private Long acctId;

    @Size(max = 1, message = "Active status must be 1 character")
    private String acctActiveStatus;

    @DecimalMin(value = "-9999999999.99", message = "Current balance out of range")
    @DecimalMax(value = "9999999999.99", message = "Current balance out of range")
    private BigDecimal acctCurrBal;

    @DecimalMin(value = "0.00", message = "Credit limit must be positive")
    @DecimalMax(value = "9999999999.99", message = "Credit limit out of range")
    private BigDecimal acctCreditLimit;

    @DecimalMin(value = "0.00", message = "Cash credit limit must be positive")
    @DecimalMax(value = "9999999999.99", message = "Cash credit limit out of range")
    private BigDecimal acctCashCreditLimit;

    @NotNull(message = "Open date is required")
    private LocalDate acctOpenDate;

    @NotNull(message = "Expiration date is required")
    private LocalDate acctExpirationDate;

    private LocalDate acctReissueDate;

    private BigDecimal acctCurrCycCredit;

    private BigDecimal acctCurrCycDebit;

    @Size(max = 10, message = "ZIP code must be at most 10 characters")
    private String acctAddrZip;

    @Size(max = 10, message = "Group ID must be at most 10 characters")
    private String acctGroupId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private BigDecimal availableCredit;
    private boolean active;
    private boolean expired;
}
