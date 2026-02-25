-- Flyway migration V1: Initial schema for CardDemo Java migration
-- Migrated from VSAM KSDS files, DB2 tables, and IMS hierarchical DB

-- Users table (from USRSEC VSAM file, copybook CSUSR01Y)
CREATE TABLE IF NOT EXISTS users (
    user_id         VARCHAR(8)   NOT NULL PRIMARY KEY,
    first_name      VARCHAR(20),
    last_name       VARCHAR(20),
    password        VARCHAR(8)   NOT NULL,
    user_type       CHAR(1)      NOT NULL DEFAULT 'U'  -- 'A'=Admin, 'U'=User
);

-- Accounts table (from ACCTFILE VSAM KSDS, copybook CVACT01Y, 300-byte records)
CREATE TABLE IF NOT EXISTS accounts (
    acct_id              BIGINT       NOT NULL PRIMARY KEY,
    active_status        CHAR(1)      DEFAULT 'Y',
    current_balance      DECIMAL(14,2) DEFAULT 0,
    credit_limit         DECIMAL(14,2) DEFAULT 0,
    cash_credit_limit    DECIMAL(14,2) DEFAULT 0,
    open_date            VARCHAR(10),
    expiration_date      VARCHAR(10),
    reissue_date         VARCHAR(10),
    current_cycle_credit DECIMAL(14,2) DEFAULT 0,
    current_cycle_debit  DECIMAL(14,2) DEFAULT 0,
    address_zip          VARCHAR(10),
    group_id             VARCHAR(10)
);

-- Cards table (from CARDFILE VSAM KSDS, copybook CVACT02Y, 150-byte records)
CREATE TABLE IF NOT EXISTS cards (
    card_num        VARCHAR(16)  NOT NULL PRIMARY KEY,
    acct_id         BIGINT       NOT NULL,
    cvv_code        VARCHAR(4),
    embossed_name   VARCHAR(50),
    expiration_date VARCHAR(10),
    active_status   CHAR(1)      DEFAULT 'Y'
);

-- Customers table (from CUSTFILE VSAM KSDS, copybook CVCUS01Y, 500-byte records)
CREATE TABLE IF NOT EXISTS customers (
    cust_id           BIGINT       NOT NULL PRIMARY KEY,
    first_name        VARCHAR(25),
    middle_name       VARCHAR(25),
    last_name         VARCHAR(25),
    addr_line1        VARCHAR(50),
    addr_line2        VARCHAR(50),
    addr_line3        VARCHAR(50),
    addr_state_cd     VARCHAR(2),
    addr_country_cd   VARCHAR(3),
    addr_zip          VARCHAR(10),
    phone_num1        VARCHAR(15),
    phone_num2        VARCHAR(15),
    ssn               VARCHAR(9),
    govt_issued_id    VARCHAR(20),
    dob_yyyy_mm_dd    VARCHAR(10),
    eft_account_id    VARCHAR(10),
    pri_card_holder_ind VARCHAR(1),
    fico_credit_score INTEGER
);

-- Transactions table (from TRANSACT VSAM KSDS with AIX on CARD-NUM, copybook CVTRA05Y)
CREATE TABLE IF NOT EXISTS transactions (
    tran_id         VARCHAR(16)  NOT NULL PRIMARY KEY,
    tran_type_cd    VARCHAR(2),
    tran_cat_cd     INTEGER,
    tran_source     VARCHAR(10),
    tran_desc       VARCHAR(100),
    tran_amt        DECIMAL(14,2),
    merchant_id     BIGINT,
    merchant_name   VARCHAR(50),
    merchant_city   VARCHAR(50),
    merchant_zip    VARCHAR(10),
    card_num        VARCHAR(16),
    orig_timestamp  VARCHAR(26),
    proc_timestamp  VARCHAR(26)
);

-- Index on card_num replaces VSAM Alternate Index (AIX)
CREATE INDEX IF NOT EXISTS idx_transactions_card_num ON transactions(card_num);

-- Daily Transactions table (from CVTRA06Y, backup/daily file)
CREATE TABLE IF NOT EXISTS daily_transactions (
    tran_id         VARCHAR(16)  NOT NULL PRIMARY KEY,
    tran_type_cd    VARCHAR(2),
    tran_cat_cd     INTEGER,
    tran_source     VARCHAR(10),
    tran_desc       VARCHAR(100),
    tran_amt        DECIMAL(14,2),
    merchant_id     BIGINT,
    merchant_name   VARCHAR(50),
    merchant_city   VARCHAR(50),
    merchant_zip    VARCHAR(10),
    card_num        VARCHAR(16),
    orig_timestamp  VARCHAR(26),
    proc_timestamp  VARCHAR(26)
);

-- Card/Account Cross-reference table (from XREFFILE VSAM KSDS, copybook CVACT03Y)
CREATE TABLE IF NOT EXISTS card_account_xref (
    card_num        VARCHAR(16)  NOT NULL,
    cust_id         BIGINT       NOT NULL,
    acct_id         BIGINT       NOT NULL,
    PRIMARY KEY (card_num, cust_id, acct_id)
);

-- Transaction Types table (from TRANTYPE VSAM / DB2 CARDDEMO.TRANSACTION_TYPE)
CREATE TABLE IF NOT EXISTS transaction_types (
    type_cd         VARCHAR(2)   NOT NULL PRIMARY KEY,
    type_desc       VARCHAR(50)
);

