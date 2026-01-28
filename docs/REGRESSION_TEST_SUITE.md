# CardDemo Migration Regression Test Suite

## Document Information

| Item | Details |
|------|---------|
| Project | CardDemo Mainframe to Cloud Migration |
| Version | 1.0 |
| Date | January 2026 |
| Purpose | Regression Test Suite for Migrated Application |

## 1. Overview

This document provides a comprehensive regression test suite for the CardDemo application migrated from COBOL/CICS mainframe to Java Spring Boot microservices (backend) and React TypeScript (frontend). The test suite ensures that all functionality from the original mainframe application has been correctly migrated and continues to work as expected after any code changes.

### 1.1 Regression Testing Objectives

The primary objectives are to verify that all COBOL program functionality has been correctly implemented in the migrated application, ensure no existing functionality is broken by new changes or bug fixes, validate data integrity between the original VSAM files and the new PostgreSQL database, and confirm that the user experience matches the original mainframe application behavior.

### 1.2 COBOL to Migrated Component Mapping

| Original COBOL Program | Backend Service | Frontend Component | Functionality |
|------------------------|-----------------|-------------------|---------------|
| COSGN00C | auth-service | Login.tsx, Register.tsx | User authentication |
| COUSR00C | auth-service | Register.tsx | User management |
| COCRDLIC | customer-service | Customers.tsx | Customer list/search |
| COCRDUPC | customer-service | Customers.tsx | Customer CRUD |
| COACTVWC | account-service | Accounts.tsx | Account view |
| COACTUPC | account-service | Accounts.tsx | Account update |
| COCRDSLC | card-service | Cards.tsx | Card list/search |
| COCRDUPC | card-service | Cards.tsx | Card CRUD |
| COTRN00C | transaction-service | Transactions.tsx | Transaction list |
| COTRN01C | transaction-service | Transactions.tsx | Transaction add |
| COBIL00C | payment-service | Payments.tsx | Bill payment |
| CORPT00C | reporting-service | Reports.tsx, Dashboard.tsx | Reports |
| CBTRN02C | batch-service | N/A (scheduled) | Daily transaction batch |
| CBACT03C | batch-service | N/A (scheduled) | Account interest batch |

## 2. Backend Regression Test Suite

### 2.1 Auth Service Test Cases (COSGN00C, COUSR00C)

#### RTC-AUTH-001: Valid User Login
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COSGN00C lines 150-200 |
| Preconditions | User "testuser" exists with password "Test@123" |
| Test Steps | 1. POST /api/v1/auth/login with {"username": "testuser", "password": "Test@123"} |
| Expected Result | HTTP 200, response contains accessToken, refreshToken, user object with id, username, email, roles |
| Validation | accessToken is valid JWT, refreshToken stored in database, user.username equals "testuser" |

#### RTC-AUTH-002: Invalid Password Login
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COSGN00C lines 210-230 |
| Preconditions | User "testuser" exists |
| Test Steps | 1. POST /api/v1/auth/login with {"username": "testuser", "password": "wrongpassword"} |
| Expected Result | HTTP 401, response contains error message "Invalid credentials" |
| Validation | No tokens generated, failed login attempt logged |

#### RTC-AUTH-003: Non-Existent User Login
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COSGN00C lines 180-195 |
| Preconditions | User "nonexistent" does not exist |
| Test Steps | 1. POST /api/v1/auth/login with {"username": "nonexistent", "password": "anypassword"} |
| Expected Result | HTTP 401, response contains error message "Invalid credentials" |
| Validation | Generic error message (no user enumeration) |

#### RTC-AUTH-004: Token Refresh
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | N/A (session management) |
| Preconditions | Valid refresh token exists |
| Test Steps | 1. POST /api/v1/auth/refresh with {"refreshToken": "valid-refresh-token"} |
| Expected Result | HTTP 200, response contains new accessToken |
| Validation | New accessToken is valid, old accessToken still valid until expiry |

#### RTC-AUTH-005: Expired Token Refresh
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | N/A |
| Preconditions | Refresh token is expired |
| Test Steps | 1. POST /api/v1/auth/refresh with expired token |
| Expected Result | HTTP 401, response contains error "Token expired" |
| Validation | User must re-authenticate |

#### RTC-AUTH-006: User Registration
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COUSR00C lines 100-180 |
| Preconditions | Username "newuser" does not exist |
| Test Steps | 1. POST /api/v1/auth/register with {"username": "newuser", "email": "new@test.com", "password": "New@123", "firstName": "New", "lastName": "User"} |
| Expected Result | HTTP 201, response contains user object |
| Validation | User created in database, password hashed, default role assigned |

#### RTC-AUTH-007: Duplicate Username Registration
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COUSR00C lines 120-130 |
| Preconditions | Username "existinguser" already exists |
| Test Steps | 1. POST /api/v1/auth/register with existing username |
| Expected Result | HTTP 409, response contains error "Username already exists" |
| Validation | No duplicate user created |

#### RTC-AUTH-008: User Logout
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COSGN00C lines 250-270 |
| Preconditions | User is logged in with valid tokens |
| Test Steps | 1. POST /api/v1/auth/logout with Authorization header |
| Expected Result | HTTP 200, response contains success message |
| Validation | Refresh token invalidated, subsequent requests with old token fail |

#### RTC-AUTH-009: Change Password
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COUSR00C lines 200-250 |
| Preconditions | User is authenticated |
| Test Steps | 1. POST /api/v1/auth/change-password with {"currentPassword": "Old@123", "newPassword": "New@456"} |
| Expected Result | HTTP 200, password changed successfully |
| Validation | Old password no longer works, new password works |

#### RTC-AUTH-010: Get Current User
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COSGN00C session info |
| Preconditions | User is authenticated |
| Test Steps | 1. GET /api/v1/auth/me with Authorization header |
| Expected Result | HTTP 200, response contains current user details |
| Validation | User details match authenticated user |

### 2.2 Customer Service Test Cases (COCRDLIC, COCRDUPC)

#### RTC-CUST-001: Get All Customers Paginated
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDLIC lines 100-200 |
| Preconditions | 50+ customers exist in database |
| Test Steps | 1. GET /api/v1/customers?page=0&size=10 |
| Expected Result | HTTP 200, PagedResponse with 10 customers, totalElements=50+, totalPages=5+ |
| Validation | Customers sorted by ID, pagination metadata correct |

#### RTC-CUST-002: Get Customer by ID
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDLIC lines 220-280 |
| Preconditions | Customer with ID 1 exists |
| Test Steps | 1. GET /api/v1/customers/1 |
| Expected Result | HTTP 200, customer object with all fields |
| Validation | All customer fields populated correctly |

#### RTC-CUST-003: Get Non-Existent Customer
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDLIC error handling |
| Preconditions | Customer with ID 99999 does not exist |
| Test Steps | 1. GET /api/v1/customers/99999 |
| Expected Result | HTTP 404, error message "Customer not found" |
| Validation | Appropriate error response |

