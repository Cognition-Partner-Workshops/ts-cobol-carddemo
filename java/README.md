# CardDemo - Spring Boot Application

This Spring Boot application is the migration target for the [CardDemo mainframe COBOL application](../README.md). The original application is a credit card management system built with COBOL, CICS, VSAM, and JCL. This project re-implements it using modern Java technologies.

## Prerequisites

- Java 17 or later
- Maven 3.8+

## Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Run with a specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod-postgres
```

The application starts on port **8080** by default. The H2 console is available at `http://localhost:8080/h2-console` when running with the default (dev) profile.

## Architectural Mapping

The table below shows how mainframe components map to Spring Boot components in this project.

### CICS Transactions to REST Controllers

| COBOL Program | CICS Transaction | Spring Controller (target) |
|---|---|---|
| COSGN00C | CC00 | `controller/` - Sign-on |
| COMEN01C | CM00 | `controller/` - Main menu |
| COACTVWC | CAVW | `controller/` - Account view |
| COCRDLIC | CCLI | `controller/` - Card list |
| COTRN00C | CT00 | `controller/` - Transaction list |
| COTRN01C | CT01 | `controller/` - Transaction view |
| COTRN02C | CT02 | `controller/` - Add transaction |
| COBIL00C | CB00 | `controller/` - Bill payment |
| COADM01C | CA01 | `controller/` - Admin user management |

### COBOL Programs to Service Classes

| COBOL Program | Purpose | Spring Service (target) |
|---|---|---|
| COACTVWC | Account view logic | `service/` |
| COCRDLIC | Credit card list logic | `service/` |
| COTRN00C | Transaction list logic | `service/` |
| COBIL00C | Bill payment logic | `service/` |
| CBTRN01C - CBTRN03C | Batch transaction processing | `service/` (via Spring Batch) |

### VSAM Files to JPA Repositories and Entities

| VSAM File | Copybook | JPA Entity (target) | Repository (target) |
|---|---|---|---|
| CUSTFILE | CVCUS01Y | `entity/` - Customer | `repository/` |
| ACCTFILE | CVACT01Y | `entity/` - Account | `repository/` |
| CARDFILE | CVACT02Y | `entity/` - Card | `repository/` |
| XREFFILE | CVACT03Y | `entity/` - CrossReference | `repository/` |
| TRANSACT | CVTRA05Y | `entity/` - Transaction | `repository/` |
| USRSEC | n/a | `entity/` - User | `repository/` (via Spring Security) |

### JCL Jobs to Spring Batch Jobs

| JCL Job | Purpose | Spring Batch Job (target) |
|---|---|---|
| DEFVSAM | Define VSAM files | Database schema migration |
| POSTTRAN | Post daily transactions | `config/` - Batch job config |
| TRANREPT | Transaction reporting | `config/` - Batch job config |
| INTCALC | Interest calculation | `config/` - Batch job config |

## Package Structure

```
com.carddemo
├── CardDemoApplication.java   Main entry point
├── controller/                REST API endpoints (replacing CICS transactions)
├── service/                   Business logic (replacing COBOL programs)
├── repository/                Data access layer (replacing VSAM file I/O)
├── entity/                    JPA entities (replacing COBOL copybook data structures)
├── dto/                       Data Transfer Objects for API request/response
└── config/                    Spring configuration (security, batch, database)
```

## Configuration Profiles

| Profile | Database | Usage |
|---|---|---|
| (default) | H2 in-memory | Local development |
| `prod-postgres` | PostgreSQL | Production with PostgreSQL |
| `prod-mysql` | MySQL | Production with MySQL |

## Original Application

The original COBOL application source code is located in the parent directory under `app/`. Refer to the [main README](../README.md) for details about the mainframe application.
