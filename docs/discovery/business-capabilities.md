# Business Capability Mapping

This document groups CardDemo batch jobs by business function to support microservice boundary identification during migration planning.

> **Source files:**
> - Batch inventory: [`README.md`](../../README.md) (lines 296-327)
> - JCL members: [`app/jcl/`](../../app/jcl/)
> - Scheduler: [`app/scheduler/CardDemo.controlm`](../../app/scheduler/CardDemo.controlm)

---

## Business Capability Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                     CardDemo Batch Processing                       │
├──────────────────┬──────────────────┬───────────────────────────────┤
│  Transaction     │  Interest &      │  Reference Data               │
│  Processing      │  Billing         │  Management                   │
├──────────────────┼──────────────────┼───────────────────────────────┤
│  POSTTRAN        │  INTCALC         │  MNTTRDB2                     │
│  TRANBKP         │  CREASTMT        │  TRANEXTR                     │
│  COMBTRAN        │                  │  DISCGRP                      │
│                  │                  │  TRANCATG                     │
│                  │                  │  TRANTYPE                     │
├──────────────────┴──────────────────┴───────────────────────────────┤
│  File Management          │  Data Import/Export                     │
├───────────────────────────┼─────────────────────────────────────────┤
│  CLOSEFIL                 │  CBEXPORT                               │
│  OPENFIL                  │  CBIMPORT                               │
└───────────────────────────┴─────────────────────────────────────────┘
```

---

## Transaction Processing

Jobs that handle the core lifecycle of credit card transactions: posting, backup, and consolidation.

| Job | Program | Description | Frequency | Potential Microservice |
|:----|:--------|:------------|:----------|:----------------------|
| POSTTRAN | CBTRN02C | Reads the daily transaction file (`DALYTRAN.PS`), validates against cross-reference and account data, posts valid transactions to the VSAM master, updates category balances, and writes rejects to a GDG file | Daily | Transaction Posting Service |
| TRANBKP | IDCAMS (REPROC) | Backs up the current transaction VSAM master to a GDG flat file, then deletes and redefines the VSAM cluster for the next cycle | Daily | Transaction Backup Service |
| COMBTRAN | SORT / IDCAMS | Sorts and merges the transaction backup with system-generated transactions (from interest calculations), then loads the combined result into the VSAM transaction master | Monthly | Transaction Consolidation Service |

### Key Data Flows
- **POSTTRAN** is the most critical batch job, touching 5 datasets (DALYTRAN, TRANSACT, CARDXREF, ACCTDATA, TCATBALF)
- **TRANBKP** must run before POSTTRAN to preserve the current state
- **COMBTRAN** depends on outputs from both TRANBKP (backup GDG) and INTCALC (system transactions GDG)

---

## Interest & Billing

Jobs that compute financial charges and produce customer-facing statements.

| Job | Program | Description | Frequency | Potential Microservice |
|:----|:--------|:------------|:----------|:----------------------|
| INTCALC | CBACT04C | Processes the transaction category balance file against account data, cross-reference, and disclosure group rules to compute interest charges and fees; writes system-generated interest transactions to a GDG | Monthly | Interest Calculation Service |
| CREASTMT | CBSTM03A | Produces formatted transaction statements for customers | On-demand / Monthly | Statement Generation Service |

### Key Data Flows
- **INTCALC** reads from TCATBALF, CARDXREF (including AIX path), ACCTDATA, and DISCGRP; writes to SYSTRAN GDG
- **CREASTMT** reads transaction data to produce statement output

---

## Reference Data Management

Jobs that maintain lookup/reference tables used by both online and batch processing.

| Job | Program | Description | Frequency | Potential Microservice |
|:----|:--------|:------------|:----------|:----------------------|
| MNTTRDB2 | COBTUPDT | Batch maintenance program that applies updates to the transaction type table in DB2 | Weekly | Reference Data Service |
| TRANEXTR | DSNTIAUL | Extracts the latest transaction type and category data from DB2 tables into flat files for subsequent VSAM loading | Weekly | Reference Data Service |
| DISCGRP | IDCAMS | Deletes, redefines, and reloads the Disclosure Group VSAM file from the flat file source | Weekly | Reference Data Service |
| TRANCATG | IDCAMS | Deletes, redefines, and reloads the Transaction Category VSAM file from the flat file source | Weekly | Reference Data Service |
| TRANTYPE | IDCAMS | Deletes, redefines, and reloads the Transaction Type VSAM file from the flat file source | Weekly | Reference Data Service |

### Key Data Flows
- **MNTTRDB2** updates DB2 tables, which **TRANEXTR** then extracts to flat files
- **DISCGRP**, **TRANCATG**, and **TRANTYPE** each load their respective flat files into VSAM
- These VSAM files are consumed by online CICS transactions and batch jobs (INTCALC, POSTTRAN)

---

## File Management

Infrastructure jobs that coordinate access between CICS online and batch processing.

| Job | Program | Description | Frequency | Potential Microservice |
|:----|:--------|:------------|:----------|:----------------------|
| CLOSEFIL | SDSF | Issues CEMT commands to close VSAM files (TRANSACT, CCXREF, ACCTDAT, CXACAIX, USRSEC) in the CICS region, preventing online access during batch processing | Daily, Weekly, Monthly | Replaced by distributed locking / event-driven coordination |
| OPENFIL | SDSF | Issues CEMT commands to re-open VSAM files in the CICS region after batch processing completes | Daily, Weekly, Monthly | Replaced by distributed locking / event-driven coordination |
| WAITSTEP | COBSWAIT | Timer wait utility that introduces a configurable delay in the batch chain, typically placed between the last batch processing job and OPENFIL | Daily, Weekly, Monthly | Replaced by orchestration wait/delay steps |

### Migration Considerations
- The CLOSEFIL/OPENFIL pattern is a **cross-cutting concern** used in every batch cycle
- In a microservices architecture, this will likely be replaced by:
  - Database-level locking or optimistic concurrency
  - Event-driven architecture with eventual consistency
  - Feature flags or circuit breakers to pause online access during batch windows

---

## Data Import/Export

Jobs that handle bulk data movement for migration, branch transfers, or system integration.

| Job | Program | Description | Frequency | Potential Microservice |
|:----|:--------|:------------|:----------|:----------------------|
| CBEXPORT | CBEXPORT | Reads from 5 VSAM files (Customer, Account, Card, Cross-reference, Transaction) and writes a consolidated multi-record export file to a VSAM KSDS cluster | On-demand | Data Migration Service |
| CBIMPORT | CBIMPORT | Reads the consolidated export file and splits it into 4 normalized flat files (Customer, Account, Cross-reference, Transaction) plus an error file | On-demand | Data Migration Service |

### Key Data Flows
- **CBEXPORT** consolidates data from 5 separate VSAM files into a single export dataset
- **CBIMPORT** reverses the process, normalizing back into separate files
- These jobs are particularly relevant for the migration itself, as they demonstrate the existing data extraction patterns

---

## Capability-to-Microservice Mapping Summary

| Business Capability | Current Jobs | Suggested Microservice Boundary | Key Datasets |
|:--------------------|:-------------|:-------------------------------|:-------------|
| Transaction Processing | POSTTRAN, TRANBKP, COMBTRAN | Transaction Service | TRANSACT, DALYTRAN, TCATBALF, ACCTDATA, CARDXREF |
| Interest & Billing | INTCALC, CREASTMT | Billing Service | TCATBALF, DISCGRP, ACCTDATA, CARDXREF, SYSTRAN |
| Reference Data | MNTTRDB2, TRANEXTR, DISCGRP, TRANCATG, TRANTYPE | Reference Data Service | DB2 tables, DISCGRP, TRANCATG, TRANTYPE |
| File Management | CLOSEFIL, OPENFIL, WAITSTEP | Infrastructure / Orchestration | N/A (operational) |
| Data Import/Export | CBEXPORT, CBIMPORT | Data Migration Service | EXPORT.DATA, all master files |
| Security | DUSRSECJ | Auth Service | USRSEC |
| Master Data Load | ACCTFILE, CARDFILE, CUSTFILE, XREFFILE | Account/Customer Service | ACCTDATA, CARDDATA, CUSTDATA, CARDXREF |
