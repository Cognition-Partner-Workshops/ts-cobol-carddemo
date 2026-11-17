# Trantype-update 01 (B1) - Pilot: COBTUPDT on t01-add

> **PROPOSED - not approved.** Written by an agent in step 7. No session has been started for this ticket and no
> ticket exists in any external system. Plan and owners: [tracks.md](../../tracks.md). Freeze: [shared-layouts.md](../../shared-layouts.md).

- **Work item:** B1 - a job in `java/trantype-update` applies the two inserts of `t01-add` as `COBTUPDT` does; the estate's comparator accepts SYSOUT, return code and both table snapshots.
- **Source:** `Cognition-Partner-Workshops/ts-cobol-carddemo`, written on `demo/06-lab` at `d39ebac`. `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl` -> `app/app-transaction-type-db2/cbl/COBTUPDT.cbl`.
- **Context:**
  - Hand-off: `modernization/streams/trantype-update/handoff.md`; the frozen contract of `COBTUPDT`: `shared-layouts.md` section 3.
  - Shared skills: the five `carddemo-*` skills. No annex exists; the pilot writes `.agents/skills/carddemo-stream-trantype-update/SKILL.md`.
  - Rules: none exist. The pilot lists, with source lines, the rules of the INSERT path, as the first entries of `modernization/rules/trantype-update/`.
- **Scope:**
  - Allowed files: `java/trantype-update/**` (new); `modernization/rules/trantype-update/**` (new); `modernization/streams/trantype-update/**`; `.agents/skills/carddemo-stream-trantype-update/**` (new).
  - In: read `INPFILE` (53 bytes), the echo line, record type `A`, the INSERT with the description padded to 50, the success line, one commit at the end, the two launcher lines the golden SYSOUT holds, return code 0.
  - The program is built so that the contract of `shared-layouts.md` 3.3 can be met: reachable by its name, no parameters, runs in a connection and transaction it is given, ends the run when it ends.
  - **Gaps to close** (`handoff.md` section 3): embedded SQL, SQLCODE handling, dynamic SQL, the dynamic call. For each: reproduce on `t01-add` what the case reaches, and propose the standard text and the behaviour row in the pull request. Dynamic SQL and the dynamic call are proposals from the source only; they are proven by `trantype-dynamic`. The shared skills are not edited.
  - Frozen: the two tables and `db/postgres/01-transaction-type.sql`; the comparator; the snapshot format.
  - Excluded: UPDATE, DELETE, comment lines, every SQLCODE other than zero, return code 4 (ticket 02); `CBTRNDYN`.
- **Dependencies:** approval of the freeze and the plan. Nothing of interest's module or schema.
- **Acceptance:**
  - `python3 tools/parity/compare.py trantype-update t01-add --actual <dir> --label java` exits 0: `sysout/MNTTRDB2.STEP1.txt`, `rc.json`, `db/carddemo.transaction_type.csv` (five rows, the two new descriptions padded), `db/carddemo.transaction_type_categor.csv` (empty).
  - The parity entry point is a script inside `java/trantype-update`; the `Makefile` lines are proposed, not added.
  - The seven interest cases pass unchanged: `make parity CHAIN=interest`, `make parity-java`.
  - `make parity CHAIN=trantype-update` (legacy) stays green.
- **Verification:** `mvn -q -f java/trantype-update/pom.xml verify` on JDK 21 with PostgreSQL 15 in a container; tests named by rule; evidence in `java/trantype-update/evidence/01-pilot/`.
- **Deliverables:** one branch (`trantype-update/01-pilot`) and one pull request; the new annex; the sections "Proposed for the shared skills" (the SQL standards and behaviour rows), "Proposed for the Makefile", "Proposed for CI"; the list of what the six other cases still need.
- **Ticket writes:** prohibited
- **Restrictions:** no edits to `app/`, `fixtures/`, `tools/`, `db/postgres/`, `java/interest`, `lab/`, the `Makefile`, the CI workflow, the shared skills, or anything of Track A. No merge or deploy.
