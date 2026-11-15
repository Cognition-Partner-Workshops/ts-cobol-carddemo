# interest-loader: `loadExtractsStep` (ticket 05, INT-T05)

Replaces the four replica tables from the four unloaded files and records the reconciliation figures of the
load in `carddemo.interest_run` (architecture.md sections 2, 3, 5, 6; strategy items TL3, TL4, RC1-RC4).

## Open decisions this step implements as proposed

| Decision | Proposed option built here | Where |
|---|---|---|
| REL-01 | A: file bridge at the dataset boundary - the step reads unloaded files, never VSAM | `ExtractReader`, `ExtractFile` |
| REL-02 | A: the mainframe file is the system of record; each table is a copy replaced for every run (TRUNCATE and INSERT) | `ExtractLoader`, `jdbc/ReplicaTableWriter` |
| REL-08 | A: host format = cp037 text and C/D signs (unconfirmed); F-signed and minus-zero amounts are accepted and counted (G02, G03) | `ExtractFormat.HOST` -> `CodePage.host()`, `ExtractCheck` |

All three are OPEN in `modernization/design/decisions-release.md`. Another answer changes `ExtractFormat` (REL-08)
or removes this step (REL-01, REL-02); the calculation is not touched.

## What it does, in order

1. Inserts the `interest_run` row of the run (`status = LOADING`, `parm_date`, `started_at` from the run clock).
2. Reads and checks **all four files before any table is touched** (TL3): every record is decoded by `record-codec`
   and encoded again. To the reject list go: a record the codec refuses (E1, E2, E3/G07, wrong length), a record
   that does not come back byte for byte for another reason than G02/G03, a second record with the same key (RC4).
   One reject: the reject list is written, no table is changed, `interest_run.status = FAILED`, the step ends FAILED
   with `InterestRunStoppedException(RECORD_REJECTED)`.
3. Per table, in one transaction (architecture.md section 5): `TRUNCATE`, the `INSERT`s, then the figures of the
   table compared with the figures of the file (RC1 count, RC2 sums and hash total, RC4 distinct keys), the rows
   read back and encoded again against the file (RC3), and the `loaded_*` columns of `interest_run`.
   Order: account, card_xref, tran_cat_balance, disclosure_group.
4. Profiles the loaded data for TL4 (counts only) and sets `status = LOADED`.

A failure inside step 3 rolls back that table (it holds what it held before) and sets `FAILED`; tables replaced
earlier in the same load stay replaced - that is the one-table unit of work of the architecture. Restart repeats
the step; loading twice leaves the same rows.

## Job parameters and outputs

| Parameter | Use |
|---|---|
| `parmDate` | `interest_run.parm_date` |
| `inputDir` | directory of the four files: `AWS.M2.CARDDEMO.{ACCTDATA,CARDXREF,TCATBALF,DISCGRP}.PS` + `.txt` in the fixture format |
| `format` | `fixture` (one record per line, ISO-8859-1) or `host` (fixed-length binary, TL2) |
| `outputDir` | only to place the reject list beside it |
| `rejectDir` (optional) | directory of the reject list; default `<outputDir>/../rejects` |

* Reject list: `<rejectDir>/loadExtractsStep.rejects.txt`, one line per reject: file, record number (from 1), field,
  reason, tab separated. Never a value or a record image. A load without rejects removes it.
* Job execution context: `interest.runId` = `interest_run.run_id` of the run, for the steps that follow.
* Application log: run id, table, counts, sums, profile counts. No account id, card number or amount of a record.

The step bean is `loadExtractsStep` (`LoadExtractsStepConfig`). Putting it in front of `calcInterestStep` in
`interestCalcJob` belongs to the tickets that own `batch/InterestJobConfig` (06, 09).
