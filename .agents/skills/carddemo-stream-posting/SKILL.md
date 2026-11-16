---
name: carddemo-stream-posting
description: DRAFT - NOT PROVEN. Draft annex of the posting stream (POSTTRAN -> CBTRN02C, TRANREPT -> CBTRN03C) to the shared CardDemo skills - its programs, jobs, fixture cases, datasets and copybooks as the source shows them, which shared standards and COBOL behaviours apply unchanged, which need a posting citation, and which behaviours of the two programs no shared standard covers yet. No rule, design, Java or command exists for posting. Use only as the starting point for the posting stream's rules and design; do not build from it.
---

# Stream annex: posting

> **DRAFT - NOT PROVEN.** Nothing in this annex was built, run or compared. It was written in step 5c by reading
> `app/jcl/POSTTRAN.jcl`, `app/jcl/TRANREPT.jcl`, `app/cbl/CBTRN02C.cbl`, `app/cbl/CBTRN03C.cbl`, their copybooks,
> `tools/runjcl/chains.json` and `fixtures/posting`. There are no posting rules, no design, no data map, no Java
> module and no parity target. The shared skills are proven on the interest stream only.
> **Owner who must confirm: the target architect.** Section 4 is a list of proposals for the owner, not standards.

Line numbers are those of the file named. "Search" means `grep -nE` over `CBTRN02C.cbl` and `CBTRN03C.cbl`
returned nothing.

## 1. The stream, as the source shows it

| | |
|---|---|
| Stream, chain in `tools/runjcl/chains.json` | `posting` - "Daily transaction posting: POSTTRAN (CBTRN02C) then TRANREPT (REPRO, SORT, CBTRN03C)" |
| Programs | `app/cbl/CBTRN02C.cbl` (731 lines): validates and posts the daily transactions. `app/cbl/CBTRN03C.cbl` (649 lines): prints the transaction report |
| Jobs and steps | `app/jcl/POSTTRAN.jcl`: `STEP15 EXEC PGM=CBTRN02C` (line 23, no PARM). `app/jcl/TRANREPT.jcl`: `STEP05R EXEC PROC=REPROC` (23; IDCAMS REPRO, step `PRC001` of `app/proc/REPROC.prc:21`), `STEP05R EXEC PGM=SORT` (37; the step name is used twice), `STEP10R EXEC PGM=CBTRN03C` (59) |
| Setup jobs of the harness | `DEFGDGB`, `DALYREJS`, `ACCTFILE`, `XREFFILE`, `TCATBALF`, `DISCGRP`, `TRANTYPE`, `TRANCATG`, `TRANFILE` |
| Frozen clock | `2022-07-05 10:00:00.00` |
| Copybooks of CBTRN02C | `CVTRA06Y` (daily transaction, 350), `CVTRA05Y` (transaction, 350), `CVACT03Y` (card cross-reference, 50), `CVACT01Y` (account, 300), `CVTRA01Y` (category balance, 50) - lines 102, 107, 112, 121, 126 |
| Copybooks of CBTRN03C | `CVTRA05Y`, `CVACT03Y`, `CVTRA03Y` (transaction type, 60), `CVTRA04Y` (transaction category, 60), `CVTRA07Y` (report lines, 133) - lines 93, 98, 103, 108, 113 |
| Layout used by the comparator only | `tools/parity/layouts/REJTRAN.cpy` (reject record: `CVTRA06Y` plus the 80-byte trailer of `CBTRN02C:180-182`) |
| Shared with interest | `CVTRA05Y`, `CVACT03Y`, `CVACT01Y`, `CVTRA01Y`; the datasets ACCTDATA, CARDXREF, TCATBALF; the timestamp, status-line and abend paragraphs |
| Rule-id prefix | **none yet.** `modernization/rules/posting` does not exist. The prefix is for the owner to choose (interest uses `INT-`) |
| Rules, architecture, data map, tickets | **do not exist** |
| Maven module, job, schema script, policy switches | **do not exist** (`java/posting` is not there) |
| `<parity-target>`, `<out>`, report script | **do not exist** (section 5) |

