# CardDemo Mainframe to Spring Boot Migration Plan

This document outlines the comprehensive migration strategy for converting the CardDemo mainframe COBOL/CICS/VSAM application to a modern Spring Boot 3.2 application with Java 17.

## Executive Summary

The CardDemo application is a credit card management system originally built on IBM mainframe technology. This migration will transform the application into a cloud-ready Spring Boot microservice while preserving all business logic and functionality. The migration follows a phased approach, starting with foundational data layer components and progressively building up to complete feature parity.

## Architecture Mapping Overview

The following table summarizes how mainframe components map to Spring Boot equivalents:

| Mainframe Component | Spring Boot Equivalent | Description |
|---------------------|------------------------|-------------|
| VSAM Files | JPA Entities + H2/PostgreSQL | Persistent data storage |
| Copybooks | Java DTOs/Entities | Data structure definitions |
| CICS Transactions | REST Controllers | Online transaction processing |
| CICS Programs | Service Classes | Business logic implementation |
| BMS Maps | Thymeleaf Templates (optional) / REST API | User interface screens |
| JCL Batch Jobs | Spring Batch Jobs | Batch processing operations |
| USRSEC Security | Spring Security | Authentication and authorization |

## Module Structure

The migration is organized into the following logical modules:

### Core Data Layer (Foundation)

This module establishes the data persistence foundation that all other modules depend on.

**Entities to Create:**
- `Account` - Maps to CVACT01Y copybook (account records)
- `Customer` - Maps to CVCUS01Y copybook (customer records)
- `Card` - Maps to card-related data structures
- `Transaction` - Maps to CVTRA05Y copybook (transaction records)
- `TransactionCategoryBalance` - Maps to CVTRA01Y copybook
- `User` - Maps to CSUSR01Y copybook (security/user records)

**Repositories to Create:**
- `AccountRepository`
- `CustomerRepository`
- `CardRepository`
- `TransactionRepository`
- `TransactionCategoryBalanceRepository`
- `UserRepository`

### Transaction Processing Module

This module handles online transaction processing, replacing CICS transaction programs.

**Services to Create:**
- `AccountService` - Account management operations (view, update, list)
- `CustomerService` - Customer management operations
- `CardService` - Credit card management (list, select, update)
- `TransactionService` - Transaction processing and history
- `ReportService` - Report generation

**Controllers to Create:**
- `AccountController` - REST endpoints for account operations
- `CustomerController` - REST endpoints for customer operations
- `CardController` - REST endpoints for card operations
- `TransactionController` - REST endpoints for transaction operations
- `ReportController` - REST endpoints for report generation

### Batch Processing Module

This module replaces JCL batch jobs with Spring Batch jobs.

**Batch Jobs to Create:**
- `AccountFileProcessingJob` - Replaces ACCTFILE.jcl, READACCT.jcl
- `CustomerFileProcessingJob` - Replaces CUSTFILE.jcl, READCUST.jcl
- `CardFileProcessingJob` - Replaces CARDFILE.jcl, READCARD.jcl
- `TransactionProcessingJob` - Replaces TRANFILE.jcl, POSTTRAN.jcl
- `TransactionReportJob` - Replaces TRANREPT.jcl
- `StatementGenerationJob` - Replaces CREASTMT.jcl
- `InterestCalculationJob` - Replaces INTCALC.jcl
- `DailyRejectionsJob` - Replaces DALYREJS.jcl
- `DataExportJob` - Replaces CBEXPORT.jcl
- `DataImportJob` - Replaces CBIMPORT.jcl

### Security Module

This module implements authentication and authorization, replacing the USRSEC VSAM file and related security logic.

**Components to Create:**
- `SecurityConfig` - Spring Security configuration
- `UserDetailsServiceImpl` - Custom user details service
- `JwtTokenProvider` - JWT token generation and validation (optional)
- `AuthenticationController` - Login/logout endpoints

### Optional Integration Modules

These modules handle optional mainframe integrations:

**Authorization Processing (IMS/DB2/MQ):**
- `AuthorizationService` - Transaction authorization logic
- `AuthorizationController` - Authorization endpoints

