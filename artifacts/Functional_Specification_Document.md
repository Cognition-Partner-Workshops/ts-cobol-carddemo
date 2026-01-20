# CardDemo Application - Functional Specification Document

## Document Information

| Attribute | Value |
|-----------|-------|
| Document Title | CardDemo Functional Specification |
| Version | 1.0 |
| Date | January 2026 |
| Purpose | Reverse Engineering Documentation for Modernization |

---

## 1. Executive Summary

CardDemo is a mainframe-based credit card management system designed as a demonstration environment for AWS and partner technologies in mainframe migration and modernization scenarios. The application simulates a production-grade card processing system used in the financial services industry, providing comprehensive functionality for credit card account management, transaction processing, bill payments, and administrative operations.

This document captures the complete functional specification of the CardDemo application through reverse engineering of the COBOL source code, copybooks, BMS maps, JCL jobs, and scheduler configurations. The goal is to provide a comprehensive reference for potential modernization using a forward engineering approach.

---

## 2. Business Overview

### 2.1 Application Purpose

CardDemo serves as a reference implementation and testing ground for:

- Application discovery and analysis tools
- Migration assessment methodologies
- Modernization strategy development
- Performance testing and benchmarking
- System augmentation approaches
- Service enablement patterns
- Test automation frameworks

### 2.2 Business Domain

The application operates within the **Credit Card Management** domain, encompassing:

- Customer account management
- Credit card lifecycle management
- Transaction processing and posting
- Bill payment processing
- Interest calculation and fee assessment
- Statement generation
- User security and access control
- Reporting and analytics

---

## 3. User Personas

### 3.1 Regular User (Type: 'U')

**Description**: Standard cardholders and customer service representatives who interact with the system for day-to-day account management and transaction processing.

**Characteristics**:
- Authenticated via User ID and Password (8 characters each)
- Access limited to their own account information
- Cannot perform administrative functions
- Entry point: Main Menu (COMEN01C)

**Capabilities**:
- View account details and balances
- View and update credit card information
- Browse transaction history
- Add new transactions
- Make bill payments
- Generate transaction reports
- View pending authorizations

### 3.2 Administrator User (Type: 'A')

**Description**: System administrators responsible for user management, system configuration, and oversight of all accounts.

**Characteristics**:
- Authenticated via User ID and Password (8 characters each)
- Full access to all accounts and system functions
- Can manage other users
- Entry point: Admin Menu (COADM01C)

**Capabilities**:
- All Regular User capabilities
- List all users in the system
- Add new users (Regular or Admin)
- Update existing user information
- Delete users from the system
- Access DB2 transaction type management (optional module)
- View all accounts regardless of ownership

### 3.3 Batch Operations User

**Description**: Automated system processes that execute scheduled batch jobs for end-of-day processing, interest calculation, and statement generation.

**Characteristics**:
- Non-interactive system user
- Executes via JCL job submissions
- Operates during batch windows (typically overnight)

**Capabilities**:
- Post daily transactions to master files
- Calculate and apply interest charges
- Generate customer statements
- Export data for branch migration
- Perform data maintenance operations

---

## 4. User Journey Maps

### 4.1 Regular User Journey: Account Inquiry

```
[Login Screen (CC00)]
        |
        v
[Enter User ID & Password]
        |
        v
[Validate Credentials against USRSEC file]
        |
        +--[Invalid]--> [Display Error Message] --> [Return to Login]
        |
        v [Valid - Type 'U']
[Main Menu (CM00)]
        |
        v
[Select Option 1: Account View]
        |
        v
[Account View Screen (CAVW)]
        |
        v
[Enter Account Number]
        |
        v
[Display Account Details]
   - Account ID, Status
   - Current Balance, Credit Limit
   - Cash Credit Limit
   - Open Date, Expiration Date
   - Customer Information
   - FICO Score
        |
        v
[PF3: Return to Main Menu]
```

### 4.2 Regular User Journey: Bill Payment

```
[Main Menu (CM00)]
        |
        v
[Select Option 10: Bill Payment]
        |
        v
[Bill Payment Screen (CB00)]
        |
        v
[Enter Account ID]
        |
        v
[Validate Account Exists]
        |
        +--[Not Found]--> [Display Error] --> [Return to Input]
        |
        v [Found]
[Display Current Balance]
        |
        v
[Confirm Payment (PF5)]
        |
        v
[Process Payment]
   - Create Transaction Record (Type '02', Category 2)
   - Update Account Balance (subtract payment)
   - Write to TRANSACT file
        |
        v
[Display Success Message]
        |
        v
[PF3: Return to Main Menu]
```

### 4.3 Regular User Journey: Transaction Entry

