# CardDemo COBOL/CICS to Spring Boot Migration Plan

## Executive Summary

This document provides a comprehensive inventory and migration blueprint for converting the CardDemo mainframe application from COBOL/CICS to Java Spring Boot. The CardDemo application is a credit card management system that handles customer accounts, credit cards, transactions, user authentication, reporting, and bill payment functionality.

---

## 1. System Inventory

### 1.1 Online (CICS) Programs

The application contains **28 online CICS programs** in `app/cbl/`:

| Program | Purpose | BMS Map | Transaction ID | Data Files |
|---------|---------|---------|----------------|------------|
| **COSGN00C** | User sign-on/authentication | COSGN00 | CSGN | USRSEC (user security) |
| **COMEN01C** | Main menu navigation | COMEN01 | CMEN | None (navigation only) |
| **COADM01C** | Admin menu navigation | COADM01 | CADM | None (navigation only) |
| **COACTVWC** | Account view (read-only) | COACTVW | CACV | ACCTDAT, CUSTDAT, CARDXREF |
| **COACTUPC** | Account update | COACTUP | CACU | ACCTDAT, CUSTDAT, CARDXREF |
| **COCRDLIC** | Credit card list with pagination | COCRDLI | CCLI | CARDDAT, CARDXREF, ACCTDAT |
| **COCRDSLC** | Credit card detail view | COCRDSL | CCSL | CARDDAT, CARDXREF |
| **COCRDUPC** | Credit card update | COCRDUP | CCUP | CARDDAT, CARDXREF |
| **COTRN00C** | Transaction list with pagination | COTRN00 | CTR0 | TRANSACT, CARDXREF |
| **COTRN01C** | Transaction detail view | COTRN01 | CTR1 | TRANSACT, TRANTYPE, TRANCATG |
| **COTRN02C** | Transaction add | COTRN02 | CTR2 | TRANSACT, CARDXREF, ACCTDAT |
| **CORPT00C** | Transaction reports (monthly/yearly/custom) | CORPT00 | CRPT | TRANSACT (submits batch) |
| **COBIL00C** | Bill payment | COBIL00 | CBIL | ACCTDAT, TRANSACT |
| **COUSR00C** | User list with pagination | COUSR00 | CUS0 | USRSEC |
| **COUSR01C** | User add | COUSR01 | CUS1 | USRSEC |
| **COUSR02C** | User update | COUSR02 | CUS2 | USRSEC |
| **COUSR03C** | User delete | COUSR03 | CUS3 | USRSEC |
| **COACCT01** | Account data extraction (MQ variant) | N/A | N/A | ACCTDAT |
| **CODATE01** | Date processing utility | N/A | N/A | None |
| **CBTRN01C** | Transaction browse utility | N/A | N/A | TRANSACT |
| **CBTRN02C** | Transaction posting (batch) | N/A | N/A | TRANSACT, DALYTRAN, CARDXREF, ACCTDAT, TCATBALF |
| **CBTRN03C** | Transaction report generation | N/A | N/A | TRANSACT, CARDXREF, TRANTYPE, TRANCATG |
| **CBACT01C** | Account processing | N/A | N/A | ACCTDAT |
| **CBACT02C** | Account validation | N/A | N/A | ACCTDAT |
| **CBACT03C** | Account balance update | N/A | N/A | ACCTDAT |
| **CBACT04C** | Interest calculation | N/A | N/A | TCATBALF, CARDXREF, ACCTDAT, DISCGRP |
| **CBCUS01C** | Customer processing | N/A | N/A | CUSTDAT |
| **CBSTM03A/B** | Statement generation | N/A | N/A | ACCTDAT, TRANSACT, CUSTDAT |

#### Program Flow Architecture

```
COSGN00C (Sign-on)
    │
    ├── Admin User ──► COADM01C (Admin Menu)
    │                      ├── COUSR00C (List Users)
    │                      │      ├── COUSR02C (Update User)
    │                      │      └── COUSR03C (Delete User)
    │                      └── COUSR01C (Add User)
    │
    └── Regular User ──► COMEN01C (Main Menu)
                             ├── COACTVWC (View Account)
                             ├── COACTUPC (Update Account)
                             ├── COCRDLIC (List Cards)
                             │      ├── COCRDSLC (View Card)
                             │      └── COCRDUPC (Update Card)
                             ├── COTRN00C (List Transactions)
                             │      ├── COTRN01C (View Transaction)
                             │      └── COTRN02C (Add Transaction)
                             ├── CORPT00C (Reports)
                             └── COBIL00C (Bill Payment)
```

### 1.2 Batch Programs

| Program | Purpose | Input Files | Output Files |
|---------|---------|-------------|--------------|
| **CBTRN02C** | Daily transaction posting | DALYTRAN (daily transactions), CARDXREF | TRANSACT, TCATBALF, DALYREJS (rejected) |
| **CBACT04C** | Interest/fee calculation | TCATBALF, CARDXREF, ACCTDAT, DISCGRP | SYSTRAN (system transactions) |
| **CBTRN03C** | Transaction report generation | TRANSACT, CARDXREF, TRANTYPE, TRANCATG | TRANREPT (formatted report) |
| **CBSTM03A** | Statement generation (Part A) | ACCTDAT, TRANSACT, CUSTDAT | Statement files |
| **CBSTM03B** | Statement generation (Part B) | Statement files | Final statements |
| **CBEXPORT** | Data export for migration | CUSTDAT, ACCTDAT, CARDXREF, TRANSACT, CARDDAT | EXPORT.DATA (multi-record) |
| **CBIMPORT** | Data import from export file | EXPORT.DATA | CUSTDAT, ACCTDAT, CARDXREF, TRANSACT (normalized) |

### 1.3 Copybooks (`app/cpy/`)

