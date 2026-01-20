# CardDemo Application - Technical Specification Document

## Document Information

| Attribute | Value |
|-----------|-------|
| Document Title | CardDemo Technical Specification |
| Version | 1.0 |
| Date | January 2026 |
| Purpose | Reverse Engineering Documentation for Modernization |

---

## 1. Executive Summary

This document provides a comprehensive technical specification of the CardDemo mainframe application, derived through reverse engineering of the source code. CardDemo is a credit card management system built on IBM mainframe technologies including COBOL, CICS, VSAM, JCL, and BMS. The application demonstrates typical patterns found in production mainframe systems within the financial services industry.

This technical specification serves as a foundation for modernization planning, providing detailed information about the technology stack, application architecture, data models, and system dependencies.

---

## 2. Technology Stack

### 2.1 Programming Languages

| Technology | Version/Standard | Usage |
|------------|------------------|-------|
| COBOL | COBOL 85/Enterprise COBOL | Primary application logic |
| JCL | z/OS JCL | Batch job control |
| BMS | CICS BMS | Screen definitions |

### 2.2 Runtime Environment

| Component | Description |
|-----------|-------------|
| Operating System | z/OS (IBM Mainframe) |
| Transaction Monitor | CICS (Customer Information Control System) |
| Batch Scheduler | Control-M, CA7 |
| Data Storage | VSAM (Virtual Storage Access Method) |

### 2.3 Optional Components

| Component | Description | Usage |
|-----------|-------------|-------|
| DB2 | IBM Relational Database | Transaction type management |
| IMS DB | IBM Hierarchical Database | Pending authorization storage |
| MQ | IBM Message Queue | Asynchronous messaging |

### 2.4 Development Tools

| Tool | Purpose |
|------|---------|
| IDCAMS | VSAM file management utility |
| IEBGENER | Sequential data copy utility |
| DSNTIAUL | DB2 data extraction utility |

---

## 3. Application Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    3270 Terminal Interface                           │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │ COSGN00  │ │ COMEN01  │ │ COACTVW  │ │ COTRN00  │ │ COUSR00  │  │   │
│  │  │ Sign-on  │ │ Main Menu│ │ Acct View│ │ Tran List│ │ User List│  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION LAYER                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    CICS Transaction Server                           │   │
│  │  ┌──────────────────────────────────────────────────────────────┐   │   │
│  │  │                    Online Programs (*C.cbl)                   │   │   │
│  │  │  COSGN00C │ COMEN01C │ COACTVWC │ COACTUPC │ COCRDLIC │ ...  │   │   │
│  │  └──────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    Batch Processing (JCL)                            │   │
│  │  ┌──────────────────────────────────────────────────────────────┐   │   │
│  │  │                    Batch Programs (CB*.cbl)                   │   │   │
│  │  │  CBTRN02C │ CBACT04C │ CBSTM03A │ CBSTM03B │ CBEXPORT │ ...  │   │   │
│  │  └──────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    VSAM KSDS Files                                   │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │ CUSTDATA │ │ ACCTDATA │ │ CARDDATA │ │ TRANSACT │ │ USRSEC   │  │   │
│  │  │ Customer │ │ Account  │ │ Card     │ │ Trans    │ │ Security │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │   │
│  │  │ CARDXREF │ │ TCATBALF │ │ DISCGRP  │ │ DALYTRAN │              │   │
│  │  │ Xref     │ │ Cat Bal  │ │ Disc Grp │ │ Daily Trn│              │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Component Architecture

#### 3.2.1 Online Processing Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    CICS Region                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                 Transaction Routing                        │  │
│  │  CC00 → COSGN00C    CM00 → COMEN01C    CA00 → COADM01C   │  │
│  │  CAVW → COACTVWC    CAUP → COACTUPC    CCLI → COCRDLIC   │  │
│  │  CCDL → COCRDSLC    CCUP → COCRDUPC    CT00 → COTRN00C   │  │
│  │  CT01 → COTRN01C    CT02 → COTRN02C    CB00 → COBIL00C   │  │
│  │  CR00 → CORPT00C    CU00 → COUSR00C    CU01 → COUSR01C   │  │
│  │  CU02 → COUSR02C    CU03 → COUSR03C                       │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                 File Control Table (FCT)                   │  │
│  │  ACCTDAT → ACCTDATA.VSAM.KSDS                             │  │
│  │  CARDDAT → CARDDATA.VSAM.KSDS                             │  │
│  │  CUSTDAT → CUSTDATA.VSAM.KSDS                             │  │
│  │  TRANSACT → TRANSACT.VSAM.KSDS                            │  │
│  │  USRSEC → USRSEC.VSAM.KSDS                                │  │
│  │  CARDXREF → CARDXREF.VSAM.KSDS                            │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                 Program Control Table (PCT)                │  │
│  │  Transaction → Program → Mapset                            │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

#### 3.2.2 Batch Processing Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    Batch Subsystem                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                 Job Scheduler (Control-M/CA7)              │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  Daily Jobs                                          │  │  │
│  │  │  CLOSEFIL → TRANBKP → POSTTRAN → WAITSTEP → OPENFIL │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  Weekly Jobs (Saturday)                              │  │  │
│  │  │  MNTTRDB2 → CLOSEFIL → DISCGRP → WAITSTEP → OPENFIL │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  Monthly Jobs (1st of Month)                         │  │  │
│  │  │  CLOSEFIL → INTCALC → CREASTMT → WAITSTEP → OPENFIL │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                 JCL Job Definitions                        │  │
│  │  POSTTRAN.jcl → CBTRN02C (Transaction Posting)            │  │
│  │  INTCALC.jcl → CBACT04C (Interest Calculation)            │  │
│  │  CBEXPORT.jcl → CBEXPORT (Data Export)                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 Program Flow Architecture

#### 3.3.1 Online Program Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Terminal  │────▶│    CICS     │────▶│   Program   │
│   (3270)    │     │   Region    │     │   (COBOL)   │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          │                          │
                    ▼                          ▼                          ▼
            ┌─────────────┐            ┌─────────────┐            ┌─────────────┐
            │  SEND MAP   │            │ RECEIVE MAP │            │  FILE I/O   │
            │  (BMS)      │            │  (BMS)      │            │  (VSAM)     │
            └─────────────┘            └─────────────┘            └─────────────┘
