# CardDemo Application - Java Migration Plan

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Migration Strategy](#migration-strategy)
3. [Target Architecture](#target-architecture)
4. [COBOL-to-Java Mapping Guide](#cobol-to-java-mapping-guide)
5. [Database Schema Design](#database-schema-design)
6. [Program-to-API Mapping](#program-to-api-mapping)
7. [Batch Jobs Migration](#batch-jobs-migration)
8. [Data Migration Plan](#data-migration-plan)
9. [Risks and Mitigations](#risks-and-mitigations)
10. [Session-by-Session Migration Plan](#session-by-session-migration-plan)
11. [Developer Prompts Reference](#developer-prompts-reference)

---

## Executive Summary

This document provides a comprehensive plan for migrating the CardDemo mainframe application from COBOL/CICS/VSAM/JCL to a modern Java-based architecture. The migration follows a strangler-fig pattern, allowing incremental cutover while maintaining business continuity.

The CardDemo application consists of 31 COBOL programs, 30 copybooks, 17 BMS screen maps, and 37 JCL batch jobs. The migration will transform this into a Spring Boot application with REST APIs, Spring Batch for batch processing, and a modern web frontend replacing the 3270 terminal interface.

**Estimated Timeline:** 12 sessions (approximately 3-4 months with a dedicated team)

**Target Stack:**
- Java 17 with Spring Boot 3
- PostgreSQL for data persistence
- Spring Security with JWT for authentication
- Spring Batch for batch processing
- React or Angular for frontend
- Docker/Kubernetes for deployment

---

## Migration Strategy

### Approach: Strangler-Fig Pattern

The strangler-fig pattern allows gradual replacement of mainframe functionality without a big-bang cutover. New Java services are built alongside the existing system, with traffic incrementally shifted as each component is validated.

**Phase 1: Foundation (Sessions 1-3)**
- Project scaffolding and CI/CD setup
- Database schema design and migration scripts
- Data ingestion utilities for EBCDIC files

**Phase 2: Core Services (Sessions 4-6)**
- Authentication and security
- Account and card management APIs
- Transaction processing APIs

**Phase 3: Batch Processing (Sessions 7-8)**
- Transaction posting job (POSTTRAN equivalent)
- Interest calculation and statement generation

**Phase 4: Completion (Sessions 9-12)**
- Reports and admin tooling
- Web UI development
- Optional modules migration
- Testing, parity validation, and deployment

### Migration Principles

1. **Preserve Business Logic:** Replicate COBOL validation rules and processing logic exactly before optimizing
2. **Data Integrity First:** Validate data migration with checksums and sample comparisons
3. **Incremental Validation:** Test each component against mainframe outputs before proceeding
4. **Maintain Audit Trail:** Log all operations for compliance and debugging
5. **Zero Data Loss:** Ensure all VSAM records are accurately migrated with proper type conversions

---

## Target Architecture

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| Runtime | Java 17 | Primary language |
| Framework | Spring Boot 3.x | Application framework |
| Web | Spring Web MVC | REST API development |
| Data Access | Spring Data JPA | ORM and repositories |
| Database | PostgreSQL 15+ | Primary data store |
| Migrations | Flyway | Schema version control |
| Batch | Spring Batch 5.x | Batch job processing |
| Security | Spring Security 6.x | Authentication/authorization |
| API Docs | SpringDoc OpenAPI | API documentation |
| Frontend | React 18 / Next.js | Web user interface |
| Build | Maven or Gradle | Build automation |
| Containers | Docker | Containerization |
| Orchestration | Kubernetes / ECS | Container orchestration |

### Module Structure

```
carddemo-java/
├── carddemo-domain/           # Domain entities and value objects
├── carddemo-persistence/      # JPA repositories and database config
├── carddemo-services/         # Business logic services
├── carddemo-api/              # REST controllers and DTOs
├── carddemo-batch/            # Spring Batch jobs
├── carddemo-security/         # Authentication and authorization
├── carddemo-migration/        # EBCDIC data importers
├── carddemo-ui/               # React/Angular frontend
└── carddemo-infra/            # Docker, Helm, Terraform configs
```

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Load Balancer                             │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │  API     │   │  API     │   │  API     │
        │ Instance │   │ Instance │   │ Instance │
        └──────────┘   └──────────┘   └──────────┘
              │               │               │
              └───────────────┼───────────────┘
                              ▼
                    ┌──────────────────┐
                    │   PostgreSQL     │
                    │   (RDS/Aurora)   │
                    └──────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │  Batch   │   │  Batch   │   │  Batch   │
        │  Worker  │   │  Worker  │   │  Worker  │
        └──────────┘   └──────────┘   └──────────┘
```

---

## COBOL-to-Java Mapping Guide

### Data Type Mappings

| COBOL Type | Example | Java Type | Notes |
|------------|---------|-----------|-------|
| PIC 9(n) | PIC 9(09) | Long or BigInteger | Use Long for IDs up to 18 digits |
| PIC X(n) | PIC X(25) | String | Trim trailing spaces |
| PIC S9(n)V99 | PIC S9(10)V99 | BigDecimal | Scale = 2, use for all monetary values |
| PIC S9(n) COMP | PIC S9(9) COMP | int or long | Binary storage |
| PIC 9(n) COMP-3 | PIC 9(3) COMP-3 | BigDecimal | Packed decimal, handle sign nibble |
| Date fields | PIC X(10) | LocalDate | Parse YYYY-MM-DD format |
| Timestamp | PIC X(26) | LocalDateTime | Parse DB2 timestamp format |

### Architectural Pattern Mappings

| COBOL/CICS Concept | Java Equivalent |
|--------------------|-----------------|
| COMMAREA | Stateless REST; state in request/JWT |
| CICS Transaction | REST endpoint |
| BMS Map | React/Angular component |
| VSAM KSDS | JPA Entity + Repository |
| VSAM AIX | Database secondary index |
| Copybook | Java DTO/Entity class |
| JCL Job | Spring Batch Job |
| JCL Step | Spring Batch Step |
| PERFORM paragraph | Private method |
| CALL subprogram | Service method call |
| File Status | Exception handling |
| ABEND | Throw RuntimeException |

### CICS Command Mappings

| CICS Command | Java Equivalent |
|--------------|-----------------|
| EXEC CICS READ | repository.findById() |
| EXEC CICS WRITE | repository.save() |
| EXEC CICS REWRITE | repository.save() (update) |
| EXEC CICS DELETE | repository.delete() |
| EXEC CICS STARTBR/READNEXT | repository.findAll() with pagination |
| EXEC CICS SEND MAP | Return DTO from controller |
| EXEC CICS RECEIVE MAP | @RequestBody DTO parameter |
| EXEC CICS XCTL | Redirect or service call |
| EXEC CICS RETURN | Return from controller method |

---

## Database Schema Design

### Entity-Relationship Model

The relational schema normalizes the VSAM file structures while preserving the original key relationships.

### Flyway Migration: V1__baseline.sql

```sql
-- Customer table (from CVCUS01Y - 500 bytes)
CREATE TABLE customer (
    id BIGINT PRIMARY KEY,                    -- CUST-ID (9 digits)
    first_name VARCHAR(25) NOT NULL,          -- CUST-FIRST-NAME
    middle_name VARCHAR(25),                  -- CUST-MIDDLE-NAME
    last_name VARCHAR(25) NOT NULL,           -- CUST-LAST-NAME
    address_line_1 VARCHAR(50),               -- CUST-ADDR-LINE-1
    address_line_2 VARCHAR(50),               -- CUST-ADDR-LINE-2
    address_line_3 VARCHAR(50),               -- CUST-ADDR-LINE-3
    state_code VARCHAR(2),                    -- CUST-ADDR-STATE-CD
    country_code VARCHAR(3),                  -- CUST-ADDR-COUNTRY-CD
    zip_code VARCHAR(10),                     -- CUST-ADDR-ZIP
    phone_number_1 VARCHAR(15),               -- CUST-PHONE-NUM-1
    phone_number_2 VARCHAR(15),               -- CUST-PHONE-NUM-2
    ssn VARCHAR(9),                           -- CUST-SSN (encrypted in prod)
    govt_issued_id VARCHAR(20),               -- CUST-GOVT-ISSUED-ID
    date_of_birth DATE,                       -- CUST-DOB-YYYY-MM-DD
    eft_account_id VARCHAR(10),               -- CUST-EFT-ACCOUNT-ID
    primary_card_holder BOOLEAN DEFAULT FALSE,-- CUST-PRI-CARD-HOLDER-IND
    fico_score SMALLINT,                      -- CUST-FICO-CREDIT-SCORE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Account table (from CVACT01Y - 300 bytes)
CREATE TABLE account (
    id BIGINT PRIMARY KEY,                    -- ACCT-ID (11 digits)
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    active_status VARCHAR(1) NOT NULL,        -- ACCT-ACTIVE-STATUS
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0, -- ACCT-CURR-BAL
    credit_limit DECIMAL(12,2) NOT NULL,      -- ACCT-CREDIT-LIMIT
    cash_credit_limit DECIMAL(12,2),          -- ACCT-CASH-CREDIT-LIMIT
    open_date DATE NOT NULL,                  -- ACCT-OPEN-DATE
    expiration_date DATE NOT NULL,            -- ACCT-EXPIRAION-DATE
    reissue_date DATE,                        -- ACCT-REISSUE-DATE
    cycle_credit DECIMAL(12,2) DEFAULT 0,     -- ACCT-CURR-CYC-CREDIT
    cycle_debit DECIMAL(12,2) DEFAULT 0,      -- ACCT-CURR-CYC-DEBIT
    zip_code VARCHAR(10),                     -- ACCT-ADDR-ZIP
    group_id VARCHAR(10),                     -- ACCT-GROUP-ID
    version BIGINT DEFAULT 0,                 -- Optimistic locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_customer ON account(customer_id);
CREATE INDEX idx_account_expiration ON account(expiration_date);

-- Card table (from CVACT02Y - 150 bytes)
CREATE TABLE card (
    card_number VARCHAR(16) PRIMARY KEY,      -- CARD-NUM
    account_id BIGINT NOT NULL REFERENCES account(id),
    cvv VARCHAR(3) NOT NULL,                  -- CARD-CVV-CD
    embossed_name VARCHAR(50) NOT NULL,       -- CARD-EMBOSSED-NAME
    expiration_date DATE NOT NULL,            -- CARD-EXPIRAION-DATE
    active_status VARCHAR(1) NOT NULL,        -- CARD-ACTIVE-STATUS
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_card_account ON card(account_id);

-- Card Cross-Reference table (from CVACT03Y - 50 bytes)
CREATE TABLE card_xref (
    card_number VARCHAR(16) PRIMARY KEY REFERENCES card(card_number),
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    account_id BIGINT NOT NULL REFERENCES account(id)
);

CREATE INDEX idx_xref_customer ON card_xref(customer_id);
CREATE INDEX idx_xref_account ON card_xref(account_id);

-- Transaction table (from CVTRA05Y - 350 bytes)
CREATE TABLE transaction (
    id VARCHAR(16) PRIMARY KEY,               -- TRAN-ID
    type_code VARCHAR(2) NOT NULL,            -- TRAN-TYPE-CD
    category_code INTEGER NOT NULL,           -- TRAN-CAT-CD
    source VARCHAR(10),                       -- TRAN-SOURCE
    description VARCHAR(100),                 -- TRAN-DESC
    amount DECIMAL(11,2) NOT NULL,            -- TRAN-AMT
    merchant_id BIGINT,                       -- TRAN-MERCHANT-ID
    merchant_name VARCHAR(50),                -- TRAN-MERCHANT-NAME
    merchant_city VARCHAR(50),                -- TRAN-MERCHANT-CITY
    merchant_zip VARCHAR(10),                 -- TRAN-MERCHANT-ZIP
    card_number VARCHAR(16) NOT NULL REFERENCES card(card_number),
    account_id BIGINT NOT NULL REFERENCES account(id),
    originated_at TIMESTAMP NOT NULL,         -- TRAN-ORIG-TS
    processed_at TIMESTAMP,                   -- TRAN-PROC-TS
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transaction_account ON transaction(account_id);
CREATE INDEX idx_transaction_card ON transaction(card_number);
CREATE INDEX idx_transaction_processed ON transaction(processed_at);
CREATE INDEX idx_transaction_type_cat ON transaction(type_code, category_code);

-- Transaction Category Balance table (from CVTRA01Y - 50 bytes)
CREATE TABLE tcat_balance (
    account_id BIGINT NOT NULL REFERENCES account(id),
    type_code VARCHAR(2) NOT NULL,
    category_code INTEGER NOT NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, type_code, category_code)
);

-- User Security table (from CSUSR01Y - 80 bytes)
CREATE TABLE app_user (
    id SERIAL PRIMARY KEY,
    username VARCHAR(8) UNIQUE NOT NULL,      -- SEC-USR-ID
    first_name VARCHAR(20),                   -- SEC-USR-FNAME
    last_name VARCHAR(20),                    -- SEC-USR-LNAME
    password_hash VARCHAR(255) NOT NULL,      -- SEC-USR-PWD (bcrypt)
    user_type VARCHAR(1) NOT NULL,            -- SEC-USR-TYPE (A/U)
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Daily Transaction Rejects table (for batch processing)
CREATE TABLE daily_transaction_reject (
    id SERIAL PRIMARY KEY,
    original_record TEXT NOT NULL,
    rejection_code INTEGER NOT NULL,
    rejection_reason VARCHAR(100),
    batch_run_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transaction Types reference table (optional DB2 module)
CREATE TABLE transaction_type (
    type_code VARCHAR(2) PRIMARY KEY,
    description VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

-- Transaction Categories reference table
CREATE TABLE transaction_category (
    category_code INTEGER PRIMARY KEY,
    type_code VARCHAR(2) NOT NULL REFERENCES transaction_type(type_code),
    description VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

-- Disclosure Groups table (from CVTRA02Y - 50 bytes)
CREATE TABLE disclosure_group (
    group_id VARCHAR(10) PRIMARY KEY,
    description VARCHAR(40)
);
```

---

## Program-to-API Mapping

### Online Programs to REST Endpoints

| COBOL Program | Transaction | REST Endpoint | HTTP Method | Description |
|---------------|-------------|---------------|-------------|-------------|
| COSGN00C | CC00 | /api/auth/login | POST | User authentication |
| COMEN01C | CM00 | N/A | - | Replaced by UI routing |
| COADM01C | CA00 | N/A | - | Replaced by UI routing |
| COACTVWC | CAVW | /api/accounts/{id} | GET | View account details |
| COACTUPC | CAUP | /api/accounts/{id} | PUT | Update account |
| COCRDLIC | CCLI | /api/accounts/{id}/cards | GET | List cards for account |
| COCRDSLC | CCDL | /api/cards/{cardNumber} | GET | View card details |
| COCRDUPC | CCUP | /api/cards/{cardNumber} | PUT | Update card |
| COTRN00C | CT00 | /api/accounts/{id}/transactions | GET | List transactions |
| COTRN01C | CT01 | /api/transactions/{id} | GET | View transaction |
| COTRN02C | CT02 | /api/transactions | POST | Create transaction |
| CORPT00C | CR00 | /api/reports/transactions | GET | Transaction reports |
| COBIL00C | CB00 | /api/payments | POST | Process bill payment |
| COUSR00C | CU00 | /api/admin/users | GET | List users |
| COUSR01C | CU01 | /api/admin/users | POST | Create user |
| COUSR02C | CU02 | /api/admin/users/{id} | PUT | Update user |
| COUSR03C | CU03 | /api/admin/users/{id} | DELETE | Delete user |

### API Response Codes Mapping

| COBOL Condition | HTTP Status | Response Body |
|-----------------|-------------|---------------|
| Successful operation | 200 OK | Resource data |
| Record created | 201 Created | Created resource |
| Record not found (RESP=13) | 404 Not Found | Error message |
| Validation failure | 422 Unprocessable Entity | Validation errors |
| Duplicate key (RESP=14) | 409 Conflict | Error message |
| Authentication failure | 401 Unauthorized | Error message |
| Authorization failure | 403 Forbidden | Error message |
| System error | 500 Internal Server Error | Error details |

---

## Batch Jobs Migration

### Spring Batch Job Mappings

| JCL Job | COBOL Program | Spring Batch Job | Description |
|---------|---------------|------------------|-------------|
| POSTTRAN | CBTRN02C | postTransactionsJob | Daily transaction posting |
| INTCALC | CBACT04C | interestCalculationJob | Monthly interest calculation |
| CREASTMT | CBSTM03A/B | createStatementsJob | Statement generation |
| TRANREPT | CBTRN03C | transactionReportJob | Transaction reporting |
| CBEXPORT | CBEXPORT | dataExportJob | Branch migration export |
| CBIMPORT | CBIMPORT | dataImportJob | Branch migration import |

### postTransactionsJob Design (POSTTRAN Equivalent)

```
Job: postTransactionsJob
├── Step 1: readAndValidateTransactions
│   ├── Reader: JRecordFlatFileItemReader (DALYTRAN file, CVTRA06Y layout)
│   ├── Processor: TransactionValidationProcessor
│   │   ├── Lookup card in card_xref table
│   │   ├── Verify account exists and is active
│   │   ├── Check credit limit not exceeded
│   │   ├── Verify account not expired
│   │   └── Return validated transaction or rejection
│   └── Writer: CompositeItemWriter
│       ├── TransactionWriter (valid transactions)
│       ├── AccountBalanceWriter (update balances)
│       ├── TcatBalanceWriter (update category balances)
│       └── RejectionWriter (rejected transactions)
└── Step 2: generateSummaryReport
    └── Tasklet: SummaryReportTasklet
```

**Validation Codes (matching COBOL):**
- 100: Invalid card number (not found in xref)
- 101: Account record not found
- 102: Over-limit transaction
- 103: Transaction after account expiration

### interestCalculationJob Design (INTCALC Equivalent)

```
Job: interestCalculationJob
├── Step 1: calculateInterest
│   ├── Reader: JpaPagingItemReader (accounts with balance > 0)
│   ├── Processor: InterestCalculationProcessor
│   │   ├── Apply interest rate based on account type
│   │   ├── Calculate interest amount
│   │   └── Create interest transaction record
│   └── Writer: CompositeItemWriter
│       ├── AccountBalanceWriter (add interest to balance)
│       └── TransactionWriter (record interest charge)
└── Step 2: resetCycleCounters
    └── Tasklet: CycleResetTasklet
```

### createStatementsJob Design (CREASTMT Equivalent)

```
Job: createStatementsJob
├── Step 1: generateStatements
│   ├── Reader: JpaPagingItemReader (accounts with transactions in period)
│   ├── Processor: StatementGenerationProcessor
│   │   ├── Aggregate transactions by account
│   │   ├── Calculate totals and balances
│   │   └── Generate statement data
│   └── Writer: StatementWriter
│       ├── PDF generation (Flying Saucer + Thymeleaf)
│       ├── HTML generation
│       └── Store statement records
└── Step 2: notifyCustomers (optional)
    └── Tasklet: EmailNotificationTasklet
```

---

## Data Migration Plan

### EBCDIC File Parsing with JRecord

JRecord is a Java library that reads COBOL data files using copybook definitions. It handles EBCDIC encoding, packed decimals (COMP-3), and binary fields (COMP).

**Configuration for CardDemo files:**

```java
// Example: Reading CUSTDATA.PS using CVCUS01Y copybook
CobolCopybookLoader loader = new CobolCopybookLoader();
ExternalRecord externalRecord = loader.loadCopyBook(
    "app/cpy/CVCUS01Y.cpy",
    CopybookLoader.SPLIT_NONE,
    0,
    "cp037",  // EBCDIC code page
    Convert.FMT_MAINFRAME,
    0,
    null
);

AbstractLineReader reader = LineIOProvider.getInstance()
    .getLineReader(Constants.IO_FIXED_LENGTH, externalRecord);
reader.open("app/data/EBCDIC/AWS.M2.CARDDEMO.CUSTDATA.PS");
```

### Migration Sequence

1. **Customers** (CUSTDATA.PS → customer table)
2. **Accounts** (ACCTDATA.PS → account table)
3. **Cards** (CARDDATA.PS → card table)
4. **Cross-References** (CARDXREF.PS → card_xref table)
5. **Transactions** (TRANSACT.VSAM.KSDS → transaction table)
6. **Users** (USRSEC.PS → app_user table with bcrypt passwords)
7. **Reference Data** (TRANTYPE.PS, TRANCATG.PS, DISCGRP.PS)

### Validation Checklist

- [ ] Record counts match between VSAM and PostgreSQL
- [ ] All primary keys preserved without collision
- [ ] Monetary amounts converted correctly (COMP-3 to BigDecimal)
- [ ] Dates parsed correctly (handle various formats)
- [ ] Character encoding correct (no garbled text)
- [ ] Foreign key relationships valid
- [ ] Sample records verified manually

---

## Risks and Mitigations

### Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Packed decimal precision loss | Financial discrepancies | Use BigDecimal with scale=2; extensive unit tests |
| EBCDIC encoding issues | Data corruption | Verify charset cp037; sample validation |
| Concurrent balance updates | Race conditions | Optimistic locking with version field; serialized updates per account |
| Batch job failures | Data inconsistency | Transactional chunks; restart capability; idempotent operations |
| Performance degradation | User experience | Load testing; database indexing; connection pooling |

### Business Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Business logic differences | Incorrect processing | Parity testing with golden datasets |
| Missing edge cases | Production failures | Comprehensive test coverage; parallel running |
| Compliance gaps | Regulatory issues | Audit logging; data encryption; access controls |
| User adoption | Productivity loss | Training; familiar UI patterns; documentation |

---

## Session-by-Session Migration Plan

### Session 1: Foundation and Project Scaffolding

**Objectives:**
- Establish Java project structure
- Configure CI/CD pipeline
- Create baseline database schema

**Prompts for Devin:**

```
Create a new Spring Boot 3.2 multi-module Maven project named "carddemo-java" with the following modules:
- carddemo-domain (entities and value objects)
- carddemo-persistence (JPA repositories)
- carddemo-services (business logic)
- carddemo-api (REST controllers)
- carddemo-batch (Spring Batch jobs)
- carddemo-migration (data importers)

Add these dependencies to the parent POM:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-boot-starter-security
- spring-batch-core
- flyway-core
- postgresql driver
- lombok
- mapstruct
- springdoc-openapi-starter-webmvc-ui
- jrecord (net.sf.JRecord:JRecordV1)

Configure application.yml for PostgreSQL datasource and Flyway.
Create Dockerfile for the application.
Set up GitHub Actions workflow for build and test.
```

```
Create Flyway migration V1__baseline.sql with the complete database schema for CardDemo including:
- customer table (matching CVCUS01Y copybook)
- account table (matching CVACT01Y copybook)
- card table (matching CVACT02Y copybook)
- card_xref table (matching CVACT03Y copybook)
- transaction table (matching CVTRA05Y copybook)
- tcat_balance table (matching CVTRA01Y copybook)
- app_user table (matching CSUSR01Y copybook)
- daily_transaction_reject table for batch rejects
- transaction_type and transaction_category reference tables

Include appropriate indexes for foreign keys and frequently queried columns.
Add version column to account table for optimistic locking.
```

---

### Session 2: Domain Entities and Persistence Layer

**Objectives:**
- Create JPA entities for all tables
- Implement Spring Data repositories
- Add seed data for testing

**Prompts for Devin:**

```
Create JPA entity classes in carddemo-domain module for:

1. Customer entity with fields matching CVCUS01Y:
   - id (Long, 9 digits)
   - firstName, middleName, lastName (String)
   - addressLine1, addressLine2, addressLine3 (String)
   - stateCode, countryCode, zipCode (String)
   - phoneNumber1, phoneNumber2 (String)
   - ssn, govtIssuedId (String)
   - dateOfBirth (LocalDate)
   - eftAccountId (String)
   - primaryCardHolder (Boolean)
   - ficoScore (Integer)
   - createdAt, updatedAt (LocalDateTime)

2. Account entity with fields matching CVACT01Y:
   - id (Long, 11 digits)
   - customer (ManyToOne relationship)
   - activeStatus (String, 1 char)
   - currentBalance, creditLimit, cashCreditLimit (BigDecimal scale=2)
   - openDate, expirationDate, reissueDate (LocalDate)
   - cycleCredit, cycleDebit (BigDecimal scale=2)
   - zipCode, groupId (String)
   - version (Long for optimistic locking)

3. Card entity with fields matching CVACT02Y:
   - cardNumber (String, 16 chars, primary key)
   - account (ManyToOne relationship)
   - cvv (String, 3 chars)
   - embossedName (String, 50 chars)
   - expirationDate (LocalDate)
   - activeStatus (String, 1 char)

4. CardXref entity with fields matching CVACT03Y
5. Transaction entity with fields matching CVTRA05Y
6. TcatBalance entity with composite key
7. AppUser entity with fields matching CSUSR01Y

Use Lombok annotations (@Data, @Entity, @Table).
Add @Table annotations with appropriate indexes.
```

```
Create Spring Data JPA repositories in carddemo-persistence module:

1. CustomerRepository with methods:
   - findByLastNameContaining(String lastName)
   - findBySsn(String ssn)

2. AccountRepository with methods:
   - findByCustomerId(Long customerId)
   - findByExpirationDateBefore(LocalDate date)
   - findActiveAccountsWithBalance()

3. CardRepository with methods:
   - findByAccountId(Long accountId)
   - findByCardNumberStartingWith(String prefix)

4. CardXrefRepository with methods:
   - findByCustomerId(Long customerId)
   - findByAccountId(Long accountId)

5. TransactionRepository with methods:
   - findByAccountIdOrderByProcessedAtDesc(Long accountId, Pageable pageable)
   - findByCardNumberAndProcessedAtBetween(String cardNumber, LocalDateTime start, LocalDateTime end)
   - findByTypeCodeAndCategoryCode(String typeCode, Integer categoryCode)

6. TcatBalanceRepository
7. AppUserRepository with findByUsername(String username)

Create Flyway migration V2__seed_test_data.sql with minimal test data.
```

---

### Session 3: EBCDIC Data Ingestion Utilities

**Objectives:**
- Build JRecord-based file parsers
- Implement data import services
- Validate against source files

**Prompts for Devin:**

```
Create data import utilities in carddemo-migration module using JRecord library:

1. Create CopybookParser utility class that:
   - Loads COBOL copybook definitions from app/cpy/ directory
   - Configures EBCDIC charset (cp037)
   - Handles fixed-length record format
   - Converts COMP and COMP-3 fields to Java types

2. Create CustomerImportService that:
   - Reads AWS.M2.CARDDEMO.CUSTDATA.PS using CVCUS01Y copybook
   - Maps EBCDIC fields to Customer entity
   - Batch inserts with chunk size 1000
   - Handles duplicate key conflicts (upsert)
   - Logs import statistics

3. Create similar import services for:
   - AccountImportService (ACCTDATA.PS, CVACT01Y)
   - CardImportService (CARDDATA.PS, CVACT02Y)
   - CardXrefImportService (CARDXREF.PS, CVACT03Y)
   - TransactionImportService (TRANSACT files, CVTRA05Y)
   - UserImportService (USRSEC.PS, CSUSR01Y) - generate bcrypt hashes

4. Create ImportCommandLineRunner that accepts arguments:
   --dataset=CUSTDATA|ACCTDATA|CARDDATA|XREFDATA|TRANSACT|USRSEC
   --file=/path/to/file
   --validate-only (dry run mode)

5. Write integration tests that:
   - Read sample records from app/data/EBCDIC/
   - Verify record counts
   - Validate specific field values for known records
   - Check COMP-3 decimal conversion accuracy
```

---

### Session 4: Authentication and Security

**Objectives:**
- Implement Spring Security configuration
- Create login endpoint (replacing COSGN00C)
- Set up JWT token authentication

**Prompts for Devin:**

```
Implement authentication in carddemo-security module:

1. Create SecurityConfig class with:
   - JWT-based stateless authentication
   - Password encoding with BCrypt
   - Public endpoints: /api/auth/login, /api/health, /swagger-ui/**
   - Role-based access: ADMIN for /api/admin/**, USER for other endpoints
   - CORS configuration for frontend

2. Create AuthController with POST /api/auth/login endpoint:
   - Request body: { "username": "string", "password": "string" }
   - Validate credentials against app_user table
   - Return JWT token with user details and role
   - Map user_type 'A' to ROLE_ADMIN, 'U' to ROLE_USER

3. Create JwtTokenProvider utility:
   - Generate JWT with configurable expiration
   - Validate and parse JWT tokens
   - Extract username and roles from token

4. Create UserDetailsService implementation:
   - Load user from AppUserRepository
   - Map to Spring Security UserDetails

5. Create AuthenticationResponse DTO:
   - token (String)
   - username (String)
   - userType (String)
   - expiresAt (LocalDateTime)

6. Migrate users from USRSEC.PS:
   - Convert plaintext passwords to bcrypt hashes
   - Preserve user IDs and types
   - Default users: ADMIN001/PASSWORD (admin), USER0001/PASSWORD (user)
```

---

### Session 5: Account and Card Services

**Objectives:**
- Implement account CRUD operations (CAVW/CAUP equivalents)
- Implement card CRUD operations (CCLI/CCDL/CCUP equivalents)
- Add validation matching COBOL business rules

**Prompts for Devin:**

```
Implement account and card services in carddemo-services and carddemo-api modules:

1. Create AccountService with methods:
   - getAccountById(Long id) - throws NotFoundException if not found
   - getAccountsByCustomerId(Long customerId)
   - updateAccount(Long id, AccountUpdateRequest request)
   - Validation: active status must be 'Y' or 'N', dates must be valid

2. Create AccountController with endpoints:
   - GET /api/accounts/{id} - returns AccountResponse DTO
   - PUT /api/accounts/{id} - accepts AccountUpdateRequest, returns updated AccountResponse
   - GET /api/customers/{customerId}/accounts - returns list of accounts

3. Create CardService with methods:
   - getCardByNumber(String cardNumber)
   - getCardsByAccountId(Long accountId)
   - updateCard(String cardNumber, CardUpdateRequest request)
   - Validation: card number format, expiration date not in past for new cards

4. Create CardController with endpoints:
   - GET /api/cards/{cardNumber}
   - PUT /api/cards/{cardNumber}
   - GET /api/accounts/{accountId}/cards

5. Create DTOs:
   - AccountResponse, AccountUpdateRequest
   - CardResponse, CardUpdateRequest
   - Use MapStruct for entity-DTO mapping

6. Add OpenAPI annotations for Swagger documentation

7. Write unit tests for services and integration tests for controllers
```

---

### Session 6: Transaction Services and Business Rules

**Objectives:**
- Implement transaction APIs (CT00/CT01/CT02 equivalents)
- Replicate CBTRN02C validation logic
- Handle balance updates atomically

**Prompts for Devin:**

```
Implement transaction services matching COBOL business logic:

1. Create TransactionService with methods:

   getTransactionById(String id):
   - Return transaction details or throw NotFoundException

   getTransactionsByAccountId(Long accountId, Pageable pageable):
   - Return paginated transactions ordered by processed date desc

   createTransaction(TransactionCreateRequest request):
   - Implement validation matching CBTRN02C:
     a) Lookup card_xref by cardNumber - reject with code 100 if not found
     b) Load account by xref.accountId - reject with code 101 if not found
     c) Calculate tempBalance = cycleCredit - cycleDebit + amount
     d) Reject with code 102 if tempBalance > creditLimit
     e) Reject with code 103 if account.expirationDate < transaction.originatedAt
   - On success (within @Transactional):
     a) Create transaction record with generated ID
     b) Update account: currentBalance += amount, cycleCredit or cycleDebit += amount
     c) Update or create tcat_balance record
     d) Set processedAt to current timestamp
   - Return created transaction

2. Create TransactionController with endpoints:
   - GET /api/transactions/{id}
   - GET /api/accounts/{accountId}/transactions?page=0&size=20
   - POST /api/transactions

3. Create TransactionCreateRequest DTO with fields:
   - cardNumber, typeCode, categoryCode, source, description
   - amount (BigDecimal), merchantId, merchantName, merchantCity, merchantZip
   - originatedAt (LocalDateTime)

4. Create TransactionValidationException with:
   - rejectionCode (100, 101, 102, 103)
   - rejectionReason (String)

5. Create GlobalExceptionHandler to map exceptions to HTTP responses:
   - TransactionValidationException -> 422 with rejection details
   - NotFoundException -> 404
   - OptimisticLockingFailureException -> 409 (retry)

6. Write comprehensive tests including:
   - Valid transaction creation
   - Each rejection scenario (100, 101, 102, 103)
   - Concurrent transaction handling
```

---

### Session 7: Batch Processing - Transaction Posting Job

**Objectives:**
- Implement Spring Batch job equivalent to POSTTRAN
- Configure JRecord reader for DALYTRAN file
- Handle rejections and statistics

**Prompts for Devin:**

```
Implement postTransactionsJob in carddemo-batch module:

1. Create DailyTransactionDTO matching CVTRA06Y copybook:
   - id, typeCode, categoryCode, source, description
   - amount (BigDecimal), merchantId, merchantName, merchantCity, merchantZip
   - cardNumber, originatedAt, processedAt

2. Create JRecordItemReader<DailyTransactionDTO>:
   - Configure with CVTRA06Y copybook
   - EBCDIC charset cp037
   - Fixed length 350 bytes
   - Map COMP-3 amount field correctly

3. Create TransactionValidationProcessor:
   - Implement ItemProcessor<DailyTransactionDTO, ValidatedTransaction>
   - Reuse validation logic from TransactionService
   - Return null for rejected items (handled by skip listener)
   - Wrap rejections in custom exception for listener

4. Create TransactionItemWriter:
   - Implement ItemWriter<ValidatedTransaction>
   - Within transaction: save transaction, update account, update tcat_balance

5. Create RejectionItemWriter:
   - Write rejected transactions to daily_transaction_reject table
   - Include rejection code and reason

6. Create PostTransactionsJobConfig:
   - Define job with single step
   - Chunk size: 500
   - Skip policy: skip validation failures, limit 10000
   - Retry policy: retry database errors 3 times
   - Listeners for logging and statistics

7. Create job parameters:
   - inputFile: path to DALYTRAN file
   - runDate: processing date
   - batchRunId: unique identifier

8. Create JobLauncherController:
   - POST /api/batch/post-transactions
   - Accept file path and run date
   - Return job execution ID

9. Write integration tests:
   - Process sample DALYTRAN file
   - Verify transaction counts
   - Verify rejection handling
   - Verify balance updates
```

---

### Session 8: Interest Calculation and Statement Generation

**Objectives:**
- Implement interest calculation job (INTCALC equivalent)
- Implement statement generation job (CREASTMT equivalent)
- Configure job scheduling

**Prompts for Devin:**

```
Implement interestCalculationJob:

1. Create InterestCalculationProcessor:
   - Read accounts with currentBalance > 0
   - Apply configurable interest rate (e.g., 18% APR / 12 months)
   - Calculate interest amount
   - Create interest transaction record

2. Create InterestCalculationWriter:
   - Update account.currentBalance += interestAmount
   - Save interest transaction
   - Reset cycle counters if end of billing cycle

3. Create InterestCalculationJobConfig:
   - Chunk size: 100
   - Job parameters: runDate, interestRate

Implement createStatementsJob:

1. Create StatementData DTO:
   - accountId, customerId
   - statementPeriodStart, statementPeriodEnd
   - openingBalance, closingBalance
   - totalCredits, totalDebits
   - transactions list
   - minimumPaymentDue, paymentDueDate

2. Create StatementGenerationProcessor:
   - Aggregate transactions for account in period
   - Calculate statement totals
   - Determine minimum payment (e.g., 2% of balance or $25 minimum)

3. Create StatementWriter:
   - Generate PDF using Thymeleaf template + Flying Saucer
   - Generate HTML version
   - Store statement record with file paths
   - Optionally upload to S3

4. Create statement Thymeleaf template:
   - Header with customer and account info
   - Transaction table
   - Summary section
   - Payment information

5. Create StatementJobConfig:
   - Chunk size: 50
   - Job parameters: statementMonth, statementYear

6. Create scheduling configuration:
   - Interest calculation: 1st of each month
   - Statement generation: 5th of each month
   - Use @Scheduled or Quartz for scheduling
```

---

### Session 9: Reports and Admin Tooling

**Objectives:**
- Implement transaction reports (CORPT00C equivalent)
- Implement user management APIs (COUSR00C-03C equivalents)
- Add audit logging

**Prompts for Devin:**

```
Implement reporting and admin features:

1. Create ReportService with methods:
   - generateTransactionReport(ReportCriteria criteria)
   - Criteria: accountId, dateRange, typeCode, categoryCode, minAmount, maxAmount
   - Return paginated results or export to CSV/JSON

2. Create ReportController:
   - GET /api/reports/transactions?accountId=&startDate=&endDate=&format=json|csv
   - Support pagination for JSON
   - Stream CSV for large exports

3. Create UserManagementService:
   - listUsers(Pageable pageable)
   - getUserById(Long id)
   - createUser(UserCreateRequest request) - hash password with bcrypt
   - updateUser(Long id, UserUpdateRequest request)
   - deleteUser(Long id) - soft delete (set enabled=false)
   - resetPassword(Long id, String newPassword)

4. Create AdminController:
   - GET /api/admin/users
   - GET /api/admin/users/{id}
   - POST /api/admin/users
   - PUT /api/admin/users/{id}
   - DELETE /api/admin/users/{id}
   - POST /api/admin/users/{id}/reset-password
   - Require ROLE_ADMIN for all endpoints

5. Create AuditLog entity and service:
   - Track user actions: login, logout, create, update, delete
   - Fields: userId, action, entityType, entityId, timestamp, details
   - Use Spring AOP or event listeners

6. Create AuditController:
   - GET /api/admin/audit-logs?userId=&action=&startDate=&endDate=
   - Admin only access
```

---

### Session 10: Web UI Development

**Objectives:**
- Create React frontend replacing BMS screens
- Implement authentication flow
- Build main application screens

**Prompts for Devin:**

```
Create React frontend in carddemo-ui module:

1. Initialize React 18 project with:
   - TypeScript
   - React Router for navigation
   - Axios for API calls
   - React Query for data fetching
   - Tailwind CSS or Material-UI for styling
   - React Hook Form for forms

2. Create authentication:
   - Login page matching COSGN00 screen layout
   - JWT token storage in localStorage
   - Axios interceptor for Authorization header
   - Protected route wrapper

3. Create layout components:
   - Header with app title, user info, logout
   - Sidebar navigation (matching menu options)
   - Footer with version info

4. Create pages:
   - Dashboard (landing after login)
   - Account List and Detail pages
   - Card List and Detail pages
   - Transaction List and Detail pages
   - Transaction Add form
   - Reports page with filters
   - Admin: User List, Add, Edit pages

5. Create reusable components:
   - DataTable with pagination and sorting
   - Form inputs with validation
   - Error message display (matching BMS ERRMSG)
   - Loading indicators
   - Confirmation dialogs

6. Implement form validations matching COBOL rules:
   - Required fields
   - Format validations (card number, dates)
   - Business rule validations (credit limit)

7. Add error handling:
   - API error responses to user-friendly messages
   - Network error handling
   - Session expiration handling

8. Configure proxy for development:
   - Proxy API calls to Spring Boot backend
```

---

### Session 11: Optional Modules Migration

**Objectives:**
- Migrate IMS/DB2/MQ integrations if needed
- Implement transaction type management
- Add message queue integration

**Prompts for Devin:**

```
Implement optional module integrations:

1. Transaction Type Management (DB2 module equivalent):
   - Create TransactionTypeService for CRUD operations
   - Create TransactionCategoryService
   - Create admin endpoints:
     - GET/POST/PUT/DELETE /api/admin/transaction-types
     - GET/POST/PUT/DELETE /api/admin/transaction-categories
   - Create batch job to sync reference data (TRANEXTR equivalent)

2. Message Queue Integration (MQ module equivalent):
   - Add spring-boot-starter-activemq or spring-jms dependency
   - Create JMS configuration for ActiveMQ or IBM MQ
   - Create message DTOs matching copybook layouts
   - Implement listeners:
     - DateInquiryListener (CDRD equivalent)
     - AccountInquiryListener (CDRA equivalent)
   - Implement producers for responses

3. Authorization Processing (IMS-DB2-MQ module equivalent):
   - Create PendingAuthorization entity
   - Create AuthorizationService:
     - processAuthorizationRequest()
     - getPendingAuthorizations()
     - approveAuthorization()
     - rejectAuthorization()
   - Create batch job for purging expired authorizations (CBPAUP0J equivalent)
   - Create endpoints:
     - GET /api/authorizations/pending
     - POST /api/authorizations/{id}/approve
     - POST /api/authorizations/{id}/reject

4. If not using MQ, create REST alternatives:
   - POST /api/system/date - return system date
   - GET /api/accounts/{id}/details - detailed account inquiry
```

---

### Session 12: Testing, Parity Validation, and Deployment

**Objectives:**
- Comprehensive testing against mainframe outputs
- Performance testing
- Production deployment setup

**Prompts for Devin:**

```
Implement testing and deployment:

1. Parity Testing:
   - Create ParityTestSuite that:
     - Loads golden EBCDIC input files
     - Runs Java batch jobs
     - Compares outputs to COBOL-generated files
     - Reports discrepancies
   - Test cases:
     - Transaction posting: verify balances match
     - Interest calculation: verify amounts match
     - Statement generation: verify totals match

2. Integration Tests:
   - End-to-end API tests with Testcontainers (PostgreSQL)
   - Batch job tests with sample data
   - Authentication flow tests
   - Concurrent transaction tests

3. Performance Tests:
   - Use Gatling or JMeter
   - Scenarios:
     - 100 concurrent users browsing accounts
     - 50 concurrent transaction submissions
     - Batch job with 100,000 transactions
   - Establish baseline metrics

4. Deployment Configuration:
   - Create Dockerfile with multi-stage build
   - Create docker-compose.yml for local development
   - Create Kubernetes manifests or Helm chart:
     - Deployment for API
     - Deployment for batch workers
     - Service and Ingress
     - ConfigMap and Secrets
     - HorizontalPodAutoscaler
   - Create Terraform for AWS:
     - RDS PostgreSQL
     - ECS or EKS cluster
     - ALB load balancer
     - S3 for statements
     - CloudWatch for logging

5. CI/CD Pipeline:
   - Build and test on PR
   - Build Docker image on merge
   - Deploy to staging automatically
   - Manual approval for production
   - Database migration as part of deployment

6. Monitoring and Observability:
   - Add Micrometer metrics
   - Configure OpenTelemetry tracing
   - Create CloudWatch dashboards
   - Set up alerts for errors and latency

7. Documentation:
   - API documentation (OpenAPI/Swagger)
   - Deployment runbook
   - Troubleshooting guide
   - Data migration procedures
```

---

## Developer Prompts Reference

### Quick Reference Prompts

**Create JPA Entity from Copybook:**
```
Generate a JPA entity for [EntityName] matching the COBOL copybook [CopybookName].cpy with these fields:
[List fields with COBOL types]
Use appropriate Java types: PIC 9(n) -> Long, PIC X(n) -> String, PIC S9(n)V99 -> BigDecimal.
Add @Entity, @Table with indexes, @Id, and relationship annotations.
```

**Implement Service Method with COBOL Logic:**
```
Implement [methodName] in [ServiceName] that replicates the logic from COBOL program [ProgramName].cbl:
[Describe the business logic]
Include validation, error handling, and transactional boundaries.
```

**Create Spring Batch Reader for EBCDIC File:**
```
Create a JRecord-based FlatFileItemReader for [FileName] using copybook [CopybookName].cpy:
- EBCDIC charset cp037
- Fixed length [N] bytes
- Map COMP-3 fields to BigDecimal
- Return [DTOName] objects
```

**Implement REST Endpoint Matching CICS Transaction:**
```
Create REST endpoint [HTTP Method] [Path] equivalent to CICS transaction [TranCode] (program [ProgramName]):
- Request body/params: [describe]
- Response: [describe]
- Validations: [list validations from COBOL]
- Error responses: [map COBOL conditions to HTTP status codes]
```

**Create Batch Job Matching JCL:**
```
Create Spring Batch job [jobName] equivalent to JCL job [JobName] running program [ProgramName]:
- Input: [describe input files/tables]
- Processing: [describe transformation logic]
- Output: [describe output files/tables]
- Error handling: [describe rejection/retry logic]
```

---

## Appendix: File Mappings

### Source to Target Mapping

| COBOL Source | Java Target |
|--------------|-------------|
| app/cbl/COSGN00C.cbl | AuthController.java |
| app/cbl/COACTVWC.cbl | AccountController.getAccount() |
| app/cbl/COACTUPC.cbl | AccountController.updateAccount() |
| app/cbl/COCRDLIC.cbl | CardController.getCardsByAccount() |
| app/cbl/COCRDSLC.cbl | CardController.getCard() |
| app/cbl/COCRDUPC.cbl | CardController.updateCard() |
| app/cbl/COTRN00C.cbl | TransactionController.getTransactions() |
| app/cbl/COTRN01C.cbl | TransactionController.getTransaction() |
| app/cbl/COTRN02C.cbl | TransactionController.createTransaction() |
| app/cbl/CBTRN02C.cbl | PostTransactionsJob |
| app/cbl/CBACT04C.cbl | InterestCalculationJob |
| app/cbl/CBSTM03A.cbl | CreateStatementsJob |
| app/cpy/CVCUS01Y.cpy | Customer.java |
| app/cpy/CVACT01Y.cpy | Account.java |
| app/cpy/CVACT02Y.cpy | Card.java |
| app/cpy/CVACT03Y.cpy | CardXref.java |
| app/cpy/CVTRA05Y.cpy | Transaction.java |
| app/cpy/CSUSR01Y.cpy | AppUser.java |

---

## Document Information

**Version:** 1.0  
**Last Updated:** February 2026  
**Target Application Version:** CardDemo v2.0  
**Target Java Version:** Java 17 with Spring Boot 3.2

For questions or contributions, please refer to the [CONTRIBUTING.md](../CONTRIBUTING.md) file in the repository root.
