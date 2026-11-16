# Classified report - replay lab, interest stream

Written by `make lab-gate` (`lab/replay.py gate`). Observed is the legacy COBOL run, target is the Java job of `java/interest`, expected is what the rulebook derives. Values are decoded with the copybook codec of `tools/parity`.

> **Not SME sign-off.** Every decision of `modernization/rules/interest/decisions.md` is a question an agent raised from the code. A value classed UNRESOLVED is waiting for one of them.

| | |
|---|---|
| Baseline of the estate | `6face58c092d254a9b89ce8bbfbddce561720a40` (app/ and fixtures/ unchanged since) |
| Frozen business date | 2022-07-05 10:00:00.00 (clock of the run), PARM 2022071800 |
| Toolchain | GnuCOBOL 3.1.2.0, Open COBOL ESQL 1.4.0, Python (estate) 3.10.12, Java (target run) 21.0.13, PostgreSQL 15.20, Python (lab) 3.10.12, Docker 29.9.2 |
| Cases | 7 golden, 6 probe |

## Gate

```
lab-gate: replay of the interest stream (INTCALC -> CBACT04C against java/interest)
golden i01-baseline                 MATCH 48  INTENDED_CHANGE 0  UNRESOLVED 8  UNCLASSIFIED 0
golden i02-tie-half-cent            MATCH 45  INTENDED_CHANGE 0  UNRESOLVED 11  UNCLASSIFIED 0
golden i03-zero-rate-skipped        MATCH 25  INTENDED_CHANGE 0  UNRESOLVED 6  UNCLASSIFIED 0
golden i04-default-group-fallback   MATCH 25  INTENDED_CHANGE 0  UNRESOLVED 5  UNCLASSIFIED 0
golden i05-multi-category           MATCH 36  INTENDED_CHANGE 0  UNRESOLVED 8  UNCLASSIFIED 0
golden i06-negative-balance         MATCH 14  INTENDED_CHANGE 0  UNRESOLVED 16  UNCLASSIFIED 0
golden i07-nontie-rounding          MATCH 48  INTENDED_CHANGE 0  UNRESOLVED 8  UNCLASSIFIED 0
golden total: 7 cases, 303 compared values: MATCH 241  INTENDED_CHANGE 0  UNRESOLVED 62  UNCLASSIFIED 0
golden rule coverage: 13 of 20 rules exercised
probe  p01-tie-ladder               MATCH 77  INTENDED_CHANGE 0  UNRESOLVED 21  UNCLASSIFIED 0
probe  p02-tie-negative             MATCH 36  INTENDED_CHANGE 0  UNRESOLVED 34  UNCLASSIFIED 0
probe  p03-last-account             MATCH 104  INTENDED_CHANGE 0  UNRESOLVED 18  UNCLASSIFIED 0
probe  p04-no-disclosure-group      MATCH 50  INTENDED_CHANGE 0  UNRESOLVED 7  UNCLASSIFIED 0
probe  p05-negative-balance         MATCH 37  INTENDED_CHANGE 0  UNRESOLVED 33  UNCLASSIFIED 0
probe  p06-sub-cent-inactive        MATCH 38  INTENDED_CHANGE 0  UNRESOLVED 32  UNCLASSIFIED 0
probe total: 6 cases, 487 compared values: MATCH 342  INTENDED_CHANGE 0  UNRESOLVED 145  UNCLASSIFIED 0
probe rule coverage: 15 of 20 rules exercised
all total: 13 cases, 790 compared values: MATCH 583  INTENDED_CHANGE 0  UNRESOLVED 207  UNCLASSIFIED 0
rule coverage: 15 of 20 rules exercised by these cases; not exercised: INT-R08, INT-H02, INT-H03, INT-H05, INT-H06
rule verdicts, legacy: no rule violated in any case
rule verdicts, target: no rule violated in any case
gate: PASS (UNCLASSIFIED = 0)
```

## What the lab does not prove

