-- =============================================================================
-- CardDemo Database Schema - V1 Initial Schema
-- Flyway Migration Script
--
-- Migrated from COBOL/VSAM/IMS/DB2 to PostgreSQL
-- Each table corresponds to a VSAM KSDS file, IMS segment, or DB2 table
-- =============================================================================

-- -----------------------------------------------------------------------------
-- USER_SECURITY table (from VSAM file AWS.M2.CARDDEMO.USRSEC.PS)
-- Copybook: CSUSR01Y (80-byte record)
-- COBOL key: SEC-USR-ID PIC X(08)
-- -----------------------------------------------------------------------------
CREATE TABLE user_security (
    usr_id              VARCHAR(8)      NOT NULL,
    usr_first_name      VARCHAR(20)     NOT NULL,
    usr_last_name       VARCHAR(20)     NOT NULL,
    usr_password        VARCHAR(100)    NOT NULL,
    usr_type            VARCHAR(1)      NOT NULL DEFAULT 'U',
    CONSTRAINT pk_user_security PRIMARY KEY (usr_id),
    CONSTRAINT ck_user_type CHECK (usr_type IN ('A', 'U'))
);

COMMENT ON TABLE user_security IS 'User security credentials (from VSAM USRSEC, copybook CSUSR01Y)';
COMMENT ON COLUMN user_security.usr_type IS 'A=Admin, U=User (from 88-level SEC-USR-TYPE)';

-- -----------------------------------------------------------------------------
-- ACCOUNT table (from VSAM file AWS.M2.CARDDEMO.ACCTDATA.PS)
-- Copybook: CVACT01Y (300-byte record)
-- COBOL key: ACCT-ID PIC 9(11)
-- -----------------------------------------------------------------------------
CREATE TABLE account (
    acct_id                 BIGINT          NOT NULL,
    active_status           VARCHAR(1)      NOT NULL DEFAULT 'Y',
    current_balance         DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    credit_limit            DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    cash_credit_limit       DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    open_date               DATE,
    expiration_date         DATE,
    reissue_date            DATE,
    current_cycle_credit    DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    current_cycle_debit     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    address_zip             VARCHAR(10),
    group_id                VARCHAR(10),
    CONSTRAINT pk_account PRIMARY KEY (acct_id)
);

CREATE INDEX idx_account_group_id ON account (group_id);
CREATE INDEX idx_account_active_status ON account (active_status);

COMMENT ON TABLE account IS 'Account master (from VSAM ACCTDATA, copybook CVACT01Y)';

-- -----------------------------------------------------------------------------
-- CUSTOMER table (from VSAM file AWS.M2.CARDDEMO.CUSTDATA.PS)
-- Copybook: CVCUS01Y (500-byte record)
-- COBOL key: CUST-ID PIC 9(09)
-- -----------------------------------------------------------------------------
CREATE TABLE customer (
    cust_id                 BIGINT          NOT NULL,
    first_name              VARCHAR(25),
    middle_name             VARCHAR(25),
    last_name               VARCHAR(25),
    addr_line1              VARCHAR(50),
    addr_line2              VARCHAR(50),
    addr_line3              VARCHAR(50),
    addr_state_cd           VARCHAR(2),
    addr_country_cd         VARCHAR(3),
    addr_zip                VARCHAR(10),
    phone_num1              VARCHAR(15),
    phone_num2              VARCHAR(15),
    ssn                     VARCHAR(9),
    govt_issued_id          VARCHAR(20),
    date_of_birth           DATE,
    eft_account_id          VARCHAR(10),
    pri_card_holder_ind     VARCHAR(1)      DEFAULT 'Y',
    fico_credit_score       INTEGER         DEFAULT 0,
    CONSTRAINT pk_customer PRIMARY KEY (cust_id)
);

CREATE INDEX idx_customer_ssn ON customer (ssn);
CREATE INDEX idx_customer_last_name ON customer (last_name);

COMMENT ON TABLE customer IS 'Customer master (from VSAM CUSTDATA, copybook CVCUS01Y)';

-- -----------------------------------------------------------------------------
-- CARD table (from VSAM file AWS.M2.CARDDEMO.CARDDATA.PS)
-- Copybook: CVACT02Y (150-byte record)
-- COBOL key: CARD-NUM PIC X(16)
-- -----------------------------------------------------------------------------
CREATE TABLE card (
    card_num                VARCHAR(16)     NOT NULL,
    acct_id                 BIGINT          NOT NULL,
    cvv_code                INTEGER,
    embossed_name           VARCHAR(50),
    expiration_date         DATE,
    active_status           VARCHAR(1)      NOT NULL DEFAULT 'Y',
    CONSTRAINT pk_card PRIMARY KEY (card_num),
    CONSTRAINT fk_card_account FOREIGN KEY (acct_id) REFERENCES account (acct_id)
);

