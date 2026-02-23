# CardDemo Application Inventory Analysis

## Document Information
| Item | Detail |
|------|--------|
| **Document** | Comprehensive Inventory Analysis |
| **Application** | CardDemo - Mainframe Credit Card Management System |
| **Phase** | Phase 1.1 - Assessment & Architecture Design |
| **Version** | 1.0 |

---

## 1. COBOL Program Inventory

### 1.1 Online Programs (CICS)

| # | Program | Transaction | BMS Map | Function | Lines | Module | Dependencies |
|---|---------|-------------|---------|----------|-------|--------|-------------|
| 1 | `COSGN00C` | CC00 | COSGN00 | Signon Screen - Authenticates users against USRSEC VSAM file | ~650 | Core | CSUSR01Y, COCOM01Y, COMEN02Y, CSDAT01Y, CSMSG01Y, COTTL01Y |
| 2 | `COMEN01C` | CM00 | COMEN01 | Main Menu - Displays user/admin menu options based on user type | ~550 | Core | COCOM01Y, COMEN02Y, COADM02Y, CSDAT01Y, CSMSG01Y |
| 3 | `COACTVWC` | CAVW | COACTVW | Account View - Displays account details with card cross-reference lookup | ~480 | Core | CVACT01Y, CVACT03Y, CVCUS01Y, COCOM01Y, CSDAT01Y |
| 4 | `COACTUPC` | CAUP | COACTUP | Account Update - Modifies account information with validation | ~720 | Core | CVACT01Y, CVACT03Y, CVCUS01Y, COCOM01Y, CSDAT01Y |
| 5 | `COCRDLIC` | CCLI | COCRDLI | Credit Card List - Browses cards with forward/backward paging | ~600 | Core | CVACT02Y, CVACT03Y, COCOM01Y, CVCRD01Y, CSDAT01Y |
| 6 | `COCRDSLC` | CCDL | COCRDSL | Credit Card View - Displays card details and associated account | ~450 | Core | CVACT02Y, CVACT03Y, COCOM01Y, CSDAT01Y |
| 7 | `COCRDUPC` | CCUP | COCRDUP | Credit Card Update - Modifies card information with validation | ~680 | Core | CVACT02Y, CVACT03Y, COCOM01Y, CSDAT01Y |
| 8 | `COTRN00C` | CT00 | COTRN00 | Transaction List - Browses transactions with filtering and paging | ~750 | Core | CVTRA05Y, CVACT03Y, COCOM01Y, CSDAT01Y |
| 9 | `COTRN01C` | CT01 | COTRN01 | Transaction View - Displays transaction details | ~420 | Core | CVTRA05Y, CVACT03Y, COCOM01Y, CSDAT01Y |
| 10 | `COTRN02C` | CT02 | COTRN02 | Transaction Add - Creates new transactions with validation | ~850 | Core | CVTRA05Y, CVTRA06Y, CVACT03Y, CVACT01Y, COCOM01Y, CSDAT01Y |
| 11 | `CORPT00C` | CR00 | CORPT00 | Transaction Reports - Submits batch report generation with date range | ~380 | Core | COCOM01Y, CSDAT01Y |
| 12 | `COBIL00C` | CB00 | COBIL00 | Bill Payment - Processes bill payments against account balances | ~620 | Core | CVACT01Y, CVACT03Y, CVTRA05Y, COCOM01Y, CSDAT01Y |
| 13 | `COADM01C` | CA00 | COADM01 | Admin Menu - Displays administration options for admin users | ~350 | Core + DB2 Ext | COADM02Y, COCOM01Y, CSDAT01Y |
| 14 | `COUSR00C` | CU00 | COUSR00 | List Users - Browses security user records with paging | ~520 | Core | CSUSR01Y, COCOM01Y, CSDAT01Y |
| 15 | `COUSR01C` | CU01 | COUSR01 | Add User - Creates new security user records | ~480 | Core | CSUSR01Y, COCOM01Y, CSDAT01Y |
| 16 | `COUSR02C` | CU02 | COUSR02 | Update User - Modifies existing user security records | ~510 | Core | CSUSR01Y, COCOM01Y, CSDAT01Y |
| 17 | `COUSR03C` | CU03 | COUSR03 | Delete User - Removes user security records with confirmation | ~440 | Core | CSUSR01Y, COCOM01Y, CSDAT01Y |
| 18 | `CSUTLDTC` | - | - | Date Utility - Provides date formatting and validation services | ~200 | Core | CSDAT01Y, CSUTLDPY, CSUTLDWY |
| 19 | `COPAUA0C` | CP00 | - | Authorization Request Processor - MQ trigger processes auth requests | ~900 | IMS-DB2-MQ | CCPAURQY, CCPAURLY, CIPAUSMY, CIPAUDTY, CVACT03Y |
| 20 | `COPAUS0C` | CPVS | COPAU00 | Pending Authorization Summary - Displays auth summaries from IMS | ~600 | IMS-DB2-MQ | CIPAUSMY, COCOM01Y, CVACT03Y |
| 21 | `COPAUS1C` | CPVD | COPAU01 | Pending Authorization Details - Shows auth detail from IMS, marks fraud | ~700 | IMS-DB2-MQ | CIPAUDTY, CIPAUSMY, COCOM01Y |
| 22 | `COPAUS2C` | (Called) | - | Fraud Marking - Inserts fraud records into DB2 AUTHFRDS table | ~350 | IMS-DB2-MQ | CIPAUDTY, DB2 AUTHFRDS DCL |
| 23 | `COTRTUPC` | CTTU | COTRTUP | Transaction Type Add/Edit - Manages tran types via DB2 embedded SQL | ~650 | DB2 Ext | DB2 TRANSACTION_TYPE DCL |
| 24 | `COTRTLIC` | CTLI | COTRTLI | Transaction Type List - Lists/updates/deletes tran types with DB2 cursors | ~750 | DB2 Ext | DB2 TRANSACTION_TYPE DCL, TRANSACTION_TYPE_CATEGORY DCL |
| 25 | `CODATE01` | CDRD | - | System Date Inquiry via MQ - Demonstrates MQ request/response pattern | ~300 | MQ Ext | MQ queues |
| 26 | `COACCT01` | CDRA | - | Account Details Inquiry via MQ - Retrieves account data via MQ | ~400 | MQ Ext | CVACT01Y, MQ queues |

