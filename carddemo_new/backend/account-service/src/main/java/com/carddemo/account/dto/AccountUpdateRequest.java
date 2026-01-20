package com.carddemo.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateRequest {
    @Size(max = 1, message = "Active status must be Y or N")
    private String activeStatus;

    @DecimalMin(value = "0.0", message = "Credit limit must be positive")
    private BigDecimal creditLimit;

    @DecimalMin(value = "0.0", message = "Cash credit limit must be positive")
    private BigDecimal cashCreditLimit;

    private LocalDate expirationDate;

    @Size(max = 10, message = "Group ID must be at most 10 characters")
    private String groupId;
}
