# Parity report - ticket 07-dataset-export (java/interest)

Revision of the code: `f4c9161d82e32f343f798f111fffdb983f88eb67` (branch `demo/05-07-dataset-export`).
Produced 2026-11-15T11:34:24Z, comparator `tools/parity/compare.py` unchanged.

## Result per case

`python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java`

| Case | In the ticket's scope | Java job exit code | Comparator exit code | Result | Why |
|---|---|---|---|---|---|
| i01-baseline | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)` | no regression of the job; the export itself is proved by the second table |
| i02-tie-half-cent | no - not named by the ticket; run to show no regression | 0 | 0 | **PASS** - `interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)` | passed on demo/05b-integration before this change and still passes; the export step is not in interestCalcJob yet, so this run does not go through it |
| i03-zero-rate-skipped | no - not named by the ticket; run to show no regression | 0 | 0 | **PASS** - `interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)` | passed on demo/05b-integration before this change and still passes; the export step is not in interestCalcJob yet, so this run does not go through it |
| i04-default-group-fallback | no - not named by the ticket; run to show no regression | 0 | 0 | **PASS** - `interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)` | passed on demo/05b-integration before this change and still passes; the export step is not in interestCalcJob yet, so this run does not go through it |
| i05-multi-category | no - not named by the ticket; run to show no regression | 0 | 0 | **PASS** - `interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)` | passed on demo/05b-integration before this change and still passes; the export step is not in interestCalcJob yet, so this run does not go through it |
| i06-negative-balance | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)` | no regression of the job; the export itself is proved by the second table |
| i07-nontie-rounding | no - not named by the ticket; run to show no regression | 0 | 0 | **PASS** - `interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)` | passed on demo/05b-integration before this change and still passes; the export step is not in interestCalcJob yet, so this run does not go through it |

## What this table proves for ticket 07, and the export's own comparison

`make parity-java` runs `interestCalcJob`, which on this branch still has one step on file adapters
(`batch/InterestJobConfig`, not an Allowed file of this ticket). `exportDatasetsStep` is offered as a bean and
is not part of that job, so the table above proves that the seven cases still pass with this change, not the
export. The export is proved on directories that `DatasetExportIT` exports from PostgreSQL 15 rows inserted
by SQL (ticket Verification: "compare.py PASS on the exported directories"); the commands and their output are
at the end of `test-output.txt`:

| Case | Directory | Comparator exit code | Result |
|---|---|---|---|
| i01-baseline | `java/interest/target/exported/i01-baseline/actual` | 0 | **PASS** - `interest/i01-baseline [java-export]: PASS (4 records, 50 fields, 0 differences)` |
| i06-negative-balance | `java/interest/target/exported/i06-negative-balance/actual` | 0 | **PASS** - `interest/i06-negative-balance [java-export]: PASS (2 records, 25 fields, 0 differences)` |

## Acceptance run: `make parity-java CASE=i01-baseline`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
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

## Acceptance run: `make parity-java CASE=i06-negative-balance`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
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

## All cases: `make parity-java` (exit code 0)

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)
interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)
interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
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

    job_name     |  status   |                   steps                   |                   run_id                    
-----------------+-----------+-------------------------------------------+---------------------------------------------
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794742462-14770-i07-nontie-rounding
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794742460-14770-i06-negative-balance
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794742458-14770-i05-multi-category
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794742455-14770-i04-default-group-fallback
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794742453-14770-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794742452-14770-i02-tie-half-cent
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794742450-14770-i01-baseline
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794742447-14454-i06-negative-balance
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794742443-14144-i01-baseline
(9 rows)

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