### 1.2 Batch Programs

| # | Program | Job | Function | Module | Dependencies |
|---|---------|-----|----------|--------|-------------|
| 1 | `CBACT01C` | - | Account File Processing - Reads and processes account master file | Core | CVACT01Y |
| 2 | `CBACT02C` | - | Account Data Validation - Validates account data integrity | Core | CVACT01Y, CVACT03Y |
| 3 | `CBACT03C` | - | Account Reporting - Generates account summary reports | Core | CVACT01Y |
| 4 | `CBACT04C` | INTCALC | Interest Calculations - Computes interest charges based on disclosure groups | Core | CVACT01Y, CVTRA01Y, CVTRA02Y |
| 5 | `CBCUS01C` | - | Customer File Processing - Reads and processes customer records | Core | CVCUS01Y |
| 6 | `CBEXPORT` | - | Data Export - Exports multi-record data to sequential file using CVEXPORT copybook | Core | CVEXPORT, CVCUS01Y, CVACT01Y, CVACT02Y, CVTRA05Y, CVACT03Y |
| 7 | `CBIMPORT` | - | Customer Data Import - Imports customer data from external sources | Core | CVCUS01Y |
| 8 | `CBSTM03A` | CREASTMT | Statement Generation - Produces transaction statements with report formatting | Core | CVTRA05Y, CVTRA07Y, CVACT01Y, CVACT03Y |
| 9 | `CBSTM03B` | - | Statement Generation (Alternate) - Alternate statement formatting | Core | CVTRA05Y, CVTRA07Y |
| 10 | `CBTRN01C` | - | Transaction File Processing - Reads and processes transaction records | Core | CVTRA05Y, CVTRA06Y |
| 11 | `CBTRN02C` | POSTTRAN | Transaction Posting - Posts daily transactions, updates balances and category totals | Core | CVTRA05Y, CVTRA06Y, CVACT01Y, CVTRA01Y, CVACT03Y |
| 12 | `CBTRN03C` | TRANREPT | Transaction Report - Generates daily transaction report with totals | Core | CVTRA05Y, CVTRA07Y, CVACT01Y, CVTRA03Y, CVTRA04Y |
| 13 | `COBSWAIT` | WAITSTEP | Wait Utility - Pauses batch job for specified time interval | Core | - |
| 14 | `CBPAUP0C` | CBPAUP0J | Purge Expired Authorizations - Removes expired IMS auth records, adjusts credit | IMS-DB2-MQ | CIPAUSMY, CIPAUDTY, CVACT01Y |
| 15 | `COBTUPDT` | MNTTRDB2 | Transaction Type Batch Maintenance - Updates tran types in DB2 batch mode | DB2 Ext | DB2 TRANSACTION_TYPE DCL |
| 16 | `PAUDBLOD` | - | IMS Database Load - Loads authorization data into IMS database | IMS-DB2-MQ | CIPAUSMY, CIPAUDTY |
| 17 | `PAUDBUNL` | - | IMS Database Unload - Unloads IMS authorization data | IMS-DB2-MQ | CIPAUSMY, CIPAUDTY |
| 18 | `DBUNLDGS` | - | IMS Database Unload (Generic Segment) - Generic IMS segment unload | IMS-DB2-MQ | IMS PCBs |

