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

## Extended Prompts Library

This section provides comprehensive, copy-paste ready prompts for each major migration task. Each prompt is designed to be self-contained and can be used in a separate Devin session.

### Session 1 Prompts: Project Foundation

**Prompt 1.1: Initialize Spring Boot Multi-Module Project**
```
Create a new Spring Boot 3.2 multi-module Maven project for migrating the CardDemo mainframe application to Java.

Project structure:
- Parent POM: carddemo-java (packaging: pom)
- Modules:
  - carddemo-domain: JPA entities and value objects
  - carddemo-persistence: Spring Data repositories and database configuration
  - carddemo-services: Business logic and service layer
  - carddemo-api: REST controllers, DTOs, and API configuration
  - carddemo-batch: Spring Batch jobs for batch processing
  - carddemo-security: Spring Security configuration and JWT handling
  - carddemo-migration: EBCDIC data import utilities

Parent POM dependencies to include:
- spring-boot-starter-parent: 3.2.0
- Java version: 17

Module-specific dependencies:
- carddemo-domain: jakarta.persistence-api, lombok, jakarta.validation-api
- carddemo-persistence: spring-boot-starter-data-jpa, postgresql, flyway-core
- carddemo-services: spring-boot-starter-validation (depends on domain, persistence)
- carddemo-api: spring-boot-starter-web, springdoc-openapi-starter-webmvc-ui:2.3.0, mapstruct:1.5.5.Final (depends on services, security)
- carddemo-batch: spring-boot-starter-batch, net.sf.JRecord:JRecordV1:0.90.4 (depends on services)
- carddemo-security: spring-boot-starter-security, jjwt-api:0.12.3, jjwt-impl, jjwt-jackson (depends on persistence)
- carddemo-migration: net.sf.JRecord:JRecordV1:0.90.4 (depends on persistence)

Create application.yml in carddemo-api with:
- Server port: 8080
- PostgreSQL datasource configuration (use environment variables)
- Flyway enabled with baseline-on-migrate: true
- JPA hibernate ddl-auto: validate
- Logging level: INFO for application, DEBUG for SQL

Create application-dev.yml with local PostgreSQL settings:
- URL: jdbc:postgresql://localhost:5432/carddemo
- Username: carddemo
- Password: carddemo

Generate .gitignore for Java/Maven project.
```

**Prompt 1.2: Create Flyway Baseline Migration**
```
Create Flyway migration V1__baseline_schema.sql for the CardDemo application database schema.

Tables to create (based on COBOL copybooks):

1. customer (from CVCUS01Y.cpy - 500 bytes):
   - id: BIGINT PRIMARY KEY (maps to CUST-ID, 9 digits)
   - first_name: VARCHAR(25) NOT NULL
   - middle_name: VARCHAR(25)
   - last_name: VARCHAR(25) NOT NULL
   - address_line_1, address_line_2, address_line_3: VARCHAR(50)
   - state_code: VARCHAR(2)
   - country_code: VARCHAR(3)
   - zip_code: VARCHAR(10)
   - phone_number_1, phone_number_2: VARCHAR(15)
   - ssn: VARCHAR(9)
   - govt_issued_id: VARCHAR(20)
   - date_of_birth: DATE
   - eft_account_id: VARCHAR(10)
   - primary_card_holder: BOOLEAN DEFAULT FALSE
   - fico_score: SMALLINT
   - created_at, updated_at: TIMESTAMP with defaults

2. account (from CVACT01Y.cpy - 300 bytes):
   - id: BIGINT PRIMARY KEY (maps to ACCT-ID, 11 digits)
   - customer_id: BIGINT NOT NULL REFERENCES customer(id)
   - active_status: VARCHAR(1) NOT NULL
   - current_balance: DECIMAL(12,2) NOT NULL DEFAULT 0
   - credit_limit: DECIMAL(12,2) NOT NULL
   - cash_credit_limit: DECIMAL(12,2)
   - open_date: DATE NOT NULL
   - expiration_date: DATE NOT NULL
   - reissue_date: DATE
   - cycle_credit, cycle_debit: DECIMAL(12,2) DEFAULT 0
   - zip_code: VARCHAR(10)
   - group_id: VARCHAR(10)
   - version: BIGINT DEFAULT 0 (for optimistic locking)
   - created_at, updated_at: TIMESTAMP
   - Indexes: customer_id, expiration_date

3. card (from CVACT02Y.cpy - 150 bytes):
   - card_number: VARCHAR(16) PRIMARY KEY
   - account_id: BIGINT NOT NULL REFERENCES account(id)
   - cvv: VARCHAR(3) NOT NULL
   - embossed_name: VARCHAR(50) NOT NULL
   - expiration_date: DATE NOT NULL
   - active_status: VARCHAR(1) NOT NULL
   - created_at, updated_at: TIMESTAMP
   - Index: account_id

4. card_xref (from CVACT03Y.cpy - 50 bytes):
   - card_number: VARCHAR(16) PRIMARY KEY REFERENCES card(card_number)
   - customer_id: BIGINT NOT NULL REFERENCES customer(id)
   - account_id: BIGINT NOT NULL REFERENCES account(id)
   - Indexes: customer_id, account_id

5. transaction (from CVTRA05Y.cpy - 350 bytes):
   - id: VARCHAR(16) PRIMARY KEY
   - type_code: VARCHAR(2) NOT NULL
   - category_code: INTEGER NOT NULL
   - source: VARCHAR(10)
   - description: VARCHAR(100)
   - amount: DECIMAL(11,2) NOT NULL
   - merchant_id: BIGINT
   - merchant_name: VARCHAR(50)
   - merchant_city: VARCHAR(50)
   - merchant_zip: VARCHAR(10)
   - card_number: VARCHAR(16) NOT NULL REFERENCES card(card_number)
   - account_id: BIGINT NOT NULL REFERENCES account(id)
   - originated_at: TIMESTAMP NOT NULL
   - processed_at: TIMESTAMP
   - created_at: TIMESTAMP
   - Indexes: account_id, card_number, processed_at, (type_code, category_code)

6. tcat_balance (transaction category balance):
   - Composite PK: (account_id, type_code, category_code)
   - balance: DECIMAL(12,2) NOT NULL DEFAULT 0
   - updated_at: TIMESTAMP

7. app_user (from CSUSR01Y.cpy - 80 bytes):
   - id: SERIAL PRIMARY KEY
   - username: VARCHAR(8) UNIQUE NOT NULL
   - first_name, last_name: VARCHAR(20)
   - password_hash: VARCHAR(255) NOT NULL
   - user_type: VARCHAR(1) NOT NULL (A=Admin, U=User)
   - enabled: BOOLEAN DEFAULT TRUE
   - created_at, updated_at: TIMESTAMP

8. daily_transaction_reject (for batch processing):
   - id: SERIAL PRIMARY KEY
   - original_record: TEXT NOT NULL
   - rejection_code: INTEGER NOT NULL
   - rejection_reason: VARCHAR(100)
   - batch_run_id: VARCHAR(50)
   - created_at: TIMESTAMP

9. transaction_type (reference data):
   - type_code: VARCHAR(2) PRIMARY KEY
   - description: VARCHAR(50) NOT NULL
   - active: BOOLEAN DEFAULT TRUE

10. transaction_category (reference data):
    - category_code: INTEGER PRIMARY KEY
    - type_code: VARCHAR(2) REFERENCES transaction_type
    - description: VARCHAR(50) NOT NULL
    - active: BOOLEAN DEFAULT TRUE

11. disclosure_group (from CVTRA02Y.cpy):
    - group_id: VARCHAR(10) PRIMARY KEY
    - description: VARCHAR(40)

Use PostgreSQL syntax. Add appropriate NOT NULL constraints and foreign keys.
```

