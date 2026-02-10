# CardDemo Application - Business Capabilities

> **Analysis Date:** 2026-02-10
> **Duration:** ~30 minutes
> **Method:** Code-only analysis of COBOL programs, copybooks, BMS maps, and JCL
> **Repository:** aws-mainframe-modernization-carddemo

---

## 1. User Authentication & Session Management

**Description:** Validates user credentials and routes users to appropriate menus based on their role (admin vs regular user).

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `COSGN00C.cbl` | Sign-on screen, credential validation, role-based routing | Lines 108-257 |

### Code Evidence

- **User ID and Password validation:** `COSGN00C.cbl` lines 117-130 — EVALUATE block checks that User ID and Password are not empty before proceeding.
- **Credential lookup:** `COSGN00C.cbl` lines 211-219 — Reads USRSEC file using `EXEC CICS READ DATASET(WS-USRSEC-FILE)` with user ID as key.
- **Password comparison:** `COSGN00C.cbl` line 223 — `IF SEC-USR-PWD = WS-USER-PWD` direct comparison of stored vs entered password.
- **Role-based routing:** `COSGN00C.cbl` lines 230-240 — `IF CDEMO-USRTYP-ADMIN` routes to `COADM01C` (admin menu), else routes to `COMEN01C` (regular menu).
- **User not found handling:** `COSGN00C.cbl` lines 247-251 — Response code 13 (NOTFND) displays 'User not found'.

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| USRSEC | READ | `COSGN00C.cbl` line 212 |

---

## 2. Menu Navigation & Access Control

**Description:** Presents role-appropriate menu options and enforces access restrictions. Admin users see user management options; regular users see account/transaction/card operations.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `COADM01C.cbl` | Admin menu — routes to user management programs | Lines 119-158 |
| `COMEN01C.cbl` | Regular user menu — routes to account/card/transaction programs | Lines 115-191 |

### Code Evidence

- **Admin menu options:** `COADM02Y.cpy` lines 19-54 — 6 options: User List (`COUSR00C`), User Add (`COUSR01C`), User Update (`COUSR02C`), User Delete (`COUSR03C`), Transaction Type List (`COTRTLIC`), Transaction Type Maintenance (`COTRTUPC`).
- **Regular menu options:** `COMEN02Y.cpy` lines 19-91 — 11 options: Account View, Account Update, Credit Card List, Credit Card View, Credit Card Update, Transaction List, Transaction View, Transaction Add, Transaction Reports, Bill Payment, Pending Authorization View.
- **Admin-only restriction:** `COMEN01C.cbl` lines 136-143 — `IF CDEMO-USRTYP-USER AND CDEMO-MENU-OPT-USRTYPE(WS-OPTION) = 'A'` blocks regular users from admin-flagged options with message 'No access - Admin Only option'.
- **XCTL navigation:** `COADM01C.cbl` lines 145-148 — Uses `EXEC CICS XCTL PROGRAM(CDEMO-ADMIN-OPT-PGMNAME(WS-OPTION))` to transfer control.

---

## 3. Account Management

**Description:** View and update account details including balances, credit limits, dates, and associated customer information.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `COACTVWC.cbl` | View account details with customer information | Lines 460-523, 687-715 |
| `COACTUPC.cbl` | Update account and customer data with extensive validation | Lines 858-899, 1600-1775 |

### Code Evidence

