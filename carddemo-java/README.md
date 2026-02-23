# CardDemo Java 17 Migration

Modern Java 17 / Spring Boot 3.x implementation of the CardDemo mainframe credit card management system, migrated from COBOL/CICS/VSAM/DB2/IMS.

## Architecture Overview

| Mainframe Component | Java Replacement |
|---------------------|-----------------|
| COBOL programs | Java 17 classes |
| CICS online transactions | Spring MVC REST controllers |
| JCL batch jobs | Spring Batch jobs |
| VSAM KSDS/ESDS files | PostgreSQL + Spring Data JPA |
| IMS DB (HIDAM) | PostgreSQL relational tables |
| DB2 embedded SQL | Spring Data JPA repositories |
| IBM MQ | ActiveMQ Artemis + Spring JMS |
| BMS maps (3270 screens) | Thymeleaf templates / REST API |
| RACF security | Spring Security + JWT |
| Copybooks | JPA entity classes |

## Project Structure

```
carddemo-java/
├── pom.xml                          # Parent POM (Spring Boot 3.2.5, Java 17)
├── docker-compose.yml               # PostgreSQL + ActiveMQ Artemis
├── carddemo-core/                   # Domain entities, repositories, exceptions
│   ├── src/main/java/.../domain/    # 13 JPA entity classes
│   ├── src/main/java/.../repository/# 13 Spring Data JPA repositories
│   ├── src/main/java/.../exception/ # Custom exception classes
│   └── src/main/resources/db/migration/  # Flyway SQL migrations
├── carddemo-api/                    # REST API (Spring MVC)
│   ├── src/main/java/.../controller/# 8 REST controllers (24 endpoints)
│   ├── src/main/java/.../dto/       # Request/Response DTOs
│   ├── src/main/java/.../service/   # Business logic services
│   └── src/main/java/.../config/    # Security, OpenAPI config
├── carddemo-batch/                  # Batch processing (Spring Batch)
│   └── src/main/java/.../job/       # 4 batch job configurations
├── carddemo-web/                    # Web UI (Thymeleaf) - Phase 3+
├── carddemo-integration/            # JMS messaging (ActiveMQ Artemis)
│   ├── src/main/java/.../config/    # JMS configuration
│   └── src/main/java/.../jms/       # Message sender/listener
└── carddemo-test/                   # Integration & E2E tests
```

## Prerequisites