```

#### 3.3.2 Batch Program Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Scheduler  │────▶│    JCL      │────▶│   Program   │────▶│   Files     │
│  (Control-M)│     │   Job       │     │   (COBOL)   │     │   (VSAM)    │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

---

## 4. Technical Architecture

### 4.1 System Context Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              z/OS Environment                                │
│                                                                              │
│  ┌─────────────────────┐        ┌─────────────────────┐                     │
│  │    CICS Region      │        │   Batch Region      │                     │
│  │                     │        │                     │                     │
│  │  ┌───────────────┐  │        │  ┌───────────────┐  │                     │
│  │  │ Online        │  │        │  │ Batch         │  │                     │
│  │  │ Programs      │  │        │  │ Programs      │  │                     │
│  │  │ (17 programs) │  │        │  │ (5 programs)  │  │                     │
│  │  └───────┬───────┘  │        │  └───────┬───────┘  │                     │
│  │          │          │        │          │          │                     │
│  │  ┌───────▼───────┐  │        │  ┌───────▼───────┐  │                     │
│  │  │ BMS Maps      │  │        │  │ JCL Jobs      │  │                     │
│  │  │ (17 mapsets)  │  │        │  │ (8 jobs)      │  │                     │
│  │  └───────────────┘  │        │  └───────────────┘  │                     │
│  └──────────┬──────────┘        └──────────┬──────────┘                     │
│             │                              │                                 │
│             └──────────────┬───────────────┘                                │
│                            │                                                 │
│                   ┌────────▼────────┐                                       │
│                   │   VSAM Files    │                                       │
│                   │   (10 files)    │                                       │
│                   └─────────────────┘                                       │
│                                                                              │
│  ┌─────────────────────┐        ┌─────────────────────┐                     │
│  │   Control-M         │        │   CA7               │                     │
│  │   Scheduler         │        │   Scheduler         │                     │
│  └─────────────────────┘        └─────────────────────┘                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Mainframe LPAR                                     │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        z/OS Operating System                         │   │
│  │                                                                       │   │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌─────────────────┐  │   │
│  │  │   CICS Region     │  │   Batch Region    │  │   DB2 Region    │  │   │
│  │  │   (CICSPROD)      │  │   (BATCHPRD)      │  │   (Optional)    │  │   │
│  │  └───────────────────┘  └───────────────────┘  └─────────────────┘  │   │
│  │                                                                       │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │                    Dataset Catalog                               │ │   │
│  │  │  AWS.M2.CARDDEMO.LOADLIB      - Load Library                    │ │   │
│  │  │  AWS.M2.CARDDEMO.COPYLIB      - Copybook Library                │ │   │
│  │  │  AWS.M2.CARDDEMO.PROCLIB      - Procedure Library               │ │   │
│  │  │  AWS.M2.CARDDEMO.*.VSAM.KSDS  - VSAM Data Files                 │ │   │
│  │  │  AWS.M2.CARDDEMO.*.PS         - Sequential Files                │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Network Architecture

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   3270 Terminal │◀───────▶│   TN3270 Server │◀───────▶│   CICS Region   │
│   (Client)      │   TCP/IP│   (Gateway)     │   SNA   │   (Mainframe)   │
└─────────────────┘         └─────────────────┘         └─────────────────┘
```

---

## 5. Data Model

### 5.1 Physical Data Model

#### 5.1.1 VSAM File Specifications

| File Name | DD Name | Type | Record Length | Key Position | Key Length | Description |
|-----------|---------|------|---------------|--------------|------------|-------------|
| CUSTDATA | CUSTFILE | KSDS | 500 | 1 | 9 | Customer Master |
| ACCTDATA | ACCTFILE | KSDS | 300 | 1 | 11 | Account Master |
| CARDDATA | CARDFILE | KSDS | 150 | 1 | 16 | Card Master |
| CARDXREF | XREFFILE | KSDS | 50 | 1 | 16 | Card Cross-Reference |
| TRANSACT | TRANFILE | KSDS | 350 | 1 | 16 | Transaction Master |
| USRSEC | USRSEC | KSDS | 80 | 1 | 8 | User Security |
| TCATBALF | TCATBALF | KSDS | 50 | 1 | 17 | Transaction Category Balance |
| DISCGRP | DISCGRP | KSDS | 50 | 1 | 16 | Disclosure Group |
| DALYTRAN | DALYTRAN | PS | 350 | N/A | N/A | Daily Transactions (Sequential) |
| DALYREJS | DALYREJS | PS | 430 | N/A | N/A | Rejected Transactions (Sequential) |

#### 5.1.2 Alternate Index Specifications

| Base File | AIX Name | AIX Path | Key Position | Key Length | Description |
|-----------|----------|----------|--------------|------------|-------------|
| CARDDATA | CARDAIX | CARDDATA.VSAM.AIX.PATH | 17 | 11 | Card by Account ID |
| CARDXREF | CXACAIX | CARDXREF.VSAM.AIX.PATH | 26 | 11 | Xref by Account ID |
| TRANSACT | TRANAIX | TRANSACT.VSAM.AIX.PATH | 263 | 16 | Transaction by Card Number |

### 5.2 Record Layouts

#### 5.2.1 Customer Record (CVCUS01Y.cpy) - 500 bytes

| Field Name | PIC Clause | Offset | Length | Description |
|------------|------------|--------|--------|-------------|
| CUST-ID | 9(09) | 1 | 9 | Customer ID (Primary Key) |
| CUST-FIRST-NAME | X(25) | 10 | 25 | First Name |
| CUST-MIDDLE-NAME | X(25) | 35 | 25 | Middle Name |
| CUST-LAST-NAME | X(25) | 60 | 25 | Last Name |
| CUST-ADDR-LINE-1 | X(50) | 85 | 50 | Address Line 1 |
| CUST-ADDR-LINE-2 | X(50) | 135 | 50 | Address Line 2 |
| CUST-ADDR-LINE-3 | X(50) | 185 | 50 | Address Line 3 |
| CUST-ADDR-STATE-CD | X(02) | 235 | 2 | State Code |
| CUST-ADDR-COUNTRY-CD | X(03) | 237 | 3 | Country Code |
| CUST-ADDR-ZIP | X(10) | 240 | 10 | ZIP Code |
| CUST-PHONE-NUM-1 | X(15) | 250 | 15 | Phone Number 1 |
| CUST-PHONE-NUM-2 | X(15) | 265 | 15 | Phone Number 2 |
| CUST-SSN | 9(09) | 280 | 9 | Social Security Number |
| CUST-GOVT-ISSUED-ID | X(20) | 289 | 20 | Government ID |
| CUST-DOB-YYYY-MM-DD | X(10) | 309 | 10 | Date of Birth |
| CUST-EFT-ACCOUNT-ID | X(10) | 319 | 10 | EFT Account ID |
| CUST-PRI-CARD-HOLDER-IND | X(01) | 329 | 1 | Primary Cardholder Indicator |
| CUST-FICO-CREDIT-SCORE | 9(03) | 330 | 3 | FICO Score |
| FILLER | X(168) | 333 | 168 | Reserved |

#### 5.2.2 Account Record (CVACT01Y.cpy) - 300 bytes

| Field Name | PIC Clause | Offset | Length | Description |
|------------|------------|--------|--------|-------------|
| ACCT-ID | 9(11) | 1 | 11 | Account ID (Primary Key) |
| ACCT-ACTIVE-STATUS | X(01) | 12 | 1 | Active Status |
| ACCT-CURR-BAL | S9(10)V99 | 13 | 12 | Current Balance |
| ACCT-CREDIT-LIMIT | S9(10)V99 | 25 | 12 | Credit Limit |
| ACCT-CASH-CREDIT-LIMIT | S9(10)V99 | 37 | 12 | Cash Credit Limit |
| ACCT-OPEN-DATE | X(10) | 49 | 10 | Open Date |
| ACCT-EXPIRAION-DATE | X(10) | 59 | 10 | Expiration Date |
| ACCT-REISSUE-DATE | X(10) | 69 | 10 | Reissue Date |
| ACCT-CURR-CYC-CREDIT | S9(10)V99 | 79 | 12 | Current Cycle Credit |
| ACCT-CURR-CYC-DEBIT | S9(10)V99 | 91 | 12 | Current Cycle Debit |
| ACCT-ADDR-ZIP | X(10) | 103 | 10 | Address ZIP |
| ACCT-GROUP-ID | X(10) | 113 | 10 | Group ID |
| FILLER | X(178) | 123 | 178 | Reserved |

#### 5.2.3 Card Record (CVACT02Y.cpy) - 150 bytes

