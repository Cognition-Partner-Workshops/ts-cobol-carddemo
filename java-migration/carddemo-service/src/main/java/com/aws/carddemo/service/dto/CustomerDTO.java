package com.aws.carddemo.service.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private Long customerId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String stateCode;
    private String countryCode;
    private String zipCode;
    private String phoneNumber1;
    private String phoneNumber2;
    private String govtIssuedId;
    private LocalDate dateOfBirth;
    private String eftAccountId;
    private Boolean primaryCardHolder;
    private Integer ficoCreditScore;
}
