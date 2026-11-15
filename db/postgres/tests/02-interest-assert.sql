-- Assertions for db/postgres/02-interest.sql (ticket INT-T02, shared interface FI-4).
--
-- Run:  psql -v ON_ERROR_STOP=1 -q -At -f db/postgres/tests/02-interest-assert.sql
--       (db/postgres/tests/run.sh does the apply and this script on a fresh database)
-- Every assertion prints one line "PASS <name>".  The first one that fails prints the differing
-- rows as NOTICE lines, raises an exception and psql exits 3 (non-zero).  The script is one
-- transaction that is rolled back: it leaves no row and no object behind.
--
-- The expected values below are written by hand from modernization/design/data-map-interest.md
-- (one line per data-map row, in the order of the data map), never read back from the schema.
-- This directory is not an init directory: the postgres image ignores sub-directories of
-- /docker-entrypoint-initdb.d, so the tests never run at container start.

\set ON_ERROR_STOP 1
BEGIN;
-- User carddemo has schema carddemo on its search path ("$user"); fix the path so that the
-- catalog prints every name of that schema qualified, whoever runs the script.
SET LOCAL search_path = pg_catalog;

-- ---------------------------------------------------------------------------------------------
-- helpers
-- ---------------------------------------------------------------------------------------------
CREATE FUNCTION pg_temp.assert_no_rows(name text, query text) RETURNS text
LANGUAGE plpgsql AS $f$
DECLARE line text; n integer := 0;
BEGIN
    FOR line IN EXECUTE query LOOP
        n := n + 1;
        RAISE NOTICE 'FAIL % : %', name, line;
    END LOOP;
    IF n > 0 THEN
        RAISE EXCEPTION 'FAIL % (% differing rows)', name, n;
    END IF;
    RETURN 'PASS ' || name;
END $f$;

-- The statement must be rejected with exactly this SQLSTATE; it runs in a sub-transaction.
CREATE FUNCTION pg_temp.assert_rejected(name text, expected_state text, statement text) RETURNS text
LANGUAGE plpgsql AS $f$
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = expected_state THEN
            RETURN 'PASS ' || name || ' - rejected, SQLSTATE ' || SQLSTATE || ': ' || SQLERRM;
        END IF;
        RAISE EXCEPTION 'FAIL % : expected SQLSTATE %, got % (%)', name, expected_state, SQLSTATE, SQLERRM;
    END;
    RAISE EXCEPTION 'FAIL % : the statement was accepted', name;
END $f$;

CREATE FUNCTION pg_temp.assert_accepted(name text, statement text) RETURNS text
LANGUAGE plpgsql AS $f$
BEGIN
    EXECUTE statement;
    RETURN 'PASS ' || name || ' - accepted';
END $f$;

-- ---------------------------------------------------------------------------------------------
-- 1. Columns: information_schema.columns equals the data map row for row
--    (name, data type, numeric precision and scale, character length, nullability; also the
--    order of the rows and the collation the data map writes).
-- ---------------------------------------------------------------------------------------------
CREATE TEMP TABLE expected_column
(   table_name text, ordinal_position integer, column_name text, data_type text,
    numeric_precision integer, numeric_scale integer, character_maximum_length integer,
    is_nullable text, collation_name text) ON COMMIT DROP;

