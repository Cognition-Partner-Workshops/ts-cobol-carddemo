# Side-by-side comparison - i01-demo

Legacy run: `fixtures/interest/i01-baseline/expected`. Target run: `work/sidebyside-demo-target`.

Compared field by field with `tools/parity/compare.py` (unchanged).

Timestamp rule used (SS3) for TRAN-ORIG-TS and TRAN-PROC-TS: exact: the timestamps of the two runs must be equal (the target ran with the legacy run's clock). 4 timestamp(s) of the target well formed, 0 not.

Result: 4 difference(s): 1 defect(s), 3 classified under a decision or a gap. REL-04 (cycles) and REL-05 (bar for equal) are open; this report does not decide them.

| Where | Field | Legacy | Target | Class | Why |
|---|---|---|---|---|---|
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-AMT` | 10.00 | 10.01 | **INT-Q01** | one cent apart: truncated on one side, rounded on the other |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-ORIG-TS` | 2022-07-05-10.00.00.000000 | 2022-07-06-01.02.03.450000 | **INT-Q03** | both are well formed and differ in value: the runs took their clocks at different moments |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-06-01.02.03.450000 | **INT-Q03** | both are well formed and differ in value: the runs took their clocks at different moments |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 2 (TRAN-ID=2022071800000002) | `TRAN-CARD-NUM` | 4000000000002002 | 4000000000002003 | **defect** | no decision or gap explains a difference in this field |
