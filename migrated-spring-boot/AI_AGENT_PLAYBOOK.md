# AI Agent Playbook: CardDemo Spring Boot Migration

This playbook provides detailed instructions for an AI coding agent to migrate the CardDemo mainframe application to Spring Boot. The agent should follow these instructions precisely to ensure consistent, high-quality code generation.

## Agent Role and Context

You are an AI coding agent responsible for migrating a mainframe COBOL/CICS/VSAM application called CardDemo to a modern Spring Boot 3.2 application using Java 17. The CardDemo application is a credit card management system that handles accounts, customers, cards, transactions, and user authentication.

### Source Technology Stack (Mainframe)
- COBOL programs for business logic
- CICS for online transaction processing
- VSAM files for data persistence
- BMS maps for terminal screens
- JCL for batch job execution
- Copybooks for data structure definitions

### Target Technology Stack (Spring Boot)
- Spring Boot 3.2.0 with Java 17
- Spring Data JPA for data persistence
- H2 database for development (PostgreSQL for production)
- Spring Web for REST APIs
- Spring Batch for batch processing
- Spring Security for authentication/authorization
- Spring Validation for input validation

### Architecture Mapping
- CICS Transactions → REST Controllers
- VSAM Files → JPA Entities with H2/PostgreSQL
- JCL Jobs → Spring Batch Jobs
- BMS Maps → REST API responses (frontend handled separately)
- Copybooks → Java DTOs and Entities
- USRSEC Security → Spring Security

## Project Structure

All code must be placed in the following directory structure under `src/main/java/com/carddemo/`:

```
com.carddemo
├── CardDemoApplication.java          # Main application class (already exists)
├── config/                           # Configuration classes
│   ├── SecurityConfig.java
│   ├── BatchConfig.java
│   └── WebConfig.java
├── controller/                       # REST controllers
│   ├── AccountController.java
│   ├── CustomerController.java
│   ├── CardController.java
│   ├── TransactionController.java
│   ├── UserController.java
│   └── AuthenticationController.java
├── service/                          # Business logic services
│   ├── AccountService.java
│   ├── CustomerService.java
│   ├── CardService.java
│   ├── TransactionService.java
│   └── UserService.java
├── repository/                       # JPA repositories
│   ├── AccountRepository.java
│   ├── CustomerRepository.java
│   ├── CardRepository.java
│   ├── TransactionRepository.java
│   └── UserRepository.java
├── entity/                           # JPA entities
│   ├── Account.java
│   ├── Customer.java
│   ├── Card.java
│   ├── Transaction.java
│   ├── TransactionCategoryBalance.java
│   └── User.java
├── dto/                              # Data Transfer Objects
│   ├── request/
│   │   ├── AccountCreateRequest.java
│   │   ├── AccountUpdateRequest.java
│   │   └── ...
│   └── response/
│       ├── AccountResponse.java
│       ├── ApiResponse.java
│       └── ...
├── exception/                        # Exception handling
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   └── InvalidRequestException.java
├── batch/                            # Spring Batch components
│   ├── config/
│   ├── job/
│   ├── reader/
│   ├── processor/
│   └── writer/
├── security/                         # Security components
│   └── UserDetailsServiceImpl.java
└── util/                             # Utility classes
    └── DataTypeConverter.java
```

## First Wave Implementation Details

The first wave focuses on establishing the foundational data layer. Complete these tasks in order, as later tasks depend on earlier ones.

### Task 1: Create the Account Entity

**File:** `src/main/java/com/carddemo/entity/Account.java`

**Source Reference:** `app/cpy/CVACT01Y.cpy`

**Implementation:**

