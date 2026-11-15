# UI test plan - rules-INTCALC.html

Board: `modernization/rules/interest/rules-INTCALC.html`, opened from its `file://` path in Chrome.
Driven by hand with mouse and keyboard (no call into the page's functions). Demo decisions are
test data, not SME sign-off; they are cleared at the end and the exported CSV is not committed.
The same rows are automated in `e2e/test_rules_board.py` (`make board-e2e`), one test per row.

| # | Check | Action | Expected result |
|---|---|---|---|
| 1 | Page loads clean | Open the file in a fresh tab with the DevTools console open; scroll from top to bottom | No console error; the summary "What the monthly interest run does" shows; all 20 cards show (each with id, type, statement) |
| 2 | Card count per type | Read the count next to each group heading and count the cards under it; compare with `rules.yaml` | RULE 8, QUIRK 6, HOLE 6 - equal to the number of entries of each type in `rules.yaml` (20 in all); "Cards 20" in the totals |
| 3 | Source excerpt | On card INT-R05 look at "Source excerpt"; click it; compare with `app/cbl/CBACT04C.cbl` lines 464-465 | Collapsed at first (no code visible); the click opens it; it shows lines 464 and 465 with the same text as the file (`COMPUTE WS-MONTHLY-INT` / `= ( TRAN-CAT-BAL * DIS-INT-RATE) / 1200`) |
| 4 | Approve, Reject, Comment | Type a reviewer name; click Approve on INT-R01; click Reject on INT-Q01; type a comment on INT-H01 and click Save comment | INT-R01 shows APPROVED, INT-Q01 shows REJECTED, INT-H01 shows the comment and the reviewer; totals read Approved 1, Rejected 1, Commented 1, Undecided 18 |
| 5 | Reviewer name required | Empty the reviewer name field; click Approve on INT-R02 | The decision is refused: INT-R02 stays UNDECIDED, a message on the card says the name is missing, the name field is marked; totals unchanged |
| 6 | Decisions survive reload | Put the reviewer name back; reload the page (F5) | The three decisions of check 4 and the totals are as before the reload |
| 7 | Export | Click Export CSV; open the downloaded file | A file `rules-INTCALC-decisions.csv` downloads; header plus 20 rows, one per card; the rows of INT-R01, INT-Q01, INT-H01 carry the decision, comment and reviewer of check 4; the others say UNDECIDED |
| 8 | Clear decisions | Click Clear decisions, then "Yes, clear all"; then reload | Every card shows UNDECIDED and no comment; totals read Approved 0, Rejected 0, Commented 0, Undecided 20; the same after the reload |
| 9 | Notice visible | Load the page at the top, window 1280 x 800 or larger, without scrolling | The "Not SME sign-off" notice is fully inside the visible window; it stays visible while scrolling |
