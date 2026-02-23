# CardDemo Data Migration Strategy

## Document Information
| Item | Detail |
|------|--------|
| **Document** | Data Migration Strategy |
| **Application** | CardDemo - Java 17 Migration |
| **Phase** | Phase 1.3 - Data Migration Planning |
| **Version** | 1.0 |

---

## 1. VSAM to Relational Database Mapping

### 1.1 User Security (CSUSR01Y → USER_SECURITY)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| SEC-USR-ID | X(08) | String | usr_id | VARCHAR(8) | Primary Key |
| SEC-USR-FNAME | X(20) | String | usr_first_name | VARCHAR(20) | |
| SEC-USR-LNAME | X(20) | String | usr_last_name | VARCHAR(20) | |
| SEC-USR-PWD | X(08) | String | usr_password | VARCHAR(72) | BCrypt hashed in Java |
| SEC-USR-TYPE | X(01) | String | usr_type | CHAR(1) | 'A'=Admin, 'U'=User |
| SEC-USR-FILLER | X(23) | - | - | - | Dropped (padding) |

**Record Length**: 80 bytes → Variable (normalized)

### 1.2 Account Master (CVACT01Y → ACCOUNT)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| ACCT-ID | 9(11) | Long | acct_id | BIGINT | Primary Key |
| ACCT-ACTIVE-STATUS | X(01) | String | active_status | CHAR(1) | |
| ACCT-CURR-BAL | S9(10)V99 | BigDecimal | current_balance | DECIMAL(12,2) | Signed with 2 decimals |
| ACCT-CREDIT-LIMIT | S9(10)V99 | BigDecimal | credit_limit | DECIMAL(12,2) | |
| ACCT-CASH-CREDIT-LIMIT | S9(10)V99 | BigDecimal | cash_credit_limit | DECIMAL(12,2) | |
| ACCT-OPEN-DATE | X(10) | LocalDate | open_date | DATE | Parse from string |
| ACCT-EXPIRAION-DATE | X(10) | LocalDate | expiration_date | DATE | Note: typo preserved |
| ACCT-REISSUE-DATE | X(10) | LocalDate | reissue_date | DATE | |
| ACCT-CURR-CYC-CREDIT | S9(10)V99 | BigDecimal | current_cycle_credit | DECIMAL(12,2) | |
| ACCT-CURR-CYC-DEBIT | S9(10)V99 | BigDecimal | current_cycle_debit | DECIMAL(12,2) | |
| ACCT-ADDR-ZIP | X(10) | String | address_zip | VARCHAR(10) | |
| ACCT-GROUP-ID | X(10) | String | group_id | VARCHAR(10) | Links to disclosure groups |
| FILLER | X(178) | - | - | - | Dropped (padding) |

**Record Length**: 300 bytes → Variable (normalized)

### 1.3 Card Master (CVACT02Y → CARD)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| CARD-NUM | X(16) | String | card_num | VARCHAR(16) | Primary Key |
| CARD-ACCT-ID | 9(11) | Long | acct_id | BIGINT | FK → ACCOUNT |
| CARD-CVV-CD | 9(03) | Integer | cvv_code | INTEGER | Encrypt at rest |
| CARD-EMBOSSED-NAME | X(50) | String | embossed_name | VARCHAR(50) | |
| CARD-EXPIRAION-DATE | X(10) | LocalDate | expiration_date | DATE | |
| CARD-ACTIVE-STATUS | X(01) | String | active_status | CHAR(1) | |
| FILLER | X(59) | - | - | - | Dropped |

**Record Length**: 150 bytes → Variable (normalized)