| Field Name | PIC Clause | Offset | Length | Description |
|------------|------------|--------|--------|-------------|
| CARD-NUM | X(16) | 1 | 16 | Card Number (Primary Key) |
| CARD-ACCT-ID | 9(11) | 17 | 11 | Account ID (Foreign Key) |
| CARD-CVV-CD | 9(03) | 28 | 3 | CVV Code |
| CARD-EMBOSSED-NAME | X(50) | 31 | 50 | Embossed Name |
| CARD-EXPIRAION-DATE | X(10) | 81 | 10 | Expiration Date |
| CARD-ACTIVE-STATUS | X(01) | 91 | 1 | Active Status (Y/N) |
| FILLER | X(59) | 92 | 59 | Reserved |

#### 5.2.4 Card Cross-Reference Record (CVACT03Y.cpy) - 50 bytes

| Field Name | PIC Clause | Offset | Length | Description |
|------------|------------|--------|--------|-------------|
| XREF-CARD-NUM | X(16) | 1 | 16 | Card Number (Primary Key) |
| XREF-CUST-ID | 9(09) | 17 | 9 | Customer ID (Foreign Key) |
| XREF-ACCT-ID | 9(11) | 26 | 11 | Account ID (Foreign Key) |
| FILLER | X(14) | 37 | 14 | Reserved |

#### 5.2.5 Transaction Record (CVTRA05Y.cpy) - 350 bytes

| Field Name | PIC Clause | Offset | Length | Description |
|------------|------------|--------|--------|-------------|
| TRAN-ID | X(16) | 1 | 16 | Transaction ID (Primary Key) |
| TRAN-TYPE-CD | X(02) | 17 | 2 | Transaction Type Code |
| TRAN-CAT-CD | 9(04) | 19 | 4 | Category Code |
| TRAN-SOURCE | X(10) | 23 | 10 | Transaction Source |
| TRAN-DESC | X(100) | 33 | 100 | Description |
| TRAN-AMT | S9(09)V99 | 133 | 11 | Amount |
| TRAN-MERCHANT-ID | 9(09) | 144 | 9 | Merchant ID |
| TRAN-MERCHANT-NAME | X(50) | 153 | 50 | Merchant Name |
| TRAN-MERCHANT-CITY | X(50) | 203 | 50 | Merchant City |
| TRAN-MERCHANT-ZIP | X(10) | 253 | 10 | Merchant ZIP |
| TRAN-CARD-NUM | X(16) | 263 | 16 | Card Number (Foreign Key) |
| TRAN-ORIG-TS | X(26) | 279 | 26 | Original Timestamp |
| TRAN-PROC-TS | X(26) | 305 | 26 | Processed Timestamp |
| FILLER | X(20) | 331 | 20 | Reserved |

#### 5.2.6 User Security Record (CSUSR01Y.cpy) - 80 bytes

| Field Name | PIC Clause | Offset | Length | Description |
|------------|------------|--------|--------|-------------|
| SEC-USR-ID | X(08) | 1 | 8 | User ID (Primary Key) |
| SEC-USR-FNAME | X(20) | 9 | 20 | First Name |
| SEC-USR-LNAME | X(20) | 29 | 20 | Last Name |
| SEC-USR-PWD | X(08) | 49 | 8 | Password |
| SEC-USR-TYPE | X(01) | 57 | 1 | User Type (A/U) |
| SEC-USR-FILLER | X(23) | 58 | 23 | Reserved |

#### 5.2.7 Transaction Category Balance Record (CVTRA01Y.cpy) - 50 bytes

| Field Name | PIC Clause | Offset | Length | Description |
|------------|------------|--------|--------|-------------|
| TRANCAT-ACCT-ID | 9(11) | 1 | 11 | Account ID (PK1) |
| TRANCAT-TYPE-CD | X(02) | 12 | 2 | Type Code (PK2) |
| TRANCAT-CD | 9(04) | 14 | 4 | Category Code (PK3) |
| TRAN-CAT-BAL | S9(09)V99 | 18 | 11 | Category Balance |
| FILLER | X(22) | 29 | 22 | Reserved |

#### 5.2.8 Disclosure Group Record (CVTRA02Y.cpy) - 50 bytes

| Field Name | PIC Clause | Offset | Length | Description |
|------------|------------|--------|--------|-------------|
| DIS-ACCT-GROUP-ID | X(10) | 1 | 10 | Account Group ID (PK1) |
| DIS-TRAN-TYPE-CD | X(02) | 11 | 2 | Transaction Type Code (PK2) |
| DIS-TRAN-CAT-CD | 9(04) | 13 | 4 | Category Code (PK3) |
| DIS-INT-RATE | S9(04)V99 | 17 | 6 | Interest Rate |
| FILLER | X(28) | 23 | 28 | Reserved |

### 5.3 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Online Data Flow                                   │
│                                                                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ Terminal │───▶│ COSGN00C │───▶│ USRSEC   │───▶│ Validate │              │
│  │ Input    │    │ Sign-on  │    │ File     │    │ User     │              │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘              │
│                                                        │                     │
│                                                        ▼                     │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ COACTVWC │◀──▶│ ACCTDATA │    │ CUSTDATA │◀──▶│ COACTUPC │              │
│  │ View     │    │ File     │    │ File     │    │ Update   │              │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘              │
│                                                                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ COTRN02C │───▶│ TRANSACT │    │ CARDXREF │◀──▶│ COBIL00C │              │
│  │ Add Tran │    │ File     │    │ File     │    │ Payment  │              │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           Batch Data Flow                                    │
│                                                                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ DALYTRAN │───▶│ CBTRN02C │───▶│ TRANSACT │    │ DALYREJS │              │
│  │ Input    │    │ Posting  │    │ Master   │    │ Rejects  │              │
│  └──────────┘    └────┬─────┘    └──────────┘    └──────────┘              │
│                       │                                ▲                     │
│                       │          ┌──────────┐          │                     │
│                       └─────────▶│ TCATBALF │──────────┘                     │
│                                  │ Cat Bal  │                                │
│                                  └──────────┘                                │
│                                                                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ TCATBALF │───▶│ CBACT04C │───▶│ ACCTDATA │    │ DISCGRP  │              │
│  │ Input    │    │ Interest │    │ Update   │    │ Rates    │              │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘              │
│                                                                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ All VSAM │───▶│ CBEXPORT │───▶│ EXPORT   │    │ CBIMPORT │              │
│  │ Files    │    │ Program  │    │ File     │    │ Program  │              │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Dependency Mapping

### 6.1 Program Dependencies

#### 6.1.1 Online Program Dependencies

