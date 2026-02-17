# Critical Patterns

This document identifies architectural patterns in the CardDemo batch system that must be understood and preserved (or intentionally replaced) during migration to microservices.

> **Source files:**
> - JCL: [`app/jcl/`](../../app/jcl/)
> - Scheduler: [`app/scheduler/CardDemo.controlm`](../../app/scheduler/CardDemo.controlm), [`app/scheduler/CardDemo.ca7`](../../app/scheduler/CardDemo.ca7)

---

## 1. CLOSEFIL/OPENFIL Pattern

### Description

Every batch cycle (daily, weekly, monthly) is bookended by CLOSEFIL at the start and OPENFIL at the end. This pattern ensures CICS online transactions cannot access VSAM files while batch jobs are updating them.

### Implementation

**CLOSEFIL** (`app/jcl/CLOSEFIL.jcl`) issues CICS CEMT commands via SDSF:
```
/F CICSAWSA,'CEMT SET FIL(TRANSACT ) CLO'
/F CICSAWSA,'CEMT SET FIL(CCXREF ) CLO'
/F CICSAWSA,'CEMT SET FIL(ACCTDAT ) CLO'
/F CICSAWSA,'CEMT SET FIL(CXACAIX ) CLO'
/F CICSAWSA,'CEMT SET FIL(USRSEC ) CLO'
```

**OPENFIL** (`app/jcl/OPENFIL.jcl`) reverses the operation:
```
/F CICSAWSA,'CEMT SET FIL(TRANSACT ) OPE'
/F CICSAWSA,'CEMT SET FIL(CCXREF ) OPE'
/F CICSAWSA,'CEMT SET FIL(ACCTDAT ) OPE'
/F CICSAWSA,'CEMT SET FIL(CXACAIX ) OPE'
/F CICSAWSA,'CEMT SET FIL(USRSEC ) OPE'
```

### Files Affected

| CICS File | VSAM Dataset | Purpose |
|:----------|:-------------|:--------|
| TRANSACT | AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | Transaction master |
| CCXREF | AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS | Card cross-reference |
| ACCTDAT | AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | Account data |
| CXACAIX | AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH | Cross-reference alternate index |
| USRSEC | AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS | User security |

### Usage Across Cycles

| Cycle | CLOSEFIL | OPENFIL | Control-M Folder |
|:------|:---------|:--------|:-----------------|
| Daily | Line 4 (JOBISN=1) | Line 20 (JOBISN=4) | DAILY-TransactionBackup |
| Weekly (Disclosure) | Line 33 (JOBISN=1) | Line 50 (JOBISN=4) | WEEKLY-DisclosureGroupsRefresh |
| Monthly | Line 65 (JOBISN=1) | Line 87 (JOBISN=4) | MONTHLY-InterestCalculation |

### Migration Considerations

- **Problem:** In a distributed system, there is no equivalent of "closing a file" to prevent access
- **Options for replacement:**
  - **Database-level locking:** Use row/table-level locks during batch processing windows
  - **Event-driven architecture:** Publish "batch-started" / "batch-completed" events; online services pause writes
  - **Read replicas:** Process batch against a separate replica; swap when complete
  - **Feature flags / circuit breakers:** Temporarily disable online write paths during batch windows
  - **Optimistic concurrency:** Use versioning to detect and resolve conflicts between batch and online updates

---

## 2. DB2 Integration Pattern

### Description

The CardDemo application uses DB2 as a secondary data store for reference data (transaction types and categories). A set of jobs maintains DB2 tables and extracts data back to flat files for loading into VSAM.

### Implementation

**CREADB21** (uses program `DSNTEP4`):
- Creates the CardDemo DB2 database schema
- Loads initial data into transaction type and category tables
- Part of the optional "Db2: Transaction Type Mgmt" module

**MNTTRDB2** (`app/jcl/MNTTRDB2.jcl`, uses program `COBTUPDT`):
- Batch maintenance program that applies updates to transaction types in DB2
- Runs weekly as the first job in the `WEEKLY-TransactionTypesDBRefresh` folder
- Uses COBOL program `COBTUPDT` for DB2 operations (cursors, SQL inserts/updates/deletes)

