# COTRN02C — Add Transaction Program Documentation

> **Program:** COTRN02C.cbl
> **Application:** CardDemo
> **Type:** CICS COBOL Online Program
> **Function:** Add a new Transaction to TRANSACT file
> **TRANID:** CT02
> **Source:** `app/cbl/COTRN02C.cbl` (784 lines)

---

## 1. Overview

COTRN02C is a CICS online program that allows users to add new financial transactions to the TRANSACT VSAM file. It provides a 3270 terminal screen for entering transaction details, validates all input fields (key fields and data fields), resolves card-to-account relationships via cross-reference files, auto-generates a unique transaction ID, and writes the new record to the TRANSACT file upon user confirmation.

### Flow Diagram

```
                    ┌─────────────────────┐
                    │   CICS ENTRY (CT02) │
                    │   or XCTL from menu │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │  EIBCALEN = 0?      │
                    │  (No COMMAREA)      │
                    └──────┬───────┬──────┘
                     YES   │       │  NO
              ┌────────────▼┐  ┌───▼──────────────────┐
              │ RETURN TO   │  │ First entry?          │
              │ COSGN00C    │  │ (NOT CDEMO-PGM-REENTER)│
              └─────────────┘  └───┬───────────┬──────┘
                                YES│           │NO
                    ┌──────────────▼┐  ┌───────▼──────────┐
                    │ Initialize    │  │ RECEIVE screen    │
                    │ screen        │  │ EVALUATE EIBAID   │
                    │ Pre-populate  │  └───┬───┬───┬───┬───┘
                    │ if card sel'd │      │   │   │   │
                    │ SEND screen   │  ENTER PF3 PF4 PF5 OTHER
                    └───────────────┘    │   │   │   │    │
                                         │   │   │   │    │
                          ┌──────────────▼┐  │   │   │  ┌─▼───────────┐
                          │PROCESS-ENTER  │  │   │   │  │Invalid key  │
                          │-KEY           │  │   │   │  │message      │
                          └──────┬────────┘  │   │   │  └─────────────┘
                                 │           │   │   │
                    ┌────────────▼─────┐     │   │   │
                    │VALIDATE-INPUT    │     │   │   │
                    │-KEY-FIELDS       │     │   │   │
                    │(Acct ID or Card#)│     │   │   │
                    └────────┬─────────┘     │   │   │
                             │               │   │   │
                    ┌────────▼─────────┐     │   │   │
                    │VALIDATE-INPUT    │     │   │   │
                    │-DATA-FIELDS      │     │   │   │
                    │(11 fields)       │     │   │   │
                    └────────┬─────────┘     │   │   │
                             │               │   │   │
                    ┌────────▼─────────┐     │   │   │
                    │Confirm = 'Y'?    │  ┌──▼┐ ┌▼┐ ┌▼──────────┐
                    └───┬──────────┬───┘  │RTN│ │CL│ │COPY-LAST  │
                    YES │          │ NO   │PRV│ │EA│ │-TRAN-DATA │
               ┌────────▼───┐  ┌──▼────┐ │SCR│ │R │ │(PF5)      │
               │ADD-         │  │Prompt │ └───┘ └──┘ └───────────┘
               │TRANSACTION  │  │confirm│
               └──────┬──────┘  └───────┘
                      │
          ┌───────────▼───────────┐
          │ STARTBR HIGH-VALUES   │
          │ READPREV (get last ID)│
          │ ENDBR                 │
          │ New ID = Last ID + 1  │
          └───────────┬───────────┘
                      │
          ┌───────────▼───────────┐
          │ Build TRAN-RECORD     │
          │ from screen fields    │
          └───────────┬───────────┘
                      │
          ┌───────────▼───────────┐
          │ WRITE-TRANSACT-FILE   │
          │ (EXEC CICS WRITE)     │
          └───────────┬───────────┘
                      │
          ┌───────────▼───────────┐
          │ Success message with  │
          │ new Transaction ID    │
          │ "Your Tran ID is ..." │
          └───────────────────────┘
```

---

## 2. Dependencies

### 2.1 Copybooks

