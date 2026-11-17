# Hand-off: posting stream (Track A)

> **PROPOSED - not approved.** Written by an agent in step 7 from the source on `demo/06-lab` at `d39ebac`. No
> posting rule, design, data map, Java module or parity target exists. Nothing was run for posting.
> **Owner who must confirm: the target architect.** Plan and owners: `modernization/design/tracks.md`.

| | |
|---|---|
| Chain | `posting` in `tools/runjcl/chains.json` |
| Jobs and programs | `app/jcl/POSTTRAN.jcl` STEP15 -> `app/cbl/CBTRN02C.cbl` (731 lines); then `app/jcl/TRANREPT.jcl` STEP05R (IDCAMS REPRO), STEP05R (SORT), STEP10R -> `app/cbl/CBTRN03C.cbl` (649 lines) |
| Cases | seven, `fixtures/posting/p01` to `p07` (section 7) |
| Frozen clock | `2022-07-05 10:00:00.00` |
| Shared and frozen | `modernization/design/shared-layouts.md`, sections 1, 4 and 5 |
| Draft annex | `.agents/skills/carddemo-stream-posting/SKILL.md` - DRAFT, NOT PROVEN |

## 1. Reused unchanged from interest

| What | Where | How posting uses it |
|---|---|---|
| Schema | `db/postgres/02-interest.sql`: `carddemo.account`, `carddemo.card_xref`, `carddemo.tran_cat_balance` | same tables, same columns. Posting adds no column and alters none. Writing to `account` waits for SHR-01 |
| Mapping | `data-map-interest.md`: the rows of `CVACT01Y`, `CVACT03Y`, `CVTRA01Y`, `CVTRA05Y`; encoding rules E1-E6; gaps G01-G12 | cited by row id, not copied |
| Codec | `java/interest`, `com.carddemo.interest.codec` | the four shared layouts decode and encode as for interest. How a second module reaches it is SHR-02; no copy is made |
| Comparator | `tools/parity/compare.py posting <case> --actual <dir> --label java`; `tools/parity/layouts/REJTRAN.cpy` | already takes `posting`; the report dataset is compared as text |
| Legacy side | `make up`, `make build`, `make run CHAIN=posting`, `make parity CHAIN=posting` | unchanged |
| Lab | `lab/` (`make lab`, `make lab-gate`) | **runs interest only** (`lab/replay.py`, `lab/cases.py`). Posting reuses it as its regression check of interest, unchanged. A posting lab is not part of this hand-off |
| Shared skills | `carddemo-conversion-standards`, `carddemo-ticket-build`, `carddemo-ticket-verify`, `carddemo-pitfalls`, `carddemo-parity` | read-only. Per the draft annex: of 32 standards 12 apply unchanged, 18 need a posting citation, 2 leave a gap |
| Its annex | `.agents/skills/carddemo-stream-posting/SKILL.md` | **present, as a draft.** The starting point of the pilot, which proves or corrects it; nothing is built from it as it stands |
| Evidence layout | `java/<stream>/evidence/<ticket>/` with `test-output.txt`, `parity-report.md`, `report.md` | same form |

Not reused: the interest annex, `interest-*` skills, `parity-report.sh`, the targets `parity-java`, `recon-java`,
`sidebyside-java`, `parity-naive`, `rules`, `design` (all interest only), `carddemo.interest_transaction`,
`carddemo.interest_run`.

## 2. What is new

### 2.1 Layouts no stream has mapped

| Layout | Dataset | Length | Role | What is new about it |
|---|---|---|---|---|
| `CVTRA06Y` daily transaction | `DALYTRAN.PS` | 350 | input of `CBTRN02C`, sequential (`POSTTRAN.jcl:28-29`) | field for field the layout of `CVTRA05Y` under other names; needs its own data-map rows and codec layout. It is the driving file: one item is one record, in file order, no key |
| `CVTRA03Y` transaction type | `TRANTYPE.PS` -> `TRANTYPE.VSAM.KSDS` | 60 | read by `CBTRN03C:495` | lookup for the report; not found abends (`:496-500`). File image of a Db2 table (section 6) |
| `CVTRA04Y` transaction category | `TRANCATG.PS` -> `TRANCATG.VSAM.KSDS` | 60 | read by `CBTRN03C:505` | lookup by type and category; not found abends. Key holds `PIC 9(04)` (SHR-03) |
| `CVTRA07Y` report lines | `TRANREPT(+1)` | 133 | written by `CBTRN03C:345` | not a data record: seven print lines with literals and edited pictures `-ZZZ,ZZZ,ZZZ.ZZ`, `+ZZZ,ZZZ,ZZZ.ZZ`. No encoding rule E1-E6 covers an edited picture |
| `REJTRAN` (comparator only) | `DALYREJS(+1)` | 430 | written by `CBTRN02C:451` | the 350 bytes of the input record plus an 80-byte trailer: reason `9(04)` and text `X(76)` |
| `DATEPARM` (no copybook) | `DATEPARM` | 80 | read once by `CBTRN03C:221` | start date, blank, end date |

