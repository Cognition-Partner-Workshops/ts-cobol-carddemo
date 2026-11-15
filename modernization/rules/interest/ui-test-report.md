# UI test report - rules-INTCALC.html

Run by hand (mouse and keyboard, Chrome, `file://`) against commit `3113ac4`, following `ui-test-plan.md`.
The demo decisions were cleared afterwards; the exported CSV is not in the repository. Not SME sign-off.

| # | Check | Result | Observed | Screenshot |
|---|---|---|---|---|
| 1 | Page loads clean | PASS | Console empty; summary and all 20 cards rendered | `evidence/check-01-page-load.png` |
| 2 | Card count per type | PASS | RULE 8, QUIRK 6, HOLE 6, Cards 20 - equal to `rules.yaml` | `evidence/check-02-type-counts.png` |
| 3 | Source excerpt | PASS | INT-R05 collapsed; click showed lines 464-465, same text as `CBACT04C.cbl` | `evidence/check-03-source-excerpt.png` |
| 4 | Approve, Reject, Comment | PASS | INT-R01 APPROVED, INT-Q01 REJECTED, INT-H01 comment and reviewer; totals 1 / 1 / 1 / 18 | `evidence/check-04-decisions.png` |
| 5 | Reviewer name required | PASS | Approve on INT-R02 refused with a message, name field marked, totals unchanged | `evidence/check-05-reviewer-required.png` |
| 6 | Decisions survive reload | PASS | After F5 the three decisions, reviewer and totals were unchanged | `evidence/check-06-reload-persistence.png` |
| 7 | Export | PASS | `rules-INTCALC-decisions.csv`: header + 20 rows; the three rows carry decision, comment, reviewer | `evidence/check-07-csv-export.png` |
| 8 | Clear decisions | PASS | All 20 cards UNDECIDED, no comments, also after F5 | `evidence/check-08-clear-persisted.png` |
| 9 | Notice visible | PASS | Notice fully visible at the top without scrolling (window 1280 x 928) and while scrolling | `evidence/check-09-notice-visible.png` |

Automated counterpart: `make board-e2e` (`e2e/test_rules_board.py`) - `9 passed in 3.19s`.
The screen recording of the manual run is attached to the session, not committed.