### 1.4 Customer Master (CVCUS01Y → CUSTOMER)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| CUST-ID | 9(09) | Long | cust_id | BIGINT | Primary Key |
| CUST-FIRST-NAME | X(25) | String | first_name | VARCHAR(25) | |
| CUST-MIDDLE-NAME | X(25) | String | middle_name | VARCHAR(25) | |
| CUST-LAST-NAME | X(25) | String | last_name | VARCHAR(25) | |
| CUST-ADDR-LINE-1 | X(50) | String | addr_line_1 | VARCHAR(50) | |
| CUST-ADDR-LINE-2 | X(50) | String | addr_line_2 | VARCHAR(50) | |
| CUST-ADDR-LINE-3 | X(50) | String | addr_line_3 | VARCHAR(50) | |
| CUST-ADDR-STATE-CD | X(02) | String | addr_state_code | CHAR(2) | |
| CUST-ADDR-COUNTRY-CD | X(03) | String | addr_country_code | CHAR(3) | |
| CUST-ADDR-ZIP | X(10) | String | addr_zip | VARCHAR(10) | |
| CUST-PHONE-NUM-1 | X(15) | String | phone_num_1 | VARCHAR(15) | |
| CUST-PHONE-NUM-2 | X(15) | String | phone_num_2 | VARCHAR(15) | |
| CUST-SSN | 9(09) | String | ssn | VARCHAR(11) | Encrypt at rest |
| CUST-GOVT-ISSUED-ID | X(20) | String | govt_issued_id | VARCHAR(20) | |
| CUST-DOB-YYYY-MM-DD | X(10) | LocalDate | date_of_birth | DATE | |
| CUST-EFT-ACCOUNT-ID | X(10) | String | eft_account_id | VARCHAR(10) | |
| CUST-PRI-CARD-HOLDER-IND | X(01) | String | primary_card_holder | CHAR(1) | |
| CUST-FICO-CREDIT-SCORE | 9(03) | Integer | fico_credit_score | INTEGER | |
| FILLER | X(168) | - | - | - | Dropped |

**Record Length**: 500 bytes → Variable (normalized)

### 1.5 Card-Account-Customer Cross Reference (CVACT03Y → CARD_XREF)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| XREF-CARD-NUM | X(16) | String | card_num | VARCHAR(16) | PK, FK → CARD |
| XREF-CUST-ID | 9(09) | Long | cust_id | BIGINT | FK → CUSTOMER |
| XREF-ACCT-ID | 9(11) | Long | acct_id | BIGINT | FK → ACCOUNT |
| FILLER | X(14) | - | - | - | Dropped |

**Record Length**: 50 bytes → Variable (normalized)

### 1.6 Transaction (CVTRA05Y → TRANSACTION)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| TRAN-ID | X(16) | String | tran_id | VARCHAR(16) | Primary Key |
| TRAN-TYPE-CD | X(02) | String | type_code | CHAR(2) | FK → TRANSACTION_TYPE |
| TRAN-CAT-CD | 9(04) | Integer | category_code | INTEGER | |
| TRAN-SOURCE | X(10) | String | source | VARCHAR(10) | |
| TRAN-DESC | X(100) | String | description | VARCHAR(100) | |
| TRAN-AMT | S9(09)V99 | BigDecimal | amount | DECIMAL(11,2) | |
| TRAN-MERCHANT-ID | 9(09) | Long | merchant_id | BIGINT | |
| TRAN-MERCHANT-NAME | X(50) | String | merchant_name | VARCHAR(50) | |
| TRAN-MERCHANT-CITY | X(50) | String | merchant_city | VARCHAR(50) | |
| TRAN-MERCHANT-ZIP | X(10) | String | merchant_zip | VARCHAR(10) | |
| TRAN-CARD-NUM | X(16) | String | card_num | VARCHAR(16) | FK → CARD |
| TRAN-ORIG-TS | X(26) | LocalDateTime | orig_timestamp | TIMESTAMP | |
| TRAN-PROC-TS | X(26) | LocalDateTime | proc_timestamp | TIMESTAMP | |
| FILLER | X(20) | - | - | - | Dropped |

**Record Length**: 350 bytes → Variable (normalized)

