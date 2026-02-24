# COBOL Business Rules Analysis

## CardDemo Programs: COBIL00C, COCRDUPC, COCRDSLC

This document details the business rules extracted from three COBOL programs in the
AWS CardDemo application, covering bill payment processing, credit card updates,
and credit card selection/detail views.

---

## 1. COBIL00C.cbl - Bill Payment

### 1.1 Calculation Formulas

| Formula | COBOL Reference | Description |
|---------|----------------|-------------|
| `TRAN-AMT = ACCT-CURR-BAL` | Line 224 | Payment amount is always the full current balance. No partial payments supported. |
| `ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT` | Line 234 | New balance after payment. Since TRAN-AMT equals the full balance, result is always zero. |
| `WS-TRAN-ID-NUM = MAX(TRAN-ID) + 1` | Lines 216-217 | Transaction ID is auto-incremented from the highest existing ID. |

**Key observation**: This program implements a "pay in full" model only. There is no
minimum payment calculation, no interest rate application, and no fee computation.
The payment always clears the entire balance to zero.

### 1.2 Transaction Record Constants

| Field | Value | COBOL Line |
|-------|-------|------------|
| TRAN-TYPE-CD | `'02'` | 220 |
| TRAN-CAT-CD | `2` | 221 |
| TRAN-SOURCE | `'POS TERM'` | 222 |
| TRAN-DESC | `'BILL PAYMENT - ONLINE'` | 223 |
| TRAN-MERCHANT-ID | `999999999` | 226 |
| TRAN-MERCHANT-NAME | `'BILL PAYMENT'` | 227 |
| TRAN-MERCHANT-CITY | `'N/A'` | 228 |
| TRAN-MERCHANT-ZIP | `'N/A'` | 229 |

### 1.3 Branching / Decision Logic

```
START
  |
  v
Account ID empty? --> YES --> Error: "Acct ID can NOT be empty..."
  |
  NO
  v
Confirmation = 'Y'/'y'? --> Read account, proceed to payment
Confirmation = 'N'/'n'? --> Clear screen, cancel
Confirmation = blank?   --> Read account, show details (no payment)
Confirmation = other?   --> Error: "Invalid value. Valid values are (Y/N)..."
  |
  v
Balance <= 0? --> YES --> Error: "You have nothing to pay..."
  |
  NO
  v
Read CXACAIX (card cross-reference)
Generate next transaction ID (max + 1)
Create transaction record
COMPUTE new_balance = balance - payment_amount
Update account record
Display: "Payment successful. Your Transaction ID is {id}."
```

### 1.4 Edge Cases Handled

1. **Empty account ID**: Caught before any file I/O (lines 159-167)
2. **Account not found**: Two distinct lookups (ACCTDAT and CXACAIX) each handle NOTFND
3. **Zero balance**: `ACCT-CURR-BAL <= ZEROS` check (line 198)
4. **Negative balance** (credit): Same check covers negative values
5. **Duplicate transaction ID**: DUPKEY/DUPREC on write (lines 533-539)
6. **File I/O errors**: Generic handler with RESP/RESP2 codes for each file operation
7. **Invalid confirmation**: Only Y/y/N/n/blank accepted (lines 173-191)
8. **Empty transaction file**: ENDFILE on READPREV sets TRAN-ID to ZEROS (line 488)

---

## 2. COCRDUPC.cbl - Credit Card Update

### 2.1 Validation Rules (Input Edit Formulas)

| Field | Rule | Valid Range | COBOL Reference |
|-------|------|-------------|----------------|
| Account ID | Numeric, 11 digits, non-zero | `00000000001` - `99999999999` | 1210-EDIT-ACCOUNT (721-756) |
| Card Number | Numeric, 16 digits, non-zero | `0000000000000001` - `9999999999999999` | 1220-EDIT-CARD (762-800) |
| Card Name | Alphabets and spaces only | A-Z, a-z, space | 1230-EDIT-NAME (806-843) |
| Card Status | Must be Y or N | `'Y'`, `'N'` | 1240-EDIT-CARDSTATUS (845-876) |
| Expiry Month | Numeric, 1-12 | `01` - `12` | 1250-EDIT-EXPIRY-MON (877-912) |
| Expiry Year | Numeric, 1950-2099 | `1950` - `2099` | 1260-EDIT-EXPIRY-YEAR (913-947) |

### 2.2 State Machine (CCUP-CHANGE-ACTION)

```
                         +--> CHANGES_NOT_OK ('E') -- validation failed
                         |         |
NOT_FETCHED --> SHOW_DETAILS --> CHANGES_OK_NOT_CONFIRMED ('N')
(LOW-VALUES)     ('S')           |
                                 | F5 pressed
                                 v
                     +-- CHANGES_OKAYED_AND_DONE ('C')
                     |
                     +-- CHANGES_OKAYED_LOCK_ERROR ('L') -- could not lock
                     |
                     +-- CHANGES_OKAYED_BUT_FAILED ('F') -- rewrite failed
```