| Program | Copybooks | BMS Maps | Files Accessed | Called Programs |
|---------|-----------|----------|----------------|-----------------|
| COSGN00C | COCOM01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y, DFHAID, DFHBMSCA | COSGN00 | USRSEC | COMEN01C, COADM01C |
| COMEN01C | COCOM01Y, COMEN02Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COMEN01 | - | Various via XCTL |
| COADM01C | COCOM01Y, COADM02Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COADM01 | - | COUSR00C-03C |
| COACTVWC | COCOM01Y, CVACT01Y, CVCUS01Y, CVACT03Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COACTVW | ACCTDAT, CUSTDAT, CARDXREF | - |
| COACTUPC | COCOM01Y, CVACT01Y, CVCUS01Y, CVACT03Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COACTUP | ACCTDAT, CUSTDAT, CARDXREF | - |
| COCRDLIC | COCOM01Y, CVACT02Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COCRDLI | CARDDAT | COCRDSLC, COCRDUPC |
| COCRDSLC | COCOM01Y, CVACT02Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COCRDSL | CARDDAT | - |
| COCRDUPC | COCOM01Y, CVACT02Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COCRDUP | CARDDAT | - |
| COTRN00C | COCOM01Y, CVTRA05Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COTRN00 | TRANSACT | COTRN01C |
| COTRN01C | COCOM01Y, CVTRA05Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COTRN01 | TRANSACT | - |
| COTRN02C | COCOM01Y, CVTRA05Y, CVACT03Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COTRN02 | TRANSACT, CARDXREF | - |
| COBIL00C | COCOM01Y, CVACT01Y, CVTRA05Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COBIL00 | ACCTDAT, TRANSACT | - |
| CORPT00C | COCOM01Y, CVTRA05Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | CORPT00 | TRANSACT | - |
| COUSR00C | COCOM01Y, CSUSR01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COUSR00 | USRSEC | COUSR02C, COUSR03C |
| COUSR01C | COCOM01Y, CSUSR01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COUSR01 | USRSEC | - |
| COUSR02C | COCOM01Y, CSUSR01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COUSR02 | USRSEC | - |
| COUSR03C | COCOM01Y, CSUSR01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, DFHAID, DFHBMSCA | COUSR03 | USRSEC | - |

#### 6.1.2 Batch Program Dependencies

| Program | Copybooks | Files Accessed (Input) | Files Accessed (Output) |
|---------|-----------|------------------------|-------------------------|
| CBTRN02C | CVTRA05Y, CVACT01Y, CVACT03Y, CVTRA01Y | DALYTRAN, XREFFILE, ACCTFILE | TRANFILE, DALYREJS, TCATBALF |
| CBACT04C | CVACT01Y, CVTRA01Y, CVTRA02Y | TCATBALF, XREFFILE, DISCGRP, ACCTFILE | ACCTFILE, TRANSACT |
| CBSTM03A | CVTRA05Y, CVCUS01Y, CVACT01Y, COSTM01 | TRANSACT, CUSTDATA, ACCTDATA | STMTFILE, HTMLFILE |
| CBSTM03B | CVTRA05Y, CVCUS01Y, CVACT01Y | TRANSACT, CUSTDATA, ACCTDATA | - |
| CBEXPORT | CVCUS01Y, CVACT01Y, CVACT02Y, CVACT03Y, CVTRA05Y, CVEXPORT | CUSTFILE, ACCTFILE, XREFFILE, TRANSACT, CARDFILE | EXPFILE |
| CBIMPORT | CVEXPORT | EXPFILE | CUSTFILE, ACCTFILE, XREFFILE, TRANSACT, CARDFILE |

### 6.2 Copybook Dependencies

| Copybook | Description | Used By Programs |
|----------|-------------|------------------|
| COCOM01Y | Communication Area | All online programs |
| COMEN02Y | Main Menu Options | COMEN01C |
| COADM02Y | Admin Menu Options | COADM01C |
| COTTL01Y | Title/Header Data | All online programs |
| CSDAT01Y | Date Formatting | All online programs |
| CSMSG01Y | Message Constants | All online programs |
| CSUSR01Y | User Security Record | COSGN00C, COUSR00C-03C |
| CVACT01Y | Account Record | COACTVWC, COACTUPC, COBIL00C, CBTRN02C, CBACT04C |
| CVACT02Y | Card Record | COCRDLIC, COCRDSLC, COCRDUPC, CBEXPORT |
| CVACT03Y | Card Cross-Reference | COACTVWC, COACTUPC, COTRN02C, CBTRN02C, CBEXPORT |
| CVCUS01Y | Customer Record | COACTVWC, COACTUPC, CBSTM03A, CBEXPORT |
| CVTRA05Y | Transaction Record | COTRN00C, COTRN01C, COTRN02C, COBIL00C, CORPT00C, CBTRN02C, CBEXPORT |
| CVTRA01Y | Transaction Category Balance | CBTRN02C, CBACT04C |
| CVTRA02Y | Disclosure Group | CBACT04C |
| CVEXPORT | Export Record | CBEXPORT, CBIMPORT |
| DFHAID | CICS AID Keys | All online programs |
| DFHBMSCA | BMS Attributes | All online programs |

### 6.3 File Dependencies

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           File Dependency Graph                              │
│                                                                              │
│                          ┌──────────────┐                                   │
│                          │   CUSTDATA   │                                   │
│                          │  (Customer)  │                                   │
│                          └──────┬───────┘                                   │
│                                 │                                            │
│                                 │ 1:N                                        │
│                                 ▼                                            │
│  ┌──────────────┐       ┌──────────────┐       ┌──────────────┐            │
│  │   ACCTDATA   │◀──────│   CARDXREF   │──────▶│   CARDDATA   │            │
│  │   (Account)  │  N:1  │  (Xref)      │  1:1  │   (Card)     │            │
│  └──────┬───────┘       └──────────────┘       └──────────────┘            │
│         │                      │                      │                      │
│         │ 1:N                  │ 1:N                  │ 1:N                  │
│         ▼                      ▼                      ▼                      │
│  ┌──────────────┐       ┌──────────────┐       ┌──────────────┐            │
│  │   TCATBALF   │       │   TRANSACT   │◀──────│   DALYTRAN   │            │
│  │  (Cat Bal)   │       │ (Transaction)│       │  (Daily Trn) │            │
│  └──────────────┘       └──────────────┘       └──────────────┘            │
│         │                                                                    │
│         │ N:1                                                                │
│         ▼                                                                    │
│  ┌──────────────┐                                                           │
│  │   DISCGRP    │                                                           │
│  │ (Disc Group) │                                                           │
│  └──────────────┘                                                           │
│                                                                              │
│  ┌──────────────┐                                                           │
│  │    USRSEC    │  (Independent - User Security)                            │
│  └──────────────┘                                                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Batch Job Specifications

### 7.1 Job Definitions

#### 7.1.1 POSTTRAN - Daily Transaction Posting

```
Job Name: POSTTRAN
Program: CBTRN02C
Schedule: Daily (after CLOSEFIL)
Purpose: Post daily transactions to master files

Input Files:
  - DALYTRAN (AWS.M2.CARDDEMO.DALYTRAN.PS) - Daily transactions
  - XREFFILE (AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS) - Card cross-reference
  - ACCTFILE (AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS) - Account master

Output Files:
  - TRANFILE (AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS) - Transaction master
  - DALYREJS (AWS.M2.CARDDEMO.DALYREJS(+1)) - Rejected transactions
  - TCATBALF (AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS) - Category balance

Processing Logic:
  1. Open all input and output files
  2. Read daily transaction record
  3. Validate card exists in XREF file
  4. Validate account exists in ACCT file
  5. If valid: Write to TRANSACT, update TCATBALF
  6. If invalid: Write to DALYREJS with reason code
  7. Repeat until EOF
  8. Close files and report statistics
```

#### 7.1.2 INTCALC - Interest Calculation

```
Job Name: INTCALC
Program: CBACT04C
Schedule: Monthly (1st of month)
Purpose: Calculate and apply interest charges

Input Files:
  - TCATBALF (AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS) - Category balances
  - XREFFILE (AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS) - Card cross-reference
  - XREFFIL1 (AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH) - Xref by account
  - DISCGRP (AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS) - Interest rates

Output Files:
  - ACCTFILE (AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS) - Account master
  - TRANSACT (AWS.M2.CARDDEMO.SYSTRAN(+1)) - Interest transactions

Processing Logic:
  1. Read category balance record
  2. Lookup account group ID via XREF alternate index
  3. Lookup interest rate from DISCGRP
  4. Calculate monthly interest: Balance * (Rate / 12)
  5. Update account balance
  6. Reset cycle credit/debit counters
  7. Write interest transaction record
  8. Repeat for all category balances
```

