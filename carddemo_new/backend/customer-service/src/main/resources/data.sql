-- Sample customers
INSERT INTO customers (customer_id, first_name, middle_name, last_name, address_line1, address_line2, state_code, country_code, zip_code, phone_number1, phone_number2, ssn, date_of_birth, primary_cardholder, fico_score, created_at, updated_at)
VALUES ('000000001', 'John', 'Michael', 'Smith', '123 Main Street', 'Apt 4B', 'NY', 'USA', '10001', '212-555-0101', '212-555-0102', '123456789', '1985-03-15', 'Y', 750, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO customers (customer_id, first_name, middle_name, last_name, address_line1, address_line2, state_code, country_code, zip_code, phone_number1, phone_number2, ssn, date_of_birth, primary_cardholder, fico_score, created_at, updated_at)
VALUES ('000000002', 'Jane', 'Elizabeth', 'Doe', '456 Oak Avenue', 'Suite 100', 'CA', 'USA', '90210', '310-555-0201', '310-555-0202', '987654321', '1990-07-22', 'Y', 800, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO customers (customer_id, first_name, middle_name, last_name, address_line1, address_line2, state_code, country_code, zip_code, phone_number1, phone_number2, ssn, date_of_birth, primary_cardholder, fico_score, created_at, updated_at)
VALUES ('000000003', 'Robert', 'James', 'Johnson', '789 Pine Road', '', 'TX', 'USA', '75001', '214-555-0301', '', '456789123', '1978-11-08', 'Y', 680, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