---

## 2. CICS Transaction Inventory

### 2.1 Core Transactions

| Transaction ID | Program | Function | User Type | BMS Map | Target REST Endpoint |
|---------------|---------|----------|-----------|---------|---------------------|
| CC00 | COSGN00C | Signon Screen | All | COSGN00 | `POST /api/auth/login` |
| CM00 | COMEN01C | Main Menu | All | COMEN01 | `GET /api/menu` |
| CAVW | COACTVWC | Account View | User | COACTVW | `GET /api/accounts/{id}` |
| CAUP | COACTUPC | Account Update | User | COACTUP | `PUT /api/accounts/{id}` |
| CCLI | COCRDLIC | Credit Card List | User | COCRDLI | `GET /api/cards` |
| CCDL | COCRDSLC | Credit Card View | User | COCRDSL | `GET /api/cards/{id}` |
| CCUP | COCRDUPC | Credit Card Update | User | COCRDUP | `PUT /api/cards/{id}` |
| CT00 | COTRN00C | Transaction List | User | COTRN00 | `GET /api/transactions` |
| CT01 | COTRN01C | Transaction View | User | COTRN01 | `GET /api/transactions/{id}` |
| CT02 | COTRN02C | Transaction Add | User | COTRN02 | `POST /api/transactions` |
| CR00 | CORPT00C | Transaction Reports | User | CORPT00 | `POST /api/reports/transactions` |
| CB00 | COBIL00C | Bill Payment | User | COBIL00 | `POST /api/payments` |
| CA00 | COADM01C | Admin Menu | Admin | COADM01 | `GET /api/admin/menu` |
| CU00 | COUSR00C | List Users | Admin | COUSR00 | `GET /api/admin/users` |
| CU01 | COUSR01C | Add User | Admin | COUSR01 | `POST /api/admin/users` |
| CU02 | COUSR02C | Update User | Admin | COUSR02 | `PUT /api/admin/users/{id}` |
| CU03 | COUSR03C | Delete User | Admin | COUSR03 | `DELETE /api/admin/users/{id}` |

### 2.2 Optional Module Transactions

| Transaction ID | Program | Function | Module | Target REST Endpoint |
|---------------|---------|----------|--------|---------------------|
| CP00 | COPAUA0C | Process Authorization Requests | IMS-DB2-MQ | `POST /api/authorizations/process` (JMS listener) |
| CPVS | COPAUS0C | Pending Authorization Summary | IMS-DB2-MQ | `GET /api/authorizations/summary/{accountId}` |
| CPVD | COPAUS1C | Pending Authorization Details | IMS-DB2-MQ | `GET /api/authorizations/{id}` |
| CTTU | COTRTUPC | Transaction Type Add/Edit | DB2 Ext | `POST/PUT /api/admin/transaction-types` |
| CTLI | COTRTLIC | Transaction Type List/Delete | DB2 Ext | `GET/DELETE /api/admin/transaction-types` |
| CDRD | CODATE01 | System Date Inquiry via MQ | MQ Ext | `GET /api/system/date` (JMS) |
| CDRA | COACCT01 | Account Details via MQ | MQ Ext | `GET /api/accounts/{id}/mq` (JMS) |

