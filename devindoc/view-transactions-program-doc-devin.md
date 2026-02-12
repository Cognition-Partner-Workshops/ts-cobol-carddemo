# COTRN01C — View Transaction Program Documentation

> **Program:** COTRN01C.cbl
> **Application:** CardDemo
> **Type:** CICS COBOL Online Program
> **Function:** View a Transaction from TRANSACT file
> **TRANID:** CT01
> **Source:** `app/cbl/COTRN01C.cbl` (331 lines)

---

## 1. Overview

COTRN01C is a CICS online program that displays the full details of a single transaction from the TRANSACT VSAM file. Users can enter a Transaction ID directly or arrive with a pre-selected ID from the transaction list screen (COTRN00C). The program reads the transaction record and displays all fields including card number, type, category, source, description, amount, dates, and merchant information in a read-only detail view.

### Flow Diagram

```
                    ┌───────────────────────┐
                    │   CICS ENTRY (CT01)   │
                    │   or XCTL from        │
                    │   COTRN00C (list)      │
                    └──────────┬────────────┘
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
                    ┌──────────────▼──────────┐│
                    │ Initialize screen       ││
                    │ Pre-selected TRN ID?    ││
                    │ ├── YES: auto-lookup    ││
                    │ └── NO: show blank      ││
                    │ SEND screen             ││
                    └─────────────────────────┘│
                                       ┌───────▼──────────┐
                                       │ RECEIVE screen    │
                                       │ EVALUATE EIBAID   │
                                       └───┬───┬───┬───┬───┘
                                           │   │   │   │
                                       ENTER PF3 PF4 PF5 OTHER
                                         │   │   │   │    │
                          ┌──────────────▼┐  │   │   │  ┌─▼───────────┐
                          │PROCESS-ENTER  │  │   │   │  │Invalid key  │
                          │-KEY           │  │   │   │  │message      │
                          └──────┬────────┘  │   │   │  └─────────────┘
                                 │           │   │   │
                    ┌────────────▼─────┐     │   │   │
                    │Tran ID empty?    │  ┌──▼┐ ┌▼┐ ┌▼──────────┐
                    └───┬──────────┬───┘  │RTN│ │CL│ │RETURN to  │
                    YES │          │ NO   │PRV│ │EA│ │COTRN00C   │
              ┌─────────▼──┐ ┌────▼────┐  │SCR│ │R │ │(list)     │
              │Error:      │ │READ     │  └───┘ └──┘ └───────────┘
              │"Tran ID    │ │TRANSACT │
              │can NOT be  │ │FILE     │
              │empty..."   │ └────┬────┘
              └────────────┘      │
                           ┌──────▼──────┐
                           │Found?       │
                           └──┬──────┬───┘
                          YES │      │ NO
                    ┌─────────▼──┐ ┌─▼──────────┐
                    │Populate    │ │Error:       │
                    │all detail  │ │"Transaction │
                    │fields on   │ │ID NOT       │
                    │screen      │ │found..."    │
                    │SEND screen │ └─────────────┘
                    └────────────┘
```

---

## 2. Dependencies

### 2.1 Copybooks

| Copybook | Purpose | Reference |
|----------|---------|-----------|
| `COCOM01Y.cpy` | COMMAREA structure shared across all CardDemo programs | Line 52 |
| `COTRN01.cpy` | BMS map I/O area definitions for COTRN1AI/COTRN1AO | Line 63 |
| `COTTL01Y.cpy` | Screen title constants (CCDA-TITLE01, CCDA-TITLE02) | Line 65 |
| `CSDAT01Y.cpy` | Date/time formatting fields (WS-CURDATE, WS-CURTIME) | Line 66 |
| `CSMSG01Y.cpy` | Common message constants (CCDA-MSG-INVALID-KEY) | Line 67 |
| `CVTRA05Y.cpy` | Transaction record layout (TRAN-RECORD, 350 bytes) | Line 69 |
| `DFHAID.cpy` | CICS AID key definitions (DFHENTER, DFHPF3, DFHPF4, DFHPF5) | Line 71 |
| `DFHBMSCA.cpy` | BMS attribute constants | Line 72 |

