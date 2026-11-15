-- Assertions on the schema of db/postgres/02-interest.sql (ticket INT-T02, FI-4).
-- Expected values are written by hand from modernization/design/data-map-interest.md, never
-- read back from the schema under test. Run through db/postgres/tests/run.sh, or by hand:
--     psql -v ON_ERROR_STOP=1 -f db/postgres/tests/02-interest-assertions.sql
-- Prints one line per assertion and ends with an error (psql exit code 3) if any failed.
-- Everything runs in one transaction that is rolled back: no row and no object is left.

\set QUIET on
\pset pager off
BEGIN;
SET LOCAL client_min_messages = warning;
-- Names print schema-qualified whatever the role's search_path is (a role named carddemo has
-- schema carddemo on its path).
SET LOCAL search_path = pg_catalog;

CREATE TEMP TABLE assertion (seq SERIAL, name TEXT NOT NULL, ok BOOLEAN NOT NULL, detail TEXT) ON COMMIT DROP;

-- ---------------------------------------------------------------------------------------
-- 1. Columns: name, order, data type, precision, scale, length, nullability, collation.
--    One line per data-map row; kind is the type as information_schema names it.
-- ---------------------------------------------------------------------------------------
CREATE TEMP TABLE expected_column
(   table_name TEXT, ordinal INTEGER, column_name TEXT, data_type TEXT,
    num_precision INTEGER, num_scale INTEGER, char_length INTEGER, is_nullable TEXT, collation_name TEXT
) ON COMMIT DROP;

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
-- carddemo.interest_run (no copybook). The data map names run_id, parm_date and status and
-- gives started_at / finished_at and the loaded_* / written_* figures as a pattern.
('interest_run',  1, 'run_id',      'bigint',                   64, 0, NULL, 'NO',  NULL),
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
SELECT table_name::text, ordinal_position::integer AS ordinal, column_name::text, data_type::text,
       numeric_precision::integer AS num_precision, numeric_scale::integer AS num_scale,
       character_maximum_length::integer AS char_length, is_nullable::text, collation_name::text
FROM information_schema.columns
WHERE table_schema = 'carddemo'
  AND table_name NOT IN ('transaction_type', 'transaction_type_categor');

INSERT INTO assertion (name, ok, detail)
SELECT 'tables: carddemo holds the two of 01-transaction-type.sql and the six of the data map',
       string_agg(table_name, ',' ORDER BY table_name)
         = 'account,card_xref,disclosure_group,interest_run,interest_transaction,tran_cat_balance,transaction_type,transaction_type_categor',
       string_agg(table_name, ',' ORDER BY table_name)
FROM information_schema.tables WHERE table_schema = 'carddemo';

INSERT INTO assertion (name, ok, detail)
SELECT 'columns: ' || t.table_name || ' equals the data map row for row ('
         || (SELECT count(*) FROM expected_column e WHERE e.table_name = t.table_name) || ' columns)',
       NOT EXISTS (SELECT 1 FROM (SELECT * FROM expected_column e WHERE e.table_name = t.table_name
                                  EXCEPT SELECT * FROM actual_column) missing)
         AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM actual_column a WHERE a.table_name = t.table_name
                                        EXCEPT SELECT * FROM expected_column) extra),
       (SELECT string_agg(d, '; ') FROM (
            SELECT 'expected ' || m::text AS d FROM (SELECT * FROM expected_column e WHERE e.table_name = t.table_name
                                                     EXCEPT SELECT * FROM actual_column) m
            UNION ALL
            SELECT 'actual ' || x::text FROM (SELECT * FROM actual_column a WHERE a.table_name = t.table_name
                                              EXCEPT SELECT * FROM expected_column) x) diff)
FROM (VALUES ('account'), ('card_xref'), ('tran_cat_balance'), ('disclosure_group'),
             ('interest_transaction'), ('interest_run')) AS t(table_name);

INSERT INTO assertion (name, ok, detail)
SELECT 'columns: interest_run.run_id is GENERATED ALWAYS AS IDENTITY',
       is_identity = 'YES' AND identity_generation = 'ALWAYS', is_identity || ' ' || coalesce(identity_generation, '-')
FROM information_schema.columns
WHERE table_schema = 'carddemo' AND table_name = 'interest_run' AND column_name = 'run_id';

INSERT INTO assertion (name, ok, detail)
SELECT 'columns: no column of the six tables has a default', count(*) = 0, string_agg(table_name || '.' || column_name, ',')
FROM information_schema.columns
WHERE table_schema = 'carddemo' AND table_name NOT IN ('transaction_type', 'transaction_type_categor')
  AND column_default IS NOT NULL;

