-- ============================================================================
-- CardDemo Mainframe-to-Postgres Migration: Target Schema DDL
-- Source: COBOL copybooks (CVCUS01Y, CVACT01Y, CVACT02Y, CVACT03Y,
--         CVTRA03Y, CVTRA04Y, CVTRA01Y, CVTRA02Y, CVTRA05Y/CVTRA06Y)
-- Generated from data mapping analysis of app/cpy/ and app/data/ASCII/
-- ============================================================================

BEGIN;

-- ============================================================================
-- Schema
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS carddemo;
SET search_path TO carddemo;

-- ============================================================================
-- Staging tables  (raw fixed-width ingest; all TEXT for initial load)
-- ============================================================================

CREATE TABLE carddemo.stg_customer (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

CREATE TABLE carddemo.stg_account (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

CREATE TABLE carddemo.stg_card (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

CREATE TABLE carddemo.stg_card_xref (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

CREATE TABLE carddemo.stg_transaction (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

CREATE TABLE carddemo.stg_tran_type (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

CREATE TABLE carddemo.stg_tran_category (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

CREATE TABLE carddemo.stg_disclosure_group (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

CREATE TABLE carddemo.stg_tran_cat_bal (
    raw_record          TEXT        NOT NULL,
    load_batch_id       BIGINT      NOT NULL,
    load_ts             TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_file         TEXT,
    source_line_num     INTEGER
);

-- ============================================================================
-- Reference / lookup tables  (no FK dependencies)
-- ============================================================================

-- Source: CVTRA03Y.cpy  |  trantype.txt  |  RECLN 60
CREATE TABLE carddemo.transaction_type (
    tran_type_cd            CHAR(2)         NOT NULL,
    tran_type_desc          VARCHAR(50)     NOT NULL,
    created_ts              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_transaction_type
        PRIMARY KEY (tran_type_cd)
);

COMMENT ON TABLE  carddemo.transaction_type IS
    'Transaction type reference data. Source: CVTRA03Y.cpy / trantype.txt (RECLN 60).';

-- Source: CVTRA04Y.cpy  |  trancatg.txt  |  RECLN 60
CREATE TABLE carddemo.transaction_category (
    tran_type_cd            CHAR(2)         NOT NULL,
    tran_cat_cd             SMALLINT        NOT NULL,
    tran_cat_type_desc      VARCHAR(50)     NOT NULL,
    created_ts              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_transaction_category
        PRIMARY KEY (tran_type_cd, tran_cat_cd),
    CONSTRAINT fk_trancat_tran_type
        FOREIGN KEY (tran_type_cd)
        REFERENCES carddemo.transaction_type (tran_type_cd)
);

COMMENT ON TABLE  carddemo.transaction_category IS
    'Transaction category reference data. Source: CVTRA04Y.cpy / trancatg.txt (RECLN 60).';

-- ============================================================================
-- Core entity tables
-- ============================================================================

-- Source: CVCUS01Y.cpy / CUSTREC.cpy  |  custdata.txt  |  RECLN 500
CREATE TABLE carddemo.customer (
    cust_id                     BIGINT          NOT NULL,
    cust_first_name             VARCHAR(25)     NOT NULL,
    cust_middle_name            VARCHAR(25),
    cust_last_name              VARCHAR(25)     NOT NULL,
    cust_addr_line_1            VARCHAR(50),
    cust_addr_line_2            VARCHAR(50),
    cust_addr_line_3            VARCHAR(50),
    cust_addr_state_cd          CHAR(2),
    cust_addr_country_cd        CHAR(3)         DEFAULT 'USA',
    cust_addr_zip               VARCHAR(10),
    cust_phone_num_1            VARCHAR(15),
    cust_phone_num_2            VARCHAR(15),
    cust_ssn                    VARCHAR(11),        -- stored encrypted; XXX-XX-XXXX
    cust_govt_issued_id         VARCHAR(20),
    cust_dob                    DATE,
    cust_eft_account_id         VARCHAR(10),
    cust_pri_card_holder_ind    BOOLEAN         DEFAULT FALSE,
    cust_fico_credit_score      SMALLINT,
    created_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_customer
        PRIMARY KEY (cust_id),
    CONSTRAINT chk_cust_fico_range
        CHECK (cust_fico_credit_score IS NULL
            OR (cust_fico_credit_score BETWEEN 0 AND 999))
);

COMMENT ON TABLE  carddemo.customer IS
    'Customer master data. Source: CVCUS01Y.cpy / custdata.txt (RECLN 500).';
COMMENT ON COLUMN carddemo.customer.cust_ssn IS
    'Social Security Number. Format: XXX-XX-XXXX. Must be encrypted at rest (pgcrypto / TDE).';

-- Source: CVACT01Y.cpy  |  acctdata.txt  |  RECLN 300
CREATE TABLE carddemo.account (
    acct_id                     BIGINT          NOT NULL,
    acct_active_status          BOOLEAN         NOT NULL DEFAULT TRUE,
    acct_curr_bal               NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    acct_credit_limit           NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    acct_cash_credit_limit      NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    acct_open_date              DATE            NOT NULL,
    acct_expiration_date        DATE,
    acct_reissue_date           DATE,
    acct_curr_cyc_credit        NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    acct_curr_cyc_debit         NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    acct_addr_zip               VARCHAR(10),
    acct_group_id               VARCHAR(10),
    created_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_account
        PRIMARY KEY (acct_id),
    CONSTRAINT chk_acct_credit_limit_positive
        CHECK (acct_credit_limit >= 0),
    CONSTRAINT chk_acct_cash_credit_limit_positive
        CHECK (acct_cash_credit_limit >= 0)
);

COMMENT ON TABLE  carddemo.account IS
    'Account master data. Source: CVACT01Y.cpy / acctdata.txt (RECLN 300).';

-- Source: CVACT02Y.cpy  |  carddata.txt  |  RECLN 150
CREATE TABLE carddemo.card (
    card_num                    VARCHAR(16)     NOT NULL,
    card_acct_id                BIGINT          NOT NULL,
    card_cvv_cd                 VARCHAR(3)      NOT NULL,   -- encrypt at rest for PCI
    card_embossed_name          VARCHAR(50)     NOT NULL,
    card_expiration_date        DATE            NOT NULL,
    card_active_status          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_card
        PRIMARY KEY (card_num),
    CONSTRAINT fk_card_account
        FOREIGN KEY (card_acct_id)
        REFERENCES carddemo.account (acct_id)
);

COMMENT ON TABLE  carddemo.card IS
    'Card master data. Source: CVACT02Y.cpy / carddata.txt (RECLN 150).';
COMMENT ON COLUMN carddemo.card.card_cvv_cd IS
    'Card verification value. Must be encrypted at rest for PCI-DSS compliance.';

-- Source: CVACT03Y.cpy  |  cardxref.txt  |  RECLN 50
CREATE TABLE carddemo.card_xref (
    xref_card_num               VARCHAR(16)     NOT NULL,
    xref_cust_id                BIGINT          NOT NULL,
    xref_acct_id                BIGINT          NOT NULL,
    created_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_card_xref
        PRIMARY KEY (xref_card_num),
    CONSTRAINT fk_xref_card
        FOREIGN KEY (xref_card_num)
        REFERENCES carddemo.card (card_num),
    CONSTRAINT fk_xref_customer
        FOREIGN KEY (xref_cust_id)
        REFERENCES carddemo.customer (cust_id),
    CONSTRAINT fk_xref_account
        FOREIGN KEY (xref_acct_id)
        REFERENCES carddemo.account (acct_id)
);

COMMENT ON TABLE  carddemo.card_xref IS
    'Card-to-customer-to-account cross-reference. Source: CVACT03Y.cpy / cardxref.txt (RECLN 50).';

-- Source: CVTRA02Y.cpy  |  discgrp.txt  |  RECLN 50
CREATE TABLE carddemo.disclosure_group (
    dis_acct_group_id           VARCHAR(10)     NOT NULL,
    dis_tran_type_cd            CHAR(2)         NOT NULL,
    dis_tran_cat_cd             SMALLINT        NOT NULL,
    dis_int_rate                NUMERIC(6,2)    NOT NULL DEFAULT 0.00,
    created_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_disclosure_group
        PRIMARY KEY (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd),
    CONSTRAINT fk_disgrp_tran_type
        FOREIGN KEY (dis_tran_type_cd)
        REFERENCES carddemo.transaction_type (tran_type_cd),
    CONSTRAINT fk_disgrp_tran_category
        FOREIGN KEY (dis_tran_type_cd, dis_tran_cat_cd)
        REFERENCES carddemo.transaction_category (tran_type_cd, tran_cat_cd)
);

COMMENT ON TABLE  carddemo.disclosure_group IS
    'Interest rate disclosure by account group, type, and category. Source: CVTRA02Y.cpy / discgrp.txt (RECLN 50).';

-- Source: CVTRA01Y.cpy  |  tcatbal.txt  |  RECLN 50
CREATE TABLE carddemo.tran_cat_balance (
    trancat_acct_id             BIGINT          NOT NULL,
    trancat_type_cd             CHAR(2)         NOT NULL,
    trancat_cd                  SMALLINT        NOT NULL,
    tran_cat_bal                NUMERIC(11,2)   NOT NULL DEFAULT 0.00,
    created_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_tran_cat_balance
        PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd),
    CONSTRAINT fk_tcatbal_account
        FOREIGN KEY (trancat_acct_id)
        REFERENCES carddemo.account (acct_id),
    CONSTRAINT fk_tcatbal_tran_type
        FOREIGN KEY (trancat_type_cd)
        REFERENCES carddemo.transaction_type (tran_type_cd),
    CONSTRAINT fk_tcatbal_tran_category
        FOREIGN KEY (trancat_type_cd, trancat_cd)
        REFERENCES carddemo.transaction_category (tran_type_cd, tran_cat_cd)
);

COMMENT ON TABLE  carddemo.tran_cat_balance IS
    'Transaction category balance per account. Source: CVTRA01Y.cpy / tcatbal.txt (RECLN 50).';

-- Source: CVTRA05Y.cpy / CVTRA06Y.cpy  |  dailytran.txt  |  RECLN 350
CREATE TABLE carddemo.transaction (
    tran_id                     VARCHAR(16)     NOT NULL,
    tran_type_cd                CHAR(2)         NOT NULL,
    tran_cat_cd                 SMALLINT        NOT NULL,
    tran_source                 VARCHAR(10),
    tran_desc                   VARCHAR(100),
    tran_amt                    NUMERIC(11,2)   NOT NULL DEFAULT 0.00,
    tran_merchant_id            BIGINT,
    tran_merchant_name          VARCHAR(50),
    tran_merchant_city          VARCHAR(50),
    tran_merchant_zip           VARCHAR(10),
    tran_card_num               VARCHAR(16)     NOT NULL,
    tran_orig_ts                TIMESTAMP,
    tran_proc_ts                TIMESTAMP       DEFAULT now(),
    created_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_ts                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_transaction
        PRIMARY KEY (tran_id),
    CONSTRAINT fk_tran_card
        FOREIGN KEY (tran_card_num)
        REFERENCES carddemo.card (card_num),
    CONSTRAINT fk_tran_type
        FOREIGN KEY (tran_type_cd)
        REFERENCES carddemo.transaction_type (tran_type_cd),
    CONSTRAINT fk_tran_category
        FOREIGN KEY (tran_type_cd, tran_cat_cd)
        REFERENCES carddemo.transaction_category (tran_type_cd, tran_cat_cd)
);

COMMENT ON TABLE  carddemo.transaction IS
    'Transaction detail records. Source: CVTRA05Y.cpy / dailytran.txt (RECLN 350).';

-- ============================================================================
-- Indexes  (beyond PK)
-- ============================================================================
CREATE INDEX idx_customer_ssn        ON carddemo.customer (cust_ssn);
CREATE INDEX idx_customer_last_name  ON carddemo.customer (cust_last_name);
CREATE INDEX idx_account_group_id    ON carddemo.account (acct_group_id);
CREATE INDEX idx_card_acct_id        ON carddemo.card (card_acct_id);
CREATE INDEX idx_xref_cust_id        ON carddemo.card_xref (xref_cust_id);
CREATE INDEX idx_xref_acct_id        ON carddemo.card_xref (xref_acct_id);
CREATE INDEX idx_tran_card_num       ON carddemo.transaction (tran_card_num);
CREATE INDEX idx_tran_orig_ts        ON carddemo.transaction (tran_orig_ts);
CREATE INDEX idx_tran_type_cat       ON carddemo.transaction (tran_type_cd, tran_cat_cd);
CREATE INDEX idx_tcatbal_acct        ON carddemo.tran_cat_balance (trancat_acct_id);

-- ============================================================================
-- ETL audit table
-- ============================================================================
CREATE TABLE carddemo.etl_batch_log (
    batch_id            BIGSERIAL       NOT NULL,
    entity_name         VARCHAR(50)     NOT NULL,
    source_file         TEXT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'STARTED',
    records_staged      INTEGER         DEFAULT 0,
    records_validated   INTEGER         DEFAULT 0,
    records_rejected    INTEGER         DEFAULT 0,
    records_inserted    INTEGER         DEFAULT 0,
    records_updated     INTEGER         DEFAULT 0,
    started_ts          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_ts        TIMESTAMPTZ,
    error_message       TEXT,

    CONSTRAINT pk_etl_batch_log
        PRIMARY KEY (batch_id),
    CONSTRAINT chk_etl_status
        CHECK (status IN ('STARTED','VALIDATING','LOADING','COMPLETED','FAILED'))
);

COMMENT ON TABLE  carddemo.etl_batch_log IS
    'Audit log tracking each ETL batch run per entity.';

-- ============================================================================
-- ETL rejection table
-- ============================================================================
CREATE TABLE carddemo.etl_rejected_records (
    rejection_id        BIGSERIAL       NOT NULL,
    batch_id            BIGINT          NOT NULL,
    entity_name         VARCHAR(50)     NOT NULL,
    source_line_num     INTEGER,
    raw_record          TEXT,
    rejection_reason    TEXT            NOT NULL,
    rejected_ts         TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_etl_rejected
        PRIMARY KEY (rejection_id),
    CONSTRAINT fk_rejected_batch
        FOREIGN KEY (batch_id)
        REFERENCES carddemo.etl_batch_log (batch_id)
);

COMMENT ON TABLE  carddemo.etl_rejected_records IS
    'Records that failed validation during ETL and were rejected.';

COMMIT;