### 2.2 VSAM Files Accessed

| File DD Name | Access Mode | Purpose | Code Reference |
|-------------|-------------|---------|----------------|
| `TRANSACT` | READ (with UPDATE) | Read a single transaction record by TRAN-ID | Lines 39, 269-296 |

### 2.3 Called Programs (via XCTL)

| Program | Call Type | Purpose | Code Reference |
|---------|-----------|---------|----------------|
| `COMEN01C` | XCTL | Return to main menu (PF3 default) | Lines 117, 205-208 |
| `COTRN00C` | XCTL | Return to transaction list (PF5) | Lines 126-127, 205-208 |
| `COSGN00C` | XCTL | Return to sign-on (no COMMAREA fallback) | Lines 95, 199-200 |

### 2.4 BMS Map

| Map | Mapset | Purpose |
|-----|--------|---------|
| `COTRN1A` | `COTRN01` | View Transaction detail screen layout (input/output) |

### 2.5 Programs That Call COTRN01C

| Caller | Mechanism | Code Evidence |
|--------|-----------|---------------|
| `COTRN00C.cbl` | XCTL when user selects 'S' on a transaction row | `COTRN00C.cbl` lines 188, 192-195 |

---

## 3. Detailed Functionality

### 3.1 Main Entry Logic (MAIN-PARA, lines 86-139)

1. **Initialization (lines 88-92):** Resets error flag and user-modified flag. Clears message area on both working storage and screen output.
2. **No COMMAREA (EIBCALEN = 0):** Returns to sign-on screen `COSGN00C` (lines 94-96).
3. **First entry (NOT CDEMO-PGM-REENTER, lines 99-109):**
   - Sets `CDEMO-PGM-REENTER` to TRUE for next interaction
   - Initializes screen output area to LOW-VALUES
   - Positions cursor on Transaction ID input field (`TRNIDINL`)
   - **Pre-selected transaction check (lines 103-108):** If `CDEMO-CT01-TRN-SELECTED` is non-empty (set by COTRN00C), copies the selected transaction ID into the input field and calls `PROCESS-ENTER-KEY` to auto-load the transaction details
   - Sends the view screen
4. **Re-entry (lines 110-133):** Receives screen input and evaluates the AID key:
   - **ENTER:** Calls `PROCESS-ENTER-KEY` to look up and display transaction (line 114)
   - **PF3:** Returns to calling program or main menu `COMEN01C` (lines 115-122)
   - **PF4:** Clears all screen fields via `CLEAR-CURRENT-SCREEN` (lines 123-124)
   - **PF5:** Returns to transaction list `COTRN00C` (lines 125-127)
   - **Other:** Displays invalid key error message (lines 128-131)
5. **CICS RETURN (lines 136-139):** Returns to CICS with TRANSID `CT01` and COMMAREA for pseudo-conversational operation.

### 3.2 Transaction Lookup (PROCESS-ENTER-KEY, lines 144-192)

**Validation (lines 146-156):**
- If Transaction ID input field (`TRNIDINI`) is empty (SPACES or LOW-VALUES): sets error flag, displays 'Tran ID can NOT be empty...', positions cursor on the field.
- If non-empty: proceeds to file read.

**Clear and Read (lines 158-173):**
If no error from validation:
1. Clears all detail output fields to SPACES (lines 159-171)
2. Moves the input Transaction ID to `TRAN-ID` (line 172)
3. Calls `READ-TRANSACT-FILE` (line 173)

