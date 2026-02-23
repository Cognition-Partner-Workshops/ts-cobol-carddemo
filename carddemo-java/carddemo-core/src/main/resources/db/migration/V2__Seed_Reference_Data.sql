-- =============================================================================
-- CardDemo Reference Data Seed - V2
-- Populates lookup tables with initial reference data
-- =============================================================================

-- Transaction Types (from VSAM TRANTYPE file)
INSERT INTO transaction_type (type_code, description) VALUES ('SA', 'Sale');
INSERT INTO transaction_type (type_code, description) VALUES ('RT', 'Return');
INSERT INTO transaction_type (type_code, description) VALUES ('PM', 'Payment');
INSERT INTO transaction_type (type_code, description) VALUES ('CA', 'Cash Advance');
INSERT INTO transaction_type (type_code, description) VALUES ('FE', 'Fee');
INSERT INTO transaction_type (type_code, description) VALUES ('IN', 'Interest');
INSERT INTO transaction_type (type_code, description) VALUES ('AD', 'Adjustment');

-- Transaction Categories for Sale type
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('SA', 1, 'Retail Purchase');
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('SA', 2, 'Online Purchase');
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('SA', 3, 'Recurring Payment');
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('SA', 4, 'Pre-Authorization');

-- Transaction Categories for Return type
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('RT', 1, 'Merchandise Return');
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('RT', 2, 'Service Refund');

-- Transaction Categories for Fee type
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('FE', 1, 'Annual Fee');
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('FE', 2, 'Late Payment Fee');
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('FE', 3, 'Over Limit Fee');
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('FE', 4, 'Foreign Transaction Fee');

-- Transaction Categories for Interest type
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('IN', 1, 'Purchase Interest');
INSERT INTO transaction_category (type_code, category_code, description) VALUES ('IN', 2, 'Cash Advance Interest');

-- Default admin user (password: 'admin' encoded with BCrypt)
-- Original COBOL had plain text passwords; Java uses BCrypt
INSERT INTO user_security (usr_id, usr_first_name, usr_last_name, usr_password, usr_type)
VALUES ('ADMIN', 'System', 'Administrator',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'A');
