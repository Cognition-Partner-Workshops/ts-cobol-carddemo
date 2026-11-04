# i07-nontie-rounding
Sub-cent remainders that are not ties: 1000.33 * 18.50 / 1200 = 15.42175 and
777.77 * 21.00 / 1200 = 13.610975.  Truncation and half-even both give 15.42 and 13.61,
so this case passes under `make parity` and `make parity-naive`.
