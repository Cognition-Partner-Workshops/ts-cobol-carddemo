# CardDemo Application Architecture

This document provides a visual overview of the CardDemo mainframe application architecture, including component relationships and data flows.

## Table of Contents
- [High-Level Application Architecture](#high-level-application-architecture)
- [Online Transaction Flow](#online-transaction-flow)
- [Batch Processing Components](#batch-processing-components)
- [Data Files and Copybooks](#data-files-and-copybooks)
- [Program Dependencies](#program-dependencies)

## High-Level Application Architecture

The CardDemo application follows a classic mainframe architecture with CICS for online transactions and JCL for batch processing.

```mermaid
flowchart TB
    subgraph "Authentication Layer"
        COSGN00C[COSGN00C<br/>Sign-on Screen<br/>CC00]
    end
    
    subgraph "Menu Layer"
        COMEN01C[COMEN01C<br/>User Main Menu<br/>CM00]
        COADM01C[COADM01C<br/>Admin Menu<br/>CA00]
    end
    
    subgraph "Account Management"
        COACTVWC[COACTVWC<br/>Account View<br/>CAVW]
        COACTUPC[COACTUPC<br/>Account Update<br/>CAUP]
    end
    
    subgraph "Credit Card Management"
        COCRDLIC[COCRDLIC<br/>Card List<br/>CCLI]
        COCRDSLC[COCRDSLC<br/>Card View<br/>CCDL]
        COCRDUPC[COCRDUPC<br/>Card Update<br/>CCUP]
    end
    
    subgraph "Transaction Management"
        COTRN00C[COTRN00C<br/>Transaction List<br/>CT00]
        COTRN01C[COTRN01C<br/>Transaction View<br/>CT01]
        COTRN02C[COTRN02C<br/>Transaction Add<br/>CT02]
    end
    
    subgraph "Reports & Payments"
        CORPT00C[CORPT00C<br/>Reports<br/>CR00]
        COBIL00C[COBIL00C<br/>Bill Payment<br/>CB00]
    end
    
    subgraph "User Administration"
        COUSR00C[COUSR00C<br/>List Users<br/>CU00]
        COUSR01C[COUSR01C<br/>Add User<br/>CU01]
        COUSR02C[COUSR02C<br/>Update User<br/>CU02]
        COUSR03C[COUSR03C<br/>Delete User<br/>CU03]
    end
    
    COSGN00C -->|Admin| COADM01C
    COSGN00C -->|User| COMEN01C
    
    COMEN01C --> COACTVWC
    COMEN01C --> COACTUPC
    COMEN01C --> COCRDLIC
    COMEN01C --> COTRN00C
    COMEN01C --> CORPT00C
    COMEN01C --> COBIL00C
    
    COADM01C --> COUSR00C
    
    COACTVWC --> COCRDLIC
    COCRDLIC --> COCRDSLC
    COCRDLIC --> COCRDUPC
    COTRN00C --> COTRN01C
    COTRN00C --> COTRN02C
    
    COUSR00C --> COUSR01C
    COUSR00C --> COUSR02C
    COUSR00C --> COUSR03C
```

## Online Transaction Flow

This diagram shows how users navigate through the application based on their role (Admin vs Regular User).

```mermaid
flowchart LR
    subgraph "Entry Point"
        LOGIN[CC00 - Login]
    end
    
    subgraph "User Functions"
        UMENU[CM00 - User Menu]
        ACCT[Account Functions]
        CARD[Card Functions]
        TRAN[Transaction Functions]
        BILL[Bill Payment]
        RPT[Reports]
    end
    
    subgraph "Admin Functions"
        AMENU[CA00 - Admin Menu]
        USR[User Management]
    end
    
    LOGIN -->|User Type = U| UMENU
    LOGIN -->|User Type = A| AMENU
    
    UMENU --> ACCT
    UMENU --> CARD
    UMENU --> TRAN
    UMENU --> BILL
    UMENU --> RPT
    
    AMENU --> USR
    
    ACCT -->|PF3| UMENU
    CARD -->|PF3| UMENU
    TRAN -->|PF3| UMENU
    BILL -->|PF3| UMENU
    RPT -->|PF3| UMENU
    USR -->|PF3| AMENU
```

## Batch Processing Components

The batch processing layer handles daily transaction posting, interest calculations, and statement generation.

```mermaid
flowchart LR
    subgraph "Input Files"
        DALYTRAN[(DALYTRAN<br/>Daily Transactions)]
        ACCTDATA[(ACCTDATA<br/>Account Data)]
        CARDDATA[(CARDDATA<br/>Card Data)]
        CUSTDATA[(CUSTDATA<br/>Customer Data)]
    end
    
    subgraph "Batch Programs"
        CBTRN02C[CBTRN02C<br/>Post Transactions]
        CBACT04C[CBACT04C<br/>Interest Calculator]
        CBSTM03A[CBSTM03A<br/>Statement Generator]
        CBTRN03C[CBTRN03C<br/>Transaction Report]
    end
    
    subgraph "Output Files"
        TRANSACT[(TRANSACT<br/>Transaction Master)]
        TCATBALF[(TCATBALF<br/>Category Balance)]
        STATEMENTS[(Statements)]
        REPORTS[(Reports)]
    end
    
    DALYTRAN --> CBTRN02C
    CBTRN02C --> TRANSACT
    CBTRN02C --> TCATBALF
    
    TCATBALF --> CBACT04C
    ACCTDATA --> CBACT04C
    CBACT04C --> TRANSACT
    
    TRANSACT --> CBSTM03A
    CBSTM03A --> STATEMENTS
    
    TRANSACT --> CBTRN03C
    CBTRN03C --> REPORTS
```

## Data Files and Copybooks

This diagram shows the relationship between VSAM data files and their corresponding copybook layouts.

```mermaid
flowchart TB
    subgraph "VSAM Files"
        USRSEC[(USRSEC<br/>User Security)]
        ACCTDAT[(ACCTDAT<br/>Account Master)]
        CARDDAT[(CARDDAT<br/>Card Master)]
        CUSTDAT[(CUSTDAT<br/>Customer Master)]
        CARDXREF[(CARDXREF<br/>Card-Account XREF)]
        TRANSACT[(TRANSACT<br/>Transactions)]
        DISCGRP[(DISCGRP<br/>Disclosure Groups)]
        TCATBALF[(TCATBALF<br/>Category Balance)]
    end
    
    subgraph "Copybooks - Data Layouts"
        CSUSR01Y[CSUSR01Y<br/>User Record]
        CVACT01Y[CVACT01Y<br/>Account Record]
        CVACT02Y[CVACT02Y<br/>Card Record]
        CVCUS01Y[CVCUS01Y<br/>Customer Record]
        CVACT03Y[CVACT03Y<br/>XREF Record]
        CVTRA05Y[CVTRA05Y<br/>Transaction Record]
    end
    
    subgraph "Copybooks - Common"
        COCOM01Y[COCOM01Y<br/>COMMAREA]
        COTTL01Y[COTTL01Y<br/>Screen Titles]
        CSDAT01Y[CSDAT01Y<br/>Date Handling]
        CSMSG01Y[CSMSG01Y<br/>Messages]
    end
    
    USRSEC -.-> CSUSR01Y
    ACCTDAT -.-> CVACT01Y
    CARDDAT -.-> CVACT02Y
    CUSTDAT -.-> CVCUS01Y
    CARDXREF -.-> CVACT03Y
    TRANSACT -.-> CVTRA05Y
```

## Program Dependencies

This diagram illustrates how programs depend on shared copybooks and components.

```mermaid
graph TD
    subgraph "Shared Copybooks"
        COCOM01Y[COCOM01Y - COMMAREA]
        DFHAID[DFHAID - AID Keys]
        DFHBMSCA[DFHBMSCA - BMS Attributes]
    end
    
    subgraph "Online Programs"
        ONLINE[All CICS Programs]
    end
    
    subgraph "Batch Programs"
        BATCH[All Batch Programs]
    end
    
    subgraph "Data Copybooks"
        DATA[CVACT01Y, CVACT02Y, CVACT03Y<br/>CVCUS01Y, CVTRA05Y, etc.]
    end
    
    COCOM01Y --> ONLINE
    DFHAID --> ONLINE
    DFHBMSCA --> ONLINE
    DATA --> ONLINE
    DATA --> BATCH
```

## Component Summary

### Online Programs (CICS)

| Program | Transaction | Function | BMS Map |
|---------|-------------|----------|---------|
| COSGN00C | CC00 | Sign-on Screen | COSGN00 |
| COMEN01C | CM00 | User Main Menu | COMEN01 |
| COADM01C | CA00 | Admin Menu | COADM01 |
| COACTVWC | CAVW | Account View | COACTVW |
| COACTUPC | CAUP | Account Update | COACTUP |
| COCRDLIC | CCLI | Credit Card List | COCRDLI |
| COCRDSLC | CCDL | Credit Card View | COCRDSL |
| COCRDUPC | CCUP | Credit Card Update | COCRDUP |
| COTRN00C | CT00 | Transaction List | COTRN00 |
| COTRN01C | CT01 | Transaction View | COTRN01 |
| COTRN02C | CT02 | Transaction Add | COTRN02 |
| CORPT00C | CR00 | Transaction Reports | CORPT00 |
| COBIL00C | CB00 | Bill Payment | COBIL00 |
| COUSR00C | CU00 | List Users | COUSR00 |
| COUSR01C | CU01 | Add User | COUSR01 |
| COUSR02C | CU02 | Update User | COUSR02 |
| COUSR03C | CU03 | Delete User | COUSR03 |

### Batch Programs

| Program | Function |
|---------|----------|
| CBTRN02C | Post daily transactions |
| CBACT04C | Calculate interest |
| CBSTM03A | Generate statements |
| CBTRN03C | Generate transaction reports |

### Key Copybooks

| Copybook | Purpose |
|----------|---------|
| COCOM01Y | Communication area between programs |
| CVACT01Y | Account record layout |
| CVACT02Y | Card record layout |
| CVACT03Y | Card-Account cross-reference |
| CVCUS01Y | Customer record layout |
| CVTRA05Y | Transaction record layout |
| CSUSR01Y | User security record layout |