### 2.2 The reject file

`CBTRN02C` validates each record: card not in the cross-reference is reason 100 (`:383`), account not found 101
(`:395`), over the limit 102 (`:403-405`), after the expiry date 103 (`:414`). The limit and expiry tests both
run; when both fail 103 replaces 102. A rejected record goes to `DALYREJS` with its trailer and **the run goes
on**; it ends with return code 4 when anything was rejected (`:230`). Four of the seven cases record
`"STEP15": 4`. No shared standard has a path that rejects without stopping, or a normal end with a non-zero
code (draft annex, section 4, proposals 3 and 4).

### 2.3 The report

`TRANREPT.jcl` does three things before a line is printed, and two of them are not COBOL:

1. STEP05R, `REPROC`: unloads `TRANSACT.VSAM.KSDS` to `TRANSACT.BKUP(+1)` (`:23-33`).
2. STEP05R, SORT: keeps records whose `TRAN-PROC-TS` date (position 305, 10 bytes) lies between the literals
   `2022-01-01` and `2022-07-06`, and sorts by card number (position 263, 16 bytes, declared `ZD`) ascending,
   to `TRANSACT.DALY(+1)` (`:37-55`).
3. STEP10R, `CBTRN03C`: reads the date range again from `DATEPARM` (`:73-74`), prints a detail line per
   transaction with the type and category text, an account total at each change of card number, a page total
   every 20 lines (`CBTRN03C:282`) and a grand total.

New for any stream: a utility step that carries business selection and order; a date range given twice (JCL
literals and a file); a printed report as a parity output; `DISPLAY TRAN-RECORD` of every transaction into
SYSOUT (`:180`); the record area used after end of file, which adds the last amount to the totals a second time;
`NEXT SENTENCE` (`:177`). All are in the draft annex, section 4, as proposals 1, 2, 5, 6 and 7.

### 2.4 Other new work

* A posting rulebook (`modernization/rules/posting/`) with its own rule-id prefix - none is chosen.
* A posting design: architecture, data map for the layouts of 2.1, tables for what posting writes.
* A Java module `java/posting` and a parity entry point that runs the seven cases.
* Two jobs and two programs in one stream; `rc.json` holds both jobs with their setup steps.

## 3. Open asks

| Id | Ask | Why | Evidence | Owner | Blocks |
|---|---|---|---|---|---|
| PA-1 | Answer SHR-02: how `java/posting` reaches the record codec | four of posting's layouts are the frozen ones | `shared-layouts.md` 1.2 | target architect | the pilot |
| PA-2 | Answer SHR-01: who may write the three account columns | both streams update them | `CBTRN02C:547-554`, `CBACT04C:352-356` | data architect | the database steps of posting, not the pilot |
| PA-3 | State the order of POSTTRAN against INTCALC (SHR-07, INT-H06, ask A6) | section 5 | `CardDemo.ca7:70`, `CardDemo.controlm:69` | scheduler owner | any combined run; not the seven cases |
| PA-4 | State how `TRANTYPE` and `TRANCATG` are refreshed from the Db2 tables and in which order against TRANREPT | section 6 | `TRANEXTR.jcl`, `app/jcl/TRANTYPE.jcl:54-61` | scheduler owner | the choice between file and table lookup |
| PA-5 | Choose the rule-id prefix and where posting's design documents go | `skills-shared.md` section 5, "Assumed, not decided" | - | target architect | the rest of each program, not the pilot |
| PA-6 | Is TRANREPT scheduled at all, and after which job? | it is in neither scheduler export | search `TRANREPT` in `app/scheduler`: nothing | scheduler owner | the job boundary of the report |
| PA-7 | What does `OPEN OUTPUT` do to a `TRANSACT` cluster that already holds records, and is the `ZD` sort order on a `PIC X(16)` key the same on the host? | the off-host run cannot settle either | `CBTRN02C:256`, `TRANREPT.jcl:41, :46`; draft annex, "What the off-host run cannot settle" | mainframe application owner | host parity only |
| PA-8 | Which of the two date ranges is the parameter of the converted job, and what if they differ? | JCL literals and `DATEPARM` hold the same range twice | `TRANREPT.jcl:43-44, :73-74`, `CBTRN03C:220-243` | business owner | the rest of `CBTRN03C` |
| PA-9 | Decide the eleven proposals of the draft annex, section 4 | they are behaviours no shared standard covers | `.agents/skills/carddemo-stream-posting/SKILL.md` | target architect | the shared text; the pilot proposes, it does not edit |

## 4. Which stream updates the balances

| Balance | Posting | Interest |
|---|---|---|
| Category balance (`TCATBALF`) | creates and updates (`CBTRN02C:504-528`) | reads only (`CBACT04C:236, :326`) |
| Account balance `ACCT-CURR-BAL` | adds each posted amount (`:547`) | adds the interest of the account (`:352`) |
| Cycle credit and cycle debit | adds each amount to one of them (`:549, :551`); tests the limit against them (`:403-405`) | sets both to zero (`:353-354`) |

