# Ticket 02 schema - parity report

Revision `b0e47a4f26d0c33852b4b2ec11879e124ee30c15`.

## Cases in the ticket's scope

None. The ticket says "Fixture cases: none (no data)": it adds DDL and SQL assertions, no Java and no
data, and nothing in `java/interest` reads the new tables yet (tickets 05-07 do). The proof of this
ticket is `test-output.txt` in this directory (psql apply and assertion output).

## Regression run (not acceptance of this ticket)

Run once at the revision above to show the branch behaves as its base does: `make parity-java`, then
`tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java` per case.

| Case | Result | In the ticket's scope | Why |
|---|---|---|---|
| i01-baseline | PASS (4 records, 50 fields, 0 differences) | no | no data in this ticket; unchanged from the base |
| i02-tie-half-cent | PASS (4 records, 50 fields, 0 differences) | no | same |
| i03-zero-rate-skipped | FAIL (2 records, 25 fields, 4 differences) | no | INT-R04 is not built on the base (`ZERO_RATE_NOT_BUILT`, job exit 12); owner is ticket 04 |
| i04-default-group-fallback | FAIL (2 records, 25 fields, 4 differences) | no | INT-R03 is not built on the base (job exit 12); owner is ticket 04 |
| i05-multi-category | PASS (3 records, 38 fields, 0 differences) | no | same as i01 |
| i06-negative-balance | PASS (2 records, 25 fields, 0 differences) | no | same as i01 |
| i07-nontie-rounding | PASS (4 records, 50 fields, 0 differences) | no | same as i01 |

i03 and i04 fail in the same way on `demo/05b-integration` as `interest-ticket-verify` step 8 records
(job exit 12, comparator exit 1); this branch changes no file the job runs.

`mvn -B -f java/interest/pom.xml clean verify` at the same revision: `Tests run: 60, Failures: 0,
Errors: 0, Skipped: 0` (unit), `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` (integration),
`BUILD SUCCESS`.

`.agents/skills/interest-ticket-verify/parity-report.sh` was not used: it needs at least one case the
ticket names, and this ticket names none.
