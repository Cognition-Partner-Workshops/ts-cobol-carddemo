# Replay lab - interest stream

One case, two engines, three things compared:

| | What | Where it comes from |
|---|---|---|
| expected | the outcome the rulebook derives | `modernization/rules/interest/expected/<case>.json` for a golden case; the same evaluator (`modernization/rules/interest/tools/evaluate.py`) run on the inputs of an edge probe or a custom input |
| observed | the legacy COBOL run | `INTCALC` -> `CBACT04C` under GnuCOBOL in the `estate` container, through the recorder of `tools/parity`. For a golden case the rerun is also compared with the golden files, as `make parity` does |
| target | the Java implementation | `java/interest`, three steps through PostgreSQL (profile `database`), launched as `make parity-java` launches it |

Outputs are decoded with the copybook codec of `tools/parity` (`CVTRA05Y`, `CVACT01Y`); nothing is compared as raw bytes.

```sh
make lab          # the page: http://127.0.0.1:8090/   (LAB_PORT=8095 make lab for another port)
make lab-gate     # 7 golden cases + 6 edge probes; report in work/lab/report.md; exit 0 only when UNCLASSIFIED is 0
make lab-test     # unit tests of the classification and the verdicts (no engine needed)
make lab-e2e      # Playwright suite of the page, one test per row of ui-test-plan.md
```

Host: Docker, make, python3 with PyYAML (`pip install -r lab/requirements.txt`; pytest and playwright are for the two
test targets, plus `python3 -m playwright install chromium`), JDK 21 and Maven for the jar. `make lab` and `make lab-gate`
start the containers, compile the estate when `loadlib/` is empty, apply `db/postgres/02-interest.sql` when missing and
build `java/interest/target/interest.jar` when it is older than its sources.

## Classes of a compared value

| Class | Meaning |
|---|---|
| MATCH | observed and target agree |
| INTENDED_CHANGE | they differ and an approved decision in `modernization/rules/interest/decisions.md` says they should: the target has the value of the approved option, the legacy run the as-coded value |
| UNRESOLVED | they differ by two options of a decision that is still open, or they agree on a value the rulebook marks OPEN and the decision is still open |
| UNCLASSIFIED | any other difference - including a legacy rerun that differs from a golden file |

The gate exits 0 only when UNCLASSIFIED is zero. It also prints rule coverage: how many of the 20 rules the cases
exercised.

A decision counts as approved when the Status cell of its register row reads `APPROVED <option>` (for example
`APPROVED B`). **Today every row is OPEN**, so no value can be INTENDED_CHANGE; that class is shown by the unit tests
(`lab/tests`), not by a run. The lab never writes the register.

Compared values: per balance of the input whether an interest transaction was written and every field of it; every
field of every account record after the run; the job log; how the run ended; the return codes; the number of
transactions. A value of the rulebook that no output file carries (the rate group and the rate) is not compared; it
shows through `TRAN-AMT`.

## Rule verdicts

Independently of the comparison, each engine is judged against the rulebook: CONFORMS or VIOLATES per rule. An engine
violates a rule when a value the rulebook cites that rule for is not the required one - the determined value, the
approved option, or the as-coded option while the decision is open. Two engines that agree with each other and both
break a rule therefore show as two VIOLATES while the comparison says MATCH. A rule the case does not exercise is
NOT EXERCISED, and a rule whose outcome the source does not determine for the case (INT-H03, INT-H05) is
NOT DETERMINED; neither is a pass. A violated rule is reported in the gate output; it does not change the exit code.

When a wrong value is another option of one open rule, that rule alone is blamed; otherwise every rule the value cites
is blamed, which can name more rules than the one at fault.

## Cases

* **Golden** - the seven fixtures of `fixtures/interest`.
* **Edge probes** - `lab/probes.json`: positive and negative rounding ties, the last account on the file, an account
  with no disclosure group, negative balances, interest below one cent on an inactive account. They have no golden
  file. Their inputs are encoded with the copybook codec into `work/lab/cases/` and run through the legacy lane to
  get the observed side. Nothing is recorded under `fixtures/`.
* **Custom** - accounts and balances typed into the page as JSON, checked before anything runs.

## What the lab does not prove

* Sequential batch replay only: one clean run of INTCALC per case on small synthetic inputs. No rerun, no restart
  after a failure, no volume.
* No CICS: the online programs that read and write the same account file are not run.
* No concurrency: one run at a time; nothing else touches the files or the tables during a run.
* GnuCOBOL arithmetic, not Enterprise COBOL: the observed side is GnuCOBOL 3 with `-std=ibm`; intermediate results,
  truncation and overflow under the host compiler options are not shown.
* No real Db2 and no VSAM: indexed files are GnuCOBOL files, the target database is PostgreSQL 15 in a container.
* ASCII with zoned signs as the off-host harness writes them, not cp037 host files; a frozen clock.
* A rule verdict is a check against the rulebook's own evaluator, which an agent wrote from the code; it is not a
  business owner's view. Rules no case exercised have no verdict: INT-R08 (failure path), INT-H02 (second run),
  INT-H03 (several cards), INT-H05 (overflow), INT-H06 (job order).
* The lab does not run the database assertions RC1, RC2 and RC5 of `make parity-java`; that gate stays the one for them.

## Files

| Path | What |
|---|---|
| `replay.py` | both lanes, decoding, classification, rule verdicts, the gate and the classified report |
| `legacy_lane.py` | the legacy lane, run inside the `estate` container |
| `cases.py`, `probes.json` | the cases; checking and encoding of probe and custom inputs |
| `server.py`, `page.html` | the page |
| `tests/` | unit tests (`make lab-test`) |
| `e2e/` | Playwright suite (`make lab-e2e`) |
| `ui-test-plan.md`, `evidence/` | the UI test plan with its results, and the evidence of the run |
