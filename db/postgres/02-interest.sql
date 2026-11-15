-- Schema of the interest stream (INTCALC -> CBACT04C), ticket INT-T02, shared interface FI-4.
--
-- Specification: modernization/design/data-map-interest.md (architecture.md 0.1.0, section 3).
-- Column names, types, precision, scale, NOT NULL, CHECKs, collations, primary keys and the
-- index card_xref_acct_ix are taken from it without change.  Seven objects: four replica
-- tables (account, card_xref, tran_cat_balance, disclosure_group), two owned tables
-- (interest_run, interest_transaction) and the index card_xref_acct_ix.
--
-- Needs: PostgreSQL 15 with db/postgres/01-transaction-type.sql applied (it creates schema
-- carddemo).  That script is not changed and none of its tables is referenced (G09).
--
-- Apply:  psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql
-- The script is one transaction.  A second run FAILS CLEANLY: the first CREATE TABLE ends with
-- 'relation "account" already exists', psql exits 3, the transaction is rolled back and the
-- database is exactly as it was.  It is not a no-op and it never drops or alters anything.
-- Assertions: db/postgres/tests/02-interest-assert.sql.
--
-- Not here on purpose: foreign keys between the replica tables (the legacy files have none, and
-- a balance whose account or rate is missing must reach the job so that it stops, INT-R08);
-- foreign keys to carddemo.transaction_type (G09); the Spring Batch job repository (framework
-- script, schema batch); retention (REL-12).

BEGIN;

-- CVACT01Y, AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS, record length 300.  Replica.
CREATE TABLE carddemo.account
(   acct_id                        NUMERIC(11,0) NOT NULL CHECK (acct_id >= 0),
    acct_active_status             CHAR(1) NOT NULL,
    acct_curr_bal                  NUMERIC(12,2) NOT NULL,
    acct_credit_limit              NUMERIC(12,2) NOT NULL,
    acct_cash_credit_limit         NUMERIC(12,2) NOT NULL,
    acct_open_date                 CHAR(10) NOT NULL,
    acct_expiraion_date            CHAR(10) NOT NULL,
    acct_reissue_date              CHAR(10) NOT NULL,
    acct_curr_cyc_credit           NUMERIC(12,2) NOT NULL,
    acct_curr_cyc_debit            NUMERIC(12,2) NOT NULL,
    acct_addr_zip                  CHAR(10) NOT NULL,
    acct_group_id                  CHAR(10) NOT NULL,
    acct_filler                    BYTEA NOT NULL CHECK (octet_length(acct_filler) = 178),
    PRIMARY KEY (acct_id));

-- CVACT03Y, AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS, record length 50.  Replica.
CREATE TABLE carddemo.card_xref
(   xref_card_num                  CHAR(16) NOT NULL,
    xref_cust_id                   NUMERIC(9,0) NOT NULL CHECK (xref_cust_id >= 0),
    xref_acct_id                   NUMERIC(11,0) NOT NULL CHECK (xref_acct_id >= 0),
    xref_filler                    BYTEA NOT NULL CHECK (octet_length(xref_filler) = 14),
    PRIMARY KEY (xref_card_num));

-- The legacy alternate index KEYS(11,25) NONUNIQUEKEY on XREF-ACCT-ID (G08, INT-H03): not unique.
CREATE INDEX card_xref_acct_ix ON carddemo.card_xref (xref_acct_id, xref_card_num);

-- CVTRA01Y, AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS, record length 50.  Replica.
-- Read in key order: ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd (E6, G06).
CREATE TABLE carddemo.tran_cat_balance
(   trancat_acct_id                NUMERIC(11,0) NOT NULL CHECK (trancat_acct_id >= 0),
    trancat_type_cd                CHAR(2) COLLATE "C" NOT NULL,
    trancat_cd                     NUMERIC(4,0) NOT NULL CHECK (trancat_cd >= 0),
    tran_cat_bal                   NUMERIC(11,2) NOT NULL,
    trancat_filler                 BYTEA NOT NULL CHECK (octet_length(trancat_filler) = 22),
    PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd));

-- CVTRA02Y, AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS, record length 50.  Replica.
CREATE TABLE carddemo.disclosure_group
(   dis_acct_group_id              CHAR(10) COLLATE "C" NOT NULL,
    dis_tran_type_cd               CHAR(2) COLLATE "C" NOT NULL,
    dis_tran_cat_cd                NUMERIC(4,0) NOT NULL CHECK (dis_tran_cat_cd >= 0),
    dis_int_rate                   NUMERIC(6,2) NOT NULL,
    dis_filler                     BYTEA NOT NULL CHECK (octet_length(dis_filler) = 28),
    PRIMARY KEY (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd));

