# Posting 01 (A1) - Pilot: thin slice across both programs on p01-baseline-mixed

> **PROPOSED - not approved.** Written by an agent in step 7. No session has been started for this ticket and no
> ticket exists in any external system. Plan and owners: [tracks.md](../../tracks.md). Freeze: [shared-layouts.md](../../shared-layouts.md).

- **Work item:** A1 - a job in `java/posting` posts the four transactions of `p01-baseline-mixed` as `CBTRN02C` does and prints the report as `CBTRN03C` does, after the unload, date filter and sort of `TRANREPT.jcl`; the estate's comparator accepts the outputs.
- **Source:** `Cognition-Partner-Workshops/ts-cobol-carddemo`, written on `demo/06-lab` at `d39ebac`. `app/jcl/POSTTRAN.jcl` -> `app/cbl/CBTRN02C.cbl`; `app/jcl/TRANREPT.jcl` -> `app/cbl/CBTRN03C.cbl`.
- **Context:**
  - Hand-off: `modernization/streams/posting/handoff.md`.
  - Draft annex: `.agents/skills/carddemo-stream-posting/SKILL.md` (DRAFT - the pilot proves or corrects the rows it touches).
  - Shared skills: `carddemo-conversion-standards`, `carddemo-ticket-build`, `carddemo-ticket-verify`, `carddemo-pitfalls`, `carddemo-parity`.
  - Mapping: `data-map-interest.md` rows of `CVACT01Y`, `CVACT03Y`, `CVTRA01Y`, `CVTRA05Y`; encoding rules E1-E4, fixture code page only.
  - Rules: none exist. The pilot lists, with source lines, the rules of the valid path it implements, as the first entries of `modernization/rules/posting/`.
- **Scope:**
  - Allowed files: `java/posting/**` (new); `modernization/rules/posting/**` (new); `modernization/streams/posting/**`; `.agents/skills/carddemo-stream-posting/SKILL.md`.
  - In: the valid path of `CBTRN02C` (read the daily file, look up card and account, update the category balance, the account and the transaction file, the four SYSOUT lines, return code 0); the REPRO, the date filter and the sort of `TRANREPT.jcl:23-55`; the report of `CBTRN03C` as `p01` prints it, including the totals at end of file and the SYSOUT lines.
  - File adapters only. No table of `db/postgres/02-interest.sql` is written (SHR-01).
  - Frozen: everything in `shared-layouts.md`; the recorded output format; the comparator.
  - Excluded: rejects and return code 4, creation of a category balance, the empty day, error paths (ticket 02, 03); database steps; host code page; a posting lab.
- **Dependencies:** approval of the freeze and the plan; SHR-02 answered (how `java/posting` reaches the record codec).
- **Acceptance:**
  - `python3 tools/parity/compare.py posting p01-baseline-mixed --actual <dir> --label java` exits 0: `TRANSACT` (4 records), `ACCTDATA` (2), `TCATBALF` (4), `DALYREJS` (empty), the report (13 lines), both SYSOUT files, `rc.json`.
  - The parity entry point of the stream is a script inside `java/posting`; the `Makefile` lines are proposed, not added.
  - The seven interest cases pass unchanged: `make parity CHAIN=interest`, `make parity-java`.
  - `make parity CHAIN=posting` (legacy) stays green.
- **Verification:** `mvn -q -f java/posting/pom.xml verify` on JDK 21; unit tests named by rule; the comparator report of `p01` and the test summary from the last commit, in `java/posting/evidence/01-pilot/`.
- **Deliverables:** one branch (`posting/01-pilot`) and one pull request; the annex updated from what was built and run; a section "Proposed for the Makefile", "Proposed for CI" and "Proposed for the shared skills"; the list of what the six other cases still need.
- **Ticket writes:** prohibited
- **Restrictions:** no edits to `app/`, `fixtures/`, `tools/`, `java/interest`, `db/postgres/`, `lab/`, the `Makefile`, the CI workflow, the shared skills, or anything of Track B. No merge or deploy.