**Prompt 1.3: Create CI/CD Pipeline and Docker Configuration**
```
Create CI/CD configuration and Docker setup for the CardDemo Java application.

1. Create Dockerfile in project root:
   - Multi-stage build
   - Stage 1: Maven build with eclipse-temurin:17-jdk-alpine
   - Stage 2: Runtime with eclipse-temurin:17-jre-alpine
   - Copy JAR from carddemo-api/target
   - Expose port 8080
   - Set JAVA_OPTS for container memory settings
   - Entry point: java $JAVA_OPTS -jar app.jar

2. Create docker-compose.yml for local development:
   - PostgreSQL 15 service:
     - Port 5432
     - Volume for data persistence
     - Environment: POSTGRES_DB=carddemo, POSTGRES_USER=carddemo, POSTGRES_PASSWORD=carddemo
   - Application service:
     - Build from Dockerfile
     - Port 8080
     - Depends on postgres
     - Environment variables for database connection
     - Health check endpoint: /actuator/health

3. Create .github/workflows/ci.yml:
   - Trigger on push to main and pull requests
   - Jobs:
     a. build:
        - Setup Java 17
        - Cache Maven dependencies
        - Run: mvn clean verify
        - Upload test results
     b. docker:
        - Needs: build
        - Only on main branch
        - Build and push Docker image to GitHub Container Registry
        - Tag with commit SHA and 'latest'

4. Create .github/workflows/deploy.yml:
   - Trigger on workflow_dispatch with environment input
   - Jobs:
     a. deploy-staging (if environment=staging):
        - Deploy to staging environment
     b. deploy-production (if environment=production):
        - Require manual approval
        - Deploy to production

Include appropriate secrets references for database credentials and container registry.
```

### Session 2 Prompts: Domain Entities

**Prompt 2.1: Create Customer Entity**
```
Create the Customer JPA entity in carddemo-domain module matching COBOL copybook CVCUS01Y.cpy.

COBOL copybook structure (500 bytes total):
01 CUSTOMER-RECORD.
   05 CUST-ID                    PIC 9(09).
   05 CUST-FIRST-NAME            PIC X(25).
   05 CUST-MIDDLE-NAME           PIC X(25).
   05 CUST-LAST-NAME             PIC X(25).
   05 CUST-ADDR-LINE-1           PIC X(50).
   05 CUST-ADDR-LINE-2           PIC X(50).
   05 CUST-ADDR-LINE-3           PIC X(50).
   05 CUST-ADDR-STATE-CD         PIC X(02).
   05 CUST-ADDR-COUNTRY-CD       PIC X(03).
   05 CUST-ADDR-ZIP              PIC X(10).
   05 CUST-PHONE-NUM-1           PIC X(15).
   05 CUST-PHONE-NUM-2           PIC X(15).
   05 CUST-SSN                   PIC X(09).
   05 CUST-GOVT-ISSUED-ID        PIC X(20).
   05 CUST-DOB-YYYY-MM-DD        PIC X(10).
   05 CUST-EFT-ACCOUNT-ID        PIC X(10).
   05 CUST-PRI-CARD-HOLDER-IND   PIC X(01).
   05 CUST-FICO-CREDIT-SCORE     PIC 9(03).
   05 FILLER                     PIC X(168).

Java entity requirements:
- Package: com.carddemo.domain.entity
- Use Lombok @Data, @Entity, @Table annotations
- @Table(name = "customer", indexes = {...})
- Map CUST-ID to Long id with @Id (no @GeneratedValue - IDs come from mainframe)
- Map all PIC X fields to String with appropriate @Column(length=N)
- Map CUST-DOB to LocalDate dateOfBirth
- Map CUST-PRI-CARD-HOLDER-IND to Boolean primaryCardHolder (convert 'Y'/'N')
- Map CUST-FICO-CREDIT-SCORE to Integer ficoScore
- Add @CreationTimestamp createdAt and @UpdateTimestamp updatedAt
- Add @OneToMany relationship to Account (mappedBy = "customer")
- Override equals/hashCode based on id only
- Add builder pattern with @Builder

Create CustomerRepository in carddemo-persistence:
- Package: com.carddemo.persistence.repository
- Extend JpaRepository<Customer, Long>
- Add methods:
  - List<Customer> findByLastNameContainingIgnoreCase(String lastName)
  - Optional<Customer> findBySsn(String ssn)
  - Page<Customer> findByStateCode(String stateCode, Pageable pageable)
```

**Prompt 2.2: Create Account Entity**
```
Create the Account JPA entity in carddemo-domain module matching COBOL copybook CVACT01Y.cpy.

COBOL copybook structure (300 bytes total):
01 ACCOUNT-RECORD.
   05 ACCT-ID                    PIC 9(11).
   05 ACCT-ACTIVE-STATUS         PIC X(01).
   05 ACCT-CURR-BAL              PIC S9(10)V99.
   05 ACCT-CREDIT-LIMIT          PIC S9(10)V99.
   05 ACCT-CASH-CREDIT-LIMIT     PIC S9(10)V99.
   05 ACCT-OPEN-DATE             PIC X(10).
   05 ACCT-EXPIRAION-DATE        PIC X(10).
   05 ACCT-REISSUE-DATE          PIC X(10).
   05 ACCT-CURR-CYC-CREDIT       PIC S9(10)V99.
   05 ACCT-CURR-CYC-DEBIT        PIC S9(10)V99.
   05 ACCT-ADDR-ZIP              PIC X(10).
   05 ACCT-GROUP-ID              PIC X(10).
   05 FILLER                     PIC X(178).

Java entity requirements:
- Package: com.carddemo.domain.entity
- Use Lombok @Data, @Entity, @Table, @Builder annotations
- @Table with indexes on customer_id and expiration_date
- Map ACCT-ID to Long id with @Id
- Map PIC S9(10)V99 fields to BigDecimal with @Column(precision=12, scale=2)
- Map date fields to LocalDate
- Add @ManyToOne relationship to Customer with @JoinColumn(name = "customer_id")
- Add @OneToMany relationship to Card (mappedBy = "account")
- Add @Version Long version for optimistic locking
- Add @CreationTimestamp and @UpdateTimestamp

Business methods to add:
- boolean isActive() - returns activeStatus.equals("Y")
- boolean isExpired() - returns expirationDate.isBefore(LocalDate.now())
- BigDecimal getAvailableCredit() - returns creditLimit.subtract(currentBalance)
- void addToBalance(BigDecimal amount) - updates currentBalance and appropriate cycle field

Create AccountRepository in carddemo-persistence:
- Extend JpaRepository<Account, Long>
- Add methods:
  - List<Account> findByCustomerId(Long customerId)
  - List<Account> findByActiveStatusAndExpirationDateAfter(String status, LocalDate date)
  - @Query for finding accounts over credit limit
  - Page<Account> findByGroupId(String groupId, Pageable pageable)
```

