# CardDemo Application - Business Rules

> **Analysis Date:** 2026-02-10
> **Duration:** ~30 minutes
> **Method:** Code-only analysis of COBOL programs, copybooks, and validation logic
> **Repository:** aws-mainframe-modernization-carddemo

---

## 1. Authentication Rules

### BR-AUTH-01: User ID Required for Sign-On

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A user must provide a User ID to sign in to the system. |
| **Condition** | User ID field is empty (SPACES or LOW-VALUES). |
| **Outcome** | Sign-on is rejected with message 'Please enter User ID ...'. |
| **Program** | `COSGN00C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 108-140) |
| **Code Reference** | Lines 118-122: `WHEN USERIDI OF COSGN0AI = SPACES OR LOW-VALUES` |

### BR-AUTH-02: Password Required for Sign-On

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A user must provide a password to sign in to the system. |
| **Condition** | Password field is empty (SPACES or LOW-VALUES). |
| **Outcome** | Sign-on is rejected with message 'Please enter Password ...'. |
| **Program** | `COSGN00C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 108-140) |
| **Code Reference** | Lines 123-127: `WHEN PASSWDI OF COSGN0AI = SPACES OR LOW-VALUES` |

### BR-AUTH-03: User Must Exist in Security File

| Attribute | Detail |
|-----------|--------|
| **Business Description** | The User ID provided at sign-on must exist in the user security database. |
| **Condition** | User ID lookup returns response code 13 (NOTFND). |
| **Outcome** | Sign-on is rejected with message 'User not found. Try again ...'. |
| **Program** | `COSGN00C.cbl` |
| **Logic Block** | `READ-USER-SEC-FILE` (lines 209-257) |
| **Code Reference** | Lines 247-251: `WHEN 13 ... MOVE 'User not found. Try again ...' TO WS-MESSAGE` |

### BR-AUTH-04: Password Must Match Stored Value

| Attribute | Detail |
|-----------|--------|
| **Business Description** | The password entered must exactly match the password stored in the user security record. |
| **Condition** | `SEC-USR-PWD` does not equal `WS-USER-PWD`. |
| **Outcome** | Sign-on is rejected with message 'Wrong Password. Try again ...'. |
| **Program** | `COSGN00C.cbl` |
| **Logic Block** | `READ-USER-SEC-FILE` (lines 209-257) |
| **Code Reference** | Lines 223, 241-245: `IF SEC-USR-PWD = WS-USER-PWD ... ELSE MOVE 'Wrong Password. Try again ...'` |

### BR-AUTH-05: Admin Users Route to Admin Menu

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Users with admin user type are directed to the administration menu upon successful sign-on. |
| **Condition** | `CDEMO-USRTYP-ADMIN` is true (SEC-USR-TYPE = 'A'). |
| **Outcome** | Control transfers to `COADM01C` (admin menu). |
| **Program** | `COSGN00C.cbl` |
| **Logic Block** | `READ-USER-SEC-FILE` (lines 209-257) |
| **Code Reference** | Lines 230-234: `IF CDEMO-USRTYP-ADMIN EXEC CICS XCTL PROGRAM ('COADM01C')` |

### BR-AUTH-06: Regular Users Route to Main Menu

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Users without admin privileges are directed to the regular user menu upon successful sign-on. |
| **Condition** | `CDEMO-USRTYP-ADMIN` is false. |
| **Outcome** | Control transfers to `COMEN01C` (regular menu). |
| **Program** | `COSGN00C.cbl` |
| **Logic Block** | `READ-USER-SEC-FILE` (lines 209-257) |
| **Code Reference** | Lines 235-239: `ELSE EXEC CICS XCTL PROGRAM ('COMEN01C')` |

---

## 2. Access Control Rules

### BR-ACC-01: Regular Users Cannot Access Admin-Only Options

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Menu options flagged as admin-only are blocked for regular (non-admin) users. |
| **Condition** | User type is 'U' (regular) AND menu option's user type flag is 'A' (admin). |
| **Outcome** | Access denied with message 'No access - Admin Only option...'. |
| **Program** | `COMEN01C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 115-191) |
| **Code Reference** | Lines 136-143: `IF CDEMO-USRTYP-USER AND CDEMO-MENU-OPT-USRTYPE(WS-OPTION) = 'A'` |

### BR-ACC-02: Menu Option Must Be Valid

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Users must enter a valid numeric option number from the displayed menu. |
| **Condition** | Option is not numeric, exceeds option count, or is zero. |
| **Outcome** | Rejected with message 'Please enter a valid option number...'. |
| **Program** | `COMEN01C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 115-191) |
| **Code Reference** | Lines 127-134: `IF WS-OPTION IS NOT NUMERIC OR WS-OPTION > CDEMO-MENU-OPT-COUNT OR WS-OPTION = ZEROS` |

