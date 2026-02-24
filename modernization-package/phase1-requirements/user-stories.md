# User Stories: Transaction Processing Module Modernization

> **Source Module:** Transaction Processing (COTRN00C, COTRN01C, COTRN02C)
> **BRE Reference:** `transactions-processing-module-doc-devin.md`
> **Target Stack:** Java 21, Spring Boot 3, Spring Data JPA, React, PostgreSQL
> **Total Stories:** 19 (6 List + 5 View + 8 Add)
> **Total Business Rules:** 30 (3 Cross-Functional + 8 List + 5 View + 14 Add)

---

## Business Rule Traceability Summary

The table below proves that all 30 Business Rules are covered by at least one User Story.

| Business Rule | User Story(ies) | Description |
|--------------|----------------|-------------|
| BR-CF-01 | US-LT-01, US-LT-06, US-VT-05, US-AT-08 | Session Required — unauthenticated users redirected to login |
| BR-CF-02 | US-LT-01, US-VT-01, US-AT-04 | Pseudo-Conversational Operation — stateless REST |
| BR-CF-03 | US-LT-01, US-VT-02, US-AT-03 | Invalid Key Handling — unrecognized actions return error |
| BR-LT-01 | US-LT-01 | Page Size Fixed at 10 |
| BR-LT-02 | US-LT-04 | Numeric Filter Validation |
| BR-LT-03 | US-LT-05 | Valid Selection Value ('S' only) |
| BR-LT-04 | US-LT-01, US-LT-04 | Empty Filter Browses from Start |
| BR-LT-05 | US-LT-02 | Forward Pagination Boundary |
| BR-LT-06 | US-LT-03 | Backward Pagination Boundary |
| BR-LT-07 | US-LT-01, US-LT-02, US-LT-03 | Page State Preservation |
| BR-LT-08 | US-LT-05 | Selection Triggers Detail View |
| BR-VT-01 | US-VT-02 | Transaction ID Required |
| BR-VT-02 | US-VT-02 | Transaction Must Exist |
| BR-VT-03 | US-VT-01 | Pre-Selected Auto-Load |
| BR-VT-04 | US-VT-01, US-VT-02, US-VT-03 | Read-Only Display |
| BR-VT-05 | US-VT-04 | PF5 Returns to List |
| BR-AT-01 | US-AT-01, US-AT-02, US-AT-03 | Account or Card Required |
| BR-AT-02 | US-AT-01, US-AT-03 | Account ID Numeric |
| BR-AT-03 | US-AT-02, US-AT-03 | Card Number Numeric |
| BR-AT-04 | US-AT-01, US-AT-02, US-AT-03 | Account/Card Must Exist |
| BR-AT-05 | US-AT-01, US-AT-02 | Cross-Reference Resolution |
| BR-AT-06 | US-AT-03 | All 11 Data Fields Mandatory |
| BR-AT-07 | US-AT-03 | Type/Category Must Be Numeric |
| BR-AT-08 | US-AT-03 | Amount Format Required |
| BR-AT-09 | US-AT-03 | Date Format Required |
| BR-AT-10 | US-AT-03 | Date Validity Required |
| BR-AT-11 | US-AT-03 | Merchant ID Numeric |
| BR-AT-12 | US-AT-04 | Explicit Confirmation |
| BR-AT-13 | US-AT-05 | Auto-Increment Transaction ID |
| BR-AT-14 | US-AT-05 | Duplicate ID Rejection |

**Quality Gate: PASSED — All 30/30 Business Rules are traceable to at least one User Story.**

---

## 1. Transaction Listing Stories (US-LT-01 through US-LT-06)

### US-LT-01: View Paginated Transaction List

**Epic:** EPIC-02 (Transaction List — CT00)

**Title:** As a user, I want to view a paginated list of all transactions so that I can review transaction activity.

**Description:**
When a user navigates to the Transaction List screen (replacing legacy COTRN00C / CT00), the system displays the first page of transactions from the database, sorted by Transaction ID ascending. Each page shows exactly 10 rows. Each row displays: Transaction ID, Date (derived from origination timestamp), Description, and Amount. The current page number is displayed on the screen.

