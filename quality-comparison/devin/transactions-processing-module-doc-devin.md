# Functional Specification: Transaction Processing Module

> **Module:** Transaction Processing
> **Application:** CardDemo — CICS Online Mainframe Application
> **Document Type:** Functional Specification
> **Source Programs:** COTRN00C (List), COTRN01C (View), COTRN02C (Add)
> **Source References:** `devindoc/list-transactions-program-doc-devin.md`, `devindoc/view-transactions-program-doc-devin.md`, `devindoc/add-transactions-program-doc-devin.md`

---

## 1. Introduction

### 1.1 Purpose

This document provides a comprehensive functional specification for the Transaction Processing Module of the CardDemo application. It describes the module's business objectives, functional capabilities, user interactions, data handling, validation rules, inter-program navigation, and error management. This specification is written from a business analyst perspective and consolidates information from the three underlying program analysis documents.

### 1.2 Scope

The Transaction Processing Module encompasses three core functions:

| Function | Program | TRANID | Description |
|----------|---------|--------|-------------|
| List Transactions | COTRN00C | CT00 | Paginated browse of transaction records with selection capability |
| View Transaction | COTRN01C | CT01 | Read-only detail display of a single transaction |
| Add Transaction | COTRN02C | CT02 | Data entry, validation, and creation of a new transaction record |

**Out of Scope:** Transaction update, transaction deletion, batch transaction posting (CBTRN01C/CBTRN02C), transaction reporting (CBTRN03C/CORPT00C), and interest calculation (CBACT04C). These are handled by separate modules.

### 1.3 Intended Audience

- Business analysts performing requirements traceability
- Application architects planning modernization
- QA teams designing test cases
- Development teams implementing equivalent functionality in a target platform

---

## 2. Module Overview

### 2.1 Business Purpose

The Transaction Processing Module enables CardDemo users to manage financial transactions associated with credit card accounts. It provides the ability to:

1. **Browse** all recorded transactions in a paginated list view
2. **Inspect** the full details of any individual transaction
3. **Record** new transactions against validated accounts and cards

These functions represent the core operational workflow for transaction data entry and inquiry within the CardDemo application.

### 2.2 Module Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     COMEN01C                             │
│                   (Main Menu)                            │
│                                                          │
│   Option 4: Transaction List    Option 8: Transaction Add│
└────────┬──────────────────────────────────┬──────────────┘
         │ XCTL                             │ XCTL
         ▼                                  ▼
┌────────────────────┐            ┌────────────────────┐
│     COTRN00C       │            │     COTRN02C       │
│  List Transactions │            │  Add Transaction   │
│  (CT00)            │            │  (CT02)            │
│                    │            │                    │
│  - 10 rows/page    │            │  - 13 input fields │
│  - PF7/PF8 paging  │            │  - 6-phase valid.  │
│  - Filter by ID    │            │  - Y/N confirm     │
│  - Select 'S'      │            │  - Auto-gen ID     │
└────────┬───────────┘            │  - Copy last (PF5) │
         │ XCTL (Select 'S')     └────────────────────┘
         ▼
