# Entity-Relationship Diagram: PostgreSQL Schema

> **Module:** Transaction Processing (CardDemo Modernization)
> **Phase:** 3 - Design
> **Target Database:** PostgreSQL 15+
> **Notation:** Mermaid.js
> **Source Copybooks:** CVTRA05Y.cpy, CVACT01Y.cpy, CVACT02Y.cpy, CVACT03Y.cpy, CVCUS01Y.cpy

---

## 1. Complete Entity-Relationship Diagram

This diagram shows the full PostgreSQL schema with all foreign key relationships, replacing the legacy VSAM KSDS/AIX file structures.

```mermaid
erDiagram
    customer ||--o{ card_cross_reference : "has cards via"
    account ||--o{ card_cross_reference : "linked through"
    account ||--o{ card : "owns"
    card_cross_reference ||--|| card : "maps to"
    card ||--o{ transaction : "charged on"

    customer {
        NUMERIC_9_0 customer_id PK "NOT NULL — CUST-ID 9(09)"
        VARCHAR_25 first_name "NOT NULL — CUST-FIRST-NAME X(25)"
        VARCHAR_25 middle_name "NULLABLE — CUST-MIDDLE-NAME X(25)"
        VARCHAR_25 last_name "NOT NULL — CUST-LAST-NAME X(25)"
        VARCHAR_50 address_line_1 "NULLABLE — CUST-ADDR-LINE-1 X(50)"
        VARCHAR_50 address_line_2 "NULLABLE — CUST-ADDR-LINE-2 X(50)"
        VARCHAR_50 address_line_3 "NULLABLE — CUST-ADDR-LINE-3 X(50)"
        VARCHAR_2 state_code "NULLABLE — CUST-ADDR-STATE-CD X(02)"
        VARCHAR_3 country_code "NULLABLE — CUST-ADDR-COUNTRY-CD X(03)"
        VARCHAR_10 address_zip "NULLABLE — CUST-ADDR-ZIP X(10)"
        VARCHAR_15 phone_number_1 "NULLABLE — CUST-PHONE-NUM-1 X(15)"
        VARCHAR_15 phone_number_2 "NULLABLE — CUST-PHONE-NUM-2 X(15)"
        NUMERIC_9_0 ssn "UNIQUE — CUST-SSN 9(09)"
        VARCHAR_20 government_issued_id "NULLABLE — CUST-GOVT-ISSUED-ID X(20)"
        DATE date_of_birth "NULLABLE — CUST-DOB-YYYY-MM-DD X(10)"
        VARCHAR_10 eft_account_id "NULLABLE — CUST-EFT-ACCOUNT-ID X(10)"
        VARCHAR_1 primary_card_holder_ind "NULLABLE — CUST-PRI-CARD-HOLDER-IND X(01)"
        NUMERIC_3_0 fico_credit_score "NULLABLE — CUST-FICO-CREDIT-SCORE 9(03)"
    }

    account {
        NUMERIC_11_0 account_id PK "NOT NULL — ACCT-ID 9(11)"
        VARCHAR_1 active_status "NOT NULL — ACCT-ACTIVE-STATUS X(01)"
        NUMERIC_12_2 current_balance "NOT NULL — ACCT-CURR-BAL S9(10)V99"
        NUMERIC_12_2 credit_limit "NOT NULL — ACCT-CREDIT-LIMIT S9(10)V99"
        NUMERIC_12_2 cash_credit_limit "NOT NULL — ACCT-CASH-CREDIT-LIMIT S9(10)V99"
        DATE open_date "NOT NULL — ACCT-OPEN-DATE X(10)"
        DATE expiration_date "NULLABLE — ACCT-EXPIRAION-DATE X(10)"
        DATE reissue_date "NULLABLE — ACCT-REISSUE-DATE X(10)"
        NUMERIC_12_2 current_cycle_credit "NOT NULL — ACCT-CURR-CYC-CREDIT S9(10)V99"
        NUMERIC_12_2 current_cycle_debit "NOT NULL — ACCT-CURR-CYC-DEBIT S9(10)V99"
        VARCHAR_10 address_zip "NULLABLE — ACCT-ADDR-ZIP X(10)"
        VARCHAR_10 group_id "NULLABLE — ACCT-GROUP-ID X(10)"
    }

    card {
        VARCHAR_16 card_number PK "NOT NULL — CARD-NUM X(16)"
        NUMERIC_11_0 account_id FK "NOT NULL — CARD-ACCT-ID 9(11) → account.account_id"
        NUMERIC_3_0 cvv_code "NOT NULL — CARD-CVV-CD 9(03)"
        VARCHAR_50 embossed_name "NOT NULL — CARD-EMBOSSED-NAME X(50)"
        DATE expiration_date "NOT NULL — CARD-EXPIRAION-DATE X(10)"
        VARCHAR_1 active_status "NOT NULL — CARD-ACTIVE-STATUS X(01)"
    }

    card_cross_reference {
        VARCHAR_16 card_number PK "NOT NULL — XREF-CARD-NUM X(16) → card.card_number"
        NUMERIC_9_0 customer_id FK "NOT NULL — XREF-CUST-ID 9(09) → customer.customer_id"
        NUMERIC_11_0 account_id FK "NOT NULL — XREF-ACCT-ID 9(11) → account.account_id"
    }

    transaction {
        VARCHAR_16 transaction_id PK "NOT NULL — TRAN-ID X(16)"
        VARCHAR_16 card_number FK "NOT NULL — TRAN-CARD-NUM X(16) → card.card_number"
        VARCHAR_2 type_code "NOT NULL — TRAN-TYPE-CD X(02)"
        NUMERIC_4_0 category_code "NOT NULL — TRAN-CAT-CD 9(04)"
        VARCHAR_10 source "NOT NULL — TRAN-SOURCE X(10)"
        VARCHAR_100 description "NOT NULL — TRAN-DESC X(100)"
        NUMERIC_11_2 amount "NOT NULL — TRAN-AMT S9(09)V99"
        NUMERIC_9_0 merchant_id "NOT NULL — TRAN-MERCHANT-ID 9(09)"
        VARCHAR_50 merchant_name "NOT NULL — TRAN-MERCHANT-NAME X(50)"
        VARCHAR_50 merchant_city "NOT NULL — TRAN-MERCHANT-CITY X(50)"
        VARCHAR_10 merchant_zip "NOT NULL — TRAN-MERCHANT-ZIP X(10)"
        TIMESTAMP origination_ts "NOT NULL — TRAN-ORIG-TS X(26)"
        TIMESTAMP processing_ts "NOT NULL — TRAN-PROC-TS X(26)"
    }
```

