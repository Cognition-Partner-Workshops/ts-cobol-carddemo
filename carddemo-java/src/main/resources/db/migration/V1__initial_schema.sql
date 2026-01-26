-- CardDemo Java Migration - Initial Database Schema
-- Migrated from COBOL/VSAM/DB2 to PostgreSQL

-- Account Master Table (from CVACT01Y.cpy - 300 bytes)
CREATE TABLE accounts (
    acct_id BIGINT PRIMARY KEY,
    acct_active_status CHAR(1) NOT NULL DEFAULT 'Y',
    acct_curr_bal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    acct_credit_limit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    acct_cash_credit_limit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    acct_open_date DATE NOT NULL,
    acct_expiration_date DATE NOT NULL,
    acct_reissue_date DATE,
    acct_curr_cyc_credit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    acct_curr_cyc_debit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    acct_addr_zip VARCHAR(10),
    acct_group_id VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Customer Master Table (from CVCUS01Y.cpy - 500 bytes)
CREATE TABLE customers (
    cust_id BIGINT PRIMARY KEY,
    cust_first_name VARCHAR(25) NOT NULL,
    cust_middle_name VARCHAR(25),
    cust_last_name VARCHAR(25) NOT NULL,
    cust_addr_line_1 VARCHAR(50),
    cust_addr_line_2 VARCHAR(50),
    cust_addr_line_3 VARCHAR(50),
    cust_addr_state_cd CHAR(2),
    cust_addr_country_cd CHAR(3),
    cust_addr_zip VARCHAR(10),
    cust_phone_num_1 VARCHAR(15),
    cust_phone_num_2 VARCHAR(15),
    cust_ssn VARCHAR(9),
    cust_govt_issued_id VARCHAR(20),
    cust_dob DATE,
    cust_eft_account_id VARCHAR(10),
    cust_pri_card_holder_ind CHAR(1) DEFAULT 'N',
    cust_fico_credit_score INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Card Master Table (from CVACT02Y.cpy - 150 bytes)
CREATE TABLE cards (
    card_num VARCHAR(16) PRIMARY KEY,
    card_acct_id BIGINT NOT NULL REFERENCES accounts(acct_id),
    card_cvv_cd VARCHAR(3) NOT NULL,
    card_embossed_name VARCHAR(50),
    card_expiration_date DATE NOT NULL,
    card_active_status CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Card Cross-Reference Table (from CVACT03Y.cpy - 50 bytes)
CREATE TABLE card_xref (
    xref_card_num VARCHAR(16) PRIMARY KEY REFERENCES cards(card_num),
    xref_cust_id BIGINT NOT NULL REFERENCES customers(cust_id),
    xref_acct_id BIGINT NOT NULL REFERENCES accounts(acct_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transaction Master Table (from CVTRA05Y.cpy - 350 bytes)
CREATE TABLE transactions (
    tran_id VARCHAR(16) PRIMARY KEY,
    tran_type_cd CHAR(2) NOT NULL,
    tran_cat_cd INTEGER NOT NULL,
    tran_source VARCHAR(10),
    tran_desc VARCHAR(100),
    tran_amt DECIMAL(11, 2) NOT NULL,
    tran_merchant_id BIGINT,
    tran_merchant_name VARCHAR(50),
    tran_merchant_city VARCHAR(50),
    tran_merchant_zip VARCHAR(10),
    tran_card_num VARCHAR(16) NOT NULL REFERENCES cards(card_num),
    tran_orig_ts TIMESTAMP NOT NULL,
    tran_proc_ts TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Security Table (from CSUSR01Y.cpy - 80 bytes)
CREATE TABLE users (
    user_id VARCHAR(8) PRIMARY KEY,
    user_first_name VARCHAR(20) NOT NULL,
    user_last_name VARCHAR(20) NOT NULL,
    user_password VARCHAR(255) NOT NULL,
    user_type CHAR(1) NOT NULL DEFAULT 'U',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_type CHECK (user_type IN ('A', 'U'))
);

-- Transaction Category Balance Table (from CVTRA01Y.cpy)
CREATE TABLE tran_cat_balance (
    trancat_acct_id BIGINT NOT NULL REFERENCES accounts(acct_id),
    trancat_type_cd CHAR(2) NOT NULL,
    trancat_cd INTEGER NOT NULL,
    tran_cat_bal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd)
);

-- Daily Transaction Table (for batch processing)
CREATE TABLE daily_transactions (
    id BIGSERIAL PRIMARY KEY,
    tran_id VARCHAR(16) NOT NULL,
    tran_type_cd CHAR(2) NOT NULL,
    tran_cat_cd INTEGER NOT NULL,
    tran_source VARCHAR(10),
    tran_desc VARCHAR(100),
    tran_amt DECIMAL(11, 2) NOT NULL,
    tran_merchant_id BIGINT,
    tran_merchant_name VARCHAR(50),
    tran_merchant_city VARCHAR(50),
    tran_merchant_zip VARCHAR(10),
    tran_card_num VARCHAR(16) NOT NULL,
    tran_orig_ts TIMESTAMP NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rejected Transactions Table (for batch processing)
CREATE TABLE rejected_transactions (
    id BIGSERIAL PRIMARY KEY,
    tran_id VARCHAR(16) NOT NULL,
    tran_data TEXT NOT NULL,
    rejection_code INTEGER NOT NULL,
    rejection_reason VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transaction Backup Table (for TRANBKP job)
CREATE TABLE transaction_backups (
    id BIGSERIAL PRIMARY KEY,
    backup_date DATE NOT NULL,
    tran_id VARCHAR(16) NOT NULL,
    tran_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Authorization Module Tables (from app-authorization-ims-db2-mq)

-- Authorization Request Table
CREATE TABLE auth_requests (
    auth_id BIGSERIAL PRIMARY KEY,
    card_num VARCHAR(16) NOT NULL,
    tran_amt DECIMAL(11, 2) NOT NULL,
    merchant_id BIGINT,
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(50),
    merchant_zip VARCHAR(10),
    auth_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    auth_code VARCHAR(10),
    decline_reason VARCHAR(100),
    request_ts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_ts TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Fraud Detection Table (AUTHFRDS from DB2)
CREATE TABLE auth_fraud_detection (
    fraud_id BIGSERIAL PRIMARY KEY,
    card_num VARCHAR(16) NOT NULL,
    auth_id BIGINT REFERENCES auth_requests(auth_id),
    fraud_score DECIMAL(5, 2),
    fraud_indicators JSONB,
    is_fraud BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by VARCHAR(8),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transaction Type Management Tables (from app-transaction-type-db2)

-- Transaction Type Category Table
CREATE TABLE transaction_type_categories (
    category_id SERIAL PRIMARY KEY,
    category_code VARCHAR(10) NOT NULL UNIQUE,
    category_name VARCHAR(50) NOT NULL,
    category_desc VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transaction Type Table
CREATE TABLE transaction_types (
    type_id SERIAL PRIMARY KEY,
    type_code CHAR(2) NOT NULL UNIQUE,
    type_name VARCHAR(50) NOT NULL,
    type_desc VARCHAR(200),
    category_id INTEGER REFERENCES transaction_type_categories(category_id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Disclosure Groups Table (for DISCGRP job)
CREATE TABLE disclosure_groups (
    group_id SERIAL PRIMARY KEY,
    group_code VARCHAR(10) NOT NULL UNIQUE,
    group_name VARCHAR(50) NOT NULL,
    group_desc VARCHAR(200),
    effective_date DATE NOT NULL,
    expiration_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Account Disclosure Group Mapping
CREATE TABLE account_disclosure_groups (
    acct_id BIGINT NOT NULL REFERENCES accounts(acct_id),
    group_id INTEGER NOT NULL REFERENCES disclosure_groups(group_id),
    assigned_date DATE NOT NULL DEFAULT CURRENT_DATE,
    PRIMARY KEY (acct_id, group_id)
);

-- Interest Calculation Table (for INTCALC job)
CREATE TABLE interest_calculations (
    id BIGSERIAL PRIMARY KEY,
    acct_id BIGINT NOT NULL REFERENCES accounts(acct_id),
    calc_date DATE NOT NULL,
    principal_balance DECIMAL(12, 2) NOT NULL,
    interest_rate DECIMAL(5, 4) NOT NULL,
    interest_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Batch Job Execution Log
CREATE TABLE batch_job_log (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(50) NOT NULL,
    job_status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    records_processed INTEGER DEFAULT 0,
    records_rejected INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Account Extraction Queue (for MQ messaging)
CREATE TABLE account_extraction_queue (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL UNIQUE,
    acct_id BIGINT NOT NULL REFERENCES accounts(acct_id),
    request_type VARCHAR(20) NOT NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_data JSONB,
    response_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- Indexes for performance optimization
CREATE INDEX idx_accounts_status ON accounts(acct_active_status);
CREATE INDEX idx_accounts_expiration ON accounts(acct_expiration_date);
CREATE INDEX idx_accounts_group ON accounts(acct_group_id);

CREATE INDEX idx_customers_name ON customers(cust_last_name, cust_first_name);
CREATE INDEX idx_customers_ssn ON customers(cust_ssn);

CREATE INDEX idx_cards_acct ON cards(card_acct_id);
CREATE INDEX idx_cards_status ON cards(card_active_status);
CREATE INDEX idx_cards_expiration ON cards(card_expiration_date);

CREATE INDEX idx_xref_cust ON card_xref(xref_cust_id);
CREATE INDEX idx_xref_acct ON card_xref(xref_acct_id);

CREATE INDEX idx_transactions_card ON transactions(tran_card_num);
CREATE INDEX idx_transactions_date ON transactions(tran_orig_ts);
CREATE INDEX idx_transactions_type ON transactions(tran_type_cd);

CREATE INDEX idx_daily_tran_processed ON daily_transactions(processed);
CREATE INDEX idx_daily_tran_card ON daily_transactions(tran_card_num);

CREATE INDEX idx_rejected_tran_code ON rejected_transactions(rejection_code);

CREATE INDEX idx_auth_requests_card ON auth_requests(card_num);
CREATE INDEX idx_auth_requests_status ON auth_requests(auth_status);

CREATE INDEX idx_fraud_card ON auth_fraud_detection(card_num);
CREATE INDEX idx_fraud_reviewed ON auth_fraud_detection(reviewed);

CREATE INDEX idx_interest_calc_acct ON interest_calculations(acct_id);
CREATE INDEX idx_interest_calc_date ON interest_calculations(calc_date);

CREATE INDEX idx_batch_job_name ON batch_job_log(job_name);
CREATE INDEX idx_batch_job_status ON batch_job_log(job_status);
