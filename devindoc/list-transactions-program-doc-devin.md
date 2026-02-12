# COTRN00C — List Transactions Program Documentation

> **Program:** COTRN00C.cbl
> **Application:** CardDemo
> **Type:** CICS COBOL Online Program
> **Function:** List Transactions from TRANSACT file
> **TRANID:** CT00
> **Source:** `app/cbl/COTRN00C.cbl` (700 lines)

---

## 1. Overview

COTRN00C is a CICS online program that displays a paginated list of transactions from the TRANSACT VSAM file. It presents up to 10 transactions per page on a 3270 terminal screen, supports forward and backward pagination, allows optional filtering by Transaction ID, and enables the user to select a specific transaction for detailed viewing by transferring control to COTRN01C.

### Flow Diagram

```
                    ┌─────────────────────┐
                    │   CICS ENTRY (CT00) │
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
                    │ PROCESS-ENTER │  │ EVALUATE EIBAID   │
                    │ SEND screen   │  └───┬───┬───┬───┬───┘
                    └───────────────┘      │   │   │   │
                                       ENTER PF3 PF7 PF8 OTHER
                                         │   │   │   │    │
                          ┌──────────────▼┐  │   │   │  ┌─▼───────────┐
                          │PROCESS-ENTER  │  │   │   │  │Invalid key  │
                          │-KEY           │  │   │   │  │message      │
                          └──────┬────────┘  │   │   │  └─────────────┘
                                 │           │   │   │
                    ┌────────────▼─────┐     │   │   │
                    │Check selection   │     │   │   │
                    │(SEL0001-SEL0010) │  ┌──▼┐ ┌▼─────┐ ┌▼─────────┐
                    └────────┬─────────┘  │RTN│ │PAGE  │ │PAGE      │
                             │            │PRV│ │BACK  │ │FORWARD   │
                  ┌──────────▼──────────┐ │SCR│ │(PF7) │ │(PF8)     │
                  │Selection = 'S'?     │ └───┘ └──────┘ └──────────┘
                  └───┬──────────┬──────┘
                  YES │          │ NO/NONE
           ┌──────────▼───┐  ┌──▼────────────────┐
           │XCTL to       │  │Validate Tran ID   │
           │COTRN01C      │  │filter (optional)   │
           │(View Detail) │  │PROCESS-PAGE-FORWARD│
           └──────────────┘  └────────────────────┘
                                      │
                    ┌─────────────────▼──────────────────┐
                    │ STARTBR → Read up to 10 records    │
                    │ → Populate screen rows              │
                    │ → Check if more pages exist         │
                    │ → ENDBR → SEND screen              │
                    └────────────────────────────────────┘
```

---

## 2. Dependencies

### 2.1 Copybooks

| Copybook | Purpose | Reference |
|----------|---------|-----------|
| `COCOM01Y.cpy` | COMMAREA structure shared across all CardDemo programs | Line 61 |
| `COTRN00.cpy` | BMS map I/O area definitions for COTRN0AI/COTRN0AO | Line 72 |
| `COTTL01Y.cpy` | Screen title constants (CCDA-TITLE01, CCDA-TITLE02) | Line 74 |
| `CSDAT01Y.cpy` | Date/time formatting fields (WS-CURDATE, WS-CURTIME, WS-TIMESTAMP) | Line 75 |
| `CSMSG01Y.cpy` | Common message constants (CCDA-MSG-INVALID-KEY) | Line 76 |
| `CVTRA05Y.cpy` | Transaction record layout (TRAN-RECORD, 350 bytes) | Line 78 |
| `DFHAID.cpy` | CICS AID key definitions (DFHENTER, DFHPF3, DFHPF7, DFHPF8) | Line 80 |
| `DFHBMSCA.cpy` | BMS attribute constants | Line 81 |

### 2.2 VSAM Files Accessed

| File DD Name | Access Mode | Purpose | Code Reference |
|-------------|-------------|---------|----------------|
| `TRANSACT` | STARTBR, READNEXT, READPREV, ENDBR | Browse transactions for paginated display | Lines 39, 593-696 |

### 2.3 Called Programs