---

## 2. Foreign Key Relationships

```mermaid
graph TB
    subgraph "PostgreSQL Tables"
        CUST[customer<br/>PK: customer_id NUMERIC(9,0)]
        ACCT[account<br/>PK: account_id NUMERIC(11,0)]
        CARD[card<br/>PK: card_number VARCHAR(16)]
        XREF[card_cross_reference<br/>PK: card_number VARCHAR(16)]
        TRAN[transaction<br/>PK: transaction_id VARCHAR(16)]
        SEQ[transaction_id_seq<br/>PostgreSQL SEQUENCE]
    end

    CARD -->|FK: account_id| ACCT
    XREF -->|FK: card_number| CARD
    XREF -->|FK: account_id| ACCT
    XREF -->|FK: customer_id| CUST
    TRAN -->|FK: card_number| CARD
    SEQ -.->|generates| TRAN

    style CUST fill:#fff3e0
    style ACCT fill:#e8f5e9
    style CARD fill:#e3f2fd
    style XREF fill:#fce4ec
    style TRAN fill:#e1f5fe
    style SEQ fill:#f3e5f5,stroke-dasharray: 5 5
```

### Foreign Key Summary

| Child Table | Column | References | Parent Table | Column | On Delete | Purpose |
|---|---|---|---|---|---|---|
| `card` | `account_id` | → | `account` | `account_id` | RESTRICT | Card belongs to account |
| `card_cross_reference` | `card_number` | → | `card` | `card_number` | RESTRICT | XREF maps to card |
| `card_cross_reference` | `account_id` | → | `account` | `account_id` | RESTRICT | XREF links account |
| `card_cross_reference` | `customer_id` | → | `customer` | `customer_id` | RESTRICT | XREF links customer |
| `transaction` | `card_number` | → | `card` | `card_number` | RESTRICT | Transaction charged to card |

> **Note:** `ON DELETE RESTRICT` is used instead of `CASCADE` because the legacy VSAM system has no cascading behavior. Deleting parent records should be explicitly managed during data migration.

---

## 3. Index Strategy

```mermaid
graph LR
    subgraph "Primary Key Indexes (Auto-created)"
        PK1[customer_pkey<br/>ON customer(customer_id)]
        PK2[account_pkey<br/>ON account(account_id)]
        PK3[card_pkey<br/>ON card(card_number)]
        PK4[card_cross_reference_pkey<br/>ON card_cross_reference(card_number)]
        PK5[transaction_pkey<br/>ON transaction(transaction_id)]
    end

    subgraph "Foreign Key Indexes"
        FK1[idx_card_account_id<br/>ON card(account_id)]
        FK2[idx_xref_account_id<br/>ON card_cross_reference(account_id)<br/>⚡ Replaces CXACAIX AIX]
        FK3[idx_xref_customer_id<br/>ON card_cross_reference(customer_id)]
        FK4[idx_transaction_card_number<br/>ON transaction(card_number)]
    end

    subgraph "Additional Indexes"
        IDX1[idx_customer_ssn<br/>ON customer(ssn)<br/>UNIQUE]
    end

    style FK2 fill:#ffcdd2
```

### Index Details