This is the primary entry point for transaction browsing and serves as the hub for navigating to transaction details.

**Acceptance Criteria:**
1. On initial load, the first 10 transactions are displayed starting from the lowest Transaction ID.
2. Each row shows: Transaction ID, Date (MM/DD/YY format), Description, and Amount (formatted as +99999999.99).
3. The current page number is displayed on the screen (starting at page 1).
4. Page state (first ID, last ID, page number, has-next flag) is maintained across interactions.
5. If no transactions exist, an empty list is shown (no error).
6. Unauthenticated users are redirected to the login screen (BR-CF-01).
7. Unrecognized actions (invalid keys) display "Invalid Key Pressed" without altering data (BR-CF-03).

**Business Rules:** BR-CF-01, BR-CF-02, BR-CF-03, BR-LT-01, BR-LT-04, BR-LT-07

---

### US-LT-02: Page Forward Through Transactions

**Epic:** EPIC-02 (Transaction List — CT00)

**Title:** As a user, I want to page forward (PF8) through transactions so that I can view additional records.

**Description:**
When viewing the transaction list, the user can advance to the next page of 10 transactions by clicking the "Next Page" button (replacing PF8). The page number increments by 1 and the next 10 transactions are loaded. If there are no more records beyond the current page, the system displays the boundary message "You are already at the bottom of the page..." and the list remains unchanged.

**Acceptance Criteria:**
1. Clicking "Next Page" loads the next 10 transactions after the last record on the current page.
2. The page number increments by 1.
3. Page state (first/last Transaction IDs) is updated to reflect the new page.
4. If the current page is the last page (no more records after it), clicking "Next Page" displays the message: **"You are already at the bottom of the page..."**
5. The list data remains unchanged when the boundary message is shown (no blank page).
6. The has-next flag correctly reflects whether more records exist.

**Business Rules:** BR-LT-05, BR-LT-07

---

### US-LT-03: Page Backward Through Transactions

**Epic:** EPIC-02 (Transaction List — CT00)

**Title:** As a user, I want to page backward (PF7) through transactions so that I can return to earlier records.

**Description:**
When viewing the transaction list, the user can go back to the previous page of 10 transactions by clicking the "Previous Page" button (replacing PF7). The page number decrements by 1 and the previous 10 transactions are loaded. If the user is already on page 1, the system displays the boundary message "You are already at the top of the page..." and the list remains unchanged.

**Acceptance Criteria:**
1. Clicking "Previous Page" loads the 10 transactions before the first record on the current page.
2. The page number decrements by 1.
3. Page state (first/last Transaction IDs) is updated to reflect the new page.
4. If the user is on page 1, clicking "Previous Page" displays the message: **"You are already at the top of the page..."**
5. The list data remains unchanged when the boundary message is shown.
6. Backward navigation from page 3 → page 2 → page 1 works correctly.

**Business Rules:** BR-LT-06, BR-LT-07

---

### US-LT-04: Filter by Starting Transaction ID

**Epic:** EPIC-02 (Transaction List — CT00)

**Title:** As a user, I want to filter by a starting Transaction ID so that I can jump to a specific range.

**Description:**
The transaction list provides a filter input field where the user can enter a Transaction ID. When submitted, the list restarts from that Transaction ID (or the nearest record at or after that ID). The filter value must be numeric; non-numeric values are rejected with an error message.

**Acceptance Criteria:**
1. Entering a valid numeric Transaction ID and submitting restarts the list from that ID (or the first record >= that ID).
2. The page number resets to 1 when a filter is applied.
3. Entering a non-numeric value displays the error: **"Tran ID must be Numeric ..."**
4. Leaving the filter empty and submitting loads from the first record in the file (BR-LT-04).
5. Filtering to an ID beyond all existing records shows an empty page or appropriate message.

**Business Rules:** BR-LT-02, BR-LT-04

---

### US-LT-05: Select Transaction for Detailed View

**Epic:** EPIC-02 (Transaction List — CT00)

**Title:** As a user, I want to select a transaction with 'S' so that I can view its full details.

