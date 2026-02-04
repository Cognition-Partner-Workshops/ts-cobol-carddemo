package com.aws.carddemo.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer entity - migrated from CVCUS01Y.cpy
 * Original VSAM record length: 500 bytes
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_ssn", columnList = "ssn"),
    @Index(name = "idx_customer_fico", columnList = "ficoCreditScore")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @NotBlank
    @Size(max = 25)
    @Column(name = "first_name", length = 25, nullable = false)
    private String firstName;

    @Size(max = 25)
    @Column(name = "middle_name", length = 25)
    private String middleName;

    @NotBlank
    @Size(max = 25)
    @Column(name = "last_name", length = 25, nullable = false)
    private String lastName;

    @Size(max = 50)
    @Column(name = "address_line_1", length = 50)
    private String addressLine1;

    @Size(max = 50)
    @Column(name = "address_line_2", length = 50)
    private String addressLine2;

    @Size(max = 50)
    @Column(name = "address_line_3", length = 50)
    private String addressLine3;

    @Size(max = 2)
    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Size(max = 3)
    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Size(max = 10)
    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Size(max = 15)
    @Column(name = "phone_number_1", length = 15)
    private String phoneNumber1;

    @Size(max = 15)
    @Column(name = "phone_number_2", length = 15)
    private String phoneNumber2;

    @Column(name = "ssn")
    private Long ssn;

    @Size(max = 20)
    @Column(name = "govt_issued_id", length = 20)
    private String govtIssuedId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 10)
    @Column(name = "eft_account_id", length = 10)
    private String eftAccountId;

    @Column(name = "primary_card_holder")
    private Boolean primaryCardHolder;

    @Min(300)
    @Max(850)
    @Column(name = "fico_credit_score")
    private Integer ficoCreditScore;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CardCrossReference> cardCrossReferences = new ArrayList<>();

    @Version
    private Long version;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(firstName);
        if (middleName != null && !middleName.isBlank()) {
            sb.append(" ").append(middleName);
        }
        sb.append(" ").append(lastName);
        return sb.toString();
    }
}
