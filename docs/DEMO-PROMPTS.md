# Modernization demo prompts (runbook sequence)

Six copy-paste prompts that walk this repository through the engineer
runbook's story:

**ingest -> inventory -> analyze -> isolate -> specify -> design -> execute -> replay -> accept**

Run them in order, one Devin session each, and review the PR of one before
starting the next.  Every prompt is self-contained.  Setup, preflight and the
presenter agenda are in [DEMO-SETUP.md](DEMO-SETUP.md).  The shorter
single-topic prompts in [LAB.md](LAB.md) still work on their own.

The demo stream is the **`interest`** chain: scheduler folder
`MONTHLY-InterestCalculation` -> `app/jcl/INTCALC.jcl` -> `app/cbl/CBACT04C.cbl`.
It is small, it runs off-host, it has seven golden cases, and it carries two
real legacy quirks to rule on.

Conventions used by all prompts:

| Path | Content | Written by |
|---|---|---|
| `app/`, `fixtures/` | the estate and its golden files - never edited | - |
| `store/` | toolbox scan results and HTML reports | prompt 1 |
| `modernization/streams/interest/` | stream manifest and scope report | prompt 2 |
| `modernization/rules/interest/` | rules, review board, decision register, rule-derived expectations | prompt 3 |
| `modernization/design/` | architecture, code-and-data strategy, tickets | prompt 4 |
| `java/interest/` | the target implementation | prompt 5 |
| `lab/` | classified replay and its local web page | prompt 6 |

## 0. One-shot preparation prompt (optional)

Use this instead of prompts 1-4 when you want a single session to prepare the
whole "before the build" package and stop for approval.

```
Prepare a modernization pilot on Cognition-Partner-Workshops/ts-cobol-carddemo
at the current main revision. Record that revision's SHA in every artifact.
The estate has COBOL batch, CICS/BMS, copybooks, JCL, Db2, IMS and MQ
sub-applications, HLASM, one PL/I routine, a CSD and Control-M / CA-7
scheduler exports. It has no FOCUS and no production data.
Target: Java 21, Spring Batch, PostgreSQL 15.
Approved tooling: the mftb plugin loaded in this session (state its version)
and the repository's own Docker harness (make up / build / parity).
Read AGENTS.md first and follow its rules: never edit app/ or
fixtures/*/expected.

1. Inventory what is supplied, what is missing and what runs off-host.
2. Run the toolbox estate and module passes, inspect the findings and
   generate the HTML reports under store/.
3. Select exactly one stream: the interest chain rooted at INTCALC.jcl.
   Keep proven and candidate edges apart.
4. Extract source-cited, business-first RULE / QUIRK / HOLE entries for that
   stream and derive expected outcomes from the rules alone, not from either
   execution lane.
5. Build an SME review board and a decision register. Do not assume sign-off.
6. Write the architecture, a code-and-data strategy and dependency-aware
   engineering tickets as files under modernization/design/.
Stop there. Do not write Java, do not dispatch implementation sessions and do
not write to any ticket system. Deliver the readable artifacts as attachments,
list every gap and runtime limit, and open one PR against main. Do not merge.
```

## 1. Intake, inventory and toolbox reports

