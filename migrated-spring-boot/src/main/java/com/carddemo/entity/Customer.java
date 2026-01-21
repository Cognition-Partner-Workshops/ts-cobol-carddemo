package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * JPA entity representing a customer.
 * Migrated from mainframe copybook: CVCUS01Y.cpy (CUSTOMER-RECORD)
 *
 * <p>This entity maps the VSAM customer file structure to a relational database table.
 * Sensitive data like SSN should be handled with appropriate security measures.
 *
 * @see com.carddemo.repository.CustomerRepository
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @NotNull
    @Size(max = 25)
    @Column(name = "first_name", length = 25, nullable = false)
    private String firstName;

    @Size(max = 25)
    @Column(name = "middle_name", length = 25)
    private String middleName;

    @NotNull
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
    @Column(name = "government_issued_id", length = 20)
    private String governmentIssuedId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 10)
    @Column(name = "eft_account_id", length = 10)
    private String eftAccountId;

    @Size(max = 1)
    @Column(name = "primary_card_holder_indicator", length = 1)
    private String primaryCardHolderIndicator;

    @Column(name = "fico_credit_score")
    private Integer ficoCreditScore;

    public Customer() {
    }

    public Customer(Long customerId, String firstName, String lastName) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressLine3() {
        return addressLine3;
    }

    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getPhoneNumber1() {
        return phoneNumber1;
    }

    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1;
    }

    public String getPhoneNumber2() {
        return phoneNumber2;
    }

    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2;
    }

    public Long getSsn() {
        return ssn;
    }

    public void setSsn(Long ssn) {
        this.ssn = ssn;
    }

    public String getGovernmentIssuedId() {
        return governmentIssuedId;
    }

    public void setGovernmentIssuedId(String governmentIssuedId) {
        this.governmentIssuedId = governmentIssuedId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEftAccountId() {
        return eftAccountId;
    }

    public void setEftAccountId(String eftAccountId) {
        this.eftAccountId = eftAccountId;
    }

    public String getPrimaryCardHolderIndicator() {
        return primaryCardHolderIndicator;
    }

    public void setPrimaryCardHolderIndicator(String primaryCardHolderIndicator) {
        this.primaryCardHolderIndicator = primaryCardHolderIndicator;
    }

    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }

    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", stateCode='" + stateCode + '\'' +
                '}';
    }
}