| State | COBOL Value | Description |
|-------|-------------|-------------|
| NOT_FETCHED | LOW-VALUES/SPACES | Initial state, no card data loaded |
| SHOW_DETAILS | `'S'` | Card details displayed for editing |
| CHANGES_NOT_OK | `'E'` | Edits failed validation |
| CHANGES_OK_NOT_CONFIRMED | `'N'` | Edits validated, awaiting F5 to save |
| CHANGES_OKAYED_AND_DONE | `'C'` | Successfully committed to database |
| CHANGES_OKAYED_LOCK_ERROR | `'L'` | Could not lock record for update |
| CHANGES_OKAYED_BUT_FAILED | `'F'` | Lock acquired but REWRITE failed |

### 2.3 Optimistic Locking (9300-CHECK-CHANGE-IN-REC)

Before writing, the program re-reads the record with UPDATE lock and compares:
- `CARD-CVV-CD` vs original
- `CARD-EMBOSSED-NAME` (uppercased) vs original
- `CARD-EXPIRAION-DATE(1:4)` (year) vs original
- `CARD-EXPIRAION-DATE(6:2)` (month) vs original
- `CARD-EXPIRAION-DATE(9:2)` (day) vs original
- `CARD-ACTIVE-STATUS` vs original

If ANY field differs, the update is aborted with "Record changed by some one else."

### 2.4 Change Detection (Case-Insensitive)

```cobol
IF (FUNCTION UPPER-CASE(CCUP-NEW-CARDDATA) EQUAL
    FUNCTION UPPER-CASE(CCUP-OLD-CARDDATA))
    SET NO-CHANGES-DETECTED TO TRUE
```

Card data compared includes: name, expiry year, month, day, and active status.
If no changes detected, no write is performed.

### 2.5 Name Normalization

On read, embossed name is converted to uppercase:
```cobol
INSPECT CARD-EMBOSSED-NAME CONVERTING LIT-LOWER TO LIT-UPPER
```

### 2.6 Input Normalization

Asterisk `'*'` and spaces are treated as blank/LOW-VALUES for all input fields
(lines 589-635). This is a CICS convention for "no input provided."

### 2.7 Edge Cases Handled

1. **No search criteria**: Both account and card blank = "No input received"
2. **Card not found**: "Did not find cards for this search condition"
3. **No changes made**: "No change detected with respect to values fetched"
4. **Concurrent modification**: Optimistic lock failure detected and reported
5. **Record lock failure**: "Could not lock record for update"
6. **Rewrite failure after lock**: "Update of record failed"
7. **Asterisk input**: Treated as blank across all fields

---

## 3. COCRDSLC.cbl - Credit Card Selection

### 3.1 Validation Rules

| Field | Rule | Valid Range | COBOL Reference |
|-------|------|-------------|----------------|
| Account ID | Numeric, 11 digits, non-zero | `00000000001` - `99999999999` | 2210-EDIT-ACCOUNT (647-683) |
| Card Number | Numeric, 16 digits, non-zero | `0000000000000001` - `9999999999999999` | 2220-EDIT-CARD (685-724) |

### 3.2 Cross-Field Validation

Both fields blank = "No input received" (lines 637-640).

### 3.3 Branching / Decision Logic

```
START
  |
  v
PF03 pressed? --> Exit to calling program or main menu
  |
  v
Coming from COCRDLIC (list screen)?
  --> YES: Input already validated, read data directly, display
  --> NO:
       First entry (PGM-ENTER)? --> Show blank search form
       Re-entry (PGM-REENTER)? --> Validate inputs
           |
           v
       Input error? --> Show form with error messages
           |
           NO
           v
       Read card data by card number (primary key)
       Display card details
```

### 3.4 Card Lookup Methods

1. **By card number (primary key)**: `9100-GETCARD-BYACCTCARD` reads CARDDAT file
   using card number as RIDFLD (lines 736-773)
2. **By account ID (alternate index)**: `9150-GETCARD-BYACCT` reads CARDAIX file
   using account ID (lines 779-812)

### 3.5 Screen Attribute Logic

| Context | Account/Card Fields | Behavior |
|---------|-------------------|----------|
| From COCRDLIC (list screen) | Protected (DFHBMPRF) | Read-only, user came with pre-selected card |
| Direct entry | Unprotected (DFHBMFSE) | Editable, user must enter search criteria |

### 3.6 Visual Feedback

- **Invalid fields**: Highlighted in red (DFHRED)
- **Blank fields on re-entry**: Show asterisk `'*'` in red
- **Info messages**: Hidden (DFHBMDAR) when no info; neutral color otherwise

### 3.7 Edge Cases Handled

1. **Account not found via alternate index**: "Did not find this account in cards database"
2. **Card not found by card number**: "Did not find cards for this search condition"
3. **File read error**: Generic error with operation name, file name, RESP/RESP2 codes
4. **Unexpected data scenario**: ABEND with code '0001'
5. **No EIBCALEN**: First invocation initializes all working storage areas
6. **Asterisk/spaces input**: Normalized to LOW-VALUES (blank)