```java
package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "account_id", length = 11)
    private Long accountId;

    @NotNull
    @Size(max = 1)
    @Column(name = "active_status", length = 1, nullable = false)
    private String activeStatus;

    @NotNull
    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal currentBalance;

    @NotNull
    @Column(name = "credit_limit", precision = 12, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @NotNull
    @Column(name = "cash_credit_limit", precision = 12, scale = 2, nullable = false)
    private BigDecimal cashCreditLimit;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    @Column(name = "current_cycle_credit", precision = 12, scale = 2)
    private BigDecimal currentCycleCredit;

    @Column(name = "current_cycle_debit", precision = 12, scale = 2)
    private BigDecimal currentCycleDebit;

    @Size(max = 10)
    @Column(name = "address_zip", length = 10)
    private String addressZip;

    @Size(max = 10)
    @Column(name = "group_id", length = 10)
    private String groupId;

    // Default constructor required by JPA
    public Account() {
    }

    // All-args constructor
    public Account(Long accountId, String activeStatus, BigDecimal currentBalance,
                   BigDecimal creditLimit, BigDecimal cashCreditLimit, LocalDate openDate,
                   LocalDate expirationDate, LocalDate reissueDate, BigDecimal currentCycleCredit,
                   BigDecimal currentCycleDebit, String addressZip, String groupId) {
        this.accountId = accountId;
        this.activeStatus = activeStatus;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.openDate = openDate;
        this.expirationDate = expirationDate;
        this.reissueDate = reissueDate;
        this.currentCycleCredit = currentCycleCredit;
        this.currentCycleDebit = currentCycleDebit;
        this.addressZip = addressZip;
        this.groupId = groupId;
    }

    // Getters and setters for all fields
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { this.activeStatus = activeStatus; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public BigDecimal getCashCreditLimit() { return cashCreditLimit; }
    public void setCashCreditLimit(BigDecimal cashCreditLimit) { this.cashCreditLimit = cashCreditLimit; }

    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public LocalDate getReissueDate() { return reissueDate; }
    public void setReissueDate(LocalDate reissueDate) { this.reissueDate = reissueDate; }

    public BigDecimal getCurrentCycleCredit() { return currentCycleCredit; }
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) { this.currentCycleCredit = currentCycleCredit; }

    public BigDecimal getCurrentCycleDebit() { return currentCycleDebit; }
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) { this.currentCycleDebit = currentCycleDebit; }

    public String getAddressZip() { return addressZip; }
    public void setAddressZip(String addressZip) { this.addressZip = addressZip; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
}
```

**Field Mapping from CVACT01Y.cpy:**
| COBOL Field | Java Field | Type Conversion |
|-------------|------------|-----------------|
| ACCT-ID PIC 9(11) | accountId | Long |
| ACCT-ACTIVE-STATUS PIC X(01) | activeStatus | String |
| ACCT-CURR-BAL PIC S9(10)V99 | currentBalance | BigDecimal |
| ACCT-CREDIT-LIMIT PIC S9(10)V99 | creditLimit | BigDecimal |
| ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 | cashCreditLimit | BigDecimal |
| ACCT-OPEN-DATE PIC X(10) | openDate | LocalDate |
| ACCT-EXPIRAION-DATE PIC X(10) | expirationDate | LocalDate |
| ACCT-REISSUE-DATE PIC X(10) | reissueDate | LocalDate |
| ACCT-CURR-CYC-CREDIT PIC S9(10)V99 | currentCycleCredit | BigDecimal |
| ACCT-CURR-CYC-DEBIT PIC S9(10)V99 | currentCycleDebit | BigDecimal |
| ACCT-ADDR-ZIP PIC X(10) | addressZip | String |
| ACCT-GROUP-ID PIC X(10) | groupId | String |

### Task 2: Create the Customer Entity

**File:** `src/main/java/com/carddemo/entity/Customer.java`

**Source Reference:** `app/cpy/CVCUS01Y.cpy`

**Implementation:**

