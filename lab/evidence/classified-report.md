# Classified report - replay lab, interest stream

Written by `make lab-gate` (`lab/replay.py gate`). Observed is the legacy COBOL run, target is the Java job of `java/interest`, expected is what the rulebook derives. Values are decoded with the copybook codec of `tools/parity`.

> **Not SME sign-off.** Every decision of `modernization/rules/interest/decisions.md` is a question an agent raised from the code. A value classed UNRESOLVED is waiting for one of them.

| | |
|---|---|
| Baseline of the estate | `6face58c092d254a9b89ce8bbfbddce561720a40` (app/ and fixtures/ unchanged since) |
| Frozen business date | 2022-07-05 10:00:00.00 (clock of the run), PARM 2022071800 |
| Toolchain | GnuCOBOL 3.1.2.0, Open COBOL ESQL 1.4.0, Python (estate) 3.10.12, Java (target run) 21.0.13, PostgreSQL 15.20, Python (lab) 3.10.12, Docker 29.9.2 |
| Target options | none (the defaults of java/interest) |
| Cases | 7 golden, 11 probe |

## Gate

```
lab-gate: replay of the interest stream (INTCALC -> CBACT04C against java/interest)
golden i01-baseline                 MATCH 48  INTENDED_CHANGE 0  UNRESOLVED 8 (engines agree 8, engines differ 0)  UNCLASSIFIED 0
golden i02-tie-half-cent            MATCH 45  INTENDED_CHANGE 0  UNRESOLVED 11 (engines agree 11, engines differ 0)  UNCLASSIFIED 0
golden i03-zero-rate-skipped        MATCH 25  INTENDED_CHANGE 0  UNRESOLVED 6 (engines agree 6, engines differ 0)  UNCLASSIFIED 0
golden i04-default-group-fallback   MATCH 25  INTENDED_CHANGE 0  UNRESOLVED 5 (engines agree 5, engines differ 0)  UNCLASSIFIED 0
golden i05-multi-category           MATCH 36  INTENDED_CHANGE 0  UNRESOLVED 8 (engines agree 8, engines differ 0)  UNCLASSIFIED 0
golden i06-negative-balance         MATCH 14  INTENDED_CHANGE 0  UNRESOLVED 16 (engines agree 16, engines differ 0)  UNCLASSIFIED 0
golden i07-nontie-rounding          MATCH 48  INTENDED_CHANGE 0  UNRESOLVED 8 (engines agree 8, engines differ 0)  UNCLASSIFIED 0
golden total: 7 cases, 303 compared values: MATCH 241  INTENDED_CHANGE 0  UNRESOLVED 62 (engines agree 62, engines differ 0)  UNCLASSIFIED 0
golden rule coverage: 13 of 18 rules a replay can exercise were exercised by these cases; not exercised: INT-Q06, INT-R08, INT-H02, INT-H03, INT-H04
probe  p01-tie-ladder               MATCH 77  INTENDED_CHANGE 0  UNRESOLVED 21 (engines agree 21, engines differ 0)  UNCLASSIFIED 0
probe  p02-tie-negative             MATCH 36  INTENDED_CHANGE 0  UNRESOLVED 34 (engines agree 34, engines differ 0)  UNCLASSIFIED 0
probe  p03-last-account             MATCH 104  INTENDED_CHANGE 0  UNRESOLVED 18 (engines agree 18, engines differ 0)  UNCLASSIFIED 0
probe  p04-no-disclosure-group      MATCH 50  INTENDED_CHANGE 0  UNRESOLVED 7 (engines agree 7, engines differ 0)  UNCLASSIFIED 0
probe  p05-negative-balance         MATCH 37  INTENDED_CHANGE 0  UNRESOLVED 33 (engines agree 33, engines differ 0)  UNCLASSIFIED 0
probe  p06-sub-cent-inactive        MATCH 38  INTENDED_CHANGE 0  UNRESOLVED 32 (engines agree 32, engines differ 0)  UNCLASSIFIED 0
probe  p07-stop-missing-account     MATCH 3  INTENDED_CHANGE 0  UNRESOLVED 0 (engines agree 0, engines differ 0)  UNCLASSIFIED 0
probe  p08-stop-missing-card        MATCH 3  INTENDED_CHANGE 0  UNRESOLVED 0 (engines agree 0, engines differ 0)  UNCLASSIFIED 0
probe  p09-stop-missing-default-rate MATCH 3  INTENDED_CHANGE 0  UNRESOLVED 0 (engines agree 0, engines differ 0)  UNCLASSIFIED 0
probe  p10-run-twice                MATCH 72  INTENDED_CHANGE 0  UNRESOLVED 92 (engines agree 92, engines differ 0)  UNCLASSIFIED 0
probe  p11-two-cards                MATCH 0  INTENDED_CHANGE 0  UNRESOLVED 83 (engines agree 0, engines differ 83)  UNCLASSIFIED 0
probe total: 11 cases, 743 compared values: MATCH 423  INTENDED_CHANGE 0  UNRESOLVED 320 (engines agree 237, engines differ 83)  UNCLASSIFIED 0
probe rule coverage: 18 of 18 rules a replay can exercise were exercised by these cases
all total: 18 cases, 1046 compared values: MATCH 664  INTENDED_CHANGE 0  UNRESOLVED 382 (engines agree 299, engines differ 83)  UNCLASSIFIED 0
rule coverage: 18 of 18 rules a replay can exercise were exercised by these cases
open questions no case can settle: 2 - INT-H05 (needs host evidence: the result of an overflow depends on compiler options the estate does not show); INT-H06 (needs an answer from operations: the order of posting, the file reloads and the interest run is outside a single job)
rule verdicts, legacy: no rule violated in any case
rule verdicts, target: no rule violated in any case
gate condition 1, no compared value is UNCLASSIFIED: passed
gate condition 2, neither engine VIOLATES a rule in any case: passed
gate: PASS
```