---

## 3. Batch Job Inventory (JCL)

### 3.1 Initialization Jobs

| Job | Program/Utility | Function | Frequency | Dependencies |
|-----|----------------|----------|-----------|-------------|
| DUSRSECJ | IEBGENER | Initial load of user security VSAM file from PS | One-time | USRSEC.PS source |
| ACCTFILE | IDCAMS | Define/load Account Master KSDS from PS data | One-time/Refresh | ACCTDATA.PS source |
| CARDFILE | IDCAMS | Define/load Card Master KSDS from PS data | One-time/Refresh | CARDDATA.PS source |
| CUSTFILE | IDCAMS | Define/load Customer Master KSDS from PS data | One-time/Refresh | CUSTDATA.PS source |
| XREFFILE | IDCAMS | Define/load Card-Account-Customer XREF KSDS | One-time/Refresh | CARDXREF.PS source |
| TRANFILE | IDCAMS | Copy initial transaction file to VSAM KSDS | One-time/Refresh | TRANSACT source |
| DISCGRP | IDCAMS | Load Disclosure Group data to VSAM | One-time/Refresh | DISCGRP.PS source |
| TRANCATG | IDCAMS | Load Transaction Category Types to VSAM | One-time/Refresh | TRANCATG.PS source |
| TRANTYPE | IDCAMS | Load Transaction Types to VSAM | One-time/Refresh | TRANTYPE.PS source |
| TCATBALF | IDCAMS | Load Transaction Category Balance to VSAM | One-time/Refresh | TCATBALF.PS source |
| DEFGDGB | IDCAMS | Define GDG (Generation Data Group) bases | One-time | None |
| DEFGDGD | IDCAMS | Define additional GDG bases for DB2 | One-time | DB2 Ext |
| CREADB21 | DSNTEP4 | Create DB2 database tables and load initial data | One-time | DB2 Ext |
| ESDSRRDS | IDCAMS | Create ESDS and RRDS VSAM files | One-time | None |

### 3.2 Daily Processing Jobs

| Job | Program/Utility | Function | Frequency | Dependencies |
|-----|----------------|----------|-----------|-------------|
| CLOSEFIL | IEFBR14 | Close VSAM files opened by CICS | Before batch | CICS region |
| POSTTRAN | CBTRN02C | Core transaction posting - processes daily transactions, updates account balances and category totals | Daily | CLOSEFIL, data files loaded |
| INTCALC | CBACT04C | Calculate interest charges based on disclosure group rates and category balances | Daily (after POSTTRAN) | POSTTRAN complete |
| TRANBKP | IDCAMS | Backup/refresh transaction master file | Daily | Before/after POSTTRAN |
| COMBTRAN | SORT | Combine system transactions with daily transactions | Daily | After POSTTRAN |
| CREASTMT | CBSTM03A | Generate transaction statements with report formatting | Daily | After COMBTRAN |
| TRANIDX | IDCAMS | Define/rebuild alternate index on transaction file | Daily | After TRANBKP |
| TRANREPT | CBTRN03C | Generate daily transaction report - submitted from CICS | On-demand | Transaction data |
| TRANEXTR | DSNTIAUL | Extract latest DB2 data for transaction types to VSAM-compatible files | Daily | DB2 Ext, DB2 available |
| OPENFIL | IEFBR14 | Reopen VSAM files in CICS | After batch | All batch complete |
| WAITSTEP | COBSWAIT | Wait/pause job for configurable time interval | As needed | None |
| CBPAUP0J | CBPAUP0C | Purge expired IMS authorization records, adjust available credit | Daily | IMS-DB2-MQ Ext |
| MNTTRDB2 | COBTUPDT | Batch maintenance of transaction types in DB2 | As needed | DB2 Ext |

