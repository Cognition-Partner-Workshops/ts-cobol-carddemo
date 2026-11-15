# Reconciliation reports - ticket 10-data-move-kit

Revision of the code: `8a58bb9c54b8c66838c3a560dd8dfd4f3aadd8dc` (branch `demo/05-10-data-move-kit`). Produced 2026-11-15T13:02:59Z.

## `make recon-java` (the seven cases, each loaded and calculated first)

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [recon]: EQUAL (RC1 EQUAL, RC2 EQUAL, RC3 EQUAL, RC4 EQUAL, RC5 EQUAL; RC6 NOT AVAILABLE: needs a mainframe unload)
interest/i02-tie-half-cent [recon]: EQUAL (RC1 EQUAL, RC2 EQUAL, RC3 EQUAL, RC4 EQUAL, RC5 EQUAL; RC6 NOT AVAILABLE: needs a mainframe unload)
interest/i03-zero-rate-skipped [recon]: EQUAL (RC1 EQUAL, RC2 EQUAL, RC3 EQUAL, RC4 EQUAL, RC5 EQUAL; RC6 NOT AVAILABLE: needs a mainframe unload)
interest/i04-default-group-fallback [recon]: EQUAL (RC1 EQUAL, RC2 EQUAL, RC3 EQUAL, RC4 EQUAL, RC5 EQUAL; RC6 NOT AVAILABLE: needs a mainframe unload)
interest/i05-multi-category [recon]: EQUAL (RC1 EQUAL, RC2 EQUAL, RC3 EQUAL, RC4 EQUAL, RC5 EQUAL; RC6 NOT AVAILABLE: needs a mainframe unload)
interest/i06-negative-balance [recon]: EQUAL (RC1 EQUAL, RC2 EQUAL, RC3 EQUAL, RC4 EQUAL, RC5 EQUAL; RC6 NOT AVAILABLE: needs a mainframe unload)
interest/i07-nontie-rounding [recon]: EQUAL (RC1 EQUAL, RC2 EQUAL, RC3 EQUAL, RC4 EQUAL, RC5 EQUAL; RC6 NOT AVAILABLE: needs a mainframe unload)
report: work/recon-java/report.md
exit code 0
```

## Error path: `make recon-java CASE=i01-baseline RECON_LOAD=no RECON_OUT=work/recon-java-err`

The tables hold the last case that ran (i07-nontie-rounding); the report is asked for the files of i01-baseline and for an output directory that does not exist. Lines are DIFFERENT or NOT AVAILABLE, none of the affected ones is EQUAL, and the target ends with a non-zero exit code.

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [recon]: DIFFERENT (RC1 EQUAL, RC2 DIFFERENT, RC3 DIFFERENT, RC4 DIFFERENT, RC5 NOT AVAILABLE; RC6 NOT AVAILABLE: needs a mainframe unload)
report: work/recon-java-err/report.md
make: *** [Makefile:240: recon-java] Error 1
exit code 2
```

| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |
|---|---|---|---|---|
| RC1 | **EQUAL** | 8 | 0 | 0 |
| RC2 | **DIFFERENT** | 3 | 14 | 0 |
| RC3 | **DIFFERENT** | 1 | 3 | 0 |
| RC4 | **DIFFERENT** | 6 | 6 | 0 |
| RC5 | **NOT AVAILABLE** | 4 | 0 | 3 |
| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | 1 |

Lines of that report that are not EQUAL:

| Check | Figure | First side | Value | Second side | Value | Result | Note |
|---|---|---|---|---|---|---|---|
| RC2 | **DIFFERENT** | 3 | 14 | 0 |
| RC3 | **DIFFERENT** | 1 | 3 | 0 |
| RC4 | **DIFFERENT** | 6 | 6 | 0 |
| RC5 | **NOT AVAILABLE** | 4 | 0 | 3 |
| RC2 | hash total of ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 4003 | interest_run.loaded_acct_id_sum | 4019.00 | **DIFFERENT** |  |
| RC2 | hash total of ACCT-ID, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 4003 | carddemo.account | 4019 | **DIFFERENT** |  |
| RC2 | sum of ACCT-CURR-BAL, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 3400.00 | interest_run.loaded_acct_curr_bal_sum | 1778.10 | **DIFFERENT** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 11000.00 | interest_run.loaded_acct_credit_limit_sum | 10000.00 | **DIFFERENT** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 11000.00 | carddemo.account | 10000.00 | **DIFFERENT** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5500.00 | interest_run.loaded_acct_cash_credit_limit_sum | 5000.00 | **DIFFERENT** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5500.00 | carddemo.account | 5000.00 | **DIFFERENT** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 3400.00 | interest_run.loaded_acct_curr_cyc_credit_sum | 1778.10 | **DIFFERENT** |  |
| RC2 | hash total of XREF-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 4003 | interest_run.loaded_xref_acct_id_sum | 4019.00 | **DIFFERENT** |  |
| RC2 | hash total of XREF-ACCT-ID, in the table now | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 4003 | carddemo.card_xref | 4019 | **DIFFERENT** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4003 | interest_run.loaded_trancat_acct_id_sum | 4019.00 | **DIFFERENT** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4003 | carddemo.tran_cat_balance | 4019 | **DIFFERENT** |  |
| RC2 | sum of TRAN-CAT-BAL, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 3400.00 | interest_run.loaded_tran_cat_bal_sum | 1778.10 | **DIFFERENT** |  |
| RC2 | sum of TRAN-CAT-BAL, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 3400.00 | carddemo.tran_cat_balance | 1778.10 | **DIFFERENT** |  |
| RC3 | rows of account that encode to their source record | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2 | carddemo.account | 0 | **DIFFERENT** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 4 (rows that differ 0, rows without a record 2, records without a row 2); ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT left out: written by the calculation (INT-R07), see RC5 |
| RC3 | rows of card_xref that encode to their source record | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2 | carddemo.card_xref | 0 | **DIFFERENT** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 4 (rows that differ 0, rows without a record 2, records without a row 2) |
| RC3 | rows of tran_cat_balance that encode to their source record | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 0 | **DIFFERENT** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 4 (rows that differ 0, rows without a record 2, records without a row 2) |
| RC4 | lowest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, first record | 2001 | carddemo.account, lowest key | 2009 | **DIFFERENT** | table order: collation C (E6) |
| RC4 | highest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, last record | 2002 | carddemo.account, highest key | 2010 | **DIFFERENT** | table order: collation C (E6) |
| RC4 | lowest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, first record | 4000000000002001 | carddemo.card_xref, lowest key | 4000000000002009 | **DIFFERENT** | table order: collation C (E6) |
| RC4 | highest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, last record | 4000000000002002 | carddemo.card_xref, highest key | 4000000000002010 | **DIFFERENT** | table order: collation C (E6) |
| RC4 | lowest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, first record | 2001/01/1 | carddemo.tran_cat_balance, lowest key | 2009/01/1 | **DIFFERENT** | table order: collation C (E6) |
| RC4 | highest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, last record | 2002/01/1 | carddemo.tran_cat_balance, highest key | 2010/01/1 | **DIFFERENT** | table order: collation C (E6) |
| RC5 | transactions written | carddemo.interest_transaction, rows of the run | 2 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00, records | NOT AVAILABLE (no exported file datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt) | **NOT AVAILABLE** |  |
| RC5 | sum of TRAN-AMT | carddemo.interest_transaction | 29.03 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | NOT AVAILABLE (no exported file datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt) | **NOT AVAILABLE** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation | carddemo.account | 1793.52 | exported AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | NOT AVAILABLE (no exported file datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt) | **NOT AVAILABLE** |  |