**Prompt 2.3: Create Card and CardXref Entities**
```
Create Card and CardXref JPA entities matching COBOL copybooks CVACT02Y.cpy and CVACT03Y.cpy.

CVACT02Y.cpy - Card Record (150 bytes):
01 CARD-RECORD.
   05 CARD-NUM                   PIC X(16).
   05 CARD-ACCT-ID               PIC 9(11).
   05 CARD-CVV-CD                PIC 9(03).
   05 CARD-EMBOSSED-NAME         PIC X(50).
   05 CARD-EXPIRAION-DATE        PIC X(10).
   05 CARD-ACTIVE-STATUS         PIC X(01).
   05 FILLER                     PIC X(59).

CVACT03Y.cpy - Card Cross-Reference (50 bytes):
01 CARD-XREF-RECORD.
   05 XREF-CARD-NUM              PIC X(16).
   05 XREF-CUST-ID               PIC 9(09).
   05 XREF-ACCT-ID               PIC 9(11).
   05 FILLER                     PIC X(14).

Card entity requirements:
- Package: com.carddemo.domain.entity
- @Id on cardNumber (String, not auto-generated)
- @ManyToOne to Account with @JoinColumn(name = "account_id")
- Map CVV as String (preserve leading zeros)
- Add validation: @Pattern for card number format
- Business methods:
  - boolean isActive()
  - boolean isExpired()
  - String getMaskedCardNumber() - returns "****-****-****-" + last 4 digits

CardXref entity requirements:
- Package: com.carddemo.domain.entity
- @Id on cardNumber (same as Card)
- @OneToOne to Card with @MapsId
- @ManyToOne to Customer
- @ManyToOne to Account
- This entity enables lookups: card -> customer, card -> account

Create repositories:
- CardRepository:
  - findByAccountId(Long accountId)
  - findByCardNumberStartingWith(String prefix)
  - findByActiveStatusAndExpirationDateAfter(String status, LocalDate date)

- CardXrefRepository:
  - findByCustomerId(Long customerId)
  - findByAccountId(Long accountId)
  - Optional<CardXref> findByCardNumber(String cardNumber)
```

**Prompt 2.4: Create Transaction Entity**
```
Create the Transaction JPA entity matching COBOL copybook CVTRA05Y.cpy.

COBOL copybook structure (350 bytes):
01 TRAN-RECORD.
   05 TRAN-ID                    PIC X(16).
   05 TRAN-TYPE-CD               PIC X(02).
   05 TRAN-CAT-CD                PIC 9(04).
   05 TRAN-SOURCE                PIC X(10).
   05 TRAN-DESC                  PIC X(100).
   05 TRAN-AMT                   PIC S9(09)V99.
   05 TRAN-MERCHANT-ID           PIC 9(09).
   05 TRAN-MERCHANT-NAME         PIC X(50).
   05 TRAN-MERCHANT-CITY         PIC X(50).
   05 TRAN-MERCHANT-ZIP          PIC X(10).
   05 TRAN-CARD-NUM              PIC X(16).
   05 TRAN-ORIG-TS               PIC X(26).
   05 TRAN-PROC-TS               PIC X(26).
   05 FILLER                     PIC X(44).

Java entity requirements:
- Package: com.carddemo.domain.entity
- @Id on id (String, 16 chars)
- Map TRAN-AMT to BigDecimal with precision=11, scale=2
- Map timestamps to LocalDateTime
- @ManyToOne to Card with @JoinColumn(name = "card_number")
- @ManyToOne to Account with @JoinColumn(name = "account_id")
- Add @CreationTimestamp for createdAt

Transaction type codes (for reference):
- "PR" = Purchase
- "CR" = Credit/Return
- "CA" = Cash Advance
- "PA" = Payment
- "FE" = Fee
- "IN" = Interest

Create TransactionRepository:
- findByAccountIdOrderByProcessedAtDesc(Long accountId, Pageable pageable)
- findByCardNumber(String cardNumber, Pageable pageable)
- findByAccountIdAndProcessedAtBetween(Long accountId, LocalDateTime start, LocalDateTime end)
- findByTypeCodeAndCategoryCode(String typeCode, Integer categoryCode)
- @Query to sum amounts by account and type for a date range
- @Query to count transactions by merchant

Also create TcatBalance entity for transaction category balances:
- Composite key: (accountId, typeCode, categoryCode) using @EmbeddedId
- balance: BigDecimal
- updatedAt: LocalDateTime
- @ManyToOne to Account
```

### Session 3 Prompts: Data Migration

**Prompt 3.1: Create JRecord EBCDIC Parser Utility**
```
Create a JRecord-based utility for parsing EBCDIC mainframe files in carddemo-migration module.

Requirements:
1. Create CopybookParserConfig class:
   - Configure JRecord for EBCDIC charset cp037
   - Set file organization to fixed-length records
   - Handle COMP (binary) and COMP-3 (packed decimal) fields
   - Support multiple copybook formats

2. Create abstract BaseEbcdicParser<T> class:
   - Generic type T for the target DTO
   - Method: void configure(String copybookPath, int recordLength)
   - Method: Stream<T> parseFile(Path filePath)
   - Method: T mapRecord(AbstractLine line) - abstract, implemented by subclasses
   - Handle file open/close with try-with-resources
   - Log parsing statistics (records read, errors, duration)

3. Create utility methods for field conversion:
   - String getStringField(AbstractLine line, String fieldName) - trim trailing spaces
   - Long getLongField(AbstractLine line, String fieldName) - handle PIC 9(n)
   - BigDecimal getPackedDecimalField(AbstractLine line, String fieldName, int scale) - handle COMP-3
   - BigDecimal getBinaryField(AbstractLine line, String fieldName, int scale) - handle COMP
   - LocalDate getDateField(AbstractLine line, String fieldName, String pattern)
   - LocalDateTime getTimestampField(AbstractLine line, String fieldName)

4. Create CustomerEbcdicParser extends BaseEbcdicParser<CustomerImportDto>:
   - Copybook: CVCUS01Y.cpy
   - Record length: 500 bytes
   - Map all fields from copybook to DTO
   - Handle CUST-PRI-CARD-HOLDER-IND conversion ('Y'/'N' to boolean)

5. Create CustomerImportDto:
   - All fields as simple types (no JPA annotations)
   - Method: Customer toEntity() - converts to JPA entity

6. Write unit tests:
   - Test with sample EBCDIC data from app/data/EBCDIC/AWS.M2.CARDDEMO.CUSTDATA.PS
   - Verify field values for known records
   - Test COMP-3 decimal conversion accuracy
   - Test character encoding (special characters, spaces)

Dependencies to add to pom.xml:
- net.sf.JRecord:JRecordV1:0.90.4
- net.sf.cb2xml:cb2xml:1.0.0
```

**Prompt 3.2: Create Data Import Services**
```
Create data import services for migrating VSAM data to PostgreSQL in carddemo-migration module.

1. Create ImportService interface:
   - void importFromFile(Path filePath)
   - void importFromFile(Path filePath, boolean validateOnly)
   - ImportResult getLastImportResult()

2. Create ImportResult record:
   - long totalRecords
   - long successfulRecords
   - long failedRecords
   - List<ImportError> errors
   - Duration duration

3. Create CustomerImportService implements ImportService:
   - Inject CustomerRepository and CustomerEbcdicParser
   - Parse EBCDIC file in batches of 1000 records
   - Use saveAll() for batch inserts
   - Handle duplicate key conflicts with upsert logic:
     - If customer exists, update non-key fields
     - If new, insert
   - Track and log errors without stopping import
   - Return ImportResult with statistics

4. Create similar import services for:
   - AccountImportService (ACCTDATA.PS, CVACT01Y, 300 bytes)
   - CardImportService (CARDDATA.PS, CVACT02Y, 150 bytes)
   - CardXrefImportService (CARDXREF.PS, CVACT03Y, 50 bytes)
   - TransactionImportService (TRANSACT files, CVTRA05Y, 350 bytes)

5. Create UserImportService with special handling:
   - Parse USRSEC.PS using CSUSR01Y copybook
   - Convert plaintext passwords to bcrypt hashes
   - Map SEC-USR-TYPE 'A' to ADMIN role, 'U' to USER role
   - Default password if empty: generate random and log

6. Create ImportCommandLineRunner:
   - Implement CommandLineRunner
   - Accept command line arguments:
     --import --dataset=CUSTDATA --file=/path/to/file
     --import --dataset=ALL --directory=/path/to/data
     --validate-only (dry run mode)
   - Execute appropriate import service
   - Print summary report

7. Create ImportController for REST-based imports:
   - POST /api/admin/import/{dataset}
   - Accept multipart file upload
   - Return ImportResult as JSON
   - Require ADMIN role

Write integration tests using Testcontainers with PostgreSQL.
```

