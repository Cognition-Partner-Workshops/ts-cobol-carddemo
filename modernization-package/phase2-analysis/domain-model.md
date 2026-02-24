# Business Domain Model: CardDemo Transaction Processing Module

> **Module:** Transaction Processing (CardDemo Modernization)
> **Phase:** 2 - Analysis
> **Source Copybooks:** CVTRA05Y.cpy, CVACT01Y.cpy, CVACT02Y.cpy, CVACT03Y.cpy, CVCUS01Y.cpy

---

## 1. Entity Relationship Diagram

```mermaid
erDiagram
    CUSTOMER ||--o{ CARD_CROSS_REFERENCE : "has cards via"
    ACCOUNT ||--o{ CARD_CROSS_REFERENCE : "linked through"
    CARD_CROSS_REFERENCE ||--|| CARD : "maps to"
    ACCOUNT ||--o{ CARD : "owns"
    CARD ||--o{ TRANSACTION : "charges to"

    CUSTOMER {
        NUMERIC_9_0 customer_id PK "CUST-ID 9(09)"
        VARCHAR_25 first_name "CUST-FIRST-NAME X(25)"
        VARCHAR_25 middle_name "CUST-MIDDLE-NAME X(25)"
        VARCHAR_25 last_name "CUST-LAST-NAME X(25)"
        VARCHAR_50 address_line_1 "CUST-ADDR-LINE-1 X(50)"
        VARCHAR_50 address_line_2 "CUST-ADDR-LINE-2 X(50)"
        VARCHAR_50 address_line_3 "CUST-ADDR-LINE-3 X(50)"
        VARCHAR_2 state_code "CUST-ADDR-STATE-CD X(02)"
        VARCHAR_3 country_code "CUST-ADDR-COUNTRY-CD X(03)"
        VARCHAR_10 address_zip "CUST-ADDR-ZIP X(10)"
        VARCHAR_15 phone_number_1 "CUST-PHONE-NUM-1 X(15)"
        VARCHAR_15 phone_number_2 "CUST-PHONE-NUM-2 X(15)"
        NUMERIC_9_0 ssn "CUST-SSN 9(09)"
        VARCHAR_20 government_issued_id "CUST-GOVT-ISSUED-ID X(20)"
        DATE date_of_birth "CUST-DOB-YYYY-MM-DD X(10)"
        VARCHAR_10 eft_account_id "CUST-EFT-ACCOUNT-ID X(10)"
        VARCHAR_1 primary_card_holder_ind "CUST-PRI-CARD-HOLDER-IND X(01)"
        NUMERIC_3_0 fico_credit_score "CUST-FICO-CREDIT-SCORE 9(03)"
    }

    ACCOUNT {
        NUMERIC_11_0 account_id PK "ACCT-ID 9(11)"
        VARCHAR_1 active_status "ACCT-ACTIVE-STATUS X(01)"
        NUMERIC_12_2 current_balance "ACCT-CURR-BAL S9(10)V99"
        NUMERIC_12_2 credit_limit "ACCT-CREDIT-LIMIT S9(10)V99"
        NUMERIC_12_2 cash_credit_limit "ACCT-CASH-CREDIT-LIMIT S9(10)V99"
        DATE open_date "ACCT-OPEN-DATE X(10)"
        DATE expiration_date "ACCT-EXPIRAION-DATE X(10)"
        DATE reissue_date "ACCT-REISSUE-DATE X(10)"
        NUMERIC_12_2 current_cycle_credit "ACCT-CURR-CYC-CREDIT S9(10)V99"
        NUMERIC_12_2 current_cycle_debit "ACCT-CURR-CYC-DEBIT S9(10)V99"
        VARCHAR_10 address_zip "ACCT-ADDR-ZIP X(10)"
        VARCHAR_10 group_id "ACCT-GROUP-ID X(10)"
    }

    CARD {
        VARCHAR_16 card_number PK "CARD-NUM X(16)"
        NUMERIC_11_0 account_id FK "CARD-ACCT-ID 9(11)"
        NUMERIC_3_0 cvv_code "CARD-CVV-CD 9(03)"
        VARCHAR_50 embossed_name "CARD-EMBOSSED-NAME X(50)"
        DATE expiration_date "CARD-EXPIRAION-DATE X(10)"
        VARCHAR_1 active_status "CARD-ACTIVE-STATUS X(01)"
    }

    CARD_CROSS_REFERENCE {
        VARCHAR_16 card_number PK "XREF-CARD-NUM X(16)"
        NUMERIC_9_0 customer_id FK "XREF-CUST-ID 9(09)"
        NUMERIC_11_0 account_id FK "XREF-ACCT-ID 9(11)"
    }

    TRANSACTION {
        VARCHAR_16 transaction_id PK "TRAN-ID X(16)"
        VARCHAR_2 type_code "TRAN-TYPE-CD X(02)"
        NUMERIC_4_0 category_code "TRAN-CAT-CD 9(04)"
        VARCHAR_10 source "TRAN-SOURCE X(10)"
        VARCHAR_100 description "TRAN-DESC X(100)"
        NUMERIC_11_2 amount "TRAN-AMT S9(09)V99"
        NUMERIC_9_0 merchant_id "TRAN-MERCHANT-ID 9(09)"
        VARCHAR_50 merchant_name "TRAN-MERCHANT-NAME X(50)"
        VARCHAR_50 merchant_city "TRAN-MERCHANT-CITY X(50)"
        VARCHAR_10 merchant_zip "TRAN-MERCHANT-ZIP X(10)"
        VARCHAR_16 card_number FK "TRAN-CARD-NUM X(16)"
        TIMESTAMP origination_ts "TRAN-ORIG-TS X(26)"
        TIMESTAMP processing_ts "TRAN-PROC-TS X(26)"
    }
```

