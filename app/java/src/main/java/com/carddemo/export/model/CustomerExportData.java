package com.carddemo.export.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Customer export data corresponding to COBOL EXPORT-CUSTOMER-DATA structure.
 * Maps to record type 'C' in the CVEXPORT multi-record format.
 * 
 * COBOL Structure (460 bytes total):
 * - EXP-CUST-ID: PIC 9(09) COMP - 4 bytes binary (Long in Java)
 * - EXP-CUST-FIRST-NAME: PIC X(25) - 25 bytes
 * - EXP-CUST-MIDDLE-NAME: PIC X(25) - 25 bytes
 * - EXP-CUST-LAST-NAME: PIC X(25) - 25 bytes
 * - EXP-CUST-ADDR-LINES OCCURS 3 TIMES: 3 x 50 = 150 bytes
 * - EXP-CUST-ADDR-STATE-CD: PIC X(02) - 2 bytes
 * - EXP-CUST-ADDR-COUNTRY-CD: PIC X(03) - 3 bytes
 * - EXP-CUST-ADDR-ZIP: PIC X(10) - 10 bytes
 * - EXP-CUST-PHONE-NUMS OCCURS 2 TIMES: 2 x 15 = 30 bytes
 * - EXP-CUST-SSN: PIC 9(09) - 9 bytes display
 * - EXP-CUST-GOVT-ISSUED-ID: PIC X(20) - 20 bytes
 * - EXP-CUST-DOB-YYYY-MM-DD: PIC X(10) - 10 bytes
 * - EXP-CUST-EFT-ACCOUNT-ID: PIC X(10) - 10 bytes
 * - EXP-CUST-PRI-CARD-HOLDER-IND: PIC X(01) - 1 byte
 * - EXP-CUST-FICO-CREDIT-SCORE: PIC 9(03) COMP-3 - 2 bytes packed decimal (Integer in Java)
 * - FILLER: PIC X(134) - 134 bytes
 */
public final class CustomerExportData implements ExportRecordData {
    
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private final Long customerId;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final List<String> addressLines;
    private final String stateCode;
    private final String countryCode;
    private final String zipCode;
    private final List<String> phoneNumbers;
    private final String ssn;
    private final String governmentIssuedId;
    private final LocalDate dateOfBirth;
    private final String eftAccountId;
    private final String primaryCardHolderIndicator;
    private final Integer ficoCreditScore;
    
    private CustomerExportData(Builder builder) {
        this.customerId = Objects.requireNonNull(builder.customerId, "Customer ID is required");
        this.firstName = truncateOrPad(builder.firstName, 25);
        this.middleName = truncateOrPad(builder.middleName, 25);
        this.lastName = truncateOrPad(builder.lastName, 25);
        this.addressLines = validateAddressLines(builder.addressLines);
        this.stateCode = truncateOrPad(builder.stateCode, 2);
        this.countryCode = truncateOrPad(builder.countryCode, 3);
        this.zipCode = truncateOrPad(builder.zipCode, 10);
        this.phoneNumbers = validatePhoneNumbers(builder.phoneNumbers);
        this.ssn = validateSsn(builder.ssn);
        this.governmentIssuedId = truncateOrPad(builder.governmentIssuedId, 20);
        this.dateOfBirth = builder.dateOfBirth;
        this.eftAccountId = truncateOrPad(builder.eftAccountId, 10);
        this.primaryCardHolderIndicator = truncateOrPad(builder.primaryCardHolderIndicator, 1);
        this.ficoCreditScore = validateFicoScore(builder.ficoCreditScore);
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
    
    private List<String> validateAddressLines(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (lines != null && i < lines.size()) {
                result.add(truncateOrPad(lines.get(i), 50));
            } else {
                result.add("");
            }
        }
        return Collections.unmodifiableList(result);
    }
    
