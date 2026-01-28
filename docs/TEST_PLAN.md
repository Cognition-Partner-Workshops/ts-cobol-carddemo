# CardDemo Migration Test Plan

## Document Information

| Item | Details |
|------|---------|
| Project | CardDemo Mainframe to Cloud Migration |
| Version | 1.0 |
| Date | January 2026 |
| Status | Draft |

## 1. Introduction

### 1.1 Purpose

This test plan defines the testing strategy, approach, and procedures for validating the migrated CardDemo application. The original COBOL/CICS mainframe application has been modernized to a Java Spring Boot microservices backend with a React TypeScript frontend.

### 1.2 Scope

Testing covers the complete migrated application stack including all backend microservices, the React frontend, API integrations, database operations, and end-to-end business workflows that replicate the original mainframe functionality.

### 1.3 References

| Document | Location |
|----------|----------|
| Functional Requirements | docs/FUNCTIONAL_REQUIREMENTS.md |
| Architecture Documentation | docs/ARCHITECTURE.md |
| Migration Guide | docs/MIGRATION_MODERNIZATION_GUIDE.md |
| User Stories | docs/USER_STORIES.md |

## 2. Test Strategy

### 2.1 Testing Levels

The testing approach follows a pyramid structure with multiple levels of validation:

**Level 1 - Unit Testing** focuses on individual components in isolation. Backend services use JUnit 5 with Mockito for mocking dependencies. Frontend components use Jest with React Testing Library for component testing.

**Level 2 - Integration Testing** validates interactions between components. This includes API endpoint testing with Spring Boot Test, database integration with H2/PostgreSQL, and service-to-service communication through the API Gateway.

**Level 3 - System Testing** covers end-to-end workflows that span multiple services and the frontend application. This validates complete business processes from user interaction through database persistence.

**Level 4 - Acceptance Testing** confirms the migrated application meets all functional requirements from the original mainframe system. Business stakeholders validate that all COBOL program functionality has been correctly migrated.

### 2.2 Testing Types

| Type | Description | Tools |
|------|-------------|-------|
| Functional | Verify features work as specified | JUnit, Jest, Cypress |
| Integration | Test component interactions | Spring Boot Test, Testcontainers |
| Performance | Validate response times and throughput | JMeter, k6 |
| Security | Identify vulnerabilities | OWASP ZAP, SonarQube |
| Regression | Ensure changes don't break existing functionality | Automated test suites |
| User Acceptance | Business validation of migrated features | Manual testing with stakeholders |

## 3. Test Environment

### 3.1 Environment Configuration

**Development Environment**
- Backend: Java 21, Spring Boot 3.2.2, H2 in-memory database
- Frontend: Node.js 18+, Vite dev server on port 3000
- Services run locally with docker-compose

**Test Environment**
- Backend: Java 21, Spring Boot 3.2.2, PostgreSQL 15
- Frontend: Built React app served by nginx
- Deployed to AWS ECS with RDS PostgreSQL

**Staging Environment**
- Production-equivalent configuration
- AWS RDS PostgreSQL with production-like data volume
- Load balancer and auto-scaling enabled

### 3.2 Service Ports

| Service | Development Port | Description |
|---------|-----------------|-------------|
| API Gateway | 8080 | Routes all frontend requests |
| Auth Service | 8081 | Authentication and JWT management |
| Customer Service | 8082 | Customer CRUD operations |
| Account Service | 8083 | Account management |
| Card Service | 8084 | Card operations |
| Transaction Service | 8085 | Transaction processing |
| Payment Service | 8086 | Bill payment handling |
| Reporting Service | 8087 | Reports and analytics |
| Batch Service | 8088 | Batch job processing |
| Frontend | 3000 | React development server |

### 3.3 Test Data

Test data is derived from the original VSAM files and includes customers with various FICO scores, accounts with different credit limits and balances, cards in active and inactive states, historical transactions across multiple categories, and payments in various statuses.

## 4. Backend Testing

### 4.1 Unit Test Cases - Auth Service

| TC-AUTH-001 | User Login with Valid Credentials |
|-------------|-----------------------------------|
| Objective | Verify successful authentication returns JWT tokens |
| Preconditions | User exists in database with valid credentials |
| Steps | 1. POST /api/v1/auth/login with username and password |
| Expected | 200 OK with accessToken, refreshToken, and user details |
| COBOL Mapping | COSGN00C sign-on validation |