- **Account view data retrieval chain:** `COACTVWC.cbl` lines 687-715 — Performs three sequential lookups: `9200-GETCARDXREF-BYACCT` (XREF by account), `9300-GETACCTDATA-BYACCT` (account master), `9400-GETCUSTDATA-BYCUST` (customer data).
- **Account display fields:** `COACTVWC.cbl` lines 473-490 — Displays ACCT-ACTIVE-STATUS, ACCT-CURR-BAL, ACCT-CREDIT-LIMIT, ACCT-CASH-CREDIT-LIMIT, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT, ACCT-OPEN-DATE, ACCT-EXPIRAION-DATE, ACCT-REISSUE-DATE, ACCT-GROUP-ID.
- **Customer display fields:** `COACTVWC.cbl` lines 493-523 — Displays CUST-ID, CUST-SSN (formatted xxx-xx-xxxx), CUST-FICO-CREDIT-SCORE, CUST-DOB, name, address, phone numbers, government ID, EFT account, primary cardholder indicator.
- **Account ID validation:** `COACTVWC.cbl` lines 649-681 — Account ID must be numeric, non-zero, 11 digits.
- **Account update change detection:** `COACTUPC.cbl` lines 1681-1773 — `1205-COMPARE-OLD-NEW` compares all old vs new fields for both account and customer data; only proceeds if `CHANGE-HAS-OCCURRED`.
- **FICO score range validation:** `COACTUPC.cbl` lines 848-849 — `88 FICO-RANGE-IS-VALID VALUES 300 THROUGH 850`.
- **Field-level validation:** `COACTUPC.cbl` lines 1600-1662 — Validates state code, zip code (numeric, 5 digits), city (alpha), country (alpha), phone numbers (US format), EFT account ID (numeric), primary cardholder (Y/N).

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| ACCTDAT | READ, UPDATE | `COACTVWC.cbl` line 777; `COACTUPC.cbl` update processing |
| CARDXREF (AIX) | READ | `COACTVWC.cbl` line 728 |
| CUSTDAT | READ, UPDATE | `COACTVWC.cbl` line 710; `COACTUPC.cbl` update processing |

---

## 4. Credit Card Management

**Description:** List, view, and update credit card details including card number, CVV, embossed name, expiration date, and active status.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `COCRDLIC.cbl` | List credit cards for an account | Menu option 3 in `COMEN02Y.cpy` line 40 |
| `COCRDSLC.cbl` | View credit card detail | Lines 1-4, 247-370 |
| `COCRDUPC.cbl` | Update credit card details with validation | Lines 900-1027 |

### Code Evidence

- **Card detail retrieval:** `COCRDSLC.cbl` lines 339-347 — When coming from card list, reads card data using pre-validated account and card number via `9000-READ-DATA`.
- **Card data file access:** `COCRDSLC.cbl` lines 187-190 — Reads CARDDAT file (`LIT-CARDFILENAME VALUE 'CARDDAT'`) and CARDAIX alternate index (`LIT-CARDFILENAME-ACCT-PATH VALUE 'CARDAIX'`).
- **Card update expiry month validation:** `COCRDUPC.cbl` lines 900-907 — Validates expiry month is within valid range; sets `FLG-CARDEXPMON-NOT-OK` if invalid.
- **Card update expiry year validation:** `COCRDUPC.cbl` lines 913-943 — Validates expiry year is numeric and within `VALID-YEAR` range.
- **Card update write processing:** `COCRDUPC.cbl` lines 988-1001 — `9200-WRITE-PROCESSING` handles optimistic locking: checks for concurrent modification (`DATA-WAS-CHANGED-BEFORE-UPDATE`), lock errors (`COULD-NOT-LOCK-FOR-UPDATE`), and update failures (`LOCKED-BUT-UPDATE-FAILED`).

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| CARDDAT | READ, UPDATE | `COCRDSLC.cbl` line 188; `COCRDUPC.cbl` write processing |
| CARDAIX | READ | `COCRDSLC.cbl` line 190 |
| CARDXREF | READ | `COCRDSLC.cbl` via xref lookups |

---

## 5. Transaction Management (Online)

**Description:** List, view, and add credit card transactions through the online CICS interface.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `COTRN00C.cbl` | List transactions | Menu option 6 in `COMEN02Y.cpy` line 58 |
| `COTRN01C.cbl` | View a single transaction by ID | Lines 144-192 |
| `COTRN02C.cbl` | Add a new transaction with validation | Lines 164-299 |

### Code Evidence