#### RTC-CUST-004: Search Customers by Name
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDLIC lines 300-350 |
| Preconditions | Customers with last name "Smith" exist |
| Test Steps | 1. GET /api/v1/customers/search?query=Smith |
| Expected Result | HTTP 200, list of customers matching "Smith" |
| Validation | All returned customers have "Smith" in name |

#### RTC-CUST-005: Search Customers by SSN
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDLIC lines 360-400 |
| Preconditions | Customer with SSN "123-45-6789" exists |
| Test Steps | 1. GET /api/v1/customers/search?query=123-45-6789 |
| Expected Result | HTTP 200, customer with matching SSN |
| Validation | SSN matches exactly |

#### RTC-CUST-006: Search Customers by Phone
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COCRDLIC lines 410-450 |
| Preconditions | Customer with phone "555-123-4567" exists |
| Test Steps | 1. GET /api/v1/customers/search?query=555-123-4567 |
| Expected Result | HTTP 200, customer with matching phone |
| Validation | Phone matches exactly |

#### RTC-CUST-007: Get Customers by State
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COCRDLIC state filter |
| Preconditions | Customers in state "CA" exist |
| Test Steps | 1. GET /api/v1/customers/state/CA |
| Expected Result | HTTP 200, list of California customers |
| Validation | All returned customers have state "CA" |

#### RTC-CUST-008: Create New Customer
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDUPC lines 100-200 |
| Preconditions | Valid customer data prepared |
| Test Steps | 1. POST /api/v1/customers with customer JSON |
| Expected Result | HTTP 201, created customer with generated ID |
| Validation | Customer persisted in database, all fields saved correctly |

```json
{
  "firstName": "John",
  "middleName": "Q",
  "lastName": "Public",
  "addressLine1": "123 Main St",
  "addressLine2": "Apt 4B",
  "city": "Anytown",
  "state": "CA",
  "zipCode": "90210",
  "country": "USA",
  "phone": "555-123-4567",
  "ssn": "987-65-4321",
  "ficoScore": 750,
  "dateOfBirth": "1985-06-15"
}
```

#### RTC-CUST-009: Create Customer with Invalid Data
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDUPC validation |
| Preconditions | Invalid customer data (missing required fields) |
| Test Steps | 1. POST /api/v1/customers with {"firstName": ""} |
| Expected Result | HTTP 400, validation error messages |
| Validation | Specific field errors returned |

#### RTC-CUST-010: Create Customer with Duplicate SSN
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDUPC lines 150-160 |
| Preconditions | Customer with SSN "123-45-6789" exists |
| Test Steps | 1. POST /api/v1/customers with duplicate SSN |
| Expected Result | HTTP 409, error "SSN already exists" |
| Validation | No duplicate customer created |

#### RTC-CUST-011: Update Customer
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDUPC lines 250-350 |
| Preconditions | Customer with ID 1 exists |
| Test Steps | 1. PUT /api/v1/customers/1 with updated data |
| Expected Result | HTTP 200, updated customer object |
| Validation | Changes persisted, unchanged fields preserved |

#### RTC-CUST-012: Update Non-Existent Customer
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COCRDUPC error handling |
| Preconditions | Customer with ID 99999 does not exist |
| Test Steps | 1. PUT /api/v1/customers/99999 with data |
| Expected Result | HTTP 404, error "Customer not found" |
| Validation | No data modified |

#### RTC-CUST-013: Delete Customer
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDUPC lines 400-450 |
| Preconditions | Customer with ID 100 exists, no active accounts |
| Test Steps | 1. DELETE /api/v1/customers/100 |
| Expected Result | HTTP 204, no content |
| Validation | Customer removed from database |

#### RTC-CUST-014: Delete Customer with Active Accounts
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDUPC lines 410-420 |
| Preconditions | Customer has active accounts |
| Test Steps | 1. DELETE /api/v1/customers/{id} |
| Expected Result | HTTP 409, error "Cannot delete customer with active accounts" |
| Validation | Customer not deleted, accounts preserved |

#### RTC-CUST-015: Partial Customer Search
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COCRDLIC wildcard search |
| Preconditions | Customers with names starting with "Jo" exist |
| Test Steps | 1. GET /api/v1/customers/search?query=Jo |
| Expected Result | HTTP 200, customers matching partial name |
| Validation | Returns John, Joseph, Joanna, etc. |

### 2.3 Account Service Test Cases (COACTVWC, COACTUPC)

#### RTC-ACCT-001: Get All Accounts Paginated
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COACTVWC lines 100-180 |
| Preconditions | 100+ accounts exist |
| Test Steps | 1. GET /api/v1/accounts?page=0&size=20 |
| Expected Result | HTTP 200, PagedResponse with 20 accounts |
| Validation | Pagination metadata correct |

#### RTC-ACCT-002: Get Account by ID
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COACTVWC lines 200-250 |
| Preconditions | Account with ID 1 exists |
| Test Steps | 1. GET /api/v1/accounts/1 |
| Expected Result | HTTP 200, account object with all fields |
| Validation | All account fields populated |

#### RTC-ACCT-003: Get Accounts by Customer ID
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COACTVWC lines 260-300 |
| Preconditions | Customer 1 has 3 accounts |
| Test Steps | 1. GET /api/v1/accounts/customer/1 |
| Expected Result | HTTP 200, list of 3 accounts |
| Validation | All accounts belong to customer 1 |

#### RTC-ACCT-004: Get Active Accounts Only
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COACTVWC active filter |
| Preconditions | Mix of active and inactive accounts |
| Test Steps | 1. GET /api/v1/accounts/active |
| Expected Result | HTTP 200, only active accounts |
| Validation | All returned accounts have activeStatus='Y' |

#### RTC-ACCT-005: Get Over-Limit Accounts
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COACTVWC over-limit report |
| Preconditions | Accounts with balance > creditLimit exist |
| Test Steps | 1. GET /api/v1/accounts/over-limit |
| Expected Result | HTTP 200, over-limit accounts |
| Validation | All returned accounts have currentBalance > creditLimit |

#### RTC-ACCT-006: Get Account Summary
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COACTVWC summary |
| Preconditions | Accounts exist |
| Test Steps | 1. GET /api/v1/accounts/summary |
| Expected Result | HTTP 200, summary with totalAccounts, totalBalance, totalCreditLimit |
| Validation | Aggregated values correct |

#### RTC-ACCT-007: Create New Account
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COACTUPC lines 100-180 |
| Preconditions | Valid customer exists |
| Test Steps | 1. POST /api/v1/accounts with account JSON |
| Expected Result | HTTP 201, created account with generated ID |
| Validation | Account persisted, linked to customer |

