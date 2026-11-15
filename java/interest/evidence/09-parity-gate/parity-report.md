# Parity report - ticket 09-parity-gate (java/interest)

Revision of the code: `93d0eeec2b7487aacd46fc0d9e10ce417a71fc65` (branch `demo/05-09-parity-gate`).
Produced 2026-11-15T12:30:42Z, comparator `tools/parity/compare.py` unchanged.

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

All seven cases are acceptance cases of ticket 09. Every run in this report is `interestCalcJob` with the profile
`tables`: loadExtractsStep, calcInterestStep on the tables and exportDatasetsStep through the `db` service
(PostgreSQL 15); the job repository section at the end shows the three steps of every launch (the order inside the
`steps` column is not the execution order: the query of the script does not sort it).

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
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794745840-47044-i07-nontie-rounding
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794745838-47044-i06-negative-balance
 interestCalcJob | COMPLETED | exportDatasetsStep:COMPLETED read=0 write=2, loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1 | 1794745836-47044-i05-multi-category
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1, loadExtractsStep:COMPLETED read=0 write=18, exportDatasetsStep:COMPLETED read=0 write=1 | 1794745835-47044-i04-default-group-fallback
 interestCalcJob | COMPLETED | exportDatasetsStep:COMPLETED read=0 write=1, loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1 | 1794745833-47044-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794745831-47044-i02-tie-half-cent
 interestCalcJob | COMPLETED | exportDatasetsStep:COMPLETED read=0 write=2, loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2 | 1794745829-47044-i01-baseline
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794745824-46544-i07-nontie-rounding
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794745820-46099-i06-negative-balance
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=2 | 1794745816-45596-i05-multi-category
 interestCalcJob | COMPLETED | exportDatasetsStep:COMPLETED read=0 write=1, loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1 | 1794745811-45154-i04-default-group-fallback
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=1 | 1794745807-44682-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794745803-44202-i02-tie-half-cent
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2 | 1794745799-43709-i01-baseline
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

## The gate's own report (`work/parity-java/report.md` of the run of all cases above)

Written by `make parity-java`: the comparator's sections (the same as above), then per case the assertions of RC1, RC2 and RC5 on the `interest_run` row of that launch, then the summary. Each line is assertion, the value from the files in brackets, `t` (holds) or `f`.