CREATE INDEX idx_card_acct_id ON card (acct_id);
CREATE INDEX idx_card_active_status ON card (active_status);

COMMENT ON TABLE card IS 'Card master (from VSAM CARDDATA, copybook CVACT02Y)';

-- -----------------------------------------------------------------------------
-- CARD_XREF table (from VSAM file AWS.M2.CARDDEMO.CARDXREF.PS)
-- Copybook: CVACT03Y (50-byte record)
-- COBOL key: XREF-CARD-NUM PIC X(16)
-- Links cards to customers and accounts
-- -----------------------------------------------------------------------------
CREATE TABLE card_xref (
    card_num                VARCHAR(16)     NOT NULL,
    cust_id                 BIGINT          NOT NULL,
    acct_id                 BIGINT          NOT NULL,
    CONSTRAINT pk_card_xref PRIMARY KEY (card_num),
    CONSTRAINT fk_xref_customer FOREIGN KEY (cust_id) REFERENCES customer (cust_id),
    CONSTRAINT fk_xref_account FOREIGN KEY (acct_id) REFERENCES account (acct_id)
);

CREATE INDEX idx_xref_cust_id ON card_xref (cust_id);
CREATE INDEX idx_xref_acct_id ON card_xref (acct_id);

COMMENT ON TABLE card_xref IS 'Card-Account-Customer cross-reference (from VSAM CARDXREF, copybook CVACT03Y)';

-- -----------------------------------------------------------------------------
-- TRANSACTION_TYPE table (from VSAM file AWS.M2.CARDDEMO.TRANTYPE.PS)
-- Copybook: CVTRA03Y (60-byte record)
-- COBOL key: TRAN-TYPE-CD PIC X(02)
-- Also used by DB2 module (app-transaction-type-db2)
-- -----------------------------------------------------------------------------
CREATE TABLE transaction_type (
    type_code               VARCHAR(2)      NOT NULL,
    description             VARCHAR(50),
    CONSTRAINT pk_transaction_type PRIMARY KEY (type_code)
);

COMMENT ON TABLE transaction_type IS 'Transaction type lookup (from VSAM TRANTYPE, copybook CVTRA03Y)';

-- -----------------------------------------------------------------------------
-- TRANSACTION_CATEGORY table (from VSAM file AWS.M2.CARDDEMO.TRANCATG.PS)
-- Copybook: CVTRA04Y (60-byte record)
-- COBOL key: (TRAN-TYPE-CD, TRAN-CAT-CD)
-- -----------------------------------------------------------------------------
CREATE TABLE transaction_category (
    type_code               VARCHAR(2)      NOT NULL,
    category_code           INTEGER         NOT NULL,
    description             VARCHAR(50),
    CONSTRAINT pk_transaction_category PRIMARY KEY (type_code, category_code),
    CONSTRAINT fk_tcat_type FOREIGN KEY (type_code) REFERENCES transaction_type (type_code)
);

COMMENT ON TABLE transaction_category IS 'Transaction category lookup (from VSAM TRANCATG, copybook CVTRA04Y)';

-- -----------------------------------------------------------------------------
-- TRANSACTION table (from VSAM file AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS)
-- Copybook: CVTRA05Y (350-byte record)
-- COBOL key: TRAN-ID PIC X(16)
-- This is the only true VSAM KSDS file; others are sequential (PS)
-- -----------------------------------------------------------------------------
CREATE TABLE transaction (
    tran_id                 VARCHAR(16)     NOT NULL,
    type_code               VARCHAR(2),
    category_code           INTEGER,
    source                  VARCHAR(10),
    description             VARCHAR(100),
    amount                  DECIMAL(11, 2)  NOT NULL DEFAULT 0.00,
    merchant_id             VARCHAR(15),
    merchant_name           VARCHAR(40),
    merchant_city           VARCHAR(30),
    merchant_zip            VARCHAR(10),
    card_num                VARCHAR(16),
    orig_timestamp          TIMESTAMP,
    proc_timestamp          TIMESTAMP,
    CONSTRAINT pk_transaction PRIMARY KEY (tran_id)
);

