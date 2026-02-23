# CardDemo Target Architecture Design Document

## Document Information
| Item | Detail |
|------|--------|
| **Document** | Target Architecture Design |
| **Application** | CardDemo - Java 17 Migration |
| **Phase** | Phase 1.2 - Architecture Design |
| **Version** | 1.0 |

---

## 1. Technology Stack

### 1.1 Core Technologies

| Layer | Technology | Version | Replaces |
|-------|-----------|---------|----------|
| **Language** | Java | 17 (LTS) | COBOL |
| **Framework** | Spring Boot | 3.2.x | CICS |
| **Web/API** | Spring MVC + Spring WebFlux | 6.1.x | CICS BMS Screens |
| **Batch** | Spring Batch | 5.1.x | JCL + COBOL Batch |
| **ORM** | Spring Data JPA / Hibernate | 6.4.x | VSAM File I/O |
| **Database** | PostgreSQL | 16.x | VSAM KSDS Files |
| **Messaging** | Spring JMS + Apache ActiveMQ Artemis | 2.31.x | IBM MQ |
| **Security** | Spring Security | 6.2.x | RACF |
| **API Docs** | SpringDoc OpenAPI | 2.3.x | - |
| **Migration** | Flyway | 10.x | IDCAMS |
| **Build** | Apache Maven | 3.9.x | JCL Compile |
| **Testing** | JUnit 5 + Mockito + Spring Boot Test | 5.10.x | - |
| **Containerization** | Docker + Docker Compose | Latest | Mainframe LPAR |

### 1.2 Additional Libraries

| Library | Purpose |
|---------|---------|
| MapStruct | COBOL copybook-to-DTO mapping (compile-time) |
| Lombok | Reduce boilerplate in entity/DTO classes |
| Jackson | JSON serialization/deserialization |
| Hibernate Validator | Bean validation (replacing COBOL validation logic) |
| Micrometer + Prometheus | Metrics and monitoring |
| Logback + SLF4J | Structured logging |
| Testcontainers | Integration testing with real PostgreSQL |

---

## 2. High-Level Architecture

### 2.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  Web Browser  │  │  REST Client │  │  External Systems (MQ)   │  │
│  │  (Thymeleaf)  │  │  (Swagger)   │  │                          │  │
│  └──────┬───────┘  └──────┬───────┘  └────────────┬─────────────┘  │
│         │                  │                        │                 │
└─────────┼──────────────────┼────────────────────────┼─────────────── ┘
          │                  │                        │