```
Work in Cognition-Partner-Workshops/ts-cobol-carddemo on the current main
revision. Read AGENTS.md first. Do not edit anything under app/ or fixtures/.

Part A - intake. Write modernization/INTAKE.md with: the baseline SHA and
today's date; what the estate supplies by category (COBOL batch, CICS/BMS,
copybooks, JCL and procs, Db2 DDL and DCLGENs, IMS, MQ, HLASM, PL/I, CSD,
scheduler exports, control cards, data); what is absent or cannot be executed
off-host, taken from docs/BASELINE.md and docs/LEGACY-LANGUAGES.md and checked
against the source; and which parts are executable today. Prove the last
point by running `make up && make build && make parity` and quoting the
result. Mark missing categories as missing; do not invent them.

Part B - toolbox. Use the mftb plugin loaded in this session (estate-analysis
entry point). State its version. Run it from the repository root so the store
lands in ./store, with `app` as the only estate root; fixtures/, tools/, db/,
docs/ and generated output are not estate. Sweep the estate, name the modules
with cited evidence until `mftb modules --check` passes, scan each module,
then for the module that owns CBACT04C run the dynamic-call, connections,
traces, chains (from JCL INTCALC and from the scheduler exports in
app/scheduler) and online (app/csd) passes with findings, and generate the
module report and the estate report.

Part C - inspect. Do not trust a green command. Cross-check at least ten
scanner results against the source and list them in INTAKE.md with file and
line. Report every catalogue ask (the sweep raises them for the PL/I member,
the HLASM members, the EBCDIC data files and several copybooks) as an open
question with an owner placeholder, not as a resolved item.

Deliver: store/ committed, modernization/INTAKE.md, and the estate and module
HTML reports attached to your final message. Open one PR against main. Do not
merge.
```

## 2. Isolate one stream

```
In Cognition-Partner-Workshops/ts-cobol-carddemo, using the store/ produced by
the intake PR (rescan if it is missing or older than main), isolate exactly
one stream: the interest chain. Its root is the batch job app/jcl/INTCALC.jcl,
scheduled in the Control-M folder MONTHLY-InterestCalculation in
app/scheduler. Use the mftb batch-chain-analysis entry point for the chain
and docs/chain-graph.md as a second, independent view; where the two disagree,
say so and decide from the source.

Write modernization/streams/interest/manifest.json and a self-contained
modernization/streams/interest/stream-INTCALC.html covering:
- the in-scope set: jobs, steps, programs, copybooks, datasets with DD names,
  GDG generations and record layouts;
- every call and data edge marked PROVEN (static, cited) or CANDIDATE
  (dynamic or inferred), never merged;
- the setup jobs the chain needs before it can run (see tools/runjcl/chains.json)
  marked as setup, not as stream logic;
- outside-stream dependencies: every other job, program or CICS transaction
  that reads or writes the same datasets, with the direction of the edge;
- the predecessor and successor jobs the scheduler exports name, and any
  ordering the exports do not prove;
- open asks.

Do not equate a file name, a PROGRAM-ID and a load-module name without
evidence. Do not widen the scope to the posting chain. No Java. Attach the
HTML to your final message and open one PR against main. Do not merge.
```

## 3. Business rules, SME board and independent expectations

```
In Cognition-Partner-Workshops/ts-cobol-carddemo, write the business
specification of the interest stream (app/jcl/INTCALC.jcl, app/cbl/CBACT04C.cbl
and the copybooks it uses). Scope is modernization/streams/interest/manifest.json.

1. Rules. Write modernization/rules/interest/rules.yaml. Each entry has an id,
   a type, a one-sentence business statement a card-operations manager would
   recognise, the source citation (file and line range), and the fixture cases
   that exercise it. Types:
   RULE  - business policy the program implements on purpose;
   QUIRK - behaviour that follows from how the code is written and that
           nobody has confirmed as policy;
   HOLE  - a case the code does not handle or the estate does not let us see.
   Ask of every entry: is this a business rule, really? Implementation detail
   that carries no policy goes in a separate technical-notes section. Above
   the entries put a plain-language summary of what the monthly interest run
   does. At minimum classify: the interest formula and its truncation to the
   cent, the final account on TCATBALF not having its balance updated, the
   zero-rate skip, the default disclosure-group fallback, negative balances,
   and the transaction id and timestamp the run stamps.

2. Expectations. For every case under fixtures/interest derive the expected
   business outcome from rules.yaml and the case's input/ files only, as
   modernization/rules/interest/expected/<case>.json. Write a small rulebook
   evaluator that does this. It must not read fixtures/*/expected, must not
   call the COBOL and must not reuse tools/parity/naive_ref.py. Where a rule
   is undecided (a QUIRK or HOLE), emit both candidate outcomes and mark the
   expectation OPEN instead of picking one.

3. Board. Generate a self-contained modernization/rules/interest/rules-INTCALC.html:
   the business summary on top, then RULE / QUIRK / HOLE cards with the source
   excerpt collapsed by default, Approve / Reject / Comment controls, a
   reviewer-name field, decisions kept in the browser only, and an Export
   decisions button that downloads a CSV. Add a check command that fails when
   the HTML is out of date with rules.yaml.

4. Register. Write modernization/rules/interest/decisions.md: one row per
   QUIRK and HOLE with the question for the business, the options, the effect
   of each option on the fixtures, an owner placeholder and status OPEN.
   Nothing in this PR is SME sign-off; say so at the top of the board and the
   register.

Verify the board in a browser: approve one card, reject one, add a comment,
export, and confirm the CSV holds every card. Record that and attach the
recording and the HTML. Clear the demo decisions afterwards and do not commit
the CSV. No Java. Open one PR against main. Do not merge.
```