# The seven reports (`work/recon-java/report.md`)

## Reconciliation report - i01-baseline: **EQUAL**

Unloaded files: `fixtures/interest/i01-baseline/input`. Exported files: `work/recon-java/run/i01-baseline/actual`. Run: `interest_run.run_id` 24, status EXPORTED, PARM 2022071800. The report reads the files, `carddemo.interest_run` and the tables in one read-only transaction; it writes nothing.

| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |
|---|---|---|---|---|
| RC1 | **EQUAL** | 8 | 0 | 0 |
| RC2 | **EQUAL** | 17 | 0 | 0 |
| RC3 | **EQUAL** | 4 | 0 | 0 |
| RC4 | **EQUAL** | 12 | 0 | 0 |
| RC5 | **EQUAL** | 7 | 0 | 0 |
| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | 1 |

### Figures (the columns of the reconciliation table of strategy-interest.md, from interest_run and the tables)

| Accounts | Cards | Balances | Rates | Sum ACCT-CURR-BAL before | Sum TRAN-CAT-BAL | Sum DIS-INT-RATE | Transactions written | Sum TRAN-AMT | Sum ACCT-CURR-BAL after | Balance change | Interest of last account (not added) | RC5 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2 | 2 | 2 | 15 | 3400.00 | 3400.00 | 120.50 | 2 | 47.00 | 3410.00 | 10.00 | 37.00 | holds |

### Lines

| Check | Figure | First side | Value | Second side | Value | Result | Note |
|---|---|---|---|---|---|---|---|
| RC1 | records and rows of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2 | carddemo.account | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of account | interest_run.loaded_account_count | 2 | carddemo.account | 2 | **EQUAL** |  |
| RC1 | records and rows of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2 | carddemo.card_xref | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of card_xref | interest_run.loaded_card_xref_count | 2 | carddemo.card_xref | 2 | **EQUAL** |  |
| RC1 | records and rows of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of tran_cat_balance | interest_run.loaded_tran_cat_balance_count | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | records and rows of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC1 | count recorded at the load of disclosure_group | interest_run.loaded_disclosure_group_count | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 4003 | interest_run.loaded_acct_id_sum | 4003.00 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 4003 | carddemo.account | 4003 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-BAL, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 3400.00 | interest_run.loaded_acct_curr_bal_sum | 3400.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 11000.00 | interest_run.loaded_acct_credit_limit_sum | 11000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 11000.00 | carddemo.account | 11000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5500.00 | interest_run.loaded_acct_cash_credit_limit_sum | 5500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5500.00 | carddemo.account | 5500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 3400.00 | interest_run.loaded_acct_curr_cyc_credit_sum | 3400.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-DEBIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 0.00 | interest_run.loaded_acct_curr_cyc_debit_sum | 0.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 4003 | interest_run.loaded_xref_acct_id_sum | 4003.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, in the table now | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 4003 | carddemo.card_xref | 4003 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4003 | interest_run.loaded_trancat_acct_id_sum | 4003.00 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4003 | carddemo.tran_cat_balance | 4003 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 3400.00 | interest_run.loaded_tran_cat_bal_sum | 3400.00 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 3400.00 | carddemo.tran_cat_balance | 3400.00 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, recorded at the load | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | interest_run.loaded_dis_int_rate_sum | 120.50 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, in the table now | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | carddemo.disclosure_group | 120.50 | **EQUAL** |  |
| RC3 | rows of account that encode to their source record | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2 | carddemo.account | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0; ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT left out: written by the calculation (INT-R07), see RC5 |
| RC3 | rows of card_xref that encode to their source record | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2 | carddemo.card_xref | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of tran_cat_balance that encode to their source record | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of disclosure_group that encode to their source record | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC4 | distinct keys (ACCT-ID) and rows of account | carddemo.account, distinct keys | 2 | carddemo.account, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, first record | 2001 | carddemo.account, lowest key | 2001 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, last record | 2002 | carddemo.account, highest key | 2002 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (XREF-CARD-NUM) and rows of card_xref | carddemo.card_xref, distinct keys | 2 | carddemo.card_xref, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, first record | 4000000000002001 | carddemo.card_xref, lowest key | 4000000000002001 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, last record | 4000000000002002 | carddemo.card_xref, highest key | 4000000000002002 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (TRANCAT-ACCT-ID+TRANCAT-TYPE-CD+TRANCAT-CD) and rows of tran_cat_balance | carddemo.tran_cat_balance, distinct keys | 2 | carddemo.tran_cat_balance, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, first record | 2001/01/1 | carddemo.tran_cat_balance, lowest key | 2001/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, last record | 2002/01/1 | carddemo.tran_cat_balance, highest key | 2002/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (DIS-ACCT-GROUP-ID+DIS-TRAN-TYPE-CD+DIS-TRAN-CAT-CD) and rows of disclosure_group | carddemo.disclosure_group, distinct keys | 15 | carddemo.disclosure_group, rows | 15 | **EQUAL** |  |
| RC4 | lowest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, first record | DEFAULT/01/1 | carddemo.disclosure_group, lowest key | DEFAULT/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, last record | STANDARD/05/1 | carddemo.disclosure_group, highest key | STANDARD/05/1 | **EQUAL** | table order: collation C (E6) |
| RC5 | transactions written | carddemo.interest_transaction, rows of the run | 2 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00, records | 2 | **EQUAL** |  |
| RC5 | transactions written, recorded by the run | interest_run.written_transaction_count | 2 | carddemo.interest_transaction, rows of the run | 2 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT | carddemo.interest_transaction | 47.00 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | 47.00 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT, recorded by the run | interest_run.written_tran_amt_sum | 47.00 | carddemo.interest_transaction | 47.00 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation | carddemo.account | 3410.00 | exported AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | 3410.00 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation, recorded by the run | interest_run.written_acct_curr_bal_sum | 3410.00 | carddemo.account | 3410.00 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04) | carddemo.interest_transaction, sum of TRAN-AMT | 47.00 | balance change + interest of the last account | 47.00 | **EQUAL** | balance change 10.00 (3400.00 before, 3410.00 after); interest of the last account, not added: 37.00 (last account 00000002002) |
| RC6 | ACCTDATA unloaded again after the write-back: count and the five sums of RC2 | ACCTDATA unloaded again | NOT AVAILABLE (needs a mainframe unload after the write-back) | carddemo.account | NOT AVAILABLE (not compared: there is no second unload off-host) | **NOT AVAILABLE** | cannot be shown off-host; mainframe operations owner |

### Trial-load profile (TL4), from the unloaded files

| What the mapping gaps ask for | Id | Found |
|---|---|---|
| Accounts with several cards | G08, INT-H03 | 0 |
| Type codes of the balances | G06 | 01 |
| Accounts with mixed type codes (digits and letters) | G06 | 0 |
| Minus zero amounts | G03 | 0 |
| F-signed amounts | G02 | 0 |
| Fillers that are not blank | E4 | 0 |
| Accounts that are not active | INT-H04 | 0 |
| Balances without a rate and without a DEFAULT rate | INT-R08 | 0 |

The profile is clean: nothing a mapping gap or an open rule asks about was found.

### Reject list (TL3)

