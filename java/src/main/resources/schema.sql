DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS card_xref;
DROP TABLE IF EXISTS category_balance;
DROP TABLE IF EXISTS disclosure_group;
DROP TABLE IF EXISTS daily_transaction;
DROP TABLE IF EXISTS posted_transaction;
DROP TABLE IF EXISTS rejected_transaction;
DROP TABLE IF EXISTS interest_transaction;

CREATE TABLE account (
  acct_id NUMERIC(11,0) PRIMARY KEY, acct_active_status VARCHAR(1), acct_curr_bal NUMERIC(12,2),
  acct_credit_limit NUMERIC(12,2), acct_cash_credit_limit NUMERIC(12,2), acct_open_date VARCHAR(10),
  acct_expiraion_date VARCHAR(10), acct_reissue_date VARCHAR(10), acct_curr_cyc_credit NUMERIC(12,2),
  acct_curr_cyc_debit NUMERIC(12,2), acct_addr_zip VARCHAR(10), acct_group_id VARCHAR(10));

CREATE TABLE card_xref (
  xref_card_num VARCHAR(16) PRIMARY KEY, xref_cust_id NUMERIC(9,0), xref_acct_id NUMERIC(11,0));

CREATE TABLE category_balance (
  trancat_acct_id NUMERIC(11,0), trancat_type_cd VARCHAR(2), trancat_cd NUMERIC(4,0), tran_cat_bal NUMERIC(11,2),
  PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd));

CREATE TABLE disclosure_group (
  dis_acct_group_id VARCHAR(10), dis_tran_type_cd VARCHAR(2), dis_tran_cat_cd NUMERIC(4,0), dis_int_rate NUMERIC(6,2),
  PRIMARY KEY (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd));

CREATE TABLE daily_transaction (
  seq NUMERIC(9,0) PRIMARY KEY, dalytran_id VARCHAR(16), dalytran_type_cd VARCHAR(2), dalytran_cat_cd NUMERIC(4,0),
  dalytran_source VARCHAR(10), dalytran_desc VARCHAR(100), dalytran_amt NUMERIC(11,2), dalytran_merchant_id NUMERIC(9,0),
  dalytran_merchant_name VARCHAR(50), dalytran_merchant_city VARCHAR(50), dalytran_merchant_zip VARCHAR(10),
  dalytran_card_num VARCHAR(16), dalytran_orig_ts VARCHAR(26), dalytran_proc_ts VARCHAR(26));

CREATE TABLE posted_transaction (
  tran_id VARCHAR(16) PRIMARY KEY, tran_type_cd VARCHAR(2), tran_cat_cd NUMERIC(4,0), tran_source VARCHAR(10),
  tran_desc VARCHAR(100), tran_amt NUMERIC(11,2), tran_merchant_id NUMERIC(9,0), tran_merchant_name VARCHAR(50),
  tran_merchant_city VARCHAR(50), tran_merchant_zip VARCHAR(10), tran_card_num VARCHAR(16), tran_orig_ts VARCHAR(26),
  tran_proc_ts VARCHAR(26));

CREATE TABLE interest_transaction (
  tran_id VARCHAR(16) PRIMARY KEY, tran_type_cd VARCHAR(2), tran_cat_cd NUMERIC(4,0), tran_source VARCHAR(10),
  tran_desc VARCHAR(100), tran_amt NUMERIC(11,2), tran_merchant_id NUMERIC(9,0), tran_merchant_name VARCHAR(50),
  tran_merchant_city VARCHAR(50), tran_merchant_zip VARCHAR(10), tran_card_num VARCHAR(16), tran_orig_ts VARCHAR(26),
  tran_proc_ts VARCHAR(26));

CREATE TABLE rejected_transaction (
  seq NUMERIC(9,0) PRIMARY KEY, dalytran_id VARCHAR(16), dalytran_type_cd VARCHAR(2), dalytran_cat_cd NUMERIC(4,0),
  dalytran_source VARCHAR(10), dalytran_desc VARCHAR(100), dalytran_amt NUMERIC(11,2), dalytran_merchant_id NUMERIC(9,0),
  dalytran_merchant_name VARCHAR(50), dalytran_merchant_city VARCHAR(50), dalytran_merchant_zip VARCHAR(10),
  dalytran_card_num VARCHAR(16), dalytran_orig_ts VARCHAR(26), dalytran_proc_ts VARCHAR(26),
  validation_fail_reason NUMERIC(4,0), validation_fail_reason_desc VARCHAR(76));

-- Deliberately retained across INTCALC runs to preserve promotion cycle state.
CREATE TABLE IF NOT EXISTS promotion_cycle (
  acct_id NUMERIC(11,0) NOT NULL, promo_group VARCHAR(10) NOT NULL, cycles_used INTEGER NOT NULL,
  PRIMARY KEY (acct_id, promo_group));