---

## 2. Java Class Diagram

```mermaid
classDiagram
    class Transaction {
        -String transactionId
        -String typeCode
        -Integer categoryCode
        -String source
        -String description
        -BigDecimal amount
        -Long merchantId
        -String merchantName
        -String merchantCity
        -String merchantZip
        -String cardNumber
        -LocalDateTime originationTimestamp
        -LocalDateTime processingTimestamp
        +getTransactionId() String
        +setTransactionId(String) void
        +getTypeCode() String
        +setTypeCode(String) void
        +getCategoryCode() Integer
        +setCategoryCode(Integer) void
        +getSource() String
        +setSource(String) void
        +getDescription() String
        +setDescription(String) void
        +getAmount() BigDecimal
        +setAmount(BigDecimal) void
        +getMerchantId() Long
        +setMerchantId(Long) void
        +getMerchantName() String
        +setMerchantName(String) void
        +getMerchantCity() String
        +setMerchantCity(String) void
        +getMerchantZip() String
        +setMerchantZip(String) void
        +getCardNumber() String
        +setCardNumber(String) void
        +getOriginationTimestamp() LocalDateTime
        +setOriginationTimestamp(LocalDateTime) void
        +getProcessingTimestamp() LocalDateTime
        +setProcessingTimestamp(LocalDateTime) void
    }

    class Account {
        -Long accountId
        -String activeStatus
        -BigDecimal currentBalance
        -BigDecimal creditLimit
        -BigDecimal cashCreditLimit
        -LocalDate openDate
        -LocalDate expirationDate
        -LocalDate reissueDate
        -BigDecimal currentCycleCredit
        -BigDecimal currentCycleDebit
        -String addressZip
        -String groupId
        +getAccountId() Long
        +getActiveStatus() String
        +getCurrentBalance() BigDecimal
        +getCreditLimit() BigDecimal
        +getCashCreditLimit() BigDecimal
        +getOpenDate() LocalDate
        +getExpirationDate() LocalDate
        +getReissueDate() LocalDate
        +getCurrentCycleCredit() BigDecimal
        +getCurrentCycleDebit() BigDecimal
        +getAddressZip() String
        +getGroupId() String
    }

    class Card {
        -String cardNumber
        -Long accountId
        -Integer cvvCode
        -String embossedName
        -LocalDate expirationDate
        -String activeStatus
        +getCardNumber() String
        +getAccountId() Long
        +getCvvCode() Integer
        +getEmbossedName() String
        +getExpirationDate() LocalDate
        +getActiveStatus() String
    }

    class CardCrossReference {
        -String cardNumber
        -Long customerId
        -Long accountId
        +getCardNumber() String
        +getCustomerId() Long
        +getAccountId() Long
    }

    class Customer {
        -Long customerId
        -String firstName
        -String middleName
        -String lastName
        -String addressLine1
        -String addressLine2
        -String addressLine3
        -String stateCode
        -String countryCode
        -String addressZip
        -String phoneNumber1
        -String phoneNumber2
        -Long ssn
        -String governmentIssuedId
        -LocalDate dateOfBirth
        -String eftAccountId
        -String primaryCardHolderIndicator
        -Integer ficoCreditScore
        +getCustomerId() Long
        +getFirstName() String
        +getLastName() String
        +getSsn() Long
        +getDateOfBirth() LocalDate
        +getFicoCreditScore() Integer
    }

    Customer "1" --> "*" CardCrossReference : has
    Account "1" --> "*" CardCrossReference : linked
    CardCrossReference "1" --> "1" Card : maps to
    Account "1" --> "*" Card : owns
    Card "1" --> "*" Transaction : charged on
```

---

## 3. Service Layer Architecture