**Display (lines 176-192):**
If the read was successful (no error flag), populates all screen output fields from the transaction record:
- `TRAN-ID` → Transaction ID display field (line 178)
- `TRAN-CARD-NUM` → Card Number (line 179)
- `TRAN-TYPE-CD` → Type Code (line 180)
- `TRAN-CAT-CD` → Category Code (line 181)
- `TRAN-SOURCE` → Source (line 182)
- `TRAN-AMT` → Amount (formatted as +99999999.99) (lines 177, 183)
- `TRAN-DESC` → Description (line 184)
- `TRAN-ORIG-TS` → Origination Timestamp (line 185)
- `TRAN-PROC-TS` → Processing Timestamp (line 186)
- `TRAN-MERCHANT-ID` → Merchant ID (line 187)
- `TRAN-MERCHANT-NAME` → Merchant Name (line 188)
- `TRAN-MERCHANT-CITY` → Merchant City (line 189)
- `TRAN-MERCHANT-ZIP` → Merchant Zip (line 190)

### 3.3 File Read (READ-TRANSACT-FILE, lines 267-296)

Uses `EXEC CICS READ` with the UPDATE option to read from the TRANSACT VSAM file:
- **Dataset:** `WS-TRANSACT-FILE` ('TRANSACT')
- **Key:** `TRAN-ID` (16-byte key, full key length)
- **Into:** `TRAN-RECORD` (350-byte record)

**Response handling (lines 280-296):**
- **NORMAL:** Read successful, continues to display logic.
- **NOTFND:** Transaction ID not found in file — error 'Transaction ID NOT found...' (lines 283-288).
- **OTHER:** Unexpected error — displays RESP/REAS codes to console and error 'Unable to lookup Transaction...' (lines 289-295).

**Note:** The READ uses the `UPDATE` option (line 275), which acquires an exclusive lock on the record. This is typically used when a subsequent REWRITE is planned. In this read-only view program, this appears to be a code artifact that does not affect functionality but does hold a record lock until the task ends.

### 3.4 Screen Clear (CLEAR-CURRENT-SCREEN, lines 301-304)

Triggered by PF4. Calls `INITIALIZE-ALL-FIELDS` (sets all input/display fields to SPACES, cursor to Transaction ID field) then sends the cleared screen.

### 3.5 Header Population (POPULATE-HEADER-INFO, lines 243-262)

Populates screen header with:
- Application titles (CCDA-TITLE01, CCDA-TITLE02)
- Transaction name (CT01) and program name (COTRN01C)
- Current date in MM/DD/YY format
- Current time in HH:MM:SS format

---

## 4. Summary

COTRN01C is a simple, focused read-only transaction detail viewer that:
- Accepts a Transaction ID either from user input or pre-selected from the list screen (COTRN00C)
- Reads the full transaction record from the TRANSACT VSAM file
- Displays all 13 transaction fields (ID, card number, type, category, source, amount, description, origination timestamp, processing timestamp, merchant ID, name, city, zip)
- Supports PF3 (return to menu/caller), PF4 (clear screen), and PF5 (return to transaction list)
- Operates pseudo-conversationally with COMMAREA state preservation

**Lines of code:** 331
**Paragraphs/Sections:** 9 (MAIN-PARA, PROCESS-ENTER-KEY, RETURN-TO-PREV-SCREEN, SEND-TRNVIEW-SCREEN, RECEIVE-TRNVIEW-SCREEN, POPULATE-HEADER-INFO, READ-TRANSACT-FILE, CLEAR-CURRENT-SCREEN, INITIALIZE-ALL-FIELDS)

---

## 5. Business Rules

### BR-01: Transaction ID Is Required
A Transaction ID must be provided to view transaction details. The field cannot be empty.
- **Condition:** Transaction ID input field is SPACES or LOW-VALUES
- **Outcome:** Error — 'Tran ID can NOT be empty...'
- **Code:** Lines 147-152

### BR-02: Transaction Must Exist in TRANSACT File
The provided Transaction ID must match an existing record in the TRANSACT VSAM file.
- **Condition:** CICS READ returns DFHRESP(NOTFND)
- **Outcome:** Error — 'Transaction ID NOT found...'
- **Code:** Lines 283-288