```json
{
  "customerId": 1,
  "creditLimit": 5000.00,
  "cashCreditLimit": 1000.00,
  "openDate": "2026-01-15",
  "expirationDate": "2029-01-15",
  "reissueDate": "2028-01-15",
  "groupId": "GRP001"
}
```

#### RTC-ACCT-008: Create Account for Non-Existent Customer
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COACTUPC validation |
| Preconditions | Customer 99999 does not exist |
| Test Steps | 1. POST /api/v1/accounts with customerId=99999 |
| Expected Result | HTTP 404, error "Customer not found" |
| Validation | No account created |

#### RTC-ACCT-009: Update Account
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COACTUPC lines 200-280 |
| Preconditions | Account with ID 1 exists |
| Test Steps | 1. PUT /api/v1/accounts/1 with updated creditLimit |
| Expected Result | HTTP 200, updated account |
| Validation | Credit limit changed, other fields preserved |

#### RTC-ACCT-010: Activate Account
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COACTUPC lines 300-330 |
| Preconditions | Inactive account exists |
| Test Steps | 1. POST /api/v1/accounts/1/activate |
| Expected Result | HTTP 200, account with activeStatus='Y' |
| Validation | Status changed to active |

#### RTC-ACCT-011: Deactivate Account
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COACTUPC lines 340-370 |
| Preconditions | Active account exists |
| Test Steps | 1. POST /api/v1/accounts/1/deactivate |
| Expected Result | HTTP 200, account with activeStatus='N' |
| Validation | Status changed to inactive |

#### RTC-ACCT-012: Delete Account
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COACTUPC lines 400-450 |
| Preconditions | Account with no transactions exists |
| Test Steps | 1. DELETE /api/v1/accounts/100 |
| Expected Result | HTTP 204, no content |
| Validation | Account removed from database |

#### RTC-ACCT-013: Delete Account with Transactions
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COACTUPC constraint check |
| Preconditions | Account has transaction history |
| Test Steps | 1. DELETE /api/v1/accounts/{id} |
| Expected Result | HTTP 409, error "Cannot delete account with transactions" |
| Validation | Account preserved |

#### RTC-ACCT-014: Credit Limit Validation
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COACTUPC validation |
| Preconditions | None |
| Test Steps | 1. POST /api/v1/accounts with creditLimit=-1000 |
| Expected Result | HTTP 400, validation error |
| Validation | Negative credit limit rejected |

#### RTC-ACCT-015: Account Balance Calculation
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COACTVWC balance display |
| Preconditions | Account with transactions exists |
| Test Steps | 1. GET /api/v1/accounts/1 |
| Expected Result | HTTP 200, currentBalance reflects all transactions |
| Validation | Balance = sum of debits - sum of credits |

### 2.4 Card Service Test Cases (COCRDSLC, COCRDUPC)

#### RTC-CARD-001: Get All Cards Paginated
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDSLC lines 100-180 |
| Preconditions | 200+ cards exist |
| Test Steps | 1. GET /api/v1/cards?page=0&size=25 |
| Expected Result | HTTP 200, PagedResponse with 25 cards |
| Validation | Pagination correct |

#### RTC-CARD-002: Get Card by Card Number
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDSLC lines 200-250 |
| Preconditions | Card "4111111111111111" exists |
| Test Steps | 1. GET /api/v1/cards/4111111111111111 |
| Expected Result | HTTP 200, card object |
| Validation | All card fields populated |

#### RTC-CARD-003: Get Cards by Account ID
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDSLC lines 260-300 |
| Preconditions | Account 1 has 2 cards |
| Test Steps | 1. GET /api/v1/cards/account/1 |
| Expected Result | HTTP 200, list of 2 cards |
| Validation | All cards linked to account 1 |

#### RTC-CARD-004: Get Cards by Customer ID
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDSLC customer cards |
| Preconditions | Customer 1 has cards across multiple accounts |
| Test Steps | 1. GET /api/v1/cards/customer/1 |
| Expected Result | HTTP 200, all customer's cards |
| Validation | Cards from all customer's accounts returned |

#### RTC-CARD-005: Get Active Cards Only
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDSLC active filter |
| Preconditions | Mix of active and inactive cards |
| Test Steps | 1. GET /api/v1/cards/active |
| Expected Result | HTTP 200, only active cards |
| Validation | All returned cards have activeStatus='Y' |

#### RTC-CARD-006: Get Cards Expiring Soon (30 days)
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDSLC expiration report |
| Preconditions | Cards with various expiration dates |
| Test Steps | 1. GET /api/v1/cards/expiring-soon?days=30 |
| Expected Result | HTTP 200, cards expiring within 30 days |
| Validation | All returned cards expire within 30 days |

#### RTC-CARD-007: Get Cards Expiring Soon (90 days)
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COCRDSLC expiration report |
| Preconditions | Cards with various expiration dates |
| Test Steps | 1. GET /api/v1/cards/expiring-soon?days=90 |
| Expected Result | HTTP 200, cards expiring within 90 days |
| Validation | All returned cards expire within 90 days |

#### RTC-CARD-008: Search Cards by Last Four Digits
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDSLC search |
| Preconditions | Cards ending in "1234" exist |
| Test Steps | 1. GET /api/v1/cards/search?lastFour=1234 |
| Expected Result | HTTP 200, matching cards |
| Validation | All returned cards end in "1234" |

#### RTC-CARD-009: Issue New Card
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDUPC lines 100-180 |
| Preconditions | Valid account exists |
| Test Steps | 1. POST /api/v1/cards with card JSON |
| Expected Result | HTTP 201, card with generated number |
| Validation | Card persisted, linked to account |

```json
{
  "accountId": 1,
  "customerId": 1,
  "cardholderName": "JOHN Q PUBLIC",
  "expirationDate": "2029-01-31",
  "cvv": "123"
}
```

#### RTC-CARD-010: Issue Card for Non-Existent Account
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDUPC validation |
| Preconditions | Account 99999 does not exist |
| Test Steps | 1. POST /api/v1/cards with accountId=99999 |
| Expected Result | HTTP 404, error "Account not found" |
| Validation | No card created |

#### RTC-CARD-011: Update Card
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDUPC lines 200-280 |
| Preconditions | Card exists |
| Test Steps | 1. PUT /api/v1/cards/{cardNumber} with updated data |
| Expected Result | HTTP 200, updated card |
| Validation | Changes persisted |

#### RTC-CARD-012: Activate Card
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDUPC lines 300-330 |
| Preconditions | Inactive card exists |
| Test Steps | 1. POST /api/v1/cards/{cardNumber}/activate |
| Expected Result | HTTP 200, card with activeStatus='Y' |
| Validation | Card can now be used for transactions |

#### RTC-CARD-013: Deactivate Card
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDUPC lines 340-370 |
| Preconditions | Active card exists |
| Test Steps | 1. POST /api/v1/cards/{cardNumber}/deactivate |
| Expected Result | HTTP 200, card with activeStatus='N' |
| Validation | Card blocked from transactions |