-- ---------------------------------------------------------------------------------------
-- 2. Keys, index, CHECKs and foreign keys.
-- ---------------------------------------------------------------------------------------
CREATE TEMP VIEW actual_key AS
SELECT c.conrelid::regclass::text AS table_name, c.contype::text AS kind,
       (SELECT string_agg(a.attname, ', ' ORDER BY k.ord)
        FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum) AS columns
FROM pg_constraint c
WHERE c.connamespace = 'carddemo'::regnamespace AND c.contype IN ('p', 'u')
  AND c.conrelid NOT IN ('carddemo.transaction_type'::regclass, 'carddemo.transaction_type_categor'::regclass);

INSERT INTO assertion (name, ok, detail)
SELECT 'keys: primary key of ' || e.table_name || ' is (' || e.columns || ')',
       EXISTS (SELECT 1 FROM actual_key a WHERE a.table_name = 'carddemo.' || e.table_name AND a.kind = 'p' AND a.columns = e.columns),
       (SELECT string_agg(a.columns, ' | ') FROM actual_key a WHERE a.table_name = 'carddemo.' || e.table_name AND a.kind = 'p')
FROM (VALUES ('account', 'acct_id'),
             ('card_xref', 'xref_card_num'),
             ('tran_cat_balance', 'trancat_acct_id, trancat_type_cd, trancat_cd'),
             ('disclosure_group', 'dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd'),
             ('interest_transaction', 'run_id, seq_no'),
             ('interest_run', 'run_id')) AS e(table_name, columns);

INSERT INTO assertion (name, ok, detail)
SELECT 'keys: the only UNIQUE constraint is interest_transaction (run_id, tran_id)',
       coalesce(string_agg(table_name || ' (' || columns || ')', ' | '), '') = 'carddemo.interest_transaction (run_id, tran_id)',
       string_agg(table_name || ' (' || columns || ')', ' | ')
FROM actual_key WHERE kind = 'u';

INSERT INTO assertion (name, ok, detail)
SELECT 'index: card_xref_acct_ix ON card_xref (xref_acct_id, xref_card_num) exists and is not unique',
       count(*) = 1 AND bool_and(NOT i.indisunique) AND bool_and(pg_get_indexdef(i.indexrelid)
         = 'CREATE INDEX card_xref_acct_ix ON carddemo.card_xref USING btree (xref_acct_id, xref_card_num)'),
       string_agg(pg_get_indexdef(i.indexrelid), ' | ')
FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
WHERE c.relnamespace = 'carddemo'::regnamespace AND c.relname = 'card_xref_acct_ix';

INSERT INTO assertion (name, ok, detail)
SELECT 'index: no index beyond the six primary keys, the unique constraint and card_xref_acct_ix',
       count(*) = 8, count(*)::text || ' indexes'
FROM pg_index i JOIN pg_class t ON t.oid = i.indrelid
WHERE t.relnamespace = 'carddemo'::regnamespace AND t.relname NOT IN ('transaction_type', 'transaction_type_categor');

INSERT INTO assertion (name, ok, detail)
SELECT 'checks: the 13 CHECKs of the data map and no other',
       coalesce(string_agg(c.conrelid::regclass::text || ': ' || pg_get_constraintdef(c.oid), E'\n' ORDER BY c.conrelid::regclass::text, pg_get_constraintdef(c.oid)), '') =
       concat_ws(E'\n',
         'carddemo.account: CHECK ((acct_id >= (0)::numeric))',
         'carddemo.account: CHECK ((octet_length(acct_filler) = 178))',
         'carddemo.card_xref: CHECK ((octet_length(xref_filler) = 14))',
         'carddemo.card_xref: CHECK ((xref_acct_id >= (0)::numeric))',
         'carddemo.card_xref: CHECK ((xref_cust_id >= (0)::numeric))',
         'carddemo.disclosure_group: CHECK ((dis_tran_cat_cd >= (0)::numeric))',
         'carddemo.disclosure_group: CHECK ((octet_length(dis_filler) = 28))',
         'carddemo.interest_transaction: CHECK ((octet_length(tran_filler) = 20))',
         'carddemo.interest_transaction: CHECK ((tran_cat_cd >= (0)::numeric))',
         'carddemo.interest_transaction: CHECK ((tran_merchant_id >= (0)::numeric))',
         'carddemo.tran_cat_balance: CHECK ((octet_length(trancat_filler) = 22))',
         'carddemo.tran_cat_balance: CHECK ((trancat_acct_id >= (0)::numeric))',
         'carddemo.tran_cat_balance: CHECK ((trancat_cd >= (0)::numeric))'),
       string_agg(c.conrelid::regclass::text || ': ' || pg_get_constraintdef(c.oid), ' | ' ORDER BY c.conrelid::regclass::text, pg_get_constraintdef(c.oid))
FROM pg_constraint c
WHERE c.connamespace = 'carddemo'::regnamespace AND c.contype = 'c';