```
[Main Menu (CM00)]
        |
        v
[Select Option 8: Transaction Add]
        |
        v
[Transaction Add Screen (CT02)]
        |
        v
[Enter Transaction Details]
   - Account ID (11 digits)
   - Card Number (16 digits)
   - Transaction Type Code
   - Category Code
   - Source
   - Description
   - Amount (+99999999.99)
   - Original Date (YYYY-MM-DD)
   - Processed Date (YYYY-MM-DD)
   - Merchant Information
        |
        v
[Validate All Fields]
        |
        +--[Validation Error]--> [Display Error] --> [Return to Input]
        |
        v [Valid]
[Generate Transaction ID]
        |
        v
[Write Transaction Record]
        |
        v
[Display Success Message]
        |
        v
[PF3: Return to Main Menu]
```

### 4.4 Administrator Journey: User Management

```
[Login Screen (CC00)]
        |
        v
[Enter Admin Credentials]
        |
        v
[Validate - Type 'A']
        |
        v
[Admin Menu (CA00)]
        |
        v
[Select Option 1: User List]
        |
        v
[User List Screen (CU00)]
   - Display 10 users per page
   - PF7: Page Up, PF8: Page Down
   - Select 'U' to Update, 'D' to Delete
        |
        +--[Select 'U']--> [User Update Screen (CU02)]
        |                         |
        |                         v
        |                  [Modify User Details]
        |                         |
        |                         v
        |                  [PF5: Save Changes]
        |
        +--[Select 'D']--> [User Delete Screen (CU03)]
                                  |
                                  v
                           [Confirm Deletion (PF5)]
                                  |
                                  v
                           [Delete User Record]
```

### 4.5 Batch Processing Journey: Daily Transaction Posting

```
[Scheduler Triggers POSTTRAN Job]
        |
        v
[CLOSEFIL: Close CICS File Access]
        |
        v
[CBTRN02C: Transaction Posting Program]
        |
        v
[Open Files]
   - DALYTRAN (Daily Transactions - Input)
   - TRANSACT (Transaction Master - Output)
   - XREFFILE (Card Cross-Reference)
   - ACCTFILE (Account Master)
   - TCATBALF (Transaction Category Balance)
   - DALYREJS (Rejected Transactions - Output)
        |
        v
[Read Daily Transaction Record]
        |
        +--[EOF]--> [Close Files] --> [Report Statistics] --> [End]
        |
        v
[Validate Transaction]
   - Lookup Card in XREF file
   - Lookup Account in ACCT file
        |
        +--[Invalid]--> [Write to Reject File] --> [Read Next]
        |
        v [Valid]
[Post Transaction]
   - Write to TRANSACT file
   - Update TCATBALF (category balance)
   - Update ACCTFILE (account balance)
        |
        v
[Read Next Transaction]
        |
        v
[OPENFIL: Restore CICS File Access]
```

---

## 5. Complete Business Rule Extraction (BRE)

### 5.1 Authentication and Authorization Rules

#### BR-AUTH-001: User Authentication
- **Rule**: Users must provide a valid User ID (8 characters) and Password (8 characters) to access the system
- **Source**: COSGN00C.cbl, lines 150-180
- **Validation**: Credentials are validated against the USRSEC VSAM file
- **Error Handling**: Invalid credentials display "Invalid userid and/or password" message

#### BR-AUTH-002: User Type Routing
- **Rule**: Upon successful authentication, users are routed based on their user type
- **Source**: COSGN00C.cbl, lines 185-210
- **Logic**:
  - Type 'A' (Admin): Route to Admin Menu (COADM01C)
  - Type 'U' (User): Route to Main Menu (COMEN01C)

#### BR-AUTH-003: Session Management
- **Rule**: User session information is maintained in CARDDEMO-COMMAREA throughout the session
- **Source**: COCOM01Y.cpy
- **Data Elements**:
  - CDEMO-FROM-TRANID: Source transaction ID
  - CDEMO-FROM-PROGRAM: Source program name
  - CDEMO-USER-ID: Current user ID
  - CDEMO-USER-TYPE: User type ('A' or 'U')
  - CDEMO-PGM-CONTEXT: Program context (0=Enter, 1=Reenter)

### 5.2 Account Management Rules

#### BR-ACCT-001: Account Number Format
- **Rule**: Account numbers must be exactly 11 numeric digits
- **Source**: COACTVWC.cbl, COACTUPC.cbl
- **Validation**: ACCT-ID PIC 9(11)

#### BR-ACCT-002: Account Status Values
- **Rule**: Account status must be a single character indicating active/inactive state
- **Source**: CVACT01Y.cpy
- **Field**: ACCT-ACTIVE-STATUS PIC X(01)

#### BR-ACCT-003: Credit Limit Validation
- **Rule**: Credit limit must be a signed numeric value with 2 decimal places
- **Source**: CVACT01Y.cpy
- **Field**: ACCT-CREDIT-LIMIT PIC S9(10)V99
- **Range**: -9999999999.99 to +9999999999.99

#### BR-ACCT-004: Cash Credit Limit Validation
- **Rule**: Cash credit limit must be a signed numeric value with 2 decimal places
- **Source**: CVACT01Y.cpy
- **Field**: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99

