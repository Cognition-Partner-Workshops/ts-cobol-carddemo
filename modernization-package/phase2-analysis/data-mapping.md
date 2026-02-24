# Data Mapping: COBOL PIC Clauses to Java Types and PostgreSQL Column Types

> **Module:** Transaction Processing (CardDemo Modernization)
> **Phase:** 2 - Analysis
> **Source Copybooks:** CVTRA05Y.cpy, CVACT01Y.cpy, CVACT02Y.cpy, CVACT03Y.cpy, CVCUS01Y.cpy

---

## 1. PIC Clause Mapping Reference

This section defines the general rules for translating COBOL PIC clauses to their Java and PostgreSQL equivalents.

| COBOL PIC Pattern | Description | Java Type | PostgreSQL Type | Notes |
|---|---|---|---|---|
| `X(n)` | Alphanumeric, n characters | `String` | `VARCHAR(n)` | Direct character-for-character mapping |
| `9(n)` | Unsigned numeric, n digits | `Long` / `Integer` | `NUMERIC(n,0)` or `BIGINT` | Use `Long` if n > 9, `Integer` if n <= 9 |
| `S9(n)V99` | Signed numeric with 2 implied decimal places | `BigDecimal` | `NUMERIC(n+2, 2)` | The `V` implies a decimal point not stored in the data; `S` indicates sign |
| `S9(n) COMP` | Signed binary integer | `int` / `long` | `INTEGER` / `BIGINT` | COMP = binary representation; used for internal counters |
| `PIC X(n)` (timestamp) | Alphanumeric used as timestamp | `LocalDateTime` | `TIMESTAMP` | Application-level convention, not a COBOL type |
| `PIC X(10)` (date) | Alphanumeric used as date (YYYY-MM-DD) | `LocalDate` | `DATE` | Application-level convention |
| `FILLER PIC X(n)` | Reserved/padding bytes | *Not mapped* | *Not mapped* | FILLER fields are padding for fixed-length records; no equivalent needed in relational model |

---

## 2. Transaction Record (CVTRA05Y.cpy) - 350 Bytes

**COBOL Record:** `TRAN-RECORD` (01 level)
**Java Entity:** `Transaction`
**PostgreSQL Table:** `transaction`

| # | COBOL Field | PIC Clause | Bytes | Offset | Java Field | Java Type | PostgreSQL Column | PostgreSQL Type | Constraints |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `TRAN-ID` | `X(16)` | 16 | 0-15 | `transactionId` | `String` | `transaction_id` | `VARCHAR(16)` | `PRIMARY KEY` |
| 2 | `TRAN-TYPE-CD` | `X(02)` | 2 | 16-17 | `typeCode` | `String` | `type_code` | `VARCHAR(2)` | `NOT NULL` |
| 3 | `TRAN-CAT-CD` | `9(04)` | 4 | 18-21 | `categoryCode` | `Integer` | `category_code` | `NUMERIC(4,0)` | `NOT NULL` |
| 4 | `TRAN-SOURCE` | `X(10)` | 10 | 22-31 | `source` | `String` | `source` | `VARCHAR(10)` | `NOT NULL` |
| 5 | `TRAN-DESC` | `X(100)` | 100 | 32-131 | `description` | `String` | `description` | `VARCHAR(100)` | `NOT NULL` |
| 6 | `TRAN-AMT` | `S9(09)V99` | 11 | 132-142 | `amount` | `BigDecimal` | `amount` | `NUMERIC(11,2)` | `NOT NULL` |
| 7 | `TRAN-MERCHANT-ID` | `9(09)` | 9 | 143-151 | `merchantId` | `Long` | `merchant_id` | `NUMERIC(9,0)` | `NOT NULL` |
| 8 | `TRAN-MERCHANT-NAME` | `X(50)` | 50 | 152-201 | `merchantName` | `String` | `merchant_name` | `VARCHAR(50)` | `NOT NULL` |
| 9 | `TRAN-MERCHANT-CITY` | `X(50)` | 50 | 202-251 | `merchantCity` | `String` | `merchant_city` | `VARCHAR(50)` | `NOT NULL` |
| 10 | `TRAN-MERCHANT-ZIP` | `X(10)` | 10 | 252-261 | `merchantZip` | `String` | `merchant_zip` | `VARCHAR(10)` | `NOT NULL` |
| 11 | `TRAN-CARD-NUM` | `X(16)` | 16 | 262-277 | `cardNumber` | `String` | `card_number` | `VARCHAR(16)` | `NOT NULL`, `FK -> card.card_number` |
| 12 | `TRAN-ORIG-TS` | `X(26)` | 26 | 278-303 | `originationTimestamp` | `LocalDateTime` | `origination_ts` | `TIMESTAMP` | `NOT NULL` |
| 13 | `TRAN-PROC-TS` | `X(26)` | 26 | 304-329 | `processingTimestamp` | `LocalDateTime` | `processing_ts` | `TIMESTAMP` | `NOT NULL` |
| 14 | `FILLER` | `X(20)` | 20 | 330-349 | *Not mapped* | *N/A* | *Not mapped* | *N/A* | Padding bytes for 350-byte record alignment |
| | | **Total** | **350** | | | | | | |

