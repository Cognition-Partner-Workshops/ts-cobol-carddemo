# Trantype-dynamic 01 (B3) - CBTRNDYN: dynamic SQL and the call by name

> **PROPOSED - not approved.** Written by an agent in step 7. No session has been started for this ticket and no
> ticket exists in any external system. Plan and owners: [tracks.md](../../tracks.md). Freeze: [shared-layouts.md](../../shared-layouts.md).

- **Work item:** B3 - a job in `java/trantype-dynamic` runs the statement of `DYNSQL` and then the converted `COBTUPDT`, found by the name in the job's parameter, as `CBTRNDYN` does; the comparator accepts the three cases.
- **Source:** `Cognition-Partner-Workshops/ts-cobol-carddemo`, written on `demo/06-lab` at `d39ebac`. `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl` -> `app/app-transaction-type-db2/cbl/CBTRNDYN.cbl` -> `COBTUPDT`.
- **Context:** `modernization/streams/trantype-dynamic/handoff.md`; `shared-layouts.md` section 3.3; the annex and rules of `trantype-update`; the pilot's proposals for dynamic SQL and the dynamic call, and the target architect's answer if given. This session first writes the rules of `CBTRNDYN`, with source lines, and its own annex.
- **Scope:**
  - Allowed files: `java/trantype-dynamic/**` (new); `modernization/rules/trantype-dynamic/**` (new); `modernization/streams/trantype-dynamic/**`; `.agents/skills/carddemo-stream-trantype-dynamic/**` (new).
  - In: building the statement from `DYNSQL` (`:67-85`): comment lines dropped, columns 1 to 72, the printed line count; PREPARE and EXECUTE with their SYSOUT lines and return code 8 (`:87-102`); the call of the program named in the parameter, made even after a failed statement (`:59-63`); one unit of work across the call; the callee ending the run, so that `CBTRNDYN: END` is not printed; the step's return code as recorded.
  - `d03` is reproduced as recorded (the later UPDATE fails, the table is unchanged, return code 4); the other behaviour stays behind a named switch (SHR-06).
  - The converted `COBTUPDT` is used as it is. A change it needs is asked of `trantype-update`.
  - Excluded: which names and which statements are allowed in production (asks TD-1, TD-2) - reported, not decided.
- **Dependencies:** trantype-update ticket 02 merged and its seven cases passing.
- **Acceptance:**
  - For `d01`, `d02`, `d03`, the comparator reports no difference on `sysout/MNTTRDYN.STEP1.txt`, `rc.json` (`STEP1` is 4 in `d03`) and both table snapshots.
  - The seven `trantype-update` cases and the seven interest cases pass unchanged; `make parity CHAIN=trantype-dynamic` stays green.
- **Verification:** `mvn -q -f java/trantype-dynamic/pom.xml verify`; a test per rule; evidence in `java/trantype-dynamic/evidence/01-cbtrndyn/`.
- **Deliverables:** one branch (`trantype-dynamic/01-cbtrndyn`) and one pull request; the stream's annex; what the three cases proved or corrected in the pilot's proposals; proposals for shared files.
- **Ticket writes:** prohibited
- **Restrictions:** no edits to `app/`, `fixtures/`, `tools/`, `db/postgres/`, `java/interest`, `java/trantype-update`, `lab/`, the `Makefile`, the CI workflow, the shared skills, or anything of Track A. No merge or deploy.