### BR-03: Pre-Selected Transaction Auto-Loads
When navigating from the transaction list (COTRN00C) with a pre-selected transaction, the detail view automatically loads the transaction without requiring the user to press ENTER.
- **Condition:** `CDEMO-CT01-TRN-SELECTED` is non-empty on first entry
- **Outcome:** Transaction ID is populated and `PROCESS-ENTER-KEY` is called automatically
- **Code:** Lines 103-108

### BR-04: View Is Read-Only
The transaction detail screen is display-only. No update or delete operations are available from this screen.
- **Condition:** N/A
- **Outcome:** No REWRITE or DELETE operations exist in the program. Only READ is performed.
- **Code:** The program contains no write/update/delete CICS commands for TRANSACT

### BR-05: PF5 Returns to Transaction List
Pressing PF5 returns the user to the transaction list screen (COTRN00C), preserving navigation context.
- **Condition:** User presses PF5
- **Outcome:** XCTL to COTRN00C with COMMAREA
- **Code:** Lines 125-127

---

## 6. User Stories

### US-01: View Transaction from List Selection
**As a** CardDemo user,
**I want to** select a transaction from the list and see its full details,
**So that** I can review all information about a specific transaction.

**Acceptance Criteria:**
- From COTRN00C, I select a transaction with 'S'
- COTRN01C automatically loads and displays all transaction fields (lines 103-108, 176-191)
- I see: Transaction ID, Card Number, Type Code, Category Code, Source, Amount, Description, Origination Timestamp, Processing Timestamp, Merchant ID, Merchant Name, Merchant City, Merchant Zip

### US-02: View Transaction by Manual ID Entry
**As a** CardDemo user,
**I want to** type a Transaction ID directly and press ENTER,
**So that** I can look up a specific transaction by its ID.

**Acceptance Criteria:**
- I type a Transaction ID in the input field and press ENTER
- If the transaction exists: all detail fields are populated (lines 176-191)
- If it doesn't exist: error 'Transaction ID NOT found...' (lines 283-288)
- If the field is empty: error 'Tran ID can NOT be empty...' (lines 147-152)

### US-03: Clear the View Screen
**As a** CardDemo user,
**I want to** press PF4 to clear the screen,
**So that** I can enter a different Transaction ID to look up.

**Acceptance Criteria:**
- All detail fields are cleared to SPACES (lines 311-326)
- Cursor is positioned on the Transaction ID input field (line 311)
- I can type a new Transaction ID

### US-04: Return to Transaction List
**As a** CardDemo user,
**I want to** press PF5 to return to the transaction list,
**So that** I can continue browsing or select another transaction.

**Acceptance Criteria:**
- Control transfers to COTRN00C (lines 126-127)
- COMMAREA is preserved with page state from the list (line 207)

### US-05: Return to Main Menu
**As a** CardDemo user,
**I want to** press PF3 to return to the calling screen or main menu,
**So that** I can navigate to other features.

**Acceptance Criteria:**
- If called from another program: returns to that program (lines 118-120)
- If no calling program: returns to COMEN01C main menu (line 117)
- COMMAREA is preserved (line 207)

---

## 7. Workflow

### Step-by-Step Processing Flow