**Description:**
Each row in the transaction list has a selection field. The user can enter 'S' (or 's') next to a transaction row and submit. The system navigates to the Transaction View screen (replacing COTRN01C) with the selected Transaction ID, where all 13 detail fields are displayed. Any selection value other than 'S'/'s' is rejected with an error.

**Acceptance Criteria:**
1. Entering 'S' or 's' next to a transaction row and submitting navigates to the Transaction View screen with that transaction's data pre-loaded.
2. The selected Transaction ID is passed to the view screen (via URL parameter or state).
3. Entering any value other than 'S'/'s' in the selection field displays the error: **"Invalid selection. Valid value is S"**
4. Leaving the selection field empty and submitting does not navigate away (list refreshes normally).
5. Only one transaction can be selected at a time (if multiple 'S' entries exist, the first one is processed).

**Business Rules:** BR-LT-03, BR-LT-08

---

### US-LT-06: Return to Main Menu from List

**Epic:** EPIC-02 (Transaction List — CT00)

**Title:** As a user, I want to press PF3 to return to the main menu so that I can access other functions.

**Description:**
From the transaction list screen, the user can click the "Back to Menu" button (replacing PF3) to return to the main application menu. This exits the transaction list and navigates to the main menu screen (replacing COMEN01C).

**Acceptance Criteria:**
1. Clicking "Back to Menu" navigates to the main menu screen.
2. No data is lost or modified during navigation.
3. Session remains active after returning to menu (BR-CF-01).

**Business Rules:** BR-CF-01

---

## 2. Transaction Viewing Stories (US-VT-01 through US-VT-05)

### US-VT-01: Auto-Load Transaction from List Selection

**Epic:** EPIC-03 (Transaction View — CT01)

**Title:** As a user, I want to select a transaction from the list and automatically see its details so that I don't have to re-enter the ID.

**Description:**
When navigating from the Transaction List screen (via 'S' selection), the Transaction View screen automatically loads and displays all 13 detail fields for the selected transaction. The user does not need to enter the Transaction ID manually — it is passed from the list screen and the transaction is fetched automatically on page load.

**Acceptance Criteria:**
1. Arriving from the List screen with a pre-selected Transaction ID automatically fetches and displays the transaction.
2. All 13 fields are displayed: Transaction ID, Card Number, Type Code, Category Code, Source, Amount (+99999999.99 format), Description, Origination Timestamp, Processing Timestamp, Merchant ID, Merchant Name, Merchant City, Merchant Zip.
3. No user action (ENTER press) is required to load the transaction.
4. All displayed fields are read-only (no editing possible) (BR-VT-04).
5. The Transaction ID used for auto-load is visible in the Transaction ID field.

**Business Rules:** BR-CF-02, BR-VT-03, BR-VT-04

---

### US-VT-02: Manual Transaction ID Lookup

**Epic:** EPIC-03 (Transaction View — CT01)

**Title:** As a user, I want to enter a Transaction ID manually and view its details so that I can look up specific transactions.

**Description:**
On the Transaction View screen, the user can type a Transaction ID into the input field and submit to look up and display that transaction's details. If the ID is empty, an error is shown. If the ID does not exist in the database, a not-found error is shown.

**Acceptance Criteria:**
1. Entering a valid Transaction ID and submitting displays all 13 fields for that transaction.
2. Submitting with an empty Transaction ID field displays the error: **"Tran ID can NOT be empty..."**
3. Entering a Transaction ID that does not exist displays the error: **"Transaction ID NOT found..."**
4. After a successful lookup, all fields are read-only (BR-VT-04).
5. Unrecognized actions display "Invalid Key Pressed" (BR-CF-03).
6. If an unexpected database error occurs, display: **"Unable to lookup Transaction..."**

**Business Rules:** BR-CF-03, BR-VT-01, BR-VT-02, BR-VT-04

---

### US-VT-03: Clear Screen for New Lookup

**Epic:** EPIC-03 (Transaction View — CT01)

**Title:** As a user, I want to press PF4 to clear the screen so that I can look up a different transaction.

**Description:**
Clicking the "Clear" button (replacing PF4) on the Transaction View screen resets all 13 display fields to blank and positions the cursor on the Transaction ID input field, allowing the user to enter a new Transaction ID for lookup.

