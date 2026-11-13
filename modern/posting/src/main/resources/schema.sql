DROP TABLE IF EXISTS records;

CREATE TABLE IF NOT EXISTS account (
    acct_id NUMERIC(11,0) PRIMARY KEY,
    acct_active_status CHAR(1) NOT NULL,
    acct_curr_bal NUMERIC(12,2) NOT NULL,
    acct_credit_limit NUMERIC(12,2) NOT NULL,
    acct_cash_credit_limit NUMERIC(12,2) NOT NULL,
    acct_open_date CHAR(10) NOT NULL,
    acct_expiraion_date CHAR(10) NOT NULL,
    acct_reissue_date CHAR(10) NOT NULL,
    acct_curr_cyc_credit NUMERIC(12,2) NOT NULL,
    acct_curr_cyc_debit NUMERIC(12,2) NOT NULL,
    acct_addr_zip CHAR(10) NOT NULL,
    acct_group_id CHAR(10) NOT NULL
);
CREATE TABLE IF NOT EXISTS card_xref (
    xref_card_num CHAR(16) PRIMARY KEY,
    xref_cust_id NUMERIC(9,0) NOT NULL,
    xref_acct_id NUMERIC(11,0) NOT NULL
);
CREATE INDEX IF NOT EXISTS card_xref_account ON card_xref (xref_acct_id);
CREATE TABLE IF NOT EXISTS tran_type (
    tran_type CHAR(2) PRIMARY KEY,
    tran_type_desc CHAR(50) NOT NULL
);
CREATE TABLE IF NOT EXISTS tran_category (
    tran_type_cd CHAR(2) NOT NULL,
    tran_cat_cd NUMERIC(4,0) NOT NULL,
    tran_cat_type_desc CHAR(50) NOT NULL,
    PRIMARY KEY (tran_type_cd, tran_cat_cd)
);
CREATE TABLE IF NOT EXISTS tran_cat_balance (
    trancat_acct_id NUMERIC(11,0) NOT NULL,
    trancat_type_cd CHAR(2) NOT NULL,
    trancat_cd NUMERIC(4,0) NOT NULL,
    tran_cat_bal NUMERIC(11,2) NOT NULL,
    PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd)
);
CREATE TABLE IF NOT EXISTS "transaction" (
    tran_id CHAR(16) PRIMARY KEY,
    tran_type_cd CHAR(2) NOT NULL,
    tran_cat_cd NUMERIC(4,0) NOT NULL,
    tran_source CHAR(10) NOT NULL,
    tran_desc CHAR(100) NOT NULL,
    tran_amt NUMERIC(11,2) NOT NULL,
    tran_merchant_id NUMERIC(9,0) NOT NULL,
    tran_merchant_name CHAR(50) NOT NULL,
    tran_merchant_city CHAR(50) NOT NULL,
    tran_merchant_zip CHAR(10) NOT NULL,
    tran_card_num CHAR(16) NOT NULL,
    tran_orig_ts CHAR(26) NOT NULL,
    tran_proc_ts CHAR(26) NOT NULL
);
