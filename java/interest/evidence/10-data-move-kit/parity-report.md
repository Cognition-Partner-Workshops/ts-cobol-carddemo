# Parity report - ticket 10-data-move-kit (java/interest)

Revision of the code: `8a58bb9c54b8c66838c3a560dd8dfd4f3aadd8dc` (branch `demo/05-10-data-move-kit`).
Produced 2026-11-15T13:02:36Z, comparator `tools/parity/compare.py` unchanged.

## Result per case

`python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java`

| Case | In the ticket's scope | Java job exit code | Comparator exit code | Result | Why |
|---|---|---|---|---|---|
| i01-baseline | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)` |  |
| i02-tie-half-cent | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)` |  |
| i03-zero-rate-skipped | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)` |  |
| i04-default-group-fallback | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)` |  |
| i05-multi-category | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)` |  |
| i06-negative-balance | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)` |  |
| i07-nontie-rounding | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)` |  |

All seven cases are named by the ticket ("Fixture cases: all seven"); all seven passed on `demo/05b-integration`
before the change and still pass. The parity run does not execute the code of this ticket (package `recon` is not
part of `interestCalcJob`); the cases prove that nothing else changed. The ticket's own runs on the seven cases are
`recon-report.md` (`make recon-java`: the figures of the reconciliation table of strategy-interest.md) and
`sidebyside-report.md` (`make sidebyside-java`) in this directory.

## Acceptance run: `make parity-java CASE=i01-baseline`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
interest/i01-baseline [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
report: work/parity-java/report.md
```

Files under `work/parity-java/i01-baseline/actual` and record counts:

```
work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i01-baseline/actual/rc.json
work/parity-java/i01-baseline/actual/sysout/INTCALC.STEP15.txt
  2 work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
  2 work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
  4 total
```

Output of the Java job for i01-baseline:

`datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt`

```
00000002001Y00000010100{00000050000{00000025000{2019-03-152026-12-312026-12-3100000000000{00000000000{10115     GOLD
00000002002Y00000024000{00000060000{00000030000{2019-03-152026-12-312026-12-3100000024000{00000000000{10115     STANDARD

```

`datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt`

```
2022071800000001010005System    Int. for a/c 00000002001                                                                            0000000100{000000000                                                                                                              40000000000020012022-07-05-10.00.00.0000002022-07-05-10.00.00.000000
2022071800000002010005System    Int. for a/c 00000002002                                                                            0000000370{000000000                                                                                                              40000000000020022022-07-05-10.00.00.0000002022-07-05-10.00.00.000000

```

`rc.json`

```
{
 "INTCALC": {
  "maxrc": 0,
  "steps": {
   "STEP05": 0,
   "STEP10": 0,
   "STEP15": 0,
   "STEP20": 0,
   "STEP25": 0,
   "STEP30": 0
  }
 }
}
```

`sysout/INTCALC.STEP15.txt`

```
START OF EXECUTION OF PROGRAM CBACT04C
000000020010100010000010000{                      
000000020020100010000024000{                      
END OF EXECUTION OF PROGRAM CBACT04C

```

