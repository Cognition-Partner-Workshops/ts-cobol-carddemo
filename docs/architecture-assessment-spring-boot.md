# CardDemo Mainframe-to-Spring Boot Architecture Assessment & Modernization Plan

## 1. Executive Summary

This document provides a comprehensive architecture assessment for modernizing the CardDemo mainframe credit card management application from its current COBOL/CICS/VSAM stack to a Java Spring Boot application. It inventories every component by layer, maps each to a modern equivalent, outlines data migration strategies, and defines the security transformation plan.

**Current Stack:** COBOL, CICS, VSAM (KSDS/AIX), JCL, RACF, Assembler, with optional DB2, IMS DB, and MQ extensions.

**Target Stack:** Java 17+, Spring Boot 3.x, Spring MVC/REST, Spring Data JPA, Spring Security, Spring Batch, Spring AMQP/Kafka, PostgreSQL, React (or Angular) frontend.

---

## 2. Component Inventory by Layer

### 2.1 Presentation Layer

The current presentation layer consists of 17 BMS (Basic Mapping Support) map definitions that render 3270 terminal screens, driven by CICS transactions. Each screen is defined by a BMS map source file (`app/bms/*.bms`), a generated BMS copybook (`app/cpy-bms/*.CPY`), and a COBOL program that handles screen I/O and navigation.

#### 2.1.1 Online Screen Inventory

| Transaction ID | BMS Map File | BMS Copybook | COBOL Program | Function | User Type | Spring Boot Target |
|:--------------|:-------------|:-------------|:--------------|:---------|:----------|:-------------------|
| CC00 | `app/bms/COSGN00.bms` | `app/cpy-bms/COSGN00.CPY` | `app/cbl/COSGN00C.cbl` | Sign-on Screen | All | `AuthController` + `/api/auth/login` |
| CM00 | `app/bms/COMEN01.bms` | `app/cpy-bms/COMEN01.CPY` | `app/cbl/COMEN01C.cbl` | Main Menu | All | Frontend routing / `MenuController` |
| CAVW | `app/bms/COACTVW.bms` | `app/cpy-bms/COACTVW.CPY` | `app/cbl/COACTVWC.cbl` | Account View | User | `AccountController.getAccount()` |
| CAUP | `app/bms/COACTUP.bms` | `app/cpy-bms/COACTUP.CPY` | `app/cbl/COACTUPC.cbl` | Account Update | User | `AccountController.updateAccount()` |
| CCLI | `app/bms/COCRDLI.bms` | `app/cpy-bms/COCRDLI.CPY` | `app/cbl/COCRDLIC.cbl` | Credit Card List | User | `CardController.listCards()` |
| CCDL | `app/bms/COCRDSL.bms` | `app/cpy-bms/COCRDSL.CPY` | `app/cbl/COCRDSLC.cbl` | Credit Card View | User | `CardController.getCard()` |
| CCUP | `app/bms/COCRDUP.bms` | `app/cpy-bms/COCRDUP.CPY` | `app/cbl/COCRDUPC.cbl` | Credit Card Update | User | `CardController.updateCard()` |
| CT00 | `app/bms/COTRN00.bms` | `app/cpy-bms/COTRN00.CPY` | `app/cbl/COTRN00C.cbl` | Transaction List | User | `TransactionController.listTransactions()` |
| CT01 | `app/bms/COTRN01.bms` | `app/cpy-bms/COTRN01.CPY` | `app/cbl/COTRN01C.cbl` | Transaction View | User | `TransactionController.getTransaction()` |
| CT02 | `app/bms/COTRN02.bms` | `app/cpy-bms/COTRN02.CPY` | `app/cbl/COTRN02C.cbl` | Transaction Add | User | `TransactionController.createTransaction()` |
| CR00 | `app/bms/CORPT00.bms` | `app/cpy-bms/CORPT00.CPY` | `app/cbl/CORPT00C.cbl` | Transaction Reports | User | `ReportController.generateReport()` |
| CB00 | `app/bms/COBIL00.bms` | `app/cpy-bms/COBIL00.CPY` | `app/cbl/COBIL00C.cbl` | Bill Payment | User | `BillPaymentController.processPayment()` |
| CA00 | `app/bms/COADM01.bms` | `app/cpy-bms/COADM01.CPY` | `app/cbl/COADM01C.cbl` | Admin Menu | Admin | Frontend routing / `AdminController` |
| CU00 | `app/bms/COUSR00.bms` | `app/cpy-bms/COUSR00.CPY` | `app/cbl/COUSR00C.cbl` | User List | Admin | `UserController.listUsers()` |
| CU01 | `app/bms/COUSR01.bms` | `app/cpy-bms/COUSR01.CPY` | `app/cbl/COUSR01C.cbl` | User Add | Admin | `UserController.createUser()` |
| CU02 | `app/bms/COUSR02.bms` | `app/cpy-bms/COUSR02.CPY` | `app/cbl/COUSR02C.cbl` | User Update | Admin | `UserController.updateUser()` |
| CU03 | `app/bms/COUSR03.bms` | `app/cpy-bms/COUSR03.CPY` | `app/cbl/COUSR03C.cbl` | User Delete | Admin | `UserController.deleteUser()` |

#### 2.1.2 Optional Module Screens

| Transaction ID | BMS Map | Program | Function | Optional Module | Spring Boot Target |
|:--------------|:--------|:--------|:---------|:----------------|:-------------------|
| CPVS | `app/app-authorization-ims-db2-mq/bms/COPAU00` | `COPAUS0C` | Pending Authorization Summary | IMS-DB2-MQ | `AuthorizationController.listPendingAuthorizations()` |
| CPVD | `app/app-authorization-ims-db2-mq/bms/COPAU01` | `COPAUS1C` | Pending Authorization Details | IMS-DB2-MQ | `AuthorizationController.getAuthorizationDetail()` |
| CTTU | `app/app-transaction-type-db2/bms/COTRTUP` | `COTRTUPC` | Tran Type Add/Edit | DB2 | `TransactionTypeController.createOrUpdate()` |
| CTLI | `app/app-transaction-type-db2/bms/COTRTLI` | `COTRTLIC` | Tran Type List/Update/Delete | DB2 | `TransactionTypeController.listAndManage()` |
| CDRD | (No BMS) | `CODATE01` | System Date Inquiry via MQ | MQ | `SystemController.getSystemDate()` |
| CDRA | (No BMS) | `COACCT01` | Account Details Inquiry via MQ | MQ | `AccountController.getAccountViaMQ()` |

#### 2.1.3 Presentation Layer Mapping Strategy

| Mainframe Concept | Spring Boot Equivalent |
|:------------------|:----------------------|
| BMS Maps (3270 screens) | React/Angular SPA with REST API calls |
| BMS Map Copybooks (`app/cpy-bms/*.CPY`) | Frontend component models / TypeScript interfaces |
| CICS SEND MAP / RECEIVE MAP | REST API JSON request/response via `@RestController` |
| CICS Transaction IDs (CC00, CM00, etc.) | REST API endpoints (e.g., `/api/auth/login`, `/api/accounts/{id}`) |
| PF key navigation (PF3=Exit, PF7/PF8=Scroll) | Frontend buttons, pagination controls, keyboard shortcuts |
| CICS COMMAREA (`app/cpy/COCOM01Y.cpy`) | HTTP session / JWT token payload / Spring `@SessionAttributes` |
| Screen field validation | Jakarta Bean Validation (`@Valid`, `@NotNull`, etc.) + frontend validation |
| Screen title (`app/cpy/COTTL01Y.cpy`) | Frontend application header / branding component |

