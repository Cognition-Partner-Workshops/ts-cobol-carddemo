# Demo setup guide

How to prepare and present the modernization workflow from the engineer
runbook on this repository.  The prompts are in
[DEMO-PROMPTS.md](DEMO-PROMPTS.md).

The story: **ingest -> inventory -> analyze -> isolate -> specify -> design ->
execute -> replay -> accept**, on one stream (the `interest` chain).  The demo
proves a method on one bounded stream.  It does not prove that the whole
estate is ready for production, and the presenter should say so.

## 1. What this repository gives you, and what it does not

| Runbook stage | Here today | Produced by |
|---|---|---|
| Estate in Git with a baseline | yes: `app/` (COBOL, CICS/BMS, copybooks, JCL, Db2, IMS, MQ, HLASM, PL/I, CSD, Control-M and CA-7 exports) | already in `main` |
| Legacy lane that runs off-host | yes: Docker harness, GnuCOBOL 3 + Open COBOL ESQL + PostgreSQL 15, four chains | `make up && make build` |
| Golden files and field-level comparator | yes: 24 cases under `fixtures/`, `tools/parity/compare.py` | `make parity` |
| A divergence to explain | yes: half-even reference fails on the half-cent tie only | `make parity-naive CASE=i02-tie-half-cent` |
| Toolbox estate and module reports | no | prompt 1 |
| One-stream manifest and scope report | partly: `docs/chain-graph.md` | prompt 2 |
| Business rules, SME board, decision register | no (quirks are listed in `docs/BASELINE.md`, not as reviewable cards) | prompt 3 |
| Architecture, code-and-data strategy, tickets | no | prompt 4 |
| Java / Spring Batch / PostgreSQL target | no - by design, the repo is the "before" estate | prompt 5 |
| Classified replay lab with a web page | no | prompt 6 |

Differences from the runbook's reference demo that change what you say:

* **No FOCUS.**  Skip the report-replay track; do not imply it exists.
* **The stream root is a real batch job.**  `INTCALC` is in the JCL and in the
  scheduler exports, so the chain is evidenced, not hand-picked.
* **The legacy lane is GnuCOBOL, not Enterprise COBOL**, and Db2 is emulated
  with Open COBOL ESQL on PostgreSQL.  `docs/BASELINE.md` lists every stand-in.
  CICS programs, HLASM and PL/I are analysis-only.