### 1.7 Transaction Category Balance (CVTRA01Y → TRAN_CAT_BALANCE)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| TRANCAT-ACCT-ID | 9(11) | Long | acct_id | BIGINT | Composite PK, FK → ACCOUNT |
| TRANCAT-TYPE-CD | X(02) | String | type_code | CHAR(2) | Composite PK |
| TRANCAT-CD | 9(04) | Integer | category_code | INTEGER | Composite PK |
| TRAN-CAT-BAL | S9(09)V99 | BigDecimal | balance | DECIMAL(11,2) | |
| FILLER | X(22) | - | - | - | Dropped |

**Record Length**: 50 bytes → Variable (normalized)

### 1.8 Disclosure Group (CVTRA02Y → DISCLOSURE_GROUP)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| DIS-ACCT-GROUP-ID | X(10) | String | acct_group_id | VARCHAR(10) | Composite PK |
| DIS-TRAN-TYPE-CD | X(02) | String | tran_type_code | CHAR(2) | Composite PK |
| DIS-TRAN-CAT-CD | 9(04) | Integer | tran_cat_code | INTEGER | Composite PK |
| DIS-INT-RATE | S9(04)V99 | BigDecimal | interest_rate | DECIMAL(6,2) | |
| FILLER | X(28) | - | - | - | Dropped |

**Record Length**: 50 bytes → Variable (normalized)

### 1.9 Transaction Type (CVTRA03Y → TRANSACTION_TYPE)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| TRAN-TYPE | X(02) | String | type_code | CHAR(2) | Primary Key |
| TRAN-TYPE-DESC | X(50) | String | description | VARCHAR(50) | |
| FILLER | X(08) | - | - | - | Dropped |

**Record Length**: 60 bytes → Variable (normalized)

### 1.10 Transaction Category (CVTRA04Y → TRANSACTION_CATEGORY)

| COBOL Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------|-----------|-----------|-----------|---------|-------|
| TRAN-TYPE-CD | X(02) | String | type_code | CHAR(2) | Composite PK, FK → TRANSACTION_TYPE |
| TRAN-CAT-CD | 9(04) | Integer | category_code | INTEGER | Composite PK |
| TRAN-CAT-TYPE-DESC | X(50) | String | description | VARCHAR(50) | |
| FILLER | X(04) | - | - | - | Dropped |

**Record Length**: 60 bytes → Variable (normalized)

---

## 2. IMS Hierarchical to Relational Conversion

### 2.1 Authorization Summary (CIPAUSMY → AUTHORIZATION_SUMMARY)

| IMS Segment Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------------|-----------|-----------|-----------|---------|-------|
| PA-ACCT-ID | S9(11) COMP-3 | Long | acct_id | BIGINT | Primary Key, FK → ACCOUNT |
| PA-CUST-ID | 9(09) | Long | cust_id | BIGINT | FK → CUSTOMER |
| PA-AUTH-STATUS | X(01) | String | auth_status | CHAR(1) | |
| PA-ACCOUNT-STATUS | X(02) OCCURS 5 | String | account_status_1..5 | CHAR(2) x5 | Flatten OCCURS |
| PA-CREDIT-LIMIT | S9(09)V99 COMP-3 | BigDecimal | credit_limit | DECIMAL(11,2) | Packed decimal |
| PA-CASH-LIMIT | S9(09)V99 COMP-3 | BigDecimal | cash_limit | DECIMAL(11,2) | |
| PA-CREDIT-BALANCE | S9(09)V99 COMP-3 | BigDecimal | credit_balance | DECIMAL(11,2) | |
| PA-CASH-BALANCE | S9(09)V99 COMP-3 | BigDecimal | cash_balance | DECIMAL(11,2) | |
| PA-APPROVED-AUTH-CNT | S9(04) COMP | Integer | approved_auth_count | INTEGER | Binary |
| PA-DECLINED-AUTH-CNT | S9(04) COMP | Integer | declined_auth_count | INTEGER | |
| PA-APPROVED-AUTH-AMT | S9(09)V99 COMP-3 | BigDecimal | approved_auth_amount | DECIMAL(11,2) | |
| PA-DECLINED-AUTH-AMT | S9(09)V99 COMP-3 | BigDecimal | declined_auth_amount | DECIMAL(11,2) | |

