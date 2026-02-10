# CardDemo Application - End-to-End Execution Flow

**Repository**: `aws-mainframe-modernization-carddemo`
**Analysis Date**: 2026-02-09
**Analysis Duration**: ~25 minutes (automated trace of all inter-program calls, batch sequencing, and data passing)
**Source**: All conclusions traced to code artifacts in the repository.

---

## Table of Contents

1. [System Entry Points](#1-system-entry-points)
2. [Online (CICS) Execution Flow](#2-online-cics-execution-flow)
3. [Program-to-Program Calls](#3-program-to-program-calls)
4. [Data Passed Between Programs](#4-data-passed-between-programs)
5. [Batch Job Execution Flows](#5-batch-job-execution-flows)
6. [Batch Job Sequencing (Scheduler)](#6-batch-job-sequencing-scheduler)
7. [Online-to-Batch Bridge](#7-online-to-batch-bridge)
8. [Complete Data Flow Diagram](#8-complete-data-flow-diagram)

---

## 1. System Entry Points

### 1.1 Online Entry Point (CICS)

| Entry Point | TRANID | Program | Source File | Mechanism |
|---|---|---|---|---|
| Sign-On Screen | `CC00` | `COSGN00C` | `app/cbl/COSGN00C.cbl` | CICS terminal user types TRANID `CC00` |

- `COSGN00C` is the sole entry point for all online users.
- Source: `COSGN00C.cbl` line 37: `05 WS-TRANID PIC X(04) VALUE 'CC00'.`
- First interaction: `EIBCALEN = 0` triggers initial sign-on screen display (`COSGN00C.cbl` line 80-83).

### 1.2 Batch Entry Points

| Entry Point | JCL | Program Executed | Source File | Trigger |
|---|---|---|---|---|
| Daily Transaction Post | `POSTTRAN.jcl` | `CBTRN02C` | `app/jcl/POSTTRAN.jcl` line 23 | Scheduler (CA7/Control-M) |
| Transaction Report | `TRANREPT.jcl` | `CBTRN03C` | `app/jcl/TRANREPT.jcl` line 59 | Scheduler or CICS TDQ submission |
| Interest Calculation | `INTCALC.jcl` | `CBACT04C` | `app/jcl/INTCALC.jcl` line 22 | Scheduler |
| Statement Creation | `CREASTMT.JCL` | `CBSTM03A` | `app/jcl/CREASTMT.JCL` line 79 | Scheduler |
| Data Import | `CBIMPORT.jcl` | `CBIMPORT` | `app/jcl/CBIMPORT.jcl` | Manual/Migration |
| Data Export | `CBEXPORT.jcl` | `CBEXPORT` | `app/jcl/CBEXPORT.jcl` | Manual/Migration |

---

## 2. Online (CICS) Execution Flow

### 2.1 Authentication Flow

```
User Terminal
    |
    | TRANID 'CC00'
    v
COSGN00C (Sign-On)
    |
    | Reads USRSEC file (VSAM KSDS)
    | Key: WS-USER-ID (8 bytes)
    | Source: COSGN00C.cbl lines 211-219
    |
    | Validates: SEC-USR-PWD = WS-USER-PWD
    | Source: COSGN00C.cbl line 223
    |
    +---> IF CDEMO-USRTYP-ADMIN ('A')
    |         |
    |         | EXEC CICS XCTL PROGRAM('COADM01C') COMMAREA(CARDDEMO-COMMAREA)
    |         | Source: COSGN00C.cbl lines 231-234
    |         v
    |     COADM01C (Admin Menu, TRANID 'CA00')
    |
    +---> IF CDEMO-USRTYP-USER ('U')
              |
              | EXEC CICS XCTL PROGRAM('COMEN01C') COMMAREA(CARDDEMO-COMMAREA)
              | Source: COSGN00C.cbl lines 236-239
              v
          COMEN01C (Regular User Menu, TRANID 'CM00')
```

**Data populated into COMMAREA before transfer** (source: `COSGN00C.cbl` lines 224-228):
- `CDEMO-FROM-TRANID` = `CC00`
- `CDEMO-FROM-PROGRAM` = `COSGN00C`
- `CDEMO-USER-ID` = entered user ID
- `CDEMO-USER-TYPE` = `SEC-USR-TYPE` from USRSEC file ('A' or 'U')
- `CDEMO-PGM-CONTEXT` = `ZEROS` (fresh entry)

### 2.2 Regular User Menu Routing (COMEN01C)

Source: `app/cpy/COMEN02Y.cpy` lines 19-98

| Menu Option | Label | Target Program | Source (COMEN02Y.cpy) |
|---|---|---|---|
| 1 | Account View | `COACTVWC` | Line 28 |
| 2 | Account Update | `COACTUPC` | Line 34 |
| 3 | Credit Card List | `COCRDLIC` | Line 40 |
| 4 | Credit Card View | `COCRDSLC` | Line 46 |
| 5 | Credit Card Update | `COCRDUPC` | Line 52 |
| 6 | Transaction List | `COTRN00C` | Line 58 |
| 7 | Transaction View | `COTRN01C` | Line 64 |
| 8 | Transaction Add | `COTRN02C` | Line 71 |
| 9 | Transaction Reports | `CORPT00C` | Line 77 |
| 10 | Bill Payment | `COBIL00C` | Line 83 |
| 11 | Pending Authorization View | `COPAUS0C` | Line 89 |

**Transfer mechanism**: `EXEC CICS XCTL PROGRAM(CDEMO-MENU-OPT-PGMNAME(WS-OPTION)) COMMAREA(CARDDEMO-COMMAREA)`
Source: `COMEN01C.cbl` lines 184-187

**Data set in COMMAREA before transfer** (source: `COMEN01C.cbl` lines 178-183):
- `CDEMO-FROM-TRANID` = `CM00`
- `CDEMO-FROM-PROGRAM` = `COMEN01C`
- `CDEMO-PGM-CONTEXT` = `ZEROS`

### 2.3 Admin Menu Routing (COADM01C)

Source: `app/cpy/COADM02Y.cpy` lines 19-59

| Menu Option | Label | Target Program | Source (COADM02Y.cpy) |
|---|---|---|---|
| 1 | User List (Security) | `COUSR00C` | Line 29 |
| 2 | User Add (Security) | `COUSR01C` | Line 34 |
| 3 | User Update (Security) | `COUSR02C` | Line 39 |
| 4 | User Delete (Security) | `COUSR03C` | Line 44 |
| 5 | Transaction Type List/Update (Db2) | `COTRTLIC` | Line 49 |
| 6 | Transaction Type Maintenance (Db2) | `COTRTUPC` | Line 53 |

**Transfer mechanism**: `EXEC CICS XCTL PROGRAM(CDEMO-ADMIN-OPT-PGMNAME(WS-OPTION)) COMMAREA(CARDDEMO-COMMAREA)`
Source: `COADM01C.cbl` lines 145-148

### 2.4 Return Flow (All Online Programs)

Every online program returns to CICS with:
```
EXEC CICS RETURN TRANSID(WS-TRANID) COMMAREA(CARDDEMO-COMMAREA)
```

When the user presses **PF3** from a functional program, the program XCTLs back to its menu:
- Regular programs XCTL to `CDEMO-TO-PROGRAM` (typically `COMEN01C`)
- Admin programs XCTL to `CDEMO-TO-PROGRAM` (typically `COADM01C`)
- Menu programs XCTL to `COSGN00C` on PF3

Source examples:
- `COMEN01C.cbl` lines 97-98: PF3 returns to `COSGN00C`
- `COADM01C.cbl` lines 100-102: PF3 returns to `COSGN00C`
- `COCRDLIC.cbl` lines 402-405: PF3 XCTLs to `LIT-MENUPGM` (menu program)

---

## 3. Program-to-Program Calls

### 3.1 Online XCTL Transfers (Control Transfer, No Return)

| From Program | To Program | Mechanism | COMMAREA Passed | Source |
|---|---|---|---|---|
| `COSGN00C` | `COADM01C` | XCTL | `CARDDEMO-COMMAREA` | `COSGN00C.cbl:231-234` |
| `COSGN00C` | `COMEN01C` | XCTL | `CARDDEMO-COMMAREA` | `COSGN00C.cbl:236-239` |
| `COMEN01C` | `COACTVWC` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COACTUPC` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COCRDLIC` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COCRDSLC` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COCRDUPC` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COTRN00C` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COTRN01C` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COTRN02C` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `CORPT00C` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COBIL00C` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:184-187` |
| `COMEN01C` | `COPAUS0C` | XCTL | `CARDDEMO-COMMAREA` | `COMEN01C.cbl:156-159` |
| `COADM01C` | `COUSR00C` | XCTL | `CARDDEMO-COMMAREA` | `COADM01C.cbl:145-148` |
| `COADM01C` | `COUSR01C` | XCTL | `CARDDEMO-COMMAREA` | `COADM01C.cbl:145-148` |
| `COADM01C` | `COUSR02C` | XCTL | `CARDDEMO-COMMAREA` | `COADM01C.cbl:145-148` |
| `COADM01C` | `COUSR03C` | XCTL | `CARDDEMO-COMMAREA` | `COADM01C.cbl:145-148` |
| `COADM01C` | `COTRTLIC` | XCTL | `CARDDEMO-COMMAREA` | `COADM01C.cbl:145-148` |
| `COADM01C` | `COTRTUPC` | XCTL | `CARDDEMO-COMMAREA` | `COADM01C.cbl:145-148` |
| `COACTVWC` | `CDEMO-TO-PROGRAM` | XCTL | `CARDDEMO-COMMAREA` | `COACTVWC.cbl:349-352` |
| `COACTUPC` | `CDEMO-TO-PROGRAM` | XCTL | `CARDDEMO-COMMAREA` | `COACTUPC.cbl:956-959` |
| `COCRDLIC` | `CCARD-NEXT-PROG` | XCTL | `CARDDEMO-COMMAREA` | `COCRDLIC.cbl:538-541` |
| `COCRDSLC` | `CDEMO-TO-PROGRAM` | XCTL | `CARDDEMO-COMMAREA` | `COCRDSLC.cbl:331-334` |
| `COCRDUPC` | `CDEMO-TO-PROGRAM` | XCTL | `CARDDEMO-COMMAREA` | `COCRDUPC.cbl:473-476` |

### 3.2 Online CALL (Subroutine Call with Return)

| Calling Program | Called Program | Data Passed (USING clause) | Source |
|---|---|---|---|
| `COTRN02C` | `CSUTLDTC` | `CSUTLDTC-DATE`, `CSUTLDTC-DATE-FORMAT`, `CSUTLDTC-RESULT` | `COTRN02C.cbl:393-395` |
| `COTRN02C` | `CSUTLDTC` | `CSUTLDTC-DATE`, `CSUTLDTC-DATE-FORMAT`, `CSUTLDTC-RESULT` | `COTRN02C.cbl:413-415` |
| `CORPT00C` | `CSUTLDTC` | `CSUTLDTC-DATE`, `CSUTLDTC-DATE-FORMAT`, `CSUTLDTC-RESULT` | `CORPT00C.cbl:392-394` |
| `CORPT00C` | `CSUTLDTC` | `CSUTLDTC-DATE`, `CSUTLDTC-DATE-FORMAT`, `CSUTLDTC-RESULT` | `CORPT00C.cbl:412-414` |

`CSUTLDTC` is a shared date validation utility. It calls the LE runtime `CEEDAYS` to validate dates.
Source: `CSUTLDTC.cbl` line 116: `CALL "CEEDAYS" USING WS-DATE-TO-TEST, WS-DATE-FORMAT, ...`

### 3.3 Batch CALL (Subroutine Call with Return)

| Calling Program | Called Program | Data Passed (USING clause) | Source |
|---|---|---|---|
| `CBSTM03A` | `CBSTM03B` | `WS-M03B-AREA` (file operation request block) | `CBSTM03A.CBL:351` |

**WS-M03B-AREA structure** (source: `CBSTM03B.CBL` lines 100-112):
- `LK-M03B-DD` (PIC X(08)) - DD name identifying the file (TRNXFILE, XREFFILE, CUSTFILE, ACCTFILE)
- `LK-M03B-OPER` (PIC X(01)) - Operation: O=Open, C=Close, R=Read sequential, K=Read by key, W=Write, Z=Rewrite
- `LK-M03B-RC` (PIC X(02)) - Return code
- `LK-M03B-KEY` (PIC X(25)) - Key for keyed reads
- `LK-M03B-KEY-LN` (PIC S9(4)) - Key length
- `LK-M03B-FLDT` (PIC X(1000)) - Record data buffer

### 3.4 Batch Programs with No Inter-Program Calls

These batch programs are standalone (no CALL to other programs):

| Program | Source | Function |
|---|---|---|
| `CBTRN01C` | `app/cbl/CBTRN01C.cbl` | Daily transaction lookup (reads files directly) |
| `CBTRN02C` | `app/cbl/CBTRN02C.cbl` | Transaction posting (reads/writes files directly) |
| `CBTRN03C` | `app/cbl/CBTRN03C.cbl` | Transaction report generation (reads files directly) |
| `CBACT04C` | `app/cbl/CBACT04C.cbl` | Interest calculation (reads/writes files directly) |
| `CBACT01C` | `app/cbl/CBACT01C.cbl` | Account file read (standalone batch) |
| `CBACT02C` | `app/cbl/CBACT02C.cbl` | Card file read (standalone batch) |
| `CBACT03C` | `app/cbl/CBACT03C.cbl` | Cross-reference file read (standalone batch) |
| `CBCUS01C` | `app/cbl/CBCUS01C.cbl` | Customer file read (standalone batch) |

---

## 4. Data Passed Between Programs

### 4.1 CARDDEMO-COMMAREA (Online Programs)

All online CICS programs share a single communication area defined in `app/cpy/COCOM01Y.cpy`.

**Structure** (source: `COCOM01Y.cpy` lines 19-44):

```
01 CARDDEMO-COMMAREA.
   05 CDEMO-GENERAL-INFO.
      10 CDEMO-FROM-TRANID          PIC X(04)     -- Source transaction ID
      10 CDEMO-FROM-PROGRAM         PIC X(08)     -- Source program name
      10 CDEMO-TO-TRANID            PIC X(04)     -- Target transaction ID
      10 CDEMO-TO-PROGRAM           PIC X(08)     -- Target program name
      10 CDEMO-USER-ID              PIC X(08)     -- Authenticated user ID
      10 CDEMO-USER-TYPE            PIC X(01)     -- 'A'=Admin, 'U'=User
      10 CDEMO-PGM-CONTEXT          PIC 9(01)     -- 0=Fresh entry, 1=Re-entry
   05 CDEMO-CUSTOMER-INFO.
      10 CDEMO-CUST-ID              PIC 9(09)     -- Selected customer ID
      10 CDEMO-CUST-FNAME           PIC X(25)     -- Customer first name
      10 CDEMO-CUST-MNAME           PIC X(25)     -- Customer middle name
      10 CDEMO-CUST-LNAME           PIC X(25)     -- Customer last name
   05 CDEMO-ACCOUNT-INFO.
      10 CDEMO-ACCT-ID              PIC 9(11)     -- Selected account ID
      10 CDEMO-ACCT-STATUS          PIC X(01)     -- Account status
   05 CDEMO-CARD-INFO.
      10 CDEMO-CARD-NUM             PIC 9(16)     -- Selected card number
   05 CDEMO-MORE-INFO.
      10 CDEMO-LAST-MAP             PIC X(7)      -- Last BMS map sent
      10 CDEMO-LAST-MAPSET          PIC X(7)      -- Last BMS mapset used
```

**Key usage patterns**:
- `COSGN00C` populates `CDEMO-USER-ID`, `CDEMO-USER-TYPE` from USRSEC file
- Menu programs (`COMEN01C`, `COADM01C`) populate `CDEMO-FROM-TRANID`, `CDEMO-FROM-PROGRAM`, `CDEMO-PGM-CONTEXT`
- List programs (`COCRDLIC`) populate `CDEMO-CARD-NUM` for detail programs (`COCRDSLC`, `COCRDUPC`)
- All programs use `CDEMO-PGM-CONTEXT` to distinguish first entry (0) from re-entry (1)

### 4.2 Date Validation Parameters (CSUTLDTC)

Passed via `CALL 'CSUTLDTC' USING`:

| Parameter | Type | Direction | Description |
|---|---|---|---|
| `CSUTLDTC-DATE` | PIC X(10) | Input | Date string to validate |
| `CSUTLDTC-DATE-FORMAT` | PIC X(10) | Input | Format pattern |
| `CSUTLDTC-RESULT` | Group | Output | Severity code (`CSUTLDTC-RESULT-SEV-CD`) and message number (`CSUTLDTC-RESULT-MSG-NUM`) |

### 4.3 Batch File I/O Service (CBSTM03B)

Passed via `CALL 'CBSTM03B' USING WS-M03B-AREA`:

| Field | Direction | Description |
|---|---|---|
| `LK-M03B-DD` | Input | DD name: TRNXFILE, XREFFILE, CUSTFILE, ACCTFILE |
| `LK-M03B-OPER` | Input | Operation code: O/C/R/K/W/Z |
| `LK-M03B-RC` | Output | File status return code (2 bytes) |
| `LK-M03B-KEY` | Input | Record key for keyed reads |
| `LK-M03B-KEY-LN` | Input | Length of key |
| `LK-M03B-FLDT` | In/Out | Record data buffer (up to 1000 bytes) |

### 4.4 Batch Inter-File Data Passing

Batch programs pass data between steps via VSAM files and sequential datasets. Key data flows:

| Source Program | Target Program | Data Medium | Dataset Name |
|---|---|---|---|
| CICS online transactions | `CBTRN02C` | Sequential file | `AWS.M2.CARDDEMO.DALYTRAN.PS` |
| `CBTRN02C` | `CBTRN03C` | VSAM KSDS | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` |
| `CBTRN02C` | `CBTRN02C` (rejects) | GDG sequential | `AWS.M2.CARDDEMO.DALYREJS(+1)` |
| `CBTRN02C` | `CBACT04C` | VSAM KSDS | `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` |
| `CBACT04C` | `CBTRN02C` (next cycle) | Sequential | `AWS.M2.CARDDEMO.SYSTRAN(+1)` |
| SORT step (TRANREPT) | `CBTRN03C` | GDG sequential | `AWS.M2.CARDDEMO.TRANSACT.DALY(+1)` |
| `CBTRN03C` | Print queue | GDG sequential | `AWS.M2.CARDDEMO.TRANREPT(+1)` |
| SORT step (CREASTMT) | `CBSTM03A` | VSAM KSDS | `AWS.M2.CARDDEMO.TRXFL.VSAM.KSDS` |
| `CBSTM03A` | Output | Sequential PS | `AWS.M2.CARDDEMO.STATEMNT.PS` |
| `CBSTM03A` | Output | Sequential PS | `AWS.M2.CARDDEMO.STATEMNT.HTML` |

### 4.5 JCL PARM Data

| JCL | Program | PARM Value | Purpose | Source |
|---|---|---|---|---|
| `INTCALC.jcl` | `CBACT04C` | `'2022071800'` | Processing date (YYYYMMDDNN) | `INTCALC.jcl` line 22 |

`CBACT04C` receives PARM via LINKAGE SECTION (`CBACT04C.cbl` lines 175-178):
```
01  EXTERNAL-PARMS.
    05  PARM-LENGTH    PIC S9(04) COMP.
    05  PARM-DATE      PIC X(10).
```

---

## 5. Batch Job Execution Flows

### 5.1 Daily Transaction Processing (POSTTRAN.jcl)

```
POSTTRAN.jcl
    |
    STEP15: EXEC PGM=CBTRN02C
        |
        Input Files:
        |  DALYTRAN  <-- AWS.M2.CARDDEMO.DALYTRAN.PS (daily transactions, sequential)
        |  XREFFILE  <-- AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS (card-to-account xref)
        |  ACCTFILE  <-- AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS (account master, I-O)
        |  TCATBALF  <-- AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS (category balances, I-O)
        |
        Processing (CBTRN02C.cbl lines 202-219):
        |  1. Read each daily transaction record
        |  2. Validate: lookup card in XREFFILE (1500-A-LOOKUP-XREF)
        |  3. Validate: lookup account in ACCTFILE (1500-B-LOOKUP-ACCT)
        |  4. If valid: write to TRANFILE and update ACCTFILE/TCATBALF (2000-POST-TRANSACTION)
        |  5. If invalid: write to DALYREJS with failure reason (2500-WRITE-REJECT-REC)
        |
        Output Files:
           TRANFILE  --> AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS (posted transactions)
           DALYREJS  --> AWS.M2.CARDDEMO.DALYREJS(+1) (rejected transactions, GDG)
```

### 5.2 Transaction Report Generation (TRANREPT.jcl)

```
TRANREPT.jcl
    |
    STEP05R: EXEC PROC=REPROC
    |   Unloads TRANSACT.VSAM.KSDS to TRANSACT.BKUP(+1) (GDG backup)
    |
    STEP05R: EXEC PGM=SORT
    |   Input:  TRANSACT.BKUP(+1)
    |   Filter: TRAN-PROC-DT between PARM-START-DATE and PARM-END-DATE
    |   Sort:   by TRAN-CARD-NUM ascending
    |   Output: TRANSACT.DALY(+1) (filtered/sorted transactions)
    |   Source: TRANREPT.jcl lines 37-55
    |
    STEP10R: EXEC PGM=CBTRN03C
        |
        Input Files:
        |  TRANFILE  <-- TRANSACT.DALY(+1) (filtered transactions)
        |  CARDXREF  <-- CARDXREF.VSAM.KSDS (card xref for account lookup)
        |  TRANTYPE  <-- TRANTYPE.VSAM.KSDS (transaction type descriptions)
        |  TRANCATG  <-- TRANCATG.VSAM.KSDS (transaction category descriptions)
        |  DATEPARM  <-- DATEPARM (date range parameters)
        |
        Processing (CBTRN03C.cbl lines 170-206):
        |  1. Read date parameters from DATEPARM file
        |  2. For each transaction in date range:
        |     a. Lookup card in CARDXREF for account ID
        |     b. Lookup transaction type description
        |     c. Lookup transaction category description
        |     d. Write formatted report line
        |  3. Accumulate page/account/grand totals
        |
        Output:
           TRANREPT  --> TRANREPT(+1) (formatted report, GDG, LRECL=133)
```

### 5.3 Interest Calculation (INTCALC.jcl)

```
INTCALC.jcl
    |
    STEP15: EXEC PGM=CBACT04C,PARM='2022071800'
        |
        Input Files:
        |  TCATBALF  <-- TCATBALF.VSAM.KSDS (category balances, sequential read)
        |  XREFFILE  <-- CARDXREF.VSAM.KSDS (card xref)
        |  XREFFIL1  <-- CARDXREF.VSAM.AIX.PATH (alternate index by account ID)
        |  ACCTFILE  <-- ACCTDATA.VSAM.KSDS (account master, I-O for update)
        |  DISCGRP   <-- DISCGRP.VSAM.KSDS (discount/interest rate groups)
        |
        Processing (CBACT04C.cbl lines 188-222):
        |  1. Read each TCATBALF record sequentially
        |  2. When account changes:
        |     a. Update previous account balances (1050-UPDATE-ACCOUNT)
        |     b. Read new account data (1100-GET-ACCT-DATA)
        |     c. Read xref data via AIX (1110-GET-XREF-DATA)
        |  3. Lookup interest rate from DISCGRP (1200-GET-INTEREST-RATE)
        |  4. Compute interest (1300-COMPUTE-INTEREST)
        |  5. Compute fees (1400-COMPUTE-FEES)
        |  6. Write interest/fee transaction records
        |
        Output:
           TRANSACT  --> SYSTRAN(+1) (generated interest/fee transactions, GDG)
           ACCTFILE  --> ACCTDATA.VSAM.KSDS (updated balances via REWRITE)
```

### 5.4 Statement Creation (CREASTMT.JCL)

```
CREASTMT.JCL
    |
    DELDEF01: EXEC PGM=IDCAMS
    |   Delete and redefine TRXFL.VSAM.KSDS (temp transaction file keyed by card+tran)
    |   Source: CREASTMT.JCL lines 22-40
    |
    STEP010: EXEC PGM=SORT
    |   Input:  TRANSACT.VSAM.KSDS
    |   Sort:   by card number (pos 263,16) then tran ID (pos 1,16)
    |   Reformat: rearrange fields with OUTREC
    |   Output: TRXFL.SEQ (sequential)
    |   Source: CREASTMT.JCL lines 44-55
    |
    STEP020: EXEC PGM=IDCAMS
    |   REPRO TRXFL.SEQ into TRXFL.VSAM.KSDS
    |   Source: CREASTMT.JCL lines 56-62
    |
    STEP030: EXEC PGM=IEFBR14
    |   Delete previous statement output files
    |   Source: CREASTMT.JCL lines 66-75
    |
    STEP040: EXEC PGM=CBSTM03A
        |
        Input Files (via CBSTM03B subroutine):
        |  TRNXFILE  <-- TRXFL.VSAM.KSDS (sorted transactions)
        |  XREFFILE  <-- CARDXREF.VSAM.KSDS (card xref)
        |  ACCTFILE  <-- ACCTDATA.VSAM.KSDS (account data)
        |  CUSTFILE  <-- CUSTDATA.VSAM.KSDS (customer data)
        |
        Processing (CBSTM03A.CBL lines 310-329):
        |  1. Read XREFFILE sequentially (each card)
        |  2. For each card: read customer (CUSTFILE) and account (ACCTFILE) by key
        |  3. Read all transactions for the card from TRNXFILE
        |  4. Generate statement in text and HTML formats
        |
        Subroutine Calls:
        |  CBSTM03A --> CALL 'CBSTM03B' USING WS-M03B-AREA
        |  (all file I/O delegated to CBSTM03B)
        |  Source: CBSTM03A.CBL line 351 (and lines 377, 401, 734, 746, etc.)
        |
        Output:
           STMTFILE  --> STATEMNT.PS (text statements)
           HTMLFILE  --> STATEMNT.HTML (HTML statements)
```

---

## 6. Batch Job Sequencing (Scheduler)

### 6.1 Daily Chain (CA7 Scheduler)

Source: `app/scheduler/CardDemo.ca7` lines 18-149

```
CLOSEFIL (Close CICS files for batch)
    |
    | Triggers: JOB=CBPAUP0J (line 43)
    v
CBPAUP0J (Pending Authorization Update)
    |
    | Triggers: JOB=POSTTRAN (line 70)
    v
POSTTRAN (Post Daily Transactions - CBTRN02C)
    |
    | Triggers: JOB=WAITSTEP (line 97)
    v
WAITSTEP (Wait for processing to complete)
    |
    | Triggers: JOB=OPENFIL (line 124)
    v
OPENFIL (Re-open CICS files)
```

### 6.2 Weekly Reference Data Chain (CA7 Scheduler)

Source: `app/scheduler/CardDemo.ca7` lines 159-217

```
CLOSEFIL (Close CICS files)
    |
    | Triggers: JOB=TRANTYPE (line 162)
    v
TRANTYPE (Load transaction types)
    |
    | Triggers: JOB=WAITSTEP (line 189)
    | Triggers: JOB=CLOSEFIL1, CLOSEFIL2 (lines 216-217)
    v
WAITSTEP --> CLOSEFIL1 --> TRANCATG (Load transaction categories)
          --> CLOSEFIL2 --> ...
```

### 6.3 Daily Chain (Control-M Scheduler)

Source: `app/scheduler/CardDemo.controlm` lines 3-25

Folder: `DAILY-TransactionBackup`

```
CLOSEFIL
    |
    | OUTCOND: DAILY-TransactionBackup-CLOSEFIL (+)
    v
TRANBKP
    | INCOND: DAILY-TransactionBackup-CLOSEFIL
    | OUTCOND: DAILY-TransactionBackup-TRANBKP (+)
    v
WAITSTEP
    | INCOND: DAILY-TransactionBackup-TRANBKP
    | OUTCOND: DAILY-TransactionBackup-WAITSTEP (+)
    v
OPENFIL
    | INCOND: DAILY-TransactionBackup-WAITSTEP
```

### 6.4 Weekly Disclosure Groups Refresh (Control-M Scheduler)

Source: `app/scheduler/CardDemo.controlm` lines 32-56

Folder: `WEEKLY-DisclosureGroupsRefresh` (runs Saturdays)

```
MNTTRDB2 (Maintain Transaction Types in DB2)
    |
    | Precondition for:
    v
CLOSEFIL
    |
    | OUTCOND: WEEKLY-DisclosureGroupsRefresh-CLOSEFIL (+)
    v
DISCGRP (Load Disclosure Groups)
    |
    | OUTCOND: WEEKLY-DisclosureGroupsRefresh-DISCGRP (+)
    v
WAITSTEP
    |
    v
OPENFIL
```

---

## 7. Online-to-Batch Bridge

### 7.1 Report Submission via CICS TDQ

The online program `CORPT00C` bridges online and batch by writing JCL to the CICS Transient Data Queue (TDQ) for the internal reader.

Source: `CORPT00C.cbl` lines 462-535

**Flow**:
```
CORPT00C (Transaction Reports screen)
    |
    | User selects report type (Monthly/Yearly/Custom)
    | User confirms with 'Y'
    |
    | PERFORM SUBMIT-JOB-TO-INTRDR (line 435)
    |   |
    |   | Writes JCL lines from JOB-LINES array to TDQ 'JOBS'
    |   | EXEC CICS WRITEQ TD QUEUE('JOBS') FROM(JCL-RECORD) (line 517-523)
    |   |
    |   v
    | TDQ 'JOBS' (mapped to Internal Reader)
    |   |
    |   v
    | JES Internal Reader submits batch JCL
    |   |
    |   v
    | Batch report job executes (CBTRN03C)
```

**Data passed**:
- `PARM-START-DATE-1`, `PARM-START-DATE-2` - Report start date from screen input
- `PARM-END-DATE-1`, `PARM-END-DATE-2` - Report end date from screen input
- Source: `CORPT00C.cbl` lines 429-432

### 7.2 Internal Reader JCL Chaining

Source: `app/jcl/INTRDRJ1.JCL`

```
INTRDRJ1.JCL
    |
    IDCAMS step: REPRO backup of FTP test data (line 6-12)
    |
    STEP01: EXEC PGM=IEBGENER
        |
        | SYSUT1: reads INTRDRJ2 JCL member from PDS
        | SYSUT2: writes to INTRDR (Internal Reader)
        | Source: INTRDRJ1.JCL lines 14-18
        |
        v
    INTRDRJ2.JCL is submitted to JES for execution
```

---

## 8. Complete Data Flow Diagram

### 8.1 Online Data Flow

```
Terminal User
    |
    | 3270 Screen I/O (BMS Maps)
    v
COSGN00C --reads--> USRSEC (VSAM KSDS, user credentials)
    |
    | COMMAREA: user-id, user-type, from-program
    v
COMEN01C/COADM01C (Menu)
    |
    | COMMAREA: from-tranid, from-program, pgm-context
    v
+---> COACTVWC --reads--> ACCTDATA, CUSTDATA, CARDXREF
+---> COACTUPC --reads/writes--> ACCTDATA
+---> COCRDLIC --reads--> CARDDATA, CARDXREF, CUSTDATA
+---> COCRDSLC --reads--> CARDDATA, CARDXREF, ACCTDATA
+---> COCRDUPC --reads/writes--> CARDDATA
+---> COTRN00C --reads--> TRANSACT, CARDXREF
+---> COTRN01C --reads--> TRANSACT, CARDXREF
+---> COTRN02C --reads/writes--> TRANSACT --calls--> CSUTLDTC (date validation)
+---> CORPT00C --writes--> TDQ 'JOBS' (submits batch) --calls--> CSUTLDTC
+---> COBIL00C --reads/writes--> TRANSACT, ACCTDATA
+---> COUSR00C --reads--> USRSEC
+---> COUSR01C --writes--> USRSEC
+---> COUSR02C --reads/writes--> USRSEC
+---> COUSR03C --reads/writes--> USRSEC
```

### 8.2 Nightly Batch Data Flow

```
Scheduler (CA7/Control-M)
    |
    v
CLOSEFIL.jcl (CICS CEMT CLOSE files)
    |
    v
POSTTRAN.jcl --> CBTRN02C
    | Reads: DALYTRAN.PS, CARDXREF, ACCTDATA, TCATBALF
    | Writes: TRANSACT.VSAM.KSDS, DALYREJS(+1)
    | Updates: ACCTDATA (account balances), TCATBALF (category balances)
    |
    v
INTCALC.jcl --> CBACT04C
    | Reads: TCATBALF, CARDXREF, CARDXREF.AIX, DISCGRP
    | Writes: SYSTRAN(+1) (interest/fee transactions)
    | Updates: ACCTDATA (add interest to balance)
    |
    v
TRANREPT.jcl --> SORT --> CBTRN03C
    | Reads: TRANSACT (backup+sort), CARDXREF, TRANTYPE, TRANCATG, DATEPARM
    | Writes: TRANREPT(+1) (formatted report)
    |
    v
CREASTMT.JCL --> SORT --> IDCAMS --> CBSTM03A (calls CBSTM03B)
    | Reads: TRANSACT, CARDXREF, ACCTDATA, CUSTDATA
    | Writes: STATEMNT.PS, STATEMNT.HTML
    |
    v
WAITSTEP.jcl --> COBSWAIT (delay)
    |
    v
OPENFIL.jcl (CICS CEMT OPEN files)
```

### 8.3 Key VSAM Files as Integration Points

| VSAM File | Written By (Batch) | Written By (Online) | Read By (Batch) | Read By (Online) |
|---|---|---|---|---|
| ACCTDATA.VSAM.KSDS | CBTRN02C, CBACT04C | COACTUPC | CBTRN01C, CBTRN02C, CBACT04C, CBSTM03A | COACTVWC, COACTUPC, COCRDSLC |
| TRANSACT.VSAM.KSDS | CBTRN02C | COTRN02C, COBIL00C | CBTRN03C, CBSTM03A | COTRN00C, COTRN01C |
| CARDXREF.VSAM.KSDS | -- | -- | CBTRN01C, CBTRN02C, CBTRN03C, CBACT04C, CBSTM03A | COCRDLIC, COCRDSLC, COTRN00C, COTRN01C |
| CUSTDATA.VSAM.KSDS | -- | -- | CBSTM03A, CBCUS01C | COACTVWC, COCRDLIC |
| CARDDATA.VSAM.KSDS | -- | COCRDUPC | CBTRN01C | COCRDLIC, COCRDSLC, COCRDUPC |
| USRSEC | -- | COUSR01C, COUSR02C, COUSR03C | -- | COSGN00C, COUSR00C |
| TCATBALF.VSAM.KSDS | CBTRN02C | -- | CBACT04C | -- |
| DISCGRP.VSAM.KSDS | -- | -- | CBACT04C | -- |

---

*End of document. All program references, call chains, and data flows traced to source code artifacts in the repository.*
