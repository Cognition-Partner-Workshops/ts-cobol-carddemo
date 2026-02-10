# CardDemo Application - Logical Data Model

> **Analysis Date:** 2026-02-10
> **Duration:** ~30 minutes
> **Method:** Code-only analysis of copybooks, file definitions, and program CRUD operations
> **Repository:** aws-mainframe-modernization-carddemo

---

## Entity Overview

The CardDemo system manages **8 core entities** stored in VSAM files. All record layouts are defined in copybooks under `app/cpy/`.

| # | Entity | Copybook | Record Length | VSAM Type | Primary Key | File DD Name |
|---|--------|----------|---------------|-----------|-------------|-------------|
| 1 | Account | `CVACT01Y.cpy` | 300 | KSDS | ACCT-ID (9(11)) | ACCTDAT |
| 2 | Card | `CVACT02Y.cpy` | 150 | KSDS | CARD-NUM (X(16)) | CARDDAT |
| 3 | Card Cross-Reference | `CVACT03Y.cpy` | 50 | KSDS | XREF-CARD-NUM (X(16)) | CARDXREF |
| 4 | Customer | `CUSTREC.cpy` | 500 | KSDS | CUST-ID (9(09)) | CUSTDAT |
| 5 | Transaction | `CVTRA05Y.cpy` | 350 | KSDS | TRAN-ID (X(16)) | TRANSACT |
| 6 | Daily Transaction | `CVTRA06Y.cpy` | 350 | Sequential (PS) | DALYTRAN-ID (X(16)) | DALYTRAN |
| 7 | Transaction Category Balance | `CVTRA01Y.cpy` | 50 | KSDS | Composite: ACCT-ID + TYPE-CD + CAT-CD | TCATBAL |
| 8 | Disclosure Group | `CVTRA02Y.cpy` | 50 | KSDS | Composite: GROUP-ID + TYPE-CD + CAT-CD | DISCGRP |
| 9 | User Security | `CSUSR01Y.cpy` | 80 | KSDS | SEC-USR-ID (X(08)) | USRSEC |

---

## Entity Detail

### 1. Account (ACCOUNT-RECORD)

**Copybook:** `CVACT01Y.cpy` (lines 1-21)
**Business Meaning:** Represents a credit card account with its financial limits, balances, and lifecycle dates.

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `ACCT-ID` | 9(11) | Unique 11-digit account identifier. Primary key for account lookups. |
| `ACCT-ACTIVE-STATUS` | X(01) | Current status of the account (active/inactive). |
| `ACCT-CURR-BAL` | S9(10)V99 | Current account balance. Updated by transaction posting (`CBTRN02C.cbl` line 547) and bill payment (`COBIL00C.cbl` line 234). |
| `ACCT-CREDIT-LIMIT` | S9(10)V99 | Maximum credit allowed. Used in over-limit validation (`CBTRN02C.cbl` line 407). |
| `ACCT-CASH-CREDIT-LIMIT` | S9(10)V99 | Maximum cash advance credit allowed. |
| `ACCT-OPEN-DATE` | X(10) | Date the account was opened. Displayed in account view (`COACTVWC.cbl` line 487). |
| `ACCT-EXPIRAION-DATE` | X(10) | Account expiration date. Used in expiration validation (`CBTRN02C.cbl` line 414). |
| `ACCT-REISSUE-DATE` | X(10) | Date the account/card was last reissued. |
| `ACCT-CURR-CYC-CREDIT` | S9(10)V99 | Current billing cycle credit total. Updated during posting: credits added here (`CBTRN02C.cbl` line 549). |
| `ACCT-CURR-CYC-DEBIT` | S9(10)V99 | Current billing cycle debit total. Updated during posting: debits added here (`CBTRN02C.cbl` line 551). |
| `ACCT-ADDR-ZIP` | X(10) | Account holder's zip code. |
| `ACCT-GROUP-ID` | X(10) | Discount/disclosure group identifier. Links to Disclosure Group entity for interest rate lookup (`CBACT04C.cbl` line 436). |
| `FILLER` | X(178) | Reserved space. |