```java
package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
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

    @Size(max = 1)
    @Column(name = "primary_card_holder_ind", length = 1)
    private String primaryCardHolderIndicator;

    @Min(0)
    @Max(999)
    @Column(name = "fico_credit_score")
    private Integer ficoCreditScore;

    public Customer() {
    }

    // Getters and setters for all fields
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getAddressLine3() { return addressLine3; }
    public void setAddressLine3(String addressLine3) { this.addressLine3 = addressLine3; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getPhoneNumber1() { return phoneNumber1; }
    public void setPhoneNumber1(String phoneNumber1) { this.phoneNumber1 = phoneNumber1; }

    public String getPhoneNumber2() { return phoneNumber2; }
    public void setPhoneNumber2(String phoneNumber2) { this.phoneNumber2 = phoneNumber2; }

    public Long getSsn() { return ssn; }
    public void setSsn(Long ssn) { this.ssn = ssn; }

    public String getGovtIssuedId() { return govtIssuedId; }
    public void setGovtIssuedId(String govtIssuedId) { this.govtIssuedId = govtIssuedId; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEftAccountId() { return eftAccountId; }
    public void setEftAccountId(String eftAccountId) { this.eftAccountId = eftAccountId; }

    public String getPrimaryCardHolderIndicator() { return primaryCardHolderIndicator; }
    public void setPrimaryCardHolderIndicator(String primaryCardHolderIndicator) { this.primaryCardHolderIndicator = primaryCardHolderIndicator; }

    public Integer getFicoCreditScore() { return ficoCreditScore; }
    public void setFicoCreditScore(Integer ficoCreditScore) { this.ficoCreditScore = ficoCreditScore; }
}
```

### Task 3: Create the Card Entity

**File:** `src/main/java/com/carddemo/entity/Card.java`

**Source Reference:** Card data structures from various copybooks

**Implementation:**

```java
package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @NotNull
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Size(max = 1)
    @Column(name = "card_status", length = 1)
    private String cardStatus;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Size(max = 50)
    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    public Card() {
    }

    // Getters and setters
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCardStatus() { return cardStatus; }
    public void setCardStatus(String cardStatus) { this.cardStatus = cardStatus; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public String getEmbossedName() { return embossedName; }
    public void setEmbossedName(String embossedName) { this.embossedName = embossedName; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
```

### Task 4: Create the Transaction Entity

**File:** `src/main/java/com/carddemo/entity/Transaction.java`

**Source Reference:** `app/cpy/CVTRA05Y.cpy`

**Implementation:**

```java
package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "transaction_id", length = 16)
    private String transactionId;

    @NotNull
    @Size(max = 2)
    @Column(name = "type_code", length = 2, nullable = false)
    private String typeCode;

    @Column(name = "category_code")
    private Integer categoryCode;

    @Size(max = 10)
    @Column(name = "source", length = 10)
    private String source;

    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

    @NotNull
    @Column(name = "amount", precision = 11, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Size(max = 50)
    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Size(max = 50)
    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Size(max = 10)
    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @NotNull
    @Size(max = 16)
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    @Column(name = "origination_timestamp")
    private LocalDateTime originationTimestamp;

    @Column(name = "processing_timestamp")
    private LocalDateTime processingTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", insertable = false, updatable = false)
    private Card card;

    public Transaction() {
    }

    // Getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public Integer getCategoryCode() { return categoryCode; }
    public void setCategoryCode(Integer categoryCode) { this.categoryCode = categoryCode; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getMerchantCity() { return merchantCity; }
    public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }

    public String getMerchantZip() { return merchantZip; }
    public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public LocalDateTime getOriginationTimestamp() { return originationTimestamp; }
    public void setOriginationTimestamp(LocalDateTime originationTimestamp) { this.originationTimestamp = originationTimestamp; }

    public LocalDateTime getProcessingTimestamp() { return processingTimestamp; }
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) { this.processingTimestamp = processingTimestamp; }

    public Card getCard() { return card; }
    public void setCard(Card card) { this.card = card; }
}
```

### Task 5: Create the User Entity

**File:** `src/main/java/com/carddemo/entity/User.java`

**Source Reference:** `app/cpy/CSUSR01Y.cpy`

**Implementation:**

```java
package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Size(max = 8)
    @Column(name = "user_id", length = 8)
    private String userId;

    @NotBlank
    @Size(max = 20)
    @Column(name = "first_name", length = 20, nullable = false)
    private String firstName;

    @NotBlank
    @Size(max = 20)
    @Column(name = "last_name", length = 20, nullable = false)
    private String lastName;

    @NotBlank
    @Size(max = 60)
    @Column(name = "password", length = 60, nullable = false)
    private String password;

    @NotNull
    @Size(max = 1)
    @Column(name = "user_type", length = 1, nullable = false)
    private String userType;

    public User() {
    }

    public User(String userId, String firstName, String lastName, String password, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.userType = userType;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public boolean isAdmin() {
        return "A".equals(this.userType);
    }
}
```