The application contains **30 copybooks** defining data structures:

#### Core Entity Copybooks

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| **CVACT01Y** | Account record layout | ACCT-ID (11), ACCT-ACTIVE-STATUS, ACCT-CURR-BAL, ACCT-CREDIT-LIMIT, ACCT-CASH-CREDIT-LIMIT, ACCT-OPEN-DATE, ACCT-EXPIRAION-DATE, ACCT-REISSUE-DATE, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT, ACCT-GROUP-ID |
| **CVACT02Y** | Account record (alternate) | Similar to CVACT01Y with different layout |
| **CVACT03Y** | Account cross-reference | XREF-ACCT-ID, XREF-CARD-NUM, XREF-CUST-ID |
| **CVCUS01Y** | Customer record | CUST-ID (9), CUST-FIRST-NAME, CUST-MIDDLE-NAME, CUST-LAST-NAME, CUST-ADDR-LINE-1/2/3, CUST-ADDR-STATE-CD, CUST-ADDR-ZIP, CUST-ADDR-COUNTRY-CD, CUST-PHONE-NUM-1/2, CUST-SSN, CUST-GOVT-ISSUED-ID, CUST-DOB-YYYY-MM-DD, CUST-EFT-ACCOUNT-ID, CUST-PRI-CARD-HOLDER-IND, CUST-FICO-CREDIT-SCORE |
| **CVCRD01Y** | Card work areas | CC-ACCT-ID, CC-CARD-NUM, CC-CUST-ID, CCARD-AID (key handling) |
| **CUSTREC** | Customer entity (duplicate) | Same as CVCUS01Y |

#### Transaction Copybooks

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| **CVTRA01Y** | Transaction record | TRAN-ID (16), TRAN-TYPE-CD, TRAN-CAT-CD, TRAN-SOURCE, TRAN-DESC, TRAN-AMT, TRAN-CARD-NUM, TRAN-MERCHANT-ID/NAME/CITY/ZIP, TRAN-ORIG-TS, TRAN-PROC-TS |
| **CVTRA02Y** | Transaction type | TRAN-TYPE-CD (2), TRAN-TYPE-DESC |
| **CVTRA03Y** | Transaction category | TRAN-CAT-CD (4), TRAN-CAT-TYPE-CD, TRAN-CAT-DESC |
| **CVTRA04Y** | Daily transaction input | DALYTRAN-* fields for batch input |
| **CVTRA05Y** | Transaction category balance | TCATBAL-* fields for category totals |
| **CVTRA06Y** | Discount group | DISCGRP-* fields for interest rate groups |
| **CVTRA07Y** | Card cross-reference | CARD-XREF-* fields |

#### System/Communication Copybooks

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| **COCOM01Y** | Inter-program communication area (COMMAREA) | CDEMO-FROM-TRANID, CDEMO-FROM-PROGRAM, CDEMO-TO-TRANID, CDEMO-TO-PROGRAM, CDEMO-USER-ID, CDEMO-USER-TYPE (A=Admin, U=User), CDEMO-CUST-ID, CDEMO-ACCT-ID, CDEMO-CARD-NUM |
| **CSUSR01Y** | User security record | SEC-USR-ID (8), SEC-USR-FNAME (20), SEC-USR-LNAME (20), SEC-USR-PWD (8), SEC-USR-TYPE (1) |
| **COADM02Y** | Admin menu options | 6 menu options (User CRUD + DB2 transaction type) |
| **COMEN02Y** | Main menu options | 11 menu options (Account, Card, Transaction, Report, Bill Payment) |
| **COTTL01Y** | Screen title constants | CCDA-TITLE01, CCDA-TITLE02 |

#### Utility Copybooks

| Copybook | Purpose |
|----------|---------|
| **CSDAT01Y** | Date/time working storage structures |
| **CSMSG01Y** | Common message constants |
| **CSMSG02Y** | Abend data structures |
| **CSSETATY** | Screen attribute setting logic |
| **CSSTRPFY** | PF key mapping (ENTER, CLEAR, PA1-2, PF1-24) |
| **CSUTLDPY** | Date validation procedure division code |
| **CSUTLDWY** | Date validation working storage |
| **CSLKPCDY** | Lookup codes (phone area codes, state codes, state+zip combinations) |
| **CODATECN** | Date conversion record structure |
| **COSTM01** | Transaction record layout for reporting |
| **CVEXPORT** | Multi-record export layout for branch migration |
| **UNUSED1Y** | Unused data structure |

### 1.4 BMS Maps (`app/bms/`)

The application contains **17 BMS screen definitions**:

| Map | Screen | Key Fields |
|-----|--------|------------|
| **COSGN00** | Sign-on/Login | USERID (8), PASSWD (8, dark), ERRMSG |
| **COMEN01** | Main Menu | OPTION (2), OPTN001-012 (menu items), ERRMSG |
| **COADM01** | Admin Menu | OPTION (2), OPTN001-012 (menu items), ERRMSG |
| **COACTVW** | Account View (read-only) | ACCTSID, ACSTTUS, ADTOPEN, ACRDLIM, AEXPDT, ACSHLIM, AREISDT, ACURBAL, ACRCYCR, ACRCYDB, customer/address fields |
| **COACTUP** | Account Update (editable) | Same as COACTVW with UNPROT attribute |
| **COCRDLI** | Card List | ACCTSID, CARDSID, PAGENO, 7 rows (CRDSEL, ACCTNO, CRDNUM, CRDSTS) |
| **COCRDSL** | Card Detail View | ACCTSID, CARDSID, CRDNAME, CRDSTCD, EXPMON, EXPYEAR |
| **COCRDUP** | Card Update | Same as COCRDSL with UNPROT for editing |
| **COTRN00** | Transaction List | TRNIDIN (search), PAGENUM, 10 rows (SEL, TRNID, TDATE, TDESC, TAMT) |
| **COTRN01** | Transaction View | TRNIDIN, TRNID, CARDNUM, TTYPCD, TCATCD, TRNSRC, TDESC, TRNAMT, TORIGDT, TPROCDT, MID, MNAME, MCITY, MZIP |
| **COTRN02** | Transaction Add | ACTIDIN/CARDNIN, TTYPCD, TCATCD, TRNSRC, TDESC, TRNAMT, TORIGDT, TPROCDT, MID, MNAME, MCITY, MZIP, CONFIRM |
| **CORPT00** | Reports | MONTHLY, YEARLY, CUSTOM checkboxes, SDTMM/DD/YYYY, EDTMM/DD/YYYY, CONFIRM |
| **COBIL00** | Bill Payment | ACTIDIN, CURBAL, CONFIRM |
| **COUSR00** | User List | USRIDIN (search), PAGENUM, 10 rows (SEL, USRID, FNAME, LNAME, UTYPE) |
| **COUSR01** | User Add | FNAME, LNAME, USERID, PASSWD, USRTYPE |
| **COUSR02** | User Update | Same as COUSR01 |
| **COUSR03** | User Delete | User details (read-only), CONFIRM |

### 1.5 JCL Jobs (`app/jcl/`)

The application contains **39 JCL jobs**:

#### Data Definition Jobs

| Job | Purpose |
|-----|---------|
| **ACCTFILE** | Define ACCTDATA VSAM cluster |
| **CARDFILE** | Define CARDDATA VSAM cluster |
| **CUSTFILE** | Define CUSTDATA VSAM cluster |
| **TRANFILE** | Define TRANSACT VSAM cluster |
| **XREFFILE** | Define CARDXREF VSAM cluster |
| **TRANTYPE** | Define TRANTYPE VSAM cluster |
| **TRANCATG** | Define TRANCATG VSAM cluster |
| **TCATBALF** | Define TCATBALF (transaction category balance) VSAM cluster |
| **DISCGRP** | Define DISCGRP (discount group) VSAM cluster |
| **REPTFILE** | Define report output files |
| **DEFGDGB/DEFGDGD** | Define GDG (Generation Data Group) bases |
| **ESDSRRDS** | Define ESDS/RRDS datasets |

#### Batch Processing Jobs

| Job | Purpose | Program |
|-----|---------|---------|
| **POSTTRAN** | Post daily transactions to master | CBTRN02C |
| **INTCALC** | Calculate interest and fees | CBACT04C |
| **TRANREPT** | Generate transaction reports | CBTRN03C |
| **COMBTRAN** | Combine and sort transactions | SORT + IDCAMS |
| **TRANBKP** | Backup transaction file | IDCAMS REPRO |
| **TRANIDX** | Build transaction indexes | IDCAMS |
| **DALYREJS** | Process daily rejects | Utility |
| **PRTCATBL** | Print category balance | Utility |

#### Data Management Jobs

| Job | Purpose |
|-----|---------|
| **CBEXPORT** | Export all data for migration |
| **CBIMPORT** | Import data from export file |
| **READACCT** | Read/dump account file |
| **READCARD** | Read/dump card file |
| **READCUST** | Read/dump customer file |
| **READXREF** | Read/dump cross-reference file |
| **OPENFIL** | Open VSAM files for processing |
| **CLOSEFIL** | Close VSAM files |

#### Utility Jobs

| Job | Purpose |
|-----|---------|
| **DUSRSECJ** | Define user security file |
| **CBADMCDJ** | Admin card processing |
| **DEFCUST** | Define customer data |
| **FTPJCL** | FTP data transfer |
| **INTRDRJ1/2** | Internal reader jobs |
| **TXT2PDF1** | Convert text to PDF |
| **WAITSTEP** | Wait/delay step |

### 1.6 Data Files (VSAM Datasets)

| Dataset | Type | Key | Record Size | Purpose |
|---------|------|-----|-------------|---------|
| **ACCTDATA.VSAM.KSDS** | KSDS | ACCT-ID (11) | ~300 bytes | Account master |
| **CARDDATA.VSAM.KSDS** | KSDS | CARD-NUM (16) | ~150 bytes | Card master |
| **CUSTDATA.VSAM.KSDS** | KSDS | CUST-ID (9) | ~500 bytes | Customer master |
| **TRANSACT.VSAM.KSDS** | KSDS | TRAN-ID (16) | 350 bytes | Transaction master |
| **CARDXREF.VSAM.KSDS** | KSDS | CARD-NUM (16) | ~50 bytes | Card-Account-Customer cross-reference |
| **CARDXREF.VSAM.AIX.PATH** | AIX | ACCT-ID | - | Alternate index by account |
| **USRSEC.VSAM.KSDS** | KSDS | USER-ID (8) | 80 bytes | User security |
| **TRANTYPE.VSAM.KSDS** | KSDS | TYPE-CD (2) | ~50 bytes | Transaction type codes |
| **TRANCATG.VSAM.KSDS** | KSDS | CAT-CD (4) | ~50 bytes | Transaction category codes |
| **TCATBALF.VSAM.KSDS** | KSDS | Composite | ~100 bytes | Transaction category balances |
| **DISCGRP.VSAM.KSDS** | KSDS | GROUP-ID | ~50 bytes | Discount/interest rate groups |
| **DALYTRAN.PS** | Sequential | N/A | 430 bytes | Daily transaction input |
| **DALYREJS.GDG** | GDG | N/A | 430 bytes | Daily rejected transactions |
| **SYSTRAN.GDG** | GDG | N/A | 350 bytes | System-generated transactions |

---

## 2. Optional Modules Inventory