    private List<String> validatePhoneNumbers(List<String> phones) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if (phones != null && i < phones.size()) {
                result.add(truncateOrPad(phones.get(i), 15));
            } else {
                result.add("");
            }
        }
        return Collections.unmodifiableList(result);
    }
    
    private String validateSsn(String ssn) {
        if (ssn == null) {
            return "";
        }
        String digitsOnly = ssn.replaceAll("[^0-9]", "");
        if (digitsOnly.length() > 9) {
            return digitsOnly.substring(0, 9);
        }
        return digitsOnly;
    }
    
    private Integer validateFicoScore(Integer score) {
        if (score == null) {
            return null;
        }
        if (score < 0 || score > 999) {
            throw new IllegalArgumentException("FICO score must be between 0 and 999");
        }
        return score;
    }
    
    @Override
    public RecordType getRecordType() {
        return RecordType.CUSTOMER;
    }
    
    public Long getCustomerId() {
        return customerId;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getMiddleName() {
        return middleName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (!firstName.isBlank()) {
            sb.append(firstName.trim());
        }
        if (!middleName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }
        if (!lastName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName.trim());
        }
        return sb.toString();
    }
    
    public List<String> getAddressLines() {
        return addressLines;
    }
    
    public String getAddressLine(int index) {
        if (index < 0 || index >= 3) {
            throw new IndexOutOfBoundsException("Address line index must be 0, 1, or 2");
        }
        return addressLines.get(index);
    }
    
    public String getStateCode() {
        return stateCode;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }
    
    public String getPhoneNumber(int index) {
        if (index < 0 || index >= 2) {
            throw new IndexOutOfBoundsException("Phone number index must be 0 or 1");
        }
        return phoneNumbers.get(index);
    }
    
    public String getPrimaryPhoneNumber() {
        return phoneNumbers.get(0);
    }
    
    public String getSecondaryPhoneNumber() {
        return phoneNumbers.get(1);
    }
    
    public String getSsn() {
        return ssn;
    }
    
    public String getMaskedSsn() {
        if (ssn == null || ssn.length() < 4) {
            return "***-**-****";
        }
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }
    
    public String getGovernmentIssuedId() {
        return governmentIssuedId;
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public String getFormattedDateOfBirth() {
        return dateOfBirth != null ? DATE_FORMATTER.format(dateOfBirth) : "";
    }
    
    public String getEftAccountId() {
        return eftAccountId;
    }
    
    public String getPrimaryCardHolderIndicator() {
        return primaryCardHolderIndicator;
    }
    
    public boolean isPrimaryCardHolder() {
        return "Y".equalsIgnoreCase(primaryCardHolderIndicator);
    }
    
    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerExportData that = (CustomerExportData) o;
        return Objects.equals(customerId, that.customerId) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(middleName, that.middleName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(addressLines, that.addressLines) &&
                Objects.equals(stateCode, that.stateCode) &&
                Objects.equals(countryCode, that.countryCode) &&
                Objects.equals(zipCode, that.zipCode) &&
                Objects.equals(phoneNumbers, that.phoneNumbers) &&
                Objects.equals(ssn, that.ssn) &&
                Objects.equals(governmentIssuedId, that.governmentIssuedId) &&
                Objects.equals(dateOfBirth, that.dateOfBirth) &&
                Objects.equals(eftAccountId, that.eftAccountId) &&
                Objects.equals(primaryCardHolderIndicator, that.primaryCardHolderIndicator) &&
                Objects.equals(ficoCreditScore, that.ficoCreditScore);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId, firstName, middleName, lastName, addressLines,
                stateCode, countryCode, zipCode, phoneNumbers, ssn, governmentIssuedId,
                dateOfBirth, eftAccountId, primaryCardHolderIndicator, ficoCreditScore);
    }
    
    @Override
    public String toString() {
        return "CustomerExportData{" +
                "customerId=" + customerId +
                ", name='" + getFullName() + '\'' +
                ", stateCode='" + stateCode + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", ssn='" + getMaskedSsn() + '\'' +
                ", dateOfBirth=" + getFormattedDateOfBirth() +
                ", primaryCardHolder=" + isPrimaryCardHolder() +
                ", ficoCreditScore=" + ficoCreditScore +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(CustomerExportData source) {
        return new Builder()
                .customerId(source.customerId)
                .firstName(source.firstName)
                .middleName(source.middleName)
                .lastName(source.lastName)
                .addressLines(new ArrayList<>(source.addressLines))
                .stateCode(source.stateCode)
                .countryCode(source.countryCode)
                .zipCode(source.zipCode)
                .phoneNumbers(new ArrayList<>(source.phoneNumbers))
                .ssn(source.ssn)
                .governmentIssuedId(source.governmentIssuedId)
                .dateOfBirth(source.dateOfBirth)
                .eftAccountId(source.eftAccountId)
                .primaryCardHolderIndicator(source.primaryCardHolderIndicator)
                .ficoCreditScore(source.ficoCreditScore);
    }
    
    public static final class Builder {
        private Long customerId;
        private String firstName;
        private String middleName;
        private String lastName;
        private List<String> addressLines = new ArrayList<>();
        private String stateCode;
        private String countryCode;
        private String zipCode;
        private List<String> phoneNumbers = new ArrayList<>();
        private String ssn;
        private String governmentIssuedId;
        private LocalDate dateOfBirth;
        private String eftAccountId;
        private String primaryCardHolderIndicator;
        private Integer ficoCreditScore;
        
        private Builder() {}
        
        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }
        
        public Builder middleName(String middleName) {
            this.middleName = middleName;
            return this;
        }
        
        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        
        public Builder addressLines(List<String> addressLines) {
            this.addressLines = addressLines != null ? new ArrayList<>(addressLines) : new ArrayList<>();
            return this;
        }
        
        public Builder addressLine(int index, String line) {
            while (addressLines.size() <= index) {
                addressLines.add("");
            }
            addressLines.set(index, line);
            return this;
        }
        
        public Builder stateCode(String stateCode) {
            this.stateCode = stateCode;
            return this;
        }
        
        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }
        
        public Builder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }
        
        public Builder phoneNumbers(List<String> phoneNumbers) {
            this.phoneNumbers = phoneNumbers != null ? new ArrayList<>(phoneNumbers) : new ArrayList<>();
            return this;
        }
        
        public Builder phoneNumber(int index, String phone) {
            while (phoneNumbers.size() <= index) {
                phoneNumbers.add("");
            }
            phoneNumbers.set(index, phone);
            return this;
        }
        
        public Builder primaryPhoneNumber(String phone) {
            return phoneNumber(0, phone);
        }
        
        public Builder secondaryPhoneNumber(String phone) {
            return phoneNumber(1, phone);
        }
        
        public Builder ssn(String ssn) {
            this.ssn = ssn;
            return this;
        }
        
        public Builder governmentIssuedId(String governmentIssuedId) {
            this.governmentIssuedId = governmentIssuedId;
            return this;
        }
        
        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }
        
        public Builder dateOfBirth(String dateOfBirth) {
            if (dateOfBirth != null && !dateOfBirth.isBlank()) {
                this.dateOfBirth = LocalDate.parse(dateOfBirth, DATE_FORMATTER);
            }
            return this;
        }
        
        public Builder eftAccountId(String eftAccountId) {
            this.eftAccountId = eftAccountId;
            return this;
        }
        
        public Builder primaryCardHolderIndicator(String indicator) {
            this.primaryCardHolderIndicator = indicator;
            return this;
        }
        
        public Builder primaryCardHolder(boolean isPrimary) {
            this.primaryCardHolderIndicator = isPrimary ? "Y" : "N";
            return this;
        }
        
        public Builder ficoCreditScore(Integer ficoCreditScore) {
            this.ficoCreditScore = ficoCreditScore;
            return this;
        }
        
        public CustomerExportData build() {
            return new CustomerExportData(this);
        }
    }
}
