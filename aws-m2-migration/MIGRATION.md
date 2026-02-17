# Phase 1: Lift-and-Shift Migration to AWS Mainframe Modernization

## Overview

This document describes the Phase 1 migration of the CardDemo mainframe application's batch processing system from an on-premises Control-M scheduler to AWS Mainframe Modernization (M2) with AWS-native orchestration services.

**Migration approach**: Lift-and-shift. All COBOL programs remain unchanged. Only the scheduling and infrastructure layer is modernized.

## Architecture

### Source (On-Premises)

```
Control-M Scheduler
    ├── DAILY-TransactionBackup (Folder)
    │   └── CLOSEFIL → TRANBKP → WAITSTEP → OPENFIL
    │
    ├── WEEKLY-TransactionTypesDBRefresh (Folder)
    │   ├── MNTTRDB2
    │   │
    │   ├── WEEKLY-DisclosureGroupsRefresh (Smart Folder, after MNTTRDB2)
    │   │   └── CLOSEFIL → DISCGRP → WAITSTEP → OPENFIL
    │   │
    │   └── WEEKLY-TransactionTypesDBRefresh (Smart Folder, after MNTTRDB2)
    │       └── TRANEXTR
    │
    └── MONTHLY-InterestCalculation (Folder)
        └── CLOSEFIL → INTCALC → COMBTRAN → WAITSTEP → OPENFIL
```

Job dependencies are enforced via Control-M INCOND/OUTCOND conditions forming sequential chains within each folder.

### Target (AWS)

```
Amazon EventBridge Scheduler
    ├── Daily Schedule (cron: 0 2 * * ? *)
    │   └── Step Functions: CardDemo-Daily-TransactionBackup
    │       └── CLOSEFIL → TRANBKP → WAITSTEP → OPENFIL
    │
    ├── Weekly Schedule (cron: 0 3 ? * SAT *)
    │   └── Step Functions: CardDemo-Weekly-TransactionTypesDBRefresh
    │       └── MNTTRDB2 → Parallel [
    │                         TRANEXTR,
    │                         CardDemo-Weekly-DisclosureGroupsRefresh
    │                           └── CLOSEFIL → DISCGRP → WAITSTEP → OPENFIL
    │                       ]
    │
    └── Monthly Schedule (cron: 0 4 1 * ? *)
        └── Step Functions: CardDemo-Monthly-InterestCalculation
            └── CLOSEFIL → INTCALC → COMBTRAN → WAITSTEP → OPENFIL

AWS Mainframe Modernization (M2) Runtime
    ├── COBOL batch programs (unchanged)
    ├── JCL files (unchanged)
    └── VSAM data on EFS / S3
```

## Component Mapping

### Scheduler Migration

| Control-M Component | AWS Service | Resource |
|---|---|---|
| DAILY-TransactionBackup folder | EventBridge Scheduler + Step Functions | `CardDemo-Daily-TransactionBackup` |
| WEEKLY-TransactionTypesDBRefresh folder | EventBridge Scheduler + Step Functions | `CardDemo-Weekly-TransactionTypesDBRefresh` |
| WEEKLY-DisclosureGroupsRefresh smart folder | Step Functions (nested execution) | `CardDemo-Weekly-DisclosureGroupsRefresh` |
| MONTHLY-InterestCalculation folder | EventBridge Scheduler + Step Functions | `CardDemo-Monthly-InterestCalculation` |
| INCOND/OUTCOND job dependencies | Step Functions sequential state transitions | Choice states with polling |
| MAXRERUN=5 retry policy | Step Functions Retry configuration | `MaxAttempts: 3` per API call |
| MAXWAIT=7 timeout | EventBridge Scheduler retry policy | `MaximumEventAgeInSeconds: 3600` |
| DAYS=ALL (daily) | EventBridge cron | `cron(0 2 * * ? *)` |
| DAYS=SA (weekly) | EventBridge cron | `cron(0 3 ? * SAT *)` |
| Monthly schedule | EventBridge cron | `cron(0 4 1 * ? *)` |

### Job-to-JCL Mapping

| Job Name | JCL File | Program | Description |
|---|---|---|---|
| CLOSEFIL | `CLOSEFIL.jcl` | SDSF/IEFBR14 | Close VSAM files in CICS for exclusive batch access |
| OPENFIL | `OPENFIL.jcl` | SDSF/IEFBR14 | Reopen VSAM files in CICS |
| TRANBKP | `TRANBKP.jcl` | IDCAMS/REPROC | REPRO transaction VSAM to GDG backup, DELETE/DEFINE cluster |
| WAITSTEP | `WAITSTEP.jcl` | COBSWAIT | Wait 36 seconds (PARM=00003600 centiseconds) |
| INTCALC | `INTCALC.jcl` | CBACT04C | Interest/fee calculation on TCATBALF balances |
| COMBTRAN | `COMBTRAN.jcl` | SORT/IDCAMS | Sort-merge system + daily transactions, REPRO to VSAM master |
| DISCGRP | `DISCGRP.jcl` | IDCAMS | Refresh Disclosure Group VSAM from flat file |
| MNTTRDB2 | `MNTTRDB2.jcl` | COBTUPDT | DB2 Transaction Type table maintenance (optional module) |
| TRANEXTR | `TRANEXTR.jcl` | DSNTIAUL | Extract DB2 transaction type/category data (optional module) |

