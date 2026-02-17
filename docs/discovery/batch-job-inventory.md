# Batch Job Inventory

This document catalogs all batch jobs in the CardDemo application, organized by their scheduling frequency.

> **Source files:**
> - Scheduler: [`app/scheduler/CardDemo.controlm`](../../app/scheduler/CardDemo.controlm)
> - CA7 config: [`app/scheduler/CardDemo.ca7`](../../app/scheduler/CardDemo.ca7)
> - JCL members: [`app/jcl/`](../../app/jcl/)

---

## Daily Cycle Jobs

**Folder:** `DAILY-TransactionBackup` (Control-M lines 3-25)

| Job Name | Program | Description | Dependencies (INCOND) |
|:---------|:--------|:------------|:----------------------|
| CLOSEFIL | SDSF (IEFBR14) | Closes VSAM files opened by CICS to allow batch processing | None (first job in chain) |
| TRANBKP | IDCAMS / REPROC proc | Backup (REPRO) the Transaction VSAM master to a GDG flat file, then delete and redefine the VSAM cluster | CLOSEFIL must complete |
| POSTTRAN | CBTRN02C | Core transaction processing - reads daily transaction file, cross-reference, and account data to post transactions and update category balances | TRANBKP must complete (per CA7 trigger chain) |
| WAITSTEP | COBSWAIT | Timer wait step; pauses the workflow for a configured interval before proceeding | POSTTRAN must complete (per CA7: POSTTRAN triggers WAITSTEP) |
| OPENFIL | SDSF (IEFBR14) | Re-opens VSAM files in the CICS region so online processing can resume | WAITSTEP must complete |

### Daily Chain (Control-M INCOND/OUTCOND)

```
CLOSEFIL ──OUTCOND──▶ TRANBKP ──OUTCOND──▶ WAITSTEP ──OUTCOND──▶ OPENFIL
```

### Daily Chain (CA7 Triggered Jobs)

```
CLOSEFIL ──triggers──▶ CBPAUP0J ──triggers──▶ POSTTRAN ──triggers──▶ WAITSTEP ──triggers──▶ OPENFIL
```

> **Note:** The CA7 configuration shows CLOSEFIL triggering CBPAUP0J (Purge Expired Authorizations) before POSTTRAN, which is part of the optional IMS-DB2-MQ Pending Authorizations module.

---

## Weekly Cycle Jobs

### Folder: `WEEKLY-TransactionTypesDBRefresh` (Control-M lines 26-63)

| Job Name | Program | Description | Dependencies (INCOND) |
|:---------|:--------|:------------|:----------------------|
| MNTTRDB2 | COBTUPDT | Batch maintenance program for transaction type updates in DB2 tables | None (first job in weekly cycle) |
| TRANEXTR | DSNTIAUL | Extracts transaction type and category tables from DB2 into flat files for VSAM loading | MNTTRDB2 must complete |

### Folder: `WEEKLY-DisclosureGroupsRefresh` (Control-M lines 32-56, SMART_FOLDER)

Runs on **Saturdays** (`DAYS="SA"`).

| Job Name | Program | Description | Dependencies (INCOND) |
|:---------|:--------|:------------|:----------------------|
| CLOSEFIL | SDSF (IEFBR14) | Closes VSAM files in CICS for batch update | MNTTRDB2 must complete (cross-folder dependency) |
| DISCGRP | IDCAMS | Deletes, redefines, and reloads Disclosure Group VSAM file from flat file (`AWS.M2.CARDDEMO.DISCGRP.PS`) | CLOSEFIL must complete |
| TRANCATG | IDCAMS | Deletes, redefines, and reloads Transaction Category VSAM from flat file (`AWS.M2.CARDDEMO.TRANCATG.PS`) | (CA7: triggered by CLOSEFIL1) |
| TRANTYPE | IDCAMS | Deletes, redefines, and reloads Transaction Type VSAM from flat file (`AWS.M2.CARDDEMO.TRANTYPE.PS`) | (CA7: triggered by CLOSEFIL) |
| WAITSTEP | COBSWAIT | Timer wait step | DISCGRP must complete |
| OPENFIL | SDSF (IEFBR14) | Re-opens files in CICS | WAITSTEP must complete |

---

## Monthly Cycle Jobs

**Folder:** `MONTHLY-InterestCalculation` (Control-M lines 64-92)