| TC-AUTH-002 | User Login with Invalid Credentials |
|-------------|-------------------------------------|
| Objective | Verify authentication fails with incorrect password |
| Preconditions | User exists in database |
| Steps | 1. POST /api/v1/auth/login with wrong password |
| Expected | 401 Unauthorized with error message |
| COBOL Mapping | COSGN00C invalid sign-on handling |

| TC-AUTH-003 | Token Refresh |
|-------------|---------------|
| Objective | Verify refresh token generates new access token |
| Preconditions | Valid refresh token exists |
| Steps | 1. POST /api/v1/auth/refresh with refreshToken |
| Expected | 200 OK with new accessToken |
| COBOL Mapping | Session management (implicit in CICS) |

| TC-AUTH-004 | User Registration |
|-------------|-------------------|
| Objective | Verify new user can register |
| Preconditions | Username does not exist |
| Steps | 1. POST /api/v1/auth/register with user details |
| Expected | 201 Created with user information |
| COBOL Mapping | COUSR00C user creation |

### 4.2 Unit Test Cases - Customer Service

| TC-CUST-001 | Get All Customers with Pagination |
|-------------|-----------------------------------|
| Objective | Verify paginated customer list retrieval |
| Preconditions | Multiple customers exist in database |
| Steps | 1. GET /api/v1/customers?page=0&size=10 |
| Expected | 200 OK with PagedResponse containing customers |
| COBOL Mapping | COCRDLIC customer list display |

| TC-CUST-002 | Get Customer by ID |
|-------------|-------------------|
| Objective | Verify single customer retrieval |
| Preconditions | Customer with ID exists |
| Steps | 1. GET /api/v1/customers/{id} |
| Expected | 200 OK with customer details |
| COBOL Mapping | COCRDLIC customer detail view |

| TC-CUST-003 | Search Customers |
|-------------|-----------------|
| Objective | Verify customer search by name, SSN, or phone |
| Preconditions | Customers exist matching search criteria |
| Steps | 1. GET /api/v1/customers/search?query=Smith |
| Expected | 200 OK with matching customers |
| COBOL Mapping | COCRDLIC search functionality |

| TC-CUST-004 | Create Customer |
|-------------|-----------------|
| Objective | Verify new customer creation |
| Preconditions | Valid customer data provided |
| Steps | 1. POST /api/v1/customers with customer JSON |
| Expected | 201 Created with new customer ID |
| COBOL Mapping | COCRDUPC customer add |

| TC-CUST-005 | Update Customer |
|-------------|-----------------|
| Objective | Verify customer information update |
| Preconditions | Customer exists |
| Steps | 1. PUT /api/v1/customers/{id} with updated data |
| Expected | 200 OK with updated customer |
| COBOL Mapping | COCRDUPC customer update |

| TC-CUST-006 | Delete Customer |
|-------------|-----------------|
| Objective | Verify customer deletion |
| Preconditions | Customer exists with no active accounts |
| Steps | 1. DELETE /api/v1/customers/{id} |
| Expected | 204 No Content |
| COBOL Mapping | COCRDUPC customer delete |

### 4.3 Unit Test Cases - Account Service

| TC-ACCT-001 | Get All Accounts |
|-------------|------------------|
| Objective | Verify paginated account list retrieval |
| Preconditions | Accounts exist in database |
| Steps | 1. GET /api/v1/accounts?page=0&size=10 |
| Expected | 200 OK with PagedResponse containing accounts |
| COBOL Mapping | COACTVWC account list |

| TC-ACCT-002 | Get Accounts by Customer ID |
|-------------|----------------------------|
| Objective | Verify retrieval of customer's accounts |
| Preconditions | Customer has associated accounts |
| Steps | 1. GET /api/v1/accounts/customer/{customerId} |
| Expected | 200 OK with customer's accounts |
| COBOL Mapping | COACTVWC customer account view |

| TC-ACCT-003 | Get Active Accounts |
|-------------|---------------------|
| Objective | Verify filtering of active accounts only |
| Preconditions | Mix of active and inactive accounts exist |
| Steps | 1. GET /api/v1/accounts/active |
| Expected | 200 OK with only active accounts |
| COBOL Mapping | COACTVWC active filter |

| TC-ACCT-004 | Get Over-Limit Accounts |
|-------------|------------------------|
| Objective | Verify retrieval of accounts exceeding credit limit |
| Preconditions | Accounts with balance > credit limit exist |
| Steps | 1. GET /api/v1/accounts/over-limit |
| Expected | 200 OK with over-limit accounts |
| COBOL Mapping | COACTVWC over-limit report |

