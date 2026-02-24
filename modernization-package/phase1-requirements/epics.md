# Epics: Transaction Processing Module Modernization

> **Source Module:** Transaction Processing (COTRN00C, COTRN01C, COTRN02C)
> **BRE Reference:** `transactions-processing-module-doc-devin.md`
> **Target Stack:** Java 21, Spring Boot 3, Spring Data JPA, React, PostgreSQL

---

## Epic Overview

| Epic ID | Epic Name | Function Area | User Stories | Business Rules |
|---------|-----------|--------------|-------------|---------------|
| EPIC-01 | Infrastructure & Foundation | Cross-Functional | — | BR-CF-01, BR-CF-02, BR-CF-03 |
| EPIC-02 | Transaction List (CT00) | List Transactions | US-LT-01 — US-LT-06 | BR-LT-01 — BR-LT-08 |
| EPIC-03 | Transaction View (CT01) | View Transaction | US-VT-01 — US-VT-05 | BR-VT-01 — BR-VT-05 |
| EPIC-04 | Transaction Add (CT02) | Add Transaction | US-AT-01 — US-AT-08 | BR-AT-01 — BR-AT-14 |
| EPIC-05 | Cross-Reference Resolution | Add Transaction (shared) | US-AT-01, US-AT-02 | BR-AT-04, BR-AT-05 |
| EPIC-06 | Validation Chain Engine | Add Transaction (shared) | US-AT-03 | BR-AT-01 — BR-AT-11 |
| EPIC-07 | Database & Data Migration | Cross-Functional | — | All data-related BRs |
| EPIC-08 | React Frontend | Cross-Functional | All 19 stories | All BRs (UI layer) |

---

## EPIC-01: Infrastructure & Foundation

### Description
Establish the foundational project scaffolding, authentication, session management, and shared infrastructure that replaces the legacy CICS/COMMAREA-based runtime environment. This epic covers the cross-functional behaviors shared by all three programs (COTRN00C, COTRN01C, COTRN02C).

### Scope
- Spring Boot 3 project setup (Java 21, Maven/Gradle, Spring Data JPA)
- PostgreSQL database configuration and Flyway migration framework
- Authentication/authorization replacing CICS COMMAREA session check (BR-CF-01)
- Stateless REST architecture replacing pseudo-conversational model (BR-CF-02)
- Global exception handler for invalid key/action handling (BR-CF-03)
- React project scaffolding with routing
- CI/CD pipeline setup (GitHub Actions)
- Docker Compose for local development

### Business Rules Covered
| Rule ID | Rule | Implementation |
|---------|------|---------------|
| BR-CF-01 | Session Required | JWT or Spring Security session; redirect to login if unauthenticated |
| BR-CF-02 | Pseudo-Conversational Operation | Stateless REST request/response pattern |
| BR-CF-03 | Invalid Key Handling | Global error handler returns "Invalid Key Pressed" for unrecognized actions |

### Acceptance Criteria
- Spring Boot application starts and serves REST endpoints
- PostgreSQL connected via Spring Data JPA with Flyway migrations
- Unauthenticated requests redirect to login (replacing COSGN00C)
- Unrecognized actions return appropriate error response
- React app renders with routing between List, View, and Add screens
- CI/CD pipeline runs build, test, and lint on each push

### Dependencies
- None (this is the foundation epic)

### Estimated Effort
- Backend: 1-2 weeks
- Frontend: 1 week

---

## EPIC-02: Transaction List (CT00)

### Description
Implement the paginated transaction list view that replaces the COTRN00C program. Users can browse all transactions, navigate forward/backward through pages, filter by Transaction ID, and select a transaction for detailed viewing.

### Scope
- REST API endpoint: `GET /api/transactions` with pagination parameters
- Cursor-based pagination (replacing COMMAREA state fields)
- Filter by starting Transaction ID
- Selection mechanism to navigate to Transaction View
- Page size fixed at 10 rows (BR-LT-01)
- Boundary messages for top/bottom of data
- Navigation back to main menu

