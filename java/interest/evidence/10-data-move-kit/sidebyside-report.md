# Side-by-side comparison - ticket 10-data-move-kit

Revision of the code: `8a58bb9c54b8c66838c3a560dd8dfd4f3aadd8dc` (branch `demo/05-10-data-move-kit`). Produced 2026-11-15T13:02:59Z.

## `make sidebyside-java` (recorded legacy run against work/parity-java/<case>/actual, timestamps format)

```
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [side by side, timestamps format]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i02-tie-half-cent [side by side, timestamps format]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i03-zero-rate-skipped [side by side, timestamps format]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i04-default-group-fallback [side by side, timestamps format]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i05-multi-category [side by side, timestamps format]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i06-negative-balance [side by side, timestamps format]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i07-nontie-rounding [side by side, timestamps format]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
report: work/sidebyside-java/report.md
exit code 0
```

## `make sidebyside-java TS=exact`

```
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [side by side, timestamps exact]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i02-tie-half-cent [side by side, timestamps exact]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i03-zero-rate-skipped [side by side, timestamps exact]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i04-default-group-fallback [side by side, timestamps exact]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i05-multi-category [side by side, timestamps exact]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i06-negative-balance [side by side, timestamps exact]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
interest/i07-nontie-rounding [side by side, timestamps exact]: EQUAL (0 difference lines, 0 accepted by the timestamp rule; per class: none)
report: work/sidebyside-java/report.md
exit code 0
```

## A target run that differs: i02-tie-half-cent with rounding HALF_EVEN and another clock

`make parity-java CASE=i02-tie-half-cent INTEREST_OUT=work/shadow INTEREST_OPTS=--interest.policy.rounding=HALF_EVEN INTEREST_CLOCK="2022-08-05 11:00:00.00"` (the gate fails on purpose), then the two timestamp rules:

### `make sidebyside-java LEGACY=fixtures/interest/i02-tie-half-cent/expected TARGET=work/shadow/i02-tie-half-cent/actual NAME=i02-shadow TS=format SIDEBYSIDE_OUT=work/sidebyside-shadow-format`

```
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i02-shadow [side by side, timestamps format]: DIFFERENT (6 difference lines, 4 accepted by the timestamp rule; per class: {INT-Q01=1, INT-Q03=4, defect=1})
report: work/sidebyside-shadow-format/report.md
make: *** [Makefile:290: sidebyside-java] Error 1
exit code 2
```

#### Side by side - i02-shadow: **DIFFERENT**

Legacy run: `fixtures/interest/i02-tie-half-cent/expected`. Target run: `work/shadow/i02-tie-half-cent/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=50 records=4 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=format`: TRAN-ORIG-TS and TRAN-PROC-TS are compared for format only (YYYY-MM-DD-HH.MM.SS.hh0000): two values of that format that differ are listed with class INT-Q03 and accepted.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| INT-Q01 | 1 |
| INT-Q03 | 4 |
| defect | 1 |

| Where | Field | Legacy | Target | Class | Why | Accepted by the timestamp rule |
|---|---|---|---|---|---|---|
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 1 (ACCT-ID=2003) | `ACCT-CURR-BAL` | 1247.85 | 1247.86 | defect | no decision or gap explains a difference in this field | no |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-AMT` | 12.35 | 12.36 | INT-Q01 | the amounts are one cent apart: truncated against rounded | no |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-08-05-11.00.00.000000 | INT-Q03 | both values have the format; the clocks of the two runs differ | yes |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-08-05-11.00.00.000000 | INT-Q03 | both values have the format; the clocks of the two runs differ | yes |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 2 (TRAN-ID=2022071800000002) | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-08-05-11.00.00.000000 | INT-Q03 | both values have the format; the clocks of the two runs differ | yes |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 2 (TRAN-ID=2022071800000002) | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-08-05-11.00.00.000000 | INT-Q03 | both values have the format; the clocks of the two runs differ | yes |


### `make sidebyside-java LEGACY=fixtures/interest/i02-tie-half-cent/expected TARGET=work/shadow/i02-tie-half-cent/actual NAME=i02-shadow TS=exact SIDEBYSIDE_OUT=work/sidebyside-shadow-exact`

```
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i02-shadow [side by side, timestamps exact]: DIFFERENT (6 difference lines, 0 accepted by the timestamp rule; per class: {INT-Q01=1, INT-Q03=4, defect=1})
report: work/sidebyside-shadow-exact/report.md
make: *** [Makefile:290: sidebyside-java] Error 1
exit code 2
```

#### Side by side - i02-shadow: **DIFFERENT**

Legacy run: `fixtures/interest/i02-tie-half-cent/expected`. Target run: `work/shadow/i02-tie-half-cent/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=50 records=4 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the legacy run's clock, and a value that differs is a difference of class INT-Q03.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| INT-Q01 | 1 |
| INT-Q03 | 4 |
| defect | 1 |

