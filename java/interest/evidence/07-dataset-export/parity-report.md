# Parity report - ticket 07-dataset-export (java/interest)

Revision of the code: `e88aaf25122fef91a37e35de5365171d4169ddc6` (branch `demo/05-07-dataset-export`).
Produced 2026-11-15T16:16:20Z, comparator `tools/parity/compare.py` unchanged.

## Ticket 07: `compare.py` on the exported directories (final-revision evidence)

The export of this ticket is not a step of the job yet (the job configuration, package `batch`, belongs to
ticket 06), so `make parity-java` below still runs the file adapters of ticket 01 and does not exercise it.
The directories compared here are written by the export itself, in `DatasetExportIT`
(`intR06_i01BaselineExportEqualsRecorded`, `e2_i06NegativeAmountsAreDSignedAsRecorded`) during the
`mvn -B -f java/interest/pom.xml clean verify` of `test-output.txt`, same revision: the tables hold the finished
run inserted by SQL (`java/interest/src/test/resources/export/<case>-finished-run.sql`), the run log is written by
`calcInterestStep` on the same case, and `DatasetExporter.export(1, target/export-parity/<case>/actual, FIXTURE)`
replaces the directory.

```sh
for c in i01-baseline i06-negative-balance; do
  python3 tools/parity/compare.py interest $c --actual java/interest/target/export-parity/$c/actual --label java-export --report work/export-compare.md
  echo "  exit $?"
done
```
```
interest/i01-baseline [java-export]: PASS (4 records, 50 fields, 0 differences)
  exit 0
interest/i06-negative-balance [java-export]: PASS (2 records, 25 fields, 0 differences)
  exit 0
```

| Case named by the ticket | Exported directory | Comparator exit code | Result |
|---|---|---|---|
| i01-baseline | `java/interest/target/export-parity/i01-baseline/actual` | 0 | **PASS** (4 records, 50 fields, 0 differences) |
| i06-negative-balance | `java/interest/target/export-parity/i06-negative-balance/actual` | 0 | **PASS** (2 records, 25 fields, 0 differences) |

The other five cases do not apply to the export: the ticket names no finished run for them, and no table-writing
calculation step exists on this branch to produce one. Their `make parity-java` result (file adapters) is in the
table of the next section, unchanged from the base branch.

Files under `java/interest/target/export-parity` and record counts:

```
./i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
./i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
./i01-baseline/actual/rc.json
./i01-baseline/actual/sysout/INTCALC.STEP15.txt
./i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
./i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
./i06-negative-balance/actual/rc.json
./i06-negative-balance/actual/sysout/INTCALC.STEP15.txt
   2 i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
   2 i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
   1 i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
   1 i06-negative-balance/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
   6 total
```

Comparator report (`work/export-compare.md`):

### interest / i01-baseline — java-export: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i06-negative-balance — java-export: **PASS**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.

## Result per case: `make parity-java` (file adapters of ticket 01, not the export)

`python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java`

| Case | In the ticket's scope | Java job exit code | Comparator exit code | Result | Why |
|---|---|---|---|---|---|
| i01-baseline | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)` |  |
| i02-tie-half-cent | no | 0 | 0 | **PASS** - `interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)` | the ticket names i01-baseline and i06-negative-balance only; passes on the base branch already |
| i03-zero-rate-skipped | no | 12 | 1 | **FAIL** - `interest/i03-zero-rate-skipped [java]: FAIL (2 records, 25 fields, 4 differences)` | INT-R04 is ticket 04 (not in `demo/05b-integration`): the job stops with ZERO_RATE_NOT_BUILT, as on the base branch; this ticket does not touch the calculation |
| i04-default-group-fallback | no | 12 | 1 | **FAIL** - `interest/i04-default-group-fallback [java]: FAIL (2 records, 25 fields, 4 differences)` | INT-R03 is ticket 04 (not in `demo/05b-integration`): the job stops with RATE_FALLBACK_NOT_BUILT, as on the base branch; this ticket does not touch the calculation |
| i05-multi-category | no | 0 | 0 | **PASS** - `interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)` | the ticket names i01-baseline and i06-negative-balance only; passes on the base branch already |
| i06-negative-balance | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)` |  |
| i07-nontie-rounding | no | 0 | 0 | **PASS** - `interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)` | the ticket names i01-baseline and i06-negative-balance only; passes on the base branch already |

## Acceptance run: `make parity-java CASE=i01-baseline`

