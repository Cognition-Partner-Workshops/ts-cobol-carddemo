# CardDemo Application - Complete Inventory

**Repository**: `aws-mainframe-modernization-carddemo`
**Analysis Date**: 2026-02-09
**Source**: All conclusions traced to code artifacts in the repository.

---

## Table of Contents

1. [Programs Inventory](#1-programs-inventory)
   - 1.1 [Online CICS Programs](#11-online-cics-programs)
   - 1.2 [Batch COBOL Programs](#12-batch-cobol-programs)
   - 1.3 [Utility / Shared Programs](#13-utility--shared-programs)
2. [Copybooks Inventory](#2-copybooks-inventory)
3. [JCL Inventory](#3-jcl-inventory)
4. [BMS Maps Inventory](#4-bms-maps-inventory)
5. [Logical Grouping by Responsibility](#5-logical-grouping-by-responsibility)
6. [Batch vs Online Classification Summary](#6-batch-vs-online-classification-summary)
7. [Scheduler Configurations](#7-scheduler-configurations)
8. [Data Files Inventory](#8-data-files-inventory)

---

## 1. Programs Inventory

**Location**: `app/cbl/`
**Total Programs**: 31

### 1.1 Online CICS Programs

All online programs use CICS SEND MAP/RECEIVE MAP, reference EIBCALEN for first-call detection, and use EXEC CICS XCTL for inter-program navigation. Each has a 4-character TRANID.

| # | Program | TRANID | Source File | Function | Files Accessed | Copybooks Used |
|---|---------|--------|-------------|----------|----------------|----------------|
| 1 | COSGN00C | CC00 | `app/cbl/COSGN00C.cbl` | User authentication and session management. Entry point for all users. Validates credentials against USRSEC file. Routes to COADM01C (admin) or COMEN01C (regular user). | USRSEC | COCOM01Y, COSGN00, CSUSR01Y, CSDAT01Y, CSMSG01Y, COTTL01Y, CSSETATY, CSMSG02Y |
| 2 | COMEN01C | CM00 | `app/cbl/COMEN01C.cbl` | Main menu for regular users. Hub dispatcher routing to account, card, transaction, and report operations. | None (dispatcher only) | COCOM01Y, COMEN01, COMEN02Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y |
| 3 | COADM01C | CA00 | `app/cbl/COADM01C.cbl` | Admin menu for admin users. Routes to user management and admin functions. | USRSEC | COCOM01Y, COADM01, COADM02Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y |
| 4 | COACTVWC | CAVW | `app/cbl/COACTVWC.cbl` | View account details. Reads account, customer, and card data. Validates account number and displays related information. | ACCTDAT, CUSTDAT, CARDDAT, CXACAIX | COCOM01Y, COACTVW, CVACT01Y, CVACT02Y, CVCUS01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSMSG02Y, CSUSR01Y |
| 5 | COACTUPC | CAUP | `app/cbl/COACTUPC.cbl` | Update account information. Validates credit limits, current balance, dates, FICO score. Performs comprehensive input validation before update. | ACCTDAT, CUSTDAT, CARDDAT, CXACAIX | COCOM01Y, COACTUP, CVACT01Y, CVACT02Y, CVCUS01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSMSG02Y, CSUSR01Y, CSUTLDPY, CSUTLDWY |
| 6 | COCRDLIC | CCLI | `app/cbl/COCRDLIC.cbl` | List credit cards. Lists all cards (admin) or cards associated with an account (regular user). Supports pagination and selection for detail/update. | CARDDAT, CARDAIX | COCOM01Y, COCRDLI, CVCRD01Y, CVACT02Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y |
| 7 | COCRDSLC | CCDL | `app/cbl/COCRDSLC.cbl` | View credit card details. Displays card number, account, CVV, embossed name, expiration date, and status. | CARDDAT, CARDAIX | COCOM01Y, COCRDSL, CVCRD01Y, CVACT02Y, CVCUS01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSMSG02Y, CSUSR01Y |
| 8 | COCRDUPC | CCUP | `app/cbl/COCRDUPC.cbl` | Update credit card details. Validates card name (alpha only), status (Y/N), expiry month (1-12), expiry year (1950-2099). Supports lock-for-update and change detection. | CARDDAT, CARDAIX | COCOM01Y, COCRDUP, CVCRD01Y, CVACT02Y, CVCUS01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSMSG02Y, CSUSR01Y |
| 9 | COTRN00C | CT00 | `app/cbl/COTRN00C.cbl` | List transactions with pagination. Supports search by transaction ID. Allows selection for detail view. | TRANSACT | COCOM01Y, COTRN00, CVTRA05Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y |
| 10 | COTRN01C | CT01 | `app/cbl/COTRN01C.cbl` | View individual transaction details. Reads from TRANSACT by transaction ID and displays all fields. | TRANSACT | COCOM01Y, COTRN01, CVTRA05Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y |
| 11 | COTRN02C | CT02 | `app/cbl/COTRN02C.cbl` | Add new transactions. Validates account and card numbers against ACCTDAT and CCXREF. Supports transaction type, category, source, amount, and merchant info. | TRANSACT, ACCTDAT, CCXREF, CXACAIX | COCOM01Y, COTRN02, CVTRA05Y, CVACT01Y, CVACT03Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y |
| 12 | COBIL00C | CB00 | `app/cbl/COBIL00C.cbl` | Bill payment processing. Reads account data, creates transaction record for bill payment, updates account balance. | TRANSACT, ACCTDAT, CXACAIX | COCOM01Y, COBIL00, CVTRA05Y, CVACT01Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y |
| 13 | CORPT00C | CR00 | `app/cbl/CORPT00C.cbl` | Print transaction reports by submitting batch job from online using extra partition TDQ. Supports monthly, yearly, and custom date range reports. | TRANSACT | COCOM01Y, CORPT00, CVTRA05Y, COTTL01Y, CSDAT01Y, CSMSG01Y, CSUSR01Y |
| 14 | COUSR00C | CU00 | `app/cbl/COUSR00C.cbl` | List all users from USRSEC file with pagination. Supports selection for update or delete operations. | USRSEC | COCOM01Y, COUSR00, CSUSR01Y, COTTL01Y, CSDAT01Y, CSMSG01Y |
| 15 | COUSR01C | CU01 | `app/cbl/COUSR01C.cbl` | Add new user to USRSEC file. Validates first name, last name, user ID, password, and user type (admin/regular). | USRSEC | COCOM01Y, COUSR01, CSUSR01Y, COTTL01Y, CSDAT01Y, CSMSG01Y |
| 16 | COUSR02C | CU02 | `app/cbl/COUSR02C.cbl` | Update user information in USRSEC file. Tracks modifications and validates all fields before update. | USRSEC | COCOM01Y, COUSR02, CSUSR01Y, COTTL01Y, CSDAT01Y, CSMSG01Y |
| 17 | COUSR03C | CU03 | `app/cbl/COUSR03C.cbl` | Delete user from USRSEC file. Requires confirmation before deletion. | USRSEC | COCOM01Y, COUSR03, CSUSR01Y, COTTL01Y, CSDAT01Y, CSMSG01Y |

### 1.2 Batch COBOL Programs

All batch programs use standard COBOL FILE-CONTROL with OPEN/READ/WRITE/CLOSE operations. No CICS dependencies.

| # | Program | Source File | Function | Files Accessed (Input) | Files Accessed (Output) |
|---|---------|-------------|----------|------------------------|-------------------------|
| 1 | CBTRN01C | `app/cbl/CBTRN01C.cbl` | Post daily transaction records. Reads DALYTRAN-FILE sequentially, validates against CUSTOMER-FILE, XREF-FILE, CARD-FILE, ACCOUNT-FILE. Writes valid transactions to TRANSACT-FILE. | DALYTRAN, CUSTOMER-FILE, XREF-FILE, CARD-FILE, ACCOUNT-FILE | TRANSACT-FILE |
| 2 | CBTRN02C | `app/cbl/CBTRN02C.cbl` | Core daily transaction processing. Reads daily transactions, validates against cross-reference and account files, posts to transaction master. Generates reject file for failed validations. | DALYTRAN, CARDXREF, ACCTDAT, TRANTYPE, TRANCATG | TRANSACT, DALYREJS |
| 3 | CBTRN03C | `app/cbl/CBTRN03C.cbl` | Print transaction detail report. Reads TRANSACT sequentially, looks up XREF, TRANTYPE, TRANCATG. Writes formatted report to REPORT-FILE with date filtering. | TRANSACT, XREF-FILE, TRANTYPE-FILE, TRANCATG-FILE | REPORT-FILE |
| 4 | CBACT01C | `app/cbl/CBACT01C.cbl` | Read account file and write into multiple output files. Demonstrates various file organization types (indexed, sequential, line sequential, variable-length). | ACCTFILE (indexed VSAM KSDS) | OUT-FILE (sequential), ARRY-FILE (line sequential), VBRC-FILE (variable-length) |
| 5 | CBACT02C | `app/cbl/CBACT02C.cbl` | Read and print card data file. Reads CARDFILE (indexed VSAM KSDS) sequentially and displays each record. | CARDFILE (indexed) | Display output only |
| 6 | CBACT03C | `app/cbl/CBACT03C.cbl` | Read and print account cross-reference data file. Reads XREFFILE (indexed VSAM KSDS) sequentially and displays each record. | XREFFILE (indexed) | Display output only |
| 7 | CBACT04C | `app/cbl/CBACT04C.cbl` | Monthly interest calculation on account balances. Reads transaction category balances, calculates interest, updates account file. Runs on 1st of month. | TCATBAL-FILE (indexed), DISCGRP-FILE (indexed), ACCOUNT-FILE (indexed) | ACCOUNT-FILE (updated in place) |
| 8 | CBCUS01C | `app/cbl/CBCUS01C.cbl` | Read and print customer data file. Reads CUSTFILE (indexed VSAM KSDS) sequentially and displays each record. | CUSTFILE (indexed) | Display output only |
| 9 | CBSTM03A | `app/cbl/CBSTM03A.CBL` | Statement generation main program. Generates customer statements in plain text and HTML formats from transaction data. Calls CBSTM03B for all file I/O. Uses ALTER statement for dynamic flow control. | Via CBSTM03B: TRNX-FILE, XREF-FILE, CUST-FILE, ACCT-FILE | Statement output (text and HTML) |
| 10 | CBSTM03B | `app/cbl/CBSTM03B.CBL` | Statement generation file I/O subroutine. Called by CBSTM03A. Handles OPEN/READ/CLOSE for TRNX-FILE, XREF-FILE, CUST-FILE, ACCT-FILE. Uses LINKAGE SECTION for parameter passing. | TRNX-FILE (indexed), XREF-FILE (indexed), CUST-FILE (indexed), ACCT-FILE (indexed) | Return data via LINKAGE |
| 11 | CBEXPORT | `app/cbl/CBEXPORT.cbl` | Export customer data for branch migration. Reads 5 normalized VSAM files (customer, account, card, cross-reference, transaction) and consolidates into single multi-record export file with 500-byte records. Each record tagged with type ('C'=Customer, 'A'=Account, 'X'=Cross-ref, 'T'=Transaction, 'D'=Card). | CUSTFILE, ACCTFILE, CARDFILE, XREFFILE, TRANFILE | EXPORT-FILE (sequential, 500-byte records) |
| 12 | CBIMPORT | `app/cbl/CBIMPORT.cbl` | Import customer data for branch migration. Reads consolidated export file, demultiplexes by record type, writes to separate normalized import files. Logs errors to error file. | IMPORT-FILE (sequential) | CUST-IMPORT, ACCT-IMPORT, CARD-IMPORT, XREF-IMPORT, TRAN-IMPORT, ERROR-FILE |
| 13 | COBSWAIT | `app/cbl/COBSWAIT.cbl` | Utility: Wait program. Accepts wait time parameter (in centiseconds) from SYSIN, calls MVSWAIT to pause execution. Used by WAITSTEP JCL for job synchronization. | SYSIN (parameter) | None |

### 1.3 Utility / Shared Programs

| # | Program | Source File | Type | Function |
|---|---------|-------------|------|----------|
| 1 | CSUTLDTC | `app/cbl/CSUTLDTC.cbl` | Called subroutine (CICS or Batch) | Date validation utility. Accepts date and format via LINKAGE SECTION, calls CEEDAYS API to validate. Returns validation result message. Used by COACTUPC for date field validation. |

---

## 2. Copybooks Inventory

**Location**: `app/cpy/`
**Total Copybooks**: 30

### 2.1 Data Record Layouts

| # | Copybook | Source File | Purpose | Key Fields | Record Length |
|---|----------|-------------|---------|------------|---------------|
| 1 | CVACT01Y | `app/cpy/CVACT01Y.cpy` | Account master record layout | ACCT-ID (11 digits), ACCT-ACTIVE-STATUS, ACCT-CURR-BAL, ACCT-CREDIT-LIMIT, ACCT-CASH-CREDIT-LIMIT, ACCT-OPEN-DATE, ACCT-EXPIRAION-DATE, ACCT-REISSUE-DATE, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT, ACCT-GROUP-ID | 300 bytes |
| 2 | CVACT02Y | `app/cpy/CVACT02Y.cpy` | Card master record layout | CARD-NUM (16 chars), CARD-ACCT-ID (11 digits), CARD-CVV-CD (3 digits), CARD-EMBOSSED-NAME (50 chars), CARD-EXPIRAION-DATE, CARD-ACTIVE-STATUS | 150 bytes |
| 3 | CVACT03Y | `app/cpy/CVACT03Y.cpy` | Card cross-reference record layout | XREF-CARD-NUM (16 chars), XREF-CUST-ID (9 digits), XREF-ACCT-ID (11 digits) | 50 bytes |
| 4 | CVCUS01Y | `app/cpy/CVCUS01Y.cpy` | Customer master record layout | CUST-ID (9 digits), CUST-FIRST-NAME, CUST-MIDDLE-NAME, CUST-LAST-NAME, CUST-ADDR-LINE-1/2/3, CUST-ADDR-STATE-CD, CUST-ADDR-ZIP, CUST-PHONE-NUM-1/2, CUST-SSN, CUST-GOVT-ISSUED-ID, CUST-DOB-YYYY-MM-DD, CUST-EFT-ACCOUNT-ID, CUST-PRI-CARD-HOLDER-IND, CUST-FICO-CREDIT-SCORE | 500 bytes |
| 5 | CVTRA05Y | `app/cpy/CVTRA05Y.cpy` | Transaction master record layout | TRAN-ID (16 chars), TRAN-TYPE-CD, TRAN-CAT-CD, TRAN-SOURCE, TRAN-DESC, TRAN-AMT, TRAN-CARD-NUM, TRAN-MERCHANT-ID, TRAN-MERCHANT-NAME, TRAN-MERCHANT-CITY, TRAN-MERCHANT-ZIP, TRAN-ORIG-TS, TRAN-PROC-TS | 350 bytes |
| 6 | CVTRA06Y | `app/cpy/CVTRA06Y.cpy` | Daily transaction record layout | DALYTRAN-ID, fields parallel to CVTRA05Y for daily input processing | 350 bytes |
| 7 | CVEXPORT | `app/cpy/CVEXPORT.cpy` | Multi-record export format for branch migration | EXPORT-REC-TYPE ('C'/'A'/'X'/'T'/'D'), EXPORT-SEQUENCE-NUM (9 digits), EXPORT-TIMESTAMP, EXPORT-BRANCH-ID, EXPORT-REGION-CODE. Uses REDEFINES for 5 entity types. | 500 bytes |
| 8 | CUSTREC | `app/cpy/CUSTREC.cpy` | Alternate customer record layout | Customer record fields | Not found in code (record length not explicitly stated) |

### 2.2 Screen and Transaction Layouts

| # | Copybook | Source File | Purpose |
|---|----------|-------------|---------|
| 9 | COCOM01Y | `app/cpy/COCOM01Y.cpy` | Application COMMAREA layout. Shared communication area between all online programs. Contains CDEMO-FROM-TRANID, CDEMO-FROM-PROGRAM, CDEMO-TO-TRANID, CDEMO-TO-PROGRAM, CDEMO-ACCT-ID, CDEMO-CARD-NUM, CDEMO-CUST-ID, CDEMO-LAST-MAP, CDEMO-LAST-MAPSET, user type flags (CDEMO-USRTYP-USER/ADMIN), program flow flags (CDEMO-PGM-ENTER/REENTER). |
| 10 | COADM02Y | `app/cpy/COADM02Y.cpy` | Admin menu option data structure. |
| 11 | COMEN02Y | `app/cpy/COMEN02Y.cpy` | Main menu option data structure. |
| 12 | COTTL01Y | `app/cpy/COTTL01Y.cpy` | Screen title line definitions. Common header used across all online screens. |
| 13 | CVCRD01Y | `app/cpy/CVCRD01Y.cpy` | Credit card working storage variables. Common card-related working variables shared by COCRDLIC, COCRDSLC, COCRDUPC. |
| 14 | COSTM01 | `app/cpy/COSTM01.CPY` | Transaction report/statement record layout. Used by CBSTM03A for statement generation. Contains TRNX-CARD-NUM, TRNX-ID. |

### 2.3 Transaction Type and Category Layouts

| # | Copybook | Source File | Purpose |
|---|----------|-------------|---------|
| 15 | CVTRA01Y | `app/cpy/CVTRA01Y.cpy` | Transaction type record layout. |
| 16 | CVTRA02Y | `app/cpy/CVTRA02Y.cpy` | Transaction type filter/search layout. |
| 17 | CVTRA03Y | `app/cpy/CVTRA03Y.cpy` | Transaction category record layout. |
| 18 | CVTRA04Y | `app/cpy/CVTRA04Y.cpy` | Transaction category filter/search layout. |
| 19 | CVTRA07Y | `app/cpy/CVTRA07Y.cpy` | Transaction category balance record layout. Used by CBACT04C for interest calculation. |

### 2.4 Common Utility Copybooks

| # | Copybook | Source File | Purpose |
|---|----------|-------------|---------|
| 20 | CSDAT01Y | `app/cpy/CSDAT01Y.cpy` | Current date working storage. Provides WS-CURDATE fields used by all online programs for date display. |
| 21 | CSMSG01Y | `app/cpy/CSMSG01Y.cpy` | Common message definitions. Standardized messages used across online programs. |
| 22 | CSMSG02Y | `app/cpy/CSMSG02Y.cpy` | Abend handling variables. Contains ABEND-CULPRIT, ABEND-CODE, ABEND-REASON. |
| 23 | CSUSR01Y | `app/cpy/CSUSR01Y.cpy` | Signed-on user data. Contains user session information used across all online programs. |
| 24 | CSSETATY | `app/cpy/CSSETATY.cpy` | Set attribute utility. Screen attribute manipulation for BMS maps. |
| 25 | CSSTRPFY | `app/cpy/CSSTRPFY.cpy` | String processing functions. |
| 26 | CSLKPCDY | `app/cpy/CSLKPCDY.cpy` | Lookup code definitions. |
| 27 | CSUTLDPY | `app/cpy/CSUTLDPY.cpy` | Date utility parameter layout. Used by COACTUPC when calling CSUTLDTC. |
| 28 | CSUTLDWY | `app/cpy/CSUTLDWY.cpy` | Date utility working storage. Used with CSUTLDPY for date validation. |
| 29 | CODATECN | `app/cpy/CODATECN.cpy` | Date conversion copybook. |
| 30 | UNUSED1Y | `app/cpy/UNUSED1Y.cpy` | Unused copybook placeholder. Not referenced by any program in the codebase. |

---

## 3. JCL Inventory

**Location**: `app/jcl/`
**Total JCL Scripts**: 39

### 3.1 Core Batch Processing Jobs

| # | JCL | Source File | Function | Programs Executed | Frequency (per scheduler) |
|---|-----|-------------|----------|-------------------|---------------------------|
| 1 | POSTTRAN | `app/jcl/POSTTRAN.jcl` | Post daily transactions to transaction master. Core nightly batch job. | CBTRN02C | Daily |
| 2 | INTCALC | `app/jcl/INTCALC.jcl` | Calculate monthly interest on account balances. | CBACT04C | Monthly (1st of month) |
| 3 | CREASTMT | `app/jcl/CREASTMT.JCL` | Generate customer statements in text and HTML. | CBSTM03A, CBSTM03B | Monthly |
| 4 | TRANREPT | `app/jcl/TRANREPT.jcl` | Generate transaction detail report. | CBTRN03C | On demand / Daily |

### 3.2 Data Export/Import Jobs

| # | JCL | Source File | Function | Programs Executed |
|---|-----|-------------|----------|-------------------|
| 5 | CBEXPORT | `app/jcl/CBEXPORT.jcl` | Export customer data for branch migration. | CBEXPORT |
| 6 | CBIMPORT | `app/jcl/CBIMPORT.jcl` | Import customer data for branch migration. | CBIMPORT |

### 3.3 File Management and Synchronization Jobs

| # | JCL | Source File | Function |
|---|-----|-------------|----------|
| 7 | CLOSEFIL | `app/jcl/CLOSEFIL.jcl` | Close CICS files before batch processing. Prevents online/batch conflicts. |
| 8 | OPENFIL | `app/jcl/OPENFIL.jcl` | Open CICS files after batch processing completes. Restores online access. |
| 9 | WAITSTEP | `app/jcl/WAITSTEP.jcl` | Synchronization job. Executes COBSWAIT to pause for a specified interval. Used as barrier between batch job phases. |
| 10 | TRANBKP | `app/jcl/TRANBKP.jcl` | Backup transaction file to GDG (Generation Data Group). |
| 11 | COMBTRAN | `app/jcl/COMBTRAN.jcl` | Combine/merge transaction files. |

### 3.4 VSAM Data Loading Jobs

These JCL scripts use IDCAMS (DEFINE CLUSTER, DELETE, REPRO) and IEBGENER to initialize VSAM KSDS clusters from physical sequential (PS) datasets.

| # | JCL | Source File | Function | Target VSAM File |
|---|-----|-------------|----------|------------------|
| 12 | ACCTFILE | `app/jcl/ACCTFILE.jcl` | Define and load account VSAM KSDS file. | AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS |
| 13 | CARDFILE | `app/jcl/CARDFILE.jcl` | Define and load card VSAM KSDS file. | AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS |
| 14 | CUSTFILE | `app/jcl/CUSTFILE.jcl` | Define and load customer VSAM KSDS file. | AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS |
| 15 | XREFFILE | `app/jcl/XREFFILE.jcl` | Define and load cross-reference VSAM KSDS file. | AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS |
| 16 | TRANFILE | `app/jcl/TRANFILE.jcl` | Define and load transaction VSAM KSDS file. | AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS |
| 17 | DUSRSECJ | `app/jcl/DUSRSECJ.jcl` | Define and load user security VSAM file from in-stream data. | AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS |

### 3.5 Reference Data Loading Jobs

| # | JCL | Source File | Function | Target File |
|---|-----|-------------|----------|-------------|
| 18 | TRANTYPE | `app/jcl/TRANTYPE.jcl` | Load/refresh transaction type reference data. | Transaction type VSAM file |
| 19 | TRANCATG | `app/jcl/TRANCATG.jcl` | Load/refresh transaction category reference data. | Transaction category VSAM file |
| 20 | TCATBALF | `app/jcl/TCATBALF.jcl` | Load/refresh transaction category balance file. | TCATBAL VSAM file |
| 21 | DISCGRP | `app/jcl/DISCGRP.jcl` | Load/refresh disclosure group data. | Disclosure group VSAM file |
| 22 | TRANIDX | `app/jcl/TRANIDX.jcl` | Define/build transaction alternate index (AIX). | TRANSACT AIX path |
| 23 | REPTFILE | `app/jcl/REPTFILE.jcl` | Define report file datasets. | Report output files |

### 3.6 Data Read/Print Utility Jobs

| # | JCL | Source File | Function | Program Executed |
|---|-----|-------------|----------|------------------|
| 24 | READACCT | `app/jcl/READACCT.jcl` | Read and display account file contents. | CBACT01C |
| 25 | READCARD | `app/jcl/READCARD.jcl` | Read and display card file contents. | CBACT02C |
| 26 | READCUST | `app/jcl/READCUST.jcl` | Read and display customer file contents. | CBCUS01C |
| 27 | READXREF | `app/jcl/READXREF.jcl` | Read and display cross-reference file contents. | CBACT03C |
| 28 | PRTCATBL | `app/jcl/PRTCATBL.jcl` | Print transaction category balance file. | Not found in code (likely utility) |

### 3.7 GDG and Catalog Definition Jobs

| # | JCL | Source File | Function |
|---|-----|-------------|----------|
| 29 | DEFGDGB | `app/jcl/DEFGDGB.jcl` | Define GDG base for transaction backups (AWS.M2.CARDDEMO.TRANSACT.BKUP). |
| 30 | DEFGDGD | `app/jcl/DEFGDGD.jcl` | Define GDG base for daily transaction data. |
| 31 | DEFCUST | `app/jcl/DEFCUST.jcl` | Define customer VSAM cluster. |
| 32 | DALYREJS | `app/jcl/DALYREJS.jcl` | Define/manage daily rejects file for failed transaction validations. |

### 3.8 Specialized / Optional Jobs

| # | JCL | Source File | Function |
|---|-----|-------------|----------|
| 33 | CBADMCDJ | `app/jcl/CBADMCDJ.jcl` | Admin card maintenance job (optional). |
| 34 | ESDSRRDS | `app/jcl/ESDSRRDS.jcl` | ESDS and RRDS file definitions (demonstrates non-KSDS VSAM types). |
| 35 | FTPJCL | `app/jcl/FTPJCL.JCL` | FTP file transfer job. |
| 36 | INTRDRJ1 | `app/jcl/INTRDRJ1.JCL` | Internal reader job submission (method 1). |
| 37 | INTRDRJ2 | `app/jcl/INTRDRJ2.JCL` | Internal reader job submission (method 2). |
| 38 | TXT2PDF1 | `app/jcl/TXT2PDF1.JCL` | Convert text report to PDF format. |

---

## 4. BMS Maps Inventory

**Location**: `app/bms/`
**Total BMS Maps**: 17

Each BMS map defines a 3270 terminal screen layout used by its corresponding CICS COBOL program.

| # | BMS Map | Source File | Associated Program | Associated TRANID | Screen Purpose |
|---|---------|-------------|-------------------|-------------------|----------------|
| 1 | COSGN00 | `app/bms/COSGN00.bms` | COSGN00C | CC00 | Sign-on screen. User ID and password fields. |
| 2 | COMEN01 | `app/bms/COMEN01.bms` | COMEN01C | CM00 | Main menu screen for regular users. |
| 3 | COADM01 | `app/bms/COADM01.bms` | COADM01C | CA00 | Admin menu screen. |
| 4 | COACTVW | `app/bms/COACTVW.bms` | COACTVWC | CAVW | Account view screen. |
| 5 | COACTUP | `app/bms/COACTUP.bms` | COACTUPC | CAUP | Account update screen. |
| 6 | COCRDLI | `app/bms/COCRDLI.bms` | COCRDLIC | CCLI | Credit card list screen. |
| 7 | COCRDSL | `app/bms/COCRDSL.bms` | COCRDSLC | CCDL | Credit card detail/select screen. |
| 8 | COCRDUP | `app/bms/COCRDUP.bms` | COCRDUPC | CCUP | Credit card update screen. |
| 9 | COTRN00 | `app/bms/COTRN00.bms` | COTRN00C | CT00 | Transaction list screen. |
| 10 | COTRN01 | `app/bms/COTRN01.bms` | COTRN01C | CT01 | Transaction view screen. |
| 11 | COTRN02 | `app/bms/COTRN02.bms` | COTRN02C | CT02 | Transaction add screen. |
| 12 | COBIL00 | `app/bms/COBIL00.bms` | COBIL00C | CB00 | Bill payment screen. |
| 13 | CORPT00 | `app/bms/CORPT00.bms` | CORPT00C | CR00 | Report generation screen. |
| 14 | COUSR00 | `app/bms/COUSR00.bms` | COUSR00C | CU00 | User list screen. |
| 15 | COUSR01 | `app/bms/COUSR01.bms` | COUSR01C | CU01 | User add screen. |
| 16 | COUSR02 | `app/bms/COUSR02.bms` | COUSR02C | CU02 | User update screen. |
| 17 | COUSR03 | `app/bms/COUSR03.bms` | COUSR03C | CU03 | User delete confirmation screen. |

---

## 5. Logical Grouping by Responsibility

### 5.1 Authentication and Session Management

| Component | Type | Artifact |
|-----------|------|----------|
| COSGN00C | Online Program | User login, credential validation against USRSEC |
| COSGN00 | BMS Map | Sign-on screen |
| CSUSR01Y | Copybook | Signed-on user session data |

**Data Flow**: User enters credentials -> COSGN00C validates against USRSEC file -> Routes to COADM01C (admin type 'A') or COMEN01C (regular user).

### 5.2 Menu Navigation

| Component | Type | Artifact |
|-----------|------|----------|
| COMEN01C | Online Program | Regular user main menu dispatcher |
| COADM01C | Online Program | Admin user menu dispatcher |
| COMEN01 | BMS Map | Main menu screen |
| COADM01 | BMS Map | Admin menu screen |
| COCOM01Y | Copybook | Application COMMAREA for inter-program communication |
| COMEN02Y | Copybook | Menu option definitions |
| COADM02Y | Copybook | Admin menu option definitions |

**Navigation**: COMEN01C dispatches to account (COACTVWC/COACTUPC), card (COCRDLIC), transaction (COTRN00C/COTRN02C), bill pay (COBIL00C), and report (CORPT00C) screens via EXEC CICS XCTL.

### 5.3 Account Management

| Component | Type | Artifact |
|-----------|------|----------|
| COACTVWC | Online Program | View account details |
| COACTUPC | Online Program | Update account information |
| COACTVW | BMS Map | Account view screen |
| COACTUP | BMS Map | Account update screen |
| CVACT01Y | Copybook | Account record layout |

**Files**: ACCTDAT (VSAM KSDS, primary), CUSTDAT, CARDDAT, CXACAIX (alternate index path).

### 5.4 Card Management

| Component | Type | Artifact |
|-----------|------|----------|
| COCRDLIC | Online Program | List credit cards |
| COCRDSLC | Online Program | View card details |
| COCRDUPC | Online Program | Update card information |
| COCRDLI | BMS Map | Card list screen |
| COCRDSL | BMS Map | Card detail screen |
| COCRDUP | BMS Map | Card update screen |
| CVACT02Y | Copybook | Card record layout |
| CVCRD01Y | Copybook | Card working storage variables |

**Files**: CARDDAT (VSAM KSDS), CARDAIX (alternate index by account).

### 5.5 Transaction Management (Online)

| Component | Type | Artifact |
|-----------|------|----------|
| COTRN00C | Online Program | List transactions |
| COTRN01C | Online Program | View transaction detail |
| COTRN02C | Online Program | Add new transaction |
| COTRN00 | BMS Map | Transaction list screen |
| COTRN01 | BMS Map | Transaction view screen |
| COTRN02 | BMS Map | Transaction add screen |
| CVTRA05Y | Copybook | Transaction record layout |

**Files**: TRANSACT (VSAM KSDS), ACCTDAT, CCXREF, CXACAIX.

### 5.6 Bill Payment

| Component | Type | Artifact |
|-----------|------|----------|
| COBIL00C | Online Program | Process bill payment |
| COBIL00 | BMS Map | Bill payment screen |

**Files**: TRANSACT (write), ACCTDAT (read/update), CXACAIX.

### 5.7 Reporting (Online Trigger)

| Component | Type | Artifact |
|-----------|------|----------|
| CORPT00C | Online Program | Submit batch report job via TDQ |
| CORPT00 | BMS Map | Report parameter entry screen |

**Mechanism**: Uses CICS extra partition Transient Data Queue (TDQ) to submit batch report job.

### 5.8 User Administration

| Component | Type | Artifact |
|-----------|------|----------|
| COUSR00C | Online Program | List users |
| COUSR01C | Online Program | Add user |
| COUSR02C | Online Program | Update user |
| COUSR03C | Online Program | Delete user |
| COUSR00-03 | BMS Maps | User CRUD screens |

**Files**: USRSEC (VSAM KSDS). Admin-only functions accessed via COADM01C.

### 5.9 Batch Transaction Processing

| Component | Type | Artifact |
|-----------|------|----------|
| CBTRN01C | Batch Program | Post daily transactions (variant 1) |
| CBTRN02C | Batch Program | Post daily transactions (variant 2, with reject handling) |
| CBTRN03C | Batch Program | Print transaction detail report |
| POSTTRAN | JCL | Execute CBTRN02C |
| TRANREPT | JCL | Execute CBTRN03C |
| CVTRA06Y | Copybook | Daily transaction record layout |

**Flow**: DALYTRAN (daily input) -> CBTRN02C validates -> TRANSACT (posted) + DALYREJS (rejected).

### 5.10 Interest Calculation and Statement Generation

| Component | Type | Artifact |
|-----------|------|----------|
| CBACT04C | Batch Program | Monthly interest calculation |
| CBSTM03A | Batch Program | Statement generation (main) |
| CBSTM03B | Batch Program | Statement generation (I/O subroutine) |
| INTCALC | JCL | Execute CBACT04C |
| CREASTMT | JCL | Execute CBSTM03A/B |
| COSTM01 | Copybook | Statement record layout |
| CVTRA07Y | Copybook | Category balance layout |

### 5.11 Data Migration (Export/Import)

| Component | Type | Artifact |
|-----------|------|----------|
| CBEXPORT | Batch Program | Export 5 VSAM files to single multi-record file |
| CBIMPORT | Batch Program | Import multi-record file back to separate files |
| CBEXPORT | JCL | Execute CBEXPORT program |
| CBIMPORT | JCL | Execute CBIMPORT program |
| CVEXPORT | Copybook | 500-byte export record layout with REDEFINES |

### 5.12 Data Read/Print Utilities

| Component | Type | Artifact |
|-----------|------|----------|
| CBACT01C | Batch Program | Read/write account file |
| CBACT02C | Batch Program | Read/print card file |
| CBACT03C | Batch Program | Read/print cross-reference file |
| CBCUS01C | Batch Program | Read/print customer file |
| READACCT | JCL | Execute CBACT01C |
| READCARD | JCL | Execute CBACT02C |
| READCUST | JCL | Execute CBCUS01C |
| READXREF | JCL | Execute CBACT03C |

### 5.13 File Management and Job Scheduling

| Component | Type | Artifact |
|-----------|------|----------|
| COBSWAIT | Batch Program | Wait/synchronization utility |
| CLOSEFIL | JCL | Close CICS files for batch |
| OPENFIL | JCL | Open CICS files after batch |
| WAITSTEP | JCL | Job synchronization barrier |
| TRANBKP | JCL | Transaction file backup to GDG |
| DEFGDGB | JCL | Define GDG for backups |
| DEFGDGD | JCL | Define GDG for daily data |

### 5.14 Shared Infrastructure

| Component | Type | Artifact |
|-----------|------|----------|
| CSUTLDTC | Shared Program | Date validation via CEEDAYS API |
| CSDAT01Y | Copybook | Date working storage |
| CSSETATY | Copybook | Screen attribute utility |
| CSSTRPFY | Copybook | String processing |
| CSLKPCDY | Copybook | Lookup codes |
| CSUTLDPY | Copybook | Date utility parameters |
| CSUTLDWY | Copybook | Date utility working storage |
| CODATECN | Copybook | Date conversion |
| CSMSG01Y | Copybook | Common messages |
| CSMSG02Y | Copybook | Abend variables |
| COTTL01Y | Copybook | Screen titles |

---

## 6. Batch vs Online Classification Summary

### Online (CICS) Components: 17 programs

All programs in `app/cbl/` with names ending in `C` and containing `EXEC CICS` commands.

| Program | TRANID | Responsibility Group |
|---------|--------|---------------------|
| COSGN00C | CC00 | Authentication |
| COMEN01C | CM00 | Menu Navigation |
| COADM01C | CA00 | Menu Navigation (Admin) |
| COACTVWC | CAVW | Account Management |
| COACTUPC | CAUP | Account Management |
| COCRDLIC | CCLI | Card Management |
| COCRDSLC | CCDL | Card Management |
| COCRDUPC | CCUP | Card Management |
| COTRN00C | CT00 | Transaction Management |
| COTRN01C | CT01 | Transaction Management |
| COTRN02C | CT02 | Transaction Management |
| COBIL00C | CB00 | Bill Payment |
| CORPT00C | CR00 | Reporting |
| COUSR00C | CU00 | User Administration |
| COUSR01C | CU01 | User Administration |
| COUSR02C | CU02 | User Administration |
| COUSR03C | CU03 | User Administration |

**Evidence**: Each program contains `EXEC CICS SEND MAP`, `EXEC CICS RECEIVE MAP`, `EXEC CICS XCTL`, `EXEC CICS RETURN` commands and references EIBCALEN. TRANIDs are defined as `WS-TRANID` or `LIT-THISTRANID` in WORKING-STORAGE.

### Batch Components: 13 programs

All programs in `app/cbl/` using standard COBOL FILE-CONTROL with no CICS dependencies.

| Program | Responsibility Group |
|---------|---------------------|
| CBTRN01C | Transaction Processing |
| CBTRN02C | Transaction Processing |
| CBTRN03C | Transaction Reporting |
| CBACT01C | Data Utilities |
| CBACT02C | Data Utilities |
| CBACT03C | Data Utilities |
| CBACT04C | Interest Calculation |
| CBCUS01C | Data Utilities |
| CBSTM03A | Statement Generation |
| CBSTM03B | Statement Generation (Subroutine) |
| CBEXPORT | Data Migration |
| CBIMPORT | Data Migration |
| COBSWAIT | Job Synchronization |

**Evidence**: Each program header states `Type: BATCH COBOL Program`. No `EXEC CICS` commands found. Programs use standard COBOL `OPEN`, `READ`, `WRITE`, `CLOSE` file operations. File assignments use DD names matching JCL DD statements.

### Shared/Callable: 1 program

| Program | Type |
|---------|------|
| CSUTLDTC | Called subroutine (date validation). Uses LINKAGE SECTION, no CICS or batch file operations. Called via `CALL 'CSUTLDTC'` from COACTUPC. |

---

## 7. Scheduler Configurations

**Location**: `app/scheduler/`

### 7.1 Control-M Schedule (`CardDemo.controlm`)

XML-based schedule defining job folders with condition-based dependencies.

| Folder | Frequency | Job Chain | Description |
|--------|-----------|-----------|-------------|
| DAILY-TransactionBackup | Daily (ALL days) | CLOSEFIL -> TRANBKP -> WAITSTEP -> OPENFIL | Close files, backup transactions, synchronize, reopen files. |
| WEEKLY-TransactionTypesDBRefresh | Weekly | MNTTRDB2 | Refresh transaction types from DB2 (optional module). |
| WEEKLY-DisclosureGroupsRefresh | Weekly (Saturday) | CLOSEFIL -> DISCGRP -> WAITSTEP -> OPENFIL | Update disclosure groups with file close/open bracket. |
| MONTHLY-Interest-Statements | Monthly (1st) | Not found in code (inferred from INTCALC/CREASTMT jobs) | Interest calculation and statement generation. |

**Dependency Mechanism**: INCOND/OUTCOND XML attributes. Each job produces an OUTCOND that the next job requires as INCOND.

### 7.2 CA7 Schedule (`CardDemo.ca7`)

Legacy mainframe scheduler using completion-based trigger chains.

| Chain | Trigger Flow | Description |
|-------|-------------|-------------|
| Daily Processing | CLOSEFIL -> CBPAUP0J -> POSTTRAN -> WAITSTEP -> OPENFIL | Nightly batch: close files, purge authorizations, post transactions, sync, reopen. |
| Weekly Type Refresh | CLOSEFIL -> TRANTYPE -> WAITSTEP -> (CLOSEFIL1, CLOSEFIL2) | Refresh transaction types, then fork to parallel branches. |
| Branch 1 (SCHID=031) | CLOSEFIL1 -> TRANCATG -> WAITSTEP -> DISCGRP -> WAITSTEP -> OPENFIL | Category and disclosure group refresh. |
| Branch 2 (SCHID=032) | CLOSEFIL2 -> TRANEXTR -> WAITSTEP -> OPENFIL | Extract transaction types from DB2 to VSAM. |

**Note**: CBPAUP0J (purge expired authorizations) is referenced in CA7 but no corresponding JCL file found in `app/jcl/`. This is an optional IMS-DB2-MQ module job.

---

## 8. Data Files Inventory

### 8.1 VSAM KSDS (Key-Sequenced Data Sets)

Primary data storage. All online programs access these via CICS file control; batch programs use standard COBOL file I/O.

| # | Logical File | Dataset Name | Key | Record Length | Purpose |
|---|-------------|--------------|-----|---------------|---------|
| 1 | ACCTDAT | AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | ACCT-ID (11 digits) | 300 bytes | Account master file |
| 2 | CARDDAT | AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS | CARD-NUM (16 chars) | 150 bytes | Card master file |
| 3 | CUSTDAT | AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS | CUST-ID (9 digits) | 500 bytes | Customer master file |
| 4 | CCXREF / CARDXREF | AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS | XREF-CARD-NUM (16 chars) | 50 bytes | Card-to-account cross-reference |
| 5 | TRANSACT | AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | TRAN-ID (16 chars) | 350 bytes | Transaction master file |
| 6 | USRSEC | AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS | User ID | Variable | User security/credentials |

### 8.2 Alternate Index (AIX) Paths

| # | AIX Path | Base File | Alternate Key | Used By |
|---|----------|-----------|---------------|---------|
| 1 | CARDAIX | CARDDAT | Account ID | COCRDLIC, COCRDSLC, COCRDUPC |
| 2 | CXACAIX | CARDXREF | Account ID | COACTVWC, COACTUPC, COBIL00C, COTRN02C |

### 8.3 Physical Sequential (PS) Datasets

| # | Dataset | Purpose |
|---|---------|---------|
| 1 | AWS.M2.CARDDEMO.DALYTRAN.PS | Daily transaction input for batch posting |
| 2 | AWS.M2.CARDDEMO.EXPORT.DATA.PS | Branch migration export file (EBCDIC, 500-byte records) |
| 3 | AWS.M2.CARDDEMO.USRSEC.PS | User security seed data |

### 8.4 Generation Data Groups (GDG)

| # | GDG Base | Purpose |
|---|----------|---------|
| 1 | AWS.M2.CARDDEMO.TRANSACT.BKUP | Transaction backup history |
| 2 | AWS.M2.CARDDEMO.SYSTRAN | System transaction logs |
| 3 | AWS.M2.CARDDEMO.TRANREPT | Transaction reports |

### 8.5 Reference Data Files

| # | Logical File | Purpose | Used By |
|---|-------------|---------|---------|
| 1 | TRANTYPE | Transaction type codes | CBTRN02C, CBTRN03C |
| 2 | TRANCATG | Transaction category codes | CBTRN02C, CBTRN03C |
| 3 | TCATBAL | Transaction category balances | CBACT04C |
| 4 | DISCGRP | Disclosure group definitions | CBACT04C |

### 8.6 Sample Data Files

**Location**: `app/data/`

EBCDIC-encoded sample data files are provided in `app/data/EBCDIC/` for loading into VSAM clusters.

---

## Appendix A: Program Call Graph

```
COSGN00C (CC00 - Signon)
  |
  +-- [Admin] --> COADM01C (CA00 - Admin Menu)
  |                 |
  |                 +-- COUSR00C (CU00 - List Users)
  |                 |     +-- COUSR01C (CU01 - Add User)
  |                 |     +-- COUSR02C (CU02 - Update User)
  |                 |     +-- COUSR03C (CU03 - Delete User)
  |                 |
  |                 +-- [Same functions as regular menu]
  |
  +-- [Regular] --> COMEN01C (CM00 - Main Menu)
                      |
                      +-- COACTVWC (CAVW - View Account)
                      +-- COACTUPC (CAUP - Update Account)
                      |     +-- CSUTLDTC (Date Validation Subroutine)
                      +-- COCRDLIC (CCLI - List Cards)
                      |     +-- COCRDSLC (CCDL - Card Detail)
                      |     +-- COCRDUPC (CCUP - Update Card)
                      +-- COTRN00C (CT00 - List Transactions)
                      |     +-- COTRN01C (CT01 - View Transaction)
                      +-- COTRN02C (CT02 - Add Transaction)
                      +-- COBIL00C (CB00 - Bill Payment)
                      +-- CORPT00C (CR00 - Submit Report)
```

## Appendix B: Batch Job Dependency Chain (Daily)

```
CLOSEFIL.jcl  (Close CICS files)
    |
    v
TRANBKP.jcl   (Backup transactions to GDG)
    |
    v
POSTTRAN.jcl   (CBTRN02C: Post daily transactions)
    |
    v
WAITSTEP.jcl   (COBSWAIT: Synchronization barrier)
    |
    v
OPENFIL.jcl    (Reopen CICS files)
```

## Appendix C: Naming Conventions Observed in Code

| Pattern | Convention | Example |
|---------|-----------|---------|
| Online programs | `CO` prefix + function code + `C` suffix | COSGN00C, COMEN01C |
| Batch programs | `CB` prefix + function code + `C` suffix | CBTRN02C, CBACT04C |
| Batch subroutines | `CB` prefix + function code + letter suffix | CBSTM03B |
| Utility programs | `CS` prefix | CSUTLDTC |
| Copybooks (data) | `CV` prefix + entity + sequence + `Y` suffix | CVACT01Y, CVCUS01Y |
| Copybooks (common) | `CS` prefix + function + `Y` suffix | CSDAT01Y, CSMSG01Y |
| Copybooks (screen) | `CO` prefix + screen code + `Y` suffix | COCOM01Y, COMEN02Y |
| BMS maps | Match program name minus trailing `C` | COSGN00 (for COSGN00C) |
| JCL jobs | Uppercase function name | POSTTRAN, INTCALC |
| TRANIDs | `C` + 1 letter + 2 digits | CC00, CM00, CT02 |
