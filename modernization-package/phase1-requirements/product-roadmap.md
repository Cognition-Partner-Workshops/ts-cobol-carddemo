# Product Roadmap: Transaction Processing Module Modernization

> **Source Module:** Transaction Processing (COTRN00C, COTRN01C, COTRN02C)
> **Legacy Platform:** CICS/COBOL on z/OS with VSAM data stores
> **Target Platform:** Java 21, Spring Boot 3, Spring Data JPA, React, PostgreSQL
> **BRE Reference:** `transactions-processing-module-doc-devin.md`

---

## Vision Statement

Modernize the CardDemo Transaction Processing Module from a CICS/COBOL mainframe application into a cloud-native microservice architecture. The modernized system will deliver 100% business logic parity with the legacy module while enabling modern DevOps practices, horizontal scalability, and a responsive web-based user interface.

---

## Roadmap Phases

### Phase 1: Requirements & Planning (Current Phase)

**Duration:** 1-2 Weeks
**Objective:** Establish full requirements traceability from legacy BRE to modernized system.

| Deliverable | Description | Status |
|-------------|-------------|--------|
| Product Roadmap | This document — full modernization scope and timeline | In Progress |
| Epics | Major functional areas decomposed into epics | In Progress |
| User Stories | 19 user stories with acceptance criteria and BR mapping | In Progress |
| BR Traceability Matrix | All 30 business rules mapped to at least one user story | In Progress |
| Data Mapping Document | COBOL PIC clauses mapped to Java/PostgreSQL types | Planned |

**Exit Criteria:**
- All 30 BRs traceable to user stories
- Stakeholder sign-off on requirements

---

### Phase 2: Architecture & Design

**Duration:** 2-3 Weeks
**Objective:** Define the technical architecture, API contracts, and database schema.

| Deliverable | Description |
|-------------|-------------|
| API Specification | OpenAPI 3.0 spec for all three functions (List, View, Add) replacing 3270 screens |
| Database Schema | PostgreSQL schema with Flyway migrations; VSAM KSDS/AIX mapped to relational tables |
| Sequence Diagrams | Interaction flows for validation chain, cross-reference resolution, pagination |
| Architecture Decision Records | Key decisions: session management (JWT vs. stateless), ID generation strategy, error handling patterns |
| Component Diagram | Spring Boot service layer, repository layer, controller layer, React component tree |

**Key Decisions:**
| Legacy Concept | Modernization Strategy |
|---------------|----------------------|
| COMMAREA state | Stateless REST with pagination tokens or JWT session |
| VSAM TRANSACT (KSDS) | PostgreSQL `transactions` table with BIGINT sequence PK |
| VSAM CXACAIX (AIX) | PostgreSQL `card_cross_reference` table with indexes on `account_id` and `card_number` |
| VSAM CCXREF (KSDS) | Same `card_cross_reference` table, queried by card number |
| BMS Maps (3270 screens) | React components with responsive layout |
| CICS XCTL navigation | React Router client-side navigation + REST API calls |
| Pseudo-conversational model | Stateless HTTP request/response |
| PF keys (PF3, PF4, PF5, PF7, PF8) | UI buttons and keyboard shortcuts |
| COBOL PIC S9(09)V99 | Java `BigDecimal` / PostgreSQL `NUMERIC(11,2)` |
| COBOL PIC X(16) for TRAN-ID | PostgreSQL `BIGINT` with sequence or `VARCHAR(16)` |
| CSUTLDTC date validation | Java `LocalDate` parsing with `DateTimeFormatter` |

**Exit Criteria:**
- API spec reviewed and approved
- Database schema reviewed (Flyway scripts generated)
- Architecture diagrams complete

---

### Phase 3: Foundation & Infrastructure

**Duration:** 1-2 Weeks
**Objective:** Set up project scaffolding, CI/CD pipeline, and database infrastructure.

| Deliverable | Description |
|-------------|-------------|
| Spring Boot Project | Java 21, Spring Boot 3, Spring Data JPA, Maven/Gradle build |
| PostgreSQL Setup | Docker Compose for local dev; Flyway migration scripts for `transactions`, `card_cross_reference` tables |
| React Project | Create React App or Vite-based project with routing scaffold |
| CI/CD Pipeline | GitHub Actions for build, test, lint, and deploy |
| Seed Data | Migration of sample VSAM data to PostgreSQL for development and testing |

