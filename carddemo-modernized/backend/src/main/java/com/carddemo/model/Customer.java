package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
public class Customer {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String customerId;
    
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
    
    private String ssn;
    private String govtIssuedId;
    private LocalDate dateOfBirth;
    
    private String eftAccountId;
    private String primaryCardHolderInd;
    private Integer ficoCreditScore;
}