```mermaid
classDiagram
    class TransactionService {
        -TransactionRepository transactionRepo
        -CardCrossReferenceRepository xrefRepo
        -TransactionValidationService validationService
        +listTransactions(pageable, filterTransactionId) Page~TransactionSummaryDTO~
        +viewTransaction(transactionId) TransactionDetailDTO
        +addTransaction(AddTransactionRequest) TransactionDetailDTO
        -generateNextTransactionId() String
        -resolveCardFromAccount(accountId) String
        -resolveAccountFromCard(cardNumber) Long
    }

    class TransactionValidationService {
        +validateKeyFields(request) void
        +validateMandatoryFields(request) void
        +validateNumericTypes(request) void
        +validateAmountFormat(amount) void
        +validateDates(origDate, procDate) void
        +validateMerchantId(merchantId) void
    }

    class TransactionRepository {
        <<interface>>
        +findById(transactionId) Optional~Transaction~
        +findAll(pageable) Page~Transaction~
        +findMaxTransactionId() Optional~String~
        +save(transaction) Transaction
    }

    class CardCrossReferenceRepository {
        <<interface>>
        +findByCardNumber(cardNumber) Optional~CardCrossReference~
        +findByAccountId(accountId) Optional~CardCrossReference~
    }

    TransactionService --> TransactionRepository : uses
    TransactionService --> CardCrossReferenceRepository : uses
    TransactionService --> TransactionValidationService : delegates validation
```

---

## 4. Transaction Record Field Inventory (Quality Gate)

Every field from the 350-byte `CVTRA05Y.cpy` record is accounted for below:

| # | COBOL Field | PIC Clause | Bytes | Mapped to Java | Mapped to PostgreSQL | Status |
|---|---|---|---|---|---|---|
| 1 | `TRAN-ID` | `X(16)` | 16 | `String transactionId` | `VARCHAR(16) PK` | Mapped |
| 2 | `TRAN-TYPE-CD` | `X(02)` | 2 | `String typeCode` | `VARCHAR(2)` | Mapped |
| 3 | `TRAN-CAT-CD` | `9(04)` | 4 | `Integer categoryCode` | `NUMERIC(4,0)` | Mapped |
| 4 | `TRAN-SOURCE` | `X(10)` | 10 | `String source` | `VARCHAR(10)` | Mapped |
| 5 | `TRAN-DESC` | `X(100)` | 100 | `String description` | `VARCHAR(100)` | Mapped |
| 6 | `TRAN-AMT` | `S9(09)V99` | 11 | `BigDecimal amount` | `NUMERIC(11,2)` | Mapped |
| 7 | `TRAN-MERCHANT-ID` | `9(09)` | 9 | `Long merchantId` | `NUMERIC(9,0)` | Mapped |
| 8 | `TRAN-MERCHANT-NAME` | `X(50)` | 50 | `String merchantName` | `VARCHAR(50)` | Mapped |
| 9 | `TRAN-MERCHANT-CITY` | `X(50)` | 50 | `String merchantCity` | `VARCHAR(50)` | Mapped |
| 10 | `TRAN-MERCHANT-ZIP` | `X(10)` | 10 | `String merchantZip` | `VARCHAR(10)` | Mapped |
| 11 | `TRAN-CARD-NUM` | `X(16)` | 16 | `String cardNumber` | `VARCHAR(16) FK` | Mapped |
| 12 | `TRAN-ORIG-TS` | `X(26)` | 26 | `LocalDateTime originationTimestamp` | `TIMESTAMP` | Mapped |
| 13 | `TRAN-PROC-TS` | `X(26)` | 26 | `LocalDateTime processingTimestamp` | `TIMESTAMP` | Mapped |
| 14 | `FILLER` | `X(20)` | 20 | *Not mapped (padding)* | *Not mapped* | Excluded (expected) |
| | | **Total** | **350** | **13 fields + FILLER** | **13 columns** | **Complete** |

---

## 5. Aggregate Boundaries (DDD)

```mermaid
graph TB
    subgraph "Transaction Aggregate"
        T[Transaction Entity<br/>Root]
    end

    subgraph "Account Aggregate"
        A[Account Entity<br/>Root]
        C[Card Entity]
        A --> C
    end

    subgraph "Customer Aggregate"
        CU[Customer Entity<br/>Root]
    end

    subgraph "Cross-Reference Aggregate"
        XR[CardCrossReference Entity<br/>Root]
    end

    T -.->|card_number FK| C
    XR -.->|card_number FK| C
    XR -.->|account_id FK| A
    XR -.->|customer_id FK| CU

    style T fill:#e1f5fe
    style A fill:#e8f5e9
    style CU fill:#fff3e0
    style XR fill:#fce4ec
```

### Aggregate Design Rationale

| Aggregate | Root Entity | Bounded Context | Rationale |
|---|---|---|---|
| **Transaction** | `Transaction` | Transaction Processing | Transactions are created independently and reference cards by ID only. No cascading updates needed. |
| **Account** | `Account` | Account Management | Account owns cards. Card lifecycle is managed through the account. |
| **Customer** | `Customer` | Customer Management | Customer data is maintained independently; linked to accounts via cross-reference. |
| **Cross-Reference** | `CardCrossReference` | Lookup/Resolution | Bridge entity for resolving Account <-> Card <-> Customer relationships. Read-heavy, write-rare. |