### Session 4 Prompts: Authentication

**Prompt 4.1: Implement Spring Security with JWT**
```
Implement JWT-based authentication in carddemo-security module, replacing COBOL program COSGN00C.

1. Create SecurityConfig class:
   - @EnableWebSecurity, @EnableMethodSecurity
   - Configure SecurityFilterChain:
     - Disable CSRF (stateless API)
     - Enable CORS with configurable origins
     - Session management: STATELESS
     - Public endpoints: /api/auth/**, /api/health, /swagger-ui/**, /v3/api-docs/**
     - /api/admin/** requires ROLE_ADMIN
     - All other /api/** requires authentication
   - Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
   - Configure AuthenticationManager with custom UserDetailsService
   - Configure PasswordEncoder (BCrypt)

2. Create JwtTokenProvider:
   - @Value for jwt.secret and jwt.expiration from properties
   - generateToken(UserDetails userDetails): String
     - Claims: username, roles, issued at, expiration
     - Sign with HS256
   - validateToken(String token): boolean
   - getUsernameFromToken(String token): String
   - getRolesFromToken(String token): List<String>

3. Create JwtAuthenticationFilter extends OncePerRequestFilter:
   - Extract token from Authorization header (Bearer scheme)
   - Validate token
   - Load UserDetails and set SecurityContext
   - Handle expired/invalid tokens gracefully

4. Create AppUserDetailsService implements UserDetailsService:
   - Inject AppUserRepository
   - loadUserByUsername(String username):
     - Find user in database
     - Map user_type 'A' to ROLE_ADMIN, 'U' to ROLE_USER
     - Return Spring Security User object
   - Throw UsernameNotFoundException if not found

5. Create AuthController:
   - POST /api/auth/login
     - Request: LoginRequest { username, password }
     - Validate credentials
     - Generate JWT token
     - Response: AuthResponse { token, username, userType, expiresAt }
   - POST /api/auth/refresh (optional)
     - Accept valid token, return new token with extended expiration
   - GET /api/auth/me
     - Return current user details from SecurityContext

6. Create DTOs:
   - LoginRequest with @NotBlank validation
   - AuthResponse
   - UserPrincipal (custom UserDetails implementation)

7. Add to application.yml:
   jwt:
     secret: ${JWT_SECRET:your-256-bit-secret-key-here}
     expiration: 86400000  # 24 hours in milliseconds

8. Write tests:
   - Test successful login with valid credentials
   - Test login failure with invalid password
   - Test login failure with unknown user
   - Test protected endpoint without token (401)
   - Test protected endpoint with valid token (200)
   - Test admin endpoint with user role (403)
   - Test expired token handling
```

**Prompt 4.2: Migrate User Data with Password Hashing**
```
Create user data migration that converts mainframe USRSEC records to Spring Security compatible format.

1. Analyze CSUSR01Y.cpy structure:
   01 SEC-USER-DATA.
      05 SEC-USR-ID              PIC X(08).
      05 SEC-USR-FNAME           PIC X(20).
      05 SEC-USR-LNAME           PIC X(20).
      05 SEC-USR-PWD             PIC X(08).
      05 SEC-USR-TYPE            PIC X(01).
      05 FILLER                  PIC X(23).

2. Create UserMigrationService:
   - Inject PasswordEncoder (BCrypt)
   - Method: migrateUsers(Path usrsecFile)
   - For each record:
     a. Parse user data using JRecord
     b. Check if user exists in app_user table
     c. If exists, skip or update based on flag
     d. If new:
        - Hash password with BCrypt (12 rounds)
        - Map SEC-USR-TYPE to user_type
        - Insert into app_user
   - Return migration statistics

3. Create default users if USRSEC file not available:
   - ADMIN001 / PASSWORD -> admin user
   - USER0001 / PASSWORD -> regular user
   - Create Flyway migration V2__seed_users.sql with bcrypt hashes

4. Create PasswordMigrationUtil:
   - Method: String hashPassword(String plaintext)
   - Method: boolean verifyPassword(String plaintext, String hash)
   - Handle empty/null passwords (generate random)
   - Log warnings for weak passwords

5. Create UserManagementService for ongoing user operations:
   - createUser(CreateUserRequest request)
   - updateUser(Long id, UpdateUserRequest request)
   - changePassword(Long id, ChangePasswordRequest request)
   - deleteUser(Long id) - soft delete (set enabled=false)
   - resetPassword(Long id) - generate temporary password

6. Add password policy validation:
   - Minimum 8 characters
   - At least one uppercase, one lowercase, one digit
   - Not same as username
   - Create @ValidPassword annotation with ConstraintValidator

Write integration tests verifying:
- Migrated users can authenticate
- Password hashing is consistent
- User type mapping is correct
- Default users are created correctly
```

### Session 5 Prompts: Account and Card APIs

**Prompt 5.1: Implement Account Service and Controller**
```
Implement Account management APIs equivalent to COBOL programs COACTVWC (view) and COACTUPC (update).

1. Create AccountService in carddemo-services:
   - Inject AccountRepository, CustomerRepository

   Methods:
   a. AccountDto getAccountById(Long id)
      - Find account or throw NotFoundException
      - Map to DTO including customer summary

   b. Page<AccountDto> getAccountsByCustomerId(Long customerId, Pageable pageable)
      - Verify customer exists
      - Return paginated accounts

   c. AccountDto updateAccount(Long id, UpdateAccountRequest request)
      - Find account or throw NotFoundException
      - Validate business rules:
        - Credit limit cannot be negative
        - Cash credit limit cannot exceed credit limit
        - Expiration date cannot be in the past
        - Active status must be 'Y' or 'N'
      - Update allowed fields only (not balance, not ID)
      - Save and return updated DTO

   d. Page<AccountDto> searchAccounts(AccountSearchCriteria criteria, Pageable pageable)
      - Support filtering by: status, group, expiration date range, balance range
      - Use Specification for dynamic queries

2. Create AccountController in carddemo-api:
   - @RestController, @RequestMapping("/api/accounts")
   - @Tag(name = "Accounts") for OpenAPI

   Endpoints:
   - GET /{id} -> getAccountById
   - PUT /{id} -> updateAccount
   - GET /search -> searchAccounts
   - GET /customer/{customerId} -> getAccountsByCustomerId

3. Create DTOs:
   - AccountDto: all account fields + customerName, cardCount
   - UpdateAccountRequest: creditLimit, cashCreditLimit, expirationDate, activeStatus, groupId
   - AccountSearchCriteria: status, groupId, expirationDateFrom, expirationDateTo, minBalance, maxBalance

4. Create AccountMapper using MapStruct:
   - Entity to DTO mapping
   - Handle nested customer mapping
   - Calculate derived fields (cardCount)

5. Add validation annotations to request DTOs:
   - @NotNull, @Positive, @Size, @Pattern
   - Custom @FutureOrPresent for expiration date

6. Create AccountSpecification for dynamic queries:
   - Build Specification from AccountSearchCriteria
   - Handle null criteria fields gracefully

7. Write tests:
   - Unit tests for AccountService with mocked repository
   - Integration tests for AccountController with TestRestTemplate
   - Test validation error responses
   - Test not found scenarios
   - Test optimistic locking conflict
```

