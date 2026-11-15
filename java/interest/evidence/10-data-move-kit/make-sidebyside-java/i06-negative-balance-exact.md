# Side-by-side comparison - i06-negative-balance

Legacy run: `fixtures/interest/i06-negative-balance/expected`. Target run: `work/parity-java/i06-negative-balance/actual`.

Compared field by field with `tools/parity/compare.py` (unchanged).

Timestamp rule used (SS3) for TRAN-ORIG-TS and TRAN-PROC-TS: exact: the timestamps of the two runs must be equal (the target ran with the legacy run's clock). 2 timestamp(s) of the target well formed, 0 not.

Result: 0 difference(s): 0 defect(s), 0 classified under a decision or a gap. REL-04 (cycles) and REL-05 (bar for equal) are open; this report does not decide them.