**Exit Criteria:**
- `mvn clean install` / `npm run build` pass
- Flyway migrations run successfully
- CI pipeline green

---

### Phase 4: Core Development — Transaction List (CT00)

**Duration:** 2-3 Weeks
**Objective:** Implement the List Transactions function with full business rule parity.

| Component | Description | Business Rules |
|-----------|-------------|---------------|
| REST API: `GET /api/transactions` | Paginated list endpoint with cursor-based pagination | BR-LT-01, BR-LT-04, BR-LT-05, BR-LT-06, BR-LT-07 |
| REST API: `GET /api/transactions?startId={id}` | Filter by starting Transaction ID | BR-LT-02, BR-LT-04 |
| Service Layer | Pagination logic, boundary detection, filter validation | BR-LT-01 through BR-LT-08 |
| React: TransactionList component | Table with 10 rows, page nav, filter input, selection | BR-LT-01, BR-LT-03, BR-LT-08 |
| Error Handling | Numeric filter validation, invalid selection, boundary messages | BR-LT-02, BR-LT-03, BR-LT-05, BR-LT-06 |
| Cross-Functional | Session validation, invalid key handling | BR-CF-01, BR-CF-02, BR-CF-03 |

**User Stories:** US-LT-01 through US-LT-06

**Exit Criteria:**
- All 8 BR-LT rules pass acceptance tests
- Pagination boundary messages match legacy exactly
- 10-row page size enforced

---

### Phase 5: Core Development — Transaction View (CT01)

**Duration:** 1-2 Weeks
**Objective:** Implement the View Transaction function with full business rule parity.

| Component | Description | Business Rules |
|-----------|-------------|---------------|
| REST API: `GET /api/transactions/{id}` | Read single transaction by ID | BR-VT-01, BR-VT-02 |
| Service Layer | Lookup, not-found handling, read-only enforcement | BR-VT-01 through BR-VT-04 |
| React: TransactionView component | 13-field read-only detail display | BR-VT-03, BR-VT-04 |
| Navigation: Auto-load from List | Pre-selected ID from list auto-loads detail | BR-VT-03 |
| Navigation: Return to List | PF5 equivalent returns to list with state preserved | BR-VT-05 |
| Cross-Functional | Session validation, invalid key handling | BR-CF-01, BR-CF-02, BR-CF-03 |

**User Stories:** US-VT-01 through US-VT-05

**Exit Criteria:**
- All 5 BR-VT rules pass acceptance tests
- 13 fields displayed correctly
- Auto-load from list works without redundant lookup

---

### Phase 6: Core Development — Transaction Add (CT02)

**Duration:** 3-4 Weeks
**Objective:** Implement the Add Transaction function including the full 6-phase validation chain.

| Component | Description | Business Rules |
|-----------|-------------|---------------|
| REST API: `POST /api/transactions` | Create transaction endpoint | BR-AT-01 through BR-AT-14 |
| Validation Phase 1: Key Fields | Account ID / Card Number conditional requirement and cross-reference resolution | BR-AT-01, BR-AT-02, BR-AT-03, BR-AT-04, BR-AT-05 |
| Validation Phase 2: Mandatory Fields | 11 mandatory field checks | BR-AT-06 |
| Validation Phase 3: Numeric Types | Type Code and Category Code numeric validation | BR-AT-07 |
| Validation Phase 4: Amount Format | Signed decimal format validation (+/-99999999.99) | BR-AT-08 |
| Validation Phase 5: Date Validation | YYYY-MM-DD format + calendar validity | BR-AT-09, BR-AT-10 |
| Validation Phase 6: Merchant ID | Numeric check on Merchant ID | BR-AT-11 |
| Confirmation Flow | Y/N confirmation mechanism | BR-AT-12 |
| ID Generation | Thread-safe auto-increment (PostgreSQL sequence) | BR-AT-13 |
| Duplicate Handling | Unique constraint violation handling | BR-AT-14 |
| Cross-Reference Service | Account ↔ Card bidirectional resolution | BR-AT-04, BR-AT-05 |
| React: TransactionAdd component | 14-field form with validation feedback, PF key buttons | All BR-AT rules |
| Copy Last Transaction (PF5) | Populate form from most recent transaction | US-AT-06 |
| Cross-Functional | Session validation, invalid key handling | BR-CF-01, BR-CF-02, BR-CF-03 |

