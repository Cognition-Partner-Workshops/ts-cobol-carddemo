package com.carddemo.billing;

import java.math.BigDecimal;

/**
 * Java equivalent of the COBOL ACCOUNT-RECORD copybook (CVACT01Y.cpy).
 *
 * <pre>
 * 01  ACCOUNT-RECORD.
 *     05  ACCT-ID                 PIC 9(11).
 *     05  ACCT-ACTIVE-STATUS      PIC X(01).
 *     05  ACCT-CURR-BAL           PIC S9(10)V99.
 *     05  ACCT-CREDIT-LIMIT       PIC S9(10)V99.
 *     05  ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99.
 *     05  ACCT-OPEN-DATE          PIC X(10).
 *     05  ACCT-EXPIRAION-DATE     PIC X(10).
 *     05  ACCT-REISSUE-DATE       PIC X(10).
 *     05  ACCT-CURR-CYC-CREDIT    PIC S9(10)V99.
 *     05  ACCT-CURR-CYC-DEBIT     PIC S9(10)V99.
 *     05  ACCT-ADDR-ZIP           PIC X(10).
 *     05  ACCT-GROUP-ID           PIC X(10).
 * </pre>
 */
public class AccountRecord {

    private String accountId;
    private String activeStatus;
    private BigDecimal currentBalance;
    private BigDecimal creditLimit;
    private BigDecimal cashCreditLimit;
    private String openDate;
    private String expirationDate;
    private String reissueDate;
    private BigDecimal currentCycleCredit;
    private BigDecimal currentCycleDebit;
    private String addressZip;
    private String groupId;

    public AccountRecord() {
        this.currentBalance = BigDecimal.ZERO;
        this.creditLimit = BigDecimal.ZERO;
        this.cashCreditLimit = BigDecimal.ZERO;
        this.currentCycleCredit = BigDecimal.ZERO;
        this.currentCycleDebit = BigDecimal.ZERO;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
    }

    public String getOpenDate() {
        return openDate;
    }

    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getReissueDate() {
        return reissueDate;
    }

    public void setReissueDate(String reissueDate) {
        this.reissueDate = reissueDate;
    }

    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit;
    }

    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