---

### 2. Card (CARD-RECORD)

**Copybook:** `CVACT02Y.cpy` (lines 1-15)
**Business Meaning:** Represents a physical credit card with its security and identification attributes.

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `CARD-NUM` | X(16) | 16-digit card number. Primary key. Used for card lookups and displayed in card detail view (`COCRDSLC.cbl`). |
| `CARD-ACCT-ID` | 9(11) | Account this card belongs to. Foreign key to Account entity. |
| `CARD-CVV-CD` | 9(03) | 3-digit Card Verification Value code. Security field. |
| `CARD-EMBOSSED-NAME` | X(50) | Cardholder name as embossed on the physical card. |
| `CARD-EXPIRAION-DATE` | X(10) | Card expiration date. Validated during card update (`COCRDUPC.cbl` lines 900-943). |
| `CARD-ACTIVE-STATUS` | X(01) | Card active/inactive status. |
| `FILLER` | X(59) | Reserved space. |

---

### 3. Card Cross-Reference (CARD-XREF-RECORD)

**Copybook:** `CVACT03Y.cpy` (lines 1-12)
**Business Meaning:** Maps card numbers to customer and account IDs. Serves as the central lookup table to resolve relationships between cards, customers, and accounts. Accessed via primary key (card number) and alternate index (account ID).

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `XREF-CARD-NUM` | X(16) | Card number. Primary key. Used for card-to-account resolution. |
| `XREF-CUST-ID` | 9(09) | Customer ID who owns this card. Foreign key to Customer entity. |
| `XREF-ACCT-ID` | 9(11) | Account ID this card belongs to. Foreign key to Account entity. |
| `FILLER` | X(14) | Reserved space. |

**Alternate Indexes:**
- **CARDAIX / CXACAIX:** Alternate index by `XREF-ACCT-ID`, used to look up cards by account (`COACTVWC.cbl` line 728, `COBIL00C.cbl` line 211, `COTRN02C.cbl` line 208).

---

### 4. Customer (CUSTOMER-RECORD)

**Copybook:** `CUSTREC.cpy` (lines 1-27)
**Business Meaning:** Represents a customer (cardholder) with personal, contact, and identification information.

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `CUST-ID` | 9(09) | Unique 9-digit customer identifier. Primary key. |
| `CUST-FIRST-NAME` | X(25) | Customer's first name. |
| `CUST-MIDDLE-NAME` | X(25) | Customer's middle name. |
| `CUST-LAST-NAME` | X(25) | Customer's last name. |
| `CUST-ADDR-LINE-1` | X(50) | Street address line 1. |
| `CUST-ADDR-LINE-2` | X(50) | Street address line 2. |
| `CUST-ADDR-LINE-3` | X(50) | City (used as city field per `COACTVWC.cbl` line 513: `MOVE CUST-ADDR-LINE-3 TO ACSCITYO`). |
| `CUST-ADDR-STATE-CD` | X(02) | US state code (2 characters). Validated in `COACTUPC.cbl` line 1600. |
| `CUST-ADDR-COUNTRY-CD` | X(03) | Country code (3 characters). |
| `CUST-ADDR-ZIP` | X(10) | ZIP code. Validated as numeric, 5 digits in `COACTUPC.cbl` lines 1605-1611. |
| `CUST-PHONE-NUM-1` | X(15) | Primary phone number. US format validated in `COACTUPC.cbl` lines 1632-1638. |
| `CUST-PHONE-NUM-2` | X(15) | Secondary phone number. |
| `CUST-SSN` | 9(09) | Social Security Number. Displayed formatted as xxx-xx-xxxx (`COACTVWC.cbl` lines 496-504). |
| `CUST-GOVT-ISSUED-ID` | X(20) | Government-issued identification number. |
| `CUST-DOB-YYYYMMDD` | X(10) | Date of birth in YYYYMMDD format. |
| `CUST-EFT-ACCOUNT-ID` | X(10) | Electronic Funds Transfer account identifier. |
| `CUST-PRI-CARD-HOLDER-IND` | X(01) | Primary cardholder indicator (Y/N). Validated in `COACTUPC.cbl` lines 1657-1662. |
| `CUST-FICO-CREDIT-SCORE` | 9(03) | FICO credit score. Valid range: 300-850 (`COACTUPC.cbl` lines 848-849). |
| `FILLER` | X(168) | Reserved space. |

