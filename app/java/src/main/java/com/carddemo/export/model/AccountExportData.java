package com.carddemo.export.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Account export data corresponding to COBOL EXPORT-ACCOUNT-DATA structure.
 * Maps to record type 'A' in the CVEXPORT multi-record format.
 * 
 * COBOL Structure (460 bytes total):
 * - EXP-ACCT-ID: PIC 9(11) - 11 bytes display
 * - EXP-ACCT-ACTIVE-STATUS: PIC X(01) - 1 byte
 * - EXP-ACCT-CURR-BAL: PIC S9(10)V99 COMP-3 - 7 bytes packed decimal (BigDecimal in Java)
 * - EXP-ACCT-CREDIT-LIMIT: PIC S9(10)V99 - 13 bytes display (BigDecimal in Java)
 * - EXP-ACCT-CASH-CREDIT-LIMIT: PIC S9(10)V99 COMP-3 - 7 bytes packed decimal (BigDecimal in Java)
 * - EXP-ACCT-OPEN-DATE: PIC X(10) - 10 bytes
 * - EXP-ACCT-EXPIRAION-DATE: PIC X(10) - 10 bytes
 * - EXP-ACCT-REISSUE-DATE: PIC X(10) - 10 bytes
 * - EXP-ACCT-CURR-CYC-CREDIT: PIC S9(10)V99 - 13 bytes display (BigDecimal in Java)
 * - EXP-ACCT-CURR-CYC-DEBIT: PIC S9(10)V99 COMP - 8 bytes binary (BigDecimal in Java)
 * - EXP-ACCT-ADDR-ZIP: PIC X(10) - 10 bytes
 * - EXP-ACCT-GROUP-ID: PIC X(10) - 10 bytes
 * - FILLER: PIC X(352) - 352 bytes
 */
public final class AccountExportData implements ExportRecordData {
    
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MONETARY_SCALE = 2;
    
    private final String accountId;
    private final String activeStatus;
    private final BigDecimal currentBalance;
    private final BigDecimal creditLimit;
    private final BigDecimal cashCreditLimit;
    private final LocalDate openDate;
    private final LocalDate expirationDate;
    private final LocalDate reissueDate;
    private final BigDecimal currentCycleCredit;
    private final BigDecimal currentCycleDebit;
    private final String zipCode;
    private final String groupId;
    
    private AccountExportData(Builder builder) {
        this.accountId = validateAccountId(builder.accountId);
        this.activeStatus = truncateOrPad(builder.activeStatus, 1);
        this.currentBalance = normalizeMonetary(builder.currentBalance);
        this.creditLimit = normalizeMonetary(builder.creditLimit);
        this.cashCreditLimit = normalizeMonetary(builder.cashCreditLimit);
        this.openDate = builder.openDate;
        this.expirationDate = builder.expirationDate;
        this.reissueDate = builder.reissueDate;
        this.currentCycleCredit = normalizeMonetary(builder.currentCycleCredit);
        this.currentCycleDebit = normalizeMonetary(builder.currentCycleDebit);
        this.zipCode = truncateOrPad(builder.zipCode, 10);
        this.groupId = truncateOrPad(builder.groupId, 10);
    }
    
    private String validateAccountId(String accountId) {
        Objects.requireNonNull(accountId, "Account ID is required");
        String digitsOnly = accountId.replaceAll("[^0-9]", "");
        if (digitsOnly.length() > 11) {
            return digitsOnly.substring(0, 11);
        }
        return digitsOnly;
    }
    
    private String truncateOrPad(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }
    
    private BigDecimal normalizeMonetary(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }
    