#### 7.1.3 CBEXPORT - Data Export

```
Job Name: CBEXPORT
Program: CBEXPORT
Schedule: On-demand
Purpose: Export data for branch migration

Input Files:
  - CUSTFILE (AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS)
  - ACCTFILE (AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS)
  - XREFFILE (AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS)
  - TRANSACT (AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS)
  - CARDFILE (AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS)

Output Files:
  - EXPFILE (AWS.M2.CARDDEMO.EXPORT.DATA) - Multi-record export file

Processing Logic:
  1. Generate timestamp for export batch
  2. Initialize sequence counter
  3. Read and export all customer records (Type 'C')
  4. Read and export all account records (Type 'A')
  5. Read and export all xref records (Type 'X')
  6. Read and export all transaction records (Type 'T')
  7. Read and export all card records (Type 'K')
  8. Report export statistics
```

### 7.2 Job Scheduling

#### 7.2.1 Control-M Schedule

| Folder | Job | Dependency | Schedule |
|--------|-----|------------|----------|
| DAILY-TransactionBackup | CLOSEFIL | None | Daily |
| DAILY-TransactionBackup | TRANBKP | CLOSEFIL | Daily |
| DAILY-TransactionBackup | WAITSTEP | TRANBKP | Daily |
| DAILY-TransactionBackup | OPENFIL | WAITSTEP | Daily |
| WEEKLY-TransactionTypesDBRefresh | MNTTRDB2 | None | Saturday |
| WEEKLY-DisclosureGroupsRefresh | CLOSEFIL | MNTTRDB2 | Saturday |
| WEEKLY-DisclosureGroupsRefresh | DISCGRP | CLOSEFIL | Saturday |
| WEEKLY-DisclosureGroupsRefresh | WAITSTEP | DISCGRP | Saturday |
| WEEKLY-DisclosureGroupsRefresh | OPENFIL | WAITSTEP | Saturday |
| MONTHLY-InterestCalculation | CLOSEFIL | None | 1st of Month |
| MONTHLY-InterestCalculation | INTCALC | CLOSEFIL | 1st of Month |
| MONTHLY-InterestCalculation | CREASTMT | INTCALC | 1st of Month |
| MONTHLY-InterestCalculation | WAITSTEP | CREASTMT | 1st of Month |
| MONTHLY-InterestCalculation | OPENFIL | WAITSTEP | 1st of Month |

#### 7.2.2 Job Dependency Flow

```
Daily Processing:
CLOSEFIL ──▶ TRANBKP ──▶ POSTTRAN ──▶ WAITSTEP ──▶ OPENFIL

Weekly Processing (Saturday):
MNTTRDB2 ──▶ CLOSEFIL ──▶ DISCGRP ──▶ WAITSTEP ──▶ OPENFIL

Monthly Processing (1st):
CLOSEFIL ──▶ INTCALC ──▶ CREASTMT ──▶ WAITSTEP ──▶ OPENFIL
```

---

## 8. Screen Specifications

### 8.1 BMS Map Inventory

| Mapset | Map | Transaction | Description | Size |
|--------|-----|-------------|-------------|------|
| COSGN00 | COSGN0A | CC00 | Sign-on Screen | 24x80 |
| COMEN01 | COMEN1A | CM00 | Main Menu | 24x80 |
| COADM01 | COADM1A | CA00 | Admin Menu | 24x80 |
| COACTVW | COACTVWA | CAVW | Account View | 24x80 |
| COACTUP | COACTUPA | CAUP | Account Update | 24x80 |
| COCRDLI | COCRDLIA | CCLI | Card List | 24x80 |
| COCRDSL | COCRDSLA | CCDL | Card Detail | 24x80 |
| COCRDUP | COCRDUPA | CCUP | Card Update | 24x80 |
| COTRN00 | COTRN0A | CT00 | Transaction List | 24x80 |
| COTRN01 | COTRN1A | CT01 | Transaction View | 24x80 |
| COTRN02 | COTRN2A | CT02 | Transaction Add | 24x80 |
| COBIL00 | COBIL0A | CB00 | Bill Payment | 24x80 |
| CORPT00 | CORPT0A | CR00 | Reports | 24x80 |
| COUSR00 | COUSR0A | CU00 | User List | 24x80 |
| COUSR01 | COUSR1A | CU01 | User Add | 24x80 |
| COUSR02 | COUSR2A | CU02 | User Update | 24x80 |
| COUSR03 | COUSR3A | CU03 | User Delete | 24x80 |

### 8.2 Screen Layout Standards

All screens follow a consistent layout:

```
Line 1:  Tran: XXXX  [Title 1]                              Date: MM/DD/YY
Line 2:  Prog: XXXXXXXX  [Title 2]                          Time: HH:MM:SS
Line 3:  [Additional Header Info]
Lines 4-22: [Screen-specific content]
Line 23: [Error/Status Message - Red/Green]
Line 24: [Function Key Legend]
```

### 8.3 Field Attributes

| Attribute | Usage |
|-----------|-------|
| ASKIP | Auto-skip (protected) |
| UNPROT | Unprotected (input) |
| NUM | Numeric only |
| BRT | Bright intensity |
| NORM | Normal intensity |
| DRK | Dark (hidden - passwords) |
| FSET | Field set |
| IC | Initial cursor position |

---

## 9. CICS Resource Definitions

### 9.1 Transaction Definitions

| Transaction | Program | Description | Security |
|-------------|---------|-------------|----------|
| CC00 | COSGN00C | Sign-on | Public |
| CM00 | COMEN01C | Main Menu | User |
| CA00 | COADM01C | Admin Menu | Admin |
| CAVW | COACTVWC | Account View | User |
| CAUP | COACTUPC | Account Update | User |
| CCLI | COCRDLIC | Card List | User |
| CCDL | COCRDSLC | Card Detail | User |
| CCUP | COCRDUPC | Card Update | User |
| CT00 | COTRN00C | Transaction List | User |
| CT01 | COTRN01C | Transaction View | User |
| CT02 | COTRN02C | Transaction Add | User |
| CB00 | COBIL00C | Bill Payment | User |
| CR00 | CORPT00C | Reports | User |
| CU00 | COUSR00C | User List | Admin |
| CU01 | COUSR01C | User Add | Admin |
| CU02 | COUSR02C | User Update | Admin |
| CU03 | COUSR03C | User Delete | Admin |

### 9.2 File Control Table (FCT) Entries

| DD Name | Dataset Name | Type | Access |
|---------|--------------|------|--------|
| ACCTDAT | AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | KSDS | Read/Update |
| CARDDAT | AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS | KSDS | Read/Update |
| CUSTDAT | AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS | KSDS | Read/Update |
| TRANSACT | AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | KSDS | Read/Write |
| USRSEC | AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS | KSDS | Read/Update/Delete |
| CARDXREF | AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS | KSDS | Read |
| CARDAIX | AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX.PATH | AIX | Read |
| CXACAIX | AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH | AIX | Read |

### 9.3 CICS Commands Used

