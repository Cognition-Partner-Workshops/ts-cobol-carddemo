# Parity report - ticket 09-parity-gate (java/interest)

Revision of the code: `422fc1c25adde16713cc6e9e40de9471c79110bd` (branch `demo/05-g4-gate`).
Produced 2026-11-15T20:07:25Z, comparator `tools/parity/compare.py` unchanged.

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

## Acceptance run: `make parity-java CASE=i01-baseline`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
recon i01-baseline: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i01-baseline/recon.md
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
parity-java: 1 of 1 cases PASS
report: work/parity-java/report.md
```

Files under `work/parity-java/i01-baseline/actual` and record counts:

```
work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i01-baseline/actual/rc.json
work/parity-java/i01-baseline/actual/sysout/INTCALC.STEP15.txt
work/parity-java/i01-baseline/recon.md
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
recon i02-tie-half-cent: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i02-tie-half-cent/recon.md
interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
parity-java: 1 of 1 cases PASS
report: work/parity-java/report.md
```

Files under `work/parity-java/i02-tie-half-cent/actual` and record counts:

```
work/parity-java/i02-tie-half-cent/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i02-tie-half-cent/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i02-tie-half-cent/actual/rc.json
work/parity-java/i02-tie-half-cent/actual/sysout/INTCALC.STEP15.txt
work/parity-java/i02-tie-half-cent/recon.md
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
recon i03-zero-rate-skipped: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i03-zero-rate-skipped/recon.md
interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)
parity-java: 1 of 1 cases PASS
report: work/parity-java/report.md
```

Files under `work/parity-java/i03-zero-rate-skipped/actual` and record counts:

```
work/parity-java/i03-zero-rate-skipped/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i03-zero-rate-skipped/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i03-zero-rate-skipped/actual/rc.json
work/parity-java/i03-zero-rate-skipped/actual/sysout/INTCALC.STEP15.txt
work/parity-java/i03-zero-rate-skipped/recon.md
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
recon i04-default-group-fallback: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i04-default-group-fallback/recon.md
interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)
parity-java: 1 of 1 cases PASS
report: work/parity-java/report.md
```

Files under `work/parity-java/i04-default-group-fallback/actual` and record counts:

```
work/parity-java/i04-default-group-fallback/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i04-default-group-fallback/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i04-default-group-fallback/actual/rc.json
work/parity-java/i04-default-group-fallback/actual/sysout/INTCALC.STEP15.txt
work/parity-java/i04-default-group-fallback/recon.md
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
recon i05-multi-category: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i05-multi-category/recon.md
interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
parity-java: 1 of 1 cases PASS
report: work/parity-java/report.md
```

Files under `work/parity-java/i05-multi-category/actual` and record counts:

```
work/parity-java/i05-multi-category/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i05-multi-category/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i05-multi-category/actual/rc.json
work/parity-java/i05-multi-category/actual/sysout/INTCALC.STEP15.txt
work/parity-java/i05-multi-category/recon.md
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
recon i06-negative-balance: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i06-negative-balance/recon.md
interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
parity-java: 1 of 1 cases PASS
report: work/parity-java/report.md
```

Files under `work/parity-java/i06-negative-balance/actual` and record counts:

```
work/parity-java/i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i06-negative-balance/actual/rc.json
work/parity-java/i06-negative-balance/actual/sysout/INTCALC.STEP15.txt
work/parity-java/i06-negative-balance/recon.md
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
recon i07-nontie-rounding: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i07-nontie-rounding/recon.md
interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
parity-java: 1 of 1 cases PASS
report: work/parity-java/report.md
```

Files under `work/parity-java/i07-nontie-rounding/actual` and record counts:

```
work/parity-java/i07-nontie-rounding/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
work/parity-java/i07-nontie-rounding/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
work/parity-java/i07-nontie-rounding/actual/rc.json
work/parity-java/i07-nontie-rounding/actual/sysout/INTCALC.STEP15.txt
work/parity-java/i07-nontie-rounding/recon.md
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
recon i01-baseline: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i01-baseline/recon.md
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
recon i02-tie-half-cent: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i02-tie-half-cent/recon.md
interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
recon i03-zero-rate-skipped: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i03-zero-rate-skipped/recon.md
interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)
recon i04-default-group-fallback: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i04-default-group-fallback/recon.md
interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)
recon i05-multi-category: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i05-multi-category/recon.md
interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
recon i06-negative-balance: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i06-negative-balance/recon.md
interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
recon i07-nontie-rounding: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java/i07-nontie-rounding/recon.md
interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
parity-java: 7 of 7 cases PASS
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
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, exportDatasetsStep:COMPLETED read=0 write=4, calcInterestStep:COMPLETED read=2 write=2 | 1794773242-37948-i07-nontie-rounding
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1, loadExtractsStep:COMPLETED read=0 write=18, exportDatasetsStep:COMPLETED read=0 write=2 | 1794773239-37948-i06-negative-balance
 interestCalcJob | COMPLETED | exportDatasetsStep:COMPLETED read=0 write=3, loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1 | 1794773235-37948-i05-multi-category
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=2, loadExtractsStep:COMPLETED read=0 write=18 | 1794773232-37948-i04-default-group-fallback
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=2 | 1794773229-37948-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | exportDatasetsStep:COMPLETED read=0 write=4, loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2 | 1794773226-37948-i02-tie-half-cent
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=4 | 1794773223-37948-i01-baseline
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=4, loadExtractsStep:COMPLETED read=0 write=21 | 1794773218-37445-i07-nontie-rounding
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=2 | 1794773213-36994-i06-negative-balance
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=3 | 1794773208-36488-i05-multi-category
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=18, calcInterestStep:COMPLETED read=1 write=1, exportDatasetsStep:COMPLETED read=0 write=2 | 1794773202-36016-i04-default-group-fallback
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=19, exportDatasetsStep:COMPLETED read=0 write=2, calcInterestStep:COMPLETED read=1 write=1 | 1794773196-35514-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | exportDatasetsStep:COMPLETED read=0 write=4, loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2 | 1794773191-35026-i02-tie-half-cent
 interestCalcJob | COMPLETED | loadExtractsStep:COMPLETED read=0 write=21, exportDatasetsStep:COMPLETED read=0 write=4, calcInterestStep:COMPLETED read=2 write=2 | 1794773186-34521-i01-baseline
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

