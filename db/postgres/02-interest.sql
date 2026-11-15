-- Schema of the interest stream (INTCALC -> CBACT04C), ticket INT-T02, shared interface FI-4.
-- Specification: modernization/design/data-map-interest.md. Column names, types, precision,
-- scale, NOT NULL, CHECKs, collations, primary keys and the index card_xref_acct_ix are taken
-- from it without change; each column cites its data-map row as <copybook>/<FIELD>.
--
-- Needs schema carddemo, created by 01-transaction-type.sql. Apply to a database without
-- these tables:
--     psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql
-- The script is one transaction. Applied a second time it fails at the first CREATE TABLE
-- (ERROR: relation "interest_run" already exists, psql exit code 3) and changes nothing;
-- it is not a no-op and it never drops or alters an existing table.
--
-- No foreign key between the replica tables and none to carddemo.transaction_type (G09): the
-- legacy files have none, and a balance whose account or rate is missing must reach the job.
-- Dates and timestamps of the legacy records stay text (G04, G10).

BEGIN;

-- No copybook. One row per run; written by interest-run-control only.
CREATE TABLE carddemo.interest_run
(   run_id                             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parm_date                          CHAR(10) NOT NULL,          -- PARM of INTCALC.STEP15 (INT-Q02)
    started_at                         TIMESTAMPTZ,                -- wall clock of the target run
    finished_at                        TIMESTAMPTZ,
    status                             VARCHAR(12) NOT NULL,       -- LOADED, CALCULATED, EXPORTED, FAILED, ABANDONED
    -- Reconciliation figures (strategy-interest.md RC1, RC2, RC5). The data map gives the
    -- pattern "loaded_* / written_* counts and sums: BIGINT / NUMERIC(18,2)", not the names.
    loaded_account_count               BIGINT,                     -- RC1
    loaded_card_xref_count             BIGINT,                     -- RC1
    loaded_tran_cat_balance_count      BIGINT,                     -- RC1
    loaded_disclosure_group_count      BIGINT,                     -- RC1
    loaded_acct_curr_bal_sum           NUMERIC(18,2),              -- RC2
    loaded_acct_credit_limit_sum       NUMERIC(18,2),              -- RC2
    loaded_acct_cash_credit_limit_sum  NUMERIC(18,2),              -- RC2
    loaded_acct_curr_cyc_credit_sum    NUMERIC(18,2),              -- RC2
    loaded_acct_curr_cyc_debit_sum     NUMERIC(18,2),              -- RC2
    loaded_tran_cat_bal_sum            NUMERIC(18,2),              -- RC2
    loaded_dis_int_rate_sum            NUMERIC(18,2),              -- RC2
    loaded_acct_id_sum                 NUMERIC(18,2),              -- RC2 hash total of ACCT-ID
    loaded_xref_acct_id_sum            NUMERIC(18,2),              -- RC2 hash total of XREF-ACCT-ID
    loaded_trancat_acct_id_sum         NUMERIC(18,2),              -- RC2 hash total of TRANCAT-ACCT-ID
    written_transaction_count          BIGINT,                     -- RC5
    written_tran_amt_sum               NUMERIC(18,2),              -- RC5
    written_account_count              BIGINT,                     -- accounts updated (INT-R07, INT-Q04)
    written_acct_curr_bal_sum          NUMERIC(18,2)               -- RC5: sum of ACCT-CURR-BAL after the calculation
);

-- CVACT01Y, AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS, record length 300. Replica.
CREATE TABLE carddemo.account
(   acct_id                 NUMERIC(11,0) NOT NULL CHECK (acct_id >= 0),                  -- CVACT01Y/ACCT-ID
    acct_active_status      CHAR(1) NOT NULL,                                             -- CVACT01Y/ACCT-ACTIVE-STATUS
    acct_curr_bal           NUMERIC(12,2) NOT NULL,                                       -- CVACT01Y/ACCT-CURR-BAL
    acct_credit_limit       NUMERIC(12,2) NOT NULL,                                       -- CVACT01Y/ACCT-CREDIT-LIMIT
    acct_cash_credit_limit  NUMERIC(12,2) NOT NULL,                                       -- CVACT01Y/ACCT-CASH-CREDIT-LIMIT
    acct_open_date          CHAR(10) NOT NULL,                                            -- CVACT01Y/ACCT-OPEN-DATE (G04)
    acct_expiraion_date     CHAR(10) NOT NULL,                                            -- CVACT01Y/ACCT-EXPIRAION-DATE (legacy spelling, G04)
    acct_reissue_date       CHAR(10) NOT NULL,                                            -- CVACT01Y/ACCT-REISSUE-DATE (G04)
    acct_curr_cyc_credit    NUMERIC(12,2) NOT NULL,                                       -- CVACT01Y/ACCT-CURR-CYC-CREDIT
    acct_curr_cyc_debit     NUMERIC(12,2) NOT NULL,                                       -- CVACT01Y/ACCT-CURR-CYC-DEBIT
    acct_addr_zip           CHAR(10) NOT NULL,                                            -- CVACT01Y/ACCT-ADDR-ZIP
    acct_group_id           CHAR(10) NOT NULL,                                            -- CVACT01Y/ACCT-GROUP-ID
    acct_filler             BYTEA NOT NULL CHECK (octet_length(acct_filler) = 178),       -- CVACT01Y/FILLER (E4)
    PRIMARY KEY (acct_id));