- Sequential batch replay only: one clean run of INTCALC per case on small synthetic inputs. No rerun, no restart after a failure, no volume.
- No CICS: the online programs that read and write the same account file are not run.
- No concurrency: one run at a time; nothing else touches the files or the tables during a run.
- GnuCOBOL arithmetic, not Enterprise COBOL: the observed side is GnuCOBOL 3 with -std=ibm; intermediate results, truncation and overflow under the host compiler options are not shown.
- No real Db2 and no VSAM: indexed files are GnuCOBOL files, the target database is PostgreSQL 15 in a container.
- ASCII with zoned signs as the off-host harness writes them, not cp037 host files; a frozen clock.
- A rule verdict is a check against the rulebook's own evaluator, which an agent wrote from the code; it is not a business owner's view. Rules no case exercised have no verdict.

## golden: i01-baseline

# i01-baseline Two accounts in groups GOLD (12.00 %) and STANDARD (18.50 %). 1000.00 * 12.00 / 1200 = 10.00 and 2400.00 * 18.50 / 1200 = 37.00; both exact. CBACT04C writes one interest transaction per account and adds it to ACCT-CURR-BAL.

Expected from: modernization/rules/interest/expected/i01-baseline.json (status OPEN). Compared values: 56 - MATCH 48  INTENDED_CHANGE 0  UNRESOLVED 8  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (50 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000002001/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002001/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002001/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002002/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002002/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002002/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002002 | `ACCT-CURR-BAL` | 2400.00 | 2400.00 | 2400.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002002 | `ACCT-CURR-CYC-CREDIT` | 2400.00 | 2400.00 | 2400.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## golden: i02-tie-half-cent

# i02-tie-half-cent  (the rounding-tie case) Account 2003: 1235.50 * 12.00 / 1200 = 12.355 — exactly half a cent. CBACT04C's COMPUTE has no ROUNDED phrase, so COBOL truncates to **12.35**; a half-even (banker's) reference rounds to **12.36**.  Account 2004: 1234.50 gives 12.345, where half-even also yields 12.34, so only account 2003 diverges. `make parity-naive CASE=i02-tie-half-cent` must fail on TRAN-AMT and ACCT-CURR-BAL for account 2003 only.

Expected from: modernization/rules/interest/expected/i02-tie-half-cent.json (status OPEN). Compared values: 56 - MATCH 45  INTENDED_CHANGE 0  UNRESOLVED 11  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (50 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000002003/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002003/01/0001 | `TRAN-AMT` | 12.35 | 12.35 | 12.35 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002003/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002003/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002004/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002004/01/0001 | `TRAN-AMT` | 12.34 | 12.34 | 12.34 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002004/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002004/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002003 | `ACCT-CURR-BAL` | 1247.85 | 1247.85 | 1247.85 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002004 | `ACCT-CURR-BAL` | 1234.50 | 1234.50 | 1234.50 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002004 | `ACCT-CURR-CYC-CREDIT` | 1234.50 | 1234.50 | 1234.50 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-Q01 Interest is cut off at the cent, not rounded | QUIRK | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## golden: i03-zero-rate-skipped

# i03-zero-rate-skipped Account 2005 has a payment-category balance (type 02) whose disclosure rate is 0.00 %, and a purchase balance at 12.00 %.  CBACT04C skips the zero-rate category (no transaction written) and posts interest only for the purchase category.

Expected from: modernization/rules/interest/expected/i03-zero-rate-skipped.json (status OPEN). Compared values: 31 - MATCH 25  INTENDED_CHANGE 0  UNRESOLVED 6  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (25 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000002005/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002005/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002005/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002005 | `ACCT-CURR-BAL` | 700.00 | 700.00 | 700.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002005 | `ACCT-CURR-CYC-CREDIT` | 1000.00 | 1000.00 | 1000.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002005 | `ACCT-CURR-CYC-DEBIT` | 300.00 | 300.00 | 300.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R04 A zero rate means no interest | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## golden: i04-default-group-fallback

# i04-default-group-fallback Account 2006 belongs to group PLATINUM, which has no DISCGRP rows.  CBACT04C reports "DISCLOSURE GROUP RECORD MISSING / TRY WITH DEFAULT GROUP CODE" and uses the DEFAULT group: 1000.00 * 21.00 / 1200 = 17.50.

Expected from: modernization/rules/interest/expected/i04-default-group-fallback.json (status OPEN). Compared values: 30 - MATCH 25  INTENDED_CHANGE 0  UNRESOLVED 5  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (25 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000002006/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002006/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002006/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002006 | `ACCT-CURR-BAL` | 1000.00 | 1000.00 | 1000.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002006 | `ACCT-CURR-CYC-CREDIT` | 1000.00 | 1000.00 | 1000.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R03 Fallback to the DEFAULT disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## golden: i05-multi-category

# i05-multi-category Account 2007 (GOLD) carries two balances: purchases 1000.00 at 12.00 % (10.00) and cash advances 500.00 at 18.00 % (7.50).  Two interest transactions are written and the account balance grows by the 17.50 total.

Expected from: modernization/rules/interest/expected/i05-multi-category.json (status OPEN). Compared values: 44 - MATCH 36  INTENDED_CHANGE 0  UNRESOLVED 8  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (38 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000002007/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002007/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002007/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002007/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002007/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002007/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002007 | `ACCT-CURR-BAL` | 1500.00 | 1500.00 | 1500.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002007 | `ACCT-CURR-CYC-CREDIT` | 1500.00 | 1500.00 | 1500.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## golden: i06-negative-balance

# i06-negative-balance Account 2008 has a credit (negative) purchase balance of -300.00 at 12.00 %. CBACT04C computes -3.00 and writes a negative interest transaction, reducing the account balance.

Expected from: modernization/rules/interest/expected/i06-negative-balance.json (status OPEN). Compared values: 30 - MATCH 14  INTENDED_CHANGE 0  UNRESOLVED 16  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (25 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000002008/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-DESC` | Int. for a/c 00000002008 | Int. for a/c 00000002008 | Int. for a/c 00000002008 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-AMT` | -3.00 | -3.00 | -3.00 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-CARD-NUM` | 4000000000002008 | 4000000000002008 | 4000000000002008 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002008/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002008 | `ACCT-CURR-BAL` | -300.00 | -300.00 | -300.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002008 | `ACCT-CURR-CYC-DEBIT` | 300.00 | 300.00 | 300.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-Q05 Credit balances earn negative interest | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## golden: i07-nontie-rounding

# i07-nontie-rounding Sub-cent remainders that are not ties: 1000.33 * 18.50 / 1200 = 15.42175 and 777.77 * 21.00 / 1200 = 13.610975.  Truncation and half-even both give 15.42 and 13.61, so this case passes under `make parity` and `make parity-naive`.

Expected from: modernization/rules/interest/expected/i07-nontie-rounding.json (status OPEN). Compared values: 56 - MATCH 48  INTENDED_CHANGE 0  UNRESOLVED 8  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (50 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000002009/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002009/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002009/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002010/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002010/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000002010/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002010 | `ACCT-CURR-BAL` | 777.77 | 777.77 | 777.77 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000002010 | `ACCT-CURR-CYC-CREDIT` | 777.77 | 777.77 | 777.77 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-Q01 Interest is cut off at the cent, not rounded | QUIRK | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## probe: p01-tie-ladder

Four balances of one account at 12.00 %, each exactly half a cent of interest: 0.015, 0.025, 12.355 and 12.365. Truncation, half-up and half-even give three different totals (24.74, 24.78, 24.76), so the account balance shows which one an engine used. A second account closes the file.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 98 - MATCH 77  INTENDED_CHANGE 0  UNRESOLVED 21  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000003001/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0001 | `TRAN-AMT` | 0.01 | 0.01 | 0.01 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0002 | `TRAN-AMT` | 0.02 | 0.02 | 0.02 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0003 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0003 | `TRAN-AMT` | 12.35 | 12.35 | 12.35 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0003 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0003 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0004 | `TRAN-ID` | 2022071800000004 | 2022071800000004 | 2022071800000004 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0004 | `TRAN-AMT` | 12.36 | 12.36 | 12.36 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0004 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003001/01/0004 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003002/01/0001 | `TRAN-ID` | 2022071800000005 | 2022071800000005 | 2022071800000005 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003002/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003002/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003001 | `ACCT-CURR-BAL` | 2524.74 | 2524.74 | 2524.74 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003002 | `ACCT-CURR-BAL` | 900.00 | 900.00 | 900.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-Q01 Interest is cut off at the cent, not rounded | QUIRK | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## probe: p02-tie-negative

Credit balances whose interest is exactly minus half a cent: -0.025 and -12.355. Cutting off towards zero gives -0.02 and -12.35; rounding down (floor) or half-up away from zero would give -0.03 and -12.36.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 70 - MATCH 36  INTENDED_CHANGE 0  UNRESOLVED 34  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000003003/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-DESC` | Int. for a/c 00000003003 | Int. for a/c 00000003003 | Int. for a/c 00000003003 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-AMT` | -12.35 | -12.35 | -12.35 | INT-Q01, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-CARD-NUM` | 4000000000003003 | 4000000000003003 | 4000000000003003 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-DESC` | Int. for a/c 00000003003 | Int. for a/c 00000003003 | Int. for a/c 00000003003 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-AMT` | -0.02 | -0.02 | -0.02 | INT-Q01, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-CARD-NUM` | 4000000000003003 | 4000000000003003 | 4000000000003003 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003003/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003004/01/0001 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003004/01/0001 | `TRAN-AMT` | 7.70 | 7.70 | 7.70 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003004/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003004/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003003 | `ACCT-CURR-BAL` | -1250.37 | -1250.37 | -1250.37 | INT-Q01, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003004 | `ACCT-CURR-BAL` | 500.00 | 500.00 | 500.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-Q01 Interest is cut off at the cent, not rounded | QUIRK | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-Q05 Credit balances earn negative interest | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## probe: p03-last-account

Three accounts with balances; the last one has two balances and non-zero cycle amounts, and it is not the last record of the account file (account 3013 follows it there with no balance). The legacy program updates 3010 and 3011, writes the transactions of 3012 but leaves its balance and cycle amounts as they were, and never touches 3013.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 122 - MATCH 104  INTENDED_CHANGE 0  UNRESOLVED 18  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000003010/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003010/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003010/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003011/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003011/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003011/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003011/01/0002 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003011/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003011/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003012/01/0001 | `TRAN-ID` | 2022071800000004 | 2022071800000004 | 2022071800000004 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003012/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003012/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003012/01/0002 | `TRAN-ID` | 2022071800000005 | 2022071800000005 | 2022071800000005 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003012/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003012/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003012 | `ACCT-CURR-BAL` | 3000.00 | 3000.00 | 3000.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003012 | `ACCT-CURR-CYC-CREDIT` | 50.00 | 50.00 | 50.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003012 | `ACCT-CURR-CYC-DEBIT` | 60.00 | 60.00 | 60.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## probe: p04-no-disclosure-group

Account 3020 has an empty group id. Both of its balances have no rate of their own, so each falls back to the group DEFAULT with the two messages in the job log: the purchase balance is charged at 21.00 %, the payment balance has the DEFAULT rate 0 and gets no transaction. Account 3021 closes the file.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 57 - MATCH 50  INTENDED_CHANGE 0  UNRESOLVED 7  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000003020/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003020/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003020/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003021/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003021/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003021/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003021 | `ACCT-CURR-BAL` | 100.00 | 100.00 | 100.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R03 Fallback to the DEFAULT disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R04 A zero rate means no interest | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## probe: p05-negative-balance

Account 3030 has a debit and a larger credit balance, so its interest nets to -12.50 and the account balance goes down. Account 3031 has no group and a credit balance of -300.10: DEFAULT rate, interest -5.25175 cut off towards zero to -5.25, and as the last account its balance is not changed.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 70 - MATCH 37  INTENDED_CHANGE 0  UNRESOLVED 33  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000003030/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-DESC` | Int. for a/c 00000003030 | Int. for a/c 00000003030 | Int. for a/c 00000003030 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-AMT` | -22.50 | -22.50 | -22.50 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-CARD-NUM` | 4000000000003030 | 4000000000003030 | 4000000000003030 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003030/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-DESC` | Int. for a/c 00000003031 | Int. for a/c 00000003031 | Int. for a/c 00000003031 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-AMT` | -5.25 | -5.25 | -5.25 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-CARD-NUM` | 4000000000003031 | 4000000000003031 | 4000000000003031 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003031/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003030 | `ACCT-CURR-BAL` | -512.50 | -512.50 | -512.50 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003031 | `ACCT-CURR-BAL` | -300.10 | -300.10 | -300.10 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R03 Fallback to the DEFAULT disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-Q01 Interest is cut off at the cent, not rounded | QUIRK | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-Q05 Credit balances earn negative interest | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |

## probe: p06-sub-cent-inactive

Balances of 0.50 and -0.50 at 12.00 % give +0.005 and -0.005, both cut off to zero: a 0.00 transaction is still written (INT-Q06), and the second one is a zero with a negative origin. The account is inactive (INT-H04) and is charged all the same. Account 3041 closes the file.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 70 - MATCH 38  INTENDED_CHANGE 0  UNRESOLVED 32  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED | balance 00000003040/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-H04, INT-Q02, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-SOURCE` | System | System | System | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-DESC` | Int. for a/c 00000003040 | Int. for a/c 00000003040 | Int. for a/c 00000003040 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-AMT` | 0.00 | 0.00 | 0.00 | INT-H04, INT-Q01, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-CARD-NUM` | 4000000000003040 | 4000000000003040 | 4000000000003040 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-H04, INT-Q03, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-H04, INT-Q03, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `interestTransactionWritten` | yes | yes | yes | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-H04, INT-Q02, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-SOURCE` | System | System | System | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-DESC` | Int. for a/c 00000003040 | Int. for a/c 00000003040 | Int. for a/c 00000003040 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-AMT` | 0.00 | 0.00 | -0.00 | INT-H04, INT-Q01, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-MERCHANT-NAME` |  |  |  | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-MERCHANT-CITY` |  |  |  | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-CARD-NUM` | 4000000000003040 | 4000000000003040 | 4000000000003040 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-H04, INT-Q03, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003040/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-H04, INT-Q03, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003041/01/0001 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-H04, INT-Q02, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003041/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | balance 00000003041/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED | account 00000003041 | `ACCT-CURR-BAL` | 10.00 | 10.00 | 10.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | CONFORMS | CONFORMS |
| INT-Q01 Interest is cut off at the cent, not rounded | QUIRK | CONFORMS | CONFORMS |
| INT-R06 Interest is posted as a system transaction | RULE | CONFORMS | CONFORMS |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | CONFORMS | CONFORMS |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | CONFORMS | CONFORMS |
| INT-R07 Interest is added to the account at the account break | RULE | CONFORMS | CONFORMS |
| INT-Q04 The last account on the file is never updated | QUIRK | CONFORMS | CONFORMS |
| INT-Q05 Credit balances earn negative interest | QUIRK | CONFORMS | CONFORMS |
| INT-Q06 Interest of 0.00 is still posted | QUIRK | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | CONFORMS | CONFORMS |
| INT-H04 Closed or expired accounts are charged like any other | HOLE | CONFORMS | CONFORMS |