The following optional modules are referenced in documentation but **not present in the current repository**:

### 2.1 app-authorization-ims-db2-mq
- **Purpose**: IMS DB-based authorization with MQ messaging and DB2 logging
- **Status**: Not found in repository
- **Migration Note**: If implemented, would require Spring Integration for MQ and JPA for DB2 replacement

### 2.2 app-transaction-type-db2
- **Purpose**: DB2-based transaction type management with CICS online and batch interfaces
- **Status**: Not found in repository
- **Migration Note**: Transaction types are currently in VSAM (TRANTYPE.VSAM.KSDS)

### 2.3 app-vsam-mq
- **Purpose**: MQ-based account data extraction
- **Programs**: COACCT01, CODATE01
- **Status**: Programs exist but MQ integration not present
- **Migration Note**: Would map to REST APIs or Spring Integration

---

## 3. Data Model & Relationships

### 3.1 Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   CUSTOMER  │       │   ACCOUNT   │       │    CARD     │
│─────────────│       │─────────────│       │─────────────│
│ CUST-ID (PK)│◄──┐   │ ACCT-ID (PK)│◄──┐   │ CARD-NUM(PK)│
│ FIRST-NAME  │   │   │ ACTIVE-STS  │   │   │ ACCT-ID(FK) │──┐
│ MIDDLE-NAME │   │   │ CURR-BAL    │   │   │ CUST-ID(FK) │──┼─┐
│ LAST-NAME   │   │   │ CREDIT-LIM  │   │   │ CARD-NAME   │  │ │
│ ADDR-LINE-1 │   │   │ CASH-LIM    │   │   │ ACTIVE-STS  │  │ │
│ ADDR-LINE-2 │   │   │ OPEN-DATE   │   │   │ EXPIRY-DATE │  │ │
│ CITY        │   │   │ EXPIRY-DATE │   │   └─────────────┘  │ │
│ STATE       │   │   │ REISSUE-DT  │   │                    │ │
│ ZIP         │   │   │ CYC-CREDIT  │   │                    │ │
│ COUNTRY     │   │   │ CYC-DEBIT   │   │                    │ │
│ PHONE-1     │   │   │ GROUP-ID    │   │                    │ │
│ PHONE-2     │   │   └─────────────┘   │                    │ │
│ SSN         │   │          ▲          │                    │ │
│ GOVT-ID     │   │          │          │                    │ │
│ DOB         │   │   ┌──────┴──────┐   │                    │ │
│ EFT-ACCT    │   │   │  CARD-XREF  │   │                    │ │
│ PRI-HOLDER  │   │   │─────────────│   │                    │ │
│ FICO-SCORE  │   └───│ CUST-ID(FK) │◄──┼────────────────────┘ │
└─────────────┘       │ ACCT-ID(FK) │───┘                      │
                      │ CARD-NUM(FK)│◄─────────────────────────┘
                      └─────────────┘
                             │
                             ▼
                      ┌─────────────┐
                      │ TRANSACTION │
                      │─────────────│
                      │ TRAN-ID (PK)│
                      │ CARD-NUM(FK)│
                      │ TYPE-CD(FK) │──────► TRAN-TYPE
                      │ CAT-CD(FK)  │──────► TRAN-CATEGORY
                      │ SOURCE      │
                      │ DESC        │
                      │ AMOUNT      │
                      │ MERCHANT-ID │
                      │ MERCHANT-NM │
                      │ MERCHANT-CT │
                      │ MERCHANT-ZP │
                      │ ORIG-TS     │
                      │ PROC-TS     │
                      └─────────────┘

┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│  USER-SEC   │       │ TCAT-BALANCE│       │ DISC-GROUP  │
│─────────────│       │─────────────│       │─────────────│
│ USER-ID (PK)│       │ Composite PK│       │ GROUP-ID(PK)│
│ FIRST-NAME  │       │ ACCT-ID     │       │ INT-RATE    │
│ LAST-NAME   │       │ CAT-CD      │       │ DESCRIPTION │
│ PASSWORD    │       │ BALANCE     │       └─────────────┘
│ USER-TYPE   │       │ LAST-UPD    │
│ (A/U)       │       └─────────────┘
└─────────────┘
```

### 3.2 VSAM Key Structures

| File | Primary Key | Alternate Index | Key Length |
|------|-------------|-----------------|------------|
| ACCTDATA | ACCT-ID | None | 11 bytes |
| CARDDATA | CARD-NUM | None | 16 bytes |
| CUSTDATA | CUST-ID | None | 9 bytes |
| TRANSACT | TRAN-ID | CARD-NUM, PROC-DATE | 16 bytes |
| CARDXREF | CARD-NUM | ACCT-ID (AIX.PATH) | 16 bytes |
| USRSEC | USER-ID | None | 8 bytes |

### 3.3 JPA Entity Mapping

```java
// Account.java
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @Column(length = 11)
    private String accountId;
    
    @Column(name = "active_status")
    private String activeStatus;
    
    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance;
    
    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;
    
    @Column(name = "cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;
    
    @Column(name = "open_date")
    private LocalDate openDate;
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    @Column(name = "reissue_date")
    private LocalDate reissueDate;
    
    @Column(name = "current_cycle_credit", precision = 12, scale = 2)
    private BigDecimal currentCycleCredit;
    
    @Column(name = "current_cycle_debit", precision = 12, scale = 2)
    private BigDecimal currentCycleDebit;
    
    @Column(name = "group_id", length = 10)
    private String groupId;
    
    @OneToMany(mappedBy = "account")
    private List<Card> cards;
}

