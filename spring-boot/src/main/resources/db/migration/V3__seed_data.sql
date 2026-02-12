-- V3: Seed data for CardDemo application
-- Default users, transaction types, and transaction categories

-- Default admin user (password: PASSWORD, BCrypt encoded)
INSERT INTO app_user (user_id, password, first_name, last_name, user_type, enabled)
VALUES ('ADMIN001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System', 'Admin', 'A', TRUE);

-- Default regular user (password: PASSWORD, BCrypt encoded)
INSERT INTO app_user (user_id, password, first_name, last_name, user_type, enabled)
VALUES ('USER0001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Default', 'User', 'U', TRUE);

-- Transaction types from COBOL TRANTYPE data file
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('SA', 'Sale');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('RE', 'Return');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('CR', 'Credit');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('DB', 'Debit');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('PA', 'Payment');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('CA', 'Cash Advance');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('BA', 'Balance Transfer');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('FE', 'Fee');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('IN', 'Interest');
INSERT INTO transaction_type (type_cd, type_desc) VALUES ('AD', 'Adjustment');

-- Transaction categories from COBOL TRANCATG data file
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0001', 'SA', 'Retail Purchase');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0002', 'SA', 'Online Purchase');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0003', 'SA', 'Grocery');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0004', 'SA', 'Gas Station');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0005', 'SA', 'Restaurant');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0006', 'RE', 'Merchandise Return');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0007', 'CR', 'Payment Credit');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0008', 'DB', 'Annual Fee');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0009', 'PA', 'Minimum Payment');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0010', 'PA', 'Full Payment');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0011', 'CA', 'ATM Withdrawal');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0012', 'BA', 'Balance Transfer');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0013', 'FE', 'Late Fee');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0014', 'FE', 'Over Limit Fee');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0015', 'IN', 'Monthly Interest');
INSERT INTO transaction_category (cat_cd, cat_type_cd, cat_desc) VALUES ('0016', 'AD', 'Dispute Credit');