| TC-ACCT-005 | Activate Account |
|-------------|------------------|
| Objective | Verify account activation |
| Preconditions | Inactive account exists |
| Steps | 1. POST /api/v1/accounts/{id}/activate |
| Expected | 200 OK with activeStatus = 'Y' |
| COBOL Mapping | COACTUPC status change |

| TC-ACCT-006 | Deactivate Account |
|-------------|-------------------|
| Objective | Verify account deactivation |
| Preconditions | Active account exists |
| Steps | 1. POST /api/v1/accounts/{id}/deactivate |
| Expected | 200 OK with activeStatus = 'N' |
| COBOL Mapping | COACTUPC status change |

### 4.4 Unit Test Cases - Card Service

| TC-CARD-001 | Get All Cards |
|-------------|---------------|
| Objective | Verify paginated card list retrieval |
| Preconditions | Cards exist in database |
| Steps | 1. GET /api/v1/cards?page=0&size=10 |
| Expected | 200 OK with PagedResponse containing cards |
| COBOL Mapping | COCRDSLC card list |

| TC-CARD-002 | Get Card by Card Number |
|-------------|------------------------|
| Objective | Verify single card retrieval |
| Preconditions | Card with number exists |
| Steps | 1. GET /api/v1/cards/{cardNumber} |
| Expected | 200 OK with card details |
| COBOL Mapping | COCRDSLC card detail |

| TC-CARD-003 | Get Cards Expiring Soon |
|-------------|------------------------|
| Objective | Verify retrieval of cards expiring within N days |
| Preconditions | Cards with various expiration dates exist |
| Steps | 1. GET /api/v1/cards/expiring-soon?days=30 |
| Expected | 200 OK with cards expiring within 30 days |
| COBOL Mapping | COCRDSLC expiration report |

| TC-CARD-004 | Search Cards by Last Four Digits |
|-------------|----------------------------------|
| Objective | Verify card search by partial number |
| Preconditions | Cards exist in database |
| Steps | 1. GET /api/v1/cards/search?lastFour=1234 |
| Expected | 200 OK with matching cards |
| COBOL Mapping | COCRDSLC card search |

| TC-CARD-005 | Issue New Card |
|-------------|----------------|
| Objective | Verify new card creation |
| Preconditions | Valid account and customer exist |
| Steps | 1. POST /api/v1/cards with card data |
| Expected | 201 Created with generated card number |
| COBOL Mapping | COCRDUPC card issue |

| TC-CARD-006 | Activate Card |
|-------------|---------------|
| Objective | Verify card activation |
| Preconditions | Inactive card exists |
| Steps | 1. POST /api/v1/cards/{cardNumber}/activate |
| Expected | 200 OK with activeStatus = 'Y' |
| COBOL Mapping | COCRDUPC card activation |

### 4.5 Unit Test Cases - Transaction Service

| TC-TXN-001 | Get All Transactions |
|------------|---------------------|
| Objective | Verify paginated transaction list retrieval |
| Preconditions | Transactions exist in database |
| Steps | 1. GET /api/v1/transactions?page=0&size=10 |
| Expected | 200 OK with PagedResponse containing transactions |
| COBOL Mapping | COTRN00C transaction list |

| TC-TXN-002 | Get Transactions by Card Number |
|------------|--------------------------------|
| Objective | Verify retrieval of card's transaction history |
| Preconditions | Card has associated transactions |
| Steps | 1. GET /api/v1/transactions/card/{cardNumber} |
| Expected | 200 OK with card's transactions |
| COBOL Mapping | COTRN00C card transaction view |

| TC-TXN-003 | Get Transactions by Date Range |
|------------|-------------------------------|
| Objective | Verify filtering transactions by date |
| Preconditions | Transactions exist within date range |
| Steps | 1. GET /api/v1/transactions/date-range?start=2026-01-01&end=2026-01-31 |
| Expected | 200 OK with transactions in range |
| COBOL Mapping | COTRN00C date filter |

| TC-TXN-004 | Get Transactions by Type |
|------------|-------------------------|
| Objective | Verify filtering by transaction type (DR/CR) |
| Preconditions | Transactions of various types exist |
| Steps | 1. GET /api/v1/transactions/by-type?typeCode=DR |
| Expected | 200 OK with debit transactions only |
| COBOL Mapping | COTRN01C type filter |