INSERT INTO assertion (name, ok, detail)
SELECT 'foreign keys: only interest_transaction.run_id -> interest_run; none between the replica tables or to transaction_type (G09)',
       coalesce(string_agg(c.conrelid::regclass::text || ': ' || pg_get_constraintdef(c.oid), ' | '), '')
         = 'carddemo.interest_transaction: FOREIGN KEY (run_id) REFERENCES carddemo.interest_run(run_id)',
       string_agg(c.conrelid::regclass::text || ': ' || pg_get_constraintdef(c.oid), ' | ')
FROM pg_constraint c
WHERE c.connamespace = 'carddemo'::regnamespace AND c.contype = 'f'
  AND c.conrelid <> 'carddemo.transaction_type_categor'::regclass;

-- ---------------------------------------------------------------------------------------
-- 3. Errors: values the database must reject, and the neighbouring values it must accept.
--    Synthetic data only (S8.5). Each statement runs in its own subtransaction.
-- ---------------------------------------------------------------------------------------
CREATE FUNCTION pg_temp.outcome(statement TEXT) RETURNS TEXT LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE statement;
    RETURN 'accepted';
EXCEPTION WHEN OTHERS THEN
    RETURN SQLSTATE;
END $$;

CREATE FUNCTION pg_temp.account_insert(acct_id TEXT, acct_curr_bal TEXT, filler_length INTEGER) RETURNS TEXT LANGUAGE sql AS $$
    SELECT format('INSERT INTO carddemo.account VALUES (%s, ''Y'', %s, 0, 0, ''2020-01-01'', ''2030-01-01'', ''2025-01-01'', 0, 0, ''00000'', ''A000000000'', decode(repeat(''40'', %s), ''hex''))',
                  acct_id, acct_curr_bal, filler_length)
$$;

INSERT INTO carddemo.interest_run (parm_date, status) VALUES ('2022071800', 'LOADED');