**Transaction Type Management (DB2):**
- `TransactionTypeService` - Transaction type CRUD operations
- `TransactionTypeRepository` - Transaction type data access

**VSAM-MQ Integration:**
- `MessageQueueService` - Message queue operations (can use Spring AMQP or JMS)

## Development Standards

### Package Naming Conventions

All Java code resides under the base package `com.carddemo` with the following sub-packages:

```
com.carddemo
├── config/          # Configuration classes (@Configuration)
├── controller/      # REST controllers (@RestController)
├── service/         # Business logic services (@Service)
├── repository/      # JPA repositories (@Repository)
├── entity/          # JPA entities (@Entity)
├── dto/             # Data Transfer Objects
├── batch/           # Spring Batch components
│   ├── job/         # Job configurations
│   ├── step/        # Step configurations
│   ├── reader/      # ItemReaders
│   ├── processor/   # ItemProcessors
│   └── writer/      # ItemWriters
├── security/        # Security configuration and components
├── exception/       # Custom exceptions and handlers
└── util/            # Utility classes
```

### Entity and DTO Naming Patterns

**Entities:**
- Use singular nouns: `Account`, `Customer`, `Card`, `Transaction`
- Suffix with nothing (entities are the canonical data model)
- Example: `Account.java`, `Customer.java`

**DTOs:**
- Use descriptive suffixes based on purpose:
  - `*Request` for incoming API requests: `AccountUpdateRequest`
  - `*Response` for API responses: `AccountResponse`
  - `*Dto` for internal data transfer: `AccountDto`
- Example: `AccountCreateRequest.java`, `AccountResponse.java`

**Repositories:**
- Suffix with `Repository`: `AccountRepository`, `CustomerRepository`

**Services:**
- Suffix with `Service`: `AccountService`, `CustomerService`
- Implementation classes suffix with `ServiceImpl` if using interfaces

**Controllers:**
- Suffix with `Controller`: `AccountController`, `CustomerController`

### REST API Endpoint Conventions

**Base Path:** `/api/v1`

**Resource Naming:**
- Use plural nouns for collections: `/accounts`, `/customers`, `/cards`, `/transactions`
- Use kebab-case for multi-word resources: `/transaction-types`

**HTTP Methods:**
- `GET /api/v1/accounts` - List all accounts (with pagination)
- `GET /api/v1/accounts/{id}` - Get single account
- `POST /api/v1/accounts` - Create new account
- `PUT /api/v1/accounts/{id}` - Update entire account
- `PATCH /api/v1/accounts/{id}` - Partial update
- `DELETE /api/v1/accounts/{id}` - Delete account

**Query Parameters:**
- Pagination: `?page=0&size=20&sort=id,asc`
- Filtering: `?status=ACTIVE&customerId=123`
- Search: `?search=john`

**Response Format:**
```json
{
  "data": { ... },
  "message": "Success",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Error Response Format:**
```json
{
  "error": "ACCOUNT_NOT_FOUND",
  "message": "Account with ID 12345 not found",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/accounts/12345"
}
```

### Transaction Management Approach

**Using @Transactional:**
- Apply `@Transactional` at the service layer, not controller layer
- Use `@Transactional(readOnly = true)` for read-only operations
- Specify propagation and isolation levels when needed
- Handle rollback scenarios explicitly

```java
@Service
@Transactional
public class AccountService {
    
    @Transactional(readOnly = true)
    public Account findById(Long id) { ... }
    
    @Transactional(rollbackFor = Exception.class)
    public Account updateBalance(Long id, BigDecimal amount) { ... }
}
```

**Transaction Boundaries:**
- Each REST endpoint should complete within a single transaction
- Batch jobs use chunk-based transactions with configurable commit intervals
- Long-running operations should use programmatic transaction management

### Error Handling Patterns

**Replacing Mainframe Abends:**

Mainframe abend codes are replaced with custom exceptions and a global exception handler:

```java
// Custom exceptions
public class AccountNotFoundException extends RuntimeException { ... }
public class InsufficientFundsException extends RuntimeException { ... }
public class InvalidTransactionException extends RuntimeException { ... }

// Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("ACCOUNT_NOT_FOUND", ex.getMessage()));
    }
}
```

**Abend Code Mapping:**
| Mainframe Abend | Spring Exception | HTTP Status |
|-----------------|------------------|-------------|
| RESP 13 (Not Found) | `EntityNotFoundException` | 404 |
| RESP 14 (Duplicate) | `DuplicateKeyException` | 409 |
| RESP 16 (Invalid Request) | `InvalidRequestException` | 400 |
| RESP 12 (File Error) | `DataAccessException` | 500 |

### Logging Standards

**Use SLF4J with Logback:**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    
    public Account findById(Long id) {
        log.debug("Finding account by ID: {}", id);
        // ... implementation
        log.info("Account found: {}", account.getId());
        return account;
    }
}
```

**Log Levels:**
- `ERROR` - Exceptions and critical failures
- `WARN` - Recoverable issues, deprecated usage
- `INFO` - Business events, transaction completions
- `DEBUG` - Detailed flow information
- `TRACE` - Very detailed debugging (rarely used)

**Structured Logging:**
- Include correlation IDs for request tracing
- Log entry/exit of significant operations
- Never log sensitive data (passwords, full card numbers, SSN)

### Code Documentation Requirements

**Class-Level Documentation:**
```java
/**
 * Service for managing credit card accounts.
 * Handles account creation, updates, balance inquiries, and status changes.
 * 
 * <p>Migrated from mainframe programs: COACTUPC.cbl, COACTVWC.cbl
 * 
 * @see Account
 * @see AccountRepository
 */
@Service
public class AccountService { ... }
```

**Method-Level Documentation:**
```java
/**
 * Updates the account balance after a transaction.
 * 
 * @param accountId the unique account identifier
 * @param amount the transaction amount (positive for credit, negative for debit)
 * @return the updated account
 * @throws AccountNotFoundException if the account does not exist
 * @throws InsufficientFundsException if debit exceeds available balance
 */
public Account updateBalance(Long accountId, BigDecimal amount) { ... }
```

**Migration Notes:**
- Include references to original mainframe source files in class documentation
- Document any behavioral differences from the original implementation
- Note any data type conversions or business rule changes

## Migration Phases

### Phase 1: Foundation (Weeks 1-2)

**Objective:** Establish the core data layer and project infrastructure.

**Tasks:**
1. Create all JPA entities based on copybook definitions
2. Create JPA repositories for all entities
3. Set up database schema with Flyway migrations
4. Create base DTOs for API communication
5. Implement global exception handling
6. Configure logging and monitoring
7. Set up unit test infrastructure

**Dependencies:** None (this is the foundation)

**Deliverables:**
- All entity classes with JPA annotations
- All repository interfaces
- Database migration scripts
- Base configuration classes
- Unit tests for entities

**Success Criteria:**
- All entities compile without errors
- Database schema is created correctly on application startup
- Repository CRUD operations work correctly
- Unit tests pass with >80% coverage on entities

### Phase 2: Core Services (Weeks 3-4)

**Objective:** Implement business logic services for core operations.

**Tasks:**
1. Implement `AccountService` with all account operations
2. Implement `CustomerService` with customer management
3. Implement `CardService` with card operations
4. Implement `TransactionService` with transaction processing
5. Create service-level DTOs
6. Write unit tests for all services

**Dependencies:** Phase 1 (entities and repositories)

**Deliverables:**
- All service classes with business logic
- Service-level DTOs
- Unit tests for services
- Integration tests for service-repository interaction

**Success Criteria:**
- All business operations from mainframe programs are implemented
- Services handle edge cases and errors correctly
- Unit tests pass with >80% coverage
- Integration tests verify database operations

### Phase 3: REST API Layer (Weeks 5-6)

**Objective:** Expose services through REST APIs.

