package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Customer entity mapped from COBOL copybook CVCUS01Y.
 * Original VSAM file: AWS.M2.CARDDEMO.CUSTDATA.PS (KSDS, 500-byte records)
 * Primary key: CUST-ID PIC 9(09)
 */
@Entity
@Table(name = "customer", indexes = {
        @Index(name = "idx_customer_ssn", columnList = "ssn"),
        @Index(name = "idx_customer_last_name", columnList = "last_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "cust_id")
    private Long custId;

    @NotNull
    @Column(name = "first_name", length = 25)
    private String firstName;

    @Column(name = "middle_name", length = 25)
    private String middleName;

    @NotNull
    @Column(name = "last_name", length = 25)
    private String lastName;

    @Column(name = "addr_line_1", length = 50)
    private String addrLine1;

    @Column(name = "addr_line_2", length = 50)
    private String addrLine2;

    @Column(name = "addr_line_3", length = 50)
    private String addrLine3;

    @Column(name = "addr_state_code", length = 2)
    private String addrStateCode;

    @Column(name = "addr_country_code", length = 3)
    private String addrCountryCode;

    @Column(name = "addr_zip", length = 10)
    private String addrZip;

    @Column(name = "phone_num_1", length = 15)
    private String phoneNum1;

    @Column(name = "phone_num_2", length = 15)
    private String phoneNum2;

    @Column(name = "ssn", length = 11)
    private String ssn;

    @Column(name = "govt_issued_id", length = 20)
    private String govtIssuedId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "eft_account_id", length = 10)
    private String eftAccountId;

    @Column(name = "primary_card_holder", length = 1)
    private String primaryCardHolder;

    @Column(name = "fico_credit_score")
    private Integer ficoCreditScore;
}
