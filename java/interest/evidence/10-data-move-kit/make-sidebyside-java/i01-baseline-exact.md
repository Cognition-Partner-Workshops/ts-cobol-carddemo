# Side-by-side comparison - i01-baseline

Legacy run: `fixtures/interest/i01-baseline/expected`. Target run: `work/parity-java/i01-baseline/actual`.

Compared field by field with `tools/parity/compare.py` (unchanged).

Timestamp rule used (SS3) for TRAN-ORIG-TS and TRAN-PROC-TS: exact: the timestamps of the two runs must be equal (the target ran with the legacy run's clock). 4 timestamp(s) of the target well formed, 0 not.

Result: 0 difference(s): 0 defect(s), 0 classified under a decision or a gap. REL-04 (cycles) and REL-05 (bar for equal) are open; this report does not decide them.

