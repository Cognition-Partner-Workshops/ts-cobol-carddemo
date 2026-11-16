---
name: interest-ticket-verify
description: How to prove one ticket of the interest stream - run the Java job of java/interest on the fixture cases so that its output lands where the comparator reads it, compare each case with tools/parity/compare.py, read a difference, check the error path and the database, and write the evidence in the format the pilot used. Use after interest-ticket-build, when a parity case fails, or when a ticket's pull request needs its evidence. Pointer: the text is now in carddemo-ticket-verify (shared) and carddemo-stream-interest (annex, section 4).
---

# Interest ticket: verify

This skill was split in step 5c. Its text is now in two places; read both, in this order:

1. `.agents/skills/carddemo-ticket-verify/SKILL.md` - what holds for any batch stream of the estate.
2. `.agents/skills/carddemo-stream-interest/SKILL.md`, section 1 (values) and 4 (verify) - what holds for interest
   only, and for each shared item the citation and the example the shared skill left out.

The steps keep their numbers (1 to 10). The helper `parity-report.sh` is still in this directory and is unchanged:
`.agents/skills/interest-ticket-verify/parity-report.sh <ticket> <case> [<case> ...]`.

Where each section went: `modernization/design/skills-shared.md`, section 3.
