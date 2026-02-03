package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class Account {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String accountId;
    
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
}