- **Transaction view:** `COTRN01C.cbl` lines 144-192 — `PROCESS-ENTER-KEY` validates transaction ID is non-empty, reads TRANSACT file, displays all fields (ID, card number, type code, category code, source, amount, description, origination date, processing date, merchant ID/name/city/zip).
- **Transaction read:** `COTRN01C.cbl` lines 267-296 — `READ-TRANSACT-FILE` uses `EXEC CICS READ DATASET(WS-TRANSACT-FILE)` with TRAN-ID as key.
- **Transaction add - key validation:** `COTRN02C.cbl` lines 193-230 — `VALIDATE-INPUT-KEY-FIELDS` requires either Account ID or Card Number (numeric). Performs cross-reference lookup to resolve the other field.
- **Transaction add - data validation:** `COTRN02C.cbl` lines 235-299 — `VALIDATE-INPUT-DATA-FIELDS` requires all fields non-empty: Type CD, Category CD, Source, Description, Amount, Orig Date, Proc Date, Merchant ID.
- **Transaction add - confirmation:** `COTRN02C.cbl` lines 169-188 — Requires explicit 'Y' confirmation before writing; validates confirm field accepts only Y/N.

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| TRANSACT | READ, WRITE | `COTRN01C.cbl` line 270; `COTRN02C.cbl` add processing |
| CXACAIX | READ | `COTRN02C.cbl` line 208 (account-to-card xref) |
| CCXREF | READ | `COTRN02C.cbl` line 222 (card-to-account xref) |

---

## 6. Bill Payment

**Description:** Process bill payments by creating a payment transaction and reducing the account balance.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `COBIL00C.cbl` | Bill payment processing | Lines 154-244 |

### Code Evidence

- **Account balance check:** `COBIL00C.cbl` lines 198-205 — `IF ACCT-CURR-BAL <= ZEROS` rejects payment with 'You have nothing to pay'.
- **Payment confirmation:** `COBIL00C.cbl` lines 173-191 — Requires explicit 'Y'/'y' confirmation; validates confirm field.
- **Transaction creation:** `COBIL00C.cbl` lines 211-233 — Creates transaction record with: type '02', category 2, source 'POS TERM', description 'BILL PAYMENT - ONLINE', amount = current balance, merchant ID 999999999, merchant name 'BILL PAYMENT'.
- **Transaction ID generation:** `COBIL00C.cbl` lines 212-218 — Reads last transaction (READPREV from HIGH-VALUES), increments ID by 1.
- **Balance update:** `COBIL00C.cbl` line 234 — `COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT`.

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| ACCTDAT | READ, UPDATE | `COBIL00C.cbl` lines 177, 235 |
| TRANSACT | READ (browse), WRITE | `COBIL00C.cbl` lines 213-214, 233 |
| CXACAIX | READ | `COBIL00C.cbl` line 211 |

---

## 7. User Administration (Security)

**Description:** Full CRUD operations on the user security file (USRSEC) — list, add, update, and delete system users.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `COUSR00C.cbl` | List users | Admin menu option 1 in `COADM02Y.cpy` line 29 |
| `COUSR01C.cbl` | Add new user | Lines 115-266 |
| `COUSR02C.cbl` | Update existing user | Lines 215-242 |
| `COUSR03C.cbl` | Delete user | Lines 305-322 |

### Code Evidence

- **User add - field validation:** `COUSR01C.cbl` lines 115-142 — Requires all fields non-empty: First Name, Last Name, User ID, Password, User Type.
- **User add - duplicate check:** `COUSR01C.cbl` lines 260-264 — On `DFHRESP(DUPKEY)` or `DFHRESP(DUPREC)`, displays 'User ID already exist...'.
- **User update - change detection:** `COUSR02C.cbl` lines 215-242 — Compares each field (first name, last name, password, user type) individually; sets `USR-MODIFIED-YES` only if at least one field changed. Rejects with 'Please modify to update' if no changes.
- **User delete - permanent removal:** `COUSR03C.cbl` lines 305-322 — `EXEC CICS DELETE DATASET(WS-USRSEC-FILE)` permanently removes the user record.

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| USRSEC | READ, WRITE, REWRITE, DELETE | `COUSR01C.cbl` (WRITE), `COUSR02C.cbl` (REWRITE), `COUSR03C.cbl` (DELETE) |

---

## 8. Batch Transaction Posting