-- Transaction Categories table (from TRANCATG VSAM / DB2 CARDDEMO.TRANSACTION_TYPE_CATEGORY)
CREATE TABLE IF NOT EXISTS transaction_categories (
    type_cd         VARCHAR(2)   NOT NULL,
    cat_cd          INTEGER      NOT NULL,
    cat_type_desc   VARCHAR(50),
    PRIMARY KEY (type_cd, cat_cd),
    CONSTRAINT fk_tran_cat_type FOREIGN KEY (type_cd)
        REFERENCES transaction_types(type_cd)
);

-- Disclosure Groups table (from DISCGRP VSAM, copybook CVTRA02Y)
CREATE TABLE IF NOT EXISTS disclosure_groups (
    acct_group_id   VARCHAR(10)  NOT NULL,
    tran_type_cd    VARCHAR(2)   NOT NULL,
    tran_cat_cd     INTEGER      NOT NULL,
    interest_rate   DECIMAL(7,4),
    PRIMARY KEY (acct_group_id, tran_type_cd, tran_cat_cd)
);

-- Transaction Category Balances table (from TCATBALF VSAM, copybook CVTRA01Y)
CREATE TABLE IF NOT EXISTS transaction_category_balances (
    acct_id         BIGINT       NOT NULL,
    type_cd         VARCHAR(2)   NOT NULL,
    cat_cd          INTEGER      NOT NULL,
    balance         DECIMAL(14,2) DEFAULT 0,
    PRIMARY KEY (acct_id, type_cd, cat_cd)
);

-- Authorization Summary table (from IMS PAUTSUM0 root segment in DBPAUTP0)
CREATE TABLE IF NOT EXISTS authorization_summary (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    acct_id             BIGINT,
    cust_id             BIGINT,
    auth_status         VARCHAR(1),
    account_status1     VARCHAR(20),
    account_status2     VARCHAR(20),
    account_status3     VARCHAR(20),
    account_status4     VARCHAR(20),
    account_status5     VARCHAR(20),
    credit_limit        DECIMAL(14,2),
    cash_limit          DECIMAL(14,2),
    credit_balance      DECIMAL(14,2),
    cash_balance        DECIMAL(14,2),
    approved_auth_cnt   INTEGER      DEFAULT 0,
    declined_auth_cnt   INTEGER      DEFAULT 0,
    approved_auth_amt   DECIMAL(14,2) DEFAULT 0,
    declined_auth_amt   DECIMAL(14,2) DEFAULT 0
);

-- Authorization Details table (from IMS PAUTDTL1 child segment, FK to authorization_summary)
CREATE TABLE IF NOT EXISTS authorization_details (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    summary_id              BIGINT,
    auth_date               VARCHAR(8),
    auth_time               VARCHAR(8),
    auth_orig_date          VARCHAR(8),
    auth_orig_time          VARCHAR(8),
    card_num                VARCHAR(16),
    auth_type               VARCHAR(4),
    card_expiry_date        VARCHAR(4),
    message_type            VARCHAR(4),
    message_source          VARCHAR(4),
    auth_id_code            VARCHAR(10),
    auth_resp_code          VARCHAR(2),
    auth_resp_reason        VARCHAR(4),
    processing_code         INTEGER,
    transaction_amt         DECIMAL(14,2),
    approved_amt            DECIMAL(14,2),
    merchant_category_code  VARCHAR(4),
    acqr_country_code       VARCHAR(3),
    pos_entry_mode          VARCHAR(4),
    merchant_id             VARCHAR(20),
    merchant_name           VARCHAR(50),
    merchant_city           VARCHAR(30),
    merchant_state          VARCHAR(2),
    merchant_zip            VARCHAR(10),
    transaction_id          VARCHAR(20),
    match_status            VARCHAR(1),
    auth_fraud              VARCHAR(1),
    fraud_rpt_date          VARCHAR(8),
    CONSTRAINT fk_auth_detail_summary FOREIGN KEY (summary_id)
        REFERENCES authorization_summary(id)
);

-- Fraud Records table (from DB2 AUTHFRDS table in authorization module)
CREATE TABLE IF NOT EXISTS fraud_records (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    card_num                VARCHAR(16),
    auth_ts                 TIMESTAMP,
    auth_type               VARCHAR(4),
    card_expiry_date        VARCHAR(4),
    message_type            VARCHAR(4),
    message_source          VARCHAR(4),
    auth_id_code            VARCHAR(10),
    auth_resp_code          VARCHAR(2),
    auth_resp_reason        VARCHAR(4),
    processing_code         INTEGER,
    transaction_amt         DECIMAL(14,2),
    approved_amt            DECIMAL(14,2),
    merchant_category_code  VARCHAR(4),
    acqr_country_code       VARCHAR(3),
    pos_entry_mode          VARCHAR(4),
    merchant_id             VARCHAR(20),
    merchant_name           VARCHAR(50),
    merchant_city           VARCHAR(30),
    merchant_state          VARCHAR(2),
    merchant_zip            VARCHAR(10),
    transaction_id          VARCHAR(20),
    match_status            VARCHAR(1),
    auth_fraud              VARCHAR(1),
    fraud_rpt_date          DATE,
    acct_id                 BIGINT,
    cust_id                 BIGINT
);

-- Seed data: Default admin user
INSERT INTO users (user_id, first_name, last_name, password, user_type)
VALUES ('ADMIN', 'ADMIN', 'USER', 'ADMIN', 'A');

-- Seed data: Default regular user
INSERT INTO users (user_id, first_name, last_name, password, user_type)
VALUES ('USER01', 'JOHN', 'DOE', 'USER01', 'U');
