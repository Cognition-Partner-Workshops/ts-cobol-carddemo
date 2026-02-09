INSERT INTO users (user_id, first_name, last_name, password, user_type)
VALUES ('ADMIN', 'ADMIN', 'USER', 'ADMIN', 'A');

INSERT INTO users (user_id, first_name, last_name, password, user_type)
VALUES ('USER0001', 'FIRST01', 'LAST01', 'USER0001', 'U');

INSERT INTO transaction_types (type_cd, type_desc) VALUES ('01', 'Purchase');
INSERT INTO transaction_types (type_cd, type_desc) VALUES ('02', 'Return');
INSERT INTO transaction_types (type_cd, type_desc) VALUES ('03', 'Cash Advance');
INSERT INTO transaction_types (type_cd, type_desc) VALUES ('04', 'Balance Transfer');
INSERT INTO transaction_types (type_cd, type_desc) VALUES ('05', 'Payment');

INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('01', 5001, 'Retail');
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('01', 5002, 'Groceries');
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('01', 5003, 'Gas');
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('01', 5004, 'Travel');
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('01', 5005, 'Entertainment');
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('02', 5001, 'Retail Return');
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('03', 5010, 'ATM Withdrawal');
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('05', 5020, 'Online Payment');
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES ('05', 5021, 'Auto Payment');

INSERT INTO customers (cust_id, first_name, middle_name, last_name, addr_line_1, addr_state_cd, addr_country_cd, addr_zip, phone_num_1, ssn, dob, pri_card_holder, fico_credit_score)
VALUES (1, 'JOHN', 'M', 'DOE', '123 MAIN ST', 'NY', 'USA', '10001', '2125551234', 123456789, '1980-01-15', 'Y', 750);

INSERT INTO customers (cust_id, first_name, middle_name, last_name, addr_line_1, addr_state_cd, addr_country_cd, addr_zip, phone_num_1, ssn, dob, pri_card_holder, fico_credit_score)
VALUES (2, 'JANE', 'A', 'SMITH', '456 OAK AVE', 'CA', 'USA', '90001', '3105555678', 987654321, '1985-06-20', 'Y', 800);

INSERT INTO accounts (acct_id, active_status, curr_bal, credit_limit, cash_credit_limit, open_date, expiration_date, group_id)
VALUES (10000000001, 'Y', 1500.00, 10000.00, 2000.00, '2020-01-15', '2025-01-15', 'GRP001');

INSERT INTO accounts (acct_id, active_status, curr_bal, credit_limit, cash_credit_limit, open_date, expiration_date, group_id)
VALUES (10000000002, 'Y', 2500.00, 15000.00, 3000.00, '2021-03-20', '2026-03-20', 'GRP001');

INSERT INTO cards (card_num, acct_id, cvv_cd, embossed_name, expiration_date, active_status)
VALUES ('4111111111111111', 10000000001, 123, 'JOHN M DOE', '2025-01-15', 'Y');

INSERT INTO cards (card_num, acct_id, cvv_cd, embossed_name, expiration_date, active_status)
VALUES ('4222222222222222', 10000000002, 456, 'JANE A SMITH', '2026-03-20', 'Y');

INSERT INTO card_account_xref (card_num, cust_id, acct_id)
VALUES ('4111111111111111', 1, 10000000001);

INSERT INTO card_account_xref (card_num, cust_id, acct_id)
VALUES ('4222222222222222', 2, 10000000002);

INSERT INTO disclosure_groups (acct_group_id, tran_type_cd, tran_cat_cd, int_rate)
VALUES ('GRP001', '01', 5001, 18.99);
INSERT INTO disclosure_groups (acct_group_id, tran_type_cd, tran_cat_cd, int_rate)
VALUES ('GRP001', '01', 5002, 18.99);
INSERT INTO disclosure_groups (acct_group_id, tran_type_cd, tran_cat_cd, int_rate)
VALUES ('GRP001', '03', 5010, 24.99);

INSERT INTO transaction_category_balances (acct_id, type_cd, cat_cd, balance)
VALUES (10000000001, '01', 5001, 500.00);
INSERT INTO transaction_category_balances (acct_id, type_cd, cat_cd, balance)
VALUES (10000000001, '01', 5002, 200.00);
INSERT INTO transaction_category_balances (acct_id, type_cd, cat_cd, balance)
VALUES (10000000002, '01', 5001, 1000.00);