**Datasets.**

| Dataset | Layout | Role | Shown by |
|---|---|---|---|
| `AWS.M2.CARDDEMO.DALYTRAN.PS` | `CVTRA06Y`, 350 | input of CBTRN02C, sequential | `POSTTRAN.jcl:30-31`; `CBTRN02C:29-32` |
| `AWS.M2.CARDDEMO.DALYTRAN.PS.INIT` | 350 | input of the setup job `TRANFILE` (initial load of TRANSACT) | `chains.json`; `app/jcl/TRANFILE.jcl:74` |
| `AWS.M2.CARDDEMO.CARDXREF.PS` -> `CARDXREF.VSAM.KSDS` | `CVACT03Y`, 50 | read by card number in both programs | `POSTTRAN.jcl:32-33`, `TRANREPT.jcl:67-68`; `CBTRN02C:40-44`, `CBTRN03C:33-37` |
| `AWS.M2.CARDDEMO.ACCTDATA.PS` -> `ACCTDATA.VSAM.KSDS` | `CVACT01Y`, 300 | read and rewritten by CBTRN02C; output | `POSTTRAN.jcl:39-40`; `CBTRN02C:51-55, 554` |
| `AWS.M2.CARDDEMO.TCATBALF.PS` -> `TCATBALF.VSAM.KSDS` | `CVTRA01Y`, 50 | read, created or rewritten by CBTRN02C; output | `POSTTRAN.jcl:41-42`; `CBTRN02C:57-61, 474-528` |
| `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | `CVTRA05Y`, 350 | written by CBTRN02C (`OPEN OUTPUT`, random); output; unloaded by TRANREPT | `POSTTRAN.jcl:28-29`; `CBTRN02C:34-38, 256, 564`; `TRANREPT.jcl:26-27` |
| `AWS.M2.CARDDEMO.DALYREJS(+1)`, recorded as `DALYREJS.G0001V00` | `REJTRAN`, 430 | rejects of CBTRN02C; output, `DISP=(NEW,CATLG,DELETE)` | `POSTTRAN.jcl:34-38`; `CBTRN02C:46-49, 446-451` |
| `AWS.M2.CARDDEMO.TRANSACT.BKUP(+1)` | 350 | REPRO unload, then SORTIN | `TRANREPT.jcl:29-33, 38-39` |
| `AWS.M2.CARDDEMO.TRANSACT.DALY(+1)` | 350 | SORTOUT (filtered by date, sorted by card), input of CBTRN03C | `TRANREPT.jcl:41-55, 65-66` |
| `AWS.M2.CARDDEMO.TRANTYPE.PS` -> `TRANTYPE.VSAM.KSDS` | `CVTRA03Y`, 60 | read by type in CBTRN03C | `TRANREPT.jcl:69-70`; `CBTRN03C:39-43` |
| `AWS.M2.CARDDEMO.TRANCATG.PS` -> `TRANCATG.VSAM.KSDS` | `CVTRA04Y`, 60 | read by type and category in CBTRN03C | `TRANREPT.jcl:71-72`; `CBTRN03C:45-49` |
| `AWS.M2.CARDDEMO.DATEPARM` | 80: start date, blank, end date | read once by CBTRN03C | `TRANREPT.jcl:73-74`; `CBTRN03C:122-125, 220-243` |
| `AWS.M2.CARDDEMO.TRANREPT(+1)`, recorded as `TRANREPT.G0001V00` | text, 133 | the report; output | `TRANREPT.jcl:76-80`; `CBTRN03C:51-53, 85` |
| `AWS.M2.CARDDEMO.DISCGRP.PS` | 50 | loaded by the setup job; neither program reads it | `chains.json`; search `DISCGRP`: nothing |
| `sysout/POSTTRAN.STEP15.txt`, `sysout/TRANREPT.STEP10R.txt` | text | DISPLAY lines of the two programs; output | `chains.json` (`sysout`) |
| `rc.json` | text | `POSTTRAN` (`STEP15` and the setup steps) and `TRANREPT` (`PRC001`, `STEP05R`, `STEP10R`) | `fixtures/posting/p01-baseline-mixed/expected/rc.json` |

**Fixture cases** (`fixtures/posting/<case>`, each with a `README.md`):

| Case | What its README says it proves | Recorded return codes |
|---|---|---|
| `p01-baseline-mixed` | four valid transactions post; the report lists all four | POSTTRAN 0, TRANREPT 0 |
| `p02-invalid-card` | a card with no cross-reference is rejected, reason 100 | POSTTRAN 4 (STEP15 = 4), TRANREPT 0 |
| `p03-account-missing` | account not found, reason 101 | POSTTRAN 4, TRANREPT 0 |
| `p04-overlimit-reject` | exactly on the limit is accepted, above it is rejected, reason 102 | POSTTRAN 4, TRANREPT 0 |
| `p05-expired-account` | after the expiry date is rejected, reason 103; on the date is accepted | POSTTRAN 4, TRANREPT 0 |
| `p06-credits-new-category` | negative amounts go to the cycle debit; a missing balance record is created | POSTTRAN 0, TRANREPT 0 |
| `p07-empty-day` | no transactions: nothing written, a report with headers only | POSTTRAN 0, TRANREPT 0 |

## 2. The shared standards, one by one

Verdicts: **UNCHANGED** - the shared text applies as it is and posting has only to add its example.
**CITATION** - the shared text applies, but it leaves a value open that posting must supply, and the line shows
what that value has to cover. **GAP** - part of what the line shows is not covered; see section 4.
No verdict has been tested.

| Id | Verdict | The line that shows it |
|---|---|---|
| S1.1 | CITATION | Sources 1 and 2 do not exist for posting (no ticket, rules, architecture, data map). Source 4 exists: `fixtures/posting/<case>/expected` |
| S1.2 | UNCHANGED | A candidate: `CBTRN02C:546` says the account is updated "to reflect posted trans", but a failed REWRITE (554-559) only sets reason 109 and the transaction is still written (442) |
| S2.1 | CITATION | Search `GO TO`, `THRU`, `THROUGH`: nothing. But `CBTRN03C:177` has `NEXT SENTENCE` inside the main loop (section 3, row n) |
| S2.2 | UNCHANGED | Eight copybooks and the reject layout (section 1). `CVTRA07Y` is print lines, not a data record: see S6.5 and section 4, proposal 2 |
| S2.3 | CITATION | No architecture. The driving file is sequential and unkeyed (`CBTRN02C:29-32`), one item is one daily transaction (202-219); three files are updated per item (440-442) |
| S2.4 | UNCHANGED | `END-OF-FILE`, `APPL-RESULT` with `APPL-AOK`/`APPL-EOF`, `WS-CREATE-TRANCAT-REC`, `WS-FIRST-TIME`: `CBTRN02C:142-146, 190`; `CBTRN03C:128, 150-154` |
| S2.5 | CITATION | The counters: `WS-TRANSACTION-COUNT`, `WS-REJECT-COUNT` (`CBTRN02C:185-186`); page, account and grand totals and the line counter (`CBTRN03C:129-136`) |
| S3.1 | CITATION | `DALYTRAN-AMT` S9(09)V99 (`REJTRAN.cpy`), `ACCT-CREDIT-LIMIT`, `ACCT-CURR-CYC-CREDIT`, `ACCT-CURR-CYC-DEBIT` S9(10)V99 (`CVACT01Y:8, 13, 14`), `WS-TEMP-BAL` S9(09)V99 (`CBTRN02C:187`). No rate |
| S3.2 | CITATION | Stream standard: posting has to write its own. The only COMPUTE is the limit test, `CBTRN02C:403-405`; it adds and subtracts only (search `MULTIPLY`, `DIVIDE`: nothing; no other COMPUTE) |
| S3.3 | UNCHANGED | Every arithmetic statement adds scale-2 values: `CBTRN02C:403-405, 508, 527, 547-551`; `CBTRN03C:200, 287, 297` |
| S3.4 | CITATION | `WS-TEMP-BAL` S9(09)V99 receives the sum of two S9(10)V99 items and an amount (`CBTRN02C:403-405`); the ADDs at 508, 527, 547-551 and `CBTRN03C:287, 297` have no `ON SIZE ERROR` (search: nothing). Posting needs its overflow switch and rule id |
| S3.5 | UNCHANGED | Zoned signed fields in every layout; no packed or binary field on a dataset (section 3, rows d, e). Needs a data map with the encoding rules |
| S3.6 | CITATION | `CBTRN02C:504` `INITIALIZE TRAN-CAT-BAL-RECORD` before a new balance record is written (510): INITIALIZE does not touch FILLER, so the FILLER of the new record is what the last READ left. `CBTRN02C:554` rewrites the whole account record. `CBTRN02C:447-448` builds the 430-byte reject record |
| S3.7 | CITATION | No keyed driving file in CBTRN02C. The order CBTRN03C reads in is made by the SORT step: `TRANREPT.jcl:41, 46` (`TRAN-CARD-NUM,263,16,ZD`, ascending) - a zoned-decimal sort key on a PIC X(16) field. See section 4, proposal 1 |
| S3.8 | CITATION | Same timestamp paragraph as interest: `CBTRN02C:692-705`. `TRAN-ORIG-TS` is copied from the input (436). The date range comes from a file, not a PARM: `CBTRN03C:220-243`, and again as literals in `TRANREPT.jcl:43-44`. Dates are compared as text: `CBTRN02C:414`, `CBTRN03C:173-174` |
| S4.1 | CITATION | Posting needs its own behaviours table; section 3 is a first reading, without tests |
| S5.1 | CITATION | No rules, so no QUIRK or OPEN id and no switch. Candidates in section 4, proposal 8 |
| S5.2 | UNCHANGED | Dead and debugging code: commented DISPLAYs (`CBTRN02C:207, 349, 381, 389, 401-402, 449, 704`), `CBTRN02C:377` "ADD MORE VALIDATIONS HERE", the debugging DISPLAYs that do run at `CBTRN03C:198-199` (they are SYSOUT, so they are output) |
| S6.1 | CITATION | Same status-line paragraph: `CBTRN02C:714-727`, `CBTRN03C:633-646`. Posting needs its legacy-event table: 12 opens and 12 closes, and e.g. `CBTRN03C:486-490` (`INVALID CARD NUMBER : `, status 23, abend) |
| S6.2 | GAP | The abend is the same (`CBTRN02C:707-711`, `CBTRN03C:626-630`). But CBTRN02C also has a second, non-fatal path: a rejected record and return code 4 (211-216, 229-230). Section 4, proposals 3 and 4 |
| S6.3 | CITATION | No architecture. `CBTRN02C:256` opens TRANSACT for OUTPUT; ACCTDATA and TCATBALF are updated in place (311, 329), so a rerun after a failure posts the same transactions again |
| S6.4 | CITATION | Outputs in section 1: five datasets, two SYSOUT files, `rc.json`. Two GDG outputs with `DISP=(NEW,CATLG,DELETE)` (`POSTTRAN.jcl:34`, `TRANREPT.jcl:76`); `rc.json` holds 4 for a normal end with rejects |
| S6.5 | GAP | `CBTRN03C:180` `DISPLAY TRAN-RECORD` puts every 350-byte transaction, card number included, into SYSOUT; `CBTRN03C:487` prints a card number. The shared text allows this for the run log. Not covered: the report dataset itself (section 4, proposal 2) |
| S7.1 | CITATION | No rule ids |
| S7.2 | CITATION | Behaviours with no fixture: section 3 names them (for example a type or category with no row, `CBTRN03C:496-500, 506-510`) |
| S7.3 | UNCHANGED | - |
| S7.4 | UNCHANGED | `fixtures/posting/<case>/expected` |
| S8.1 | UNCHANGED | - |
| S8.2 | UNCHANGED | - |
| S8.3 | UNCHANGED | - |
| S8.4 | CITATION | No frozen interfaces yet |
| S8.5 | UNCHANGED | - |

Count: 32 standards - UNCHANGED 12, CITATION 18, GAP 2.

## 3. The COBOL-behaviours table, one by one

"02C" is `CBTRN02C.cbl`, "03C" is `CBTRN03C.cbl`. The last column says whether the shared standard named for the
row in `carddemo-conversion-standards` section 4 is enough: UNCHANGED, CITATION or GAP, as in section 2.

| | Behaviour | In this stream | Evidence | Shared standard |
|---|---|---|---|---|
| a | Decimal truncation, no ROUNDED | ABSENT | Search `ROUNDED`: nothing; no multiplication or division, every operand has two decimals | - |
| b | High-order digits lost, no ON SIZE ERROR | PRESENT | Search `SIZE ERROR`: nothing. 02C:403-405 (S9(09)V99 receives S9(10)V99 operands), 508, 527, 547-551; 03C:287, 297; counters 02C:206, 214 | S3.4: CITATION |
| c | Intermediate precision inside COMPUTE | ABSENT | 02C:403-405 adds and subtracts values of one scale | - |
| d | Sign handling: unsigned fields that drop a sign; zoned overpunch | Overpunch PRESENT; a signed amount is DISPLAYed | 02C:548 tests the sign; 03C:198-199 `DISPLAY 'TRAN-AMT ' TRAN-AMT` - the golden file shows `TRAN-AMT 00000005000+` (`fixtures/posting/p04-overlimit-reject/expected/sysout/TRANREPT.STEP10R.txt`), which is how GnuCOBOL prints a signed zoned item | S3.5: UNCHANGED for the data; S6.5: CITATION for the DISPLAY (what the host prints: confirm on the mainframe) |
| e | Packed and binary fields; binary truncation by compile option | PRESENT in storage only | 03C:129-132 (`COMP-3` line counter and page size); binary: 02C:134, 142, 147-148; 03C:142, 150, 155-156. No packed or binary field in the eight copybooks or `REJTRAN.cpy` (search `COMP`: nothing) | S3.5: UNCHANGED |
| f | Non-numeric data in a numeric field | PRESENT | 02C:346, 383, 395, 474 and 03C:249, 485, 495, 505 `READ INTO` without a check; 02C:715 | S3.5: UNCHANGED |
| g | Fixed width: padding, truncation on MOVE, comparison with padding, STRING, reference modification | PRESENT | Reference modification 02C:414, 717, 724; 03C:173-174. 02C:447-448 (350 + 80 bytes). 03C:363-371 moves into print fields. Search `STRING`: nothing | S3.6: CITATION |
| h | EBCDIC against ASCII: code page and collating order | PRESENT | Text comparison of dates 02C:414, 03C:173-174; control break on a PIC X(16) card number 03C:181; the SORT key `TRANREPT.jcl:41, 46` | S3.7: CITATION; the SORT is a GAP (proposal 1) |
| i | REDEFINES | PRESENT in the programs | 02C:135, 160; 03C:143. Search `REDEFINES` in the eight copybooks: nothing | S2.2: UNCHANGED |
| j | FILLER and fixed record length across a round trip | PRESENT | 02C:554 (account rewritten whole), 504-510 (FILLER of a new balance record not set by INITIALIZE), 425-436 (transaction FILLER never set); `CVTRA06Y:18` | S3.6: CITATION |
| k | Tables: OCCURS, subscripts from 1, OCCURS DEPENDING ON | ABSENT | Search `OCCURS` in both programs and the eight copybooks: nothing | - |
| l | WORKING-STORAGE kept from record to record; fields used before set | PRESENT | 02C:185-187, 190; 03C:128-137. 03C:173 tests `TRAN-PROC-TS` and 200 adds `TRAN-AMT` after the read that hit end of file, so both use the previous record | S2.3: CITATION; the end-of-file use is a GAP (proposal 5) |
| m | Control break and end of file | PRESENT in 03C | 03C:181-188 (break on card number writes the account total of the previous card); 197-203 (end of file writes page and grand totals, never the last card's account total, and adds the last amount a second time: the golden report of p04 shows one detail line of 50.00 and totals of 100.00) | S2.3, S5.1: CITATION |
| n | GO TO, PERFORM THRU, fall-through | `NEXT SENTENCE` PRESENT; the rest ABSENT | 03C:177: a transaction outside the date range runs `NEXT SENTENCE`, which continues after the next period - the one that ends the loop at 206. Search `GO TO`, `THRU`: nothing | S2.1: GAP - `NEXT SENTENCE` is not named (proposal 6) |
| o | Level-88 condition names | PRESENT | 02C:143-144; 03C:151-152; 03C:515-520, 533-538 set the code with `ADD 8 TO ZERO GIVING` and `SUBTRACT ... FROM` itself | S2.4: UNCHANGED |
| p | Indexed files: sequential and random access, alternate index, START, duplicate keys, REWRITE after READ | PRESENT; alternate index and START ABSENT | 02C:34-61 (four indexed files, random); 256 `OPEN OUTPUT` of an indexed file; 564 random WRITE (a duplicate `TRAN-ID` abends, 566-577); 474-479 then 510 or 528 (read, then create or rewrite); 554 REWRITE after READ | S2.3: CITATION |
| q | File status codes and abends | PRESENT | 02C:236-343, 346-368, 451-463, 481-493, 512-524, 530-542, 566-577, 582-690, 707-727; 03C:220-243, 249-271, 345-358, 376-482, 484-510, 513-646. Not checked: the status after 02C:383, 395 and 554 beyond `INVALID KEY` | S6.1-S6.3: CITATION |
| r | JCL: PARM, GDG generation, DISP, step return code | PRESENT; PARM ABSENT | `POSTTRAN.jcl:23` (no PARM), `:34-38`; `TRANREPT.jcl:29-33, 38-39, 51-55, 65-66, 76-80` (four `(+1)` generations, two of them read again by a later step of the same job); 02C:229-230 (`MOVE 4 TO RETURN-CODE`) | S6.4: CITATION; return code 4 is a GAP (proposal 4) |
| s | DISPLAY output compared by parity | PRESENT | 02C:194, 227-228, 232, 476-477; 03C:160, 180, 198-199, 215, 232-233 | S6.5: CITATION |
| t | Business date from PARM against the system clock | PRESENT, from a file and from JCL literals instead of a PARM | 03C:220-243 (`DATEPARM`), `TRANREPT.jcl:43-44` (`PARM-START-DATE`, `PARM-END-DATE` in the SORT); clock 02C:437-438, 693 | S3.8: CITATION |
| u | CURRENT-DATE, hundredths, timestamp format | PRESENT in 02C | 02C:149-174, 692-705 - the paragraph of interest | S3.8: CITATION |
| v | Two-digit years and Julian dates | ABSENT | Search `ACCEPT`, `DAY-OF`, `JULIAN`: nothing | - |
| w | PARM length is never checked | ABSENT | No PARM, no LINKAGE SECTION. The nearest thing: 03C:221 reads 21 bytes of an 80-byte record and never checks that they are dates | - (proposal 7) |
| x | a failed keyed READ leaves the record area as it was | PRESENT | 02C:474-479 then 504 (INITIALIZE leaves the FILLER); 02C:383-391 (after reason 100, `CARD-XREF-RECORD` keeps the last card found) | S2.4: UNCHANGED |
| y | alphanumeric and numeric items mixed | PRESENT | 02C:394 (`XREF-ACCT-ID` 9(11) to `FD-ACCT-ID`), 414; 03C:488, 498, 508 (`MOVE 23 TO IO-STATUS`, a numeric literal into a two-byte group) | S2.2: UNCHANGED |
| z | abend by `CALL 'CEE3ABD'` with no STOP RUN after it | PRESENT | 02C:707-711; 03C:626-630; `tools/stubs/CEE3ABD.cbl` | S6.2: UNCHANGED |
| aa | dead and empty code | PRESENT | 02C:207, 349, 377, 381, 389, 401-402, 449, 704; 02C:374-375 (`ELSE CONTINUE`) | S5.2: UNCHANGED |

Count: 27 rows - ABSENT 5 (a, c, k, v, w), PRESENT 22 (n only as `NEXT SENTENCE`). Of the 22: the shared standard
is UNCHANGED for 8 (e, f, i, o, x, y, z, aa), needs a posting CITATION for 10 (b, d, g, j, m, p, q, s, t, u), and
leaves a GAP, in part or whole, for 4 (h, l, n, r).

**What the off-host run cannot settle** (beyond the shared paragraph): how the host prints the signed item of
03C:198-199 (row d); what `OPEN OUTPUT` does to a KSDS that already holds records (02C:256, row p; the cluster
is defined by `app/jcl/TRANFILE.jcl:49`); the order of a `ZD` sort key on non-digit bytes (row h); and whether a
relative generation `(+1)` named twice in one job is the same generation (row r) - `tools/runjcl` treats it so.
All four: **confirm on the mainframe.**

## 4. Behaviours no shared standard covers yet - proposals for the target architect

These are not standards. Each is a behaviour of CBTRN02C, CBTRN03C or their JCL, with the line that shows it,
that no shared standard and no behaviour row answers. The owner decides whether it becomes a shared standard, a
row of the behaviours table, or a posting rule.

1. **Utility steps: IDCAMS REPRO and SORT.** `TRANREPT.jcl:23-33` unloads a KSDS; `:37-55` filters by a date range
   (`INCLUDE COND`, 47-48) and sorts by card number as zoned decimal (41, 46). The filter and the order are
   business behaviour written in JCL, not in COBOL. No standard says how a utility step is converted, where its
   selection and its sort order live, or how its intermediate generations (`TRANSACT.BKUP`, `TRANSACT.DALY`) are
   treated. S3.7 covers key order of a file read by a program only.
2. **A printed report as an output.** `CBTRN03C:274-374` writes 133-byte lines with headers, a page break every
   20 lines (`FUNCTION MOD`, 282), page, account and grand totals, and edited fields (`CVTRA07Y:30, 54, 60, 66`:
   `-ZZZ,ZZZ,ZZZ.ZZ`, `+ZZZ,ZZZ,ZZZ.ZZ`). The comparator treats the report as text (`chains.json`: `"text"`).
   No standard covers numeric-edited pictures, page layout, or what S6.5 means for a report that prints account
   ids (`CBTRN03C:364`). Build-run section 6 has a related proposal from ticket 10.
3. **A rejected record that does not stop the run.** `CBTRN02C:208-216, 446-451`: a failed validation writes the
   record with an 80-byte reason trailer to DALYREJS and the run continues. S6.2 knows one stop path and "no
   skip"; it has no word for a business reject, its reason codes (100 to 103, and 109 at 556) or the reject
   dataset as a parity output.
4. **A normal end with a non-zero return code.** `CBTRN02C:229-230` ends with return code 4 when anything was
   rejected; four of the seven golden `rc.json` files hold `"STEP15": 4`. S6.2 says "the exit code is one
   constant" and interest's exit codes are 0 and stopped. Nothing says how a warning code maps to the job's exit
   status or what the scheduler does with it.
5. **The record area after end of file.** `CBTRN03C:172-174` tests the date of the record after a read that
   returned end of file, and `:197-203` adds its amount to the totals again. Row x covers a failed *keyed* READ
   only. The golden files hold the doubled total, so it has to be reproduced.
6. **`NEXT SENTENCE`.** `CBTRN03C:177`. Row n names GO TO, PERFORM THRU and fall-through. What the statement does
   here - leave the loop at the first transaction outside the date range - depends on where the next period is
   (206). No fixture has such a transaction: the SORT step filters them out first.
7. **Run dates from a parameter file.** `CBTRN03C:220-243` and `TRANREPT.jcl:43-44, 73-74`: the same date range
   is given twice, once as a file and once as literals in the job. S3.8 covers a PARM and the clock. Nothing says
   which of the two is the parameter of the converted job, or what happens when they differ or the file is
   empty (235-236 then ends the run without a report line).
8. **Validation order and the last reason wins.** `CBTRN02C:403-420`: the limit test and the expiry test both
   run; when both fail, reason 103 replaces 102. The limit is tested against cycle credit minus cycle debit
   plus the amount (403-405; `docs/BASELINE.md:113`), not against the balance. These are rules for the posting
   rulebook (QUIRK or OPEN), which S5.1 then covers - but no standard says how the *order* of validations is
   kept when each becomes its own unit of code under S2.1.
9. **An error that is noted and ignored.** `CBTRN02C:554-559`: a failed account REWRITE sets reason 109, and
   nothing reads it: the transaction is written (442), no reject is written, and the next record clears the
   reason (208). The file status of the keyed READs at 383 and 395 is not tested beyond `INVALID KEY`. Section 6
   of the standards assumes every status is tested.
10. **Create-or-update of a keyed record.** `CBTRN02C:474-528`: read by key; not found is not an error but
    creates the record (the DISPLAY at 476-477 is SYSOUT), found rewrites it. Row p lists access types but not
    this pattern, nor `OPEN OUTPUT` of an indexed file (256).
11. **Two programs in one stream.** Interest is one program and one job. Posting is two jobs, one of them with
    three steps, and the second reads what the first wrote. Nothing in the shared skills says whether a stream is
    one Spring Batch job or several, or how `rc.json` (two jobs) and two SYSOUT files map to it.

## 5. Commands that work only for interest today

A posting session cannot run these, or cannot run them with `posting` in place of `interest`:

| Command | Why |
|---|---|
| `make parity-java [CASE=...]` | the target runs `java/interest/target/interest.jar` on `fixtures/interest` only (`Makefile`, `INTEREST_JAR`, `ls fixtures/interest`) |
| `make recon-java`, `make sidebyside-java` | interest's data move kit (`com.carddemo.interest.recon.DataMoveKit`) |
| `make parity-naive CASE=...` | `tools/parity/naive_ref.py interest` |
| `make rules`, `make rules-check`, `make board-e2e` | `modernization/rules/interest/tools` |
| `make design`, `make design-check` | builds the interest design outputs |
| `.agents/skills/interest-ticket-verify/parity-report.sh` | `fixtures/interest`, `java/interest/evidence`, `make parity-java` are written into it |
| `mvn -f java/<stream>/pom.xml ...` (build steps 7, 8; verify step 8; the combined check) | there is no `java/posting` |
| the job by hand, the error path, the job-repository and `interest_run` queries (annex of interest, section 4, steps 4, 6, 7) | interest's jar, parameters, tables |
| `docker compose exec -T db psql ... -f /docker-entrypoint-initdb.d/02-interest.sql` | interest's schema; posting has no script under `db/postgres` |

Commands that already take `posting`, from the legacy side (`AGENTS.md`, `carddemo-parity`; not run for this draft):
`make run CHAIN=posting`, `make parity CHAIN=posting [CASE=...]`, and
`python3 tools/parity/compare.py posting <case> --actual <dir> --label java`. In step 5c `make parity` ran all
chains, posting included, and its seven cases passed.

## 6. Not in this draft

No rule, no rule id, no standard, no Java, no test name, no legacy-event table, no checklist. No command was
run for posting other than `make parity`. The verdicts of sections 2 and 3 are a reading of the source against
the shared text; a posting pilot ticket is what would prove or correct them.