#### RTC-CARD-014: Delete Card
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COCRDUPC lines 400-450 |
| Preconditions | Card with no transactions exists |
| Test Steps | 1. DELETE /api/v1/cards/{cardNumber} |
| Expected Result | HTTP 204, no content |
| Validation | Card removed from database |

#### RTC-CARD-015: Card Number Generation
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDUPC card generation |
| Preconditions | None |
| Test Steps | 1. POST /api/v1/cards multiple times |
| Expected Result | Each card has unique 16-digit number |
| Validation | Numbers pass Luhn algorithm check |

### 2.5 Transaction Service Test Cases (COTRN00C, COTRN01C)

#### RTC-TXN-001: Get All Transactions Paginated
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COTRN00C lines 100-180 |
| Preconditions | 1000+ transactions exist |
| Test Steps | 1. GET /api/v1/transactions?page=0&size=50 |
| Expected Result | HTTP 200, PagedResponse with 50 transactions |
| Validation | Sorted by date descending |

#### RTC-TXN-002: Get Transaction by ID
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COTRN00C lines 200-250 |
| Preconditions | Transaction with ID 1 exists |
| Test Steps | 1. GET /api/v1/transactions/1 |
| Expected Result | HTTP 200, transaction object |
| Validation | All fields populated |

#### RTC-TXN-003: Get Transactions by Card Number
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COTRN00C lines 260-320 |
| Preconditions | Card has 50 transactions |
| Test Steps | 1. GET /api/v1/transactions/card/4111111111111111 |
| Expected Result | HTTP 200, list of 50 transactions |
| Validation | All transactions for specified card |

#### RTC-TXN-004: Get Transactions by Date Range
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COTRN00C date filter |
| Preconditions | Transactions in January 2026 exist |
| Test Steps | 1. GET /api/v1/transactions/date-range?start=2026-01-01&end=2026-01-31 |
| Expected Result | HTTP 200, transactions in range |
| Validation | All transactions within date range |

#### RTC-TXN-005: Get Transactions by Type (Debit)
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COTRN00C type filter |
| Preconditions | Debit transactions exist |
| Test Steps | 1. GET /api/v1/transactions/by-type?typeCode=DR |
| Expected Result | HTTP 200, only debit transactions |
| Validation | All transactions have typeCode='DR' |

#### RTC-TXN-006: Get Transactions by Type (Credit)
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COTRN00C type filter |
| Preconditions | Credit transactions exist |
| Test Steps | 1. GET /api/v1/transactions/by-type?typeCode=CR |
| Expected Result | HTTP 200, only credit transactions |
| Validation | All transactions have typeCode='CR' |

#### RTC-TXN-007: Get Transactions by Merchant
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COTRN00C merchant filter |
| Preconditions | Transactions at "AMAZON" exist |
| Test Steps | 1. GET /api/v1/transactions/by-merchant?merchant=AMAZON |
| Expected Result | HTTP 200, Amazon transactions |
| Validation | All transactions have merchant containing "AMAZON" |

#### RTC-TXN-008: Get Transaction Summary
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COTRN00C summary |
| Preconditions | Transactions exist |
| Test Steps | 1. GET /api/v1/transactions/summary |
| Expected Result | HTTP 200, summary with totalTransactions, totalDebits, totalCredits |
| Validation | Aggregated values correct |

#### RTC-TXN-009: Create Purchase Transaction
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COTRN01C lines 100-200 |
| Preconditions | Active card exists |
| Test Steps | 1. POST /api/v1/transactions with purchase data |
| Expected Result | HTTP 201, transaction created |
| Validation | Account balance updated |

```json
{
  "cardNumber": "4111111111111111",
  "typeCode": "DR",
  "categoryCode": "PURCHASE",
  "transactionSource": "POS",
  "transactionDescription": "WALMART STORE #1234",
  "amount": 125.50,
  "merchantId": "WALMART001",
  "merchantName": "WALMART",
  "merchantCity": "ANYTOWN",
  "merchantZip": "90210"
}
```

#### RTC-TXN-010: Create Payment Transaction
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COTRN01C payment |
| Preconditions | Account has balance |
| Test Steps | 1. POST /api/v1/transactions with payment data |
| Expected Result | HTTP 201, credit transaction created |
| Validation | Account balance reduced |

#### RTC-TXN-011: Create Transaction on Inactive Card
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COTRN01C validation |
| Preconditions | Card is inactive |
| Test Steps | 1. POST /api/v1/transactions with inactive card |
| Expected Result | HTTP 400, error "Card is not active" |
| Validation | Transaction rejected |

#### RTC-TXN-012: Create Transaction Exceeding Credit Limit
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COTRN01C limit check |
| Preconditions | Account near credit limit |
| Test Steps | 1. POST /api/v1/transactions with amount exceeding available credit |
| Expected Result | HTTP 400, error "Transaction exceeds credit limit" |
| Validation | Transaction rejected |

#### RTC-TXN-013: Transaction Amount Validation
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COTRN01C validation |
| Preconditions | None |
| Test Steps | 1. POST /api/v1/transactions with amount=0 |
| Expected Result | HTTP 400, validation error |
| Validation | Zero/negative amounts rejected |

#### RTC-TXN-014: Transaction Date Validation
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COTRN01C date handling |
| Preconditions | None |
| Test Steps | 1. POST /api/v1/transactions with future date |
| Expected Result | HTTP 400, error "Future date not allowed" |
| Validation | Future transactions rejected |

#### RTC-TXN-015: Transaction Audit Trail
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COTRN01C audit |
| Preconditions | Transaction created |
| Test Steps | 1. Verify transaction has timestamp and source |
| Expected Result | Transaction has createdAt, transactionSource |
| Validation | Audit fields populated |

### 2.6 Payment Service Test Cases (COBIL00C)

#### RTC-PAY-001: Get All Payments Paginated
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COBIL00C lines 100-180 |
| Preconditions | 100+ payments exist |
| Test Steps | 1. GET /api/v1/payments?page=0&size=20 |
| Expected Result | HTTP 200, PagedResponse with 20 payments |
| Validation | Pagination correct |

#### RTC-PAY-002: Get Payment by ID
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C lines 200-250 |
| Preconditions | Payment with ID 1 exists |
| Test Steps | 1. GET /api/v1/payments/1 |
| Expected Result | HTTP 200, payment object |
| Validation | All fields populated |

#### RTC-PAY-003: Get Payments by Account ID
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C account payments |
| Preconditions | Account 1 has 10 payments |
| Test Steps | 1. GET /api/v1/payments/account/1 |
| Expected Result | HTTP 200, list of 10 payments |
| Validation | All payments for account 1 |

#### RTC-PAY-004: Get Payments by Status
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C status filter |
| Preconditions | Payments with various statuses exist |
| Test Steps | 1. GET /api/v1/payments/status/COMPLETED |
| Expected Result | HTTP 200, completed payments only |
| Validation | All payments have status='COMPLETED' |