### Byte Count Verification

```
TRAN-ID:            16
TRAN-TYPE-CD:        2
TRAN-CAT-CD:         4
TRAN-SOURCE:        10
TRAN-DESC:         100
TRAN-AMT:           11  (S9(09)V99 = 9 digits + sign + implied decimal = 11 display bytes)
TRAN-MERCHANT-ID:    9
TRAN-MERCHANT-NAME: 50
TRAN-MERCHANT-CITY: 50
TRAN-MERCHANT-ZIP:  10
TRAN-CARD-NUM:      16
TRAN-ORIG-TS:       26
TRAN-PROC-TS:       26
FILLER:             20
                   ───
Total:             350 bytes  ✓
```

### TRAN-AMT Detail (S9(09)V99)

- **`S`**: Signed value (positive or negative amounts)
- **`9(09)`**: 9 integer digits (max 999,999,999)
- **`V`**: Implied decimal point (not stored in data, but assumed between positions)
- **`99`**: 2 fractional digits
- **Java**: `BigDecimal` with scale 2 - NEVER use `float` or `double` for financial data
- **PostgreSQL**: `NUMERIC(11,2)` - 9 integer + 2 decimal digits, with sign handled natively
- **Display format**: `+99999999.99` (sign + 8 digits + decimal + 2 digits, as defined in `WS-TRAN-AMT`)

---

## 3. Account Record (CVACT01Y.cpy) - 300 Bytes

**COBOL Record:** `ACCOUNT-RECORD` (01 level)
**Java Entity:** `Account`
**PostgreSQL Table:** `account`

| # | COBOL Field | PIC Clause | Bytes | Java Field | Java Type | PostgreSQL Column | PostgreSQL Type | Constraints |
|---|---|---|---|---|---|---|---|---|
| 1 | `ACCT-ID` | `9(11)` | 11 | `accountId` | `Long` | `account_id` | `NUMERIC(11,0)` | `PRIMARY KEY` |
| 2 | `ACCT-ACTIVE-STATUS` | `X(01)` | 1 | `activeStatus` | `String` | `active_status` | `VARCHAR(1)` | `NOT NULL` |
| 3 | `ACCT-CURR-BAL` | `S9(10)V99` | 12 | `currentBalance` | `BigDecimal` | `current_balance` | `NUMERIC(12,2)` | `NOT NULL` |
| 4 | `ACCT-CREDIT-LIMIT` | `S9(10)V99` | 12 | `creditLimit` | `BigDecimal` | `credit_limit` | `NUMERIC(12,2)` | `NOT NULL` |
| 5 | `ACCT-CASH-CREDIT-LIMIT` | `S9(10)V99` | 12 | `cashCreditLimit` | `BigDecimal` | `cash_credit_limit` | `NUMERIC(12,2)` | `NOT NULL` |
| 6 | `ACCT-OPEN-DATE` | `X(10)` | 10 | `openDate` | `LocalDate` | `open_date` | `DATE` | `NOT NULL` |
| 7 | `ACCT-EXPIRAION-DATE` | `X(10)` | 10 | `expirationDate` | `LocalDate` | `expiration_date` | `DATE` | |
| 8 | `ACCT-REISSUE-DATE` | `X(10)` | 10 | `reissueDate` | `LocalDate` | `reissue_date` | `DATE` | |
| 9 | `ACCT-CURR-CYC-CREDIT` | `S9(10)V99` | 12 | `currentCycleCredit` | `BigDecimal` | `current_cycle_credit` | `NUMERIC(12,2)` | `NOT NULL` |
| 10 | `ACCT-CURR-CYC-DEBIT` | `S9(10)V99` | 12 | `currentCycleDebit` | `BigDecimal` | `current_cycle_debit` | `NUMERIC(12,2)` | `NOT NULL` |
| 11 | `ACCT-ADDR-ZIP` | `X(10)` | 10 | `addressZip` | `String` | `address_zip` | `VARCHAR(10)` | |
| 12 | `ACCT-GROUP-ID` | `X(10)` | 10 | `groupId` | `String` | `group_id` | `VARCHAR(10)` | |
| 13 | `FILLER` | `X(178)` | 178 | *Not mapped* | *N/A* | *Not mapped* | *N/A* | Padding |
| | | **Total** | **300** | | | | | |