**User Stories:** US-AT-01 through US-AT-08

**Exit Criteria:**
- All 14 BR-AT rules pass acceptance tests
- 6-phase validation chain executes in order
- Field-specific error messages with cursor positioning match legacy
- Thread-safe ID generation under concurrent access

---

### Phase 7: Integration & End-to-End Testing

**Duration:** 2-3 Weeks
**Objective:** Verify complete business logic parity and data integrity.

| Activity | Description |
|----------|-------------|
| Unit Tests | JUnit 5 + Mockito; every error message in the Error Catalog has a dedicated test case |
| Integration Tests | Full API integration tests against PostgreSQL (Testcontainers) |
| Boundary Tests | Pagination limits (top/bottom), invalid data formats, edge cases |
| Persistence Tests | Verify PostgreSQL records match legacy TRANSACT record layout |
| Error Catalog Coverage | All 30+ error messages from Section 8.2 tested |
| Cross-Reference Tests | Bidirectional Account ↔ Card resolution |
| Concurrency Tests | Concurrent ID generation, duplicate key handling |
| UI E2E Tests | Playwright or Cypress tests for all 19 user stories |

**Exit Criteria:**
- 100% error catalog coverage
- All 30 BRs verified in automated tests
- Data written to PostgreSQL matches legacy record layout
- No regression from legacy behavior

---

### Phase 8: Data Migration & Cutover

**Duration:** 2-3 Weeks
**Objective:** Migrate existing VSAM data to PostgreSQL and transition to the new system.

| Activity | Description |
|----------|-------------|
| Data Export | Extract TRANSACT, CARDXREF VSAM files to flat files or CSV |
| Data Transform | Convert EBCDIC to UTF-8; map COBOL packed decimal to Java BigDecimal |
| Data Load | Bulk insert into PostgreSQL using Spring Batch or COPY command |
| Data Validation | Row counts, checksum comparison, spot-check critical records |
| Parallel Run | Run legacy and modern systems side-by-side for validation period |
| Cutover | Switch traffic to modernized system; decommission legacy CICS transactions |

**Exit Criteria:**
- All TRANSACT records migrated with zero data loss
- Cross-reference data intact
- Parallel run shows identical behavior for sample transactions

---

## Timeline Summary

| Phase | Duration | Cumulative |
|-------|----------|-----------|
| Phase 1: Requirements & Planning | 1-2 weeks | Weeks 1-2 |
| Phase 2: Architecture & Design | 2-3 weeks | Weeks 3-5 |
| Phase 3: Foundation & Infrastructure | 1-2 weeks | Weeks 5-7 |
| Phase 4: Transaction List (CT00) | 2-3 weeks | Weeks 7-10 |
| Phase 5: Transaction View (CT01) | 1-2 weeks | Weeks 10-12 |
| Phase 6: Transaction Add (CT02) | 3-4 weeks | Weeks 12-16 |
| Phase 7: Integration & E2E Testing | 2-3 weeks | Weeks 16-19 |
| Phase 8: Data Migration & Cutover | 2-3 weeks | Weeks 19-22 |

**Total Estimated Duration: 19-22 Weeks**

---

## Risk Register

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Validation chain logic parity gap | High | Each of the 6 validation phases has dedicated unit tests; error messages compared character-by-character with legacy |
| Concurrent ID generation race condition | Medium | Use PostgreSQL `SEQUENCE` instead of legacy browse-to-end approach; eliminates race condition by design |
| Cross-reference data inconsistency | Medium | Foreign key constraints in PostgreSQL enforce referential integrity that VSAM cannot |
| EBCDIC-to-UTF8 data corruption during migration | Medium | Automated comparison scripts; spot-check packed decimal fields |
| Legacy READ with UPDATE lock contention (BR-VT-02) | Low | Eliminated by design — View uses standard SELECT without row locking |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Business Rule Coverage | 30/30 BRs implemented and tested |
| User Story Completion | 19/19 stories accepted |
| Error Message Parity | 100% of legacy error messages reproduced |
| Data Migration Accuracy | 100% row count match; zero data loss |
| Test Coverage | > 90% line coverage on service and validation layers |
| Response Time | List page load < 500ms; Add transaction < 1s |