-- CVACT03Y, AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS, record length 50. Replica.
CREATE TABLE carddemo.card_xref
(   xref_card_num           CHAR(16) NOT NULL,                                            -- CVACT03Y/XREF-CARD-NUM
    xref_cust_id            NUMERIC(9,0) NOT NULL CHECK (xref_cust_id >= 0),              -- CVACT03Y/XREF-CUST-ID
    xref_acct_id            NUMERIC(11,0) NOT NULL CHECK (xref_acct_id >= 0),             -- CVACT03Y/XREF-ACCT-ID
    xref_filler             BYTEA NOT NULL CHECK (octet_length(xref_filler) = 14),        -- CVACT03Y/FILLER (E4)
    PRIMARY KEY (xref_card_num));

-- Alternate index KEYS(11,25) NONUNIQUEKEY (app/jcl/XREFFILE.jcl:72-92): not unique (G08, INT-H03).
CREATE INDEX card_xref_acct_ix ON carddemo.card_xref (xref_acct_id, xref_card_num);

-- CVTRA01Y, AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS, record length 50. Replica.
CREATE TABLE carddemo.tran_cat_balance
(   trancat_acct_id         NUMERIC(11,0) NOT NULL CHECK (trancat_acct_id >= 0),          -- CVTRA01Y/TRANCAT-ACCT-ID
    trancat_type_cd         CHAR(2) COLLATE "C" NOT NULL,                                 -- CVTRA01Y/TRANCAT-TYPE-CD (E6, G06)
    trancat_cd              NUMERIC(4,0) NOT NULL CHECK (trancat_cd >= 0),                -- CVTRA01Y/TRANCAT-CD (G09)
    tran_cat_bal            NUMERIC(11,2) NOT NULL,                                       -- CVTRA01Y/TRAN-CAT-BAL
    trancat_filler          BYTEA NOT NULL CHECK (octet_length(trancat_filler) = 22),     -- CVTRA01Y/FILLER (E4)
    PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd));                          -- CVTRA01Y/TRAN-CAT-KEY

-- CVTRA02Y, AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS, record length 50. Replica.
CREATE TABLE carddemo.disclosure_group
(   dis_acct_group_id       CHAR(10) COLLATE "C" NOT NULL,                                -- CVTRA02Y/DIS-ACCT-GROUP-ID
    dis_tran_type_cd        CHAR(2) COLLATE "C" NOT NULL,                                 -- CVTRA02Y/DIS-TRAN-TYPE-CD
    dis_tran_cat_cd         NUMERIC(4,0) NOT NULL CHECK (dis_tran_cat_cd >= 0),           -- CVTRA02Y/DIS-TRAN-CAT-CD (G09)
    dis_int_rate            NUMERIC(6,2) NOT NULL,                                        -- CVTRA02Y/DIS-INT-RATE
    dis_filler              BYTEA NOT NULL CHECK (octet_length(dis_filler) = 28),         -- CVTRA02Y/FILLER (E4)
    PRIMARY KEY (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd));                  -- CVTRA02Y/DIS-GROUP-KEY

-- CVTRA05Y, AWS.M2.CARDDEMO.SYSTRAN(+1), record length 350. Owned: one set of rows per run.
CREATE TABLE carddemo.interest_transaction
(   tran_id                 CHAR(16) NOT NULL,                                            -- CVTRA05Y/TRAN-ID
    tran_type_cd            CHAR(2) NOT NULL,                                             -- CVTRA05Y/TRAN-TYPE-CD
    tran_cat_cd             NUMERIC(4,0) NOT NULL CHECK (tran_cat_cd >= 0),               -- CVTRA05Y/TRAN-CAT-CD (G09)
    tran_source             CHAR(10) NOT NULL,                                            -- CVTRA05Y/TRAN-SOURCE
    tran_desc               CHAR(100) NOT NULL,                                           -- CVTRA05Y/TRAN-DESC (G05)
    tran_amt                NUMERIC(11,2) NOT NULL,                                       -- CVTRA05Y/TRAN-AMT
    tran_merchant_id        NUMERIC(9,0) NOT NULL CHECK (tran_merchant_id >= 0),          -- CVTRA05Y/TRAN-MERCHANT-ID
    tran_merchant_name      CHAR(50) NOT NULL,                                            -- CVTRA05Y/TRAN-MERCHANT-NAME
    tran_merchant_city      CHAR(50) NOT NULL,                                            -- CVTRA05Y/TRAN-MERCHANT-CITY
    tran_merchant_zip       CHAR(10) NOT NULL,                                            -- CVTRA05Y/TRAN-MERCHANT-ZIP
    tran_card_num           CHAR(16) NOT NULL,                                            -- CVTRA05Y/TRAN-CARD-NUM
    tran_orig_ts            CHAR(26) NOT NULL,                                            -- CVTRA05Y/TRAN-ORIG-TS (G10)
    tran_proc_ts            CHAR(26) NOT NULL,                                            -- CVTRA05Y/TRAN-PROC-TS (G10)
    tran_filler             BYTEA NOT NULL CHECK (octet_length(tran_filler) = 20),        -- CVTRA05Y/FILLER (E4, G05)
    run_id                  BIGINT NOT NULL REFERENCES carddemo.interest_run,             -- the run (SYSTRAN generation)
    seq_no                  INTEGER NOT NULL,                                             -- write order inside the run, 1..n
    PRIMARY KEY (run_id, seq_no),
    UNIQUE (run_id, tran_id));

COMMIT;