**Acceptance Criteria:**
1. Clicking "Clear" resets all 13 detail fields to blank/empty.
2. The cursor (focus) is positioned on the Transaction ID input field.
3. No error message is displayed after clearing.
4. The screen remains in read-only mode for display fields (BR-VT-04).
5. The user can immediately type a new Transaction ID after clearing.

**Business Rules:** BR-VT-04

---

### US-VT-04: Return to Transaction List

**Epic:** EPIC-03 (Transaction View — CT01)

**Title:** As a user, I want to press PF5 to return to the transaction list so that I can continue browsing.

**Description:**
Clicking the "Back to List" button (replacing PF5) on the Transaction View screen navigates back to the Transaction List screen. The pagination state (page number, position) from the list is preserved, so the user returns to the same page they were on before selecting a transaction.

**Acceptance Criteria:**
1. Clicking "Back to List" navigates to the Transaction List screen.
2. The pagination state from the prior list session is preserved (same page, same position).
3. The user sees the same page of transactions they were viewing before selecting one.
4. No data is modified during this navigation.

**Business Rules:** BR-VT-05

---

### US-VT-05: Return to Main Menu from View

**Epic:** EPIC-03 (Transaction View — CT01)

**Title:** As a user, I want to press PF3 to return to the calling screen or main menu so that I can navigate elsewhere.

**Description:**
Clicking the "Back to Menu" button (replacing PF3) on the Transaction View screen returns the user to the calling program or main menu. If the view was accessed from the main menu directly, it returns to the main menu. If accessed from another program, it returns to that program.

**Acceptance Criteria:**
1. Clicking "Back to Menu" navigates to the main menu (or the calling program if applicable).
2. Session remains active (BR-CF-01).
3. No data is modified during navigation.

**Business Rules:** BR-CF-01

---

## 3. Transaction Addition Stories (US-AT-01 through US-AT-08)

### US-AT-01: Add Transaction by Account ID

**Epic:** EPIC-04 (Transaction Add — CT02)

**Title:** As a user, I want to add a transaction by entering an Account ID so that the system resolves the card number automatically.

**Description:**
On the Add Transaction screen (replacing COTRN02C / CT02), the user enters an Account ID in the Account ID field. The system validates that the Account ID is numeric, looks it up in the cross-reference data (replacing CXACAIX alternate index), resolves the associated Card Number, and auto-populates it on screen. This establishes the account-to-card relationship for the new transaction.

**Acceptance Criteria:**
1. Entering a valid numeric Account ID triggers a cross-reference lookup.
2. If the Account ID exists in the cross-reference table, the associated Card Number is resolved and auto-populated in the Card Number field.
3. Both Account ID and Card Number fields show their respective values after resolution.
4. If the Account ID is not numeric, the error is displayed: **"Account ID must be Numeric..."**
5. If the Account ID does not exist in the cross-reference, the error is displayed: **"Account ID NOT found..."**
6. If the cross-reference lookup encounters an unexpected error, display: **"Unable to lookup Acct in XREF AIX file..."**
7. The Card Number field is not required when Account ID is provided (BR-AT-01).

**Business Rules:** BR-AT-01, BR-AT-02, BR-AT-04, BR-AT-05

---

### US-AT-02: Add Transaction by Card Number

**Epic:** EPIC-04 (Transaction Add — CT02)

**Title:** As a user, I want to add a transaction by entering a Card Number so that the system resolves the account automatically.

**Description:**
On the Add Transaction screen, the user enters a Card Number in the Card Number field. The system validates that the Card Number is numeric, looks it up in the cross-reference data (replacing CCXREF KSDS), resolves the associated Account ID, and auto-populates it on screen. This is the alternate path for establishing the account-to-card relationship.

**Acceptance Criteria:**
1. Entering a valid numeric Card Number triggers a cross-reference lookup.
2. If the Card Number exists in the cross-reference table, the associated Account ID is resolved and auto-populated in the Account ID field.
3. Both Card Number and Account ID fields show their respective values after resolution.
4. If the Card Number is not numeric, the error is displayed: **"Card Number must be Numeric..."**
5. If the Card Number does not exist in the cross-reference, the error is displayed: **"Card Number NOT found..."**
6. If the cross-reference lookup encounters an unexpected error, display: **"Unable to lookup Card # in XREF file..."**
7. The Account ID field is not required when Card Number is provided (BR-AT-01).