| Copybook | Purpose | Reference |
|----------|---------|-----------|
| `COCOM01Y.cpy` | COMMAREA structure shared across all CardDemo programs | Line 71 |
| `COTRN02.cpy` | BMS map I/O area definitions for COTRN2AI/COTRN2AO | Line 82 |
| `COTTL01Y.cpy` | Screen title constants (CCDA-TITLE01, CCDA-TITLE02) | Line 84 |
| `CSDAT01Y.cpy` | Date/time formatting fields (WS-CURDATE, WS-CURTIME, WS-TIMESTAMP) | Line 85 |
| `CSMSG01Y.cpy` | Common message constants (CCDA-MSG-INVALID-KEY, CCDA-MSG-THANK-YOU) | Line 86 |
| `CVTRA05Y.cpy` | Transaction record layout (TRAN-RECORD, 350 bytes) | Line 88 |
| `CVACT01Y.cpy` | Account record layout (ACCOUNT-RECORD, 300 bytes) | Line 89 |
| `CVACT03Y.cpy` | Card cross-reference record layout (CARD-XREF-RECORD, 50 bytes) | Line 90 |
| `DFHAID.cpy` | CICS AID key definitions (DFHENTER, DFHPF3, etc.) | Line 92 |
| `DFHBMSCA.cpy` | BMS attribute constants (DFHGREEN, etc.) | Line 93 |

### 2.2 VSAM Files Accessed

| File DD Name | Access Mode | Purpose | Code Reference |
|-------------|-------------|---------|----------------|
| `TRANSACT` | STARTBR, READPREV, ENDBR, WRITE | Read last transaction for ID generation; write new transaction | Lines 39, 644-706, 713-749 |
| `CXACAIX` | READ (by Account ID) | Resolve Account ID to Card Number via alternate index | Lines 42, 578-604 |
| `CCXREF` | READ (by Card Number) | Resolve Card Number to Account ID via primary index | Lines 41, 611-637 |

### 2.3 Called Programs

| Program | Call Type | Purpose | Code Reference |
|---------|-----------|---------|----------------|
| `CSUTLDTC` | COBOL CALL | Date validation utility — validates YYYY-MM-DD format dates | Lines 393-395 (Orig Date), Lines 413-415 (Proc Date) |

### 2.4 BMS Map

| Map | Mapset | Purpose |
|-----|--------|---------|
| `COTRN2A` | `COTRN02` | Add Transaction screen layout (input/output) |

### 2.5 Programs That Call COTRN02C

| Caller | Mechanism | Code Evidence |
|--------|-----------|---------------|
| `COMEN01C.cbl` | XCTL via menu option 8 ("Transaction Add") | `COMEN02Y.cpy` line 73 |

---

## 3. Detailed Functionality

### 3.1 Main Entry Logic (MAIN-PARA, lines 107-159)

1. **No COMMAREA (EIBCALEN = 0):** Returns to sign-on screen `COSGN00C` via `RETURN-TO-PREV-SCREEN` (line 115-117).
2. **First entry (NOT CDEMO-PGM-REENTER):** Initializes screen to LOW-VALUES, sets cursor on Account ID field. If a card number was pre-selected from another program (`CDEMO-CT02-TRN-SELECTED`), it pre-populates the card number and processes it (lines 124-128). Sends the add transaction screen.
3. **Re-entry (CDEMO-PGM-REENTER):** Receives screen input and evaluates the AID key pressed:
   - **ENTER:** Calls `PROCESS-ENTER-KEY` to validate and add (line 135)
   - **PF3:** Returns to calling program or main menu `COMEN01C` (lines 136-143)
   - **PF4:** Clears all screen fields via `CLEAR-CURRENT-SCREEN` (line 145)
   - **PF5:** Copies data from the last transaction via `COPY-LAST-TRAN-DATA` (line 147)
   - **Other:** Displays 'Invalid Key Pressed' message (lines 148-151)
4. **CICS RETURN:** Returns to CICS with TRANSID `CT02` and COMMAREA for pseudo-conversational operation (lines 156-159).

### 3.2 Key Field Validation (VALIDATE-INPUT-KEY-FIELDS, lines 193-230)

Three-way EVALUATE:
1. **Account ID provided:** Validates numeric. Converts to numeric value. Looks up CXACAIX (alternate index on CARDXREF by account ID) to resolve to a card number. Sets both Account ID and Card Number on screen (lines 196-209).
2. **Card Number provided:** Validates numeric. Looks up CCXREF (primary index on CARDXREF by card number) to resolve to an account ID. Sets both fields on screen (lines 210-223).
3. **Neither provided:** Error — 'Account or Card Number must be entered...' (lines 224-229).

### 3.3 Data Field Validation (VALIDATE-INPUT-DATA-FIELDS, lines 235-437)