---

### 2.2 Business Logic Layer

#### 2.2.1 Online COBOL Programs (31 programs in `app/cbl/`)

| Program | Source File | Function | Proposed Spring Service |
|:--------|:-----------|:---------|:------------------------|
| COSGN00C | `app/cbl/COSGN00C.cbl` | User sign-on / authentication | `AuthenticationService.authenticate()` |
| COMEN01C | `app/cbl/COMEN01C.cbl` | Main menu navigation & routing | `MenuService` / frontend routing logic |
| COACTVWC | `app/cbl/COACTVWC.cbl` | Account view (read) | `AccountService.getAccountById()` |
| COACTUPC | `app/cbl/COACTUPC.cbl` | Account update | `AccountService.updateAccount()` |
| COCRDLIC | `app/cbl/COCRDLIC.cbl` | Credit card list with pagination | `CardService.listCards()` |
| COCRDSLC | `app/cbl/COCRDSLC.cbl` | Credit card detail view | `CardService.getCardById()` |
| COCRDUPC | `app/cbl/COCRDUPC.cbl` | Credit card update | `CardService.updateCard()` |
| COTRN00C | `app/cbl/COTRN00C.cbl` | Transaction list with pagination | `TransactionService.listTransactions()` |
| COTRN01C | `app/cbl/COTRN01C.cbl` | Transaction detail view | `TransactionService.getTransactionById()` |
| COTRN02C | `app/cbl/COTRN02C.cbl` | Transaction creation/add | `TransactionService.createTransaction()` |
| CORPT00C | `app/cbl/CORPT00C.cbl` | Transaction report generation (submits batch JCL TRANREPT) | `ReportService.generateTransactionReport()` |
| COBIL00C | `app/cbl/COBIL00C.cbl` | Bill payment processing | `BillPaymentService.processPayment()` |
| COADM01C | `app/cbl/COADM01C.cbl` | Admin menu navigation | `AdminMenuService` / frontend routing |
| COUSR00C | `app/cbl/COUSR00C.cbl` | User list (security admin) | `UserService.listUsers()` |
| COUSR01C | `app/cbl/COUSR01C.cbl` | User add (security admin) | `UserService.createUser()` |
| COUSR02C | `app/cbl/COUSR02C.cbl` | User update (security admin) | `UserService.updateUser()` |
| COUSR03C | `app/cbl/COUSR03C.cbl` | User delete (security admin) | `UserService.deleteUser()` |
| CSUTLDTC | `app/cbl/CSUTLDTC.cbl` | Date/time utility | `DateTimeUtils` utility class |
| CBACT01C | `app/cbl/CBACT01C.cbl` | Batch: Account file processing | `AccountBatchService` |
| CBACT02C | `app/cbl/CBACT02C.cbl` | Batch: Account processing (secondary) | `AccountBatchService` |
| CBACT03C | `app/cbl/CBACT03C.cbl` | Batch: Account processing (tertiary) | `AccountBatchService` |
| CBACT04C | `app/cbl/CBACT04C.cbl` | Batch: Interest calculation | `InterestCalculationService` |
| CBCUS01C | `app/cbl/CBCUS01C.cbl` | Batch: Customer file processing | `CustomerBatchService` |
| CBSTM03A | `app/cbl/CBSTM03A.CBL` | Batch: Statement generation (main) | `StatementGenerationService` |
| CBSTM03B | `app/cbl/CBSTM03B.CBL` | Batch: Statement generation (sub) | `StatementGenerationService` (helper) |
| CBTRN01C | `app/cbl/CBTRN01C.cbl` | Batch: Transaction processing (part 1) | `TransactionBatchService` |
| CBTRN02C | `app/cbl/CBTRN02C.cbl` | Batch: Transaction posting | `TransactionPostingService` |
| CBTRN03C | `app/cbl/CBTRN03C.cbl` | Batch: Transaction report generation | `TransactionReportService` |
| CBEXPORT | `app/cbl/CBEXPORT.cbl` | Batch: Data export utility | `DataExportService` |
| CBIMPORT | `app/cbl/CBIMPORT.cbl` | Batch: Data import utility | `DataImportService` |
| COBSWAIT | `app/cbl/COBSWAIT.cbl` | Batch: Wait/timer step (calls ASM MVSWAIT) | Spring Batch step delay / `Thread.sleep()` |

#### 2.2.2 Optional Module Programs

| Program | Source | Module | Function | Proposed Spring Service |
|:--------|:-------|:-------|:---------|:------------------------|
| COPAUA0C | `app/app-authorization-ims-db2-mq/cbl/` | IMS-DB2-MQ | MQ-triggered authorization processor | `AuthorizationProcessingService` + `@JmsListener` |
| COPAUS0C | `app/app-authorization-ims-db2-mq/cbl/` | IMS-DB2-MQ | Authorization summary display | `AuthorizationService.getSummary()` |
| COPAUS1C | `app/app-authorization-ims-db2-mq/cbl/` | IMS-DB2-MQ | Authorization details display | `AuthorizationService.getDetail()` |
| COPAUS2C | `app/app-authorization-ims-db2-mq/cbl/` | IMS-DB2-MQ | Fraud marking (DB2 insert) | `FraudService.markAsFraud()` |
| CBPAUP0C | `app/app-authorization-ims-db2-mq/cbl/` | IMS-DB2-MQ | Batch: Purge expired authorizations | `AuthorizationPurgeService` (Spring Batch job) |
| COTRTUPC | `app/app-transaction-type-db2/cbl/` | DB2 | Transaction type add/edit (embedded SQL) | `TransactionTypeService.createOrUpdate()` |
| COTRTLIC | `app/app-transaction-type-db2/cbl/` | DB2 | Transaction type list/update/delete (cursor) | `TransactionTypeService.listAndManage()` |
| COBTUPDT | `app/app-transaction-type-db2/cbl/` | DB2 | Batch: Transaction type maintenance | `TransactionTypeBatchService` |
| CODATE01 | `app/app-vsam-mq/cbl/` | MQ | System date inquiry via MQ | `SystemDateService` + `@JmsListener` |
| COACCT01 | `app/app-vsam-mq/cbl/` | MQ | Account details inquiry via MQ | `AccountMQService` + `@JmsListener` |

#### 2.2.3 Assembler Programs

| Program | Source File | Function | Spring Boot Equivalent |
|:--------|:-----------|:---------|:-----------------------|
| MVSWAIT | `app/asm/MVSWAIT.asm` | Timer control for batch wait steps | `Thread.sleep()` / Spring Batch `Tasklet` with delay |
| COBDATFT | `app/asm/COBDATFT.asm` | Date format conversion utility | `java.time.format.DateTimeFormatter` / `DateTimeUtils` |

#### 2.2.4 Batch Processing (JCL to Spring Batch Mapping)

Each JCL job in `app/jcl/` maps to a Spring Batch `Job` or a scheduled service task:

| JCL Job | JCL File | Current Program | Purpose | Spring Batch Equivalent |
|:--------|:---------|:----------------|:--------|:------------------------|
| CLOSEFIL | `app/jcl/CLOSEFIL.jcl` | IEFBR14 | Close VSAM files in CICS | Not needed (DB connections managed by connection pool) |
| ACCTFILE | `app/jcl/ACCTFILE.jcl` | IDCAMS | Refresh account master VSAM | `accountRefreshJob` (Spring Batch `Job`) |
| CARDFILE | `app/jcl/CARDFILE.jcl` | IDCAMS | Refresh card master VSAM | `cardRefreshJob` |
| CUSTFILE | `app/jcl/CUSTFILE.jcl` | IDCAMS | Refresh customer master VSAM | `customerRefreshJob` |
| XREFFILE | `app/jcl/XREFFILE.jcl` | IDCAMS | Load card-account-customer cross-reference | `crossReferenceRefreshJob` |
| TRANFILE | `app/jcl/TRANFILE.jcl` | IDCAMS | Load transaction master to VSAM | `transactionLoadJob` |
| TRANBKP | `app/jcl/TRANBKP.jcl` | IDCAMS | Backup/refresh transaction master | `transactionBackupJob` |
| TRANEXTR | `app/jcl/TRANEXTR.jcl` | DSNTIAUL | Extract DB2 transaction types to VSAM | Not needed (single DB source of truth) |
| TRANCATG | `app/jcl/TRANCATG.jcl` | IDCAMS | Load transaction category types | `transactionCategoryLoadJob` |
| TRANTYPE | `app/jcl/TRANTYPE.jcl` | IDCAMS | Load transaction types | `transactionTypeLoadJob` |
| DISCGRP | `app/jcl/DISCGRP.jcl` | IDCAMS | Load disclosure groups | `disclosureGroupLoadJob` |
| TCATBALF | `app/jcl/TCATBALF.jcl` | IDCAMS | Refresh transaction category balance | `tranCategoryBalanceRefreshJob` |
| DUSRSECJ | `app/jcl/DUSRSECJ.jcl` | IEBGENER | Initial load of user security file | `userSecurityLoadJob` / DB seed migration |
| POSTTRAN | `app/jcl/POSTTRAN.jcl` | CBTRN02C | Core transaction posting | `transactionPostingJob` (`ItemReader` -> `ItemProcessor` -> `ItemWriter`) |
| INTCALC | `app/jcl/INTCALC.jcl` | CBACT04C | Interest calculations | `interestCalculationJob` |
| COMBTRAN | `app/jcl/COMBTRAN.jcl` | SORT | Combine daily + system transactions | `transactionCombineJob` (SQL query or Spring Batch merge step) |
| CREASTMT | `app/jcl/CREASTMT.JCL` | CBSTM03A | Produce transaction statements | `statementGenerationJob` |
| TRANREPT | (submitted from CICS) | CBTRN03C | Transaction report | `transactionReportJob` |
| TRANIDX | `app/jcl/TRANIDX.jcl` | IDCAMS | Define AIX on transaction file | Not needed (DB indexes via JPA/DDL) |
| OPENFIL | `app/jcl/OPENFIL.jcl` | IEFBR14 | Open files in CICS | Not needed (connection pool) |
| WAITSTEP | `app/jcl/WAITSTEP.jcl` | COBSWAIT | Delay step between batch jobs | Spring Batch step with delay / scheduler interval |
| CBPAUP0J | `app/jcl/CBPAUP0J.jcl`* | CBPAUP0C | Purge expired authorizations | `authorizationPurgeJob` (Spring Batch) |
| DEFGDGB | `app/jcl/DEFGDGB.jcl` | IDCAMS | Define GDG bases | Not needed (file versioning via timestamps/DB) |
| DEFGDGD | `app/jcl/DEFGDGD.jcl` | IDCAMS | Define GDG bases (DB2) | Not needed |
| ESDSRRDS | `app/jcl/ESDSRRDS.jcl` | IDCAMS | Create ESDS/RRDS VSAM files | Not needed |
| MNTTRDB2 | (DB2 module) | COBTUPDT | Batch maintain transaction types in DB2 | `transactionTypeMaintenanceJob` |
| CBEXPORT | `app/jcl/CBEXPORT.jcl` | CBEXPORT | Data export | `dataExportJob` |
| CBIMPORT | `app/jcl/CBIMPORT.jcl` | CBIMPORT | Data import | `dataImportJob` |

#### 2.2.5 Business Logic Mapping Strategy

| Mainframe Concept | Spring Boot Equivalent |
|:------------------|:----------------------|
| COBOL program (online) | `@Service` class with business methods |
| COBOL program (batch) | Spring Batch `Tasklet` or `ItemProcessor` |
| CICS EXEC CICS commands (READ, WRITE, REWRITE, DELETE) | Spring Data JPA repository methods |
| CICS LINK / XCTL (program-to-program) | Java method calls / `@Autowired` service injection |
| COBOL COMMAREA (`COCOM01Y.cpy`) | Java DTO / session-scoped bean / JWT claims |
| COBOL copybook INCLUDE | Java class composition / shared domain objects |
| COBOL WORKING-STORAGE | Java instance/local variables |
| COBOL 88-level conditions | Java enums or boolean methods |
| COBOL SORT verb (COMBTRAN) | SQL `ORDER BY` / `Collections.sort()` / Spring Batch sort step |
| JCL job streams | Spring Batch `Job` with ordered `Step` sequences |
| JCL COND codes | Spring Batch `JobExecutionDecider` / step exit status |
| GDG (Generation Data Groups) | Timestamped file backups / database versioning |

---

### 2.3 Data Layer

#### 2.3.1 VSAM File to Database Table Mapping

Each VSAM dataset maps to a PostgreSQL table. The COBOL copybook defines the record layout, which maps to a JPA `@Entity`.