| TC-TXN-005 | Create Transaction |
|------------|-------------------|
| Objective | Verify new transaction creation |
| Preconditions | Valid card exists |
| Steps | 1. POST /api/v1/transactions with transaction data |
| Expected | 201 Created with transaction ID |
| COBOL Mapping | COTRN01C transaction add |

### 4.6 Unit Test Cases - Payment Service

| TC-PAY-001 | Get All Payments |
|------------|-----------------|
| Objective | Verify paginated payment list retrieval |
| Preconditions | Payments exist in database |
| Steps | 1. GET /api/v1/payments?page=0&size=10 |
| Expected | 200 OK with PagedResponse containing payments |
| COBOL Mapping | COBIL00C payment list |

| TC-PAY-002 | Get Pending Payments |
|------------|---------------------|
| Objective | Verify retrieval of pending payments |
| Preconditions | Payments with PENDING status exist |
| Steps | 1. GET /api/v1/payments/pending |
| Expected | 200 OK with pending payments only |
| COBOL Mapping | COBIL00C pending filter |

| TC-PAY-003 | Get Scheduled Payments |
|------------|----------------------|
| Objective | Verify retrieval of scheduled future payments |
| Preconditions | Payments with future scheduled dates exist |
| Steps | 1. GET /api/v1/payments/scheduled |
| Expected | 200 OK with scheduled payments |
| COBOL Mapping | COBIL00C scheduled payments |

| TC-PAY-004 | Create Payment |
|------------|----------------|
| Objective | Verify new payment creation |
| Preconditions | Valid account exists |
| Steps | 1. POST /api/v1/payments with payment data |
| Expected | 201 Created with payment ID |
| COBOL Mapping | COBIL00C payment creation |

| TC-PAY-005 | Process Payment |
|------------|-----------------|
| Objective | Verify payment processing |
| Preconditions | Pending payment exists |
| Steps | 1. POST /api/v1/payments/{id}/process |
| Expected | 200 OK with status = COMPLETED and confirmation number |
| COBOL Mapping | COBIL00C payment processing |

| TC-PAY-006 | Cancel Payment |
|------------|----------------|
| Objective | Verify payment cancellation |
| Preconditions | Pending or scheduled payment exists |
| Steps | 1. POST /api/v1/payments/{id}/cancel |
| Expected | 200 OK with status = CANCELLED |
| COBOL Mapping | COBIL00C payment cancellation |

### 4.7 Unit Test Cases - Reporting Service

| TC-RPT-001 | Get Dashboard Summary |
|------------|----------------------|
| Objective | Verify dashboard metrics retrieval |
| Preconditions | Data exists across all entities |
| Steps | 1. GET /api/v1/reports/dashboard |
| Expected | 200 OK with DashboardSummary containing counts and totals |
| COBOL Mapping | CORPT00C summary report |

| TC-RPT-002 | Get Account Statement |
|------------|----------------------|
| Objective | Verify account statement generation |
| Preconditions | Account with transactions exists |
| Steps | 1. GET /api/v1/reports/account/{id}/statement?start=2026-01-01&end=2026-01-31 |
| Expected | 200 OK with AccountStatement including transactions |
| COBOL Mapping | CORPT00C account statement |

| TC-RPT-003 | Get Transaction Report |
|------------|----------------------|
| Objective | Verify transaction analytics report |
| Preconditions | Transactions exist in date range |
| Steps | 1. GET /api/v1/reports/transactions?start=2026-01-01&end=2026-01-31 |
| Expected | 200 OK with transaction counts and amounts by type |
| COBOL Mapping | CORPT00C transaction report |

## 5. Frontend Testing

### 5.1 Component Test Cases

| TC-FE-001 | Login Form Validation |
|-----------|----------------------|
| Objective | Verify login form validates required fields |
| Steps | 1. Leave username empty, 2. Click Sign In |
| Expected | Form shows validation error, no API call made |

| TC-FE-002 | Login Success |
|-----------|---------------|
| Objective | Verify successful login redirects to dashboard |
| Steps | 1. Enter valid credentials, 2. Click Sign In |
| Expected | User redirected to /dashboard, tokens stored |

| TC-FE-003 | Login Failure |
|-----------|---------------|
| Objective | Verify error message on invalid credentials |
| Steps | 1. Enter invalid credentials, 2. Click Sign In |
| Expected | Error alert displayed, user stays on login page |

