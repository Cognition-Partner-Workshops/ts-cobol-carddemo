-- Assertions for db/postgres/02-interest.sql (ticket 02 / INT-T02).
-- Run: psql -v ON_ERROR_STOP=1 -f db/postgres/tests/02-interest-assertions.sql
-- Exits non-zero on the first failed assertion.  Everything runs in one transaction that is
-- rolled back, so no row is left behind.
--
-- The expected values are written here by hand from modernization/design/data-map-interest.md
-- (standard S7.4); they are not derived from the script under test.
\set QUIET on
\o /dev/null
BEGIN;
-- object names print schema-qualified, whatever the search_path of the session
SET LOCAL search_path = pg_catalog;

CREATE TEMP TABLE expected_column
(   table_name text, ordinal int, column_name text, data_type text,
    numeric_precision int, numeric_scale int, character_length int, is_nullable text, collation_name text
) ON COMMIT DROP;

INSERT INTO expected_column VALUES
-- CVACT01Y -> account
('account',  1, 'acct_id',                'numeric',   11, 0,    NULL, 'NO', NULL),
('account',  2, 'acct_active_status',     'character', NULL, NULL,  1, 'NO', NULL),
('account',  3, 'acct_curr_bal',          'numeric',   12, 2,    NULL, 'NO', NULL),
('account',  4, 'acct_credit_limit',      'numeric',   12, 2,    NULL, 'NO', NULL),
('account',  5, 'acct_cash_credit_limit', 'numeric',   12, 2,    NULL, 'NO', NULL),
('account',  6, 'acct_open_date',         'character', NULL, NULL, 10, 'NO', NULL),
('account',  7, 'acct_expiraion_date',    'character', NULL, NULL, 10, 'NO', NULL),
('account',  8, 'acct_reissue_date',      'character', NULL, NULL, 10, 'NO', NULL),
('account',  9, 'acct_curr_cyc_credit',   'numeric',   12, 2,    NULL, 'NO', NULL),
('account', 10, 'acct_curr_cyc_debit',    'numeric',   12, 2,    NULL, 'NO', NULL),
('account', 11, 'acct_addr_zip',          'character', NULL, NULL, 10, 'NO', NULL),
('account', 12, 'acct_group_id',          'character', NULL, NULL, 10, 'NO', NULL),
('account', 13, 'acct_filler',            'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- CVACT03Y -> card_xref
('card_xref', 1, 'xref_card_num', 'character', NULL, NULL, 16, 'NO', NULL),
('card_xref', 2, 'xref_cust_id',  'numeric',    9, 0,    NULL, 'NO', NULL),
('card_xref', 3, 'xref_acct_id',  'numeric',   11, 0,    NULL, 'NO', NULL),
('card_xref', 4, 'xref_filler',   'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- CVTRA01Y -> tran_cat_balance
('tran_cat_balance', 1, 'trancat_acct_id', 'numeric',   11, 0,    NULL, 'NO', NULL),
('tran_cat_balance', 2, 'trancat_type_cd', 'character', NULL, NULL,  2, 'NO', 'C'),
('tran_cat_balance', 3, 'trancat_cd',      'numeric',    4, 0,    NULL, 'NO', NULL),
('tran_cat_balance', 4, 'tran_cat_bal',    'numeric',   11, 2,    NULL, 'NO', NULL),
('tran_cat_balance', 5, 'trancat_filler',  'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- CVTRA02Y -> disclosure_group
('disclosure_group', 1, 'dis_acct_group_id', 'character', NULL, NULL, 10, 'NO', 'C'),
('disclosure_group', 2, 'dis_tran_type_cd',  'character', NULL, NULL,  2, 'NO', 'C'),
('disclosure_group', 3, 'dis_tran_cat_cd',   'numeric',    4, 0,    NULL, 'NO', NULL),
('disclosure_group', 4, 'dis_int_rate',      'numeric',    6, 2,    NULL, 'NO', NULL),
('disclosure_group', 5, 'dis_filler',        'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- CVTRA05Y -> interest_transaction, with the two columns that have no copybook field
('interest_transaction',  1, 'run_id',             'bigint',    64, 0,    NULL, 'NO', NULL),
('interest_transaction',  2, 'seq_no',             'integer',   32, 0,    NULL, 'NO', NULL),
('interest_transaction',  3, 'tran_id',            'character', NULL, NULL,  16, 'NO', NULL),
('interest_transaction',  4, 'tran_type_cd',       'character', NULL, NULL,   2, 'NO', NULL),
('interest_transaction',  5, 'tran_cat_cd',        'numeric',    4, 0,    NULL, 'NO', NULL),
('interest_transaction',  6, 'tran_source',        'character', NULL, NULL,  10, 'NO', NULL),
('interest_transaction',  7, 'tran_desc',          'character', NULL, NULL, 100, 'NO', NULL),
('interest_transaction',  8, 'tran_amt',           'numeric',   11, 2,    NULL, 'NO', NULL),
('interest_transaction',  9, 'tran_merchant_id',   'numeric',    9, 0,    NULL, 'NO', NULL),
('interest_transaction', 10, 'tran_merchant_name', 'character', NULL, NULL,  50, 'NO', NULL),
('interest_transaction', 11, 'tran_merchant_city', 'character', NULL, NULL,  50, 'NO', NULL),
('interest_transaction', 12, 'tran_merchant_zip',  'character', NULL, NULL,  10, 'NO', NULL),
('interest_transaction', 13, 'tran_card_num',      'character', NULL, NULL,  16, 'NO', NULL),
('interest_transaction', 14, 'tran_orig_ts',       'character', NULL, NULL,  26, 'NO', NULL),
('interest_transaction', 15, 'tran_proc_ts',       'character', NULL, NULL,  26, 'NO', NULL),
('interest_transaction', 16, 'tran_filler',        'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- interest_run (no copybook): the five named columns, then counts BIGINT and sums NUMERIC(18,2)
('interest_run',  1, 'run_id',      'bigint',                   64, 0,    NULL, 'NO',  NULL),
('interest_run',  2, 'parm_date',   'character',                NULL, NULL, 10, 'NO',  NULL),
('interest_run',  3, 'started_at',  'timestamp with time zone', NULL, NULL, NULL, 'YES', NULL),
('interest_run',  4, 'finished_at', 'timestamp with time zone', NULL, NULL, NULL, 'YES', NULL),
('interest_run',  5, 'status',      'character varying',        NULL, NULL, 12, 'NO',  NULL),
('interest_run',  6, 'loaded_account_count',              'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run',  7, 'loaded_card_xref_count',            'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run',  8, 'loaded_tran_cat_balance_count',     'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run',  9, 'loaded_disclosure_group_count',     'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 10, 'loaded_acct_curr_bal_sum',          'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 11, 'loaded_acct_credit_limit_sum',      'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 12, 'loaded_acct_cash_credit_limit_sum', 'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 13, 'loaded_acct_curr_cyc_credit_sum',   'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 14, 'loaded_acct_curr_cyc_debit_sum',    'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 15, 'loaded_tran_cat_bal_sum',           'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 16, 'loaded_dis_int_rate_sum',           'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 17, 'loaded_acct_id_sum',                'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 18, 'loaded_xref_acct_id_sum',           'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 19, 'loaded_trancat_acct_id_sum',        'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 20, 'written_transaction_count',         'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 21, 'written_tran_amt_sum',              'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 22, 'written_account_count',             'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 23, 'written_acct_curr_bal_sum',         'numeric', 18, 2, NULL, 'YES', NULL);

CREATE TEMP VIEW actual_column AS
SELECT table_name::text, ordinal_position::int AS ordinal, column_name::text, data_type::text,
       numeric_precision::int, numeric_scale::int, character_maximum_length::int AS character_length,
       is_nullable::text, collation_name::text
  FROM information_schema.columns
 WHERE table_schema = 'carddemo'
   AND table_name NOT IN ('transaction_type', 'transaction_type_categor');

CREATE FUNCTION pg_temp.check_that(what text, ok boolean) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF ok IS NOT TRUE THEN
        RAISE EXCEPTION 'ASSERTION FAILED: %', what;
    END IF;
    RAISE NOTICE 'ok - %', what;
END $$;

-- Runs a statement that the database must refuse with the given SQLSTATE.
CREATE FUNCTION pg_temp.check_rejected(what text, statement text, expected_state text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = expected_state THEN
            RAISE NOTICE 'ok - % rejected (SQLSTATE %: %)', what, SQLSTATE, SQLERRM;
            RETURN;
        END IF;
        RAISE EXCEPTION 'ASSERTION FAILED: % rejected with SQLSTATE % (%), expected %', what, SQLSTATE, SQLERRM, expected_state;
    END;
    RAISE EXCEPTION 'ASSERTION FAILED: % was accepted', what;
END $$;

-- 1. Tables: schema carddemo holds the two tables of 01-transaction-type.sql and the six of the data map.
SELECT pg_temp.check_that('schema carddemo holds exactly the eight tables',
    (SELECT array_agg(table_name::text ORDER BY table_name) FROM information_schema.tables WHERE table_schema = 'carddemo')
    = ARRAY['account', 'card_xref', 'disclosure_group', 'interest_run', 'interest_transaction',
            'tran_cat_balance', 'transaction_type', 'transaction_type_categor']);

-- 2. Columns: row for row against the data map, both directions.
SELECT pg_temp.check_that('columns: none missing or different from the data map (' ||
    coalesce((SELECT string_agg(table_name || '.' || column_name, ', ') FROM
        (SELECT * FROM expected_column EXCEPT SELECT * FROM actual_column) d), 'none') || ')',
    NOT EXISTS (SELECT * FROM expected_column EXCEPT SELECT * FROM actual_column));
SELECT pg_temp.check_that('columns: none beyond the data map (' ||
    coalesce((SELECT string_agg(table_name || '.' || column_name, ', ') FROM
        (SELECT * FROM actual_column EXCEPT SELECT * FROM expected_column) d), 'none') || ')',
    NOT EXISTS (SELECT * FROM actual_column EXCEPT SELECT * FROM expected_column));
SELECT pg_temp.check_that('columns per table: account 13, card_xref 4, disclosure_group 5, interest_run 23, interest_transaction 16, tran_cat_balance 5',
    (SELECT array_agg(n ORDER BY t) FROM (SELECT table_name t, count(*)::int n FROM actual_column GROUP BY 1) c)
    = ARRAY[13, 4, 5, 23, 16, 5]);
SELECT pg_temp.check_that('interest_run.run_id is GENERATED ALWAYS AS IDENTITY',
    (SELECT is_identity = 'YES' AND identity_generation = 'ALWAYS' FROM information_schema.columns
      WHERE table_schema = 'carddemo' AND table_name = 'interest_run' AND column_name = 'run_id'));

-- 3. Primary keys, in key order.
CREATE TEMP VIEW key_columns AS
SELECT c.conrelid::regclass::text AS table_name, c.contype::text, c.conname::text,
       (SELECT string_agg(a.attname, ', ' ORDER BY k.ord)
          FROM unnest(c.conkey) WITH ORDINALITY k(attnum, ord)
          JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum) AS columns
  FROM pg_constraint c
 WHERE c.connamespace = 'carddemo'::regnamespace;

SELECT pg_temp.check_that('primary key ' || t || ' (' || k || ')',
    (SELECT columns FROM key_columns WHERE table_name = 'carddemo.' || t AND contype = 'p') = k)
  FROM (VALUES ('account', 'acct_id'),
               ('card_xref', 'xref_card_num'),
               ('tran_cat_balance', 'trancat_acct_id, trancat_type_cd, trancat_cd'),
               ('disclosure_group', 'dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd'),
               ('interest_transaction', 'run_id, seq_no'),
               ('interest_run', 'run_id')) AS e(t, k);

-- 4. Index card_xref_acct_ix exists, is not unique, on (xref_acct_id, xref_card_num); (run_id, tran_id) is unique.
SELECT pg_temp.check_that('card_xref_acct_ix on (xref_acct_id, xref_card_num), not unique',
    (SELECT NOT i.indisunique AND pg_get_indexdef(i.indexrelid) =
            'CREATE INDEX card_xref_acct_ix ON carddemo.card_xref USING btree (xref_acct_id, xref_card_num)'
       FROM pg_index i WHERE i.indexrelid = to_regclass('carddemo.card_xref_acct_ix')));
SELECT pg_temp.check_that('interest_transaction (run_id, tran_id) is unique',
    (SELECT count(*) FROM key_columns WHERE table_name = 'carddemo.interest_transaction' AND contype = 'u'
        AND columns = 'run_id, tran_id') = 1);

-- 5. Foreign keys: only interest_transaction.run_id -> interest_run; none between the replica tables, none to transaction_type (G09).
SELECT pg_temp.check_that('the only foreign key is interest_transaction(run_id) -> interest_run(run_id)',
    (SELECT array_agg(conrelid::regclass::text || '(' ||
                (SELECT string_agg(attname, ', ') FROM pg_attribute WHERE attrelid = conrelid AND attnum = ANY (conkey)) ||
                ') -> ' || confrelid::regclass::text || '(' ||
                (SELECT string_agg(attname, ', ') FROM pg_attribute WHERE attrelid = confrelid AND attnum = ANY (confkey)) || ')')
       FROM pg_constraint
      WHERE contype = 'f' AND connamespace = 'carddemo'::regnamespace
        AND conrelid <> 'carddemo.transaction_type_categor'::regclass)
    = ARRAY['carddemo.interest_transaction(run_id) -> carddemo.interest_run(run_id)']);

-- 6. CHECK constraints: exactly the 13 of the data map (8 unsigned ids and codes, 5 filler lengths).
SELECT pg_temp.check_that('check ' || t || ': ' || d,
    EXISTS (SELECT 1 FROM pg_constraint WHERE contype = 'c' AND conrelid = ('carddemo.' || t)::regclass
               AND pg_get_constraintdef(oid) = d))
  FROM (VALUES ('account', 'CHECK ((acct_id >= (0)::numeric))'),
               ('account', 'CHECK ((octet_length(acct_filler) = 178))'),
               ('card_xref', 'CHECK ((xref_cust_id >= (0)::numeric))'),
               ('card_xref', 'CHECK ((xref_acct_id >= (0)::numeric))'),
               ('card_xref', 'CHECK ((octet_length(xref_filler) = 14))'),
               ('tran_cat_balance', 'CHECK ((trancat_acct_id >= (0)::numeric))'),
               ('tran_cat_balance', 'CHECK ((trancat_cd >= (0)::numeric))'),
               ('tran_cat_balance', 'CHECK ((octet_length(trancat_filler) = 22))'),
               ('disclosure_group', 'CHECK ((dis_tran_cat_cd >= (0)::numeric))'),
               ('disclosure_group', 'CHECK ((octet_length(dis_filler) = 28))'),
               ('interest_transaction', 'CHECK ((tran_cat_cd >= (0)::numeric))'),
               ('interest_transaction', 'CHECK ((tran_merchant_id >= (0)::numeric))'),
               ('interest_transaction', 'CHECK ((octet_length(tran_filler) = 20))')) AS e(t, d);
SELECT pg_temp.check_that('13 check constraints in the six tables',
    (SELECT count(*) FROM pg_constraint WHERE contype = 'c' AND connamespace = 'carddemo'::regnamespace) = 13);

-- 7. Rows the tables must accept: the largest and smallest values the PICs hold.  Synthetic data.
INSERT INTO carddemo.account VALUES
    (99999999999, 'Y', 9999999999.99, -9999999999.99, 0, '2020-01-01', '2030-01-01', '2025-01-01', 0, 0, '00000', 'A000000000',
     convert_to(repeat(' ', 178), 'LATIN1')),
    (0, 'N', 0, 0, 0, '', '', '', 0, 0, '', '', decode(repeat('00', 178), 'hex'));
INSERT INTO carddemo.card_xref VALUES
    ('9000000000002001', 999999999, 99999999999, convert_to(repeat(' ', 14), 'LATIN1')),
    ('9000000000002002', 0, 99999999999, convert_to(repeat(' ', 14), 'LATIN1'));
INSERT INTO carddemo.tran_cat_balance VALUES
    (99999999999, '01', 9999, 999999999.99, convert_to(repeat(' ', 22), 'LATIN1')),
    (99999999999, 'A1', 1, -999999999.99, convert_to(repeat(' ', 22), 'LATIN1')),
    (99999999999, '1A', 1, 0, convert_to(repeat(' ', 22), 'LATIN1'));
INSERT INTO carddemo.disclosure_group VALUES
    ('DEFAULT', '01', 1, 9999.99, convert_to(repeat(' ', 28), 'LATIN1')),
    ('A000000000', '01', 1, -9999.99, convert_to(repeat(' ', 28), 'LATIN1'));
INSERT INTO carddemo.interest_run (parm_date, status) VALUES ('2022071800', 'CALCULATED');
INSERT INTO carddemo.interest_transaction
    SELECT run_id, 1, '2022071800000001', '01', 5, 'System', 'Int. for a/c 99999999999', 999999999.99, 0, '', '', '',
           '9000000000002001', '2022-07-05-10.00.00.000000', '2022-07-05-10.00.00.000000', convert_to(repeat(' ', 20), 'LATIN1')
      FROM carddemo.interest_run;
SELECT pg_temp.check_that('boundary rows accepted: 2 accounts, 2 cards of one account, 3 balances, 2 rates, 1 run, 1 transaction',
    ARRAY[(SELECT count(*) FROM carddemo.account), (SELECT count(*) FROM carddemo.card_xref),
          (SELECT count(*) FROM carddemo.tran_cat_balance), (SELECT count(*) FROM carddemo.disclosure_group),
          (SELECT count(*) FROM carddemo.interest_run), (SELECT count(*) FROM carddemo.interest_transaction)]
    = ARRAY[2, 2, 3, 2, 1, 1]::bigint[]);

-- CHAR(n) keeps the width (E3): DEFAULT is found as CHAR(10), and the stored value is 10 bytes.
SELECT pg_temp.check_that('dis_acct_group_id DEFAULT compares as CHAR(10) and is stored padded',
    (SELECT octet_length(dis_acct_group_id) = 10 FROM carddemo.disclosure_group
      WHERE dis_acct_group_id = 'DEFAULT   ' AND dis_acct_group_id = 'DEFAULT'));
-- FILLER is raw bytes (E4): X'00' survives.
SELECT pg_temp.check_that('acct_filler keeps 178 raw bytes, X''00'' included',
    (SELECT acct_filler = decode(repeat('00', 178), 'hex') FROM carddemo.account WHERE acct_id = 0));
-- Key order (E6): the C collation sorts digits before letters, byte by byte.
SELECT pg_temp.check_that('tran_cat_balance in key order reads 01, 1A, A1 (COLLATE "C")',
    (SELECT array_agg(trancat_type_cd::text ORDER BY trancat_acct_id, trancat_type_cd, trancat_cd) FROM carddemo.tran_cat_balance)
    = ARRAY['01', '1A', 'A1']);

-- 8. Rows the database must reject (Acceptance, Errors).  22003 numeric_value_out_of_range, 23514 check_violation.
SELECT pg_temp.check_rejected('acct_curr_bal = 10000000000.00',
    $$UPDATE carddemo.account SET acct_curr_bal = 10000000000.00 WHERE acct_id = 0$$, '22003');
SELECT pg_temp.check_rejected('tran_amt = 1000000000.00',
    $$UPDATE carddemo.interest_transaction SET tran_amt = 1000000000.00$$, '22003');
SELECT pg_temp.check_rejected('dis_int_rate = 10000.00',
    $$UPDATE carddemo.disclosure_group SET dis_int_rate = 10000.00 WHERE dis_acct_group_id = 'DEFAULT'$$, '22003');
SELECT pg_temp.check_rejected('a negative acct_id',
    $$UPDATE carddemo.account SET acct_id = -1 WHERE acct_id = 0$$, '23514');
SELECT pg_temp.check_rejected('a 177-byte acct_filler',
    $$UPDATE carddemo.account SET acct_filler = convert_to(repeat(' ', 177), 'LATIN1') WHERE acct_id = 0$$, '23514');
-- the same kinds of limits on the other tables
SELECT pg_temp.check_rejected('a 179-byte acct_filler',
    $$UPDATE carddemo.account SET acct_filler = convert_to(repeat(' ', 179), 'LATIN1') WHERE acct_id = 0$$, '23514');
SELECT pg_temp.check_rejected('acct_id of 12 digits',
    $$UPDATE carddemo.account SET acct_id = 100000000000 WHERE acct_id = 0$$, '22003');
SELECT pg_temp.check_rejected('tran_cat_bal = 1000000000.00',
    $$UPDATE carddemo.tran_cat_balance SET tran_cat_bal = 1000000000.00$$, '22003');
SELECT pg_temp.check_rejected('a negative trancat_cd',
    $$UPDATE carddemo.tran_cat_balance SET trancat_cd = -1 WHERE trancat_type_cd = '01'$$, '23514');
SELECT pg_temp.check_rejected('a negative xref_cust_id',
    $$UPDATE carddemo.card_xref SET xref_cust_id = -1$$, '23514');
SELECT pg_temp.check_rejected('a 17-character tran_id',
    $$UPDATE carddemo.interest_transaction SET tran_id = '20220718000000012'$$, '22001');
SELECT pg_temp.check_rejected('a NULL acct_group_id',
    $$UPDATE carddemo.account SET acct_group_id = NULL WHERE acct_id = 0$$, '23502');
SELECT pg_temp.check_rejected('a second account with the same acct_id',
    $$INSERT INTO carddemo.account SELECT * FROM carddemo.account WHERE acct_id = 0$$, '23505');
SELECT pg_temp.check_rejected('the same tran_id twice in one run',
    $$INSERT INTO carddemo.interest_transaction
      SELECT run_id, 2, tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt, tran_merchant_id, tran_merchant_name,
             tran_merchant_city, tran_merchant_zip, tran_card_num, tran_orig_ts, tran_proc_ts, tran_filler
        FROM carddemo.interest_transaction$$, '23505');
SELECT pg_temp.check_rejected('a transaction of a run that does not exist',
    $$INSERT INTO carddemo.interest_transaction
      SELECT run_id + 1, 1, tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt, tran_merchant_id, tran_merchant_name,
             tran_merchant_city, tran_merchant_zip, tran_card_num, tran_orig_ts, tran_proc_ts, tran_filler
        FROM carddemo.interest_transaction$$, '23503');
SELECT pg_temp.check_rejected('an explicit run_id (GENERATED ALWAYS)',
    $$INSERT INTO carddemo.interest_run (run_id, parm_date, status) VALUES (1, '2022071800', 'LOADED')$$, '428C9');
SELECT pg_temp.check_rejected('a 13-character status',
    $$UPDATE carddemo.interest_run SET status = 'CALCULATED123'$$, '22001');

ROLLBACK;
\o
\echo 02-interest-assertions: all assertions passed
