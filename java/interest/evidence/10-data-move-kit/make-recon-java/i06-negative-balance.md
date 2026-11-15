# Reconciliation report - i06-negative-balance

Run: interest_run 7, status LOADED, PARM date 2022071800

RC5 in words: 1 transaction(s) summing -3.00; sum of ACCT-CURR-BAL -300.00 before and -300.00 after, a balance change of 0.00; interest of the last account -3.00, not added to the last account (INT-Q04).

Result: 31 EQUAL, 0 DIFFERENT, 6 NOT AVAILABLE. A figure that cannot be computed is NOT AVAILABLE, never EQUAL.

| Check | What | First figure | Second figure | Result | Note |
|---|---|---|---|---|---|
| RC1 | records of AWS.M2.CARDDEMO.ACCTDATA.PS.txt = rows loaded into account | unloaded file: 1 | interest_run: 1 | **EQUAL** |  |
| RC1 | records of AWS.M2.CARDDEMO.ACCTDATA.PS.txt = rows in account | unloaded file: 1 | table: 1 | **EQUAL** |  |
| RC1 | records of AWS.M2.CARDDEMO.CARDXREF.PS.txt = rows loaded into card_xref | unloaded file: 1 | interest_run: 1 | **EQUAL** |  |
| RC1 | records of AWS.M2.CARDDEMO.CARDXREF.PS.txt = rows in card_xref | unloaded file: 1 | table: 1 | **EQUAL** |  |
| RC1 | records of AWS.M2.CARDDEMO.TCATBALF.PS.txt = rows loaded into tran_cat_balance | unloaded file: 1 | interest_run: 1 | **EQUAL** |  |
| RC1 | records of AWS.M2.CARDDEMO.TCATBALF.PS.txt = rows in tran_cat_balance | unloaded file: 1 | table: 1 | **EQUAL** |  |
| RC1 | records of AWS.M2.CARDDEMO.DISCGRP.PS.txt = rows loaded into disclosure_group | unloaded file: 15 | interest_run: 15 | **EQUAL** |  |
| RC1 | records of AWS.M2.CARDDEMO.DISCGRP.PS.txt = rows in disclosure_group | unloaded file: 15 | table: 15 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-BAL | unloaded file: -300.00 | interest_run: -300.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CREDIT-LIMIT | unloaded file: 5000.00 | interest_run: 5000.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CASH-CREDIT-LIMIT | unloaded file: 2500.00 | interest_run: 2500.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-CREDIT | unloaded file: 0.00 | interest_run: 0.00 | **EQUAL** |  |
| RC2 | sum of ACCT-CURR-CYC-DEBIT | unloaded file: 300.00 | interest_run: 300.00 | **EQUAL** |  |
| RC2 | sum of TRAN-CAT-BAL | unloaded file: -300.00 | interest_run: -300.00 | **EQUAL** |  |
| RC2 | sum of DIS-INT-RATE | unloaded file: 120.50 | interest_run: 120.50 | **EQUAL** |  |
| RC2 | hash total of ACCT-ID | unloaded file: 2008 | interest_run: 2008.00 | **EQUAL** |  |
| RC2 | hash total of XREF-ACCT-ID | unloaded file: 2008 | interest_run: 2008.00 | **EQUAL** |  |
| RC2 | hash total of TRANCAT-ACCT-ID | unloaded file: 2008 | interest_run: 2008.00 | **EQUAL** |  |
| RC3 | every row of account, encoded again, equals its record of AWS.M2.CARDDEMO.ACCTDATA.PS.txt | unloaded file: 1 | rows equal or explained by G02/G03: 1 | **EQUAL** |  |
| RC3 | every row of card_xref, encoded again, equals its record of AWS.M2.CARDDEMO.CARDXREF.PS.txt | unloaded file: 1 | rows equal or explained by G02/G03: 1 | **EQUAL** |  |
| RC3 | every row of tran_cat_balance, encoded again, equals its record of AWS.M2.CARDDEMO.TCATBALF.PS.txt | unloaded file: 1 | rows equal or explained by G02/G03: 1 | **EQUAL** |  |
| RC3 | every row of disclosure_group, encoded again, equals its record of AWS.M2.CARDDEMO.DISCGRP.PS.txt | unloaded file: 15 | rows equal or explained by G02/G03: 15 | **EQUAL** |  |
| RC4 | distinct keys of account = its row count | distinct keys: 1 | rows: 1 | **EQUAL** |  |
| RC4 | lowest and highest key of account = first and last record of AWS.M2.CARDDEMO.ACCTDATA.PS.txt | first to last record (key ends): ...2008 to ...2008 | lowest to highest key (key ends): ...2008 to ...2008 | **EQUAL** |  |
| RC4 | distinct keys of card_xref = its row count | distinct keys: 1 | rows: 1 | **EQUAL** |  |
| RC4 | lowest and highest key of card_xref = first and last record of AWS.M2.CARDDEMO.CARDXREF.PS.txt | first to last record (key ends): ...2008 to ...2008 | lowest to highest key (key ends): ...2008 to ...2008 | **EQUAL** |  |
| RC4 | distinct keys of tran_cat_balance = its row count | distinct keys: 1 | rows: 1 | **EQUAL** |  |
| RC4 | lowest and highest key of tran_cat_balance = first and last record of AWS.M2.CARDDEMO.TCATBALF.PS.txt | first to last record (key ends): ...2008/01/0001 to ...2008/01/0001 | lowest to highest key (key ends): ...2008/01/0001 to ...2008/01/0001 | **EQUAL** |  |
| RC4 | distinct keys of disclosure_group = its row count | distinct keys: 15 | rows: 15 | **EQUAL** |  |
| RC4 | lowest and highest key of disclosure_group = first and last record of AWS.M2.CARDDEMO.DISCGRP.PS.txt | first to last record (key ends): ...AULT/01/0001 to ...DARD/05/0001 | lowest to highest key (key ends): ...AULT/01/0001 to ...DARD/05/0001 | **EQUAL** |  |
| RC5 | rows written to interest_transaction = records of the exported SYSTRAN generation | NOT AVAILABLE (interest_run 7 is LOADED: not calculated on the tables) | exported file: 1 | **NOT AVAILABLE** |  |
| RC5 | sum of tran_amt in interest_transaction = sum of TRAN-AMT of the exported SYSTRAN generation | NOT AVAILABLE (interest_run 7 is LOADED: not calculated on the tables) | exported file: -3.00 | **NOT AVAILABLE** |  |
| RC5 | sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL + interest of the last account (not added, INT-Q04) | exported file: -3.00 | balance change + interest of the last account: -3.00 | **EQUAL** | 0.00 + -3.00 |
| RC5 | transactions recorded by the calculation = records of the exported SYSTRAN generation | NOT AVAILABLE (not recorded) | exported file: 1 | **NOT AVAILABLE** |  |
| RC5 | sum of TRAN-AMT recorded by the calculation = sum of TRAN-AMT of the exported SYSTRAN generation | NOT AVAILABLE (not recorded) | exported file: -3.00 | **NOT AVAILABLE** |  |
| RC5 | sum of ACCT-CURR-BAL recorded after the calculation = sum of ACCT-CURR-BAL of the exported ACCTDATA | NOT AVAILABLE (not recorded) | exported file: -300.00 | **NOT AVAILABLE** |  |
| RC6 | ACCTDATA unloaded again after the write-back equals the account table (count and the five sums of RC2) | NOT AVAILABLE (needs a mainframe unload after the write-back; cannot be shown off-host) | NOT AVAILABLE (needs a mainframe unload after the write-back; cannot be shown off-host) | **NOT AVAILABLE** |  |

## Profile of the unloaded files (TL4)

Nothing found.

| Counted | Gap or rule | Count |
|---|---|---|
| accounts with several cards | G08, INT-H03 | 0 |
| accounts with letter and digit type codes | G06 | 0 |
| distinct type codes | G06 | 01 |
| minus zero amounts | G03 | 0 |
| F-signed amounts | G02 | 0 |
| records with a FILLER that is not blank | E4 | 0 |
| accounts that are not active | INT-H04 | 0 |
| balances without a rate of their group and without a DEFAULT rate | INT-R08 | 0 |
| balances without an account | INT-R08 | 0 |
| balances on the DEFAULT rate (information, not a finding) | INT-R03 | 0 |
