package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDto {
    private Long customerId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String stateCode;
    private String countryCode;
    private String zipCode;
    private String phoneNumber1;
    private String phoneNumber2;
    private String ssnMasked;
    private String govtIssuedId;
    private LocalDate dateOfBirth;
    private String eftAccountId;
    private String primaryCardholderInd;
    private Integer ficoCreditScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