**Prompt 5.2: Implement Card Service and Controller**
```
Implement Card management APIs equivalent to COBOL programs COCRDLIC (list), COCRDSLC (view), and COCRDUPC (update).

1. Create CardService in carddemo-services:
   - Inject CardRepository, CardXrefRepository, AccountRepository

   Methods:
   a. List<CardDto> getCardsByAccountId(Long accountId)
      - Verify account exists
      - Return all cards for account
      - Include masked card number in response

   b. CardDto getCardByNumber(String cardNumber)
      - Find card or throw NotFoundException
      - Include account summary in response
      - Mask CVV in response (return "***")

   c. CardDto updateCard(String cardNumber, UpdateCardRequest request)
      - Find card or throw NotFoundException
      - Validate business rules:
        - Cannot change card number
        - Expiration date must be future for active cards
        - Embossed name max 50 characters
      - Update allowed fields
      - Save and return updated DTO

   d. CardXrefDto getCardXref(String cardNumber)
      - Return cross-reference data (customer, account links)

2. Create CardController in carddemo-api:
   - @RestController, @RequestMapping("/api/cards")

   Endpoints:
   - GET /{cardNumber} -> getCardByNumber
   - PUT /{cardNumber} -> updateCard
   - GET /{cardNumber}/xref -> getCardXref
   - GET /account/{accountId} -> getCardsByAccountId

3. Create DTOs:
   - CardDto: cardNumber (masked), embossedName, expirationDate, activeStatus, accountId, accountStatus
   - CardDetailDto: extends CardDto with full card number (for authorized users only)
   - UpdateCardRequest: embossedName, expirationDate, activeStatus
   - CardXrefDto: cardNumber, customerId, customerName, accountId

4. Create CardMapper:
   - maskCardNumber(): show only last 4 digits
   - Include account status in card response

5. Add security for sensitive data:
   - Full card number only visible to ADMIN role
   - CVV never returned in API responses
   - Log access to card details for audit

6. Create custom exception:
   - CardNotFoundException extends NotFoundException
   - Include masked card number in error message

7. Write comprehensive tests:
   - Test card listing by account
   - Test card detail retrieval
   - Test card update with valid data
   - Test validation failures
   - Test card number masking
   - Test authorization for full card number access
```

### Session 6 Prompts: Transaction APIs

**Prompt 6.1: Implement Transaction Service with CBTRN02C Validation Logic**
```
Implement Transaction APIs equivalent to COBOL programs COTRN00C (list), COTRN01C (view), and COTRN02C (add).

The critical requirement is replicating the validation logic from batch program CBTRN02C.cbl which validates transactions before posting.

1. Create TransactionValidationService:
   - Inject CardXrefRepository, AccountRepository

   Method: ValidationResult validateTransaction(TransactionCreateRequest request)
   
   Validation steps (matching CBTRN02C):
   a. Card validation (rejection code 100):
      - Lookup card in card_xref by cardNumber
      - If not found: return ValidationResult.rejected(100, "Invalid card number")
   
   b. Account validation (rejection code 101):
      - Get accountId from xref
      - Lookup account by accountId
      - If not found: return ValidationResult.rejected(101, "Account not found")
   
   c. Credit limit validation (rejection code 102):
      - Calculate: tempBalance = cycleCredit - cycleDebit + transactionAmount
      - If tempBalance > creditLimit: return ValidationResult.rejected(102, "Transaction exceeds credit limit")
   
   d. Expiration validation (rejection code 103):
      - If account.expirationDate < transaction.originatedAt.toLocalDate():
        return ValidationResult.rejected(103, "Account expired")
   
   e. If all validations pass: return ValidationResult.valid(account, xref)

2. Create TransactionService:
   - Inject TransactionRepository, TransactionValidationService, AccountRepository

   Methods:
   a. Page<TransactionDto> getTransactionsByAccountId(Long accountId, Pageable pageable)
   
   b. TransactionDto getTransactionById(String id)
   
   c. TransactionDto createTransaction(TransactionCreateRequest request)
      - Call validationService.validateTransaction(request)
      - If rejected: throw TransactionValidationException with code and reason
      - If valid, within @Transactional:
        1. Generate transaction ID (format: timestamp + sequence)
        2. Create Transaction entity
        3. Update account balances:
           - currentBalance += amount
           - If amount > 0: cycleDebit += amount
           - If amount < 0: cycleCredit += abs(amount)
        4. Update or create TcatBalance record
        5. Set processedAt = now()
        6. Save all entities
        7. Return created transaction DTO

3. Create TransactionController:
   - GET /api/transactions/{id}
   - GET /api/accounts/{accountId}/transactions
   - POST /api/transactions

4. Create DTOs:
   - TransactionDto: all fields, masked card number
   - TransactionCreateRequest: cardNumber, typeCode, categoryCode, amount, merchantInfo, originatedAt
   - TransactionSearchCriteria: accountId, dateRange, typeCode, amountRange

5. Create ValidationResult:
   - boolean valid
   - Integer rejectionCode (null if valid)
   - String rejectionReason (null if valid)
   - Account account (null if invalid)
   - CardXref xref (null if invalid)

6. Create TransactionValidationException:
   - int rejectionCode
   - String rejectionReason
   - Map to HTTP 422 with structured error response

7. Handle concurrency:
   - Use @Version on Account for optimistic locking
   - Catch OptimisticLockingFailureException
   - Retry once, then return 409 Conflict

8. Write extensive tests:
   - Test each validation scenario (codes 100, 101, 102, 103)
   - Test successful transaction creation
   - Test balance updates are correct
   - Test concurrent transaction handling
   - Test transaction ID generation uniqueness
```

### Session 7 Prompts: Batch Processing