The sections below were added by hand to the output of `parity-report.sh`, from the same revision.

## Database assertions RC1, RC2, RC5 (ticket 09, Acceptance)

`make parity-java` writes `work/parity-java/<case>/recon.md` (reconciliation report of the data move kit on the run's row of
`interest_run`) and fails the case unless every line of RC1, RC2 and RC5 is EQUAL. Lines per case, from the run of all cases above:

| Case | RC1 lines EQUAL / all | RC2 lines EQUAL / all | RC5 lines EQUAL / all | Summary line of the report |
|---|---|---|---|---|
| i01-baseline | 8 / 8 | 10 / 10 | 6 / 6 | Result: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE. |
| i02-tie-half-cent | 8 / 8 | 10 / 10 | 6 / 6 | Result: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE. |
| i03-zero-rate-skipped | 8 / 8 | 10 / 10 | 6 / 6 | Result: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE. |
| i04-default-group-fallback | 8 / 8 | 10 / 10 | 6 / 6 | Result: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE. |
| i05-multi-category | 8 / 8 | 10 / 10 | 6 / 6 | Result: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE. |
| i06-negative-balance | 8 / 8 | 10 / 10 | 6 / 6 | Result: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE. |
| i07-nontie-rounding | 8 / 8 | 10 / 10 | 6 / 6 | Result: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE. |

The one NOT AVAILABLE line of every case is RC6 (needs a mainframe unload after the write-back).

RC5 in words, per case (from `recon.md`):

* i01-baseline: 2 transaction(s) summing 47.00; sum of ACCT-CURR-BAL 3400.00 before and 3410.00 after, a balance change of 10.00; interest of the last account 37.00, not added to the last account (INT-Q04).
* i02-tie-half-cent: 2 transaction(s) summing 24.69; sum of ACCT-CURR-BAL 2470.00 before and 2482.35 after, a balance change of 12.35; interest of the last account 12.34, not added to the last account (INT-Q04).
* i03-zero-rate-skipped: 1 transaction(s) summing 10.00; sum of ACCT-CURR-BAL 700.00 before and 700.00 after, a balance change of 0.00; interest of the last account 10.00, not added to the last account (INT-Q04).
* i04-default-group-fallback: 1 transaction(s) summing 17.50; sum of ACCT-CURR-BAL 1000.00 before and 1000.00 after, a balance change of 0.00; interest of the last account 17.50, not added to the last account (INT-Q04).
* i05-multi-category: 2 transaction(s) summing 17.50; sum of ACCT-CURR-BAL 1500.00 before and 1500.00 after, a balance change of 0.00; interest of the last account 17.50, not added to the last account (INT-Q04).
* i06-negative-balance: 1 transaction(s) summing -3.00; sum of ACCT-CURR-BAL -300.00 before and -300.00 after, a balance change of 0.00; interest of the last account -3.00, not added to the last account (INT-Q04).
* i07-nontie-rounding: 2 transaction(s) summing 29.03; sum of ACCT-CURR-BAL 1778.10 before and 1793.52 after, a balance change of 15.42; interest of the last account 13.61, not added to the last account (INT-Q04).

The rows of `carddemo.interest_run` of these seven runs (one row per job execution, status EXPORTED):