**Description:** Nightly batch process that reads daily transaction files, validates each transaction against card cross-reference and account data, and posts valid transactions to the master transaction file. Rejected transactions are written to a reject file.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `CBTRN01C.cbl` | Read daily transactions and validate card/account existence | Lines 154-197 |
| `CBTRN02C.cbl` | Full validation (card, account, credit limit, expiration) and posting | Lines 380-579 |

### Code Evidence

- **CBTRN01C processing loop:** `CBTRN01C.cbl` lines 164-186 — Reads DALYTRAN sequentially, looks up XREF by card number, reads account record.
- **CBTRN02C card validation:** `CBTRN02C.cbl` lines 380-391 — `1500-A-LOOKUP-XREF` reads XREF file; INVALID KEY sets rejection code 100.
- **CBTRN02C account validation:** `CBTRN02C.cbl` lines 393-421 — `1500-B-LOOKUP-ACCT` reads account file; INVALID KEY sets rejection code 101. Checks credit limit (`IF ACCT-CREDIT-LIMIT >= WS-TEMP-BAL`, rejection code 102) and expiration date (`IF ACCT-EXPIRAION-DATE >= DALYTRAN-ORIG-TS`, rejection code 103).
- **Transaction posting:** `CBTRN02C.cbl` lines 424-444 — `2000-POST-TRANSACTION` maps daily transaction fields to master transaction record, calls `2700-UPDATE-TCATBAL`, `2800-UPDATE-ACCOUNT-REC`, `2900-WRITE-TRANSACTION-FILE`.
- **Account balance update:** `CBTRN02C.cbl` lines 545-559 — `2800-UPDATE-ACCOUNT-REC` adds transaction amount to `ACCT-CURR-BAL`; credits go to `ACCT-CURR-CYC-CREDIT`, debits go to `ACCT-CURR-CYC-DEBIT`.
- **Category balance update:** `CBTRN02C.cbl` lines 467-542 — `2700-UPDATE-TCATBAL` creates or updates TCATBAL record by adding transaction amount to category balance.
- **Reject file writing:** `CBTRN02C.cbl` lines 446-465 — `2500-WRITE-REJECT-REC` writes failed transactions with validation trailer.

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| DALYTRAN | READ (sequential) | `CBTRN02C.cbl` main processing loop |
| CARDXREF | READ | `CBTRN02C.cbl` line 383 |
| ACCTDAT | READ, REWRITE | `CBTRN02C.cbl` lines 395, 554 |
| TRANSACT | WRITE | `CBTRN02C.cbl` line 564 |
| TCATBAL | READ, WRITE, REWRITE | `CBTRN02C.cbl` lines 474, 510, 528 |
| DALYREJS | WRITE | `CBTRN02C.cbl` line 451 |

---

## 9. Interest Calculation

**Description:** Monthly batch process that calculates interest charges per transaction category for each account and writes system-generated interest transactions.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `CBACT04C.cbl` | Calculate monthly interest and generate interest transactions | Lines 462-515 |

### Code Evidence

- **Interest formula:** `CBACT04C.cbl` lines 462-470 — `1300-COMPUTE-INTEREST`: `COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200`. Accumulates into `WS-TOTAL-INT`.
- **Interest rate lookup:** `CBACT04C.cbl` lines 436-439 — If discount group not found (status '23'), uses 'DEFAULT' group: `MOVE 'DEFAULT' TO FD-DIS-ACCT-GROUP-ID`.
- **System transaction creation:** `CBACT04C.cbl` lines 482-498 — Creates transaction with type '01', category '05', source 'System', description 'Int. for a/c {ACCT-ID}'.
- **Fee calculation placeholder:** `CBACT04C.cbl` lines 518-520 — `1400-COMPUTE-FEES` contains comment 'To be implemented' — not yet functional.

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| TCATBAL | READ (sequential) | `CBACT04C.cbl` main processing loop |
| DISCGRP | READ | `CBACT04C.cbl` interest rate lookup |
| CARDXREF | READ | `CBACT04C.cbl` card-to-account resolution |
| TRANSACT | WRITE | `CBACT04C.cbl` line 500 |
| ACCTDAT | READ, REWRITE | `CBACT04C.cbl` account balance update |

