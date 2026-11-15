# export - exportDatasetsStep (ticket 07, INT-T07)

`DatasetExporter.export(runId, outputDir, format)` writes one calculated run from the tables:

| File in `outputDir` | From | Order |
|---|---|---|
| `datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00[.txt]` (350-byte records, CVTRA05Y) | `carddemo.interest_transaction` of the run | `seq_no` |
| `datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS[.txt]` (300-byte records, CVACT01Y) | `carddemo.account`, every row | `acct_id` |
| `rc.json` | the normal end of the run (INT-T03) | - |
| `sysout/INTCALC.STEP15.txt` | written by the calculation step; carried over unchanged | - |

* `format=fixture`: the recorded format (FI-2) - `<DSN>.txt`, one record per line, trailing spaces cut, ISO-8859-1.
* `format=host`: `<DSN>`, fixed-length records with no separator, cp037 with C/D signs. That code page is the
  proposal of REL-08, which is open (gap G01).
* Only a run whose `interest_run.status` is `CALCULATED` is exported; anything else is refused and nothing is written.
* The files are written to `<outputDir>.export-staging-<runId>` and that directory then replaces `outputDir` as a
  whole. The run log of the calculation step must already be in `outputDir`; it is the one file carried over.
* Afterwards `interest_run` is set to `EXPORTED` with `written_systran_count`, `written_systran_tran_amt_sum` (RC5)
  and `written_acctdata_count` plus the five `written_acct_*_sum` columns (RC6). No other row is changed.

## Gaps

* **rc.json mirrors the recorded shape.** The recorded `rc.json` lists six steps under `INTCALC`. Only `STEP15` is
  a step of INTCALC (`app/jcl/INTCALC.jcl:22`); `STEP05`, `STEP10`, `STEP20`, `STEP25` and `STEP30` are steps of
  the harness's setup jobs, which `tools/parity/record.py` carries into the first job's entry. The export writes
  all six as 0 because the comparator (FI-2) compares the file as text. What the target should write outside
  the harness is open.
* The ACCTDATA image holds every account (it must equal the recorded file). REL-03 as proposed writes back only the
  accounts the run updated; no table records which accounts those are, so that selection is left to the write-back
  (excluded from this ticket, strategy CO2).
* The 20-byte FILLER of a transaction is written as spaces of the output code page (rule E4, gap G05); the column
  `tran_filler` is not read.
* `ExportDatasetsTasklet` is the body of `exportDatasetsStep`; the step is not declared in the job, because the job
  configuration (package `batch`) belongs to ticket 06. The tasklet expects `interest_run.run_id` in the job
  execution context under `interestRunId`.
* No stop reason exists for a refused export or a failed close; `WRITE_OUTPUT` (INT-R08) is used. Section 6 of the
  standards names `CLOSE_<file>`, which is for ticket 08 (package `domain`).