**Tasks:**
1. Implement `AccountController` with all endpoints
2. Implement `CustomerController` with all endpoints
3. Implement `CardController` with all endpoints
4. Implement `TransactionController` with all endpoints
5. Implement `ReportController` for report generation
6. Add request validation
7. Document APIs with OpenAPI/Swagger

**Dependencies:** Phase 2 (services)

**Deliverables:**
- All REST controllers
- Request/Response DTOs with validation
- OpenAPI documentation
- Controller unit tests
- API integration tests

**Success Criteria:**
- All CICS transactions have equivalent REST endpoints
- API documentation is complete and accurate
- All endpoints return correct responses
- Validation errors return appropriate messages

### Phase 4: Security Implementation (Weeks 7-8)

**Objective:** Implement authentication and authorization.

**Tasks:**
1. Configure Spring Security
2. Implement user authentication (replacing USRSEC)
3. Implement role-based authorization (Admin vs User)
4. Add JWT token support (optional)
5. Secure all REST endpoints
6. Implement audit logging

**Dependencies:** Phase 3 (controllers)

**Deliverables:**
- Security configuration
- Authentication endpoints
- Role-based access control
- Security tests

**Success Criteria:**
- Users can authenticate with credentials
- Admin-only endpoints are protected
- Invalid credentials are rejected
- Session management works correctly

### Phase 5: Batch Processing (Weeks 9-11)

**Objective:** Implement Spring Batch jobs to replace JCL.

**Tasks:**
1. Set up Spring Batch infrastructure
2. Implement account processing batch jobs
3. Implement transaction processing batch jobs
4. Implement report generation batch jobs
5. Implement data import/export jobs
6. Configure job scheduling

**Dependencies:** Phase 2 (services)

**Deliverables:**
- All batch job configurations
- ItemReaders, ItemProcessors, ItemWriters
- Job scheduling configuration
- Batch job tests

**Success Criteria:**
- All JCL jobs have Spring Batch equivalents
- Batch jobs process data correctly
- Jobs can be scheduled and monitored
- Error handling and restart capability works

### Phase 6: Integration and Testing (Weeks 12-13)

**Objective:** Complete integration testing and performance validation.

**Tasks:**
1. End-to-end integration testing
2. Performance testing and optimization
3. Load testing
4. Security penetration testing
5. Data migration testing
6. Documentation completion

**Dependencies:** All previous phases

**Deliverables:**
- Integration test suite
- Performance test results
- Load test results
- Security audit report
- Complete documentation

**Success Criteria:**
- All integration tests pass
- Performance meets or exceeds mainframe baseline
- No critical security vulnerabilities
- Documentation is complete

### Phase 7: Optional Modules (Weeks 14-16)

**Objective:** Implement optional integration modules.

**Tasks:**
1. Authorization processing service
2. Transaction type management
3. Message queue integration
4. External system integrations

**Dependencies:** Phase 6

**Deliverables:**
- Optional module implementations
- Integration tests
- Documentation updates

**Success Criteria:**
- Optional modules function correctly
- Integrations work with external systems
- All tests pass

## Data Type Conversion Reference

| COBOL Type | Java Type | Notes |
|------------|-----------|-------|
| PIC 9(n) | `Long` or `Integer` | Use `Long` for IDs, `Integer` for small numbers |
| PIC 9(n)V99 | `BigDecimal` | Always use BigDecimal for monetary values |
| PIC S9(n)V99 | `BigDecimal` | Signed decimal values |
| PIC X(n) | `String` | Character fields |
| PIC X(10) (date) | `LocalDate` | Date fields stored as strings |
| PIC X(26) (timestamp) | `LocalDateTime` | Timestamp fields |
| PIC 9(n) COMP | `Long` | Binary/computational fields |
| PIC 9(n) COMP-3 | `BigDecimal` | Packed decimal fields |

## File Mapping Reference

### COBOL Programs to Spring Components

