# CardDemo Spring Boot Application

This is the Java Spring Boot migration target for the CardDemo mainframe application. The original application is a credit card management system written in COBOL running on CICS with VSAM file storage.

## Purpose

This project provides the scaffolding for migrating the CardDemo mainframe application to a modern Java-based architecture using Spring Boot. Each package and class maps directly to a component in the original mainframe system.

## Building and Running

### Prerequisites

- Java 17 or later
- Maven 3.8+

### Build

```bash
cd carddemo-springboot
mvn clean package
```

### Run

```bash
mvn spring-boot:run
```

The application starts on port **8080** by default. The H2 database console is available at `http://localhost:8080/h2-console`.

## Architecture Mapping

### Mainframe to Spring Boot Component Mapping

| Mainframe Component | Spring Boot Equivalent | Package |
|---|---|---|
| CICS Transactions | REST Controllers | `com.carddemo.controller` |
| COBOL Programs | Service Classes | `com.carddemo.service` |
| VSAM KSDS Files | JPA Repositories + H2/RDBMS | `com.carddemo.repository` |
| COBOL Copybooks | JPA Entities and DTOs | `com.carddemo.entity`, `com.carddemo.dto` |
| JCL Batch Jobs | Spring Batch Jobs | `com.carddemo.config` |
| USRSEC File | Spring Security | `com.carddemo.security` |
| BMS Maps | (Frontend - separate project) | N/A |

### Entity to VSAM File Mapping

| Entity Class | VSAM File | COBOL Copybook | Description |
|---|---|---|---|
| `Customer` | CUSTFILE | CVCUS01Y | Customer records |
| `Account` | ACCTFILE | CVACT01Y | Account records |
| `Card` | CARDFILE | CVACT02Y | Card records |
| `Transaction` | TRANSACT | CVTRA05Y | Transaction records |
| `User` | USRSEC | CSUSR01Y | User security records |

### Controller to CICS Transaction Mapping

| Controller | CICS Transaction | COBOL Program | Description |
|---|---|---|---|
| `AuthController` | CC00 | COSGN00C | User sign-on |
| `AccountController` | CAVW | COACTVWC | Account view |
| `CardController` | Card mgmt | COCRDLIC, COCRDSLC | Card list/detail |
| `TransactionController` | CT02 | COTRN00C, COTRN01C, COTRN02C | Transaction processing |
| `PaymentController` | Bill pay | COBIL00C | Bill payment |

### Service to COBOL Program Mapping

| Service | COBOL Programs | Description |
|---|---|---|
| `AccountService` | COACTVWC, COACTUPC, CBACT01C | Account business logic |
| `CardService` | COCRDLIC, COCRDSLC, CBACT02C | Card management logic |
| `TransactionService` | COTRN00C, COTRN01C, COTRN02C, CBTRN01C, CBTRN02C | Transaction processing |
| `PaymentService` | COBIL00C | Bill payment processing |
| `UserService` | COSGN00C | Authentication logic |

### Batch Jobs (Spring Batch replacing JCL)

The `BatchConfig` class will host Spring Batch job definitions to replace:

| JCL Job | COBOL Program | Description |
|---|---|---|
| Account batch | CBACT01C | Account file processing |
| Card batch | CBACT02C | Card file processing |
| Transaction posting | CBTRN01C | Daily transaction posting |
| Interest calculation | CBTRN02C | Interest calculation |
| Statement generation | CBSTM03A/B | Monthly statement generation |

## Security

The application supports two user roles matching the mainframe security model:

- **ROLE_USER** - Regular users with access to account views and transaction processing
- **ROLE_ADMIN** - Admin users with full access including user and system management

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.3**
- **Spring Data JPA** - Database access (replacing VSAM I/O)
- **Spring Security** - Authentication and authorization (replacing USRSEC)
- **Spring Batch** - Batch processing (replacing JCL jobs)
- **H2 Database** - Development database (configurable for production RDBMS)
- **Maven** - Build tool
