# Posting 03 (A3) - The rest of CBTRN03C and the utility steps of TRANREPT

> **PROPOSED - not approved.** Written by an agent in step 7. No session has been started for this ticket and no
> ticket exists in any external system. Plan and owners: [tracks.md](../../tracks.md). Freeze: [shared-layouts.md](../../shared-layouts.md).

- **Work item:** A3 - the converted TRANREPT (unload, filter, sort, `CBTRN03C`) reproduces the report outputs of all seven posting cases.
- **Source:** `Cognition-Partner-Workshops/ts-cobol-carddemo`, written on `demo/06-lab` at `d39ebac`. `app/jcl/TRANREPT.jcl` -> `app/proc/REPROC.prc`, SORT, `app/cbl/CBTRN03C.cbl`; `app/cpy/CVTRA07Y.cpy`, `CVTRA03Y.cpy`, `CVTRA04Y.cpy`.
- **Context:** `modernization/streams/posting/handoff.md` sections 2.1, 2.3 and 6; the annex as the pilot left it. This session first writes the remaining rules of `CBTRN03C` and of the two utility steps, with source lines.
- **Scope:**
  - Allowed files: `java/posting`, the packages of `CBTRN03C` and of the utility steps only; `modernization/rules/posting/**` (rules of the report); the annex, sections on the report.
  - In: the date filter and the card-number order for every case; the seven print lines of `CVTRA07Y` with their edited amounts; the account total at a change of card; the page total every 20 lines (`:282`); the totals at end of file, with the last amount counted a second time (`:197-203`); the empty report of `p07`; `DISPLAY TRAN-RECORD` and the other SYSOUT lines (`:180, :198-199`); the lookups of card, type and category that abend when not found (`:485-510`); `NEXT SENTENCE` (`:177`) reproduced and reported, though no case reaches it.
  - Input until ticket 02 is merged: the recorded `TRANSACT` file of each case (`expected/datasets/AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS.txt`).
  - Reads `TRANTYPE.PS` and `TRANCATG.PS` as files. It does not read the Db2 tables and does not build the link to Track B (ask PA-4).
  - Excluded: `CBTRN02C` (ticket 02).
- **Dependencies:** ticket 01 merged. Does not wait for ticket 02.
- **Acceptance:**
  - For `p01` to `p07`, the comparator reports no difference on `TRANREPT.G0001V00`, `sysout/TRANREPT.STEP10R.txt` and the `TRANREPT` entry of `rc.json`.
  - The seven interest cases pass unchanged; `make parity CHAIN=posting` stays green.
- **Verification:** `mvn -q -f java/posting/pom.xml verify`; a test per rule; evidence in `java/posting/evidence/03-cbtrn03c/`.
- **Deliverables:** one branch (`posting/03-cbtrn03c`) and one pull request; the annex extended; proposals for shared files; the answers still missing for PA-6, PA-7 and PA-8 listed.
- **Ticket writes:** prohibited
- **Restrictions:** as ticket 01; in addition the packages of `CBTRN02C` are not edited.

After tickets 02 and 03 are merged, the gate of the stream (piece "A gate" of `tracks.md`) runs the seven cases
end to end, both programs together; it changes no product code.