**User Type Values:**
- `A` = Admin user (has access to all functions)
- `U` = Regular user (limited access)

### Task 6: Create the TransactionCategoryBalance Entity

**File:** `src/main/java/com/carddemo/entity/TransactionCategoryBalance.java`

**Source Reference:** `app/cpy/CVTRA01Y.cpy`

**Implementation:**

```java
package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_category_balances")
@IdClass(TransactionCategoryBalanceId.class)
public class TransactionCategoryBalance {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Id
    @Size(max = 2)
    @Column(name = "type_code", length = 2)
    private String typeCode;

    @Id
    @Column(name = "category_code")
    private Integer categoryCode;

    @NotNull
    @Column(name = "balance", precision = 11, scale = 2, nullable = false)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    public TransactionCategoryBalance() {
    }

    // Getters and setters
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public Integer getCategoryCode() { return categoryCode; }
    public void setCategoryCode(Integer categoryCode) { this.categoryCode = categoryCode; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}
```

**Composite Key Class:**

**File:** `src/main/java/com/carddemo/entity/TransactionCategoryBalanceId.java`

```java
package com.carddemo.entity;

import java.io.Serializable;
import java.util.Objects;

public class TransactionCategoryBalanceId implements Serializable {

    private Long accountId;
    private String typeCode;
    private Integer categoryCode;

    public TransactionCategoryBalanceId() {
    }

    public TransactionCategoryBalanceId(Long accountId, String typeCode, Integer categoryCode) {
        this.accountId = accountId;
        this.typeCode = typeCode;
        this.categoryCode = categoryCode;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public Integer getCategoryCode() { return categoryCode; }
    public void setCategoryCode(Integer categoryCode) { this.categoryCode = categoryCode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionCategoryBalanceId that = (TransactionCategoryBalanceId) o;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(typeCode, that.typeCode) &&
               Objects.equals(categoryCode, that.categoryCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, typeCode, categoryCode);
    }
}
```

### Task 7: Create JPA Repositories

Create the following repository interfaces:

**File:** `src/main/java/com/carddemo/repository/AccountRepository.java`

```java
package com.carddemo.repository;

import com.carddemo.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByActiveStatus(String activeStatus);

    List<Account> findByGroupId(String groupId);

    @Query("SELECT a FROM Account a WHERE a.currentBalance > :minBalance")
    List<Account> findAccountsWithBalanceGreaterThan(@Param("minBalance") java.math.BigDecimal minBalance);

    @Query("SELECT a FROM Account a WHERE a.expirationDate < :date")
    List<Account> findExpiredAccounts(@Param("date") java.time.LocalDate date);
}
```

**File:** `src/main/java/com/carddemo/repository/CustomerRepository.java`

```java
package com.carddemo.repository;

import com.carddemo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByLastNameIgnoreCase(String lastName);

    List<Customer> findByStateCode(String stateCode);

    Optional<Customer> findBySsn(Long ssn);

    List<Customer> findByZipCode(String zipCode);
}
```

**File:** `src/main/java/com/carddemo/repository/CardRepository.java`

```java
package com.carddemo.repository;

import com.carddemo.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByAccountId(Long accountId);

    List<Card> findByCustomerId(Long customerId);

    List<Card> findByCardStatus(String cardStatus);
}
```

**File:** `src/main/java/com/carddemo/repository/TransactionRepository.java`

```java
package com.carddemo.repository;

import com.carddemo.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByCardNumber(String cardNumber);

    Page<Transaction> findByCardNumber(String cardNumber, Pageable pageable);

    List<Transaction> findByTypeCode(String typeCode);

    @Query("SELECT t FROM Transaction t WHERE t.originationTimestamp BETWEEN :start AND :end")
    List<Transaction> findTransactionsBetweenDates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Transaction t WHERE t.cardNumber = :cardNumber ORDER BY t.originationTimestamp DESC")
    List<Transaction> findRecentTransactionsByCard(@Param("cardNumber") String cardNumber, Pageable pageable);
}
```

