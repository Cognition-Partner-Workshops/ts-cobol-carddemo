# CardDemo Migration and Modernization Guide

## From COBOL/CICS Mainframe to Java Spring Boot Microservices with AWS RDS

**Document Version:** 1.0  
**Date:** January 27, 2026  
**Source Application:** CardDemo - Mainframe Credit Card Management System  
**Target Architecture:** Java 21, Spring Boot 3.x, Microservices, AWS RDS (PostgreSQL)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current State Analysis](#2-current-state-analysis)
3. [Target Architecture Overview](#3-target-architecture-overview)
4. [Migration Strategy](#4-migration-strategy)
5. [Microservices Design](#5-microservices-design)
6. [Data Migration](#6-data-migration)
7. [API Design](#7-api-design)
8. [Security Architecture](#8-security-architecture)
9. [Batch Processing Migration](#9-batch-processing-migration)
10. [Infrastructure and Deployment](#10-infrastructure-and-deployment)
11. [Testing Strategy](#11-testing-strategy)
12. [Migration Phases and Timeline](#12-migration-phases-and-timeline)
13. [Risk Assessment and Mitigation](#13-risk-assessment-and-mitigation)
14. [Appendices](#14-appendices)

---

## 1. Executive Summary

### 1.1 Purpose

This document provides a comprehensive guide for migrating the CardDemo mainframe application from its current COBOL/CICS/VSAM architecture to a modern Java Spring Boot microservices architecture running on AWS with RDS as the database layer.

### 1.2 Migration Objectives

The primary objectives of this modernization initiative are:

**Business Objectives:**
- Reduce operational costs by eliminating mainframe licensing and maintenance fees
- Improve time-to-market for new features through agile development practices
- Enable integration with modern digital channels (mobile, web, APIs)
- Enhance scalability to handle growing transaction volumes
- Improve developer productivity and attract modern talent

**Technical Objectives:**
- Replace COBOL business logic with Java Spring Boot services
- Migrate VSAM files to AWS RDS PostgreSQL relational database
- Transform batch JCL jobs to Spring Batch or scheduled microservices
- Implement RESTful APIs for all business functions
- Enable cloud-native deployment with containerization (Docker/Kubernetes)
- Implement modern security standards (OAuth 2.0, JWT)

### 1.3 Migration Approach Summary

We recommend a **Strangler Fig Pattern** combined with **Domain-Driven Design (DDD)** for this migration. This approach allows incremental migration of functionality while maintaining system stability and enabling parallel operation during the transition period.

---

## 2. Current State Analysis

### 2.1 Technology Stack Summary

| Component | Current Technology | Target Technology |
|-----------|-------------------|-------------------|
| Programming Language | COBOL | Java 21 |
| Application Framework | CICS | Spring Boot 3.x |
| Database | VSAM KSDS/AIX | AWS RDS PostgreSQL |
| Batch Processing | JCL/COBOL | Spring Batch |
| Message Queue | IBM MQ (optional) | Amazon SQS/SNS |
| User Interface | 3270 BMS Maps | React/Angular SPA + REST APIs |
| Authentication | RACF | Spring Security + OAuth 2.0/JWT |
| Job Scheduling | Control-M/CA7 | AWS EventBridge + Step Functions |

### 2.2 Application Inventory

#### Online Programs (CICS)

| Program | Transaction | Function | Target Microservice |
|---------|-------------|----------|---------------------|
| COSGN00C | CC00 | User Sign-On | auth-service |
| COMEN01C | CM00 | Main Menu | gateway-service |
| COADM01C | CA00 | Admin Menu | gateway-service |
| COACTVWC | CAVW | Account View | account-service |
| COACTUPC | CAUP | Account Update | account-service |
| COCRDLIC | CCLI | Card List | card-service |
| COCRDSLC | CCDL | Card View | card-service |
| COCRDUPC | CCUP | Card Update | card-service |
| COTRN00C | CT00 | Transaction List | transaction-service |
| COTRN01C | CT01 | Transaction View | transaction-service |
| COTRN02C | CT02 | Transaction Add | transaction-service |
| CORPT00C | CR00 | Reports | reporting-service |
| COBIL00C | CB00 | Bill Payment | payment-service |
| COUSR00C | CU00 | User List | user-service |
| COUSR01C | CU01 | User Add | user-service |
| COUSR02C | CU02 | User Update | user-service |
| COUSR03C | CU03 | User Delete | user-service |

#### Batch Programs (JCL)

| Program | Job | Function | Target Implementation |
|---------|-----|----------|----------------------|
| CBTRN02C | POSTTRAN | Transaction Posting | transaction-batch-service |
| CBACT04C | INTCALC | Interest Calculation | interest-batch-service |
| CBSTM03A/B | CREASTMT | Statement Generation | statement-batch-service |
| CBTRN03C | TRANREPT | Transaction Reports | reporting-service |
| CBEXPORT | EXPORT | Data Export | data-migration-service |
| CBIMPORT | IMPORT | Data Import | data-migration-service |

### 2.3 Data Files Inventory

| VSAM File | Description | Record Length | Target Table |
|-----------|-------------|---------------|--------------|
| CUSTDATA | Customer Master | 500 bytes | customers |
| ACCTDATA | Account Master | 300 bytes | accounts |
| CARDDATA | Card Master | 150 bytes | cards |
| CARDXREF | Card Cross-Reference | 50 bytes | card_account_xref |
| TRANSACT | Transaction Master | 350 bytes | transactions |
| USRSEC | User Security | 80 bytes | users |
| DALYTRAN | Daily Transactions | 350 bytes | daily_transactions |
| DISCGRP | Disclosure Groups | 50 bytes | disclosure_groups |
| TCATBALF | Category Balance | 50 bytes | transaction_category_balances |
| TRANTYPE | Transaction Types | 60 bytes | transaction_types |
| TRANCATG | Transaction Categories | 60 bytes | transaction_categories |

---

## 3. Target Architecture Overview

### 3.1 High-Level Architecture Diagram

```
                                    ┌─────────────────────────────────────────────────────────────┐
                                    │                        AWS Cloud                             │
                                    │  ┌─────────────────────────────────────────────────────────┐│
                                    │  │                    VPC                                   ││
┌──────────────┐                    │  │                                                          ││
│   Web App    │◄──────────────────►│  │  ┌─────────────────────────────────────────────────┐   ││
│  (React/     │        HTTPS       │  │  │              API Gateway (Kong/AWS)              │   ││
│   Angular)   │                    │  │  └─────────────────────────────────────────────────┘   ││
└──────────────┘                    │  │                          │                              ││
                                    │  │                          ▼                              ││
┌──────────────┐                    │  │  ┌─────────────────────────────────────────────────┐   ││
│  Mobile App  │◄──────────────────►│  │  │           Application Load Balancer             │   ││
│  (iOS/       │        HTTPS       │  │  └─────────────────────────────────────────────────┘   ││
│   Android)   │                    │  │                          │                              ││
└──────────────┘                    │  │          ┌───────────────┼───────────────┐              ││
                                    │  │          ▼               ▼               ▼              ││
┌──────────────┐                    │  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       ││
│   Partner    │◄──────────────────►│  │  │   auth-     │ │  account-   │ │   card-     │       ││
│    APIs      │        HTTPS       │  │  │   service   │ │   service   │ │   service   │       ││
└──────────────┘                    │  │  └─────────────┘ └─────────────┘ └─────────────┘       ││
                                    │  │          │               │               │              ││
                                    │  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       ││
                                    │  │  │ transaction │ │  payment-   │ │  reporting- │       ││
                                    │  │  │   service   │ │   service   │ │   service   │       ││
                                    │  │  └─────────────┘ └─────────────┘ └─────────────┘       ││
                                    │  │          │               │               │              ││
                                    │  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       ││
                                    │  │  │   user-     │ │   batch-    │ │  customer-  │       ││
                                    │  │  │   service   │ │   service   │ │   service   │       ││
                                    │  │  └─────────────┘ └─────────────┘ └─────────────┘       ││
                                    │  │                          │                              ││
                                    │  │          ┌───────────────┴───────────────┐              ││
                                    │  │          ▼                               ▼              ││
                                    │  │  ┌─────────────────────┐   ┌─────────────────────┐     ││
                                    │  │  │    AWS RDS          │   │    Amazon           │     ││
                                    │  │  │    PostgreSQL       │   │    ElastiCache      │     ││
                                    │  │  │    (Multi-AZ)       │   │    (Redis)          │     ││
                                    │  │  └─────────────────────┘   └─────────────────────┘     ││
                                    │  │                                                          ││
                                    │  │  ┌─────────────────────┐   ┌─────────────────────┐     ││
                                    │  │  │    Amazon SQS       │   │    Amazon S3        │     ││
                                    │  │  │    (Message Queue)  │   │    (File Storage)   │     ││
                                    │  │  └─────────────────────┘   └─────────────────────┘     ││
                                    │  │                                                          ││
                                    │  └──────────────────────────────────────────────────────────┘│
                                    └─────────────────────────────────────────────────────────────┘
```

### 3.2 Technology Stack

#### Core Framework
- **Java 21** (LTS) with Virtual Threads for improved concurrency
- **Spring Boot 3.2+** for microservices framework
- **Spring Cloud** for distributed systems patterns
- **Spring Data JPA** for database access
- **Spring Security** for authentication/authorization
- **Spring Batch** for batch processing

#### Database
- **AWS RDS PostgreSQL 15+** as primary database
- **Amazon ElastiCache (Redis)** for caching and session management
- **Flyway** for database migrations

#### Messaging
- **Amazon SQS** for asynchronous messaging
- **Amazon SNS** for event notifications
- **Amazon EventBridge** for event-driven architecture

#### Infrastructure
- **Amazon EKS** (Kubernetes) for container orchestration
- **Docker** for containerization
- **AWS ALB** for load balancing
- **Amazon API Gateway** or **Kong** for API management

#### Observability
- **Amazon CloudWatch** for logging and metrics
- **AWS X-Ray** for distributed tracing
- **Prometheus + Grafana** for monitoring dashboards

#### CI/CD
- **AWS CodePipeline** or **GitHub Actions** for CI/CD
- **AWS CodeBuild** for build automation
- **Amazon ECR** for container registry

---

## 4. Migration Strategy

### 4.1 Recommended Approach: Strangler Fig Pattern

The Strangler Fig Pattern allows gradual migration by routing traffic between legacy and modern systems. This approach minimizes risk and allows for incremental validation.

```
Phase 1: Coexistence
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                               │
│                            │                                     │
│              ┌─────────────┴─────────────┐                      │
│              ▼                           ▼                      │
│     ┌─────────────────┐         ┌─────────────────┐            │
│     │   New Spring    │         │    Legacy       │            │
│     │   Boot Services │         │    Mainframe    │            │
│     └────────┬────────┘         └────────┬────────┘            │
│              │                           │                      │
│              ▼                           ▼                      │
│     ┌─────────────────┐         ┌─────────────────┐            │
│     │   AWS RDS       │◄───────►│    VSAM Files   │            │
│     │   PostgreSQL    │  Sync   │                 │            │
│     └─────────────────┘         └─────────────────┘            │
└─────────────────────────────────────────────────────────────────┘

Phase 2: Complete Migration
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                               │
│                            │                                     │
│                            ▼                                     │
│                   ┌─────────────────┐                           │
│                   │   Spring Boot   │                           │
│                   │   Microservices │                           │
│                   └────────┬────────┘                           │
│                            │                                     │
│                            ▼                                     │
│                   ┌─────────────────┐                           │
│                   │   AWS RDS       │                           │
│                   │   PostgreSQL    │                           │
│                   └─────────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 Migration Principles

1. **Incremental Migration**: Migrate one bounded context at a time
2. **Data First**: Establish data synchronization before migrating logic
3. **Feature Parity**: Ensure functional equivalence before cutover
4. **Parallel Running**: Run both systems in parallel during transition
5. **Rollback Capability**: Maintain ability to rollback at each phase
6. **Automated Testing**: Comprehensive test coverage before migration

### 4.3 Domain-Driven Design Bounded Contexts

Based on the CardDemo functional analysis, we identify the following bounded contexts:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        CardDemo Domain Model                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │   Identity &     │  │    Customer      │  │     Account      │      │
│  │   Access Context │  │     Context      │  │     Context      │      │
│  │                  │  │                  │  │                  │      │
│  │  - User          │  │  - Customer      │  │  - Account       │      │
│  │  - Role          │  │  - Address       │  │  - Balance       │      │
│  │  - Permission    │  │  - Contact       │  │  - Credit Limit  │      │
│  │  - Session       │  │                  │  │  - Cycle Data    │      │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘      │
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │      Card        │  │   Transaction    │  │     Payment      │      │
│  │     Context      │  │     Context      │  │     Context      │      │
│  │                  │  │                  │  │                  │      │
│  │  - Card          │  │  - Transaction   │  │  - Bill Payment  │      │
│  │  - CardStatus    │  │  - TranType      │  │  - Payment       │      │
│  │  - Expiration    │  │  - TranCategory  │  │    Confirmation  │      │
│  │                  │  │  - Merchant      │  │                  │      │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘      │
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐                             │
│  │    Reporting     │  │      Batch       │                             │
│  │     Context      │  │     Context      │                             │
│  │                  │  │                  │                             │
│  │  - Report        │  │  - Interest Calc │                             │
│  │  - Statement     │  │  - Statement Gen │                             │
│  │  - DateRange     │  │  - Trans Posting │                             │
│  └──────────────────┘  └──────────────────┘                             │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Microservices Design

### 5.1 Service Catalog

| Service Name | Responsibility | Source Programs | Port |
|--------------|----------------|-----------------|------|
| auth-service | Authentication, Authorization, Session Management | COSGN00C | 8081 |
| user-service | User CRUD Operations | COUSR00C-03C | 8082 |
| customer-service | Customer Profile Management | CVCUS01Y data | 8083 |
| account-service | Account Management | COACTVWC, COACTUPC | 8084 |
| card-service | Card Lifecycle Management | COCRDLIC, COCRDSLC, COCRDUPC | 8085 |
| transaction-service | Transaction Processing | COTRN00C-02C | 8086 |
| payment-service | Bill Payment Processing | COBIL00C | 8087 |
| reporting-service | Report Generation | CORPT00C, CBTRN03C | 8088 |
| batch-service | Batch Job Orchestration | CBTRN02C, CBACT04C, CBSTM03A | 8089 |
| gateway-service | API Gateway, Routing | COMEN01C, COADM01C | 8080 |

### 5.2 Service Architecture Template

Each microservice follows a consistent layered architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    Microservice Structure                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                   Controller Layer                      │ │
│  │  - REST Controllers (@RestController)                   │ │
│  │  - Request/Response DTOs                                │ │
│  │  - Input Validation (@Valid)                            │ │
│  │  - Exception Handling (@ControllerAdvice)               │ │
│  └────────────────────────────────────────────────────────┘ │
│                            │                                 │
│                            ▼                                 │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    Service Layer                        │ │
│  │  - Business Logic (@Service)                            │ │
│  │  - Transaction Management (@Transactional)              │ │
│  │  - Domain Events                                        │ │
│  │  - Validation Rules                                     │ │
│  └────────────────────────────────────────────────────────┘ │
│                            │                                 │
│                            ▼                                 │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                  Repository Layer                       │ │
│  │  - JPA Repositories (@Repository)                       │ │
│  │  - Custom Queries (@Query)                              │ │
│  │  - Entity Mappings (@Entity)                            │ │
│  └────────────────────────────────────────────────────────┘ │
│                            │                                 │
│                            ▼                                 │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                   Database Layer                        │ │
│  │  - AWS RDS PostgreSQL                                   │ │
│  │  - Connection Pooling (HikariCP)                        │ │
│  │  - Flyway Migrations                                    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 Service Specifications

#### 5.3.1 auth-service

**Purpose:** Handle user authentication and authorization

**Key Components:**
```java
// Domain Entities
@Entity
public class User {
    @Id
    private String userId;          // Maps to SEC-USR-ID
    private String firstName;       // Maps to SEC-USR-FNAME
    private String lastName;        // Maps to SEC-USR-LNAME
    private String passwordHash;    // Hashed version of SEC-USR-PWD
    private UserType userType;      // Maps to SEC-USR-TYPE (A/U)
    private boolean active;
    private LocalDateTime lastLogin;
}

// REST Endpoints
POST /api/v1/auth/login          // User authentication
POST /api/v1/auth/logout         // Session termination
POST /api/v1/auth/refresh        // Token refresh
GET  /api/v1/auth/validate       // Token validation
```

**Migration Notes:**
- Replace RACF authentication with Spring Security + JWT
- Implement password hashing (BCrypt) - mainframe stores plain text
- Add token-based session management instead of CICS COMMAREA

#### 5.3.2 account-service

**Purpose:** Manage customer accounts

**Key Components:**
```java
// Domain Entities
@Entity
public class Account {
    @Id
    private Long accountId;              // Maps to ACCT-ID (11 digits)
    private boolean activeStatus;        // Maps to ACCT-ACTIVE-STATUS
    private BigDecimal currentBalance;   // Maps to ACCT-CURR-BAL
    private BigDecimal creditLimit;      // Maps to ACCT-CREDIT-LIMIT
    private BigDecimal cashCreditLimit;  // Maps to ACCT-CASH-CREDIT-LIMIT
    private LocalDate openDate;          // Maps to ACCT-OPEN-DATE
    private LocalDate expirationDate;    // Maps to ACCT-EXPIRAION-DATE
    private LocalDate reissueDate;       // Maps to ACCT-REISSUE-DATE
    private BigDecimal currentCycleCredit;
    private BigDecimal currentCycleDebit;
    private String zipCode;
    private String groupId;
    
    @ManyToOne
    private Customer customer;
}

// REST Endpoints
GET    /api/v1/accounts/{accountId}           // View account (CAVW)
PUT    /api/v1/accounts/{accountId}           // Update account (CAUP)
GET    /api/v1/accounts                       // List accounts
GET    /api/v1/accounts/{accountId}/balance   // Get balance
```

**Migration Notes:**
- Convert COMP-3 packed decimal to BigDecimal
- Convert mainframe date formats (CCYYMMDD) to LocalDate
- Implement optimistic locking for concurrent updates

#### 5.3.3 card-service

**Purpose:** Manage credit cards

**Key Components:**
```java
// Domain Entities
@Entity
public class Card {
    @Id
    private String cardNumber;           // Maps to CARD-NUM (16 chars)
    private Long accountId;              // Maps to CARD-ACCT-ID
    private String cvvCode;              // Maps to CARD-CVV-CD (encrypted)
    private String embossedName;         // Maps to CARD-EMBOSSED-NAME
    private LocalDate expirationDate;    // Maps to CARD-EXPIRAION-DATE
    private boolean activeStatus;        // Maps to CARD-ACTIVE-STATUS
}

@Entity
public class CardAccountXref {
    @Id
    private String cardNumber;           // Maps to XREF-CARD-NUM
    private Long customerId;             // Maps to XREF-CUST-ID
    private Long accountId;              // Maps to XREF-ACCT-ID
}

// REST Endpoints
GET    /api/v1/cards                     // List cards (CCLI)
GET    /api/v1/cards/{cardNumber}        // View card (CCDL)
PUT    /api/v1/cards/{cardNumber}        // Update card (CCUP)
GET    /api/v1/accounts/{accountId}/cards // Cards by account
```

**Migration Notes:**
- Encrypt CVV codes at rest (mainframe stores plain)
- Implement card number masking for display
- Handle VSAM AIX (alternate index) with database indexes

#### 5.3.4 transaction-service

**Purpose:** Process and manage transactions

**Key Components:**
```java
// Domain Entities
@Entity
public class Transaction {
    @Id
    private String transactionId;        // Maps to TRAN-ID (16 chars)
    private String typeCode;             // Maps to TRAN-TYPE-CD
    private Integer categoryCode;        // Maps to TRAN-CAT-CD
    private String source;               // Maps to TRAN-SOURCE
    private String description;          // Maps to TRAN-DESC
    private BigDecimal amount;           // Maps to TRAN-AMT
    private Long merchantId;             // Maps to TRAN-MERCHANT-ID
    private String merchantName;         // Maps to TRAN-MERCHANT-NAME
    private String merchantCity;         // Maps to TRAN-MERCHANT-CITY
    private String merchantZip;          // Maps to TRAN-MERCHANT-ZIP
    private String cardNumber;           // Maps to TRAN-CARD-NUM
    private LocalDateTime originalTimestamp;
    private LocalDateTime processedTimestamp;
}

// REST Endpoints
GET    /api/v1/transactions              // List transactions (CT00)
GET    /api/v1/transactions/{id}         // View transaction (CT01)
POST   /api/v1/transactions              // Add transaction (CT02)
GET    /api/v1/cards/{cardNumber}/transactions // By card
```

**Migration Notes:**
- Generate UUID-based transaction IDs instead of sequential
- Implement idempotency for transaction creation
- Add event publishing for transaction events

#### 5.3.5 payment-service

**Purpose:** Process bill payments

**Key Components:**
```java
// Domain Entities
@Entity
public class BillPayment {
    @Id
    @GeneratedValue
    private Long paymentId;
    private Long accountId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
    private String transactionId;        // Reference to created transaction
}

// REST Endpoints
POST   /api/v1/payments/bill             // Process bill payment (CB00)
GET    /api/v1/accounts/{id}/payments    // Payment history
```

**Migration Notes:**
- Implement saga pattern for payment + transaction creation
- Add payment confirmation workflow
- Implement idempotency keys for duplicate prevention

---

## 6. Data Migration

### 6.1 Database Schema Design

#### 6.1.1 Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│     users       │       │    customers    │       │    accounts     │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ user_id (PK)    │       │ customer_id(PK) │◄──────│ account_id (PK) │
│ first_name      │       │ first_name      │       │ customer_id(FK) │
│ last_name       │       │ middle_name     │       │ active_status   │
│ password_hash   │       │ last_name       │       │ current_balance │
│ user_type       │       │ address_line_1  │       │ credit_limit    │
│ active          │       │ address_line_2  │       │ cash_credit_lim │
│ created_at      │       │ address_line_3  │       │ open_date       │
│ updated_at      │       │ state_code      │       │ expiration_date │
└─────────────────┘       │ country_code    │       │ reissue_date    │
                          │ zip_code        │       │ cycle_credit    │
                          │ phone_1         │       │ cycle_debit     │
                          │ phone_2         │       │ group_id        │
                          │ ssn_encrypted   │       │ created_at      │
                          │ govt_id         │       │ updated_at      │
                          │ date_of_birth   │       └────────┬────────┘
                          │ eft_account_id  │                │
                          │ primary_holder  │                │
                          │ fico_score      │                │
                          │ created_at      │                │
                          │ updated_at      │                │
                          └─────────────────┘                │
                                                             │
┌─────────────────┐       ┌─────────────────┐                │
│      cards      │       │  card_xref      │                │
├─────────────────┤       ├─────────────────┤                │
│ card_number(PK) │◄──────│ card_number(PK) │                │
│ account_id (FK) │───────│ customer_id(FK) │                │
│ cvv_encrypted   │       │ account_id (FK) │────────────────┘
│ embossed_name   │       │ created_at      │
│ expiration_date │       └─────────────────┘
│ active_status   │
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │
         │
         ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  transactions   │       │ transaction_    │       │ transaction_    │
├─────────────────┤       │    types        │       │   categories    │
│ transaction_id  │       ├─────────────────┤       ├─────────────────┤
│   (PK)          │       │ type_code (PK)  │       │ category_code   │
│ type_code (FK)  │───────│ description     │       │   (PK)          │
│ category_code   │───────│ active          │       │ description     │
│   (FK)          │       │ created_at      │       │ active          │
│ source          │       └─────────────────┘       │ created_at      │
│ description     │                                 └─────────────────┘
│ amount          │
│ merchant_id     │       ┌─────────────────┐
│ merchant_name   │       │ disclosure_     │
│ merchant_city   │       │    groups       │
│ merchant_zip    │       ├─────────────────┤
│ card_number(FK) │       │ group_id (PK)   │
│ original_ts     │       │ type_code       │
│ processed_ts    │       │ category_code   │
│ created_at      │       │ interest_rate   │
└─────────────────┘       │ created_at      │
                          └─────────────────┘
```

#### 6.1.2 DDL Scripts

```sql
-- Users table (from USRSEC)
CREATE TABLE users (
    user_id VARCHAR(8) PRIMARY KEY,
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type CHAR(1) NOT NULL CHECK (user_type IN ('A', 'U')),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Customers table (from CUSTDATA)
CREATE TABLE customers (
    customer_id BIGINT PRIMARY KEY,
    first_name VARCHAR(25) NOT NULL,
    middle_name VARCHAR(25),
    last_name VARCHAR(25) NOT NULL,
    address_line_1 VARCHAR(50),
    address_line_2 VARCHAR(50),
    address_line_3 VARCHAR(50),
    state_code CHAR(2),
    country_code CHAR(3),
    zip_code VARCHAR(10),
    phone_1 VARCHAR(15),
    phone_2 VARCHAR(15),
    ssn_encrypted VARCHAR(255),
    govt_id VARCHAR(20),
    date_of_birth DATE,
    eft_account_id VARCHAR(10),
    primary_holder CHAR(1),
    fico_score INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Accounts table (from ACCTDATA)
CREATE TABLE accounts (
    account_id BIGINT PRIMARY KEY,
    customer_id BIGINT REFERENCES customers(customer_id),
    active_status BOOLEAN DEFAULT true,
    current_balance DECIMAL(12,2) DEFAULT 0,
    credit_limit DECIMAL(12,2) DEFAULT 0,
    cash_credit_limit DECIMAL(12,2) DEFAULT 0,
    open_date DATE,
    expiration_date DATE,
    reissue_date DATE,
    current_cycle_credit DECIMAL(12,2) DEFAULT 0,
    current_cycle_debit DECIMAL(12,2) DEFAULT 0,
    zip_code VARCHAR(10),
    group_id VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Cards table (from CARDDATA)
CREATE TABLE cards (
    card_number VARCHAR(16) PRIMARY KEY,
    account_id BIGINT REFERENCES accounts(account_id),
    cvv_encrypted VARCHAR(255),
    embossed_name VARCHAR(50),
    expiration_date DATE,
    active_status BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Card cross-reference table (from CARDXREF)
CREATE TABLE card_account_xref (
    card_number VARCHAR(16) PRIMARY KEY REFERENCES cards(card_number),
    customer_id BIGINT REFERENCES customers(customer_id),
    account_id BIGINT REFERENCES accounts(account_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transactions table (from TRANSACT)
CREATE TABLE transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    type_code VARCHAR(2) NOT NULL,
    category_code INTEGER NOT NULL,
    source VARCHAR(10),
    description VARCHAR(100),
    amount DECIMAL(11,2) NOT NULL,
    merchant_id BIGINT,
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(50),
    merchant_zip VARCHAR(10),
    card_number VARCHAR(16) REFERENCES cards(card_number),
    original_timestamp TIMESTAMP,
    processed_timestamp TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transaction types reference table (from TRANTYPE)
CREATE TABLE transaction_types (
    type_code VARCHAR(2) PRIMARY KEY,
    description VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transaction categories reference table (from TRANCATG)
CREATE TABLE transaction_categories (
    category_code INTEGER PRIMARY KEY,
    description VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Disclosure groups (from DISCGRP)
CREATE TABLE disclosure_groups (
    group_id VARCHAR(10),
    type_code VARCHAR(2),
    category_code INTEGER,
    interest_rate DECIMAL(5,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, type_code, category_code)
);

-- Transaction category balances (from TCATBALF)
CREATE TABLE transaction_category_balances (
    account_id BIGINT REFERENCES accounts(account_id),
    type_code VARCHAR(2),
    category_code INTEGER,
    balance DECIMAL(12,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, type_code, category_code)
);

-- Indexes for performance
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_cards_account ON cards(account_id);
CREATE INDEX idx_transactions_card ON transactions(card_number);
CREATE INDEX idx_transactions_timestamp ON transactions(original_timestamp);
CREATE INDEX idx_xref_customer ON card_account_xref(customer_id);
CREATE INDEX idx_xref_account ON card_account_xref(account_id);
```

### 6.2 Data Migration Process

#### 6.2.1 Migration Steps

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Data Migration Pipeline                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Step 1: Extract from VSAM                                              │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  - Use existing CBEXPORT program to create EBCDIC export file   │   │
│  │  - Or use AWS Mainframe Modernization File Transfer             │   │
│  │  - Convert EBCDIC to ASCII/UTF-8                                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  Step 2: Transform Data                                                 │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  - Convert COMP-3 packed decimal to standard decimal            │   │
│  │  - Convert mainframe dates (CCYYMMDD) to ISO format             │   │
│  │  - Hash passwords (BCrypt)                                      │   │
│  │  - Encrypt sensitive data (SSN, CVV)                            │   │
│  │  - Generate UUIDs for new transaction IDs                       │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  Step 3: Load to PostgreSQL                                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  - Disable foreign key constraints                              │   │
│  │  - Bulk load reference data first (types, categories)           │   │
│  │  - Load master data (customers, accounts, cards)                │   │
│  │  - Load transactional data (transactions)                       │   │
│  │  - Re-enable constraints and validate                           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  Step 4: Validate                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  - Record count reconciliation                                  │   │
│  │  - Checksum validation on key fields                            │   │
│  │  - Business rule validation                                     │   │
│  │  - Sample data verification                                     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 6.2.2 Data Type Mapping

| COBOL Type | Example | PostgreSQL Type | Notes |
|------------|---------|-----------------|-------|
| PIC 9(n) | PIC 9(11) | BIGINT | For IDs |
| PIC 9(n)V99 | PIC S9(10)V99 | DECIMAL(12,2) | For amounts |
| PIC X(n) | PIC X(16) | VARCHAR(n) | For strings |
| PIC X(10) date | CCYYMMDD | DATE | Convert format |
| PIC X(26) timestamp | ISO format | TIMESTAMP | Convert format |
| COMP-3 | Packed decimal | DECIMAL | Unpack first |

#### 6.2.3 Sample Migration Script (Java)

```java
@Service
public class DataMigrationService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void migrateCustomers(Path exportFile) {
        try (BufferedReader reader = Files.newBufferedReader(exportFile)) {
            String line;
            List<Customer> batch = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("C")) { // Customer record type
                    Customer customer = parseCustomerRecord(line);
                    batch.add(customer);
                    
                    if (batch.size() >= 1000) {
                        saveBatch(batch);
                        batch.clear();
                    }
                }
            }
            
            if (!batch.isEmpty()) {
                saveBatch(batch);
            }
        }
    }
    
    private Customer parseCustomerRecord(String record) {
        // Parse fixed-width COBOL record
        Customer customer = new Customer();
        customer.setCustomerId(Long.parseLong(record.substring(1, 10).trim()));
        customer.setFirstName(record.substring(10, 35).trim());
        customer.setMiddleName(record.substring(35, 60).trim());
        customer.setLastName(record.substring(60, 85).trim());
        // ... continue parsing other fields
        
        // Convert mainframe date format
        String dobString = record.substring(200, 210).trim();
        if (!dobString.isEmpty()) {
            customer.setDateOfBirth(parseMainframeDate(dobString));
        }
        
        // Encrypt SSN
        String ssn = record.substring(180, 189).trim();
        customer.setSsnEncrypted(encryptionService.encrypt(ssn));
        
        return customer;
    }
    
    private LocalDate parseMainframeDate(String dateStr) {
        // Format: YYYY-MM-DD from mainframe
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(dateStr, formatter);
    }
}
```

---

## 7. API Design

### 7.1 API Standards

All APIs follow RESTful design principles with the following standards:

- **Base URL**: `https://api.carddemo.com/api/v1`
- **Authentication**: Bearer token (JWT)
- **Content-Type**: `application/json`
- **Versioning**: URL path versioning (`/v1/`, `/v2/`)
- **Pagination**: Offset-based with `page` and `size` parameters
- **Error Format**: RFC 7807 Problem Details

### 7.2 API Specifications

#### 7.2.1 Authentication API

```yaml
openapi: 3.0.3
info:
  title: CardDemo Auth API
  version: 1.0.0

paths:
  /api/v1/auth/login:
    post:
      summary: User Login
      description: Authenticate user and return JWT token
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - userId
                - password
              properties:
                userId:
                  type: string
                  maxLength: 8
                  example: "ADMIN001"
                password:
                  type: string
                  maxLength: 8
                  example: "PASSWORD"
      responses:
        '200':
          description: Successful authentication
          content:
            application/json:
              schema:
                type: object
                properties:
                  accessToken:
                    type: string
                  refreshToken:
                    type: string
                  tokenType:
                    type: string
                    example: "Bearer"
                  expiresIn:
                    type: integer
                    example: 3600
                  userType:
                    type: string
                    enum: [ADMIN, USER]
        '401':
          description: Invalid credentials
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'
```

#### 7.2.2 Account API

```yaml
paths:
  /api/v1/accounts/{accountId}:
    get:
      summary: Get Account Details
      description: Retrieve account information (maps to CAVW transaction)
      security:
        - bearerAuth: []
      parameters:
        - name: accountId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Account details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Account'
        '404':
          description: Account not found
          
    put:
      summary: Update Account
      description: Update account information (maps to CAUP transaction)
      security:
        - bearerAuth: []
      parameters:
        - name: accountId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AccountUpdate'
      responses:
        '200':
          description: Account updated
        '400':
          description: Validation error

components:
  schemas:
    Account:
      type: object
      properties:
        accountId:
          type: integer
          format: int64
          example: 12345678901
        customerId:
          type: integer
          format: int64
        activeStatus:
          type: boolean
        currentBalance:
          type: number
          format: decimal
          example: 1500.50
        creditLimit:
          type: number
          format: decimal
          example: 10000.00
        cashCreditLimit:
          type: number
          format: decimal
          example: 2000.00
        openDate:
          type: string
          format: date
        expirationDate:
          type: string
          format: date
        reissueDate:
          type: string
          format: date
        currentCycleCredit:
          type: number
          format: decimal
        currentCycleDebit:
          type: number
          format: decimal
        groupId:
          type: string
        customer:
          $ref: '#/components/schemas/Customer'
```

#### 7.2.3 Transaction API

```yaml
paths:
  /api/v1/transactions:
    get:
      summary: List Transactions
      description: Get paginated list of transactions (maps to CT00)
      security:
        - bearerAuth: []
      parameters:
        - name: cardNumber
          in: query
          schema:
            type: string
        - name: startDate
          in: query
          schema:
            type: string
            format: date
        - name: endDate
          in: query
          schema:
            type: string
            format: date
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 10
      responses:
        '200':
          description: Transaction list
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/Transaction'
                  totalElements:
                    type: integer
                  totalPages:
                    type: integer
                  
    post:
      summary: Create Transaction
      description: Add new transaction (maps to CT02)
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TransactionCreate'
      responses:
        '201':
          description: Transaction created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Transaction'
```

### 7.3 Error Handling

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotFound(
            AccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "Account ID NOT found..."  // Matches mainframe message
        );
        problem.setTitle("Account Not Found");
        problem.setProperty("accountId", ex.getAccountId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            ValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Validation Error");
        problem.setProperty("errors", ex.getErrors());
        return ResponseEntity.badRequest().body(problem);
    }
}
```

---

## 8. Security Architecture

### 8.1 Authentication Flow

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Client  │     │ API Gateway  │     │ Auth Service │     │   Database   │
└────┬─────┘     └──────┬───────┘     └──────┬───────┘     └──────┬───────┘
     │                  │                    │                    │
     │  POST /login     │                    │                    │
     │  {userId, pwd}   │                    │                    │
     │─────────────────►│                    │                    │
     │                  │  Forward request   │                    │
     │                  │───────────────────►│                    │
     │                  │                    │  Validate user     │
     │                  │                    │───────────────────►│
     │                  │                    │◄───────────────────│
     │                  │                    │                    │
     │                  │                    │  Verify password   │
     │                  │                    │  (BCrypt compare)  │
     │                  │                    │                    │
     │                  │  JWT Token         │                    │
     │                  │◄───────────────────│                    │
     │  {accessToken,   │                    │                    │
     │   refreshToken}  │                    │                    │
     │◄─────────────────│                    │                    │
     │                  │                    │                    │
     │  GET /accounts   │                    │                    │
     │  Authorization:  │                    │                    │
     │  Bearer <token>  │                    │                    │
     │─────────────────►│                    │                    │
     │                  │  Validate JWT      │                    │
     │                  │───────────────────►│                    │
     │                  │  Valid             │                    │
     │                  │◄───────────────────│                    │
     │                  │                    │                    │
     │                  │  Forward to        │                    │
     │                  │  account-service   │                    │
     │                  │                    │                    │
```

### 8.2 Security Implementation

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/**").authenticated()
            )
            .oauth2ResourceServer(oauth2 -> 
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(
                    jwtAuthenticationConverter())))
            .build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = 
            new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtConverter = 
            new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}
```

### 8.3 Role-Based Access Control

| Role | Permissions | Mainframe Equivalent |
|------|-------------|---------------------|
| ROLE_USER | View/update own accounts, cards, transactions | User Type 'U' |
| ROLE_ADMIN | All user permissions + user management | User Type 'A' |

```java
@Service
public class AuthorizationService {
    
    public boolean canAccessAccount(Long accountId, Authentication auth) {
        if (hasRole(auth, "ADMIN")) {
            return true;
        }
        
        // Regular users can only access their own accounts
        String userId = auth.getName();
        return accountRepository.existsByAccountIdAndUserId(accountId, userId);
    }
    
    public boolean canAccessCard(String cardNumber, Authentication auth) {
        if (hasRole(auth, "ADMIN")) {
            return true;
        }
        
        // Regular users can only see cards linked to their accounts
        String userId = auth.getName();
        return cardRepository.existsByCardNumberAndUserId(cardNumber, userId);
    }
}
```

---

## 9. Batch Processing Migration

### 9.1 Spring Batch Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Spring Batch Job Architecture                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                         Job Launcher                             │   │
│  │  (Triggered by: Scheduler, REST API, or Event)                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                            Job                                   │   │
│  │  (e.g., TransactionPostingJob, InterestCalculationJob)          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│              ┌───────────────┼───────────────┐                          │
│              ▼               ▼               ▼                          │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐           │
│  │     Step 1      │ │     Step 2      │ │     Step 3      │           │
│  │   (Validate)    │ │   (Process)     │ │   (Report)      │           │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘           │
│          │                   │                   │                      │
│          ▼                   ▼                   ▼                      │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐           │
│  │  ItemReader     │ │  ItemReader     │ │  ItemReader     │           │
│  │  ItemProcessor  │ │  ItemProcessor  │ │  ItemProcessor  │           │
│  │  ItemWriter     │ │  ItemWriter     │ │  ItemWriter     │           │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 9.2 Batch Job Mapping

#### 9.2.1 Transaction Posting Job (POSTTRAN → TransactionPostingJob)

```java
@Configuration
@EnableBatchProcessing
public class TransactionPostingJobConfig {
    
    @Bean
    public Job transactionPostingJob(JobRepository jobRepository,
            Step validateStep, Step postStep, Step reportStep) {
        return new JobBuilder("transactionPostingJob", jobRepository)
            .start(validateStep)
            .next(postStep)
            .next(reportStep)
            .listener(jobCompletionListener())
            .build();
    }
    
    @Bean
    public Step postStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("postStep", jobRepository)
            .<DailyTransaction, Transaction>chunk(100, transactionManager)
            .reader(dailyTransactionReader())
            .processor(transactionProcessor())
            .writer(transactionWriter())
            .faultTolerant()
            .skipLimit(100)
            .skip(ValidationException.class)
            .listener(skipListener())
            .build();
    }
    
    @Bean
    public ItemProcessor<DailyTransaction, Transaction> transactionProcessor() {
        return dailyTran -> {
            // Validate card exists (maps to XREF file lookup)
            CardAccountXref xref = xrefRepository
                .findByCardNumber(dailyTran.getCardNumber())
                .orElseThrow(() -> new ValidationException(
                    "Card not found: " + dailyTran.getCardNumber()));
            
            // Validate account exists (maps to ACCTDAT lookup)
            Account account = accountRepository
                .findById(xref.getAccountId())
                .orElseThrow(() -> new ValidationException(
                    "Account not found: " + xref.getAccountId()));
            
            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setTransactionId(UUID.randomUUID().toString());
            transaction.setTypeCode(dailyTran.getTypeCode());
            transaction.setCategoryCode(dailyTran.getCategoryCode());
            transaction.setAmount(dailyTran.getAmount());
            transaction.setCardNumber(dailyTran.getCardNumber());
            transaction.setProcessedTimestamp(LocalDateTime.now());
            
            // Update account balance
            account.setCurrentBalance(
                account.getCurrentBalance().add(dailyTran.getAmount()));
            accountRepository.save(account);
            
            return transaction;
        };
    }
}
```

#### 9.2.2 Interest Calculation Job (INTCALC → InterestCalculationJob)

```java
@Configuration
public class InterestCalculationJobConfig {
    
    @Bean
    public Job interestCalculationJob(JobRepository jobRepository,
            Step calculateInterestStep) {
        return new JobBuilder("interestCalculationJob", jobRepository)
            .start(calculateInterestStep)
            .build();
    }
    
    @Bean
    public Step calculateInterestStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("calculateInterestStep", jobRepository)
            .<TransactionCategoryBalance, InterestTransaction>chunk(100, 
                transactionManager)
            .reader(categoryBalanceReader())
            .processor(interestProcessor())
            .writer(interestWriter())
            .build();
    }
    
    @Bean
    public ItemProcessor<TransactionCategoryBalance, InterestTransaction> 
            interestProcessor() {
        return balance -> {
            // Get disclosure group for interest rate
            DisclosureGroup discGroup = disclosureGroupRepository
                .findByGroupIdAndTypeCodeAndCategoryCode(
                    balance.getAccount().getGroupId(),
                    balance.getTypeCode(),
                    balance.getCategoryCode())
                .orElse(null);
            
            if (discGroup == null || balance.getBalance().compareTo(
                    BigDecimal.ZERO) <= 0) {
                return null; // Skip
            }
            
            // Calculate monthly interest: (Balance * Rate) / 12
            BigDecimal monthlyInterest = balance.getBalance()
                .multiply(discGroup.getInterestRate())
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            
            // Create interest transaction
            InterestTransaction interestTran = new InterestTransaction();
            interestTran.setAccountId(balance.getAccountId());
            interestTran.setAmount(monthlyInterest);
            interestTran.setTypeCode("IN");
            interestTran.setCategoryCode(balance.getCategoryCode());
            
            return interestTran;
        };
    }
}
```

#### 9.2.3 Statement Generation Job (CREASTMT → StatementGenerationJob)

```java
@Configuration
public class StatementGenerationJobConfig {
    
    @Bean
    public Job statementGenerationJob(JobRepository jobRepository,
            Step generateStatementStep) {
        return new JobBuilder("statementGenerationJob", jobRepository)
            .start(generateStatementStep)
            .build();
    }
    
    @Bean
    public Step generateStatementStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("generateStatementStep", jobRepository)
            .<Account, Statement>chunk(50, transactionManager)
            .reader(accountReader())
            .processor(statementProcessor())
            .writer(compositeStatementWriter())
            .build();
    }
    
    @Bean
    public CompositeItemWriter<Statement> compositeStatementWriter() {
        CompositeItemWriter<Statement> writer = new CompositeItemWriter<>();
        writer.setDelegates(Arrays.asList(
            pdfStatementWriter(),    // Generate PDF
            htmlStatementWriter(),   // Generate HTML
            s3StatementWriter()      // Upload to S3
        ));
        return writer;
    }
}
```

### 9.3 Job Scheduling

```java
@Configuration
@EnableScheduling
public class BatchSchedulerConfig {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job transactionPostingJob;
    
    @Autowired
    private Job interestCalculationJob;
    
    @Autowired
    private Job statementGenerationJob;
    
    // Daily at 2:00 AM - Transaction Posting
    @Scheduled(cron = "0 0 2 * * *")
    public void runTransactionPosting() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLocalDateTime("runTime", LocalDateTime.now())
            .toJobParameters();
        jobLauncher.run(transactionPostingJob, params);
    }
    
    // Monthly on 1st at 3:00 AM - Interest Calculation
    @Scheduled(cron = "0 0 3 1 * *")
    public void runInterestCalculation() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLocalDateTime("runTime", LocalDateTime.now())
            .toJobParameters();
        jobLauncher.run(interestCalculationJob, params);
    }
    
    // Monthly on 1st at 5:00 AM - Statement Generation
    @Scheduled(cron = "0 0 5 1 * *")
    public void runStatementGeneration() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLocalDateTime("runTime", LocalDateTime.now())
            .toJobParameters();
        jobLauncher.run(statementGenerationJob, params);
    }
}
```

---

## 10. Infrastructure and Deployment

### 10.1 AWS Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              AWS Region                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                         Route 53                                 │   │
│  │                    (DNS Management)                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      CloudFront                                  │   │
│  │                   (CDN for Static Assets)                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    API Gateway                                   │   │
│  │              (Rate Limiting, Throttling)                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│  ┌───────────────────────────┴───────────────────────────┐              │
│  │                         VPC                            │              │
│  │  ┌─────────────────────────────────────────────────┐  │              │
│  │  │              Public Subnets                      │  │              │
│  │  │  ┌─────────────────────────────────────────┐    │  │              │
│  │  │  │     Application Load Balancer           │    │  │              │
│  │  │  └─────────────────────────────────────────┘    │  │              │
│  │  │  ┌─────────────────────────────────────────┐    │  │              │
│  │  │  │           NAT Gateway                    │    │  │              │
│  │  │  └─────────────────────────────────────────┘    │  │              │
│  │  └─────────────────────────────────────────────────┘  │              │
│  │                                                        │              │
│  │  ┌─────────────────────────────────────────────────┐  │              │
│  │  │              Private Subnets                     │  │              │
│  │  │                                                  │  │              │
│  │  │  ┌──────────────────────────────────────────┐   │  │              │
│  │  │  │              EKS Cluster                  │   │  │              │
│  │  │  │  ┌────────┐ ┌────────┐ ┌────────┐       │   │  │              │
│  │  │  │  │ auth-  │ │account-│ │ card-  │       │   │  │              │
│  │  │  │  │service │ │service │ │service │       │   │  │              │
│  │  │  │  └────────┘ └────────┘ └────────┘       │   │  │              │
│  │  │  │  ┌────────┐ ┌────────┐ ┌────────┐       │   │  │              │
│  │  │  │  │ trans- │ │payment-│ │ batch- │       │   │  │              │
│  │  │  │  │service │ │service │ │service │       │   │  │              │
│  │  │  │  └────────┘ └────────┘ └────────┘       │   │  │              │
│  │  │  └──────────────────────────────────────────┘   │  │              │
│  │  │                                                  │  │              │
│  │  │  ┌──────────────────────────────────────────┐   │  │              │
│  │  │  │           RDS PostgreSQL                  │   │  │              │
│  │  │  │  ┌─────────────┐    ┌─────────────┐      │   │  │              │
│  │  │  │  │   Primary   │───►│   Standby   │      │   │  │              │
│  │  │  │  │    (AZ-a)   │    │    (AZ-b)   │      │   │  │              │
│  │  │  │  └─────────────┘    └─────────────┘      │   │  │              │
│  │  │  └──────────────────────────────────────────┘   │  │              │
│  │  │                                                  │  │              │
│  │  │  ┌──────────────────────────────────────────┐   │  │              │
│  │  │  │         ElastiCache (Redis)               │   │  │              │
│  │  │  │  ┌─────────────┐    ┌─────────────┐      │   │  │              │
│  │  │  │  │   Primary   │───►│   Replica   │      │   │  │              │
│  │  │  │  └─────────────┘    └─────────────┘      │   │  │              │
│  │  │  └──────────────────────────────────────────┘   │  │              │
│  │  └─────────────────────────────────────────────────┘  │              │
│  └───────────────────────────────────────────────────────┘              │
│                                                                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│  │    Amazon S3    │  │   Amazon SQS    │  │  CloudWatch     │         │
│  │  (Statements)   │  │   (Messages)    │  │  (Monitoring)   │         │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 10.2 Kubernetes Deployment

```yaml
# account-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: account-service
  namespace: carddemo
spec:
  replicas: 3
  selector:
    matchLabels:
      app: account-service
  template:
    metadata:
      labels:
        app: account-service
    spec:
      containers:
      - name: account-service
        image: carddemo/account-service:1.0.0
        ports:
        - containerPort: 8084
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8084
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8084
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: account-service
  namespace: carddemo
spec:
  selector:
    app: account-service
  ports:
  - port: 8084
    targetPort: 8084
  type: ClusterIP
```

### 10.3 CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy CardDemo Services

on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: mvn clean package -DskipTests
    
    - name: Run Tests
      run: mvn test
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-east-1
    
    - name: Login to Amazon ECR
      id: login-ecr
      uses: aws-actions/amazon-ecr-login@v2
    
    - name: Build and push Docker images
      env:
        ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
      run: |
        for service in auth account card transaction payment reporting batch user; do
          docker build -t $ECR_REGISTRY/carddemo-$service:${{ github.sha }} \
            ./services/$service-service
          docker push $ECR_REGISTRY/carddemo-$service:${{ github.sha }}
        done
    
    - name: Deploy to EKS
      run: |
        aws eks update-kubeconfig --name carddemo-cluster
        kubectl set image deployment/account-service \
          account-service=$ECR_REGISTRY/carddemo-account:${{ github.sha }} \
          -n carddemo
        # Repeat for other services
```

---

## 11. Testing Strategy

### 11.1 Testing Pyramid

```
                    ┌───────────────┐
                    │   E2E Tests   │  (10%)
                    │   (Selenium)  │
                    └───────┬───────┘
                            │
                ┌───────────┴───────────┐
                │   Integration Tests   │  (20%)
                │   (TestContainers)    │
                └───────────┬───────────┘
                            │
        ┌───────────────────┴───────────────────┐
        │           Unit Tests                   │  (70%)
        │        (JUnit 5, Mockito)              │
        └───────────────────────────────────────┘
```

### 11.2 Test Categories

#### 11.2.1 Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private AccountService accountService;
    
    @Test
    void getAccount_WhenExists_ReturnsAccount() {
        // Given
        Long accountId = 12345678901L;
        Account account = new Account();
        account.setAccountId(accountId);
        account.setCurrentBalance(new BigDecimal("1500.00"));
        
        when(accountRepository.findById(accountId))
            .thenReturn(Optional.of(account));
        
        // When
        AccountDTO result = accountService.getAccount(accountId);
        
        // Then
        assertThat(result.getAccountId()).isEqualTo(accountId);
        assertThat(result.getCurrentBalance())
            .isEqualByComparingTo(new BigDecimal("1500.00"));
    }
    
    @Test
    void getAccount_WhenNotExists_ThrowsException() {
        // Given
        Long accountId = 99999999999L;
        when(accountRepository.findById(accountId))
            .thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> accountService.getAccount(accountId))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining("Account ID NOT found");
    }
}
```

#### 11.2.2 Integration Tests

```java
@SpringBootTest
@Testcontainers
class AccountServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("carddemo")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Test
    void updateAccount_UpdatesBalanceCorrectly() {
        // Given
        Account account = createTestAccount();
        accountRepository.save(account);
        
        AccountUpdateDTO update = new AccountUpdateDTO();
        update.setCurrentBalance(new BigDecimal("2000.00"));
        
        // When
        AccountDTO result = accountService.updateAccount(
            account.getAccountId(), update);
        
        // Then
        assertThat(result.getCurrentBalance())
            .isEqualByComparingTo(new BigDecimal("2000.00"));
        
        Account saved = accountRepository.findById(account.getAccountId()).get();
        assertThat(saved.getCurrentBalance())
            .isEqualByComparingTo(new BigDecimal("2000.00"));
    }
}
```

#### 11.2.3 Functional Equivalence Tests

```java
/**
 * Tests to verify functional equivalence with mainframe behavior
 */
@SpringBootTest
class MainframeEquivalenceTest {
    
    @Autowired
    private AuthService authService;
    
    @Test
    void login_WithValidCredentials_ReturnsToken() {
        // Mainframe: COSGN00C validates against USRSEC file
        // Modern: auth-service validates against users table
        
        LoginRequest request = new LoginRequest("ADMIN001", "PASSWORD");
        LoginResponse response = authService.login(request);
        
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getUserType()).isEqualTo("ADMIN");
    }
    
    @Test
    void login_WithInvalidUser_ReturnsUserNotFound() {
        // Mainframe message: "User not found. Try again ..."
        
        LoginRequest request = new LoginRequest("INVALID", "PASSWORD");
        
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("User not found");
    }
    
    @Test
    void login_WithWrongPassword_ReturnsWrongPassword() {
        // Mainframe message: "Wrong Password. Try again ..."
        
        LoginRequest request = new LoginRequest("ADMIN001", "WRONGPWD");
        
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("Wrong Password");
    }
}
```

### 11.3 Performance Testing

```java
@SpringBootTest
class PerformanceTest {
    
    @Test
    void transactionList_MeetsPerformanceRequirement() {
        // Mainframe requirement: List screens display within 2 seconds
        
        long startTime = System.currentTimeMillis();
        
        Page<TransactionDTO> result = transactionService.listTransactions(
            PageRequest.of(0, 10));
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThan(2000);
        assertThat(result.getContent()).hasSize(10);
    }
}
```

---

## 12. Migration Phases and Timeline

### 12.1 Phase Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Migration Timeline (12 Months)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Phase 1: Foundation (Months 1-3)                                       │
│  ├── Infrastructure setup (AWS, EKS, RDS)                               │
│  ├── CI/CD pipeline establishment                                       │
│  ├── Database schema design and creation                                │
│  ├── Data migration tooling development                                 │
│  └── Auth service implementation                                        │
│                                                                          │
│  Phase 2: Core Services (Months 4-6)                                    │
│  ├── Customer service                                                   │
│  ├── Account service                                                    │
│  ├── Card service                                                       │
│  ├── Initial data migration                                             │
│  └── Integration testing                                                │
│                                                                          │
│  Phase 3: Transaction Processing (Months 7-9)                           │
│  ├── Transaction service                                                │
│  ├── Payment service                                                    │
│  ├── Batch processing (Spring Batch)                                    │
│  ├── Data synchronization setup                                         │
│  └── Parallel running with mainframe                                    │
│                                                                          │
│  Phase 4: Reporting & Cutover (Months 10-12)                            │
│  ├── Reporting service                                                  │
│  ├── Statement generation                                               │
│  ├── User acceptance testing                                            │
│  ├── Performance tuning                                                 │
│  ├── Cutover planning and execution                                     │
│  └── Mainframe decommissioning                                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 12.2 Detailed Phase Breakdown

#### Phase 1: Foundation (Months 1-3)

| Week | Activities | Deliverables |
|------|------------|--------------|
| 1-2 | AWS infrastructure setup | VPC, EKS cluster, RDS instance |
| 3-4 | CI/CD pipeline | GitHub Actions workflows, ECR repos |
| 5-6 | Database design | Schema DDL, Flyway migrations |
| 7-8 | Data migration tools | ETL scripts, validation tools |
| 9-10 | Auth service | JWT authentication, user management |
| 11-12 | Testing framework | Test infrastructure, initial tests |

#### Phase 2: Core Services (Months 4-6)

| Week | Activities | Deliverables |
|------|------------|--------------|
| 13-14 | Customer service | CRUD operations, API endpoints |
| 15-16 | Account service | View/update functionality |
| 17-18 | Card service | Card management features |
| 19-20 | Initial data migration | Reference data, test data |
| 21-22 | Integration testing | Service integration tests |
| 23-24 | API gateway setup | Kong/AWS API Gateway config |

#### Phase 3: Transaction Processing (Months 7-9)

| Week | Activities | Deliverables |
|------|------------|--------------|
| 25-26 | Transaction service | List, view, add transactions |
| 27-28 | Payment service | Bill payment processing |
| 29-30 | Transaction posting batch | Spring Batch job |
| 31-32 | Interest calculation batch | Monthly interest job |
| 33-34 | Data sync setup | Bidirectional sync with mainframe |
| 35-36 | Parallel running | Both systems processing |

#### Phase 4: Reporting & Cutover (Months 10-12)

| Week | Activities | Deliverables |
|------|------------|--------------|
| 37-38 | Reporting service | Report generation |
| 39-40 | Statement generation | PDF/HTML statements |
| 41-42 | UAT | User acceptance testing |
| 43-44 | Performance tuning | Optimization, load testing |
| 45-46 | Cutover preparation | Runbooks, rollback plans |
| 47-48 | Cutover execution | Go-live, monitoring |

---

## 13. Risk Assessment and Mitigation

### 13.1 Risk Matrix

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Data migration errors | Medium | High | Comprehensive validation, checksums, parallel running |
| Performance degradation | Medium | High | Load testing, performance benchmarks, caching |
| Business logic discrepancies | High | High | Functional equivalence tests, UAT, parallel running |
| Integration failures | Medium | Medium | Contract testing, API versioning, circuit breakers |
| Security vulnerabilities | Low | High | Security audits, penetration testing, encryption |
| Team skill gaps | Medium | Medium | Training, pair programming, external consultants |
| Timeline delays | Medium | Medium | Agile methodology, buffer time, MVP approach |
| Mainframe dependency during transition | High | Medium | Strangler fig pattern, data sync, rollback capability |

### 13.2 Mitigation Details

#### Data Migration Risks

```
Mitigation Strategy:
1. Develop comprehensive data validation framework
2. Implement record-by-record checksums
3. Run parallel systems with data comparison reports
4. Maintain rollback capability to mainframe
5. Perform multiple dry-run migrations before cutover
```

#### Business Logic Discrepancies

```
Mitigation Strategy:
1. Create functional equivalence test suite
2. Document all mainframe business rules
3. Implement shadow mode (process on both systems, compare results)
4. Extensive UAT with business users
5. Gradual traffic shifting with monitoring
```

---

## 14. Appendices

### 14.1 COBOL to Java Type Mapping Reference

| COBOL Declaration | Java Type | Notes |
|-------------------|-----------|-------|
| PIC 9(n) | int/long | Use long for n > 9 |
| PIC 9(n)V9(m) | BigDecimal | Preserve precision |
| PIC S9(n)V9(m) COMP-3 | BigDecimal | Unpack first |
| PIC X(n) | String | Trim trailing spaces |
| PIC 9(8) (date) | LocalDate | Convert CCYYMMDD |
| PIC X(26) (timestamp) | LocalDateTime | Parse ISO format |

### 14.2 Error Message Mapping

| Mainframe Message | HTTP Status | API Error Code |
|-------------------|-------------|----------------|
| "User not found. Try again ..." | 401 | AUTH_USER_NOT_FOUND |
| "Wrong Password. Try again ..." | 401 | AUTH_INVALID_PASSWORD |
| "Account ID NOT found..." | 404 | ACCOUNT_NOT_FOUND |
| "You have nothing to pay..." | 400 | PAYMENT_ZERO_BALANCE |
| "Invalid value. Valid values are (Y/N)..." | 400 | VALIDATION_INVALID_VALUE |
| "Please select only one record" | 400 | VALIDATION_MULTIPLE_SELECTION |

### 14.3 Glossary

| Term | Definition |
|------|------------|
| CICS | Customer Information Control System - IBM transaction processing |
| VSAM | Virtual Storage Access Method - IBM file storage |
| KSDS | Key-Sequenced Data Set - VSAM file type |
| AIX | Alternate Index - Secondary index on VSAM file |
| JCL | Job Control Language - Batch job definitions |
| COMMAREA | Communication Area - Data passed between CICS programs |
| BMS | Basic Mapping Support - Screen definitions |
| COMP-3 | Packed decimal format |
| Strangler Fig | Migration pattern for gradual replacement |
| DDD | Domain-Driven Design |

### 14.4 Reference Documents

1. CardDemo Architecture Document (ARCHITECTURE.md)
2. CardDemo Functional Requirements (FUNCTIONAL_REQUIREMENTS.md)
3. CardDemo User Stories (USER_STORIES.md)
4. AWS Mainframe Modernization Documentation
5. Spring Boot Reference Documentation
6. Spring Batch Reference Documentation

---

**Document End**

*This migration guide was generated to support the modernization of the CardDemo mainframe application to a Java Spring Boot microservices architecture on AWS.*