| Program | Call Type | Purpose | Code Reference |
|---------|-----------|---------|----------------|
| `COTRN01C` | XCTL | View selected transaction detail | Lines 188, 192-195 |
| `COMEN01C` | XCTL | Return to main menu (PF3) | Lines 123, 518-521 |
| `COSGN00C` | XCTL | Return to sign-on (no COMMAREA) | Lines 108, 518-521 |

### 2.4 BMS Map

| Map | Mapset | Purpose |
|-----|--------|---------|
| `COTRN0A` | `COTRN00` | Transaction list screen layout (input/output) |

### 2.5 Programs That Call COTRN00C

| Caller | Mechanism | Code Evidence |
|--------|-----------|---------------|
| `COMEN01C.cbl` | XCTL via menu option 4 ("Transaction List") | `COMEN02Y.cpy` |
| `COTRN01C.cbl` | XCTL via PF5 (return to list) | `COTRN01C.cbl` lines 125-127 |

---

## 3. Detailed Functionality

### 3.1 Main Entry Logic (MAIN-PARA, lines 95-141)

1. **Initialization (lines 97-105):** Resets error flag, EOF flag, next-page flag, and erase flag. Clears message area. Positions cursor on Transaction ID filter field.
2. **No COMMAREA (EIBCALEN = 0):** Returns to sign-on screen `COSGN00C` (lines 107-109).
3. **First entry (NOT CDEMO-PGM-REENTER):** Initializes screen to LOW-VALUES, processes enter key (loads first page of transactions), sends the list screen (lines 112-116).
4. **Re-entry (CDEMO-PGM-REENTER):** Receives screen input and evaluates the AID key:
   - **ENTER:** Calls `PROCESS-ENTER-KEY` for selection handling and page load (line 121)
   - **PF3:** Returns to main menu `COMEN01C` (lines 122-124)
   - **PF7:** Page backward via `PROCESS-PF7-KEY` (lines 125-126)
   - **PF8:** Page forward via `PROCESS-PF8-KEY` (lines 127-128)
   - **Other:** Invalid key message (lines 129-133)
5. **CICS RETURN:** Returns to CICS with TRANSID `CT00` and COMMAREA (lines 138-141).

### 3.2 Transaction Selection (PROCESS-ENTER-KEY, lines 146-229)

**Selection detection (lines 148-204):**
The screen displays up to 10 transaction rows, each with a selection field (SEL0001I through SEL0010I). The program checks each selection field sequentially. When a non-empty selection is found, the corresponding transaction ID (TRNID01I through TRNID10I) is captured into `CDEMO-CT00-TRN-SELECTED`.

**Selection routing (lines 183-204):**
- **'S' or 's':** Transfer control to `COTRN01C` (View Transaction) via XCTL with the selected transaction ID in COMMAREA (lines 186-195).
- **Other value:** Error — 'Invalid selection. Valid value is S' (lines 196-201).
- **No selection:** Proceeds to transaction listing.

**Transaction ID filter (lines 206-219):**
- If the Transaction ID filter field is empty: sets `TRAN-ID` to LOW-VALUES (browse from beginning).
- If populated and numeric: uses the value as the starting point for browse.
- If populated and not numeric: error — 'Tran ID must be Numeric ...' (lines 212-217).

**Page load (lines 221-229):**
Resets page number to 0, then calls `PROCESS-PAGE-FORWARD` to load the first page.

### 3.3 Forward Pagination (PROCESS-PAGE-FORWARD, lines 279-328)

1. **STARTBR** on TRANSACT file at the current `TRAN-ID` position (line 281).
2. If not initial entry, performs one READNEXT to skip past the current position (lines 285-287).
3. **Initializes** all 10 display rows to spaces (lines 289-293).
4. **Reads up to 10 records** sequentially using READNEXT in a loop (lines 297-303). Each record is formatted and placed in the corresponding screen row via `POPULATE-TRAN-DATA`.
5. **Next-page detection:** After reading 10 records, attempts one more READNEXT. If successful, sets `NEXT-PAGE-YES` (more data exists). If ENDFILE, sets `NEXT-PAGE-NO` (lines 305-320).
6. **Increments page number** and ends browse (lines 306-307, 322).
7. **Sends** the populated screen (line 326).