---

## 3. User Management Rules

### BR-USR-01: All Fields Required for User Creation

| Attribute | Detail |
|-----------|--------|
| **Business Description** | When creating a new user, all profile fields must be provided: first name, last name, user ID, password, and user type. |
| **Condition** | Any of the five fields is empty (SPACES or LOW-VALUES). |
| **Outcome** | User creation is rejected with field-specific error message (e.g., 'First Name can NOT be empty...'). |
| **Program** | `COUSR01C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 115-146) |
| **Code Reference** | Lines 118-142: EVALUATE block checks each field sequentially |

### BR-USR-02: User ID Must Be Unique

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A new user cannot be created with a User ID that already exists in the system. |
| **Condition** | CICS WRITE returns `DFHRESP(DUPKEY)` or `DFHRESP(DUPREC)`. |
| **Outcome** | User creation is rejected with message 'User ID already exist...'. |
| **Program** | `COUSR01C.cbl` |
| **Logic Block** | `WRITE-USER-SEC-FILE` (called from PROCESS-ENTER-KEY) |
| **Code Reference** | Lines 260-264: `WHEN DFHRESP(DUPKEY) WHEN DFHRESP(DUPREC) ... MOVE 'User ID already exist...'` |

### BR-USR-03: User Update Requires At Least One Change

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A user record update is only processed if at least one field has been modified. Submitting an update with no changes is rejected. |
| **Condition** | All fields (first name, last name, password, user type) remain unchanged from the stored values. |
| **Outcome** | Update rejected with message 'Please modify to update ...'. |
| **Program** | `COUSR02C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 215-242) |
| **Code Reference** | Lines 236-241: `IF USR-MODIFIED-YES PERFORM UPDATE-USER-SEC-FILE ELSE MOVE 'Please modify to update ...'` |

### BR-USR-04: User Deletion Is Permanent

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Deleting a user permanently removes their record from the user security file. There is no soft-delete or recovery mechanism. |
| **Condition** | User deletion is confirmed and executed. |
| **Outcome** | Record removed from USRSEC file via `EXEC CICS DELETE`. |
| **Program** | `COUSR03C.cbl` |
| **Logic Block** | `DELETE-USER-SEC-FILE` (lines 305-322) |
| **Code Reference** | Lines 307-311: `EXEC CICS DELETE DATASET(WS-USRSEC-FILE)` |

---

## 4. Account Validation Rules

### BR-ACCT-01: Account ID Must Be Numeric and Non-Zero

| Attribute | Detail |
|-----------|--------|
| **Business Description** | When looking up an account, the account number must be a non-zero, 11-digit numeric value. |
| **Condition** | Account ID is not numeric OR equals zero. |
| **Outcome** | Rejected with message 'Account Filter must be a non-zero 11 digit number'. |
| **Program** | `COACTVWC.cbl` |
| **Logic Block** | `2210-EDIT-ACCOUNT` (lines 649-681) |
| **Code Reference** | Lines 666-676: `IF CC-ACCT-ID IS NOT NUMERIC OR CC-ACCT-ID EQUAL ZEROES` |

### BR-ACCT-02: Account Update Requires Field Change

| Attribute | Detail |
|-----------|--------|
| **Business Description** | An account/customer update is only saved if at least one field has been changed. All old vs new field values are compared. |
| **Condition** | All account fields (status, balances, limits, dates, group ID) AND all customer fields (name, address, phone, SSN, DOB, etc.) are identical between old and new values. |
| **Outcome** | No update is performed; `NO-CHANGES-DETECTED` flag is set. |
| **Program** | `COACTUPC.cbl` |
| **Logic Block** | `1205-COMPARE-OLD-NEW` (lines 1681-1775) |
| **Code Reference** | Lines 1684-1773: Exhaustive field-by-field comparison using UPPER-CASE and TRIM functions |

