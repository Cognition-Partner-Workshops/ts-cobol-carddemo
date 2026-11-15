# Page check - stream-INTCALC.html

Page `modernization/streams/interest/stream-INTCALC.html`, opened in Chrome on the session machine from its `file://` path. Baseline `6face58c092d254a9b89ce8bbfbddce561720a40` (head of `demo/01-intake`).

How it was driven:

* By hand with the desktop mouse and keyboard: loaded the page, scrolled both diagrams, clicked edge label D3 in the stream diagram (landed on the D3 row of the Edges tab), opened a source reference and closed it with Esc, moved between tabs with the arrow keys, picked a dataset in the Outside-stream drop-down and typed a search.
* Then by `tools/page_check.js` (Playwright attached to the same Chrome), which sends real mouse clicks and key presses to every tab, link, filter and collapsible section and compares what the page shows with a recount of `manifest.json` read from disk. It is not a file read.

Result on the final revision: every check passes.

| Check | Result | What was done |
|---|---|---|
| Loads with no console error and no missing asset | PASS | 0 console errors or warnings and 0 failed requests across the whole run; the page requests 0 other files and references 0 external assets |
| Tabs | PASS | 8 tabs clicked with the mouse (8 showed their panel alone); arrow, Home and End keys moved scope > edges > scope > asks > overview > overview |
| Collapsible sections | PASS | 18 of 18 opened and closed (mouse click, then Enter key); 6 Expand all / Collapse all buttons worked |
| Links | PASS | 763 links clicked: 651/651 source references opened the cited lines and closed with Esc, 53/53 in-page links and 59/59 diagram links landed on a visible target; opening the file with #A4 lands on the ask; 0 links leave the file |
| Filters | PASS | edges: 5 drop-down options, 3 searches, Clear; 6 status x actor combinations; outside: 22 drop-down options, 3 searches, Clear; asks: 3 drop-down options, 2 searches, Clear; every "showing N of M" equals the recount of manifest.json |
| Diagrams render | PASS | 2 inline SVG diagrams drawn (1180x561 with 18 boxes, 1180x301 with 10 boxes), no empty or clipped label; the stream diagram carries all 18 edge ids of the manifest; the scheduler diagram has 5 setup and 5 folder jobs |
| Counts equal the data | PASS | 18 cards, 19 tables, the dataset matrix and the PROVEN/CANDIDATE totals in the text equal a recount of manifest.json (e.g. 18 edges = 15 PROVEN + 3 CANDIDATE; 111 outside rows) |
| Baseline SHA shown | PASS | header shows 6face58c092d254a9b89ce8bbfbddce561720a40, the head of demo/01-intake |

## Found and fixed before the final revision

* Scheduler diagram: condition labels overlapped the job boxes and the setup arrow crossed the folder title. Labels moved above the arrows and the title below the boxes.
* Stream diagram: the labels of D8 and D9 sat far from their own arrows, and three edge labels could be covered by a later arrow so a click went to the wrong edge. Labels now sit near the source end and are drawn above every line.
* Outside-stream table: twelve columns pushed the evidence column off the screen. Actor kind, module and program were folded into the Actor and Via cells; source references may wrap.

## Limits

* The first two runs of the script reported link failures that were faults of the script, not the page: it clicked the middle of a wrapped link's bounding box, and it clicked diagram links while they were under the sticky tab bar. The script now clicks the first box of the link text after scrolling it to the middle of the window.
* The check was run in one browser (Chrome on Linux) at 1400 x 900. Other browsers and narrow screens were not checked.
* A source reference shows only the cited lines stored in the page (at most 40 per reference), not the whole file.