---

### 5. Transaction (TRAN-RECORD)

**Copybook:** `CVTRA05Y.cpy` (lines 1-22)
**Business Meaning:** Represents a posted financial transaction against a credit card account. Contains transaction details, merchant information, and timestamps.

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `TRAN-ID` | X(16) | Unique transaction identifier. Primary key. Auto-incremented for new transactions (`COBIL00C.cbl` lines 216-218). |
| `TRAN-TYPE-CD` | X(02) | Transaction type code. '01' = interest charge (`CBACT04C.cbl` line 482), '02' = bill payment (`COBIL00C.cbl` line 220). |
| `TRAN-CAT-CD` | 9(04) | Transaction category code. '05' = interest (`CBACT04C.cbl` line 483), '2' = bill payment (`COBIL00C.cbl` line 221). |
| `TRAN-SOURCE` | X(10) | Transaction source. 'System' for interest charges (`CBACT04C.cbl` line 484), 'POS TERM' for bill payments (`COBIL00C.cbl` line 222). |
| `TRAN-DESC` | X(100) | Free-text description. 'Int. for a/c {ID}' for interest (`CBACT04C.cbl` lines 485-489), 'BILL PAYMENT - ONLINE' for payments (`COBIL00C.cbl` line 223). |
| `TRAN-AMT` | S9(09)V99 | Transaction amount (signed). Positive = credit, negative = debit. |
| `TRAN-MERCHANT-ID` | 9(09) | Merchant identifier. 999999999 for bill payments (`COBIL00C.cbl` line 226), 0 for interest (`CBACT04C.cbl` line 491). |
| `TRAN-MERCHANT-NAME` | X(50) | Merchant name. 'BILL PAYMENT' for payments (`COBIL00C.cbl` line 227). |
| `TRAN-MERCHANT-CITY` | X(50) | Merchant city. |
| `TRAN-MERCHANT-ZIP` | X(10) | Merchant ZIP code. |
| `TRAN-CARD-NUM` | X(16) | Card number used for this transaction. Links to Card entity. |
| `TRAN-ORIG-TS` | X(26) | Origination timestamp. When the transaction occurred. |
| `TRAN-PROC-TS` | X(26) | Processing timestamp. When the transaction was processed/posted. Set to DB2-format timestamp in batch (`CBTRN02C.cbl` line 438). |
| `FILLER` | X(20) | Reserved space. |

---

### 6. Daily Transaction (DALYTRAN-RECORD)

**Copybook:** `CVTRA06Y.cpy` (lines 1-22)
**Business Meaning:** Represents an unposted daily transaction awaiting batch processing. Identical structure to Transaction but stored in a sequential file for batch input.

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `DALYTRAN-ID` | X(16) | Transaction identifier from the originating system. |
| `DALYTRAN-TYPE-CD` | X(02) | Transaction type code. |
| `DALYTRAN-CAT-CD` | 9(04) | Transaction category code. |
| `DALYTRAN-SOURCE` | X(10) | Originating source system. |
| `DALYTRAN-DESC` | X(100) | Transaction description. |
| `DALYTRAN-AMT` | S9(09)V99 | Transaction amount. Used in credit limit check (`CBTRN02C.cbl` line 405). |
| `DALYTRAN-MERCHANT-ID` | 9(09) | Merchant identifier. |
| `DALYTRAN-MERCHANT-NAME` | X(50) | Merchant name. |
| `DALYTRAN-MERCHANT-CITY` | X(50) | Merchant city. |
| `DALYTRAN-MERCHANT-ZIP` | X(10) | Merchant ZIP code. |
| `DALYTRAN-CARD-NUM` | X(16) | Card number. Used for cross-reference lookup (`CBTRN02C.cbl` line 382). |
| `DALYTRAN-ORIG-TS` | X(26) | Origination timestamp. First 10 chars compared to expiration date (`CBTRN02C.cbl` line 414). |
| `DALYTRAN-PROC-TS` | X(26) | Processing timestamp (set during batch posting). |
| `FILLER` | X(20) | Reserved space. |

