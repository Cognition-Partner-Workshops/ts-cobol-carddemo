package com.aws.carddemo.service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {
    private Long accountId;
    private String activeStatus;
    private BigDecimal currentBalance;
    private BigDecimal creditLimit;
    private BigDecimal cashCreditLimit;
    private LocalDate openDate;
    private LocalDate expirationDate;
    private LocalDate reissueDate;
    private BigDecimal currentCycleCredit;
    private BigDecimal currentCycleDebit;
    private String zipCode;
    private String groupId;
    private BigDecimal availableCredit;
    private boolean overLimit;
}