No record rejected.

## Reconciliation report - i02-tie-half-cent: **EQUAL**

Unloaded files: `fixtures/interest/i02-tie-half-cent/input`. Exported files: `work/recon-java/run/i02-tie-half-cent/actual`. Run: `interest_run.run_id` 25, status EXPORTED, PARM 2022071800. The report reads the files, `carddemo.interest_run` and the tables in one read-only transaction; it writes nothing.

| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |
|---|---|---|---|---|
| RC1 | **EQUAL** | 8 | 0 | 0 |
| RC2 | **EQUAL** | 17 | 0 | 0 |
| RC3 | **EQUAL** | 4 | 0 | 0 |
| RC4 | **EQUAL** | 12 | 0 | 0 |
| RC5 | **EQUAL** | 7 | 0 | 0 |
| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | 1 |

### Figures (the columns of the reconciliation table of strategy-interest.md, from interest_run and the tables)

| Accounts | Cards | Balances | Rates | Sum ACCT-CURR-BAL before | Sum TRAN-CAT-BAL | Sum DIS-INT-RATE | Transactions written | Sum TRAN-AMT | Sum ACCT-CURR-BAL after | Balance change | Interest of last account (not added) | RC5 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2 | 2 | 2 | 15 | 2470.00 | 2470.00 | 120.50 | 2 | 24.69 | 2482.35 | 12.35 | 12.34 | holds |

### Lines

| Check | Figure | First side | Value | Second side | Value | Result | Note |
|---|---|---|---|---|---|---|---|
| RC1 | records and rows of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2 | carddemo.account | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of account | interest_run.loaded_account_count | 2 | carddemo.account | 2 | **EQUAL** |  |
| RC1 | records and rows of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2 | carddemo.card_xref | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of card_xref | interest_run.loaded_card_xref_count | 2 | carddemo.card_xref | 2 | **EQUAL** |  |
| RC1 | records and rows of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of tran_cat_balance | interest_run.loaded_tran_cat_balance_count | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | records and rows of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC1 | count recorded at the load of disclosure_group | interest_run.loaded_disclosure_group_count | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 4007 | interest_run.loaded_acct_id_sum | 4007.00 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 4007 | carddemo.account | 4007 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-BAL, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2470.00 | interest_run.loaded_acct_curr_bal_sum | 2470.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 10000.00 | interest_run.loaded_acct_credit_limit_sum | 10000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 10000.00 | carddemo.account | 10000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | interest_run.loaded_acct_cash_credit_limit_sum | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | carddemo.account | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2470.00 | interest_run.loaded_acct_curr_cyc_credit_sum | 2470.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-DEBIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 0.00 | interest_run.loaded_acct_curr_cyc_debit_sum | 0.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 4007 | interest_run.loaded_xref_acct_id_sum | 4007.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, in the table now | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 4007 | carddemo.card_xref | 4007 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4007 | interest_run.loaded_trancat_acct_id_sum | 4007.00 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4007 | carddemo.tran_cat_balance | 4007 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2470.00 | interest_run.loaded_tran_cat_bal_sum | 2470.00 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2470.00 | carddemo.tran_cat_balance | 2470.00 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, recorded at the load | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | interest_run.loaded_dis_int_rate_sum | 120.50 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, in the table now | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | carddemo.disclosure_group | 120.50 | **EQUAL** |  |
| RC3 | rows of account that encode to their source record | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2 | carddemo.account | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0; ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT left out: written by the calculation (INT-R07), see RC5 |
| RC3 | rows of card_xref that encode to their source record | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2 | carddemo.card_xref | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of tran_cat_balance that encode to their source record | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of disclosure_group that encode to their source record | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC4 | distinct keys (ACCT-ID) and rows of account | carddemo.account, distinct keys | 2 | carddemo.account, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, first record | 2003 | carddemo.account, lowest key | 2003 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, last record | 2004 | carddemo.account, highest key | 2004 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (XREF-CARD-NUM) and rows of card_xref | carddemo.card_xref, distinct keys | 2 | carddemo.card_xref, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, first record | 4000000000002003 | carddemo.card_xref, lowest key | 4000000000002003 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, last record | 4000000000002004 | carddemo.card_xref, highest key | 4000000000002004 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (TRANCAT-ACCT-ID+TRANCAT-TYPE-CD+TRANCAT-CD) and rows of tran_cat_balance | carddemo.tran_cat_balance, distinct keys | 2 | carddemo.tran_cat_balance, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, first record | 2003/01/1 | carddemo.tran_cat_balance, lowest key | 2003/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, last record | 2004/01/1 | carddemo.tran_cat_balance, highest key | 2004/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (DIS-ACCT-GROUP-ID+DIS-TRAN-TYPE-CD+DIS-TRAN-CAT-CD) and rows of disclosure_group | carddemo.disclosure_group, distinct keys | 15 | carddemo.disclosure_group, rows | 15 | **EQUAL** |  |
| RC4 | lowest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, first record | DEFAULT/01/1 | carddemo.disclosure_group, lowest key | DEFAULT/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, last record | STANDARD/05/1 | carddemo.disclosure_group, highest key | STANDARD/05/1 | **EQUAL** | table order: collation C (E6) |
| RC5 | transactions written | carddemo.interest_transaction, rows of the run | 2 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00, records | 2 | **EQUAL** |  |
| RC5 | transactions written, recorded by the run | interest_run.written_transaction_count | 2 | carddemo.interest_transaction, rows of the run | 2 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT | carddemo.interest_transaction | 24.69 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | 24.69 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT, recorded by the run | interest_run.written_tran_amt_sum | 24.69 | carddemo.interest_transaction | 24.69 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation | carddemo.account | 2482.35 | exported AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | 2482.35 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation, recorded by the run | interest_run.written_acct_curr_bal_sum | 2482.35 | carddemo.account | 2482.35 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04) | carddemo.interest_transaction, sum of TRAN-AMT | 24.69 | balance change + interest of the last account | 24.69 | **EQUAL** | balance change 12.35 (2470.00 before, 2482.35 after); interest of the last account, not added: 12.34 (last account 00000002004) |
| RC6 | ACCTDATA unloaded again after the write-back: count and the five sums of RC2 | ACCTDATA unloaded again | NOT AVAILABLE (needs a mainframe unload after the write-back) | carddemo.account | NOT AVAILABLE (not compared: there is no second unload off-host) | **NOT AVAILABLE** | cannot be shown off-host; mainframe operations owner |

### Trial-load profile (TL4), from the unloaded files

| What the mapping gaps ask for | Id | Found |
|---|---|---|
| Accounts with several cards | G08, INT-H03 | 0 |
| Type codes of the balances | G06 | 01 |
| Accounts with mixed type codes (digits and letters) | G06 | 0 |
| Minus zero amounts | G03 | 0 |
| F-signed amounts | G02 | 0 |
| Fillers that are not blank | E4 | 0 |
| Accounts that are not active | INT-H04 | 0 |
| Balances without a rate and without a DEFAULT rate | INT-R08 | 0 |

The profile is clean: nothing a mapping gap or an open rule asks about was found.

### Reject list (TL3)

No record rejected.

## Reconciliation report - i03-zero-rate-skipped: **EQUAL**

Unloaded files: `fixtures/interest/i03-zero-rate-skipped/input`. Exported files: `work/recon-java/run/i03-zero-rate-skipped/actual`. Run: `interest_run.run_id` 26, status EXPORTED, PARM 2022071800. The report reads the files, `carddemo.interest_run` and the tables in one read-only transaction; it writes nothing.

| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |
|---|---|---|---|---|
| RC1 | **EQUAL** | 8 | 0 | 0 |
| RC2 | **EQUAL** | 17 | 0 | 0 |
| RC3 | **EQUAL** | 4 | 0 | 0 |
| RC4 | **EQUAL** | 12 | 0 | 0 |
| RC5 | **EQUAL** | 7 | 0 | 0 |
| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | 1 |

### Figures (the columns of the reconciliation table of strategy-interest.md, from interest_run and the tables)

| Accounts | Cards | Balances | Rates | Sum ACCT-CURR-BAL before | Sum TRAN-CAT-BAL | Sum DIS-INT-RATE | Transactions written | Sum TRAN-AMT | Sum ACCT-CURR-BAL after | Balance change | Interest of last account (not added) | RC5 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 2 | 15 | 700.00 | 700.00 | 120.50 | 1 | 10.00 | 700.00 | 0.00 | 10.00 | holds |

### Lines

| Check | Figure | First side | Value | Second side | Value | Result | Note |
|---|---|---|---|---|---|---|---|
| RC1 | records and rows of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1 | carddemo.account | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of account | interest_run.loaded_account_count | 1 | carddemo.account | 1 | **EQUAL** |  |
| RC1 | records and rows of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 1 | carddemo.card_xref | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of card_xref | interest_run.loaded_card_xref_count | 1 | carddemo.card_xref | 1 | **EQUAL** |  |
| RC1 | records and rows of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of tran_cat_balance | interest_run.loaded_tran_cat_balance_count | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | records and rows of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC1 | count recorded at the load of disclosure_group | interest_run.loaded_disclosure_group_count | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2005 | interest_run.loaded_acct_id_sum | 2005.00 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2005 | carddemo.account | 2005 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-BAL, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 700.00 | interest_run.loaded_acct_curr_bal_sum | 700.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | interest_run.loaded_acct_credit_limit_sum | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | carddemo.account | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2500.00 | interest_run.loaded_acct_cash_credit_limit_sum | 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2500.00 | carddemo.account | 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1000.00 | interest_run.loaded_acct_curr_cyc_credit_sum | 1000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-DEBIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 300.00 | interest_run.loaded_acct_curr_cyc_debit_sum | 300.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2005 | interest_run.loaded_xref_acct_id_sum | 2005.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, in the table now | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2005 | carddemo.card_xref | 2005 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4010 | interest_run.loaded_trancat_acct_id_sum | 4010.00 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4010 | carddemo.tran_cat_balance | 4010 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 700.00 | interest_run.loaded_tran_cat_bal_sum | 700.00 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 700.00 | carddemo.tran_cat_balance | 700.00 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, recorded at the load | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | interest_run.loaded_dis_int_rate_sum | 120.50 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, in the table now | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | carddemo.disclosure_group | 120.50 | **EQUAL** |  |
| RC3 | rows of account that encode to their source record | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1 | carddemo.account | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0; ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT left out: written by the calculation (INT-R07), see RC5 |
| RC3 | rows of card_xref that encode to their source record | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 1 | carddemo.card_xref | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of tran_cat_balance that encode to their source record | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of disclosure_group that encode to their source record | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC4 | distinct keys (ACCT-ID) and rows of account | carddemo.account, distinct keys | 1 | carddemo.account, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, first record | 2005 | carddemo.account, lowest key | 2005 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, last record | 2005 | carddemo.account, highest key | 2005 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (XREF-CARD-NUM) and rows of card_xref | carddemo.card_xref, distinct keys | 1 | carddemo.card_xref, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, first record | 4000000000002005 | carddemo.card_xref, lowest key | 4000000000002005 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, last record | 4000000000002005 | carddemo.card_xref, highest key | 4000000000002005 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (TRANCAT-ACCT-ID+TRANCAT-TYPE-CD+TRANCAT-CD) and rows of tran_cat_balance | carddemo.tran_cat_balance, distinct keys | 2 | carddemo.tran_cat_balance, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, first record | 2005/01/1 | carddemo.tran_cat_balance, lowest key | 2005/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, last record | 2005/02/1 | carddemo.tran_cat_balance, highest key | 2005/02/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (DIS-ACCT-GROUP-ID+DIS-TRAN-TYPE-CD+DIS-TRAN-CAT-CD) and rows of disclosure_group | carddemo.disclosure_group, distinct keys | 15 | carddemo.disclosure_group, rows | 15 | **EQUAL** |  |
| RC4 | lowest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, first record | DEFAULT/01/1 | carddemo.disclosure_group, lowest key | DEFAULT/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, last record | STANDARD/05/1 | carddemo.disclosure_group, highest key | STANDARD/05/1 | **EQUAL** | table order: collation C (E6) |
| RC5 | transactions written | carddemo.interest_transaction, rows of the run | 1 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00, records | 1 | **EQUAL** |  |
| RC5 | transactions written, recorded by the run | interest_run.written_transaction_count | 1 | carddemo.interest_transaction, rows of the run | 1 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT | carddemo.interest_transaction | 10.00 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | 10.00 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT, recorded by the run | interest_run.written_tran_amt_sum | 10.00 | carddemo.interest_transaction | 10.00 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation | carddemo.account | 700.00 | exported AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | 700.00 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation, recorded by the run | interest_run.written_acct_curr_bal_sum | 700.00 | carddemo.account | 700.00 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04) | carddemo.interest_transaction, sum of TRAN-AMT | 10.00 | balance change + interest of the last account | 10.00 | **EQUAL** | balance change 0.00 (700.00 before, 700.00 after); interest of the last account, not added: 10.00 (last account 00000002005) |
| RC6 | ACCTDATA unloaded again after the write-back: count and the five sums of RC2 | ACCTDATA unloaded again | NOT AVAILABLE (needs a mainframe unload after the write-back) | carddemo.account | NOT AVAILABLE (not compared: there is no second unload off-host) | **NOT AVAILABLE** | cannot be shown off-host; mainframe operations owner |

### Trial-load profile (TL4), from the unloaded files

| What the mapping gaps ask for | Id | Found |
|---|---|---|
| Accounts with several cards | G08, INT-H03 | 0 |
| Type codes of the balances | G06 | 01, 02 |
| Accounts with mixed type codes (digits and letters) | G06 | 0 |
| Minus zero amounts | G03 | 0 |
| F-signed amounts | G02 | 0 |
| Fillers that are not blank | E4 | 0 |
| Accounts that are not active | INT-H04 | 0 |
| Balances without a rate and without a DEFAULT rate | INT-R08 | 0 |

The profile is clean: nothing a mapping gap or an open rule asks about was found.

### Reject list (TL3)

No record rejected.

## Reconciliation report - i04-default-group-fallback: **EQUAL**

Unloaded files: `fixtures/interest/i04-default-group-fallback/input`. Exported files: `work/recon-java/run/i04-default-group-fallback/actual`. Run: `interest_run.run_id` 27, status EXPORTED, PARM 2022071800. The report reads the files, `carddemo.interest_run` and the tables in one read-only transaction; it writes nothing.

| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |
|---|---|---|---|---|
| RC1 | **EQUAL** | 8 | 0 | 0 |
| RC2 | **EQUAL** | 17 | 0 | 0 |
| RC3 | **EQUAL** | 4 | 0 | 0 |
| RC4 | **EQUAL** | 12 | 0 | 0 |
| RC5 | **EQUAL** | 7 | 0 | 0 |
| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | 1 |

### Figures (the columns of the reconciliation table of strategy-interest.md, from interest_run and the tables)

| Accounts | Cards | Balances | Rates | Sum ACCT-CURR-BAL before | Sum TRAN-CAT-BAL | Sum DIS-INT-RATE | Transactions written | Sum TRAN-AMT | Sum ACCT-CURR-BAL after | Balance change | Interest of last account (not added) | RC5 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 15 | 1000.00 | 1000.00 | 120.50 | 1 | 17.50 | 1000.00 | 0.00 | 17.50 | holds |

### Lines

| Check | Figure | First side | Value | Second side | Value | Result | Note |
|---|---|---|---|---|---|---|---|
| RC1 | records and rows of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1 | carddemo.account | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of account | interest_run.loaded_account_count | 1 | carddemo.account | 1 | **EQUAL** |  |
| RC1 | records and rows of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 1 | carddemo.card_xref | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of card_xref | interest_run.loaded_card_xref_count | 1 | carddemo.card_xref | 1 | **EQUAL** |  |
| RC1 | records and rows of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1 | carddemo.tran_cat_balance | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of tran_cat_balance | interest_run.loaded_tran_cat_balance_count | 1 | carddemo.tran_cat_balance | 1 | **EQUAL** |  |
| RC1 | records and rows of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC1 | count recorded at the load of disclosure_group | interest_run.loaded_disclosure_group_count | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2006 | interest_run.loaded_acct_id_sum | 2006.00 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2006 | carddemo.account | 2006 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-BAL, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1000.00 | interest_run.loaded_acct_curr_bal_sum | 1000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | interest_run.loaded_acct_credit_limit_sum | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | carddemo.account | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2500.00 | interest_run.loaded_acct_cash_credit_limit_sum | 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2500.00 | carddemo.account | 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1000.00 | interest_run.loaded_acct_curr_cyc_credit_sum | 1000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-DEBIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 0.00 | interest_run.loaded_acct_curr_cyc_debit_sum | 0.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2006 | interest_run.loaded_xref_acct_id_sum | 2006.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, in the table now | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2006 | carddemo.card_xref | 2006 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2006 | interest_run.loaded_trancat_acct_id_sum | 2006.00 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2006 | carddemo.tran_cat_balance | 2006 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1000.00 | interest_run.loaded_tran_cat_bal_sum | 1000.00 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1000.00 | carddemo.tran_cat_balance | 1000.00 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, recorded at the load | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | interest_run.loaded_dis_int_rate_sum | 120.50 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, in the table now | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | carddemo.disclosure_group | 120.50 | **EQUAL** |  |
| RC3 | rows of account that encode to their source record | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1 | carddemo.account | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0; ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT left out: written by the calculation (INT-R07), see RC5 |
| RC3 | rows of card_xref that encode to their source record | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 1 | carddemo.card_xref | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of tran_cat_balance that encode to their source record | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1 | carddemo.tran_cat_balance | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of disclosure_group that encode to their source record | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC4 | distinct keys (ACCT-ID) and rows of account | carddemo.account, distinct keys | 1 | carddemo.account, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, first record | 2006 | carddemo.account, lowest key | 2006 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, last record | 2006 | carddemo.account, highest key | 2006 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (XREF-CARD-NUM) and rows of card_xref | carddemo.card_xref, distinct keys | 1 | carddemo.card_xref, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, first record | 4000000000002006 | carddemo.card_xref, lowest key | 4000000000002006 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, last record | 4000000000002006 | carddemo.card_xref, highest key | 4000000000002006 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (TRANCAT-ACCT-ID+TRANCAT-TYPE-CD+TRANCAT-CD) and rows of tran_cat_balance | carddemo.tran_cat_balance, distinct keys | 1 | carddemo.tran_cat_balance, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, first record | 2006/01/1 | carddemo.tran_cat_balance, lowest key | 2006/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, last record | 2006/01/1 | carddemo.tran_cat_balance, highest key | 2006/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (DIS-ACCT-GROUP-ID+DIS-TRAN-TYPE-CD+DIS-TRAN-CAT-CD) and rows of disclosure_group | carddemo.disclosure_group, distinct keys | 15 | carddemo.disclosure_group, rows | 15 | **EQUAL** |  |
| RC4 | lowest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, first record | DEFAULT/01/1 | carddemo.disclosure_group, lowest key | DEFAULT/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, last record | STANDARD/05/1 | carddemo.disclosure_group, highest key | STANDARD/05/1 | **EQUAL** | table order: collation C (E6) |
| RC5 | transactions written | carddemo.interest_transaction, rows of the run | 1 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00, records | 1 | **EQUAL** |  |
| RC5 | transactions written, recorded by the run | interest_run.written_transaction_count | 1 | carddemo.interest_transaction, rows of the run | 1 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT | carddemo.interest_transaction | 17.50 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | 17.50 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT, recorded by the run | interest_run.written_tran_amt_sum | 17.50 | carddemo.interest_transaction | 17.50 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation | carddemo.account | 1000.00 | exported AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | 1000.00 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation, recorded by the run | interest_run.written_acct_curr_bal_sum | 1000.00 | carddemo.account | 1000.00 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04) | carddemo.interest_transaction, sum of TRAN-AMT | 17.50 | balance change + interest of the last account | 17.50 | **EQUAL** | balance change 0.00 (1000.00 before, 1000.00 after); interest of the last account, not added: 17.50 (last account 00000002006) |
| RC6 | ACCTDATA unloaded again after the write-back: count and the five sums of RC2 | ACCTDATA unloaded again | NOT AVAILABLE (needs a mainframe unload after the write-back) | carddemo.account | NOT AVAILABLE (not compared: there is no second unload off-host) | **NOT AVAILABLE** | cannot be shown off-host; mainframe operations owner |

### Trial-load profile (TL4), from the unloaded files

| What the mapping gaps ask for | Id | Found |
|---|---|---|
| Accounts with several cards | G08, INT-H03 | 0 |
| Type codes of the balances | G06 | 01 |
| Accounts with mixed type codes (digits and letters) | G06 | 0 |
| Minus zero amounts | G03 | 0 |
| F-signed amounts | G02 | 0 |
| Fillers that are not blank | E4 | 0 |
| Accounts that are not active | INT-H04 | 0 |
| Balances without a rate and without a DEFAULT rate | INT-R08 | 0 |

The profile is clean: nothing a mapping gap or an open rule asks about was found.

### Reject list (TL3)

No record rejected.

## Reconciliation report - i05-multi-category: **EQUAL**

Unloaded files: `fixtures/interest/i05-multi-category/input`. Exported files: `work/recon-java/run/i05-multi-category/actual`. Run: `interest_run.run_id` 28, status EXPORTED, PARM 2022071800. The report reads the files, `carddemo.interest_run` and the tables in one read-only transaction; it writes nothing.

| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |
|---|---|---|---|---|
| RC1 | **EQUAL** | 8 | 0 | 0 |
| RC2 | **EQUAL** | 17 | 0 | 0 |
| RC3 | **EQUAL** | 4 | 0 | 0 |
| RC4 | **EQUAL** | 12 | 0 | 0 |
| RC5 | **EQUAL** | 7 | 0 | 0 |
| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | 1 |