#### BR-ACCT-005: Current Balance Tracking
- **Rule**: Current balance is updated with each transaction and interest calculation
- **Source**: CVACT01Y.cpy
- **Field**: ACCT-CURR-BAL PIC S9(10)V99

#### BR-ACCT-006: Date Format Standards
- **Rule**: All dates must be in format YYYY-MM-DD (10 characters)
- **Source**: COACTUPC.cbl, CVACT01Y.cpy
- **Fields**: ACCT-OPEN-DATE, ACCT-EXPIRAION-DATE, ACCT-REISSUE-DATE

#### BR-ACCT-007: Cycle Credit/Debit Tracking
- **Rule**: Current cycle credits and debits are tracked separately and reset during interest calculation
- **Source**: CVACT01Y.cpy, CBACT04C.cbl
- **Fields**: ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT

### 5.3 Credit Card Management Rules

#### BR-CARD-001: Card Number Format
- **Rule**: Card numbers must be exactly 16 characters
- **Source**: CVACT02Y.cpy, COCRDLIC.cbl
- **Field**: CARD-NUM PIC X(16)

#### BR-CARD-002: Card-Account Association
- **Rule**: Each card must be associated with exactly one account
- **Source**: CVACT02Y.cpy
- **Field**: CARD-ACCT-ID PIC 9(11)

#### BR-CARD-003: CVV Code Format
- **Rule**: CVV code must be exactly 3 numeric digits
- **Source**: CVACT02Y.cpy
- **Field**: CARD-CVV-CD PIC 9(03)

#### BR-CARD-004: Embossed Name Validation
- **Rule**: Card embossed name must contain only alphabetic characters and spaces
- **Source**: COCRDUPC.cbl, lines 200-250
- **Field**: CARD-EMBOSSED-NAME PIC X(50)
- **Validation**: Each character checked for ALPHABETIC or SPACE

#### BR-CARD-005: Card Status Values
- **Rule**: Card status must be 'Y' (Active) or 'N' (Inactive)
- **Source**: COCRDUPC.cbl
- **Field**: CARD-ACTIVE-STATUS PIC X(01)

#### BR-CARD-006: Card Expiration Date Validation
- **Rule**: Expiration date must have valid month (1-12) and year (1950-2099)
- **Source**: COCRDUPC.cbl
- **Validation**:
  - Month: Must be between 01 and 12
  - Year: Must be between 1950 and 2099

#### BR-CARD-007: Card List Display
- **Rule**: Card list displays up to 7 cards per screen with pagination
- **Source**: COCRDLIC.cbl
- **Navigation**: PF7 (Page Up), PF8 (Page Down)

#### BR-CARD-008: Card Selection Actions
- **Rule**: Users can select cards for viewing ('S') or updating ('U')
- **Source**: COCRDLIC.cbl

### 5.4 Transaction Processing Rules

#### BR-TRAN-001: Transaction ID Format
- **Rule**: Transaction IDs are 16-character unique identifiers
- **Source**: CVTRA05Y.cpy
- **Field**: TRAN-ID PIC X(16)

#### BR-TRAN-002: Transaction Type Code
- **Rule**: Transaction type is a 2-character code
- **Source**: CVTRA05Y.cpy
- **Field**: TRAN-TYPE-CD PIC X(02)
- **Examples**: '01' (Purchase), '02' (Payment)

#### BR-TRAN-003: Transaction Category Code
- **Rule**: Transaction category is a 4-digit numeric code
- **Source**: CVTRA05Y.cpy
- **Field**: TRAN-CAT-CD PIC 9(04)

#### BR-TRAN-004: Transaction Amount Format
- **Rule**: Transaction amount must be signed numeric with 2 decimal places
- **Source**: CVTRA05Y.cpy, COTRN02C.cbl
- **Field**: TRAN-AMT PIC S9(09)V99
- **Display Format**: +99999999.99

#### BR-TRAN-005: Transaction Date Format
- **Rule**: Transaction dates must be in YYYY-MM-DD format
- **Source**: COTRN02C.cbl
- **Fields**: TRAN-ORIG-TS, TRAN-PROC-TS (26-character timestamps)

#### BR-TRAN-006: Transaction Source
- **Rule**: Transaction source is a 10-character field indicating origin
- **Source**: CVTRA05Y.cpy
- **Field**: TRAN-SOURCE PIC X(10)
- **Examples**: 'POS TERM', 'ONLINE', 'ATM'

#### BR-TRAN-007: Merchant Information
- **Rule**: Merchant details include ID (9 digits), Name (50 chars), City (50 chars), ZIP (10 chars)
- **Source**: CVTRA05Y.cpy
- **Fields**: TRAN-MERCHANT-ID, TRAN-MERCHANT-NAME, TRAN-MERCHANT-CITY, TRAN-MERCHANT-ZIP