## What the lab does not prove

- Sequential batch replay only: one clean run of INTCALC per case on small synthetic inputs. No rerun, no restart after a failure, no volume.
- No CICS: the online programs that read and write the same account file are not run.
- No concurrency: one run at a time; nothing else touches the files or the tables during a run.
- GnuCOBOL arithmetic, not Enterprise COBOL: the observed side is GnuCOBOL 3 with -std=ibm; intermediate results, truncation and overflow under the host compiler options are not shown.
- No real Db2 and no VSAM: indexed files are GnuCOBOL files, the target database is PostgreSQL 15 in a container.
- ASCII with zoned signs as the off-host harness writes them, not cp037 host files; a frozen clock.
- A rule verdict is a check against the rulebook's own evaluator, which an agent wrote from the code; it is not a business owner's view. Rules no case exercised have no verdict.
- A run that stops is compared on how it ended, its return code and its job log only. The files it leaves behind are not compared, and neither is a restart.
- A probe of an open question (INT-H02 second run, INT-H03 several cards) shows what each engine does today, not what is right. For INT-H03 the legacy side is GnuCOBOL, which may differ from the host on the duplicate-key read.

## golden: i01-baseline

# i01-baseline Two accounts in groups GOLD (12.00 %) and STANDARD (18.50 %). 1000.00 * 12.00 / 1200 = 10.00 and 2400.00 * 18.50 / 1200 = 37.00; both exact. CBACT04C writes one interest transaction per account and adds it to ACCT-CURR-BAL.

