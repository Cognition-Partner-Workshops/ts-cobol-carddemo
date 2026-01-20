-- Sample customers
INSERT INTO customers (customer_id, first_name, middle_name, last_name, address_line1, address_line2, state_code, country_code, zip_code, phone_number1, phone_number2, ssn, date_of_birth, primary_cardholder, fico_score, created_at, updated_at)
VALUES ('000000001', 'John', 'Michael', 'Smith', '123 Main Street', 'Apt 4B', 'NY', 'USA', '10001', '212-555-0101', '212-555-0102', '123456789', '1985-03-15', 'Y', 750, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO customers (customer_id, first_name, middle_name, last_name, address_line1, address_line2, state_code, country_code, zip_code, phone_number1, phone_number2, ssn, date_of_birth, primary_cardholder, fico_score, created_at, updated_at)
VALUES ('000000002', 'Jane', 'Elizabeth', 'Doe', '456 Oak Avenue', 'Suite 100', 'CA', 'USA', '90210', '310-555-0201', '310-555-0202', '987654321', '1990-07-22', 'Y', 800, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO customers (customer_id, first_name, middle_name, last_name, address_line1, address_line2, state_code, country_code, zip_code, phone_number1, phone_number2, ssn, date_of_birth, primary_cardholder, fico_score, created_at, updated_at)
VALUES ('000000003', 'Robert', 'James', 'Johnson', '789 Pine Road', '', 'TX', 'USA', '75001', '214-555-0301', '', '456789123', '1978-11-08', 'Y', 680, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample accounts
INSERT INTO accounts (account_id, active_status, current_balance, credit_limit, cash_credit_limit, open_date, expiration_date, current_cycle_credit, current_cycle_debit, group_id, customer_id, created_at, updated_at)
VALUES ('00000000001', 'Y', 1500.00, 10000.00, 2000.00, '2020-01-15', '2025-01-15', 500.00, 2000.00, 'STANDARD', '000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO accounts (account_id, active_status, current_balance, credit_limit, cash_credit_limit, open_date, expiration_date, current_cycle_credit, current_cycle_debit, group_id, customer_id, created_at, updated_at)
VALUES ('00000000002', 'Y', 3200.50, 15000.00, 3000.00, '2019-06-20', '2024-06-20', 1200.00, 4400.50, 'PREMIUM', '000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO accounts (account_id, active_status, current_balance, credit_limit, cash_credit_limit, open_date, expiration_date, current_cycle_credit, current_cycle_debit, group_id, customer_id, created_at, updated_at)
VALUES ('00000000003', 'Y', 750.25, 5000.00, 1000.00, '2021-03-10', '2026-03-10', 250.00, 1000.25, 'BASIC', '000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO accounts (account_id, active_status, current_balance, credit_limit, cash_credit_limit, open_date, expiration_date, current_cycle_credit, current_cycle_debit, group_id, customer_id, created_at, updated_at)
VALUES ('00000000004', 'N', 0.00, 8000.00, 1500.00, '2018-09-05', '2023-09-05', 0.00, 0.00, 'STANDARD', '000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
