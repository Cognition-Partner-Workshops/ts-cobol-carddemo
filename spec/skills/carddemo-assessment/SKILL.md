---
name: carddemo-assessment
description: Conventions the CardDemo assessment used for the inventory, complexity score, wave plan, rule citations and rule test names. Use when you write or review spec/assessment (entries, inventory.json, waves.json), harness/assessment.py, or a rule table or rule test under spec/specification and spec/tests/rules.
---

# CardDemo assessment conventions

These conventions are what the assessment on `baseline/sdd-2610` actually applied. The code that enforces them
is `spec/harness/assessment.py` and `spec/tests/test_assessment.py`. If you change a convention, change all three.

## Build and check

    cd spec
    python3 -m harness.assessment          # entries/<group>.json -> inventory.json + waves.json
    python3 -m pytest -q tests/test_assessment.py

Never edit `inventory.json` or `waves.json` by hand. Edit `assessment/entries/<group>.json` and rebuild. The groups are
`batch`, `cics`, `db2`, `ims-mq` and `utility`. When a program appears in more than one group, the first group in that
order keeps the entry, the others go into `also_in`, and the merge logs a `dedupe` issue.
Nothing under `app/` changes during an assessment.

## Inventory entry (`inventory.json` -> `programs[]`)

`inventory.json` has these top-level keys: `baseline`, `complexity` (the formula text), `counts` (programs per group), `programs`,
`members` (every file under `app/` with `covered_by`, `source` = `group` or `merge`, and `unreferenced`), `coverage`,
`issues` (taken from the group `issues` / `unresolved` / `open_points` plus the merge's own `dedupe`, `complexity` and
`coverage` checks) and `groups` (each group's metadata).

Every program entry has these fields:

| field | meaning |
|---|---|
| `name` | member name, 1-8 chars, `^[A-Z][A-Z0-9#@$]{0,7}$` |
| `type` | free text, for example `COBOL batch program`, `COBOL CICS online`, `COBOL batch subprogram`, `system-utility`. A type ending in `subprogram` is pulled into its caller's wave |
| `group` | owning group, see above |
| `source` | repo-relative path, for example `app/cbl/CBACT01C.cbl` |
| `jcl` | JCL step references `jcl/<JOB>.jcl:<JOB>.<STEP>`. A bare path counts as the member name |
| `copybooks` | COPY members (Db2 `dcl` members go in `dcl`) |
| `datasets` | list of `{dd, dsn, file, access}` objects (see below) |
| `cics_transactions` | transaction ids from `app/csd/CARDDEMO.CSD` |
| `external_calls` | distinct CALL / XCTL / LINK targets. Plain names, or `{target, kind}`. Targets the merge cannot resolve through `CDEMO-TO-PROGRAM` are skipped |
| `called_by` | callers, when known |
| `unreferenced` | `true` when no JCL, CSD entry, caller or other member names it |
| `complexity` | the complexity object below, or `null` / `{}` when unscored |
| `wave` | filled in by the merge: `Wave <n>: <JOB>[ (deferred)]`, or `""` when the program is in no wave |

Groups may add other fields (`evidence`, `db2_tables`, `sql_statements`, `ims`, `mq_queues`, `bms_mapset`, `jcl_steps`, ...).
The merge passes them through unchanged.

Each dataset object holds the JCL DD name (`dd`), the catalogued DSN (`dsn`), the COBOL FD / SELECT name (`file`) and
`access`, which is `read`, `write` or `read-write`. Non-VSAM resources use `cics_file`, `ims_db`, `queue` or `gsam_dbd` in place of
`dsn`. Graph edges come from `access`: `read` gives dataset -> program, `write`/`insert`/`delete`/`replace` gives
program -> dataset, and a blank `access` counts as read. Plain-string datasets are still accepted but produce no edges, so
new entries use objects.

## Complexity

    score = paragraphs + 2 x file_io_verbs + 3 x external_calls
    band  = low (score < 40) | medium (40-79) | high (score >= 80)

`complexity` object: `{"paragraphs", "file_io_verbs", "external_calls", "score", "band"}`. Optional detail fields are
`paragraph_basis`, `io_breakdown`, `sql_detail`, `dli_detail`, `mq_detail` and `note`.

- `paragraphs`: paragraph and section labels in Area A of the PROCEDURE DIVISION of the member itself. Procedure
  copybooks are not expanded.
- `file_io_verbs`: occurrences in the PROCEDURE DIVISION, with comments and literals excluded, of:
  - COBOL file verbs `OPEN CLOSE READ WRITE REWRITE DELETE START`;
  - EXEC CICS file commands `READ WRITE REWRITE DELETE UNLOCK STARTBR READNEXT READPREV RESETBR ENDBR` (the cics group also
    counted `WRITEQ READQ DELETEQ`);
  - executable EXEC SQL (`SELECT INTO`, `FETCH`, cursor `OPEN`/`CLOSE`, `INSERT`, `UPDATE`, `DELETE`). `INCLUDE`,
    `DECLARE` and `WHENEVER` are not counted;
  - DL/I calls (`EXEC DLI`, `CALL 'CBLTDLI'`, including SCHD/TERM/CHKP);
  - MQ API calls `MQOPEN MQGET MQPUT MQPUT1 MQCLOSE`.
- `external_calls`: distinct targets of `CALL`, `EXEC CICS XCTL` and `EXEC CICS LINK`. CBLTDLI and the MQ API are counted
  as I/O, not here. `XCTL PROGRAM(CDEMO-TO-PROGRAM)` resolves to the literal targets MOVEd into it.
- System utilities (`type: system-utility`, for example IDCAMS, SORT, IEBGENER, IKJEFT01) are not scored:
  `complexity` is `null`. Non-program members kept as entries (a copybook or proc) are not scored either.
- The merge recomputes every score and band. A mismatch is logged as a `complexity` issue in `inventory.json`.

## Wave plan (`waves.json`)

- The order is the `batch_path` job order in `spec/manifest.yaml` (POSTTRAN, INTCALC, CREASTMT), giving one wave per job:
  `Wave <n>: <JOB>`.
- Members are the `EXEC PGM=` programs of that job's JCL, plus any static CALL target whose `type` ends in `subprogram`.
- `after` lists each job named in `reads_output_of`, together with the datasets this wave reads that the earlier job writes.
  For example, INTCALC comes after POSTTRAN through `ACCTDATA` and `TCATBALF`.
- A job with `optional: true` becomes a deferred wave: its name gets ` (deferred)` and its status is
  `deferred: inventoried, not specified or proven yet`. CREASTMT (CBSTM03A/B) is deferred. Other waves get
  `specify and prove`.
- System utilities (`group: utility`) and members marked `unreferenced` are never wave members. Every other program
  keeps `wave: ""`. Only the batch path is specified and proven. Db2, IMS and MQ are inventory only, and CICS is inventory
  and dependency graph only.

## Rule citations and test names

- Cite a rule's source as `app/cbl/<PROGRAM>.cbl:<first>-<last>`, using the upper-case program name and the inclusive line range of
  the statements that implement the rule, for example `app/cbl/CBACT04C.cbl:<first>-<last>` with the real line numbers (the template placeholder is `app/cbl/PROGRAM.cbl:120-134`).
- Name a rule test `test_<program>_r<NN>_<short_name>`, where `<program>` is the lower-case program name, `NN` is the two-digit rule number
  matching `R<NN>` in the rule table, and `<short_name>` is snake_case. It lives in
  `spec/tests/rules/test_<program>.py`, for example
  `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r01_short_name`.
- The rule table row format and the green/amber rule come from `spec/specification/_template.md` and the
  `executable-specification-standard` skill.
