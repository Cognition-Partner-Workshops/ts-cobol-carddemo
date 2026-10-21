# CBACT04C — monthly interest calculator

Job: INTCALC (app/jcl/INTCALC.jcl), step STEP15, PARM '2022071800'. Reads: transaction category balances (TCATBALF, sequential by key, input only), card cross-reference (XREFFILE, random by the account-id alternate key), account master (ACCTFILE, random, rewritten), disclosure groups (DISCGRP, random). Writes: interest transactions (TRANSACT, dataset `interest`) and the rewritten account master (dataset `accounts`). Runs after POSTTRAN and reads the category balances and account balances POSTTRAN left behind.

## Rules

| Rule | COBOL citation | Acceptance test | Status |
|---|---|---|---|
| R01 Category balance records are processed in key order (account, transaction type, category) and one interest transaction is written for each category balance whose interest rate is not zero, in that same order. | `app/cbl/CBACT04C.cbl:188-217` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r01_one_transaction_per_rated_category_in_key_order` | green |
| R02 The annual interest rate is looked up in the disclosure groups by the account's group id plus the category balance's transaction type and category code. | `app/cbl/CBACT04C.cbl:210-213` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r02_rate_from_account_group_type_and_category` | green |
| R03 When the disclosure groups have no record for that group, type and category (blank group, or a group such as GOLDPROMO1 with no records), the rate of the DEFAULT group for the same type and category is used. | `app/cbl/CBACT04C.cbl:436-439` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r03_missing_group_rate_falls_back_to_default` | green |
| R04 A category balance whose rate is zero produces no interest transaction (for example payments and credits, and account 51 type 07 under DEFAULT). | `app/cbl/CBACT04C.cbl:214-217` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r04_zero_rate_writes_no_transaction` | green |
| R05 The monthly interest on a category balance is interest = category balance x annual rate / 1200. | `app/cbl/CBACT04C.cbl:464-465` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r05_monthly_interest_is_balance_times_rate_over_1200` | green |
| R06 The monthly interest is truncated to the cent, never rounded (243.41 x 15.00 / 1200 = 3.042625 gives 3.04; 61.77 x 25.00 / 1200 = 1.286875 gives 1.28). | `app/cbl/CBACT04C.cbl:464-465` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r06_interest_truncated_to_cents_not_rounded` | green |
| R07 A zero category balance with a non-zero rate still produces an interest transaction of 0.00 (the decision is on the rate, not the amount). | `app/cbl/CBACT04C.cbl:214-217` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r07_zero_balance_with_rate_writes_zero_amount` | green |
| R08 A negative (credit) category balance with a non-zero rate produces a negative interest transaction by the same formula, truncated toward zero (-139.76 x 15.00 / 1200 = -1.747 gives -1.74), which is then added to the account balance. | `app/cbl/CBACT04C.cbl:464-465` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r08_negative_balance_earns_no_interest_credit` | amber (business question pending) |
| R09 The interest transaction id is the 10-character JCL PARM date followed by a 6-digit sequence number that starts at 000001 and goes up by one for every interest transaction written (2022071800000001, 2022071800000002, ...). | `app/cbl/CBACT04C.cbl:474-480` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r09_transaction_id_is_parm_date_plus_sequence` | green |
| R10 Every interest transaction has type code 01, category code 0005, source 'System', merchant id 0 and blank merchant name, city and zip. | `app/cbl/CBACT04C.cbl:482-494` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r10_fixed_transaction_fields` | green |
| R11 The interest transaction description is 'Int. for a/c ' followed by the 11-digit zero-padded account id. | `app/cbl/CBACT04C.cbl:485-489` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r11_description_names_the_account` | green |
| R12 The interest transaction card number is the card the cross-reference file holds for the account (looked up by account id). | `app/cbl/CBACT04C.cbl:393-398` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r12_card_number_from_cross_reference` | green |
| R13 The origination and processing timestamps of every interest transaction are both the run date and time in the form YYYY-MM-DD-HH.MM.SS.hh0000 (hundredths of a second followed by '0000'); for the run clock 2022/07/18 01:00:00.00 that is 2022-07-18-01.00.00.000000. | `app/cbl/CBACT04C.cbl:613-626` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r13_timestamps_are_run_time_in_db2_format` | green |
| R14 When the program moves on to the next account, the previous account's current balance becomes current balance + the sum of all its interest transactions of this run. | `app/cbl/CBACT04C.cbl:350-352` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r14_account_balance_gains_total_interest` | green |
| R15 At the same time the account's current cycle credit and current cycle debit are reset to 0.00, even when its total interest is zero. | `app/cbl/CBACT04C.cbl:353-354` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r15_cycle_credit_and_debit_reset_to_zero` | green |
| R16 The account rewrite changes only the current balance and the two cycle totals; every other account field keeps its value. | `app/cbl/CBACT04C.cbl:350-356` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r16_other_account_fields_unchanged` | green |
| R17 The last account in category balance order is never rewritten: its interest transactions are written but its balance does not receive the interest and its cycle totals are not reset (account 54 keeps balance 90.00 and cycle credit 90.00 although interest transaction 2022071800000088 of 1.12 is written). | `app/cbl/CBACT04C.cbl:188-222` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r17_last_account_is_updated_too` | amber (business question pending) |
| R18 The interest calculation does not change the transaction category balances (the file is opened for input only); interest is not posted to any category. | `app/cbl/CBACT04C.cbl:234-236` | `spec/tests/rules/test_cbact04c.py::test_cbact04c_r18_category_balances_not_changed_by_interest` | green |