| Command | Purpose | Programs |
|---------|---------|----------|
| SEND MAP | Display screen | All online |
| RECEIVE MAP | Get user input | All online |
| READ | Read file record | All online |
| WRITE | Write new record | COTRN02C, COUSR01C, COBIL00C |
| REWRITE | Update record | COACTUPC, COCRDUPC, COUSR02C |
| DELETE | Delete record | COUSR03C |
| STARTBR | Start browse | COCRDLIC, COTRN00C, COUSR00C |
| READNEXT | Read next in browse | COCRDLIC, COTRN00C, COUSR00C |
| READPREV | Read previous in browse | COCRDLIC, COTRN00C, COUSR00C |
| ENDBR | End browse | COCRDLIC, COTRN00C, COUSR00C |
| XCTL | Transfer control | All online |
| RETURN | Return to CICS | All online |
| WRITEQ TD | Write to TDQ | CORPT00C |

---

## 10. Error Handling and Logging

### 10.1 CICS Response Code Handling

```cobol
EVALUATE WS-RESP-CD
    WHEN DFHRESP(NORMAL)
        CONTINUE
    WHEN DFHRESP(NOTFND)
        MOVE 'Record not found' TO WS-MESSAGE
    WHEN DFHRESP(DUPREC)
        MOVE 'Duplicate record' TO WS-MESSAGE
    WHEN DFHRESP(INVREQ)
        MOVE 'Invalid request' TO WS-MESSAGE
    WHEN OTHER
        DISPLAY 'RESP:' WS-RESP-CD 'REAS:' WS-REAS-CD
        MOVE 'System error' TO WS-MESSAGE
END-EVALUATE
```

### 10.2 Batch Return Codes

| Return Code | Meaning |
|-------------|---------|
| 0 | Successful completion |
| 4 | Warnings (some records rejected) |
| 8 | Errors (processing continued) |
| 12 | Severe errors (processing stopped) |
| 16 | Fatal errors (abend) |

### 10.3 Abend Handling

```cobol
9999-ABEND-PROGRAM.
    DISPLAY 'PROGRAM ABEND - ' WS-PGMNAME
    DISPLAY 'FILE STATUS: ' WS-FILE-STATUS
    CALL 'CEE3ABD' USING WS-ABEND-CODE WS-TIMING
    STOP RUN.
```

---

## 11. Performance Considerations

### 11.1 File Access Patterns

| Pattern | Description | Optimization |
|---------|-------------|--------------|
| Direct Read | Single record by key | Use KSDS primary key |
| Browse | Sequential access | Use STARTBR/READNEXT |
| AIX Access | Access by alternate key | Define alternate index |
| Batch Sequential | Process all records | Use sequential file organization |

### 11.2 VSAM Tuning Parameters

| Parameter | Recommended Value | Purpose |
|-----------|-------------------|---------|
| FREESPACE | (10 10) | CI and CA free space |
| BUFFERSPACE | 4096 | Buffer allocation |
| SHAREOPTIONS | (2 3) | Cross-region sharing |
| RECORDSIZE | (avg max) | Record size specification |

### 11.3 Batch Window Requirements

| Job | Estimated Duration | Window |
|-----|-------------------|--------|
| POSTTRAN | 30-60 minutes | Nightly |
| INTCALC | 15-30 minutes | Monthly |
| CREASTMT | 60-120 minutes | Monthly |
| CBEXPORT | 30-60 minutes | On-demand |

---

## 12. Security Architecture

### 12.1 Authentication Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   User      │────▶│  COSGN00C   │────▶│   USRSEC    │
│   Input     │     │  Validate   │     │   File      │
└─────────────┘     └──────┬──────┘     └─────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
      ┌─────────────┐           ┌─────────────┐
      │  Type 'A'   │           │  Type 'U'   │
      │  Admin Menu │           │  Main Menu  │
      └─────────────┘           └─────────────┘
