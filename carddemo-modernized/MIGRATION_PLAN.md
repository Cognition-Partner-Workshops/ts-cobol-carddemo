# CardDemo Mainframe to Java/Spring Boot Migration Plan

## Executive Summary

This document outlines the migration strategy for converting the CardDemo mainframe COBOL application to a modern Java Spring Boot microservices architecture with MongoDB and React frontend.

## Source System Analysis

### Original COBOL Data Structures

The CardDemo application uses the following VSAM KSDS files:

| Entity | Copybook | Record Length | Primary Key |
|--------|----------|---------------|-------------|
| Customer | CVCUS01Y.cpy | 500 bytes | CUST-ID (9 digits) |
| Account | CVACT01Y.cpy | 300 bytes | ACCT-ID (11 digits) |
| Card | CVACT02Y.cpy | 150 bytes | CARD-NUM (16 chars) |
| Card Cross-Reference | CVACT03Y.cpy | 50 bytes | XREF-CARD-NUM |
| Transaction | CVTRA05Y.cpy | 350 bytes | TRAN-ID (16 chars) |
| User Security | CSUSR01Y.cpy | 80 bytes | SEC-USR-ID (8 chars) |

### Original COBOL Programs

| Program | Function | Target Microservice |
|---------|----------|---------------------|
| COSGN00C | User Sign-on | Auth Service |
| COMEN01C | Main Menu | Frontend Router |
| COACTVWC | Account View | Account Service |
| COACTUPC | Account Update | Account Service |
| COCRDLIC | Card List | Card Service |
| COCRDSLC | Card View | Card Service |
| COCRDUPC | Card Update | Card Service |
| COTRN00C | Transaction List | Transaction Service |
| COTRN01C | Transaction View | Transaction Service |
| COTRN02C | Transaction Add | Transaction Service |
| COBIL00C | Bill Payment | Transaction Service |
| COUSR00C-03C | User Management | User Service |

## Target Architecture

### Technology Stack

- **Backend**: Java 17, Spring Boot 3.x, Spring Data MongoDB
- **Database**: MongoDB
- **Authentication**: JWT (JSON Web Tokens)
- **Frontend**: React 18, TypeScript, Tailwind CSS
- **Build Tool**: Maven

### Microservices Architecture

```
                    +------------------+
                    |   React Frontend |
                    +--------+---------+
                             |
                    +--------v---------+
                    |   API Gateway    |
                    +--------+---------+
                             |
        +--------------------+--------------------+
        |          |         |         |         |
+-------v--+ +-----v----+ +--v-----+ +-v------+ +v--------+
| Customer | | Account  | | Card   | |  Trans | |  Auth   |
| Service  | | Service  | | Service| | Service| | Service |
+----+-----+ +----+-----+ +---+----+ +---+----+ +----+----+
     |            |           |          |           |
     +------------+-----------+----------+-----------+
                             |
                    +--------v---------+
                    |     MongoDB      |
                    +------------------+
```

### MongoDB Collections

1. **customers** - Customer master data
2. **accounts** - Account information
3. **cards** - Credit card details
4. **cardXrefs** - Card to customer/account cross-reference
5. **transactions** - Transaction records
6. **users** - User authentication data

## Data Model Mapping

### Customer Document
```json
{
  "_id": "ObjectId",
  "customerId": "string (9 digits)",
  "firstName": "string",
  "middleName": "string",
  "lastName": "string",
  "addressLine1": "string",
  "addressLine2": "string",
  "addressLine3": "string",
  "stateCode": "string",
  "countryCode": "string",
  "zipCode": "string",
  "phoneNumber1": "string",
  "phoneNumber2": "string",
  "ssn": "string",
  "govtIssuedId": "string",
  "dateOfBirth": "date",
  "eftAccountId": "string",
  "primaryCardHolderInd": "string",
  "ficoCreditScore": "number"
}
```