### BR-ACCT-03: FICO Score Must Be Between 300 and 850

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A customer's FICO credit score must fall within the valid range of 300 to 850. |
| **Condition** | FICO score value is outside the range 300-850. |
| **Outcome** | Input validation fails; `FICO-RANGE-IS-VALID` condition is false. |
| **Program** | `COACTUPC.cbl` |
| **Logic Block** | Data definition (line 848) used in edit routines |
| **Code Reference** | Lines 848-849: `88 FICO-RANGE-IS-VALID VALUES 300 THROUGH 850` |

### BR-ACCT-04: State and Zip Code Must Be Consistent

| Attribute | Detail |
|-----------|--------|
| **Business Description** | When both state code and zip code are provided and valid, they must be consistent with each other (cross-field validation). |
| **Condition** | Both `FLG-STATE-ISVALID` and `FLG-ZIPCODE-ISVALID` are true. |
| **Outcome** | Cross-field validation `1280-EDIT-US-STATE-ZIP-CD` is performed. |
| **Program** | `COACTUPC.cbl` |
| **Logic Block** | `1200-EDIT-MAP-INPUTS` (lines 1664-1669) |
| **Code Reference** | Lines 1665-1669: `IF FLG-STATE-ISVALID AND FLG-ZIPCODE-ISVALID PERFORM 1280-EDIT-US-STATE-ZIP-CD` |

### BR-ACCT-05: Phone Numbers Must Follow US Format

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Customer phone numbers must conform to the US phone number format (area code + exchange + number). |
| **Condition** | Phone number does not pass US phone format validation. |
| **Outcome** | Input validation fails. |
| **Program** | `COACTUPC.cbl` |
| **Logic Block** | `1200-EDIT-MAP-INPUTS` (lines 1632-1646) |
| **Code Reference** | Lines 1632-1646: `PERFORM 1260-EDIT-US-PHONE-NUM` for both Phone Number 1 and Phone Number 2 |

---

## 5. Credit Card Validation Rules

### BR-CARD-01: Card Expiry Month Must Be Valid

| Attribute | Detail |
|-----------|--------|
| **Business Description** | When updating a credit card, the expiration month must be a valid month value. |
| **Condition** | Expiry month is not within valid range. |
| **Outcome** | Update rejected; `FLG-CARDEXPMON-NOT-OK` flag set, message 'Card expiry month not valid'. |
| **Program** | `COCRDUPC.cbl` |
| **Logic Block** | `1250-EDIT-EXPIRY-MON` (lines 900-911) |
| **Code Reference** | Lines 900-907: Validates month value; sets `FLG-CARDEXPMON-NOT-OK` and `CARD-EXPIRY-MONTH-NOT-VALID` on failure |

### BR-CARD-02: Card Expiry Year Must Be Valid

| Attribute | Detail |
|-----------|--------|
| **Business Description** | When updating a credit card, the expiration year must be a valid numeric year. |
| **Condition** | Expiry year is empty, zero, or not a `VALID-YEAR`. |
| **Outcome** | Update rejected; `FLG-CARDEXPYEAR-NOT-OK` flag set. |
| **Program** | `COCRDUPC.cbl` |
| **Logic Block** | `1260-EDIT-EXPIRY-YEAR` (lines 913-946) |
| **Code Reference** | Lines 916-943: Checks for empty/zero, then validates against `VALID-YEAR` condition |

### BR-CARD-03: Card Update Uses Optimistic Locking

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Card updates detect concurrent modifications. If another user changed the record between read and write, the update is cancelled and fresh data is shown. |
| **Condition** | Data changed between read and write attempt. |
| **Outcome** | Three possible outcomes: lock error (`COULD-NOT-LOCK-FOR-UPDATE`), update failed (`LOCKED-BUT-UPDATE-FAILED`), or data changed (`DATA-WAS-CHANGED-BEFORE-UPDATE`). Screen refreshes with current data. |
| **Program** | `COCRDUPC.cbl` |
| **Logic Block** | `2000-DECIDE-ACTION` (lines 948-1027) |
| **Code Reference** | Lines 988-1001: EVALUATE handles `9200-WRITE-PROCESSING` outcomes |

---

## 6. Transaction Validation Rules (Online)

### BR-TRN-01: Transaction ID Required for View