So in the data, posting comes first: interest computes on category balances that only posting (or the reload job
`TCATBALF`) creates, and it closes the cycle amounts that posting builds up and tests.

## 5. What `app/scheduler` proves about that order

Proven:

* `CardDemo.ca7:43-124`: `CLOSEFIL` triggers `CBPAUP0J`, which triggers `POSTTRAN` (`:70`), then `WAITSTEP`, then
  `OPENFIL`. POSTTRAN is in the CA 7 listing only.
* `CardDemo.controlm:64-89`: folder `MONTHLY-InterestCalculation` runs `CLOSEFIL`, `INTCALC` (`:69`), `COMBTRAN`,
  `WAITSTEP`, `OPENFIL`, each on the condition of the one before. INTCALC is in the Control-M export only.
* Both chains start with `CLOSEFIL` and end with `OPENFIL`, so neither runs while the other's files are open to
  CICS - if `CLOSEFIL` does close them (manifest U4, ask A7).

Not proven:

* **No condition, trigger or dataset trigger orders POSTTRAN against INTCALC**, and no file says which export is
  authoritative (manifest U2, ask A6; decision INT-H06, OPEN).
* `TRANREPT` is in neither export (ask PA-6).
* The day of the month, and whether the reload jobs `TCATBALF` and `ACCTFILE` run between the two.

The seven posting cases and the seven interest cases each start from their own input files, so no case depends
on the order. It stays ask PA-3 and register row SHR-07.

## 6. The link to Track B: the two lookup files

Proven by the source:

* `app/app-transaction-type-db2/jcl/TRANEXTR.jcl` STEP40 (`:65-84`) unloads `CARDDEMO.TRANSACTION_TYPE` to
  `TRANTYPE.PS`: type, description cast to `CHAR(50)`, eight characters `0`, 60 bytes, ordered by type. STEP50
  (`:95-115`) unloads `CARDDEMO.TRANSACTION_TYPE_CATEGORY` to `TRANCATG.PS`: type, category, description, four
  characters `0`, ordered by type and category. STEP10 to STEP30 first back the old files up and delete them.
* `TRANTYPE.PS` and `TRANCATG.PS` are 60 bytes with the fields in the order of `CVTRA03Y` and `CVTRA04Y`.
* `app/jcl/TRANTYPE.jcl:54-61` and `app/jcl/TRANCATG.jcl:54-61` copy those files into `TRANTYPE.VSAM.KSDS` and
  `TRANCATG.VSAM.KSDS`; `TRANREPT.jcl:69-72` gives these two clusters to `CBTRN03C`.
* `CardDemo.controlm:57-59`: `TRANEXTR` waits for the condition `WEEKLY-TransactionTypesDBRefresh-MNTTRDB2`,
  which `MNTTRDB2` sets (`:27-28`). `CardDemo.ca7:162, :244`: `TRANTYPE` and `TRANCATG` are triggered after
  `CLOSEFIL`.

Left open (ask PA-4):

* TRANEXTR writes the `.PS` files, not the clusters. Nothing orders the reload jobs `TRANTYPE` and `TRANCATG`
  after `TRANEXTR`: one is in Control-M, the others in CA 7.
* `MNTTRDYN` is in neither export, so a change made by the ad-hoc job has no proven path to the files.
* TRANEXTR does not run off-host (`DSNTIAUL`), is in no chain and has no recorded output. The posting fixtures
  supply `TRANTYPE.PS` and `TRANCATG.PS` directly, with spaces where TRANEXTR would write `0`.
* Whether the report's lookups later read the two tables directly instead of the files is a design choice for
  after both tracks; until then posting reads the files and Track B does not write them.

## 7. What posting must pass

For each case: `python3 tools/parity/compare.py posting <case> --actual <dir> --label java` exits 0, on five
datasets, two SYSOUT files and `rc.json`. Before and after, `make parity CHAIN=posting` stays green.

| Case | Proves (its README) | `STEP15` |
|---|---|---|
| `p01-baseline-mixed` | four valid transactions post; the report lists all four | 0 |
| `p02-invalid-card` | card with no cross-reference, reason 100 | 4 |
| `p03-account-missing` | account not found, reason 101 | 4 |
| `p04-overlimit-reject` | on the limit accepted, above it rejected, reason 102 | 4 |
| `p05-expired-account` | after the expiry date rejected, reason 103; on the date accepted | 4 |
| `p06-credits-new-category` | negative amounts go to the cycle debit; a missing category balance is created | 0 |
| `p07-empty-day` | no transactions, nothing written to TRANSACT or DALYREJS. The README says the report has headers only; the golden report file is empty (0 bytes), and the golden file is what is compared | 0 |

After every change the seven interest cases pass unchanged: `make parity CHAIN=interest` and `make parity-java`.

## 8. Not to touch

`app/`, `fixtures/`, `tools/parity`, `tools/runjcl`, `java/interest`, `db/postgres/01-transaction-type.sql`,
`db/postgres/02-interest.sql`, `lab/`, the shared skills, the `Makefile` and the CI workflow (proposed to their
owner, `tracks.md` section 7), and anything of Track B.
