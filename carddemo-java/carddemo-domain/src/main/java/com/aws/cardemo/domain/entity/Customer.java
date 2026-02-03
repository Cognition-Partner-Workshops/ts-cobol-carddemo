package com.aws.cardemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(name = "customer_id", length = 9)
    private String customerId;

    @NotNull
    @Column(name = "first_name", length = 25)
    private String firstName;

    @Column(name = "middle_name", length = 25)
    private String middleName;

    @NotNull
    @Column(name = "last_name", length = 25)
    private String lastName;

    @Column(name = "address_line1", length = 50)
    private String addressLine1;

    @Column(name = "address_line2", length = 50)
    private String addressLine2;

    @Column(name = "address_line3", length = 50)
    private String addressLine3;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "phone_number1", length = 15)
    private String phoneNumber1;

    @Column(name = "phone_number2", length = 15)
    private String phoneNumber2;

    @Column(name = "ssn", length = 9)
    private String ssn;

    @Column(name = "government_id", length = 20)
    private String governmentId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "fico_credit_score")
    private Integer ficoCreditScore;
}