| Index Name | Table | Column(s) | Type | Legacy Equivalent | Purpose |
|---|---|---|---|---|---|
| `customer_pkey` | `customer` | `customer_id` | PRIMARY KEY | CUSTDAT KSDS primary key | Customer lookup |
| `account_pkey` | `account` | `account_id` | PRIMARY KEY | ACCTDAT KSDS primary key | Account lookup |
| `card_pkey` | `card` | `card_number` | PRIMARY KEY | CARDDAT KSDS primary key | Card lookup |
| `card_cross_reference_pkey` | `card_cross_reference` | `card_number` | PRIMARY KEY | CCXREF KSDS primary key | Card → Account resolution (Path B) |
| `idx_xref_account_id` | `card_cross_reference` | `account_id` | B-TREE | **CXACAIX Alternate Index** | Account → Card resolution (Path A) |
| `idx_xref_customer_id` | `card_cross_reference` | `customer_id` | B-TREE | N/A | Customer lookup via XREF |
| `transaction_pkey` | `transaction` | `transaction_id` | PRIMARY KEY | TRANSACT KSDS primary key | Transaction lookup |
| `idx_transaction_card_number` | `transaction` | `card_number` | B-TREE | N/A | Transactions by card |
| `idx_card_account_id` | `card` | `account_id` | B-TREE | N/A | Cards by account |
| `idx_customer_ssn` | `customer` | `ssn` | UNIQUE | N/A | SSN uniqueness |

---

## 4. Sequence (Transaction ID Generation)

```mermaid
sequenceDiagram
    participant App as Spring Boot
    participant SEQ as transaction_id_seq
    participant DB as transaction table

    App->>SEQ: SELECT nextval('transaction_id_seq')
    SEQ-->>App: 151
    App->>App: LPAD('151', 16, '0') → "0000000000000151"
    App->>DB: INSERT INTO transaction (transaction_id, ...) VALUES ('0000000000000151', ...)
```

**Sequence Definition:**
```sql
CREATE SEQUENCE transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE;
```

> **Design Decision:** PostgreSQL `SEQUENCE` replaces the legacy browse-to-end pattern (`STARTBR HIGH-VALUES → READPREV → +1`). Sequences are atomic and thread-safe, eliminating the race condition risk documented in BRE Section 10.1.

---

## 5. VSAM-to-PostgreSQL Mapping Summary

| VSAM File | Type | Record Length | PostgreSQL Table | Columns | PK |
|---|---|---|---|---|---|
| `TRANSACT` | KSDS | 350 bytes | `transaction` | 13 data + FILLER excluded | `transaction_id VARCHAR(16)` |
| `CCXREF` | KSDS | 50 bytes | `card_cross_reference` | 3 data + FILLER excluded | `card_number VARCHAR(16)` |
| `CXACAIX` | AIX on CCXREF | 50 bytes | *(index only)* `idx_xref_account_id` | N/A | N/A |
| `ACCTDAT` | KSDS | 300 bytes | `account` | 12 data + FILLER excluded | `account_id NUMERIC(11,0)` |
| `CARDDAT` | KSDS | 150 bytes | `card` | 6 data + FILLER excluded | `card_number VARCHAR(16)` |
| `CUSTDAT` | KSDS | 500 bytes | `customer` | 18 data + FILLER excluded | `customer_id NUMERIC(9,0)` |

---

## 6. Data Integrity Constraints

| Constraint | Table | Type | Business Rule |
|---|---|---|---|
| `transaction_pkey` | `transaction` | PRIMARY KEY | BR-AT-14: Duplicate ID rejection |
| `card_cross_reference_pkey` | `card_cross_reference` | PRIMARY KEY | BR-AT-04: Card number uniqueness in XREF |
| `fk_transaction_card` | `transaction` | FOREIGN KEY | Referential integrity (card must exist) |
| `fk_card_account` | `card` | FOREIGN KEY | Card must belong to valid account |
| `fk_xref_card` | `card_cross_reference` | FOREIGN KEY | XREF card must exist in card table |
| `fk_xref_account` | `card_cross_reference` | FOREIGN KEY | XREF account must exist in account table |
| `fk_xref_customer` | `card_cross_reference` | FOREIGN KEY | XREF customer must exist in customer table |
| `transaction_id_seq` | `transaction` | SEQUENCE | BR-AT-13: Thread-safe auto-increment |
| `idx_xref_account_id` | `card_cross_reference` | INDEX | BR-AT-05: Account → Card resolution |
| `customer_ssn_unique` | `customer` | UNIQUE INDEX | SSN uniqueness |

---

## 7. Table Creation Order (Dependency Graph)

Tables must be created in dependency order to satisfy foreign key constraints:

```mermaid
graph TD
    C[1. customer] --> XREF[4. card_cross_reference]
    A[2. account] --> CARD[3. card]
    A --> XREF
    CARD --> XREF
    CARD --> T[5. transaction]
    SEQ[0. transaction_id_seq] -.-> T

    style SEQ fill:#f3e5f5,stroke-dasharray: 5 5
    style C fill:#fff3e0
    style A fill:#e8f5e9
    style CARD fill:#e3f2fd
    style XREF fill:#fce4ec
    style T fill:#e1f5fe
```

**Creation Order:**
1. `transaction_id_seq` (sequence — no dependencies)
2. `customer` (no foreign keys)
3. `account` (no foreign keys)
4. `card` (depends on: `account`)
5. `card_cross_reference` (depends on: `card`, `account`, `customer`)
6. `transaction` (depends on: `card`)