```
docker compose up -d --wait db
 Network ts-cobol-carddemo_default Creating 
 Volume ts-cobol-carddemo_carddemo-db Creating 
 Volume ts-cobol-carddemo_carddemo-db Creating 
 Network ts-cobol-carddemo_default Creating 
 Volume ts-cobol-carddemo_carddemo-db Created 
 Volume ts-cobol-carddemo_carddemo-db Created 
 Network ts-cobol-carddemo_default Created 
 Network ts-cobol-carddemo_default Created 
 Container ts-cobol-carddemo-db-1 Creating 
 Container ts-cobol-carddemo-db-1 Created 
 Container ts-cobol-carddemo-db-1 Starting 
 Container ts-cobol-carddemo-db-1 Started 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
Unable to find image 'eclipse-temurin:21-jre' locally
21-jre: Pulling from library/eclipse-temurin
d2157724d428: Pulling fs layer
a034bdfd5bba: Pulling fs layer
517b871c26ea: Pulling fs layer
96aae9022e0e: Pulling fs layer
60ca781aad8b: Pulling fs layer
1ed58412d3b9: Pulling fs layer
a034bdfd5bba: Already exists
517b871c26ea: Already exists
96aae9022e0e: Already exists
d2157724d428: Download complete
1ed58412d3b9: Download complete
60ca781aad8b: Download complete
517b871c26ea: Pull complete
96aae9022e0e: Pull complete
1b2a18b4af3a: Download complete
dfe24317d103: Download complete
a034bdfd5bba: Pull complete
d2157724d428: Pull complete
1ed58412d3b9: Pull complete
60ca781aad8b: Pull complete
Digest: sha256:18bb4a91a3dea3f03ee31d3755af34dfb40f904fc77ab56cdc2f092e32fff9c2
Status: Downloaded newer image for eclipse-temurin:21-jre
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

## All cases: `make parity-java` (exit code 2)

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
2026-11-15T16:16:13.850Z ERROR 1 --- [interest] [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step calcInterestStep in job interestCalcJob
com.carddemo.interest.domain.InterestRunStoppedException: ZERO_RATE_NOT_BUILT (INT-R04): a balance has a zero rate; the no-interest rule is ticket 04
2026-11-15T16:16:13.852Z ERROR 1 --- [interest] [           main] c.c.interest.batch.CalcInterestRun       : run 1794759372-11419-i03-zero-rate-skipped step calcInterestStep stopped: accounts=0 transactions=0; no output kept
interest/i03-zero-rate-skipped: java job ended with exit code 12
interest/i03-zero-rate-skipped [java]: FAIL (2 records, 25 fields, 4 differences)
2026-11-15T16:16:15.414Z ERROR 1 --- [interest] [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step calcInterestStep in job interestCalcJob
com.carddemo.interest.domain.InterestRunStoppedException: RATE_FALLBACK_NOT_BUILT (INT-R03): a group has no rate for a balance; the DEFAULT fallback is ticket 04
2026-11-15T16:16:15.416Z ERROR 1 --- [interest] [           main] c.c.interest.batch.CalcInterestRun       : run 1794759373-11419-i04-default-group-fallback step calcInterestStep stopped: accounts=0 transactions=0; no output kept
interest/i04-default-group-fallback: java job ended with exit code 12
interest/i04-default-group-fallback [java]: FAIL (2 records, 25 fields, 4 differences)
interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
report: work/parity-java/report.md
make: *** [Makefile:96: parity-java] Error 1
```

## Comparator report (`work/parity-java/compare.md`, written by tools/parity/compare.py)

### interest / i01-baseline — java: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i02-tie-half-cent — java: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


### interest / i03-zero-rate-skipped — java: **FAIL (4 differences)**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.

| Where | Field | Expected (legacy) | Actual (candidate) |
|---|---|---|---|
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | `(file)` | 1 records | missing |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | `(file)` | 1 records | missing |
| SYSOUT INTCALC.STEP15 | `(text)` |  | @@ -1,4 +0,0 @@<br>-START OF EXECUTION OF PROGRAM CBACT04C<br>-000000020050100010000010000{                      <br>-000000020050200010000003000}                      <br>-END OF EXECUTION OF PROGRAM CBACT04C |
| rc.json | `(text)` |  | @@ -1,13 +0,0 @@<br>-{<br>- "INTCALC": {<br>-  "maxrc": 0,<br>-  "steps": {<br>-   "STEP05": 0,<br>-   "STEP10": 0,<br>-   "STEP15": 0,<br>-   "STEP20": 0,<br>-   "STEP25": 0,<br>-   "STEP30": 0<br>-  }<br>- }<br>-} |

Fields differing: `(file)`, `(text)`

### interest / i04-default-group-fallback — java: **FAIL (4 differences)**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.

| Where | Field | Expected (legacy) | Actual (candidate) |
|---|---|---|---|
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS | `(file)` | 1 records | missing |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 | `(file)` | 1 records | missing |
| SYSOUT INTCALC.STEP15 | `(text)` |  | @@ -1,5 +0,0 @@<br>-START OF EXECUTION OF PROGRAM CBACT04C<br>-000000020060100010000010000{                      <br>-DISCLOSURE GROUP RECORD MISSING<br>-TRY WITH DEFAULT GROUP CODE<br>-END OF EXECUTION OF PROGRAM CBACT04C |
| rc.json | `(text)` |  | @@ -1,13 +0,0 @@<br>-{<br>- "INTCALC": {<br>-  "maxrc": 0,<br>-  "steps": {<br>-   "STEP05": 0,<br>-   "STEP10": 0,<br>-   "STEP15": 0,<br>-   "STEP20": 0,<br>-   "STEP25": 0,<br>-   "STEP30": 0<br>-  }<br>- }<br>-} |

Fields differing: `(file)`, `(text)`

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
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794759378-11419-i07-nontie-rounding
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794759377-11419-i06-negative-balance
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794759375-11419-i05-multi-category
 interestCalcJob | FAILED    | calcInterestStep:FAILED read=1 write=0    | 1794759373-11419-i04-default-group-fallback
 interestCalcJob | FAILED    | calcInterestStep:FAILED read=1 write=0    | 1794759372-11419-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794759370-11419-i02-tie-half-cent
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794759369-11419-i01-baseline
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794759365-11148-i06-negative-balance
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794759356-10751-i01-baseline
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