**Business Rules:** BR-AT-01, BR-AT-03, BR-AT-04, BR-AT-05

---

### US-AT-03: Validate Input with Specific Error Messages

**Epic:** EPIC-04 (Transaction Add — CT02)

**Title:** As a user, I want all my input validated with specific error messages so that I know exactly what to fix.

**Description:**
When the user submits the Add Transaction form, the system executes a 6-phase sequential validation chain. Each phase checks specific fields, and the first validation failure halts the chain and returns a field-specific error message. The cursor (focus) is positioned on the offending field so the user knows exactly what to fix. This is the most complex validation logic in the module and must achieve exact message parity with the legacy system.

**Acceptance Criteria:**

**Phase 1 — Key Field Validation:**
1. If both Account ID and Card Number are empty, display: **"Account or Card Number must be entered..."** (BR-AT-01)
2. If Account ID is provided but not numeric, display: **"Account ID must be Numeric..."** (BR-AT-02)
3. If Card Number is provided but not numeric, display: **"Card Number must be Numeric..."** (BR-AT-03)
4. If Account ID is numeric but not found in cross-reference, display: **"Account ID NOT found..."** (BR-AT-04)
5. If Card Number is numeric but not found in cross-reference, display: **"Card Number NOT found..."** (BR-AT-04)

**Phase 2 — Mandatory Field Checks (11 fields):**
6. If Type Code is empty, display: **"Type CD can NOT be empty..."** (BR-AT-06)
7. If Category Code is empty, display: **"Category CD can NOT be empty..."** (BR-AT-06)
8. If Source is empty, display: **"Source can NOT be empty..."** (BR-AT-06)
9. If Description is empty, display: **"Description can NOT be empty..."** (BR-AT-06)
10. If Amount is empty, display: **"Amount can NOT be empty..."** (BR-AT-06)
11. If Origination Date is empty, display: **"Orig Date can NOT be empty..."** (BR-AT-06)
12. If Processing Date is empty, display: **"Proc Date can NOT be empty..."** (BR-AT-06)
13. If Merchant ID is empty, display: **"Merchant ID can NOT be empty..."** (BR-AT-06)
14. If Merchant Name is empty, display: **"Merchant Name can NOT be empty..."** (BR-AT-06)
15. If Merchant City is empty, display: **"Merchant City can NOT be empty..."** (BR-AT-06)
16. If Merchant Zip is empty, display: **"Merchant Zip can NOT be empty..."** (BR-AT-06)

**Phase 3 — Numeric Type Checks:**
17. If Type Code is not numeric, display: **"Type CD must be Numeric..."** (BR-AT-07)
18. If Category Code is not numeric, display: **"Category CD must be Numeric..."** (BR-AT-07)

**Phase 4 — Amount Format Validation:**
19. If Amount does not match +/-99999999.99 format, display: **"Amount should be in format -99999999.99"** (BR-AT-08)

**Phase 5 — Date Validation:**
20. If Origination Date does not match YYYY-MM-DD format, display: **"Orig Date should be in format YYYY-MM-DD"** (BR-AT-09)
21. If Processing Date does not match YYYY-MM-DD format, display: **"Proc Date should be in format YYYY-MM-DD"** (BR-AT-09)
22. If Origination Date is not a valid calendar date (e.g., 2024-02-30), display: **"Orig Date - Not a valid date..."** (BR-AT-10)
23. If Processing Date is not a valid calendar date, display: **"Proc Date - Not a valid date..."** (BR-AT-10)

**Phase 6 — Merchant ID Numeric Check:**
24. If Merchant ID is not numeric, display: **"Merchant ID must be Numeric..."** (BR-AT-11)

**General:**
25. Each error message is accompanied by cursor focus on the offending field.
26. Validation phases execute in strict sequential order (1 → 2 → 3 → 4 → 5 → 6); the first error in any phase halts the chain.
27. Unrecognized actions display "Invalid Key Pressed" (BR-CF-03).

