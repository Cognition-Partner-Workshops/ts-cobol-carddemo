-- Transaction Types
INSERT INTO transaction_types (type_code, description) VALUES ('PR', 'Purchase');
INSERT INTO transaction_types (type_code, description) VALUES ('CR', 'Credit/Return');
INSERT INTO transaction_types (type_code, description) VALUES ('CA', 'Cash Advance');
INSERT INTO transaction_types (type_code, description) VALUES ('FE', 'Fee');
INSERT INTO transaction_types (type_code, description) VALUES ('IN', 'Interest');
INSERT INTO transaction_types (type_code, description) VALUES ('PM', 'Payment');

-- Transaction Categories
INSERT INTO transaction_categories (category_code, description) VALUES ('0001', 'Retail Purchase');
INSERT INTO transaction_categories (category_code, description) VALUES ('0002', 'Online Purchase');
INSERT INTO transaction_categories (category_code, description) VALUES ('0003', 'Travel');
INSERT INTO transaction_categories (category_code, description) VALUES ('0004', 'Dining');
INSERT INTO transaction_categories (category_code, description) VALUES ('0005', 'Fuel');
INSERT INTO transaction_categories (category_code, description) VALUES ('0006', 'Groceries');
INSERT INTO transaction_categories (category_code, description) VALUES ('0007', 'Entertainment');
INSERT INTO transaction_categories (category_code, description) VALUES ('0008', 'Utilities');
INSERT INTO transaction_categories (category_code, description) VALUES ('0009', 'Healthcare');
INSERT INTO transaction_categories (category_code, description) VALUES ('0010', 'Other');

-- Sample Transactions
INSERT INTO transactions (transaction_id, type_code, category_code, source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, card_number, account_id, original_timestamp, status, created_at)
VALUES ('2024010112000001', 'PR', '0001', 'POS', 'WALMART STORE #1234', 125.50, '123456789', 'WALMART', 'NEW YORK', '10001', '4111111111111111', '00000000001', '2024-01-01 12:00:00', 'POSTED', CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_id, type_code, category_code, source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, card_number, account_id, original_timestamp, status, created_at)
VALUES ('2024010214300002', 'PR', '0004', 'POS', 'STARBUCKS #5678', 15.75, '987654321', 'STARBUCKS', 'NEW YORK', '10002', '4111111111111111', '00000000001', '2024-01-02 14:30:00', 'POSTED', CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_id, type_code, category_code, source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, card_number, account_id, original_timestamp, status, created_at)
VALUES ('2024010309150003', 'PR', '0005', 'POS', 'SHELL GAS STATION', 45.00, '456789123', 'SHELL', 'BROOKLYN', '11201', '4111111111111111', '00000000001', '2024-01-03 09:15:00', 'POSTED', CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_id, type_code, category_code, source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, card_number, account_id, original_timestamp, status, created_at)
VALUES ('2024010416450004', 'PR', '0002', 'ONLINE', 'AMAZON.COM', 89.99, '111222333', 'AMAZON', 'SEATTLE', '98101', '5333333333333333', '00000000002', '2024-01-04 16:45:00', 'POSTED', CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_id, type_code, category_code, source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, card_number, account_id, original_timestamp, status, created_at)
VALUES ('2024010511200005', 'PM', '0010', 'ONLINE', 'PAYMENT - THANK YOU', -500.00, '', 'PAYMENT', '', '', '4111111111111111', '00000000001', '2024-01-05 11:20:00', 'POSTED', CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_id, type_code, category_code, source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, card_number, account_id, original_timestamp, status, created_at)
VALUES ('2024010618300006', 'CA', '0010', 'ATM', 'CASH ADVANCE - ATM', 200.00, '999888777', 'ATM WITHDRAWAL', 'LOS ANGELES', '90210', '5333333333333333', '00000000002', '2024-01-06 18:30:00', 'POSTED', CURRENT_TIMESTAMP);