```
Step 1: Entry — Two paths into COTRN01C

  Path A: From Transaction List (COTRN00C)
        → User selects transaction with 'S'
        → XCTL to COTRN01C with CDEMO-CT01-TRN-SELECTED set
        → Program detects pre-selected ID (line 103-104)
        → Auto-populates Transaction ID field (lines 105-106)
        → Auto-calls PROCESS-ENTER-KEY (line 107)
        → Transaction details loaded and displayed

  Path B: Direct entry or from menu
        → XCTL to COTRN01C without pre-selected ID
        → Blank detail screen displayed
        → Cursor on Transaction ID input field

Step 2: User enters Transaction ID (if not pre-selected)
        → Presses ENTER

Step 3: PROCESS-ENTER-KEY
        ├── Validate: Transaction ID not empty (lines 146-156)
        │   └── If empty → Error: "Tran ID can NOT be empty..."
        ├── Clear all detail display fields (lines 158-171)
        ├── Move input ID to TRAN-ID (line 172)
        └── READ-TRANSACT-FILE (line 173)

Step 4: READ-TRANSACT-FILE (lines 267-296)
        ├── EXEC CICS READ DATASET('TRANSACT')
        │   INTO(TRAN-RECORD) RIDFLD(TRAN-ID) UPDATE
        ├── NORMAL → Continue to display
        ├── NOTFND → Error: "Transaction ID NOT found..."
        └── OTHER  → Error: "Unable to lookup Transaction..."

Step 5: Display transaction details (lines 176-191)
        ├── TRAN-ID        → Transaction ID field
        ├── TRAN-CARD-NUM  → Card Number field
        ├── TRAN-TYPE-CD   → Type Code field
        ├── TRAN-CAT-CD    → Category Code field
        ├── TRAN-SOURCE    → Source field
        ├── TRAN-AMT       → Amount field (formatted +99999999.99)
        ├── TRAN-DESC      → Description field
        ├── TRAN-ORIG-TS   → Origination Timestamp field
        ├── TRAN-PROC-TS   → Processing Timestamp field
        ├── TRAN-MERCHANT-ID   → Merchant ID field
        ├── TRAN-MERCHANT-NAME → Merchant Name field
        ├── TRAN-MERCHANT-CITY → Merchant City field
        └── TRAN-MERCHANT-ZIP  → Merchant Zip field

Step 6: SEND-TRNVIEW-SCREEN (lines 213-225)
        ├── Populate header (date, time, titles)
        ├── Set error message area
        └── EXEC CICS SEND MAP('COTRN1A') MAPSET('COTRN01') ERASE CURSOR

Step 7: CICS RETURN with TRANSID CT01 and COMMAREA
        → Pseudo-conversational wait

Step 8: User interaction (next cycle)
        ├── ENTER → New lookup (Step 3)
        ├── PF3   → Return to caller/menu
        │           ├── CDEMO-FROM-PROGRAM set → XCTL to that program
        │           └── Not set → XCTL to COMEN01C
        ├── PF4   → Clear all fields, show blank screen
        ├── PF5   → XCTL to COTRN00C (transaction list)
        └── Other → "Invalid Key Pressed" message
```

### Screen Layout

```
┌──────────────────────────────────────────────────────────────────────┐
│  CardDemo                              View Transaction    CT01     │
│  COTRN01C                              MM/DD/YY  HH:MM:SS          │
│                                                                      │
│  Transaction ID: [________________]                                  │
│                                                                      │
│  Tran ID:        XXXXXXXXXXXXXXXX                                    │
│  Card Number:    XXXXXXXXXXXXXXXX                                    │
│  Type Code:      XX                                                  │
│  Category Code:  XXXX                                                │
│  Source:         XXXXXXXXXX                                          │
│  Amount:         +99999999.99                                        │
│  Description:    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX │
│  Orig Date:      YYYY-MM-DD-HH.MM.SS.MMMMMM                        │
│  Proc Date:      YYYY-MM-DD-HH.MM.SS.MMMMMM                        │
│  Merchant ID:    999999999                                           │
│  Merchant Name:  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX   │
│  Merchant City:  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX   │
│  Merchant Zip:   XXXXXXXXXX                                          │
│                                                                      │
│  [Error/Info Message Area]                                           │
│  PF3=Back  PF4=Clear  PF5=Transaction List                          │
└──────────────────────────────────────────────────────────────────────┘
```

### Navigation Map

```
    COMEN01C (Main Menu)
        │
        │  Menu option → XCTL
        ▼
    COTRN00C (Transaction List) ◄────────── PF5
        │                                     │
        │  Select 'S' → XCTL                  │
        ▼                                     │
    COTRN01C (View Transaction) ──────────────┘
        │
        │  PF3 → XCTL
        ▼
    COMEN01C or calling program
```