### Figures (the columns of the reconciliation table of strategy-interest.md, from interest_run and the tables)

| Accounts | Cards | Balances | Rates | Sum ACCT-CURR-BAL before | Sum TRAN-CAT-BAL | Sum DIS-INT-RATE | Transactions written | Sum TRAN-AMT | Sum ACCT-CURR-BAL after | Balance change | Interest of last account (not added) | RC5 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 2 | 15 | 1500.00 | 1500.00 | 120.50 | 2 | 17.50 | 1500.00 | 0.00 | 17.50 | holds |

### Lines

| Check | Figure | First side | Value | Second side | Value | Result | Note |
|---|---|---|---|---|---|---|---|
| RC1 | records and rows of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1 | carddemo.account | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of account | interest_run.loaded_account_count | 1 | carddemo.account | 1 | **EQUAL** |  |
| RC1 | records and rows of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 1 | carddemo.card_xref | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of card_xref | interest_run.loaded_card_xref_count | 1 | carddemo.card_xref | 1 | **EQUAL** |  |
| RC1 | records and rows of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of tran_cat_balance | interest_run.loaded_tran_cat_balance_count | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | records and rows of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC1 | count recorded at the load of disclosure_group | interest_run.loaded_disclosure_group_count | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2007 | interest_run.loaded_acct_id_sum | 2007.00 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2007 | carddemo.account | 2007 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-BAL, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1500.00 | interest_run.loaded_acct_curr_bal_sum | 1500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | interest_run.loaded_acct_credit_limit_sum | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | carddemo.account | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2500.00 | interest_run.loaded_acct_cash_credit_limit_sum | 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2500.00 | carddemo.account | 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1500.00 | interest_run.loaded_acct_curr_cyc_credit_sum | 1500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-DEBIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 0.00 | interest_run.loaded_acct_curr_cyc_debit_sum | 0.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2007 | interest_run.loaded_xref_acct_id_sum | 2007.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, in the table now | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2007 | carddemo.card_xref | 2007 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4014 | interest_run.loaded_trancat_acct_id_sum | 4014.00 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4014 | carddemo.tran_cat_balance | 4014 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1500.00 | interest_run.loaded_tran_cat_bal_sum | 1500.00 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1500.00 | carddemo.tran_cat_balance | 1500.00 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, recorded at the load | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | interest_run.loaded_dis_int_rate_sum | 120.50 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, in the table now | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | carddemo.disclosure_group | 120.50 | **EQUAL** |  |
| RC3 | rows of account that encode to their source record | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1 | carddemo.account | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0; ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT left out: written by the calculation (INT-R07), see RC5 |
| RC3 | rows of card_xref that encode to their source record | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 1 | carddemo.card_xref | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of tran_cat_balance that encode to their source record | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of disclosure_group that encode to their source record | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC4 | distinct keys (ACCT-ID) and rows of account | carddemo.account, distinct keys | 1 | carddemo.account, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, first record | 2007 | carddemo.account, lowest key | 2007 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, last record | 2007 | carddemo.account, highest key | 2007 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (XREF-CARD-NUM) and rows of card_xref | carddemo.card_xref, distinct keys | 1 | carddemo.card_xref, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, first record | 4000000000002007 | carddemo.card_xref, lowest key | 4000000000002007 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, last record | 4000000000002007 | carddemo.card_xref, highest key | 4000000000002007 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (TRANCAT-ACCT-ID+TRANCAT-TYPE-CD+TRANCAT-CD) and rows of tran_cat_balance | carddemo.tran_cat_balance, distinct keys | 2 | carddemo.tran_cat_balance, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, first record | 2007/01/1 | carddemo.tran_cat_balance, lowest key | 2007/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, last record | 2007/01/2 | carddemo.tran_cat_balance, highest key | 2007/01/2 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (DIS-ACCT-GROUP-ID+DIS-TRAN-TYPE-CD+DIS-TRAN-CAT-CD) and rows of disclosure_group | carddemo.disclosure_group, distinct keys | 15 | carddemo.disclosure_group, rows | 15 | **EQUAL** |  |
| RC4 | lowest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, first record | DEFAULT/01/1 | carddemo.disclosure_group, lowest key | DEFAULT/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, last record | STANDARD/05/1 | carddemo.disclosure_group, highest key | STANDARD/05/1 | **EQUAL** | table order: collation C (E6) |
| RC5 | transactions written | carddemo.interest_transaction, rows of the run | 2 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00, records | 2 | **EQUAL** |  |
| RC5 | transactions written, recorded by the run | interest_run.written_transaction_count | 2 | carddemo.interest_transaction, rows of the run | 2 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT | carddemo.interest_transaction | 17.50 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | 17.50 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT, recorded by the run | interest_run.written_tran_amt_sum | 17.50 | carddemo.interest_transaction | 17.50 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation | carddemo.account | 1500.00 | exported AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | 1500.00 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation, recorded by the run | interest_run.written_acct_curr_bal_sum | 1500.00 | carddemo.account | 1500.00 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04) | carddemo.interest_transaction, sum of TRAN-AMT | 17.50 | balance change + interest of the last account | 17.50 | **EQUAL** | balance change 0.00 (1500.00 before, 1500.00 after); interest of the last account, not added: 17.50 (last account 00000002007) |
| RC6 | ACCTDATA unloaded again after the write-back: count and the five sums of RC2 | ACCTDATA unloaded again | NOT AVAILABLE (needs a mainframe unload after the write-back) | carddemo.account | NOT AVAILABLE (not compared: there is no second unload off-host) | **NOT AVAILABLE** | cannot be shown off-host; mainframe operations owner |

### Trial-load profile (TL4), from the unloaded files

| What the mapping gaps ask for | Id | Found |
|---|---|---|
| Accounts with several cards | G08, INT-H03 | 0 |
| Type codes of the balances | G06 | 01 |
| Accounts with mixed type codes (digits and letters) | G06 | 0 |
| Minus zero amounts | G03 | 0 |
| F-signed amounts | G02 | 0 |
| Fillers that are not blank | E4 | 0 |
| Accounts that are not active | INT-H04 | 0 |
| Balances without a rate and without a DEFAULT rate | INT-R08 | 0 |

The profile is clean: nothing a mapping gap or an open rule asks about was found.

### Reject list (TL3)

No record rejected.

## Reconciliation report - i06-negative-balance: **EQUAL**

Unloaded files: `fixtures/interest/i06-negative-balance/input`. Exported files: `work/recon-java/run/i06-negative-balance/actual`. Run: `interest_run.run_id` 29, status EXPORTED, PARM 2022071800. The report reads the files, `carddemo.interest_run` and the tables in one read-only transaction; it writes nothing.

| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |
|---|---|---|---|---|
| RC1 | **EQUAL** | 8 | 0 | 0 |
| RC2 | **EQUAL** | 17 | 0 | 0 |
| RC3 | **EQUAL** | 4 | 0 | 0 |
| RC4 | **EQUAL** | 12 | 0 | 0 |
| RC5 | **EQUAL** | 7 | 0 | 0 |
| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | 1 |

### Figures (the columns of the reconciliation table of strategy-interest.md, from interest_run and the tables)