---

### 7. Transaction Category Balance (TRAN-CAT-BAL-RECORD)

**Copybook:** `CVTRA01Y.cpy` (lines 1-14)
**Business Meaning:** Tracks the running balance for each combination of account, transaction type, and category. Used as the basis for interest calculation.

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `TRAN-CAT-KEY` | (composite) | Composite primary key: |
| — `TRANCAT-ACCT-ID` | 9(11) | Account ID. Foreign key to Account entity. |
| — `TRANCAT-TYPE-CD` | X(02) | Transaction type code. |
| — `TRANCAT-CD` | 9(04) | Transaction category code. |
| `TRAN-CAT-BAL` | S9(09)V99 | Running balance for this account/type/category combination. Updated during posting (`CBTRN02C.cbl` lines 508, 527). Used in interest calculation (`CBACT04C.cbl` line 464). |
| `FILLER` | X(22) | Reserved space. |

---

### 8. Disclosure Group (DIS-GROUP-RECORD)

**Copybook:** `CVTRA02Y.cpy` (lines 1-14)
**Business Meaning:** Defines interest rates for specific combinations of account group, transaction type, and category. Used during interest calculation to determine the applicable annual interest rate.

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `DIS-GROUP-KEY` | (composite) | Composite primary key: |
| — `DIS-ACCT-GROUP-ID` | X(10) | Account group identifier. Matches `ACCT-GROUP-ID` from Account entity. Falls back to 'DEFAULT' if not found (`CBACT04C.cbl` line 437). |
| — `DIS-TRAN-TYPE-CD` | X(02) | Transaction type code. |
| — `DIS-TRAN-CAT-CD` | 9(04) | Transaction category code. |
| `DIS-INT-RATE` | S9(04)V99 | Annual interest rate. Used in formula: Monthly Interest = (Balance * Rate) / 1200 (`CBACT04C.cbl` line 465). |
| `FILLER` | X(28) | Reserved space. |

---

### 9. User Security (SEC-USER-DATA)

**Copybook:** `CSUSR01Y.cpy` (lines 17-27)
**Business Meaning:** Stores user authentication credentials and role information for system access control.

| Field | PIC | Business Semantics |
|-------|-----|--------------------|
| `SEC-USR-ID` | X(08) | User ID. Primary key. Used for sign-on authentication (`COSGN00C.cbl` line 215). |
| `SEC-USR-FNAME` | X(20) | User's first name. Required for creation (`COUSR01C.cbl` line 118). |
| `SEC-USR-LNAME` | X(20) | User's last name. Required for creation (`COUSR01C.cbl` line 123). |
| `SEC-USR-PWD` | X(08) | User's password (stored in plain text). Compared during sign-on (`COSGN00C.cbl` line 223). |
| `SEC-USR-TYPE` | X(01) | User role type. 'A' = Admin (routes to COADM01C), other = Regular user (routes to COMEN01C). See `COSGN00C.cbl` lines 230-240. |
| `SEC-USR-FILLER` | X(23) | Reserved space. |

---

## Entity Relationships

