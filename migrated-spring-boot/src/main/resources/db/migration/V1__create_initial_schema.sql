-- CardDemo Initial Schema Migration
-- Migrated from mainframe VSAM file structures
-- Version: 1.0.0

-- Accounts table (from CVACT01Y.cpy - ACCOUNT-RECORD)
CREATE TABLE accounts (
    account_id BIGINT PRIMARY KEY,
    active_status VARCHAR(1) NOT NULL,
    current_balance DECIMAL(12, 2) NOT NULL,
    credit_limit DECIMAL(12, 2) NOT NULL,
    cash_credit_limit DECIMAL(12, 2) NOT NULL,
    open_date DATE,
    expiration_date DATE,
    reissue_date DATE,
    current_cycle_credit DECIMAL(12, 2) NOT NULL,
    current_cycle_debit DECIMAL(12, 2) NOT NULL,
    address_zip VARCHAR(10),
    group_id VARCHAR(10)
);

-- Customers table (from CVCUS01Y.cpy - CUSTOMER-RECORD)
CREATE TABLE customers (
    customer_id BIGINT PRIMARY KEY,
    first_name VARCHAR(25) NOT NULL,
    middle_name VARCHAR(25),
    last_name VARCHAR(25) NOT NULL,
    address_line_1 VARCHAR(50),
    address_line_2 VARCHAR(50),
    address_line_3 VARCHAR(50),
    state_code VARCHAR(2),
    country_code VARCHAR(3),
    zip_code VARCHAR(10),
    phone_number_1 VARCHAR(15),
    phone_number_2 VARCHAR(15),
    ssn BIGINT,
    government_issued_id VARCHAR(20),
    date_of_birth DATE,
    eft_account_id VARCHAR(10),
    primary_card_holder_indicator VARCHAR(1),
    fico_credit_score INTEGER
);

-- Cards table (from card data structure)
CREATE TABLE cards (
    card_number VARCHAR(16) PRIMARY KEY,
    account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    cardholder_name VARCHAR(50),
    expiration_date DATE,
    active_status VARCHAR(1) NOT NULL
);

-- Transactions table (from CVTRA05Y.cpy - TRAN-RECORD)
CREATE TABLE transactions (
    transaction_id VARCHAR(16) PRIMARY KEY,
    type_code VARCHAR(2) NOT NULL,
    category_code INTEGER NOT NULL,
    source VARCHAR(10),
    description VARCHAR(100),
    amount DECIMAL(11, 2) NOT NULL,
    merchant_id BIGINT,
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(50),
    merchant_zip VARCHAR(10),
    card_number VARCHAR(16) NOT NULL,
    origination_timestamp TIMESTAMP,
    processing_timestamp TIMESTAMP
);

-- Transaction Category Balances table (from CVTRA01Y.cpy - TRAN-CAT-BAL-RECORD)
CREATE TABLE transaction_category_balances (
    account_id BIGINT NOT NULL,
    type_code VARCHAR(2) NOT NULL,
    category_code INTEGER NOT NULL,
    balance DECIMAL(11, 2) NOT NULL,
    PRIMARY KEY (account_id, type_code, category_code)
);

-- Users table (from CSUSR01Y.cpy - SEC-USER-DATA)
CREATE TABLE users (
    user_id VARCHAR(8) PRIMARY KEY,
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    password VARCHAR(8) NOT NULL,
    user_type VARCHAR(1) NOT NULL
);

-- Create indexes for common query patterns
CREATE INDEX idx_accounts_active_status ON accounts(active_status);
CREATE INDEX idx_accounts_group_id ON accounts(group_id);
CREATE INDEX idx_accounts_address_zip ON accounts(address_zip);

CREATE INDEX idx_customers_last_name ON customers(last_name);
CREATE INDEX idx_customers_state_code ON customers(state_code);
CREATE INDEX idx_customers_zip_code ON customers(zip_code);
CREATE INDEX idx_customers_ssn ON customers(ssn);

CREATE INDEX idx_cards_account_id ON cards(account_id);
CREATE INDEX idx_cards_customer_id ON cards(customer_id);
CREATE INDEX idx_cards_active_status ON cards(active_status);
CREATE INDEX idx_cards_expiration_date ON cards(expiration_date);

CREATE INDEX idx_transactions_card_number ON transactions(card_number);
CREATE INDEX idx_transactions_type_code ON transactions(type_code);
CREATE INDEX idx_transactions_category_code ON transactions(category_code);
CREATE INDEX idx_transactions_merchant_id ON transactions(merchant_id);
CREATE INDEX idx_transactions_origination_timestamp ON transactions(origination_timestamp);

CREATE INDEX idx_tcb_account_id ON transaction_category_balances(account_id);
CREATE INDEX idx_tcb_type_code ON transaction_category_balances(type_code);

CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_last_name ON users(last_name);