#### RTC-PAY-005: Get Pending Payments
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C pending filter |
| Preconditions | Pending payments exist |
| Test Steps | 1. GET /api/v1/payments/pending |
| Expected Result | HTTP 200, pending payments |
| Validation | All payments have status='PENDING' |

#### RTC-PAY-006: Get Scheduled Payments
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C scheduled filter |
| Preconditions | Scheduled payments exist |
| Test Steps | 1. GET /api/v1/payments/scheduled |
| Expected Result | HTTP 200, scheduled payments |
| Validation | All payments have status='SCHEDULED' and future scheduledDate |

#### RTC-PAY-007: Get Total Payments by Account
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COBIL00C summary |
| Preconditions | Account has completed payments |
| Test Steps | 1. GET /api/v1/payments/account/1/total |
| Expected Result | HTTP 200, total amount paid |
| Validation | Sum of completed payments |

#### RTC-PAY-008: Create ACH Payment
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COBIL00C lines 300-400 |
| Preconditions | Valid account exists |
| Test Steps | 1. POST /api/v1/payments with ACH data |
| Expected Result | HTTP 201, payment with status='PENDING' |
| Validation | Payment created, awaiting processing |

```json
{
  "accountId": 1,
  "amount": 500.00,
  "paymentMethod": "ACH",
  "routingNumber": "021000021",
  "bankAccountNumber": "123456789",
  "scheduledDate": "2026-01-28"
}
```

#### RTC-PAY-009: Create Debit Card Payment
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COBIL00C debit payment |
| Preconditions | Valid account exists |
| Test Steps | 1. POST /api/v1/payments with debit card data |
| Expected Result | HTTP 201, payment created |
| Validation | Payment linked to debit card |

```json
{
  "accountId": 1,
  "amount": 250.00,
  "paymentMethod": "DEBIT",
  "debitCardNumber": "4111111111111111",
  "scheduledDate": "2026-01-28"
}
```

#### RTC-PAY-010: Create Check Payment
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C check payment |
| Preconditions | Valid account exists |
| Test Steps | 1. POST /api/v1/payments with check data |
| Expected Result | HTTP 201, payment created |
| Validation | Check number recorded |

#### RTC-PAY-011: Create Cash Payment
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C cash payment |
| Preconditions | Valid account exists |
| Test Steps | 1. POST /api/v1/payments with cash method |
| Expected Result | HTTP 201, payment created |
| Validation | Cash payment recorded |

#### RTC-PAY-012: Schedule Future Payment
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C scheduled payment |
| Preconditions | Valid account exists |
| Test Steps | 1. POST /api/v1/payments with future scheduledDate |
| Expected Result | HTTP 201, payment with status='SCHEDULED' |
| Validation | Payment not processed until scheduled date |

#### RTC-PAY-013: Process Pending Payment
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COBIL00C lines 500-600 |
| Preconditions | Pending payment exists |
| Test Steps | 1. POST /api/v1/payments/1/process |
| Expected Result | HTTP 200, payment with status='COMPLETED', confirmationNumber assigned |
| Validation | Account balance reduced, transaction created |

#### RTC-PAY-014: Cancel Pending Payment
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C lines 650-700 |
| Preconditions | Pending payment exists |
| Test Steps | 1. POST /api/v1/payments/1/cancel |
| Expected Result | HTTP 200, payment with status='CANCELLED' |
| Validation | Payment not processed |

#### RTC-PAY-015: Cancel Completed Payment
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C validation |
| Preconditions | Completed payment exists |
| Test Steps | 1. POST /api/v1/payments/1/cancel |
| Expected Result | HTTP 400, error "Cannot cancel completed payment" |
| Validation | Completed payments cannot be cancelled |

#### RTC-PAY-016: Payment Amount Validation
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COBIL00C validation |
| Preconditions | None |
| Test Steps | 1. POST /api/v1/payments with amount=0 |
| Expected Result | HTTP 400, validation error |
| Validation | Zero/negative amounts rejected |

#### RTC-PAY-017: Payment Exceeds Balance
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COBIL00C validation |
| Preconditions | Account balance is $1000 |
| Test Steps | 1. POST /api/v1/payments with amount=$5000 |
| Expected Result | HTTP 200, payment created (overpayment allowed) |
| Validation | Overpayments result in credit balance |

### 2.7 Reporting Service Test Cases (CORPT00C)

#### RTC-RPT-001: Get Dashboard Summary
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | CORPT00C lines 100-200 |
| Preconditions | Data exists across all entities |
| Test Steps | 1. GET /api/v1/reports/dashboard |
| Expected Result | HTTP 200, DashboardSummary object |
| Validation | All metrics populated correctly |

Expected response structure:
```json
{
  "totalCustomers": 500,
  "totalAccounts": 750,
  "totalCards": 1200,
  "activeCards": 1000,
  "totalTransactions": 50000,
  "totalTransactionAmount": 2500000.00,
  "pendingPayments": 25,
  "pendingPaymentAmount": 12500.00,
  "accountsOverLimit": 15,
  "cardsExpiringSoon": 45
}
```

#### RTC-RPT-002: Get Account Statement
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | CORPT00C lines 250-400 |
| Preconditions | Account with transactions exists |
| Test Steps | 1. GET /api/v1/reports/account/1/statement?start=2026-01-01&end=2026-01-31 |
| Expected Result | HTTP 200, AccountStatement object |
| Validation | Statement includes all transactions in range |

Expected response structure:
```json
{
  "accountId": 1,
  "customerId": 1,
  "customerName": "John Q Public",
  "statementPeriodStart": "2026-01-01",
  "statementPeriodEnd": "2026-01-31",
  "openingBalance": 1500.00,
  "closingBalance": 2100.00,
  "totalDebits": 800.00,
  "totalCredits": 200.00,
  "minimumPaymentDue": 50.00,
  "paymentDueDate": "2026-02-15",
  "transactions": [...]
}
```

#### RTC-RPT-003: Get Account Statement - No Transactions
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | CORPT00C empty statement |
| Preconditions | Account exists but no transactions in range |
| Test Steps | 1. GET /api/v1/reports/account/1/statement?start=2020-01-01&end=2020-01-31 |
| Expected Result | HTTP 200, statement with empty transactions array |
| Validation | Statement generated with zero activity |

#### RTC-RPT-004: Get Transaction Report
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | CORPT00C lines 450-550 |
| Preconditions | Transactions exist in date range |
| Test Steps | 1. GET /api/v1/reports/transactions?start=2026-01-01&end=2026-01-31 |
| Expected Result | HTTP 200, TransactionReport object |
| Validation | Report includes aggregated data |