### 2.2 Authorization Detail (CIPAUDTY → AUTHORIZATION_DETAIL)

| IMS Segment Field | PIC Clause | Java Type | DB Column | DB Type | Notes |
|-------------------|-----------|-----------|-----------|---------|-------|
| PA-AUTH-DATE-9C | S9(05) COMP-3 | LocalDate | auth_date | DATE | Key part 1 |
| PA-AUTH-TIME-9C | S9(09) COMP-3 | LocalTime | auth_time | TIME | Key part 2 |
| PA-AUTH-ORIG-DATE | X(06) | String | auth_orig_date | VARCHAR(6) | |
| PA-AUTH-ORIG-TIME | X(06) | String | auth_orig_time | VARCHAR(6) | |
| PA-CARD-NUM | X(16) | String | card_num | VARCHAR(16) | FK → CARD |
| PA-AUTH-TYPE | X(04) | String | auth_type | CHAR(4) | |
| PA-CARD-EXPIRY-DATE | X(04) | String | card_expiry_date | CHAR(4) | |
| PA-MESSAGE-TYPE | X(06) | String | message_type | VARCHAR(6) | |
| PA-MESSAGE-SOURCE | X(06) | String | message_source | VARCHAR(6) | |
| PA-AUTH-ID-CODE | X(06) | String | auth_id_code | VARCHAR(6) | |
| PA-AUTH-RESP-CODE | X(02) | String | auth_resp_code | CHAR(2) | '00'=Approved |
| PA-AUTH-RESP-REASON | X(04) | String | auth_resp_reason | CHAR(4) | |
| PA-PROCESSING-CODE | 9(06) | Integer | processing_code | INTEGER | |
| PA-TRANSACTION-AMT | S9(10)V99 COMP-3 | BigDecimal | transaction_amount | DECIMAL(12,2) | |
| PA-APPROVED-AMT | S9(10)V99 COMP-3 | BigDecimal | approved_amount | DECIMAL(12,2) | |
| PA-MERCHANT-CATAGORY-CODE | X(04) | String | merchant_category_code | CHAR(4) | |
| PA-ACQR-COUNTRY-CODE | X(03) | String | acquirer_country_code | CHAR(3) | |
| PA-POS-ENTRY-MODE | 9(02) | Integer | pos_entry_mode | SMALLINT | |
| PA-MERCHANT-ID | X(15) | String | merchant_id | VARCHAR(15) | |
| PA-MERCHANT-NAME | X(22) | String | merchant_name | VARCHAR(22) | |
| PA-MERCHANT-CITY | X(13) | String | merchant_city | VARCHAR(13) | |
| PA-MERCHANT-STATE | X(02) | String | merchant_state | CHAR(2) | |
| PA-MERCHANT-ZIP | X(09) | String | merchant_zip | VARCHAR(9) | |
| PA-TRANSACTION-ID | X(15) | String | transaction_id | VARCHAR(15) | |
| PA-MATCH-STATUS | X(01) | String | match_status | CHAR(1) | P/D/E/M |
| PA-AUTH-FRAUD | X(01) | String | auth_fraud | CHAR(1) | F/R |
| PA-FRAUD-RPT-DATE | X(08) | String | fraud_report_date | DATE | |

**IMS Parent-Child Relationship**:
- `AUTHORIZATION_DETAIL.acct_id` → FK to `AUTHORIZATION_SUMMARY.acct_id`
- Auto-generated `id` (BIGSERIAL) as surrogate PK for AUTHORIZATION_DETAIL
- Composite unique constraint on (acct_id, auth_date, auth_time, card_num)

### 2.3 DB2 Fraud Tracking (AUTHFRDS → AUTH_FRAUD)