| Attribute | Detail |
|-----------|--------|
| **Business Description** | To view a transaction, a Transaction ID must be provided. |
| **Condition** | Transaction ID field is empty (SPACES or LOW-VALUES). |
| **Outcome** | Rejected with message 'Tran ID can NOT be empty...'. |
| **Program** | `COTRN01C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 144-174) |
| **Code Reference** | Lines 147-152: `WHEN TRNIDINI OF COTRN1AI = SPACES OR LOW-VALUES` |

### BR-TRN-02: Account or Card Number Required for Transaction Add

| Attribute | Detail |
|-----------|--------|
| **Business Description** | When adding a new transaction, either an Account ID or a Card Number must be provided. The system resolves the other via cross-reference. |
| **Condition** | Both Account ID and Card Number are empty. |
| **Outcome** | Rejected with message 'Account or Card Number must be entered...'. |
| **Program** | `COTRN02C.cbl` |
| **Logic Block** | `VALIDATE-INPUT-KEY-FIELDS` (lines 193-230) |
| **Code Reference** | Lines 224-229: `WHEN OTHER ... MOVE 'Account or Card Number must be entered...'` |

### BR-TRN-03: Account ID Must Be Numeric

| Attribute | Detail |
|-----------|--------|
| **Business Description** | If an Account ID is provided for a new transaction, it must be a numeric value. |
| **Condition** | Account ID is not numeric. |
| **Outcome** | Rejected with message 'Account ID must be Numeric...'. |
| **Program** | `COTRN02C.cbl` |
| **Logic Block** | `VALIDATE-INPUT-KEY-FIELDS` (lines 193-230) |
| **Code Reference** | Lines 197-203: `IF ACTIDINI OF COTRN2AI IS NOT NUMERIC` |

### BR-TRN-04: All Transaction Data Fields Required

| Attribute | Detail |
|-----------|--------|
| **Business Description** | When adding a new transaction, all data fields must be provided: Type Code, Category Code, Source, Description, Amount, Origination Date, Processing Date, and Merchant ID. |
| **Condition** | Any of the required fields is empty (SPACES or LOW-VALUES). |
| **Outcome** | Rejected with field-specific error message (e.g., 'Type CD can NOT be empty...'). |
| **Program** | `COTRN02C.cbl` |
| **Logic Block** | `VALIDATE-INPUT-DATA-FIELDS` (lines 235-299) |
| **Code Reference** | Lines 251-299: EVALUATE block checks each field sequentially |

### BR-TRN-05: Transaction Add Requires Explicit Confirmation

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A new transaction is only written after the user explicitly confirms by entering 'Y'. |
| **Condition** | Confirm field is not 'Y' or 'y'. |
| **Outcome** | If 'N'/'n'/empty: message 'Confirm to add this transaction...'. If other: message 'Invalid value. Valid values are (Y/N)...'. |
| **Program** | `COTRN02C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 164-188) |
| **Code Reference** | Lines 169-188: EVALUATE CONFIRMI checks for Y/y, N/n, spaces, and other values |

---

## 7. Bill Payment Rules

### BR-BILL-01: Account Must Have Positive Balance for Payment

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A bill payment can only be processed if the account has an outstanding balance greater than zero. |
| **Condition** | `ACCT-CURR-BAL <= ZEROS`. |
| **Outcome** | Payment rejected with message 'You have nothing to pay...'. |
| **Program** | `COBIL00C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 154-244) |
| **Code Reference** | Lines 198-205: `IF ACCT-CURR-BAL <= ZEROS ... MOVE 'You have nothing to pay...'` |

### BR-BILL-02: Bill Payment Requires Explicit Confirmation

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A bill payment is only processed after the user explicitly confirms by entering 'Y'. Only Y/N values are accepted. |
| **Condition** | Confirm field is not 'Y' or 'y'. |
| **Outcome** | If 'N'/'n': screen clears. If empty: prompts 'Confirm to make a bill payment...'. If other: message 'Invalid value. Valid values are (Y/N)...'. |
| **Program** | `COBIL00C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 154-244) |
| **Code Reference** | Lines 173-191: EVALUATE CONFIRMI for Y/y, N/n, spaces, and other values |