**Phase 1 — Empty field checks (lines 251-320):**
All 11 data fields are checked for empty (SPACES or LOW-VALUES):
- Type Code (TTYPCDI)
- Category Code (TCATCDI)
- Source (TRNSRCI)
- Description (TDESCI)
- Amount (TRNAMTI)
- Origination Date (TORIGDTI)
- Processing Date (TPROCDTI)
- Merchant ID (MIDI)
- Merchant Name (MNAMEI)
- Merchant City (MCITYI)
- Merchant Zip (MZIPI)

**Phase 2 — Numeric checks (lines 322-337):**
- Type Code must be numeric
- Category Code must be numeric

**Phase 3 — Amount format validation (lines 339-351):**
Amount must match format `-99999999.99` (sign + 8 digits + decimal point + 2 digits).

**Phase 4 — Date format validation (lines 353-381):**
Both Origination Date and Processing Date must match `YYYY-MM-DD` format (4 digits, hyphen, 2 digits, hyphen, 2 digits).

**Phase 5 — Date validity (lines 389-427):**
Both dates are validated using the `CSUTLDTC` utility program. Checks that dates are actual valid calendar dates (not just format). Message number 2513 is excluded from errors (likely a warning).

**Phase 6 — Merchant ID numeric check (lines 430-436):**
Merchant ID must be numeric.

### 3.4 Transaction ID Generation (ADD-TRANSACTION, lines 442-466)

1. Sets `TRAN-ID` to `HIGH-VALUES` (maximum key value)
2. Starts browse on TRANSACT file at the end (`STARTBR`)
3. Reads the previous record (`READPREV`) — this retrieves the last transaction by ID
4. Ends browse (`ENDBR`)
5. Converts the last transaction ID to numeric, adds 1 to generate the new ID
6. Initializes a new `TRAN-RECORD` and populates all fields from screen input
7. Calls `WRITE-TRANSACT-FILE` to persist the record

### 3.5 Copy Last Transaction (COPY-LAST-TRAN-DATA, lines 471-495)

Triggered by PF5. Validates key fields first, then:
1. Browses to the last transaction in the file
2. Copies all data fields (type, category, source, amount, description, dates, merchant info) from the last transaction into the screen input fields
3. Calls `PROCESS-ENTER-KEY` to continue validation flow

### 3.6 Write Transaction (WRITE-TRANSACT-FILE, lines 711-749)

Uses `EXEC CICS WRITE` to write the new record. Handles three outcomes:
- **NORMAL:** Clears all fields, displays success message in green: "Transaction added successfully. Your Tran ID is {ID}." (lines 724-734)
- **DUPKEY/DUPREC:** Displays 'Tran ID already exist...' (lines 735-741)
- **OTHER:** Displays 'Unable to Add Transaction...' (lines 742-748)

### 3.7 Cross-Reference Lookups

**READ-CXACAIX-FILE (lines 576-604):** Reads CARDXREF via alternate index (CXACAIX) using Account ID as key. Returns card number for the account.

**READ-CCXREF-FILE (lines 609-637):** Reads CARDXREF via primary index (CCXREF) using Card Number as key. Returns account ID for the card.

Both handle NOTFND (account/card not found) and other errors.

---

## 4. Summary

COTRN02C is a fully interactive CICS transaction entry program that:
- Accepts either Account ID or Card Number and resolves the other via cross-reference lookup
- Validates 11 mandatory data fields with type checks, format checks, and date validity via external utility
- Auto-generates sequential transaction IDs by reading the last record in the file
- Requires explicit 'Y' confirmation before writing
- Provides a "copy last transaction" feature (PF5) for efficient repeat data entry
- Operates pseudo-conversationally with COMMAREA state preservation

**Lines of code:** 784
**Paragraphs/Sections:** 17 (MAIN-PARA, PROCESS-ENTER-KEY, VALIDATE-INPUT-KEY-FIELDS, VALIDATE-INPUT-DATA-FIELDS, ADD-TRANSACTION, COPY-LAST-TRAN-DATA, RETURN-TO-PREV-SCREEN, SEND-TRNADD-SCREEN, RECEIVE-TRNADD-SCREEN, POPULATE-HEADER-INFO, READ-CXACAIX-FILE, READ-CCXREF-FILE, STARTBR-TRANSACT-FILE, READPREV-TRANSACT-FILE, ENDBR-TRANSACT-FILE, WRITE-TRANSACT-FILE, CLEAR-CURRENT-SCREEN, INITIALIZE-ALL-FIELDS)

---

## 5. Business Rules