Direct migration from DB2 table with minimal changes:

| DB2 Column | DB2 Type | Java Type | PG Column | PG Type |
|-----------|---------|-----------|-----------|---------|
| CARD_NUM | CHAR(16) | String | card_num | VARCHAR(16) |
| AUTH_TS | TIMESTAMP | LocalDateTime | auth_timestamp | TIMESTAMP |
| AUTH_TYPE | CHAR(4) | String | auth_type | CHAR(4) |
| CARD_EXPIRY_DATE | CHAR(4) | String | card_expiry_date | CHAR(4) |
| MESSAGE_TYPE | CHAR(6) | String | message_type | VARCHAR(6) |
| MESSAGE_SOURCE | CHAR(6) | String | message_source | VARCHAR(6) |
| AUTH_ID_CODE | CHAR(6) | String | auth_id_code | VARCHAR(6) |
| AUTH_RESP_CODE | CHAR(2) | String | auth_resp_code | CHAR(2) |
| AUTH_RESP_REASON | CHAR(4) | String | auth_resp_reason | CHAR(4) |
| PROCESSING_CODE | CHAR(6) | String | processing_code | VARCHAR(6) |
| TRANSACTION_AMT | DECIMAL(12,2) | BigDecimal | transaction_amount | DECIMAL(12,2) |
| APPROVED_AMT | DECIMAL(12,2) | BigDecimal | approved_amount | DECIMAL(12,2) |
| MERCHANT_CATAGORY_CODE | CHAR(4) | String | merchant_category_code | CHAR(4) |
| ACQR_COUNTRY_CODE | CHAR(3) | String | acquirer_country_code | CHAR(3) |
| POS_ENTRY_MODE | SMALLINT | Integer | pos_entry_mode | SMALLINT |
| MERCHANT_ID | CHAR(15) | String | merchant_id | VARCHAR(15) |
| MERCHANT_NAME | VARCHAR(22) | String | merchant_name | VARCHAR(22) |
| MERCHANT_CITY | CHAR(13) | String | merchant_city | VARCHAR(13) |
| MERCHANT_STATE | CHAR(02) | String | merchant_state | CHAR(2) |
| MERCHANT_ZIP | CHAR(09) | String | merchant_zip | VARCHAR(9) |
| TRANSACTION_ID | CHAR(15) | String | transaction_id | VARCHAR(15) |
| MATCH_STATUS | CHAR(1) | String | match_status | CHAR(1) |
| AUTH_FRAUD | CHAR(1) | String | auth_fraud | CHAR(1) |
| FRAUD_RPT_DATE | DATE | LocalDate | fraud_report_date | DATE |
| ACCT_ID | DECIMAL(11) | Long | acct_id | BIGINT |
| CUST_ID | DECIMAL(9) | Long | cust_id | BIGINT |

---

## 3. COBOL Data Type Conversion Rules

### 3.1 Numeric Types

| COBOL PIC | Storage | Java Type | PostgreSQL Type | Conversion Notes |
|-----------|---------|-----------|----------------|-----------------|
| 9(n) | Display (zoned decimal) | Long / Integer | BIGINT / INTEGER | Strip zone bits |
| S9(n) | Signed zoned decimal | Long / Integer | BIGINT / INTEGER | Handle sign nibble |
| S9(n)V99 | Signed with implied decimal | BigDecimal | DECIMAL(n+2,2) | Insert decimal point |
| 9(n) COMP | Binary (2 or 4 bytes) | Integer / Long | INTEGER / BIGINT | Direct binary conversion |
| S9(n) COMP | Signed binary | Integer / Long | INTEGER / BIGINT | Two's complement |
| S9(n)V99 COMP-3 | Packed decimal | BigDecimal | DECIMAL(n+2,2) | Unpack BCD, insert decimal |
| 9(n) COMP-3 | Unsigned packed | BigDecimal | DECIMAL(n,0) | Unpack BCD |