┌─────────┼──────────────────┼────────────────────────┼─────────────── ┐
│         ▼                  ▼                        ▼                 │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                    API GATEWAY / SECURITY                       │ │
│  │              Spring Security (JWT Authentication)               │ │
│  └─────────────────────────┬───────────────────────────────────────┘ │
│                            │                                         │
│  ┌─────────────────────────┼───────────────────────────────────────┐ │
│  │              PRESENTATION LAYER (carddemo-web)                  │ │
│  │  ┌────────────┐  ┌────────────┐  ┌──────────────────────────┐  │ │
│  │  │ Web MVC    │  │ Thymeleaf  │  │ Error Handling            │  │ │
│  │  │ Controllers│  │ Templates  │  │ (GlobalExceptionHandler)  │  │ │
│  │  └──────┬─────┘  └────────────┘  └──────────────────────────┘  │ │
│  └─────────┼──────────────────────────────────────────────────────┘  │
│            │                                                         │
│  ┌─────────┼──────────────────────────────────────────────────────┐  │
│  │         ▼     API LAYER (carddemo-api)                         │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────────────┐   │  │
│  │  │ REST       │  │ DTO        │  │ Request/Response        │   │  │
│  │  │ Controllers│  │ Mappers    │  │ Validation              │   │  │
│  │  └──────┬─────┘  └────────────┘  └────────────────────────┘   │  │
│  └─────────┼─────────────────────────────────────────────────────┘   │
│            │                                                         │
│  ┌─────────┼─────────────────────────────────────────────────────┐   │
│  │         ▼     SERVICE LAYER (carddemo-api)                     │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │  │
│  │  │ Auth Service  │  │ Account Svc  │  │ Transaction Svc    │   │  │
│  │  │ (COSGN00C)   │  │ (COACTVWC,   │  │ (COTRN00C-02C,    │   │  │
│  │  │              │  │  COACTUPC)   │  │  CBTRN02C)         │   │  │
│  │  └──────────────┘  └──────────────┘  └────────────────────┘   │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │  │
│  │  │ Card Service  │  │ Payment Svc  │  │ Report Service     │   │  │
│  │  │ (COCRDLIC,   │  │ (COBIL00C)   │  │ (CORPT00C,        │   │  │
│  │  │  COCRDUPC)   │  │              │  │  CBTRN03C)         │   │  │
│  │  └──────────────┘  └──────────────┘  └────────────────────┘   │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │  │
│  │  │ User Admin   │  │ Auth Process │  │ Interest Calc      │   │  │
│  │  │ Service      │  │ Service      │  │ Service             │   │  │
│  │  │ (COUSR00C-   │  │ (COPAUA0C)   │  │ (CBACT04C)         │   │  │
│  │  │  COUSR03C)   │  │              │  │                    │   │  │
│  │  └──────────────┘  └──────────────┘  └────────────────────┘   │  │
│  └───────────────────────────┬───────────────────────────────────┘   │
│                              │                                       │
│  ┌───────────────────────────┼───────────────────────────────────┐   │
│  │                           ▼                                    │  │
│  │           DATA ACCESS LAYER (carddemo-core)                    │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │  │
│  │  │ JPA Entities  │  │ Spring Data  │  │ Custom             │   │  │
│  │  │ (Copybook     │  │ JPA          │  │ Query              │   │  │
│  │  │  mappings)    │  │ Repositories │  │ Methods            │   │  │
│  │  └──────────────┘  └──────────────┘  └────────────────────┘   │  │
│  └───────────────────────────┬───────────────────────────────────┘   │
│                              │                                       │
│  ┌───────────────────────────┼───────────────────────────────────┐   │
│  │                           ▼                                    │  │
│  │           BATCH LAYER (carddemo-batch)                         │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │  │
│  │  │ Job Config   │  │ Item         │  │ Item               │   │  │
│  │  │ (JCL → Spring│  │ Readers      │  │ Writers            │   │  │
│  │  │  Batch Jobs) │  │ (VSAM reads) │  │ (DB writes)        │   │  │
│  │  └──────────────┘  └──────────────┘  └────────────────────┘   │  │
│  │  ┌──────────────┐  ┌──────────────┐                           │  │
│  │  │ Item         │  │ Job          │                           │  │
│  │  │ Processors   │  │ Listeners    │                           │  │
│  │  │ (Business    │  │ (Logging,    │                           │  │
│  │  │  logic)      │  │  Monitoring) │                           │  │
│  │  └──────────────┘  └──────────────┘                           │  │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │           INTEGRATION LAYER (carddemo-integration)             │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │  │
│  │  │ JMS Listeners│  │ JMS Senders  │  │ Message            │   │  │
│  │  │ (MQ Trigger  │  │ (Auth Reply) │  │ Converters         │   │  │
│  │  │  → Auth)     │  │              │  │ (CSV ↔ Java)       │   │  │
│  │  └──────────────┘  └──────────────┘  └────────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │                    INFRASTRUCTURE                               │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │  │
│  │  │ PostgreSQL   │  │ ActiveMQ     │  │ Flyway             │   │  │
│  │  │ Database     │  │ Artemis      │  │ Migrations         │   │  │
│  │  └──────────────┘  └──────────────┘  └────────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 Module Dependency Diagram

```
┌──────────────────┐
│  carddemo-web    │──────┐
│  (Thymeleaf UI)  │      │
└──────────────────┘      │
                          ▼
┌──────────────────┐   ┌──────────────────┐
│  carddemo-test   │──▶│  carddemo-api    │
│  (Integration    │   │  (REST API +     │
│   Tests)         │   │   Services)      │
└──────────────────┘   └────────┬─────────┘
                                │
┌──────────────────┐            │
│ carddemo-        │            │
│ integration      │────────┐   │
│ (JMS/Messaging)  │        │   │
└──────────────────┘        │   │
                            ▼   ▼
┌──────────────────┐   ┌──────────────────┐
│  carddemo-batch  │──▶│  carddemo-core   │
│  (Spring Batch   │   │  (Entities,      │
│   Jobs)          │   │   Repositories,  │
└──────────────────┘   │   Exceptions)    │
                       └──────────────────┘
```

---

## 3. Component Mapping: COBOL to Java

### 3.1 Online Transaction Mapping

| COBOL Component | Java Component | Layer | Pattern |
|----------------|---------------|-------|---------|
| CICS Transaction Handler | Spring MVC Controller | API | REST Controller |
| BMS Map (Screen) | Thymeleaf Template / JSON Response | Presentation | Template/DTO |
| COBOL COMMAREA | Spring Session / JWT Token | Cross-cutting | Stateless Auth |
| COBOL Business Logic | Spring Service Bean | Service | Service Pattern |
| VSAM READ/WRITE | Spring Data JPA Repository | Data Access | Repository Pattern |
| COBOL COPY statement | Java import / Entity class | Domain | Entity/DTO |
| CICS SEND MAP | Controller return / ResponseEntity | API | REST Response |
| CICS RECEIVE MAP | @RequestBody / @ModelAttribute | API | Request Binding |
| CICS RETURN TRANSID | HTTP Redirect / SPA Navigation | Presentation | Redirect |