-- No copybook.  One row per run; written by interest-run-control only.
-- The data map names run_id, parm_date, started_at, finished_at and status.  It gives the
-- reconciliation figures only as "loaded_* / written_* counts and sums, BIGINT / NUMERIC(18,2)"
-- (strategy items RC1-RC6); the column names below are this ticket's reading of that row, one
-- column per figure, every count BIGINT and every sum NUMERIC(18,2), all nullable because a
-- step fills them when it ends.  See the gap list of the pull request.
CREATE TABLE carddemo.interest_run
(   run_id                         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parm_date                      CHAR(10) NOT NULL,
    started_at                     TIMESTAMPTZ,
    finished_at                    TIMESTAMPTZ,
    status                         VARCHAR(12) NOT NULL,
    -- RC1: rows loaded per replica table (loadExtractsStep)
    loaded_account_count           BIGINT,
    loaded_card_xref_count         BIGINT,
    loaded_tran_cat_balance_count  BIGINT,
    loaded_disclosure_group_count  BIGINT,
    -- RC2: sums over the loaded rows, and the account ids of each file as a hash total
    loaded_acct_curr_bal_sum       NUMERIC(18,2),
    loaded_acct_credit_limit_sum   NUMERIC(18,2),
    loaded_acct_cash_credit_limit_sum NUMERIC(18,2),
    loaded_acct_curr_cyc_credit_sum NUMERIC(18,2),
    loaded_acct_curr_cyc_debit_sum NUMERIC(18,2),
    loaded_tran_cat_bal_sum        NUMERIC(18,2),
    loaded_dis_int_rate_sum        NUMERIC(18,2),
    loaded_acct_id_sum             NUMERIC(18,2),
    loaded_xref_acct_id_sum        NUMERIC(18,2),
    loaded_trancat_acct_id_sum     NUMERIC(18,2),
    -- RC3: round-trip differences counted per gap id
    loaded_g02_difference_count    BIGINT,
    loaded_g03_difference_count    BIGINT,
    -- RC4: distinct keys per replica table
    loaded_account_key_count       BIGINT,
    loaded_card_xref_key_count     BIGINT,
    loaded_tran_cat_balance_key_count BIGINT,
    loaded_disclosure_group_key_count BIGINT,
    -- RC5: interest transactions inserted (calcInterestStep), the interest of the account that
    -- ends the input and is not added (INT-Q04), and the exported SYSTRAN generation
    written_transaction_count      BIGINT,
    written_tran_amt_sum           NUMERIC(18,2),
    written_interest_not_added_sum NUMERIC(18,2),
    written_systran_count          BIGINT,
    written_systran_tran_amt_sum   NUMERIC(18,2),
    -- RC6: the exported ACCTDATA image (count and the five sums of RC2)
    written_acctdata_count         BIGINT,
    written_acct_curr_bal_sum      NUMERIC(18,2),
    written_acct_credit_limit_sum  NUMERIC(18,2),
    written_acct_cash_credit_limit_sum NUMERIC(18,2),
    written_acct_curr_cyc_credit_sum NUMERIC(18,2),
    written_acct_curr_cyc_debit_sum NUMERIC(18,2));

-- CVTRA05Y, AWS.M2.CARDDEMO.SYSTRAN(+1), record length 350.  Owned: one set of rows per run.
CREATE TABLE carddemo.interest_transaction
(   tran_id                        CHAR(16) NOT NULL,
    tran_type_cd                   CHAR(2) NOT NULL,
    tran_cat_cd                    NUMERIC(4,0) NOT NULL CHECK (tran_cat_cd >= 0),
    tran_source                    CHAR(10) NOT NULL,
    tran_desc                      CHAR(100) NOT NULL,
    tran_amt                       NUMERIC(11,2) NOT NULL,
    tran_merchant_id               NUMERIC(9,0) NOT NULL CHECK (tran_merchant_id >= 0),
    tran_merchant_name             CHAR(50) NOT NULL,
    tran_merchant_city             CHAR(50) NOT NULL,
    tran_merchant_zip              CHAR(10) NOT NULL,
    tran_card_num                  CHAR(16) NOT NULL,
    tran_orig_ts                   CHAR(26) NOT NULL,
    tran_proc_ts                   CHAR(26) NOT NULL,
    tran_filler                    BYTEA NOT NULL CHECK (octet_length(tran_filler) = 20),
    run_id                         BIGINT NOT NULL REFERENCES carddemo.interest_run,
    seq_no                         INTEGER NOT NULL,
    PRIMARY KEY (run_id, seq_no),
    UNIQUE (run_id, tran_id));

COMMIT;