┌────────────────────┐
│     COTRN01C       │
│  View Transaction  │
│  (CT01)            │
│                    │
│  - 13 detail fields│
│  - Read-only       │
│  - PF5 → back to   │
│    list            │
└────────────────────┘
```

### 2.3 Shared Infrastructure

All three programs share the following common infrastructure:

| Component | Description | Source |
|-----------|-------------|--------|
| **COMMAREA** (`COCOM01Y.cpy`) | Shared data area passed between programs for state preservation across pseudo-conversational interactions | All three programs |
| **TRANSACT VSAM File** | Primary data store — KSDS (Keyed Sequential Data Set) keyed by 16-byte Transaction ID | All three programs |
| **CVTRA05Y.cpy** | Transaction record layout (TRAN-RECORD, 350 bytes) | All three programs |
| **COTTL01Y.cpy** | Screen title constants | All three programs |
| **CSDAT01Y.cpy** | Date/time formatting fields | All three programs |
| **CSMSG01Y.cpy** | Common error message constants | All three programs |
| **Pseudo-conversational model** | All programs use CICS RETURN with TRANSID and COMMAREA for stateless operation | All three programs |

---

## 3. Functional Requirements

### 3.1 FR-01: List Transactions (COTRN00C)

#### 3.1.1 Description
The system shall display a paginated list of transactions from the TRANSACT file, showing summary information for each transaction. Users can navigate forward and backward through pages, optionally filter by a starting Transaction ID, and select a transaction for detailed viewing.

#### 3.1.2 Screen Layout

Each row in the transaction list displays:

| Column | Field | Format | Source Field |
|--------|-------|--------|-------------|
| Selection | User input | 1 character ('S') | SEL0001I – SEL0010I |
| Transaction ID | Display | 16-character numeric | TRAN-ID |
| Date | Display | MM/DD/YY | Derived from TRAN-ORIG-TS |
| Description | Display | Text | TRAN-DESC |
| Amount | Display | +99999999.99 | TRAN-AMT |

#### 3.1.3 Functional Behavior

| Action | Trigger | Behavior |
|--------|---------|----------|
| Initial Load | Entry from menu (COMEN01C) | Display first page of transactions starting from lowest Transaction ID |
| Page Forward | PF8 | Load next 10 transactions. If at end of file, display 'You are already at the bottom of the page...' |
| Page Backward | PF7 | Load previous 10 transactions. If at page 1, display 'You are already at the top of the page...' |
| Filter | Enter Transaction ID in filter field + ENTER | Restart listing from the specified Transaction ID |
| Select for View | Enter 'S' next to a transaction row + ENTER | Transfer to View Transaction screen (COTRN01C) with the selected Transaction ID |
| Return to Menu | PF3 | Return to main menu (COMEN01C) |

#### 3.1.4 Pagination State

The following state is maintained in COMMAREA across interactions:

| State Field | Purpose |
|------------|---------|
| `CDEMO-CT00-TRNID-FIRST` | Transaction ID of the first record on the current page |
| `CDEMO-CT00-TRNID-LAST` | Transaction ID of the last record on the current page |
| `CDEMO-CT00-PAGE-NUM` | Current page number (displayed on screen) |
| `CDEMO-CT00-NEXT-PAGE-FLG` | 'Y' if more records exist beyond current page, 'N' if at end |

---

### 3.2 FR-02: View Transaction (COTRN01C)

#### 3.2.1 Description
The system shall display the complete details of a single transaction in a read-only format. The transaction can be identified either by direct user input of a Transaction ID or by automatic pre-selection from the List Transactions screen.

#### 3.2.2 Display Fields

The following 13 fields are displayed for a transaction:

| # | Field | Source Field | Description |
|---|-------|-------------|-------------|
| 1 | Transaction ID | TRAN-ID | Unique 16-byte identifier |
| 2 | Card Number | TRAN-CARD-NUM | Associated credit card number |
| 3 | Type Code | TRAN-TYPE-CD | Transaction type classification |
| 4 | Category Code | TRAN-CAT-CD | Transaction category classification |
| 5 | Source | TRAN-SOURCE | Transaction origination source |
| 6 | Amount | TRAN-AMT | Transaction amount (formatted +99999999.99) |
| 7 | Description | TRAN-DESC | Transaction description text |
| 8 | Origination Timestamp | TRAN-ORIG-TS | When the transaction originated |
| 9 | Processing Timestamp | TRAN-PROC-TS | When the transaction was processed |
| 10 | Merchant ID | TRAN-MERCHANT-ID | Merchant identifier |
| 11 | Merchant Name | TRAN-MERCHANT-NAME | Merchant business name |
| 12 | Merchant City | TRAN-MERCHANT-CITY | Merchant city location |
| 13 | Merchant Zip | TRAN-MERCHANT-ZIP | Merchant postal code |

#### 3.2.3 Functional Behavior

| Action | Trigger | Behavior |
|--------|---------|----------|
| Auto-Load from List | Entry from COTRN00C with pre-selected ID | Automatically read and display the selected transaction without user action |
| Manual Lookup | Enter Transaction ID + ENTER | Read and display the specified transaction. Error if empty or not found |
| Clear Screen | PF4 | Reset all fields to allow a new Transaction ID entry |
| Return to List | PF5 | Transfer back to List Transactions (COTRN00C), preserving pagination state |
| Return to Menu/Caller | PF3 | Return to calling program or main menu (COMEN01C) |

#### 3.2.4 Read-Only Constraint
This screen is strictly read-only. No update or delete operations are available from this function. The transaction record is not modified.

---

### 3.3 FR-03: Add Transaction (COTRN02C)

#### 3.3.1 Description
The system shall allow users to create a new transaction record in the TRANSACT file. The user provides an Account ID or Card Number to identify the associated account, enters all required transaction details, and confirms the addition. The system auto-generates a unique Transaction ID and writes the record upon confirmation.

#### 3.3.2 Input Fields

| # | Field | Input Name | Type | Required | Validation |
|---|-------|-----------|------|----------|------------|
| 1 | Account ID | ACTIDINI | Numeric | Conditional (if Card # not provided) | Must be numeric; must exist in CXACAIX cross-reference |
| 2 | Card Number | CARDNINI | Numeric | Conditional (if Acct ID not provided) | Must be numeric; must exist in CCXREF cross-reference |
| 3 | Type Code | TTYPCDI | Numeric | Yes | Must not be empty; must be numeric |
| 4 | Category Code | TCATCDI | Numeric | Yes | Must not be empty; must be numeric |
| 5 | Source | TRNSRCI | Text | Yes | Must not be empty |
| 6 | Description | TDESCI | Text | Yes | Must not be empty |
| 7 | Amount | TRNAMTI | Signed Decimal | Yes | Must not be empty; format: +/-99999999.99 |
| 8 | Origination Date | TORIGDTI | Date | Yes | Must not be empty; format: YYYY-MM-DD; must be valid calendar date |
| 9 | Processing Date | TPROCDTI | Date | Yes | Must not be empty; format: YYYY-MM-DD; must be valid calendar date |
| 10 | Merchant ID | MIDI | Numeric | Yes | Must not be empty; must be numeric |
| 11 | Merchant Name | MNAMEI | Text | Yes | Must not be empty |
| 12 | Merchant City | MCITYI | Text | Yes | Must not be empty |
| 13 | Merchant Zip | MZIPI | Text | Yes | Must not be empty |
| 14 | Confirmation | CONFIRMI | Y/N | Yes | Must be 'Y'/'y' to proceed; 'N'/'n' or blank prompts; other value is invalid |

#### 3.3.3 Functional Behavior

| Action | Trigger | Behavior |
|--------|---------|----------|
| Initial Display | Entry from menu or XCTL | Display blank form. If card number pre-selected, auto-populate card number and resolve account |
| Validate & Add | ENTER | Run full validation chain (6 phases). If confirm = 'Y', generate new ID and write record |
| Return to Menu/Caller | PF3 | Return to calling program or main menu (COMEN01C) |
| Clear Form | PF4 | Reset all input fields to blank; position cursor on Account ID |
| Copy Last Transaction | PF5 | Validate key fields, then populate all data fields from the most recent transaction in the file |
| Invalid Key | Any other key | Display 'Invalid Key Pressed' message |

#### 3.3.4 Validation Chain

Validation is performed in six sequential phases. Each phase may halt processing and return control to the screen with an error message and cursor positioned on the offending field.

```
Phase 1: Key Field Validation
├── Account ID provided → Numeric check → CXACAIX lookup → Resolve Card Number
├── Card Number provided → Numeric check → CCXREF lookup → Resolve Account ID
└── Neither provided → Error: "Account or Card Number must be entered..."