| VSAM Dataset | Copybook | Record Length | Proposed Table | JPA Entity | Primary Key |
|:-------------|:---------|:-------------|:---------------|:-----------|:------------|
| `AWS.M2.CARDDEMO.USRSEC.PS` | `app/cpy/CSUSR01Y.cpy` | 80 | `users` | `User` | `user_id` (SEC-USR-ID) |
| `AWS.M2.CARDDEMO.ACCTDATA.PS` | `app/cpy/CVACT01Y.cpy` | 300 | `accounts` | `Account` | `account_id` (ACCT-ID) |
| `AWS.M2.CARDDEMO.CARDDATA.PS` | `app/cpy/CVACT02Y.cpy` | 150 | `cards` | `Card` | `card_number` (CARD-NUM) |
| `AWS.M2.CARDDEMO.CUSTDATA.PS` | `app/cpy/CVCUS01Y.cpy` | 500 | `customers` | `Customer` | `customer_id` (CUST-ID) |
| `AWS.M2.CARDDEMO.CARDXREF.PS` | `app/cpy/CVACT03Y.cpy` | 50 | `card_xref` | `CardCrossReference` | `card_number` (XREF-CARD-NUM) |
| `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | `app/cpy/CVTRA05Y.cpy` | 350 | `transactions` | `Transaction` | `transaction_id` (TRAN-ID) |
| `AWS.M2.CARDDEMO.DALYTRAN.PS` | `app/cpy/CVTRA06Y.cpy` | 350 | `daily_transactions` | `DailyTransaction` | `transaction_id` (DALYTRAN-ID) |
| `AWS.M2.CARDDEMO.DISCGRP.PS` | `app/cpy/CVTRA02Y.cpy` | 50 | `disclosure_groups` | `DisclosureGroup` | Composite: (`group_id`, `tran_type_cd`, `tran_cat_cd`) |
| `AWS.M2.CARDDEMO.TRANCATG.PS` | `app/cpy/CVTRA04Y.cpy` | 60 | `transaction_categories` | `TransactionCategory` | Composite: (`tran_type_cd`, `tran_cat_cd`) |
| `AWS.M2.CARDDEMO.TRANTYPE.PS` | `app/cpy/CVTRA03Y.cpy` | 60 | `transaction_types` | `TransactionType` | `tran_type` (TRAN-TYPE) |
| `AWS.M2.CARDDEMO.TCATBALF.PS` | `app/cpy/CVTRA01Y.cpy` | 50 | `tran_category_balances` | `TransactionCategoryBalance` | Composite: (`account_id`, `type_cd`, `cat_cd`) |

#### 2.3.2 Detailed Entity Field Mappings

**`User` Entity** (from `CSUSR01Y.cpy` - Record Length 80)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type | Notes |
|:------------|:-----------|:-----------|:----------|:----------|:--------|:------|
| SEC-USR-ID | X(08) | userId | String | user_id | VARCHAR(8) | PK |
| SEC-USR-FNAME | X(20) | firstName | String | first_name | VARCHAR(20) | |
| SEC-USR-LNAME | X(20) | lastName | String | last_name | VARCHAR(20) | |
| SEC-USR-PWD | X(08) | password | String | password_hash | VARCHAR(255) | BCrypt hashed |
| SEC-USR-TYPE | X(01) | userType | UserType (enum) | user_type | VARCHAR(1) | 'A'=Admin, 'U'=User |

**`Account` Entity** (from `CVACT01Y.cpy` - Record Length 300)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| ACCT-ID | 9(11) | accountId | Long | account_id | BIGINT |
| ACCT-ACTIVE-STATUS | X(01) | activeStatus | String | active_status | VARCHAR(1) |
| ACCT-CURR-BAL | S9(10)V99 | currentBalance | BigDecimal | current_balance | DECIMAL(12,2) |
| ACCT-CREDIT-LIMIT | S9(10)V99 | creditLimit | BigDecimal | credit_limit | DECIMAL(12,2) |
| ACCT-CASH-CREDIT-LIMIT | S9(10)V99 | cashCreditLimit | BigDecimal | cash_credit_limit | DECIMAL(12,2) |
| ACCT-OPEN-DATE | X(10) | openDate | LocalDate | open_date | DATE |
| ACCT-EXPIRAION-DATE | X(10) | expirationDate | LocalDate | expiration_date | DATE |
| ACCT-REISSUE-DATE | X(10) | reissueDate | LocalDate | reissue_date | DATE |
| ACCT-CURR-CYC-CREDIT | S9(10)V99 | currentCycleCredit | BigDecimal | current_cycle_credit | DECIMAL(12,2) |
| ACCT-CURR-CYC-DEBIT | S9(10)V99 | currentCycleDebit | BigDecimal | current_cycle_debit | DECIMAL(12,2) |
| ACCT-ADDR-ZIP | X(10) | addressZip | String | address_zip | VARCHAR(10) |
| ACCT-GROUP-ID | X(10) | groupId | String | group_id | VARCHAR(10) |

**`Card` Entity** (from `CVACT02Y.cpy` - Record Length 150)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| CARD-NUM | X(16) | cardNumber | String | card_number | VARCHAR(16) |
| CARD-ACCT-ID | 9(11) | accountId | Long | account_id | BIGINT |
| CARD-CVV-CD | 9(03) | cvvCode | String | cvv_code | VARCHAR(3) |
| CARD-EMBOSSED-NAME | X(50) | embossedName | String | embossed_name | VARCHAR(50) |
| CARD-EXPIRAION-DATE | X(10) | expirationDate | LocalDate | expiration_date | DATE |
| CARD-ACTIVE-STATUS | X(01) | activeStatus | String | active_status | VARCHAR(1) |

**`Customer` Entity** (from `CVCUS01Y.cpy` - Record Length 500)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| CUST-ID | 9(09) | customerId | Long | customer_id | BIGINT |
| CUST-FIRST-NAME | X(25) | firstName | String | first_name | VARCHAR(25) |
| CUST-MIDDLE-NAME | X(25) | middleName | String | middle_name | VARCHAR(25) |
| CUST-LAST-NAME | X(25) | lastName | String | last_name | VARCHAR(25) |
| CUST-ADDR-LINE-1 | X(50) | addressLine1 | String | address_line_1 | VARCHAR(50) |
| CUST-ADDR-LINE-2 | X(50) | addressLine2 | String | address_line_2 | VARCHAR(50) |
| CUST-ADDR-LINE-3 | X(50) | addressLine3 | String | address_line_3 | VARCHAR(50) |
| CUST-ADDR-STATE-CD | X(02) | stateCode | String | state_code | VARCHAR(2) |
| CUST-ADDR-COUNTRY-CD | X(03) | countryCode | String | country_code | VARCHAR(3) |
| CUST-ADDR-ZIP | X(10) | zip | String | zip | VARCHAR(10) |
| CUST-PHONE-NUM-1 | X(15) | phoneNumber1 | String | phone_number_1 | VARCHAR(15) |
| CUST-PHONE-NUM-2 | X(15) | phoneNumber2 | String | phone_number_2 | VARCHAR(15) |
| CUST-SSN | 9(09) | ssn | String | ssn | VARCHAR(9) |
| CUST-GOVT-ISSUED-ID | X(20) | govtIssuedId | String | govt_issued_id | VARCHAR(20) |
| CUST-DOB-YYYY-MM-DD | X(10) | dateOfBirth | LocalDate | date_of_birth | DATE |
| CUST-EFT-ACCOUNT-ID | X(10) | eftAccountId | String | eft_account_id | VARCHAR(10) |
| CUST-PRI-CARD-HOLDER-IND | X(01) | primaryCardHolderInd | String | primary_card_holder | VARCHAR(1) |
| CUST-FICO-CREDIT-SCORE | 9(03) | ficoCreditScore | Integer | fico_credit_score | INTEGER |

**`CardCrossReference` Entity** (from `CVACT03Y.cpy` - Record Length 50)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| XREF-CARD-NUM | X(16) | cardNumber | String | card_number | VARCHAR(16) |
| XREF-CUST-ID | 9(09) | customerId | Long | customer_id | BIGINT |
| XREF-ACCT-ID | 9(11) | accountId | Long | account_id | BIGINT |

**`Transaction` Entity** (from `CVTRA05Y.cpy` - Record Length 350)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| TRAN-ID | X(16) | transactionId | String | transaction_id | VARCHAR(16) |
| TRAN-TYPE-CD | X(02) | typeCode | String | type_code | VARCHAR(2) |
| TRAN-CAT-CD | 9(04) | categoryCode | Integer | category_code | INTEGER |
| TRAN-SOURCE | X(10) | source | String | source | VARCHAR(10) |
| TRAN-DESC | X(100) | description | String | description | VARCHAR(100) |
| TRAN-AMT | S9(09)V99 | amount | BigDecimal | amount | DECIMAL(11,2) |
| TRAN-MERCHANT-ID | 9(09) | merchantId | Long | merchant_id | BIGINT |
| TRAN-MERCHANT-NAME | X(50) | merchantName | String | merchant_name | VARCHAR(50) |
| TRAN-MERCHANT-CITY | X(50) | merchantCity | String | merchant_city | VARCHAR(50) |
| TRAN-MERCHANT-ZIP | X(10) | merchantZip | String | merchant_zip | VARCHAR(10) |
| TRAN-CARD-NUM | X(16) | cardNumber | String | card_number | VARCHAR(16) |
| TRAN-ORIG-TS | X(26) | originTimestamp | LocalDateTime | origin_timestamp | TIMESTAMP |
| TRAN-PROC-TS | X(26) | processedTimestamp | LocalDateTime | processed_timestamp | TIMESTAMP |

**`TransactionType` Entity** (from `CVTRA03Y.cpy` - Record Length 60)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| TRAN-TYPE | X(02) | typeCode | String | type_code | VARCHAR(2) |
| TRAN-TYPE-DESC | X(50) | description | String | description | VARCHAR(50) |

**`TransactionCategory` Entity** (from `CVTRA04Y.cpy` - Record Length 60)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| TRAN-TYPE-CD | X(02) | typeCode | String | type_code | VARCHAR(2) |
| TRAN-CAT-CD | 9(04) | categoryCode | Integer | category_code | INTEGER |
| TRAN-CAT-TYPE-DESC | X(50) | description | String | description | VARCHAR(50) |

**`TransactionCategoryBalance` Entity** (from `CVTRA01Y.cpy` - Record Length 50)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| TRANCAT-ACCT-ID | 9(11) | accountId | Long | account_id | BIGINT |
| TRANCAT-TYPE-CD | X(02) | typeCode | String | type_code | VARCHAR(2) |
| TRANCAT-CD | 9(04) | categoryCode | Integer | category_code | INTEGER |
| TRAN-CAT-BAL | S9(09)V99 | balance | BigDecimal | balance | DECIMAL(11,2) |

**`DisclosureGroup` Entity** (from `CVTRA02Y.cpy` - Record Length 50)

| COBOL Field | PIC Clause | Java Field | Java Type | DB Column | DB Type |
|:------------|:-----------|:-----------|:----------|:----------|:--------|
| DIS-ACCT-GROUP-ID | X(10) | accountGroupId | String | account_group_id | VARCHAR(10) |
| DIS-TRAN-TYPE-CD | X(02) | tranTypeCode | String | tran_type_code | VARCHAR(2) |
| DIS-TRAN-CAT-CD | 9(04) | tranCategoryCode | Integer | tran_category_code | INTEGER |
| DIS-INT-RATE | S9(04)V99 | interestRate | BigDecimal | interest_rate | DECIMAL(6,2) |

#### 2.3.3 Optional Module Data Structures

**Authorization Fraud Record** (DB2 table `AUTHFRDS` from `app/app-authorization-ims-db2-mq/ddl/`)

| DB2 Column | DB Type | Java Field | Java Type | Target DB Column |
|:-----------|:--------|:-----------|:----------|:-----------------|
| CARD_NUM | CHAR(16) | cardNumber | String | card_number |
| AUTH_TS | TIMESTAMP | authTimestamp | LocalDateTime | auth_timestamp |
| AUTH_TYPE | CHAR(4) | authType | String | auth_type |
| CARD_EXPIRY_DATE | CHAR(4) | cardExpiryDate | String | card_expiry_date |
| MESSAGE_TYPE | CHAR(6) | messageType | String | message_type |
| MESSAGE_SOURCE | CHAR(6) | messageSource | String | message_source |
| AUTH_ID_CODE | CHAR(6) | authIdCode | String | auth_id_code |
| AUTH_RESP_CODE | CHAR(2) | authRespCode | String | auth_resp_code |
| AUTH_RESP_REASON | CHAR(4) | authRespReason | String | auth_resp_reason |
| PROCESSING_CODE | CHAR(6) | processingCode | String | processing_code |
| TRANSACTION_AMT | DECIMAL(12,2) | transactionAmount | BigDecimal | transaction_amount |
| APPROVED_AMT | DECIMAL(12,2) | approvedAmount | BigDecimal | approved_amount |
| MERCHANT_CATAGORY_CODE | CHAR(4) | merchantCategoryCode | String | merchant_category_code |
| ACQR_COUNTRY_CODE | CHAR(3) | acquirerCountryCode | String | acquirer_country_code |
| POS_ENTRY_MODE | SMALLINT | posEntryMode | Integer | pos_entry_mode |
| MERCHANT_ID | CHAR(15) | merchantId | String | merchant_id |
| MERCHANT_NAME | VARCHAR(22) | merchantName | String | merchant_name |
| MERCHANT_CITY | CHAR(13) | merchantCity | String | merchant_city |
| MERCHANT_STATE | CHAR(02) | merchantState | String | merchant_state |
| MERCHANT_ZIP | CHAR(09) | merchantZip | String | merchant_zip |
| TRANSACTION_ID | CHAR(15) | transactionId | String | transaction_id |
| MATCH_STATUS | CHAR(1) | matchStatus | String | match_status |
| AUTH_FRAUD | CHAR(1) | authFraud | String | auth_fraud |
| FRAUD_RPT_DATE | DATE | fraudReportDate | LocalDate | fraud_report_date |
| ACCT_ID | DECIMAL(11) | accountId | Long | account_id |
| CUST_ID | DECIMAL(9) | customerId | Long | customer_id |

**IMS Hierarchical Segments** (from `app/app-authorization-ims-db2-mq/ims/`)

| IMS Segment | IMS DBD | Copybook | Proposed Table | Notes |
|:------------|:--------|:---------|:---------------|:------|
| PAUTSUM0 (root) | DBPAUTP0 | CIPAUSMY | `pending_authorizations` | One row per card with summary |
| PAUTDTL1 (child) | DBPAUTP0 | CIPAUDTY | `pending_authorization_details` | FK to `pending_authorizations` |
| PAUTINDX (index) | DBPAUTX0 | - | DB index on `pending_authorizations` | Automatic via JPA `@Index` |

**DB2 Transaction Type Tables** (from `app/app-transaction-type-db2/ddl/`)

| DB2 Table | Proposed Table | Notes |
|:----------|:---------------|:------|
| CARDDEMO.TRANSACTION_TYPE | `transaction_types` | Already mapped above; single source of truth in modernized system |
| CARDDEMO.TRANSACTION_TYPE_CATEGORY | `transaction_categories` | Already mapped above; FK to `transaction_types` |

#### 2.3.4 Entity Relationship Summary

```
customers (1) ----< (N) card_xref >---- (1) accounts
                         |
                         | (1)
                         v
                       cards (N) ----< (N) transactions
                                              |
                                              | (N..1)
                                              v
                                        transaction_types (1) ----< (N) transaction_categories
                                                                           |
                                              accounts (1) ----< (N) tran_category_balances
                                                     |
                                                     +---------< (N) disclosure_groups

