package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Account entity - migrated from VSAM file ACCTFILE / copybook CVACT01Y.
 * Original record length: 300 bytes.
 *
 * Fields mapped from COBOL:
 *   ACCT-ID                 PIC 9(11)       -> acctId
 *   ACCT-ACTIVE-STATUS      PIC X(01)       -> activeStatus
 *   ACCT-CURR-BAL           PIC S9(10)V99   -> currentBalance
 *   ACCT-CREDIT-LIMIT       PIC S9(10)V99   -> creditLimit
 *   ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99   -> cashCreditLimit
 *   ACCT-OPEN-DATE          PIC X(10)       -> openDate
 *   ACCT-EXPIRAION-DATE     PIC X(10)       -> expirationDate
 *   ACCT-REISSUE-DATE       PIC X(10)       -> reissueDate
 *   ACCT-CURR-CYC-CREDIT    PIC S9(10)V99   -> currentCycleCredit
 *   ACCT-CURR-CYC-DEBIT     PIC S9(10)V99   -> currentCycleDebit
 *   ACCT-ADDR-ZIP           PIC X(10)       -> addressZip
 *   ACCT-GROUP-ID           PIC X(10)       -> groupId
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "active_status", length = 1)
    @Size(max = 1)
    private String activeStatus;

    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    @Column(name = "open_date", length = 10)
    @Size(max = 10)
    private String openDate;

    @Column(name = "expiration_date", length = 10)
    @Size(max = 10)
    private String expirationDate;

    @Column(name = "reissue_date", length = 10)
    @Size(max = 10)
    private String reissueDate;

    @Column(name = "current_cycle_credit", precision = 12, scale = 2)
    private BigDecimal currentCycleCredit;

    @Column(name = "current_cycle_debit", precision = 12, scale = 2)
    private BigDecimal currentCycleDebit;

    @Column(name = "address_zip", length = 10)
    @Size(max = 10)
    private String addressZip;

    @Column(name = "group_id", length = 10)
    @Size(max = 10)
    private String groupId;

    public Account() {
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
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