| COBOL Program | Type | Spring Component |
|---------------|------|------------------|
| COSGN00C | CICS | `AuthenticationController`, `AuthenticationService` |
| COMEN01C | CICS | `MenuController` (or handled by frontend) |
| COADM01C | CICS | `AdminController` |
| COACTUPC | CICS | `AccountController.updateAccount()` |
| COACTVWC | CICS | `AccountController.getAccount()` |
| COBIL00C | CICS | `BillingController`, `BillingService` |
| COCRDLIC | CICS | `CardController.listCards()` |
| COCRDSLC | CICS | `CardController.getCard()` |
| COCRDUPC | CICS | `CardController.updateCard()` |
| CORPT00C | CICS | `ReportController`, `ReportService` |
| COTRN00C | CICS | `TransactionController.listTransactions()` |
| COTRN01C | CICS | `TransactionController.getTransaction()` |
| COTRN02C | CICS | `TransactionController.createTransaction()` |
| COUSR00C-03C | CICS | `UserController`, `UserService` |
| CBACT01C-04C | Batch | `AccountBatchJob` |
| CBCUS01C | Batch | `CustomerBatchJob` |
| CBTRN01C-03C | Batch | `TransactionBatchJob` |
| CBEXPORT | Batch | `DataExportJob` |
| CBIMPORT | Batch | `DataImportJob` |

### Copybooks to Entities

| Copybook | Entity | Key Fields |
|----------|--------|------------|
| CVACT01Y | `Account` | `accountId` (11 digits) |
| CVCUS01Y | `Customer` | `customerId` (9 digits) |
| CVTRA05Y | `Transaction` | `transactionId` (16 chars) |
| CVTRA01Y | `TransactionCategoryBalance` | `accountId` + `typeCode` + `categoryCode` |
| CSUSR01Y | `User` | `userId` (8 chars) |

## Risk Mitigation

**Data Integrity Risks:**
- Implement comprehensive validation matching mainframe edits
- Use database constraints to enforce data integrity
- Implement audit logging for all data changes

**Performance Risks:**
- Profile and optimize database queries
- Implement caching for frequently accessed data
- Use connection pooling and query optimization

**Security Risks:**
- Follow OWASP security guidelines
- Implement proper authentication and authorization
- Encrypt sensitive data at rest and in transit

**Migration Risks:**
- Maintain parallel operation during transition
- Implement comprehensive testing at each phase
- Plan for rollback scenarios

## Appendix: Project Structure

```
migrated-spring-boot/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── carddemo/
│   │   │           ├── CardDemoApplication.java
│   │   │           ├── config/
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   ├── BatchConfig.java
│   │   │           │   └── WebConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── AccountController.java
│   │   │           │   ├── CustomerController.java
│   │   │           │   ├── CardController.java
│   │   │           │   ├── TransactionController.java
│   │   │           │   └── AuthenticationController.java
│   │   │           ├── service/
│   │   │           │   ├── AccountService.java
│   │   │           │   ├── CustomerService.java
│   │   │           │   ├── CardService.java
│   │   │           │   ├── TransactionService.java
│   │   │           │   └── UserService.java
│   │   │           ├── repository/
│   │   │           │   ├── AccountRepository.java
│   │   │           │   ├── CustomerRepository.java
│   │   │           │   ├── CardRepository.java
│   │   │           │   ├── TransactionRepository.java
│   │   │           │   └── UserRepository.java
│   │   │           ├── entity/
│   │   │           │   ├── Account.java
│   │   │           │   ├── Customer.java
│   │   │           │   ├── Card.java
│   │   │           │   ├── Transaction.java
│   │   │           │   └── User.java
│   │   │           ├── dto/
│   │   │           │   ├── request/
│   │   │           │   └── response/
│   │   │           ├── batch/
│   │   │           │   ├── job/
│   │   │           │   ├── reader/
│   │   │           │   ├── processor/
│   │   │           │   └── writer/
│   │   │           ├── security/
│   │   │           ├── exception/
│   │   │           │   ├── GlobalExceptionHandler.java
│   │   │           │   └── custom exceptions...
│   │   │           └── util/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/
│   │           └── migration/
│   └── test/
│       └── java/
│           └── com/
│               └── carddemo/
│                   ├── controller/
│                   ├── service/
│                   ├── repository/
│                   └── integration/
└── MIGRATION_PLAN.md
```