**Business Rules:** BR-CF-03, BR-AT-01, BR-AT-02, BR-AT-03, BR-AT-04, BR-AT-06, BR-AT-07, BR-AT-08, BR-AT-09, BR-AT-10, BR-AT-11

---

### US-AT-04: Confirm Before Saving Transaction

**Epic:** EPIC-04 (Transaction Add — CT02)

**Title:** As a user, I want to confirm with 'Y' before the transaction is saved so that I can review my entries first.

**Description:**
After all 6 validation phases pass, the system requires explicit user confirmation before writing the transaction record. The user must enter 'Y' or 'y' in the confirmation field to proceed. Entering 'N', 'n', or leaving the field blank results in a prompt message asking the user to confirm. Any other value is rejected as invalid.

**Acceptance Criteria:**
1. After all validation passes, the confirmation field is highlighted/focused.
2. Entering 'Y' or 'y' and submitting proceeds to write the transaction record to the database.
3. Entering 'N', 'n', blank, or empty value displays the prompt: **"Confirm to add this transaction..."**
4. Entering any value other than 'Y'/'y'/'N'/'n'/blank displays the error: **"Invalid value. Valid values are (Y/N)..."**
5. The form data is preserved during the confirmation interaction (user entries are not lost).
6. The stateless REST pattern handles confirmation as part of the request payload (BR-CF-02).

**Business Rules:** BR-CF-02, BR-AT-12

---

### US-AT-05: See Auto-Generated Transaction ID After Add

**Epic:** EPIC-04 (Transaction Add — CT02)

**Title:** As a user, I want to see the auto-generated Transaction ID after a successful add so that I have a reference.

**Description:**
When a transaction is successfully written to the database, the system generates a unique Transaction ID using a monotonically increasing sequence (replacing the legacy browse-to-end algorithm). The success message displays the generated ID in green text, and all form fields are cleared for the next entry.

**Acceptance Criteria:**
1. On successful write, the system displays: **"Transaction added successfully. Your Tran ID is {ID}."** in green.
2. The generated Transaction ID is unique and monotonically increasing (greater than all existing IDs).
3. All form fields are cleared to blank after a successful add.
4. If the generated ID collides with an existing record (unlikely with sequences but handled), the error is displayed: **"Tran ID already exist..."** (BR-AT-14).
5. If an unexpected database error occurs during write, the error is displayed: **"Unable to Add Transaction..."**
6. The generated ID is a valid 16-character (or equivalent) identifier consistent with the legacy format.

**Business Rules:** BR-AT-13, BR-AT-14

---

### US-AT-06: Copy Last Transaction Data

**Epic:** EPIC-04 (Transaction Add — CT02)

**Title:** As a user, I want to press PF5 to copy the last transaction's data so that I can quickly add similar transactions.

**Description:**
Clicking the "Copy Last" button (replacing PF5) on the Add Transaction screen validates the key fields (Account ID or Card Number), then populates all data fields from the most recent transaction in the database (the transaction with the highest Transaction ID). This feature reduces data entry effort when adding multiple similar transactions.

