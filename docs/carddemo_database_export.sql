-- =====================================================================
-- CardDemo Database Export for AWS RDS PostgreSQL
-- =====================================================================
-- This SQL file contains DDL statements and sample data for migrating
-- the CardDemo mainframe application from VSAM to PostgreSQL.
--
-- Source: aws-mainframe-modernization-carddemo
-- Target: AWS RDS PostgreSQL 14+
-- Generated: 2026-01-27
--
-- Usage:
--   psql -h <rds-endpoint> -U <username> -d <database> -f carddemo_database_export.sql
--
-- =====================================================================

-- Drop existing tables if they exist (in reverse dependency order)
DROP TABLE IF EXISTS transaction_category_balances CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS disclosure_groups CASCADE;
DROP TABLE IF EXISTS transaction_categories CASCADE;
DROP TABLE IF EXISTS transaction_types CASCADE;
DROP TABLE IF EXISTS card_account_xref CASCADE;
DROP TABLE IF EXISTS cards CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =====================================================================
-- Table: users
-- Source: USRSEC VSAM file (CSUSR01Y.cpy)
-- Description: User authentication and authorization
-- =====================================================================

CREATE TABLE users (
    user_id VARCHAR(8) PRIMARY KEY,
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type VARCHAR(1) NOT NULL CHECK (user_type IN ('A', 'U')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

COMMENT ON TABLE users IS 'User authentication and authorization data';
COMMENT ON COLUMN users.user_type IS 'A=Admin, U=User';

-- =====================================================================
-- Table: customers
-- Source: CUSTDATA VSAM file (CVCUS01Y.cpy)
-- Description: Customer master data
-- =====================================================================

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
    ssn VARCHAR(9),
    govt_issued_id VARCHAR(20),
    date_of_birth DATE,
    eft_account_id VARCHAR(10),
    primary_cardholder_ind VARCHAR(1) CHECK (primary_cardholder_ind IN ('Y', 'N')),
    fico_credit_score INTEGER CHECK (fico_credit_score BETWEEN 300 AND 850),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE customers IS 'Customer master data';
COMMENT ON COLUMN customers.ssn IS 'Social Security Number (encrypted in production)';
COMMENT ON COLUMN customers.fico_credit_score IS 'FICO credit score (300-850)';

CREATE INDEX idx_customers_ssn ON customers(ssn);
CREATE INDEX idx_customers_name ON customers(last_name, first_name);
CREATE INDEX idx_customers_zip ON customers(zip_code);

-- =====================================================================
-- Table: accounts
-- Source: ACCTDATA VSAM file (CVACT01Y.cpy)
-- Description: Credit card account information
-- =====================================================================

CREATE TABLE accounts (
    account_id BIGINT PRIMARY KEY,
    active_status VARCHAR(1) NOT NULL CHECK (active_status IN ('Y', 'N')),
    current_balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(12, 2) NOT NULL,
    cash_credit_limit DECIMAL(12, 2) NOT NULL,
    open_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    reissue_date DATE,
    current_cycle_credit DECIMAL(12, 2) DEFAULT 0.00,
    current_cycle_debit DECIMAL(12, 2) DEFAULT 0.00,
    address_zip VARCHAR(10),
    group_id VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE accounts IS 'Credit card account information';
COMMENT ON COLUMN accounts.active_status IS 'Y=Active, N=Inactive';
COMMENT ON COLUMN accounts.group_id IS 'Disclosure group ID for interest rates';

CREATE INDEX idx_accounts_status ON accounts(active_status);
CREATE INDEX idx_accounts_group ON accounts(group_id);
CREATE INDEX idx_accounts_expiration ON accounts(expiration_date);

-- =====================================================================
-- Table: cards
-- Source: CARDDATA VSAM file (CVACT02Y.cpy)
-- Description: Credit card details
-- =====================================================================

CREATE TABLE cards (
    card_number VARCHAR(16) PRIMARY KEY,
    account_id BIGINT NOT NULL,
    cvv_code VARCHAR(3) NOT NULL,
    embossed_name VARCHAR(50) NOT NULL,
    expiration_date DATE NOT NULL,
    active_status VARCHAR(1) NOT NULL CHECK (active_status IN ('Y', 'N')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

COMMENT ON TABLE cards IS 'Credit card details';
COMMENT ON COLUMN cards.cvv_code IS 'Card Verification Value (encrypted in production)';
COMMENT ON COLUMN cards.active_status IS 'Y=Active, N=Inactive';

CREATE INDEX idx_cards_account ON cards(account_id);
CREATE INDEX idx_cards_status ON cards(active_status);
CREATE INDEX idx_cards_expiration ON cards(expiration_date);

-- =====================================================================
-- Table: card_account_xref
-- Source: CARDXREF VSAM file (CVACT03Y.cpy)
-- Description: Cross-reference between cards, customers, and accounts
-- =====================================================================

CREATE TABLE card_account_xref (
    card_number VARCHAR(16) NOT NULL,
    customer_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (card_number, customer_id, account_id),
    FOREIGN KEY (card_number) REFERENCES cards(card_number),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

COMMENT ON TABLE card_account_xref IS 'Cross-reference between cards, customers, and accounts';

CREATE INDEX idx_xref_customer ON card_account_xref(customer_id);
CREATE INDEX idx_xref_account ON card_account_xref(account_id);

-- =====================================================================
-- Table: transaction_types
-- Source: TRANTYPE VSAM file (CVTRA03Y.cpy)
-- Description: Transaction type reference data
-- =====================================================================

CREATE TABLE transaction_types (
    transaction_type_code VARCHAR(2) PRIMARY KEY,
    description VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE transaction_types IS 'Transaction type reference data';

-- =====================================================================
-- Table: transaction_categories
-- Source: TRANCATG VSAM file (CVTRA04Y.cpy)
-- Description: Transaction category reference data
-- =====================================================================

CREATE TABLE transaction_categories (
    transaction_type_code VARCHAR(2) NOT NULL,
    transaction_category_code INTEGER NOT NULL,
    description VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_type_code, transaction_category_code),
    FOREIGN KEY (transaction_type_code) REFERENCES transaction_types(transaction_type_code)
);

COMMENT ON TABLE transaction_categories IS 'Transaction category reference data';

CREATE INDEX idx_trancat_type ON transaction_categories(transaction_type_code);

-- =====================================================================
-- Table: disclosure_groups
-- Source: DISCGRP VSAM file (CVTRA02Y.cpy)
-- Description: Interest rate disclosure groups
-- =====================================================================

CREATE TABLE disclosure_groups (
    account_group_id VARCHAR(10) NOT NULL,
    transaction_type_code VARCHAR(2) NOT NULL,
    transaction_category_code INTEGER NOT NULL,
    interest_rate DECIMAL(6, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_group_id, transaction_type_code, transaction_category_code),
    FOREIGN KEY (transaction_type_code, transaction_category_code) 
        REFERENCES transaction_categories(transaction_type_code, transaction_category_code)
);

COMMENT ON TABLE disclosure_groups IS 'Interest rate disclosure groups';
COMMENT ON COLUMN disclosure_groups.interest_rate IS 'Annual percentage rate (APR)';

CREATE INDEX idx_discgrp_group ON disclosure_groups(account_group_id);

-- =====================================================================
-- Table: transactions
-- Source: TRANSACT VSAM file (CVTRA05Y.cpy)
-- Description: Transaction records
-- =====================================================================

CREATE TABLE transactions (
    transaction_id VARCHAR(16) PRIMARY KEY,
    transaction_type_code VARCHAR(2) NOT NULL,
    transaction_category_code INTEGER NOT NULL,
    transaction_source VARCHAR(10),
    description VARCHAR(100),
    amount DECIMAL(11, 2) NOT NULL,
    merchant_id BIGINT,
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(50),
    merchant_zip VARCHAR(10),
    card_number VARCHAR(16) NOT NULL,
    origination_timestamp TIMESTAMP NOT NULL,
    processing_timestamp TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (card_number) REFERENCES cards(card_number),
    FOREIGN KEY (transaction_type_code, transaction_category_code) 
        REFERENCES transaction_categories(transaction_type_code, transaction_category_code)
);

COMMENT ON TABLE transactions IS 'Transaction records';
COMMENT ON COLUMN transactions.transaction_source IS 'Source of transaction (e.g., POS TERM, OPERATOR, ATM)';

CREATE INDEX idx_trans_card ON transactions(card_number);
CREATE INDEX idx_trans_type ON transactions(transaction_type_code, transaction_category_code);
CREATE INDEX idx_trans_orig_ts ON transactions(origination_timestamp);
CREATE INDEX idx_trans_merchant ON transactions(merchant_id);

-- =====================================================================
-- Table: transaction_category_balances
-- Source: TCATBALF VSAM file (CVTRA01Y.cpy)
-- Description: Transaction category balances by account
-- =====================================================================

CREATE TABLE transaction_category_balances (
    account_id BIGINT NOT NULL,
    transaction_type_code VARCHAR(2) NOT NULL,
    transaction_category_code INTEGER NOT NULL,
    balance DECIMAL(11, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, transaction_type_code, transaction_category_code),
    FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    FOREIGN KEY (transaction_type_code, transaction_category_code) 
        REFERENCES transaction_categories(transaction_type_code, transaction_category_code)
);

COMMENT ON TABLE transaction_category_balances IS 'Transaction category balances by account';

CREATE INDEX idx_tcatbal_account ON transaction_category_balances(account_id);

-- =====================================================================
-- SAMPLE DATA INSERTS
-- =====================================================================

-- Insert Users (sample data from USRSEC)
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type) VALUES
('ADMIN001', 'System', 'Administrator', '$2a$10$XYZ...', 'A'),
('USER0001', 'John', 'Smith', '$2a$10$ABC...', 'U'),
('USER0002', 'Jane', 'Doe', '$2a$10$DEF...', 'U');

-- Insert Transaction Types (from trantype.txt)
INSERT INTO transaction_types (transaction_type_code, description) VALUES
('01', 'Purchase'),
('02', 'Payment'),
('03', 'Credit'),
('04', 'Authorization'),
('05', 'Refund'),
('06', 'Reversal'),
('07', 'Adjustment');

-- Insert Transaction Categories (from trancatg.txt)
INSERT INTO transaction_categories (transaction_type_code, transaction_category_code, description) VALUES
('01', 1, 'Regular Sales Draft'),
('01', 2, 'Regular Cash Advance'),
('01', 3, 'Convenience Check Debit'),
('01', 4, 'ATM Cash Advance'),
('01', 5, 'Interest Amount'),
('02', 1, 'Cash payment'),
('02', 2, 'Electronic payment'),
('02', 3, 'Check payment'),
('03', 1, 'Credit to Account'),
('03', 2, 'Credit to Purchase balance'),
('03', 3, 'Credit to Cash balance'),
('04', 1, 'Zero dollar authorization'),
('04', 2, 'Online purchase authorization'),
('04', 3, 'Travel booking authorization'),
('05', 1, 'Refund credit'),
('06', 1, 'Fraud reversal'),
('06', 2, 'Non-fraud reversal'),
('07', 1, 'Sales draft credit adjustment');

-- Insert Customers (from custdata.txt - first 10 records)
INSERT INTO customers (customer_id, first_name, middle_name, last_name, address_line_1, address_line_2, address_line_3, state_code, country_code, zip_code, phone_number_1, phone_number_2, ssn, govt_issued_id, date_of_birth, eft_account_id, primary_cardholder_ind, fico_credit_score) VALUES
(1, 'Immanuel', 'Madeline', 'Kessler', '618 Deshaun Route', 'Apt. 802', 'Altenwerthshire', 'NC', 'USA', '12546', '(908)119-8310', '(373)693-8684', '020973888', '0000000000493684371', '1961-06-08', '0053581756', 'Y', 274),
(2, 'Enrico', 'April', 'Rosenbaum', '4917 Myrna Flats', 'Apt. 453', 'West Bernita', 'IN', 'USA', '22770', '(429)706-9510', '(744)950-5272', '587518382', '0000000005062103711', '1961-10-08', '0069194009', 'Y', 268),
(3, 'Larry', 'Cody', 'Homenick', '362 Esta Parks', 'Apt. 390', 'New Gladys', 'GA', 'USA', '19852-6716', '(950)396-9024', '(685)168-8826', '317460867', '0000000000524193031', '1987-11-30', '0006465789', 'Y', 616),
(4, 'Delbert', 'Kaia', 'Parisian', '638 Blanda Gateway', 'Apt. 076', 'Lake Virginie', 'MI', 'USA', '39035-0455', '(801)603-4121', '(156)074-6837', '660354258', '0000000000685792491', '1985-01-13', '0040802739', 'Y', 776),
(5, 'Treva', 'Manley', 'Schowalter', '5653 Legros Plaza', 'Apt. 968', 'Alvinaport', 'MI', 'USA', '02251-1698', '(978)775-4633', '(439)943-7644', '611264288', '0000000006397997541', '1971-09-29', '0006365573', 'Y', 529),
(6, 'Ignacio', 'Emery', 'Douglas', '3963 Yasmin Port', 'Suite 756', 'Port Josephstad', 'VI', 'USA', '46713-5148', '(277)743-4266', '(519)010-8739', '880329521', '0000000009755354961', '1994-11-29', '0067163009', 'Y', 753),
(7, 'Cooper', 'Dennis', 'Mayert', '6490 Zakary Locks', 'Apt. 765', 'Madieport', 'AL', 'USA', '34206-2974', '(698)282-4096', '(458)199-0016', '835138951', '0000000009590131701', '1977-05-06', '0024571415', 'Y', 499),
(8, 'Kelsie', 'Jordyn', 'Dicki', '0925 Welch Streets', 'Apt. 152', 'North Nanniestad', 'SC', 'USA', '27610', '(345)563-7159', '(443)197-1271', '295270759', '0000000001097469911', '1964-03-25', '0033132723', 'Y', 51),
(9, 'Melvin', 'Regan', 'Ondricka', '87893 Samson Flats', 'Apt. 135', 'New Braden', 'VI', 'USA', '21113', '(035)456-1404', '(412)440-3130', '842035847', '0000000005682994511', '1975-11-07', '0039446039', 'Y', 699),
(10, 'Maybell', 'Creola', 'Mann', '77933 Adah Dale', 'Suite 343', 'Andersonfurt', 'CT', 'USA', '44803-4279', '(614)594-2619', '(667)057-0235', '754755746', '0000000002128247551', '1980-06-11', '0093803568', 'Y', 476);

-- Insert Accounts (from acctdata.txt - first 10 records)
INSERT INTO accounts (account_id, active_status, current_balance, credit_limit, cash_credit_limit, open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit, address_zip, group_id) VALUES
(1, 'Y', 1940.00, 20200.00, 10200.00, '2014-11-20', '2025-05-20', '2025-05-20', 0.00, 0.00, 'A000000000', 'A000000000'),
(2, 'Y', 1580.00, 61300.00, 54480.00, '2013-06-19', '2024-08-11', '2024-08-11', 0.00, 0.00, 'A000000000', 'A000000000'),
(3, 'Y', 1470.00, 49090.00, 5380.00, '2013-08-23', '2024-01-10', '2024-01-10', 0.00, 0.00, 'A000000000', 'A000000000'),
(4, 'Y', 400.00, 35030.00, 27890.00, '2012-11-17', '2023-12-16', '2023-12-16', 0.00, 0.00, 'A000000000', 'A000000000'),
(5, 'Y', 3450.00, 38190.00, 24300.00, '2012-10-03', '2025-03-09', '2025-03-09', 0.00, 0.00, 'A000000000', 'A000000000'),
(6, 'Y', 2180.00, 35840.00, 29480.00, '2017-12-23', '2025-10-08', '2025-10-08', 0.00, 0.00, 'A000000000', 'A000000000'),
(7, 'Y', 1930.00, 20650.00, 2640.00, '2012-10-12', '2024-12-13', '2024-12-13', 0.00, 0.00, 'A000000000', 'A000000000'),
(8, 'Y', 6050.00, 61040.00, 13180.00, '2012-01-04', '2024-05-20', '2024-05-20', 0.00, 0.00, 'A000000000', 'A000000000'),
(9, 'Y', 5600.00, 82010.00, 20650.00, '2016-08-27', '2024-12-27', '2024-12-27', 0.00, 0.00, 'A000000000', 'A000000000'),
(10, 'Y', 1590.00, 54010.00, 44420.00, '2015-09-13', '2023-01-27', '2023-01-27', 0.00, 0.00, 'A000000000', 'A000000000');

-- Insert Cards (from carddata.txt - first 10 records)
INSERT INTO cards (card_number, account_id, cvv_code, embossed_name, expiration_date, active_status) VALUES
('0500024453765740', 50, '747', 'Aniya Von', '2023-03-09', 'Y'),
('0683586198171516', 27, '567', 'Ward Jones', '2025-07-13', 'Y'),
('0923877193247330', 2, '028', 'Enrico Rosenbaum', '2024-08-11', 'Y'),
('0927987108636232', 20, '003', 'Carter Veum', '2024-03-13', 'Y'),
('0982496213629795', 12, '075', 'Maci Robel', '2023-07-07', 'Y'),
('1014086565224350', 44, '640', 'Irving Emard', '2024-01-17', 'Y'),
('1142167692878931', 37, '625', 'Shany Walker', '2023-10-24', 'Y'),
('1561409106491600', 35, '031', 'Angelica Dach', '2025-09-23', 'Y'),
('2745303720002090', 39, '033', 'Aliyah Berge', '2025-09-08', 'Y'),
('2760836797107565', 24, '859', 'Stefanie Dickinson', '2025-02-11', 'Y');

-- Insert Card-Account-Customer Cross-Reference (from cardxref.txt - first 10 records)
INSERT INTO card_account_xref (card_number, customer_id, account_id) VALUES
('0500024453765740', 5, 50),
('0683586198171516', 27, 27),
('0923877193247330', 2, 2),
('0927987108636232', 20, 20),
('0982496213629795', 12, 12),
('1014086565224350', 44, 44),
('1142167692878931', 37, 37),
('1561409106491600', 35, 35),
('2745303720002090', 39, 39),
('2760836797107565', 24, 24);

-- Insert Disclosure Groups (from discgrp.txt - sample records)
INSERT INTO disclosure_groups (account_group_id, transaction_type_code, transaction_category_code, interest_rate) VALUES
('A000000000', '01', 1, 1.50),
('A000000000', '01', 2, 2.50),
('A000000000', '01', 3, 2.50),
('A000000000', '01', 4, 2.50),
('A000000000', '02', 1, 0.00),
('A000000000', '02', 2, 0.00),
('A000000000', '02', 3, 0.00),
('A000000000', '03', 1, 0.00),
('A000000000', '03', 2, 0.00),
('A000000000', '03', 3, 0.00),
('DEFAULT', '01', 1, 1.50),
('DEFAULT', '01', 2, 2.50),
('DEFAULT', '01', 3, 2.50),
('DEFAULT', '01', 4, 2.50),
('DEFAULT', '02', 1, 0.00),
('DEFAULT', '02', 2, 0.00),
('DEFAULT', '02', 3, 0.00),
('DEFAULT', '03', 1, 0.00),
('DEFAULT', '03', 2, 0.00),
('DEFAULT', '03', 3, 0.00),
('ZEROAPR', '01', 1, 0.00),
('ZEROAPR', '01', 2, 0.00),
('ZEROAPR', '01', 3, 0.00),
('ZEROAPR', '01', 4, 0.00);

-- Insert Transaction Category Balances (from tcatbal.txt - first 10 records)
INSERT INTO transaction_category_balances (account_id, transaction_type_code, transaction_category_code, balance) VALUES
(1, '01', 1, 0.00),
(2, '01', 1, 0.00),
(3, '01', 1, 0.00),
(4, '01', 1, 0.00),
(5, '01', 1, 0.00),
(6, '01', 1, 0.00),
(7, '01', 1, 0.00),
(8, '01', 1, 0.00),
(9, '01', 1, 0.00),
(10, '01', 1, 0.00);

-- Insert Sample Transactions (from dailytran.txt - first 5 records)
INSERT INTO transactions (transaction_id, transaction_type_code, transaction_category_code, transaction_source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, card_number, origination_timestamp, processing_timestamp) VALUES
('0000000000683580', '01', 1, 'POS TERM', 'Purchase at Abshire-Lowe', 5047.00, 800000000, 'Abshire-Lowe', 'North Enoshaven', '72112', '4859452612877065', '2022-06-10 19:27:53', NULL),
('0000000001774260', '03', 1, 'OPERATOR', 'Return item at Nitzsche, Nicolas and Lowe', -9190.00, 800000000, 'Nitzsche, Nicolas and Lowe', 'Fidelshire', '53378', '0927987108636232', '2022-06-10 19:27:53', NULL),
('0000000006292564', '01', 1, 'POS TERM', 'Purchase at Ernser, Roob and Gleason', 678.80, 800000000, 'Ernser, Roob and Gleason', 'North Makenziemouth', '78487-7965', '6009619150674526', '2022-06-10 19:27:53', NULL),
('0000000009101861', '01', 1, 'POS TERM', 'Purchase at Guann LLC', 2817.00, 800000000, 'Guann LLC', 'South Lynn', '51508-9166', '8040580410348680', '2022-06-10 19:27:53', NULL),
('0000000010142252', '01', 1, 'POS TERM', 'Purchase at Kertzmann-Schoen', 4546.60, 800000000, 'Kertzmann-Schoen', 'East Eulahstad', '98754-1089', '5656830544981216', '2022-06-10 19:27:53', NULL);

-- =====================================================================
-- CREATE VIEWS FOR COMMON QUERIES
-- =====================================================================

-- View: Customer Account Summary
CREATE OR REPLACE VIEW v_customer_account_summary AS
SELECT 
    c.customer_id,
    c.first_name,
    c.last_name,
    c.ssn,
    a.account_id,
    a.active_status,
    a.current_balance,
    a.credit_limit,
    a.expiration_date,
    COUNT(DISTINCT cd.card_number) as card_count
FROM customers c
JOIN card_account_xref x ON c.customer_id = x.customer_id
JOIN accounts a ON x.account_id = a.account_id
LEFT JOIN cards cd ON x.card_number = cd.card_number
GROUP BY c.customer_id, c.first_name, c.last_name, c.ssn, 
         a.account_id, a.active_status, a.current_balance, 
         a.credit_limit, a.expiration_date;

COMMENT ON VIEW v_customer_account_summary IS 'Customer account summary with card counts';

-- View: Transaction History
CREATE OR REPLACE VIEW v_transaction_history AS
SELECT 
    t.transaction_id,
    t.card_number,
    c.customer_id,
    cust.first_name || ' ' || cust.last_name as customer_name,
    a.account_id,
    t.transaction_type_code,
    tt.description as transaction_type,
    t.transaction_category_code,
    tc.description as transaction_category,
    t.amount,
    t.merchant_name,
    t.merchant_city,
    t.origination_timestamp,
    t.description
FROM transactions t
JOIN cards cd ON t.card_number = cd.card_number
JOIN card_account_xref x ON cd.card_number = x.card_number
JOIN customers cust ON x.customer_id = cust.customer_id
JOIN accounts a ON x.account_id = a.account_id
JOIN transaction_types tt ON t.transaction_type_code = tt.transaction_type_code
JOIN transaction_categories tc ON t.transaction_type_code = tc.transaction_type_code 
    AND t.transaction_category_code = tc.transaction_category_code;

COMMENT ON VIEW v_transaction_history IS 'Complete transaction history with customer and account details';

-- View: Account Balance Summary
CREATE OR REPLACE VIEW v_account_balance_summary AS
SELECT 
    a.account_id,
    a.current_balance,
    a.credit_limit,
    a.credit_limit - a.current_balance as available_credit,
    COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0) as total_charges,
    COALESCE(SUM(CASE WHEN t.amount < 0 THEN ABS(t.amount) ELSE 0 END), 0) as total_payments,
    COUNT(t.transaction_id) as transaction_count
FROM accounts a
LEFT JOIN cards c ON a.account_id = c.account_id
LEFT JOIN transactions t ON c.card_number = t.card_number
    AND t.origination_timestamp >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY a.account_id, a.current_balance, a.credit_limit;

COMMENT ON VIEW v_account_balance_summary IS 'Account balance summary with 30-day transaction totals';

-- =====================================================================
-- CREATE FUNCTIONS FOR BUSINESS LOGIC
-- =====================================================================

-- Function: Calculate Available Credit
CREATE OR REPLACE FUNCTION calculate_available_credit(p_account_id BIGINT)
RETURNS DECIMAL(12, 2) AS $$
DECLARE
    v_credit_limit DECIMAL(12, 2);
    v_current_balance DECIMAL(12, 2);
BEGIN
    SELECT credit_limit, current_balance
    INTO v_credit_limit, v_current_balance
    FROM accounts
    WHERE account_id = p_account_id;
    
    RETURN v_credit_limit - v_current_balance;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_available_credit IS 'Calculate available credit for an account';

-- Function: Update Account Balance
CREATE OR REPLACE FUNCTION update_account_balance()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE accounts
    SET current_balance = current_balance + NEW.amount,
        updated_at = CURRENT_TIMESTAMP
    WHERE account_id = (
        SELECT account_id 
        FROM cards 
        WHERE card_number = NEW.card_number
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Update account balance on transaction insert
CREATE TRIGGER trg_update_account_balance
AFTER INSERT ON transactions
FOR EACH ROW
EXECUTE FUNCTION update_account_balance();

COMMENT ON TRIGGER trg_update_account_balance ON transactions IS 'Automatically update account balance when transaction is inserted';

-- =====================================================================
-- GRANT PERMISSIONS (adjust as needed for your environment)
-- =====================================================================

-- Create application user role (uncomment and adjust as needed)
-- CREATE ROLE carddemo_app WITH LOGIN PASSWORD 'your_secure_password';
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO carddemo_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO carddemo_app;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO carddemo_app;

-- =====================================================================
-- VERIFICATION QUERIES
-- =====================================================================

-- Verify record counts
SELECT 'users' as table_name, COUNT(*) as record_count FROM users
UNION ALL
SELECT 'customers', COUNT(*) FROM customers
UNION ALL
SELECT 'accounts', COUNT(*) FROM accounts
UNION ALL
SELECT 'cards', COUNT(*) FROM cards
UNION ALL
SELECT 'card_account_xref', COUNT(*) FROM card_account_xref
UNION ALL
SELECT 'transaction_types', COUNT(*) FROM transaction_types
UNION ALL
SELECT 'transaction_categories', COUNT(*) FROM transaction_categories
UNION ALL
SELECT 'disclosure_groups', COUNT(*) FROM disclosure_groups
UNION ALL
SELECT 'transaction_category_balances', COUNT(*) FROM transaction_category_balances
UNION ALL
SELECT 'transactions', COUNT(*) FROM transactions
ORDER BY table_name;

-- =====================================================================
-- END OF EXPORT
-- =====================================================================

-- Notes:
-- 1. Password hashes in users table are placeholders - implement proper bcrypt hashing
-- 2. SSN and CVV fields should be encrypted in production using pgcrypto or AWS KMS
-- 3. Adjust GRANT statements based on your security requirements
-- 4. Consider partitioning the transactions table for better performance
-- 5. Review and adjust indexes based on your query patterns
-- 6. Set up regular VACUUM and ANALYZE jobs for optimal performance
-- 7. Configure connection pooling (e.g., PgBouncer) for production use
-- 8. Enable AWS RDS automated backups and point-in-time recovery
-- 9. Set up CloudWatch alarms for database metrics
-- 10. Review and implement row-level security policies as needed
