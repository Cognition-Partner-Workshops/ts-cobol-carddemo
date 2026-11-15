-- Schema of the interest stream (INTCALC -> CBACT04C), ticket 02 / INT-T02, frozen interface FI-4.
-- Specification: modernization/design/data-map-interest.md (rows cited as <copybook>/<FIELD>).
-- Column names, types, precision, scale, NOT NULL, CHECKs, collations, primary keys and the
-- index card_xref_acct_ix are taken from it without change.
--
-- Needs: PostgreSQL 15 with db/postgres/01-transaction-type.sql applied (schema carddemo).
-- Run:   psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql
--
-- The script is one transaction.  A second run FAILS CLEANLY: the first CREATE TABLE raises
-- "relation ... already exists", the transaction is rolled back, psql exits 3 (ON_ERROR_STOP)
-- and no object or row is changed.  Nothing is dropped or replaced by this script.
--
-- Not here, on purpose: foreign keys between the replica tables or to
-- carddemo.transaction_type (the legacy files have none; G09), DATE/TIMESTAMP typing of the
-- text dates (G04, G10), the Spring Batch job repository (schema batch, framework script),
-- retention (REL-12).
BEGIN;

-- CVACT01Y -> carddemo.account.  AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS, record length 300, KEYS(11 0).  Replica.
CREATE TABLE carddemo.account
(   acct_id                        NUMERIC(11,0) NOT NULL CHECK (acct_id >= 0),              -- CVACT01Y/ACCT-ID 9(11), E1
    acct_active_status             CHAR(1) NOT NULL,                                         -- CVACT01Y/ACCT-ACTIVE-STATUS X(01), E3
    acct_curr_bal                  NUMERIC(12,2) NOT NULL,                                   -- CVACT01Y/ACCT-CURR-BAL S9(10)V99, E2
    acct_credit_limit              NUMERIC(12,2) NOT NULL,                                   -- CVACT01Y/ACCT-CREDIT-LIMIT S9(10)V99, E2
    acct_cash_credit_limit         NUMERIC(12,2) NOT NULL,                                   -- CVACT01Y/ACCT-CASH-CREDIT-LIMIT S9(10)V99, E2
    acct_open_date                 CHAR(10) NOT NULL,                                        -- CVACT01Y/ACCT-OPEN-DATE X(10), E3 (G04)
    acct_expiraion_date            CHAR(10) NOT NULL,                                        -- CVACT01Y/ACCT-EXPIRAION-DATE X(10), E3 (legacy spelling, G04)
    acct_reissue_date              CHAR(10) NOT NULL,                                        -- CVACT01Y/ACCT-REISSUE-DATE X(10), E3 (G04)
    acct_curr_cyc_credit           NUMERIC(12,2) NOT NULL,                                   -- CVACT01Y/ACCT-CURR-CYC-CREDIT S9(10)V99, E2
    acct_curr_cyc_debit            NUMERIC(12,2) NOT NULL,                                   -- CVACT01Y/ACCT-CURR-CYC-DEBIT S9(10)V99, E2
    acct_addr_zip                  CHAR(10) NOT NULL,                                        -- CVACT01Y/ACCT-ADDR-ZIP X(10), E3
    acct_group_id                  CHAR(10) NOT NULL,                                        -- CVACT01Y/ACCT-GROUP-ID X(10), E3
    acct_filler                    BYTEA NOT NULL CHECK (octet_length(acct_filler) = 178),   -- CVACT01Y/FILLER X(178), E4
    PRIMARY KEY (acct_id));

-- CVACT03Y -> carddemo.card_xref.  AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS, record length 50, KEYS(16 0).  Replica.
CREATE TABLE carddemo.card_xref
(   xref_card_num                  CHAR(16) NOT NULL,                                        -- CVACT03Y/XREF-CARD-NUM X(16), E3
    xref_cust_id                   NUMERIC(9,0) NOT NULL CHECK (xref_cust_id >= 0),          -- CVACT03Y/XREF-CUST-ID 9(09), E1
    xref_acct_id                   NUMERIC(11,0) NOT NULL CHECK (xref_acct_id >= 0),         -- CVACT03Y/XREF-ACCT-ID 9(11), E1 (G08, INT-H03)
    xref_filler                    BYTEA NOT NULL CHECK (octet_length(xref_filler) = 14),    -- CVACT03Y/FILLER X(14), E4
    PRIMARY KEY (xref_card_num));