pending_authorizations (1) ----< (N) pending_authorization_details
authorization_fraud_records (standalone)

users (standalone - security)
daily_transactions (staging table for batch processing)
```

#### 2.3.5 Data Layer Mapping Strategy

| Mainframe Concept | Spring Boot Equivalent |
|:------------------|:----------------------|
| VSAM KSDS (Keyed Sequential) | PostgreSQL table with primary key |
| VSAM AIX (Alternate Index) | PostgreSQL secondary index / `@Index` annotation |
| VSAM ESDS (Entry Sequenced) | PostgreSQL table with auto-increment ID |
| VSAM RRDS (Relative Record) | PostgreSQL table with integer PK |
| IDCAMS DEFINE/REPRO | Flyway/Liquibase database migrations |
| COBOL copybook (record layout) | JPA `@Entity` class |
| COBOL REDEFINES | Java inheritance or union type mapping |
| COBOL COMP / COMP-3 (packed decimal) | `BigDecimal` |
| COBOL signed numeric S9(n)V99 | `BigDecimal` |
| COBOL PIC 9(n) | `Long` or `Integer` |
| COBOL PIC X(n) | `String` |
| IMS DB hierarchical segments | Relational tables with foreign keys |
| DB2 tables | PostgreSQL tables (minimal change) |
| GDG (Generation Data Groups) | Timestamped backup tables or file archives |

---

## 3. Integration Points & Modernization Targets

### 3.1 Integration Point Inventory

| # | Integration Point | Current Technology | Source Files | Modernization Target |
|:--|:------------------|:-------------------|:-------------|:---------------------|
| 1 | User Authentication | VSAM file + RACF | `COSGN00C.cbl`, `CSUSR01Y.cpy` | Spring Security + DB-backed `UserDetailsService` |
| 2 | Screen Navigation | CICS COMMAREA | `COCOM01Y.cpy`, `COMEN02Y.cpy`, `COADM02Y.cpy` | JWT token + frontend React Router |
| 3 | Online VSAM CRUD | CICS FILE CONTROL (READ/WRITE/REWRITE/DELETE) | All online `.cbl` files | Spring Data JPA repositories |
| 4 | Batch File Processing | JCL + IDCAMS + IEBGENER | `app/jcl/*.jcl` | Spring Batch jobs |
| 5 | Report Generation | Batch COBOL (CBSTM03A/B, CBTRN03C) + CICS submit | `CORPT00C.cbl`, `CBSTM03A.CBL`, `CBTRN03C.cbl` | Spring Batch + JasperReports / PDF generation |
| 6 | DB2 Integration | Embedded static SQL with SQLCA | `COTRTUPC`, `COTRTLIC`, `COBTUPDT`, `COPAUS2C` | Spring Data JPA (seamless - already relational) |
| 7 | IMS DB Integration | DL/I calls (GU, GN, ISRT, REPL, DLET) | `COPAUA0C`, `COPAUS0C`, `COPAUS1C`, `CBPAUP0C` | Spring Data JPA (hierarchical -> relational mapping) |
| 8 | MQ Integration | CICS MQ API (MQGET/MQPUT) | `CODATE01`, `COACCT01`, `COPAUA0C` | Spring AMQP (RabbitMQ) or Spring Kafka |
| 9 | Date/Time Utilities | ASM program COBDATFT + COBOL utility CSUTLDTC | `app/asm/COBDATFT.asm`, `CSUTLDTC.cbl` | `java.time` API |
| 10 | Timer/Wait Control | ASM program MVSWAIT | `app/asm/MVSWAIT.asm` | `Thread.sleep()` / `ScheduledExecutorService` |
| 11 | Data Export/Import | COBOL programs CBEXPORT/CBIMPORT | `CBEXPORT.cbl`, `CBIMPORT.cbl` | Spring Batch `FlatFileItemReader/Writer` or REST API |
| 12 | Cross-reference Lookup | VSAM XREF file | `CVACT03Y.cpy` | JPA `@ManyToOne` / `@JoinColumn` relationships |
| 13 | Interest Calculation | Batch COBOL CBACT04C | `CBACT04C.cbl`, `CVTRA02Y.cpy` (disclosure groups) | `InterestCalculationService` Spring Batch job |
| 14 | Transaction Posting | Batch COBOL CBTRN02C | `CBTRN02C.cbl` | `TransactionPostingService` Spring Batch job |
| 15 | Statement Creation | Batch COBOL CBSTM03A/B | `CBSTM03A.CBL`, `CBSTM03B.CBL`, `CVTRA07Y.cpy` | `StatementGenerationService` + PDF template |

### 3.2 Integration Modernization Details

#### 3.2.1 DB2 Integration (Transaction Type Management)

**Current:** Static embedded SQL in COBOL with DB2 precompiler, SQLCA error handling, cursor-based pagination.

**Files:**
- Programs: `app/app-transaction-type-db2/cbl/COTRTUPC.cbl`, `COTRTLIC.cbl`, `COBTUPDT.cbl`
- DDL: `app/app-transaction-type-db2/ddl/`
- DCL (host variable declarations): `app/app-transaction-type-db2/dcl/`

**Target:** Spring Data JPA with `JpaRepository` interfaces. Cursors map to `Pageable`/`Page<T>`. SQLCA error handling maps to JPA exception handling (`@Transactional`, `DataIntegrityViolationException`).

```
Current: EXEC SQL SELECT ... INTO :HOST-VAR FROM TRANSACTION_TYPE WHERE ...
Target:  transactionTypeRepository.findByTypeCode(typeCode)

Current: EXEC SQL DECLARE CURSOR ... / OPEN / FETCH / CLOSE
Target:  transactionTypeRepository.findAll(PageRequest.of(page, size, Sort.by("typeCode")))

Current: EXEC SQL INSERT INTO ...
Target:  transactionTypeRepository.save(entity)

Current: EXEC SQL DELETE FROM ... WHERE ...
Target:  transactionTypeRepository.deleteByTypeCode(typeCode)
```

#### 3.2.2 IMS DB Integration (Authorization Storage)

**Current:** IMS DL/I calls to HIDAM database with hierarchical segments (root: PAUTSUM0, child: PAUTDTL1).

**Files:**
- Programs: `app/app-authorization-ims-db2-mq/cbl/`
- IMS DBDs: `app/app-authorization-ims-db2-mq/ims/` (DBPAUTP0, DBPAUTX0)
- IMS PSBs: PSBPAUTB (batch), PSBPAUTL (online)
- Copybooks: `app/app-authorization-ims-db2-mq/cpy/` (CIPAUSMY, CIPAUDTY)

**Target:** Flatten to relational tables with JPA `@OneToMany` relationship:
- `PendingAuthorization` entity (from root segment PAUTSUM0)
- `PendingAuthorizationDetail` entity (from child segment PAUTDTL1) with `@ManyToOne` FK to parent

```
Current: CALL 'CBLTDLI' USING GU, PCB, IO-AREA, SSA
Target:  pendingAuthorizationRepository.findByCardNumber(cardNumber)

Current: CALL 'CBLTDLI' USING GN, PCB, DETAIL-IO-AREA
Target:  pendingAuthDetailRepository.findByAuthorization(authorization)
```

#### 3.2.3 MQ Integration (Asynchronous Messaging)

**Current:** CICS MQ API calls (MQGET/MQPUT) for request/response patterns.

**Files:**
- Programs: `app/app-vsam-mq/cbl/CODATE01.cbl`, `COACCT01.cbl`
- MQ Queues: `AWS.M2.CARDDEMO.PAUTH.REQUEST`, `AWS.M2.CARDDEMO.PAUTH.REPLY`
- Message formats: CSV-based (authorization), structured COBOL records (date/account)

**Target:** Spring AMQP with RabbitMQ (or Spring Kafka for event streaming):

```java
// Authorization Request Listener (replaces COPAUA0C MQ trigger)
@RabbitListener(queues = "carddemo.authorization.request")
public AuthorizationResponse processAuthorization(AuthorizationRequest request) { ... }

// System Date Inquiry (replaces CODATE01)
@RabbitListener(queues = "carddemo.system.date.request")
public DateResponse getSystemDate(DateRequest request) { ... }

// Account Inquiry (replaces COACCT01)
@RabbitListener(queues = "carddemo.account.request")
public AccountResponse getAccountDetails(AccountRequest request) { ... }
```

Alternatively, these can be exposed as simple REST endpoints since the MQ pattern was primarily used for system integration, which REST APIs handle natively in a microservices context.

---

## 4. Data Migration Strategy

### 4.1 Migration Approach Overview

| Phase | Activity | Tools |
|:------|:---------|:------|
| 1. Schema Creation | Generate PostgreSQL DDL from entity definitions | Flyway migrations / JPA `ddl-auto=validate` |
| 2. EBCDIC Data Extract | Extract VSAM data files to flat files (already in `app/data/`) | Existing JCL utilities or `CBEXPORT.cbl` |
| 3. Character Encoding | Convert EBCDIC to UTF-8 | `iconv` or custom Java converter |
| 4. Data Transformation | Parse fixed-length records per copybook layouts, convert packed decimals | Custom Spring Batch `ItemReader` with copybook-aware parsing |
| 5. Data Load | Insert transformed records into PostgreSQL | Spring Batch `JdbcBatchItemWriter` or JPA `saveAll()` |
| 6. Validation | Compare record counts and checksums | SQL count queries + hash comparison |

### 4.2 Copybook-to-DDL Migration Map

For each VSAM file, the migration path is:

```
VSAM Dataset (EBCDIC) 
  -> Extract to flat file (binary)
    -> Parse using copybook layout (COBOL PIC clauses define offsets and lengths)
      -> Convert EBCDIC encoding to UTF-8
        -> Transform packed decimal (COMP-3) and signed numeric fields
          -> Load into PostgreSQL table via Spring Batch
```

### 4.3 Key Data Type Conversions

| COBOL PIC Clause | Bytes | PostgreSQL Type | Java Type | Conversion Notes |
|:-----------------|:------|:----------------|:----------|:-----------------|
| PIC X(n) | n | VARCHAR(n) | String | EBCDIC -> UTF-8 character mapping |
| PIC 9(n) | n | BIGINT or INTEGER | Long / Integer | Strip leading zeros |
| PIC S9(n)V99 | n+2 (display) | DECIMAL(n+2, 2) | BigDecimal | Handle sign byte (trailing or leading) |
| PIC S9(n)V99 COMP-3 | ceil((n+3)/2) | DECIMAL(n+2, 2) | BigDecimal | Unpack BCD nibbles, extract sign nibble |
| PIC 9(n) COMP | 2 or 4 bytes | INTEGER / BIGINT | Integer / Long | Binary to integer conversion |
| PIC X(10) (date) | 10 | DATE | LocalDate | Parse with `DateTimeFormatter` |
| PIC X(26) (timestamp) | 26 | TIMESTAMP | LocalDateTime | Parse with `DateTimeFormatter` |

### 4.4 Sample Data Available

The repository includes sample data files in `app/data/` (EBCDIC format) that can be used for migration testing. The `CBEXPORT.cbl` and `CBIMPORT.cbl` programs (with copybook `app/cpy/CVEXPORT.cpy`) provide existing export/import patterns that inform the migration tooling design.

### 4.5 Cross-Reference Denormalization

The current VSAM cross-reference file (`CVACT03Y.cpy`) maintains card-to-customer-to-account relationships as a flat lookup. In the modernized schema, this can be handled via:

1. **Option A (Recommended):** Use JPA `@ManyToOne` relationships directly on the `Card` entity to reference `Account` and `Customer`, eliminating the need for a separate cross-reference table.

2. **Option B:** Retain the `card_xref` table as a join table if backward compatibility with batch processes is required during a transition period.

---

## 5. Security Architecture Transformation

### 5.1 Current Security Model (RACF + VSAM)

| Aspect | Current Implementation | Source Reference |
|:-------|:----------------------|:-----------------|
| Authentication | VSAM user security file lookup | `COSGN00C.cbl` reads `CSUSR01Y.cpy` records |
| Password Storage | Plaintext in VSAM file (PIC X(08)) | `SEC-USR-PWD` field in `CSUSR01Y.cpy` |
| User Roles | Single character flag: 'A' (Admin), 'U' (User) | `SEC-USR-TYPE` in `CSUSR01Y.cpy`; 88-level conditions `CDEMO-USRTYP-ADMIN`/`CDEMO-USRTYP-USER` in `COCOM01Y.cpy` |
| Session Management | CICS COMMAREA passed between transactions | `COCOM01Y.cpy` carries `CDEMO-USER-ID` and `CDEMO-USER-TYPE` |
| Menu Authorization | COBOL program checks `SEC-USR-TYPE` to show/hide menu options | `COMEN02Y.cpy` (user menu) and `COADM02Y.cpy` (admin menu) define role-gated options |
| Default Credentials | ADMIN001/PASSWORD (admin), USER0001/PASSWORD (user) | `README.md` lines 203-204 |

### 5.2 Target Security Model (Spring Security)

#### 5.2.1 Authentication

| Component | Implementation |
|:----------|:---------------|
| Authentication Provider | `DaoAuthenticationProvider` with custom `UserDetailsService` |
| Password Encoding | `BCryptPasswordEncoder` (replaces plaintext VSAM storage) |
| Token Management | JWT (JSON Web Token) for stateless authentication |
| Login Endpoint | `POST /api/auth/login` returns JWT access + refresh tokens |
| Token Validation | `JwtAuthenticationFilter` on every request |

#### 5.2.2 Authorization

| Component | Implementation |
|:----------|:---------------|
| Role Model | `ROLE_USER` and `ROLE_ADMIN` (maps from SEC-USR-TYPE 'U'/'A') |
| Method Security | `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints |
| URL Security | `SecurityFilterChain` bean with `.requestMatchers("/api/admin/**").hasRole("ADMIN")` |
| Menu Visibility | Frontend conditionally renders admin menu based on JWT role claim |

#### 5.2.3 Session Management

| Mainframe Concept | Spring Boot Equivalent |
|:------------------|:----------------------|
| CICS COMMAREA (`COCOM01Y.cpy`) | JWT payload containing userId, userType, and navigation context |
| `CDEMO-FROM-TRANID` / `CDEMO-TO-TRANID` | Frontend route history / React Router state |
| `CDEMO-FROM-PROGRAM` / `CDEMO-TO-PROGRAM` | Not needed (SPA handles navigation client-side) |
| `CDEMO-USER-ID` | JWT `sub` (subject) claim |
| `CDEMO-USER-TYPE` | JWT `roles` claim array |
| `CDEMO-PGM-CONTEXT` (ENTER/REENTER) | Frontend form state (new vs. edit mode) |
| `CDEMO-ACCT-ID`, `CDEMO-CARD-NUM`, `CDEMO-CUST-ID` | URL path parameters or query parameters |
| `CDEMO-LAST-MAP` / `CDEMO-LAST-MAPSET` | Browser history / React Router `useLocation()` |

#### 5.2.4 Security Configuration Skeleton

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### 5.2.5 User Migration

During migration, the 8-character plaintext passwords from the VSAM security file must be BCrypt-hashed before loading into PostgreSQL:

```
VSAM Record: SEC-USR-ID="ADMIN001" SEC-USR-PWD="PASSWORD" SEC-USR-TYPE="A"
    |
    v
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type)
VALUES ('ADMIN001', '...', '...', '$2a$10$...BCrypt hash of PASSWORD...', 'A');
```

---

## 6. Proposed Spring Boot Project Structure

```
carddemo-spring-boot/
├── src/main/java/com/carddemo/
│   ├── CardDemoApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   └── BatchConfig.java
│   ├── controller/
│   │   ├── AuthController.java           # CC00 (COSGN00C)
│   │   ├── AccountController.java        # CAVW, CAUP (COACTVWC, COACTUPC)
│   │   ├── CardController.java           # CCLI, CCDL, CCUP (COCRDLIC, COCRDSLC, COCRDUPC)
│   │   ├── TransactionController.java    # CT00, CT01, CT02 (COTRN00C-02C)
│   │   ├── ReportController.java         # CR00 (CORPT00C)
│   │   ├── BillPaymentController.java    # CB00 (COBIL00C)
│   │   ├── UserController.java           # CU00-CU03 (COUSR00C-03C)
│   │   ├── TransactionTypeController.java # CTTU, CTLI (COTRTUPC, COTRTLIC)
│   │   ├── AuthorizationController.java  # CPVS, CPVD (COPAUS0C, COPAUS1C)
│   │   └── SystemController.java         # CDRD, CDRA (CODATE01, COACCT01)
│   ├── service/
│   │   ├── AuthenticationService.java
│   │   ├── AccountService.java
│   │   ├── CardService.java
│   │   ├── CustomerService.java
│   │   ├── TransactionService.java
│   │   ├── ReportService.java
│   │   ├── BillPaymentService.java
│   │   ├── UserService.java
│   │   ├── TransactionTypeService.java
│   │   ├── AuthorizationService.java
│   │   ├── FraudService.java
│   │   └── InterestCalculationService.java
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── User.java                 # CSUSR01Y.cpy
│   │   │   ├── Account.java              # CVACT01Y.cpy
│   │   │   ├── Card.java                 # CVACT02Y.cpy
│   │   │   ├── Customer.java             # CVCUS01Y.cpy
│   │   │   ├── CardCrossReference.java   # CVACT03Y.cpy
│   │   │   ├── Transaction.java          # CVTRA05Y.cpy
│   │   │   ├── DailyTransaction.java     # CVTRA06Y.cpy
│   │   │   ├── TransactionType.java      # CVTRA03Y.cpy
│   │   │   ├── TransactionCategory.java  # CVTRA04Y.cpy
│   │   │   ├── TransactionCategoryBalance.java # CVTRA01Y.cpy
│   │   │   ├── DisclosureGroup.java      # CVTRA02Y.cpy
│   │   │   ├── PendingAuthorization.java # IMS PAUTSUM0
│   │   │   ├── PendingAuthorizationDetail.java # IMS PAUTDTL1
│   │   │   └── AuthorizationFraudRecord.java # DB2 AUTHFRDS
│   │   └── enums/
│   │       └── UserType.java             # A=ADMIN, U=USER
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── AccountRepository.java
│   │   ├── CardRepository.java
│   │   ├── CustomerRepository.java
│   │   ├── CardCrossReferenceRepository.java
│   │   ├── TransactionRepository.java
│   │   ├── DailyTransactionRepository.java
│   │   ├── TransactionTypeRepository.java
│   │   ├── TransactionCategoryRepository.java
│   │   ├── TransactionCategoryBalanceRepository.java
│   │   ├── DisclosureGroupRepository.java
│   │   ├── PendingAuthorizationRepository.java
│   │   └── AuthorizationFraudRecordRepository.java
│   ├── batch/
│   │   ├── jobs/
│   │   │   ├── TransactionPostingJobConfig.java    # POSTTRAN
│   │   │   ├── InterestCalculationJobConfig.java   # INTCALC
│   │   │   ├── StatementGenerationJobConfig.java   # CREASTMT
│   │   │   ├── TransactionReportJobConfig.java     # TRANREPT
│   │   │   ├── TransactionCombineJobConfig.java    # COMBTRAN
│   │   │   ├── AuthorizationPurgeJobConfig.java    # CBPAUP0J
│   │   │   └── DataRefreshJobConfig.java           # ACCTFILE, CARDFILE, CUSTFILE, etc.
│   │   └── processors/
│   │       ├── TransactionPostingProcessor.java    # CBTRN02C logic
│   │       ├── InterestCalculationProcessor.java   # CBACT04C logic
│   │       └── StatementProcessor.java             # CBSTM03A/B logic
│   ├── messaging/
│   │   ├── AuthorizationRequestListener.java       # CP00 MQ trigger
│   │   ├── SystemDateRequestListener.java          # CDRD
│   │   └── AccountRequestListener.java             # CDRA
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── CustomUserDetailsService.java
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── util/
│       └── DateTimeUtils.java            # CSUTLDTC.cbl + COBDATFT.asm
├── src/main/resources/
│   ├── application.yml
│   ├── db/migration/                     # Flyway migrations
│   │   ├── V1__create_users_table.sql
│   │   ├── V2__create_accounts_table.sql
│   │   ├── V3__create_cards_table.sql
│   │   ├── V4__create_customers_table.sql
│   │   ├── V5__create_transactions_tables.sql
│   │   ├── V6__create_reference_tables.sql
│   │   ├── V7__create_authorization_tables.sql
│   │   └── V8__seed_default_data.sql
│   └── reports/                          # Report templates (CVTRA07Y.cpy equivalent)
└── pom.xml
```

---

## 7. Summary of Key Metrics

| Metric | Count |
|:-------|:------|
| COBOL Programs (base) | 31 |
| COBOL Programs (optional modules) | 10 |
| Assembler Programs | 2 |
| Total Source Programs | 43 |
| BMS Map Definitions | 17 (base) + 4 (optional) = 21 |
| Copybooks (data) | 29 (base) + 5 (optional) = 34 |
| BMS Copybooks | 17 (base) + 4 (optional) = 21 |
| CICS Transactions | 18 (base) + 6 (optional) = 24 |
| JCL Jobs | 37 (base) + 3 (optional) = 40 |
| VSAM Datasets | 11 |
| DB2 Tables (optional) | 3 (TRANSACTION_TYPE, TRANSACTION_TYPE_CATEGORY, AUTHFRDS) |
| IMS Databases (optional) | 2 (DBPAUTP0, DBPAUTX0) |
| MQ Queues (optional) | 4 |
| Proposed JPA Entities | 14 |
| Proposed Spring Services | 12+ |
| Proposed REST Controllers | 10 |
| Proposed Spring Batch Jobs | 7+ |
| Proposed DB Migration Scripts | 8+ |
