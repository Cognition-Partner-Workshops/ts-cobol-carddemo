package com.carddemo.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity for the customer table.
 * Replaces CUSTDAT VSAM KSDS (CVCUS01Y.cpy, 500 bytes).
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @Column(name = "customer_id", precision = 9, scale = 0, nullable = false)
    private BigDecimal customerId;

    @Column(name = "first_name", length = 25, nullable = false)
    private String firstName;

    @Column(name = "middle_name", length = 25)
    private String middleName;

    @Column(name = "last_name", length = 25, nullable = false)
    private String lastName;

    @Column(name = "address_line_1", length = 50)
    private String addressLine1;

    @Column(name = "address_line_2", length = 50)
    private String addressLine2;

    @Column(name = "address_line_3", length = 50)
    private String addressLine3;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "address_zip", length = 10)
    private String addressZip;

    @Column(name = "phone_number_1", length = 15)
    private String phoneNumber1;

    @Column(name = "phone_number_2", length = 15)
    private String phoneNumber2;

    @Column(name = "ssn", precision = 9, scale = 0)
    private BigDecimal ssn;

    @Column(name = "government_issued_id", length = 20)
    private String governmentIssuedId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "eft_account_id", length = 10)
    private String eftAccountId;

    @Column(name = "primary_card_holder_ind", length = 1)
    private String primaryCardHolderInd;

    @Column(name = "fico_credit_score", precision = 3, scale = 0)
    private BigDecimal ficoCreditScore;

    public Customer() {
    }

    public BigDecimal getCustomerId() {
        return customerId;
    }

    public void setCustomerId(BigDecimal customerId) {
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

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
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

    public BigDecimal getSsn() {
        return ssn;
    }

    public void setSsn(BigDecimal ssn) {
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

    public String getPrimaryCardHolderInd() {
        return primaryCardHolderInd;
    }

    public void setPrimaryCardHolderInd(String primaryCardHolderInd) {
        this.primaryCardHolderInd = primaryCardHolderInd;
    }

    public BigDecimal getFicoCreditScore() {
        return ficoCreditScore;
    }

    public void setFicoCreditScore(BigDecimal ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }
}