**File:** `src/main/java/com/carddemo/repository/UserRepository.java`

```java
package com.carddemo.repository;

import com.carddemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);

    List<User> findByUserType(String userType);

    boolean existsByUserId(String userId);
}
```

**File:** `src/main/java/com/carddemo/repository/TransactionCategoryBalanceRepository.java`

```java
package com.carddemo.repository;

import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCategoryBalanceRepository 
        extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {

    List<TransactionCategoryBalance> findByAccountId(Long accountId);

    List<TransactionCategoryBalance> findByAccountIdAndTypeCode(Long accountId, String typeCode);
}
```

### Task 8: Create Exception Classes

**File:** `src/main/java/com/carddemo/exception/ResourceNotFoundException.java`

```java
package com.carddemo.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}
```

**File:** `src/main/java/com/carddemo/exception/DuplicateResourceException.java`

```java
package com.carddemo.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}
```

**File:** `src/main/java/com/carddemo/exception/InvalidRequestException.java`

```java
package com.carddemo.exception;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**File:** `src/main/java/com/carddemo/exception/GlobalExceptionHandler.java`

```java
package com.carddemo.exception;

import com.carddemo.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "DUPLICATE_RESOURCE",
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(
            InvalidRequestException ex, WebRequest request) {
        log.warn("Invalid request: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "INVALID_REQUEST",
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        ErrorResponse error = new ErrorResponse(
                "VALIDATION_FAILED",
                "Validation failed for one or more fields",
                LocalDateTime.now(),
                request.getDescription(false),
                errors
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### Task 9: Create Base DTOs

**File:** `src/main/java/com/carddemo/dto/response/ErrorResponse.java`

```java
package com.carddemo.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {

    private String error;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, String> validationErrors;

    public ErrorResponse(String error, String message, LocalDateTime timestamp, String path) {
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
    }

    public ErrorResponse(String error, String message, LocalDateTime timestamp, String path, 
                         Map<String, String> validationErrors) {
        this(error, message, timestamp, path);
        this.validationErrors = validationErrors;
    }

    // Getters and setters
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Map<String, String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; }
}
```

**File:** `src/main/java/com/carddemo/dto/response/ApiResponse.java`

```java
package com.carddemo.dto.response;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private T data;
    private String message;
    private LocalDateTime timestamp;

    public ApiResponse(T data, String message) {
        this.data = data;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "Success");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message);
    }

    // Getters and setters
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
```

## Task Execution Guidelines

### Commit Structure

Each commit should represent one logical unit of work. Follow this pattern:

1. **Entity commits:** One commit per entity class
   - Example: "Add Account entity based on CVACT01Y copybook"

2. **Repository commits:** Group related repositories
   - Example: "Add JPA repositories for core entities"

3. **Exception commits:** Group exception classes together
   - Example: "Add exception classes and global exception handler"

4. **Service commits:** One commit per service class
   - Example: "Add AccountService with CRUD operations"

5. **Controller commits:** One commit per controller
   - Example: "Add AccountController with REST endpoints"

### Data Type Conversion Rules

When converting COBOL data types to Java:

| COBOL Picture | Java Type | Conversion Notes |
|---------------|-----------|------------------|
| PIC 9(n) where n <= 9 | Integer | Use for small numeric values |
| PIC 9(n) where n > 9 | Long | Use for large numeric values and IDs |
| PIC S9(n)V99 | BigDecimal | Always use for monetary values |
| PIC X(n) | String | Trim trailing spaces |
| PIC X(10) date format | LocalDate | Parse using DateTimeFormatter |
| PIC X(26) timestamp | LocalDateTime | Parse using DateTimeFormatter |

### Transaction Boundary Implementation

Apply `@Transactional` at the service layer:

```java
@Service
@Transactional
public class AccountService {

    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Account updateBalance(Long id, BigDecimal amount) {
        Account account = findById(id);
        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        return accountRepository.save(account);
    }
}
```

### Testing Expectations

For each component, create corresponding tests:

1. **Entity tests:** Verify JPA mappings work correctly
2. **Repository tests:** Use `@DataJpaTest` for repository testing
3. **Service tests:** Use `@MockBean` for dependencies
4. **Controller tests:** Use `@WebMvcTest` for controller testing
5. **Integration tests:** Use `@SpringBootTest` for full integration

Example test structure:

```java
@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldSaveAndRetrieveAccount() {
        Account account = new Account();
        account.setAccountId(12345678901L);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCashCreditLimit(new BigDecimal("1000.00"));

        Account saved = accountRepository.save(account);
        
        assertThat(saved.getAccountId()).isEqualTo(12345678901L);
        assertThat(saved.getActiveStatus()).isEqualTo("Y");
    }
}
```

## Subsequent Phases Overview

### Phase 2: Core Services

After completing the first wave (entities, repositories, exceptions, base DTOs), implement service classes:

1. **AccountService** - Implement account CRUD operations, balance updates, status changes
2. **CustomerService** - Implement customer management, search functionality
3. **CardService** - Implement card operations, linking to accounts and customers
4. **TransactionService** - Implement transaction processing, history retrieval
5. **UserService** - Implement user management (preparation for security)

Each service should:
- Use constructor injection for dependencies
- Apply `@Transactional` appropriately
- Throw custom exceptions for error conditions
- Log significant operations

### Phase 3: REST Controllers

Implement REST controllers following these patterns:

1. Use `@RestController` and `@RequestMapping("/api/v1/resource")`
2. Inject services via constructor
3. Use `@Valid` for request validation
4. Return `ResponseEntity` with appropriate status codes
5. Document endpoints with OpenAPI annotations

### Phase 4: Security

Implement Spring Security:

1. Configure `SecurityFilterChain` bean
2. Implement `UserDetailsService` using `UserRepository`
3. Configure password encoding (BCrypt)
4. Define role-based access rules
5. Optionally add JWT token support

### Phase 5: Batch Processing

Implement Spring Batch jobs:

1. Configure `BatchConfig` with job repository
2. Create job configurations for each batch process
3. Implement `ItemReader`, `ItemProcessor`, `ItemWriter` for each job
4. Configure chunk size and commit intervals
5. Add job scheduling with `@Scheduled` or external scheduler

## Reference: Mainframe Source Files

The following mainframe source files should be referenced during migration:

### Copybooks (Data Structures)
- `app/cpy/CVACT01Y.cpy` - Account record structure
- `app/cpy/CVCUS01Y.cpy` - Customer record structure
- `app/cpy/CVTRA05Y.cpy` - Transaction record structure
- `app/cpy/CVTRA01Y.cpy` - Transaction category balance structure
- `app/cpy/CSUSR01Y.cpy` - User security record structure

### CICS Programs (Online Processing)
- `app/cbl/COSGN00C.cbl` - Sign-on processing
- `app/cbl/COMEN01C.cbl` - Main menu
- `app/cbl/COACTUPC.cbl` - Account update
- `app/cbl/COACTVWC.cbl` - Account view
- `app/cbl/COCRDLIC.cbl` - Card list
- `app/cbl/COTRN00C.cbl` - Transaction list
- `app/cbl/COUSR00C.cbl` - User management

### Batch Programs
- `app/cbl/CBACT01C.cbl` - Account file processing
- `app/cbl/CBCUS01C.cbl` - Customer file processing
- `app/cbl/CBTRN01C.cbl` - Transaction file processing
- `app/cbl/CBEXPORT.cbl` - Data export
- `app/cbl/CBIMPORT.cbl` - Data import

## Checklist for First Wave Completion

Before moving to Phase 2, verify:

- [ ] All 6 entity classes are created and compile without errors
- [ ] Composite key class for TransactionCategoryBalance is created
- [ ] All 6 repository interfaces are created
- [ ] All 3 exception classes are created
- [ ] GlobalExceptionHandler is created
- [ ] ErrorResponse and ApiResponse DTOs are created
- [ ] Application starts successfully and creates database schema
- [ ] Basic repository operations work (verified via tests or H2 console)
- [ ] All code follows the naming conventions specified in this playbook
- [ ] Each logical unit has been committed separately with descriptive messages
