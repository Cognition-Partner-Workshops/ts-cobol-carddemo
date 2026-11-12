---
name: cobol-chain-to-spring-batch
description: Convert one CardDemo COBOL batch chain (tools/runjcl/chains.json) to a Java 21 / Spring Batch / PostgreSQL job under modern/<chain> and prove it with `make parity CHAIN=<chain>`, following the org's "COBOL batch parity standard". Use when migrating a chain or when a converted chain fails parity.
---

# COBOL chain -> Spring Batch

Follows the org standard **COBOL batch parity standard**: equal means `tools/parity/compare.py`
passes every case of the chain (datasets field by field, SYSOUT, `rc.json`, DB snapshots, frozen
clock). Learned on `interest` (INTCALC -> CBACT04C, `modern/interest`).

## Layout and harness contract

* Maven aggregator `modern/pom.xml`; add your chain as `<module><chain></module>`.
* Shared codec `modern/record-codec` (`org.carddemo.modern.codec`): `Layouts.<NAME>` are hand-written
  `RecordLayout`s per copybook; `decode(line)` / `encode(map)` / `encodeLine(map)`. Add missing
  copybooks to `Layouts.java` (and a length test); never write a second codec. If the shared codec is
  wrong for your chain, fix it minimally and say so in the PR.
* The module builds a Spring Boot jar `modern/<chain>/target/<chain>.jar` taking
  `--case=<fixtures/<chain>/<case>> --out=<dir> --estate.root=<repo>`. `modern/run-candidate.sh`
  runs it; `make parity CHAIN=<chain>` builds `modern/` (`make modern-build`) and then, per case,
  compares the COBOL rerun (`[legacy rerun]`) and the jar (`[java modern/<chain>]`).
  `TARGET=java` skips the COBOL rerun. Candidate logs: `work/parity/<chain>-<case>-java.log`.
* Write outputs exactly like `tools/parity/record.py`: `datasets/<DSN>.txt` (GDG `(0)`/`(+1)` ->
  `G0001V00`, trailing spaces stripped), `manifest.json` (`layout`, `lrecl`, `records`),
  `sysout/<JOB>.<STEP>.txt`, `rc.json` (`{JOB: {maxrc, steps}}` incl. the setup jobs' steps),
  `db/<table>.csv` when the chain has `db_tables`. See `modern/interest/.../RecordedRun.java`.

## Procedure

1. `make up && make build && make parity CHAIN=<chain> TARGET=legacy` - legacy green first.
2. Read the program, its JCL, `chains.json` entry, every fixture `README.md`, and `AGENTS.md`
   quirks. List each quirk you must reproduce, with the fixture that proves it.
3. Model VSAM clusters as PostgreSQL tables keyed like the KSDS/AIX (`schema-<chain>.sql`); the
   setup jobs (IDCAMS REPRO) become an input loader. Unload in key order (KSDS) or write order (PS).
4. Port the program loop as a chunked step: `JdbcCursorItemReader` in key order, a step-scoped
   `ItemProcessor` holding the program's working storage (control breaks, counters), an
   `ItemWriter` for REWRITE/WRITE. Name procedure paragraphs in Javadoc.
5. Reproduce DISPLAY text into SYSOUT and map abends to return codes (CEE3ABD -> 12 here).
6. Run `make parity CHAIN=<chain>`; read `work/parity/report.md`; fix the Java, never the goldens.
7. Unit-test arithmetic and codec edge cases (`make modern-build` runs them).

## Gotchas found so far

* **No ROUNDED = truncation.** COBOL stores excess decimals by truncating toward zero and drops
  high-order digits without ON SIZE ERROR. Use `RoundingMode.DOWN` (see `CobolNumeric.store`);
  HALF_EVEN fails `i02-tie-half-cent` (12.36 vs 12.35) and nothing else.
* **PIC X keys are space-padded.** A literal MOVEd to a `PIC X(10)` key (e.g. `'DEFAULT'`) must be
  padded before a lookup, because the codec returns TEXT fields raw.
* **Control-break flush at EOF.** Check whether the program really writes the last group; CBACT04C
  does not (C2C-06: the EOF branch is unreachable, so the last account is never rewritten). Keep it,
  name it in code and PR, and ask the business whether to fix it after cutover.
* **FILLER must survive a REWRITE.** `compare.py` skips FILLER, so parity cannot catch this. Load
  with `layout.decode(line, true)` and carry `FILLER@<offset>` into `encode`, because SYSOUT DISPLAYs
  of whole records include it.
* **An abend does not roll back.** CEE3ABD's stand-in ends with STOP RUN, so every REWRITE/WRITE
  before the abend (including the control-break REWRITE on the abending record) stays. Do not let a
  Spring Batch chunk rollback undo them: catch the abend in the processor, write that item, then
  `setTerminateOnly()`. SYSOUT then ends with `U+0000000999`, a blank line and libcob's `implicit
  CLOSE` warnings, in reverse OPEN order.
* **Duplicate keys in a REPRO into a KSDS:** the first record wins and the rest are skipped
  (IDC3302I, still RC 0). Use `INSERT ... ON CONFLICT DO NOTHING`, not a failing primary key.
* Probe edge cases that have no golden by copying a fixture's `input/` to a scratch case, running
  `record.py` (legacy) and the candidate into `work/`, and diffing with `diff -r`. Remove the scratch
  case afterwards; goldens change only via `make record` on request.
* Zoned overpunch is EBCDIC style (`{`/`A`-`I` positive, `}`/`J`-`R` negative); unsigned PIC 9
  fields have no overpunch.
* The estate image's Maven is 3.6.3: pin `maven-compiler-plugin` >= 3.6 so `release 21` works.
  If Maven Central rate-limits, pass a mirror: `MVN_ARGS="-s /estate/work/m2-settings.xml"`.
* Clock: build timestamps from the chain's frozen clock (`chains.json` `clock`), DB2 format
  `yyyy-MM-dd-HH.mm.ss.SS0000`.

## Forbidden

Editing `fixtures/*/expected`, COBOL under `app/`, or copybooks; `make record` without an explicit
request; silently fixing a documented quirk.