### Infrastructure Mapping

| On-Premises Component | AWS Service |
|---|---|
| Mainframe LPAR | AWS M2 Runtime Environment |
| VSAM datasets | EFS mount + M2 dataset catalog |
| GDG datasets | M2 GDG catalog (managed by runtime) |
| JCL library (PDS) | S3 bucket |
| COBOL load library | S3 bucket / M2 application package |
| DB2 subsystem | Amazon RDS for Db2 (or M2-managed) |
| SDSF (CICS file commands) | M2 CICS file management |
| SYSOUT/SYSPRINT | CloudWatch Logs |

## Dependency Chain Preservation

The Control-M INCOND/OUTCOND mechanism is preserved using Step Functions sequential state transitions:

### Daily Cycle
```
CLOSEFIL (OUTCOND: +CLOSEFIL)
    → TRANBKP (INCOND: CLOSEFIL, OUTCOND: -CLOSEFIL, +TRANBKP)
        → WAITSTEP (INCOND: TRANBKP, OUTCOND: -TRANBKP, +WAITSTEP)
            → OPENFIL (INCOND: WAITSTEP, OUTCOND: -WAITSTEP)
```

In Step Functions, each job starts only after the previous job's polling loop confirms `Status: Succeeded`. A `Failed` status triggers the emergency OPENFIL recovery path.

### Weekly Cycle (Cross-Folder Dependency)
The Control-M config has a cross-folder dependency: `MNTTRDB2` in the `WEEKLY-TransactionTypesDBRefresh` folder produces an OUTCOND consumed by both:
- The `DisclosureGroupsRefresh` smart folder (CLOSEFIL's INCOND)
- The `TransactionTypesDBRefresh` smart folder (TRANEXTR's INCOND)

This is preserved using a Step Functions `Parallel` state that runs both branches after MNTTRDB2 succeeds. The DisclosureGroupsRefresh branch invokes a nested state machine execution (`states:startExecution.sync:2`).

## File Locking Pattern

The CLOSEFIL/OPENFIL pattern is critical for data integrity:

1. **CLOSEFIL** sends CEMT commands to close VSAM files (TRANSACT, CCXREF, ACCTDAT, CXACAIX, USRSEC) in the CICS region, preventing online transactions from accessing data during batch processing.

2. **Batch jobs** run with exclusive access to the VSAM datasets.

3. **WAITSTEP** provides a 36-second safety delay for buffer flush.

4. **OPENFIL** reopens the VSAM files for CICS online access.

**Emergency recovery**: If any batch job fails after CLOSEFIL, the state machine executes an `EmergencyOpenFiles` state to reopen VSAM files before transitioning to the failure state. This prevents CICS from being locked out indefinitely.

## CloudFormation Stacks

Deploy in this order:

| Stack | Template | Description |
|---|---|---|
| 1. `carddemo-m2-environment` | `cloudformation/m2-environment.yaml` | M2 runtime, EFS, S3 bucket, M2 application |
| 2. `carddemo-m2-iam-roles` | `cloudformation/iam-roles.yaml` | Step Functions, EventBridge, M2 execution roles |
| 3. `carddemo-m2-batch-orchestration` | `cloudformation/batch-orchestration.yaml` | State machines, schedules, alarms, log groups |

## Deployment

### Automated

```bash
./scripts/deploy.sh \
  --vpc-id vpc-xxxxxxxxx \
  --subnet-ids "subnet-aaa,subnet-bbb" \
  --region us-east-1 \
  --engine-type microfocus \
  --notification-email ops@example.com
```

### Manual Steps After Deployment

1. Upload compiled COBOL load modules to S3
2. Upload VSAM/PS data files to EFS or S3
3. Start the M2 environment
4. Deploy the M2 application version
5. Run a manual test of each state machine before enabling schedules

## Monitoring

| Metric | Source | Alert |
|---|---|---|
| Batch job execution status | Step Functions execution history | CloudWatch Alarm on `ExecutionsFailed` |
| Individual JCL job logs | CloudWatch Logs (`/aws/m2/carddemo-m2/batch-jobs`) | - |
| State machine execution logs | CloudWatch Logs (`/aws/stepfunctions/CardDemo-*`) | - |
| Schedule invocation failures | EventBridge Scheduler metrics | SNS notification (if configured) |

## Files in This Directory

```
aws-m2-migration/
├── MIGRATION.md                          # This document
├── cloudformation/
│   ├── m2-environment.yaml               # M2 runtime, EFS, S3, application
│   ├── iam-roles.yaml                    # IAM roles for Step Functions, EventBridge, M2
│   └── batch-orchestration.yaml          # State machines, schedules, alarms
├── batch-definitions/
│   ├── daily-transaction-backup.asl.json             # Daily cycle state machine (ASL)
│   ├── weekly-transaction-types-db-refresh.asl.json  # Weekly parent state machine (ASL)
│   ├── weekly-disclosure-groups-refresh.asl.json     # Weekly sub-workflow state machine (ASL)
│   ├── monthly-interest-calculation.asl.json         # Monthly cycle state machine (ASL)
│   └── m2-application-definition.json                # M2 application definition (datasets, programs)
└── scripts/
    └── deploy.sh                         # Automated deployment script
```
