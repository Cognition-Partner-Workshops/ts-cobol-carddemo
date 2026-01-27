# CardDemo Application - Functional Requirements Document

**Document Version:** 1.0  
**Date:** January 22, 2026  
**Application:** CardDemo - Mainframe Credit Card Management System  
**Repository:** Cognition-Partner-Workshops/aws-mainframe-modernization-carddemo

---

## 1. Executive Summary

CardDemo is a comprehensive mainframe application that simulates a credit card management system. Built primarily using COBOL with CICS for online transaction processing and JCL for batch operations, the application provides functionality for managing customer accounts, credit cards, transactions, bill payments, and administrative functions. The system supports two user roles (Regular Users and Administrators) with role-based access control.

This document outlines the functional requirements derived from analysis of the application's COBOL source code, copybooks, BMS maps, and supporting documentation.

---

## 2. System Overview

### 2.1 Technology Stack

The CardDemo application utilizes the following core technologies:

**Primary Technologies:**
- COBOL: Primary programming language for business logic
- CICS: Online transaction processing and screen management
- VSAM (KSDS with AIX): Primary data storage mechanism
- JCL: Batch job processing and scheduling
- BMS: Basic Mapping Support for screen definitions
- Assembler: System-level utilities (MVSWAIT, COBDATFT)

**Optional Technologies (Extended Modules):**
- DB2: Relational database for transaction type management
- IMS DB: Hierarchical database for authorization processing
- MQ: Message queuing for asynchronous operations

### 2.2 User Roles

The system supports two distinct user roles:

1. **Regular Users (User Type: 'U')**: Can perform standard card management functions including viewing accounts, managing cards, processing transactions, making bill payments, and generating reports.

2. **Administrator Users (User Type: 'A')**: Can perform all regular user functions plus administrative functions including user management and transaction type management.

---

## 3. Functional Requirements - Online Components

### 3.1 Authentication Module

#### FR-AUTH-001: User Sign-On
**Program:** COSGN00C  
**Transaction:** CC00  
**Screen:** COSGN00

**Description:** The system shall provide a secure sign-on mechanism for user authentication.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-AUTH-001.1 | The system shall display a sign-on screen requesting User ID and Password |
| FR-AUTH-001.2 | The system shall validate that User ID is not empty before processing |
| FR-AUTH-001.3 | The system shall validate that Password is not empty before processing |
| FR-AUTH-001.4 | The system shall convert User ID and Password to uppercase for validation |
| FR-AUTH-001.5 | The system shall authenticate users against the USRSEC VSAM file |
| FR-AUTH-001.6 | The system shall display "User not found" message for invalid User IDs |
| FR-AUTH-001.7 | The system shall display "Wrong Password" message for incorrect passwords |
| FR-AUTH-001.8 | Upon successful authentication, the system shall route Admin users to the Admin Menu (COADM01C) |
| FR-AUTH-001.9 | Upon successful authentication, the system shall route Regular users to the Main Menu (COMEN01C) |
| FR-AUTH-001.10 | The system shall display current date and time on the sign-on screen |
| FR-AUTH-001.11 | PF3 shall exit the application with a "Thank You" message |

**Input Fields:**
- User ID (8 characters, alphanumeric)
- Password (8 characters, alphanumeric)

**Validation Rules:**
- User ID must exist in USRSEC file
- Password must match stored password for the User ID

---

### 3.2 Main Menu Module (Regular Users)

#### FR-MENU-001: Main Menu Navigation
**Program:** COMEN01C  
**Transaction:** CM00  
**Screen:** COMEN01

**Description:** The system shall provide a main menu for regular users to access application functions.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-MENU-001.1 | The system shall display up to 12 menu options for regular users |
| FR-MENU-001.2 | The system shall validate that entered option number is numeric |
| FR-MENU-001.3 | The system shall validate that option number is within valid range |
| FR-MENU-001.4 | The system shall restrict Admin-only options from regular users |
| FR-MENU-001.5 | The system shall display "No access - Admin Only option" for restricted options |
| FR-MENU-001.6 | The system shall navigate to the selected program upon valid option entry |
| FR-MENU-001.7 | PF3 shall return to the Sign-On screen |
| FR-MENU-001.8 | The system shall display "This option is coming soon" for unimplemented features |

