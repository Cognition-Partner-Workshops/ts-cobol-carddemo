# Step 1 fan-out - one analysis agent per module

Baseline: `main` at `a5da4ee6bb4261392ac0a44a0756b82914b97fe8`. Toolbox: mftb 0.2.1.
The module map (`store/catalog.json`, `store/modules.json`) was committed on
`demo/01-intake` at `2eccddc` and frozen before the fan-out; no child changed it.

| Item | Value |
|---|---|
| Workflow | `carddemo-step1-module-fan-out` |
| Workflow run id | `wfr-8c7fd831cb134545805ba82d66778cae` |
| Script | `modernization/legacy/fan-out-workflow.py` (commit `f6619ba`) |
| Module ids read from | `store/modules.json` on `origin/demo/01-intake` |
| Concurrency | at most 4 agents at a time |
| Retry rule | one retry per failed agent, then "not analyzed" |
| Result | 4 of 4 modules analyzed, all on the first attempt; no retry was used |

## Per module

Counts are the ones each child returned: programs in `store/<MODULE>/programs.json`
(kind = program), edges in `store/<MODULE>/traces.json`, open findings in
`store/<MODULE>/findings.json`.

| Module | Result | Attempts | Child session | Branch | PR (base `demo/01-intake`) | Programs | Edges | Open asks |
|---|---|---|---|---|---|---|---|---|
| CARDDEMO | analyzed | 1 | https://partner-workshops.devinenterprise.com/sessions/53ec663e09eb41769c73301bbaaca29b | `demo/01-intake-CARDDEMO` | https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/168 | 32 | 211 | 236 |
| AUTH | analyzed | 1 | https://partner-workshops.devinenterprise.com/sessions/ff3713ddd27e41fab65e3a844c561ffd | `demo/01-intake-AUTH` | https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/167 | 8 | 59 | 135 |
| TRANTYPE | analyzed | 1 | https://partner-workshops.devinenterprise.com/sessions/0271b03f471d4082b9942609603f4371 | `demo/01-intake-TRANTYPE` | https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/166 | 4 | 50 | 65 |
| VSAMMQ | analyzed | 1 | https://partner-workshops.devinenterprise.com/sessions/405d4451323d4f9aa18c72f320838fb4 | `demo/01-intake-VSAMMQ` | https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/165 | 2 | 18 | 34 |
| **Total** | 4 analyzed | | | | | 46 | 338 | 470 |

## Modules not analyzed

None.

## What each child said it did not cover

Reported by the children, repeated here unchanged in substance:

* **CARDDEMO** - assembler (`COBDATFT`, `MVSWAIT`) and PL/I (`CBFEECAL`) logic; BMS field attributes; members outside its roots (`COPAUS0C`, `COTRTLIC`, `COTRTUPC`, `COCRDSEC`, jobs `CBPAUP0J`, `MNTTRDB2`, `TRANEXTR`). 11 of 24 dynamic XCTL sites and 19 environment-profile questions stay open. Per-transaction pages were written for 2 entry points and 2 menus only.
* **AUTH** - the 16 non-AUTH jobs of CA-7 table CARDDEMO and the five Control-M folders; the `DBPAUTP0` IMS unload utility step (PARM form mftb does not parse); BMS and assembler source. The XCTL at `COPAUS0C.cbl:674` stays an open ask.
* **TRANTYPE** - four scheduler tables that run no TRANTYPE job got a one-line summary; BMS macro source is outside its roots. The `CBTRNDYN` dynamic CALL and dynamic SQL and the `COTRTUPC` commarea-driven XCTL stay open asks.
* **VSAMMQ** - batch-chain analysis was not run because the module has no JCL, procedure or scheduler input.

Edges between modules are not visible to any single child; they are derived in
`modernization/legacy/call-graph.md`.

## Combining

Each child branch changed files only under `store/<MODULE>/` and
`store/reports/<MODULE>/` (checked with `git diff --name-only` against
`demo/01-intake`). They were combined on `demo/01-intake` with
`git merge --squash`, one commit per child:

| Child branch | Child head | Commit on `demo/01-intake` |
|---|---|---|
| `demo/01-intake-CARDDEMO` | `752195d` | `daa9390` |
| `demo/01-intake-AUTH` | `c018c2c` | `3be89a7` |
| `demo/01-intake-TRANTYPE` | `fe07761` | `3c3f5a6` |
| `demo/01-intake-VSAMMQ` | `7bf3ae3` | `7a7eb36` |

A squash was used on purpose: a true merge would put the child commits on the
base branch and GitHub would then close the four child PRs as merged. With the
squash the four PRs stay open for a person to review. No PR was merged and
nothing was pushed to `main`.

The estate report was then generated with `python3 -m mftb report-estate`:
`store/estate.json` and `store/reports/estate.html` (4 modules scanned, 10
shared, 0 unplaced, 26 connections between modules, 53 open asks).