| TC-FE-004 | Protected Route Redirect |
|-----------|-------------------------|
| Objective | Verify unauthenticated users redirected to login |
| Steps | 1. Clear auth tokens, 2. Navigate to /dashboard |
| Expected | User redirected to /login |

| TC-FE-005 | Logout |
|-----------|--------|
| Objective | Verify logout clears session |
| Steps | 1. Click Logout button |
| Expected | Tokens cleared, user redirected to /login |

### 5.2 Customer Management Test Cases

| TC-FE-CUST-001 | Customer List Display |
|----------------|----------------------|
| Objective | Verify customer list loads and displays |
| Steps | 1. Navigate to /customers |
| Expected | Customer table displays with pagination |

| TC-FE-CUST-002 | Customer Search |
|----------------|-----------------|
| Objective | Verify customer search functionality |
| Steps | 1. Enter search term, 2. Click Search |
| Expected | Table updates with matching customers |

| TC-FE-CUST-003 | Add Customer Modal |
|----------------|-------------------|
| Objective | Verify add customer form opens and submits |
| Steps | 1. Click Add Customer, 2. Fill form, 3. Click Save |
| Expected | Modal closes, new customer appears in list |

| TC-FE-CUST-004 | Edit Customer |
|----------------|---------------|
| Objective | Verify customer edit functionality |
| Steps | 1. Click Edit on customer row, 2. Modify data, 3. Save |
| Expected | Customer data updated in list |

| TC-FE-CUST-005 | Delete Customer |
|----------------|-----------------|
| Objective | Verify customer deletion with confirmation |
| Steps | 1. Click Delete, 2. Confirm deletion |
| Expected | Customer removed from list |

### 5.3 Account Management Test Cases

| TC-FE-ACCT-001 | Account List Display |
|----------------|---------------------|
| Objective | Verify account list loads with status indicators |
| Steps | 1. Navigate to /accounts |
| Expected | Account table displays with active/inactive badges |

| TC-FE-ACCT-002 | Filter Active Accounts |
|----------------|----------------------|
| Objective | Verify active account filter |
| Steps | 1. Click "Active Only" filter |
| Expected | Only active accounts displayed |

| TC-FE-ACCT-003 | Filter Over-Limit Accounts |
|----------------|---------------------------|
| Objective | Verify over-limit account filter |
| Steps | 1. Click "Over Limit" filter |
| Expected | Only over-limit accounts displayed |

| TC-FE-ACCT-004 | Activate Account |
|----------------|------------------|
| Objective | Verify account activation |
| Steps | 1. Click Activate on inactive account |
| Expected | Account status changes to Active |

| TC-FE-ACCT-005 | Deactivate Account |
|----------------|-------------------|
| Objective | Verify account deactivation |
| Steps | 1. Click Deactivate on active account |
| Expected | Account status changes to Inactive |

### 5.4 Card Management Test Cases

| TC-FE-CARD-001 | Card List Display |
|----------------|------------------|
| Objective | Verify card list with masked numbers |
| Steps | 1. Navigate to /cards |
| Expected | Cards display with **** **** **** XXXX format |

| TC-FE-CARD-002 | Search by Last Four |
|----------------|---------------------|
| Objective | Verify card search by last 4 digits |
| Steps | 1. Enter 4 digits in search, 2. View results |
| Expected | Matching cards displayed |

| TC-FE-CARD-003 | View Expiring Cards |
|----------------|---------------------|
| Objective | Verify expiring soon filter |
| Steps | 1. Click "Expiring Soon" filter |
| Expected | Cards expiring within 30 days displayed |

| TC-FE-CARD-004 | Issue New Card |
|----------------|----------------|
| Objective | Verify new card issuance |
| Steps | 1. Click Issue New Card, 2. Fill form, 3. Submit |
| Expected | New card appears in list |

### 5.5 Transaction Test Cases

| TC-FE-TXN-001 | Transaction List Display |
|---------------|-------------------------|
| Objective | Verify transaction history display |
| Steps | 1. Navigate to /transactions |
| Expected | Transactions display with type badges and amounts |

| TC-FE-TXN-002 | Filter by Card Number |
|---------------|----------------------|
| Objective | Verify card number filter |
| Steps | 1. Enter card number in filter |
| Expected | Only transactions for that card displayed |

| TC-FE-TXN-003 | Filter by Date Range |
|---------------|---------------------|
| Objective | Verify date range filter |
| Steps | 1. Select start and end dates |
| Expected | Only transactions in range displayed |

