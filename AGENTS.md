# CardDemo legacy estate - agent notes

This repository is the **"before" estate** for a mainframe modernization lab:
COBOL, JCL, CICS/BMS, copybooks, Db2, a little HLASM and PL/I, plus a Docker
harness that runs the real batch JCL off-host and proves equivalence with
golden files.  Migration targets (Java, Spring Batch, PostgreSQL schemas for
VSAM data) do **not** live here until a lab session creates them.

## Build and run

```sh
make up                       # docker compose up (estate: GnuCOBOL 3 + ocesql, db: PostgreSQL 15)
make build                    # compile app/cbl and the Db2 sub-application into ./loadlib
make run CHAIN=posting        # run one chain on its first fixture (CASE=<name> for another)
make parity                   # rerun every chain/case and compare field by field
make parity CHAIN=interest CASE=i02-tie-half-cent
make parity-naive CASE=i02-tie-half-cent   # half-even Python reference: must FAIL on this case only
make record CHAIN=... CASE=...             # (re)write golden files - only when the user asks
make chain-graph && make chain-graph-check # docs/chain-graph.md (CI fails when stale)
make reset                                 # wipe datasets/, loadlib/, work/ and the DB volume
```

All of it runs inside the `estate` container; the host only needs Docker and
`make`.  `make shell` opens a shell in the container.

Chains (`tools/runjcl/chains.json`): `posting` (POSTTRAN + TRANREPT ->
CBTRN02C, CBTRN03C), `interest` (INTCALC -> CBACT04C), `trantype-update`
(MNTTRDB2 -> COBTUPDT via ocesql/PostgreSQL), `trantype-dynamic` (MNTTRDYN ->
CBTRNDYN dynamic SQL + dynamic CALL).  Fixtures: `fixtures/<chain>/<case>/`
with `input/`, `expected/` and a `README.md` stating what the case proves.

## Rules

* **Never edit `fixtures/*/expected`, the COBOL under `app/`, or the copybooks
  to make parity pass.**  Parity failures mean the candidate is wrong (or the
  legacy behaviour is a documented quirk you must reproduce).  Golden files
  change only through `make record` on an explicit request.
* Files under `app/` are the customer's estate: only additive changes (new
  JCL steps, new programs).  `tools/esql_prep.py` normalises *scratch copies*
  for Open COBOL ESQL; do not "fix" the originals.
* Parity means **field-level** equality via copybooks (`tools/parity/compare.py`),
  including COMP-3, zoned overpunch signs and EBCDIC, plus SYSOUT, return
  codes and DB snapshots.  Byte-identical files are the goal, not "close".
* Legacy quirks are features: CBACT04C truncates interest (no `ROUNDED`) and
  never updates the last account on TCATBALF; CBTRN02C checks the limit against
  cycle amounts; COBTUPDT ignores SQLCODE +100.  See `docs/BASELINE.md`.
* No Java migration lives here unless the lab prompt asks for it, and then it
  goes in a new top-level directory, proving itself with `make parity`.
* Synthetic data only.  No real names, card numbers or customer references.

## Gotchas

* Copybooks in `app/cpy` are tab-indented; `tools/build.sh` expands them to
  `work/cpy` before `cobc`.  Compile flags: `-std=ibm -fsign=EBCDIC`.
* The clock is frozen per chain (`COB_CURRENT_DATE=2022-07-05 10:00:00.00`);
  without the `.00` the hundredths drift and `PROC-TS` differs between runs.
* ocesql needs `COB_PRE_LOAD=libocesql`; PREPARE host variables must be a
  VARCHAR group with `<name>-LEN` / `<name>-ARR` 49-levels.
* `CVTRA07Y` is a real report copybook; the reject-record layout for parity is
  `tools/parity/layouts/REJTRAN.cpy`.
* GDG generations restart at `G0001V00` for every case (fresh `datasets/`).
* CICS programs (`app/cbl/CO*`), HLASM and PL/I are analysis-only off-host:
  `docs/BASELINE.md`, `docs/LEGACY-LANGUAGES.md`.
* Where things are: `tools/runjcl` (JCL runner), `tools/parity` (codec,
  recorder, comparator, naive reference), `tools/fixtures/gen_fixtures.py`
  (fixture inputs), `tools/chaingraph` (discovery graph), `db/postgres`
  (translated Db2 DDL), `docs/LAB.md` (lab prompts).