### BR-BILL-03: Bill Payment Amount Equals Full Current Balance

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Bill payments always pay the full current account balance. Partial payments are not supported. |
| **Condition** | Payment is confirmed. |
| **Outcome** | Transaction amount set to `ACCT-CURR-BAL`; new balance computed as `ACCT-CURR-BAL - TRAN-AMT` (resulting in zero). |
| **Program** | `COBIL00C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 154-244) |
| **Code Reference** | Line 224: `MOVE ACCT-CURR-BAL TO TRAN-AMT`; Line 234: `COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT` |

### BR-BILL-04: Bill Payment Creates System Transaction

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Each bill payment generates a transaction record with fixed system values: type '02', category 2, source 'POS TERM', description 'BILL PAYMENT - ONLINE', merchant ID 999999999. |
| **Condition** | Payment confirmed and processed. |
| **Outcome** | New TRANSACT record created with system-assigned values. |
| **Program** | `COBIL00C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 154-244) |
| **Code Reference** | Lines 220-229: Fixed value assignments for type, category, source, description, merchant |

### BR-BILL-05: Transaction ID Auto-Increments

| Attribute | Detail |
|-----------|--------|
| **Business Description** | New bill payment transaction IDs are generated by reading the last transaction in the file and incrementing by 1. |
| **Condition** | New transaction is being created. |
| **Outcome** | `TRAN-ID = last TRAN-ID + 1`. |
| **Program** | `COBIL00C.cbl` |
| **Logic Block** | `PROCESS-ENTER-KEY` (lines 154-244) |
| **Code Reference** | Lines 212-218: `MOVE HIGH-VALUES TO TRAN-ID`, STARTBR, READPREV, ENDBR, `ADD 1 TO WS-TRAN-ID-NUM` |

---

## 8. Batch Transaction Posting Rules

### BR-POST-01: Card Must Exist in Cross-Reference

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A daily transaction can only be posted if the card number exists in the card cross-reference file. |
| **Condition** | Card number lookup in XREF file returns INVALID KEY. |
| **Outcome** | Transaction rejected with validation code 100 and reason 'INVALID CARD NUMBER FOUND'. |
| **Program** | `CBTRN02C.cbl` |
| **Logic Block** | `1500-A-LOOKUP-XREF` (lines 380-391) |
| **Code Reference** | Lines 384-387: `INVALID KEY MOVE 100 TO WS-VALIDATION-FAIL-REASON MOVE 'INVALID CARD NUMBER FOUND'` |

### BR-POST-02: Account Must Exist

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A daily transaction can only be posted if the account referenced by the card cross-reference exists in the account master file. |
| **Condition** | Account lookup in ACCOUNT-FILE returns INVALID KEY. |
| **Outcome** | Transaction rejected with validation code 101 and reason 'ACCOUNT RECORD NOT FOUND'. |
| **Program** | `CBTRN02C.cbl` |
| **Logic Block** | `1500-B-LOOKUP-ACCT` (lines 393-421) |
| **Code Reference** | Lines 396-399: `INVALID KEY MOVE 101 TO WS-VALIDATION-FAIL-REASON MOVE 'ACCOUNT RECORD NOT FOUND'` |

### BR-POST-03: Transaction Must Not Exceed Credit Limit

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A transaction is rejected if it would cause the account balance to exceed the credit limit. The projected balance is calculated as current cycle credits minus current cycle debits plus the transaction amount. |
| **Condition** | `ACCT-CREDIT-LIMIT < (ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT)`. |
| **Outcome** | Transaction rejected with validation code 102 and reason 'OVERLIMIT TRANSACTION'. |
| **Program** | `CBTRN02C.cbl` |
| **Logic Block** | `1500-B-LOOKUP-ACCT` (lines 393-421) |
| **Code Reference** | Lines 403-413: `COMPUTE WS-TEMP-BAL = ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT` then `IF ACCT-CREDIT-LIMIT >= WS-TEMP-BAL` |

### BR-POST-04: Account Must Not Be Expired

| Attribute | Detail |
|-----------|--------|
| **Business Description** | A transaction is rejected if it was originated after the account's expiration date. |
| **Condition** | `ACCT-EXPIRAION-DATE < DALYTRAN-ORIG-TS(1:10)` (first 10 characters = date portion). |
| **Outcome** | Transaction rejected with validation code 103 and reason 'TRANSACTION RECEIVED AFTER ACCT EXPIRATION'. |
| **Program** | `CBTRN02C.cbl` |
| **Logic Block** | `1500-B-LOOKUP-ACCT` (lines 393-421) |
| **Code Reference** | Lines 414-420: `IF ACCT-EXPIRAION-DATE >= DALYTRAN-ORIG-TS(1:10) ... ELSE MOVE 103 TO WS-VALIDATION-FAIL-REASON` |

### BR-POST-05: Posted Transactions Update Account Balance