Expected response structure:
```json
{
  "reportPeriodStart": "2026-01-01",
  "reportPeriodEnd": "2026-01-31",
  "totalTransactions": 5000,
  "totalDebitAmount": 450000.00,
  "totalCreditAmount": 125000.00,
  "transactionsByType": {
    "PURCHASE": 3500,
    "PAYMENT": 1200,
    "REFUND": 300
  },
  "transactionsByCategory": {
    "RETAIL": 2000,
    "FOOD": 1000,
    "TRAVEL": 500,
    "OTHER": 1500
  }
}
```

#### RTC-RPT-005: Dashboard Metrics Accuracy
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | CORPT00C calculations |
| Preconditions | Known data set |
| Test Steps | 1. Insert known data, 2. GET /api/v1/reports/dashboard, 3. Verify counts |
| Expected Result | All metrics match expected values |
| Validation | Cross-reference with direct database queries |

## 3. Frontend Regression Test Suite

### 3.1 Authentication UI Test Cases

#### RTC-FE-AUTH-001: Login Page Renders
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Login.tsx |
| Test Steps | 1. Navigate to /login |
| Expected Result | Login form displays with username, password fields, and Sign In button |
| Validation | All form elements visible and accessible |

#### RTC-FE-AUTH-002: Login Form Validation
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Login.tsx |
| Test Steps | 1. Leave fields empty, 2. Click Sign In |
| Expected Result | Validation errors displayed |
| Validation | Form not submitted, error messages shown |

#### RTC-FE-AUTH-003: Successful Login
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Login.tsx |
| Test Steps | 1. Enter valid credentials, 2. Click Sign In |
| Expected Result | User redirected to /dashboard |
| Validation | Tokens stored in localStorage, user state updated |

#### RTC-FE-AUTH-004: Failed Login
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Login.tsx |
| Test Steps | 1. Enter invalid credentials, 2. Click Sign In |
| Expected Result | Error alert displayed |
| Validation | User remains on login page |

#### RTC-FE-AUTH-005: Register Page Renders
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Register.tsx |
| Test Steps | 1. Navigate to /register |
| Expected Result | Registration form displays |
| Validation | All required fields present |

#### RTC-FE-AUTH-006: Successful Registration
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Register.tsx |
| Test Steps | 1. Fill all fields, 2. Click Register |
| Expected Result | User created, redirected to login |
| Validation | Success message displayed |

#### RTC-FE-AUTH-007: Protected Route Redirect
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | ProtectedRoute.tsx |
| Test Steps | 1. Clear tokens, 2. Navigate to /dashboard |
| Expected Result | Redirected to /login |
| Validation | Protected content not accessible |

#### RTC-FE-AUTH-008: Logout
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Layout.tsx |
| Test Steps | 1. Click Logout button |
| Expected Result | Tokens cleared, redirected to /login |
| Validation | User state cleared |

### 3.2 Dashboard UI Test Cases

#### RTC-FE-DASH-001: Dashboard Renders
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Dashboard.tsx |
| Test Steps | 1. Navigate to /dashboard |
| Expected Result | Dashboard displays with metric cards |
| Validation | All metric cards visible |

#### RTC-FE-DASH-002: Dashboard Metrics Load
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Dashboard.tsx |
| Test Steps | 1. Navigate to /dashboard, 2. Wait for data load |
| Expected Result | Metrics populated with values |
| Validation | No loading spinners, values displayed |

#### RTC-FE-DASH-003: Dashboard Error Handling
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Dashboard.tsx |
| Test Steps | 1. Simulate API error |
| Expected Result | Error alert displayed |
| Validation | User-friendly error message |

#### RTC-FE-DASH-004: Dashboard Refresh
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| Component | Dashboard.tsx |
| Test Steps | 1. Click refresh button |
| Expected Result | Data reloaded |
| Validation | Updated values displayed |

### 3.3 Customer Management UI Test Cases

#### RTC-FE-CUST-001: Customer List Renders
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Customers.tsx |
| Test Steps | 1. Navigate to /customers |
| Expected Result | Customer table displays |
| Validation | Table headers and data visible |

#### RTC-FE-CUST-002: Customer List Pagination
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Customers.tsx |
| Test Steps | 1. Click page 2 |
| Expected Result | Second page of customers loads |
| Validation | Different customers displayed |

#### RTC-FE-CUST-003: Customer Search
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Customers.tsx |
| Test Steps | 1. Enter search term, 2. Click Search |
| Expected Result | Filtered results displayed |
| Validation | Only matching customers shown |

#### RTC-FE-CUST-004: Add Customer Modal Opens
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Customers.tsx |
| Test Steps | 1. Click Add Customer button |
| Expected Result | Modal opens with empty form |
| Validation | All form fields present |

#### RTC-FE-CUST-005: Add Customer Success
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Customers.tsx |
| Test Steps | 1. Fill form, 2. Click Save |
| Expected Result | Modal closes, customer added to list |
| Validation | New customer visible in table |

#### RTC-FE-CUST-006: Edit Customer
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Customers.tsx |
| Test Steps | 1. Click Edit on row, 2. Modify data, 3. Save |
| Expected Result | Customer updated |
| Validation | Changes reflected in table |

#### RTC-FE-CUST-007: Delete Customer Confirmation
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Customers.tsx |
| Test Steps | 1. Click Delete, 2. Confirm |
| Expected Result | Customer removed from list |
| Validation | Customer no longer in table |

#### RTC-FE-CUST-008: Customer Form Validation
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Customers.tsx |
| Test Steps | 1. Submit empty form |
| Expected Result | Validation errors displayed |
| Validation | Required fields highlighted |

### 3.4 Account Management UI Test Cases

#### RTC-FE-ACCT-001: Account List Renders
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Accounts.tsx |
| Test Steps | 1. Navigate to /accounts |
| Expected Result | Account table displays |
| Validation | Status badges visible |

#### RTC-FE-ACCT-002: Filter Active Accounts
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Accounts.tsx |
| Test Steps | 1. Click Active Only filter |
| Expected Result | Only active accounts shown |
| Validation | All displayed accounts have Active badge |

#### RTC-FE-ACCT-003: Filter Over-Limit Accounts
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Accounts.tsx |
| Test Steps | 1. Click Over Limit filter |
| Expected Result | Only over-limit accounts shown |
| Validation | All displayed accounts exceed credit limit |

#### RTC-FE-ACCT-004: Activate Account
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Accounts.tsx |
| Test Steps | 1. Click Activate on inactive account |
| Expected Result | Account status changes to Active |
| Validation | Badge updates to Active |

#### RTC-FE-ACCT-005: Deactivate Account
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Accounts.tsx |
| Test Steps | 1. Click Deactivate on active account |
| Expected Result | Account status changes to Inactive |
| Validation | Badge updates to Inactive |

#### RTC-FE-ACCT-006: View Account Details
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Accounts.tsx |
| Test Steps | 1. Click View on account row |
| Expected Result | Account details modal opens |
| Validation | All account information displayed |