INSERT INTO expected_column VALUES
-- CVACT01Y -> carddemo.account
('account',  1, 'acct_id',                'numeric',   11, 0, NULL, 'NO', NULL),
('account',  2, 'acct_active_status',     'character', NULL, NULL, 1,  'NO', NULL),
('account',  3, 'acct_curr_bal',          'numeric',   12, 2, NULL, 'NO', NULL),
('account',  4, 'acct_credit_limit',      'numeric',   12, 2, NULL, 'NO', NULL),
('account',  5, 'acct_cash_credit_limit', 'numeric',   12, 2, NULL, 'NO', NULL),
('account',  6, 'acct_open_date',         'character', NULL, NULL, 10, 'NO', NULL),
('account',  7, 'acct_expiraion_date',    'character', NULL, NULL, 10, 'NO', NULL),
('account',  8, 'acct_reissue_date',      'character', NULL, NULL, 10, 'NO', NULL),
('account',  9, 'acct_curr_cyc_credit',   'numeric',   12, 2, NULL, 'NO', NULL),
('account', 10, 'acct_curr_cyc_debit',    'numeric',   12, 2, NULL, 'NO', NULL),
('account', 11, 'acct_addr_zip',          'character', NULL, NULL, 10, 'NO', NULL),
('account', 12, 'acct_group_id',          'character', NULL, NULL, 10, 'NO', NULL),
('account', 13, 'acct_filler',            'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- CVACT03Y -> carddemo.card_xref
('card_xref', 1, 'xref_card_num', 'character', NULL, NULL, 16, 'NO', NULL),
('card_xref', 2, 'xref_cust_id',  'numeric',   9,  0, NULL, 'NO', NULL),
('card_xref', 3, 'xref_acct_id',  'numeric',   11, 0, NULL, 'NO', NULL),
('card_xref', 4, 'xref_filler',   'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- CVTRA01Y -> carddemo.tran_cat_balance
('tran_cat_balance', 1, 'trancat_acct_id', 'numeric',   11, 0, NULL, 'NO', NULL),
('tran_cat_balance', 2, 'trancat_type_cd', 'character', NULL, NULL, 2, 'NO', 'C'),
('tran_cat_balance', 3, 'trancat_cd',      'numeric',   4,  0, NULL, 'NO', NULL),
('tran_cat_balance', 4, 'tran_cat_bal',    'numeric',   11, 2, NULL, 'NO', NULL),
('tran_cat_balance', 5, 'trancat_filler',  'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- CVTRA02Y -> carddemo.disclosure_group
('disclosure_group', 1, 'dis_acct_group_id', 'character', NULL, NULL, 10, 'NO', 'C'),
('disclosure_group', 2, 'dis_tran_type_cd',  'character', NULL, NULL, 2,  'NO', 'C'),
('disclosure_group', 3, 'dis_tran_cat_cd',   'numeric',   4, 0, NULL, 'NO', NULL),
('disclosure_group', 4, 'dis_int_rate',      'numeric',   6, 2, NULL, 'NO', NULL),
('disclosure_group', 5, 'dis_filler',        'bytea',     NULL, NULL, NULL, 'NO', NULL),
-- CVTRA05Y -> carddemo.interest_transaction, then the two columns with no copybook field
('interest_transaction',  1, 'tran_id',            'character', NULL, NULL, 16,  'NO', NULL),
('interest_transaction',  2, 'tran_type_cd',       'character', NULL, NULL, 2,   'NO', NULL),
('interest_transaction',  3, 'tran_cat_cd',        'numeric',   4, 0, NULL, 'NO', NULL),
('interest_transaction',  4, 'tran_source',        'character', NULL, NULL, 10,  'NO', NULL),
('interest_transaction',  5, 'tran_desc',          'character', NULL, NULL, 100, 'NO', NULL),
('interest_transaction',  6, 'tran_amt',           'numeric',   11, 2, NULL, 'NO', NULL),
('interest_transaction',  7, 'tran_merchant_id',   'numeric',   9, 0, NULL, 'NO', NULL),
('interest_transaction',  8, 'tran_merchant_name', 'character', NULL, NULL, 50,  'NO', NULL),
('interest_transaction',  9, 'tran_merchant_city', 'character', NULL, NULL, 50,  'NO', NULL),
('interest_transaction', 10, 'tran_merchant_zip',  'character', NULL, NULL, 10,  'NO', NULL),
('interest_transaction', 11, 'tran_card_num',      'character', NULL, NULL, 16,  'NO', NULL),
('interest_transaction', 12, 'tran_orig_ts',       'character', NULL, NULL, 26,  'NO', NULL),
('interest_transaction', 13, 'tran_proc_ts',       'character', NULL, NULL, 26,  'NO', NULL),
('interest_transaction', 14, 'tran_filler',        'bytea',     NULL, NULL, NULL, 'NO', NULL),
('interest_transaction', 15, 'run_id',             'bigint',    64, 0, NULL, 'NO', NULL),
('interest_transaction', 16, 'seq_no',             'integer',   32, 0, NULL, 'NO', NULL),
-- carddemo.interest_run: the five columns the data map names ...
('interest_run',  1, 'run_id',      'bigint',                   64, 0, NULL, 'NO', NULL),
('interest_run',  2, 'parm_date',   'character',                NULL, NULL, 10, 'NO', NULL),
('interest_run',  3, 'started_at',  'timestamp with time zone', NULL, NULL, NULL, 'YES', NULL),
('interest_run',  4, 'finished_at', 'timestamp with time zone', NULL, NULL, NULL, 'YES', NULL),
('interest_run',  5, 'status',      'character varying',        NULL, NULL, 12, 'NO', NULL),
-- ... and its row "loaded_* / written_* counts and sums, BIGINT / NUMERIC(18,2)" (RC1-RC6):
-- every count BIGINT, every sum NUMERIC(18,2), nullable.  The names are ticket 02's reading.
('interest_run',  6, 'loaded_account_count',               'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run',  7, 'loaded_card_xref_count',             'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run',  8, 'loaded_tran_cat_balance_count',      'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run',  9, 'loaded_disclosure_group_count',      'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 10, 'loaded_acct_curr_bal_sum',           'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 11, 'loaded_acct_credit_limit_sum',       'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 12, 'loaded_acct_cash_credit_limit_sum',  'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 13, 'loaded_acct_curr_cyc_credit_sum',    'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 14, 'loaded_acct_curr_cyc_debit_sum',     'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 15, 'loaded_tran_cat_bal_sum',            'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 16, 'loaded_dis_int_rate_sum',            'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 17, 'loaded_acct_id_sum',                 'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 18, 'loaded_xref_acct_id_sum',            'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 19, 'loaded_trancat_acct_id_sum',         'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 20, 'loaded_g02_difference_count',        'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 21, 'loaded_g03_difference_count',        'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 22, 'loaded_account_key_count',           'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 23, 'loaded_card_xref_key_count',         'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 24, 'loaded_tran_cat_balance_key_count',  'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 25, 'loaded_disclosure_group_key_count',  'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 26, 'written_transaction_count',          'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 27, 'written_tran_amt_sum',               'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 28, 'written_interest_not_added_sum',     'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 29, 'written_systran_count',              'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 30, 'written_systran_tran_amt_sum',       'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 31, 'written_acctdata_count',             'bigint',  64, 0, NULL, 'YES', NULL),
('interest_run', 32, 'written_acct_curr_bal_sum',          'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 33, 'written_acct_credit_limit_sum',      'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 34, 'written_acct_cash_credit_limit_sum', 'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 35, 'written_acct_curr_cyc_credit_sum',   'numeric', 18, 2, NULL, 'YES', NULL),
('interest_run', 36, 'written_acct_curr_cyc_debit_sum',    'numeric', 18, 2, NULL, 'YES', NULL);

CREATE TEMP VIEW actual_column AS
SELECT table_name::text, ordinal_position::integer, column_name::text, data_type::text,
       numeric_precision::integer, numeric_scale::integer, character_maximum_length::integer,
       is_nullable::text, collation_name::text
  FROM information_schema.columns
 WHERE table_schema = 'carddemo'
   AND table_name NOT IN ('transaction_type', 'transaction_type_categor');

SELECT pg_temp.assert_no_rows('tables: schema carddemo holds the two tables of 01-transaction-type.sql and the six of the data map',
  $q$ SELECT 'unexpected or missing table ' || t
        FROM (SELECT table_name::text AS t FROM information_schema.tables WHERE table_schema = 'carddemo') a
        FULL JOIN (VALUES ('transaction_type'), ('transaction_type_categor'), ('account'), ('card_xref'),
                          ('tran_cat_balance'), ('disclosure_group'), ('interest_transaction'), ('interest_run')) e(t) USING (t)
       WHERE a.t IS NULL OR e.t IS NULL $q$);

SELECT pg_temp.assert_no_rows('columns: ' || t || ' equals the data map row for row (' ||
         (SELECT count(*) FROM expected_column e WHERE e.table_name = t) || ' columns)',
  format($q$ SELECT 'in the data map, not in the database: ' || e::text FROM (SELECT * FROM expected_column WHERE table_name = %1$L EXCEPT SELECT * FROM actual_column) e
             UNION ALL
             SELECT 'in the database, not in the data map: ' || a::text FROM (SELECT * FROM actual_column WHERE table_name = %1$L EXCEPT SELECT * FROM expected_column) a $q$, t))
  FROM unnest(ARRAY['account', 'card_xref', 'tran_cat_balance', 'disclosure_group', 'interest_transaction', 'interest_run']) t;

SELECT pg_temp.assert_no_rows('columns: interest_run.run_id is GENERATED ALWAYS AS IDENTITY',
  $q$ SELECT 'is_identity=' || is_identity || ' identity_generation=' || coalesce(identity_generation, '-')
        FROM information_schema.columns
       WHERE table_schema = 'carddemo' AND table_name = 'interest_run' AND column_name = 'run_id'
         AND (is_identity <> 'YES' OR identity_generation IS DISTINCT FROM 'ALWAYS') $q$);

-- ---------------------------------------------------------------------------------------------
-- 2. Keys, indexes, checks
-- ---------------------------------------------------------------------------------------------
CREATE TEMP VIEW actual_constraint AS
SELECT c.relname::text AS table_name, k.contype::text AS kind, pg_get_constraintdef(k.oid) AS definition
  FROM pg_constraint k
  JOIN pg_class c ON c.oid = k.conrelid
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'carddemo' AND c.relname NOT IN ('transaction_type', 'transaction_type_categor');

CREATE TEMP TABLE expected_constraint (table_name text, kind text, definition text) ON COMMIT DROP;
INSERT INTO expected_constraint VALUES
-- primary keys (data map, "Keys and alternate indexes")
('account',              'p', 'PRIMARY KEY (acct_id)'),
('card_xref',            'p', 'PRIMARY KEY (xref_card_num)'),
('tran_cat_balance',     'p', 'PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd)'),
('disclosure_group',     'p', 'PRIMARY KEY (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd)'),
('interest_transaction', 'p', 'PRIMARY KEY (run_id, seq_no)'),
('interest_run',         'p', 'PRIMARY KEY (run_id)'),
-- unique inside a run only
('interest_transaction', 'u', 'UNIQUE (run_id, tran_id)'),
-- the only foreign key of the six tables
('interest_transaction', 'f', 'FOREIGN KEY (run_id) REFERENCES carddemo.interest_run(run_id)'),
-- CHECKs of the data map, as PostgreSQL 15 prints them
('account',              'c', 'CHECK ((acct_id >= (0)::numeric))'),
('account',              'c', 'CHECK ((octet_length(acct_filler) = 178))'),
('card_xref',            'c', 'CHECK ((xref_cust_id >= (0)::numeric))'),
('card_xref',            'c', 'CHECK ((xref_acct_id >= (0)::numeric))'),
('card_xref',            'c', 'CHECK ((octet_length(xref_filler) = 14))'),
('tran_cat_balance',     'c', 'CHECK ((trancat_acct_id >= (0)::numeric))'),
('tran_cat_balance',     'c', 'CHECK ((trancat_cd >= (0)::numeric))'),
('tran_cat_balance',     'c', 'CHECK ((octet_length(trancat_filler) = 22))'),
('disclosure_group',     'c', 'CHECK ((dis_tran_cat_cd >= (0)::numeric))'),
('disclosure_group',     'c', 'CHECK ((octet_length(dis_filler) = 28))'),
('interest_transaction', 'c', 'CHECK ((tran_cat_cd >= (0)::numeric))'),
('interest_transaction', 'c', 'CHECK ((tran_merchant_id >= (0)::numeric))'),
('interest_transaction', 'c', 'CHECK ((octet_length(tran_filler) = 20))');

SELECT pg_temp.assert_no_rows('keys: the six primary keys of the data map',
  $q$ SELECT 'expected, not found: ' || e::text FROM (SELECT * FROM expected_constraint WHERE kind = 'p' EXCEPT SELECT * FROM actual_constraint) e
      UNION ALL SELECT 'found, not expected: ' || a::text FROM (SELECT * FROM actual_constraint WHERE kind = 'p' EXCEPT SELECT * FROM expected_constraint) a $q$);

SELECT pg_temp.assert_no_rows('keys: interest_transaction (run_id, tran_id) is unique, and no other unique constraint exists',
  $q$ SELECT 'expected, not found: ' || e::text FROM (SELECT * FROM expected_constraint WHERE kind = 'u' EXCEPT SELECT * FROM actual_constraint) e
      UNION ALL SELECT 'found, not expected: ' || a::text FROM (SELECT * FROM actual_constraint WHERE kind = 'u' EXCEPT SELECT * FROM expected_constraint) a $q$);

SELECT pg_temp.assert_no_rows('keys: one foreign key, interest_transaction.run_id -> interest_run; none between the replica tables or to transaction_type (G09)',
  $q$ SELECT 'expected, not found: ' || e::text FROM (SELECT * FROM expected_constraint WHERE kind = 'f' EXCEPT SELECT * FROM actual_constraint) e
      UNION ALL SELECT 'found, not expected: ' || a::text FROM (SELECT * FROM actual_constraint WHERE kind = 'f' EXCEPT SELECT * FROM expected_constraint) a $q$);

SELECT pg_temp.assert_no_rows('checks: the 13 CHECKs of the data map and no other',
  $q$ SELECT 'expected, not found: ' || e::text FROM (SELECT * FROM expected_constraint WHERE kind = 'c' EXCEPT SELECT * FROM actual_constraint) e
      UNION ALL SELECT 'found, not expected: ' || a::text FROM (SELECT * FROM actual_constraint WHERE kind = 'c' EXCEPT SELECT * FROM expected_constraint) a $q$);

SELECT pg_temp.assert_no_rows('index: card_xref_acct_ix exists ON card_xref (xref_acct_id, xref_card_num) and is not unique',
  $q$ SELECT 'card_xref_acct_ix: ' || coalesce(
               (SELECT 'unique=' || i.indisunique || ' ' || pg_get_indexdef(i.indexrelid)
                  FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid JOIN pg_namespace n ON n.oid = c.relnamespace
                 WHERE n.nspname = 'carddemo' AND c.relname = 'card_xref_acct_ix'), 'missing') AS found
       WHERE NOT EXISTS (
               SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'carddemo' AND c.relname = 'card_xref_acct_ix' AND NOT i.indisunique
                  AND pg_get_indexdef(i.indexrelid) = 'CREATE INDEX card_xref_acct_ix ON carddemo.card_xref USING btree (xref_acct_id, xref_card_num)') $q$);

SELECT pg_temp.assert_no_rows('index: the six tables have no index beyond the primary keys, the unique constraint and card_xref_acct_ix',
  $q$ SELECT 'unexpected or missing index ' || coalesce(a.indexname, e.indexname)
        FROM (SELECT indexname::text FROM pg_indexes WHERE schemaname = 'carddemo'
                 AND tablename NOT IN ('transaction_type', 'transaction_type_categor')) a
        FULL JOIN (VALUES ('account_pkey'), ('card_xref_pkey'), ('card_xref_acct_ix'), ('tran_cat_balance_pkey'),
                          ('disclosure_group_pkey'), ('interest_run_pkey'), ('interest_transaction_pkey'),
                          ('interest_transaction_run_id_tran_id_key')) e(indexname) USING (indexname)
       WHERE a.indexname IS NULL OR e.indexname IS NULL $q$);

-- ---------------------------------------------------------------------------------------------
-- 3. Errors: values that do not fit are rejected by the database.  Synthetic data only.
--    Each statement differs from an accepted row in the one value named.
-- ---------------------------------------------------------------------------------------------
CREATE FUNCTION pg_temp.account_row(acct_id text, acct_curr_bal text, filler_length integer) RETURNS text
LANGUAGE sql AS $f$
SELECT format($s$INSERT INTO carddemo.account VALUES (%s, 'Y', %s, 5000.00, 1000.00, '2020-01-01', '2030-01-01', '2025-01-01',
              0.00, 0.00, '00000', 'GOLD', decode(repeat('40', %s), 'hex'))$s$, acct_id, acct_curr_bal, filler_length) $f$;

CREATE FUNCTION pg_temp.rate_row(dis_int_rate text) RETURNS text
LANGUAGE sql AS $f$
SELECT format($s$INSERT INTO carddemo.disclosure_group VALUES ('GOLD', '01', 1, %s, decode(repeat('40', 28), 'hex'))$s$, dis_int_rate) $f$;

CREATE FUNCTION pg_temp.transaction_row(run_id text, seq_no integer, tran_id text, tran_amt text) RETURNS text
LANGUAGE sql AS $f$
SELECT format($s$INSERT INTO carddemo.interest_transaction VALUES (%L, '01', 5, 'System', 'Int. for a/c 00000002001', %s, 0, '', '', '',
              '4000000000002001', '2022-07-05-10.00.00.000000', '2022-07-05-10.00.00.000000', decode(repeat('20', 20), 'hex'), %s, %s)$s$,
              tran_id, tran_amt, run_id, seq_no) $f$;

-- the highest values that fit are accepted
SELECT pg_temp.assert_accepted('accepts: account with acct_id = 99999999999, acct_curr_bal = 9999999999.99 and a 178-byte acct_filler',
  pg_temp.account_row('99999999999', '9999999999.99', 178));
SELECT pg_temp.assert_accepted('accepts: account with acct_id = 0 and acct_curr_bal = -9999999999.99',
  pg_temp.account_row('0', '-9999999999.99', 178));
SELECT pg_temp.assert_accepted('accepts: dis_int_rate = 9999.99', pg_temp.rate_row('9999.99'));
SELECT pg_temp.assert_accepted('accepts: interest_run row with only parm_date and status',
  $s$INSERT INTO carddemo.interest_run (parm_date, status) VALUES ('2022071800', 'LOADED')$s$);
SELECT pg_temp.assert_accepted('accepts: tran_amt = 999999999.99',
  pg_temp.transaction_row($$currval(pg_get_serial_sequence('carddemo.interest_run', 'run_id'))$$, 1, '2022071800000001', '999999999.99'));
SELECT pg_temp.assert_accepted('accepts: two cards for one account (card_xref_acct_ix is not unique)',
  $s$INSERT INTO carddemo.card_xref VALUES ('4000000000002001', 1, 2001, decode(repeat('40', 14), 'hex')),
                                           ('4000000000002002', 1, 2001, decode(repeat('40', 14), 'hex'))$s$);

-- the five error cases of the ticket's Acceptance
SELECT pg_temp.assert_rejected('errors: acct_curr_bal = 10000000000.00', '22003', pg_temp.account_row('2001', '10000000000.00', 178));
SELECT pg_temp.assert_rejected('errors: tran_amt = 1000000000.00', '22003',
  pg_temp.transaction_row($$currval(pg_get_serial_sequence('carddemo.interest_run', 'run_id'))$$, 2, '2022071800000002', '1000000000.00'));
SELECT pg_temp.assert_rejected('errors: dis_int_rate = 10000.00', '22003', pg_temp.rate_row('10000.00'));
SELECT pg_temp.assert_rejected('errors: negative acct_id', '23514', pg_temp.account_row('-1', '0.00', 178));
SELECT pg_temp.assert_rejected('errors: 177-byte acct_filler', '23514', pg_temp.account_row('2001', '0.00', 177));

-- the same rules on the other columns and the keys
SELECT pg_temp.assert_rejected('errors: 179-byte acct_filler', '23514', pg_temp.account_row('2001', '0.00', 179));
SELECT pg_temp.assert_rejected('errors: acct_id of 12 digits', '22003', pg_temp.account_row('100000000000', '0.00', 178));
SELECT pg_temp.assert_rejected('errors: acct_curr_bal NULL', '23502', pg_temp.account_row('2001', 'NULL', 178));
SELECT pg_temp.assert_rejected('errors: duplicate acct_id', '23505', pg_temp.account_row('0', '0.00', 178));
SELECT pg_temp.assert_rejected('errors: tran_amt = -1000000000.00', '22003',
  pg_temp.transaction_row($$currval(pg_get_serial_sequence('carddemo.interest_run', 'run_id'))$$, 2, '2022071800000002', '-1000000000.00'));
SELECT pg_temp.assert_rejected('errors: second row with the same (run_id, tran_id)', '23505',
  pg_temp.transaction_row($$currval(pg_get_serial_sequence('carddemo.interest_run', 'run_id'))$$, 2, '2022071800000001', '1.00'));
SELECT pg_temp.assert_rejected('errors: second row with the same (run_id, seq_no)', '23505',
  pg_temp.transaction_row($$currval(pg_get_serial_sequence('carddemo.interest_run', 'run_id'))$$, 1, '2022071800000002', '1.00'));
SELECT pg_temp.assert_rejected('errors: interest_transaction for a run_id that is not in interest_run', '23503',
  pg_temp.transaction_row('-1', 1, '2022071800000001', '1.00'));
SELECT pg_temp.assert_rejected('errors: interest_run.run_id supplied by the caller (GENERATED ALWAYS)', '428C9',
  $s$INSERT INTO carddemo.interest_run (run_id, parm_date, status) VALUES (1, '2022071800', 'LOADED')$s$);
SELECT pg_temp.assert_rejected('errors: interest_run.status of 13 characters', '22001',
  $s$INSERT INTO carddemo.interest_run (parm_date, status) VALUES ('2022071800', 'ABANDONED-XYZ')$s$);
SELECT pg_temp.assert_rejected('errors: parm_date of 11 characters', '22001',
  $s$INSERT INTO carddemo.interest_run (parm_date, status) VALUES ('20220718001', 'LOADED')$s$);
SELECT pg_temp.assert_rejected('errors: negative xref_cust_id', '23514',
  $s$INSERT INTO carddemo.card_xref VALUES ('4000000000002003', -1, 2001, decode(repeat('40', 14), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: negative xref_acct_id', '23514',
  $s$INSERT INTO carddemo.card_xref VALUES ('4000000000002003', 1, -1, decode(repeat('40', 14), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: 13-byte xref_filler', '23514',
  $s$INSERT INTO carddemo.card_xref VALUES ('4000000000002003', 1, 2001, decode(repeat('40', 13), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: xref_card_num of 17 characters', '22001',
  $s$INSERT INTO carddemo.card_xref VALUES ('40000000000020031', 1, 2001, decode(repeat('40', 14), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: tran_cat_bal = 1000000000.00', '22003',
  $s$INSERT INTO carddemo.tran_cat_balance VALUES (2001, '01', 1, 1000000000.00, decode(repeat('40', 22), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: negative trancat_cd', '23514',
  $s$INSERT INTO carddemo.tran_cat_balance VALUES (2001, '01', -1, 0.00, decode(repeat('40', 22), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: trancat_cd of 5 digits', '22003',
  $s$INSERT INTO carddemo.tran_cat_balance VALUES (2001, '01', 10000, 0.00, decode(repeat('40', 22), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: 21-byte trancat_filler', '23514',
  $s$INSERT INTO carddemo.tran_cat_balance VALUES (2001, '01', 1, 0.00, decode(repeat('40', 21), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: negative dis_tran_cat_cd', '23514',
  $s$INSERT INTO carddemo.disclosure_group VALUES ('GOLD', '01', -1, 0.00, decode(repeat('40', 28), 'hex'))$s$);
SELECT pg_temp.assert_rejected('errors: 27-byte dis_filler', '23514',
  $s$INSERT INTO carddemo.disclosure_group VALUES ('GOLD', '02', 1, 0.00, decode(repeat('40', 27), 'hex'))$s$);

-- key order (E6): the C collation is declared on the column, so a plain ORDER BY on the key is byte order
SELECT pg_temp.assert_accepted('accepts: balances with type codes 1A, A1 and 01 for one account',
  $s$INSERT INTO carddemo.tran_cat_balance VALUES (2001, 'A1', 1, 0.00, decode(repeat('40', 22), 'hex')),
                                                  (2001, '1A', 1, 0.00, decode(repeat('40', 22), 'hex')),
                                                  (2001, 'a1', 1, 0.00, decode(repeat('40', 22), 'hex')),
                                                  (2001, '01', 1, 0.00, decode(repeat('40', 22), 'hex'))$s$);
SELECT pg_temp.assert_no_rows('collation: tran_cat_balance key order is byte order (01, 1A, A1, a1)',
  $q$ SELECT 'order was ' || o FROM (SELECT string_agg(trancat_type_cd, ',' ORDER BY trancat_acct_id, trancat_type_cd, trancat_cd) AS o
                                       FROM carddemo.tran_cat_balance WHERE trancat_acct_id = 2001) x
       WHERE o <> '01,1A,A1,a1' $q$);

ROLLBACK;
\echo 02-interest-assert.sql: all assertions passed