### Business Rules Covered
| Rule ID | Rule | Implementation |
|---------|------|---------------|
| BR-LT-01 | Page Size Fixed at 10 | `LIMIT 10` on query; page size constant |
| BR-LT-02 | Numeric Filter Validation | Server-side validation; error "Tran ID must be Numeric..." |
| BR-LT-03 | Valid Selection Value | Only 'S' accepted; error "Invalid selection. Valid value is S" |
| BR-LT-04 | Empty Filter Browses from Start | No filter → query from first record (default sort by TRAN-ID ASC) |
| BR-LT-05 | Forward Pagination Boundary | PF8 at end → "You are already at the bottom of the page..." |
| BR-LT-06 | Backward Pagination Boundary | PF7 at page 1 → "You are already at the top of the page..." |
| BR-LT-07 | Page State Preservation | Pagination tokens (first/last ID, page number, has-next flag) in API response |
| BR-LT-08 | Selection Triggers Detail View | 'S' selection navigates to `/transactions/{id}` view |

### User Stories
US-LT-01, US-LT-02, US-LT-03, US-LT-04, US-LT-05, US-LT-06

### Acceptance Criteria
- Exactly 10 rows per page
- Page forward/backward works with correct boundary messages
- Numeric filter validation with specific error message
- Selection navigates to view screen with correct transaction
- PF3 equivalent returns to main menu
- Page number displayed and tracked

### Dependencies
- EPIC-01 (Infrastructure & Foundation)
- EPIC-07 (Database schema for `transactions` table)

### Estimated Effort
- Backend: 1-2 weeks
- Frontend: 1 week

---

## EPIC-03: Transaction View (CT01)

### Description
Implement the read-only transaction detail view that replaces the COTRN01C program. Users can view all 13 fields of a transaction, either by auto-load from the list screen or by manual ID entry.

### Scope
- REST API endpoint: `GET /api/transactions/{id}`
- 13-field detail display (read-only)
- Auto-load when arriving from list with pre-selected ID
- Manual Transaction ID lookup
- Clear screen functionality
- Navigation back to list (with state preserved) and main menu

### Business Rules Covered
| Rule ID | Rule | Implementation |
|---------|------|---------------|
| BR-VT-01 | Transaction ID Required | Server validation; error "Tran ID can NOT be empty..." |
| BR-VT-02 | Transaction Must Exist | 404 handling; error "Transaction ID NOT found..." |
| BR-VT-03 | Pre-Selected Auto-Load | React route param triggers auto-fetch on mount |
| BR-VT-04 | Read-Only Display | No PUT/PATCH/DELETE endpoints; UI fields disabled |
| BR-VT-05 | PF5 Returns to List | Button navigates back to list with pagination state in URL/state |

### User Stories
US-VT-01, US-VT-02, US-VT-03, US-VT-04, US-VT-05

### Acceptance Criteria
- All 13 fields displayed correctly for a valid transaction
- Auto-load works when navigating from list with selection
- Manual ID entry works; empty → error; not found → error
- Clear resets all fields and positions cursor on ID input
- PF5 returns to list preserving pagination position
- PF3 returns to main menu
- No modification operations available (read-only enforced)

### Dependencies
- EPIC-01 (Infrastructure & Foundation)
- EPIC-02 (Transaction List — for navigation flow)
- EPIC-07 (Database schema)

### Estimated Effort
- Backend: 0.5-1 week
- Frontend: 1 week

---

## EPIC-04: Transaction Add (CT02)

### Description
Implement the transaction creation workflow that replaces the COTRN02C program. This is the most complex epic, encompassing a 14-field input form, 6-phase validation chain, cross-reference resolution, auto-generated Transaction IDs, explicit confirmation flow, and the "Copy Last Transaction" feature.

### Scope
- REST API endpoint: `POST /api/transactions`
- 14-field input form (13 data fields + confirmation)
- 6-phase sequential validation chain (delegated to EPIC-06)
- Cross-reference resolution (delegated to EPIC-05)
- Thread-safe Transaction ID auto-generation using PostgreSQL sequence
- Y/N confirmation mechanism
- Copy Last Transaction (PF5) feature
- Clear form (PF4) feature
- All field-specific error messages with cursor positioning
- Success message with generated Transaction ID