### BR-01: Account or Card Number Required
Either an Account ID or Card Number must be provided to add a transaction. The system resolves the other field automatically via cross-reference.
- **Condition:** Both Account ID and Card Number are empty
- **Outcome:** Error — 'Account or Card Number must be entered...'
- **Code:** Lines 224-229

### BR-02: Account ID Must Be Numeric
If Account ID is provided, it must be a numeric value.
- **Condition:** Account ID is not numeric
- **Outcome:** Error — 'Account ID must be Numeric...'
- **Code:** Lines 197-203

### BR-03: Card Number Must Be Numeric
If Card Number is provided, it must be a numeric value.
- **Condition:** Card Number is not numeric
- **Outcome:** Error — 'Card Number must be Numeric...'
- **Code:** Lines 211-217

### BR-04: Account/Card Must Exist in Cross-Reference
The provided Account ID or Card Number must exist in the CARDXREF file.
- **Condition:** CXACAIX or CCXREF lookup returns NOTFND
- **Outcome:** Error — 'Account ID NOT found...' or 'Card Number NOT found...'
- **Code:** Lines 591-596 (account), Lines 624-629 (card)

### BR-05: All 11 Data Fields Are Mandatory
Type Code, Category Code, Source, Description, Amount, Orig Date, Proc Date, Merchant ID, Merchant Name, Merchant City, and Merchant Zip must all be non-empty.
- **Condition:** Any field is SPACES or LOW-VALUES
- **Outcome:** Field-specific error message (e.g., 'Type CD can NOT be empty...')
- **Code:** Lines 251-317

### BR-06: Type Code and Category Code Must Be Numeric
Both the transaction type code and category code must be numeric values.
- **Condition:** Field is not numeric
- **Outcome:** Error — 'Type CD must be Numeric...' or 'Category CD must be Numeric...'
- **Code:** Lines 322-337

### BR-07: Amount Must Follow Signed Decimal Format
Amount must match format `+/-99999999.99` (sign character, 8 digits, decimal point, 2 digits).
- **Condition:** Amount does not match format
- **Outcome:** Error — 'Amount should be in format -99999999.99'
- **Code:** Lines 339-351

### BR-08: Dates Must Be Valid YYYY-MM-DD
Both Origination Date and Processing Date must conform to YYYY-MM-DD format and be valid calendar dates.
- **Condition:** Format mismatch or invalid date per CSUTLDTC utility
- **Outcome:** Error — 'Orig Date should be in format YYYY-MM-DD' or 'Not a valid date...'
- **Code:** Lines 353-427

### BR-09: Merchant ID Must Be Numeric
Merchant ID must be a numeric value.
- **Condition:** Merchant ID is not numeric
- **Outcome:** Error — 'Merchant ID must be Numeric...'
- **Code:** Lines 430-436

### BR-10: Explicit Confirmation Required
Transaction is only written when user confirms with 'Y' or 'y'. Only Y/N are valid confirmation values.
- **Condition:** Confirm field is not Y/y
- **Outcome:** Prompt 'Confirm to add this transaction...' or error 'Invalid value. Valid values are (Y/N)...'
- **Code:** Lines 169-188

### BR-11: Transaction ID Auto-Incremented
New transaction IDs are generated by reading the last (highest) transaction ID and adding 1.
- **Condition:** Transaction is being added
- **Outcome:** New ID = Last existing ID + 1
- **Code:** Lines 444-451

### BR-12: Duplicate Transaction ID Rejected
If the auto-generated ID already exists (race condition), the write is rejected.
- **Condition:** CICS WRITE returns DUPKEY or DUPREC
- **Outcome:** Error — 'Tran ID already exist...'
- **Code:** Lines 735-741

---

## 6. User Stories

### US-01: Add a Transaction by Account ID
**As a** CardDemo user,
**I want to** enter an Account ID and transaction details,
**So that** a new transaction is recorded against that account.

**Acceptance Criteria:**
- I enter an Account ID; the system resolves the Card Number via CXACAIX cross-reference (line 208)
- I fill in all transaction fields (type, category, source, description, amount, dates, merchant info)
- I enter 'Y' to confirm
- The system creates a new transaction with an auto-generated ID and displays "Transaction added successfully. Your Tran ID is {ID}." (lines 728-733)

### US-02: Add a Transaction by Card Number
**As a** CardDemo user,
**I want to** enter a Card Number and transaction details,
**So that** a new transaction is recorded against the associated account.

**Acceptance Criteria:**
- I enter a Card Number; the system resolves the Account ID via CCXREF cross-reference (line 222)
- All other criteria same as US-01