#### BR-TRAN-008: Transaction List Display
- **Rule**: Transaction list displays up to 10 transactions per screen with pagination
- **Source**: COTRN00C.cbl
- **Navigation**: PF7 (Page Up), PF8 (Page Down)

#### BR-TRAN-009: Transaction Validation for Posting
- **Rule**: Daily transactions must pass validation before posting to master file
- **Source**: CBTRN02C.cbl
- **Validations**:
  - Card number must exist in XREF file
  - Account must exist in ACCT file
  - Invalid transactions written to DALYREJS file with reason code

### 5.5 Bill Payment Rules

#### BR-BILL-001: Payment Account Validation
- **Rule**: Account must exist and have a positive balance to process payment
- **Source**: COBIL00C.cbl
- **Validation**: Read ACCTDAT file, verify ACCT-CURR-BAL > 0

#### BR-BILL-002: Payment Amount Determination
- **Rule**: Payment amount equals the current account balance
- **Source**: COBIL00C.cbl
- **Logic**: Payment clears the full outstanding balance

#### BR-BILL-003: Payment Transaction Creation
- **Rule**: Bill payment creates a transaction record with specific attributes
- **Source**: COBIL00C.cbl
- **Attributes**:
  - Transaction Type: '02'
  - Category: 2
  - Source: 'POS TERM'
  - Description: 'BILL PAYMENT - ONLINE'
  - Merchant ID: 999999999

#### BR-BILL-004: Balance Update
- **Rule**: Account balance is reduced by the payment amount
- **Source**: COBIL00C.cbl
- **Operation**: ACCT-CURR-BAL = ACCT-CURR-BAL - Payment Amount

### 5.6 Interest Calculation Rules

#### BR-INT-001: Interest Rate Lookup
- **Rule**: Interest rates are determined by account group, transaction type, and category
- **Source**: CBACT04C.cbl, CVTRA02Y.cpy
- **Lookup**: DISCGRP file using composite key (Group ID + Type + Category)

#### BR-INT-002: Interest Calculation Formula
- **Rule**: Monthly interest is calculated on transaction category balances
- **Source**: CBACT04C.cbl
- **Formula**: Interest = Category Balance * (Interest Rate / 12)

#### BR-INT-003: Balance Update After Interest
- **Rule**: Account balance is updated with total interest and cycle counters are reset
- **Source**: CBACT04C.cbl
- **Operations**:
  - ACCT-CURR-BAL = ACCT-CURR-BAL + Total Interest
  - ACCT-CURR-CYC-CREDIT = 0
  - ACCT-CURR-CYC-DEBIT = 0

### 5.7 User Management Rules

#### BR-USER-001: User ID Format
- **Rule**: User IDs must be exactly 8 characters
- **Source**: CSUSR01Y.cpy
- **Field**: SEC-USR-ID PIC X(08)

#### BR-USER-002: User Name Fields
- **Rule**: First name and last name are each 20 characters
- **Source**: CSUSR01Y.cpy
- **Fields**: SEC-USR-FNAME, SEC-USR-LNAME PIC X(20)

#### BR-USER-003: Password Format
- **Rule**: Passwords must be exactly 8 characters
- **Source**: CSUSR01Y.cpy
- **Field**: SEC-USR-PWD PIC X(08)

#### BR-USER-004: User Type Values
- **Rule**: User type must be 'A' (Admin) or 'U' (Regular User)
- **Source**: CSUSR01Y.cpy, COCOM01Y.cpy
- **Field**: SEC-USR-TYPE PIC X(01)

#### BR-USER-005: User Add Validation
- **Rule**: All fields (First Name, Last Name, User ID, Password, User Type) are required
- **Source**: COUSR01C.cbl
- **Error Messages**: "[Field] can NOT be empty..."

#### BR-USER-006: Duplicate User Prevention
- **Rule**: User IDs must be unique; duplicate IDs are rejected
- **Source**: COUSR01C.cbl
- **Error**: "User ID already exist..."

#### BR-USER-007: User Update Tracking
- **Rule**: User updates only proceed if at least one field has changed
- **Source**: COUSR02C.cbl
- **Validation**: Compare new values against existing record

#### BR-USER-008: User Delete Confirmation
- **Rule**: User deletion requires explicit confirmation via PF5 key
- **Source**: COUSR03C.cbl

### 5.8 Report Generation Rules

#### BR-RPT-001: Report Types
- **Rule**: System supports Monthly, Yearly, and Custom date range reports
- **Source**: CORPT00C.cbl
- **Options**: MONTHLY, YEARLY, CUSTOM

#### BR-RPT-002: Monthly Report Date Range
- **Rule**: Monthly report covers first day to last day of current month
- **Source**: CORPT00C.cbl
- **Calculation**: Start = YYYY-MM-01, End = Last day of month

#### BR-RPT-003: Yearly Report Date Range
- **Rule**: Yearly report covers January 1 to December 31 of current year
- **Source**: CORPT00C.cbl
- **Calculation**: Start = YYYY-01-01, End = YYYY-12-31