### 3.3 Recommended Batch Execution Sequence

```
1. CLOSEFIL   - Close CICS files
2. ACCTFILE   - Refresh Account Master
3. CARDFILE   - Refresh Card Master
4. XREFFILE   - Refresh Cross-Reference
5. CUSTFILE   - Refresh Customer Master
6. TRANBKP    - Backup Transactions
7. TRANEXTR   - Extract DB2 Transaction Types (optional)
8. TRANCATG   - Refresh Transaction Categories
9. TRANTYPE   - Refresh Transaction Types
10. DISCGRP   - Refresh Disclosure Groups
11. TCATBALF  - Refresh Category Balances
12. DUSRSECJ  - Refresh User Security
13. POSTTRAN  - Process Transactions
14. INTCALC   - Calculate Interest
15. TRANBKP   - Backup Updated Transactions
16. COMBTRAN  - Combine Transactions
17. CREASTMT  - Generate Statements
18. TRANIDX   - Rebuild Transaction Index
19. OPENFIL   - Reopen CICS Files
20. CBPAUP0J  - Purge Expired Authorizations (optional)
```

---

## 4. VSAM File Inventory

| # | Dataset Name | Type | Copybook | Record Length | Key Field(s) | Purpose |
|---|-------------|------|----------|--------------|---------------|---------|
| 1 | AWS.M2.CARDDEMO.USRSEC.PS | KSDS | CSUSR01Y | 80 | SEC-USR-ID (8 bytes) | User security/authentication records |
| 2 | AWS.M2.CARDDEMO.ACCTDATA.PS | KSDS | CVACT01Y | 300 | ACCT-ID (11 digits) | Account master records |
| 3 | AWS.M2.CARDDEMO.CARDDATA.PS | KSDS | CVACT02Y | 150 | CARD-NUM (16 bytes) | Credit card master records |
| 4 | AWS.M2.CARDDEMO.CUSTDATA.PS | KSDS | CVCUS01Y | 500 | CUST-ID (9 digits) | Customer master records |
| 5 | AWS.M2.CARDDEMO.CARDXREF.PS | KSDS | CVACT03Y | 50 | XREF-CARD-NUM (16 bytes) | Card-Account-Customer cross-reference |
| 6 | AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | KSDS | CVTRA05Y | 350 | TRAN-ID (16 bytes) | Online transaction records |
| 7 | AWS.M2.CARDDEMO.DALYTRAN.PS | Sequential | CVTRA06Y | 350 | DALYTRAN-ID (16 bytes) | Daily transaction input for posting |
| 8 | AWS.M2.CARDDEMO.DALYTRAN.PS.INIT | Sequential | CVTRA06Y | 350 | - | Transaction initialization record |
| 9 | AWS.M2.CARDDEMO.DISCGRP.PS | KSDS | CVTRA02Y | 50 | DIS-GROUP-KEY (16 bytes) | Disclosure group interest rates |
| 10 | AWS.M2.CARDDEMO.TRANCATG.PS | KSDS | CVTRA04Y | 60 | TRAN-CAT-KEY (6 bytes) | Transaction category definitions |
| 11 | AWS.M2.CARDDEMO.TRANTYPE.PS | KSDS | CVTRA03Y | 60 | TRAN-TYPE (2 bytes) | Transaction type definitions |
| 12 | AWS.M2.CARDDEMO.TCATBALF.PS | KSDS | CVTRA01Y | 50 | TRAN-CAT-KEY (17 bytes) | Transaction category running balances |

---

## 5. Copybook Inventory

### 5.1 Data Structure Copybooks

