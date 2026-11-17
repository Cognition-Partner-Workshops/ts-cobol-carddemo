# Trantype-update 02 (B2) - The rest of COBTUPDT

> **PROPOSED - not approved.** Written by an agent in step 7. No session has been started for this ticket and no
> ticket exists in any external system. Plan and owners: [tracks.md](../../tracks.md). Freeze: [shared-layouts.md](../../shared-layouts.md).

- **Work item:** B2 - the converted `COBTUPDT` reproduces all seven cases of `trantype-update`. After this ticket `COBTUPDT` is converted and `trantype-dynamic` may start.
- **Source:** `Cognition-Partner-Workshops/ts-cobol-carddemo`, written on `demo/06-lab` at `d39ebac`. `app/app-transaction-type-db2/cbl/COBTUPDT.cbl`.
- **Context:** `modernization/streams/trantype-update/handoff.md` sections 3 to 5; `shared-layouts.md` section 3 and register rows SHR-05, SHR-06; the annex the pilot wrote; the answer of the target architect to the pilot's SQL proposals, if given. This session first writes the remaining rules of `COBTUPDT`, with source lines.
- **Scope:**
  - Allowed files: `java/trantype-update/**`; `modernization/rules/trantype-update/**`; `modernization/streams/trantype-update/**`; the stream's annex.
  - In: UPDATE (`:171-195`) and DELETE (`:201-226`); comment lines (`:120-121`); an unknown record type (`:122-128`); a negative SQLCODE with its message, return code 4 and the run going on (`:154-162`, `:230-233`); the recorded code `-403` of `t06`; SQLCODE +100 as the golden file of `t05` shows it (success, return code 0), with the source's `No records found.` branch behind a named switch (SHR-05); the failed open (`:83-88`).
  - The eight points of `shared-layouts.md` 3.3 each have a test, so that `trantype-dynamic` can rely on them.
  - Excluded: `CBTRNDYN`; any change to the two tables.
- **Dependencies:** ticket 01 merged.
- **Acceptance:**
  - For `t01` to `t07`, the comparator reports no difference on SYSOUT, `rc.json` (`STEP1` is 4 in `t06`) and both table snapshots.
  - The seven interest cases pass unchanged; `make parity CHAIN=trantype-update` stays green.
- **Verification:** `mvn -q -f java/trantype-update/pom.xml verify`; a test per rule; evidence in `java/trantype-update/evidence/02-rest/`.
- **Deliverables:** one branch (`trantype-update/02-rest`) and one pull request; the annex extended with every SQLCODE path; proposals for shared files; open rules listed as decisions, not decided.
- **Ticket writes:** prohibited
- **Restrictions:** as ticket 01.
