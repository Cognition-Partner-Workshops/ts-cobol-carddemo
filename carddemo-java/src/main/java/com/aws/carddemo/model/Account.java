package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Account entity - migrated from COBOL copybook CVACT01Y.cpy
 * Original COBOL record length: 300 bytes
 */
@Entity
@Table(name = "ACCTDATA")
public class Account {

    @Id
    @Column(name = "ACCT_ID", length = 11)
    private Long acctId;

    @Column(name = "ACCT_ACTIVE_STATUS", length = 1)
    private String acctActiveStatus;

    @Column(name = "ACCT_CURR_BAL", precision = 12, scale = 2)
    private BigDecimal acctCurrBal;

    @Column(name = "ACCT_CREDIT_LIMIT", precision = 12, scale = 2)
    private BigDecimal acctCreditLimit;

    @Column(name = "ACCT_CASH_CREDIT_LIMIT", precision = 12, scale = 2)
    private BigDecimal acctCashCreditLimit;

    @Column(name = "ACCT_OPEN_DATE", length = 10)
    private String acctOpenDate;

    @Column(name = "ACCT_EXPIRAION_DATE", length = 10)
    private String acctExpirationDate;

    @Column(name = "ACCT_REISSUE_DATE", length = 10)
    private String acctReissueDate;

    @Column(name = "ACCT_CURR_CYC_CREDIT", precision = 12, scale = 2)
    private BigDecimal acctCurrCycCredit;

    @Column(name = "ACCT_CURR_CYC_DEBIT", precision = 12, scale = 2)
    private BigDecimal acctCurrCycDebit;

    @Column(name = "ACCT_ADDR_ZIP", length = 10)
    private String acctAddrZip;

    @Column(name = "ACCT_GROUP_ID", length = 10)
    private String acctGroupId;

    public Account() {
    }

    public Account(Long acctId) {
        this.acctId = acctId;
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public String getAcctActiveStatus() {
        return acctActiveStatus;
    }

    public void setAcctActiveStatus(String acctActiveStatus) {
        this.acctActiveStatus = acctActiveStatus;
    }

    public BigDecimal getAcctCurrBal() {
        return acctCurrBal;
    }

    public void setAcctCurrBal(BigDecimal acctCurrBal) {
        this.acctCurrBal = acctCurrBal;
    }

    public BigDecimal getAcctCreditLimit() {
        return acctCreditLimit;
    }

    public void setAcctCreditLimit(BigDecimal acctCreditLimit) {
        this.acctCreditLimit = acctCreditLimit;
    }

    public BigDecimal getAcctCashCreditLimit() {
        return acctCashCreditLimit;
    }

    public void setAcctCashCreditLimit(BigDecimal acctCashCreditLimit) {
        this.acctCashCreditLimit = acctCashCreditLimit;
    }

    public String getAcctOpenDate() {
        return acctOpenDate;
    }

    public void setAcctOpenDate(String acctOpenDate) {
        this.acctOpenDate = acctOpenDate;
    }

    public String getAcctExpirationDate() {
        return acctExpirationDate;
    }

    public void setAcctExpirationDate(String acctExpirationDate) {
        this.acctExpirationDate = acctExpirationDate;
    }

    public String getAcctReissueDate() {
        return acctReissueDate;
    }

    public void setAcctReissueDate(String acctReissueDate) {
        this.acctReissueDate = acctReissueDate;
    }

    public BigDecimal getAcctCurrCycCredit() {
        return acctCurrCycCredit;
    }

    public void setAcctCurrCycCredit(BigDecimal acctCurrCycCredit) {
        this.acctCurrCycCredit = acctCurrCycCredit;
    }

    public BigDecimal getAcctCurrCycDebit() {
        return acctCurrCycDebit;
    }

    public void setAcctCurrCycDebit(BigDecimal acctCurrCycDebit) {
        this.acctCurrCycDebit = acctCurrCycDebit;
    }

    public String getAcctAddrZip() {
        return acctAddrZip;
    }

    public void setAcctAddrZip(String acctAddrZip) {
        this.acctAddrZip = acctAddrZip;
    }

    public String getAcctGroupId() {
        return acctGroupId;
    }

    public void setAcctGroupId(String acctGroupId) {
        this.acctGroupId = acctGroupId;
    }

    @Override
    public String toString() {
        return "Account{" +
                "acctId=" + acctId +
                ", acctActiveStatus='" + acctActiveStatus + '\'' +
                ", acctCurrBal=" + acctCurrBal +
                ", acctCreditLimit=" + acctCreditLimit +
                ", acctCashCreditLimit=" + acctCashCreditLimit +
                ", acctOpenDate='" + acctOpenDate + '\'' +
                ", acctExpirationDate='" + acctExpirationDate + '\'' +
                ", acctReissueDate='" + acctReissueDate + '\'' +
                ", acctCurrCycCredit=" + acctCurrCycCredit +
                ", acctCurrCycDebit=" + acctCurrCycDebit +
                ", acctAddrZip='" + acctAddrZip + '\'' +
                ", acctGroupId='" + acctGroupId + '\'' +
                '}';
    }
}