* **Data is synthetic.**  No customer data is in the repo; keep it that way.
* **Parity here starts stricter than the runbook's classified replay.**
  `AGENTS.md` requires field-level equality with the golden files.  The
  MATCH / INTENDED_CHANGE / UNRESOLVED / UNCLASSIFIED classes arrive with
  prompt 6 and only take effect through the decision register.  This lab is
  Tier 0 of the parity ladder (golden-file replay off-host); a pilot adds
  Tier 1 (scheduled shadow batch on a replica); on a real customer the same
  golden cases are run against output pulled from their z/OS (for example via
  Zowe CLI or the customer's z/OSMF tooling), and the mainframe, not GnuCOBOL,
  is the acceptance oracle.

## 2. Prerequisites

* A Devin organization with this repository connected, and DeepWiki indexed
  for it (used in the "make the code navigable" beat).
* Docker with Compose v2 and GNU Make in the session's machine.  Nothing else
  is needed on the host; compilers run in the `estate` container.
* The **mftb** toolbox plugin (prompts 0-2).  The approved distribution is
  the `COG-GTM/devin-mainframe-cobol-toolbox` release (`mftb.zip`), installed
  as a personal plugin for the presenter's Devin user.  It exposes four entry
  points:
  * estate analysis - what applications are in the estate;
  * module analysis - what is in one application;
  * online transaction analysis, starting from a driver program or a
    transaction id;
  * batch chain analysis, starting from the JCL member that kicks the chain
    off.

  Check the version at the start of a session with
  `python3 -m mftb --version` run with `PYTHONPATH` set to the plugin root;
  command names vary by version, so read `python3 -m mftb --help` before
  relying on a command printed in a prompt.  On a customer estate Cognition
  runs the toolbox and hands over the report; the plugin itself is not
  installed in the customer's tenant.
* For prompts 5 and 6: outbound access to Maven Central from the session, or
  a mirror.  Parallel sessions can be rate-limited by Maven Central; run the
  pilot ticket first and fan out only after it builds.
* Optional: a ticket system.  The prompts keep tickets as Markdown files and
  forbid external ticket writes; change that line deliberately if you want
  them pushed to a tracker.

`.devin/blueprint.yaml` runs `make up && make build` and nothing else, so a
fresh session starts with the estate containers up and the programs compiled.

## 3. Preflight (do this the day before, and again one hour before)

```sh
git fetch origin && git checkout main && git pull --ff-only
git rev-parse HEAD                      # write this SHA on your notes; use it in every prompt
make reset && make up && make build     # clean start, image build, compile
make parity                             # must print PASS for all 24 cases
make parity-naive CASE=i02-tie-half-cent    # must FAIL with exactly 2 differences
make parity-naive CASE=i07-nontie-rounding  # must PASS
make chain-graph-check                  # discovery graph is current
```

Expected: 7 posting, 7 interest, 7 trantype-update and 3 trantype-dynamic
cases pass with 0 differences; the tie case fails on `TRAN-AMT`
(12.35 legacy, 12.36 half-even) and `ACCT-CURR-BAL` for account 2003.  The
report is `work/parity/report.md`.

Then check the rest of what you plan to show:

1. Freeze the revision.  Every artifact the prompts produce records the
   baseline SHA; a demo that mixes reports from two revisions will show
   counts that do not add up.
2. The business date is frozen at `2022-07-05 10:00:00.00` by the harness.
   Do not change it between rehearsal and demo.
3. Confirm the branch you present from contains every artifact you intend to
   open (the prompts land as separate PRs; merge or stack them in order).
4. Open the tabs you will use, in order: the repository, DeepWiki, the estate
   report, the stream report, the rules board, the strategy page, one ticket
   PR, the lab.
5. The lab serves on port 8090 inside the session's machine.  Share it through
   a Devin browser preview.  A preview from an earlier session stops working
   when that machine sleeps: restart `make lab` in the live session rather than
   reuse an old link.  Confirm port 8090 is free before `make lab`
   (`ss -ltn | grep ':8090 '` prints nothing); do not kill other processes to
   free it, set `LAB_PORT` instead.
6. Use a fresh browser profile for the rules board so demo approvals are
   disposable.  After every rehearsal clear the browser-local decisions on
   the board (clear the site data for the board page, or use the board's own
   reset control if prompt 3 built one) so the audience sees every card
   undecided.
7. Verify DeepWiki shows the current `main`: compare the revision it reports
   with the frozen SHA and re-index if the last index predates it.
8. Run prompt 5's pilot ticket build once in the session you will present
   from, to warm the Maven cache and confirm Testcontainers starts against
   the session's Docker.
9. Record a fallback of the lab run and the board walkthrough.  If you fall
   back to it, say that it is a recording.

## 4. Preparation order

Run the prompts ahead of time; the audience sees results, plus one or two
live steps.

| Step | Prompt | Wait for | Review before moving on |
|---|---|---|---|
| 1 | 1 - intake and toolbox | PR with `store/` and `INTAKE.md` | Open the HTML reports; spot-check three findings against source; gaps are listed as gaps |
| 2 | 2 - isolate `interest` | PR with stream manifest and HTML | One root; proven and candidate edges separate; outside consumers of the same datasets listed |
| 3 | 3 - rules and board | PR with rules, board, register, expectations | Cards read as business statements; quirks are not labelled rules; every decision is OPEN; expected values in `fixtures/` and in the rules' expectations are derived from the rule text, not copied from the legacy lane's output; any case the rules do not determine is marked UNRESOLVED with an owner |
| 4 | 4 - design and tickets | PR with architecture, strategy, tickets | Data moves are in the strategy, not only code; tickets are small and name their rule ids |
| 5 | 5 - pilot ticket, then the rest | One PR per ticket | Pilot builds and passes its cases before any fan-out; no duplicate tickets claimed |
| 6 | 6 - replay lab | PR with `lab/` | Gate is 0 unclassified; UNRESOLVED items link to register rows; limits stated |

Prompt 0 replaces steps 1-4 with one session when time is short; review its
PR against the same four checks.

Human decisions the prompts deliberately leave open:

* The two legacy quirks in the interest run - truncating instead of rounding
  the monthly interest, and not updating the balance of the last account on
  the file.  Until a named owner decides, the target reproduces both and the
  lab shows them as UNRESOLVED.
* Merging.  No prompt merges a PR.

## 5. Presenting it: 35 minutes

Timings are estimates until you have rehearsed a full take.

| Minutes | Show | Say |
|---|---|---|
| 0-3 | The repository at the frozen SHA; `INTAKE.md` | What was supplied, what is missing, what runs. The preparation makes everything after it possible. |
| 3-7 | DeepWiki for the repo; the estate and module reports | Unfamiliar code becomes navigable, and the scanner's open questions stay visible. |
| 7-11 | `stream-INTCALC.html` | One job, one program, its data. Other jobs that touch the same files are still on the page. |
| 11-17 | The rules board: summary, one RULE, one QUIRK, a decision, export | Intent is reviewed by the business. The last-account behaviour is a quirk until someone owns the decision. |
| 17-22 | `architecture.md`, the strategy page, the ticket list | Code and data move together; tickets come from approved context. |
| 22-27 | One ticket PR with its test output; the session that produced it | Each ticket is built and verified on its own, and still has to pass together. |
| 27-34 | The lab: Golden run, rule verdicts, the half-cent tie, one edge probe | Three views - what the rule says, what the mainframe did, what Java does. Preserve, change on purpose, or leave open. This lab is Tier 0 of the parity ladder (golden-file replay off-host); a pilot adds Tier 1 (scheduled shadow batch on a replica); on a real customer the same golden cases are run against output pulled from their z/OS (for example via Zowe CLI or the customer's z/OSMF tooling), and the mainframe, not GnuCOBOL, is the acceptance oracle. |
| 34-35 | Coverage, open decisions, the next stream (`posting`) | Acceptance rests on evidence and named decisions, not on a green screen. |

**20-minute version:** the estate report, the stream report, one QUIRK card,
one ticket PR, one Golden replay.

**If the lab is not built yet,** the last beat can run on what is in `main`
today: `make parity` (all green), then
`make parity-naive CASE=i02-tie-half-cent` and `work/parity/report-naive.md`.
That shows a plausible port differing from the mainframe by one cent on one
account, caught at field level.  Present it as the comparator, not as the lab.

Things to keep true while presenting:

* A click on Approve during the demo is not SME sign-off.
* Matching outputs do not prove correctness when both engines break a rule;
  that is why the lab judges each engine against the rulebook as well.
* Zero unclassified differences on seven cases is the replay gate for this
  stream.  It is not production certification.
* Do not quote session counts, durations or speed from another demo as a
  promise for the audience's estate.
* GnuCOBOL arithmetic (rounding, TRUNC, NUMPROC) is not bit-exact with
  Enterprise COBOL; the half-cent tie case is an example of a difference that
  needs mainframe evidence to settle.

### Next stream

The follow-on stream is the **`posting`** chain.  Its JCL roots are
`app/jcl/POSTTRAN.jcl` (`CBTRN02C`) followed by `app/jcl/TRANREPT.jcl`
(`CBTRN03C`); `POSTTRAN` is scheduled in `app/scheduler/CardDemo.ca7`.  It has
seven golden cases under `fixtures/posting`.

Datasets it shares with `interest`:

| Dataset | Layout | `posting` | `interest` |
|---|---|---|---|
| `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | `CVACT01Y` | updates | updates |
| `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` | `CVTRA01Y` | updates | reads |
| `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | `CVACT03Y` | reads | reads |

Both also write transaction records in the `CVTRA05Y` layout: `posting` to
`AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `interest` to a new generation of
`AWS.M2.CARDDEMO.SYSTRAN`.

The same six prompts apply with the stream name changed (`interest` ->
`posting`, `INTCALC` -> `POSTTRAN`, `java/interest` -> `java/posting`).  Run
prompt 7 in [DEMO-PROMPTS.md](DEMO-PROMPTS.md) first: it freezes the shared
record layouts so the second stream cannot change what the first one relies
on.

## 6. Reset

```sh
make reset          # drops datasets/, loadlib/, work/ and the database volume
git status          # nothing under app/ or fixtures/ may have changed
git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__   # tracked .pyc files a run rewrites
```

To start the whole demo again, drop the demo PRs and their branches, then
rerun the prompts from the frozen SHA.  The prompts open their PRs from
`devin/` branches; read the list before closing anything, because other work
may use the same prefix.

```sh
SHA=<frozen SHA from preflight>
gh pr list --state open --json number,headRefName,title \
  --jq '.[] | select(.headRefName | startswith("devin/"))'
gh pr list --state open --json number,headRefName \
  --jq '.[] | select(.headRefName | startswith("devin/")) | .number' \
  | xargs -r -n1 gh pr close --delete-branch
git fetch origin --prune && git checkout main && git pull --ff-only
git rev-parse HEAD                      # must equal $SHA
make reset && make up && make build && make parity
```

If `main` has moved past the frozen SHA (for example a demo PR was merged),
do not rewrite `main`.  Publish the frozen revision as a branch with
`git push origin "$SHA":refs/heads/demo-base`, and in every prompt replace
"the current main revision" with that SHA and "against main" with "against
demo-base".
