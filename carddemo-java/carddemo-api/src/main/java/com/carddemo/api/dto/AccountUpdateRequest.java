package com.carddemo.api.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Account update request DTO replacing BMS map COACTUP input fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountUpdateRequest {

    @Size(max = 1, message = "Active status must be 1 character")
    private String activeStatus;

    private BigDecimal creditLimit;
    private BigDecimal cashCreditLimit;
    private LocalDate expirationDate;
    private LocalDate reissueDate;

    @Size(max = 10, message = "ZIP code must not exceed 10 characters")
    private String addressZip;

    @Size(max = 10, message = "Group ID must not exceed 10 characters")
    private String groupId;
}
