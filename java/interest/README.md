# INTCALC / CBACT04C - Java port

Java 21 port of the interest batch program `app/cbl/CBACT04C.cbl` (job `INTCALC`, step
`STEP15`). It reads the same PS inputs as the `interest` chain and writes the same outputs in
the recorder format, so `tools/parity/compare.py` can judge it field by field. No database,
no framework: file in, file out.

```sh
make parity-java                 # mvn verify (unit + recorded-case tests), then parity on
                                 # the 7 golden cases and the 10 characterization cases
make parity-java MVN_FLAGS="-s work/m2-settings.xml"   # same, with a local Maven mirror
java -jar java/interest/target/carddemo-interest.jar \
     --input fixtures/interest/i01-baseline/input --out /tmp/out \
     --parm 2022071800 --clock "2022-07-05 10:00:00.00"
```

Reports: `work/parity/report-java.md` (golden), `work/parity/report-java-characterization.md`,
and `*-fields.md` with the comparator's per-case field detail.

## Layout

| Path | What |
|---|---|
| `src/main/java/.../Cbact04c.java` | the program, paragraph by paragraph |
| `src/main/java/.../InterestJob.java` | INTCALC driver: PS load, run, recorder-format outputs |
| `src/main/java/.../IndexedFile.java` | KSDS + alternate index with the setup jobs' load rules |
| `src/main/java/.../Zoned.java` | zoned decimal with EBCDIC overpunch signs, COBOL truncation |
| `src/test/java/...` | `RecordedCasesTest` (byte equality on all 17 recorded cases), `Cbact04cQuirksTest`, `ZonedTest` |
| `parity/run_parity.py` | runs the jar, `compare.py`, and a row hash per output record |
| `characterization/` | extra inputs and their outputs recorded from the real COBOL |

## Legacy behaviour reproduced (observed under GnuCOBOL, not inferred)

| Behaviour | Seen in |
|---|---|
| Interest `(TRAN-CAT-BAL * DIS-INT-RATE) / 1200` truncated toward zero, no rounding | i02, i07, c01, c02 |
| Last account on TCATBALF never rewritten (EOF branch unreachable) | all cases |
| Rate 0: no transaction, but the account is still rewritten with both cycle amounts zeroed | i03, c03 |
| Missing group: two warning lines, `DEFAULT` group used | i04, c09 |
| Negative balances give negative interest; `-0.0033` is written as `+0.00` | i06, c02 |
| No ON SIZE ERROR: interest, total and balance silently lose high-order digits | c04 |
| Missing DEFAULT row, account or card xref: abend U999, RC 12, libcob implicit-CLOSE warnings in SYSOUT, earlier writes kept | c05, c06, c07, c08 |
| Setup load is sequential: duplicate and out-of-sequence keys are dropped | c08, c09 |
| Account with several cards: first card in key order goes on TRAN-CARD-NUM | c10 |

These are findings to raise with the business, not things this port fixes.

## Characterization cases

The seven golden cases do not exercise abends, overflow or load order. `characterization/`
adds ten synthetic cases. `make_inputs.py` writes their inputs, and
`record_characterization.py` runs the unchanged recorder (`tools/parity/record.py`, real
CBACT04C under GnuCOBOL in the estate container) to write their `expected/`. The recorder
is pointed at this folder, so `fixtures/` is never touched:

```sh
make up build
docker compose exec -T estate python3 java/interest/characterization/make_inputs.py
docker compose exec -T estate python3 java/interest/characterization/record_characterization.py
```

## rc.json

The recorder merges step return codes by step name across the setup jobs and INTCALC.
What survives is DISCGRP `STEP05`/`STEP10` (DELETE/DEFINE), XREFFILE `STEP20`-`STEP30`
(AIX/PATH/BLDINDEX) and INTCALC `STEP15`. The Java job emits the same shape: the setup steps
are 0, and `STEP15`/`maxrc` is the program's RC (0, or 12 after an abend).

## Not covered

- OPEN/CLOSE/REWRITE/WRITE I/O errors other than status 23 (the harness cannot produce them).
- EBCDIC input (all cases are ASCII with EBCDIC-style signs, like the estate).
- Loading the output into PostgreSQL: that is a separate step.
