# CardDemo Application - Knowledge Base Documentation

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [System Components](#system-components)
4. [Data Model](#data-model)
5. [Online Processing (CICS)](#online-processing-cics)
6. [Batch Processing (JCL)](#batch-processing-jcl)
7. [Data Migration System](#data-migration-system)
8. [Job Scheduling](#job-scheduling)
9. [Security Model](#security-model)
10. [Optional Modules](#optional-modules)
11. [Technical Reference](#technical-reference)
12. [Glossary](#glossary)

---

## Executive Summary

CardDemo is a comprehensive mainframe credit card management application designed specifically to showcase AWS and partner technologies for mainframe migration and modernization scenarios. Built primarily in COBOL with CICS for online transaction processing and JCL for batch operations, it simulates a production-grade card processing system used in the financial services industry.

The application serves as a reference implementation for migration engineers evaluating mainframe modernization projects, solution architects designing hybrid mainframe-cloud architectures, technology partners developing modernization tools, and training organizations teaching mainframe application patterns. CardDemo intentionally incorporates diverse coding styles and patterns to provide comprehensive coverage for testing analysis, transformation, and migration tooling across different mainframe programming paradigms.

---

## Architecture Overview

### High-Level Architecture

CardDemo follows a traditional mainframe application architecture with three primary processing modes:

**Online Transaction Processing (OLTP)** handles real-time user interactions through CICS terminals. Users access the system via 3270 terminal emulators, navigating through BMS-defined screens to perform account inquiries, card management, transaction processing, and administrative functions. Each screen maps to a specific COBOL program that processes user input and updates VSAM files.

**Batch Processing** executes scheduled jobs that perform bulk data operations. These include daily transaction posting, interest calculations, statement generation, and data maintenance. Batch jobs run during off-peak hours when CICS files are closed to prevent data conflicts.

**Data Migration** provides export and import capabilities for branch migration scenarios, demonstrating data transfer patterns between systems using multi-record export files.

### Technology Stack

The core technologies include COBOL as the primary programming language, CICS for transaction processing, VSAM KSDS with Alternate Indexes for data storage, JCL for batch job control, and RACF-style security. Optional technologies extend the base functionality with DB2 for relational database management, IMS DB for hierarchical database operations, and MQ for message queuing.

### Directory Structure

```
aws-mainframe-modernization-carddemo/
├── app/
│   ├── cbl/              # COBOL source programs (31 programs)
│   ├── cpy/              # COBOL copybooks (30 copybooks)
│   ├── bms/              # BMS screen definitions (17 maps)
│   ├── jcl/              # JCL batch jobs (37 jobs)
│   ├── data/             # Sample data files
│   │   ├── ASCII/        # Human-readable data
│   │   └── EBCDIC/       # Mainframe-format data
│   ├── catlg/            # Data catalog listings
│   ├── scheduler/        # Job scheduling configurations
│   ├── asm/              # Assembler programs
│   ├── csd/              # CICS resource definitions
│   └── app-*/            # Optional module directories
├── samples/              # Runtime demonstration packages
├── scripts/              # Build and deployment scripts
└── diagrams/             # Application flow diagrams
```

---

## System Components

### COBOL Programs

CardDemo contains 31 COBOL programs divided into online (CICS) and batch categories. Online programs follow the naming convention ending with 'C' (e.g., COSGN00C) and handle interactive user sessions. Batch programs process bulk data operations and typically run as scheduled jobs.

#### Online Programs (CICS)

| Program | Transaction | Function | Description |
|---------|-------------|----------|-------------|
| COSGN00C | CC00 | Sign-on | User authentication and session initialization |
| COMEN01C | CM00 | Main Menu | Navigation hub for regular users |
| COADM01C | CA00 | Admin Menu | Navigation hub for administrators |
| COACTVWC | CAVW | Account View | Display account details |
| COACTUPC | CAUP | Account Update | Modify account information |
| COCRDLIC | CCLI | Card List | Display cards for an account |
| COCRDSLC | CCDL | Card Details | View individual card information |
| COCRDUPC | CCUP | Card Update | Modify card details |
| COTRN00C | CT00 | Transaction List | Display transaction history |
| COTRN01C | CT01 | Transaction View | View transaction details |
| COTRN02C | CT02 | Transaction Add | Create new transactions |
| CORPT00C | CR00 | Reports | Generate transaction reports |
| COBIL00C | CB00 | Bill Payment | Process bill payments |
| COUSR00C | CU00 | User List | Display system users |
| COUSR01C | CU01 | User Add | Create new users |
| COUSR02C | CU02 | User Update | Modify user information |
| COUSR03C | CU03 | User Delete | Remove users from system |

#### Batch Programs

| Program | Job | Function | Description |
|---------|-----|----------|-------------|
| CBTRN02C | POSTTRAN | Transaction Posting | Core daily transaction processing |
| CBACT04C | INTCALC | Interest Calculation | Monthly interest computation |
| CBSTM03A | CREASTMT | Statement Generation | Customer statement creation (main) |
| CBSTM03B | CREASTMT | Statement I/O | Statement file operations (subroutine) |
| CBTRN03C | TRANREPT | Transaction Report | Batch transaction reporting |
| CBEXPORT | CBEXPORT | Data Export | Branch migration export |
| CBIMPORT | CBIMPORT | Data Import | Branch migration import |
| COBSWAIT | WAITSTEP | Wait Timer | Job synchronization utility |

### Copybooks

Copybooks define reusable data structures shared across programs. They establish the record layouts for VSAM files, communication areas, and screen mappings.

#### Entity Data Structures

| Copybook | Entity | Record Length | Key Field |
|----------|--------|---------------|-----------|
| CVCUS01Y | Customer | 500 bytes | CUST-ID (9 digits) |
| CVACT01Y | Account | 300 bytes | ACCT-ID (11 digits) |
| CVACT02Y | Card | 150 bytes | CARD-NUM (16 chars) |
| CVACT03Y | Card Cross-Reference | 50 bytes | XREF-CARD-NUM |
| CVTRA05Y | Transaction | 350 bytes | TRAN-ID (16 chars) |
| CVTRA06Y | Daily Transaction | 350 bytes | DALYTRAN-ID |
| CVEXPORT | Export Record | 500 bytes | EXPORT-SEQUENCE-NUM |

#### System Copybooks

| Copybook | Purpose |
|----------|---------|
| COCOM01Y | CICS communication area (COMMAREA) |
| COMEN02Y | Main menu options definition |
| CSUSR01Y | User security record structure |
| COTTL01Y | Screen title definitions |
| CSDAT01Y | Date formatting utilities |
| CSMSG01Y | Standard message definitions |

### BMS Maps

Basic Mapping Support (BMS) maps define the 3270 terminal screen layouts. Each map corresponds to a specific transaction and COBOL program.

| Map | Mapset | Screen | Fields |
|-----|--------|--------|--------|
| COSGN0A | COSGN00 | Sign-on | User ID, Password |
| COMEN1A | COMEN01 | Main Menu | Menu options 1-11 |
| COADM1A | COADM01 | Admin Menu | Admin options |
| COACTVW | COACTVW | Account View | Account details display |
| COACTUP | COACTUP | Account Update | Editable account fields |
| COCRDLI | COCRDLI | Card List | Card listing grid |
| COCRDSL | COCRDSL | Card Details | Card information display |
| COCRDUP | COCRDUP | Card Update | Editable card fields |
| COTRN00 | COTRN00 | Transaction List | Transaction grid |
| COTRN01 | COTRN01 | Transaction View | Transaction details |
| COTRN02 | COTRN02 | Transaction Add | New transaction form |

---

## Data Model

### Entity Relationship Overview

CardDemo manages five primary entities with the following relationships:

**Customer** (CVCUS01Y) represents individuals who hold credit card accounts. Each customer has a unique 9-digit identifier and contains personal information including name, address, phone numbers, SSN, date of birth, and FICO credit score.

**Account** (CVACT01Y) represents credit card accounts linked to customers. Each account has an 11-digit identifier and tracks financial information including current balance, credit limit, cash credit limit, cycle credits/debits, and important dates (open, expiration, reissue).

**Card** (CVACT02Y) represents physical credit cards issued on accounts. Each card has a 16-character card number, CVV code, embossed name, expiration date, and active status.

**Card Cross-Reference** (CVACT03Y) links cards to customers and accounts, enabling navigation from card number to account and customer records.

**Transaction** (CVTRA05Y) records financial activities on accounts including purchases, payments, and adjustments. Each transaction has a 16-character ID, type code, category, amount, merchant information, and timestamps.

### VSAM File Definitions

| Dataset Name | Type | Key | Record Length |
|--------------|------|-----|---------------|
| AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS | KSDS | CUST-ID | 500 |
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | KSDS | ACCT-ID | 300 |
| AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS | KSDS + AIX | CARD-NUM | 150 |
| AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS | KSDS + AIX | XREF-CARD-NUM | 50 |
| AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | KSDS + AIX | TRAN-ID | 350 |
| AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS | KSDS | SEC-USR-ID | 80 |
| AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS | KSDS | Composite | 50 |
| AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS | KSDS | DISCGRP-KEY | 50 |
| AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS | KSDS | TRANCATG-KEY | 60 |
| AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS | KSDS | TRANTYPE-KEY | 60 |

### Record Layouts

#### Customer Record (CVCUS01Y - 500 bytes)

```
01  CUSTOMER-RECORD.
    05  CUST-ID                    PIC 9(09).      Positions 1-9
    05  CUST-FIRST-NAME            PIC X(25).      Positions 10-34
    05  CUST-MIDDLE-NAME           PIC X(25).      Positions 35-59
    05  CUST-LAST-NAME             PIC X(25).      Positions 60-84
    05  CUST-ADDR-LINE-1           PIC X(50).      Positions 85-134
    05  CUST-ADDR-LINE-2           PIC X(50).      Positions 135-184
    05  CUST-ADDR-LINE-3           PIC X(50).      Positions 185-234
    05  CUST-ADDR-STATE-CD         PIC X(02).      Positions 235-236
    05  CUST-ADDR-COUNTRY-CD       PIC X(03).      Positions 237-239
    05  CUST-ADDR-ZIP              PIC X(10).      Positions 240-249
    05  CUST-PHONE-NUM-1           PIC X(15).      Positions 250-264
    05  CUST-PHONE-NUM-2           PIC X(15).      Positions 265-279
    05  CUST-SSN                   PIC 9(09).      Positions 280-288
    05  CUST-GOVT-ISSUED-ID        PIC X(20).      Positions 289-308
    05  CUST-DOB-YYYY-MM-DD        PIC X(10).      Positions 309-318
    05  CUST-EFT-ACCOUNT-ID        PIC X(10).      Positions 319-328
    05  CUST-PRI-CARD-HOLDER-IND   PIC X(01).      Position 329
    05  CUST-FICO-CREDIT-SCORE     PIC 9(03).      Positions 330-332
    05  FILLER                     PIC X(168).     Positions 333-500
```

#### Account Record (CVACT01Y - 300 bytes)

```
01  ACCOUNT-RECORD.
    05  ACCT-ID                    PIC 9(11).      Positions 1-11
    05  ACCT-ACTIVE-STATUS         PIC X(01).      Position 12
    05  ACCT-CURR-BAL              PIC S9(10)V99.  Positions 13-24
    05  ACCT-CREDIT-LIMIT          PIC S9(10)V99.  Positions 25-36
    05  ACCT-CASH-CREDIT-LIMIT     PIC S9(10)V99.  Positions 37-48
    05  ACCT-OPEN-DATE             PIC X(10).      Positions 49-58
    05  ACCT-EXPIRAION-DATE        PIC X(10).      Positions 59-68
    05  ACCT-REISSUE-DATE          PIC X(10).      Positions 69-78
    05  ACCT-CURR-CYC-CREDIT       PIC S9(10)V99.  Positions 79-90
    05  ACCT-CURR-CYC-DEBIT        PIC S9(10)V99.  Positions 91-102
    05  ACCT-ADDR-ZIP              PIC X(10).      Positions 103-112
    05  ACCT-GROUP-ID              PIC X(10).      Positions 113-122
    05  FILLER                     PIC X(178).     Positions 123-300
```

#### Transaction Record (CVTRA05Y - 350 bytes)

```
01  TRAN-RECORD.
    05  TRAN-ID                    PIC X(16).      Positions 1-16
    05  TRAN-TYPE-CD               PIC X(02).      Positions 17-18
    05  TRAN-CAT-CD                PIC 9(04).      Positions 19-22
    05  TRAN-SOURCE                PIC X(10).      Positions 23-32
    05  TRAN-DESC                  PIC X(100).     Positions 33-132
    05  TRAN-AMT                   PIC S9(09)V99.  Positions 133-143
    05  TRAN-MERCHANT-ID           PIC 9(09).      Positions 144-152
    05  TRAN-MERCHANT-NAME         PIC X(50).      Positions 153-202
    05  TRAN-MERCHANT-CITY         PIC X(50).      Positions 203-252
    05  TRAN-MERCHANT-ZIP          PIC X(10).      Positions 253-262
    05  TRAN-CARD-NUM              PIC X(16).      Positions 263-278
    05  TRAN-ORIG-TS               PIC X(26).      Positions 279-304
    05  TRAN-PROC-TS               PIC X(26).      Positions 305-330
    05  FILLER                     PIC X(20).      Positions 331-350
```

---

## Online Processing (CICS)

### Transaction Flow

The online system follows a pseudo-conversational programming model where each user interaction triggers a new transaction. The COMMAREA (Communication Area) maintains state between transactions, passing context from one program invocation to the next.

#### Sign-on Process (CC00 - COSGN00C)

The sign-on transaction authenticates users and establishes their session context. When a user enters the CC00 transaction, the program displays the sign-on screen (COSGN0A) requesting User ID and Password. Upon submission, the program reads the USRSEC file to validate credentials. If authentication succeeds, the program determines the user type (Admin or Regular) and transfers control to the appropriate menu program.

```
User enters CC00 → Display COSGN0A screen → User enters credentials
    → Read USRSEC file → Validate password
    → If Admin: XCTL to COADM01C (Admin Menu)
    → If User: XCTL to COMEN01C (Main Menu)
```

#### Main Menu Navigation (CM00 - COMEN01C)

The main menu presents available functions based on user type. Regular users see options 1-11 covering account management, card operations, transactions, reports, and bill payment. The menu options are defined in COMEN02Y copybook, which specifies the option number, display name, target program, and required user type.

Available menu options include Account View, Account Update, Credit Card List, Credit Card View, Credit Card Update, Transaction List, Transaction View, Transaction Add, Transaction Reports, Bill Payment, and Pending Authorization View (optional module).

#### Account Operations

Account View (CAVW - COACTVWC) retrieves and displays account information by reading the ACCTDATA VSAM file using the account ID from the COMMAREA. Account Update (CAUP - COACTUPC) allows modification of account fields with validation before rewriting the record.

#### Card Operations

Card List (CCLI - COCRDLIC) displays all cards associated with an account by browsing the CARDDATA file using the alternate index on account ID. Card Details (CCDL - COCRDSLC) shows individual card information. Card Update (CCUP - COCRDUPC) enables modification of card attributes.

#### Transaction Operations

Transaction List (CT00 - COTRN00C) displays transaction history for an account. Transaction View (CT01 - COTRN01C) shows detailed information for a selected transaction. Transaction Add (CT02 - COTRN02C) creates new transactions, validating the card number against the cross-reference file and checking credit limits before writing to the transaction file.

### Screen Layout Standards

All CardDemo screens follow a consistent layout with a header section (lines 1-3) displaying transaction ID, program name, application title, date, time, application ID, and system ID. The body section (lines 5-22) contains the functional content specific to each screen. The message area (line 23) displays error messages and status information. The footer (line 24) shows available function keys.

### Error Handling

Online programs use CICS RESP and RESP2 codes to detect and handle errors. Common response codes include DFHRESP(NORMAL) for successful operations, DFHRESP(NOTFND) when a record is not found, DFHRESP(DUPREC) for duplicate key violations, and DFHRESP(INVREQ) for invalid requests. Error messages are displayed in the screen's ERRMSG field with appropriate color highlighting (typically red for errors).

---

## Batch Processing (JCL)

### Job Categories

CardDemo batch jobs fall into several categories based on their function:

**File Management Jobs** handle VSAM file operations including CLOSEFIL (close files for batch processing), OPENFIL (reopen files for CICS), and various file loading jobs (ACCTFILE, CARDFILE, CUSTFILE, XREFFILE, TRANFILE).

**Transaction Processing Jobs** include POSTTRAN (daily transaction posting), INTCALC (interest calculation), and COMBTRAN (combine transaction files).

**Reporting Jobs** include CREASTMT (statement generation) and TRANREPT (transaction reports).

**Data Migration Jobs** include CBEXPORT (export data) and CBIMPORT (import data).

**Utility Jobs** include DEFGDGB/DEFGDGD (define GDG bases), TRANIDX (define alternate indexes), and WAITSTEP (job synchronization).

### Core Batch Job: POSTTRAN

The POSTTRAN job executes the CBTRN02C program to process daily transactions. This is the most critical batch job in the system, responsible for posting transactions from the daily transaction file to the transaction master.

**Input Files:**
- DALYTRAN: Daily transaction file (sequential)
- XREFFILE: Card cross-reference (VSAM KSDS)
- ACCTFILE: Account master (VSAM KSDS)
- TCATBALF: Transaction category balance (VSAM KSDS)

**Output Files:**
- TRANFILE: Transaction master (VSAM KSDS)
- DALYREJS: Rejected transactions (sequential)

**Processing Logic:**

1. Open all input and output files
2. Read each record from DALYTRAN
3. Validate the transaction:
   - Look up card number in XREFFILE
   - Verify account exists in ACCTFILE
   - Check credit limit not exceeded
   - Verify account not expired
4. If valid, post transaction:
   - Update TCATBALF with category balance
   - Update ACCTFILE with new balance
   - Write to TRANFILE
5. If invalid, write to DALYREJS with reason code
6. Close all files and display statistics

**Validation Codes:**
- 100: Invalid card number
- 101: Account record not found
- 102: Over-limit transaction
- 103: Transaction after account expiration

### Interest Calculation: INTCALC

The INTCALC job executes CBACT04C to calculate monthly interest on account balances. It reads each account record, computes interest based on the current balance and applicable rate, and updates the account with the new balance.

### Statement Generation: CREASTMT

The CREASTMT job uses CBSTM03A (main program) and CBSTM03B (I/O subroutine) to generate customer statements. The job reads transaction records, groups them by account, and produces formatted statements in both text and HTML formats.

### Batch Job Sequence

A typical daily batch cycle follows this sequence:

1. CLOSEFIL - Close VSAM files in CICS
2. TRANBKP - Backup transaction file
3. POSTTRAN - Post daily transactions
4. TRANIDX - Rebuild alternate indexes
5. OPENFIL - Reopen files in CICS

A monthly cycle adds:
1. INTCALC - Calculate interest
2. CREASTMT - Generate statements

---

## Data Migration System

### Export Process (CBEXPORT)

The CBEXPORT program consolidates data from five normalized VSAM files into a single multi-record export file for branch migration scenarios. This demonstrates data transfer patterns between mainframe systems.

**Source Files:**
- CUSTFILE: Customer master
- ACCTFILE: Account master
- CARDFILE: Card master
- XREFFILE: Cross-reference
- TRANSACT: Transaction master

**Output File:**
- EXPFILE: Multi-record export file (500-byte records)

**Export Record Structure (CVEXPORT.cpy):**

Each export record contains a common header followed by entity-specific data:

```
01  EXPORT-RECORD.
    05  EXPORT-REC-TYPE            PIC X(1).       Record type indicator
    05  EXPORT-TIMESTAMP           PIC X(26).      Export timestamp
    05  EXPORT-SEQUENCE-NUM        PIC 9(9) COMP.  Sequence number (key)
    05  EXPORT-BRANCH-ID           PIC X(4).       Source branch
    05  EXPORT-REGION-CODE         PIC X(5).       Region code
    05  EXPORT-RECORD-DATA         PIC X(460).     Entity data (REDEFINES)
```

**Record Type Indicators:**
- 'C' - Customer record
- 'A' - Account record
- 'X' - Cross-reference record
- 'T' - Transaction record
- 'D' - Card record

The export process reads each source file sequentially, maps the fields to the appropriate REDEFINES structure in EXPORT-RECORD-DATA, assigns a sequence number, and writes to the export file. Statistics are maintained for each record type and displayed upon completion.

### Import Process (CBIMPORT)

The CBIMPORT program reverses the export process, reading the consolidated export file and demultiplexing records into separate normalized files based on the record type indicator.

**Input File:**
- EXPFILE: Multi-record export file

**Output Files:**
- CUSTOUT: Customer import file
- ACCTOUT: Account import file
- CARDOUT: Card import file
- XREFOUT: Cross-reference import file
- TRANOUT: Transaction import file
- ERROROUT: Error/rejected records

**Processing Logic:**

1. Open export file and all output files
2. Read each export record
3. Evaluate EXPORT-REC-TYPE:
   - 'C': Write to CUSTOUT
   - 'A': Write to ACCTOUT
   - 'X': Write to XREFOUT
   - 'T': Write to TRANOUT
   - 'D': Write to CARDOUT
   - Other: Write to ERROROUT
4. Maintain statistics by record type
5. Close all files and display summary

---

## Job Scheduling

### Control-M Configuration

The CardDemo.controlm file defines job schedules using Control-M's XML format. Jobs are organized into folders representing processing cycles.

**DAILY-TransactionBackup Folder:**

This folder contains the daily transaction backup workflow with the following job chain:

```
CLOSEFIL → TRANBKP → WAITSTEP → OPENFIL
```

Each job uses INCOND (input condition) and OUTCOND (output condition) to establish dependencies. For example, TRANBKP has INCOND "DAILY-TransactionBackup-CLOSEFIL" and OUTCOND "DAILY-TransactionBackup-TRANBKP", ensuring it runs only after CLOSEFIL completes and signals TRANBKP completion for downstream jobs.

**WEEKLY-TransactionTypesDBRefresh Folder:**

Contains the MNTTRDB2 job for maintaining transaction type reference data in DB2. Runs on Saturdays.

**WEEKLY-DisclosureGroupsRefresh Folder:**

Contains the disclosure group maintenance workflow:

```
CLOSEFIL → DISCGRP → WAITSTEP → OPENFIL
```

**MONTHLY-InterestCalculation Folder:**

Contains the monthly interest calculation and statement generation workflow:

```
CLOSEFIL → INTCALC → CREASTMT → WAITSTEP → OPENFIL
```

### CA7 Configuration

The CardDemo.ca7 file provides an alternative scheduling configuration using CA7's legacy format. It defines similar job chains with completion-based triggers rather than condition-based dependencies.

### WAITSTEP Synchronization

The WAITSTEP job (COBSWAIT program) serves as a synchronization point in batch workflows. It pauses execution for a specified duration, allowing parallel job branches to complete before proceeding. This is particularly useful when multiple independent jobs must finish before a common successor can run.

---

## Security Model

### User Authentication

CardDemo implements a simple file-based security model using the USRSEC VSAM file. Each user record (CSUSR01Y copybook) contains:

```
01 SEC-USER-DATA.
   05 SEC-USR-ID          PIC X(08).    User ID (key)
   05 SEC-USR-FNAME       PIC X(20).    First name
   05 SEC-USR-LNAME       PIC X(20).    Last name
   05 SEC-USR-PWD         PIC X(08).    Password
   05 SEC-USR-TYPE        PIC X(01).    User type (A/U)
   05 SEC-USR-FILLER      PIC X(23).    Reserved
```

### User Types

**Admin Users (Type 'A'):** Have access to all functions including user management and optional DB2 transaction type maintenance. After sign-on, admin users are directed to the Admin Menu (COADM01C).

**Regular Users (Type 'U'):** Have access to standard card management functions but cannot access administrative features. After sign-on, regular users are directed to the Main Menu (COMEN01C).

### Default Credentials

- Admin access: ADMIN001 / PASSWORD
- User access: USER0001 / PASSWORD

### Authorization Enforcement

Menu programs check the user type stored in the COMMAREA before allowing access to restricted functions. The COMEN02Y copybook defines which user type can access each menu option through the CDEMO-MENU-OPT-USRTYPE field.

---

## Optional Modules

### Credit Card Authorizations (IMS-DB2-MQ)

Located in `app/app-authorization-ims-db2-mq/`, this module demonstrates integration with IMS DB for hierarchical data storage, DB2 for relational logging, and MQ for asynchronous message processing.

**Components:**
- COPAUS0C (CPVS): Pending authorization summary
- COPAUS1C (CPVD): Pending authorization details
- COPAUA0C (CP00): Authorization request processor
- CBPAUP0C (CBPAUP0J): Batch purge of expired authorizations

**Functionality:**
- Simulate credit card authorization requests via MQ
- Retrieve customer data from IMS databases
- Log transactions in DB2 tables
- View pending authorizations in CICS

### Transaction Type Management (DB2)

Located in `app/app-transaction-type-db2/`, this module demonstrates DB2 integration for maintaining transaction type reference data.

**Components:**
- COTRTLIC (CTLI): Transaction type list with cursor-based retrieval
- COTRTUPC (CTTU): Transaction type add/update with SQL DML
- COBTUPDT (MNTTRDB2): Batch maintenance of transaction types

**Functionality:**
- Maintain transaction type codes in DB2 tables
- Demonstrate cursor operations and SQL patterns
- Synchronize DB2 data with VSAM files via TRANEXTR job

### Account Extractions (MQ-VSAM)

Located in `app/app-vsam-mq/`, this module demonstrates MQ integration for asynchronous data retrieval.

**Components:**
- CODATE01 (CDRD): System date inquiry via MQ
- COACCT01 (CDRA): Account details inquiry via MQ

**Functionality:**
- Request/response pattern using MQ channels
- Extract account data for external systems
- Demonstrate asynchronous processing patterns

---

## Technical Reference

### COBOL Coding Patterns

**File I/O Pattern:**

All programs follow a consistent pattern for file operations:

```cobol
XXXX-OPEN-FILE.
    OPEN INPUT/OUTPUT/I-O file-name
    IF file-status NOT = '00'
        DISPLAY 'ERROR OPENING file-name'
        PERFORM 9999-ABEND-PROGRAM
    END-IF.

XXXX-READ-FILE.
    READ file-name INTO work-area
    EVALUATE file-status
        WHEN '00' CONTINUE
        WHEN '10' SET END-OF-FILE TO TRUE
        WHEN OTHER PERFORM 9999-ABEND-PROGRAM
    END-EVALUATE.

XXXX-CLOSE-FILE.
    CLOSE file-name.
```

**CICS Transaction Pattern:**

Online programs use pseudo-conversational design:

```cobol
PROCEDURE DIVISION.
MAIN-PARA.
    IF EIBCALEN = 0
        PERFORM SEND-INITIAL-SCREEN
    ELSE
        MOVE DFHCOMMAREA TO CARDDEMO-COMMAREA
        EVALUATE EIBAID
            WHEN DFHENTER PERFORM PROCESS-ENTER
            WHEN DFHPF3   PERFORM RETURN-TO-MENU
            WHEN OTHER    PERFORM HANDLE-INVALID-KEY
        END-EVALUATE
    END-IF
    
    EXEC CICS RETURN
        TRANSID(WS-TRANID)
        COMMAREA(CARDDEMO-COMMAREA)
    END-EXEC.
```

**Error Handling Pattern:**

```cobol
9999-ABEND-PROGRAM.
    DISPLAY 'ABENDING PROGRAM'
    MOVE 999 TO ABCODE
    CALL 'CEE3ABD' USING ABCODE, TIMING.
```

### File Status Codes

| Code | Meaning |
|------|---------|
| 00 | Successful completion |
| 10 | End of file |
| 22 | Duplicate key |
| 23 | Record not found |
| 35 | File not found |
| 39 | File attribute mismatch |
| 41 | File already open |
| 42 | File already closed |
| 46 | No valid next record |
| 47 | Input attempted on output file |
| 48 | Output attempted on input file |

### CICS Response Codes

| Code | Constant | Meaning |
|------|----------|---------|
| 0 | DFHRESP(NORMAL) | Successful |
| 12 | DFHRESP(FILENOTFOUND) | File not defined |
| 13 | DFHRESP(NOTFND) | Record not found |
| 14 | DFHRESP(DUPREC) | Duplicate record |
| 16 | DFHRESP(INVREQ) | Invalid request |
| 18 | DFHRESP(NOSPACE) | No space available |
| 19 | DFHRESP(NOTOPEN) | File not open |
| 20 | DFHRESP(ENDFILE) | End of file |
| 21 | DFHRESP(ILLOGIC) | VSAM logic error |

### JCL DD Statement Reference

| DD Name | File | Description |
|---------|------|-------------|
| DALYTRAN | AWS.M2.CARDDEMO.DALYTRAN.PS | Daily transaction input |
| TRANFILE | AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | Transaction master |
| XREFFILE | AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS | Card cross-reference |
| ACCTFILE | AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | Account master |
| CUSTFILE | AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS | Customer master |
| CARDFILE | AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS | Card master |
| TCATBALF | AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS | Transaction category balance |
| DALYREJS | AWS.M2.CARDDEMO.DALYREJS(+1) | Rejected transactions (GDG) |

---

## Glossary

### Application Terms

**CardDemo** - The mainframe credit card management demonstration application. Entry point is the CC00 transaction.

**CICS Transaction** - A 4-character code identifying an online program (e.g., CC00, CM00, CAVW). Each transaction maps to a specific COBOL program and BMS screen.

**BMS Map** - Basic Mapping Support screen definition that specifies the 3270 terminal layout including field positions, attributes, and colors.

**COMMAREA** - Communication Area passed between CICS programs to maintain session state. Defined in COCOM01Y copybook.

**VSAM KSDS** - Virtual Storage Access Method Key-Sequenced Data Set. The primary indexed file organization used for CardDemo data files.

**AIX** - Alternate Index. A secondary key path for VSAM files enabling access by non-primary key fields.

### Transaction Processing Terms

**POSTTRAN** - The core batch job that posts daily transactions to the transaction master file.

**DALYTRAN** - Daily transaction file containing transactions to be posted during batch processing.

**XREF** - Cross-reference file linking card numbers to customer and account IDs.

**TCATBAL** - Transaction category balance file tracking totals by account and transaction category.

### Data Migration Terms

**CBEXPORT** - Export program that consolidates normalized files into a multi-record export file.

**CBIMPORT** - Import program that demultiplexes export records into separate normalized files.

**EXPORT-REC-TYPE** - Single character discriminator in export records: 'C'=Customer, 'A'=Account, 'X'=Cross-ref, 'T'=Transaction, 'D'=Card.

### Scheduling Terms

**Control-M** - Enterprise job scheduler using condition-based dependencies (INCOND/OUTCOND).

**CA7** - Legacy mainframe job scheduler using completion-based triggers.

**WAITSTEP** - Synchronization job that pauses batch workflows to allow parallel branches to complete.

**GDG** - Generation Data Group. Versioned dataset collection where each run creates a new generation.

### Technical Terms

**COMP** - Binary numeric storage format.

**COMP-3** - Packed decimal numeric storage format.

**LRECL** - Logical Record Length. Fixed size of each record in bytes.

**IDCAMS** - Access Method Services utility for VSAM operations.

**IEBGENER** - Sequential data copy utility.

**EIBCALEN** - CICS Execute Interface Block field containing COMMAREA length.

---

## Document Information

**Version:** 1.0  
**Last Updated:** February 2026  
**Application Version:** CardDemo v2.0  
**License:** Apache 2.0

For questions or contributions, please refer to the [CONTRIBUTING.md](../CONTRIBUTING.md) file in the repository root.