| TC-FE-TXN-004 | View Transaction Details |
|---------------|-------------------------|
| Objective | Verify transaction detail modal |
| Steps | 1. Click Details on transaction |
| Expected | Modal shows full transaction information |

### 5.6 Payment Test Cases

| TC-FE-PAY-001 | Payment List Display |
|---------------|---------------------|
| Objective | Verify payment history display |
| Steps | 1. Navigate to /payments |
| Expected | Payments display with status badges |

| TC-FE-PAY-002 | Make ACH Payment |
|---------------|------------------|
| Objective | Verify ACH payment creation |
| Steps | 1. Click Make Payment, 2. Select ACH, 3. Fill details, 4. Submit |
| Expected | Payment created with PENDING status |

| TC-FE-PAY-003 | Schedule Future Payment |
|---------------|------------------------|
| Objective | Verify scheduled payment creation |
| Steps | 1. Create payment with future date |
| Expected | Payment created with SCHEDULED status |

| TC-FE-PAY-004 | Process Payment |
|---------------|-----------------|
| Objective | Verify payment processing |
| Steps | 1. Click Process on pending payment |
| Expected | Status changes to COMPLETED |

| TC-FE-PAY-005 | Cancel Payment |
|---------------|----------------|
| Objective | Verify payment cancellation |
| Steps | 1. Click Cancel on pending payment |
| Expected | Status changes to CANCELLED |

### 5.7 Reports Test Cases

| TC-FE-RPT-001 | Dashboard Display |
|---------------|------------------|
| Objective | Verify dashboard metrics load |
| Steps | 1. Navigate to /dashboard |
| Expected | All metric cards display with values |

| TC-FE-RPT-002 | Generate Account Statement |
|---------------|---------------------------|
| Objective | Verify account statement generation |
| Steps | 1. Go to Reports, 2. Enter account ID and dates, 3. Generate |
| Expected | Statement displays with transactions |

| TC-FE-RPT-003 | Generate Transaction Report |
|---------------|----------------------------|
| Objective | Verify transaction report generation |
| Steps | 1. Go to Reports, 2. Select Transaction Report tab, 3. Generate |
| Expected | Report displays with analytics |

## 6. Integration Testing

### 6.1 API Gateway Integration

| TC-INT-001 | Request Routing |
|------------|-----------------|
| Objective | Verify API Gateway routes requests to correct services |
| Steps | 1. Send requests to various endpoints through gateway |
| Expected | Requests reach appropriate backend services |

| TC-INT-002 | JWT Validation |
|------------|----------------|
| Objective | Verify gateway validates JWT tokens |
| Steps | 1. Send request without token, 2. Send with invalid token, 3. Send with valid token |
| Expected | 401 for missing/invalid, 200 for valid |

| TC-INT-003 | Rate Limiting |
|------------|---------------|
| Objective | Verify rate limiting protects services |
| Steps | 1. Send rapid requests exceeding limit |
| Expected | 429 Too Many Requests after threshold |

### 6.2 Database Integration

| TC-INT-DB-001 | Transaction Rollback |
|---------------|---------------------|
| Objective | Verify database rollback on error |
| Steps | 1. Trigger operation that fails mid-transaction |
| Expected | All changes rolled back, data consistent |

| TC-INT-DB-002 | Concurrent Updates |
|---------------|-------------------|
| Objective | Verify optimistic locking prevents conflicts |
| Steps | 1. Simultaneously update same record |
| Expected | One succeeds, one fails with conflict error |

### 6.3 Service Communication

| TC-INT-SVC-001 | Cross-Service Data Retrieval |
|----------------|------------------------------|
| Objective | Verify reporting service aggregates from multiple services |
| Steps | 1. Request dashboard summary |
| Expected | Data aggregated from customer, account, card, transaction services |

## 7. End-to-End Testing

### 7.1 Complete Business Workflows

| TC-E2E-001 | New Customer Onboarding |
|------------|------------------------|
| Objective | Verify complete customer onboarding flow |
| Steps | 1. Create customer, 2. Create account, 3. Issue card, 4. Make transaction |
| Expected | All entities created and linked correctly |

| TC-E2E-002 | Bill Payment Workflow |
|------------|---------------------|
| Objective | Verify complete payment processing |
| Steps | 1. View account balance, 2. Create payment, 3. Process payment, 4. Verify balance updated |
| Expected | Account balance reduced by payment amount |

| TC-E2E-003 | Account Statement Generation |
|------------|----------------------------|
| Objective | Verify statement includes all transactions |
| Steps | 1. Create multiple transactions, 2. Generate statement |
| Expected | Statement shows all transactions with correct totals |

