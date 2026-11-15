# Side-by-side comparison - i01-demo

Legacy run: `fixtures/interest/i01-baseline/expected`. Target run: `work/sidebyside-demo-target`.

Compared field by field with `tools/parity/compare.py` (unchanged).

Timestamp rule used (SS3) for TRAN-ORIG-TS and TRAN-PROC-TS: format only: a timestamp of the target is accepted when it has the form YYYY-MM-DD-HH.MM.SS.hh0000 (G10), whatever its value. 4 timestamp(s) of the target well formed, 0 not.

Result: 2 difference(s): 1 defect(s), 1 classified under a decision or a gap. REL-04 (cycles) and REL-05 (bar for equal) are open; this report does not decide them.

| Where | Field | Legacy | Target | Class | Why |
|---|---|---|---|---|---|
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-AMT` | 10.00 | 10.01 | **INT-Q01** | one cent apart: truncated on one side, rounded on the other |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 2 (TRAN-ID=2022071800000002) | `TRAN-CARD-NUM` | 4000000000002002 | 4000000000002003 | **defect** | no decision or gap explains a difference in this field |