// Customer.java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @Column(length = 9)
    private String customerId;
    
    @Column(name = "first_name", length = 25)
    private String firstName;
    
    @Column(name = "middle_name", length = 25)
    private String middleName;
    
    @Column(name = "last_name", length = 25)
    private String lastName;
    
    @Column(name = "address_line_1", length = 50)
    private String addressLine1;
    
    @Column(name = "address_line_2", length = 50)
    private String addressLine2;
    
    @Column(name = "address_line_3", length = 50)
    private String addressLine3;
    
    @Column(name = "state_code", length = 2)
    private String stateCode;
    
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    
    @Column(name = "country_code", length = 3)
    private String countryCode;
    
    @Column(name = "phone_number_1", length = 15)
    private String phoneNumber1;
    
    @Column(name = "phone_number_2", length = 15)
    private String phoneNumber2;
    
    @Column(length = 9)
    private String ssn;
    
    @Column(name = "government_id", length = 20)
    private String governmentId;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "eft_account_id", length = 10)
    private String eftAccountId;
    
    @Column(name = "primary_card_holder")
    private String primaryCardHolder;
    
    @Column(name = "fico_score")
    private Integer ficoScore;
    
    @OneToMany(mappedBy = "customer")
    private List<Card> cards;
}

// Card.java
@Entity
@Table(name = "cards")
public class Card {
    @Id
    @Column(name = "card_number", length = 16)
    private String cardNumber;
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    @Column(name = "card_name", length = 50)
    private String cardName;
    
    @Column(name = "active_status", length = 1)
    private String activeStatus;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}

// Transaction.java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @Column(name = "transaction_id", length = 16)
    private String transactionId;
    
    @ManyToOne
    @JoinColumn(name = "card_number")
    private Card card;
    
    @ManyToOne
    @JoinColumn(name = "type_code")
    private TransactionType transactionType;
    
    @ManyToOne
    @JoinColumn(name = "category_code")
    private TransactionCategory transactionCategory;
    
    @Column(length = 10)
    private String source;
    
    @Column(length = 100)
    private String description;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "merchant_id", length = 9)
    private String merchantId;
    
    @Column(name = "merchant_name", length = 30)
    private String merchantName;
    
    @Column(name = "merchant_city", length = 25)
    private String merchantCity;
    
    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;
    
    @Column(name = "origination_timestamp")
    private LocalDateTime originationTimestamp;
    
    @Column(name = "processing_timestamp")
    private LocalDateTime processingTimestamp;
}