| Job Name | Program | Description | Dependencies (INCOND) |
|:---------|:--------|:------------|:----------------------|
| CLOSEFIL | SDSF (IEFBR14) | Closes VSAM files opened by CICS for batch processing | None (first job in monthly chain) |
| INTCALC | CBACT04C | Processes transaction category balance file to compute interest and fees on accounts | CLOSEFIL must complete |
| COMBTRAN | SORT / IDCAMS | Sorts and combines system-generated transactions with the daily transaction backup, then loads the combined result into the VSAM transaction master | INTCALC must complete |
| WAITSTEP | COBSWAIT | Timer wait step | COMBTRAN must complete |
| OPENFIL | SDSF (IEFBR14) | Re-opens files in CICS | WAITSTEP must complete |

---

## Standalone / On-Demand Batch Jobs

These jobs are not part of the scheduled cycles but are available for execution:

| Job Name | Program | Description | JCL Source |
|:---------|:--------|:------------|:-----------|
| DUSRSECJ | IEBGENER | Initial load of user security VSAM file | `app/jcl/DUSRSECJ.jcl` |
| ACCTFILE | IDCAMS | Refresh Account Master VSAM from flat file | `app/jcl/ACCTFILE.jcl` |
| CARDFILE | IDCAMS | Refresh Card Master VSAM from flat file | `app/jcl/CARDFILE.jcl` |
| CUSTFILE | IDCAMS | Refresh Customer Master VSAM from flat file | `app/jcl/CUSTFILE.jcl` |
| XREFFILE | IDCAMS | Load Customer-Account-Card cross reference VSAM | `app/jcl/XREFFILE.jcl` |
| TRANFILE | IDCAMS | Load Transaction Master VSAM file | `app/jcl/TRANFILE.jcl` |
| TCATBALF | IDCAMS | Refresh Transaction Category Balance VSAM | `app/jcl/TCATBALF.jcl` |
| TRANIDX | IDCAMS | Define alternate index (AIX) on the transaction VSAM file | `app/jcl/TRANIDX.jcl` |
| CREASTMT | CBSTM03A | Produce transaction statement report | `app/jcl/CREASTMT.JCL` |
| TRANREPT | CBTRN03C | Transaction report - submitted from CICS | `app/jcl/TRANREPT.jcl` |
| CREADB21 | DSNTEP4 | Creates CardDemo DB2 database and loads tables | (Optional: DB2 module) |
| DEFGDGB | IDCAMS | Setup GDG base entries | `app/jcl/DEFGDGB.jcl` |
| DEFGDGD | IDCAMS | Setup additional GDG bases for DB2 | `app/jcl/DEFGDGD.jcl` |
| ESDSRRDS | IDCAMS | Create ESDS and RRDS VSAM files | `app/jcl/ESDSRRDS.jcl` |
| CBEXPORT | CBEXPORT | Export customer data from VSAM files to a multi-record export file | `app/jcl/CBEXPORT.jcl` |
| CBIMPORT | CBIMPORT | Import customer data from export file into normalized flat files | `app/jcl/CBIMPORT.jcl` |
| CBPAUP0J | CBPAUP0C | Purge expired authorizations (optional IMS-DB2-MQ module) | (Optional module) |
| MNTTRDB2 | COBTUPDT | Maintain transaction type table in DB2 (optional DB2 module) | (Optional module) |

---

## Program Reference

| Program | Language | Used By Job(s) | Function |
|:--------|:---------|:----------------|:---------|
| CBTRN02C | COBOL | POSTTRAN | Core batch transaction posting |
| CBACT04C | COBOL | INTCALC | Interest and fee calculation |
| CBSTM03A | COBOL/ASM | CREASTMT | Statement generation |
| CBTRN03C | COBOL | TRANREPT | Transaction reporting |
| COBTUPDT | COBOL | MNTTRDB2 | DB2 transaction type maintenance |
| COBSWAIT | COBOL/ASM | WAITSTEP | Timer wait utility |
| CBEXPORT | COBOL | CBEXPORT | VSAM data export |
| CBIMPORT | COBOL | CBIMPORT | Data import and normalization |
| CBPAUP0C | COBOL | CBPAUP0J | Purge expired authorizations |
| DSNTEP4 | DB2 Utility | CREADB21 | DB2 DDL execution |
| DSNTIAUL | DB2 Utility | TRANEXTR | DB2 data extraction (UNLOAD) |
| IDCAMS | System Utility | Multiple | VSAM define, delete, repro operations |
| IEBGENER | System Utility | DUSRSECJ | Sequential dataset copy |
| IEFBR14 | System Utility | CLOSEFIL, OPENFIL | Placeholder program for file disposition |
| SDSF | System Utility | CLOSEFIL, OPENFIL | Issue CICS CEMT commands to close/open files |
| SORT | System Utility | COMBTRAN | Sort and merge datasets |