| TC-E2E-004 | Card Lifecycle |
|------------|----------------|
| Objective | Verify card from issuance to deactivation |
| Steps | 1. Issue card, 2. Activate, 3. Make transactions, 4. Deactivate |
| Expected | Card status changes correctly, transactions blocked when inactive |

## 8. Performance Testing

### 8.1 Load Testing Scenarios

| Scenario | Description | Target |
|----------|-------------|--------|
| Normal Load | 100 concurrent users | Response time < 500ms |
| Peak Load | 500 concurrent users | Response time < 2s |
| Stress Test | 1000 concurrent users | Graceful degradation |
| Endurance | 100 users for 8 hours | No memory leaks |

### 8.2 Performance Benchmarks

| Operation | Target Response Time | Throughput |
|-----------|---------------------|------------|
| Login | < 300ms | 100 req/s |
| Customer List | < 500ms | 50 req/s |
| Transaction Create | < 200ms | 200 req/s |
| Payment Process | < 1s | 50 req/s |
| Report Generation | < 3s | 10 req/s |

## 9. Security Testing

### 9.1 Authentication Security

| TC-SEC-001 | Password Brute Force Protection |
|------------|--------------------------------|
| Objective | Verify account lockout after failed attempts |
| Steps | 1. Attempt login with wrong password 5 times |
| Expected | Account locked, lockout message displayed |

| TC-SEC-002 | JWT Token Expiration |
|------------|---------------------|
| Objective | Verify expired tokens are rejected |
| Steps | 1. Wait for token expiration, 2. Make API request |
| Expected | 401 Unauthorized, refresh flow triggered |

| TC-SEC-003 | SQL Injection Prevention |
|------------|-------------------------|
| Objective | Verify SQL injection attempts are blocked |
| Steps | 1. Submit malicious SQL in search fields |
| Expected | Input sanitized, no SQL execution |

| TC-SEC-004 | XSS Prevention |
|------------|----------------|
| Objective | Verify script injection is prevented |
| Steps | 1. Submit script tags in form fields |
| Expected | Input escaped, no script execution |

### 9.2 Authorization Security

| TC-SEC-005 | Role-Based Access |
|------------|------------------|
| Objective | Verify users can only access authorized resources |
| Steps | 1. Attempt to access admin endpoints as regular user |
| Expected | 403 Forbidden |

| TC-SEC-006 | Data Isolation |
|------------|----------------|
| Objective | Verify users cannot access other users' data |
| Steps | 1. Attempt to view another user's account details |
| Expected | 403 Forbidden or 404 Not Found |

## 10. Regression Testing

### 10.1 COBOL Functionality Mapping

Each original COBOL program must have equivalent functionality validated:

| COBOL Program | Migrated Component | Test Cases |
|---------------|-------------------|------------|
| COSGN00C | Auth Service + Login.tsx | TC-AUTH-001 to TC-AUTH-004, TC-FE-001 to TC-FE-005 |
| COCRDLIC | Customer Service + Customers.tsx | TC-CUST-001 to TC-CUST-003, TC-FE-CUST-001 to TC-FE-CUST-002 |
| COCRDUPC | Customer Service + Customers.tsx | TC-CUST-004 to TC-CUST-006, TC-FE-CUST-003 to TC-FE-CUST-005 |
| COACTVWC | Account Service + Accounts.tsx | TC-ACCT-001 to TC-ACCT-004, TC-FE-ACCT-001 to TC-FE-ACCT-003 |
| COACTUPC | Account Service + Accounts.tsx | TC-ACCT-005 to TC-ACCT-006, TC-FE-ACCT-004 to TC-FE-ACCT-005 |
| COCRDSLC | Card Service + Cards.tsx | TC-CARD-001 to TC-CARD-004, TC-FE-CARD-001 to TC-FE-CARD-003 |
| COCRDUPC | Card Service + Cards.tsx | TC-CARD-005 to TC-CARD-006, TC-FE-CARD-004 |
| COTRN00C | Transaction Service + Transactions.tsx | TC-TXN-001 to TC-TXN-004, TC-FE-TXN-001 to TC-FE-TXN-004 |
| COTRN01C | Transaction Service + Transactions.tsx | TC-TXN-005 |
| COBIL00C | Payment Service + Payments.tsx | TC-PAY-001 to TC-PAY-006, TC-FE-PAY-001 to TC-FE-PAY-005 |
| CORPT00C | Reporting Service + Reports.tsx | TC-RPT-001 to TC-RPT-003, TC-FE-RPT-001 to TC-FE-RPT-003 |