**Menu Options (Regular Users):**
1. Account View
2. Account Update
3. Credit Card List
4. Credit Card View
5. Credit Card Update
6. Transaction List
7. Transaction View
8. Transaction Add
9. Transaction Reports
10. Bill Payment
11. Pending Authorizations (Optional Module)

---

### 3.3 Admin Menu Module

#### FR-ADMIN-001: Admin Menu Navigation
**Program:** COADM01C  
**Transaction:** CA00  
**Screen:** COADM01

**Description:** The system shall provide an administrative menu for admin users to access management functions.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-ADMIN-001.1 | The system shall display administrative menu options |
| FR-ADMIN-001.2 | The system shall validate option number input |
| FR-ADMIN-001.3 | The system shall navigate to selected administrative function |
| FR-ADMIN-001.4 | PF3 shall return to the Sign-On screen |
| FR-ADMIN-001.5 | The system shall handle missing programs gracefully with "not installed" message |

**Admin Menu Options:**
1. User List
2. User Add
3. User Update
4. User Delete
5. Transaction Type List (DB2 Module)
6. Transaction Type Add/Edit (DB2 Module)

---

### 3.4 Account Management Module

#### FR-ACCT-001: Account View
**Program:** COACTVWC  
**Transaction:** CAVW  
**Screen:** COACTVW

**Description:** The system shall allow users to view account details.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-ACCT-001.1 | The system shall accept an Account ID as search criteria |
| FR-ACCT-001.2 | The system shall validate that Account ID is an 11-digit numeric value |
| FR-ACCT-001.3 | The system shall display error if Account ID is all zeros |
| FR-ACCT-001.4 | The system shall retrieve account data from ACCTDAT file |
| FR-ACCT-001.5 | The system shall retrieve customer data from CUSTDAT file |
| FR-ACCT-001.6 | The system shall display account and customer information |
| FR-ACCT-001.7 | PF3 shall return to the calling program or Main Menu |

**Display Fields:**
- Account ID
- Customer ID
- Customer Name
- Account Status (Active/Inactive)
- Credit Limit
- Cash Credit Limit
- Current Balance
- Current Cycle Credit
- Current Cycle Debit
- Account Open Date
- Expiration Date
- Reissue Date
- FICO Score
- Account Group ID

---

#### FR-ACCT-002: Account Update
**Program:** COACTUPC  
**Transaction:** CAUP  
**Screen:** COACTUP

