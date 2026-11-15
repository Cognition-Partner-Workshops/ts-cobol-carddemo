# Parity report - ticket 06-calc-step-jdbc (java/interest)

Revision of the code: `eac5c63377cd5bcf3e2e6330e8e71c391a30215e` (branch `demo/05-06-calc-step-jdbc`).
Produced 2026-11-15T16:34:42Z, comparator `tools/parity/compare.py` unchanged.

## What the two sets of runs are

Ticket 06 puts calcInterestStep on the tables of schema `carddemo`. Its fixture cases (i01-baseline,
i05-multi-category, i07-nontie-rounding) are run **through the tables**: section "Through the tables" below.

`make parity-java` (FI-6) still runs the job on the **file adapters**, which stay the default
(`interest.calc.adapters=file`) until the loader (ticket 05) and the exporter (ticket 07) are steps of the job; the
sections written by `parity-report.sh` ("Result per case" to "Job repository and schema") are those runs and show that
the ticket did not change them: seven of seven pass, as after ticket 04.

## Through the tables (`interest.calc.adapters=jdbc`)

Produced by `CalcInterestTablesIT.throughTheTables_everyCaseEqualsTheRecordedLegacyRun` during the
`mvn -B -f java/interest/pom.xml clean verify` of `test-output.txt`, on a Testcontainers `postgres:15` with
`db/postgres/01-transaction-type.sql` and `02-interest.sql` applied. For each case the test puts the four input files
into the replica tables through the record codec, runs `interestCalcJob`, and writes
`java/interest/target/parity-tables/<case>/actual`: the two datasets encoded from `interest_transaction` (in `seq_no`
order) and `account` (in `acct_id` order), the run log the step itself wrote, and `rc.json` of a normal end. The loading
and the encoding are test code (`src/test/.../jdbc/InterestTables.java`), because tickets 05 and 07 own them.

```sh
for c in $(ls fixtures/interest); do
  python3 tools/parity/compare.py interest $c --actual java/interest/target/parity-tables/$c/actual --label java-tables --report work/parity-tables-compare.md
  echo "  exit $?"
done
```

| Case | In the ticket's scope | Comparator exit code | Result |
|---|---|---|---|
| i01-baseline | **yes - fixture case of the ticket** | 0 | **PASS** - `interest/i01-baseline [java-tables]: PASS (4 records, 50 fields, 0 differences)` |
| i02-tie-half-cent | no - run and reported | 0 | **PASS** - `interest/i02-tie-half-cent [java-tables]: PASS (4 records, 50 fields, 0 differences)` |
| i03-zero-rate-skipped | no - run and reported | 0 | **PASS** - `interest/i03-zero-rate-skipped [java-tables]: PASS (2 records, 25 fields, 0 differences)` |
| i04-default-group-fallback | no - run and reported | 0 | **PASS** - `interest/i04-default-group-fallback [java-tables]: PASS (2 records, 25 fields, 0 differences)` |
| i05-multi-category | **yes - fixture case of the ticket** | 0 | **PASS** - `interest/i05-multi-category [java-tables]: PASS (3 records, 38 fields, 0 differences)` |
| i06-negative-balance | no - run and reported | 0 | **PASS** - `interest/i06-negative-balance [java-tables]: PASS (2 records, 25 fields, 0 differences)` |
| i07-nontie-rounding | **yes - fixture case of the ticket** | 0 | **PASS** - `interest/i07-nontie-rounding [java-tables]: PASS (4 records, 50 fields, 0 differences)` |

Comparator report of these runs (`work/parity-tables-compare.md`):

```
## interest / i01-baseline — java-tables: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


## interest / i02-tie-half-cent — java-tables: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


## interest / i03-zero-rate-skipped — java-tables: **PASS**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


## interest / i04-default-group-fallback — java-tables: **PASS**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


## interest / i05-multi-category — java-tables: **PASS**

Checked 2 datasets, 3 records, 38 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


## interest / i06-negative-balance — java-tables: **PASS**

Checked 2 datasets, 2 records, 25 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.


## interest / i07-nontie-rounding — java-tables: **PASS**

Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.
```

## Result per case

`python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java`

| Case | In the ticket's scope | Java job exit code | Comparator exit code | Result | Why |
|---|---|---|---|---|---|
| i01-baseline | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)` |  |
| i02-tie-half-cent | no - not named by the ticket; run and reported | 0 | 0 | **PASS** - `interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)` | file adapters, not changed by this ticket; same result as after ticket 04 |
| i03-zero-rate-skipped | no - not named by the ticket; run and reported | 0 | 0 | **PASS** - `interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)` | file adapters, not changed by this ticket; same result as after ticket 04 |
| i04-default-group-fallback | no - not named by the ticket; run and reported | 0 | 0 | **PASS** - `interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)` | file adapters, not changed by this ticket; same result as after ticket 04 |
| i05-multi-category | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)` |  |
| i06-negative-balance | no - not named by the ticket; run and reported | 0 | 0 | **PASS** - `interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)` | file adapters, not changed by this ticket; same result as after ticket 04 |
| i07-nontie-rounding | **yes - acceptance case of the ticket** | 0 | 0 | **PASS** - `interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)` |  |

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

## Acceptance run: `make parity-java CASE=i05-multi-category`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
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

## Acceptance run: `make parity-java CASE=i07-nontie-rounding`

```
docker compose up -d --wait db
 Container ts-cobol-carddemo-db-1 Running 
 Container ts-cobol-carddemo-db-1 Waiting 
 Container ts-cobol-carddemo-db-1 Healthy 
mvn -q -f java/interest/pom.xml -DskipTests package
interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
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
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794760480-15287-i07-nontie-rounding
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794760478-15287-i06-negative-balance
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794760476-15287-i05-multi-category
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794760475-15287-i04-default-group-fallback
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794760473-15287-i03-zero-rate-skipped
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794760471-15287-i02-tie-half-cent
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794760470-15287-i01-baseline
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794760466-14977-i07-nontie-rounding
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=1 write=1 | 1794760462-14704-i05-multi-category
 interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794760458-14406-i01-baseline
(10 rows)

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