```
                    ┌──────────────────┐
                    │    Customer      │
                    │   (CUSTDAT)      │
                    │  PK: CUST-ID     │
                    └────────┬─────────┘
                             │
                             │ 1:N (via XREF)
                             │
┌───────────────┐   ┌────────┴─────────┐   ┌──────────────────┐
│   Account     │───│  Card Cross-Ref  │───│     Card         │
│  (ACCTDAT)    │   │   (CARDXREF)     │   │   (CARDDAT)      │
│ PK: ACCT-ID   │   │ PK: XREF-CARD-NUM│   │ PK: CARD-NUM     │
└───────┬───────┘   │ FK: XREF-ACCT-ID │   │ FK: CARD-ACCT-ID │
        │           │ FK: XREF-CUST-ID │   └──────────────────┘
        │           └──────────────────┘
        │
        │ 1:N
        │
┌───────┴────────┐   ┌──────────────────┐
│  Transaction   │   │ Daily Transaction│
│  (TRANSACT)    │   │   (DALYTRAN)     │
│ PK: TRAN-ID    │   │ PK: DALYTRAN-ID  │
│ FK: TRAN-CARD- │   │ FK: DALYTRAN-    │
│     NUM        │   │     CARD-NUM     │
└────────────────┘   └──────────────────┘
        │
        │ Aggregated into
        │
┌───────┴─────────┐   ┌──────────────────┐
│ Tran Category   │   │ Disclosure Group │
│   Balance       │───│   (DISCGRP)      │
│  (TCATBAL)      │   │ PK: GROUP-ID +   │
│ PK: ACCT-ID +   │   │     TYPE + CAT   │
│     TYPE + CAT  │   └──────────────────┘
└─────────────────┘
        │                      │
        └──────────┬───────────┘
                   │
            Interest Rate
              Lookup via
           ACCT-GROUP-ID

┌──────────────────┐
│  User Security   │
│   (USRSEC)       │
│ PK: SEC-USR-ID   │
└──────────────────┘
  (Standalone - no FK
   relationship to
   business entities)
```

### Relationship Details

| Relationship | Type | Linking Mechanism | Evidence |
|-------------|------|-------------------|----------|
| Customer → Card Cross-Ref | 1:N | `XREF-CUST-ID` references `CUST-ID` | `COACTVWC.cbl` line 739: `MOVE XREF-CUST-ID TO CDEMO-CUST-ID` |
| Account → Card Cross-Ref | 1:N | `XREF-ACCT-ID` references `ACCT-ID` | `CBTRN02C.cbl` line 394: `MOVE XREF-ACCT-ID TO FD-ACCT-ID` |
| Card → Card Cross-Ref | 1:1 | `XREF-CARD-NUM` references `CARD-NUM` | `CBTRN01C.cbl` line 171: `MOVE DALYTRAN-CARD-NUM TO XREF-CARD-NUM` |
| Card → Account | N:1 | `CARD-ACCT-ID` references `ACCT-ID` | `CVACT02Y.cpy` line 6: `CARD-ACCT-ID PIC 9(11)` |
| Account → Transaction | 1:N | Via Card Cross-Ref (TRAN-CARD-NUM → XREF → ACCT-ID) | Indirect: `COTRN02C.cbl` lines 206-209 resolves account to card to write transaction |
| Transaction → Card | N:1 | `TRAN-CARD-NUM` references `CARD-NUM` | `CVTRA05Y.cpy` line 15: `TRAN-CARD-NUM PIC X(16)` |
| Daily Transaction → Card | N:1 | `DALYTRAN-CARD-NUM` references `CARD-NUM` (via XREF) | `CBTRN02C.cbl` line 382: `MOVE DALYTRAN-CARD-NUM TO FD-XREF-CARD-NUM` |
| Account → Tran Category Balance | 1:N | `TRANCAT-ACCT-ID` references `ACCT-ID` | `CBTRN02C.cbl` line 469: `MOVE XREF-ACCT-ID TO FD-TRANCAT-ACCT-ID` |
| Account → Disclosure Group | N:1 | `ACCT-GROUP-ID` references `DIS-ACCT-GROUP-ID` | `CBACT04C.cbl` line 436: status '23' triggers DEFAULT group lookup |
| Tran Category Balance → Disclosure Group | Lookup | Matched by TYPE-CD + CAT-CD | `CBACT04C.cbl` lines 464-465: interest formula uses both |