### interest/i01-baseline [java] interest_run: PASS
```
one interest_run row of this launch, status EXPORTED|t
RC1 loaded_account_count = records of ACCTDATA.PS (2)|t
RC1 loaded_card_xref_count = records of CARDXREF.PS (2)|t
RC1 loaded_tran_cat_balance_count = records of TCATBALF.PS (2)|t
RC1 loaded_disclosure_group_count = records of DISCGRP.PS (15)|t
RC1 rows of account, card_xref, tran_cat_balance, disclosure_group = the four loaded counts|t
RC2 loaded_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the file (3400.00)|t
RC2 loaded_acct_credit_limit_sum = sum of ACCT-CREDIT-LIMIT in the file (11000.00)|t
RC2 loaded_acct_cash_credit_limit_sum = sum of ACCT-CASH-CREDIT-LIMIT in the file (5500.00)|t
RC2 loaded_acct_curr_cyc_credit_sum = sum of ACCT-CURR-CYC-CREDIT in the file (3400.00)|t
RC2 loaded_acct_curr_cyc_debit_sum = sum of ACCT-CURR-CYC-DEBIT in the file (0.00)|t
RC2 loaded_tran_cat_bal_sum = sum of TRAN-CAT-BAL in the file (3400.00)|t
RC2 loaded_dis_int_rate_sum = sum of DIS-INT-RATE in the file (120.50)|t
RC2 loaded_acct_id_sum = hash total of ACCT-ID (4003)|t
RC2 loaded_xref_acct_id_sum = hash total of XREF-ACCT-ID (4003)|t
RC2 loaded_trancat_acct_id_sum = hash total of TRANCAT-ACCT-ID (4003)|t
RC5 written_transaction_count = rows of interest_transaction = records of the exported SYSTRAN (2)|t
RC5 written_tran_amt_sum = sum of TRAN-AMT in the exported SYSTRAN (47.00)|t
RC5 written_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the exported ACCTDATA (3410.00)|t
RC5 sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL (10.00) + interest of the last account, not added: INT-Q04 (37.00)|t
```
### interest/i02-tie-half-cent [java] interest_run: PASS
```
one interest_run row of this launch, status EXPORTED|t
RC1 loaded_account_count = records of ACCTDATA.PS (2)|t
RC1 loaded_card_xref_count = records of CARDXREF.PS (2)|t
RC1 loaded_tran_cat_balance_count = records of TCATBALF.PS (2)|t
RC1 loaded_disclosure_group_count = records of DISCGRP.PS (15)|t
RC1 rows of account, card_xref, tran_cat_balance, disclosure_group = the four loaded counts|t
RC2 loaded_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the file (2470.00)|t
RC2 loaded_acct_credit_limit_sum = sum of ACCT-CREDIT-LIMIT in the file (10000.00)|t
RC2 loaded_acct_cash_credit_limit_sum = sum of ACCT-CASH-CREDIT-LIMIT in the file (5000.00)|t
RC2 loaded_acct_curr_cyc_credit_sum = sum of ACCT-CURR-CYC-CREDIT in the file (2470.00)|t
RC2 loaded_acct_curr_cyc_debit_sum = sum of ACCT-CURR-CYC-DEBIT in the file (0.00)|t
RC2 loaded_tran_cat_bal_sum = sum of TRAN-CAT-BAL in the file (2470.00)|t
RC2 loaded_dis_int_rate_sum = sum of DIS-INT-RATE in the file (120.50)|t
RC2 loaded_acct_id_sum = hash total of ACCT-ID (4007)|t
RC2 loaded_xref_acct_id_sum = hash total of XREF-ACCT-ID (4007)|t
RC2 loaded_trancat_acct_id_sum = hash total of TRANCAT-ACCT-ID (4007)|t
RC5 written_transaction_count = rows of interest_transaction = records of the exported SYSTRAN (2)|t
RC5 written_tran_amt_sum = sum of TRAN-AMT in the exported SYSTRAN (24.69)|t
RC5 written_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the exported ACCTDATA (2482.35)|t
RC5 sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL (12.35) + interest of the last account, not added: INT-Q04 (12.34)|t
```
### interest/i03-zero-rate-skipped [java] interest_run: PASS
```
one interest_run row of this launch, status EXPORTED|t
RC1 loaded_account_count = records of ACCTDATA.PS (1)|t
RC1 loaded_card_xref_count = records of CARDXREF.PS (1)|t
RC1 loaded_tran_cat_balance_count = records of TCATBALF.PS (2)|t
RC1 loaded_disclosure_group_count = records of DISCGRP.PS (15)|t
RC1 rows of account, card_xref, tran_cat_balance, disclosure_group = the four loaded counts|t
RC2 loaded_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the file (700.00)|t
RC2 loaded_acct_credit_limit_sum = sum of ACCT-CREDIT-LIMIT in the file (5000.00)|t
RC2 loaded_acct_cash_credit_limit_sum = sum of ACCT-CASH-CREDIT-LIMIT in the file (2500.00)|t
RC2 loaded_acct_curr_cyc_credit_sum = sum of ACCT-CURR-CYC-CREDIT in the file (1000.00)|t
RC2 loaded_acct_curr_cyc_debit_sum = sum of ACCT-CURR-CYC-DEBIT in the file (300.00)|t
RC2 loaded_tran_cat_bal_sum = sum of TRAN-CAT-BAL in the file (700.00)|t
RC2 loaded_dis_int_rate_sum = sum of DIS-INT-RATE in the file (120.50)|t
RC2 loaded_acct_id_sum = hash total of ACCT-ID (2005)|t
RC2 loaded_xref_acct_id_sum = hash total of XREF-ACCT-ID (2005)|t
RC2 loaded_trancat_acct_id_sum = hash total of TRANCAT-ACCT-ID (4010)|t
RC5 written_transaction_count = rows of interest_transaction = records of the exported SYSTRAN (1)|t
RC5 written_tran_amt_sum = sum of TRAN-AMT in the exported SYSTRAN (10.00)|t
RC5 written_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the exported ACCTDATA (700.00)|t
RC5 sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL (0.00) + interest of the last account, not added: INT-Q04 (10.00)|t
```
### interest/i04-default-group-fallback [java] interest_run: PASS
```
one interest_run row of this launch, status EXPORTED|t
RC1 loaded_account_count = records of ACCTDATA.PS (1)|t
RC1 loaded_card_xref_count = records of CARDXREF.PS (1)|t
RC1 loaded_tran_cat_balance_count = records of TCATBALF.PS (1)|t
RC1 loaded_disclosure_group_count = records of DISCGRP.PS (15)|t
RC1 rows of account, card_xref, tran_cat_balance, disclosure_group = the four loaded counts|t
RC2 loaded_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the file (1000.00)|t
RC2 loaded_acct_credit_limit_sum = sum of ACCT-CREDIT-LIMIT in the file (5000.00)|t
RC2 loaded_acct_cash_credit_limit_sum = sum of ACCT-CASH-CREDIT-LIMIT in the file (2500.00)|t
RC2 loaded_acct_curr_cyc_credit_sum = sum of ACCT-CURR-CYC-CREDIT in the file (1000.00)|t
RC2 loaded_acct_curr_cyc_debit_sum = sum of ACCT-CURR-CYC-DEBIT in the file (0.00)|t
RC2 loaded_tran_cat_bal_sum = sum of TRAN-CAT-BAL in the file (1000.00)|t
RC2 loaded_dis_int_rate_sum = sum of DIS-INT-RATE in the file (120.50)|t
RC2 loaded_acct_id_sum = hash total of ACCT-ID (2006)|t
RC2 loaded_xref_acct_id_sum = hash total of XREF-ACCT-ID (2006)|t
RC2 loaded_trancat_acct_id_sum = hash total of TRANCAT-ACCT-ID (2006)|t
RC5 written_transaction_count = rows of interest_transaction = records of the exported SYSTRAN (1)|t
RC5 written_tran_amt_sum = sum of TRAN-AMT in the exported SYSTRAN (17.50)|t
RC5 written_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the exported ACCTDATA (1000.00)|t
RC5 sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL (0.00) + interest of the last account, not added: INT-Q04 (17.50)|t
```
### interest/i05-multi-category [java] interest_run: PASS
```
one interest_run row of this launch, status EXPORTED|t
RC1 loaded_account_count = records of ACCTDATA.PS (1)|t
RC1 loaded_card_xref_count = records of CARDXREF.PS (1)|t
RC1 loaded_tran_cat_balance_count = records of TCATBALF.PS (2)|t
RC1 loaded_disclosure_group_count = records of DISCGRP.PS (15)|t
RC1 rows of account, card_xref, tran_cat_balance, disclosure_group = the four loaded counts|t
RC2 loaded_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the file (1500.00)|t
RC2 loaded_acct_credit_limit_sum = sum of ACCT-CREDIT-LIMIT in the file (5000.00)|t
RC2 loaded_acct_cash_credit_limit_sum = sum of ACCT-CASH-CREDIT-LIMIT in the file (2500.00)|t
RC2 loaded_acct_curr_cyc_credit_sum = sum of ACCT-CURR-CYC-CREDIT in the file (1500.00)|t
RC2 loaded_acct_curr_cyc_debit_sum = sum of ACCT-CURR-CYC-DEBIT in the file (0.00)|t
RC2 loaded_tran_cat_bal_sum = sum of TRAN-CAT-BAL in the file (1500.00)|t
RC2 loaded_dis_int_rate_sum = sum of DIS-INT-RATE in the file (120.50)|t
RC2 loaded_acct_id_sum = hash total of ACCT-ID (2007)|t
RC2 loaded_xref_acct_id_sum = hash total of XREF-ACCT-ID (2007)|t
RC2 loaded_trancat_acct_id_sum = hash total of TRANCAT-ACCT-ID (4014)|t
RC5 written_transaction_count = rows of interest_transaction = records of the exported SYSTRAN (2)|t
RC5 written_tran_amt_sum = sum of TRAN-AMT in the exported SYSTRAN (17.50)|t
RC5 written_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the exported ACCTDATA (1500.00)|t
RC5 sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL (0.00) + interest of the last account, not added: INT-Q04 (17.50)|t
```
### interest/i06-negative-balance [java] interest_run: PASS
```
one interest_run row of this launch, status EXPORTED|t
RC1 loaded_account_count = records of ACCTDATA.PS (1)|t
RC1 loaded_card_xref_count = records of CARDXREF.PS (1)|t
RC1 loaded_tran_cat_balance_count = records of TCATBALF.PS (1)|t
RC1 loaded_disclosure_group_count = records of DISCGRP.PS (15)|t
RC1 rows of account, card_xref, tran_cat_balance, disclosure_group = the four loaded counts|t
RC2 loaded_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the file (-300.00)|t
RC2 loaded_acct_credit_limit_sum = sum of ACCT-CREDIT-LIMIT in the file (5000.00)|t
RC2 loaded_acct_cash_credit_limit_sum = sum of ACCT-CASH-CREDIT-LIMIT in the file (2500.00)|t
RC2 loaded_acct_curr_cyc_credit_sum = sum of ACCT-CURR-CYC-CREDIT in the file (0.00)|t
RC2 loaded_acct_curr_cyc_debit_sum = sum of ACCT-CURR-CYC-DEBIT in the file (300.00)|t
RC2 loaded_tran_cat_bal_sum = sum of TRAN-CAT-BAL in the file (-300.00)|t
RC2 loaded_dis_int_rate_sum = sum of DIS-INT-RATE in the file (120.50)|t
RC2 loaded_acct_id_sum = hash total of ACCT-ID (2008)|t
RC2 loaded_xref_acct_id_sum = hash total of XREF-ACCT-ID (2008)|t
RC2 loaded_trancat_acct_id_sum = hash total of TRANCAT-ACCT-ID (2008)|t
RC5 written_transaction_count = rows of interest_transaction = records of the exported SYSTRAN (1)|t
RC5 written_tran_amt_sum = sum of TRAN-AMT in the exported SYSTRAN (-3.00)|t
RC5 written_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the exported ACCTDATA (-300.00)|t
RC5 sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL (0.00) + interest of the last account, not added: INT-Q04 (-3.00)|t
```
### interest/i07-nontie-rounding [java] interest_run: PASS
```
one interest_run row of this launch, status EXPORTED|t
RC1 loaded_account_count = records of ACCTDATA.PS (2)|t
RC1 loaded_card_xref_count = records of CARDXREF.PS (2)|t
RC1 loaded_tran_cat_balance_count = records of TCATBALF.PS (2)|t
RC1 loaded_disclosure_group_count = records of DISCGRP.PS (15)|t
RC1 rows of account, card_xref, tran_cat_balance, disclosure_group = the four loaded counts|t
RC2 loaded_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the file (1778.10)|t
RC2 loaded_acct_credit_limit_sum = sum of ACCT-CREDIT-LIMIT in the file (10000.00)|t
RC2 loaded_acct_cash_credit_limit_sum = sum of ACCT-CASH-CREDIT-LIMIT in the file (5000.00)|t
RC2 loaded_acct_curr_cyc_credit_sum = sum of ACCT-CURR-CYC-CREDIT in the file (1778.10)|t
RC2 loaded_acct_curr_cyc_debit_sum = sum of ACCT-CURR-CYC-DEBIT in the file (0.00)|t
RC2 loaded_tran_cat_bal_sum = sum of TRAN-CAT-BAL in the file (1778.10)|t
RC2 loaded_dis_int_rate_sum = sum of DIS-INT-RATE in the file (120.50)|t
RC2 loaded_acct_id_sum = hash total of ACCT-ID (4019)|t
RC2 loaded_xref_acct_id_sum = hash total of XREF-ACCT-ID (4019)|t
RC2 loaded_trancat_acct_id_sum = hash total of TRANCAT-ACCT-ID (4019)|t
RC5 written_transaction_count = rows of interest_transaction = records of the exported SYSTRAN (2)|t
RC5 written_tran_amt_sum = sum of TRAN-AMT in the exported SYSTRAN (29.03)|t
RC5 written_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the exported ACCTDATA (1793.52)|t
RC5 sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL (15.42) + interest of the last account, not added: INT-Q04 (13.61)|t
```
### Gate: PASS
profile `tables`, options ``, PARM 2022071800, clock 2022-07-05 10:00:00.00. A case whose job did not end with exit code 0 is a failed case.
| Case | Java job exit code | Comparator | interest_run (RC1, RC2, RC5) |
|---|---|---|---|
| i01-baseline | 0 | PASS | PASS |
| i02-tie-half-cent | 0 | PASS | PASS |
| i03-zero-rate-skipped | 0 | PASS | PASS |
| i04-default-group-fallback | 0 | PASS | PASS |
| i05-multi-category | 0 | PASS | PASS |
| i06-negative-balance | 0 | PASS | PASS |
| i07-nontie-rounding | 0 | PASS | PASS |