// User.java
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "user_id", length = 8)
    private String userId;
    
    @Column(name = "first_name", length = 20)
    private String firstName;
    
    @Column(name = "last_name", length = 20)
    private String lastName;
    
    @Column(length = 60) // BCrypt encoded
    private String password;
    
    @Column(name = "user_type", length = 1)
    private String userType; // 'A' = Admin, 'U' = User
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
}
```

---

## 4. Business Rules Extraction

### 4.1 Authentication & Authorization

**Sign-on Process (COSGN00C)**:
1. User enters User ID (8 chars) and Password (8 chars)
2. System reads USRSEC file by User ID
3. Password comparison (plain text in COBOL - must be hashed in Spring Boot)
4. User type determines navigation:
   - Type 'A' (Admin) → Admin Menu (COADM01C)
   - Type 'U' (User) → Main Menu (COMEN01C)
5. Failed login displays error message, allows retry

**Authorization Rules**:
- Admin users can: Manage users (CRUD), access all menu options
- Regular users can: View/update accounts, manage cards, view/add transactions, run reports, pay bills
- Session maintained via COMMAREA (CDEMO-USER-ID, CDEMO-USER-TYPE)

### 4.2 Account Management

**Account View (COACTVWC)**:
- Read account by ACCT-ID from ACCTDATA
- Read customer details via CARDXREF cross-reference
- Display: Status, balances, limits, dates, customer info, address

**Account Update (COACTUPC)**:
- Validate all input fields
- Date validation using CSUTLDPY copybook
- State/ZIP validation using CSLKPCDY lookup codes
- Phone area code validation
- Update ACCTDATA and CUSTDATA files
- File status handling: 00=success, 04=not found, others=error

### 4.3 Card Management

**Card List (COCRDLIC)**:
- Pagination: 7 cards per page
- Search by Account ID or Card Number
- Browse using CARDXREF alternate index
- Selection: 'U' for Update, 'D' for Delete (admin only)

**Card Update (COCRDUPC)**:
- Editable fields: Card name, Active status (Y/N), Expiry date (MM/YYYY)
- Validation: Expiry date must be future date
- Update CARDDATA file

### 4.4 Transaction Processing

**Transaction List (COTRN00C)**:
- Pagination: 10 transactions per page
- Search by Transaction ID
- Display: ID, Date, Description, Amount
- Selection for detail view

**Transaction Add (COTRN02C)**:
1. Enter Account ID or Card Number
2. Validate account/card exists and is active
3. Enter transaction details:
   - Type Code (2 chars) - validated against TRANTYPE
   - Category Code (4 chars) - validated against TRANCATG
   - Source (10 chars)
   - Description (60 chars)
   - Amount (-99999999.99 to 99999999.99)
   - Origination Date (YYYY-MM-DD)
   - Processing Date (YYYY-MM-DD)
   - Merchant details (ID, Name, City, ZIP)
4. Generate unique Transaction ID (timestamp-based)
5. Confirmation required (Y/N)
6. Write to TRANSACT file

**Transaction Posting (CBTRN02C - Batch)**:
1. Read daily transaction file (DALYTRAN)
2. For each transaction:
   - Validate card exists in CARDXREF
   - Validate account is active
   - Update account balance (ACCTDATA)
   - Update category balance (TCATBALF)
   - Write to transaction master (TRANSACT)
3. Rejected transactions written to DALYREJS

### 4.5 Interest Calculation (CBACT04C)

**Processing Logic**:
1. Input: Processing date as PARM (YYYYMMDD00)
2. Read TCATBALF (transaction category balances)
3. For each account:
   - Look up discount group (DISCGRP) for interest rate
   - Calculate interest based on balance and rate
   - Generate system transaction for interest charge
4. Output: SYSTRAN file with interest transactions

**Interest Formula**:
```
Monthly Interest = (Balance * Annual Rate / 12) / 100
```

### 4.6 Bill Payment (COBIL00C)

**Processing Logic**:
1. Enter Account ID
2. Display current balance
3. Confirmation required (Y/N)
4. If confirmed:
   - Create payment transaction (negative amount)
   - Update account balance to zero
   - Write transaction to TRANSACT

### 4.7 Report Generation (CORPT00C)

**Report Types**:
1. **Monthly**: Current month transactions
2. **Yearly**: Current year transactions
3. **Custom**: User-specified date range (MM/DD/YYYY to MM/DD/YYYY)

**Processing**:
- Submits batch job (TRANREPT) via internal reader
- Report sorted by card number
- Output includes: Transaction details, category totals, grand totals

---

## 5. Migration Wave Plan

### Wave 1: Foundation (Weeks 1-4)

**Scope**:
- Core data model (JPA entities)
- Database schema (PostgreSQL)
- User authentication with Spring Security
- Sign-on functionality
- Menu navigation structure

**Components**:
| COBOL | Spring Boot |
|-------|-------------|
| CSUSR01Y copybook | User entity |
| COSGN00C program | AuthController, UserService |
| COSGN00 BMS map | (API only, no UI) |
| COMEN01C, COADM01C | MenuController (navigation endpoints) |
| COCOM01Y (COMMAREA) | JWT token / Session |

**Technical Decisions**:
- Password storage: BCrypt encoding (replace plain text)
- Session management: JWT tokens (stateless) or Spring Session
- User roles: Spring Security roles (ROLE_ADMIN, ROLE_USER)

**Dependencies**: None (foundation wave)

**Complexity**: Medium

**Deliverables**:
- JPA entities: User
- REST endpoints: POST /api/auth/login, GET /api/auth/logout
- Spring Security configuration
- Database migration scripts (Flyway/Liquibase)

---

### Wave 2: Account & Card Management (Weeks 5-8)

**Scope**:
- Account entity and CRUD operations
- Customer entity and CRUD operations
- Card entity and CRUD operations
- Cross-reference relationships

**Components**:
| COBOL | Spring Boot |
|-------|-------------|
| CVACT01Y, CVACT02Y, CVACT03Y | Account, Customer, CardXref entities |
| CVCUS01Y, CUSTREC | Customer entity |
| CVCRD01Y | Card entity |
| COACTVWC, COACTUPC | AccountController, AccountService |
| COCRDLIC, COCRDSLC, COCRDUPC | CardController, CardService |
| COACTVW, COACTUP, COCRDLI, COCRDSL, COCRDUP | (API only) |

**Technical Decisions**:
- Pagination: Spring Data Pageable (replace COBOL cursor logic)
- Validation: Bean Validation annotations (@NotNull, @Size, etc.)
- State/ZIP validation: Custom validator or reference data table

**Dependencies**: Wave 1 (authentication)

**Complexity**: Medium-High

**Deliverables**:
- JPA entities: Account, Customer, Card, CardXref
- REST endpoints:
  - GET/PUT /api/accounts/{id}
  - GET /api/cards?accountId={id}&page={n}
  - GET/PUT /api/cards/{cardNumber}
- Service layer with business validation
- Repository interfaces with custom queries

---

### Wave 3: Transactions (Weeks 9-12)

**Scope**:
- Transaction entity and operations
- Transaction type and category reference data
- Transaction list, view, add functionality
- Transaction category balance tracking

**Components**:
| COBOL | Spring Boot |
|-------|-------------|
| CVTRA01Y-07Y | Transaction, TransactionType, TransactionCategory, CategoryBalance entities |
| COTRN00C, COTRN01C, COTRN02C | TransactionController, TransactionService |
| COTRN00, COTRN01, COTRN02 | (API only) |

**Technical Decisions**:
- Transaction ID generation: UUID or timestamp-based sequence
- Amount handling: BigDecimal with precision(12,2)
- Date/time: LocalDateTime for timestamps

**Dependencies**: Wave 2 (accounts, cards)

**Complexity**: High

**Deliverables**:
- JPA entities: Transaction, TransactionType, TransactionCategory, CategoryBalance
- REST endpoints:
  - GET /api/transactions?cardNumber={num}&page={n}
  - GET /api/transactions/{id}
  - POST /api/transactions
- Transaction validation service
- Reference data initialization

---

### Wave 4: Reporting & Billing (Weeks 13-16)

**Scope**:
- Report generation (monthly, yearly, custom)
- Bill payment functionality
- Statement generation

**Components**:
| COBOL | Spring Boot |
|-------|-------------|
| CORPT00C | ReportController, ReportService |
| COBIL00C | BillPaymentController, BillPaymentService |
| CBSTM03A/B | StatementService |
| CORPT00, COBIL00 | (API only) |

**Technical Decisions**:
- Report format: PDF generation (iText/JasperReports) or JSON/CSV export
- Async processing: Spring @Async for long-running reports
- Bill payment: Transactional with optimistic locking

**Dependencies**: Wave 3 (transactions)

**Complexity**: Medium

**Deliverables**:
- REST endpoints:
  - POST /api/reports (async, returns job ID)
  - GET /api/reports/{jobId}/status
  - GET /api/reports/{jobId}/download
  - POST /api/billing/pay
- Report generation service
- PDF/CSV export utilities

---

### Wave 5: Optional Modules (Weeks 17-20)

**Scope**:
- Authorization module (if IMS-DB2-MQ variant exists)
- Transaction type management (if DB2 variant exists)
- MQ integrations replacement

**Components**:
| COBOL | Spring Boot |
|-------|-------------|
| IMS DB authorization | JPA-based authorization service |
| DB2 transaction types | Same as Wave 3 (already JPA) |
| MQ messaging | REST APIs or Spring Integration |

**Technical Decisions**:
- MQ replacement: Evaluate REST vs async messaging (RabbitMQ/Kafka)
- If real-time needed: WebSocket or Server-Sent Events

**Dependencies**: Waves 1-4

**Complexity**: Low-Medium (modules may not exist)

**Deliverables**:
- Integration adapters (if needed)
- Additional REST endpoints for external systems

---

### Wave 6: Batch Processing (Weeks 21-26)

**Scope**:
- Convert JCL batch jobs to Spring Batch
- Transaction posting batch
- Interest calculation batch
- Report generation batch
- Data export/import utilities

**Components**:
| JCL/COBOL | Spring Batch |
|-----------|--------------|
| POSTTRAN (CBTRN02C) | TransactionPostingJob |
| INTCALC (CBACT04C) | InterestCalculationJob |
| TRANREPT (CBTRN03C) | TransactionReportJob |
| COMBTRAN | TransactionCombineJob |
| CBEXPORT | DataExportJob |
| CBIMPORT | DataImportJob |

**Technical Decisions**:
- Job scheduling: Spring Batch + Quartz or Kubernetes CronJob
- Chunk processing: Configurable chunk size (1000 records default)
- Error handling: Skip policy with error logging
- Restart capability: Job repository for state persistence

**Dependencies**: Waves 1-4 (all entities and services)

**Complexity**: High

**Deliverables**:
- Spring Batch jobs:
  - TransactionPostingJob (ItemReader → ItemProcessor → ItemWriter)
  - InterestCalculationJob
  - TransactionReportJob
  - DataExportJob, DataImportJob
- Job configuration and scheduling
- Monitoring endpoints: GET /api/jobs/{jobId}/status
- Error handling and retry logic

---

## 6. Technical Mapping

### 6.1 CICS Transactions → REST API Endpoints

| CICS Trans | Program | REST Endpoint | HTTP Method |
|------------|---------|---------------|-------------|
| CSGN | COSGN00C | /api/auth/login | POST |
| CMEN | COMEN01C | /api/menu/main | GET |
| CADM | COADM01C | /api/menu/admin | GET |
| CACV | COACTVWC | /api/accounts/{id} | GET |
| CACU | COACTUPC | /api/accounts/{id} | PUT |
| CCLI | COCRDLIC | /api/cards | GET |
| CCSL | COCRDSLC | /api/cards/{cardNumber} | GET |
| CCUP | COCRDUPC | /api/cards/{cardNumber} | PUT |
| CTR0 | COTRN00C | /api/transactions | GET |
| CTR1 | COTRN01C | /api/transactions/{id} | GET |
| CTR2 | COTRN02C | /api/transactions | POST |
| CRPT | CORPT00C | /api/reports | POST |
| CBIL | COBIL00C | /api/billing/pay | POST |
| CUS0 | COUSR00C | /api/users | GET |
| CUS1 | COUSR01C | /api/users | POST |
| CUS2 | COUSR02C | /api/users/{id} | PUT |
| CUS3 | COUSR03C | /api/users/{id} | DELETE |

### 6.2 BMS Screens → API Documentation

BMS screens define the UI contract. In Spring Boot, this maps to:
- **Request DTOs**: Input fields from BMS maps
- **Response DTOs**: Output fields from BMS maps
- **OpenAPI/Swagger documentation**: Field descriptions, validations

Example mapping for COTRN02 (Transaction Add):
```java
// Request DTO
public record TransactionCreateRequest(
    @NotBlank @Size(max = 11) String accountId,
    @Size(max = 16) String cardNumber,
    @NotBlank @Size(max = 2) String typeCode,
    @NotBlank @Size(max = 4) String categoryCode,
    @Size(max = 10) String source,
    @Size(max = 60) String description,
    @NotNull @DecimalMin("-99999999.99") @DecimalMax("99999999.99") BigDecimal amount,
    @NotNull LocalDate originationDate,
    LocalDate processingDate,
    @Size(max = 9) String merchantId,
    @Size(max = 30) String merchantName,
    @Size(max = 25) String merchantCity,
    @Size(max = 10) String merchantZip
) {}