| Copybook | Record Length | Purpose | Used By | Target Java Entity |
|----------|-------------|---------|---------|-------------------|
| CSUSR01Y | 80 | User Security record structure | COSGN00C, COUSR00C-03C | UserSecurity.java |
| CVACT01Y | 300 | Account master record structure | COACTVWC, COACTUPC, CBACT*, CBTRN02C | Account.java |
| CVACT02Y | 150 | Card master record structure | COCRDLIC, COCRDSLC, COCRDUPC | Card.java |
| CVCUS01Y | 500 | Customer master record structure | CBCUS01C, CBIMPORT, COACTVWC | Customer.java |
| CVACT03Y | 50 | Card-Account-Customer cross-reference | Most online programs, CBTRN02C | CardAccountCustomerXref.java |
| CVTRA05Y | 350 | Transaction record structure | COTRN00C-02C, CBTRN01C-03C, CBSTM03A | Transaction.java |
| CVTRA06Y | 350 | Daily transaction record (same layout as CVTRA05Y) | COTRN02C, CBTRN01C, CBTRN02C | DailyTransaction.java |
| CVTRA01Y | 50 | Transaction category balance | CBACT04C, CBTRN02C | TransactionCategoryBalance.java |
| CVTRA02Y | 50 | Disclosure group (interest rates) | CBACT04C | DisclosureGroup.java |
| CVTRA03Y | 60 | Transaction type definitions | CBTRN03C | TransactionType.java |
| CVTRA04Y | 60 | Transaction category definitions | CBTRN03C | TransactionCategory.java |
| CVTRA07Y | 74 (variable) | Transaction report formatting | CBSTM03A, CBTRN03C | (Report DTO) |
| CVEXPORT | 500 | Multi-record export layout with REDEFINES | CBEXPORT | (Export DTOs) |
| CUSTREC | - | Customer record (alternate) | CBIMPORT | (Reuse Customer.java) |

### 5.2 Communication/Control Copybooks

| Copybook | Purpose | Used By |
|----------|---------|---------|
| COCOM01Y | CICS COMMAREA - Communication area between programs | All online programs |
| COMEN02Y | Main menu options definition with REDEFINES | COMEN01C |
| COADM02Y | Admin menu options definition | COADM01C |
| CVCRD01Y | Credit card work areas and AID key definitions | COCRDLIC, COCRDSLC |
| CSDAT01Y | Date/time formatting work areas | All programs |
| CSMSG01Y | Standard message formatting | Online programs |
| CSMSG02Y | Extended message formatting | Online programs |
| CSSETATY | Set attribute bytes for BMS | Online programs |
| CSSTRPFY | String processing functions | Online programs |
| CSLKPCDY | Lookup code definitions | Online programs |
| CSUTLDPY | Date utility parameters | CSUTLDTC |
| CSUTLDWY | Date utility work areas | CSUTLDTC |
| CODATECN | Date conversion routines | Various |
| COTTL01Y | Screen title formatting | Online programs |
| COSTM01 | Statement formatting | CBSTM03A |

### 5.3 IMS/DB2/MQ Copybooks (Optional Modules)

| Copybook | Purpose | Module |
|----------|---------|--------|
| CIPAUSMY | IMS Pending Authorization Summary segment | IMS-DB2-MQ |
| CIPAUDTY | IMS Pending Authorization Detail segment | IMS-DB2-MQ |
| CCPAURQY | Authorization Request message structure | IMS-DB2-MQ |
| CCPAURLY | Authorization Response message structure | IMS-DB2-MQ |
| CCPAUERY | Authorization error logging structure | IMS-DB2-MQ |
| IMSFUNCS | IMS function code definitions | IMS-DB2-MQ |
| PADFLPCB | IMS PCB (Program Communication Block) - Detail | IMS-DB2-MQ |
| PASFLPCB | IMS PCB - Summary | IMS-DB2-MQ |
| PAUTBPCB | IMS PCB - Batch | IMS-DB2-MQ |

---

## 6. BMS Map Inventory