| Accounts | Cards | Balances | Rates | Sum ACCT-CURR-BAL before | Sum TRAN-CAT-BAL | Sum DIS-INT-RATE | Transactions written | Sum TRAN-AMT | Sum ACCT-CURR-BAL after | Balance change | Interest of last account (not added) | RC5 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 15 | -300.00 | -300.00 | 120.50 | 1 | -3.00 | -300.00 | 0.00 | -3.00 | holds |

### Lines

| Check | Figure | First side | Value | Second side | Value | Result | Note |
|---|---|---|---|---|---|---|---|
| RC1 | records and rows of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1 | carddemo.account | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of account | interest_run.loaded_account_count | 1 | carddemo.account | 1 | **EQUAL** |  |
| RC1 | records and rows of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 1 | carddemo.card_xref | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of card_xref | interest_run.loaded_card_xref_count | 1 | carddemo.card_xref | 1 | **EQUAL** |  |
| RC1 | records and rows of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1 | carddemo.tran_cat_balance | 1 | **EQUAL** |  |
| RC1 | count recorded at the load of tran_cat_balance | interest_run.loaded_tran_cat_balance_count | 1 | carddemo.tran_cat_balance | 1 | **EQUAL** |  |
| RC1 | records and rows of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC1 | count recorded at the load of disclosure_group | interest_run.loaded_disclosure_group_count | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2008 | interest_run.loaded_acct_id_sum | 2008.00 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2008 | carddemo.account | 2008 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-BAL, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | -300.00 | interest_run.loaded_acct_curr_bal_sum | -300.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | interest_run.loaded_acct_credit_limit_sum | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | carddemo.account | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2500.00 | interest_run.loaded_acct_cash_credit_limit_sum | 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2500.00 | carddemo.account | 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 0.00 | interest_run.loaded_acct_curr_cyc_credit_sum | 0.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-DEBIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 300.00 | interest_run.loaded_acct_curr_cyc_debit_sum | 300.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2008 | interest_run.loaded_xref_acct_id_sum | 2008.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, in the table now | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2008 | carddemo.card_xref | 2008 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2008 | interest_run.loaded_trancat_acct_id_sum | 2008.00 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2008 | carddemo.tran_cat_balance | 2008 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | -300.00 | interest_run.loaded_tran_cat_bal_sum | -300.00 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | -300.00 | carddemo.tran_cat_balance | -300.00 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, recorded at the load | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | interest_run.loaded_dis_int_rate_sum | 120.50 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, in the table now | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | carddemo.disclosure_group | 120.50 | **EQUAL** |  |
| RC3 | rows of account that encode to their source record | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1 | carddemo.account | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0; ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT left out: written by the calculation (INT-R07), see RC5 |
| RC3 | rows of card_xref that encode to their source record | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 1 | carddemo.card_xref | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of tran_cat_balance that encode to their source record | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1 | carddemo.tran_cat_balance | 1 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of disclosure_group that encode to their source record | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC4 | distinct keys (ACCT-ID) and rows of account | carddemo.account, distinct keys | 1 | carddemo.account, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, first record | 2008 | carddemo.account, lowest key | 2008 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, last record | 2008 | carddemo.account, highest key | 2008 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (XREF-CARD-NUM) and rows of card_xref | carddemo.card_xref, distinct keys | 1 | carddemo.card_xref, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, first record | 4000000000002008 | carddemo.card_xref, lowest key | 4000000000002008 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, last record | 4000000000002008 | carddemo.card_xref, highest key | 4000000000002008 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (TRANCAT-ACCT-ID+TRANCAT-TYPE-CD+TRANCAT-CD) and rows of tran_cat_balance | carddemo.tran_cat_balance, distinct keys | 1 | carddemo.tran_cat_balance, rows | 1 | **EQUAL** |  |
| RC4 | lowest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, first record | 2008/01/1 | carddemo.tran_cat_balance, lowest key | 2008/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, last record | 2008/01/1 | carddemo.tran_cat_balance, highest key | 2008/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (DIS-ACCT-GROUP-ID+DIS-TRAN-TYPE-CD+DIS-TRAN-CAT-CD) and rows of disclosure_group | carddemo.disclosure_group, distinct keys | 15 | carddemo.disclosure_group, rows | 15 | **EQUAL** |  |
| RC4 | lowest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, first record | DEFAULT/01/1 | carddemo.disclosure_group, lowest key | DEFAULT/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, last record | STANDARD/05/1 | carddemo.disclosure_group, highest key | STANDARD/05/1 | **EQUAL** | table order: collation C (E6) |
| RC5 | transactions written | carddemo.interest_transaction, rows of the run | 1 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00, records | 1 | **EQUAL** |  |
| RC5 | transactions written, recorded by the run | interest_run.written_transaction_count | 1 | carddemo.interest_transaction, rows of the run | 1 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT | carddemo.interest_transaction | -3.00 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | -3.00 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT, recorded by the run | interest_run.written_tran_amt_sum | -3.00 | carddemo.interest_transaction | -3.00 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation | carddemo.account | -300.00 | exported AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | -300.00 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation, recorded by the run | interest_run.written_acct_curr_bal_sum | -300.00 | carddemo.account | -300.00 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04) | carddemo.interest_transaction, sum of TRAN-AMT | -3.00 | balance change + interest of the last account | -3.00 | **EQUAL** | balance change 0.00 (-300.00 before, -300.00 after); interest of the last account, not added: -3.00 (last account 00000002008) |
| RC6 | ACCTDATA unloaded again after the write-back: count and the five sums of RC2 | ACCTDATA unloaded again | NOT AVAILABLE (needs a mainframe unload after the write-back) | carddemo.account | NOT AVAILABLE (not compared: there is no second unload off-host) | **NOT AVAILABLE** | cannot be shown off-host; mainframe operations owner |

### Trial-load profile (TL4), from the unloaded files

| What the mapping gaps ask for | Id | Found |
|---|---|---|
| Accounts with several cards | G08, INT-H03 | 0 |
| Type codes of the balances | G06 | 01 |
| Accounts with mixed type codes (digits and letters) | G06 | 0 |
| Minus zero amounts | G03 | 0 |
| F-signed amounts | G02 | 0 |
| Fillers that are not blank | E4 | 0 |
| Accounts that are not active | INT-H04 | 0 |
| Balances without a rate and without a DEFAULT rate | INT-R08 | 0 |

The profile is clean: nothing a mapping gap or an open rule asks about was found.

### Reject list (TL3)

No record rejected.

## Reconciliation report - i07-nontie-rounding: **EQUAL**

Unloaded files: `fixtures/interest/i07-nontie-rounding/input`. Exported files: `work/recon-java/run/i07-nontie-rounding/actual`. Run: `interest_run.run_id` 30, status EXPORTED, PARM 2022071800. The report reads the files, `carddemo.interest_run` and the tables in one read-only transaction; it writes nothing.

| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |
|---|---|---|---|---|
| RC1 | **EQUAL** | 8 | 0 | 0 |
| RC2 | **EQUAL** | 17 | 0 | 0 |
| RC3 | **EQUAL** | 4 | 0 | 0 |
| RC4 | **EQUAL** | 12 | 0 | 0 |
| RC5 | **EQUAL** | 7 | 0 | 0 |
| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | 1 |

### Figures (the columns of the reconciliation table of strategy-interest.md, from interest_run and the tables)

| Accounts | Cards | Balances | Rates | Sum ACCT-CURR-BAL before | Sum TRAN-CAT-BAL | Sum DIS-INT-RATE | Transactions written | Sum TRAN-AMT | Sum ACCT-CURR-BAL after | Balance change | Interest of last account (not added) | RC5 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2 | 2 | 2 | 15 | 1778.10 | 1778.10 | 120.50 | 2 | 29.03 | 1793.52 | 15.42 | 13.61 | holds |