// Response DTO
public record TransactionResponse(
    String transactionId,
    String cardNumber,
    String typeCode,
    String typeDescription,
    String categoryCode,
    String categoryDescription,
    String source,
    String description,
    BigDecimal amount,
    String merchantId,
    String merchantName,
    String merchantCity,
    String merchantZip,
    LocalDateTime originationTimestamp,
    LocalDateTime processingTimestamp
) {}
```

### 6.3 VSAM Files → JPA Entities + PostgreSQL Tables

| VSAM File | JPA Entity | PostgreSQL Table | Notes |
|-----------|------------|------------------|-------|
| ACCTDATA.VSAM.KSDS | Account | accounts | Primary key: account_id |
| CARDDATA.VSAM.KSDS | Card | cards | Primary key: card_number |
| CUSTDATA.VSAM.KSDS | Customer | customers | Primary key: customer_id |
| TRANSACT.VSAM.KSDS | Transaction | transactions | Primary key: transaction_id |
| CARDXREF.VSAM.KSDS | CardXref | card_xrefs | Composite key or separate table |
| USRSEC.VSAM.KSDS | User | users | Primary key: user_id |
| TRANTYPE.VSAM.KSDS | TransactionType | transaction_types | Reference data |
| TRANCATG.VSAM.KSDS | TransactionCategory | transaction_categories | Reference data |
| TCATBALF.VSAM.KSDS | CategoryBalance | category_balances | Composite key |
| DISCGRP.VSAM.KSDS | DiscountGroup | discount_groups | Reference data |

### 6.4 COBOL Copybooks → Java DTOs/Records

| Copybook | Java Class | Type |
|----------|------------|------|
| CVACT01Y | AccountDto | Record |
| CVCUS01Y | CustomerDto | Record |
| CVCRD01Y | CardDto | Record |
| CVTRA01Y | TransactionDto | Record |
| CVTRA02Y | TransactionTypeDto | Record |
| CVTRA03Y | TransactionCategoryDto | Record |
| CSUSR01Y | UserDto | Record |
| COCOM01Y | SessionContext | Class (for JWT claims) |

### 6.5 JCL Batch Jobs → Spring Batch Jobs

| JCL Job | Spring Batch Job | Schedule |
|---------|------------------|----------|
| POSTTRAN | TransactionPostingJob | Daily (EOD) |
| INTCALC | InterestCalculationJob | Monthly (EOM) |
| TRANREPT | TransactionReportJob | On-demand |
| COMBTRAN | TransactionCombineJob | Daily (after POSTTRAN) |
| CBEXPORT | DataExportJob | On-demand |
| CBIMPORT | DataImportJob | On-demand |

**Spring Batch Job Structure**:
```java
@Configuration
public class TransactionPostingJobConfig {
    
