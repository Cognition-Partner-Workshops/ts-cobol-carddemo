# Data Dependencies Map

This document maps all VSAM files, flat files, and DB2 tables used by the CardDemo batch processing system, along with their relationships to batch jobs.

> **Source files:**
> - Dataset definitions: [`README.md`](../../README.md) (lines 131-144)
> - JCL members: [`app/jcl/`](../../app/jcl/)
> - Copybooks: [`app/cpy/`](../../app/cpy/)

---

## Dataset Inventory

### Flat Files (Sequential / PS)

| Dataset Name | Description | Copybook | Format | Length | Read By | Written By |
|:-------------|:------------|:---------|:-------|-------:|:--------|:-----------|
| AWS.M2.CARDDEMO.USRSEC.PS | User Security file | CSUSR01Y | FB | 80 | DUSRSECJ | (external load) |
| AWS.M2.CARDDEMO.ACCTDATA.PS | Account Data | CVACT01Y | FB | 300 | ACCTFILE | (external load) |
| AWS.M2.CARDDEMO.CARDDATA.PS | Card Data | CVACT02Y | FB | 150 | CARDFILE | (external load) |
| AWS.M2.CARDDEMO.CUSTDATA.PS | Customer Data | CVCUS01Y | FB | 500 | CUSTFILE | (external load) |
| AWS.M2.CARDDEMO.CARDXREF.PS | Customer Account Card Cross Reference | CVACT03Y | FB | 50 | XREFFILE | (external load) |
| AWS.M2.CARDDEMO.DALYTRAN.PS.INIT | Transaction database initialization record | CVTRA06Y | FB | 350 | TRANFILE | (external load) |
| AWS.M2.CARDDEMO.DALYTRAN.PS | Transaction data for posting | CVTRA06Y | FB | 350 | POSTTRAN | Online CICS transactions |
| AWS.M2.CARDDEMO.DISCGRP.PS | Disclosure Groups | CVTRA02Y | FB | 50 | DISCGRP | TRANEXTR (from DB2) |
| AWS.M2.CARDDEMO.TRANCATG.PS | Transaction Category Types | CVTRA04Y | FB | 60 | TRANCATG | TRANEXTR (from DB2) |
| AWS.M2.CARDDEMO.TRANTYPE.PS | Transaction Types | CVTRA03Y | FB | 60 | TRANTYPE | TRANEXTR (from DB2) |
| AWS.M2.CARDDEMO.TCATBALF.PS | Transaction Category Balance | CVTRA01Y | FB | 50 | TCATBALF | (external load) |

### VSAM Files (KSDS)

| Dataset Name | Description | Key Length | Key Offset | Record Size | Used By Jobs |
|:-------------|:------------|:-----------|:-----------|:------------|:-------------|
| AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | Online transaction master | 16 | 0 | 350 | TRANBKP (read/delete), POSTTRAN (write), COMBTRAN (write), INTCALC (indirect via TCATBALF), CLOSEFIL/OPENFIL (close/open in CICS) |
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | Account master | - | - | 300 | ACCTFILE (load), POSTTRAN (read), INTCALC (read), CBEXPORT (read) |
| AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS | Card data master | - | - | 150 | CARDFILE (load), CBEXPORT (read) |
| AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS | Customer master | - | - | 500 | CUSTFILE (load), CBEXPORT (read) |
| AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS | Card-Account-Customer cross reference | - | - | 50 | XREFFILE (load), POSTTRAN (read), INTCALC (read), CBEXPORT (read) |
| AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH | Alternate index path for cross reference | - | - | - | INTCALC (read via XREFFIL1) |
| AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS | Disclosure groups | 16 | 0 | 50 | DISCGRP (load), INTCALC (read) |
| AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS | Transaction category types | 6 | 0 | 60 | TRANCATG (load) |
| AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS | Transaction types | 2 | 0 | 60 | TRANTYPE (load) |
| AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS | Transaction category balance | - | - | 50 | TCATBALF (load), POSTTRAN (read/write), INTCALC (read) |
| AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS | User security | - | - | 80 | DUSRSECJ (load) |
| AWS.M2.CARDDEMO.EXPORT.DATA | Export data cluster | 4 | 28 | 500 | CBEXPORT (write), CBIMPORT (read) |

### GDG (Generation Data Group) Files