### Business Rules Covered
| Rule ID | Rule | Implementation |
|---------|------|---------------|
| BR-AT-01 | Account or Card Required | Validation Phase 1 |
| BR-AT-02 | Account ID Numeric | Validation Phase 1 |
| BR-AT-03 | Card Number Numeric | Validation Phase 1 |
| BR-AT-04 | Account/Card Must Exist | Cross-reference lookup (EPIC-05) |
| BR-AT-05 | Cross-Reference Resolution | Bidirectional resolution service (EPIC-05) |
| BR-AT-06 | All 11 Data Fields Mandatory | Validation Phase 2 |
| BR-AT-07 | Type/Category Must Be Numeric | Validation Phase 3 |
| BR-AT-08 | Amount Format Required | Validation Phase 4 |
| BR-AT-09 | Date Format Required | Validation Phase 5 |
| BR-AT-10 | Date Validity Required | Validation Phase 5 |
| BR-AT-11 | Merchant ID Numeric | Validation Phase 6 |
| BR-AT-12 | Explicit Confirmation | Y/N confirmation flow in API and UI |
| BR-AT-13 | Auto-Increment Transaction ID | PostgreSQL sequence `transaction_id_seq` |
| BR-AT-14 | Duplicate ID Rejection | Unique constraint on `transaction_id`; error "Tran ID already exist..." |

### User Stories
US-AT-01, US-AT-02, US-AT-03, US-AT-04, US-AT-05, US-AT-06, US-AT-07, US-AT-08

### Acceptance Criteria
- All 14 BR-AT rules pass automated tests
- 6-phase validation executes in strict order; first error halts chain
- Each error produces the exact legacy error message
- Cross-reference resolves Account → Card and Card → Account
- Confirmation: Y → write; N/blank → prompt; other → error
- Success shows generated Transaction ID in green
- PF5 copies most recent transaction data into form
- PF4 clears all fields; PF3 returns to menu
- Concurrent transaction additions produce unique IDs

### Dependencies
- EPIC-01 (Infrastructure & Foundation)
- EPIC-05 (Cross-Reference Resolution)
- EPIC-06 (Validation Chain Engine)
- EPIC-07 (Database schema)

### Estimated Effort
- Backend: 2-3 weeks
- Frontend: 1-2 weeks

---

## EPIC-05: Cross-Reference Resolution

### Description
Implement the bidirectional Account ID ↔ Card Number resolution service that replaces the CXACAIX (Alternate Index) and CCXREF (KSDS) VSAM file lookups. This service is critical for the Add Transaction function's Phase 1 validation.

### Scope
- `card_cross_reference` PostgreSQL table (replacing CARDXREF VSAM file)
- REST API endpoint: `GET /api/cross-reference/by-account/{accountId}`
- REST API endpoint: `GET /api/cross-reference/by-card/{cardNumber}`
- Service layer for bidirectional resolution
- Indexes on both `account_id` and `card_number` columns
- Not-found error handling matching legacy messages

### Business Rules Covered
| Rule ID | Rule | Implementation |
|---------|------|---------------|
| BR-AT-04 | Account/Card Must Exist | Query returns 404 if not found; error messages match legacy |
| BR-AT-05 | Cross-Reference Resolution | Path A: Account ID → Card Number; Path B: Card Number → Account ID |

### Acceptance Criteria
- Account ID lookup returns associated Card Number (or 404)
- Card Number lookup returns associated Account ID (or 404)
- Error messages: "Account ID NOT found..." and "Card Number NOT found..."
- XREF lookup errors: "Unable to lookup Acct in XREF AIX file..." / "Unable to lookup Card # in XREF file..."
- Both lookup paths covered by unit and integration tests

### Dependencies
- EPIC-01 (Infrastructure & Foundation)
- EPIC-07 (Database schema for `card_cross_reference` table)

### Estimated Effort
- 0.5-1 week

---

## EPIC-06: Validation Chain Engine

### Description
Implement the 6-phase sequential validation engine that replaces the COBOL validation logic in COTRN02C. Each phase must execute in order, and the first validation failure halts the chain and returns a field-specific error message with cursor positioning information.

