# CBTRN02C — post the daily transaction file

Job: POSTTRAN (app/jcl/POSTTRAN.jcl), step STEP15, no PARM. Reads: DALYTRAN (daily transactions, CVTRA06Y), XREFFILE (card cross-reference, CVACT03Y), ACCTFILE (account master, CVACT01Y, updated in place), TCATBALF (transaction category balances, CVTRA01Y, updated in place). Writes: TRANFILE (posted transactions, CVTRA05Y), DALYREJS (rejected daily records with a validation trailer), ACCTFILE, TCATBALF.

Each daily record is validated (card, account, credit limit, expiration). A valid record is posted: the transaction file gets a copy with the processing timestamp, the category balance and the account balance are increased by the amount. An invalid record is written to the rejects file with a reason code and description.

## Rules

| Rule | COBOL citation | Acceptance test | Status |
|---|---|---|---|
| R01 Every daily transaction is processed in file order and ends up either posted to the transaction file or written to the rejects file, never both and never dropped (posted + rejected = daily records). | `app/cbl/CBTRN02C.cbl:202-219` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r01_every_daily_record_posted_or_rejected` | green |
| R02 A transaction whose card number is not in the card cross-reference is rejected with reason 100 "INVALID CARD NUMBER FOUND", and no further check is made. | `app/cbl/CBTRN02C.cbl:370-392` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r02_unknown_card_rejected_100` | green |
| R03 A transaction whose card is cross-referenced to an account that is not in the account master is rejected with reason 101 "ACCOUNT RECORD NOT FOUND". | `app/cbl/CBTRN02C.cbl:393-399` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r03_card_without_account_rejected_101` | green |
| R04 A transaction is rejected with reason 102 "OVERLIMIT TRANSACTION" when cycle credits − cycle debits + transaction amount is greater than the account credit limit; the account's current balance is not used, and because posted payments are kept as negative cycle debits, a payment raises the figure checked against the limit. | `app/cbl/CBTRN02C.cbl:403-413` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r04_overlimit_judged_on_balance` | amber (business question pending) |
| R05 The credit limit is inclusive: a transaction that brings the checked figure exactly to the limit is accepted and one cent more is rejected; the figure uses the cycle totals as updated by transactions already posted in the same run, and rejected transactions do not count (account 53: limit 500.00, opening 421.37; 95.00 rejected, 30.00 and 48.63 posted to reach 500.00, 0.01 rejected). | `app/cbl/CBTRN02C.cbl:403-413` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r05_limit_is_inclusive_and_rejects_do_not_count` | green |
| R06 A transaction whose origination date (first 10 characters of the origination timestamp, YYYY-MM-DD) is later than the account expiration date is rejected with reason 103 "TRANSACTION RECEIVED AFTER ACCT EXPIRATION"; a transaction on the expiration date itself is accepted. | `app/cbl/CBTRN02C.cbl:414-420` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r06_after_expiration_rejected_103_same_day_accepted` | green |
| R07 When a transaction fails both the credit-limit check and the expiration check, only the expiration reason 103 is reported, because the expiration check runs last and overwrites the reason. | `app/cbl/CBTRN02C.cbl:403-420` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r07_expiration_reason_wins_over_overlimit` | green |
| R08 A reject record is the 350-byte daily record unchanged, followed by the 4-digit reason code and the 76-character reason description. | `app/cbl/CBTRN02C.cbl:446-451` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r08_reject_record_is_daily_record_plus_reason` | green |
| R09 A posted transaction is keyed by the daily transaction id and copies type, category, source, description, amount, merchant id, name, city and zip, card number and origination timestamp unchanged from the daily record. | `app/cbl/CBTRN02C.cbl:424-436` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r09_posted_record_copies_daily_fields` | green |
| R10 The processing timestamp of a posted transaction is the run date and time in the form YYYY-MM-DD-HH.MM.SS.hh0000 (hundredths of a second followed by four zeros), whatever the daily record carried. | `app/cbl/CBTRN02C.cbl:437-438` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r10_processing_timestamp_is_run_time` | green |
| R11 For each posted transaction the category balance keyed by (account id from the cross-reference, transaction type, transaction category) is increased by the signed amount: new balance = opening balance + sum of posted amounts for that key. | `app/cbl/CBTRN02C.cbl:467-501` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r11_category_balance_adds_posted_amounts` | green |
| R12 When no category balance exists for the key, a new one is created with that key and a balance starting at zero plus the posted amount. | `app/cbl/CBTRN02C.cbl:503-508` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r12_missing_category_balance_created` | green |
| R13 For each posted transaction the account current balance is increased by the signed amount (purchases raise it, payments, credits and refunds lower it): balance after posting = opening balance + sum of posted amounts; INTCALC later adds its interest on top. | `app/cbl/CBTRN02C.cbl:545-554` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r13_account_balance_adds_posted_amounts` | green |
| R14 A rejected transaction posts nothing: it is not on the transaction file and changes no category balance or account balance. | `app/cbl/CBTRN02C.cbl:211-216` | `spec/tests/rules/test_cbtrn02c.py::test_cbtrn02c_r14_rejected_transactions_change_nothing` | green |

## Data dictionary

Offsets are zero-based byte positions. Signed numeric fields are zoned decimal with the sign overpunched in the last byte.

### CVTRA06Y DALYTRAN-RECORD (DALYTRAN, read; 350 bytes)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| DALYTRAN-ID | X(16) | 0 | 16 | Transaction id; becomes the posted transaction key |
| DALYTRAN-TYPE-CD | X(02) | 16 | 2 | Transaction type (01 purchase, 02 payment, 03 credit, 05 refund) |
| DALYTRAN-CAT-CD | 9(04) | 18 | 4 | Transaction category within the type |
| DALYTRAN-SOURCE | X(10) | 22 | 10 | Origin channel (POS TERM, OPERATOR) |
| DALYTRAN-DESC | X(100) | 32 | 100 | Description |
| DALYTRAN-AMT | S9(09)V99 | 132 | 11 | Signed amount; negative for payments, credits and refunds |
| DALYTRAN-MERCHANT-ID | 9(09) | 143 | 9 | Merchant id |
| DALYTRAN-MERCHANT-NAME | X(50) | 152 | 50 | Merchant name |
| DALYTRAN-MERCHANT-CITY | X(50) | 202 | 50 | Merchant city |
| DALYTRAN-MERCHANT-ZIP | X(10) | 252 | 10 | Merchant zip |
| DALYTRAN-CARD-NUM | X(16) | 262 | 16 | Card number, looked up in the cross-reference |
| DALYTRAN-ORIG-TS | X(26) | 278 | 26 | Origination timestamp; first 10 bytes are the date checked against expiry |
| DALYTRAN-PROC-TS | X(26) | 304 | 26 | Processing timestamp; ignored on input, replaced on posting |
| FILLER | X(20) | 330 | 20 | Unused |

### CVTRA05Y TRAN-RECORD (TRANFILE, written; 350 bytes, key TRAN-ID)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| TRAN-ID | X(16) | 0 | 16 | Record key, = DALYTRAN-ID |
| TRAN-TYPE-CD | X(02) | 16 | 2 | = DALYTRAN-TYPE-CD |
| TRAN-CAT-CD | 9(04) | 18 | 4 | = DALYTRAN-CAT-CD |
| TRAN-SOURCE | X(10) | 22 | 10 | = DALYTRAN-SOURCE |
| TRAN-DESC | X(100) | 32 | 100 | = DALYTRAN-DESC |
| TRAN-AMT | S9(09)V99 | 132 | 11 | = DALYTRAN-AMT |
| TRAN-MERCHANT-ID | 9(09) | 143 | 9 | = DALYTRAN-MERCHANT-ID |
| TRAN-MERCHANT-NAME | X(50) | 152 | 50 | = DALYTRAN-MERCHANT-NAME |
| TRAN-MERCHANT-CITY | X(50) | 202 | 50 | = DALYTRAN-MERCHANT-CITY |
| TRAN-MERCHANT-ZIP | X(10) | 252 | 10 | = DALYTRAN-MERCHANT-ZIP |
| TRAN-CARD-NUM | X(16) | 262 | 16 | = DALYTRAN-CARD-NUM |
| TRAN-ORIG-TS | X(26) | 278 | 26 | = DALYTRAN-ORIG-TS |
| TRAN-PROC-TS | X(26) | 304 | 26 | Run timestamp YYYY-MM-DD-HH.MM.SS.hh0000 |
| FILLER | X(20) | 330 | 20 | Unused |

### CVACT03Y CARD-XREF-RECORD (XREFFILE, read by card number; 50 bytes)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| XREF-CARD-NUM | X(16) | 0 | 16 | Record key, card number |
| XREF-CUST-ID | 9(09) | 16 | 9 | Customer id (not used) |
| XREF-ACCT-ID | 9(11) | 25 | 11 | Account the card belongs to |
| FILLER | X(14) | 36 | 14 | Unused |

### CVACT01Y ACCOUNT-RECORD (ACCTFILE, read and rewritten; 300 bytes, key ACCT-ID)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| ACCT-ID | 9(11) | 0 | 11 | Record key |
| ACCT-ACTIVE-STATUS | X(01) | 11 | 1 | Active flag (not checked) |
| ACCT-CURR-BAL | S9(10)V99 | 12 | 12 | Current balance; + posted amount |
| ACCT-CREDIT-LIMIT | S9(10)V99 | 24 | 12 | Credit limit used by the over-limit check |
| ACCT-CASH-CREDIT-LIMIT | S9(10)V99 | 36 | 12 | Cash credit limit (not checked) |
| ACCT-OPEN-DATE | X(10) | 48 | 10 | Open date (not used) |
| ACCT-EXPIRAION-DATE | X(10) | 58 | 10 | Expiration date YYYY-MM-DD, compared as text with the origination date |
| ACCT-REISSUE-DATE | X(10) | 68 | 10 | Reissue date (not used) |
| ACCT-CURR-CYC-CREDIT | S9(10)V99 | 78 | 12 | Cycle-to-date total of posted amounts ≥ 0 |
| ACCT-CURR-CYC-DEBIT | S9(10)V99 | 90 | 12 | Cycle-to-date total of posted amounts < 0 (kept negative) |
| ACCT-ADDR-ZIP | X(10) | 102 | 10 | Zip (not used) |
| ACCT-GROUP-ID | X(10) | 112 | 10 | Disclosure group (used by INTCALC, not here) |
| FILLER | X(178) | 122 | 178 | Unused |

### CVTRA01Y TRAN-CAT-BAL-RECORD (TCATBALF, read, rewritten or created; 50 bytes)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| TRANCAT-ACCT-ID | 9(11) | 0 | 11 | Key part 1, account id from the cross-reference |
| TRANCAT-TYPE-CD | X(02) | 11 | 2 | Key part 2, transaction type |
| TRANCAT-CD | 9(04) | 13 | 4 | Key part 3, transaction category |
| TRAN-CAT-BAL | S9(09)V99 | 17 | 11 | Category balance; + posted amount |
| FILLER | X(22) | 28 | 22 | Unused |

### DALYREJS REJECT-RECORD (DALYREJS, written; 430 bytes, defined in the program at app/cbl/CBTRN02C.cbl:176-182)

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| REJECT-TRAN-DATA | X(350) | 0 | 350 | The daily record as read (CVTRA06Y layout) |
| WS-VALIDATION-FAIL-REASON | 9(04) | 350 | 4 | Reason code 100, 101, 102 or 103 |
| WS-VALIDATION-FAIL-REASON-DESC | X(76) | 354 | 76 | Reason description |

## Assumptions

- Stub CEE3ABD (spec/runner/stubs/CEE3ABD.cbl): the Language Environment abend service called by 9999-ABEND-PROGRAM (app/cbl/CBTRN02C.cbl:707-711) does not exist under GnuCOBOL; the stub displays the abend code and ends the run with return code 12. No abend happened on the seeded data. CBTRN02C takes no PARM, so PARMDRV is not used.
- The run date and time come from `run.current_date` in spec/manifest.yaml (2022/07/18 01:00:00.00), supplied to GnuCOBOL as COB_CURRENT_DATE; R10 reads the same value.
- The account and category balance outputs are read after the whole batch path. INTCALC (CBACT04C) only reads the category balances, so R11, R12 and R14 see CBTRN02C's result directly. INTCALC adds interest to the account balance and resets the cycle credit and debit to zero, so R13 adds the interest records back in, and the cycle totals CBTRN02C leaves (`app/cbl/CBTRN02C.cbl:548-552`) are observable only through the over-limit decisions of R05.
- Not observable in the output datasets and therefore not given a rule: the job return code 4 when at least one transaction is rejected (`app/cbl/CBTRN02C.cbl:227-231`); the processed/rejected count displays; the reason 109 set when the account rewrite fails (`app/cbl/CBTRN02C.cbl:554-559`), which cannot happen because the account was read just before and the reason is never written anywhere; abends on file status errors.
- The posting arithmetic is exact to the cent (the amount has two decimals like every balance). High-order truncation on overflow (ADD without ON SIZE ERROR into S9(09)V99 category balances and S9(10)V99 account balances) is not exercised by the seeded data.
- The active-status flag and the cash credit limit are not checked by the program; cash advances are judged against the overall credit limit only.
- R04: the seeded account 53 opens with cycle credit equal to its balance and no payments, so on the seeded data the cycle-to-date reading and the balance reading reject the same transactions. The amber test therefore also requires a seeded transaction that separates the two readings; until the fixtures carry one, it fails against any implementation.
