# Parity report - ticket 02-schema (java/interest)

Revision of the code: `aaec6b539982a9346c27d62b032b899e6695dc22` (branch `demo/05-02-schema`).
Produced 2026-11-15T10:55:08Z, comparator `tools/parity/compare.py` unchanged.

## Result per case

`python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java`

| Case | In the ticket's scope | Java job exit code | Comparator exit code | Result | Why |
|---|---|---|---|---|---|
| i01-baseline | no - N/A, ticket 02-schema names no fixture case (no data); named on the command line only because parity-report.sh needs one case | 0 | 0 | **PASS** - `interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)` | unchanged from demo/05b-integration (pilot: PASS) |
| i02-tie-half-cent | no - N/A, ticket 02-schema names no fixture case (no data) | 0 | 0 | **PASS** - `interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)` | unchanged from demo/05b-integration (pilot: PASS) |
| i03-zero-rate-skipped | no - N/A, ticket 02-schema names no fixture case (no data) | 12 | 1 | **FAIL** - `interest/i03-zero-rate-skipped [java]: FAIL (2 records, 25 fields, 4 differences)` | unchanged from demo/05b-integration: the job stops with ZERO_RATE_NOT_BUILT (INT-R04, ticket 04); the pilot evidence shows the same exit 12 and FAIL |
| i04-default-group-fallback | no - N/A, ticket 02-schema names no fixture case (no data) | 12 | 1 | **FAIL** - `interest/i04-default-group-fallback [java]: FAIL (2 records, 25 fields, 4 differences)` | unchanged from demo/05b-integration: the job stops with RATE_FALLBACK_NOT_BUILT (INT-R03, ticket 04); the pilot evidence shows the same exit 12 and FAIL |
| i05-multi-category | no - N/A, ticket 02-schema names no fixture case (no data) | 0 | 0 | **PASS** - `interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)` | unchanged from demo/05b-integration (pilot: PASS) |
| i06-negative-balance | no - N/A, ticket 02-schema names no fixture case (no data) | 0 | 0 | **PASS** - `interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)` | unchanged from demo/05b-integration (pilot: PASS) |
| i07-nontie-rounding | no - N/A, ticket 02-schema names no fixture case (no data) | 0 | 0 | **PASS** - `interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)` | unchanged from demo/05b-integration (pilot: PASS) |

## Run of one case: `make parity-java CASE=i01-baseline` (not an acceptance case: the ticket has none; the db service holds the six new tables during this run)

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
Unable to find image 'eclipse-temurin:21-jre' locally
21-jre: Pulling from library/eclipse-temurin
d2157724d428: Pulling fs layer
a034bdfd5bba: Pulling fs layer
96aae9022e0e: Pulling fs layer
517b871c26ea: Pulling fs layer
60ca781aad8b: Pulling fs layer
1ed58412d3b9: Pulling fs layer
517b871c26ea: Already exists
a034bdfd5bba: Already exists
96aae9022e0e: Already exists
d2157724d428: Download complete
1ed58412d3b9: Download complete
dfe24317d103: Download complete
60ca781aad8b: Download complete
1b2a18b4af3a: Download complete
517b871c26ea: Pull complete
96aae9022e0e: Pull complete
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

## All cases: `make parity-java` (exit code 2)

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
2026-11-15T10:55:01.734Z ERROR 1 --- [interest] [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step calcInterestStep in job interestCalcJob
com.carddemo.interest.domain.InterestRunStoppedException: ZERO_RATE_NOT_BUILT (INT-R04): a balance has a zero rate; the no-interest rule is ticket 04
2026-11-15T10:55:01.736Z ERROR 1 --- [interest] [           main] c.c.interest.batch.CalcInterestRun       : run 1794740100-11009-i03-zero-rate-skipped step calcInterestStep stopped: accounts=0 transactions=0; no output kept
interest/i03-zero-rate-skipped: java job ended with exit code 12
interest/i03-zero-rate-skipped [java]: FAIL (2 records, 25 fields, 4 differences)
2026-11-15T10:55:03.212Z ERROR 1 --- [interest] [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step calcInterestStep in job interestCalcJob
com.carddemo.interest.domain.InterestRunStoppedException: RATE_FALLBACK_NOT_BUILT (INT-R03): a group has no rate for a balance; the DEFAULT fallback is ticket 04
2026-11-15T10:55:03.215Z ERROR 1 --- [interest] [           main] c.c.interest.batch.CalcInterestRun       : run 1794740101-11009-i04-default-group-fallback step calcInterestStep stopped: accounts=0 transactions=0; no output kept
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
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794740106-11009-i07-nontie-rounding
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794740104-11009-i06-negative-balance
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794740103-11009-i05-multi-category
 interestCalcJob | FAILED    | calcInterestStep:FAILED read=1 write=0    | 1794740101-11009-i04-default-group-fallback
 interestCalcJob | FAILED    | calcInterestStep:FAILED read=1 write=0    | 1794740100-11009-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794740098-11009-i02-tie-half-cent
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794740097-11009-i01-baseline
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794740088-10610-i01-baseline
(8 rows)

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