#### BR-RPT-004: Custom Report Date Validation
- **Rule**: Custom date ranges must have valid month (1-12) and day (1-31)
- **Source**: CORPT00C.cbl
- **Validation**: Numeric checks on MM, DD, YYYY components

#### BR-RPT-005: Report Job Submission
- **Rule**: Reports are generated by submitting batch JCL to internal reader
- **Source**: CORPT00C.cbl
- **Mechanism**: CICS TDQ (Transient Data Queue) to INTRDR

### 5.9 Customer Data Rules

#### BR-CUST-001: Customer ID Format
- **Rule**: Customer IDs are 9-digit numeric identifiers
- **Source**: CVCUS01Y.cpy
- **Field**: CUST-ID PIC 9(09)

#### BR-CUST-002: Customer Name Fields
- **Rule**: Customer names include First (25), Middle (25), and Last (25) name fields
- **Source**: CVCUS01Y.cpy
- **Fields**: CUST-FIRST-NAME, CUST-MIDDLE-NAME, CUST-LAST-NAME

#### BR-CUST-003: Address Structure
- **Rule**: Customer address includes 3 address lines (50 chars each), state (2), country (3), ZIP (10)
- **Source**: CVCUS01Y.cpy
- **Fields**: CUST-ADDR-LINE-1/2/3, CUST-ADDR-STATE-CD, CUST-ADDR-COUNTRY-CD, CUST-ADDR-ZIP

#### BR-CUST-004: Phone Number Format
- **Rule**: Two phone numbers supported, each 15 characters
- **Source**: CVCUS01Y.cpy
- **Fields**: CUST-PHONE-NUM-1, CUST-PHONE-NUM-2
- **Display Format**: (XXX)XXX-XXXX (validated in COACTUPC.cbl)

#### BR-CUST-005: SSN Format
- **Rule**: Social Security Number is 9 numeric digits
- **Source**: CVCUS01Y.cpy
- **Field**: CUST-SSN PIC 9(09)
- **Display Format**: XXX-XX-XXXX (validated in COACTUPC.cbl)

#### BR-CUST-006: FICO Score
- **Rule**: FICO credit score is a 3-digit numeric value
- **Source**: CVCUS01Y.cpy
- **Field**: CUST-FICO-CREDIT-SCORE PIC 9(03)

#### BR-CUST-007: Primary Cardholder Indicator
- **Rule**: Single character indicating if customer is primary cardholder
- **Source**: CVCUS01Y.cpy
- **Field**: CUST-PRI-CARD-HOLDER-IND PIC X(01)

### 5.10 Cross-Reference Rules

#### BR-XREF-001: Card-Customer-Account Linkage
- **Rule**: Cross-reference file links card numbers to customer IDs and account IDs
- **Source**: CVACT03Y.cpy
- **Structure**:
  - XREF-CARD-NUM PIC X(16) - Primary Key
  - XREF-CUST-ID PIC 9(09)
  - XREF-ACCT-ID PIC 9(11)

#### BR-XREF-002: Alternate Index Access
- **Rule**: Cross-reference can be accessed by account ID via alternate index
- **Source**: CBACT04C.cbl, JCL files
- **AIX Path**: CARDXREF.VSAM.AIX.PATH

---

## 6. Business Architecture

### 6.1 Business Capability Model

```
CardDemo Business Capabilities
├── Customer Management
│   ├── Customer Information Maintenance
│   ├── Customer Profile View
│   └── Customer Data Export/Import
│
├── Account Management
│   ├── Account Inquiry
│   ├── Account Update
│   ├── Balance Management
│   └── Credit Limit Administration
│
├── Card Management
│   ├── Card Listing
│   ├── Card Detail View
│   ├── Card Update
│   └── Card-Account Association
│
├── Transaction Processing
│   ├── Transaction Entry
│   ├── Transaction Inquiry
│   ├── Transaction Posting (Batch)
│   ├── Transaction Validation
│   └── Transaction Reporting
│
├── Payment Processing
│   ├── Bill Payment
│   ├── Payment Posting
│   └── Balance Update
│
├── Financial Processing
│   ├── Interest Calculation
│   ├── Fee Assessment
│   └── Statement Generation
│
├── Security Administration
│   ├── User Authentication
│   ├── User Management (CRUD)
│   ├── Access Control
│   └── Session Management
│
└── System Administration
    ├── Batch Job Management
    ├── File Management
    ├── Report Generation
    └── Data Migration
```

### 6.2 Business Process Hierarchy

#### Level 0: Enterprise Processes
1. **Credit Card Operations** - End-to-end credit card lifecycle management

#### Level 1: Business Processes
1. Customer Onboarding
2. Account Servicing
3. Transaction Processing
4. Payment Processing
5. Financial Closing
6. Reporting and Analytics

#### Level 2: Sub-Processes

**Customer Onboarding**
- Customer Registration
- Account Creation
- Card Issuance
- Cross-Reference Setup