CREATE INDEX idx_transaction_card_num ON transaction (card_num);
CREATE INDEX idx_transaction_orig_ts ON transaction (orig_timestamp DESC);
CREATE INDEX idx_transaction_type_cat ON transaction (type_code, category_code);
CREATE INDEX idx_transaction_merchant_id ON transaction (merchant_id);

COMMENT ON TABLE transaction IS 'Transaction records (from VSAM TRANSACT KSDS, copybook CVTRA05Y)';

-- -----------------------------------------------------------------------------
-- TRANSACTION_CATEGORY_BALANCE table (from VSAM file AWS.M2.CARDDEMO.TCATBALF.PS)
-- Copybook: CVTRA01Y (50-byte record)
-- COBOL key: (TRAN-CAT-BAL-ACCT-ID, TRAN-CAT-BAL-TYPE-CD, TRAN-CAT-BAL-CAT-CD)
-- Used by interest calculation batch job (CBACT04C)
-- -----------------------------------------------------------------------------
CREATE TABLE transaction_category_balance (
    acct_id                 BIGINT          NOT NULL,
    type_code               VARCHAR(2)      NOT NULL,
    category_code           INTEGER         NOT NULL,
    balance                 DECIMAL(11, 2)  NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_tcat_balance PRIMARY KEY (acct_id, type_code, category_code),
    CONSTRAINT fk_tcb_account FOREIGN KEY (acct_id) REFERENCES account (acct_id)
);

COMMENT ON TABLE transaction_category_balance IS 'Running balance per account/type/category (from VSAM TCATBALF, copybook CVTRA01Y)';

-- -----------------------------------------------------------------------------
-- DISCLOSURE_GROUP table (from VSAM file AWS.M2.CARDDEMO.DISCGRP.PS)
-- Copybook: CVTRA02Y (50-byte record)
-- COBOL key: (DIS-ACCT-GROUP-ID, DIS-TRAN-TYPE-CD, DIS-TRAN-CAT-CD)
-- Defines interest rates per account group + transaction type/category
-- Used by interest calculation batch job (CBACT04C)
-- -----------------------------------------------------------------------------
CREATE TABLE disclosure_group (
    acct_group_id           VARCHAR(10)     NOT NULL,
    tran_type_code          VARCHAR(2)      NOT NULL,
    tran_cat_code           INTEGER         NOT NULL,
    interest_rate           DECIMAL(6, 2)   NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_disclosure_group PRIMARY KEY (acct_group_id, tran_type_code, tran_cat_code)
);

COMMENT ON TABLE disclosure_group IS 'Interest rate disclosure groups (from VSAM DISCGRP, copybook CVTRA02Y)';

-- =============================================================================
-- Authorization Module Tables (from IMS DB and DB2)
-- Module: app/app-authorization-ims-db2-mq/
-- =============================================================================

-- -----------------------------------------------------------------------------
-- AUTHORIZATION_SUMMARY table (from IMS segment PAUTSUM0)
-- IMS database: DBPAUTP0 (HIDAM primary), root segment
-- Copybook: CIPAUSMY
-- IMS key: PA-ACCT-ID
-- -----------------------------------------------------------------------------
CREATE TABLE authorization_summary (
    acct_id                 BIGINT          NOT NULL,
    cust_id                 BIGINT,
    auth_status             VARCHAR(1),
    account_status_1        VARCHAR(2),
    account_status_2        VARCHAR(2),
    account_status_3        VARCHAR(2),
    account_status_4        VARCHAR(2),
    account_status_5        VARCHAR(2),
    credit_limit            DECIMAL(11, 2),
    cash_limit              DECIMAL(11, 2),
    credit_balance          DECIMAL(11, 2),
    cash_balance            DECIMAL(11, 2),
    approved_auth_count     INTEGER         DEFAULT 0,
    declined_auth_count     INTEGER         DEFAULT 0,
    approved_auth_amount    DECIMAL(11, 2)  DEFAULT 0.00,
    declined_auth_amount    DECIMAL(11, 2)  DEFAULT 0.00,
    CONSTRAINT pk_auth_summary PRIMARY KEY (acct_id)
);

COMMENT ON TABLE authorization_summary IS 'Authorization summary (from IMS segment PAUTSUM0, HIDAM root)';

