-- The tables after a finished run of fixtures/interest/i06-negative-balance, written by hand from the
-- case's input files and the rules (not from any program output):
--   account 00000002008 (GOLD, type 01 category 0001, rate 12.00): -300.00 * 12.00 / 1200 = -3.00 (INT-Q05)
-- It is the only account, so it ends the input and is not updated (INT-Q04): it stays as loaded.
INSERT INTO carddemo.interest_run (run_id, parm_date, status, written_transaction_count, written_tran_amt_sum, written_interest_not_added_sum)
OVERRIDING SYSTEM VALUE
VALUES (1, '2022071800', 'CALCULATED', 1, -3.00, -3.00);

INSERT INTO carddemo.account (acct_id, acct_active_status, acct_curr_bal, acct_credit_limit, acct_cash_credit_limit,
    acct_open_date, acct_expiraion_date, acct_reissue_date, acct_curr_cyc_credit, acct_curr_cyc_debit,
    acct_addr_zip, acct_group_id, acct_filler)
VALUES
    (2008, 'Y', -300.00, 5000.00, 2500.00, '2019-03-15', '2026-12-31', '2026-12-31', 0.00, 300.00, '10115', 'GOLD', convert_to(repeat(' ', 178), 'LATIN1'));

INSERT INTO carddemo.interest_transaction (run_id, seq_no, tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc,
    tran_amt, tran_merchant_id, tran_merchant_name, tran_merchant_city, tran_merchant_zip, tran_card_num,
    tran_orig_ts, tran_proc_ts, tran_filler)
VALUES
    (1, 1, '2022071800000001', '01', 5, 'System', 'Int. for a/c 00000002008', -3.00, 0, '', '', '', '4000000000002008',
     '2022-07-05-10.00.00.000000', '2022-07-05-10.00.00.000000', convert_to(repeat(' ', 20), 'LATIN1'));