```
 run_id |  status  | acct | xref | tcat | disc | bal_before | tcat_sum | tran | tran_sum | upd | bal_after 
--------+----------+------+------+------+------+------------+----------+------+----------+-----+-----------
     30 | EXPORTED |    2 |    2 |    2 |   15 |    3400.00 |  3400.00 |    2 |    47.00 |   1 |   3410.00
     31 | EXPORTED |    2 |    2 |    2 |   15 |    2470.00 |  2470.00 |    2 |    24.69 |   1 |   2482.35
     32 | EXPORTED |    1 |    1 |    2 |   15 |     700.00 |   700.00 |    1 |    10.00 |   0 |    700.00
     33 | EXPORTED |    1 |    1 |    1 |   15 |    1000.00 |  1000.00 |    1 |    17.50 |   0 |   1000.00
     34 | EXPORTED |    1 |    1 |    2 |   15 |    1500.00 |  1500.00 |    2 |    17.50 |   0 |   1500.00
     35 | EXPORTED |    1 |    1 |    1 |   15 |    -300.00 |  -300.00 |    1 |    -3.00 |   0 |   -300.00
     36 | EXPORTED |    2 |    2 |    2 |   15 |    1778.10 |  1778.10 |    2 |    29.03 |   1 |   1793.52
(7 rows)

```

(The seven newest EXPORTED rows are those of the negative control below; the seven before them are the gate's.)

## Negative control: `make parity-java INTEREST_ARGS="--interest.policy.rounding=HALF_EVEN" PARITY_JAVA_OUT=work/parity-java-half-even` (exit code 2)

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
recon i01-baseline: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java-half-even/i01-baseline/recon.md
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
recon i02-tie-half-cent: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java-half-even/i02-tie-half-cent/recon.md
interest/i02-tie-half-cent [java]: FAIL (4 records, 50 fields, 2 differences)
recon i03-zero-rate-skipped: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java-half-even/i03-zero-rate-skipped/recon.md
interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)
recon i04-default-group-fallback: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java-half-even/i04-default-group-fallback/recon.md
interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)
recon i05-multi-category: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java-half-even/i05-multi-category/recon.md
interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
recon i06-negative-balance: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java-half-even/i06-negative-balance/recon.md
interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
recon i07-nontie-rounding: 36 EQUAL, 0 DIFFERENT, 1 NOT AVAILABLE -> work/parity-java-half-even/i07-nontie-rounding/recon.md
interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
parity-java: 6 of 7 cases PASS
report: work/parity-java-half-even/report.md
make: *** [Makefile:108: parity-java] Error 1
```

The only failing case, from `work/parity-java-half-even/report.md`:

### interest / i02-tie-half-cent — java: **FAIL (2 differences)**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.

| Where | Field | Expected (legacy) | Actual (candidate) |
|---|---|---|---|
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 1 (ACCT-ID=2003) | `ACCT-CURR-BAL` | 1247.85 | 1247.86 |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-AMT` | 12.35 | 12.36 |

Fields differing: `ACCT-CURR-BAL`, `TRAN-AMT`

`make parity-naive CASE=i02-tie-half-cent` (exit code 2), the estate's own half-even reference, fails on the same two fields:

```
interest/i02-tie-half-cent: naive half-even reference wrote 2 interest transactions to work/parity/interest/i02-tie-half-cent/naive
interest/i02-tie-half-cent [naive half-even reference]: FAIL (4 records, 50 fields, 2 differences)
report: work/parity/report-naive.md
make: *** [Makefile:76: parity-naive] Error 1
```

| Where | Field | Expected (legacy) | Actual (candidate) |
|---|---|---|---|
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 1 (ACCT-ID=2003) | `ACCT-CURR-BAL` | 1247.85 | 1247.86 |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-AMT` | 12.35 | 12.36 |

Fields differing: `ACCT-CURR-BAL`, `TRAN-AMT`

## Errors: a case whose run fails is reported as failed, not skipped

A refused launch (PARM of eight characters, INT-Q02): `make parity-java CASE=i01-baseline INTEREST_PARM=20220718 PARITY_JAVA_OUT=work/parity-java-error` (exit code 2)

```
Caused by: org.springframework.batch.core.JobParametersInvalidException: INT-Q02: parmDate must be exactly 10 characters
interest/i01-baseline: java job ended with exit code 12
interest/i01-baseline [java]: FAIL (4 records, 50 fields, 4 differences)
parity-java: 0 of 1 cases PASS
report: work/parity-java-error/report.md
make: *** [Makefile:108: parity-java] Error 1
```

A failed load (the card cross-reference file is missing), the job by hand with the profile `database` (job exit code 12); no output directory is left:

```
2026-11-15T20:08:25.078Z ERROR 1 --- [interest] [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step loadExtractsStep in job interestCalcJob
com.carddemo.interest.domain.InterestRunStoppedException: OPEN_INPUT (INT-R08): cannot read input file AWS.M2.CARDDEMO.CARDXREF.PS.txt
$ ls work/manual/miss
input
```