```

### 12.2 Authorization Matrix

| Function | Admin (A) | User (U) |
|----------|-----------|----------|
| Sign-on | Yes | Yes |
| Main Menu | Yes | Yes |
| Admin Menu | Yes | No |
| Account View | Yes | Yes |
| Account Update | Yes | Yes |
| Card Management | Yes | Yes |
| Transaction Management | Yes | Yes |
| Bill Payment | Yes | Yes |
| Reports | Yes | Yes |
| User Management | Yes | No |

### 12.3 Data Protection

- Passwords stored in plain text (USRSEC file) - modernization should implement encryption
- Password field displayed as dark (DRK attribute) on screen
- No session timeout implemented - modernization should add timeout
- No audit logging - modernization should implement audit trail

---

## 13. Modernization Considerations

### 13.1 Technology Mapping

| Current Technology | Modern Equivalent |
|-------------------|-------------------|
| COBOL | Java, C#, Python |
| CICS | Spring Boot, .NET Core |
| VSAM KSDS | PostgreSQL, MySQL, DynamoDB |
| BMS Maps | React, Angular, Vue.js |
| JCL | AWS Step Functions, Airflow |
| Control-M/CA7 | AWS EventBridge, CloudWatch Events |

### 13.2 Data Migration Strategy

1. **Extract**: Use CBEXPORT to create multi-record export file
2. **Transform**: Convert EBCDIC to ASCII, normalize data structures
3. **Load**: Import into target relational database

### 13.3 Application Refactoring Patterns

| Pattern | Description |
|---------|-------------|
| Rehost | Lift and shift to cloud mainframe emulator |
| Replatform | Convert COBOL to Java with minimal changes |
| Refactor | Redesign as microservices architecture |
| Replace | Implement using modern SaaS solution |

### 13.4 API Candidates

| Function | API Endpoint | Method |
|----------|--------------|--------|
| Authentication | /api/auth/login | POST |
| Account View | /api/accounts/{id} | GET |
| Account Update | /api/accounts/{id} | PUT |
| Card List | /api/cards | GET |
| Card Detail | /api/cards/{num} | GET |
| Transaction List | /api/transactions | GET |
| Transaction Add | /api/transactions | POST |
| Bill Payment | /api/payments | POST |
| User Management | /api/users | CRUD |

---

## 14. Appendix

### 14.1 Program Inventory

#### 14.1.1 Online Programs (CICS)

| Program | Type | Lines | Description |
|---------|------|-------|-------------|
| COSGN00C | Online | ~400 | Sign-on - User authentication and session management |
| COMEN01C | Online | ~350 | Main Menu - Navigation hub for all user functions |
| COADM01C | Online | ~290 | Admin Menu - Gateway to administrative functions |
| COACTVWC | Online | ~500 | Account View - Display account details |
| COACTUPC | Online | ~800 | Account Update - Modify account information |
| COCRDLIC | Online | ~600 | Card List - Display cards for an account |
| COCRDSLC | Online | ~400 | Card Detail - Display individual card details |
| COCRDUPC | Online | ~700 | Card Update - Modify card information |
| COTRN00C | Online | ~600 | Transaction List - Browse transactions |
| COTRN01C | Online | ~400 | Transaction View - Display transaction details |
| COTRN02C | Online | ~700 | Transaction Add - Create new transactions |
| COBIL00C | Online | ~500 | Bill Payment - Process bill payments |
| CORPT00C | Online | ~650 | Reports - Generate and display reports |
| COUSR00C | Online | ~700 | User List - Display system users |
| COUSR01C | Online | ~300 | User Add - Create new users |
| COUSR02C | Online | ~415 | User Update - Modify user information |
| COUSR03C | Online | ~360 | User Delete - Remove users from system |

#### 14.1.2 Batch Programs

| Program | Type | Lines | Description |
|---------|------|-------|-------------|
| CBACT01C | Batch | ~431 | Account File Reader - Reads account file and writes to multiple output formats (fixed, array, variable-length records) |
| CBACT02C | Batch | ~179 | Card File Reader - Reads and prints card data file |
| CBACT03C | Batch | ~179 | Cross-Reference Reader - Reads and prints account cross-reference data file |
| CBACT04C | Batch | ~650 | Interest Calculation - Calculates monthly interest on account balances |
| CBCUS01C | Batch | ~179 | Customer File Reader - Reads and prints customer data file |
| CBTRN01C | Batch | ~495 | Transaction Validator - Validates daily transactions against cross-reference and account files |
| CBTRN02C | Batch | ~730 | Transaction Posting - Posts validated transactions to master files |
| CBTRN03C | Batch | ~650 | Transaction Report - Generates detailed transaction reports with date filtering |
| CBEXPORT | Batch | ~580 | Data Export - Exports data for branch migration |
| CBIMPORT | Batch | ~500 | Data Import - Imports data from branch migration files |

#### 14.1.3 Utility Programs

| Program | Type | Lines | Description |
|---------|------|-------|-------------|
| COBSWAIT | Utility | ~41 | Wait Utility - Pauses execution for specified centiseconds (calls MVSWAIT) |
| CSUTLDTC | Utility | ~158 | Date Validation - Validates dates using CEEDAYS Language Environment API |

#### 14.1.4 Optional Module Programs

##### Credit Card Authorizations (IMS-DB2-MQ)

| Program | Type | Transaction | Description |
|---------|------|-------------|-------------|
| COPAUA0C | CICS | CP00 | Authorization Request Processor - MQ trigger program that processes credit card authorization requests, validates against VSAM, stores in IMS |
| COPAUS0C | CICS | CPVS | Authorization Summary Display - Shows pending authorizations with account details, reads from IMS and VSAM |
| COPAUS1C | CICS | CPVD | Authorization Details Display - Shows comprehensive authorization information, updates IMS and inserts to DB2 |
| COPAUS2C | CICS | (Called) | Fraud Marking - Marks transactions as fraudulent and updates DB2 fraud tracking table |
| CBPAUP0C | Batch | - | Expired Authorization Purge - Batch program to purge expired authorizations and adjust available credit |
| DBUNLDGS | Batch | - | GSAM Unload - Unloads data from GSAM for IMS processing |
| PAUDBLOD | Batch | - | Authorization DB Load - Loads authorization data into IMS database |
| PAUDBUNL | Batch | - | Authorization DB Unload - Unloads authorization data from IMS database |

##### Transaction Type Management (DB2)

| Program | Type | Transaction | Description |
|---------|------|-------------|-------------|
| COTRTLIC | CICS | CTLI | Transaction Type List - Lists transaction types from DB2 with forward/backward cursor navigation, supports update and delete |
| COTRTUPC | CICS | CTTU | Transaction Type Add/Edit - Adds new or edits existing transaction types in DB2 using static embedded SQL |
| COBTUPDT | Batch | - | Transaction Type Batch Maintenance - Batch program for maintaining transaction type table in DB2 |

##### Account Extractions (MQ-VSAM)

| Program | Type | Transaction | Description |
|---------|------|-------------|-------------|
| CODATE01 | CICS | CDRD | System Date Inquiry via MQ - Demonstrates MQ request/response pattern for system date retrieval |
| COACCT01 | CICS | CDRA | Account Details Inquiry via MQ - Retrieves account information through MQ channels from VSAM |

### 14.2 Copybook Inventory

#### 14.2.1 Data Structure Copybooks

| Copybook | Lines | Description |
|----------|-------|-------------|
| CVACT01Y | 21 | Account Record - Account master data structure (300 bytes) |
| CVACT02Y | 15 | Card Record - Card master data structure (150 bytes) |
| CVACT03Y | 12 | Card Cross-Reference - Card-to-account-to-customer mapping (50 bytes) |
| CVCUS01Y | 27 | Customer Record - Customer master data structure (500 bytes) |
| CVTRA01Y | 14 | Transaction Category Balance - Category balance tracking |
| CVTRA02Y | 14 | Disclosure Group - Interest rate disclosure groups |
| CVTRA03Y | 11 | Transaction Type - Transaction type codes and descriptions (60 bytes) |
| CVTRA04Y | 13 | Transaction Category - Transaction category codes and descriptions (60 bytes) |
| CVTRA05Y | 22 | Transaction Record - Transaction master data structure (350 bytes) |
| CVTRA06Y | 22 | Daily Transaction Record - Daily transaction input format (350 bytes) |
| CVTRA07Y | 74 | Transaction Report Layout - Report headers, detail lines, and totals |
| CVEXPORT | 50 | Export Record - Multi-record export format for branch migration (500 bytes) |
| CUSTREC | - | Customer Record (alternate format) |
| COSTM01 | - | Statement Transaction Layout - Statement report record format |

#### 14.2.2 Communication and Control Copybooks

| Copybook | Lines | Description |
|----------|-------|-------------|
| COCOM01Y | 48 | Communication Area - CICS COMMAREA structure for program-to-program communication |
| COMEN02Y | 102 | Main Menu Options - Menu option definitions and navigation |
| COADM02Y | 63 | Admin Menu Options - Administrative menu option definitions |
| COTTL01Y | 20 | Title Constants - Screen title and header constants |
| CVCRD01Y | 47 | Card Work Areas - AID key definitions, navigation fields, account/card/customer IDs |

#### 14.2.3 Utility Copybooks

| Copybook | Lines | Description |
|----------|-------|-------------|
| CSDAT01Y | 30 | Date Formatting - Date display formatting structures |
| CSMSG01Y | 25 | Message Constants - Standard message text constants |
| CSMSG02Y | - | Message Constants (extended) - Additional message definitions |
| CSUSR01Y | 27 | User Security Record - User authentication data structure (80 bytes) |
| CSUTLDWY | 90 | Date Validation Working Storage - Date editing fields and flags |
| CSUTLDPY | 376 | Date Validation Procedures - Reusable date validation paragraphs |
| CODATECN | 53 | Date Conversion - Date format conversion structure (YYYYMMDD/YYYY-MM-DD) |
| CSSETATY | - | Set Attribute - Screen field attribute manipulation |
| CSSTRPFY | - | String Processing - String manipulation utilities |
| CSLKPCDY | - | Lookup Code - Code lookup utilities |

#### 14.2.4 Unused/Reserved Copybooks

| Copybook | Lines | Description |
|----------|-------|-------------|
| UNUSED1Y | - | Reserved for future use |

#### 14.2.5 Optional Module Copybooks

##### Credit Card Authorizations (IMS-DB2-MQ)

| Copybook | Description |
|----------|-------------|
| CCPAUERY | Authorization Error Response - Error handling for authorization requests |
| CCPAURLY | Authorization Reply - MQ reply message structure for authorization responses |
| CCPAURQY | Authorization Request - MQ request message structure for authorization requests |
| CIPAUDTY | IMS Authorization Detail - IMS segment structure for authorization details |
| CIPAUSMY | IMS Authorization Summary - IMS segment structure for authorization summary |
| IMSFUNCS | IMS Functions - IMS DL/I function code definitions |
| PADFLPCB | IMS PCB Definition - Program Communication Block for authorization detail |
| PASFLPCB | IMS PCB Definition - Program Communication Block for authorization summary |
| PAUTBPCB | IMS PCB Definition - Program Communication Block for authorization table |
| COPAU00 | BMS Copybook - Generated from COPAU00.bms for authorization summary screen |
| COPAU01 | BMS Copybook - Generated from COPAU01.bms for authorization detail screen |

##### Transaction Type Management (DB2)

| Copybook | Description |
|----------|-------------|
| CSDB2RPY | DB2 Reply Structure - Response structure for DB2 operations |
| CSDB2RWY | DB2 Row Working Storage - Working storage for DB2 row processing |
| COTRTLI | BMS Copybook - Generated from COTRTLI.bms for transaction type list screen |
| COTRTUP | BMS Copybook - Generated from COTRTUP.bms for transaction type update screen |

### 14.5 BMS Map Inventory

#### 14.5.1 Core Application BMS Maps

| BMS Map | Screen | Program | Description |
|---------|--------|---------|-------------|
| COSGN00.bms | Sign-on | COSGN00C | User authentication screen |
| COMEN01.bms | Main Menu | COMEN01C | Regular user main menu |
| COADM01.bms | Admin Menu | COADM01C | Administrator menu |
| COACTVW.bms | Account View | COACTVWC | Account details display |
| COACTUP.bms | Account Update | COACTUPC | Account modification |
| COCRDLI.bms | Card List | COCRDLIC | Card listing with pagination |
| COCRDSL.bms | Card Detail | COCRDSLC | Single card details |
| COCRDUP.bms | Card Update | COCRDUPC | Card modification |
| COTRN00.bms | Transaction List | COTRN00C | Transaction listing |
| COTRN01.bms | Transaction View | COTRN01C | Transaction details |
| COTRN02.bms | Transaction Add | COTRN02C | New transaction entry |
| COBIL00.bms | Bill Payment | COBIL00C | Bill payment processing |
| CORPT00.bms | Reports | CORPT00C | Transaction reports |
| COUSR00.bms | User List | COUSR00C | User listing (Admin) |
| COUSR01.bms | User Add | COUSR01C | New user entry (Admin) |
| COUSR02.bms | User Update | COUSR02C | User modification (Admin) |
| COUSR03.bms | User Delete | COUSR03C | User deletion (Admin) |

#### 14.5.2 Optional Module BMS Maps

##### Credit Card Authorizations (IMS-DB2-MQ)

| BMS Map | Screen | Program | Description |
|---------|--------|---------|-------------|
| COPAU00.bms | Auth Summary | COPAUS0C | Pending authorization summary display |
| COPAU01.bms | Auth Detail | COPAUS1C | Authorization detail view with fraud marking |

##### Transaction Type Management (DB2)

| BMS Map | Screen | Program | Description |
|---------|--------|---------|-------------|
| COTRTLI.bms | Type List | COTRTLIC | Transaction type listing from DB2 |
| COTRTUP.bms | Type Update | COTRTUPC | Transaction type add/edit |

### 14.3 JCL Job Inventory

#### 14.3.1 Data Loading Jobs

| JCL Job | Description |
|---------|-------------|
| ACCTFILE.jcl | Define and load Account VSAM KSDS file |
| CARDFILE.jcl | Define and load Card VSAM KSDS file |
| CUSTFILE.jcl | Define and load Customer VSAM KSDS file |
| XREFFILE.jcl | Define and load Card Cross-Reference VSAM KSDS file |
| TRANFILE.jcl | Define and load Transaction VSAM KSDS file |
| DUSRSECJ.jcl | Create and load User Security file |
| DEFCUST.jcl | Define Customer VSAM cluster |
| TCATBALF.jcl | Define and load Transaction Category Balance file |
| DISCGRP.jcl | Define and load Disclosure Group file |
| TRANTYPE.jcl | Define and load Transaction Type reference file |
| TRANCATG.jcl | Define and load Transaction Category reference file |

#### 14.3.2 Batch Processing Jobs

| JCL Job | Program | Description |
|---------|---------|-------------|
| POSTTRAN.jcl | CBTRN02C | Daily transaction posting - validates and posts transactions to master files |
| INTCALC.jcl | CBACT04C | Monthly interest calculation on account balances |
| CBEXPORT.jcl | CBEXPORT | Data export for branch migration |
| CBIMPORT.jcl | CBIMPORT | Data import from branch migration files |
| TRANREPT.jcl | CBTRN03C | Generate transaction detail report with date filtering |
| COMBTRAN.jcl | - | Combine transaction files |

#### 14.3.3 File Management Jobs

| JCL Job | Description |
|---------|-------------|
| CLOSEFIL.jcl | Close CICS files before batch processing |
| OPENFIL.jcl | Open CICS files after batch processing |
| TRANBKP.jcl | Backup transaction file to GDG |
| WAITSTEP.jcl | Synchronization job - waits for parallel jobs to complete |

#### 14.3.4 Index and Report Jobs

| JCL Job | Description |
|---------|-------------|
| TRANIDX.jcl | Build alternate index for transaction file |
| REPTFILE.jcl | Define report output file |
| PRTCATBL.jcl | Print transaction category balance report |
| DALYREJS.jcl | Process daily transaction rejections |

#### 14.3.5 Utility Jobs

| JCL Job | Description |
|---------|-------------|
| READACCT.jcl | Read and display account file contents |
| READCARD.jcl | Read and display card file contents |
| READCUST.jcl | Read and display customer file contents |
| READXREF.jcl | Read and display cross-reference file contents |
| DEFGDGB.jcl | Define Generation Data Group base |
| DEFGDGD.jcl | Define Generation Data Group with model |
| ESDSRRDS.jcl | Define ESDS/RRDS VSAM files |
| CREASTMT.JCL | Create customer statements |
| TXT2PDF1.JCL | Convert text reports to PDF format |
| FTPJCL.JCL | FTP file transfer job |
| INTRDRJ1.JCL | Internal reader job 1 |
| INTRDRJ2.JCL | Internal reader job 2 |
| CBADMCDJ.jcl | Administrative card processing |

#### 14.3.6 Optional Module Jobs

##### Credit Card Authorizations (IMS-DB2-MQ)

| JCL Job | Program | Description |
|---------|---------|-------------|
| CBPAUP0J.jcl | CBPAUP0C | Batch job to purge expired authorizations |
| DBPAUTP0.jcl | - | IMS database definition for authorization storage |
| LOADPADB.JCL | PAUDBLOD | Load authorization data into IMS database |
| UNLDGSAM.JCL | DBUNLDGS | Unload GSAM data for IMS processing |
| UNLDPADB.JCL | PAUDBUNL | Unload authorization data from IMS database |

##### Transaction Type Management (DB2)

| JCL Job | Program | Description |
|---------|---------|-------------|
| CREADB21.jcl | DSNTEP4 | Creates CardDemo DB2 database and loads transaction type tables |
| TRANEXTR.jcl | DSNTIAUL | Extracts latest DB2 data for transaction types to VSAM-compatible files |
| MNTTRDB2.jcl | COBTUPDT | Batch maintenance of transaction type table in DB2 |

### 14.4 Dataset Naming Convention

```
AWS.M2.CARDDEMO.<entity>.<type>.<organization>

Examples:
AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS
AWS.M2.CARDDEMO.DALYTRAN.PS
AWS.M2.CARDDEMO.TRANSACT.BKUP (GDG)
AWS.M2.CARDDEMO.LOADLIB (PDS)
```

---

*Document generated through reverse engineering of CardDemo COBOL source code for modernization planning purposes.*