### 3.4 Backward Pagination (PROCESS-PAGE-BACKWARD, lines 333-376)

1. **STARTBR** on TRANSACT file at the first transaction ID of the current page (line 335).
2. Performs one READPREV to skip past the current position (lines 339-341).
3. **Initializes** all 10 display rows (lines 343-347).
4. **Reads up to 10 records** in reverse using READPREV, filling rows from position 10 down to 1 (lines 349-357).
5. **Page number adjustment:** Decrements page number if more records exist before the current page (lines 359-368).
6. **Ends browse** and sends the screen (lines 371-374).

### 3.5 Page Boundary Handling

**PF7 (Page Up) — PROCESS-PF7-KEY (lines 234-252):**
- If already on page 1: displays 'You are already at the top of the page...' and does not paginate (lines 247-251).
- Otherwise: uses first transaction ID of current page as the starting position and calls `PROCESS-PAGE-BACKWARD`.

**PF8 (Page Down) — PROCESS-PF8-KEY (lines 257-274):**
- If no more pages (`NEXT-PAGE-NO`): displays 'You are already at the bottom of the page...' (lines 270-273).
- Otherwise: uses last transaction ID of current page as the starting position and calls `PROCESS-PAGE-FORWARD`.

### 3.6 Row Population (POPULATE-TRAN-DATA, lines 381-445)

For each transaction record read, populates the corresponding screen row with:
- **Transaction ID** (TRNID01I through TRNID10I)
- **Date** (formatted as MM/DD/YY from TRAN-ORIG-TS timestamp)
- **Description** (TRAN-DESC)
- **Amount** (TRAN-AMT formatted as +99999999.99)

The first record's ID is saved as `CDEMO-CT00-TRNID-FIRST` (line 393) and the last record's ID as `CDEMO-CT00-TRNID-LAST` (line 439) for pagination state.

### 3.7 Screen Send (SEND-TRNLST-SCREEN, lines 527-549)

Two send modes:
- **ERASE mode (SEND-ERASE-YES):** Full screen redraw with ERASE option (lines 533-540).
- **Non-erase mode (SEND-ERASE-NO):** Partial screen update without clearing (lines 541-549). Used for boundary messages to preserve current data.

---

## 4. Summary

COTRN00C is a paginated transaction list viewer that:
- Displays 10 transactions per page from the TRANSACT VSAM file
- Supports forward (PF8) and backward (PF7) pagination with boundary detection
- Allows optional filtering by starting Transaction ID
- Enables single-transaction selection (enter 'S') to drill into COTRN01C for detail view
- Maintains page state (first/last IDs, page number, next-page flag) in COMMAREA across pseudo-conversational interactions
- Formats each row with Transaction ID, Date (MM/DD/YY), Description, and Amount

**Lines of code:** 700
**Paragraphs/Sections:** 16 (MAIN-PARA, PROCESS-ENTER-KEY, PROCESS-PF7-KEY, PROCESS-PF8-KEY, PROCESS-PAGE-FORWARD, PROCESS-PAGE-BACKWARD, POPULATE-TRAN-DATA, INITIALIZE-TRAN-DATA, RETURN-TO-PREV-SCREEN, SEND-TRNLST-SCREEN, RECEIVE-TRNLST-SCREEN, POPULATE-HEADER-INFO, STARTBR-TRANSACT-FILE, READNEXT-TRANSACT-FILE, READPREV-TRANSACT-FILE, ENDBR-TRANSACT-FILE)

---

## 5. Business Rules

### BR-01: Transaction ID Filter Must Be Numeric
If a starting Transaction ID is provided in the filter field, it must be a numeric value.
- **Condition:** Transaction ID filter is non-empty and not numeric
- **Outcome:** Error — 'Tran ID must be Numeric ...'
- **Code:** Lines 209-217

### BR-02: Only 'S' Is a Valid Selection Value
When selecting a transaction from the list, only 'S' or 's' is accepted.
- **Condition:** Selection field contains a value other than 'S'/'s'
- **Outcome:** Error — 'Invalid selection. Valid value is S'
- **Code:** Lines 196-201