#### RTC-FE-ACCT-007: Account Balance Display
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Accounts.tsx |
| Test Steps | 1. View account in table |
| Expected Result | Balance formatted as currency |
| Validation | Correct currency format ($X,XXX.XX) |

### 3.5 Card Management UI Test Cases

#### RTC-FE-CARD-001: Card List Renders
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Cards.tsx |
| Test Steps | 1. Navigate to /cards |
| Expected Result | Card table displays |
| Validation | Card numbers masked |

#### RTC-FE-CARD-002: Card Number Masking
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Cards.tsx |
| Test Steps | 1. View card in table |
| Expected Result | Card number shows as **** **** **** XXXX |
| Validation | Only last 4 digits visible |

#### RTC-FE-CARD-003: Search by Last Four
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Cards.tsx |
| Test Steps | 1. Enter 4 digits in search, 2. Search |
| Expected Result | Matching cards displayed |
| Validation | All cards end with searched digits |

#### RTC-FE-CARD-004: View Expiring Cards
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Cards.tsx |
| Test Steps | 1. Click Expiring Soon filter |
| Expected Result | Cards expiring within 30 days shown |
| Validation | Expiration dates within 30 days |

#### RTC-FE-CARD-005: Issue New Card
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Cards.tsx |
| Test Steps | 1. Click Issue New Card, 2. Fill form, 3. Submit |
| Expected Result | New card created |
| Validation | Card appears in list |

#### RTC-FE-CARD-006: Activate Card
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Cards.tsx |
| Test Steps | 1. Click Activate on inactive card |
| Expected Result | Card status changes to Active |
| Validation | Badge updates |

#### RTC-FE-CARD-007: Deactivate Card
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Cards.tsx |
| Test Steps | 1. Click Deactivate on active card |
| Expected Result | Card status changes to Inactive |
| Validation | Badge updates |

### 3.6 Transaction UI Test Cases

#### RTC-FE-TXN-001: Transaction List Renders
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Transactions.tsx |
| Test Steps | 1. Navigate to /transactions |
| Expected Result | Transaction table displays |
| Validation | Type badges and amounts visible |

#### RTC-FE-TXN-002: Filter by Card Number
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Transactions.tsx |
| Test Steps | 1. Enter card number in filter |
| Expected Result | Only transactions for that card shown |
| Validation | All transactions match card |

#### RTC-FE-TXN-003: Filter by Date Range
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Transactions.tsx |
| Test Steps | 1. Select start and end dates |
| Expected Result | Transactions in range displayed |
| Validation | All dates within range |

#### RTC-FE-TXN-004: Filter by Type
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| Component | Transactions.tsx |
| Test Steps | 1. Select Debit type filter |
| Expected Result | Only debit transactions shown |
| Validation | All transactions are debits |

#### RTC-FE-TXN-005: View Transaction Details
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Transactions.tsx |
| Test Steps | 1. Click Details on transaction |
| Expected Result | Detail modal opens |
| Validation | All transaction fields displayed |

#### RTC-FE-TXN-006: Add Transaction
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Transactions.tsx |
| Test Steps | 1. Click Add Transaction, 2. Fill form, 3. Submit |
| Expected Result | Transaction created |
| Validation | Transaction appears in list |

#### RTC-FE-TXN-007: Amount Formatting
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| Component | Transactions.tsx |
| Test Steps | 1. View transaction amounts |
| Expected Result | Amounts formatted as currency |
| Validation | Debits show negative, credits positive |

### 3.7 Payment UI Test Cases

#### RTC-FE-PAY-001: Payment List Renders
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Payments.tsx |
| Test Steps | 1. Navigate to /payments |
| Expected Result | Payment table displays |
| Validation | Status badges visible |

#### RTC-FE-PAY-002: Filter by Status
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Payments.tsx |
| Test Steps | 1. Select Pending status filter |
| Expected Result | Only pending payments shown |
| Validation | All payments have Pending status |

#### RTC-FE-PAY-003: Make ACH Payment
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Payments.tsx |
| Test Steps | 1. Click Make Payment, 2. Select ACH, 3. Fill bank details, 4. Submit |
| Expected Result | Payment created with Pending status |
| Validation | ACH fields validated |

#### RTC-FE-PAY-004: Make Debit Payment
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Payments.tsx |
| Test Steps | 1. Click Make Payment, 2. Select Debit, 3. Enter card, 4. Submit |
| Expected Result | Payment created |
| Validation | Debit card validated |

#### RTC-FE-PAY-005: Schedule Future Payment
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Payments.tsx |
| Test Steps | 1. Create payment with future date |
| Expected Result | Payment created with Scheduled status |
| Validation | Scheduled date in future |

#### RTC-FE-PAY-006: Process Payment
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Payments.tsx |
| Test Steps | 1. Click Process on pending payment |
| Expected Result | Status changes to Completed |
| Validation | Confirmation number assigned |

#### RTC-FE-PAY-007: Cancel Payment
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Payments.tsx |
| Test Steps | 1. Click Cancel on pending payment |
| Expected Result | Status changes to Cancelled |
| Validation | Payment cannot be processed |

#### RTC-FE-PAY-008: Payment Method Conditional Fields
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Payments.tsx |
| Test Steps | 1. Select ACH - bank fields appear, 2. Select Debit - card field appears |
| Expected Result | Correct fields shown for each method |
| Validation | Only relevant fields displayed |

### 3.8 Reports UI Test Cases

#### RTC-FE-RPT-001: Reports Page Renders
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Reports.tsx |
| Test Steps | 1. Navigate to /reports |
| Expected Result | Reports page with tabs displays |
| Validation | Account Statement and Transaction Report tabs visible |

#### RTC-FE-RPT-002: Generate Account Statement
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Component | Reports.tsx |
| Test Steps | 1. Enter account ID, 2. Select dates, 3. Click Generate |
| Expected Result | Statement displays |
| Validation | Transactions and totals shown |

#### RTC-FE-RPT-003: Generate Transaction Report
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Component | Reports.tsx |
| Test Steps | 1. Select Transaction Report tab, 2. Select dates, 3. Generate |
| Expected Result | Report displays |
| Validation | Analytics and breakdowns shown |

#### RTC-FE-RPT-004: Report Date Validation
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| Component | Reports.tsx |
| Test Steps | 1. Select end date before start date |
| Expected Result | Validation error |
| Validation | Invalid date range rejected |

#### RTC-FE-RPT-005: Empty Report Handling
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| Component | Reports.tsx |
| Test Steps | 1. Generate report for date range with no data |
| Expected Result | Empty state message displayed |
| Validation | User-friendly message shown |

## 4. Integration Regression Test Cases

### 4.1 API Gateway Integration

#### RTC-INT-001: Request Routing
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Test Steps | 1. Send requests to /api/v1/customers, /api/v1/accounts, etc. through gateway |
| Expected Result | Requests routed to correct backend services |
| Validation | Responses from appropriate services |

