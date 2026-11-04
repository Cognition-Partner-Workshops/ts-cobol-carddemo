# i02-tie-half-cent  (the rounding-tie case)
Account 2003: 1235.50 * 12.00 / 1200 = 12.355 — exactly half a cent.
CBACT04C's COMPUTE has no ROUNDED phrase, so COBOL truncates to **12.35**;
a half-even (banker's) reference rounds to **12.36**.  Account 2004: 1234.50 gives
12.345, where half-even also yields 12.34, so only account 2003 diverges.
`make parity-naive CASE=i02-tie-half-cent` must fail on TRAN-AMT and ACCT-CURR-BAL
for account 2003 only.