**Prompt 7.1: Implement Post Transactions Batch Job**
```
Implement Spring Batch job equivalent to JCL job POSTTRAN running COBOL program CBTRN02C.

This job reads daily transaction file (DALYTRAN), validates each transaction, posts valid ones to the database, and writes rejections to a reject file/table.

1. Create DailyTransactionDto matching CVTRA06Y.cpy:
   - All fields from copybook
   - Method to convert to TransactionCreateRequest

2. Create JRecordItemReader<DailyTransactionDto>:
   - Extend AbstractItemStreamItemReader
   - Configure with CVTRA06Y copybook
   - EBCDIC charset cp037
   - Fixed length 350 bytes
   - Implement read() to return next record or null at EOF
   - Implement open/close for file handling

3. Create TransactionValidationProcessor implements ItemProcessor<DailyTransactionDto, ValidatedTransaction>:
   - Inject TransactionValidationService
   - process(DailyTransactionDto item):
     - Convert to TransactionCreateRequest
     - Call validationService.validateTransaction()
     - If valid: return ValidatedTransaction with account and xref
     - If invalid: throw TransactionRejectedException (handled by skip listener)

4. Create ValidatedTransaction:
   - DailyTransactionDto originalRecord
   - Account account
   - CardXref xref

5. Create TransactionPostingWriter implements ItemWriter<ValidatedTransaction>:
   - Inject TransactionRepository, AccountRepository, TcatBalanceRepository
   - write(Chunk<ValidatedTransaction> items):
     - For each item in single transaction:
       - Create Transaction entity
       - Update Account balances
       - Update TcatBalance
       - Save all
     - Use batch operations for efficiency

6. Create RejectionWriter implements ItemWriter<DailyTransactionDto>:
   - Write to daily_transaction_reject table
   - Include rejection code, reason, batch run ID
   - Alternative: write to rejection file

7. Create TransactionRejectedException:
   - Contains DailyTransactionDto and rejection details
   - Used by skip listener to route to rejection writer

8. Create PostTransactionsJobConfig:
   ```java
   @Configuration
   public class PostTransactionsJobConfig {
       @Bean
       public Job postTransactionsJob(JobRepository jobRepository,
                                      Step postTransactionsStep) {
           return new JobBuilder("postTransactionsJob", jobRepository)
               .incrementer(new RunIdIncrementer())
               .start(postTransactionsStep)
               .build();
       }

       @Bean
       public Step postTransactionsStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        ItemReader<DailyTransactionDto> reader,
                                        ItemProcessor<DailyTransactionDto, ValidatedTransaction> processor,
                                        ItemWriter<ValidatedTransaction> writer) {
           return new StepBuilder("postTransactionsStep", jobRepository)
               .<DailyTransactionDto, ValidatedTransaction>chunk(500, transactionManager)
               .reader(reader)
               .processor(processor)
               .writer(writer)
               .faultTolerant()
               .skip(TransactionRejectedException.class)
               .skipLimit(Integer.MAX_VALUE)
               .listener(rejectionSkipListener())
               .listener(jobCompletionListener())
               .build();
       }
   }
   ```

9. Create RejectionSkipListener implements SkipListener<DailyTransactionDto, ValidatedTransaction>:
   - onSkipInProcess(): write rejected item to rejection table

10. Create JobCompletionListener implements JobExecutionListener:
    - Log job statistics: total, processed, rejected, duration
    - Send notification on completion (optional)

11. Create BatchJobController:
    - POST /api/batch/post-transactions
    - Parameters: inputFile path, runDate
    - Launch job asynchronously
    - Return job execution ID
    - GET /api/batch/jobs/{executionId} - get job status

12. Write integration tests:
    - Test with sample DALYTRAN file
    - Verify correct records posted
    - Verify rejections captured with correct codes
    - Verify account balances updated correctly
    - Test restart capability after failure
```

### Session 8 Prompts: Interest and Statements

**Prompt 8.1: Implement Interest Calculation Batch Job**
```
Implement Spring Batch job equivalent to JCL job INTCALC running COBOL program CBACT04C.

This job calculates monthly interest on account balances and posts interest charges as transactions.

1. Create InterestCalculationJobConfig:
   - Job: interestCalculationJob
   - Step 1: calculateInterestStep
   - Step 2: resetCycleCountersStep (optional, end of billing cycle)

2. Create AccountItemReader:
   - JpaPagingItemReader for accounts
   - Query: accounts where currentBalance > 0 and activeStatus = 'Y'
   - Page size: 100

3. Create InterestCalculationProcessor implements ItemProcessor<Account, InterestCharge>:
   - Calculate interest based on configurable rate
   - Default: 18% APR / 12 = 1.5% monthly
   - Interest = currentBalance * monthlyRate
   - Round to 2 decimal places (HALF_UP)
   - Create InterestCharge record with account and amount

4. Create InterestCharge:
   - Account account
   - BigDecimal interestAmount
   - LocalDate calculationDate

5. Create InterestChargeWriter implements ItemWriter<InterestCharge>:
   - For each interest charge:
     a. Create Transaction with:
        - typeCode = "IN" (Interest)
        - categoryCode = 9999 (Interest charge)
        - amount = interestAmount
        - description = "Monthly Interest Charge"
        - cardNumber = primary card for account
        - originatedAt = processedAt = now()
     b. Update account:
        - currentBalance += interestAmount
        - cycleDebit += interestAmount
     c. Update TcatBalance for interest category

6. Create CycleResetTasklet (optional step):
   - Reset cycleCredit and cycleDebit to 0 for all accounts
   - Run at end of billing cycle
   - Configurable via job parameter

7. Add job parameters:
   - interestRate: BigDecimal (default 0.015)
   - calculationDate: LocalDate (default today)
   - resetCycle: boolean (default false)

8. Create InterestCalculationService for ad-hoc calculations:
   - calculateInterestForAccount(Long accountId)
   - previewInterestCharges(LocalDate asOfDate) - dry run

9. Schedule job execution:
   - Create @Scheduled method or Quartz trigger
   - Run on 1st of each month at 2:00 AM
   - Configurable via properties

10. Write tests:
    - Test interest calculation accuracy
    - Test transaction creation
    - Test balance updates
    - Test with zero balance accounts (should skip)
    - Test with negative balance (credit balance - no interest)
```

**Prompt 8.2: Implement Statement Generation Batch Job**
```
Implement Spring Batch job equivalent to JCL job CREASTMT running COBOL programs CBSTM03A/CBSTM03B.

This job generates monthly statements for all accounts with activity.

1. Create StatementGenerationJobConfig:
   - Job: createStatementsJob
   - Step 1: generateStatementsStep
   - Step 2: (optional) emailNotificationStep

2. Create StatementData:
   - accountId, customerId
   - customerName, customerAddress
   - statementDate, statementPeriodStart, statementPeriodEnd
   - previousBalance, paymentsAndCredits, purchasesAndDebits
   - feesCharged, interestCharged
   - newBalance
   - minimumPaymentDue, paymentDueDate
   - List<TransactionSummary> transactions
   - creditLimit, availableCredit

3. Create AccountStatementReader:
   - JpaPagingItemReader for accounts
   - Filter: accounts with transactions in statement period
   - Or: all active accounts (generate even if no activity)

4. Create StatementGenerationProcessor implements ItemProcessor<Account, StatementData>:
   - Query transactions for account in statement period
   - Calculate:
     - Previous balance (from prior statement or opening balance)
     - Sum of payments/credits (negative amounts)
     - Sum of purchases/debits (positive amounts)
     - Fees and interest (by type code)
     - New balance
   - Calculate minimum payment:
     - 2% of balance or $25, whichever is greater
     - If balance < $25, minimum = balance
   - Set payment due date: 25 days from statement date
   - Build StatementData with all transactions

5. Create StatementWriter implements ItemWriter<StatementData>:
   - For each statement:
     a. Generate PDF using Thymeleaf + Flying Saucer (or OpenPDF)
     b. Generate HTML version
     c. Save files to configured location (local or S3)
     d. Create Statement record in database:
        - statementId, accountId, statementDate
        - pdfPath, htmlPath
        - newBalance, minimumPayment, dueDate
        - generatedAt

6. Create statement Thymeleaf template (statement-template.html):
   - Header: Bank logo, statement date, account number
   - Customer info: Name, address
   - Account summary: Previous balance, activity, new balance
   - Transaction table: Date, description, amount, running balance
   - Payment info: Minimum due, due date, payment address
   - Footer: Terms, contact info

7. Create StatementPdfGenerator:
   - Use Flying Saucer (org.xhtmlrenderer:flying-saucer-pdf)
   - Or OpenPDF (com.github.librepdf:openpdf)
   - Render HTML template to PDF
   - Configure fonts for consistent rendering

8. Add job parameters:
   - statementMonth: int (1-12)
   - statementYear: int
   - outputDirectory: String
   - emailStatements: boolean

9. Create StatementService for on-demand generation:
   - generateStatementForAccount(Long accountId, YearMonth period)
   - getStatementHistory(Long accountId)
   - downloadStatement(String statementId)

10. Create StatementController:
    - GET /api/accounts/{accountId}/statements
    - GET /api/statements/{statementId}/pdf
    - POST /api/statements/generate (admin, trigger batch)

11. Write tests:
    - Test statement calculation accuracy
    - Test PDF generation
    - Test with various transaction patterns
    - Test minimum payment calculation edge cases
```

