# CardDemo Application Architecture Document

## Executive Summary

CardDemo is a comprehensive mainframe credit card management application designed to simulate a production-grade card processing system used in the financial services industry. Built primarily using COBOL, CICS, VSAM, and JCL, it serves as a reference implementation for AWS and partner technologies in mainframe migration and modernization scenarios.

This document provides a detailed overview of the current architecture, including system components, data flows, processing patterns, and integration points.

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Core Technology Stack](#core-technology-stack)
4. [Application Subsystems](#application-subsystems)
5. [Data Architecture](#data-architecture)
6. [Online Processing (CICS)](#online-processing-cics)
7. [Batch Processing (JCL)](#batch-processing-jcl)
8. [Job Scheduling](#job-scheduling)
9. [Optional Extension Modules](#optional-extension-modules)
10. [Security Architecture](#security-architecture)
11. [Integration Patterns](#integration-patterns)

## System Overview

CardDemo implements a complete credit card management lifecycle including customer management, account administration, card issuance, transaction processing, bill payments, and statement generation. The application is structured around two primary processing modes: online transaction processing through CICS and batch processing through JCL jobs.

### Primary Use Cases

The application supports the following business functions:

**For Regular Users:**
- View and update account information
- Manage credit cards (list, view details, update)
- View, add, and process transactions
- Generate transaction reports
- Make bill payments
- View pending authorizations (with optional module)

**For Admin Users:**
- User management (list, add, update, delete)
- Transaction type management (with DB2 optional module)
- System administration functions

## Architecture Diagram

```
+-----------------------------------------------------------------------------------+
|                              CardDemo Application                                  |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  +---------------------------+     +---------------------------+                  |
|  |    Online Processing      |     |    Batch Processing       |                  |
|  |         (CICS)            |     |         (JCL)             |                  |
|  +---------------------------+     +---------------------------+                  |
|  |                           |     |                           |                  |
|  | +-------+  +-------+      |     | +-------+  +-------+      |                  |
|  | |CC00   |  |CM00   |      |     | |POSTTRAN|  |INTCALC|     |                  |
|  | |Signon |  |Menu   |      |     | |Trans   |  |Interest|    |                  |
|  | +-------+  +-------+      |     | |Posting |  |Calc    |    |                  |
|  |                           |     | +-------+  +-------+      |                  |
|  | +-------+  +-------+      |     |                           |                  |
|  | |CAVW   |  |CCLI   |      |     | +-------+  +-------+      |                  |
|  | |Account|  |Card   |      |     | |CREASTMT| |TRANBKP |     |                  |
|  | |View   |  |List   |      |     | |Statement| |Backup  |    |                  |
|  | +-------+  +-------+      |     | +-------+  +-------+      |                  |
|  |                           |     |                           |                  |
|  | +-------+  +-------+      |     | +-------+  +-------+      |                  |
|  | |CT00   |  |CB00   |      |     | |CBEXPORT| |CBIMPORT|     |                  |
|  | |Trans  |  |Bill   |      |     | |Data    | |Data    |     |                  |
|  | |List   |  |Payment|      |     | |Export  | |Import  |     |                  |
|  | +-------+  +-------+      |     | +-------+  +-------+      |                  |
|  +---------------------------+     +---------------------------+                  |
|                |                              |                                   |
|                v                              v                                   |
|  +-------------------------------------------------------------------+           |
|  |                        Data Layer (VSAM KSDS)                      |           |
|  +-------------------------------------------------------------------+           |
|  | +----------+ +----------+ +----------+ +----------+ +----------+  |           |
|  | | CUSTDATA | | ACCTDATA | | CARDDATA | | TRANSACT | | CARDXREF |  |           |
|  | | Customer | | Account  | | Card     | | Trans    | | Cross-Ref|  |           |
|  | | Master   | | Master   | | Master   | | Master   | | File     |  |           |
|  | +----------+ +----------+ +----------+ +----------+ +----------+  |           |
|  |                                                                    |           |
|  | +----------+ +----------+ +----------+ +----------+ +----------+  |           |
|  | | USRSEC   | | DALYTRAN | | DISCGRP  | | TCATBALF | | TRANTYPE |  |           |
|  | | User     | | Daily    | | Disclosure| | Trans Cat| | Trans    |  |           |
|  | | Security | | Trans    | | Groups   | | Balance  | | Types    |  |           |
|  | +----------+ +----------+ +----------+ +----------+ +----------+  |           |
|  +-------------------------------------------------------------------+           |
|                                                                                   |
+-----------------------------------------------------------------------------------+
|                         Optional Extension Modules                                |
+-----------------------------------------------------------------------------------+
|  +---------------------------+     +---------------------------+                  |
|  | IMS-DB2-MQ Authorization  |     | DB2 Transaction Type Mgmt |                  |
|  | - MQ Request/Response     |     | - Static Embedded SQL     |                  |
|  | - IMS Hierarchical DB     |     | - Cursor Processing       |                  |
|  | - DB2 Fraud Analytics     |     | - CRUD Operations         |                  |
|  +---------------------------+     +---------------------------+                  |
+-----------------------------------------------------------------------------------+
```

## Core Technology Stack

### Primary Technologies

| Technology | Purpose | Usage in CardDemo |
|------------|---------|-------------------|
| COBOL | Primary programming language | All business logic implementation |
| CICS | Transaction processing | Online user interactions via 3270 terminals |
| VSAM (KSDS with AIX) | Data storage | Primary indexed file storage for all master files |
| JCL | Batch processing | Job control for batch operations |
| RACF | Security | User authentication and authorization |
| Assembler | System utilities | Timer control (MVSWAIT), date conversion (COBDATFT) |

### Optional Technologies

| Technology | Purpose | Module |
|------------|---------|--------|
| DB2 | Relational database | Transaction Type Management, Fraud Analytics |
| IMS DB | Hierarchical database | Authorization storage |
| MQ | Message queuing | Authorization request/response processing |

### Data Formats

The application uses various mainframe data formats including COMP (binary), COMP-3 (packed decimal), zoned decimal, signed and unsigned numeric fields. Record formats include FB (Fixed Block), VB (Variable Block), and FBA.

## Application Subsystems

CardDemo is organized into four primary subsystems:

### 1. Online Processing Subsystem (CICS)

Handles interactive credit card account management through 3270 terminal interfaces. Programs follow the naming convention `CO*C.cbl` for online COBOL programs.

**Key Components:**
- Signon System (COSGN00C): User authentication and session management
- Main Menu (COMEN01C): Navigation hub for regular users
- Admin Menu (COADM01C): Administrative functions gateway
- Account Management (COACTVWC, COACTUPC): View and update customer accounts
- Card Management (COCRDLIC, COCRDSLC, COCRDUPC): Card lifecycle operations
- Transaction Processing (COTRN00C, COTRN01C, COTRN02C): Transaction inquiry and posting
- Payment System (COBIL00C): Bill payment processing
- User Management (COUSR00C-COUSR03C): User administration

### 2. Batch Processing Subsystem (JCL)

Handles daily transaction posting, interest calculations, statement generation, and data maintenance. Programs follow the naming convention `CB*C.cbl` for batch COBOL programs.

**Key Jobs:**
- POSTTRAN (CBTRN02C): Core daily transaction processing
- INTCALC (CBACT04C): Monthly interest calculation
- CREASTMT (CBSTM03A/B): Statement generation in text and HTML formats
- TRANBKP: Transaction backup
- COMBTRAN: Combine system transactions with daily ones

### 3. Data Migration Subsystem

Supports branch migration scenarios where customer data moves between systems.

**Export Process (CBEXPORT):**
1. Reads five normalized VSAM files (customer, account, card, cross-reference, transaction)
2. Consolidates into single multi-record export file (500-byte records)
3. Each record tagged with type identifier ('C', 'A', 'X', 'T', 'D')
4. Includes metadata: timestamp, sequence number, branch ID, region code

**Import Process (CBIMPORT):**
1. Reads consolidated export file
2. Demultiplexes by record type
3. Writes to separate normalized import files
4. Logs errors to separate error file for data quality tracking

### 4. Job Scheduling Subsystem

Manages automated execution of batch jobs using enterprise schedulers.

**Control-M Configuration:**
- Daily Jobs: Transaction backup (CLOSEFIL → TRANBKP → WAITSTEP → OPENFIL)
- Weekly Jobs: Transaction type refresh (Saturdays), disclosure group maintenance
- Monthly Jobs: Interest calculation, statement generation
- Uses condition-based dependencies (INCOND/OUTCOND)

**CA7 Configuration:**
- Legacy mainframe scheduler with completion-based triggers
- Complex dependency chains with parallel execution branches
- WAITSTEP jobs serve as synchronization hubs

## Data Architecture

### Master Data Files (VSAM KSDS)

| Dataset Name | Description | Copybook | Record Length | Key Field |
|--------------|-------------|----------|---------------|-----------|
| AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS | Customer master file | CVCUS01Y | 500 bytes | CUST-ID (9 digits) |
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | Account master file | CVACT01Y | 300 bytes | ACCT-ID (11 digits) |
| AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS | Card master file (with AIX) | CVACT02Y | 150 bytes | CARD-NUM (16 chars) |
| AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS | Card-to-account cross-reference (with AIX) | CVACT03Y | 50 bytes | XREF-CARD-NUM |
| AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | Transaction master file (with AIX) | CVTRA05Y | 350 bytes | TRAN-ID (16 chars) |
| AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS | User security file | CSUSR01Y | 80 bytes | SEC-USR-ID |

### Reference Data Files

| Dataset Name | Description | Copybook | Record Length |
|--------------|-------------|----------|---------------|
| AWS.M2.CARDDEMO.DISCGRP.PS | Disclosure Groups | CVTRA02Y | 50 bytes |
| AWS.M2.CARDDEMO.TRANCATG.PS | Transaction Category Types | CVTRA04Y | 60 bytes |
| AWS.M2.CARDDEMO.TRANTYPE.PS | Transaction Types | CVTRA03Y | 60 bytes |
| AWS.M2.CARDDEMO.TCATBALF.PS | Transaction Category Balance | CVTRA01Y | 50 bytes |

### Transactional Data Files

| Dataset Name | Description | Copybook | Record Length |
|--------------|-------------|----------|---------------|
| AWS.M2.CARDDEMO.DALYTRAN.PS | Daily transaction input file | CVTRA06Y | 350 bytes |
| AWS.M2.CARDDEMO.EXPORT.DATA.PS | Branch migration export file (EBCDIC) | CVEXPORT | 500 bytes |

### Generation Data Groups (GDG)

| GDG Base | Purpose |
|----------|---------|
| AWS.M2.CARDDEMO.TRANSACT.BKUP | Transaction backup history |
| AWS.M2.CARDDEMO.SYSTRAN | System transaction logs |
| AWS.M2.CARDDEMO.TRANREPT | Transaction reports |

### Entity Relationship Model

```
+-------------+       +-------------+       +-------------+
|  CUSTOMER   |       |   ACCOUNT   |       |    CARD     |
|-------------|       |-------------|       |-------------|
| CUST-ID (PK)|<---+  | ACCT-ID (PK)|<---+  | CARD-NUM(PK)|
| FIRST-NAME  |    |  | ACTIVE-STAT |    |  | ACCT-ID(FK) |
| MIDDLE-NAME |    |  | CURR-BAL    |    |  | CVV-CD      |
| LAST-NAME   |    |  | CREDIT-LIM  |    |  | EMBOSS-NAME |
| ADDR-LINE-1 |    |  | CASH-LIMIT  |    |  | EXPIRY-DATE |
| ADDR-LINE-2 |    |  | OPEN-DATE   |    |  | ACTIVE-STAT |
| ADDR-LINE-3 |    |  | EXPIRY-DATE |    |  +-------------+
| STATE-CD    |    |  | REISSUE-DT  |    |        |
| COUNTRY-CD  |    |  | CYC-CREDIT  |    |        |
| ZIP         |    |  | CYC-DEBIT   |    |        v
| PHONE-1     |    |  | ADDR-ZIP    |    |  +-------------+
| PHONE-2     |    |  | GROUP-ID    |    |  | CARD-XREF   |
| SSN         |    |  +-------------+    |  |-------------|
| GOVT-ID     |    |        ^            +--| CARD-NUM(PK)|
| DOB         |    |        |               | CUST-ID(FK) |
| EFT-ACCT-ID |    +--------+---------------| ACCT-ID(FK) |
| PRI-HOLDER  |                             +-------------+
| FICO-SCORE  |                                    |
+-------------+                                    |
                                                   v
                                            +-------------+
                                            | TRANSACTION |
                                            |-------------|
                                            | TRAN-ID (PK)|
                                            | TYPE-CD     |
                                            | CAT-CD      |
                                            | SOURCE      |
                                            | DESC        |
                                            | AMT         |
                                            | MERCHANT-ID |
                                            | MERCH-NAME  |
                                            | MERCH-CITY  |
                                            | MERCH-ZIP   |
                                            | CARD-NUM(FK)|
                                            | ORIG-TS     |
                                            | PROC-TS     |
                                            +-------------+
```

## Online Processing (CICS)

### Transaction Flow

The online system follows a standard CICS pseudo-conversational programming model:

1. **Initial Entry**: User enters transaction code (e.g., CC00 for signon)
2. **Map Send**: Program sends BMS map to terminal
3. **User Input**: User enters data and presses a function key
4. **Map Receive**: Program receives input from BMS map
5. **Processing**: Business logic executes
6. **Response**: Program sends response map or transfers control

### CICS Transactions

| Transaction | BMS Map | Program | Function |
|-------------|---------|---------|----------|
| CC00 | COSGN00 | COSGN00C | Signon Screen |
| CM00 | COMEN01 | COMEN01C | Main Menu |
| CAVW | COACTVW | COACTVWC | Account View |
| CAUP | COACTUP | COACTUPC | Account Update |
| CCLI | COCRDLI | COCRDLIC | Credit Card List |
| CCDL | COCRDSL | COCRDSLC | Credit Card View |
| CCUP | COCRDUP | COCRDUPC | Credit Card Update |
| CT00 | COTRN00 | COTRN00C | Transaction List |
| CT01 | COTRN01 | COTRN01C | Transaction View |
| CT02 | COTRN02 | COTRN02C | Transaction Add |
| CR00 | CORPT00 | CORPT00C | Transaction Reports |
| CB00 | COBIL00 | COBIL00C | Bill Payment |
| CA00 | COADM01 | COADM01C | Admin Menu |
| CU00 | COUSR00 | COUSR00C | List Users |
| CU01 | COUSR01 | COUSR01C | Add User |
| CU02 | COUSR02 | COUSR02C | Update User |
| CU03 | COUSR03 | COUSR03C | Delete User |

### Program Structure Pattern

All CICS programs follow a consistent structure:

```cobol
PROCEDURE DIVISION.
    PERFORM 0000-MAIN-PARA.
    GOBACK.

0000-MAIN-PARA.
    EVALUATE EIBCALEN
        WHEN 0 PERFORM 1000-SEND-MAP
        WHEN OTHER PERFORM 2000-RECEIVE-MAP
    END-EVALUATE.

1000-SEND-MAP.
    EXEC CICS SEND MAP('mapname') 
              MAPSET('mapset')
              ERASE
    END-EXEC.

2000-RECEIVE-MAP.
    EXEC CICS RECEIVE MAP('mapname')
                      MAPSET('mapset')
                      INTO(WS-MAP-AREA)
    END-EXEC.
```

### COMMAREA Structure

Programs pass data between transactions using a common communication area (COMMAREA) defined in COCOM01Y.cpy:

- User identification and type
- Source transaction and program
- Destination transaction and program
- Program context flags
- Account and card selection data

## Batch Processing (JCL)

### Daily Batch Cycle

The standard daily batch processing sequence:

```
CLOSEFIL  -->  TRANBKP  -->  POSTTRAN  -->  TRANIDX  -->  OPENFIL
    |              |             |              |             |
    v              v             v              v             v
Close CICS    Backup       Post daily     Define AIX    Reopen
file access   transactions transactions   on trans file files
```

### Key Batch Programs

#### CBTRN02C - Transaction Posting

The core transaction posting program reads daily transactions and posts them to the master transaction file:

1. Opens input files (DALYTRAN, XREF, ACCOUNT) and output files (TRANSACT, DALYREJS)
2. Reads each daily transaction record
3. Validates transaction against cross-reference file
4. Validates account exists and is active
5. Posts valid transactions to transaction master
6. Writes rejected transactions to reject file with reason codes
7. Updates account balances

#### CBACT04C - Interest Calculation

Monthly interest calculation program:

1. Reads transaction category balance file sequentially
2. For each account, retrieves disclosure group interest rates
3. Computes monthly interest based on balance and rate
4. Updates account master with new balance
5. Creates interest transaction records

#### CBSTM03A/B - Statement Generation

Statement generation using main program and I/O subroutine:

1. CBSTM03A: Main processing logic, statement formatting
2. CBSTM03B: File I/O operations (called subroutine)
3. Generates both plain text and HTML format statements
4. Uses ALTER statements for dynamic flow control
5. Demonstrates mainframe control block addressing

### Batch Job Dependencies

| Job | Depends On | Produces |
|-----|------------|----------|
| CLOSEFIL | - | Files closed for batch |
| TRANBKP | CLOSEFIL | Transaction backup GDG |
| POSTTRAN | TRANBKP | Posted transactions |
| INTCALC | POSTTRAN | Interest transactions |
| CREASTMT | INTCALC | Statement files |
| OPENFIL | CREASTMT | Files reopened for online |

## Job Scheduling

### Control-M Configuration

The application includes Control-M job definitions organized into folders:

**DAILY-TransactionBackup Folder:**
- CLOSEFIL: Closes CICS file access
- TRANBKP: Creates transaction backup
- WAITSTEP: Synchronization point
- OPENFIL: Reopens files for CICS

**WEEKLY-TransactionTypesDBRefresh Folder:**
- MNTTRDB2: Maintains transaction type DB2 tables

**WEEKLY-DisclosureGroupsRefresh Folder:**
- CLOSEFIL: Closes files
- DISCGRP: Refreshes disclosure groups
- WAITSTEP: Synchronization
- OPENFIL: Reopens files

### Job Scheduling Pattern

```
Daily Schedule (All Days):
  CLOSEFIL [23:00] 
      --> TRANBKP 
          --> WAITSTEP 
              --> OPENFIL

Weekly Schedule (Saturdays):
  MNTTRDB2 
      --> CLOSEFIL 
          --> DISCGRP 
              --> WAITSTEP 
                  --> OPENFIL

Monthly Schedule (1st of Month):
  INTCALC 
      --> CREASTMT
```

## Optional Extension Modules

### 1. Credit Card Authorizations (IMS-DB2-MQ)

This extension adds real-time authorization processing capabilities:

**Components:**
- COPAUA0C: MQ-triggered authorization processor
- COPAUS0C: Authorization summary display (CPVS transaction)
- COPAUS1C: Authorization details display (CPVD transaction)
- COPAUS2C: Fraud marking and DB2 update
- CBPAUP0C: Batch purge of expired authorizations

**Data Flow:**
1. Cloud-based POS emulator sends authorization request via MQ
2. CICS program processes request triggered by MQ message
3. Account and customer data retrieved via VSAM cross-reference
4. Business rules applied to approve/decline
5. Response sent back via reply MQ queue
6. Authorization details stored in IMS database
7. Fraud cases logged to DB2 for analytics

**IMS Database Structure:**
- DBPAUTP0 (HIDAM): Primary authorization database
- DBPAUTX0 (HIDAM Index): Index for authorization database
- Segments: PAUTSUM0 (root), PAUTDTL1 (child)

**DB2 Table:**
- AUTHFRDS: Fraud tracking table with card number, timestamp, authorization details

### 2. Transaction Type Management (DB2)

This extension demonstrates DB2 integration patterns:

**Components:**
- COTRTUPC: Transaction type add/edit (CTTU transaction)
- COTRTLIC: Transaction type list/update/delete (CTLI transaction)
- COBTUPDT: Batch maintenance program

**DB2 Tables:**
- TRANSACTION_TYPE: Transaction type codes and descriptions
- TRANSACTION_TYPE_CATEGORY: Transaction categories with foreign key to types

**Integration Patterns Demonstrated:**
- Static embedded SQL with host variables
- Forward and backward cursor processing
- CRUD operations with proper error handling
- DB2 precompiler integration in CICS

### 3. Account Extractions (MQ-VSAM)

Demonstrates asynchronous processing patterns:

**Transactions:**
- CDRD (CODATE01): System date inquiry via MQ
- CDRA (COACCT01): Account details inquiry via MQ

## Security Architecture

### User Authentication

The application implements a simple security model using the USRSEC VSAM file:

**User Record Structure (CSUSR01Y.cpy):**
- SEC-USR-ID: 8-character user identifier
- SEC-USR-FNAME: First name (20 characters)
- SEC-USR-LNAME: Last name (20 characters)
- SEC-USR-PWD: Password (8 characters)
- SEC-USR-TYPE: User type ('A' for Admin, 'U' for User)

### Authentication Flow

1. User enters credentials on signon screen (CC00)
2. COSGN00C reads USRSEC file using user ID as key
3. Password comparison performed
4. User type determines menu access (Admin vs Regular)
5. COMMAREA populated with user context for subsequent transactions

### Authorization Model

| User Type | Menu Access | Functions |
|-----------|-------------|-----------|
| Admin (A) | COADM01C | User management, Transaction type management |
| User (U) | COMEN01C | Account view/update, Card management, Transactions, Payments |

### Default Credentials

| User ID | Password | Type |
|---------|----------|------|
| ADMIN001 | PASSWORD | Admin |
| USER0001 | PASSWORD | User |

## Integration Patterns

### File I/O Pattern

All programs follow a consistent file I/O pattern:

```cobol
1000-OPEN-FILES.
    OPEN INPUT CUSTOMER-FILE
         I-O TRANSACTION-FILE
    IF NOT FILE-STATUS-OK
        PERFORM 9999-ABEND-PROGRAM
    END-IF.

2000-READ-RECORD.
    READ CUSTOMER-FILE INTO WS-CUSTOMER-RECORD
    EVALUATE FILE-STATUS
        WHEN '00' CONTINUE
        WHEN '10' SET END-OF-FILE TO TRUE
        WHEN OTHER PERFORM 9999-ABEND-PROGRAM
    END-EVALUATE.

9000-CLOSE-FILES.
    CLOSE CUSTOMER-FILE TRANSACTION-FILE.
```

### Error Handling

Programs implement standard error handling:

- File status checking after every I/O operation
- CICS RESP/RESP2 checking for CICS commands
- SQLCA checking for DB2 operations
- Abend processing with diagnostic display

### Cross-Reference Navigation

The CARDXREF file enables navigation between entities:

```
Card Number --> CARDXREF --> Customer ID + Account ID
                                |              |
                                v              v
                           CUSTDATA       ACCTDATA
```

### Batch-Online Coordination

The CLOSEFIL/OPENFIL pattern ensures data integrity:

1. CLOSEFIL job closes VSAM files in CICS (CEMT SET FILE CLOSED)
2. Batch jobs process files with exclusive access
3. OPENFIL job reopens files in CICS (CEMT SET FILE OPEN)

## Repository Structure

```
aws-mainframe-modernization-carddemo/
├── app/
│   ├── cbl/              # COBOL source programs (31 programs)
│   ├── cpy/              # COBOL copybooks (30 copybooks)
│   ├── bms/              # BMS screen definitions (17 maps)
│   ├── jcl/              # JCL job scripts (38 jobs)
│   ├── data/             # Sample data files
│   │   ├── ASCII/        # ASCII format data
│   │   └── EBCDIC/       # EBCDIC format data (14 files)
│   ├── catlg/            # Data catalog listings
│   ├── scheduler/        # Job scheduling definitions
│   │   ├── CardDemo.controlm
│   │   └── CardDemo.ca7
│   ├── app-authorization-ims-db2-mq/  # Optional IMS-DB2-MQ module
│   ├── app-transaction-type-db2/       # Optional DB2 module
│   └── app-vsam-mq/                    # Optional MQ-VSAM module
├── diagrams/             # Application flow diagrams
├── samples/              # Runtime demonstration packages
│   └── m2/
│       ├── mf/          # Micro Focus runtime
│       └── unikix/      # UniKix runtime
└── README.md            # Primary documentation
```

## Technical Highlights Summary

| Component | Domain Features | Technical Features |
|-----------|-----------------|-------------------|
| Base Application | Customer, Account, Card, Transaction, Bill Payment, Statement/Report | COBOL, CICS, JCL (Batch), VSAM (KSDS with AIX) |
| Optional Features | Authorization, Fraud, Transaction Type Extension | DB2, MQ, IMS DB, JCL Utilities, Complex data formats, Various dataset types, Advanced copybook structures |

## Conclusion

CardDemo provides a comprehensive mainframe application architecture that demonstrates real-world patterns used in financial services. Its modular design with core VSAM-based processing and optional DB2/IMS/MQ extensions makes it an ideal reference implementation for modernization initiatives. The application's intentional use of various coding styles and patterns exercises analysis, transformation, and migration tooling across different mainframe programming paradigms.