**Account Servicing**
- Account Inquiry
- Account Modification
- Card Management
- Customer Updates

**Transaction Processing**
- Transaction Capture
- Transaction Validation
- Transaction Posting
- Rejection Handling

**Payment Processing**
- Payment Initiation
- Payment Validation
- Balance Update
- Payment Confirmation

**Financial Closing**
- Daily Transaction Backup
- Interest Calculation
- Fee Assessment
- Statement Generation

**Reporting and Analytics**
- Transaction Reports
- Monthly Statements
- Yearly Summaries
- Custom Reports

### 6.3 Business Services

| Service | Description | Programs |
|---------|-------------|----------|
| Authentication Service | Validates user credentials and establishes sessions | COSGN00C |
| Account Service | Manages account information and balances | COACTVWC, COACTUPC |
| Card Service | Manages credit card lifecycle | COCRDLIC, COCRDSLC, COCRDUPC |
| Transaction Service | Handles transaction entry and inquiry | COTRN00C, COTRN01C, COTRN02C |
| Payment Service | Processes bill payments | COBIL00C |
| User Service | Manages system users | COUSR00C, COUSR01C, COUSR02C, COUSR03C |
| Report Service | Generates transaction reports | CORPT00C |
| Posting Service | Posts daily transactions to master files | CBTRN02C |
| Interest Service | Calculates and applies interest | CBACT04C |
| Statement Service | Generates customer statements | CBSTM03A, CBSTM03B |
| Export Service | Exports data for migration | CBEXPORT |
| Import Service | Imports data from migration | CBIMPORT |

---

## 7. Business Domain Model

### 7.1 Core Entities

#### Customer Entity
```
Customer
├── Customer ID (PK) - 9 digits
├── First Name - 25 chars
├── Middle Name - 25 chars
├── Last Name - 25 chars
├── Address Line 1 - 50 chars
├── Address Line 2 - 50 chars
├── Address Line 3 - 50 chars
├── State Code - 2 chars
├── Country Code - 3 chars
├── ZIP Code - 10 chars
├── Phone Number 1 - 15 chars
├── Phone Number 2 - 15 chars
├── SSN - 9 digits
├── Government ID - 20 chars
├── Date of Birth - 10 chars (YYYY-MM-DD)
├── EFT Account ID - 10 chars
├── Primary Cardholder Indicator - 1 char
└── FICO Credit Score - 3 digits
```

#### Account Entity
```
Account
├── Account ID (PK) - 11 digits
├── Active Status - 1 char
├── Current Balance - Signed 10.2 decimal
├── Credit Limit - Signed 10.2 decimal
├── Cash Credit Limit - Signed 10.2 decimal
├── Open Date - 10 chars (YYYY-MM-DD)
├── Expiration Date - 10 chars (YYYY-MM-DD)
├── Reissue Date - 10 chars (YYYY-MM-DD)
├── Current Cycle Credit - Signed 10.2 decimal
├── Current Cycle Debit - Signed 10.2 decimal
├── Address ZIP - 10 chars
└── Group ID - 10 chars
```

#### Card Entity
```
Card
├── Card Number (PK) - 16 chars
├── Account ID (FK) - 11 digits
├── CVV Code - 3 digits
├── Embossed Name - 50 chars
├── Expiration Date - 10 chars (YYYY-MM-DD)
└── Active Status - 1 char (Y/N)
```

#### Transaction Entity
```
Transaction
├── Transaction ID (PK) - 16 chars
├── Type Code - 2 chars
├── Category Code - 4 digits
├── Source - 10 chars
├── Description - 100 chars
├── Amount - Signed 9.2 decimal
├── Merchant ID - 9 digits
├── Merchant Name - 50 chars
├── Merchant City - 50 chars
├── Merchant ZIP - 10 chars
├── Card Number (FK) - 16 chars
├── Original Timestamp - 26 chars
└── Processed Timestamp - 26 chars
```

#### Card Cross-Reference Entity
```
CardXref
├── Card Number (PK) - 16 chars
├── Customer ID (FK) - 9 digits
└── Account ID (FK) - 11 digits
```

#### User Security Entity
```
UserSecurity
├── User ID (PK) - 8 chars
├── First Name - 20 chars
├── Last Name - 20 chars
├── Password - 8 chars
└── User Type - 1 char (A/U)
```

#### Transaction Category Balance Entity
```
TransactionCategoryBalance
├── Account ID (PK1) - 11 digits
├── Type Code (PK2) - 2 chars
├── Category Code (PK3) - 4 digits
└── Balance - Signed 9.2 decimal
```

#### Disclosure Group Entity
```
DisclosureGroup
├── Account Group ID (PK1) - 10 chars
├── Transaction Type Code (PK2) - 2 chars
├── Transaction Category Code (PK3) - 4 digits
└── Interest Rate - Signed 4.2 decimal
```