| Mapset | Map | Program | Screen Function |
|--------|-----|---------|----------------|
| COSGN00 | COSGN0A | COSGN00C | Signon screen with userid/password fields |
| COMEN01 | COMEN1A | COMEN01C | Main menu with numbered options |
| COACTVW | COACTVWA | COACTVWC | Account view display |
| COACTUP | COACTUPA | COACTUPC | Account update form |
| COCRDLI | COCRDIA | COCRDLIC | Credit card list with scrollable display |
| COCRDSL | COCRDSA | COCRDSLC | Credit card detail view |
| COCRDUP | COCRDPA | COCRDUPC | Credit card update form |
| COTRN00 | COTRN0A | COTRN00C | Transaction list with filters |
| COTRN01 | COTRN1A | COTRN01C | Transaction detail view |
| COTRN02 | COTRN2A | COTRN02C | Transaction add form |
| CORPT00 | CORPT0A | CORPT00C | Report parameters (date range) |
| COBIL00 | COBIL0A | COBIL00C | Bill payment form |
| COADM01 | COADM1A | COADM01C | Admin menu |
| COUSR00 | COUSR0A | COUSR00C | User list display |
| COUSR01 | COUSR1A | COUSR01C | User add form |
| COUSR02 | COUSR2A | COUSR02C | User update form |
| COUSR03 | COUSR3A | COUSR03C | User delete confirmation |

---

## 7. Optional Module Inventory

### 7.1 Credit Card Authorizations (IMS-DB2-MQ)

**Location**: `app/app-authorization-ims-db2-mq/`

| Component Type | Items |
|---------------|-------|
| COBOL Programs | COPAUA0C, COPAUS0C, COPAUS1C, COPAUS2C, CBPAUP0C, PAUDBLOD, PAUDBUNL, DBUNLDGS |
| BMS Maps | COPAU00, COPAU01 |
| Copybooks | CIPAUSMY, CIPAUDTY, CCPAURQY, CCPAURLY, CCPAUERY, IMSFUNCS, PADFLPCB, PASFLPCB, PAUTBPCB |
| IMS DBDs | DBPAUTP0 (HIDAM primary), DBPAUTX0 (HIDAM index) |
| IMS PSBs | PSBPAUTB (BMP), PSBPAUTL (Load) |
| IMS Segments | PAUTSUM0 (root), PAUTDTL1 (child), PAUTINDX (index) |
| DB2 Tables | AUTHFRDS (fraud tracking) |
| MQ Queues | AWS.M2.CARDDEMO.PAUTH.REQUEST, AWS.M2.CARDDEMO.PAUTH.REPLY |
| JCL Jobs | CBPAUP0J |

### 7.2 Transaction Type Management (DB2)

**Location**: `app/app-transaction-type-db2/`

| Component Type | Items |
|---------------|-------|
| COBOL Programs | COTRTUPC, COTRTLIC, COBTUPDT |
| BMS Maps | COTRTUP, COTRTLI |
| DB2 Tables | CARDDEMO.TRANSACTION_TYPE, CARDDEMO.TRANSACTION_TYPE_CATEGORY |
| DDL Scripts | TRNTYPE.ddl, TRNTYCAT.ddl, XTRNTYPE.ddl, XTRNTYCAT.ddl |
| JCL Jobs | CREADB21, TRANEXTR, MNTTRDB2 |

### 7.3 Account Extractions (MQ)

**Location**: `app/app-vsam-mq/`

| Component Type | Items |
|---------------|-------|
| COBOL Programs | CODATE01, COACCT01 |
| MQ Queues | CARDDEMO.REQUEST.QUEUE, CARDDEMO.RESPONSE.QUEUE |
| Transactions | CDRD, CDRA |

---

## 8. Complexity Assessment Summary

| Category | Count |
|----------|-------|
| Total COBOL Programs (Core) | 31 |
| Total COBOL Programs (Optional) | 13 |
| **Total COBOL Programs** | **44** |
| Total COBOL Lines (Core) | ~20,650 |
| Online CICS Transactions (Core) | 17 |
| Online CICS Transactions (Optional) | 7 |
| **Total CICS Transactions** | **24** |
| Batch Jobs (Core) | ~17 |
| Batch Jobs (Optional) | ~5 |
| **Total Batch Jobs** | **~22** |
| VSAM KSDS Files | 12 |
| BMS Maps | 17+ |
| Copybooks (Core) | 29 |
| Copybooks (Optional) | 9 |
| **Total Copybooks** | **38** |
| IMS Databases | 2 |
| DB2 Tables | 3 |
| MQ Queues | 4 |
