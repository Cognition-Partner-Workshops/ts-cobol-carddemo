CREATE SCHEMA IF NOT EXISTS posting;
CREATE TABLE IF NOT EXISTS posting.account (
    acct_id BIGINT PRIMARY KEY, record TEXT NOT NULL,
    curr_bal NUMERIC(12,2) NOT NULL, credit_limit NUMERIC(12,2) NOT NULL,
    cyc_credit NUMERIC(12,2) NOT NULL, cyc_debit NUMERIC(12,2) NOT NULL,
    expiration VARCHAR(10) NOT NULL
);
CREATE TABLE IF NOT EXISTS posting.card_xref (
    card_num VARCHAR(16) COLLATE "C" PRIMARY KEY, acct_id BIGINT NOT NULL, record TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS posting.tran_cat_bal (
    acct_id BIGINT NOT NULL, type_cd VARCHAR(2) COLLATE "C" NOT NULL,
    cat_cd INTEGER NOT NULL, balance NUMERIC(11,2) NOT NULL, record TEXT NOT NULL,
    PRIMARY KEY (acct_id, type_cd, cat_cd)
);
CREATE TABLE IF NOT EXISTS posting.transaction_type (
    type_cd VARCHAR(2) COLLATE "C" PRIMARY KEY, description VARCHAR(50) NOT NULL
);
CREATE TABLE IF NOT EXISTS posting.transaction_category (
    type_cd VARCHAR(2) COLLATE "C" NOT NULL, cat_cd INTEGER NOT NULL,
    description VARCHAR(50) NOT NULL, PRIMARY KEY(type_cd, cat_cd)
);
CREATE TABLE IF NOT EXISTS posting.disclosure_group (
    group_id VARCHAR(10) COLLATE "C" NOT NULL, type_cd VARCHAR(2) COLLATE "C" NOT NULL,
    cat_cd INTEGER NOT NULL, record TEXT NOT NULL, PRIMARY KEY(group_id, type_cd, cat_cd)
);
CREATE TABLE IF NOT EXISTS posting.transaction_master (
    tran_id VARCHAR(16) COLLATE "C" PRIMARY KEY, card_num VARCHAR(16) COLLATE "C" NOT NULL,
    proc_date VARCHAR(10) NOT NULL, record TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS posting.daily_transaction (seq BIGINT PRIMARY KEY, record TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS posting.reject (seq BIGINT PRIMARY KEY, record TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS posting.report_input (
    seq BIGINT PRIMARY KEY, card_num VARCHAR(16) COLLATE "C" NOT NULL, record TEXT NOT NULL
);