- **Java 17** (LTS) - [Download](https://adoptium.net/)
- **Maven 3.9+** - [Download](https://maven.apache.org/)
- **Docker & Docker Compose** - [Download](https://www.docker.com/)
- **PostgreSQL 16** (or use Docker)
- **ActiveMQ Artemis 2.31+** (or use Docker)

## Quick Start

### 1. Start Infrastructure

```bash
cd carddemo-java
docker-compose up -d
```

This starts:
- **PostgreSQL** on port 5432 (user: `carddemo`, password: `carddemo`, database: `carddemo`)
- **ActiveMQ Artemis** on port 61616 (management console: http://localhost:8161, user: `artemis`, password: `artemis`)

### 2. Build the Project

```bash
mvn clean compile
```

### 3. Run the API Application

```bash
cd carddemo-api
mvn spring-boot:run
```

The API will be available at http://localhost:8080

### 4. Access Swagger UI

Open http://localhost:8080/swagger-ui.html for interactive API documentation.

### 5. Run with Development Profile (H2 in-memory database)

```bash
cd carddemo-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:carddemo`)

## REST API Endpoints

| Method | Endpoint | CICS Transaction | Description |
|--------|----------|-----------------|-------------|
| POST | `/api/auth/login` | CC00 (COSGN00C) | User authentication |
| POST | `/api/auth/logout` | CC00 | Session logout |
| GET | `/api/accounts/{id}` | CAVW (COACTVWC) | View account |
| PUT | `/api/accounts/{id}` | CAUP (COACTUPC) | Update account |
| GET | `/api/cards` | CCLI (COCRDLIC) | List cards |
| GET | `/api/cards/{num}` | CCDL (COCRDSLC) | View card |
| PUT | `/api/cards/{num}` | CCUP (COCRDUPC) | Update card |
| GET | `/api/transactions` | CT00 (COTRN00C) | List transactions |
| GET | `/api/transactions/{id}` | CT01 (COTRN01C) | View transaction |
| POST | `/api/transactions` | CT02 (COTRN02C) | Create transaction |
| POST | `/api/payments` | CB00 (COBIL00C) | Bill payment |
| POST | `/api/reports/transactions` | CR00 (CORPT00C) | Transaction report |
| GET | `/api/admin/users` | CU00 (COUSR00C) | List users |
| POST | `/api/admin/users` | CU01 (COUSR01C) | Create user |
| PUT | `/api/admin/users/{id}` | CU02 (COUSR02C) | Update user |
| DELETE | `/api/admin/users/{id}` | CU03 (COUSR03C) | Delete user |
| GET | `/api/admin/transaction-types` | CTLI (COTRTLIC) | List types |
| POST | `/api/admin/transaction-types` | CTTU (COTRTUPC) | Create type |
| PUT | `/api/admin/transaction-types/{code}` | CTTU | Update type |
| DELETE | `/api/admin/transaction-types/{code}` | CTTU | Delete type |

## Batch Jobs

| Spring Batch Job | JCL Job | COBOL Program | Description |
|-----------------|---------|---------------|-------------|
| `transactionPostingJob` | POSTTRAN | CBTRN02C | Post transactions to accounts |
| `interestCalculationJob` | INTCALC | CBACT04C | Calculate interest charges |
| `statementGenerationJob` | CREASTMT | CBSTM03A | Generate account statements |
| `transactionReportJob` | TRANREPT | CBTRN03C | Generate transaction reports |

## Database Schema

The database schema is managed by Flyway migrations in `carddemo-core/src/main/resources/db/migration/`:

- `V1__Initial_Schema.sql` - All table definitions, indexes, and constraints
- `V2__Seed_Reference_Data.sql` - Reference data (transaction types, categories, default admin user)

### Entity-Table Mapping

| JPA Entity | Database Table | VSAM/IMS Source | Copybook |
|-----------|---------------|-----------------|----------|
| Account | account | ACCTDATA.PS | CVACT01Y |
| Card | card | CARDDATA.PS | CVACT02Y |
| Customer | customer | CUSTDATA.PS | CVCUS01Y |
| Transaction | transaction | TRANSACT.VSAM.KSDS | CVTRA05Y |
| UserSecurity | user_security | USRSEC.PS | CSUSR01Y |
| CardXref | card_xref | CARDXREF.PS | CVACT03Y |
| TransactionType | transaction_type | TRANTYPE.PS | CVTRA03Y |
| TransactionCategory | transaction_category | TRANCATG.PS | CVTRA04Y |
| TransactionCategoryBalance | transaction_category_balance | TCATBALF.PS | CVTRA01Y |
| DisclosureGroup | disclosure_group | DISCGRP.PS | CVTRA02Y |
| AuthorizationSummary | authorization_summary | IMS PAUTSUM0 | CIPAUSMY |
| AuthorizationDetail | authorization_detail | IMS PAUTDTL1 | CIPAUDTY |
| AuthFraud | auth_fraud | DB2 AUTHFRDS | - |

## Technology Stack

- **Java 17** (LTS)
- **Spring Boot 3.2.5**
- **Spring Data JPA** + Hibernate
- **Spring Batch 5.x**
- **Spring Security 6.x**
- **Spring JMS** + ActiveMQ Artemis
- **PostgreSQL 16** (production) / H2 (development/test)
- **Flyway** for database migrations
- **SpringDoc OpenAPI 2.3.0** for API documentation
- **Lombok** for reducing boilerplate
- **MapStruct 1.5.5** for DTO mapping
- **Testcontainers** for integration testing

## Migration Phase Status

- **Phase 1: Assessment & Architecture Design** - Complete
  - Inventory analysis (docs/phase1/01-inventory-analysis.md)
  - Architecture design (docs/phase1/02-architecture-design.md)
  - Data migration strategy (docs/phase1/03-data-migration-strategy.md)
  - OpenAPI specification (docs/phase1/04-openapi-specification.yaml)
- **Phase 2: Foundation Setup** - Complete
  - Multi-module Maven project structure
  - JPA entity classes (13 entities)
  - Spring Data JPA repositories (13 repositories)
  - Database schema (Flyway migrations)
  - REST controllers, DTOs, services
  - Spring Batch job configurations
  - JMS/messaging configuration
  - Spring Security configuration
  - Docker Compose environment
- **Phase 3: Core Module Migration** - Planned
- **Phase 4: Batch & Integration** - Planned
- **Phase 5: Testing & Validation** - Planned
- **Phase 6: Deployment & Cutover** - Planned