| Attribute | Detail |
|-----------|--------|
| **Business Description** | When a transaction is posted, the account current balance is updated. Positive amounts are added to cycle credits; negative amounts are added to cycle debits. |
| **Condition** | Transaction passes all validation and is posted. |
| **Outcome** | `ACCT-CURR-BAL += DALYTRAN-AMT`. If amount >= 0: `ACCT-CURR-CYC-CREDIT += DALYTRAN-AMT`. If amount < 0: `ACCT-CURR-CYC-DEBIT += DALYTRAN-AMT`. |
| **Program** | `CBTRN02C.cbl` |
| **Logic Block** | `2800-UPDATE-ACCOUNT-REC` (lines 545-559) |
| **Code Reference** | Lines 547-552: `ADD DALYTRAN-AMT TO ACCT-CURR-BAL` then `IF DALYTRAN-AMT >= 0 ADD DALYTRAN-AMT TO ACCT-CURR-CYC-CREDIT ELSE ADD DALYTRAN-AMT TO ACCT-CURR-CYC-DEBIT` |

### BR-POST-06: Posted Transactions Update Category Balances

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Each posted transaction updates (or creates) a transaction category balance record keyed by account ID, transaction type, and category code. |
| **Condition** | Transaction is posted. TCATBAL record may or may not exist. |
| **Outcome** | If TCATBAL record exists: `TRAN-CAT-BAL += DALYTRAN-AMT` (REWRITE). If not: new record created with initial balance = transaction amount (WRITE). |
| **Program** | `CBTRN02C.cbl` |
| **Logic Block** | `2700-UPDATE-TCATBAL` (lines 467-501), `2700-A-CREATE-TCATBAL-REC` (lines 503-524), `2700-B-UPDATE-TCATBAL-REC` (lines 526-542) |
| **Code Reference** | Lines 473-499: READ with INVALID KEY creates new record; otherwise updates existing |

### BR-POST-07: Rejected Transactions Written to Reject File

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Transactions that fail validation are written to a reject file with the original transaction data and a validation trailer containing the failure reason. |
| **Condition** | Any validation (card lookup, account lookup, credit limit, expiration) fails. |
| **Outcome** | Original transaction + validation trailer written to DALYREJS file. |
| **Program** | `CBTRN02C.cbl` |
| **Logic Block** | `2500-WRITE-REJECT-REC` (lines 446-465) |
| **Code Reference** | Lines 447-451: `MOVE DALYTRAN-RECORD TO REJECT-TRAN-DATA` then `WRITE FD-REJS-RECORD FROM REJECT-RECORD` |

---

## 9. Interest Calculation Rules

### BR-INT-01: Monthly Interest Formula

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Monthly interest is calculated as the transaction category balance multiplied by the annual interest rate, divided by 1200 (to convert annual rate to monthly). |
| **Condition** | Interest calculation is triggered for each account's transaction category balance. |
| **Outcome** | `Monthly Interest = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200`. |
| **Program** | `CBACT04C.cbl` |
| **Logic Block** | `1300-COMPUTE-INTEREST` (lines 462-470) |
| **Code Reference** | Lines 464-465: `COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200` |

### BR-INT-02: Default Interest Rate Used When Group Not Found

| Attribute | Detail |
|-----------|--------|
| **Business Description** | If an account's discount group is not found in the disclosure group file, the system falls back to a 'DEFAULT' group for interest rate lookup. |
| **Condition** | Disclosure group file read returns status '23' (record not found). |
| **Outcome** | Account group ID is set to 'DEFAULT' and a second lookup is performed. |
| **Program** | `CBACT04C.cbl` |
| **Logic Block** | Interest rate lookup (lines 436-439) |
| **Code Reference** | Lines 436-438: `IF DISCGRP-STATUS = '23' MOVE 'DEFAULT' TO FD-DIS-ACCT-GROUP-ID PERFORM 1200-A-GET-DEFAULT-INT-RATE` |

### BR-INT-03: Interest Generates System Transaction

| Attribute | Detail |
|-----------|--------|
| **Business Description** | Each interest charge creates a system-generated transaction record with fixed identifiers: type code '01', category code '05', source 'System'. |
| **Condition** | Interest has been computed for a transaction category. |
| **Outcome** | New TRANSACT record written with TRAN-TYPE-CD='01', TRAN-CAT-CD='05', TRAN-SOURCE='System', TRAN-DESC='Int. for a/c {ACCT-ID}', TRAN-AMT=calculated interest. |
| **Program** | `CBACT04C.cbl` |
| **Logic Block** | `1300-B-WRITE-TX` (lines 473-498) |
| **Code Reference** | Lines 482-490: Fixed value assignments for system transaction fields |

