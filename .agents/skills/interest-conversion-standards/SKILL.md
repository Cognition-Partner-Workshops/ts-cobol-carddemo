---
name: interest-conversion-standards
description: Conversion standards for the interest stream (INTCALC -> CBACT04C) moving to Java 21 / Spring Batch 5 / PostgreSQL 15. Every ticket session under modernization/design/tickets must follow them and fill in the review checklist at the end. Use when implementing or reviewing any interest ticket, or when a question comes up about how a CBACT04C behaviour is reproduced in Java. Pointer: the text is now in carddemo-conversion-standards (shared) and carddemo-stream-interest (annex, section 2).
---

# Interest stream conversion standards

This skill was split in step 5c. Its text is now in two places; read both, in this order:

1. `.agents/skills/carddemo-conversion-standards/SKILL.md` - what holds for any batch stream of the estate.
2. `.agents/skills/carddemo-stream-interest/SKILL.md`, section 2 (standards, behaviours table, legacy-event table, review checklist in 2.9) - what holds for interest
   only, and for each shared item the citation and the example the shared skill left out.

Every standard keeps its id (S1.1 to S8.5) and its section number; the behaviours keep their letters (a to aa). The
standards are still **PROPOSED**; the owner who must confirm them is still **the target architect.**

Where each section went: `modernization/design/skills-shared.md`, section 3.