**Description:** The system shall allow users to update account information.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-ACCT-002.1 | The system shall display current account information for editing |
| FR-ACCT-002.2 | The system shall validate all input fields before update |
| FR-ACCT-002.3 | The system shall validate Account Status as 'Y' or 'N' |
| FR-ACCT-002.4 | The system shall validate Credit Limit as a valid signed number |
| FR-ACCT-002.5 | The system shall validate Cash Credit Limit as a valid signed number |
| FR-ACCT-002.6 | The system shall validate Current Balance as a valid signed number |
| FR-ACCT-002.7 | The system shall validate date fields (Open Date, Expiry Date, Reissue Date) |
| FR-ACCT-002.8 | The system shall validate FICO Score as numeric |
| FR-ACCT-002.9 | The system shall validate US Phone Number format (###)###-#### |
| FR-ACCT-002.10 | The system shall validate US SSN format (###-##-####) |
| FR-ACCT-002.11 | The system shall detect if any changes were made before updating |
| FR-ACCT-002.12 | The system shall update the ACCTDAT file with validated changes |
| FR-ACCT-002.13 | PF3 shall return without saving changes |

**Editable Fields:**
- Account Status
- Credit Limit
- Cash Credit Limit
- Current Balance
- Current Cycle Credit
- Current Cycle Debit
- Open Date
- Expiration Date
- Reissue Date
- FICO Score
- Customer Information (Name, Address, Phone, SSN, DOB)

**Validation Rules:**
- SSN Part 1: Cannot be 0, 666, or 900-999
- Phone Number: Must be in (###)###-#### format
- Dates: Must be valid calendar dates in CCYYMMDD format
- FICO Score: Must be numeric

---

### 3.5 Credit Card Management Module

#### FR-CARD-001: Credit Card List
**Program:** COCRDLIC  
**Transaction:** CCLI  
**Screen:** COCRDLI

**Description:** The system shall display a list of credit cards.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-CARD-001.1 | Admin users shall see all credit cards in the system |
| FR-CARD-001.2 | Regular users shall see only cards associated with their account |
| FR-CARD-001.3 | The system shall display 7 cards per page |
| FR-CARD-001.4 | The system shall support pagination (PF7=Page Up, PF8=Page Down) |
| FR-CARD-001.5 | Users can select a card with 'S' to view details |
| FR-CARD-001.6 | Users can select a card with 'U' to update |
| FR-CARD-001.7 | The system shall display "Please select only one record" if multiple selections |
| FR-CARD-001.8 | PF3 shall return to the Main Menu |

**Display Fields per Card:**
- Account Number
- Card Number (16 digits)
- Card Status

**Selection Codes:**
- S: View card details
- U: Update card

---

#### FR-CARD-002: Credit Card View
**Program:** COCRDSLC  
**Transaction:** CCDL  
**Screen:** COCRDSL

**Description:** The system shall display detailed credit card information.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-CARD-002.1 | The system shall display all card details for the selected card |
| FR-CARD-002.2 | The system shall retrieve card data from CARDDAT file |
| FR-CARD-002.3 | PF3 shall return to the Credit Card List |

**Display Fields:**
- Card Number
- Account ID
- Customer ID
- Card Status
- CVV Code
- Expiration Date
- Card Embossed Name
- Card Active Date

---

#### FR-CARD-003: Credit Card Update
**Program:** COCRDUPC  
**Transaction:** CCUP  
**Screen:** COCRDUP

**Description:** The system shall allow users to update credit card information.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-CARD-003.1 | The system shall display current card information for editing |
| FR-CARD-003.2 | The system shall validate all input fields |
| FR-CARD-003.3 | The system shall validate CVV as 3-digit numeric |
| FR-CARD-003.4 | The system shall validate expiration date format |
| FR-CARD-003.5 | The system shall update CARDDAT file with changes |
| FR-CARD-003.6 | PF3 shall return without saving |

---

### 3.6 Transaction Management Module

#### FR-TRAN-001: Transaction List
**Program:** COTRN00C  
**Transaction:** CT00  
**Screen:** COTRN00

**Description:** The system shall display a list of transactions.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-TRAN-001.1 | The system shall display transactions from TRANSACT file |
| FR-TRAN-001.2 | The system shall display 10 transactions per page |
| FR-TRAN-001.3 | The system shall support filtering by Transaction ID |
| FR-TRAN-001.4 | Transaction ID filter must be numeric |
| FR-TRAN-001.5 | PF7 shall page backward through transactions |
| FR-TRAN-001.6 | PF8 shall page forward through transactions |
| FR-TRAN-001.7 | Users can select 'S' to view transaction details |
| FR-TRAN-001.8 | PF3 shall return to Main Menu |

**Display Fields per Transaction:**
- Transaction ID
- Transaction Date
- Transaction Amount

---

#### FR-TRAN-002: Transaction View
**Program:** COTRN01C  
**Transaction:** CT01  
**Screen:** COTRN01

**Description:** The system shall display detailed transaction information.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-TRAN-002.1 | The system shall display all details for selected transaction |
| FR-TRAN-002.2 | PF3 shall return to Transaction List |

**Display Fields:**
- Transaction ID
- Transaction Type Code
- Transaction Category Code
- Transaction Source
- Transaction Description
- Transaction Amount
- Card Number
- Merchant ID
- Merchant Name
- Merchant City
- Merchant ZIP
- Original Timestamp
- Processing Timestamp

---

#### FR-TRAN-003: Transaction Add
**Program:** COTRN02C  
**Transaction:** CT02  
**Screen:** COTRN02

**Description:** The system shall allow users to add new transactions.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-TRAN-003.1 | The system shall generate a unique Transaction ID |
| FR-TRAN-003.2 | The system shall validate all required fields |
| FR-TRAN-003.3 | The system shall validate Transaction Type Code |
| FR-TRAN-003.4 | The system shall validate Transaction Category Code |
| FR-TRAN-003.5 | The system shall validate Transaction Amount as numeric |
| FR-TRAN-003.6 | The system shall validate Card Number exists |
| FR-TRAN-003.7 | The system shall write new transaction to TRANSACT file |
| FR-TRAN-003.8 | The system shall set timestamps automatically |
| FR-TRAN-003.9 | PF3 shall cancel and return |

**Input Fields:**
- Transaction Type Code
- Transaction Category Code
- Transaction Source
- Transaction Description
- Transaction Amount
- Card Number
- Merchant Information

---

### 3.7 Bill Payment Module

#### FR-BILL-001: Bill Payment Processing
**Program:** COBIL00C  
**Transaction:** CB00  
**Screen:** COBIL00

**Description:** The system shall allow users to pay their account balance in full.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-BILL-001.1 | The system shall accept Account ID for payment |
| FR-BILL-001.2 | The system shall validate Account ID is not empty |
| FR-BILL-001.3 | The system shall retrieve current balance from ACCTDAT file |
| FR-BILL-001.4 | The system shall display current balance to user |
| FR-BILL-001.5 | The system shall display "You have nothing to pay" if balance is zero or negative |
| FR-BILL-001.6 | The system shall require confirmation (Y/N) before processing payment |
| FR-BILL-001.7 | The system shall validate confirmation input as 'Y' or 'N' |
| FR-BILL-001.8 | Upon confirmation, the system shall create a bill payment transaction |
| FR-BILL-001.9 | The transaction type shall be '02' (Bill Payment) |
| FR-BILL-001.10 | The transaction category shall be 2 |
| FR-BILL-001.11 | The transaction source shall be 'POS TERM' |
| FR-BILL-001.12 | The transaction description shall be 'BILL PAYMENT - ONLINE' |
| FR-BILL-001.13 | The system shall update account balance to zero after payment |
| FR-BILL-001.14 | PF3 shall return to previous screen |
| FR-BILL-001.15 | PF4 shall clear the current screen |

**Transaction Record Created:**
- Transaction ID: Auto-generated (previous max + 1)
- Transaction Type: 02
- Transaction Category: 2
- Source: POS TERM
- Description: BILL PAYMENT - ONLINE
- Amount: Current Balance
- Merchant ID: 999999999
- Merchant Name: BILL PAYMENT
- Merchant City: N/A
- Merchant ZIP: N/A

---

### 3.8 Transaction Reports Module

#### FR-REPT-001: Transaction Report Generation
**Program:** CORPT00C  
**Transaction:** CR00  
**Screen:** CORPT00

**Description:** The system shall allow users to generate transaction reports.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-REPT-001.1 | The system shall support three report types: Monthly, Yearly, Custom |
| FR-REPT-001.2 | Monthly report shall use current month date range |
| FR-REPT-001.3 | Yearly report shall use current year date range (Jan 1 - Dec 31) |
| FR-REPT-001.4 | Custom report shall accept user-defined start and end dates |
| FR-REPT-001.5 | The system shall validate custom date inputs |
| FR-REPT-001.6 | Start Date Month must be 01-12 |
| FR-REPT-001.7 | Start Date Day must be 01-31 |
| FR-REPT-001.8 | Start Date Year must be numeric |
| FR-REPT-001.9 | End Date Month must be 01-12 |
| FR-REPT-001.10 | End Date Day must be 01-31 |
| FR-REPT-001.11 | End Date Year must be numeric |
| FR-REPT-001.12 | The system shall submit batch job to internal reader for report generation |
| FR-REPT-001.13 | PF3 shall return to Main Menu |

**Report Selection Options:**
- Monthly: Select to generate current month report
- Yearly: Select to generate current year report
- Custom: Enter start date (MM/DD/YYYY) and end date (MM/DD/YYYY)

---

### 3.9 User Management Module (Admin Only)

#### FR-USER-001: User List
**Program:** COUSR00C  
**Transaction:** CU00  
**Screen:** COUSR00

**Description:** The system shall allow administrators to view all system users.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-USER-001.1 | The system shall display users from USRSEC file |
| FR-USER-001.2 | The system shall display 10 users per page |
| FR-USER-001.3 | The system shall support pagination (PF7/PF8) |
| FR-USER-001.4 | Users can filter by User ID |
| FR-USER-001.5 | Selection 'U' shall navigate to User Update |
| FR-USER-001.6 | Selection 'D' shall navigate to User Delete |
| FR-USER-001.7 | PF3 shall return to Admin Menu |

**Display Fields:**
- User ID
- First Name
- Last Name
- User Type

---

#### FR-USER-002: User Add
**Program:** COUSR01C  
**Transaction:** CU01  
**Screen:** COUSR01

**Description:** The system shall allow administrators to add new users.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-USER-002.1 | The system shall accept new user information |
| FR-USER-002.2 | User ID must be unique |
| FR-USER-002.3 | User ID must not be empty |
| FR-USER-002.4 | Password must not be empty |
| FR-USER-002.5 | First Name must not be empty |
| FR-USER-002.6 | Last Name must not be empty |
| FR-USER-002.7 | User Type must be 'A' (Admin) or 'U' (User) |
| FR-USER-002.8 | The system shall write new user to USRSEC file |
| FR-USER-002.9 | PF3 shall cancel and return |

**Input Fields:**
- User ID (8 characters)
- Password (8 characters)
- First Name (20 characters)
- Last Name (20 characters)
- User Type (A/U)

---

#### FR-USER-003: User Update
**Program:** COUSR02C  
**Transaction:** CU02  
**Screen:** COUSR02

**Description:** The system shall allow administrators to update existing users.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-USER-003.1 | The system shall display current user information |
| FR-USER-003.2 | User ID shall not be editable |
| FR-USER-003.3 | The system shall validate all editable fields |
| FR-USER-003.4 | The system shall update USRSEC file with changes |
| FR-USER-003.5 | PF3 shall cancel and return |

---

#### FR-USER-004: User Delete
**Program:** COUSR03C  
**Transaction:** CU03  
**Screen:** COUSR03

**Description:** The system shall allow administrators to delete users.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-USER-004.1 | The system shall display user information for confirmation |
| FR-USER-004.2 | The system shall require confirmation before deletion |
| FR-USER-004.3 | The system shall delete user from USRSEC file upon confirmation |
| FR-USER-004.4 | PF3 shall cancel and return |

---

## 4. Functional Requirements - Batch Components

### 4.1 Transaction Posting

#### FR-BATCH-001: Daily Transaction Posting
**Program:** CBTRN02C  
**Job:** POSTTRAN

**Description:** The system shall process and post daily transactions from the daily transaction file.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-BATCH-001.1 | The system shall read transactions from DALYTRAN file |
| FR-BATCH-001.2 | The system shall validate each transaction |
| FR-BATCH-001.3 | The system shall lookup card in XREF file to validate card number |
| FR-BATCH-001.4 | The system shall lookup account in ACCTDAT file |
| FR-BATCH-001.5 | Valid transactions shall be written to TRANSACT file |
| FR-BATCH-001.6 | Invalid transactions shall be written to DALYREJS (reject) file |
| FR-BATCH-001.7 | The system shall update account balances in ACCTDAT |
| FR-BATCH-001.8 | The system shall update transaction category balances in TCATBALF |
| FR-BATCH-001.9 | The system shall display count of processed and rejected transactions |
| FR-BATCH-001.10 | Return code 4 shall be set if any transactions were rejected |

**Input Files:**
- DALYTRAN: Daily transaction input file
- XREFFILE: Card cross-reference file
- ACCTFILE: Account master file

**Output Files:**
- TRANFILE: Transaction master file
- DALYREJS: Rejected transactions file
- TCATBALF: Transaction category balance file

---

### 4.2 Interest Calculation

#### FR-BATCH-002: Interest Calculation
**Program:** CBACT04C  
**Job:** INTCALC

**Description:** The system shall calculate interest charges based on transaction categories and disclosure groups.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-BATCH-002.1 | The system shall read transaction category balances from TCATBALF |
| FR-BATCH-002.2 | The system shall retrieve account information from ACCTDAT |
| FR-BATCH-002.3 | The system shall retrieve card cross-reference from XREFFILE |
| FR-BATCH-002.4 | The system shall lookup interest rates from DISCGRP file |
| FR-BATCH-002.5 | The system shall compute interest based on category balance and rate |
| FR-BATCH-002.6 | The system shall compute applicable fees |
| FR-BATCH-002.7 | The system shall update account current balance with interest |
| FR-BATCH-002.8 | The system shall reset current cycle credit/debit to zero |
| FR-BATCH-002.9 | The system shall write interest transactions to TRANSACT file |

**Interest Calculation:**
- Monthly Interest = (Category Balance * Interest Rate) / 12
- Total Interest = Sum of all category interests for account

---

### 4.3 Statement Generation

#### FR-BATCH-003: Account Statement Generation
**Program:** CBSTM03A  
**Job:** CREASTMT

**Description:** The system shall generate account statements in plain text and HTML formats.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-BATCH-003.1 | The system shall read card cross-reference data |
| FR-BATCH-003.2 | The system shall retrieve customer information |
| FR-BATCH-003.3 | The system shall retrieve account information |
| FR-BATCH-003.4 | The system shall retrieve transaction history |
| FR-BATCH-003.5 | The system shall generate plain text statement file |
| FR-BATCH-003.6 | The system shall generate HTML statement file |
| FR-BATCH-003.7 | Statement shall include customer name and address |
| FR-BATCH-003.8 | Statement shall include account ID and current balance |
| FR-BATCH-003.9 | Statement shall include FICO score |
| FR-BATCH-003.10 | Statement shall include transaction summary with totals |

**Statement Contents:**
- Header: Bank name and address
- Customer: Name and mailing address
- Account Details: Account ID, Current Balance, FICO Score
- Transaction Summary: Transaction ID, Details, Amount
- Total Expenses

**Output Formats:**
- Plain Text (80 characters per line)
- HTML (formatted table layout)

---

### 4.4 Transaction Reports

#### FR-BATCH-004: Transaction Report Generation
**Program:** CBTRN03C  
**Job:** TRANREPT

**Description:** The system shall generate transaction reports based on date range parameters.

**Functional Requirements:**

| Req ID | Requirement Description |
|--------|------------------------|
| FR-BATCH-004.1 | The system shall accept start date and end date parameters |
| FR-BATCH-004.2 | The system shall filter transactions within date range |
| FR-BATCH-004.3 | The system shall generate formatted report output |
| FR-BATCH-004.4 | Report shall be sorted by card number and transaction date |

---

## 5. Data Requirements

### 5.1 Data Files

| File Name | Type | Description | Key Field |
|-----------|------|-------------|-----------|
| USRSEC | VSAM KSDS | User security/authentication data | User ID |
| ACCTDAT | VSAM KSDS | Account master data | Account ID |
| CARDDAT | VSAM KSDS | Credit card data | Card Number |
| CUSTDAT | VSAM KSDS | Customer master data | Customer ID |
| CARDXREF | VSAM KSDS | Card-Account-Customer cross-reference | Card Number |
| CXACAIX | VSAM AIX | Alternate index on CARDXREF by Account ID | Account ID |
| TRANSACT | VSAM KSDS | Transaction master file | Transaction ID |
| DALYTRAN | Sequential | Daily transaction input file | N/A |
| TCATBALF | VSAM KSDS | Transaction category balance | Account ID + Type + Category |
| DISCGRP | VSAM KSDS | Disclosure groups (interest rates) | Group ID + Type + Category |
| TRANCATG | VSAM KSDS | Transaction category types | Category Code |
| TRANTYPE | VSAM KSDS | Transaction types | Type Code |

### 5.2 Record Layouts

#### User Security Record (CSUSR01Y)
| Field | Type | Length | Description |
|-------|------|--------|-------------|
| SEC-USR-ID | X | 8 | User ID |
| SEC-USR-FNAME | X | 20 | First Name |
| SEC-USR-LNAME | X | 20 | Last Name |
| SEC-USR-PWD | X | 8 | Password |
| SEC-USR-TYPE | X | 1 | User Type (A/U) |
| SEC-USR-FILLER | X | 23 | Reserved |

#### Account Record (CVACT01Y)
| Field | Type | Length | Description |
|-------|------|--------|-------------|
| ACCT-ID | 9 | 11 | Account ID |
| ACCT-ACTIVE-STATUS | X | 1 | Active Status (Y/N) |
| ACCT-CURR-BAL | S9V99 | 12 | Current Balance |
| ACCT-CREDIT-LIMIT | S9V99 | 12 | Credit Limit |
| ACCT-CASH-CREDIT-LIMIT | S9V99 | 12 | Cash Credit Limit |
| ACCT-OPEN-DATE | X | 10 | Account Open Date |
| ACCT-EXPIRAION-DATE | X | 10 | Expiration Date |
| ACCT-REISSUE-DATE | X | 10 | Reissue Date |
| ACCT-CURR-CYC-CREDIT | S9V99 | 12 | Current Cycle Credit |
| ACCT-CURR-CYC-DEBIT | S9V99 | 12 | Current Cycle Debit |
| ACCT-GROUP-ID | X | 10 | Account Group ID |
| ACCT-FICO-CREDIT-SCORE | 9 | 3 | FICO Score |

#### Card Record (CVACT02Y)
| Field | Type | Length | Description |
|-------|------|--------|-------------|
| CARD-NUM | X | 16 | Card Number |
| CARD-ACCT-ID | 9 | 11 | Account ID |
| CARD-CVV-CD | 9 | 3 | CVV Code |
| CARD-EMBOSSED-NAME | X | 50 | Embossed Name |
| CARD-EXPIRAION-DATE | X | 10 | Expiration Date |
| CARD-ACTIVE-STATUS | X | 1 | Active Status |

#### Transaction Record (CVTRA05Y)
| Field | Type | Length | Description |
|-------|------|--------|-------------|
| TRAN-ID | X | 16 | Transaction ID |
| TRAN-TYPE-CD | X | 2 | Transaction Type Code |
| TRAN-CAT-CD | 9 | 4 | Transaction Category Code |
| TRAN-SOURCE | X | 10 | Transaction Source |
| TRAN-DESC | X | 100 | Transaction Description |
| TRAN-AMT | S9V99 | 12 | Transaction Amount |
| TRAN-CARD-NUM | X | 16 | Card Number |
| TRAN-MERCHANT-ID | 9 | 9 | Merchant ID |
| TRAN-MERCHANT-NAME | X | 50 | Merchant Name |
| TRAN-MERCHANT-CITY | X | 50 | Merchant City |
| TRAN-MERCHANT-ZIP | X | 10 | Merchant ZIP |
| TRAN-ORIG-TS | X | 26 | Original Timestamp |
| TRAN-PROC-TS | X | 26 | Processing Timestamp |

---

## 6. Optional Module Requirements

### 6.1 Credit Card Authorizations (IMS-DB2-MQ)

#### FR-OPT-001: Pending Authorization Summary
**Program:** COPAUS0C  
**Transaction:** CPVS

**Description:** Display summary of pending credit card authorizations.

#### FR-OPT-002: Pending Authorization Details
**Program:** COPAUS1C  
**Transaction:** CPVD

**Description:** Display and process pending authorization details.

#### FR-OPT-003: Authorization Request Processing
**Program:** COPAUA0C  
**Transaction:** CP00

**Description:** Process authorization requests via MQ trigger.

#### FR-OPT-004: Purge Expired Authorizations
**Program:** CBPAUP0C  
**Job:** CBPAUP0J

**Description:** Batch purge of expired authorization records.

### 6.2 Transaction Type Management (DB2)

#### FR-OPT-005: Transaction Type List
**Program:** COTRTLIC  
**Transaction:** CTLI

**Description:** List, update, and delete transaction types from DB2.

#### FR-OPT-006: Transaction Type Add/Edit
**Program:** COTRTUPC  
**Transaction:** CTTU

**Description:** Add or edit transaction types in DB2.

### 6.3 MQ Integration

#### FR-OPT-007: System Date Inquiry via MQ
**Program:** CODATE01  
**Transaction:** CDRD

**Description:** Inquire system date through MQ request/response.

#### FR-OPT-008: Account Details Inquiry via MQ
**Program:** COACCT01  
**Transaction:** CDRA

**Description:** Inquire account details through MQ request/response.

---

## 7. Screen Navigation Flow

### 7.1 Regular User Flow

```
Sign-On (CC00)
    |
    v
Main Menu (CM00)
    |
    +-- Account View (CAVW)
    |
    +-- Account Update (CAUP)
    |
    +-- Credit Card List (CCLI)
    |       |
    |       +-- Credit Card View (CCDL)
    |       |
    |       +-- Credit Card Update (CCUP)
    |
    +-- Transaction List (CT00)
    |       |
    |       +-- Transaction View (CT01)
    |
    +-- Transaction Add (CT02)
    |
    +-- Transaction Reports (CR00)
    |
    +-- Bill Payment (CB00)
    |
    +-- Pending Authorizations (CPVS) [Optional]
            |
            +-- Authorization Details (CPVD)
```

### 7.2 Admin User Flow

```
Sign-On (CC00)
    |
    v
Admin Menu (CA00)
    |
    +-- User List (CU00)
    |       |
    |       +-- User Update (CU02)
    |       |
    |       +-- User Delete (CU03)
    |
    +-- User Add (CU01)
    |
    +-- Transaction Type List (CTLI) [Optional - DB2]
    |
    +-- Transaction Type Add/Edit (CTTU) [Optional - DB2]
```

---

## 8. Keyboard Function Keys

| Key | Function |
|-----|----------|
| ENTER | Submit/Process current screen |
| PF3 | Exit/Return to previous screen |
| PF4 | Clear screen (where applicable) |
| PF7 | Page Up (previous page) |
| PF8 | Page Down (next page) |

---

## 9. Error Handling Requirements

### 9.1 Input Validation Errors

| Error Condition | Message |
|-----------------|---------|
| Empty User ID | "Please enter User ID ..." |
| Empty Password | "Please enter Password ..." |
| User not found | "User not found. Try again ..." |
| Wrong password | "Wrong Password. Try again ..." |
| Invalid option | "Please enter a valid option number..." |
| Admin-only access | "No access - Admin Only option..." |
| Invalid key pressed | "Invalid key pressed..." |
| Account not found | "Account ID NOT found..." |
| Nothing to pay | "You have nothing to pay..." |
| Invalid confirmation | "Invalid value. Valid values are (Y/N)..." |

### 9.2 File Operation Errors

| Error Condition | Response Code | Action |
|-----------------|---------------|--------|
| Record not found | 13 | Display appropriate message |
| File not open | Various | Abend program |
| I/O error | Various | Display error and abend |

---

## 10. Security Requirements

| Req ID | Requirement Description |
|--------|------------------------|
| FR-SEC-001 | All users must authenticate before accessing the system |
| FR-SEC-002 | Passwords shall be stored in the USRSEC file |
| FR-SEC-003 | User type shall determine accessible functions |
| FR-SEC-004 | Admin functions shall only be accessible to Admin users |
| FR-SEC-005 | Session context shall be maintained via COMMAREA |
| FR-SEC-006 | PF3 from Sign-On shall terminate the session |

---

## 11. Performance Requirements

| Req ID | Requirement Description |
|--------|------------------------|
| FR-PERF-001 | Online transactions shall complete within 3 seconds |
| FR-PERF-002 | List screens shall display within 2 seconds |
| FR-PERF-003 | Batch jobs shall process minimum 1000 transactions per minute |
| FR-PERF-004 | Statement generation shall complete within batch window |

---

## 12. Appendix

### 12.1 Transaction Codes

| Code | Program | Description |
|------|---------|-------------|
| CC00 | COSGN00C | Sign-On |
| CM00 | COMEN01C | Main Menu |
| CA00 | COADM01C | Admin Menu |
| CAVW | COACTVWC | Account View |
| CAUP | COACTUPC | Account Update |
| CCLI | COCRDLIC | Credit Card List |
| CCDL | COCRDSLC | Credit Card View |
| CCUP | COCRDUPC | Credit Card Update |
| CT00 | COTRN00C | Transaction List |
| CT01 | COTRN01C | Transaction View |
| CT02 | COTRN02C | Transaction Add |
| CR00 | CORPT00C | Transaction Reports |
| CB00 | COBIL00C | Bill Payment |
| CU00 | COUSR00C | User List |
| CU01 | COUSR01C | User Add |
| CU02 | COUSR02C | User Update |
| CU03 | COUSR03C | User Delete |

### 12.2 Batch Job Summary

| Job Name | Program | Description |
|----------|---------|-------------|
| POSTTRAN | CBTRN02C | Post daily transactions |
| INTCALC | CBACT04C | Calculate interest |
| CREASTMT | CBSTM03A | Generate statements |
| TRANREPT | CBTRN03C | Generate transaction reports |
| COMBTRAN | SORT | Combine transaction files |
| TRANBKP | IDCAMS | Backup transactions |

### 12.3 Default User Credentials

| User ID | Password | Type | Description |
|---------|----------|------|-------------|
| ADMIN001 | PASSWORD | Admin | Default administrator |
| USER0001 | PASSWORD | User | Default regular user |

---

**Document End**

*This functional requirements document was generated through analysis of the CardDemo COBOL source code, copybooks, BMS maps, JCL procedures, and README documentation.*