-- Legacy alternate index KEYS(11,25) NONUNIQUEKEY on XREF-ACCT-ID (app/jcl/XREFFILE.jcl:72-92): not unique.
CREATE INDEX card_xref_acct_ix ON carddemo.card_xref (xref_acct_id, xref_card_num);

-- CVTRA01Y -> carddemo.tran_cat_balance.  AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS, record length 50, KEYS(17 0).  Replica.
CREATE TABLE carddemo.tran_cat_balance
(   trancat_acct_id                NUMERIC(11,0) NOT NULL CHECK (trancat_acct_id >= 0),      -- CVTRA01Y/TRANCAT-ACCT-ID 9(11), E1
    trancat_type_cd                CHAR(2) COLLATE "C" NOT NULL,                             -- CVTRA01Y/TRANCAT-TYPE-CD X(02), E3, E6 (G06)
    trancat_cd                     NUMERIC(4,0) NOT NULL CHECK (trancat_cd >= 0),            -- CVTRA01Y/TRANCAT-CD 9(04), E1 (G09)
    tran_cat_bal                   NUMERIC(11,2) NOT NULL,                                   -- CVTRA01Y/TRAN-CAT-BAL S9(09)V99, E2
    trancat_filler                 BYTEA NOT NULL CHECK (octet_length(trancat_filler) = 22), -- CVTRA01Y/FILLER X(22), E4
    PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd));                             -- CVTRA01Y/TRAN-CAT-KEY

-- CVTRA02Y -> carddemo.disclosure_group.  AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS, record length 50, KEYS(16 0).  Replica.
CREATE TABLE carddemo.disclosure_group
(   dis_acct_group_id              CHAR(10) COLLATE "C" NOT NULL,                            -- CVTRA02Y/DIS-ACCT-GROUP-ID X(10), E3
    dis_tran_type_cd               CHAR(2) COLLATE "C" NOT NULL,                             -- CVTRA02Y/DIS-TRAN-TYPE-CD X(02), E3
    dis_tran_cat_cd                NUMERIC(4,0) NOT NULL CHECK (dis_tran_cat_cd >= 0),       -- CVTRA02Y/DIS-TRAN-CAT-CD 9(04), E1 (G09)
    dis_int_rate                   NUMERIC(6,2) NOT NULL,                                    -- CVTRA02Y/DIS-INT-RATE S9(04)V99, E2
    dis_filler                     BYTEA NOT NULL CHECK (octet_length(dis_filler) = 28),     -- CVTRA02Y/FILLER X(28), E4
    PRIMARY KEY (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd));                     -- CVTRA02Y/DIS-GROUP-KEY