## Acceptance run: `make parity-java CASE=i02-tie-half-cent`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
interest/i02-tie-half-cent [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
report: work/parity-java/report.md
```

Files under `work/parity-java/i02-tie-half-cent/actual` and record counts:

```
work/parity-java/i02-tie-half-cent/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i02-tie-half-cent/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i02-tie-half-cent/actual/rc.json
work/parity-java/i02-tie-half-cent/actual/sysout/INTCALC.STEP15.txt
  2 work/parity-java/i02-tie-half-cent/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
  2 work/parity-java/i02-tie-half-cent/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
  4 total
```

Output of the Java job for i02-tie-half-cent:

`datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt`

```
00000002003Y00000012478E00000050000{00000025000{2019-03-152026-12-312026-12-3100000000000{00000000000{10115     GOLD
00000002004Y00000012345{00000050000{00000025000{2019-03-152026-12-312026-12-3100000012345{00000000000{10115     GOLD

```

`datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt`

```
2022071800000001010005System    Int. for a/c 00000002003                                                                            0000000123E000000000                                                                                                              40000000000020032022-07-05-10.00.00.0000002022-07-05-10.00.00.000000
2022071800000002010005System    Int. for a/c 00000002004                                                                            0000000123D000000000                                                                                                              40000000000020042022-07-05-10.00.00.0000002022-07-05-10.00.00.000000

```

`rc.json`

```
{
 "INTCALC": {
  "maxrc": 0,
  "steps": {
   "STEP05": 0,
   "STEP10": 0,
   "STEP15": 0,
   "STEP20": 0,
   "STEP25": 0,
   "STEP30": 0
  }
 }
}
```

`sysout/INTCALC.STEP15.txt`

```
START OF EXECUTION OF PROGRAM CBACT04C
000000020030100010000012355{                      
000000020040100010000012345{                      
END OF EXECUTION OF PROGRAM CBACT04C

```

## Acceptance run: `make parity-java CASE=i03-zero-rate-skipped`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)
interest/i03-zero-rate-skipped [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
report: work/parity-java/report.md
```

Files under `work/parity-java/i03-zero-rate-skipped/actual` and record counts:

```
work/parity-java/i03-zero-rate-skipped/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i03-zero-rate-skipped/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i03-zero-rate-skipped/actual/rc.json
work/parity-java/i03-zero-rate-skipped/actual/sysout/INTCALC.STEP15.txt
  1 work/parity-java/i03-zero-rate-skipped/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
  1 work/parity-java/i03-zero-rate-skipped/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
  2 total
```

Output of the Java job for i03-zero-rate-skipped:

`datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt`

```
00000002005Y00000007000{00000050000{00000025000{2019-03-152026-12-312026-12-3100000010000{00000003000{10115     GOLD

```

`datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt`

```
2022071800000001010005System    Int. for a/c 00000002005                                                                            0000000100{000000000                                                                                                              40000000000020052022-07-05-10.00.00.0000002022-07-05-10.00.00.000000

```

`rc.json`

```
{
 "INTCALC": {
  "maxrc": 0,
  "steps": {
   "STEP05": 0,
   "STEP10": 0,
   "STEP15": 0,
   "STEP20": 0,
   "STEP25": 0,
   "STEP30": 0
  }
 }
}
```

`sysout/INTCALC.STEP15.txt`

```
START OF EXECUTION OF PROGRAM CBACT04C
000000020050100010000010000{                      
000000020050200010000003000}                      
END OF EXECUTION OF PROGRAM CBACT04C

```

## Acceptance run: `make parity-java CASE=i04-default-group-fallback`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)
interest/i04-default-group-fallback [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
report: work/parity-java/report.md
```

Files under `work/parity-java/i04-default-group-fallback/actual` and record counts:

```
work/parity-java/i04-default-group-fallback/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i04-default-group-fallback/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i04-default-group-fallback/actual/rc.json
work/parity-java/i04-default-group-fallback/actual/sysout/INTCALC.STEP15.txt
  1 work/parity-java/i04-default-group-fallback/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
  1 work/parity-java/i04-default-group-fallback/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
  2 total
```

Output of the Java job for i04-default-group-fallback:

`datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt`

```
00000002006Y00000010000{00000050000{00000025000{2019-03-152026-12-312026-12-3100000010000{00000000000{10115     PLATINUM

```

`datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt`

```
2022071800000001010005System    Int. for a/c 00000002006                                                                            0000000175{000000000                                                                                                              40000000000020062022-07-05-10.00.00.0000002022-07-05-10.00.00.000000

```

`rc.json`

```
{
 "INTCALC": {
  "maxrc": 0,
  "steps": {
   "STEP05": 0,
   "STEP10": 0,
   "STEP15": 0,
   "STEP20": 0,
   "STEP25": 0,
   "STEP30": 0
  }
 }
}
```

`sysout/INTCALC.STEP15.txt`

```
START OF EXECUTION OF PROGRAM CBACT04C
000000020060100010000010000{                      
DISCLOSURE GROUP RECORD MISSING
TRY WITH DEFAULT GROUP CODE
END OF EXECUTION OF PROGRAM CBACT04C

```

## Acceptance run: `make parity-java CASE=i05-multi-category`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
interest/i05-multi-category [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
report: work/parity-java/report.md
```

Files under `work/parity-java/i05-multi-category/actual` and record counts:

```
work/parity-java/i05-multi-category/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i05-multi-category/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i05-multi-category/actual/rc.json
work/parity-java/i05-multi-category/actual/sysout/INTCALC.STEP15.txt
  1 work/parity-java/i05-multi-category/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
  2 work/parity-java/i05-multi-category/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
  3 total
```

Output of the Java job for i05-multi-category:

`datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt`

```
00000002007Y00000015000{00000050000{00000025000{2019-03-152026-12-312026-12-3100000015000{00000000000{10115     GOLD

```

`datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt`

```
2022071800000001010005System    Int. for a/c 00000002007                                                                            0000000100{000000000                                                                                                              40000000000020072022-07-05-10.00.00.0000002022-07-05-10.00.00.000000
2022071800000002010005System    Int. for a/c 00000002007                                                                            0000000075{000000000                                                                                                              40000000000020072022-07-05-10.00.00.0000002022-07-05-10.00.00.000000

```

`rc.json`

```
{
 "INTCALC": {
  "maxrc": 0,
  "steps": {
   "STEP05": 0,
   "STEP10": 0,
   "STEP15": 0,
   "STEP20": 0,
   "STEP25": 0,
   "STEP30": 0
  }
 }
}
```

`sysout/INTCALC.STEP15.txt`

```
START OF EXECUTION OF PROGRAM CBACT04C
000000020070100010000010000{                      
000000020070100020000005000{                      
END OF EXECUTION OF PROGRAM CBACT04C

```

## Acceptance run: `make parity-java CASE=i06-negative-balance`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
interest/i06-negative-balance [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
report: work/parity-java/report.md
```

Files under `work/parity-java/i06-negative-balance/actual` and record counts:

```
work/parity-java/i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i06-negative-balance/actual/rc.json
work/parity-java/i06-negative-balance/actual/sysout/INTCALC.STEP15.txt
  1 work/parity-java/i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
  1 work/parity-java/i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
  2 total
```

Output of the Java job for i06-negative-balance:

`datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt`

```
00000002008Y00000003000}00000050000{00000025000{2019-03-152026-12-312026-12-3100000000000{00000003000{10115     GOLD

```

`datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt`

```
2022071800000001010005System    Int. for a/c 00000002008                                                                            0000000030}000000000                                                                                                              40000000000020082022-07-05-10.00.00.0000002022-07-05-10.00.00.000000

```

`rc.json`

```
{
 "INTCALC": {
  "maxrc": 0,
  "steps": {
   "STEP05": 0,
   "STEP10": 0,
   "STEP15": 0,
   "STEP20": 0,
   "STEP25": 0,
   "STEP30": 0
  }
 }
}
```

`sysout/INTCALC.STEP15.txt`

```
START OF EXECUTION OF PROGRAM CBACT04C
000000020080100010000003000}                      
END OF EXECUTION OF PROGRAM CBACT04C

```

## Acceptance run: `make parity-java CASE=i07-nontie-rounding`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
interest/i07-nontie-rounding [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
report: work/parity-java/report.md
```

Files under `work/parity-java/i07-nontie-rounding/actual` and record counts:

```
work/parity-java/i07-nontie-rounding/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i07-nontie-rounding/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i07-nontie-rounding/actual/rc.json
work/parity-java/i07-nontie-rounding/actual/sysout/INTCALC.STEP15.txt
  2 work/parity-java/i07-nontie-rounding/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
  2 work/parity-java/i07-nontie-rounding/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
  4 total
```

Output of the Java job for i07-nontie-rounding:

`datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt`

```
00000002009Y00000010157E00000050000{00000025000{2019-03-152026-12-312026-12-3100000000000{00000000000{10115     STANDARD
00000002010Y00000007777G00000050000{00000025000{2019-03-152026-12-312026-12-3100000007777G00000000000{10115     DEFAULT

```

`datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt`

```
2022071800000001010005System    Int. for a/c 00000002009                                                                            0000000154B000000000                                                                                                              40000000000020092022-07-05-10.00.00.0000002022-07-05-10.00.00.000000
2022071800000002010005System    Int. for a/c 00000002010                                                                            0000000136A000000000                                                                                                              40000000000020102022-07-05-10.00.00.0000002022-07-05-10.00.00.000000

```

`rc.json`

```
{
 "INTCALC": {
  "maxrc": 0,
  "steps": {
   "STEP05": 0,
   "STEP10": 0,
   "STEP15": 0,
   "STEP20": 0,
   "STEP25": 0,
   "STEP30": 0
  }
 }
}
```

`sysout/INTCALC.STEP15.txt`

```
START OF EXECUTION OF PROGRAM CBACT04C
000000020090100010000010003C                      
000000020100100010000007777G                      
END OF EXECUTION OF PROGRAM CBACT04C

```

## All cases: `make parity-java` (exit code 0)

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
interest/i01-baseline [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
interest/i02-tie-half-cent [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)
interest/i03-zero-rate-skipped [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)
interest/i04-default-group-fallback [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
interest/i05-multi-category [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
interest/i06-negative-balance [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
interest/i07-nontie-rounding [java] interest_run: PASS (20 of 20 assertions of RC1, RC2, RC5 hold)
report: work/parity-java/report.md
```

## Comparator report (`work/parity-java/compare.md`, written by tools/parity/compare.py)

### interest / i01-baseline — java: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i02-tie-half-cent — java: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i03-zero-rate-skipped — java: **PASS**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i04-default-group-fallback — java: **PASS**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i05-multi-category — java: **PASS**

Checked 2 datasets, 3 records, 38 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i06-negative-balance — java: **PASS**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i07-nontie-rounding — java: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


## Job repository and schema after these runs (`db` service)

```
         server_version          
---------------------------------
 15.20 (Debian 15.20-1.pgdg13+1)
(1 row)

    job_name     |  status   |                                                               steps                                                                |                   run_id                    
-----------------+-----------+------------------------------------------------------------------------------------------------------------------------------------+---------------------------------------------
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794747754-30902-i07-nontie-rounding
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794747752-30902-i06-negative-balance
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=2 | 1794747750-30902-i05-multi-category
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794747748-30902-i04-default-group-fallback
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794747747-30902-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794747745-30902-i02-tie-half-cent
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794747743-30902-i01-baseline
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794747739-30419-i07-nontie-rounding
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794747735-29974-i06-negative-balance
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=2 | 1794747731-29504-i05-multi-category
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794747726-29015-i04-default-group-fallback
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794747722-28551-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794747718-28072-i02-tie-half-cent
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794747714-27584-i01-baseline
(14 rows)

 table_schema |        table_name        
--------------+--------------------------
 carddemo     | account
 carddemo     | card_xref
 carddemo     | disclosure_group
 carddemo     | interest_run
 carddemo     | interest_transaction
 carddemo     | tran_cat_balance
 carddemo     | transaction_type
 carddemo     | transaction_type_categor
(8 rows)

```