### 3.2 String Types

| COBOL PIC | Java Type | PostgreSQL Type | Conversion Notes |
|-----------|-----------|----------------|-----------------|
| X(n) | String | VARCHAR(n) | EBCDIC → UTF-8 conversion |
| A(n) | String | VARCHAR(n) | Alphabetic only |
| 9(n) (used as key) | String | VARCHAR(n) | Preserve leading zeros |

### 3.3 Date/Time Types

| COBOL Format | Java Type | PostgreSQL Type | Example |
|-------------|-----------|----------------|---------|
| X(10) date string | LocalDate | DATE | "2024-01-15" |
| X(26) timestamp | LocalDateTime | TIMESTAMP | "2024-01-15 10:30:00.000000" |
| X(06) date (MMDDYY) | LocalDate | DATE | Parse with formatter |
| X(06) time (HHMMSS) | LocalTime | TIME | Parse with formatter |
| S9(05) COMP-3 date | LocalDate | DATE | Julian/packed date conversion |

### 3.4 Special Structures

| COBOL Structure | Java Mapping | Notes |
|----------------|-------------|-------|
| REDEFINES | Separate classes or union type | Use inheritance or composition |
| OCCURS n TIMES | List<T> or array | Fixed-size → variable collection |
| OCCURS DEPENDING ON | List<T> | Variable-size collection |
| FILLER | Omitted | Not mapped to Java |
| 88-level (condition) | Enum or constants | Boolean conditions → enum values |
| GROUP level | Embedded class or flattened fields | Depends on complexity |

---

## 4. Index Strategy (VSAM AIX Replacement)

### 4.1 Primary Key Indexes

| Table | Primary Key | Index Type |
|-------|------------|-----------|
| USER_SECURITY | usr_id | B-tree (unique) |
| ACCOUNT | acct_id | B-tree (unique) |
| CARD | card_num | B-tree (unique) |
| CUSTOMER | cust_id | B-tree (unique) |
| CARD_XREF | card_num | B-tree (unique) |
| TRANSACTION | tran_id | B-tree (unique) |
| TRANSACTION_TYPE | type_code | B-tree (unique) |
| TRANSACTION_CATEGORY | (type_code, category_code) | B-tree (unique composite) |
| TRAN_CAT_BALANCE | (acct_id, type_code, category_code) | B-tree (unique composite) |
| DISCLOSURE_GROUP | (acct_group_id, tran_type_code, tran_cat_code) | B-tree (unique composite) |

### 4.2 Alternate Index Equivalents

| VSAM AIX Purpose | PostgreSQL Index | Index Definition |
|-----------------|-----------------|------------------|
| Transaction by card number | idx_transaction_card_num | `CREATE INDEX ON transaction(card_num)` |
| Transaction by timestamp | idx_transaction_orig_ts | `CREATE INDEX ON transaction(orig_timestamp)` |
| Card by account ID | idx_card_acct_id | `CREATE INDEX ON card(acct_id)` |
| Cross-ref by customer ID | idx_xref_cust_id | `CREATE INDEX ON card_xref(cust_id)` |
| Cross-ref by account ID | idx_xref_acct_id | `CREATE INDEX ON card_xref(acct_id)` |
| Account by group ID | idx_account_group_id | `CREATE INDEX ON account(group_id)` |
| Auth detail by card number | idx_auth_detail_card | `CREATE INDEX ON authorization_detail(card_num)` |
| Auth fraud by card+timestamp | idx_auth_fraud_card_ts | `CREATE UNIQUE INDEX ON auth_fraud(card_num, auth_timestamp DESC)` |

---

## 5. Data Migration Approach

### 5.1 Migration Phases

