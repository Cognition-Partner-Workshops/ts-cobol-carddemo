# CardDemo - Mainframe Credit Card Management Application

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)

## Executive Summary

CardDemo is a comprehensive mainframe application that simulates a credit card management system. It serves as a realistic demonstration environment for AWS and partner technologies in mainframe migration and modernization scenarios.

The application provides hands-on experience with common mainframe patterns and technologies, making it an ideal testbed for evaluating modernization approaches including application discovery, migration assessment, performance testing, service enablement, and test automation.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Description](#description)
- [Technologies](#technologies)
- [Optional Features](#optional-features)
- [Installation](#installation)
- [Running Batch Jobs](#running-batch-jobs)
- [Application Details](#application-details)
  - [User Functions](#user-functions)
  - [Admin Functions](#admin-functions)
  - [Application Inventory](#application-inventory)
  - [Application Screens](#application-screens)
- [Technical Highlights](#technical-highlights)
- [Support](#support)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Project Status](#project-status)

---

## Quick Start

For users who want to get started quickly:

1. Clone this repository to your local environment
2. Set up your mainframe environment with CICS, VSAM, and JCL support
3. Follow the [Installation](#installation) steps to deploy the application
4. Access the application using transaction **CC00** with credentials:
   - **Admin**: ADMIN001 / PASSWORD
   - **User**: USER0001 / PASSWORD

---

## Description

CardDemo is a mainframe application designed to test and showcase AWS and partner technology for mainframe migration and modernization use-cases. The application intentionally incorporates various coding styles and patterns to exercise analysis, transformation, and migration tooling across different mainframe programming paradigms.

### Key Use Cases

CardDemo provides a realistic environment for the following activities:

- **Application Discovery and Analysis**: Understand mainframe application structures, dependencies, and data flows
- **Migration Assessment and Planning**: Evaluate migration strategies and estimate effort for modernization projects
- **Modernization Strategy Development**: Test different approaches including rehosting, replatforming, and refactoring
- **Performance Testing**: Benchmark application performance across different environments
- **System Augmentation**: Extend mainframe capabilities with cloud services
- **Service Enablement and Extraction**: Expose mainframe functionality as modern APIs
- **Test Creation and Automation**: Develop comprehensive test suites for mainframe applications

---

## Technologies

### Core Technologies

The base CardDemo application uses the following mainframe technologies:

| Technology | Purpose |
|:-----------|:--------|
| **COBOL** | Primary programming language for business logic |
| **CICS** | Transaction processing and online screen management |
| **VSAM (KSDS with AIX)** | Data storage with key-sequenced datasets and alternate indexes |
| **JCL** | Batch job processing and workflow management |
| **RACF** | Security and access control |
| **ASSEMBLER** | System-level utilities including MVSWAIT (timer control) and COBDATFT (date formatting) |

### Optional Technologies

The following technologies are available through optional extension modules:

| Technology | Purpose |
|:-----------|:--------|
| **DB2** | Relational database management for transaction types and fraud tracking |
| **IMS DB** | Hierarchical database for authorization storage |
| **MQ** | Message queuing for asynchronous processing and system integration |
| **JCL Utilities** | FTP, TXT2PDF, DB2 LOAD/UNLOAD, IMS DB LOAD/UNLOAD, Internal Reader |

### Advanced Data Features

CardDemo demonstrates various mainframe data handling capabilities:

- **Data Formats**: COMP, COMP-3, Zoned Decimal, Signed, Unsigned
- **Dataset Types**: VSAM (ESDS/RRDS), GDG, PDS
- **Record Formats**: VB, FBA, and others
- **Copybook Structures**: REDEFINES, OCCURS, OCCURS DEPENDING ON

---

## Optional Features

CardDemo includes several optional modules that extend the base functionality. Each module demonstrates specific integration patterns commonly found in enterprise mainframe environments.

### 1. Credit Card Authorizations with IMS, DB2, and MQ

This extension simulates real-world credit card authorization flows, from initial merchant request to approval/decline decisions, with fraud detection capabilities.

**Key Features:**
- Authorization request processing via MQ
- Customer data retrieval from IMS databases
- Transaction logging in DB2 tables
- Pending authorization summary and details viewing
- Batch purging of expired authorizations

For detailed documentation, see the [Pending Authorization Extension](./app/app-authorization-ims-db2-mq).

### 2. Transaction Type Management with DB2

This module demonstrates DB2 integration patterns using embedded static SQL for maintaining transaction type reference data.

**Key Features:**
- Add, update, or delete transaction types through CICS transactions
- Manage transaction types through batch jobs
- Demonstrates DB2 cursors and SQL operations

### 3. Account Extractions using MQ and VSAM

This extension showcases asynchronous processing patterns for data extraction and transmission.

**Key Features:**
- System date inquiry via MQ (CDRD transaction)
- Account details inquiry via MQ (CDRA transaction)
- Demonstrates MQ request/response patterns

### 4. Additional JCL Utilities

Extended batch processing capabilities including:
- FTP integration for file transfers
- Text-to-PDF conversion
- DB2 and IMS DB load/unload operations
- Internal reader functionality

---

## Installation

This section provides step-by-step instructions for deploying CardDemo in your mainframe environment.

### Prerequisites

Before beginning installation, ensure you have:

- A mainframe environment with CICS, VSAM, and JCL support
- Appropriate access credentials and permissions
- File transfer capability between your local environment and the mainframe
- Optional: DB2, IMS DB, and MQ for extended features

### Step 1: Prepare Your Environment

Clone this repository to your local development environment and verify you have appropriate access to your mainframe environment.

### Step 2: Create Mainframe Datasets

Define a High Level Qualifier (HLQ) for your datasets, then create the following datasets with the specified formats:

| HLQ    | Name           | Format | Length |
|:-------|:---------------|:-------|-------:|
| AWS.M2 | CARDDEMO.JCL   | FB     |     80 |
| AWS.M2 | CARDDEMO.PROC  | FB     |     80 |
| AWS.M2 | CARDDEMO.CBL   | FB     |     80 |
| AWS.M2 | CARDDEMO.CPY   | FB     |     80 |
| AWS.M2 | CARDDEMO.BMS   | FB     |     80 |
| AWS.M2 | CARDDEMO.ASM   | FB     |     80 |
| AWS.M2 | CARDDEMO.MACLIB| FB     |     80 |

### Step 3: Upload Source Code

Upload the application source folders from the repository to your mainframe using $INDFILE or your preferred file transfer tool. Ensure proper transfer modes (binary/text) as appropriate for each file type.

### Step 4: Upload Sample Data

Transfer the sample data from the `main/-/data/EBCDIC/` folder to the mainframe using binary transfer mode to preserve data integrity. Create the following datasets:

| Dataset Name                      | Description                           | Copybook  | Format | Length |
|:----------------------------------|:--------------------------------------|:----------|:-------|-------:|
| AWS.M2.CARDDEMO.USRSEC.PS         | User Security file                    | CSUSR01Y  | FB     |     80 |
| AWS.M2.CARDDEMO.ACCTDATA.PS       | Account Data                          | CVACT01Y  | FB     |    300 |
| AWS.M2.CARDDEMO.CARDDATA.PS       | Card Data                             | CVACT02Y  | FB     |    150 |
| AWS.M2.CARDDEMO.CUSTDATA.PS       | Customer Data                         | CVCUS01Y  | FB     |    500 |
| AWS.M2.CARDDEMO.CARDXREF.PS       | Customer Account Card Cross reference | CVACT03Y  | FB     |     50 |
| AWS.M2.CARDDEMO.DALYTRAN.PS.INIT  | Transaction database initialization   | CVTRA06Y  | FB     |    350 |
| AWS.M2.CARDDEMO.DALYTRAN.PS       | Transaction data for posting          | CVTRA06Y  | FB     |    350 |
| AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS| Online transaction data               | CVTRA05Y  | FB     |    350 |
| AWS.M2.CARDDEMO.DISCGRP.PS        | Disclosure Groups                     | CVTRA02Y  | FB     |     50 |
| AWS.M2.CARDDEMO.TRANCATG.PS       | Transaction Category Types            | CVTRA04Y  | FB     |     60 |
| AWS.M2.CARDDEMO.TRANTYPE.PS       | Transaction Types                     | CVTRA03Y  | FB     |     60 |
| AWS.M2.CARDDEMO.TCATBALF.PS       | Transaction Category Balance          | CVTRA01Y  | FB     |     50 |

### Step 5: Initialize the Environment

Execute the following JCLs in sequence to set up the application environment:

| Job Name | Purpose                                         | Optional Module              |
|:---------|:------------------------------------------------|:-----------------------------|
| DUSRSECJ | Sets up user security VSAM file                 |                              |
| CLOSEFIL | Closes files opened by CICS                     |                              |
| ACCTFILE | Loads Account database using sample data        |                              |
| CARDFILE | Loads Card database with credit card sample data|                              |
| CUSTFILE | Creates customer database                       |                              |
| XREFFILE | Loads Customer Card account cross reference     |                              |
| CREADB21 | Creates CardDemo DB2 database and loads tables  | DB2: Transaction Type Mgmt   |
| TRANFILE | Copies initial Transaction file to VSAM         |                              |
| TRANEXTR | Extracts TRAN type and category tables from DB2 | DB2: Transaction Type Mgmt   |
| DISCGRP  | Copies initial Disclosure Group file to VSAM    |                              |
| TCATBALF | Copies initial TCATBALF file to VSAM            |                              |
| TRANCATG | Copies initial transaction category file to VSAM|                              |
| TRANTYPE | Copies initial transaction type file to VSAM    |                              |
| OPENFIL  | Makes files available to CICS                   |                              |
| DEFGDGB  | Defines GDG Base                                |                              |
| DEFGDGD  | Defines GDG Bases added for DB2                 |                              |

### Step 6: Compile the Programs

Use your standard mainframe compilation procedures to compile the COBOL programs. Sample JCLs are provided in the samples folder to assist with compilation.

### Step 7: Configure CICS Resources

You have two options for configuring CICS resources:

**Option 1 (Preferred):** Use the DFHCSDUP JCL with the CSD file in the CSD folder.

**Option 2:** Use CEDA transaction to manually define resources:

```
DEFINE LIBRARY(COM2DOLL) GROUP(CARDDEMO) DSNAME01(&HLQ..LOADLIB)
DEF PROGRAM(COCRDLIC) GROUP(CARDDEMO)
DEF MAPSET(COCRDLI) GROUP(CARDDEMO)
DEFINE PROGRAM(COSGN00C) GROUP(CARDDEMO) DA(ANY) TRANSID(CC00) DESCRIPTION(LOGIN)
DEFINE TRANSACTION(CC00) GROUP(CARDDEMO) PROGRAM(COSGN00C) TASKDATAL(ANY)
```

### Step 8: Install and Load Resources

Install the resources in your CICS region:

```
CEDA INSTALL TRANS(CCLI) GROUP(CARDDEMO)
CEDA INSTALL FILE(CARDDAT) GROUP(CARDDEMO)
CECI LOAD PROG(COCRDUP)
CECI LOAD PROG(COCRDUPC)
```

Execute NEWCOPY for mapsets and programs:

```
CEMT SET PROG(COCRDUP) NEWCOPY
CEMT SET PROG(COCRDUPC) NEWCOPY
```

### Accessing the Application

Once installation is complete, you can access CardDemo:

- **Online Functions**: Start the application using the **CC00** transaction
  - Admin access: Use userid **ADMIN001** with password **PASSWORD**
  - User access: Use userid **USER0001** with password **PASSWORD**
- **Batch Functions**: See the [Running Batch Jobs](#running-batch-jobs) section below

---

## Running Batch Jobs

The following table lists the batch jobs available in CardDemo. Execute these JCLs in sequence to run the full batch process:

| Job Name | Purpose                                             | Optional Module                        |
|:---------|:----------------------------------------------------|:---------------------------------------|
| CLOSEFIL | Closes files opened by CICS                         |                                        |
| ACCTFILE | Loads Account database using sample data            |                                        |
| CARDFILE | Loads Card database with credit card sample data    |                                        |
| XREFFILE | Loads Customer Card account cross reference to VSAM |                                        |
| CUSTFILE | Creates customer database                           |                                        |
| TRANBKP  | Creates Transaction database                        |                                        |
| TRANEXTR | Extracts latest DB2 data for Transaction types      | DB2: Transaction Type Mgmt             |
| TRANCATG | Copies latest transaction category file to VSAM     |                                        |
| TRANTYPE | Copies latest transaction type file to VSAM         |                                        |
| DISCGRP  | Copies initial disclosure Group file to VSAM        |                                        |
| TCATBALF | Copies initial TCATBALF file to VSAM                |                                        |
| DUSRSECJ | Sets up user security VSAM file                     |                                        |
| POSTTRAN | Core transaction processing job                     |                                        |
| INTCALC  | Run interest calculations                           |                                        |
| TRANBKP  | Backup Transaction database                         |                                        |
| COMBTRAN | Combine system transactions with daily ones         |                                        |
| CREASTMT | Produce transaction statement                       |                                        |
| TRANIDX  | Define alternate index on transaction file          |                                        |
| OPENFIL  | Makes files available to CICS                       |                                        |
| WAITSTEP | Defines a step to wait job for given time           |                                        |
| CBPAUP0J | Purge expired authorizations                        | IMS-DB2-MQ: Pending Authorizations     |

---

## Application Details

CardDemo is a comprehensive credit card management application built primarily using COBOL. It provides functionality for managing accounts, credit cards, transactions, and bill payments.

### User Types

The application supports two user roles with different access levels:

| Role | Description | Access Level |
|:-----|:------------|:-------------|
| **Regular Users** | Standard card management functions | View accounts, manage cards, process transactions, make payments |
| **Admin Users** | Administrative functions | All user functions plus user management and system configuration |

### User Functions

![User Function Flow](./diagrams/Application-Flow-User.png "User Function Flow")

Regular users can perform the following functions through the CardDemo interface:

- View and update account information
- Manage credit cards (list, view details, update)
- View, add, and process transactions
- Generate transaction reports
- Make bill payments
- View pending authorizations (requires optional IMS-DB2-MQ module)

### Admin Functions

![Admin Function Flow](./diagrams/Application-Flow-Admin.png "Admin Function Flow")

Admin users have access to additional administrative capabilities:

- User management (list, add, update, delete users)
- Transaction type management (requires optional DB2 module)

### Application Inventory

#### Online Components

The following table lists all online CICS transactions available in CardDemo:

| Transaction | BMS Map | Program  | Function                        | Optional Module                    | Notes                                                     |
|:------------|:--------|:---------|:--------------------------------|:-----------------------------------|:----------------------------------------------------------|
| CC00        | COSGN00 | COSGN00C | Signon Screen                   |                                    | Entry point for all users                                 |
| CM00        | COMEN01 | COMEN01C | Main Menu                       |                                    | Navigation hub for all functions                          |
| CAVW        | COACTVW | COACTVWC | Account View                    |                                    | Display account details                                   |
| CAUP        | COACTUP | COACTUPC | Account Update                  |                                    | Modify account information                                |
| CCLI        | COCRDLI | COCRDLIC | Credit Card List                |                                    | List all cards for an account                             |
| CCDL        | COCRDSL | COCRDSLC | Credit Card View                |                                    | Display card details                                      |
| CCUP        | COCRDUP | COCRDUPC | Credit Card Update              |                                    | Modify card information                                   |
| CT00        | COTRN00 | COTRN00C | Transaction List                |                                    | List transactions for a card                              |
| CT01        | COTRN01 | COTRN01C | Transaction View                |                                    | Display transaction details                               |
| CT02        | COTRN02 | COTRN02C | Transaction Add                 |                                    | Add new transaction                                       |
| CR00        | CORPT00 | CORPT00C | Transaction Reports             |                                    | Generate transaction reports                              |
| CB00        | COBIL00 | COBIL00C | Bill Payment                    |                                    | Process bill payments                                     |
| CPVS        | COPAU00 | COPAUS0C | Pending Authorization Summary   | IMS-DB2-MQ: Pending Authorizations | Read IMS and VSAM                                         |
| CPVD        | COPAU01 | COPAUS1C | Pending Authorization Details   | IMS-DB2-MQ: Pending Authorizations | Update IMS and Insert DB2                                 |
| CP00        |         | COPAUA0C | Process Authorization Requests  | IMS-DB2-MQ: Pending Authorizations | MQ trigger, request and response; Insert and Update to IMS|
| CA00        | COADM01 | COADM01C | Admin Menu                      | DB2: Transaction Type Mgmt         | Administrative functions menu                             |
| CU00        | COUSR00 | COUSR00C | List Users                      |                                    | Display all users                                         |
| CU01        | COUSR01 | COUSR01C | Add User                        |                                    | Create new user                                           |
| CU02        | COUSR02 | COUSR02C | Update User                     |                                    | Modify user information                                   |
| CU03        | COUSR03 | COUSR03C | Delete User                     |                                    | Remove user                                               |
| CTTU        | COTRTUP | COTRTUPC | Tran Type add/edit              | DB2: Transaction Type Mgmt         | Update and insert on DB2                                  |
| CTLI        | COTRTLI | COTRTLIC | Tran Type list/update/delete    | DB2: Transaction Type Mgmt         | Demonstrates cursor and delete in DB2                     |
| CDRD        |         | CODATE01 | Inquire System Date via MQ      | MQ Integration                     | Demonstrates MQ request/response pattern                  |
| CDRA        |         | COACCT01 | Inquire account details via MQ  | MQ Integration                     | Demonstrates MQ request/response pattern                  |

#### Batch Components

The following table lists all batch programs available in CardDemo:

| Job      | Program  | Function                                             | Optional Module                        |
|:---------|:---------|:-----------------------------------------------------|:---------------------------------------|
| DUSRSECJ | IEBGENER | Initial Load of User security file                   |                                        |
| DEFGDGB  | IDCAMS   | Setup GDG Bases                                      |                                        |
| DEFGDGD  | IDCAMS   | Setup more GDG Bases for DB2                         |                                        |
| ACCTFILE | IDCAMS   | Refresh Account Master                               |                                        |
| CARDFILE | IDCAMS   | Refresh Card Master                                  |                                        |
| CUSTFILE | IDCAMS   | Refresh Customer Master                              |                                        |
| CREADB21 | DSNTEP4  | Creates CardDemo DB2 database and loads tables       | DB2: Transaction Type Mgmt             |
| TRANEXTR | DSNTIAUL | Extracts latest DB2 data for Transaction types       | DB2: Transaction Type Mgmt             |
| DISCGRP  | IDCAMS   | Load Disclosure Group File                           |                                        |
| TRANFILE | IDCAMS   | Load Transaction Master file                         |                                        |
| TRANCATG | IDCAMS   | Load Transaction category types                      |                                        |
| TRANTYPE | IDCAMS   | Load Transaction type file                           |                                        |
| XREFFILE | IDCAMS   | Account, Card and Customer cross reference           |                                        |
| CLOSEFIL | IEFBR14  | Close VSAM files in CICS                             |                                        |
| TCATBALF | IDCAMS   | Refresh Transaction Category Balance                 |                                        |
| TRANBKP  | IDCAMS   | Refresh Transaction Master                           |                                        |
| POSTTRAN | CBTRN02C | Transaction processing job                           |                                        |
| TRANIDX  | IDCAMS   | Define AIX for transaction file                      |                                        |
| OPENFIL  | IEFBR14  | Open files in CICS                                   |                                        |
| INTCALC  | CBACT04C | Run interest calculations                            |                                        |
| COMBTRAN | SORT     | Combine transaction files                            |                                        |
| CREASTMT | CBSTM03A | Produce transaction statement                        |                                        |
| TRANREPT | CBTRN03C | Transaction Report - Submitted from CICS             |                                        |
| ESDSRRDS | IDCAMS   | Create ESDS and RRDS VSAM files                      |                                        |
| CBPAUP0J | CBPAUP0C | Purge Expired Authorizations                         | IMS-DB2-MQ: Pending Authorizations     |
| MNTTRDB2 | COBTUPDT | Maintain Transaction type table                      | DB2: Transaction Type Mgmt             |
| WAITSTEP | COBSWAIT | Wait job for given time                              |                                        |

### Application Screens

The following screenshots show the main screens in the CardDemo application.

#### Signon Screen

The signon screen is the entry point for all users. Enter your userid and password to access the application.

![Signon Screen](./diagrams/Signon-Screen.png "Signon Screen")

#### Main Menu

The main menu provides navigation to all user functions. Select an option number and press Enter to access the desired function.

![Main Menu](./diagrams/Main-Menu.png "Main Menu")

**Note**: Option 11 (Pending Authorizations) is only available with the optional Credit Card Authorizations feature. Please refer to [the authorization documentation](./app/app-authorization-ims-db2-mq) for details.

#### Admin Menu

The admin menu provides access to administrative functions including user management and transaction type configuration.

![Admin Menu](./diagrams/Admin-Menu.png "Admin Menu")

**Note**: Options 5 and 6 will be enabled only if you install the Transaction Type Management with DB2 optional feature (transactions CTTU and CTLI).

---

## Technical Highlights

The following table summarizes the key technical components and features of CardDemo:

| Component | Domain Features | Technical Features |
|:----------|:----------------|:-------------------|
| **Base Application** | Customer, Account, Card, Transaction, Bill Payment, Statement/Report | COBOL, CICS, JCL (Batch), VSAM (KSDS with AIX) |
| **Optional Features** | Authorization, Fraud, Transaction Type (Extension) | DB2, MQ, IMS DB, JCL Utilities, Complex data formats, Various dataset types, Advanced copybook structures |

---

## Support

For questions, issues, or improvement requests, please raise an issue in the repository with detailed information about your concern. The maintainers will respond according to availability.

When reporting issues, please include:
- A clear description of the problem or request
- Steps to reproduce (for bugs)
- Your environment details (mainframe platform, CICS version, etc.)
- Any relevant error messages or logs

---

## Roadmap

The following features are planned for upcoming releases:

### Additional Database Syntax Usage Scenarios

- **DB2 Rewards**: Calculate rewards for transactions based on transaction types, categories, and rules. This will include stored procedures, functions, and dynamic SQL.
- **Hierarchical Database**: IMS DC implementation for additional database patterns.

### Integration Enhancements

- FTP and SFTP integration for secure file transfers
- Web Service connectivity for modern API integration
- Exposure of transactions for distributed application integration

---

## Contributing

We welcome contributions and enhancements to this codebase from the mainframe community. To contribute:

1. Fork the repository
2. Create your feature branch
3. Implement your changes with appropriate tests
4. Submit a pull request with a clear description of the changes

Feel free to raise issues, create code, and submit merge requests for enhancements to help build this application as a resource for programmers wanting to understand and modernize their mainframes.

For detailed contribution guidelines, please see [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

This project is intended to be a community resource and is released under the Apache 2.0 license. See the [LICENSE](LICENSE) file for details.

---

## Project Status

The CardDemo application has been enhanced with optional features that extend its functionality:
- Credit Card Authorizations with IMS, DB2, and MQ
- Transaction Type Management with DB2
- Account Extractions using MQ and VSAM
- Additional JCL Utilities
- Enhanced Data and Copybook Features

These optional features make CardDemo an even more useful resource for customers looking to modernize their mainframe applications. With modules for DB2, MQ, IMS DB, JCL utilities, and more data formats now available, customers can leverage CardDemo to test a wider array of mainframe migration, refactoring, replatforming, and augmentation scenarios.

---

*Last updated: April 2025*