### BR-03: Page Size Is Fixed at 10 Transactions
Each page displays exactly 10 transaction rows.
- **Condition:** Page is loaded (forward or backward)
- **Outcome:** Up to 10 records are read and displayed
- **Code:** Lines 290 (forward: `UNTIL WS-IDX > 10`), 351 (backward: `UNTIL WS-IDX <= 0`)

### BR-04: Forward Pagination Stops at End of File
When the last transaction in the file has been displayed, further forward pagination is blocked.
- **Condition:** READNEXT returns ENDFILE after loading page
- **Outcome:** `NEXT-PAGE-NO` flag set; PF8 displays 'You are already at the bottom of the page...'
- **Code:** Lines 311-312 (flag), Lines 270-273 (message)

### BR-05: Backward Pagination Stops at Page 1
When on the first page, backward pagination is blocked.
- **Condition:** `CDEMO-CT00-PAGE-NUM` is 1 or less
- **Outcome:** 'You are already at the top of the page...'
- **Code:** Lines 245-251

### BR-06: Selection Triggers View Detail
Selecting a transaction with 'S' navigates to the transaction detail view (COTRN01C).
- **Condition:** Valid selection 'S' on a transaction row
- **Outcome:** XCTL to COTRN01C with selected transaction ID in COMMAREA
- **Code:** Lines 186-195

### BR-07: Empty Filter Browses from Beginning
When no Transaction ID filter is provided, the list starts from the beginning of the file.
- **Condition:** Transaction ID filter field is SPACES or LOW-VALUES
- **Outcome:** TRAN-ID set to LOW-VALUES for STARTBR (browse from first record)
- **Code:** Lines 206-207

### BR-08: Page Number Tracked in COMMAREA
The current page number is maintained across interactions via COMMAREA.
- **Condition:** Each page load (forward or backward)
- **Outcome:** Page number incremented/decremented and displayed on screen
- **Code:** Lines 306-307 (increment), 364 (decrement), 324 (display)

---

## 6. User Stories

### US-01: Browse All Transactions
**As a** CardDemo user,
**I want to** view a paginated list of all transactions,
**So that** I can review recent transaction activity.

**Acceptance Criteria:**
- On entry, the first 10 transactions are displayed (lines 115, 225)
- Each row shows: Transaction ID, Date, Description, Amount (lines 392-442)
- Page number is displayed on screen (line 324)

### US-02: Page Forward Through Transactions
**As a** CardDemo user,
**I want to** press PF8 to see the next page of transactions,
**So that** I can view older/later transactions.

**Acceptance Criteria:**
- Pressing PF8 loads the next 10 transactions (lines 127-128, 267-268)
- Page number increments (line 306-307)
- If at the last page, message displays 'You are already at the bottom of the page...' (lines 270-273)

### US-03: Page Backward Through Transactions
**As a** CardDemo user,
**I want to** press PF7 to see the previous page of transactions,
**So that** I can return to earlier transactions.

**Acceptance Criteria:**
- Pressing PF7 loads the previous 10 transactions in reverse order (lines 125-126, 245-246)
- Page number decrements (line 364)
- If at page 1, message displays 'You are already at the top of the page...' (lines 248-249)

### US-04: Filter Transactions by Starting ID
**As a** CardDemo user,
**I want to** enter a Transaction ID to start the list from a specific point,
**So that** I can quickly navigate to a range of transactions.

**Acceptance Criteria:**
- I enter a numeric Transaction ID in the filter field
- The list starts from that ID or the nearest match (lines 209-210, 281)
- Non-numeric values are rejected with 'Tran ID must be Numeric ...' (lines 212-215)

### US-05: Select a Transaction for Detail View
**As a** CardDemo user,
**I want to** enter 'S' next to a transaction,
**So that** I can view the full details of that transaction.

**Acceptance Criteria:**
- I type 'S' in the selection field next to a transaction row
- The system transfers to COTRN01C with the selected transaction ID (lines 186-195)
- Invalid selection values display 'Invalid selection. Valid value is S' (lines 198-200)

### US-06: Return to Main Menu
**As a** CardDemo user,
**I want to** press PF3 to return to the main menu,
**So that** I can navigate to other features.

**Acceptance Criteria:**
- Pressing PF3 returns to COMEN01C (lines 122-124)
- COMMAREA is preserved (line 520)