## Data dictionary

Offsets are 0-based byte positions in the record. Signed numeric fields are zoned decimal with the sign overpunched in the last byte; `V99` means two implied decimals.

### CVTRA01Y TRAN-CAT-BAL-RECORD (TCATBALF, read)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| TRANCAT-ACCT-ID | 9(11) | 0 | 11 | Account id, first part of the key; drives the account break |
| TRANCAT-TYPE-CD | X(02) | 11 | 2 | Transaction type, second part of the key and of the rate key |
| TRANCAT-CD | 9(04) | 13 | 4 | Transaction category, third part of the key and of the rate key |
| TRAN-CAT-BAL | S9(09)V99 | 17 | 11 | Category balance the interest is computed on |
| FILLER | X(22) | 28 | 22 | Unused |

### CVACT03Y CARD-XREF-RECORD (XREFFILE, read by alternate key)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| XREF-CARD-NUM | X(16) | 0 | 16 | Card number, primary key; copied to TRAN-CARD-NUM |
| XREF-CUST-ID | 9(09) | 16 | 9 | Customer id (not used) |
| XREF-ACCT-ID | 9(11) | 25 | 11 | Account id, alternate key used for the lookup |
| FILLER | X(14) | 36 | 14 | Unused |

### CVTRA02Y DIS-GROUP-RECORD (DISCGRP, read)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| DIS-ACCT-GROUP-ID | X(10) | 0 | 10 | Disclosure (rate) group; 'DEFAULT' is the fallback group |
| DIS-TRAN-TYPE-CD | X(02) | 10 | 2 | Transaction type |
| DIS-TRAN-CAT-CD | 9(04) | 12 | 4 | Transaction category |
| DIS-INT-RATE | S9(04)V99 | 16 | 6 | Annual interest rate in percent (15.00 = 15 %); 0 means no interest |
| FILLER | X(28) | 22 | 28 | Unused |

### CVACT01Y ACCOUNT-RECORD (ACCTFILE, read and rewritten)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| ACCT-ID | 9(11) | 0 | 11 | Account id, key |
| ACCT-ACTIVE-STATUS | X(01) | 11 | 1 | Active flag (unchanged) |
| ACCT-CURR-BAL | S9(10)V99 | 12 | 12 | Current balance; receives the account's total interest |
| ACCT-CREDIT-LIMIT | S9(10)V99 | 24 | 12 | Credit limit (unchanged) |
| ACCT-CASH-CREDIT-LIMIT | S9(10)V99 | 36 | 12 | Cash credit limit (unchanged) |
| ACCT-OPEN-DATE | X(10) | 48 | 10 | Open date (unchanged) |
| ACCT-EXPIRAION-DATE | X(10) | 58 | 10 | Expiration date (unchanged) |
| ACCT-REISSUE-DATE | X(10) | 68 | 10 | Reissue date (unchanged) |
| ACCT-CURR-CYC-CREDIT | S9(10)V99 | 78 | 12 | Current cycle credits; reset to 0 |
| ACCT-CURR-CYC-DEBIT | S9(10)V99 | 90 | 12 | Current cycle debits; reset to 0 |
| ACCT-ADDR-ZIP | X(10) | 102 | 10 | Address zip (unchanged) |
| ACCT-GROUP-ID | X(10) | 112 | 10 | Disclosure group of the account, first part of the rate key |
| FILLER | X(178) | 122 | 178 | Unused |

