# Parity report - ticket 01 pilot (java/interest)

Revision of the code: `0294cc0bc9f1e6b32d8ff06c09546346e55c7a2a` (branch `demo/05-pilot`). Produced 2026-11-15T08:47:57Z on JDK 21, PostgreSQL 15 (`db` service of docker-compose.yml), comparator `tools/parity/compare.py` unchanged.

## Result per case

`python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java`

| Case | In the pilot's scope | Java job exit code | Comparator exit code | Result | Why |
|---|---|---|---|---|---|
| i01-baseline | **yes - the acceptance case of ticket 01** | 0 | 0 | **PASS** - `interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)` |  |
| i02-tie-half-cent | no fixture named by the ticket; rule INT-Q01 is in scope | 0 | 0 | **PASS** - `interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)` | truncation (INT-Q01) is built |
| i03-zero-rate-skipped | no (INT-R04, ticket 04) | 12 | 1 | **FAIL** - `interest/i03-zero-rate-skipped [java]: FAIL (2 records, 25 fields, 4 differences)` | the job stops with ZERO_RATE_NOT_BUILT (INT-R04) and leaves no output |
| i04-default-group-fallback | no (INT-R03, ticket 04) | 12 | 1 | **FAIL** - `interest/i04-default-group-fallback [java]: FAIL (2 records, 25 fields, 4 differences)` | the job stops with RATE_FALLBACK_NOT_BUILT (INT-R03) and leaves no output |
| i05-multi-category | no (several categories, ticket 04) | 0 | 0 | **PASS** - `interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)` | passes without extra code: one item is one account with all its balances |
| i06-negative-balance | no (INT-Q05, ticket 04) | 0 | 0 | **PASS** - `interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)` | passes without extra code: the formula and RoundingMode.DOWN are sign-neutral; INT-Q05 has no switch or test yet |
| i07-nontie-rounding | no fixture named by the ticket; rules INT-R05, INT-Q01 are in scope | 0 | 0 | **PASS** - `interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)` |  |

Ticket 01 expected the six other cases to fail until ticket 04. Four of them pass already; only i03 and i04 fail, and they fail by stopping, not by writing different output. Passing here is not acceptance of ticket 04: INT-Q05 and the several-category case have no test in this ticket.

## Acceptance run: `make parity-java CASE=i01-baseline` (exit code 0)

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

`datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt`

```
2022071800000001010005System    Int. for a/c 00000002001                                                                            0000000100{000000000                                                                                                              40000000000020012022-07-05-10.00.00.0000002022-07-05-10.00.00.000000
2022071800000002010005System    Int. for a/c 00000002002                                                                            0000000370{000000000                                                                                                              40000000000020022022-07-05-10.00.00.0000002022-07-05-10.00.00.000000

```

`datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt`

```
00000002001Y00000010100{00000050000{00000025000{2019-03-152026-12-312026-12-3100000000000{00000000000{10115     GOLD
00000002002Y00000024000{00000060000{00000030000{2019-03-152026-12-312026-12-3100000024000{00000000000{10115     STANDARD

```

`sysout/INTCALC.STEP15.txt`

```
START OF EXECUTION OF PROGRAM CBACT04C
000000020010100010000010000{                      
000000020020100010000024000{                      
END OF EXECUTION OF PROGRAM CBACT04C

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

Observable values of the ticket: TRAN-AMT `0000000100{` = 10.00 (account 00000002001) and `0000000370{` = 37.00 (00000002002); ACCT-CURR-BAL of 00000002001 `00000010100{` = 1010.00; ACCT-CURR-BAL of 00000002002 `00000024000{` = 2400.00, unchanged (INT-Q04).

## All seven cases: `make parity-java` (exit code 2, expected non-zero while i03 and i04 are outside the pilot)

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
2026-11-15T08:47:50.382Z ERROR 1 --- [interest] [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step calcInterestStep in job interestCalcJob
com.carddemo.interest.domain.InterestRunStoppedException: ZERO_RATE_NOT_BUILT (INT-R04): a balance has a zero rate; the no-interest rule is ticket 04
2026-11-15T08:47:50.385Z ERROR 1 --- [interest] [           main] c.c.interest.batch.CalcInterestRun       : run 1794732468-21803-i03-zero-rate-skipped step calcInterestStep stopped: accounts=0 transactions=0; no output kept
interest/i03-zero-rate-skipped: java job ended with exit code 12
interest/i03-zero-rate-skipped [java]: FAIL (2 records, 25 fields, 4 differences)
2026-11-15T08:47:51.901Z ERROR 1 --- [interest] [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step calcInterestStep in job interestCalcJob
com.carddemo.interest.domain.InterestRunStoppedException: RATE_FALLBACK_NOT_BUILT (INT-R03): a group has no rate for a balance; the DEFAULT fallback is ticket 04
2026-11-15T08:47:51.904Z ERROR 1 --- [interest] [           main] c.c.interest.batch.CalcInterestRun       : run 1794732470-21803-i04-default-group-fallback step calcInterestStep stopped: accounts=0 transactions=0; no output kept
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


## Job repository and schema after these runs (`db` service, PostgreSQL 15)

```
                                                       version                                                        
----------------------------------------------------------------------------------------------------------------------
 PostgreSQL 15.20 (Debian 15.20-1.pgdg13+1) on x86_64-pc-linux-gnu, compiled by gcc (Debian 14.2.0-19) 14.2.0, 64-bit
(1 row)

    job_name     |  status   | steps |                   step                    |                   run_id                    
-----------------+-----------+-------+-------------------------------------------+---------------------------------------------
 interestCalcJob | COMPLETED |     1 | calcInterestStep:COMPLETED read=2 write=2 | 1794732475-21803-i07-nontie-rounding
 interestCalcJob | COMPLETED |     1 | calcInterestStep:COMPLETED read=1 write=1 | 1794732473-21803-i06-negative-balance
 interestCalcJob | COMPLETED |     1 | calcInterestStep:COMPLETED read=1 write=1 | 1794732472-21803-i05-multi-category
 interestCalcJob | FAILED    |     1 | calcInterestStep:FAILED read=1 write=0    | 1794732470-21803-i04-default-group-fallback
 interestCalcJob | FAILED    |     1 | calcInterestStep:FAILED read=1 write=0    | 1794732468-21803-i03-zero-rate-skipped
 interestCalcJob | COMPLETED |     1 | calcInterestStep:COMPLETED read=2 write=2 | 1794732467-21803-i02-tie-half-cent
 interestCalcJob | COMPLETED |     1 | calcInterestStep:COMPLETED read=2 write=2 | 1794732465-21803-i01-baseline
 interestCalcJob | COMPLETED |     1 | calcInterestStep:COMPLETED read=2 write=2 | 1794732461-21473-i01-baseline
(8 rows)

 table_schema |        table_name        
--------------+--------------------------
 carddemo     | transaction_type
 carddemo     | transaction_type_categor
(2 rows)

```

The run ids ending in the case name are the runs above (newest first); for i01-baseline there is one COMPLETED execution of interestCalcJob with one step per run. Schema carddemo holds only the two tables of db/postgres/01-transaction-type.sql.