## 4. Architecture, code-and-data strategy and tickets

```
In Cognition-Partner-Workshops/ts-cobol-carddemo, design the move of the
interest stream to Java 21, Spring Batch and PostgreSQL 15. Inputs: the
stream manifest in modernization/streams/interest, the rules and decision
register in modernization/rules/interest, db/postgres, and AGENTS.md.

Write, under modernization/design/:
- architecture.md - the versioned golden copy: target components, which one
  owns which table, the batch job boundary, transaction and restart
  boundaries, how the target coexists with the jobs that stay on the
  mainframe and still read or write the same datasets;
- data-map-interest.md - every field of CVACT01Y, CVACT03Y, CVTRA01Y, CVTRA02Y
  and CVTRA05Y mapped to a column with exact type, precision and scale,
  sign and encoding handling (COMP-3, zoned overpunch, EBCDIC), keys and
  alternate indexes, and the mapping gaps you could not close;
- strategy-interest.md plus a self-contained strategy-interest.html for a
  mixed audience: the move map, the build steps in order, and a "moving the
  data" section covering trial load, reconciliation counts and sums,
  side-by-side running, cutover and fallback. Mark each item PROPOSED and
  name the owner role that has to confirm it;
- tickets/NN-<slug>.md - bounded engineering tickets using this contract:
    Work item: <id and outcome>
    Source: <repo, baseline SHA, stream root INTCALC>
    Context: <rule ids, architecture.md version, data-map rows, decisions>
    Scope: <allowed files; frozen shared interfaces; excluded work>
    Dependencies: <only genuine prerequisites>
    Acceptance: <input -> observable result; errors; database assertions>
    Verification: <build, tests, fixture cases, final-revision evidence>
    Deliverables: <one branch and PR, test output, gaps>
    Ticket writes: prohibited
    Restrictions: no edits to app/ or fixtures/*/expected; no merge or deploy
  and tickets/README.md with the dependency order: which tickets can start in
  parallel, which wait on the schema, and which are final gates.

Keep business and release decisions in the decision register, not in the
ticket queue. A QUIRK with status OPEN is implemented as the legacy behaviour
and referenced by id, so it can be changed later in one place. Do not write
Java, do not create tickets in any external system and do not start
implementation sessions. Attach the HTML and open one PR against main. Stop
for approval. Do not merge.
```

## 5. Implement (pilot first, then the rest)

Send this once for the pilot ticket.  After the pilot PR is reviewed, send it
again per remaining ticket, or hand the ticket list to one session and ask it
to run the independent tickets as parallel child sessions.