### CVTRA05Y TRAN-RECORD (TRANSACT, written; dataset `interest`)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| TRAN-ID | X(16) | 0 | 16 | PARM date (10) + 6-digit sequence |
| TRAN-TYPE-CD | X(02) | 16 | 2 | Always '01' |
| TRAN-CAT-CD | 9(04) | 18 | 4 | Always 0005 |
| TRAN-SOURCE | X(10) | 22 | 10 | Always 'System' |
| TRAN-DESC | X(100) | 32 | 100 | 'Int. for a/c ' + 11-digit account id |
| TRAN-AMT | S9(09)V99 | 132 | 11 | Monthly interest, truncated to the cent |
| TRAN-MERCHANT-ID | 9(09) | 143 | 9 | Always 0 |
| TRAN-MERCHANT-NAME | X(50) | 152 | 50 | Blank |
| TRAN-MERCHANT-CITY | X(50) | 202 | 50 | Blank |
| TRAN-MERCHANT-ZIP | X(10) | 252 | 10 | Blank |
| TRAN-CARD-NUM | X(16) | 262 | 16 | Account's card from the cross-reference |
| TRAN-ORIG-TS | X(26) | 278 | 26 | Run timestamp YYYY-MM-DD-HH.MM.SS.hh0000 |
| TRAN-PROC-TS | X(26) | 304 | 26 | Same as TRAN-ORIG-TS |
| FILLER | X(20) | 330 | 20 | Spaces |

## Assumptions

- Stub CEE3ABD (`spec/runner/stubs/CEE3ABD.cbl`): the Language Environment abend service does not exist under GnuCOBOL; the stub displays the abend code and ends the run with return code 12. It is reached only from 9999-ABEND-PROGRAM (`app/cbl/CBACT04C.cbl:628-632`), which the seeded run never reaches (INTCALC ended with RC 0).
- Stub PARMDRV (`spec/runner/stubs/PARMDRV.cbl`): stands in for the JCL EXEC PARM operand; it reads PARM '2022071800' from the environment and calls CBACT04C with a halfword length and the text, as z/OS would. The tests read the PARM from the JCL, not from the stub.
- The run clock is fixed by the harness at 2022/07/18 01:00:00.00 (`spec/manifest.yaml` run.current_date, passed as COB_CURRENT_DATE); R13 assumes a port takes the same injected clock.
- Not observable in the run (no seeded case reaches them, all end in an abend with RC 12 via CEE3ABD): an account id in the category balances that is missing from the account master (`app/cbl/CBACT04C.cbl:372-391`), an account with no cross-reference record (`app/cbl/CBACT04C.cbl:393-413`), a missing DEFAULT disclosure record (`app/cbl/CBACT04C.cbl:443-460`), and any open, read, write, rewrite or close error.
- Not observable: fees. 1400-COMPUTE-FEES (`app/cbl/CBACT04C.cbl:518-520`) is an empty paragraph ("To be implemented"); no fee transaction is ever written.
- Not observable: accounts with no category balance are never read or rewritten by this program; every seeded account has at least one category balance after POSTTRAN, so R14-R16 cover all of them except the last one (R17).
- R02 versus R03: the rates of group A000000000 and DEFAULT differ only for type 07 category 0001, and no A000000000 account (52, 53, 54) holds a type 07 balance, so on the seeded data the own-group lookup is checked by value but cannot be told apart from the DEFAULT fallback.
- Seeded data: 50 of the 54 accounts have a blank ACCT-GROUP-ID (the value 'A000000000' sits in ACCT-ADDR-ZIP in `app/data/ASCII/acctdata.txt`), so they all take the DEFAULT rates via R03. This is how the copybook reads the shipped data, not a program decision.
- Seeded truncation case: `spec/failure-classes.yaml` names account 53 at 421.37 and 15.00 %, but POSTTRAN posts 78.63 to that category first, so CBACT04C sees 500.00 and the interest is an exact 6.25. Truncation (R06) is still observed on many other records.
- Each amber rule's acceptance test asserts the obvious business reading and is marked `xfail(strict=True)` until the business owner answers.