**TRANEXTR** (uses program `DSNTIAUL`):
- Extracts transaction type and category data from DB2 using the DSNTIAUL utility (DB2 UNLOAD)
- Produces flat files that are subsequently loaded into VSAM by DISCGRP, TRANCATG, and TRANTYPE jobs
- Depends on MNTTRDB2 completing successfully

### Data Flow

```
Online CICS ──▶ DB2 Tables ──▶ MNTTRDB2 (batch updates)
                    │
                    ▼
               TRANEXTR (extract to flat files)
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
      DISCGRP   TRANCATG   TRANTYPE
     (to VSAM)  (to VSAM)  (to VSAM)
          │         │         │
          └─────────┼─────────┘
                    ▼
            Online CICS reads
            Batch jobs read
```

### Migration Considerations

- **Dual data store pattern:** DB2 serves as the system of record for reference data, but VSAM copies are maintained for CICS online access
- **Data synchronization:** The weekly extract-and-load cycle means VSAM data can be up to a week behind DB2
- **In microservices:** A single Reference Data Service with a relational database could replace both DB2 and VSAM, eliminating the synchronization gap

---

## 3. GDG (Generation Data Group) Pattern

### Description

Several batch jobs use GDG datasets to maintain rolling generations of backup and intermediate files. GDGs provide automatic versioning where `(+1)` creates a new generation and `(0)` references the current generation.

### Usage

| GDG Base | Purpose | Written By | Read By |
|:---------|:--------|:-----------|:--------|
| AWS.M2.CARDDEMO.TRANSACT.BKUP | Transaction backup generations | TRANBKP (+1) | COMBTRAN (0) |
| AWS.M2.CARDDEMO.SYSTRAN | System-generated transactions | INTCALC (+1) | COMBTRAN (0) |
| AWS.M2.CARDDEMO.TRANSACT.COMBINED | Combined transaction file | COMBTRAN (+1) | COMBTRAN (reload to VSAM) |
| AWS.M2.CARDDEMO.DALYREJS | Daily rejected transactions | POSTTRAN (+1) | (audit/review) |

### Setup

GDG bases are defined by two initialization jobs:
- **DEFGDGB** (`app/jcl/DEFGDGB.jcl`): Defines the base GDG entries using IDCAMS
- **DEFGDGD** (`app/jcl/DEFGDGD.jcl`): Defines additional GDG bases for DB2-related files

### Migration Considerations

- **In microservices:** GDG patterns map to timestamped object storage (S3 versioning), database snapshots, or event sourcing
- **The rolling generation concept** provides natural audit trails and rollback capabilities that should be preserved

---

## 4. VSAM Delete-Define-Repro Pattern

### Description

Multiple jobs follow a three-step pattern for refreshing VSAM files:
1. **DELETE** the existing VSAM cluster (IDCAMS DELETE)
2. **DEFINE** a new empty cluster (IDCAMS DEFINE CLUSTER)
3. **REPRO** data from a flat file into the new cluster (IDCAMS REPRO)

### Jobs Using This Pattern

| Job | VSAM Target | Source Flat File |
|:----|:------------|:-----------------|
| DISCGRP | AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS | AWS.M2.CARDDEMO.DISCGRP.PS |
| TRANCATG | AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS | AWS.M2.CARDDEMO.TRANCATG.PS |
| TRANTYPE | AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS | AWS.M2.CARDDEMO.TRANTYPE.PS |
| TRANBKP | AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | (redefined empty after backup) |

### Migration Considerations

- **Destructive reload:** The delete-define-repro pattern means the VSAM file is unavailable during the reload window, reinforcing the need for CLOSEFIL/OPENFIL
- **In microservices:** This maps to database truncate-and-reload, or blue-green table swaps for zero-downtime reference data updates

---

## 5. Scheduler Configuration Patterns

### Control-M Configuration

**File:** `app/scheduler/CardDemo.controlm`

The Control-M configuration uses XML-based job definitions with:
- **FOLDER** elements for simple linear chains (DAILY, MONTHLY)
- **SMART_FOLDER** elements for more complex workflows with cross-folder dependencies (WEEKLY)
- **INCOND/OUTCOND** conditions for dependency management
- **SIGN="+"** to set a condition, **SIGN="-"** to consume/clear a condition