---

## 10. Transaction Reporting

**Description:** Generate transaction detail reports by reading the transaction file and enriching with cross-reference, transaction type, and category descriptions.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `CBTRN03C.cbl` | Print transaction detail report | Lines 1-57 |
| `CORPT00C.cbl` | Online report submission (submits batch JCL) | Menu option 9 in `COMEN02Y.cpy` line 77 |

### Code Evidence

- **Report file definitions:** `CBTRN03C.cbl` lines 28-57 — Reads TRANSACT (sequential), CARDXREF (indexed by card), TRANTYPE (indexed by type), TRANCATG (indexed by type+category), DATE-PARMS (date parameters). Writes to REPORT-FILE (sequential, 133-byte records).
- **Report enrichment sources:** `CBTRN03C.cbl` lines 39-49 — Joins transaction data with TRANTYPE file (transaction type descriptions) and TRANCATG file (category descriptions).

### Files Accessed

| File | Access | Evidence |
|------|--------|----------|
| TRANSACT | READ (sequential) | `CBTRN03C.cbl` line 29 |
| CARDXREF | READ | `CBTRN03C.cbl` line 33 |
| TRANTYPE | READ | `CBTRN03C.cbl` line 39 |
| TRANCATG | READ | `CBTRN03C.cbl` line 45 |
| REPORT-FILE | WRITE | `CBTRN03C.cbl` line 51 |
| DATE-PARMS | READ | `CBTRN03C.cbl` line 55 |

---

## 11. Statement Generation

**Description:** Generate customer account statements in text and HTML formats.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `CBSTM03A.CBL` | Statement generation (text format) | Header comment: 'Generate account statements' |
| `CBSTM03B.CBL` | Statement generation (HTML format) | Called by CBSTM03A |

### Code Evidence

- **Dual format output:** `CBSTM03A.CBL` generates text-format statements; calls `CBSTM03B.CBL` for HTML-format output.
- **Statement data sources:** Reads account, customer, and transaction data to produce consolidated statements.

---

## 12. Data Migration / Seeding

**Description:** Batch programs and JCL to load initial data into VSAM files from sequential input.

### Programs Involved

| Program | Role | Evidence |
|---------|------|----------|
| `CBACT01C.cbl` | Load account data from sequential to indexed file | Program header |
| `CBACT02C.cbl` | Load card data from sequential to indexed file | Program header |
| `CBACT03C.cbl` | Load card cross-reference data | Program header |
| `CBCUS01C.cbl` | Load customer data from sequential to indexed file | Program header |
| `CBTRN04C.cbl` | Load transaction data from sequential to indexed file | Program header |

### Code Evidence

- **File access pattern:** All programs follow the same pattern: OPEN INPUT (sequential source), OPEN OUTPUT (indexed VSAM target), READ sequentially, WRITE to indexed file until EOF.
- **JCL support:** Multiple JCL scripts (DEFVSAM, REPRO operations) define and populate VSAM clusters.

---

## Capability-to-Program Matrix

| Capability | Online Programs | Batch Programs |
|-----------|----------------|----------------|
| Authentication | COSGN00C | — |
| Menu Navigation | COADM01C, COMEN01C | — |
| Account Management | COACTVWC, COACTUPC | — |
| Credit Card Management | COCRDLIC, COCRDSLC, COCRDUPC | — |
| Transaction Management | COTRN00C, COTRN01C, COTRN02C | — |
| Bill Payment | COBIL00C | — |
| User Administration | COUSR00C, COUSR01C, COUSR02C, COUSR03C | — |
| Transaction Posting | — | CBTRN01C, CBTRN02C |
| Interest Calculation | — | CBACT04C |
| Transaction Reporting | CORPT00C | CBTRN03C |
| Statement Generation | — | CBSTM03A, CBSTM03B |
| Data Migration | — | CBACT01C, CBACT02C, CBACT03C, CBCUS01C, CBTRN04C |