---

## CRUD Matrix

### Programs That Create, Read, Update, or Delete Each Entity

| Entity | Create (C) | Read (R) | Update (U) | Delete (D) |
|--------|-----------|----------|-----------|-----------|
| **Account** | `CBACT01C` (batch load) | `COACTVWC` (view), `COACTUPC` (update), `COBIL00C` (bill pay), `CBTRN02C` (posting), `CBACT04C` (interest), `CBSTM03A` (statement) | `COACTUPC` (online update), `COBIL00C` (balance after payment), `CBTRN02C` (balance after posting), `CBACT04C` (interest posting) | Not found in code |
| **Card** | `CBACT02C` (batch load) | `COCRDSLC` (view), `COCRDLIC` (list), `COCRDUPC` (update) | `COCRDUPC` (online update) | Not found in code |
| **Card Cross-Ref** | `CBACT03C` (batch load) | `COACTVWC` (acct view), `COBIL00C` (bill pay), `COTRN02C` (trn add), `CBTRN01C` (batch verify), `CBTRN02C` (batch post), `CBTRN03C` (report), `CBACT04C` (interest) | Not found in code | Not found in code |
| **Customer** | `CBCUS01C` (batch load) | `COACTVWC` (view with account), `COACTUPC` (update), `CBSTM03A` (statement) | `COACTUPC` (online update) | Not found in code |
| **Transaction** | `COTRN02C` (online add), `COBIL00C` (bill payment), `CBTRN02C` (batch post), `CBACT04C` (interest charge) | `COTRN00C` (list), `COTRN01C` (view), `COBIL00C` (browse for ID), `CBTRN03C` (report), `CBSTM03A` (statement) | Not found in code | Not found in code |
| **Daily Transaction** | External system (input file) | `CBTRN01C` (batch verify), `CBTRN02C` (batch post) | Not found in code | Not found in code |
| **Tran Category Balance** | `CBTRN02C` (`2700-A-CREATE-TCATBAL-REC`) | `CBTRN02C` (`2700-UPDATE-TCATBAL`), `CBACT04C` (interest calc) | `CBTRN02C` (`2700-B-UPDATE-TCATBAL-REC`) | Not found in code |
| **Disclosure Group** | External configuration | `CBACT04C` (interest rate lookup) | Not found in code | Not found in code |
| **User Security** | `COUSR01C` (add user) | `COSGN00C` (sign-on), `COUSR00C` (list), `COUSR02C` (update), `COUSR03C` (delete) | `COUSR02C` (update user) | `COUSR03C` (delete user) |

### CRUD Evidence References