-- -----------------------------------------------------------------------------
-- AUTHORIZATION_DETAIL table (from IMS segment PAUTDTL1)
-- IMS database: DBPAUTP0, child of PAUTSUM0
-- Copybook: CIPAUDTY
-- Uses surrogate key since IMS uses positional keys
-- -----------------------------------------------------------------------------
CREATE TABLE authorization_detail (
    id                      BIGSERIAL       NOT NULL,
    acct_id                 BIGINT          NOT NULL,
    auth_date               DATE,
    auth_time               TIME,
    auth_orig_date          VARCHAR(6),
    auth_orig_time          VARCHAR(6),
    card_num                VARCHAR(16),
    auth_type               VARCHAR(4),
    card_expiry_date        VARCHAR(4),
    message_type            VARCHAR(6),
    message_source          VARCHAR(6),
    auth_id_code            VARCHAR(6),
    auth_resp_code          VARCHAR(2),
    auth_resp_reason        VARCHAR(4),
    processing_code         INTEGER,
    transaction_amount      DECIMAL(12, 2),
    approved_amount         DECIMAL(12, 2),
    merchant_category_code  VARCHAR(4),
    acquirer_country_code   VARCHAR(3),
    pos_entry_mode          INTEGER,
    merchant_id             VARCHAR(15),
    merchant_name           VARCHAR(22),
    merchant_city           VARCHAR(13),
    merchant_state          VARCHAR(2),
    merchant_zip            VARCHAR(9),
    transaction_id          VARCHAR(15),
    match_status            VARCHAR(1),
    auth_fraud              VARCHAR(1),
    fraud_report_date       DATE,
    CONSTRAINT pk_auth_detail PRIMARY KEY (id),
    CONSTRAINT fk_auth_detail_summary FOREIGN KEY (acct_id)
        REFERENCES authorization_summary (acct_id)
);

CREATE INDEX idx_auth_detail_acct_id ON authorization_detail (acct_id);
CREATE INDEX idx_auth_detail_card_num ON authorization_detail (card_num);
CREATE INDEX idx_auth_detail_match_status ON authorization_detail (match_status);
CREATE INDEX idx_auth_detail_auth_date ON authorization_detail (auth_date DESC);

COMMENT ON TABLE authorization_detail IS 'Authorization detail (from IMS segment PAUTDTL1, child of PAUTSUM0)';
COMMENT ON COLUMN authorization_detail.match_status IS 'P=Pending, D=Declined, E=Expired, M=Matched';
COMMENT ON COLUMN authorization_detail.auth_fraud IS 'F=Fraud Confirmed, R=Fraud Removed';

-- -----------------------------------------------------------------------------
-- AUTH_FRAUD table (from DB2 table AUTHFRDS)
-- Module: app/app-authorization-ims-db2-mq/
-- Records flagged as fraudulent during authorization review
-- -----------------------------------------------------------------------------
CREATE TABLE auth_fraud (
    id                      BIGSERIAL       NOT NULL,
    card_num                VARCHAR(16)     NOT NULL,
    auth_timestamp          TIMESTAMP       NOT NULL,
    auth_type               VARCHAR(4),
    card_expiry_date        VARCHAR(4),
    message_type            VARCHAR(6),
    message_source          VARCHAR(6),
    auth_id_code            VARCHAR(6),
    auth_resp_code          VARCHAR(2),
    auth_resp_reason        VARCHAR(4),
    processing_code         VARCHAR(6),
    transaction_amount      DECIMAL(12, 2),
    approved_amount         DECIMAL(12, 2),
    merchant_category_code  VARCHAR(4),
    acquirer_country_code   VARCHAR(3),
    pos_entry_mode          INTEGER,
    merchant_id             VARCHAR(15),
    merchant_name           VARCHAR(22),
    merchant_city           VARCHAR(13),
    merchant_state          VARCHAR(2),
    merchant_zip            VARCHAR(9),
    transaction_id          VARCHAR(15),
    match_status            VARCHAR(1),
    auth_fraud              VARCHAR(1),
    fraud_report_date       DATE,
    acct_id                 BIGINT,
    cust_id                 BIGINT,
    CONSTRAINT pk_auth_fraud PRIMARY KEY (id),
    CONSTRAINT uk_auth_fraud_card_ts UNIQUE (card_num, auth_timestamp)
);

CREATE INDEX idx_auth_fraud_acct_id ON auth_fraud (acct_id);
CREATE INDEX idx_auth_fraud_cust_id ON auth_fraud (cust_id);

COMMENT ON TABLE auth_fraud IS 'Fraud tracking records (from DB2 table AUTHFRDS)';