**Acceptance Criteria:**
1. Clicking "Copy Last" first validates key fields (Account ID or Card Number must be valid).
2. If key field validation fails, the appropriate error message is shown (same as Phase 1 validation).
3. If key fields are valid, all 11 data fields (Type Code, Category Code, Source, Description, Amount, Origination Date, Processing Date, Merchant ID, Merchant Name, Merchant City, Merchant Zip) are populated from the most recent transaction record.
4. The Account ID and Card Number fields are NOT overwritten by the copy (they retain the user's entries).
5. The confirmation field remains blank after the copy (user must still confirm explicitly).
6. If no transactions exist in the database, an appropriate message is shown.

**Business Rules:** BR-AT-01 (key field validation still applies during copy)

---

### US-AT-07: Clear Add Transaction Form

**Epic:** EPIC-04 (Transaction Add — CT02)

**Title:** As a user, I want to press PF4 to clear the form so that I can start a fresh entry.

**Description:**
Clicking the "Clear" button (replacing PF4) on the Add Transaction screen resets all 14 input fields (including Account ID, Card Number, all 11 data fields, and confirmation) to blank. The cursor is positioned on the Account ID field for a fresh entry.

**Acceptance Criteria:**
1. Clicking "Clear" resets all 14 input fields to blank/empty.
2. The cursor (focus) is positioned on the Account ID field.
3. No error message is displayed after clearing.
4. Any previously displayed error or success message is also cleared.
5. The form is ready for a completely new entry.

**Business Rules:** None directly (UI behavior defined in BRE Section 3.3.3)

---

### US-AT-08: Return to Menu from Add Transaction

**Epic:** EPIC-04 (Transaction Add — CT02)

**Title:** As a user, I want to press PF3 to return to the menu without adding so that I can navigate away.

**Description:**
Clicking the "Back to Menu" button (replacing PF3) on the Add Transaction screen returns the user to the calling program or main menu without writing any transaction data. Any form entries are discarded.

**Acceptance Criteria:**
1. Clicking "Back to Menu" navigates to the main menu (or calling program).
2. No transaction record is written to the database.
3. Any unsaved form data is discarded without prompting.
4. Session remains active (BR-CF-01).

**Business Rules:** BR-CF-01

---

## Appendix: User Story to Business Rule Cross-Reference Matrix

This matrix ensures bidirectional traceability — every user story maps to its relevant business rules, and every business rule is covered.

| User Story | BR-CF-01 | BR-CF-02 | BR-CF-03 | BR-LT-01 | BR-LT-02 | BR-LT-03 | BR-LT-04 | BR-LT-05 | BR-LT-06 | BR-LT-07 | BR-LT-08 | BR-VT-01 | BR-VT-02 | BR-VT-03 | BR-VT-04 | BR-VT-05 | BR-AT-01 | BR-AT-02 | BR-AT-03 | BR-AT-04 | BR-AT-05 | BR-AT-06 | BR-AT-07 | BR-AT-08 | BR-AT-09 | BR-AT-10 | BR-AT-11 | BR-AT-12 | BR-AT-13 | BR-AT-14 |
|-----------|----------|----------|----------|-----------|-----------|-----------|-----------|-----------|-----------|-----------|-----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|----------|
| US-LT-01 | X | X | X | X | | | X | | | X | | | | | | | | | | | | | | | | | | | | |
| US-LT-02 | | | | | | | | X | | X | | | | | | | | | | | | | | | | | | | | |
| US-LT-03 | | | | | | | | | X | X | | | | | | | | | | | | | | | | | | | | |
| US-LT-04 | | | | | X | | X | | | | | | | | | | | | | | | | | | | | | | | |
| US-LT-05 | | | | | | X | | | | | X | | | | | | | | | | | | | | | | | | | |
| US-LT-06 | X | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
| US-VT-01 | | X | | | | | | | | | | | | X | X | | | | | | | | | | | | | | | |
| US-VT-02 | | | X | | | | | | | | | X | X | | X | | | | | | | | | | | | | | | |
| US-VT-03 | | | | | | | | | | | | | | | X | | | | | | | | | | | | | | | |
| US-VT-04 | | | | | | | | | | | | | | | | X | | | | | | | | | | | | | | |
| US-VT-05 | X | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
| US-AT-01 | | | | | | | | | | | | | | | | | X | X | | X | X | | | | | | | | | |
| US-AT-02 | | | | | | | | | | | | | | | | | X | | X | X | X | | | | | | | | | |
| US-AT-03 | | | X | | | | | | | | | | | | | | X | X | X | X | | X | X | X | X | X | X | | | |
| US-AT-04 | | X | | | | | | | | | | | | | | | | | | | | | | | | | | X | | |
| US-AT-05 | | | | | | | | | | | | | | | | | | | | | | | | | | | | | X | X |
| US-AT-06 | | | | | | | | | | | | | | | | | X | | | | | | | | | | | | | |
| US-AT-07 | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
| US-AT-08 | X | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |

**Verification: Every BR column has at least one "X" — all 30 business rules are covered.**