| Entity | Operation | Program | Code Reference |
|--------|-----------|---------|----------------|
| Account | Read | `COACTVWC.cbl` | Line 776: `EXEC CICS READ DATASET(LIT-ACCTFILENAME)` |
| Account | Read | `CBTRN02C.cbl` | Line 395: `READ ACCOUNT-FILE INTO ACCOUNT-RECORD` |
| Account | Update | `COACTUPC.cbl` | Write processing section (REWRITE of ACCTDAT) |
| Account | Update | `COBIL00C.cbl` | Line 235: `PERFORM UPDATE-ACCTDAT-FILE` |
| Account | Update | `CBTRN02C.cbl` | Line 554: `REWRITE FD-ACCTFILE-REC FROM ACCOUNT-RECORD` |
| Card | Read | `COCRDSLC.cbl` | `9000-READ-DATA` section reads CARDDAT |
| Card | Update | `COCRDUPC.cbl` | `9200-WRITE-PROCESSING` section (REWRITE of CARDDAT) |
| Card XREF | Read | `COACTVWC.cbl` | Line 728: `EXEC CICS READ DATASET(LIT-CARDXREFNAME-ACCT-PATH)` |
| Card XREF | Read | `CBTRN02C.cbl` | Line 383: `READ XREF-FILE INTO CARD-XREF-RECORD` |
| Customer | Read | `COACTVWC.cbl` | `9400-GETCUSTDATA-BYCUST` section |
| Customer | Update | `COACTUPC.cbl` | Customer fields update in write processing |
| Transaction | Create | `COTRN02C.cbl` | `ADD-TRANSACTION` section writes to TRANSACT |
| Transaction | Create | `COBIL00C.cbl` | Line 233: `PERFORM WRITE-TRANSACT-FILE` |
| Transaction | Create | `CBTRN02C.cbl` | Line 564: `WRITE FD-TRANFILE-REC FROM TRAN-RECORD` |
| Transaction | Create | `CBACT04C.cbl` | Line 500: `WRITE FD-TRANFILE-REC FROM TRAN-RECORD` |
| Transaction | Read | `COTRN01C.cbl` | Line 270: `EXEC CICS READ DATASET(WS-TRANSACT-FILE)` |
| TCATBAL | Create | `CBTRN02C.cbl` | Line 510: `WRITE FD-TRAN-CAT-BAL-RECORD FROM TRAN-CAT-BAL-RECORD` |
| TCATBAL | Update | `CBTRN02C.cbl` | Line 528: `REWRITE FD-TRAN-CAT-BAL-RECORD FROM TRAN-CAT-BAL-RECORD` |
| TCATBAL | Read | `CBACT04C.cbl` | Sequential read in interest calculation main loop |
| Disclosure Group | Read | `CBACT04C.cbl` | Interest rate lookup with DEFAULT fallback (line 436) |
| User Security | Create | `COUSR01C.cbl` | `WRITE-USER-SEC-FILE` section |
| User Security | Read | `COSGN00C.cbl` | Line 211: `EXEC CICS READ DATASET(WS-USRSEC-FILE)` |
| User Security | Update | `COUSR02C.cbl` | `UPDATE-USER-SEC-FILE` section |
| User Security | Delete | `COUSR03C.cbl` | Line 307: `EXEC CICS DELETE DATASET(WS-USRSEC-FILE)` |

---

## Data Flow Patterns

### 1. Card-to-Account Resolution
Multiple programs need to resolve a card number to an account. The standard pattern is:
1. Use `DALYTRAN-CARD-NUM` or input card number
2. Look up `CARDXREF` by card number (primary key) or by account ID (alternate index CARDAIX/CXACAIX)
3. Extract `XREF-ACCT-ID` and/or `XREF-CUST-ID`
4. Use account ID to read `ACCTDAT`

**Programs using this pattern:** `COACTVWC`, `COBIL00C`, `COTRN02C`, `CBTRN01C`, `CBTRN02C`, `CBACT04C`

### 2. Transaction Posting Pipeline
```
DALYTRAN (sequential input)
    → Validate card via CARDXREF
    → Validate account via ACCTDAT (credit limit, expiration)
    → If valid: WRITE to TRANSACT + UPDATE TCATBAL + UPDATE ACCTDAT
    → If invalid: WRITE to DALYREJS (reject file)
```
**Programs:** `CBTRN02C.cbl` lines 380-579

### 3. Interest Calculation Pipeline
```
TCATBAL (sequential read)
    → For each category balance: lookup rate in DISCGRP (by GROUP-ID + TYPE + CAT)
    → If group not found: use DEFAULT group
    → Calculate: Monthly Interest = (Balance * Rate) / 1200
    → WRITE interest transaction to TRANSACT
    → UPDATE ACCTDAT with new balance
```
**Programs:** `CBACT04C.cbl` lines 462-515

### 4. Alternate Index Access Paths

| Alternate Index | Base File | Alternate Key | Used By |
|----------------|-----------|---------------|---------|
| CARDAIX | CARDDAT | CARD-ACCT-ID | `COCRDSLC.cbl` (card detail by account) |
| CXACAIX | CARDXREF | XREF-ACCT-ID | `COACTVWC.cbl`, `COBIL00C.cbl`, `COTRN02C.cbl` |