### Account Document
```json
{
  "_id": "ObjectId",
  "accountId": "string (11 digits)",
  "activeStatus": "string",
  "currentBalance": "decimal",
  "creditLimit": "decimal",
  "cashCreditLimit": "decimal",
  "openDate": "date",
  "expirationDate": "date",
  "reissueDate": "date",
  "currentCycleCredit": "decimal",
  "currentCycleDebit": "decimal",
  "zipCode": "string",
  "groupId": "string"
}
```

### Card Document
```json
{
  "_id": "ObjectId",
  "cardNumber": "string (16 chars)",
  "accountId": "string",
  "cvvCode": "string",
  "embossedName": "string",
  "expirationDate": "date",
  "activeStatus": "string"
}
```

### Transaction Document
```json
{
  "_id": "ObjectId",
  "transactionId": "string (16 chars)",
  "typeCode": "string",
  "categoryCode": "number",
  "source": "string",
  "description": "string",
  "amount": "decimal",
  "merchantId": "string",
  "merchantName": "string",
  "merchantCity": "string",
  "merchantZip": "string",
  "cardNumber": "string",
  "originTimestamp": "datetime",
  "processTimestamp": "datetime"
}
```

### User Document
```json
{
  "_id": "ObjectId",
  "userId": "string",
  "firstName": "string",
  "lastName": "string",
  "password": "string (hashed)",
  "userType": "string (ADMIN/USER)"
}
```

## API Endpoints

### Auth Service
- POST /api/auth/login - User authentication
- POST /api/auth/register - User registration (admin only)
- GET /api/auth/me - Get current user

### Customer Service
- GET /api/customers - List customers
- GET /api/customers/{id} - Get customer by ID
- POST /api/customers - Create customer
- PUT /api/customers/{id} - Update customer
- DELETE /api/customers/{id} - Delete customer

### Account Service
- GET /api/accounts - List accounts
- GET /api/accounts/{id} - Get account by ID
- GET /api/accounts/customer/{customerId} - Get accounts by customer
- POST /api/accounts - Create account
- PUT /api/accounts/{id} - Update account

### Card Service
- GET /api/cards - List cards
- GET /api/cards/{cardNumber} - Get card by number
- GET /api/cards/account/{accountId} - Get cards by account
- POST /api/cards - Create card
- PUT /api/cards/{cardNumber} - Update card

### Transaction Service
- GET /api/transactions - List transactions (with pagination)
- GET /api/transactions/{id} - Get transaction by ID
- GET /api/transactions/card/{cardNumber} - Get transactions by card
- POST /api/transactions - Create transaction
- POST /api/transactions/bill-payment - Process bill payment

## Frontend Pages

1. **Login Page** - User authentication
2. **Dashboard** - Main menu with navigation
3. **Account View** - View account details
4. **Account Update** - Edit account information
5. **Card List** - List all cards
6. **Card View** - View card details
7. **Card Update** - Edit card information
8. **Transaction List** - List transactions with pagination
9. **Transaction View** - View transaction details
10. **Transaction Add** - Create new transaction
11. **Bill Payment** - Process bill payments
12. **User Management** - Admin user management (admin only)

## Migration Steps

1. Set up Spring Boot project structure
2. Create MongoDB document models
3. Implement repositories and services
4. Create REST controllers
5. Implement JWT authentication
6. Build React frontend
7. Test all functionality
8. Deploy and validate

## Business Logic Migration

### Sign-on Logic (COSGN00C -> AuthService)
- Validate user credentials against users collection
- Generate JWT token on successful authentication
- Return user type (ADMIN/USER) for role-based access

### Account View (COACTVWC -> AccountService)
- Read account by ID from accounts collection
- Join with customer data via cardXrefs
- Return formatted account details

### Transaction Processing (COTRN02C -> TransactionService)
- Validate card number exists
- Validate account is active
- Create transaction record
- Update account balance

### Bill Payment (COBIL00C -> TransactionService)
- Read account current balance
- Create payment transaction
- Update account balance to zero