| Dataset Name | Description | Used By Jobs |
|:-------------|:------------|:-------------|
| AWS.M2.CARDDEMO.TRANSACT.BKUP(+1) | Transaction master backup (new generation) | TRANBKP (write) |
| AWS.M2.CARDDEMO.TRANSACT.BKUP(0) | Transaction master backup (current generation) | COMBTRAN (read) |
| AWS.M2.CARDDEMO.SYSTRAN(+1) | System-generated transactions (new generation) | INTCALC (write) |
| AWS.M2.CARDDEMO.SYSTRAN(0) | System-generated transactions (current generation) | COMBTRAN (read) |
| AWS.M2.CARDDEMO.TRANSACT.COMBINED(+1) | Combined transaction file (new generation) | COMBTRAN (write then read) |
| AWS.M2.CARDDEMO.DALYREJS(+1) | Daily rejected transactions (new generation) | POSTTRAN (write) |

### DB2 Tables (Optional Module)

| Table | Description | Used By Jobs |
|:------|:------------|:-------------|
| Transaction Types table | Transaction type reference data | CREADB21 (create/load), MNTTRDB2 (update), TRANEXTR (read/extract) |
| Transaction Categories table | Transaction category reference data | CREADB21 (create/load), TRANEXTR (read/extract) |

---

## Data Flow by Job

### POSTTRAN (Daily Transaction Processing)

```
Inputs:                              Outputs:
  AWS.M2.CARDDEMO.DALYTRAN.PS ───┐
  AWS.M2.CARDDEMO.TRANSACT.VSAM ─┤   ┌──▶ AWS.M2.CARDDEMO.TRANSACT.VSAM (updated)
  AWS.M2.CARDDEMO.CARDXREF.VSAM ─┼──▶├──▶ AWS.M2.CARDDEMO.TCATBALF.VSAM (updated)
  AWS.M2.CARDDEMO.ACCTDATA.VSAM ─┤   └──▶ AWS.M2.CARDDEMO.DALYREJS(+1)
  AWS.M2.CARDDEMO.TCATBALF.VSAM ─┘
```

### TRANBKP (Transaction Backup)

```
Inputs:                                    Outputs:
  AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS ──▶ AWS.M2.CARDDEMO.TRANSACT.BKUP(+1)
                                          AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS (deleted & redefined)
```

### INTCALC (Interest Calculation)

```
Inputs:                                    Outputs:
  AWS.M2.CARDDEMO.TCATBALF.VSAM ─────┐
  AWS.M2.CARDDEMO.CARDXREF.VSAM ─────┤
  AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX ─┼──▶ AWS.M2.CARDDEMO.SYSTRAN(+1)
  AWS.M2.CARDDEMO.ACCTDATA.VSAM ─────┤
  AWS.M2.CARDDEMO.DISCGRP.VSAM ──────┘
```

### COMBTRAN (Combine Transactions)

```
Inputs:                                    Outputs:
  AWS.M2.CARDDEMO.TRANSACT.BKUP(0) ──┐
  AWS.M2.CARDDEMO.SYSTRAN(0) ────────┼──▶ AWS.M2.CARDDEMO.TRANSACT.COMBINED(+1)
                                      └──▶ AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS (loaded)
```

### CBEXPORT (Data Export)

```
Inputs:                                    Outputs:
  AWS.M2.CARDDEMO.CUSTDATA.VSAM ─────┐
  AWS.M2.CARDDEMO.ACCTDATA.VSAM ─────┤
  AWS.M2.CARDDEMO.CARDXREF.VSAM ─────┼──▶ AWS.M2.CARDDEMO.EXPORT.DATA
  AWS.M2.CARDDEMO.TRANSACT.VSAM ─────┤
  AWS.M2.CARDDEMO.CARDDATA.VSAM ─────┘
```

### CBIMPORT (Data Import)

```
Inputs:                                    Outputs:
                                      ┌──▶ AWS.M2.CARDDEMO.CUSTDATA.IMPORT
  AWS.M2.CARDDEMO.EXPORT.DATA ───────┼──▶ AWS.M2.CARDDEMO.ACCTDATA.IMPORT
                                      ├──▶ AWS.M2.CARDDEMO.CARDXREF.IMPORT
                                      ├──▶ AWS.M2.CARDDEMO.TRANSACT.IMPORT
                                      └──▶ AWS.M2.CARDDEMO.IMPORT.ERRORS
```

---

## CICS File Definitions (Closed/Opened by CLOSEFIL/OPENFIL)

The following CICS file definitions are managed by the CLOSEFIL and OPENFIL jobs, which issue `CEMT SET FILE ... CLO/OPE` commands:

| CICS File Name | Underlying VSAM Dataset | Purpose |
|:---------------|:------------------------|:--------|
| TRANSACT | AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS | Online transaction data |
| CCXREF | AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS | Card cross-reference |
| ACCTDAT | AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | Account data |
| CXACAIX | AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH | Card cross-reference alternate index |
| USRSEC | AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS | User security |

> **Migration Note:** These files must be closed in CICS before batch jobs can safely update them, and re-opened after batch completes. This pattern will need to be replaced with a distributed locking or event-driven mechanism during migration.