### Session 9-12 Prompts: Reports, UI, and Deployment

**Prompt 9.1: Implement Transaction Reports**
```
Implement reporting APIs equivalent to COBOL program CORPT00C.

1. Create ReportService:
   - generateTransactionReport(ReportCriteria criteria): ReportResult
   - Criteria: accountId, customerId, dateRange, typeCode, categoryCode, amountRange, merchantName
   - Support multiple output formats: JSON, CSV, PDF

2. Create ReportController:
   - GET /api/reports/transactions - paginated JSON
   - GET /api/reports/transactions/export?format=csv - stream CSV
   - GET /api/reports/transactions/export?format=pdf - generate PDF
   - POST /api/reports/transactions/schedule - schedule recurring report

3. Create report aggregations:
   - Summary by transaction type
   - Summary by merchant
   - Daily/weekly/monthly totals
   - Top merchants by volume

4. Implement CSV streaming for large exports:
   - Use StreamingResponseBody
   - Write header, then stream rows
   - No memory issues with large datasets

5. Create scheduled report job:
   - Daily transaction summary
   - Weekly merchant analysis
   - Monthly account activity
   - Email reports to configured recipients
```

**Prompt 10.1: Create Next.js 15 Frontend Application**
```
Create a Next.js 15 frontend application replacing BMS 3270 screens for the CardDemo application.

1. Initialize Next.js 15 project:
   npx create-next-app@latest carddemo-ui --typescript --tailwind --eslint --app --src-dir
   
   Additional dependencies to install:
   - @tanstack/react-query (data fetching and caching)
   - axios (HTTP client)
   - react-hook-form + @hookform/resolvers + zod (form handling and validation)
   - @radix-ui/react-* or shadcn/ui (accessible UI components)
   - next-auth (authentication with JWT)
   - lucide-react (icons)
   - @tanstack/react-table (data tables)
   - date-fns (date formatting)

2. Project structure:
   src/
   ├── app/
   │   ├── (auth)/
   │   │   ├── login/page.tsx
   │   │   └── layout.tsx
   │   ├── (dashboard)/
   │   │   ├── layout.tsx
   │   │   ├── page.tsx (dashboard)
   │   │   ├── accounts/
   │   │   │   ├── page.tsx (list)
   │   │   │   └── [id]/page.tsx (detail)
   │   │   ├── cards/
   │   │   │   ├── page.tsx
   │   │   │   └── [cardNumber]/page.tsx
   │   │   ├── transactions/
   │   │   │   ├── page.tsx
   │   │   │   ├── [id]/page.tsx
   │   │   │   └── new/page.tsx
   │   │   ├── reports/page.tsx
   │   │   └── admin/
   │   │       └── users/page.tsx
   │   ├── api/
   │   │   └── auth/[...nextauth]/route.ts
   │   ├── layout.tsx
   │   └── globals.css
   ├── components/
   │   ├── ui/ (shadcn components)
   │   ├── forms/
   │   ├── tables/
   │   └── layout/
   ├── lib/
   │   ├── api-client.ts
   │   ├── auth.ts
   │   └── utils.ts
   ├── hooks/
   │   ├── use-accounts.ts
   │   ├── use-cards.ts
   │   └── use-transactions.ts
   └── types/
       └── index.ts

3. Configure NextAuth.js for JWT authentication:
   - Create auth.ts with CredentialsProvider
   - Call backend /api/auth/login endpoint
   - Store JWT token in session
   - Configure middleware for protected routes
   - Handle token refresh

4. Create API client with Axios:
   - Base URL from environment variable
   - Request interceptor to add Authorization header
   - Response interceptor for error handling
   - Automatic token refresh on 401

5. Implement Server Components where possible:
   - Use Server Components for initial data fetching
   - Use Client Components for interactive elements
   - Implement loading.tsx and error.tsx for each route
   - Use Suspense boundaries for streaming

6. Create reusable components:
   - DataTable with TanStack Table (sorting, pagination, filtering)
   - SearchForm with react-hook-form
   - Modal/Dialog with Radix UI
   - Toast notifications with sonner
   - Loading skeletons
   - Card components for summaries

7. Implement pages matching BMS screens:

   Login (COSGN00):
   - Username and password fields
   - Error message display area
   - Remember me option
   - Redirect to dashboard on success

   Dashboard:
   - Account summary cards
   - Recent transactions list
   - Quick actions

   Accounts (COACTVWC/COACTUPC):
   - Searchable/filterable table
   - Click to view details
   - Edit form with validation
   - Balance and limit display

   Cards (COCRDLIC/COCRDSLC/COCRDUPC):
   - List cards by account
   - Masked card numbers
   - Status indicators
   - Edit embossed name, status

   Transactions (COTRN00C/COTRN01C/COTRN02C):
   - Paginated transaction list
   - Date range filter
   - Transaction detail view
   - New transaction form with validation:
     - Card number lookup
     - Amount validation
     - Merchant information
     - Real-time credit limit check

   Reports (CORPT00C):
   - Filter form (date range, type, amount)
   - Results table
   - Export to CSV button
   - Print-friendly view

   Admin Users (COUSR00C-03C):
   - User list with roles
   - Create/edit user forms
   - Password reset
   - Role assignment

8. Form validation matching COBOL rules:
   - Use Zod schemas for validation
   - Card number format: 16 digits
   - Amount: positive, max 2 decimal places
   - Dates: valid format, business rules
   - Display inline errors

9. Environment configuration:
   .env.local:
   NEXT_PUBLIC_API_URL=http://localhost:8080/api
   NEXTAUTH_SECRET=your-secret
   NEXTAUTH_URL=http://localhost:3000

10. Add responsive design:
    - Mobile-first approach
    - Collapsible sidebar on mobile
    - Touch-friendly interactions
    - Accessible keyboard navigation
```

**Prompt 10.2: Implement Next.js 15 Authentication with NextAuth.js**
```
Implement authentication in the Next.js 15 CardDemo frontend using NextAuth.js v5.

1. Install dependencies:
   npm install next-auth@beta @auth/core

2. Create src/lib/auth.ts:
   - Configure NextAuth with CredentialsProvider
   - Implement authorize function to call backend /api/auth/login
   - Configure JWT callback to store backend token
   - Configure session callback to expose user data
   - Handle token refresh

3. Create src/app/api/auth/[...nextauth]/route.ts:
   - Export GET and POST handlers from NextAuth

4. Create src/middleware.ts:
   - Protect routes under /(dashboard)
   - Redirect unauthenticated users to /login
   - Allow public access to /login

5. Create login page src/app/(auth)/login/page.tsx:
   - Form with username and password
   - Use react-hook-form with Zod validation
   - Call signIn('credentials', {...})
   - Display error messages
   - Redirect to dashboard on success
   - Match COSGN00 screen layout

6. Create auth context/hooks:
   - useSession hook for client components
   - getServerSession for server components
   - useCurrentUser custom hook

7. Add logout functionality:
   - Call signOut() from next-auth/react
   - Clear any local storage
   - Redirect to login

8. Handle session expiration:
   - Detect 401 responses
   - Show session expired modal
   - Redirect to login
```