Phase 2: Mandatory Field Checks (11 fields)
├── Type Code not empty
├── Category Code not empty
├── Source not empty
├── Description not empty
├── Amount not empty
├── Origination Date not empty
├── Processing Date not empty
├── Merchant ID not empty
├── Merchant Name not empty
├── Merchant City not empty
└── Merchant Zip not empty

Phase 3: Numeric Type Checks
├── Type Code must be numeric
└── Category Code must be numeric

Phase 4: Amount Format Validation
└── Must match pattern: [+/-][8 digits].[2 digits] (e.g., -99999999.99)

Phase 5: Date Validation
├── Origination Date format: YYYY-MM-DD
├── Processing Date format: YYYY-MM-DD
├── Origination Date must be a valid calendar date (via CSUTLDTC utility)
└── Processing Date must be a valid calendar date (via CSUTLDTC utility)

Phase 6: Merchant ID Numeric Check
└── Merchant ID must be numeric
```

#### 3.3.5 Transaction ID Generation

New Transaction IDs are generated using the following algorithm:
1. Start a browse on the TRANSACT file from the highest possible key value (HIGH-VALUES)
2. Read the previous record to obtain the last (highest) existing Transaction ID
3. Increment the ID by 1
4. Use the resulting value as the new Transaction ID

This ensures sequential, monotonically increasing IDs.

#### 3.3.6 Confirmation Mechanism

After all validation passes, the user must confirm the addition:

| Confirm Value | Outcome |
|---------------|---------|
| 'Y' or 'y' | Proceed to write the transaction record |
| 'N', 'n', SPACES, or LOW-VALUES | Prompt message: 'Confirm to add this transaction...' |
| Any other value | Error: 'Invalid value. Valid values are (Y/N)...' |

#### 3.3.7 Write Outcomes

| Outcome | User Feedback |
|---------|--------------|
| Success | Green message: 'Transaction added successfully. Your Tran ID is {ID}.' All fields cleared. |
| Duplicate Key | Error: 'Tran ID already exist...' |
| Other Error | Error: 'Unable to Add Transaction...' |

---

## 4. Business Rules

### 4.1 Cross-Functional Rules

| ID | Rule | Applicable Functions | Description |
|----|------|---------------------|-------------|
| BR-CF-01 | Session Required | List, View, Add | All functions require a valid COMMAREA (active session). If COMMAREA is absent (EIBCALEN = 0), the user is redirected to the sign-on screen (COSGN00C). |
| BR-CF-02 | Pseudo-Conversational Operation | List, View, Add | All functions operate in CICS pseudo-conversational mode: process input, send screen, return to CICS with TRANSID and COMMAREA, and await next user interaction. |
| BR-CF-03 | Invalid Key Handling | List, View, Add | Any unrecognized function key displays an 'Invalid Key Pressed' error message without altering the current screen data. |

### 4.2 List Transaction Rules

| ID | Rule | Condition | Outcome |
|----|------|-----------|---------|
| BR-LT-01 | Page Size Fixed at 10 | Every page load | Exactly 10 transaction rows are displayed per page |
| BR-LT-02 | Numeric Filter Validation | Transaction ID filter is non-empty | Must be numeric; error 'Tran ID must be Numeric ...' if not |
| BR-LT-03 | Valid Selection Value | Selection field is non-empty | Only 'S'/'s' is accepted; error 'Invalid selection. Valid value is S' for other values |
| BR-LT-04 | Empty Filter Browses from Start | Filter field is empty | Listing starts from the first record in the TRANSACT file |
| BR-LT-05 | Forward Pagination Boundary | No more records after current page | PF8 displays 'You are already at the bottom of the page...' |
| BR-LT-06 | Backward Pagination Boundary | Already on page 1 | PF7 displays 'You are already at the top of the page...' |
| BR-LT-07 | Page State Preservation | Every page load | First/last Transaction IDs, page number, and next-page flag are stored in COMMAREA |
| BR-LT-08 | Selection Triggers Detail View | 'S' entered on a row | XCTL to COTRN01C with selected Transaction ID in COMMAREA |

### 4.3 View Transaction Rules

| ID | Rule | Condition | Outcome |
|----|------|-----------|---------|
| BR-VT-01 | Transaction ID Required | User presses ENTER | Transaction ID input cannot be empty; error 'Tran ID can NOT be empty...' |
| BR-VT-02 | Transaction Must Exist | CICS READ performed | Must find record; error 'Transaction ID NOT found...' if not found |
| BR-VT-03 | Pre-Selected Auto-Load | Entry from COTRN00C with selection | Transaction is automatically loaded without requiring ENTER |
| BR-VT-04 | Read-Only Display | All interactions | No update or delete operations available; display only |
| BR-VT-05 | PF5 Returns to List | User presses PF5 | Returns to COTRN00C with COMMAREA preserved |

### 4.4 Add Transaction Rules

| ID | Rule | Condition | Outcome |
|----|------|-----------|---------|
| BR-AT-01 | Account or Card Required | ENTER pressed | At least one of Account ID or Card Number must be provided |
| BR-AT-02 | Account ID Numeric | Account ID is provided | Must be numeric; error 'Account ID must be Numeric...' |
| BR-AT-03 | Card Number Numeric | Card Number is provided | Must be numeric; error 'Card Number must be Numeric...' |
| BR-AT-04 | Account/Card Must Exist | Key field provided | Must exist in cross-reference file; error 'Account ID NOT found...' or 'Card Number NOT found...' |
| BR-AT-05 | Cross-Reference Resolution | Valid Account ID or Card Number | System auto-resolves the other field. Account ID → Card Number via CXACAIX; Card Number → Account ID via CCXREF |
| BR-AT-06 | All 11 Data Fields Mandatory | ENTER pressed | Type Code, Category Code, Source, Description, Amount, Orig Date, Proc Date, Merchant ID, Name, City, Zip must all be non-empty |
| BR-AT-07 | Type/Category Must Be Numeric | Fields are non-empty | Type Code and Category Code must be numeric values |
| BR-AT-08 | Amount Format Required | Amount is non-empty | Must match +/-99999999.99 (sign + 8 digits + decimal + 2 digits) |
| BR-AT-09 | Date Format Required | Date fields are non-empty | Must match YYYY-MM-DD format |
| BR-AT-10 | Date Validity Required | Date format is correct | Must be a valid calendar date per CSUTLDTC utility |
| BR-AT-11 | Merchant ID Numeric | Merchant ID is non-empty | Must be numeric |
| BR-AT-12 | Explicit Confirmation | All validation passes | User must enter 'Y'/'y' to confirm; 'N'/'n'/blank prompts; other values are rejected |
| BR-AT-13 | Auto-Increment Transaction ID | Transaction is being added | New ID = highest existing ID + 1 |
| BR-AT-14 | Duplicate ID Rejection | CICS WRITE returns DUPKEY/DUPREC | Error 'Tran ID already exist...' |

---

## 5. User Stories

### 5.1 Transaction Listing

| ID | User Story | Acceptance Criteria |
|----|-----------|-------------------|
| US-LT-01 | As a user, I want to view a paginated list of all transactions so that I can review transaction activity. | First 10 transactions displayed on entry; each row shows Transaction ID, Date, Description, Amount; page number visible. |
| US-LT-02 | As a user, I want to page forward (PF8) through transactions so that I can view additional records. | Next 10 transactions load; page number increments; boundary message at end of data. |
| US-LT-03 | As a user, I want to page backward (PF7) through transactions so that I can return to earlier records. | Previous 10 transactions load; page number decrements; boundary message at page 1. |
| US-LT-04 | As a user, I want to filter by a starting Transaction ID so that I can jump to a specific range. | Entering a numeric ID restarts the list from that point; non-numeric values are rejected. |
| US-LT-05 | As a user, I want to select a transaction with 'S' so that I can view its full details. | Selected transaction opens in the View Transaction screen (COTRN01C). |
| US-LT-06 | As a user, I want to press PF3 to return to the main menu so that I can access other functions. | Control returns to COMEN01C. |

### 5.2 Transaction Viewing

| ID | User Story | Acceptance Criteria |
|----|-----------|-------------------|
| US-VT-01 | As a user, I want to select a transaction from the list and automatically see its details so that I don't have to re-enter the ID. | Arriving from COTRN00C with a selection auto-loads all 13 fields. |
| US-VT-02 | As a user, I want to enter a Transaction ID manually and view its details so that I can look up specific transactions. | Transaction found → all fields displayed; not found → error message; empty → error message. |
| US-VT-03 | As a user, I want to press PF4 to clear the screen so that I can look up a different transaction. | All fields cleared; cursor on Transaction ID input. |
| US-VT-04 | As a user, I want to press PF5 to return to the transaction list so that I can continue browsing. | Control returns to COTRN00C with pagination state preserved. |
| US-VT-05 | As a user, I want to press PF3 to return to the calling screen or main menu so that I can navigate elsewhere. | Returns to calling program or COMEN01C. |

### 5.3 Transaction Addition

| ID | User Story | Acceptance Criteria |
|----|-----------|-------------------|
| US-AT-01 | As a user, I want to add a transaction by entering an Account ID so that the system resolves the card number automatically. | Account ID entered → Card Number resolved from CXACAIX; both fields populated on screen. |
| US-AT-02 | As a user, I want to add a transaction by entering a Card Number so that the system resolves the account automatically. | Card Number entered → Account ID resolved from CCXREF; both fields populated on screen. |
| US-AT-03 | As a user, I want all my input validated with specific error messages so that I know exactly what to fix. | Each validation failure produces a field-specific message and positions the cursor on the error field. |
| US-AT-04 | As a user, I want to confirm with 'Y' before the transaction is saved so that I can review my entries first. | 'Y' → write; 'N'/blank → prompt; invalid → error. |
| US-AT-05 | As a user, I want to see the auto-generated Transaction ID after a successful add so that I have a reference. | Success message: "Transaction added successfully. Your Tran ID is {ID}." in green. |
| US-AT-06 | As a user, I want to press PF5 to copy the last transaction's data so that I can quickly add similar transactions. | All data fields populated from the most recent transaction; key fields still validated. |
| US-AT-07 | As a user, I want to press PF4 to clear the form so that I can start a fresh entry. | All fields cleared to blank; cursor on Account ID. |
| US-AT-08 | As a user, I want to press PF3 to return to the menu without adding so that I can navigate away. | Returns to calling program or COMEN01C without writing. |

---

## 6. Data Requirements

### 6.1 Transaction Record Structure

The transaction record is defined in copybook `CVTRA05Y.cpy` with a total length of 350 bytes. Key fields used by this module:

| Field | PIC Clause | Business Meaning |
|-------|-----------|-----------------|
| TRAN-ID | X(16) | Unique transaction identifier (primary key) |
| TRAN-CARD-NUM | X(16) | Associated credit card number |
| TRAN-TYPE-CD | X(02) | Transaction type code (numeric) |
| TRAN-CAT-CD | X(04) | Transaction category code (numeric) |
| TRAN-SOURCE | X(10) | Transaction origination source |
| TRAN-DESC | X(100) | Transaction description |
| TRAN-AMT | S9(09)V99 | Transaction amount (signed, 2 decimal places) |
| TRAN-ORIG-TS | X(26) | Origination timestamp |
| TRAN-PROC-TS | X(26) | Processing timestamp |
| TRAN-MERCHANT-ID | 9(09) | Merchant identifier (numeric) |
| TRAN-MERCHANT-NAME | X(50) | Merchant business name |
| TRAN-MERCHANT-CITY | X(50) | Merchant city |
| TRAN-MERCHANT-ZIP | X(10) | Merchant postal code |

### 6.2 VSAM File Access Patterns

| File | Key | Programs | Operations |
|------|-----|----------|------------|
| TRANSACT (KSDS) | TRAN-ID (16 bytes) | COTRN00C, COTRN01C, COTRN02C | STARTBR, READNEXT, READPREV, ENDBR, READ, WRITE |
| CXACAIX (AIX) | XREF-ACCT-ID | COTRN02C | READ (resolve Account ID → Card Number) |
| CCXREF (KSDS) | XREF-CARD-NUM | COTRN02C | READ (resolve Card Number → Account ID) |

### 6.3 Cross-Reference Resolution

The Add Transaction function uses two cross-reference paths to establish the account-to-card relationship:

```
Path A: Account ID → Card Number
   User enters Account ID
   → READ CXACAIX (alternate index on CARDXREF by XREF-ACCT-ID)
   → Returns CARD-XREF-RECORD
   → Extract XREF-CARD-NUM → populate Card Number on screen

