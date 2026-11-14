# Modernization lab prompts

Three copy-paste prompts for a live session against this repository.  Each is
self-contained; the estate must be green first (`make up && make build && make parity`).

## 1. Discovery and migration plan

```
Analyse Cognition-Partner-Workshops/ts-cobol-carddemo as a legacy z/OS estate.
Start from docs/chain-graph.md, docs/BASELINE.md and docs/LEGACY-LANGUAGES.md,
then read the JCL and COBOL yourself. Produce docs/MIGRATION-PLAN.md with:
(1) an inventory by technology (COBOL batch, CICS/BMS online, copybooks, Db2,
HLASM, PL/I, dynamic CALL/SQL) with counts and the programs you could not
fully resolve statically; (2) the batch chains and their data dependencies as
a wave plan - which chain can move first and why; (3) for the `interest` and
`posting` chains, the VSAM-to-PostgreSQL table design and the fields where
COBOL numeric semantics (COMP-3, truncation, overpunch signs) need explicit
decisions; (4) the legacy quirks you found that the business must rule on
before they are reproduced or fixed; (5) risks and open questions for the
CICS screens, the HLASM routines and the PL/I fee calculation, which cannot
be executed off-host. Do not write any Java. Open one PR against main.
```

## 2. Convert `interest` to Java 21 / Spring Batch on PostgreSQL and prove it

```
Migrate the `interest` chain (app/jcl/INTCALC.jcl -> app/cbl/CBACT04C.cbl)
to Java 21 with Spring Batch, reading and writing PostgreSQL 15 instead of
the VSAM files, in a new top-level directory java/interest. Translate the
copybooks CVACT01Y, CVACT03Y, CVTRA01Y, CVTRA02Y and CVTRA05Y into tables
and entities with exact numeric scale. Add a loader that imports a fixture
case's input/*.txt files, a Spring Batch job that reproduces INTCALC, and an
exporter that writes the outputs in the recorded fixture format
(datasets/<DSN>.txt, sysout/<JOB>.<STEP>.txt, rc.json). Then prove
equivalence with the estate's comparator: for every case under
fixtures/interest run
  python3 tools/parity/compare.py interest <case> --actual <your-output> --label java
and make all seven pass, including i02-tie-half-cent, without editing
fixtures/, app/ or the comparator. Add `make parity-java` that runs the whole
thing in Docker, wire it into CI, and document in java/interest/README.md
every legacy behaviour you had to reproduce deliberately (rounding, the last
account not being updated, frozen timestamps). Open one PR against main.
```

## 3. Explain the tie-case divergence

```
Run `make parity-naive CASE=i02-tie-half-cent` and `make parity-naive
CASE=i07-nontie-rounding` in ts-cobol-carddemo. The first fails and the
second passes. Explain, with references to app/cbl/CBACT04C.cbl,
tools/parity/naive_ref.py and work/parity/report-naive.md, exactly which
COBOL statement produces the difference, why 12.355 becomes 12.35 on the
mainframe and 12.36 in a half-even BigDecimal port, why account 2004 in the
same case (12.345) does not diverge, and which other fields change as a
consequence (ACCT-CURR-BAL). Then list the rounding/truncation rule a Java
implementation must apply for CBACT04C, CBTRN02C and the PL/I CBFEECAL
(docs/LEGACY-LANGUAGES.md worked examples), and propose a unit test matrix.
Write the answer to docs/ROUNDING.md; no code changes elsewhere. Open one PR
against main.
```

## Expected outcomes

* Prompt 1 yields a plan and no code.
* Prompt 2 yields `make parity-java` green on 7/7 interest cases; the report in
  `work/parity/report.md` is the evidence shown to the audience.
* Prompt 3 yields a written explanation; `work/parity/report-naive.md` shows the
  two differing fields for account 2003.