### Lines

| Check | Figure | First side | Value | Second side | Value | Result | Note |
|---|---|---|---|---|---|---|---|
| RC1 | records and rows of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2 | carddemo.account | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of account | interest_run.loaded_account_count | 2 | carddemo.account | 2 | **EQUAL** |  |
| RC1 | records and rows of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2 | carddemo.card_xref | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of card_xref | interest_run.loaded_card_xref_count | 2 | carddemo.card_xref | 2 | **EQUAL** |  |
| RC1 | records and rows of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | count recorded at the load of tran_cat_balance | interest_run.loaded_tran_cat_balance_count | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** |  |
| RC1 | records and rows of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC1 | count recorded at the load of disclosure_group | interest_run.loaded_disclosure_group_count | 15 | carddemo.disclosure_group | 15 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 4019 | interest_run.loaded_acct_id_sum | 4019.00 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 4019 | carddemo.account | 4019 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-BAL, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1778.10 | interest_run.loaded_acct_curr_bal_sum | 1778.10 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 10000.00 | interest_run.loaded_acct_credit_limit_sum | 10000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 10000.00 | carddemo.account | 10000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | interest_run.loaded_acct_cash_credit_limit_sum | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT, in the table now | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 5000.00 | carddemo.account | 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 1778.10 | interest_run.loaded_acct_curr_cyc_credit_sum | 1778.10 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-DEBIT, recorded at the load | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 0.00 | interest_run.loaded_acct_curr_cyc_debit_sum | 0.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 4019 | interest_run.loaded_xref_acct_id_sum | 4019.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID, in the table now | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 4019 | carddemo.card_xref | 4019 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4019 | interest_run.loaded_trancat_acct_id_sum | 4019.00 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 4019 | carddemo.tran_cat_balance | 4019 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, recorded at the load | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1778.10 | interest_run.loaded_tran_cat_bal_sum | 1778.10 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL, in the table now | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 1778.10 | carddemo.tran_cat_balance | 1778.10 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, recorded at the load | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | interest_run.loaded_dis_int_rate_sum | 120.50 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE, in the table now | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 120.50 | carddemo.disclosure_group | 120.50 | **EQUAL** |  |
| RC3 | rows of account that encode to their source record | AWS.M2.CARDDEMO.ACCTDATA.PS.txt | 2 | carddemo.account | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0; ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT left out: written by the calculation (INT-R07), see RC5 |
| RC3 | rows of card_xref that encode to their source record | AWS.M2.CARDDEMO.CARDXREF.PS.txt | 2 | carddemo.card_xref | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of tran_cat_balance that encode to their source record | AWS.M2.CARDDEMO.TCATBALF.PS.txt | 2 | carddemo.tran_cat_balance | 2 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC3 | rows of disclosure_group that encode to their source record | AWS.M2.CARDDEMO.DISCGRP.PS.txt | 15 | carddemo.disclosure_group | 15 | **EQUAL** | in the file: G02 (F-signed) 0, G03 (minus zero) 0, counted and not a failure; other differences 0 |
| RC4 | distinct keys (ACCT-ID) and rows of account | carddemo.account, distinct keys | 2 | carddemo.account, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, first record | 2009 | carddemo.account, lowest key | 2009 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of account | AWS.M2.CARDDEMO.ACCTDATA.PS.txt, last record | 2010 | carddemo.account, highest key | 2010 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (XREF-CARD-NUM) and rows of card_xref | carddemo.card_xref, distinct keys | 2 | carddemo.card_xref, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, first record | 4000000000002009 | carddemo.card_xref, lowest key | 4000000000002009 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of card_xref | AWS.M2.CARDDEMO.CARDXREF.PS.txt, last record | 4000000000002010 | carddemo.card_xref, highest key | 4000000000002010 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (TRANCAT-ACCT-ID+TRANCAT-TYPE-CD+TRANCAT-CD) and rows of tran_cat_balance | carddemo.tran_cat_balance, distinct keys | 2 | carddemo.tran_cat_balance, rows | 2 | **EQUAL** |  |
| RC4 | lowest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, first record | 2009/01/1 | carddemo.tran_cat_balance, lowest key | 2009/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of tran_cat_balance | AWS.M2.CARDDEMO.TCATBALF.PS.txt, last record | 2010/01/1 | carddemo.tran_cat_balance, highest key | 2010/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | distinct keys (DIS-ACCT-GROUP-ID+DIS-TRAN-TYPE-CD+DIS-TRAN-CAT-CD) and rows of disclosure_group | carddemo.disclosure_group, distinct keys | 15 | carddemo.disclosure_group, rows | 15 | **EQUAL** |  |
| RC4 | lowest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, first record | DEFAULT/01/1 | carddemo.disclosure_group, lowest key | DEFAULT/01/1 | **EQUAL** | table order: collation C (E6) |
| RC4 | highest key of disclosure_group | AWS.M2.CARDDEMO.DISCGRP.PS.txt, last record | STANDARD/05/1 | carddemo.disclosure_group, highest key | STANDARD/05/1 | **EQUAL** | table order: collation C (E6) |
| RC5 | transactions written | carddemo.interest_transaction, rows of the run | 2 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00, records | 2 | **EQUAL** |  |
| RC5 | transactions written, recorded by the run | interest_run.written_transaction_count | 2 | carddemo.interest_transaction, rows of the run | 2 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT | carddemo.interest_transaction | 29.03 | exported AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | 29.03 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT, recorded by the run | interest_run.written_tran_amt_sum | 29.03 | carddemo.interest_transaction | 29.03 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation | carddemo.account | 1793.52 | exported AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | 1793.52 | **EQUAL** |  |
| RC5 | sum of ACCT-CURR-BAL after the calculation, recorded by the run | interest_run.written_acct_curr_bal_sum | 1793.52 | carddemo.account | 1793.52 | **EQUAL** |  |
| RC5 | sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04) | carddemo.interest_transaction, sum of TRAN-AMT | 29.03 | balance change + interest of the last account | 29.03 | **EQUAL** | balance change 15.42 (1778.10 before, 1793.52 after); interest of the last account, not added: 13.61 (last account 00000002010) |
| RC6 | ACCTDATA unloaded again after the write-back: count and the five sums of RC2 | ACCTDATA unloaded again | NOT AVAILABLE (needs a mainframe unload after the write-back) | carddemo.account | NOT AVAILABLE (not compared: there is no second unload off-host) | **NOT AVAILABLE** | cannot be shown off-host; mainframe operations owner |

### Trial-load profile (TL4), from the unloaded files

| What the mapping gaps ask for | Id | Found |
|---|---|---|
| Accounts with several cards | G08, INT-H03 | 0 |
| Type codes of the balances | G06 | 01 |
| Accounts with mixed type codes (digits and letters) | G06 | 0 |
| Minus zero amounts | G03 | 0 |
| F-signed amounts | G02 | 0 |
| Fillers that are not blank | E4 | 0 |
| Accounts that are not active | INT-H04 | 0 |
| Balances without a rate and without a DEFAULT rate | INT-R08 | 0 |

The profile is clean: nothing a mapping gap or an open rule asks about was found.

### Reject list (TL3)

No record rejected.