```
Phase 1: Schema Creation
  └─ Execute Flyway migrations to create all tables, indexes, constraints

Phase 2: Reference Data Load
  └─ Load TRANSACTION_TYPE, TRANSACTION_CATEGORY, DISCLOSURE_GROUP
  └─ These are small lookup tables with minimal data

Phase 3: Master Data Load
  └─ Load CUSTOMER records (EBCDIC → UTF-8 conversion)
  └─ Load ACCOUNT records (packed decimal → BigDecimal)
  └─ Load CARD records
  └─ Load CARD_XREF records

Phase 4: Transactional Data Load
  └─ Load TRANSACTION records (largest volume)
  └─ Load TRAN_CAT_BALANCE records

Phase 5: Security Data Load
  └─ Load USER_SECURITY records (hash passwords with BCrypt)

Phase 6: Optional Module Data
  └─ Load AUTHORIZATION_SUMMARY records (IMS unload → CSV → SQL)
  └─ Load AUTHORIZATION_DETAIL records
  └─ Load AUTH_FRAUD records (DB2 → PostgreSQL)

Phase 7: Validation
  └─ Record count verification
  └─ Checksum validation on key numeric fields
  └─ Referential integrity verification
  └─ Sample data spot-checks
```

### 5.2 EBCDIC to ASCII/UTF-8 Conversion

| Data Category | Conversion Method |
|--------------|------------------|
| Alphanumeric fields (PIC X) | EBCDIC code page → UTF-8 using IBM ICU library |
| Numeric display (PIC 9) | Strip zone nibbles (F → digit) |
| Packed decimal (COMP-3) | Unpack BCD pairs, apply sign nibble |
| Binary (COMP) | Read as big-endian signed integer |
| Signed zoned decimal | Handle trailing sign overpunch (C/D/F) |

### 5.3 ETL Tool Recommendation

For data migration, use a Spring Batch-based ETL tool:

1. **ItemReader**: Read EBCDIC flat files with fixed-length record layout
2. **ItemProcessor**: Convert COBOL data types to Java types
3. **ItemWriter**: Write to PostgreSQL via JPA or JDBC batch insert

This leverages the same Spring Batch framework used in the target application.

---

## 6. Entity Relationship Summary

```
                    ┌──────────────┐
                    │  CUSTOMER    │
                    │  PK: cust_id │
                    └──────┬───────┘
                           │ 1
                           │
                           │ *
                    ┌──────┴───────┐        ┌──────────────┐
                    │  CARD_XREF   │───────▶│  ACCOUNT     │
                    │  PK: card_num│  *   1 │  PK: acct_id │
                    └──────┬───────┘        └──────┬───────┘
                           │ 1                      │ 1
                           │                        │
                           │ 1                      │ *
                    ┌──────┴───────┐        ┌──────┴────────────┐
                    │  CARD        │        │  TRAN_CAT_BALANCE │
                    │  PK: card_num│        │  PK: composite    │
                    └──────┬───────┘        └──────────────────┘
                           │ 1
                           │
                           │ *
                    ┌──────┴───────┐
                    │  TRANSACTION │        ┌──────────────────┐
                    │  PK: tran_id │───────▶│  TRANSACTION_TYPE│
                    └──────────────┘  *   1 │  PK: type_code   │
                                            └──────┬───────────┘
                                                   │ 1
                                                   │
                                                   │ *
                                            ┌──────┴───────────┐
                                            │  TRAN_CATEGORY   │
                                            │  PK: composite   │
                                            └──────────────────┘

  ┌──────────────┐        ┌──────────────────────┐
  │  AUTH_SUMMARY │───────▶│  AUTHORIZATION_DETAIL│
  │  PK: acct_id  │  1   * │  PK: id (surrogate)  │
  └──────────────┘        └──────────────────────┘

  ┌──────────────┐        ┌──────────────────┐
  │  USER_SECURITY│        │  DISCLOSURE_GROUP│
  │  PK: usr_id   │        │  PK: composite   │
  └──────────────┘        └──────────────────┘

  ┌──────────────┐
  │  AUTH_FRAUD  │
  │  PK:(card,ts)│
  └──────────────┘
```
