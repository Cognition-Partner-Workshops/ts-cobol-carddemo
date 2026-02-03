package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Customer entity - migrated from COBOL copybook CVCUS01Y.cpy
 * Original COBOL record length: 500 bytes
 */
@Entity
@Table(name = "CUSTDATA")
public class Customer {

    @Id
    @Column(name = "CUST_ID")
    private Long custId;

    @Column(name = "CUST_FIRST_NAME", length = 25)
    private String custFirstName;

    @Column(name = "CUST_MIDDLE_NAME", length = 25)
    private String custMiddleName;

    @Column(name = "CUST_LAST_NAME", length = 25)
    private String custLastName;

    @Column(name = "CUST_ADDR_LINE_1", length = 50)
    private String custAddrLine1;

    @Column(name = "CUST_ADDR_LINE_2", length = 50)
    private String custAddrLine2;

    @Column(name = "CUST_ADDR_LINE_3", length = 50)
    private String custAddrLine3;

    @Column(name = "CUST_ADDR_STATE_CD", length = 2)
    private String custAddrStateCd;

    @Column(name = "CUST_ADDR_COUNTRY_CD", length = 3)
    private String custAddrCountryCd;

    @Column(name = "CUST_ADDR_ZIP", length = 10)
    private String custAddrZip;

    @Column(name = "CUST_PHONE_NUM_1", length = 15)
    private String custPhoneNum1;

    @Column(name = "CUST_PHONE_NUM_2", length = 15)
    private String custPhoneNum2;

    @Column(name = "CUST_SSN")
    private Long custSsn;

    @Column(name = "CUST_GOVT_ISSUED_ID", length = 20)
    private String custGovtIssuedId;

    @Column(name = "CUST_DOB_YYYY_MM_DD", length = 10)
    private String custDobYyyyMmDd;

    @Column(name = "CUST_EFT_ACCOUNT_ID", length = 10)
    private String custEftAccountId;

    @Column(name = "CUST_PRI_CARD_HOLDER_IND", length = 1)
    private String custPriCardHolderInd;

    @Column(name = "CUST_FICO_CREDIT_SCORE")
    private Integer custFicoCreditScore;

    public Customer() {
    }

    public Customer(Long custId) {
        this.custId = custId;
    }

    public Long getCustId() {
        return custId;
    }

    public void setCustId(Long custId) {
        this.custId = custId;
    }

    public String getCustFirstName() {
        return custFirstName;
    }

    public void setCustFirstName(String custFirstName) {
        this.custFirstName = custFirstName;
    }

    public String getCustMiddleName() {
        return custMiddleName;
    }

    public void setCustMiddleName(String custMiddleName) {
        this.custMiddleName = custMiddleName;
    }

    public String getCustLastName() {
        return custLastName;
    }

    public void setCustLastName(String custLastName) {
        this.custLastName = custLastName;
    }

    public String getCustAddrLine1() {
        return custAddrLine1;
    }

    public void setCustAddrLine1(String custAddrLine1) {
        this.custAddrLine1 = custAddrLine1;
    }

    public String getCustAddrLine2() {
        return custAddrLine2;
    }

    public void setCustAddrLine2(String custAddrLine2) {
        this.custAddrLine2 = custAddrLine2;
    }

    public String getCustAddrLine3() {
        return custAddrLine3;
    }

    public void setCustAddrLine3(String custAddrLine3) {
        this.custAddrLine3 = custAddrLine3;
    }

    public String getCustAddrStateCd() {
        return custAddrStateCd;
    }

    public void setCustAddrStateCd(String custAddrStateCd) {
        this.custAddrStateCd = custAddrStateCd;
    }

    public String getCustAddrCountryCd() {
        return custAddrCountryCd;
    }

    public void setCustAddrCountryCd(String custAddrCountryCd) {
        this.custAddrCountryCd = custAddrCountryCd;
    }

    public String getCustAddrZip() {
        return custAddrZip;
    }

    public void setCustAddrZip(String custAddrZip) {
        this.custAddrZip = custAddrZip;
    }

    public String getCustPhoneNum1() {
        return custPhoneNum1;
    }

    public void setCustPhoneNum1(String custPhoneNum1) {
        this.custPhoneNum1 = custPhoneNum1;
    }

    public String getCustPhoneNum2() {
        return custPhoneNum2;
    }

    public void setCustPhoneNum2(String custPhoneNum2) {
        this.custPhoneNum2 = custPhoneNum2;
    }

    public Long getCustSsn() {
        return custSsn;
    }

    public void setCustSsn(Long custSsn) {
        this.custSsn = custSsn;
    }

    public String getCustGovtIssuedId() {
        return custGovtIssuedId;
    }

    public void setCustGovtIssuedId(String custGovtIssuedId) {
        this.custGovtIssuedId = custGovtIssuedId;
    }

    public String getCustDobYyyyMmDd() {
        return custDobYyyyMmDd;
    }

    public void setCustDobYyyyMmDd(String custDobYyyyMmDd) {
        this.custDobYyyyMmDd = custDobYyyyMmDd;
    }

    public String getCustEftAccountId() {
        return custEftAccountId;
    }

    public void setCustEftAccountId(String custEftAccountId) {
        this.custEftAccountId = custEftAccountId;
    }

    public String getCustPriCardHolderInd() {
        return custPriCardHolderInd;
    }

    public void setCustPriCardHolderInd(String custPriCardHolderInd) {
        this.custPriCardHolderInd = custPriCardHolderInd;
    }

    public Integer getCustFicoCreditScore() {
        return custFicoCreditScore;
    }

    public void setCustFicoCreditScore(Integer custFicoCreditScore) {
        this.custFicoCreditScore = custFicoCreditScore;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "custId=" + custId +
                ", custFirstName='" + custFirstName + '\'' +
                ", custMiddleName='" + custMiddleName + '\'' +
                ", custLastName='" + custLastName + '\'' +
                ", custAddrLine1='" + custAddrLine1 + '\'' +
                ", custAddrLine2='" + custAddrLine2 + '\'' +
                ", custAddrLine3='" + custAddrLine3 + '\'' +
                ", custAddrStateCd='" + custAddrStateCd + '\'' +
                ", custAddrCountryCd='" + custAddrCountryCd + '\'' +
                ", custAddrZip='" + custAddrZip + '\'' +
                ", custPhoneNum1='" + custPhoneNum1 + '\'' +
                ", custPhoneNum2='" + custPhoneNum2 + '\'' +
                ", custSsn=" + custSsn +
                ", custGovtIssuedId='" + custGovtIssuedId + '\'' +
                ", custDobYyyyMmDd='" + custDobYyyyMmDd + '\'' +
                ", custEftAccountId='" + custEftAccountId + '\'' +
                ", custPriCardHolderInd='" + custPriCardHolderInd + '\'' +
                ", custFicoCreditScore=" + custFicoCreditScore +
                '}';
    }
}