```
Implement ticket modernization/design/tickets/<NN-slug>.md in
Cognition-Partner-Workshops/ts-cobol-carddemo. The ticket is the contract:
stay inside its Scope, meet its Acceptance, and produce its Deliverables.

Before you start, check for an existing branch or open PR that already
covers this ticket and stop if there is one. Code goes under java/interest
(Java 21, Spring Batch, PostgreSQL 15). Work in your own environment: bring
up PostgreSQL with Docker, build with Maven, run the unit and integration
tests, then run the changed flow end to end on the fixture cases the ticket
names and compare with
  python3 tools/parity/compare.py interest <case> --actual <your-output> --label java
Do not edit app/, fixtures/ or the comparator, and do not re-record golden
files. Reproduce legacy behaviour for every rule whose decision is OPEN and
cite the rule id in the code path and the test name.

Report: build and test output from the final revision, the parity result per
case, what the ticket does not cover, and anything you found that belongs in
the decision register. If you fix something after review, rerun and replace
the evidence. One new branch, one PR against main. Do not update any ticket
system. Do not merge.
```

When every ticket PR is in, the combined revision still has to pass as a
whole: `make parity-java` on all seven interest cases (see prompt 2 in
[LAB.md](LAB.md) for that target) and the replay in prompt 6.

## 6. Replay lab and acceptance

```
In Cognition-Partner-Workshops/ts-cobol-carddemo, build a replay lab for the
interest stream under lab/. It compares three things per fixture case:
  expected - the rule-derived outcome in modernization/rules/interest/expected;
  observed - the legacy COBOL run (the golden files, re-run with make parity);
  target   - the Java implementation in java/interest.

1. lab/replay.py runs a case through both engines and decodes outputs with
   the copybook codec in tools/parity, never by raw bytes. It classifies every
   compared value as
     MATCH           - observed and target agree;
     INTENDED_CHANGE - they differ and an approved decision in
                       modernization/rules/interest/decisions.md says they should;
     UNRESOLVED      - they differ, or agree on behaviour the rulebook marks
                       OPEN, and the decision is still open;
     UNCLASSIFIED    - any other difference.
   The acceptance gate exits 0 only when UNCLASSIFIED is zero. It prints rule
   coverage: how many rules the cases exercised out of the total.
2. Independently of the comparison, judge each engine against the rulebook:
   CONFORMS or VIOLATES per rule. Two engines that agree with each other and
   both break a rule must show up here.
3. lab/server.py serves one local page on port 8090 (LAB_PORT overrides):
   toolchain versions, baseline SHA and the frozen business date in the
   header; a case picker with Golden (the seven fixtures), Edge probes (new
   inputs you design to stress rounding ties, the last account on the file,
   an account with no disclosure group and a negative balance) and Custom;
   the two lanes side by side; the classified differences; the rule verdicts;
   the gate; and a link to the rules board. Edge probes have no golden file:
   run them through the legacy lane to get the observed side and do not
   record them into fixtures/.
4. Add `make lab` and `make lab-gate`, and run lab-gate in CI.

Then test it yourself in a browser: run Golden, open one UNRESOLVED item and
follow it to its decision-register row, and run the edge probes. Record that.
State plainly what the lab does not prove: sequential batch replay only, no
CICS, no concurrency, GnuCOBOL rather than Enterprise COBOL arithmetic, no
real Db2. Attach the recording, a screenshot of the gate and the classified
report. Open one PR against main. Do not merge.
```

## What each prompt should leave behind

| Prompt | Evidence to show an audience |
|---|---|
| 1 | Estate and module HTML reports; `INTAKE.md` with gaps listed as gaps |
| 2 | `stream-INTCALC.html`: one root, proven and candidate edges, outside consumers |
| 3 | `rules-INTCALC.html` board, exported decisions CSV (demo only), `decisions.md` all OPEN |
| 4 | `strategy-interest.html`, `architecture.md`, ticket files with a dependency order |
| 5 | One PR per ticket with test output and per-case parity |
| 6 | The lab page: Golden run, classified counts, rule verdicts, gate result |