### US-03: Copy Previous Transaction Data
**As a** CardDemo user,
**I want to** press PF5 to copy the last transaction's data into the current form,
**So that** I can quickly add similar transactions without re-typing all fields.

**Acceptance Criteria:**
- I provide an Account ID or Card Number (key field validation runs first, line 473)
- I press PF5
- All data fields (type, category, source, amount, description, dates, merchant) are populated from the most recent transaction (lines 481-492)
- I can modify any fields before confirming

### US-04: Clear the Add Transaction Form
**As a** CardDemo user,
**I want to** press PF4 to clear all fields on the screen,
**So that** I can start a fresh transaction entry.

**Acceptance Criteria:**
- All input fields are set to SPACES (lines 764-778)
- Cursor returns to Account ID field (line 764)

### US-05: Return to Previous Screen
**As a** CardDemo user,
**I want to** press PF3 to return to the menu or calling program,
**So that** I can navigate away without adding a transaction.

**Acceptance Criteria:**
- If calling program is known (`CDEMO-FROM-PROGRAM`), returns there (line 140)
- Otherwise returns to main menu COMEN01C (line 138)
- COMMAREA is preserved for context (line 510)

### US-06: Receive Validation Feedback
**As a** CardDemo user,
**I want to** receive specific error messages when my input is invalid,
**So that** I know exactly which field to correct.

**Acceptance Criteria:**
- Each validation failure displays a specific message identifying the field and problem
- Cursor is positioned on the errored field (via MOVE -1 TO field-length)
- Error messages include: 'Account ID must be Numeric...', 'Type CD can NOT be empty...', 'Amount should be in format -99999999.99', 'Orig Date - Not a valid date...', etc.

---

## 7. Workflow

### Step-by-Step Processing Flow

```
Step 1: User navigates to "Transaction Add" from main menu (COMEN01C)
        → XCTL to COTRN02C with COMMAREA
        
Step 2: COTRN02C displays blank add transaction screen (COTRN2A map)
        → Cursor positioned on Account ID field
        
Step 3: User enters Account ID (or Card Number) and transaction details
        → Presses ENTER
        
Step 4: VALIDATE-INPUT-KEY-FIELDS
        ├── Account ID provided → Numeric check → READ CXACAIX → Resolve Card #
        ├── Card Number provided → Numeric check → READ CCXREF → Resolve Acct ID
        └── Neither → Error: "Account or Card Number must be entered..."
        
Step 5: VALIDATE-INPUT-DATA-FIELDS
        ├── Phase 1: Empty checks on all 11 fields (lines 251-317)
        ├── Phase 2: Numeric checks on Type CD, Category CD (lines 322-337)
        ├── Phase 3: Amount format check (-99999999.99) (lines 339-351)
        ├── Phase 4: Date format check (YYYY-MM-DD) (lines 353-381)
        ├── Phase 5: Date validity via CSUTLDTC utility (lines 389-427)
        └── Phase 6: Merchant ID numeric check (lines 430-436)
        
Step 6: Confirmation check
        ├── Confirm = 'Y'/'y' → Proceed to ADD-TRANSACTION
        ├── Confirm = 'N'/'n'/SPACES → Prompt: "Confirm to add..."
        └── Confirm = other → Error: "Invalid value. Valid values are (Y/N)..."
        
Step 7: ADD-TRANSACTION
        ├── Browse to end of TRANSACT file (STARTBR with HIGH-VALUES)
        ├── Read last record (READPREV) to get highest existing ID
        ├── End browse (ENDBR)
        ├── New ID = Last ID + 1
        ├── Build TRAN-RECORD from all screen input fields
        └── WRITE-TRANSACT-FILE
        
Step 8: WRITE-TRANSACT-FILE outcome
        ├── Success → Clear fields, display green message with new Tran ID
        ├── Duplicate → Error: "Tran ID already exist..."
        └── Other error → Error: "Unable to Add Transaction..."
        
Step 9: CICS RETURN with TRANSID CT02 and COMMAREA
        → Pseudo-conversational wait for next user input
```

### Alternative Flows

| Key Press | Action | Code Reference |
|-----------|--------|----------------|
| ENTER | Validate and add transaction | Line 135 |
| PF3 | Return to calling program / main menu | Lines 136-143 |
| PF4 | Clear all screen fields | Lines 144-145 |
| PF5 | Copy last transaction data into form | Lines 146-147 |
| Other | Display 'Invalid Key Pressed' error | Lines 148-151 |