---

## 4. Card Record (CVACT02Y.cpy) - 150 Bytes

**COBOL Record:** `CARD-RECORD` (01 level)
**Java Entity:** `Card`
**PostgreSQL Table:** `card`

| # | COBOL Field | PIC Clause | Bytes | Java Field | Java Type | PostgreSQL Column | PostgreSQL Type | Constraints |
|---|---|---|---|---|---|---|---|---|
| 1 | `CARD-NUM` | `X(16)` | 16 | `cardNumber` | `String` | `card_number` | `VARCHAR(16)` | `PRIMARY KEY` |
| 2 | `CARD-ACCT-ID` | `9(11)` | 11 | `accountId` | `Long` | `account_id` | `NUMERIC(11,0)` | `NOT NULL`, `FK -> account.account_id` |
| 3 | `CARD-CVV-CD` | `9(03)` | 3 | `cvvCode` | `Integer` | `cvv_code` | `NUMERIC(3,0)` | `NOT NULL` |
| 4 | `CARD-EMBOSSED-NAME` | `X(50)` | 50 | `embossedName` | `String` | `embossed_name` | `VARCHAR(50)` | `NOT NULL` |
| 5 | `CARD-EXPIRAION-DATE` | `X(10)` | 10 | `expirationDate` | `LocalDate` | `expiration_date` | `DATE` | `NOT NULL` |
| 6 | `CARD-ACTIVE-STATUS` | `X(01)` | 1 | `activeStatus` | `String` | `active_status` | `VARCHAR(1)` | `NOT NULL` |
| 7 | `FILLER` | `X(59)` | 59 | *Not mapped* | *N/A* | *Not mapped* | *N/A* | Padding |
| | | **Total** | **150** | | | | | |

---

## 5. Card Cross-Reference Record (CVACT03Y.cpy) - 50 Bytes

**COBOL Record:** `CARD-XREF-RECORD` (01 level)
**Java Entity:** `CardCrossReference`
**PostgreSQL Table:** `card_cross_reference`

| # | COBOL Field | PIC Clause | Bytes | Java Field | Java Type | PostgreSQL Column | PostgreSQL Type | Constraints |
|---|---|---|---|---|---|---|---|---|
| 1 | `XREF-CARD-NUM` | `X(16)` | 16 | `cardNumber` | `String` | `card_number` | `VARCHAR(16)` | `PRIMARY KEY`, `FK -> card.card_number` |
| 2 | `XREF-CUST-ID` | `9(09)` | 9 | `customerId` | `Long` | `customer_id` | `NUMERIC(9,0)` | `NOT NULL`, `FK -> customer.customer_id` |
| 3 | `XREF-ACCT-ID` | `9(11)` | 11 | `accountId` | `Long` | `account_id` | `NUMERIC(11,0)` | `NOT NULL`, `FK -> account.account_id` |
| 4 | `FILLER` | `X(14)` | 14 | *Not mapped* | *N/A* | *Not mapped* | *N/A* | Padding |
| | | **Total** | **50** | | | | | |

---

## 6. Customer Record (CVCUS01Y.cpy) - 500 Bytes

**COBOL Record:** `CUSTOMER-RECORD` (01 level)
**Java Entity:** `Customer`
**PostgreSQL Table:** `customer`