Expected from: modernization/rules/interest/expected/i01-baseline.json (status OPEN). Compared values: 56 - MATCH 48  INTENDED_CHANGE 0  UNRESOLVED 8 (engines agree 8, engines differ 0)  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (50 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000002001/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002001/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002001/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002002/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002002/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002002/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002002 | `ACCT-CURR-BAL` | 2400.00 | 2400.00 | 2400.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002002 | `ACCT-CURR-CYC-CREDIT` | 2400.00 | 2400.00 | 2400.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: modernization/rules/interest/expected/i02-tie-half-cent.json (status OPEN). Compared values: 56 - MATCH 45  INTENDED_CHANGE 0  UNRESOLVED 11 (engines agree 11, engines differ 0)  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (50 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000002003/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002003/01/0001 | `TRAN-AMT` | 12.35 | 12.35 | 12.35 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002003/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002003/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002004/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002004/01/0001 | `TRAN-AMT` | 12.34 | 12.34 | 12.34 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002004/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002004/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002003 | `ACCT-CURR-BAL` | 1247.85 | 1247.85 | 1247.85 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002004 | `ACCT-CURR-BAL` | 1234.50 | 1234.50 | 1234.50 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002004 | `ACCT-CURR-CYC-CREDIT` | 1234.50 | 1234.50 | 1234.50 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: modernization/rules/interest/expected/i03-zero-rate-skipped.json (status OPEN). Compared values: 31 - MATCH 25  INTENDED_CHANGE 0  UNRESOLVED 6 (engines agree 6, engines differ 0)  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (25 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000002005/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002005/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002005/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002005 | `ACCT-CURR-BAL` | 700.00 | 700.00 | 700.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002005 | `ACCT-CURR-CYC-CREDIT` | 1000.00 | 1000.00 | 1000.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002005 | `ACCT-CURR-CYC-DEBIT` | 300.00 | 300.00 | 300.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: modernization/rules/interest/expected/i04-default-group-fallback.json (status OPEN). Compared values: 30 - MATCH 25  INTENDED_CHANGE 0  UNRESOLVED 5 (engines agree 5, engines differ 0)  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (25 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000002006/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002006/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002006/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002006 | `ACCT-CURR-BAL` | 1000.00 | 1000.00 | 1000.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002006 | `ACCT-CURR-CYC-CREDIT` | 1000.00 | 1000.00 | 1000.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: modernization/rules/interest/expected/i05-multi-category.json (status OPEN). Compared values: 44 - MATCH 36  INTENDED_CHANGE 0  UNRESOLVED 8 (engines agree 8, engines differ 0)  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (38 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000002007/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002007/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002007/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002007/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002007/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002007/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002007 | `ACCT-CURR-BAL` | 1500.00 | 1500.00 | 1500.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002007 | `ACCT-CURR-CYC-CREDIT` | 1500.00 | 1500.00 | 1500.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: modernization/rules/interest/expected/i06-negative-balance.json (status OPEN). Compared values: 30 - MATCH 14  INTENDED_CHANGE 0  UNRESOLVED 16 (engines agree 16, engines differ 0)  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (25 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-DESC` | Int. for a/c 00000002008 | Int. for a/c 00000002008 | Int. for a/c 00000002008 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-AMT` | -3.00 | -3.00 | -3.00 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-CARD-NUM` | 4000000000002008 | 4000000000002008 | 4000000000002008 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002008/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002008 | `ACCT-CURR-BAL` | -300.00 | -300.00 | -300.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002008 | `ACCT-CURR-CYC-DEBIT` | 300.00 | 300.00 | 300.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: modernization/rules/interest/expected/i07-nontie-rounding.json (status OPEN). Compared values: 56 - MATCH 48  INTENDED_CHANGE 0  UNRESOLVED 8 (engines agree 8, engines differ 0)  UNCLASSIFIED 0. Legacy rerun against the golden files: PASS (50 decoded fields). Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000002009/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002009/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002009/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002010/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002010/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000002010/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002010 | `ACCT-CURR-BAL` | 777.77 | 777.77 | 777.77 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000002010 | `ACCT-CURR-CYC-CREDIT` | 777.77 | 777.77 | 777.77 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 98 - MATCH 77  INTENDED_CHANGE 0  UNRESOLVED 21 (engines agree 21, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000003001/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0001 | `TRAN-AMT` | 0.01 | 0.01 | 0.01 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0002 | `TRAN-AMT` | 0.02 | 0.02 | 0.02 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0003 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0003 | `TRAN-AMT` | 12.35 | 12.35 | 12.35 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0003 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0003 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0004 | `TRAN-ID` | 2022071800000004 | 2022071800000004 | 2022071800000004 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0004 | `TRAN-AMT` | 12.36 | 12.36 | 12.36 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0004 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003001/01/0004 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003002/01/0001 | `TRAN-ID` | 2022071800000005 | 2022071800000005 | 2022071800000005 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003002/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003002/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003001 | `ACCT-CURR-BAL` | 2524.74 | 2524.74 | 2524.74 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003002 | `ACCT-CURR-BAL` | 900.00 | 900.00 | 900.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 70 - MATCH 36  INTENDED_CHANGE 0  UNRESOLVED 34 (engines agree 34, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-DESC` | Int. for a/c 00000003003 | Int. for a/c 00000003003 | Int. for a/c 00000003003 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-AMT` | -12.35 | -12.35 | -12.35 | INT-Q01, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-CARD-NUM` | 4000000000003003 | 4000000000003003 | 4000000000003003 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-DESC` | Int. for a/c 00000003003 | Int. for a/c 00000003003 | Int. for a/c 00000003003 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-AMT` | -0.02 | -0.02 | -0.02 | INT-Q01, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-CARD-NUM` | 4000000000003003 | 4000000000003003 | 4000000000003003 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003003/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003004/01/0001 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003004/01/0001 | `TRAN-AMT` | 7.70 | 7.70 | 7.70 | INT-Q01 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003004/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003004/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003003 | `ACCT-CURR-BAL` | -1250.37 | -1250.37 | -1250.37 | INT-Q01, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003004 | `ACCT-CURR-BAL` | 500.00 | 500.00 | 500.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 122 - MATCH 104  INTENDED_CHANGE 0  UNRESOLVED 18 (engines agree 18, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000003010/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003010/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003010/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003011/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003011/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003011/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003011/01/0002 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003011/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003011/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003012/01/0001 | `TRAN-ID` | 2022071800000004 | 2022071800000004 | 2022071800000004 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003012/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003012/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003012/01/0002 | `TRAN-ID` | 2022071800000005 | 2022071800000005 | 2022071800000005 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003012/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003012/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003012 | `ACCT-CURR-BAL` | 3000.00 | 3000.00 | 3000.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003012 | `ACCT-CURR-CYC-CREDIT` | 50.00 | 50.00 | 50.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003012 | `ACCT-CURR-CYC-DEBIT` | 60.00 | 60.00 | 60.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 57 - MATCH 50  INTENDED_CHANGE 0  UNRESOLVED 7 (engines agree 7, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000003020/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003020/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003020/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003021/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003021/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003021/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003021 | `ACCT-CURR-BAL` | 100.00 | 100.00 | 100.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 70 - MATCH 37  INTENDED_CHANGE 0  UNRESOLVED 33 (engines agree 33, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000003030/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-DESC` | Int. for a/c 00000003030 | Int. for a/c 00000003030 | Int. for a/c 00000003030 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-AMT` | -22.50 | -22.50 | -22.50 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-CARD-NUM` | 4000000000003030 | 4000000000003030 | 4000000000003030 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003030/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-SOURCE` | System | System | System | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-DESC` | Int. for a/c 00000003031 | Int. for a/c 00000003031 | Int. for a/c 00000003031 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-AMT` | -5.25 | -5.25 | -5.25 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-CARD-NUM` | 4000000000003031 | 4000000000003031 | 4000000000003031 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003031/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003030 | `ACCT-CURR-BAL` | -512.50 | -512.50 | -512.50 | INT-Q05 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003031 | `ACCT-CURR-BAL` | -300.10 | -300.10 | -300.10 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 70 - MATCH 38  INTENDED_CHANGE 0  UNRESOLVED 32 (engines agree 32, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-H04, INT-Q02, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-SOURCE` | System | System | System | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-DESC` | Int. for a/c 00000003040 | Int. for a/c 00000003040 | Int. for a/c 00000003040 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-AMT` | 0.00 | 0.00 | 0.00 | INT-H04, INT-Q01, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-CARD-NUM` | 4000000000003040 | 4000000000003040 | 4000000000003040 | INT-H04, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-H04, INT-Q03, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-H04, INT-Q03, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `interestTransactionWritten` | yes | yes | yes | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-H04, INT-Q02, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-SOURCE` | System | System | System | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-DESC` | Int. for a/c 00000003040 | Int. for a/c 00000003040 | Int. for a/c 00000003040 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-AMT` | 0.00 | 0.00 | -0.00 | INT-H04, INT-Q01, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-MERCHANT-NAME` |  |  |  | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-MERCHANT-CITY` |  |  |  | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-CARD-NUM` | 4000000000003040 | 4000000000003040 | 4000000000003040 | INT-H04, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-H04, INT-Q03, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003040/01/0002 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-H04, INT-Q03, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003041/01/0001 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-H04, INT-Q02, INT-Q05, INT-Q06 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003041/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | balance 00000003041/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | account 00000003041 | `ACCT-CURR-BAL` | 10.00 | 10.00 | 10.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |

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

## probe: p07-stop-missing-account

INT-R08. Account 3051 has a balance and a card but no record on the account file. The run stops at its balance: 'ACCOUNT NOT FOUND', 'ERROR READING ACCOUNT FILE', the file status, 'ABENDING PROGRAM'. Account 3050 before it has already been charged. Compared: how the run ended, the return code and the job log.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 3 - MATCH 3  INTENDED_CHANGE 0  UNRESOLVED 0 (engines agree 0, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: abend - return code 12; target: abend - exit code 12.

- both runs stopped. Compared: how the run ended, the return code and the job log up to 'ABENDING PROGRAM'. Return code: legacy 12 (STEP15 of rc.json; 12 is the value of the off-host abend stub, U0999 on the host), target 12 (exit code of the Java process (no rc.json is written when the job stops)).
- not compared - the files a stopped run leaves behind (a restart is INT-H02): legacy left 1 interest transaction(s) and an account file of 2 record(s); target exported no file.
- not compared - 6 line(s) of the legacy job log after 'ABENDING PROGRAM': the line of the off-host abend stub and warnings of the GnuCOBOL runtime.
- job log line(s) both engines write that the rulebook's expectation does not list: 'FILE STATUS IS: NNNN0023'. The rulebook's own lines are all there, in order, so the lab does not count this as a violated rule; it is a gap of the rulebook's evaluator (INT-T08 describes the line).

Every compared value is MATCH.

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | NOT JUDGED | NOT JUDGED |
| INT-R05 Monthly interest formula | RULE | NOT JUDGED | NOT JUDGED |
| INT-R06 Interest is posted as a system transaction | RULE | NOT JUDGED | NOT JUDGED |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-R07 Interest is added to the account at the account break | RULE | NOT JUDGED | NOT JUDGED |
| INT-Q04 The last account on the file is never updated | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-R08 A missing record or a file error stops the run | RULE | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | NOT JUDGED | NOT JUDGED |

## probe: p08-stop-missing-card

INT-R08. Account 3061 is on the account file but has no card in the cross-reference. The run stops at its balance: 'ACCOUNT NOT FOUND', 'ERROR READING XREF FILE', the file status, 'ABENDING PROGRAM'. Compared: how the run ended, the return code and the job log.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 3 - MATCH 3  INTENDED_CHANGE 0  UNRESOLVED 0 (engines agree 0, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: abend - return code 12; target: abend - exit code 12.

- both runs stopped. Compared: how the run ended, the return code and the job log up to 'ABENDING PROGRAM'. Return code: legacy 12 (STEP15 of rc.json; 12 is the value of the off-host abend stub, U0999 on the host), target 12 (exit code of the Java process (no rc.json is written when the job stops)).
- not compared - the files a stopped run leaves behind (a restart is INT-H02): legacy left 1 interest transaction(s) and an account file of 3 record(s); target exported no file.
- not compared - 6 line(s) of the legacy job log after 'ABENDING PROGRAM': the line of the off-host abend stub and warnings of the GnuCOBOL runtime.
- job log line(s) both engines write that the rulebook's expectation does not list: 'FILE STATUS IS: NNNN0023'. The rulebook's own lines are all there, in order, so the lab does not count this as a violated rule; it is a gap of the rulebook's evaluator (INT-T08 describes the line).

Every compared value is MATCH.

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | NOT JUDGED | NOT JUDGED |
| INT-R05 Monthly interest formula | RULE | NOT JUDGED | NOT JUDGED |
| INT-R06 Interest is posted as a system transaction | RULE | NOT JUDGED | NOT JUDGED |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-R07 Interest is added to the account at the account break | RULE | NOT JUDGED | NOT JUDGED |
| INT-Q04 The last account on the file is never updated | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-R08 A missing record or a file error stops the run | RULE | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | NOT JUDGED | NOT JUDGED |

## probe: p09-stop-missing-default-rate

INT-R08 after INT-R03. Account 3071 is in group SILVER, which has no rate, and the rate table has no DEFAULT row either. The fallback is tried, then the run stops: 'ERROR READING DEFAULT DISCLOSURE GROUP', the file status, 'ABENDING PROGRAM'. Compared: how the run ended, the return code and the job log.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 3 - MATCH 3  INTENDED_CHANGE 0  UNRESOLVED 0 (engines agree 0, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: abend - return code 12; target: abend - exit code 12.

- both runs stopped. Compared: how the run ended, the return code and the job log up to 'ABENDING PROGRAM'. Return code: legacy 12 (STEP15 of rc.json; 12 is the value of the off-host abend stub, U0999 on the host), target 12 (exit code of the Java process (no rc.json is written when the job stops)).
- not compared - the files a stopped run leaves behind (a restart is INT-H02): legacy left 1 interest transaction(s) and an account file of 3 record(s); target exported no file.
- not compared - 6 line(s) of the legacy job log after 'ABENDING PROGRAM': the line of the off-host abend stub and warnings of the GnuCOBOL runtime.
- job log line(s) both engines write that the rulebook's expectation does not list: 'FILE STATUS IS: NNNN0023'. The rulebook's own lines are all there, in order, so the lab does not count this as a violated rule; it is a gap of the rulebook's evaluator (INT-T08 describes the line).

Every compared value is MATCH.

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | CONFORMS | CONFORMS |
| INT-R02 Rate comes from the account's disclosure group | RULE | NOT JUDGED | NOT JUDGED |
| INT-R03 Fallback to the DEFAULT disclosure group | RULE | CONFORMS | CONFORMS |
| INT-R05 Monthly interest formula | RULE | NOT JUDGED | NOT JUDGED |
| INT-R06 Interest is posted as a system transaction | RULE | NOT JUDGED | NOT JUDGED |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-R07 Interest is added to the account at the account break | RULE | NOT JUDGED | NOT JUDGED |
| INT-Q04 The last account on the file is never updated | QUIRK | NOT JUDGED | NOT JUDGED |
| INT-R08 A missing record or a file error stops the run | RULE | CONFORMS | CONFORMS |
| INT-H01 Fees are announced but not calculated | HOLE | NOT JUDGED | NOT JUDGED |

## probe: p10-run-twice

INT-H02, an open question, not a rule. The job is run a second time on the files the first run left. Nothing stops it: both runs write the same transaction ids and the interest is added to the balance twice (1000.00, 1010.00, 1020.00). Every amount is exact to the cent, so rounding plays no part. This shows what each engine does today, not what is right.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status OPEN). Compared values: 164 - MATCH 72  INTENDED_CHANGE 0  UNRESOLVED 92 (engines agree 92, engines differ 0)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: normal end, return code 0.

> **INT-H02 - what the engines do today, not what is right.** Open question, not a rule: nothing in the job stops a second run in the same cycle. The lab shows what each engine does today when the job is run twice on the files the first run left. That is what the engines do, not what is right; what is right waits for the business answer to INT-H02.
> Legacy today: run 1: normal end, return code 0; 3 interest transaction(s) totalling 22.00 on card(s) 4000000000003090, 4000000000003091, 4000000000003092; job log ends 'END OF EXECUTION OF PROGRAM CBACT04C'; run 2: normal end, return code 0; 3 interest transaction(s) totalling 22.00 on card(s) 4000000000003090, 4000000000003091, 4000000000003092; job log ends 'END OF EXECUTION OF PROGRAM CBACT04C'.
> Target today: run 1: normal end, return code 0; 3 interest transaction(s) totalling 22.00 on card(s) 4000000000003090, 4000000000003091, 4000000000003092; job log ends 'END OF EXECUTION OF PROGRAM CBACT04C'; run 2: normal end, return code 0; 3 interest transaction(s) totalling 22.00 on card(s) 4000000000003090, 4000000000003091, 4000000000003092; job log ends 'END OF EXECUTION OF PROGRAM CBACT04C'.

- Second run of the target: the Java job reloads its tables from the files on every run, so the lab gives the account file its first run exported to its second run (the write-back of REL-03). The legacy job reads the file its first run rewrote. Generation names of SYSTRAN are not compared.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines agree | run 1: balance 00000003090/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: balance 00000003090/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: balance 00000003090/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: balance 00000003091/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: balance 00000003091/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: balance 00000003091/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: balance 00000003092/01/0001 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: balance 00000003092/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: balance 00000003092/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 1: account 00000003092 | `ACCT-CURR-BAL` | 700.00 | 700.00 | 700.00 | INT-Q04 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: run | `how the run ended` | normal end, return code 0 | normal end, return code 0 | normal end, return code 0 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: run | `job log (SYSOUT INTCALC.STEP15)` | START OF EXECUTION OF PROGRAM CBACT04C<br>000000030900100010000010000{<br>000000030910100010000005000{<br>000000030920100010000007000{<br>END OF EXECUTION OF PROGRAM CBACT04C | START OF EXECUTION OF PROGRAM CBACT04C<br>000000030900100010000010000{<br>000000030910100010000005000{<br>000000030920100010000007000{<br>END OF EXECUTION OF PROGRAM CBACT04C | START OF EXECUTION OF PROGRAM CBACT04C<br>000000030900100010000010000{<br>000000030910100010000005000{<br>000000030920100010000007000{<br>END OF EXECUTION OF PROGRAM CBACT04C | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: run | `return codes (rc.json)` | {"INTCALC": {"maxrc": 0, "steps": {"STEP05": 0, "STEP10": 0, "STEP15": 0, "STEP20": 0, "STEP25": 0, "STEP30": 0}}} | {"INTCALC": {"maxrc": 0, "steps": {"STEP05": 0, "STEP10": 0, "STEP15": 0, "STEP20": 0, "STEP25": 0, "STEP30": 0}}} | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: run | `number of interest transactions` | 3 | 3 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-ID` | 2022071800000001 | 2022071800000001 | 2022071800000001 | INT-Q02, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-SOURCE` | System | System | System | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-DESC` | Int. for a/c 00000003090 | Int. for a/c 00000003090 | Int. for a/c 00000003090 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-AMT` | 10.00 | 10.00 | 10.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-CARD-NUM` | 4000000000003090 | 4000000000003090 | 4000000000003090 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: balance 00000003090/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-ID` | 2022071800000002 | 2022071800000002 | 2022071800000002 | INT-Q02, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-SOURCE` | System | System | System | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-DESC` | Int. for a/c 00000003091 | Int. for a/c 00000003091 | Int. for a/c 00000003091 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-AMT` | 5.00 | 5.00 | 5.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-CARD-NUM` | 4000000000003091 | 4000000000003091 | 4000000000003091 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: balance 00000003091/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `interestTransactionWritten` | yes | yes | yes | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-ID` | 2022071800000003 | 2022071800000003 | 2022071800000003 | INT-Q02, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-TYPE-CD` | 01 | 01 | 01 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-CAT-CD` | 0005 | 0005 | 0005 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-SOURCE` | System | System | System | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-DESC` | Int. for a/c 00000003092 | Int. for a/c 00000003092 | Int. for a/c 00000003092 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-AMT` | 7.00 | 7.00 | 7.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | 000000000 | 000000000 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-MERCHANT-NAME` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-MERCHANT-CITY` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-MERCHANT-ZIP` |  |  |  | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-CARD-NUM` | 4000000000003092 | 4000000000003092 | 4000000000003092 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: balance 00000003092/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | 2022-07-05-10.00.00.000000 | INT-Q03, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-ID` | 00000003090 | 00000003090 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-ACTIVE-STATUS` | Y | Y | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-CURR-BAL` | 1020.00 | 1020.00 | 1020.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-CREDIT-LIMIT` | 5000.00 | 5000.00 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-CASH-CREDIT-LIMIT` | 2500.00 | 2500.00 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-OPEN-DATE` | 2019-03-15 | 2019-03-15 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-EXPIRAION-DATE` | 2026-12-31 | 2026-12-31 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-REISSUE-DATE` | 2026-12-31 | 2026-12-31 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-CURR-CYC-CREDIT` | 0.00 | 0.00 | 0.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-CURR-CYC-DEBIT` | 0.00 | 0.00 | 0.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-ADDR-ZIP` | 10115 | 10115 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003090 | `ACCT-GROUP-ID` | GOLD | GOLD | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-ID` | 00000003091 | 00000003091 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-ACTIVE-STATUS` | Y | Y | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-CURR-BAL` | 510.00 | 510.00 | 510.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-CREDIT-LIMIT` | 5000.00 | 5000.00 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-CASH-CREDIT-LIMIT` | 2500.00 | 2500.00 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-OPEN-DATE` | 2019-03-15 | 2019-03-15 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-EXPIRAION-DATE` | 2026-12-31 | 2026-12-31 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-REISSUE-DATE` | 2026-12-31 | 2026-12-31 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-CURR-CYC-CREDIT` | 0.00 | 0.00 | 0.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-CURR-CYC-DEBIT` | 0.00 | 0.00 | 0.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-ADDR-ZIP` | 10115 | 10115 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003091 | `ACCT-GROUP-ID` | GOLD | GOLD | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-ID` | 00000003092 | 00000003092 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-ACTIVE-STATUS` | Y | Y | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-CURR-BAL` | 700.00 | 700.00 | 700.00 | INT-Q04, INT-H02 | both engines agree, on behaviour whose decision is still open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-CREDIT-LIMIT` | 5000.00 | 5000.00 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-CASH-CREDIT-LIMIT` | 2500.00 | 2500.00 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-OPEN-DATE` | 2019-03-15 | 2019-03-15 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-EXPIRAION-DATE` | 2026-12-31 | 2026-12-31 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-REISSUE-DATE` | 2026-12-31 | 2026-12-31 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-CURR-CYC-CREDIT` | 0.00 | 0.00 | 0.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-CURR-CYC-DEBIT` | 0.00 | 0.00 | 0.00 | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-ADDR-ZIP` | 10115 | 10115 | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |
| UNRESOLVED - engines agree | run 2: account 00000003092 | `ACCT-GROUP-ID` | GOLD | GOLD | (the rulebook states no value) | INT-H02 | both engines agree on what a second run does; whether a second run may charge again is INT-H02, which is open |

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
| INT-H02 Nothing prevents charging the same interest twice | HOLE | CONFORMS | CONFORMS |

## probe: p11-two-cards

INT-H03, an open question, not a rule. Account 3081 has two cards. The source does not say which card number its interest transaction gets, or whether the run stops. This shows what each engine does today, not what is right; the legacy side is GnuCOBOL, which may differ from the host on the duplicate-key read.

Expected from: rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input (status UNRESOLVED). Compared values: 83 - MATCH 0  INTENDED_CHANGE 0  UNRESOLVED 83 (engines agree 0, engines differ 83)  UNCLASSIFIED 0. No golden file: the observed side is this run of the legacy lane. Legacy: normal end, return code 0; target: abend - exit code 12.

> **INT-H03 - what the engines do today, not what is right.** Open question, not a rule: the source does not say which card number an account with several cards gets, or whether the run stops. The lab shows what each engine does today. That is what the engines do, not what is right. The legacy side here is GnuCOBOL: on the read of the cross-reference by account id it may differ from the host, where a duplicate key can come back with another file status and stop the program. Only a host run or the business answer to INT-H03 settles it.
> Legacy today: normal end, return code 0; 3 interest transaction(s) totalling 22.00 on card(s) 4000000000003080, 4000000000003081, 4000000000003082; job log ends 'END OF EXECUTION OF PROGRAM CBACT04C'.
> Target today: abend - exit code 12; 0 interest transaction(s) totalling 0.00; job log ends 'ABENDING PROGRAM'.

| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |
|---|---|---|---|---|---|---|---|
| UNRESOLVED - engines differ | run | `how the run ended` | normal end, return code 0 | abend - exit code 12 | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | run | `job log (SYSOUT INTCALC.STEP15)` | START OF EXECUTION OF PROGRAM CBACT04C<br>000000030800100010000010000{<br>000000030810100010000005000{<br>000000030820100010000007000{<br>END OF EXECUTION OF PROGRAM CBACT04C | START OF EXECUTION OF PROGRAM CBACT04C<br>000000030800100010000010000{<br>000000030810100010000005000{<br>INT-H03: THE ACCOUNT HAS SEVERAL CARDS<br>ABENDING PROGRAM | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | run | `return codes (rc.json)` | {"INTCALC": {"maxrc": 0, "steps": {"STEP05": 0, "STEP10": 0, "STEP15": 0, "STEP20": 0, "STEP25": 0, "STEP30": 0}}} | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | run | `number of interest transactions` | 3 | 0 | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `interestTransactionWritten` | yes | no | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-ID` | 2022071800000001 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-TYPE-CD` | 01 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-CAT-CD` | 0005 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-SOURCE` | System | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-DESC` | Int. for a/c 00000003080 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-AMT` | 10.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-MERCHANT-NAME` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-MERCHANT-CITY` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-MERCHANT-ZIP` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-CARD-NUM` | 4000000000003080 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003080/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `interestTransactionWritten` | yes | no | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-ID` | 2022071800000002 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-TYPE-CD` | 01 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-CAT-CD` | 0005 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-SOURCE` | System | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-DESC` | Int. for a/c 00000003081 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-AMT` | 5.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-MERCHANT-NAME` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-MERCHANT-CITY` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-MERCHANT-ZIP` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-CARD-NUM` | 4000000000003081 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003081/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `interestTransactionWritten` | yes | no | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-ID` | 2022071800000003 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-TYPE-CD` | 01 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-CAT-CD` | 0005 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-SOURCE` | System | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-DESC` | Int. for a/c 00000003082 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-AMT` | 7.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-MERCHANT-ID` | 000000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-MERCHANT-NAME` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-MERCHANT-CITY` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-MERCHANT-ZIP` |  | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-CARD-NUM` | 4000000000003082 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | balance 00000003082/01/0001 | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-ID` | 00000003080 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-ACTIVE-STATUS` | Y | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-CURR-BAL` | 1010.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-CREDIT-LIMIT` | 5000.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-CASH-CREDIT-LIMIT` | 2500.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-OPEN-DATE` | 2019-03-15 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-EXPIRAION-DATE` | 2026-12-31 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-REISSUE-DATE` | 2026-12-31 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-CURR-CYC-CREDIT` | 0.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-CURR-CYC-DEBIT` | 0.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-ADDR-ZIP` | 10115 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003080 | `ACCT-GROUP-ID` | GOLD | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-ID` | 00000003081 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-ACTIVE-STATUS` | Y | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-CURR-BAL` | 505.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-CREDIT-LIMIT` | 5000.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-CASH-CREDIT-LIMIT` | 2500.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-OPEN-DATE` | 2019-03-15 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-EXPIRAION-DATE` | 2026-12-31 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-REISSUE-DATE` | 2026-12-31 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-CURR-CYC-CREDIT` | 0.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-CURR-CYC-DEBIT` | 0.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-ADDR-ZIP` | 10115 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003081 | `ACCT-GROUP-ID` | GOLD | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-ID` | 00000003082 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-ACTIVE-STATUS` | Y | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-CURR-BAL` | 700.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-CREDIT-LIMIT` | 5000.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-CASH-CREDIT-LIMIT` | 2500.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-OPEN-DATE` | 2019-03-15 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-EXPIRAION-DATE` | 2026-12-31 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-REISSUE-DATE` | 2026-12-31 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-CURR-CYC-CREDIT` | 0.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-CURR-CYC-DEBIT` | 0.00 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-ADDR-ZIP` | 10115 | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | account 00000003082 | `ACCT-GROUP-ID` | GOLD | (none) | (the rulebook does not determine this case) | INT-H03 | the source does not determine the outcome of this case and INT-H03 is open: the engines give different answers to it, or this value follows from that |
| UNRESOLVED - engines differ | case | `outcome not determined by the rulebook (INT-H03)` | normal end, return code 0 | abend - exit code 12 | account 00000003081 has 2 cards | INT-H03 | account 00000003081 has 2 cards: the source does not determine the outcome and INT-H03 is open |

| Rule | Type | Legacy | Target |
|---|---|---|---|
| INT-R01 One interest decision per category balance | RULE | NOT DETERMINED | NOT DETERMINED |
| INT-R02 Rate comes from the account's disclosure group | RULE | NOT DETERMINED | NOT DETERMINED |
| INT-R05 Monthly interest formula | RULE | NOT DETERMINED | NOT DETERMINED |
| INT-R06 Interest is posted as a system transaction | RULE | NOT DETERMINED | NOT DETERMINED |
| INT-Q02 Transaction id is the job's PARM date plus a counter | QUIRK | NOT DETERMINED | NOT DETERMINED |
| INT-Q03 Timestamps are the moment the job ran | QUIRK | NOT DETERMINED | NOT DETERMINED |
| INT-R07 Interest is added to the account at the account break | RULE | NOT DETERMINED | NOT DETERMINED |
| INT-Q04 The last account on the file is never updated | QUIRK | NOT DETERMINED | NOT DETERMINED |
| INT-H01 Fees are announced but not calculated | HOLE | NOT DETERMINED | NOT DETERMINED |
| INT-H03 Account with more than one card | HOLE | NOT DETERMINED | NOT DETERMINED |