## 11. Test Execution

### 11.1 Test Execution Schedule

| Phase | Duration | Activities |
|-------|----------|------------|
| Unit Testing | Week 1-2 | Execute all unit tests, fix defects |
| Integration Testing | Week 3 | Execute integration tests, API validation |
| System Testing | Week 4 | End-to-end workflows, UI testing |
| Performance Testing | Week 5 | Load, stress, endurance testing |
| Security Testing | Week 5 | Vulnerability scanning, penetration testing |
| UAT | Week 6 | Business stakeholder validation |
| Regression | Week 7 | Full regression before go-live |

### 11.2 Entry and Exit Criteria

**Entry Criteria:**
- Code complete and deployed to test environment
- Test data loaded
- Test environment stable
- Test cases reviewed and approved

**Exit Criteria:**
- All critical and high priority defects resolved
- 95% of test cases passed
- Performance benchmarks met
- Security scan shows no critical vulnerabilities
- UAT sign-off obtained

## 12. Defect Management

### 12.1 Defect Severity Levels

| Severity | Description | Resolution Time |
|----------|-------------|-----------------|
| Critical | System crash, data loss, security breach | 4 hours |
| High | Major feature not working, no workaround | 24 hours |
| Medium | Feature partially working, workaround exists | 3 days |
| Low | Minor issue, cosmetic defect | Next release |

### 12.2 Defect Workflow

1. Tester identifies defect and logs in tracking system
2. Development team triages and assigns severity
3. Developer fixes and marks for verification
4. Tester verifies fix in test environment
5. Defect closed or reopened based on verification

## 13. Test Deliverables

| Deliverable | Description |
|-------------|-------------|
| Test Plan | This document |
| Test Cases | Detailed test case specifications |
| Test Data | Sample data for test execution |
| Test Scripts | Automated test scripts (JUnit, Jest, Cypress) |
| Test Results | Execution results and metrics |
| Defect Report | List of defects found and status |
| Test Summary Report | Final test summary with recommendations |

## 14. Tools and Infrastructure

| Category | Tool | Purpose |
|----------|------|---------|
| Unit Testing (Backend) | JUnit 5, Mockito | Java unit tests |
| Unit Testing (Frontend) | Jest, React Testing Library | React component tests |
| Integration Testing | Spring Boot Test, Testcontainers | API and database tests |
| E2E Testing | Cypress | Browser automation |
| Performance Testing | JMeter, k6 | Load and stress testing |
| Security Testing | OWASP ZAP, SonarQube | Vulnerability scanning |
| Test Management | Jira, TestRail | Test case and defect tracking |
| CI/CD | GitHub Actions | Automated test execution |

## 15. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Test environment instability | Test delays | Dedicated test environment with monitoring |
| Insufficient test data | Incomplete coverage | Generate comprehensive test data from VSAM exports |
| Resource availability | Schedule slip | Cross-train team members |
| Integration issues | Defect discovery late | Early integration testing in sprints |
| Performance bottlenecks | Go-live delay | Performance testing throughout development |

## Appendix A: Test Data Requirements

### Customer Test Data
- 100+ customers with varying FICO scores (300-850)
- Customers in all 50 US states
- Mix of active and inactive customers

### Account Test Data
- 200+ accounts linked to customers
- Accounts with various credit limits ($1,000 - $50,000)
- Mix of active, inactive, and over-limit accounts

### Card Test Data
- 300+ cards linked to accounts
- Cards with various expiration dates (past, current month, future)
- Mix of active and inactive cards

### Transaction Test Data
- 10,000+ transactions across all cards
- Transactions spanning 12 months
- Various transaction types (purchases, payments, refunds)
- Various merchant categories

### Payment Test Data
- 500+ payments in various statuses
- Payments using different methods (ACH, debit, check, cash)
- Scheduled and processed payments

## Appendix B: Environment URLs

| Environment | Frontend URL | API Gateway URL |
|-------------|--------------|-----------------|
| Development | http://localhost:3000 | http://localhost:8080 |
| Test | https://test.carddemo.example.com | https://api-test.carddemo.example.com |
| Staging | https://staging.carddemo.example.com | https://api-staging.carddemo.example.com |
| Production | https://carddemo.example.com | https://api.carddemo.example.com |