| # | COBOL Field | PIC Clause | Bytes | Java Field | Java Type | PostgreSQL Column | PostgreSQL Type | Constraints |
|---|---|---|---|---|---|---|---|---|
| 1 | `CUST-ID` | `9(09)` | 9 | `customerId` | `Long` | `customer_id` | `NUMERIC(9,0)` | `PRIMARY KEY` |
| 2 | `CUST-FIRST-NAME` | `X(25)` | 25 | `firstName` | `String` | `first_name` | `VARCHAR(25)` | `NOT NULL` |
| 3 | `CUST-MIDDLE-NAME` | `X(25)` | 25 | `middleName` | `String` | `middle_name` | `VARCHAR(25)` | |
| 4 | `CUST-LAST-NAME` | `X(25)` | 25 | `lastName` | `String` | `last_name` | `VARCHAR(25)` | `NOT NULL` |
| 5 | `CUST-ADDR-LINE-1` | `X(50)` | 50 | `addressLine1` | `String` | `address_line_1` | `VARCHAR(50)` | |
| 6 | `CUST-ADDR-LINE-2` | `X(50)` | 50 | `addressLine2` | `String` | `address_line_2` | `VARCHAR(50)` | |
| 7 | `CUST-ADDR-LINE-3` | `X(50)` | 50 | `addressLine3` | `String` | `address_line_3` | `VARCHAR(50)` | |
| 8 | `CUST-ADDR-STATE-CD` | `X(02)` | 2 | `stateCode` | `String` | `state_code` | `VARCHAR(2)` | |
| 9 | `CUST-ADDR-COUNTRY-CD` | `X(03)` | 3 | `countryCode` | `String` | `country_code` | `VARCHAR(3)` | |
| 10 | `CUST-ADDR-ZIP` | `X(10)` | 10 | `addressZip` | `String` | `address_zip` | `VARCHAR(10)` | |
| 11 | `CUST-PHONE-NUM-1` | `X(15)` | 15 | `phoneNumber1` | `String` | `phone_number_1` | `VARCHAR(15)` | |
| 12 | `CUST-PHONE-NUM-2` | `X(15)` | 15 | `phoneNumber2` | `String` | `phone_number_2` | `VARCHAR(15)` | |
| 13 | `CUST-SSN` | `9(09)` | 9 | `ssn` | `Long` | `ssn` | `NUMERIC(9,0)` | `UNIQUE` |
| 14 | `CUST-GOVT-ISSUED-ID` | `X(20)` | 20 | `governmentIssuedId` | `String` | `government_issued_id` | `VARCHAR(20)` | |
| 15 | `CUST-DOB-YYYY-MM-DD` | `X(10)` | 10 | `dateOfBirth` | `LocalDate` | `date_of_birth` | `DATE` | |
| 16 | `CUST-EFT-ACCOUNT-ID` | `X(10)` | 10 | `eftAccountId` | `String` | `eft_account_id` | `VARCHAR(10)` | |
| 17 | `CUST-PRI-CARD-HOLDER-IND` | `X(01)` | 1 | `primaryCardHolderIndicator` | `String` | `primary_card_holder_ind` | `VARCHAR(1)` | |
| 18 | `CUST-FICO-CREDIT-SCORE` | `9(03)` | 3 | `ficoCreditScore` | `Integer` | `fico_credit_score` | `NUMERIC(3,0)` | |
| 19 | `FILLER` | `X(168)` | 168 | *Not mapped* | *N/A* | *Not mapped* | *N/A* | Padding |
| | | **Total** | **500** | | | | | |

---

## 7. Special Type Mapping Notes

### 7.1 BigDecimal Usage (Financial Fields)

All monetary amounts MUST use `BigDecimal` in Java and `NUMERIC` in PostgreSQL to preserve precision:

```java
// Java entity field
@Column(name = "amount", precision = 11, scale = 2, nullable = false)
private BigDecimal amount;

// Never: private double amount;  -- loses precision
// Never: private float amount;   -- loses precision
```

```sql
-- PostgreSQL DDL
amount NUMERIC(11,2) NOT NULL
```

### 7.2 Timestamp Conversion (X(26) to TIMESTAMP)

The COBOL `X(26)` timestamp fields (TRAN-ORIG-TS, TRAN-PROC-TS) store timestamps as character strings. In the modernized system:

- **Java**: Use `java.time.LocalDateTime` with a formatter for parsing legacy data
- **PostgreSQL**: Use `TIMESTAMP` (without time zone) or `TIMESTAMPTZ` (with time zone)
- **Migration**: Parse the 26-byte COBOL string during data migration

### 7.3 Date Conversion (X(10) to DATE)

COBOL `X(10)` date fields follow `YYYY-MM-DD` format:

- **Java**: Use `java.time.LocalDate`
- **PostgreSQL**: Use `DATE`
- **Validation**: The legacy `CSUTLDTC` utility validates calendar dates; replace with `java.time` parsing in the modernized system

### 7.4 Numeric String Fields

Some fields are stored as `X(n)` (alphanumeric) in COBOL but contain numeric values validated at the application level (e.g., `TRAN-TYPE-CD X(02)` is validated to be numeric in COTRN02C). These are mapped to `String`/`VARCHAR` to preserve the original format, with application-level validation enforced in the Spring Boot service layer.

### 7.5 FILLER Fields

COBOL FILLER fields exist solely for fixed-length record alignment in VSAM files. They carry no business data and are **not mapped** to the relational model. The total byte count of each record (including FILLER) is documented for data migration verification.
