# Page check - strategy-interest.html

Page `modernization/design/strategy-interest.html`, opened in Chrome (156.0.8078.47) on the session machine from its `file://` path. Baseline `6face58c092d254a9b89ce8bbfbddce561720a40` (head of `demo/01-intake`).

How it was driven:

* By hand with the desktop mouse and keyboard: loaded the page, clicked the Tickets tab, clicked box 06 in the ticket diagram (the ticket opened and scrolled into view), moved between tabs with the arrow keys, typed `BYTEA` into the Data map search (showing 5 of 43).
* Then by `tools/page_check.js` (Playwright attached to the same Chrome), which sends real mouse clicks and key presses to every tab, link, filter and collapsible section and compares what the page shows with a recount of `design.json` and the files under `tickets/` read from disk. It is not a file read.

Result on the final revision: every check passes. Screenshot: `evidence/page-check-final.png`.

| Check | Result | What was done |
|---|---|---|
| Loads with no console error and no missing asset | PASS | 0 console errors or warnings and 0 failed requests across the whole run; the page requests 0 other files and references 0 external assets |
| Tabs | PASS | 8 tabs clicked with the mouse (8 showed their panel alone); arrow, Home and End keys moved movemap > architecture > movemap > decisions > overview > overview |
| Collapsible sections | PASS | 31 of 31 opened and closed (mouse click, then Enter key) and have content; 5 of 5 Expand all / Collapse all pairs worked |
| Links | PASS | 403 links clicked: 17 cards, 49 diagram links and 337 in-text references landed on a visible target in the right tab (403/403); opening the file with #ticket-06 opens that ticket; 0 links leave the file |
| Filters | PASS | movemap: 8 options, 3 searches, Clear; ownership: 3 options, 2 searches, Clear; datamoves: 14 options, 2 searches, Clear; tickets: 5 options, 2 searches, Clear; datamap: 14 options, 3 searches, Clear; gaps: 6 options, 2 searches, Clear; register: 2 options, 2 searches, Clear; release: 8 options, 2 searches, Clear; every "showing N of M" and visible row count equals the recount of design.json; a diagram link to a filtered-out row clears the filter |
| Diagrams render | PASS | 3 inline SVG diagrams drawn (diagram-move 1180x565, 24 boxes, 10 arrows; diagram-flow 1180x431, 11 boxes, 13 arrows; diagram-tickets 1180x373, 10 boxes, 15 arrows); box and arrow counts equal design.json (move-map rows, ticket dependencies); no empty label, no label wider than its box or outside the drawing, no overlapping boxes or labels, no arrow drawn across a box it does not connect |
| Counts equal the data | PASS | 17 cards, 15 tables, the step, phase and ticket blocks, the numbers in 8 headings, 50 PROPOSED and 25 OPEN badges, the owner table and the reconciliation table equal a recount of design.json; ticket blocks equal the 10 files under tickets/; the embedded data equals design.json on disk |
| Baseline SHA shown | PASS | header shows 6face58c092d254a9b89ce8bbfbddce561720a40, the head of demo/01-intake; it is also in the Source line of each of the 10 ticket blocks |

## Found and fixed before the final revision

* Ticket diagram: five dependency arrows ran across boxes they do not connect (03, 04 and 08). The boxes were re-placed and the arrow from the schema to ticket 06 routed around the second column.
* Run diagram: the label "truncate + load" sat on top of the lane title "PostgreSQL 15 - schema carddemo". Moved below it.
* Both were seen by eye in the first screenshots; the first version of the script passed them. The script now also fails on an arrow drawn across a box and on overlapping labels, and it did fail on the first revision once those checks were added.
* Cutover item CO5 pointed at release decision REL-07, which is about a different question. Added REL-13 and pointed CO5 at it.

## Limits

* One browser (Chrome on Linux) at 1400x900. Other browsers and narrow screens were not checked.
* The Mermaid diagrams in `architecture.md` and `tickets/README.md` were not rendered here; they are drawn by the Git host.