## Negative control and failed runs

`switch-test-output.txt` in this directory: with `interest.policy.rounding=HALF_EVEN` the gate exits non-zero and fails on i02-tie-half-cent only (TRAN-AMT 12.35 against 12.36, ACCT-CURR-BAL 1247.85 against 1247.86 of account 2003; the job's exit code is 0 and RC1, RC2, RC5 hold); a job that stops (PARM of 8 characters, exit code 12) is reported as a failed case; the file adapters still pass (`INTEREST_PROFILE=default`).

## The three steps in the Maven suite: `InterestJobTablesIT` on Testcontainers `postgres:15`

After `mvn clean verify` (test-output.txt), `python3 tools/parity/compare.py interest <case> --actual java/interest/target/parity-job/<case>/actual --label java-job; echo "exit $?"`:

```
interest/i01-baseline [java-job]: PASS (4 records, 50 fields, 0 differences)
exit 0
interest/i02-tie-half-cent [java-job]: PASS (4 records, 50 fields, 0 differences)
exit 0
interest/i03-zero-rate-skipped [java-job]: PASS (2 records, 25 fields, 0 differences)
exit 0
interest/i04-default-group-fallback [java-job]: PASS (2 records, 25 fields, 0 differences)
exit 0
interest/i05-multi-category [java-job]: PASS (3 records, 38 fields, 0 differences)
exit 0
interest/i06-negative-balance [java-job]: PASS (2 records, 25 fields, 0 differences)
exit 0
interest/i07-nontie-rounding [java-job]: PASS (4 records, 50 fields, 0 differences)
exit 0
```

## Legacy `make parity` (`make up && make build && make parity`, exit code 0)

```
posting/p01-baseline-mixed [legacy rerun]: PASS (23 records, 92 fields, 0 differences)
posting/p02-invalid-card [legacy rerun]: PASS (12 records, 44 fields, 0 differences)
posting/p03-account-missing [legacy rerun]: PASS (12 records, 44 fields, 0 differences)
posting/p04-overlimit-reject [legacy rerun]: PASS (12 records, 44 fields, 0 differences)
posting/p05-expired-account [legacy rerun]: PASS (14 records, 60 fields, 0 differences)
posting/p06-credits-new-category [legacy rerun]: PASS (18 records, 67 fields, 0 differences)
posting/p07-empty-day [legacy rerun]: PASS (2 records, 16 fields, 0 differences)
interest/i01-baseline [legacy rerun]: PASS (4 records, 50 fields, 0 differences)
interest/i02-tie-half-cent [legacy rerun]: PASS (4 records, 50 fields, 0 differences)
interest/i03-zero-rate-skipped [legacy rerun]: PASS (2 records, 25 fields, 0 differences)
interest/i04-default-group-fallback [legacy rerun]: PASS (2 records, 25 fields, 0 differences)
interest/i05-multi-category [legacy rerun]: PASS (3 records, 38 fields, 0 differences)
interest/i06-negative-balance [legacy rerun]: PASS (2 records, 25 fields, 0 differences)
interest/i07-nontie-rounding [legacy rerun]: PASS (4 records, 50 fields, 0 differences)
trantype-update/t01-add [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-update/t02-update [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-update/t03-delete [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-update/t04-comments [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-update/t05-delete-missing [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-update/t06-duplicate-add [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-update/t07-mixed-batch [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-dynamic/d01-update-then-apply [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-dynamic/d02-delete-statement [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
trantype-dynamic/d03-bad-statement [legacy rerun]: PASS (0 records, 0 fields, 0 differences)
report: work/parity/report.md
```