### 3.2 Batch Job Mapping

| JCL/COBOL Component | Spring Batch Component | Description |
|---------------------|----------------------|-------------|
| JCL JOB Card | Spring Batch Job Configuration | Job definition with steps |
| JCL EXEC PGM | Spring Batch Step (Tasklet or Chunk) | Step execution |
| COBOL READ file | ItemReader (JpaPagingItemReader) | Read from database |
| COBOL Business Logic | ItemProcessor | Transform/validate data |
| COBOL WRITE file | ItemWriter (JpaItemWriter) | Write to database |
| IDCAMS DEFINE | Flyway Migration Script | DDL execution |
| IDCAMS REPRO | Spring Batch Step (data load) | Data copy/load |
| SORT utility | SQL ORDER BY / Java Comparator | Data sorting |
| IEBGENER | Spring Batch file copy step | File copy |
| JCL COND | Spring Batch Flow Decision | Conditional execution |
| GDG (Generation Data Group) | Timestamped backup tables/files | Versioned data |

### 3.3 Data Access Mapping

| VSAM Operation | JPA/SQL Equivalent |
|---------------|-------------------|
| READ (by key) | `repository.findById(key)` |
| READ NEXT | `repository.findAll(pageable)` |
| READ PREV | `repository.findAll(pageable)` with reverse sort |
| WRITE | `repository.save(entity)` |
| REWRITE | `repository.save(entity)` (merge) |
| DELETE | `repository.deleteById(key)` |
| START (position) | `repository.findByKeyGreaterThanEqual(key, pageable)` |
| READ with AIX | `repository.findByAlternateKey(value)` |

---

## 4. Security Architecture

### 4.1 Authentication Flow

```
┌──────────┐    POST /api/auth/login     ┌──────────────────┐
│  Client   │ ────────────────────────── ▶│  AuthController   │
│           │                             │                    │
│           │    JWT Token Response        │  Validates against │
│           │ ◀────────────────────────── │  USER_SECURITY     │
│           │                             │  table             │
│           │                             └──────────────────┘
│           │
│           │    Authorization: Bearer {token}
│           │ ──────────────────────────▶ ┌──────────────────┐
│           │                             │  Spring Security   │
│           │                             │  Filter Chain      │
│           │                             │  - JWT Validation  │
│           │                             │  - Role Extraction │
│           │                             │  - ADMIN / USER    │
└──────────┘                             └──────────────────┘
```

### 4.2 Authorization Model

| COBOL User Type | Java Role | Access |
|----------------|-----------|--------|
| SEC-USR-TYPE = 'A' | ROLE_ADMIN | Admin menu + all user functions |
| SEC-USR-TYPE = 'U' | ROLE_USER | User functions only |

---

## 5. Data Flow Diagrams

### 5.1 Transaction Processing Flow

```
┌───────────────┐     ┌────────────────┐     ┌─────────────────┐
│  Web Client    │────▶│  Transaction   │────▶│  Transaction    │
│  (Add Tran)    │     │  Controller    │     │  Service        │
└───────────────┘     └────────────────┘     └────────┬────────┘
                                                       │
                      ┌────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│  TransactionService.addTransaction()                             │
│                                                                   │
│  1. Validate card number via CardXrefRepository                  │
│  2. Validate account status via AccountRepository                 │
│  3. Generate transaction ID (TRAN-ID format: 16-char)            │
│  4. Create Transaction entity                                     │
│  5. Save to TransactionRepository                                 │
│  6. Return confirmation                                           │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Batch Processing Flow (Daily Cycle)

```
┌─────────────┐    ┌──────────────┐    ┌──────────────┐
│  Close Files  │──▶│  Load Master  │──▶│  Post Daily   │
│  (Prep)       │   │  Data Files   │   │  Transactions │
└─────────────┘    └──────────────┘    └──────┬───────┘
                                               │
                                               ▼
                   ┌──────────────┐    ┌──────────────┐
                   │  Generate     │◀──│  Calculate    │
                   │  Statements   │   │  Interest     │
                   └──────┬───────┘    └──────────────┘
                          │
                          ▼
                   ┌──────────────┐    ┌──────────────┐
                   │  Generate     │──▶│  Reopen       │
                   │  Reports      │   │  Files        │
                   └──────────────┘    └──────────────┘