-- carddemo.interest_run (no copybook).  Owned control table: one row per run.
-- The data map names the reconciliation columns only as "loaded_* / written_* counts and
-- sums, BIGINT / NUMERIC(18,2), the figures of strategy items RC1-RC6".  The names below are
-- this ticket's reading of RC1, RC2 and RC5 (modernization/design/strategy-interest.md);
-- they are nullable because each is known only after its step.
CREATE TABLE carddemo.interest_run
(   run_id                         BIGINT GENERATED ALWAYS AS IDENTITY,
    parm_date                      CHAR(10) NOT NULL,          -- the PARM of INTCALC.STEP15 (INT-Q02)
    started_at                     TIMESTAMPTZ,                -- wall clock of the target run
    finished_at                    TIMESTAMPTZ,
    status                         VARCHAR(12) NOT NULL,       -- LOADED, CALCULATED, EXPORTED, FAILED or ABANDONED
    -- RC1: record count of each unloaded file
    loaded_account_count           BIGINT,
    loaded_card_xref_count         BIGINT,
    loaded_tran_cat_balance_count  BIGINT,
    loaded_disclosure_group_count  BIGINT,
    -- RC2: sums over the loaded rows
    loaded_acct_curr_bal_sum       NUMERIC(18,2),
    loaded_acct_credit_limit_sum   NUMERIC(18,2),
    loaded_acct_cash_credit_limit_sum NUMERIC(18,2),
    loaded_acct_curr_cyc_credit_sum NUMERIC(18,2),
    loaded_acct_curr_cyc_debit_sum NUMERIC(18,2),
    loaded_tran_cat_bal_sum        NUMERIC(18,2),
    loaded_dis_int_rate_sum        NUMERIC(18,2),
    -- RC2: hash totals, the sum of the account ids of each file that has one
    loaded_acct_id_sum             NUMERIC(18,2),
    loaded_xref_acct_id_sum        NUMERIC(18,2),
    loaded_trancat_acct_id_sum     NUMERIC(18,2),
    -- RC5: what the calculation wrote
    written_transaction_count      BIGINT,
    written_tran_amt_sum           NUMERIC(18,2),
    written_account_count          BIGINT,                     -- accounts updated (the account ending the input is not, INT-Q04)
    written_acct_curr_bal_sum      NUMERIC(18,2),              -- SUM(acct_curr_bal) after the calculation
    PRIMARY KEY (run_id));

-- CVTRA05Y -> carddemo.interest_transaction.  AWS.M2.CARDDEMO.SYSTRAN(+1), record length 350.  Owned.
CREATE TABLE carddemo.interest_transaction
(   run_id                         BIGINT NOT NULL REFERENCES carddemo.interest_run,          -- which run (SYSTRAN generation) wrote the row
    seq_no                         INTEGER NOT NULL,                                         -- write order inside the run, 1..n
    tran_id                        CHAR(16) NOT NULL,                                        -- CVTRA05Y/TRAN-ID X(16), E3 (INT-Q02, INT-H05)
    tran_type_cd                   CHAR(2) NOT NULL,                                         -- CVTRA05Y/TRAN-TYPE-CD X(02), E3
    tran_cat_cd                    NUMERIC(4,0) NOT NULL CHECK (tran_cat_cd >= 0),           -- CVTRA05Y/TRAN-CAT-CD 9(04), E1 (G09)
    tran_source                    CHAR(10) NOT NULL,                                        -- CVTRA05Y/TRAN-SOURCE X(10), E3
    tran_desc                      CHAR(100) NOT NULL,                                       -- CVTRA05Y/TRAN-DESC X(100), E3 (G05, INT-T04)
    tran_amt                       NUMERIC(11,2) NOT NULL,                                   -- CVTRA05Y/TRAN-AMT S9(09)V99, E2
    tran_merchant_id               NUMERIC(9,0) NOT NULL CHECK (tran_merchant_id >= 0),      -- CVTRA05Y/TRAN-MERCHANT-ID 9(09), E1
    tran_merchant_name             CHAR(50) NOT NULL,                                        -- CVTRA05Y/TRAN-MERCHANT-NAME X(50), E3
    tran_merchant_city             CHAR(50) NOT NULL,                                        -- CVTRA05Y/TRAN-MERCHANT-CITY X(50), E3
    tran_merchant_zip              CHAR(10) NOT NULL,                                        -- CVTRA05Y/TRAN-MERCHANT-ZIP X(10), E3
    tran_card_num                  CHAR(16) NOT NULL,                                        -- CVTRA05Y/TRAN-CARD-NUM X(16), E3 (INT-R06, INT-H03)
    tran_orig_ts                   CHAR(26) NOT NULL,                                        -- CVTRA05Y/TRAN-ORIG-TS X(26), E3 (G10)
    tran_proc_ts                   CHAR(26) NOT NULL,                                        -- CVTRA05Y/TRAN-PROC-TS X(26), E3 (G10)
    tran_filler                    BYTEA NOT NULL CHECK (octet_length(tran_filler) = 20),    -- CVTRA05Y/FILLER X(20), E4 (G05)
    PRIMARY KEY (run_id, seq_no),
    CONSTRAINT interest_transaction_run_tran_id_key UNIQUE (run_id, tran_id));               -- TRAN-ID is unique inside a run only

COMMIT;