Path B: Card Number → Account ID
   User enters Card Number
   → READ CCXREF (primary index on CARDXREF by XREF-CARD-NUM)
   → Returns CARD-XREF-RECORD
   → Extract XREF-ACCT-ID → populate Account ID on screen
```

---

## 7. Navigation and Screen Flow

### 7.1 Module Navigation Map

```
    ┌──────────────────────────────────────────┐
    │             COMEN01C (Main Menu)          │
    │                                            │
    │   [Option 4]              [Option 8]       │
    └──────┬───────────────────────┬────────────┘
           │                       │
     XCTL  │                 XCTL  │
           ▼                       ▼
    ┌──────────────┐      ┌──────────────┐
    │  COTRN00C    │      │  COTRN02C    │
    │  List Trans  │      │  Add Trans   │
    │  (CT00)      │      │  (CT02)      │
    └──────┬───────┘      └──────────────┘
           │                     ▲
     XCTL  │ (Select 'S')       │ PF3 (return)
           ▼                     │
    ┌──────────────┐             │
    │  COTRN01C    │─────────────┘ (if called from menu)
    │  View Trans  │
    │  (CT01)      │
    └──────┬───────┘
           │ PF5
           ▼
    Back to COTRN00C (List)
```

### 7.2 Function Key Map

| Key | List (CT00) | View (CT01) | Add (CT02) |
|-----|-------------|-------------|------------|
| ENTER | Process selection / filter / refresh | Lookup Transaction ID | Validate & add (with confirmation) |
| PF3 | Return to main menu | Return to caller / menu | Return to caller / menu |
| PF4 | — | Clear all fields | Clear all fields |
| PF5 | — | Return to list (COTRN00C) | Copy last transaction data |
| PF7 | Page backward | — | — |
| PF8 | Page forward | — | — |
| Other | Invalid key error | Invalid key error | Invalid key error |

---

## 8. Error Handling

### 8.1 User Input Errors

All input errors follow a consistent pattern:
1. Set error flag (`WS-ERR-FLG = 'Y'`)
2. Set a specific, descriptive error message in the message area
3. Position cursor on the offending field (via `MOVE -1 TO field-length`)
4. Re-send the screen with the error message displayed

### 8.2 Error Message Catalog

#### List Transactions (COTRN00C)

| Error | Message | Trigger |
|-------|---------|---------|
| Non-numeric filter | 'Tran ID must be Numeric ...' | Filter field contains non-numeric data |
| Invalid selection | 'Invalid selection. Valid value is S' | Selection field contains value other than 'S' |
| At top | 'You are already at the top of the page...' | PF7 on page 1 |
| At bottom | 'You are already at the bottom of the page...' | PF8 when no more records exist |
| File error | 'Unable to lookup transaction...' | Unexpected CICS response from TRANSACT file |

#### View Transaction (COTRN01C)

| Error | Message | Trigger |
|-------|---------|---------|
| Empty ID | 'Tran ID can NOT be empty...' | ENTER with empty Transaction ID |
| Not found | 'Transaction ID NOT found...' | Transaction ID does not exist in TRANSACT file |
| File error | 'Unable to lookup Transaction...' | Unexpected CICS response |

#### Add Transaction (COTRN02C)

| Error | Message | Trigger |
|-------|---------|---------|
| No key field | 'Account or Card Number must be entered...' | Both Account ID and Card Number are empty |
| Non-numeric account | 'Account ID must be Numeric...' | Account ID contains non-numeric data |
| Non-numeric card | 'Card Number must be Numeric...' | Card Number contains non-numeric data |
| Account not found | 'Account ID NOT found...' | Account ID not in CXACAIX |
| Card not found | 'Card Number NOT found...' | Card Number not in CCXREF |
| Empty type | 'Type CD can NOT be empty...' | Type Code field is empty |
| Empty category | 'Category CD can NOT be empty...' | Category Code field is empty |
| Empty source | 'Source can NOT be empty...' | Source field is empty |
| Empty description | 'Description can NOT be empty...' | Description field is empty |
| Empty amount | 'Amount can NOT be empty...' | Amount field is empty |
| Empty orig date | 'Orig Date can NOT be empty...' | Origination Date is empty |
| Empty proc date | 'Proc Date can NOT be empty...' | Processing Date is empty |
| Empty merchant ID | 'Merchant ID can NOT be empty...' | Merchant ID is empty |
| Empty merchant name | 'Merchant Name can NOT be empty...' | Merchant Name is empty |
| Empty merchant city | 'Merchant City can NOT be empty...' | Merchant City is empty |
| Empty merchant zip | 'Merchant Zip can NOT be empty...' | Merchant Zip is empty |
| Non-numeric type | 'Type CD must be Numeric...' | Type Code not numeric |
| Non-numeric category | 'Category CD must be Numeric...' | Category Code not numeric |
| Bad amount format | 'Amount should be in format -99999999.99' | Amount format mismatch |
| Bad orig date format | 'Orig Date should be in format YYYY-MM-DD' | Date format mismatch |
| Bad proc date format | 'Proc Date should be in format YYYY-MM-DD' | Date format mismatch |
| Invalid orig date | 'Orig Date - Not a valid date...' | CSUTLDTC validation failure |
| Invalid proc date | 'Proc Date - Not a valid date...' | CSUTLDTC validation failure |
| Non-numeric merchant ID | 'Merchant ID must be Numeric...' | Merchant ID not numeric |
| Invalid confirm | 'Invalid value. Valid values are (Y/N)...' | Confirmation value not Y/N/blank |
| Duplicate ID | 'Tran ID already exist...' | Generated ID already exists |
| Write error | 'Unable to Add Transaction...' | Unexpected CICS WRITE error |
| XREF lookup error | 'Unable to lookup Acct in XREF AIX file...' / 'Unable to lookup Card # in XREF file...' | Unexpected CICS READ error on cross-reference |

### 8.3 CICS File Error Handling

All file operations check `WS-RESP-CD` against standard CICS response codes:
- **DFHRESP(NORMAL):** Success — continue processing
- **DFHRESP(NOTFND):** Record not found — display user-facing error
- **DFHRESP(ENDFILE):** End of file during browse — set EOF flag, send boundary message
- **DFHRESP(DUPKEY/DUPREC):** Duplicate on write — display duplicate error
- **Other:** Unexpected error — display RESP/REAS codes to console and show generic error to user

---

## 9. External Dependencies

### 9.1 Utility Programs

| Program | Type | Used By | Purpose |
|---------|------|---------|---------|
| CSUTLDTC | COBOL CALL | COTRN02C (Add) | Date validation utility. Accepts a date string and format, returns severity code and message number. Severity '0000' indicates valid date. |

### 9.2 Calling Programs

| Program | Relationship | Mechanism |
|---------|-------------|-----------|
| COMEN01C | Entry point for List (Option 4) and Add (Option 8) | XCTL with COMMAREA |
| COTRN00C | Entry point for View (selection 'S') | XCTL with selected Transaction ID in COMMAREA |

### 9.3 Return Targets

| From | Default Return | Fallback |
|------|---------------|----------|
| COTRN00C (List) | COMEN01C | COSGN00C (if no COMMAREA) |
| COTRN01C (View) | Calling program (CDEMO-FROM-PROGRAM) | COMEN01C (if not set) |
| COTRN02C (Add) | Calling program (CDEMO-FROM-PROGRAM) | COSGN00C (if not set) |

---

## 10. Non-Functional Observations

### 10.1 Concurrency

- **Add Transaction (COTRN02C):** Transaction ID generation uses a browse-to-end approach (STARTBR at HIGH-VALUES, READPREV, increment by 1). In a concurrent environment, two users could potentially generate the same ID simultaneously. The CICS WRITE with DUPKEY/DUPREC handling provides a safety net, but does not include a retry mechanism — the user would receive an error and need to retry.

- **View Transaction (COTRN01C):** The READ operation uses the UPDATE option, which acquires an exclusive record lock. Since COTRN01C is a read-only screen with no REWRITE, this lock is unnecessary and could cause contention if multiple users view the same transaction simultaneously.

### 10.2 Data Integrity

- The Add function validates that the Account ID or Card Number exists in the cross-reference file before allowing a transaction to be created, ensuring referential integrity between transactions and accounts/cards.
- All 11 data fields are mandatory, preventing incomplete transaction records.
- Date validity is enforced via an external utility (CSUTLDTC), not just format checking.

### 10.3 Usability

- The "Copy Last Transaction" (PF5) feature in Add Transaction reduces data entry effort for repetitive transactions.
- The auto-load feature in View Transaction (when arriving from List) eliminates a redundant lookup step.
- Error messages are field-specific with cursor positioning, guiding users directly to the problem area.
- The List screen provides boundary messages ("top of page" / "bottom of page") rather than silently doing nothing at pagination limits.

---

## 11. Traceability Matrix

### Programs to Functions

| Program | TRANID | Function | Lines of Code | Paragraphs |
|---------|--------|----------|---------------|------------|
| COTRN00C | CT00 | List Transactions | 700 | 16 |
| COTRN01C | CT01 | View Transaction | 331 | 9 |
| COTRN02C | CT02 | Add Transaction | 784 | 17 |
| **Total** | | | **1,815** | **42** |

### Business Rules to Programs

| Rule Category | Count | Program |
|--------------|-------|---------|
| Cross-Functional | 3 | All |
| List Transaction | 8 | COTRN00C |
| View Transaction | 5 | COTRN01C |
| Add Transaction | 14 | COTRN02C |
| **Total** | **30** | |

### User Stories to Functions

| Function | User Stories | Key Scenarios |
|----------|-------------|--------------|
| List Transactions | 6 | Browse, page forward, page backward, filter, select, return |
| View Transaction | 5 | Auto-load from list, manual lookup, clear, return to list, return to menu |
| Add Transaction | 8 | Add by account, add by card, validation feedback, confirm, success with ID, copy last, clear, return |
| **Total** | **19** | |

---

## Appendix A: Glossary

| Term | Definition |
|------|-----------|
| **COMMAREA** | Communication Area — a data structure passed between CICS programs to maintain session state across pseudo-conversational interactions |
| **VSAM KSDS** | Virtual Storage Access Method, Key-Sequenced Data Set — primary data storage mechanism indexed by a primary key |
| **AIX** | Alternate Index — a secondary access path to a VSAM file using a different key field |
| **XCTL** | CICS command to transfer control to another program, passing the COMMAREA |
| **BMS Map** | Basic Mapping Support — defines the layout of 3270 terminal screens |
| **TRANID** | Transaction Identifier — a 4-character CICS transaction code that identifies the program to invoke |
| **Pseudo-conversational** | A CICS programming pattern where the program processes input, sends a response, and returns to CICS (freeing resources) rather than waiting in a loop for the next user input |
| **STARTBR / READNEXT / READPREV / ENDBR** | CICS browse operations for sequentially reading through VSAM records |
| **DFHRESP** | CICS macro for checking response codes from CICS commands |
| **CSUTLDTC** | CardDemo shared utility program for date validation |

## Appendix B: Source Document References

| Document | Location | Content |
|----------|----------|---------|
| Add Transaction Program Analysis | `devindoc/add-transactions-program-doc-devin.md` | COTRN02C detailed program documentation |
| List Transaction Program Analysis | `devindoc/list-transactions-program-doc-devin.md` | COTRN00C detailed program documentation |
| View Transaction Program Analysis | `devindoc/view-transactions-program-doc-devin.md` | COTRN01C detailed program documentation |