#### RTC-INT-002: JWT Token Validation
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Test Steps | 1. Request without token, 2. Request with invalid token, 3. Request with valid token |
| Expected Result | 401 for missing/invalid, 200 for valid |
| Validation | Security enforced at gateway |

#### RTC-INT-003: Token Refresh Flow
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Test Steps | 1. Make request with expired access token |
| Expected Result | 401 returned, frontend refreshes token, retries request |
| Validation | Seamless token refresh |

### 4.2 Database Integration

#### RTC-INT-DB-001: Transaction Consistency
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Test Steps | 1. Create transaction, 2. Verify account balance updated |
| Expected Result | Balance reflects transaction |
| Validation | ACID properties maintained |

#### RTC-INT-DB-002: Cascade Delete
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Test Steps | 1. Delete customer with accounts |
| Expected Result | Operation blocked or cascaded correctly |
| Validation | Referential integrity maintained |

#### RTC-INT-DB-003: Concurrent Updates
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Test Steps | 1. Simultaneously update same record from two clients |
| Expected Result | One succeeds, one fails with conflict |
| Validation | Optimistic locking works |

### 4.3 Frontend-Backend Integration

#### RTC-INT-FE-001: Login Flow
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Test Steps | 1. Submit login form, 2. Verify tokens stored, 3. Verify dashboard loads |
| Expected Result | Complete login flow works |
| Validation | End-to-end authentication |

#### RTC-INT-FE-002: CRUD Operations
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| Test Steps | 1. Create customer via UI, 2. Edit customer, 3. Delete customer |
| Expected Result | All operations persist to database |
| Validation | Data consistency |

#### RTC-INT-FE-003: Error Handling
| Attribute | Value |
|-----------|-------|
| Priority | High |
| Test Steps | 1. Trigger backend error, 2. Verify frontend displays error |
| Expected Result | User-friendly error message |
| Validation | Errors propagated correctly |

## 5. End-to-End Regression Test Cases

### 5.1 Business Workflow Tests

#### RTC-E2E-001: Customer Onboarding
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COCRDLIC + COACTVWC + COCRDSLC |
| Test Steps | 1. Create customer, 2. Create account, 3. Issue card, 4. Activate card, 5. Make transaction |
| Expected Result | Complete onboarding flow works |
| Validation | All entities linked correctly |

#### RTC-E2E-002: Bill Payment
| Attribute | Value |
|-----------|-------|
| Priority | Critical |
| COBOL Reference | COBIL00C |
| Test Steps | 1. View account balance, 2. Create payment, 3. Process payment, 4. Verify balance reduced |
| Expected Result | Payment reduces account balance |
| Validation | Transaction created for payment |

#### RTC-E2E-003: Account Statement
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | CORPT00C |
| Test Steps | 1. Make multiple transactions, 2. Generate statement, 3. Verify all transactions included |
| Expected Result | Statement accurate |
| Validation | Totals match transaction sum |

#### RTC-E2E-004: Card Lifecycle
| Attribute | Value |
|-----------|-------|
| Priority | High |
| COBOL Reference | COCRDSLC + COCRDUPC |
| Test Steps | 1. Issue card, 2. Activate, 3. Make transactions, 4. Deactivate, 5. Verify transactions blocked |
| Expected Result | Card lifecycle managed correctly |
| Validation | Inactive cards cannot transact |

#### RTC-E2E-005: Account Closure
| Attribute | Value |
|-----------|-------|
| Priority | Medium |
| COBOL Reference | COACTUPC |
| Test Steps | 1. Pay off balance, 2. Deactivate cards, 3. Deactivate account |
| Expected Result | Account closed properly |
| Validation | No orphaned records |

## 6. Test Execution Checklist

### 6.1 Pre-Execution Checklist

- [ ] Test environment deployed and accessible
- [ ] Test data loaded from VSAM exports
- [ ] All services running and healthy
- [ ] Frontend build deployed
- [ ] Database connections verified
- [ ] API Gateway routing configured

### 6.2 Execution Order

1. **Phase 1: Backend Unit Tests** - Execute all RTC-AUTH, RTC-CUST, RTC-ACCT, RTC-CARD, RTC-TXN, RTC-PAY, RTC-RPT test cases
2. **Phase 2: Frontend Component Tests** - Execute all RTC-FE test cases
3. **Phase 3: Integration Tests** - Execute all RTC-INT test cases
4. **Phase 4: End-to-End Tests** - Execute all RTC-E2E test cases

### 6.3 Post-Execution Checklist

- [ ] All critical test cases passed
- [ ] All high priority test cases passed
- [ ] Defects logged for failed tests
- [ ] Test results documented
- [ ] Regression report generated

## 7. Defect Tracking

### 7.1 Defect Template

| Field | Description |
|-------|-------------|
| ID | Unique identifier (DEF-XXX) |
| Test Case | Related test case ID |
| Severity | Critical/High/Medium/Low |
| Summary | Brief description |
| Steps to Reproduce | Detailed steps |
| Expected Result | What should happen |
| Actual Result | What actually happened |
| Environment | Test environment details |
| Status | Open/In Progress/Fixed/Verified/Closed |

### 7.2 Severity Definitions

| Severity | Definition | Resolution Time |
|----------|------------|-----------------|
| Critical | System crash, data loss, security breach | 4 hours |
| High | Major feature broken, no workaround | 24 hours |
| Medium | Feature partially working, workaround exists | 3 days |
| Low | Minor issue, cosmetic | Next release |

## 8. Appendices

### Appendix A: Test Data Sets

Test data is derived from the original VSAM files and includes:
- 500 customers with varying FICO scores
- 750 accounts with different credit limits
- 1200 cards with various expiration dates
- 50,000 historical transactions
- 500 payments in various statuses

### Appendix B: Environment Configuration

| Component | Development | Test | Staging |
|-----------|-------------|------|---------|
| Frontend | localhost:3000 | test.carddemo.com | staging.carddemo.com |
| API Gateway | localhost:8080 | api-test.carddemo.com | api-staging.carddemo.com |
| Database | H2 in-memory | PostgreSQL 15 | PostgreSQL 15 |

### Appendix C: COBOL Program Reference

| Program | Lines | Primary Function |
|---------|-------|------------------|
| COSGN00C | 300 | User sign-on |
| COUSR00C | 280 | User management |
| COCRDLIC | 450 | Customer list |
| COCRDUPC | 500 | Customer CRUD |
| COACTVWC | 400 | Account view |
| COACTUPC | 480 | Account update |
| COCRDSLC | 420 | Card list |
| COCRDUPC | 460 | Card CRUD |
| COTRN00C | 380 | Transaction list |
| COTRN01C | 350 | Transaction add |
| COBIL00C | 550 | Bill payment |
| CORPT00C | 600 | Reports |