Key attributes per job:
- `MAXRERUN="5"`: Jobs can be automatically rerun up to 5 times on failure
- `MAXWAIT="7"`: Jobs will wait up to 7 days for predecessor conditions
- `AUTOARCH="1"`: Job output is automatically archived
- `TIMETO="23:00"`: Jobs must complete by 23:00

### CA7 Configuration

**File:** `app/scheduler/CardDemo.ca7`

The CA7 configuration shows a trigger-based scheduling model:
- **COMP TRIGGERS OTHER JOBS**: Completion of one job triggers submission of the next
- **SCHID**: Schedule identifier used to group related trigger chains
- Jobs like CLOSEFIL trigger multiple downstream chains (SCHID=030, 031, 032)

Notable CA7-specific patterns:
- CLOSEFIL triggers CBPAUP0J (optional authorization purge) before POSTTRAN
- WAITSTEP triggers both CLOSEFIL1 and CLOSEFIL2 (parallel branches)
- CLOSEFIL1 triggers TRANCATG, CLOSEFIL2 triggers TCATBALF (parallel processing)

### Migration Considerations

- **Dual scheduler support:** The codebase supports both Control-M and CA7, suggesting the application runs in multiple mainframe environments
- **In microservices:** Replace with AWS Step Functions, Apache Airflow, or similar workflow orchestration
- **Condition-based dependencies** map well to event-driven architectures (SNS/SQS, EventBridge)
- **Parallel branching** in CA7 (CLOSEFIL1/CLOSEFIL2) should be preserved in the target architecture for performance

---

## 6. CICS-Batch Coexistence Pattern

### Description

The CardDemo application operates in two modes:
- **Online (CICS):** Interactive transaction processing via 3270 terminals
- **Batch (JCL):** Scheduled processing of accumulated transactions

Both modes share the same VSAM data stores, creating a mutual exclusion requirement managed by the CLOSEFIL/OPENFIL pattern.

### Shared Resources

```
┌──────────────┐     ┌─────────────────────────┐     ┌──────────────┐
│  CICS Online │     │     Shared VSAM Files    │     │  Batch Jobs  │
│  Transactions│◄───▶│  TRANSACT, ACCTDAT,      │◄───▶│  POSTTRAN,   │
│  (CC00, etc.)│     │  CCXREF, CXACAIX, USRSEC │     │  INTCALC,    │
│              │     │                           │     │  TRANBKP,    │
│  (runs while │     │  Only one mode can write  │     │  COMBTRAN    │
│   files OPEN)│     │  at a time                │     │  (runs while │
│              │     │                           │     │   files CLO) │
└──────────────┘     └─────────────────────────┘     └──────────────┘
```

### Migration Considerations

- **Decoupling online and batch:** In a microservices architecture, online and batch processing should share a database with proper concurrency controls rather than requiring mutual exclusion
- **CQRS pattern:** Consider separating read and write models, with batch processing on the write side and online queries on read replicas
- **Event sourcing:** Transactions could be captured as events, with batch processing consuming event streams rather than shared files

---

## Summary of Patterns and Migration Impact

| Pattern | Current Implementation | Migration Impact | Priority |
|:--------|:----------------------|:-----------------|:---------|
| CLOSEFIL/OPENFIL | CEMT commands via SDSF | **High** - Must be replaced with distributed coordination | Critical |
| DB2 Integration | DSNTEP4, DSNTIAUL, COBOL SQL | **Medium** - DB2 can migrate to RDS/Aurora; eliminate VSAM sync | High |
| GDG Versioning | IDCAMS GDG definitions | **Medium** - Replace with S3 versioning or database snapshots | Medium |
| Delete-Define-Repro | IDCAMS three-step VSAM reload | **Low** - Standard database operations replace this | Medium |
| Scheduler Dependencies | Control-M INCOND/OUTCOND, CA7 triggers | **High** - Must be replaced with cloud-native orchestration | Critical |
| CICS-Batch Coexistence | Mutual exclusion via file open/close | **High** - Core architectural concern for microservices | Critical |
