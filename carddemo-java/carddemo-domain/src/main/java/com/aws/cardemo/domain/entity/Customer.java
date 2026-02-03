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

/**
 * JPA Entity representing a credit card customer in the CardDemo system.
 * 
 * This entity maps to the 'customers' table and stores all customer profile information
 * including personal details, contact information, and credit scoring data. It represents
 * the modernized version of the COBOL CUSTDATA-RECORD from the original mainframe application.
 * 
 * Customer data includes sensitive information (SSN, government ID) that should be
 * handled securely and access should be restricted based on user roles.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    /**
     * Unique customer identifier (primary key).
     * Maximum length: 9 characters.
     */
    @Id
    @Column(name = "customer_id", length = 9)
    private String customerId;

    /**
     * Customer's first name.
     * Required field.
     */
    @NotNull
    @Column(name = "first_name", length = 25)
    private String firstName;

    /**
     * Customer's middle name.
     * Optional field.
     */
    @Column(name = "middle_name", length = 25)
    private String middleName;

    /**
     * Customer's last name.
     * Required field.
     */
    @NotNull
    @Column(name = "last_name", length = 25)
    private String lastName;

    /**
     * Primary address line (street address).
     */
    @Column(name = "address_line1", length = 50)
    private String addressLine1;

    /**
     * Secondary address line (apartment, suite, etc.).
     */
    @Column(name = "address_line2", length = 50)
    private String addressLine2;

    /**
     * Tertiary address line (city).
     */
    @Column(name = "address_line3", length = 50)
    private String addressLine3;

    /**
     * Two-letter state code (e.g., CA, NY, TX).
     */
    @Column(name = "state_code", length = 2)
    private String stateCode;

    /**
     * Three-letter country code (e.g., USA).
     */
    @Column(name = "country_code", length = 3)
    private String countryCode;

    /**
     * Postal/ZIP code.
     */
    @Column(name = "postal_code", length = 10)
    private String postalCode;

    /**
     * Primary phone number.
     */
    @Column(name = "phone_number1", length = 15)
    private String phoneNumber1;

    /**
     * Secondary phone number.
     */
    @Column(name = "phone_number2", length = 15)
    private String phoneNumber2;

    /**
     * Social Security Number (9 digits).
     * Sensitive data - handle securely.
     */
    @Column(name = "ssn", length = 9)
    private String ssn;

    /**
     * Government-issued identification number.
     * Sensitive data - handle securely.
     */
    @Column(name = "government_id", length = 20)
    private String governmentId;

    /**
     * Customer's date of birth.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * FICO credit score (typically 300-850).
     * Used for credit limit decisions and risk assessment.
     */
    @Column(name = "fico_credit_score")
    private Integer ficoCreditScore;
}
