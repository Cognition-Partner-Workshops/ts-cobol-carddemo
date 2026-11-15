-- Synthetic rows (no fixture): inserted in an order that is neither seq_no nor acct_id order, with
-- transaction ids that do not sort like seq_no, and a second run whose rows must not be exported.
INSERT INTO carddemo.interest_run (run_id, parm_date, status, written_transaction_count, written_tran_amt_sum, written_interest_not_added_sum)
OVERRIDING SYSTEM VALUE
VALUES (1, '2022071800', 'CALCULATED', 3, 6.00, 3.00), (2, '2022081800', 'EXPORTED', 1, 9.99, 9.99);

INSERT INTO carddemo.account (acct_id, acct_active_status, acct_curr_bal, acct_credit_limit, acct_cash_credit_limit,
    acct_open_date, acct_expiraion_date, acct_reissue_date, acct_curr_cyc_credit, acct_curr_cyc_debit,
    acct_addr_zip, acct_group_id, acct_filler)
VALUES
    (2030, 'Y', 30.00, 5000.00, 2500.00, '2019-03-15', '2026-12-31', '2026-12-31', 0.00, 0.00, '10115', 'GOLD', convert_to(repeat(' ', 178), 'LATIN1')),
    (2010, 'Y', 10.00, 5000.00, 2500.00, '2019-03-15', '2026-12-31', '2026-12-31', 0.00, 0.00, '10115', 'GOLD', convert_to(repeat(' ', 178), 'LATIN1')),
    (2020, 'N', 20.00, 5000.00, 2500.00, '2019-03-15', '2026-12-31', '2026-12-31', 0.00, 0.00, '10115', 'GOLD', convert_to(repeat(' ', 178), 'LATIN1'));

INSERT INTO carddemo.interest_transaction (run_id, seq_no, tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc,
    tran_amt, tran_merchant_id, tran_merchant_name, tran_merchant_city, tran_merchant_zip, tran_card_num,
    tran_orig_ts, tran_proc_ts, tran_filler)
VALUES
    (1, 3, '2022071800000007', '01', 5, 'System', 'Int. for a/c 00000002030', 3.00, 0, '', '', '', '4000000000002030',
     '2022-07-05-10.00.00.000000', '2022-07-05-10.00.00.000000', convert_to(repeat(' ', 20), 'LATIN1')),
    (2, 1, '2022081800000001', '01', 5, 'System', 'Int. for a/c 00000002010', 9.99, 0, '', '', '', '4000000000002010',
     '2022-07-05-10.00.00.000000', '2022-07-05-10.00.00.000000', convert_to(repeat(' ', 20), 'LATIN1')),
    (1, 1, '2022071800000009', '01', 5, 'System', 'Int. for a/c 00000002010', 1.00, 0, '', '', '', '4000000000002010',
     '2022-07-05-10.00.00.000000', '2022-07-05-10.00.00.000000', convert_to(repeat(' ', 20), 'LATIN1')),
    (1, 2, '2022071800000008', '01', 5, 'System', 'Int. for a/c 00000002020', 2.00, 0, '', '', '', '4000000000002020',
     '2022-07-05-10.00.00.000000', '2022-07-05-10.00.00.000000', convert_to(repeat(' ', 20), 'LATIN1'));