    @Bean
    public Job transactionPostingJob(JobRepository jobRepository,
                                     Step validateStep,
                                     Step postStep,
                                     Step updateBalanceStep) {
        return new JobBuilder("transactionPostingJob", jobRepository)
            .start(validateStep)
            .next(postStep)
            .next(updateBalanceStep)
            .build();
    }
    
    @Bean
    public Step postStep(JobRepository jobRepository,
                         PlatformTransactionManager transactionManager,
                         ItemReader<DailyTransaction> reader,
                         ItemProcessor<DailyTransaction, Transaction> processor,
                         ItemWriter<Transaction> writer) {
        return new StepBuilder("postStep", jobRepository)
            .<DailyTransaction, Transaction>chunk(1000, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skip(ValidationException.class)
            .skipLimit(100)
            .build();
    }
}
```

### 6.6 RACF Security → Spring Security

| RACF Concept | Spring Security Equivalent |
|--------------|---------------------------|
| User ID | UserDetails.username |
| Password | UserDetails.password (BCrypt) |
| User Type (A/U) | GrantedAuthority (ROLE_ADMIN, ROLE_USER) |
| Resource access | @PreAuthorize annotations |
| Session | JWT token or Spring Session |

**Spring Security Configuration**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 6.7 MQ → Spring Integration or REST APIs

If MQ integration exists in optional modules:

| MQ Pattern | Spring Boot Alternative |
|------------|------------------------|
| Request/Reply | REST API (synchronous) |
| Fire-and-forget | @Async methods |
| Pub/Sub | Spring Integration + RabbitMQ/Kafka |
| Queue-based processing | Spring Batch with JMS ItemReader |

---

## 7. Appendix

### A. File Status Codes Reference

| Code | Meaning | Spring Boot Handling |
|------|---------|---------------------|
| 00 | Success | Normal flow |
| 04 | Record not found | ResourceNotFoundException |
| 10 | End of file | Empty result set |
| 21 | Sequence error | DataIntegrityViolationException |
| 22 | Duplicate key | DuplicateKeyException |
| 35 | File not found | Configuration error |

### B. PF Key Mapping

| PF Key | COBOL Action | REST Equivalent |
|--------|--------------|-----------------|
| ENTER | Submit/Confirm | POST/PUT request |
| PF3 | Back/Exit | Navigation (client-side) |
| PF4 | Clear | Reset form (client-side) |
| PF5 | Refresh/Browse | GET request |
| PF7 | Page Up | ?page=n-1 |
| PF8 | Page Down | ?page=n+1 |
| PF12 | Cancel/Exit | Navigation (client-side) |

### C. Data Type Mapping

| COBOL Type | Java Type | PostgreSQL Type |
|------------|-----------|-----------------|
| PIC X(n) | String | VARCHAR(n) |
| PIC 9(n) | Integer/Long | INTEGER/BIGINT |
| PIC 9(n)V9(m) | BigDecimal | NUMERIC(n+m, m) |
| PIC S9(n) COMP-3 | BigDecimal | NUMERIC |
| Date (YYYYMMDD) | LocalDate | DATE |
| Timestamp | LocalDateTime | TIMESTAMP |

### D. Estimated Timeline Summary

| Wave | Duration | Cumulative |
|------|----------|------------|
| Wave 1: Foundation | 4 weeks | 4 weeks |
| Wave 2: Account & Card | 4 weeks | 8 weeks |
| Wave 3: Transactions | 4 weeks | 12 weeks |
| Wave 4: Reporting & Billing | 4 weeks | 16 weeks |
| Wave 5: Optional Modules | 4 weeks | 20 weeks |
| Wave 6: Batch Processing | 6 weeks | 26 weeks |

**Total Estimated Duration**: 26 weeks (6.5 months)

---

## 8. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data migration complexity | High | Develop comprehensive ETL scripts; parallel run period |
| Business rule gaps | Medium | Extensive testing with SMEs; document assumptions |
| Performance differences | Medium | Load testing; optimize queries; caching strategy |
| Security model changes | High | Thorough security review; penetration testing |
| Batch job timing | Medium | Monitoring; alerting; retry mechanisms |

---

*Document Version: 1.0*
*Created: February 2026*
*Author: Devin (AI Assistant)*