### Scope
- Phase 1: Key Field Validation (Account ID or Card Number required, numeric checks, cross-reference lookup)
- Phase 2: Mandatory Field Checks (11 fields must be non-empty)
- Phase 3: Numeric Type Checks (Type Code and Category Code)
- Phase 4: Amount Format Validation (signed decimal: +/-99999999.99)
- Phase 5: Date Validation (YYYY-MM-DD format + calendar validity)
- Phase 6: Merchant ID Numeric Check
- Validation result DTO with error message, error field, and phase number
- Spring Boot validation framework integration (custom validators)

### Business Rules Covered
| Rule ID | Rule | Validation Phase |
|---------|------|-----------------|
| BR-AT-01 | Account or Card Required | Phase 1 |
| BR-AT-02 | Account ID Numeric | Phase 1 |
| BR-AT-03 | Card Number Numeric | Phase 1 |
| BR-AT-04 | Account/Card Must Exist | Phase 1 |
| BR-AT-05 | Cross-Reference Resolution | Phase 1 |
| BR-AT-06 | All 11 Data Fields Mandatory | Phase 2 |
| BR-AT-07 | Type/Category Must Be Numeric | Phase 3 |
| BR-AT-08 | Amount Format Required | Phase 4 |
| BR-AT-09 | Date Format Required | Phase 5 |
| BR-AT-10 | Date Validity Required | Phase 5 |
| BR-AT-11 | Merchant ID Numeric | Phase 6 |

### Acceptance Criteria
- Validation phases execute in strict sequential order (1 → 2 → 3 → 4 → 5 → 6)
- First validation error halts the chain
- Each error returns the exact legacy error message text
- Each error identifies the offending field (for cursor positioning in UI)
- All 26 error messages from the Add Transaction error catalog have dedicated test cases
- Amount validation accepts both `+` and `-` signs with `99999999.99` format
- Date validation checks both format (YYYY-MM-DD) and calendar validity (e.g., rejects Feb 30)

### Dependencies
- EPIC-05 (Cross-Reference Resolution — used in Phase 1)

### Estimated Effort
- 1-2 weeks

---

## EPIC-07: Database & Data Migration

### Description
Design and implement the PostgreSQL database schema that replaces the VSAM KSDS/AIX file structures, and provide migration scripts and seed data for development and testing.

### Scope
- Flyway migration scripts for:
  - `transactions` table (replacing TRANSACT VSAM KSDS)
  - `card_cross_reference` table (replacing CCXREF KSDS and CXACAIX AIX)
  - `transaction_id_seq` PostgreSQL sequence (replacing browse-to-end ID generation)
- COBOL PIC clause to PostgreSQL type mapping:
  - `TRAN-ID X(16)` → `BIGINT` (sequence-generated) or `VARCHAR(16)`
  - `TRAN-AMT S9(09)V99` → `NUMERIC(11,2)`
  - `TRAN-MERCHANT-ID 9(09)` → `BIGINT`
  - `TRAN-TYPE-CD X(02)` → `VARCHAR(2)`
  - `TRAN-CAT-CD X(04)` → `VARCHAR(4)`
  - `TRAN-ORIG-TS X(26)` → `TIMESTAMP`
  - `TRAN-PROC-TS X(26)` → `TIMESTAMP`
  - All text fields → `VARCHAR` with matching max lengths
- Indexes: Primary key on `transaction_id`; indexes on `card_cross_reference(account_id)` and `card_cross_reference(card_number)`
- Seed data for development and testing (sample transactions and cross-reference records)
- Data migration scripts for production cutover (VSAM → PostgreSQL)

### Acceptance Criteria
- Flyway migrations run cleanly from empty database
- Schema matches legacy record layout field-for-field
- Seed data allows all user stories to be tested
- Sequence generates unique, monotonically increasing IDs
- Foreign key / referential integrity constraints enforced

### Dependencies
- EPIC-01 (Infrastructure — Flyway configured)

### Estimated Effort
- 1 week

---

## EPIC-08: React Frontend