| Where | Field | Legacy | Target | Class | Why | Accepted by the timestamp rule |
|---|---|---|---|---|---|---|
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 1 (ACCT-ID=2003) | `ACCT-CURR-BAL` | 1247.85 | 1247.86 | defect | no decision or gap explains a difference in this field | no |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-AMT` | 12.35 | 12.36 | INT-Q01 | the amounts are one cent apart: truncated against rounded | no |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-08-05-11.00.00.000000 | INT-Q03 | the values differ although the comparison is exact: the target run did not have the legacy clock | no |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-08-05-11.00.00.000000 | INT-Q03 | the values differ although the comparison is exact: the target run did not have the legacy clock | no |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 2 (TRAN-ID=2022071800000002) | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-08-05-11.00.00.000000 | INT-Q03 | the values differ although the comparison is exact: the target run did not have the legacy clock | no |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 2 (TRAN-ID=2022071800000002) | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-08-05-11.00.00.000000 | INT-Q03 | the values differ although the comparison is exact: the target run did not have the legacy clock | no |


## The same clock only: i01-baseline at another time (rounding as legacy)

```
make sidebyside-java LEGACY=fixtures/interest/i01-baseline/expected TARGET=work/shadow-clock/i01-baseline/actual NAME=i01-clock TS=format SIDEBYSIDE_OUT=work/sidebyside-clock-format
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-clock [side by side, timestamps format]: EQUAL (4 difference lines, 4 accepted by the timestamp rule; per class: {INT-Q03=4})
report: work/sidebyside-clock-format/report.md
exit code 0
```

```
make sidebyside-java LEGACY=fixtures/interest/i01-baseline/expected TARGET=work/shadow-clock/i01-baseline/actual NAME=i01-clock TS=exact SIDEBYSIDE_OUT=work/sidebyside-clock-exact
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-clock [side by side, timestamps exact]: DIFFERENT (4 difference lines, 0 accepted by the timestamp rule; per class: {INT-Q03=4})
report: work/sidebyside-clock-exact/report.md
make: *** [Makefile:290: sidebyside-java] Error 1
exit code 2
```

## Error path: a legacy directory that is not a recorded run

```
mvn -q -f java/interest/pom.xml -DskipTests package
interest/nothing [side by side, timestamps format]: NOT AVAILABLE (the legacy directory has no manifest.json: it is not a run in the recorded format (FI-2))
report: work/sidebyside-err/report.md
make: *** [Makefile:290: sidebyside-java] Error 1
exit code 2
```

#### Side by side - nothing: **NOT AVAILABLE**

Legacy run: `work/nothing`. Target run: `work/shadow/i02-tie-half-cent/actual`. Compared field by field with `tools/parity/compare.py`.

Timestamp rule (SS3), `timestamps=format`: TRAN-ORIG-TS and TRAN-PROC-TS are compared for format only (YYYY-MM-DD-HH.MM.SS.hh0000): two values of that format that differ are listed with class INT-Q03 and accepted.

NOT AVAILABLE: the legacy directory has no manifest.json: it is not a run in the recorded format (FI-2). Nothing was compared; this is not a result of equal.


# The seven reports of the first run (`work/sidebyside-java/report.md`, timestamps exact)

## Side by side - i01-baseline: **EQUAL**

Legacy run: `fixtures/interest/i01-baseline/expected`. Target run: `work/parity-java/i01-baseline/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=50 records=4 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the legacy run's clock, and a value that differs is a difference of class INT-Q03.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| (none) | 0 |

## Side by side - i02-tie-half-cent: **EQUAL**

Legacy run: `fixtures/interest/i02-tie-half-cent/expected`. Target run: `work/parity-java/i02-tie-half-cent/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=50 records=4 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the legacy run's clock, and a value that differs is a difference of class INT-Q03.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| (none) | 0 |

## Side by side - i03-zero-rate-skipped: **EQUAL**

Legacy run: `fixtures/interest/i03-zero-rate-skipped/expected`. Target run: `work/parity-java/i03-zero-rate-skipped/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=25 records=2 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the legacy run's clock, and a value that differs is a difference of class INT-Q03.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| (none) | 0 |

## Side by side - i04-default-group-fallback: **EQUAL**

Legacy run: `fixtures/interest/i04-default-group-fallback/expected`. Target run: `work/parity-java/i04-default-group-fallback/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=25 records=2 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the legacy run's clock, and a value that differs is a difference of class INT-Q03.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| (none) | 0 |

## Side by side - i05-multi-category: **EQUAL**

Legacy run: `fixtures/interest/i05-multi-category/expected`. Target run: `work/parity-java/i05-multi-category/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=38 records=3 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the legacy run's clock, and a value that differs is a difference of class INT-Q03.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| (none) | 0 |

## Side by side - i06-negative-balance: **EQUAL**

Legacy run: `fixtures/interest/i06-negative-balance/expected`. Target run: `work/parity-java/i06-negative-balance/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=25 records=2 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the legacy run's clock, and a value that differs is a difference of class INT-Q03.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| (none) | 0 |

## Side by side - i07-nontie-rounding: **EQUAL**

Legacy run: `fixtures/interest/i07-nontie-rounding/expected`. Target run: `work/parity-java/i07-nontie-rounding/actual`. Compared field by field with `tools/parity/compare.py` (datasets=2 fields=50 records=4 sysout=1 db=0).

Timestamp rule (SS3), `timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the legacy run's clock, and a value that differs is a difference of class INT-Q03.

Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.

| Class | Difference lines |
|---|---|
| (none) | 0 |

