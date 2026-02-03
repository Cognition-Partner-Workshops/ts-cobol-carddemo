-- CardDemo Database Schema
-- V1: Initial table creation

-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(11) PRIMARY KEY,
    account_status VARCHAR(1) NOT NULL,
    current_balance DECIMAL(12, 2),
    credit_limit DECIMAL(12, 2),
    open_date DATE,
    expiration_date DATE,
    reissue_date DATE,
    current_cycle_credit DECIMAL(12, 2),
    current_cycle_debit DECIMAL(12, 2),
    group_id VARCHAR(10)
);

-- Customers table
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(9) PRIMARY KEY,
    first_name VARCHAR(25) NOT NULL,
    middle_name VARCHAR(25),
    last_name VARCHAR(25) NOT NULL,
    address_line1 VARCHAR(50),
    address_line2 VARCHAR(50),
    address_line3 VARCHAR(50),
    state_code VARCHAR(2),
    country_code VARCHAR(3),
    postal_code VARCHAR(10),
    phone_number1 VARCHAR(15),
    phone_number2 VARCHAR(15),
    ssn VARCHAR(9),
    government_id VARCHAR(20),
    date_of_birth DATE,
    fico_credit_score INTEGER
);

-- Cards table
CREATE TABLE IF NOT EXISTS cards (
    card_number VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(11) NOT NULL,
    card_cvv_code VARCHAR(3),
    card_embossed_name VARCHAR(50),
    card_expiry_date VARCHAR(10),
    card_active_status VARCHAR(1)
);

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(16) PRIMARY KEY,
    card_number VARCHAR(16) NOT NULL,
    transaction_type_code VARCHAR(2) NOT NULL,
    transaction_category_code VARCHAR(4),
    transaction_source VARCHAR(10),
    transaction_description VARCHAR(100),
    transaction_amount DECIMAL(12, 2),
    merchant_id VARCHAR(9),
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(50),
    merchant_zip VARCHAR(10),
    transaction_timestamp TIMESTAMP,
    original_timestamp TIMESTAMP
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(8) PRIMARY KEY,
    password VARCHAR(8) NOT NULL,
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    user_type VARCHAR(1) NOT NULL
);

-- Create indexes for better query performance
CREATE INDEX idx_accounts_status ON accounts(account_status);
CREATE INDEX idx_accounts_group ON accounts(group_id);
CREATE INDEX idx_customers_last_name ON customers(last_name);
CREATE INDEX idx_customers_state ON customers(state_code);
CREATE INDEX idx_cards_account ON cards(account_id);
CREATE INDEX idx_cards_status ON cards(card_active_status);
CREATE INDEX idx_transactions_card ON transactions(card_number);
CREATE INDEX idx_transactions_timestamp ON transactions(transaction_timestamp);
CREATE INDEX idx_transactions_merchant ON transactions(merchant_id);
CREATE INDEX idx_users_type ON users(user_type);