CREATE FUNCTION pg_temp.transaction_insert(seq_no INTEGER, tran_id TEXT, tran_amt TEXT) RETURNS TEXT LANGUAGE sql AS $$
    SELECT format('INSERT INTO carddemo.interest_transaction VALUES (%L, ''01'', 5, ''System'', ''Int. for a/c 00000000001'', %s, 0, '''', '''', '''', ''0000000000000001'', ''2022-07-05-10.00.00.000000'', ''2022-07-05-10.00.00.000000'', decode(repeat(''20'', 20), ''hex''), (SELECT max(run_id) FROM carddemo.interest_run), %s)',
                  tran_id, tran_amt, seq_no)
$$;

-- 22003 numeric_value_out_of_range, 23514 check_violation, 23505 unique_violation,
-- 23502 not_null_violation, 23503 foreign_key_violation, 428C9 generated_always
INSERT INTO assertion (name, ok, detail)
SELECT e.name, pg_temp.outcome(e.statement) = e.expected, 'expected ' || e.expected || ', got ' || pg_temp.outcome(e.statement)
FROM (VALUES
  (1,  'accepts: account with acct_curr_bal = 9999999999.99 (largest S9(10)V99) and a 178-byte filler',
       pg_temp.account_insert('1', '9999999999.99', 178), 'accepted'),
  (2,  'accepts: account with acct_curr_bal = -9999999999.99 and acct_id = 0',
       pg_temp.account_insert('0', '-9999999999.99', 178), 'accepted'),
  (3,  'rejects: acct_curr_bal = 10000000000.00',
       pg_temp.account_insert('2', '10000000000.00', 178), '22003'),
  (4,  'rejects: a negative acct_id',
       pg_temp.account_insert('-1', '0', 178), '23514'),
  (5,  'rejects: a 177-byte acct_filler',
       pg_temp.account_insert('3', '0', 177), '23514'),
  (6,  'rejects: a 179-byte acct_filler',
       pg_temp.account_insert('4', '0', 179), '23514'),
  (7,  'rejects: acct_id = 100000000000 (12 digits)',
       pg_temp.account_insert('100000000000', '0', 178), '22003'),
  (8,  'rejects: a second account with acct_id = 1 (primary key)',
       pg_temp.account_insert('1', '0', 178), '23505'),
  (9,  'rejects: a NULL acct_curr_bal',
       pg_temp.account_insert('5', 'NULL', 178), '23502'),
  (10, 'accepts: disclosure_group with dis_int_rate = 9999.99 (largest S9(04)V99)',
       $$INSERT INTO carddemo.disclosure_group VALUES ('DEFAULT', '01', 1, 9999.99, decode(repeat('40', 28), 'hex'))$$, 'accepted'),
  (11, 'rejects: dis_int_rate = 10000.00',
       $$INSERT INTO carddemo.disclosure_group VALUES ('DEFAULT', '01', 2, 10000.00, decode(repeat('40', 28), 'hex'))$$, '22003'),
  (12, 'rejects: a second disclosure_group row with the key (DEFAULT, 01, 0001), group padded to CHAR(10)',
       $$INSERT INTO carddemo.disclosure_group VALUES ('DEFAULT   ', '01', 1, 0, decode(repeat('40', 28), 'hex'))$$, '23505'),
  (13, 'accepts: interest_transaction with tran_amt = 999999999.99 (largest S9(09)V99)',
       pg_temp.transaction_insert(1, '2022071800000001', '999999999.99'), 'accepted'),
  (14, 'rejects: tran_amt = 1000000000.00',
       pg_temp.transaction_insert(2, '2022071800000002', '1000000000.00'), '22003'),
  (15, 'rejects: a second row with the same (run_id, tran_id)',
       pg_temp.transaction_insert(2, '2022071800000001', '0'), '23505'),
  (16, 'rejects: a second row with the same (run_id, seq_no)',
       pg_temp.transaction_insert(1, '2022071800000002', '0'), '23505'),
  (17, 'rejects: interest_transaction for a run_id that is not in interest_run',
       $$INSERT INTO carddemo.interest_transaction SELECT tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt, tran_merchant_id, tran_merchant_name, tran_merchant_city, tran_merchant_zip, tran_card_num, tran_orig_ts, tran_proc_ts, tran_filler, run_id + 1000, seq_no FROM carddemo.interest_transaction$$, '23503'),
  (18, 'rejects: an explicit run_id in interest_run (GENERATED ALWAYS)',
       $$INSERT INTO carddemo.interest_run (run_id, parm_date, status) VALUES (1, '2022071800', 'LOADED')$$, '428C9'),
  (19, 'accepts: two cards for one account in card_xref (card_xref_acct_ix is not unique, G08)',
       $$INSERT INTO carddemo.card_xref VALUES ('0000000000000001', 1, 1, decode(repeat('40', 14), 'hex')), ('0000000000000002', 1, 1, decode(repeat('40', 14), 'hex'))$$, 'accepted'),
  (20, 'rejects: a negative xref_acct_id',
       $$INSERT INTO carddemo.card_xref VALUES ('0000000000000003', 1, -1, decode(repeat('40', 14), 'hex'))$$, '23514'),
  (21, 'rejects: tran_cat_bal = 1000000000.00',
       $$INSERT INTO carddemo.tran_cat_balance VALUES (1, '01', 1, 1000000000.00, decode(repeat('40', 22), 'hex'))$$, '22003'),
  (22, 'rejects: trancat_cd = 10000 (five digits)',
       $$INSERT INTO carddemo.tran_cat_balance VALUES (1, '01', 10000, 0, decode(repeat('40', 22), 'hex'))$$, '22003')
) AS e(ord, name, statement, expected)
ORDER BY e.ord;

-- E6: the key order of tran_cat_balance is the byte order of the C collation (digits before
-- upper-case letters before lower-case letters), whatever the collation of the database.
INSERT INTO carddemo.tran_cat_balance
SELECT 7, code, 1, 0, decode(repeat('40', 22), 'hex') FROM (VALUES ('a1'), ('01'), ('A1'), ('1A'), ('B0')) AS t(code);

INSERT INTO assertion (name, ok, detail)
SELECT 'order: ORDER BY trancat_acct_id, trancat_type_cd, trancat_cd gives 01, 1A, A1, B0, a1 (COLLATE "C", E6)',
       string_agg(trancat_type_cd, ',' ORDER BY trancat_acct_id, trancat_type_cd, trancat_cd) = '01,1A,A1,B0,a1',
       string_agg(trancat_type_cd, ',' ORDER BY trancat_acct_id, trancat_type_cd, trancat_cd)
FROM carddemo.tran_cat_balance WHERE trancat_acct_id = 7;

-- ---------------------------------------------------------------------------------------
-- Result
-- ---------------------------------------------------------------------------------------
\pset format unaligned
\pset tuples_only on
SELECT CASE WHEN ok THEN 'PASS  ' ELSE 'FAIL  ' END || name || CASE WHEN ok THEN '' ELSE E'\n      ' || coalesce(detail, '(nothing)') END
FROM assertion ORDER BY seq;
SELECT count(*) || ' assertions, ' || count(*) FILTER (WHERE NOT ok) || ' failed' FROM assertion;

DO $$
DECLARE failed INTEGER;
BEGIN
    SELECT count(*) INTO failed FROM assertion WHERE NOT ok;
    IF failed > 0 THEN
        RAISE EXCEPTION '% schema assertion(s) failed', failed;
    END IF;
END $$;

ROLLBACK;