### Description
Build the responsive React frontend that replaces the 3270 BMS map screens. The UI must provide equivalent functionality to the legacy terminal screens while offering a modern web experience.

### Scope
- **TransactionList page** — Table with 10 rows, pagination controls (Next/Previous), filter input, selection column, page number display
- **TransactionView page** — 13-field read-only detail display, manual ID lookup, clear button, navigation buttons
- **TransactionAdd page** — 14-field input form, validation error display with field highlighting, confirmation dialog, copy-last-transaction button
- **Navigation** — React Router mapping: `/transactions` (list), `/transactions/:id` (view), `/transactions/new` (add)
- **Function Key Mapping:**
  - PF3 → "Back to Menu" button / Escape key
  - PF4 → "Clear" button
  - PF5 → "Back to List" (view) / "Copy Last" (add) button
  - PF7 → "Previous Page" button / Page Up key
  - PF8 → "Next Page" button / Page Down key
- **Error Display** — Field-specific error messages shown inline; offending field highlighted and focused
- **Success Feedback** — Green success message with generated Transaction ID after add

### User Stories
All 19 user stories (US-LT-01 through US-AT-08)

### Acceptance Criteria
- All screens render correctly and are responsive
- Function key equivalents work via buttons and keyboard shortcuts
- Validation errors display inline with field focus
- Navigation between List → View → Add works correctly
- Pagination state preserved when returning from View to List
- Copy Last Transaction populates all data fields
- Confirmation dialog shows Y/N prompt before transaction write

### Dependencies
- EPIC-01 (React project scaffolding)
- EPIC-02, EPIC-03, EPIC-04 (API endpoints to integrate with)

### Estimated Effort
- 3-4 weeks

---

## Epic Dependency Graph

```
EPIC-01 (Foundation)
  ├── EPIC-07 (Database)
  │     ├── EPIC-02 (List - CT00)
  │     ├── EPIC-03 (View - CT01)
  │     ├── EPIC-05 (Cross-Reference)
  │     │     └── EPIC-06 (Validation Chain)
  │     │           └── EPIC-04 (Add - CT02)
  │     └── EPIC-04 (Add - CT02)
  └── EPIC-08 (React Frontend)
        ├── depends on EPIC-02 API
        ├── depends on EPIC-03 API
        └── depends on EPIC-04 API
```

---

## Business Rule to Epic Traceability

| Business Rule | Epic(s) |
|--------------|---------|
| BR-CF-01 | EPIC-01 |
| BR-CF-02 | EPIC-01 |
| BR-CF-03 | EPIC-01 |
| BR-LT-01 | EPIC-02 |
| BR-LT-02 | EPIC-02 |
| BR-LT-03 | EPIC-02 |
| BR-LT-04 | EPIC-02 |
| BR-LT-05 | EPIC-02 |
| BR-LT-06 | EPIC-02 |
| BR-LT-07 | EPIC-02 |
| BR-LT-08 | EPIC-02 |
| BR-VT-01 | EPIC-03 |
| BR-VT-02 | EPIC-03 |
| BR-VT-03 | EPIC-03 |
| BR-VT-04 | EPIC-03 |
| BR-VT-05 | EPIC-03 |
| BR-AT-01 | EPIC-04, EPIC-06 |
| BR-AT-02 | EPIC-04, EPIC-06 |
| BR-AT-03 | EPIC-04, EPIC-06 |
| BR-AT-04 | EPIC-04, EPIC-05 |
| BR-AT-05 | EPIC-04, EPIC-05 |
| BR-AT-06 | EPIC-04, EPIC-06 |
| BR-AT-07 | EPIC-04, EPIC-06 |
| BR-AT-08 | EPIC-04, EPIC-06 |
| BR-AT-09 | EPIC-04, EPIC-06 |
| BR-AT-10 | EPIC-04, EPIC-06 |
| BR-AT-11 | EPIC-04, EPIC-06 |
| BR-AT-12 | EPIC-04 |
| BR-AT-13 | EPIC-04 |
| BR-AT-14 | EPIC-04 |

**All 30 Business Rules are covered by at least one Epic.**
