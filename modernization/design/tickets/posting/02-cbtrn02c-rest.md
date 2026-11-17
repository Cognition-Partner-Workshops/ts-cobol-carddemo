# Posting 02 (A2) - The rest of CBTRN02C

> **PROPOSED - not approved.** Written by an agent in step 7. No session has been started for this ticket and no
> ticket exists in any external system. Plan and owners: [tracks.md](../../tracks.md). Freeze: [shared-layouts.md](../../shared-layouts.md).

- **Work item:** A2 - the converted `CBTRN02C` reproduces the POSTTRAN outputs of all seven posting cases.
- **Source:** `Cognition-Partner-Workshops/ts-cobol-carddemo`, written on `demo/06-lab` at `d39ebac`. `app/jcl/POSTTRAN.jcl` -> `app/cbl/CBTRN02C.cbl`.
- **Context:** `modernization/streams/posting/handoff.md` sections 2.2, 4 and 7; the annex as the pilot left it; the rules the pilot started. This session first writes the remaining rules of `CBTRN02C`, with source lines, each quirk behind a named switch whose default is the legacy behaviour.
- **Scope:**
  - Allowed files: `java/posting`, the packages of `CBTRN02C` only; `modernization/rules/posting/**` (rules of `CBTRN02C`); the annex, sections on `CBTRN02C`.
  - In: reject reasons 100, 101, 102, 103 and their order, the last reason wins (`:383-420`); the 430-byte reject record (`:446-451`); return code 4 when anything was rejected (`:230`); the limit tested against the cycle amounts (`:403-405`); creation of a missing category balance and its SYSOUT line (`:474-510`); negative amounts to the cycle debit (`:547-551`); reason 109, noted and ignored (`:554-559`); the empty day; the abend paths and status lines.
  - Excluded: `CBTRN03C`, the REPRO and SORT steps (ticket 03); tables of `02-interest.sql` until SHR-01 is answered.
- **Dependencies:** ticket 01 merged.
- **Acceptance:**
  - For `p01` to `p07`, the comparator reports no difference on `TRANSACT.VSAM.KSDS`, `ACCTDATA.VSAM.KSDS`, `TCATBALF.VSAM.KSDS`, `DALYREJS.G0001V00`, `sysout/POSTTRAN.STEP15.txt` and the `POSTTRAN` entry of `rc.json` (`STEP15` is 4 in `p02` to `p05`).
  - The interface to `CBTRN03C` that ticket 01 fixed is unchanged.
  - The seven interest cases pass unchanged; `make parity CHAIN=posting` stays green.
- **Verification:** `mvn -q -f java/posting/pom.xml verify`; a test per rule; evidence in `java/posting/evidence/02-cbtrn02c/`.
- **Deliverables:** one branch (`posting/02-cbtrn02c`) and one pull request; the annex extended; proposals for shared files as sections of the pull request; open rules listed as decisions for the business, not decided.
- **Ticket writes:** prohibited
- **Restrictions:** as ticket 01; in addition the packages of `CBTRN03C` are not edited.