**Prompt 10.3: Create Next.js 15 Account Management Pages**
```
Create account management pages in Next.js 15 matching COBOL programs COACTVWC and COACTUPC.

1. Create src/app/(dashboard)/accounts/page.tsx (Server Component):
   - Fetch accounts list from API
   - Display in DataTable with columns:
     - Account ID
     - Customer Name
     - Status (badge)
     - Balance (formatted currency)
     - Credit Limit
     - Expiration Date
   - Add search/filter controls
   - Pagination with page size selector
   - Click row to navigate to detail

2. Create src/app/(dashboard)/accounts/[id]/page.tsx:
   - Fetch account details by ID
   - Display account information card
   - Show associated cards list
   - Show recent transactions
   - Edit button (opens modal or navigates to edit)

3. Create src/components/forms/account-edit-form.tsx:
   - Use react-hook-form with Zod schema
   - Fields: creditLimit, cashCreditLimit, expirationDate, activeStatus, groupId
   - Validation rules matching COBOL:
     - Credit limit must be positive
     - Cash credit limit <= credit limit
     - Expiration date must be future
     - Status must be Y or N
   - Submit handler calls PUT /api/accounts/{id}
   - Show success/error toast

4. Create src/hooks/use-accounts.ts:
   - useAccounts() - list with pagination
   - useAccount(id) - single account
   - useUpdateAccount() - mutation
   - Use TanStack Query for caching

5. Create src/types/account.ts:
   - Account interface matching API response
   - AccountUpdateRequest interface
   - AccountSearchParams interface
```

**Prompt 10.4: Create Next.js 15 Transaction Pages**
```
Create transaction pages in Next.js 15 matching COBOL programs COTRN00C, COTRN01C, and COTRN02C.

1. Create src/app/(dashboard)/transactions/page.tsx:
   - Transaction list with filters:
     - Account ID (optional)
     - Date range
     - Transaction type
     - Amount range
   - DataTable with columns:
     - Transaction ID
     - Date
     - Type
     - Description
     - Amount (color-coded: green for credits, red for debits)
     - Merchant
     - Card (masked)
   - Pagination
   - Export to CSV button

2. Create src/app/(dashboard)/transactions/[id]/page.tsx:
   - Full transaction details
   - Merchant information
   - Timestamps (originated, processed)
   - Link to account and card

3. Create src/app/(dashboard)/transactions/new/page.tsx:
   - New transaction form matching COTRN02C
   - Fields:
     - Card Number (with lookup/validation)
     - Transaction Type (dropdown)
     - Category (dropdown, filtered by type)
     - Amount
     - Merchant ID, Name, City, ZIP
     - Description
   - Real-time validation:
     - Verify card exists (API call on blur)
     - Check credit limit before submit
     - Show available credit
   - Submit creates transaction
   - Show success with transaction ID or error with rejection code

4. Create src/components/forms/transaction-form.tsx:
   - Zod schema with all validations
   - Card number lookup with debounce
   - Display account info when card found
   - Calculate and show impact on balance
   - Handle rejection codes (100, 101, 102, 103) with user-friendly messages

5. Create src/hooks/use-transactions.ts:
   - useTransactions(filters) - paginated list
   - useTransaction(id) - single transaction
   - useCreateTransaction() - mutation with error handling
   - useValidateCard(cardNumber) - card lookup
```

**Prompt 12.1: Create Deployment Configuration**
```
Create production deployment configuration for AWS.

1. Dockerfile optimization:
   - Multi-stage build
   - JRE-only runtime image
   - Non-root user
   - Health check

2. Kubernetes manifests:
   - Deployment for API (3 replicas)
   - Deployment for batch workers (1 replica)
   - Service (ClusterIP)
   - Ingress with TLS
   - ConfigMap for application config
   - Secret for credentials
   - HorizontalPodAutoscaler
   - PodDisruptionBudget

3. Terraform for AWS:
   - VPC with public/private subnets
   - RDS PostgreSQL (Multi-AZ)
   - EKS cluster or ECS Fargate
   - ALB with WAF
   - S3 for statements
   - CloudWatch log groups
   - IAM roles and policies
   - Secrets Manager for credentials

4. CI/CD with GitHub Actions:
   - Build and test on PR
   - Build Docker image on merge
   - Push to ECR
   - Deploy to staging (automatic)
   - Deploy to production (manual approval)
   - Run database migrations
   - Smoke tests after deployment

5. Monitoring:
   - Prometheus metrics endpoint
   - Grafana dashboards
   - CloudWatch alarms
   - PagerDuty integration
```

---

## Troubleshooting Guide

### Common Migration Issues

**Issue: COMP-3 (Packed Decimal) Conversion Errors**
```
Symptom: Monetary amounts are incorrect after migration
Cause: Incorrect handling of packed decimal sign nibble or scale

Solution:
1. Verify JRecord configuration uses correct charset (cp037)
2. Check scale parameter matches copybook (V99 = scale 2)
3. Test with known values:
   - COBOL: PIC S9(5)V99 COMP-3 value +12345.67
   - Hex: 01 23 45 67 0C (C = positive sign)
   - Java: new BigDecimal("12345.67")
4. Use JRecord's getFieldValue() with explicit type conversion
```

**Issue: Character Encoding Problems**
```
Symptom: Special characters appear garbled
Cause: Wrong EBCDIC code page or missing conversion

Solution:
1. Verify source file code page (usually cp037 for US)
2. Configure JRecord: Convert.FMT_MAINFRAME, "cp037"
3. Trim trailing spaces from string fields
4. Test with records containing special characters
```

**Issue: Transaction Validation Mismatch**
```
Symptom: Java rejects transactions that COBOL accepted (or vice versa)
Cause: Business logic differences

Solution:
1. Compare validation code side-by-side with CBTRN02C.cbl
2. Check date comparison logic (inclusive vs exclusive)
3. Verify credit limit calculation matches exactly
4. Run parallel testing with same input file
5. Log detailed validation results for comparison
```

**Issue: Batch Job Performance**
```
Symptom: Spring Batch job runs much slower than JCL
Cause: Inefficient database operations or small chunk size

Solution:
1. Increase chunk size (try 500-1000)
2. Use batch inserts: spring.jpa.properties.hibernate.jdbc.batch_size=50
3. Disable auto-commit during batch
4. Add database indexes for lookup queries
5. Consider partitioning for parallel processing
```

**Issue: Concurrent Transaction Conflicts**
```
Symptom: OptimisticLockingFailureException during high volume
Cause: Multiple transactions updating same account simultaneously

Solution:
1. Implement retry logic with exponential backoff
2. Consider pessimistic locking for critical updates
3. Partition batch processing by account ID
4. Use database-level serialization for balance updates
```

---

## Document Information

**Version:** 2.0  
**Last Updated:** February 2026  
**Target Application Version:** CardDemo v2.0  
**Target Java Version:** Java 17 with Spring Boot 3.2

For questions or contributions, please refer to the [CONTRIBUTING.md](../CONTRIBUTING.md) file in the repository root.