### 7.2 Entity Relationships

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│  Customer   │───────│  CardXref   │───────│   Account   │
│             │  1:N  │             │  N:1  │             │
└─────────────┘       └─────────────┘       └─────────────┘
                            │
                            │ 1:1
                            ▼
                      ┌─────────────┐
                      │    Card     │
                      │             │
                      └─────────────┘
                            │
                            │ 1:N
                            ▼
                      ┌─────────────┐
                      │ Transaction │
                      │             │
                      └─────────────┘

┌─────────────┐       ┌─────────────────────┐
│   Account   │───────│ TransCategoryBalance│
│             │  1:N  │                     │
└─────────────┘       └─────────────────────┘

┌─────────────┐       ┌─────────────────────┐
│DisclosureGrp│───────│ TransCategoryBalance│
│             │  1:N  │                     │
└─────────────┘       └─────────────────────┘
```

### 7.3 Relationship Cardinalities

| Relationship | Cardinality | Description |
|--------------|-------------|-------------|
| Customer - CardXref | 1:N | One customer can have multiple cards |
| Account - CardXref | 1:N | One account can have multiple cards |
| CardXref - Card | 1:1 | Each cross-reference maps to one card |
| Card - Transaction | 1:N | One card can have multiple transactions |
| Account - TransCategoryBalance | 1:N | One account has balances per category |
| DisclosureGroup - TransCategoryBalance | 1:N | Interest rates apply to category balances |

---

## 8. Functional Requirements Summary

### 8.1 Online Functions (CICS)

| Function ID | Name | Transaction | Program | Description |
|-------------|------|-------------|---------|-------------|
| F-ONL-001 | User Sign-on | CC00 | COSGN00C | Authenticate users and route to appropriate menu |
| F-ONL-002 | Main Menu | CM00 | COMEN01C | Display menu options for regular users |
| F-ONL-003 | Admin Menu | CA00 | COADM01C | Display menu options for admin users |
| F-ONL-004 | Account View | CAVW | COACTVWC | Display account details |
| F-ONL-005 | Account Update | CAUP | COACTUPC | Modify account information |
| F-ONL-006 | Card List | CCLI | COCRDLIC | List cards with pagination |
| F-ONL-007 | Card Detail | CCDL | COCRDSLC | Display single card details |
| F-ONL-008 | Card Update | CCUP | COCRDUPC | Modify card information |
| F-ONL-009 | Transaction List | CT00 | COTRN00C | List transactions with pagination |
| F-ONL-010 | Transaction View | CT01 | COTRN01C | Display single transaction details |
| F-ONL-011 | Transaction Add | CT02 | COTRN02C | Add new transaction |
| F-ONL-012 | Bill Payment | CB00 | COBIL00C | Process bill payment |
| F-ONL-013 | Transaction Reports | CR00 | CORPT00C | Generate transaction reports |
| F-ONL-014 | User List | CU00 | COUSR00C | List all users (Admin) |
| F-ONL-015 | User Add | CU01 | COUSR01C | Add new user (Admin) |
| F-ONL-016 | User Update | CU02 | COUSR02C | Update user (Admin) |
| F-ONL-017 | User Delete | CU03 | COUSR03C | Delete user (Admin) |

### 8.2 Batch Functions (JCL)

| Function ID | Name | Job | Program | Description |
|-------------|------|-----|---------|-------------|
| F-BAT-001 | Transaction Posting | POSTTRAN | CBTRN02C | Post daily transactions to master files |
| F-BAT-002 | Interest Calculation | INTCALC | CBACT04C | Calculate and apply interest charges |
| F-BAT-003 | Statement Generation | CREASTMT | CBSTM03A/B | Generate customer statements |
| F-BAT-004 | Data Export | CBEXPORT | CBEXPORT | Export data for branch migration |
| F-BAT-005 | Data Import | CBIMPORT | CBIMPORT | Import data from migration |
| F-BAT-006 | File Close | CLOSEFIL | - | Close CICS file access for batch |
| F-BAT-007 | File Open | OPENFIL | - | Restore CICS file access after batch |
| F-BAT-008 | Transaction Backup | TRANBKP | - | Backup transaction files |

### 8.3 Optional Functions (DB2/IMS/MQ)

| Function ID | Name | Transaction | Program | Description |
|-------------|------|-------------|---------|-------------|
| F-OPT-001 | Transaction Type List | CTLI | COTRTLIC | List transaction types from DB2 |
| F-OPT-002 | Transaction Type Update | CTTU | COTRTUPC | Update transaction types in DB2 |
| F-OPT-003 | Pending Auth View | CPVS | COPAUS0C | View pending authorizations (IMS) |
| F-OPT-004 | Pending Auth Detail | CPVD | COPAUS1C | View authorization details (IMS) |

---

## 9. Screen Navigation Map

```
                                    ┌─────────────┐
                                    │   CC00      │
                                    │  Sign-on    │
                                    └──────┬──────┘
                                           │
                    ┌──────────────────────┴──────────────────────┐
                    │                                              │
                    ▼                                              ▼
            ┌───────────────┐                              ┌───────────────┐
            │     CM00      │                              │     CA00      │
            │  Main Menu    │                              │  Admin Menu   │
            │  (User)       │                              │  (Admin)      │
            └───────┬───────┘                              └───────┬───────┘
                    │                                              │
    ┌───────────────┼───────────────┐              ┌───────────────┼───────────────┐
    │       │       │       │       │              │       │       │       │       │
    ▼       ▼       ▼       ▼       ▼              ▼       ▼       ▼       ▼       ▼
┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐      ┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐
│ CAVW ││ CAUP ││ CCLI ││ CT00 ││ CB00 │      │ CU00 ││ CU01 ││ CU02 ││ CU03 ││ CTLI │
│ Acct ││ Acct ││ Card ││ Tran ││ Bill │      │ User ││ User ││ User ││ User ││ Tran │
│ View ││Update││ List ││ List ││ Pay  │      │ List ││ Add  ││Update││Delete││ Type │
└──────┘└──────┘└──┬───┘└──┬───┘└──────┘      └──┬───┘└──────┘└──────┘└──────┘└──────┘
                   │       │                     │
           ┌───────┴───┐   │                     │
           │           │   │                     │
           ▼           ▼   ▼                     │
       ┌──────┐    ┌──────┐┌──────┐              │
       │ CCDL │    │ CCUP ││ CT01 │              │
       │ Card │    │ Card ││ Tran │              │
       │Detail│    │Update││ View │              │
       └──────┘    └──────┘└──────┘              │
                                                 │
                           ┌─────────────────────┘
                           │
                   ┌───────┴───────┐
                   │               │
                   ▼               ▼
               ┌──────┐        ┌──────┐
               │ CU02 │        │ CU03 │
               │ User │        │ User │
               │Update│        │Delete│
               └──────┘        └──────┘
```

---

## 10. Key Function Keys

| Key | Function | Context |
|-----|----------|---------|
| ENTER | Submit/Confirm | All screens |
| PF3 | Exit/Return to Previous | All screens |
| PF4 | Clear Screen | User management screens |
| PF5 | Save/Confirm Action | Update/Delete screens |
| PF7 | Page Up | List screens |
| PF8 | Page Down | List screens |
| PF12 | Return to Admin Menu | Admin screens |

---

## 11. Error Handling

### 11.1 Validation Error Messages

| Error Code | Message | Context |
|------------|---------|---------|
| AUTH-001 | "Invalid userid and/or password" | Sign-on |
| AUTH-002 | "Please enter a valid option number..." | Menu selection |
| ACCT-001 | "Account ID NOT found..." | Account lookup |
| CARD-001 | "Card number must be 16 digits" | Card validation |
| CARD-002 | "Card name must contain only alphabets" | Card update |
| CARD-003 | "Card status must be Y or N" | Card update |
| TRAN-001 | "Account ID must be numeric" | Transaction add |
| TRAN-002 | "Amount format invalid" | Transaction add |
| USER-001 | "[Field] can NOT be empty..." | User management |
| USER-002 | "User ID already exist..." | User add |
| USER-003 | "User ID NOT found..." | User lookup |
| NAV-001 | "You are already at the top of the page..." | Pagination |
| NAV-002 | "You are already at the bottom of the page..." | Pagination |

### 11.2 System Error Handling

- File I/O errors result in program abend with diagnostic display
- CICS response codes are captured in WS-RESP-CD and WS-REAS-CD
- Batch programs set return codes (0=success, 4=warnings, 12=errors)
- Rejected transactions are written to DALYREJS file with reason codes

---

## 12. Appendix: Transaction Code Reference

| Transaction | Program | Description |
|-------------|---------|-------------|
| CC00 | COSGN00C | Sign-on Screen |
| CM00 | COMEN01C | Main Menu (Regular Users) |
| CA00 | COADM01C | Admin Menu |
| CAVW | COACTVWC | Account View |
| CAUP | COACTUPC | Account Update |
| CCLI | COCRDLIC | Credit Card List |
| CCDL | COCRDSLC | Credit Card Detail |
| CCUP | COCRDUPC | Credit Card Update |
| CT00 | COTRN00C | Transaction List |
| CT01 | COTRN01C | Transaction View |
| CT02 | COTRN02C | Transaction Add |
| CB00 | COBIL00C | Bill Payment |
| CR00 | CORPT00C | Transaction Reports |
| CU00 | COUSR00C | User List |
| CU01 | COUSR01C | User Add |
| CU02 | COUSR02C | User Update |
| CU03 | COUSR03C | User Delete |
| CTLI | COTRTLIC | Transaction Type List (DB2) |
| CTTU | COTRTUPC | Transaction Type Update (DB2) |

---

*Document generated through reverse engineering of CardDemo COBOL source code for modernization planning purposes.*