    @Override
    public RecordType getRecordType() {
        return RecordType.ACCOUNT;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public String getActiveStatus() {
        return activeStatus;
    }
    
    public boolean isActive() {
        return "Y".equalsIgnoreCase(activeStatus);
    }
    
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
    
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }
    
    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(currentBalance);
    }
    
    public BigDecimal getCreditUtilizationPercentage() {
        if (creditLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentBalance.divide(creditLimit, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    public LocalDate getOpenDate() {
        return openDate;
    }
    
    public String getFormattedOpenDate() {
        return openDate != null ? DATE_FORMATTER.format(openDate) : "";
    }
    
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    public String getFormattedExpirationDate() {
        return expirationDate != null ? DATE_FORMATTER.format(expirationDate) : "";
    }
    
    public LocalDate getReissueDate() {
        return reissueDate;
    }
    
    public String getFormattedReissueDate() {
        return reissueDate != null ? DATE_FORMATTER.format(reissueDate) : "";
    }
    
    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }
    
    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }
    
    public BigDecimal getCurrentCycleNetActivity() {
        return currentCycleCredit.subtract(currentCycleDebit);
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountExportData that = (AccountExportData) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(activeStatus, that.activeStatus) &&
                Objects.equals(currentBalance, that.currentBalance) &&
                Objects.equals(creditLimit, that.creditLimit) &&
                Objects.equals(cashCreditLimit, that.cashCreditLimit) &&
                Objects.equals(openDate, that.openDate) &&
                Objects.equals(expirationDate, that.expirationDate) &&
                Objects.equals(reissueDate, that.reissueDate) &&
                Objects.equals(currentCycleCredit, that.currentCycleCredit) &&
                Objects.equals(currentCycleDebit, that.currentCycleDebit) &&
                Objects.equals(zipCode, that.zipCode) &&
                Objects.equals(groupId, that.groupId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(accountId, activeStatus, currentBalance, creditLimit,
                cashCreditLimit, openDate, expirationDate, reissueDate,
                currentCycleCredit, currentCycleDebit, zipCode, groupId);
    }
    
    @Override
    public String toString() {
        return "AccountExportData{" +
                "accountId='" + accountId + '\'' +
                ", activeStatus='" + activeStatus + '\'' +
                ", currentBalance=" + currentBalance +
                ", creditLimit=" + creditLimit +
                ", cashCreditLimit=" + cashCreditLimit +
                ", openDate=" + getFormattedOpenDate() +
                ", expirationDate=" + getFormattedExpirationDate() +
                ", groupId='" + groupId + '\'' +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(AccountExportData source) {
        return new Builder()
                .accountId(source.accountId)
                .activeStatus(source.activeStatus)
                .currentBalance(source.currentBalance)
                .creditLimit(source.creditLimit)
                .cashCreditLimit(source.cashCreditLimit)
                .openDate(source.openDate)
                .expirationDate(source.expirationDate)
                .reissueDate(source.reissueDate)
                .currentCycleCredit(source.currentCycleCredit)
                .currentCycleDebit(source.currentCycleDebit)
                .zipCode(source.zipCode)
                .groupId(source.groupId);
    }
    
    public static final class Builder {
        private String accountId;
        private String activeStatus;
        private BigDecimal currentBalance;
        private BigDecimal creditLimit;
        private BigDecimal cashCreditLimit;
        private LocalDate openDate;
        private LocalDate expirationDate;
        private LocalDate reissueDate;
        private BigDecimal currentCycleCredit;
        private BigDecimal currentCycleDebit;
        private String zipCode;
        private String groupId;
        
        private Builder() {}
        
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }
        
        public Builder activeStatus(String activeStatus) {
            this.activeStatus = activeStatus;
            return this;
        }
        
        public Builder active(boolean isActive) {
            this.activeStatus = isActive ? "Y" : "N";
            return this;
        }
        
        public Builder currentBalance(BigDecimal currentBalance) {
            this.currentBalance = currentBalance;
            return this;
        }
        
        public Builder currentBalance(String currentBalance) {
            if (currentBalance != null && !currentBalance.isBlank()) {
                this.currentBalance = new BigDecimal(currentBalance);
            }
            return this;
        }
        
        public Builder creditLimit(BigDecimal creditLimit) {
            this.creditLimit = creditLimit;
            return this;
        }
        
        public Builder creditLimit(String creditLimit) {
            if (creditLimit != null && !creditLimit.isBlank()) {
                this.creditLimit = new BigDecimal(creditLimit);
            }
            return this;
        }
        
        public Builder cashCreditLimit(BigDecimal cashCreditLimit) {
            this.cashCreditLimit = cashCreditLimit;
            return this;
        }
        
        public Builder cashCreditLimit(String cashCreditLimit) {
            if (cashCreditLimit != null && !cashCreditLimit.isBlank()) {
                this.cashCreditLimit = new BigDecimal(cashCreditLimit);
            }
            return this;
        }
        
        public Builder openDate(LocalDate openDate) {
            this.openDate = openDate;
            return this;
        }
        
        public Builder openDate(String openDate) {
            if (openDate != null && !openDate.isBlank()) {
                this.openDate = LocalDate.parse(openDate, DATE_FORMATTER);
            }
            return this;
        }
        
        public Builder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }
        
        public Builder expirationDate(String expirationDate) {
            if (expirationDate != null && !expirationDate.isBlank()) {
                this.expirationDate = LocalDate.parse(expirationDate, DATE_FORMATTER);
            }
            return this;
        }
        
        public Builder reissueDate(LocalDate reissueDate) {
            this.reissueDate = reissueDate;
            return this;
        }
        
        public Builder reissueDate(String reissueDate) {
            if (reissueDate != null && !reissueDate.isBlank()) {
                this.reissueDate = LocalDate.parse(reissueDate, DATE_FORMATTER);
            }
            return this;
        }
        
        public Builder currentCycleCredit(BigDecimal currentCycleCredit) {
            this.currentCycleCredit = currentCycleCredit;
            return this;
        }
        
        public Builder currentCycleCredit(String currentCycleCredit) {
            if (currentCycleCredit != null && !currentCycleCredit.isBlank()) {
                this.currentCycleCredit = new BigDecimal(currentCycleCredit);
            }
            return this;
        }
        
        public Builder currentCycleDebit(BigDecimal currentCycleDebit) {
            this.currentCycleDebit = currentCycleDebit;
            return this;
        }
        
        public Builder currentCycleDebit(String currentCycleDebit) {
            if (currentCycleDebit != null && !currentCycleDebit.isBlank()) {
                this.currentCycleDebit = new BigDecimal(currentCycleDebit);
            }
            return this;
        }
        
        public Builder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }
        
        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }
        
        public AccountExportData build() {
            return new AccountExportData(this);
        }
    }
}