```

### 5.3 Authorization Processing Flow (JMS)

```
┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐
│  External     │    │  ActiveMQ    │    │  AuthorizationListener│
│  POS System   │──▶│  Request     │──▶│  (JMS @JmsListener)   │
│               │    │  Queue       │    │                       │
└──────────────┘    └──────────────┘    └──────────┬────────────┘
                                                    │
                                                    ▼
                                        ┌──────────────────────┐
                                        │  AuthorizationService │
                                        │  1. Parse CSV request │
                                        │  2. Lookup card XREF  │
                                        │  3. Validate account  │
                                        │  4. Apply rules       │
                                        │  5. Save auth summary │
                                        │  6. Save auth detail  │
                                        │  7. Send response     │
                                        └──────────┬────────────┘
                                                    │
                    ┌──────────────┐                 │
                    │  ActiveMQ    │◀────────────────┘
                    │  Reply Queue │
                    └──────────────┘
```

---

## 6. Deployment Architecture

### 6.1 Local Development

```
┌──────────────────────────────────────────────────┐
│                Docker Compose                      │
│                                                    │
│  ┌──────────────┐  ┌──────────────┐               │
│  │  PostgreSQL   │  │  ActiveMQ    │               │
│  │  Port: 5432   │  │  Artemis     │               │
│  │               │  │  Port: 61616 │               │
│  │               │  │  Console:    │               │
│  │               │  │  8161        │               │
│  └──────────────┘  └──────────────┘               │
│                                                    │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│  Spring Boot Application (IDE / mvn spring-boot:run)│
│  Port: 8080                                        │
│  Profiles: local                                    │
│  Swagger UI: http://localhost:8080/swagger-ui.html │
└──────────────────────────────────────────────────┘
```

### 6.2 Production Deployment (Target)

```
┌─────────────────────────────────────────────────────────┐
│                     AWS Cloud                             │
│                                                           │
│  ┌───────────┐   ┌─────────────────────────────────────┐ │
│  │  ALB       │──▶│  ECS / EKS Cluster                  │ │
│  │            │   │  ┌─────────────┐ ┌─────────────┐    │ │
│  │            │   │  │ CardDemo    │ │ CardDemo    │    │ │
│  │            │   │  │ API (x3)   │ │ Batch (x1)  │    │ │
│  │            │   │  └──────┬──────┘ └──────┬──────┘    │ │
│  └───────────┘   └─────────┼────────────────┼──────────┘  │
│                             │                │             │
│  ┌──────────────────────────┼────────────────┼──────────┐ │
│  │  Data Services           │                │           │ │
│  │  ┌──────────────┐  ┌────┴───────────┐               │ │
│  │  │  Amazon RDS   │  │  Amazon MQ     │               │ │
│  │  │  PostgreSQL   │  │  (ActiveMQ)    │               │ │
│  │  └──────────────┘  └────────────────┘               │ │
│  └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────── ┘
```

---

## 7. Cross-Cutting Concerns

### 7.1 Logging Strategy

| COBOL Mechanism | Java Replacement |
|----------------|-----------------|
| DISPLAY statements | SLF4J Logger (DEBUG level) |
| CICS WRITEQ TD | SLF4J Logger (INFO/ERROR level) |
| Abend codes | Custom exceptions with error codes |
| CICS DUMP | Structured JSON logging with MDC |

### 7.2 Error Handling

| COBOL Mechanism | Java Replacement |
|----------------|-----------------|
| RESP/RESP2 codes | Try-catch with custom exceptions |
| CICS HANDLE CONDITION | @ControllerAdvice + @ExceptionHandler |
| SQLCA (DB2 errors) | Spring DataAccessException hierarchy |
| ABEND | RuntimeException subclasses |
| WS-RETURN-MSG | ResponseEntity with error DTOs |

### 7.3 Transaction Management

| COBOL/CICS | Spring/Java |
|-----------|------------|
| CICS SYNCPOINT | @Transactional (auto-commit) |
| CICS SYNCPOINT ROLLBACK | @Transactional rollbackFor |
| Two-phase commit (IMS + DB2) | JTA / Spring Transaction Manager |
| CICS Unit of Work | Spring @Transactional boundary |

---

## 8. Migration Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|-----------|
| COMP-3 (Packed Decimal) precision loss | High | Use BigDecimal for all monetary fields |
| VSAM AIX ordering differences | Medium | Design proper composite indexes |
| COBOL implicit type conversions | Medium | Explicit Java type converters |
| CICS pseudo-conversational pattern | Low | Stateless REST + JWT replaces COMMAREA |
| IMS hierarchical → relational | Medium | Careful parent-child FK design |
| MQ message format compatibility | Low | Message converter with CSV parsing |
| EBCDIC to ASCII encoding | Medium | Handle during data migration ETL |
| Zoned decimal / unsigned numeric | Medium | Custom parsers during data import |