### BR-INT-04: Fee Calculation Not Yet Implemented

| Attribute | Detail |
|-----------|--------|
| **Business Description** | The system has a placeholder for fee calculation, but it is not yet functional. |
| **Condition** | N/A |
| **Outcome** | No fees are calculated (stub procedure). |
| **Program** | `CBACT04C.cbl` |
| **Logic Block** | `1400-COMPUTE-FEES` (lines 518-520) |
| **Code Reference** | Lines 518-520: Contains only comment 'To be implemented' and EXIT |

---

## Summary Table

| Rule ID | Category | Business Rule | Program |
|---------|----------|---------------|---------|
| BR-AUTH-01 | Authentication | User ID required | COSGN00C |
| BR-AUTH-02 | Authentication | Password required | COSGN00C |
| BR-AUTH-03 | Authentication | User must exist | COSGN00C |
| BR-AUTH-04 | Authentication | Password must match | COSGN00C |
| BR-AUTH-05 | Authentication | Admin routes to admin menu | COSGN00C |
| BR-AUTH-06 | Authentication | Regular user routes to main menu | COSGN00C |
| BR-ACC-01 | Access Control | Admin-only option restriction | COMEN01C |
| BR-ACC-02 | Access Control | Valid menu option required | COMEN01C |
| BR-USR-01 | User Management | All fields required for creation | COUSR01C |
| BR-USR-02 | User Management | User ID must be unique | COUSR01C |
| BR-USR-03 | User Management | Update requires change | COUSR02C |
| BR-USR-04 | User Management | Deletion is permanent | COUSR03C |
| BR-ACCT-01 | Account Validation | Account ID must be numeric/non-zero | COACTVWC |
| BR-ACCT-02 | Account Validation | Update requires field change | COACTUPC |
| BR-ACCT-03 | Account Validation | FICO score range 300-850 | COACTUPC |
| BR-ACCT-04 | Account Validation | State-zip consistency check | COACTUPC |
| BR-ACCT-05 | Account Validation | US phone format required | COACTUPC |
| BR-CARD-01 | Credit Card | Expiry month must be valid | COCRDUPC |
| BR-CARD-02 | Credit Card | Expiry year must be valid | COCRDUPC |
| BR-CARD-03 | Credit Card | Optimistic locking on update | COCRDUPC |
| BR-TRN-01 | Transaction (Online) | Transaction ID required for view | COTRN01C |
| BR-TRN-02 | Transaction (Online) | Account or card number required | COTRN02C |
| BR-TRN-03 | Transaction (Online) | Account ID must be numeric | COTRN02C |
| BR-TRN-04 | Transaction (Online) | All data fields required | COTRN02C |
| BR-TRN-05 | Transaction (Online) | Explicit confirmation required | COTRN02C |
| BR-BILL-01 | Bill Payment | Positive balance required | COBIL00C |
| BR-BILL-02 | Bill Payment | Explicit confirmation required | COBIL00C |
| BR-BILL-03 | Bill Payment | Full balance payment only | COBIL00C |
| BR-BILL-04 | Bill Payment | Creates system transaction | COBIL00C |
| BR-BILL-05 | Bill Payment | Auto-increment transaction ID | COBIL00C |
| BR-POST-01 | Batch Posting | Card must exist in XREF | CBTRN02C |
| BR-POST-02 | Batch Posting | Account must exist | CBTRN02C |
| BR-POST-03 | Batch Posting | Credit limit check | CBTRN02C |
| BR-POST-04 | Batch Posting | Account expiration check | CBTRN02C |
| BR-POST-05 | Batch Posting | Balance update on posting | CBTRN02C |
| BR-POST-06 | Batch Posting | Category balance update | CBTRN02C |
| BR-POST-07 | Batch Posting | Rejected to reject file | CBTRN02C |
| BR-INT-01 | Interest Calc | Monthly interest formula | CBACT04C |
| BR-INT-02 | Interest Calc | Default rate fallback | CBACT04C |
| BR-INT-03 | Interest Calc | System transaction generation | CBACT04C |
| BR-INT-04 | Interest Calc | Fee calc not implemented | CBACT04C |