---

## 7. Workflow

### Step-by-Step Processing Flow

```
Step 1: User selects "Transaction List" from main menu (COMEN01C)
        → XCTL to COTRN00C with COMMAREA

Step 2: First entry — Initialize and load first page
        ├── Set CDEMO-PGM-REENTER = TRUE (line 113)
        ├── Initialize screen to LOW-VALUES (line 114)
        ├── PROCESS-ENTER-KEY → PROCESS-PAGE-FORWARD
        │   ├── STARTBR on TRANSACT at LOW-VALUES (beginning)
        │   ├── Initialize 10 display rows to SPACES
        │   ├── READNEXT up to 10 records into rows 1-10
        │   ├── Attempt 11th READNEXT to check for more pages
        │   ├── ENDBR
        │   └── Page number = 1
        └── SEND-TRNLST-SCREEN (with ERASE)

Step 3: Screen displays with up to 10 transactions:
        ┌──────────────────────────────────────────────────────────────┐
        │  Tran ID Filter: [________________]     Page: 1             │
        │  Sel  Tran ID           Date      Description       Amount  │
        │  [ ]  0000000000000001  01/15/26  Purchase at...  -00000125│
        │  [ ]  0000000000000002  01/15/26  Payment recvd   +00001000│
        │  ...  (up to 10 rows)                                       │
        │  PF3=Back  PF7=Prev  PF8=Next                              │
        └──────────────────────────────────────────────────────────────┘

Step 4: User interaction (pseudo-conversational cycle)
        ├── ENTER with selection:
        │   ├── Check SEL0001I-SEL0010I for non-empty value
        │   ├── If 'S'/'s': XCTL to COTRN01C with selected TRAN-ID
        │   └── If other: Error "Invalid selection. Valid value is S"
        │
        ├── ENTER with filter ID:
        │   ├── Validate numeric
        │   ├── Reset page to 0
        │   └── PROCESS-PAGE-FORWARD from that ID
        │
        ├── PF7 (Page Up):
        │   ├── If page > 1: PROCESS-PAGE-BACKWARD
        │   │   ├── STARTBR at CDEMO-CT00-TRNID-FIRST
        │   │   ├── READPREV to skip current position
        │   │   ├── READPREV 10 records (filling rows 10→1)
        │   │   ├── ENDBR
        │   │   └── Decrement page number
        │   └── If page = 1: "You are already at the top..."
        │
        ├── PF8 (Page Down):
        │   ├── If NEXT-PAGE-YES: PROCESS-PAGE-FORWARD
        │   │   ├── STARTBR at CDEMO-CT00-TRNID-LAST
        │   │   ├── READNEXT to skip current position
        │   │   ├── READNEXT 10 records (filling rows 1→10)
        │   │   ├── Check for more with 11th READNEXT
        │   │   ├── ENDBR
        │   │   └── Increment page number
        │   └── If NEXT-PAGE-NO: "You are already at the bottom..."
        │
        ├── PF3: XCTL to COMEN01C (return to menu)
        │
        └── Other key: "Invalid Key Pressed" message

Step 5: CICS RETURN with TRANSID CT00 and COMMAREA
        → Pseudo-conversational wait for next user input
        → Loop back to Step 4
```

### State Management via COMMAREA

| Field | Purpose | Updated By |
|-------|---------|------------|
| `CDEMO-CT00-TRNID-FIRST` | First transaction ID on current page | POPULATE-TRAN-DATA (row 1, line 393) |
| `CDEMO-CT00-TRNID-LAST` | Last transaction ID on current page | POPULATE-TRAN-DATA (row 10, line 439) |
| `CDEMO-CT00-PAGE-NUM` | Current page number | PROCESS-PAGE-FORWARD (line 307), PROCESS-PAGE-BACKWARD (line 364) |
| `CDEMO-CT00-NEXT-PAGE-FLG` | Whether more pages exist ('Y'/'N') | Lines 310, 312, 315 |
| `CDEMO-CT00-TRN-SEL-FLG` | Selection flag from user ('S') | Lines 150-178 |
| `CDEMO-CT00-TRN-SELECTED` | Selected transaction ID | Lines 151-178 |
