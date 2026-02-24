-- ============================================================
-- Flyway Migration V7: Seed Data for Development and Testing
-- ============================================================
-- Sample data enabling all 19 user stories to be tested.
-- Includes: customers, accounts, cards, cross-references,
-- and transactions with realistic CardDemo-style values.
--
-- This data supports:
-- - CT00 List: Multiple transactions for pagination testing
-- - CT01 View: Specific transactions to view by ID
-- - CT02 Add: Valid cross-reference records for resolution
-- ============================================================

-- ============================================================
-- Customers (CUSTDAT)
-- ============================================================
INSERT INTO customer (customer_id, first_name, middle_name, last_name, address_line_1, state_code, country_code, address_zip, phone_number_1, ssn, date_of_birth, primary_card_holder_ind, fico_credit_score)
VALUES
    (100000001, 'John',    'A',  'Smith',    '123 Main Street',      'NY', 'USA', '10001',     '212-555-0101', 123456789, '1985-03-15', 'Y', 750),
    (100000002, 'Jane',    'B',  'Doe',      '456 Oak Avenue',       'CA', 'USA', '90210',     '310-555-0202', 987654321, '1990-07-22', 'Y', 680),
    (100000003, 'Robert',  'C',  'Johnson',  '789 Pine Road',        'TX', 'USA', '75001',     '214-555-0303', 456789123, '1978-11-30', 'Y', 720),
    (100000004, 'Maria',   NULL, 'Garcia',   '321 Elm Boulevard',    'FL', 'USA', '33101',     '305-555-0404', 789123456, '1992-01-10', 'Y', 695),
    (100000005, 'David',   'E',  'Williams', '654 Maple Lane',       'IL', 'USA', '60601',     '312-555-0505', 321654987, '1988-09-05', 'N', 710);

-- ============================================================
-- Accounts (ACCTDAT)
-- ============================================================
INSERT INTO account (account_id, active_status, current_balance, credit_limit, cash_credit_limit, open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit, address_zip, group_id)
VALUES
    (00000000001, 'Y',  1250.75,  10000.00, 2000.00, '2020-01-15', '2027-01-15', NULL,         500.00,   750.25, '10001', 'GROUP001'),
    (00000000002, 'Y',  3500.00,  15000.00, 3000.00, '2019-06-20', '2026-06-20', '2023-06-20', 1200.00,  800.00, '90210', 'GROUP001'),
    (00000000003, 'Y',  -125.50,  8000.00,  1500.00, '2021-03-10', '2028-03-10', NULL,         0.00,     125.50, '75001', 'GROUP002'),
    (00000000004, 'N',  0.00,     5000.00,  1000.00, '2018-11-01', '2024-11-01', NULL,         0.00,     0.00,   '33101', 'GROUP002'),
    (00000000005, 'Y',  8750.25,  20000.00, 5000.00, '2022-08-15', '2029-08-15', NULL,         2500.00,  1500.00,'60601', 'GROUP003');

-- ============================================================
-- Cards (CARDDAT)
-- ============================================================
INSERT INTO card (card_number, account_id, cvv_code, embossed_name, expiration_date, active_status)
VALUES
    ('4111111111111111', 00000000001, 123, 'JOHN A SMITH',      '2027-01-15', 'Y'),
    ('4222222222222222', 00000000002, 456, 'JANE B DOE',        '2026-06-20', 'Y'),
    ('4333333333333333', 00000000003, 789, 'ROBERT C JOHNSON',  '2028-03-10', 'Y'),
    ('4444444444444444', 00000000004, 321, 'MARIA GARCIA',      '2024-11-01', 'N'),
    ('4555555555555555', 00000000005, 654, 'DAVID E WILLIAMS',  '2029-08-15', 'Y'),
    ('4111111111112222', 00000000001, 111, 'JOHN A SMITH',      '2027-01-15', 'Y');

-- ============================================================
-- Card Cross-References (CCXREF / CXACAIX)
-- ============================================================
-- These records enable the bidirectional resolution:
--   Path A: Account ID → Card Number (via idx_xref_account_id)
--   Path B: Card Number → Account ID (via PK)
--
-- Business Rules: BR-AT-04, BR-AT-05
-- ============================================================
INSERT INTO card_cross_reference (card_number, customer_id, account_id)
VALUES
    ('4111111111111111', 100000001, 00000000001),
    ('4222222222222222', 100000002, 00000000002),
    ('4333333333333333', 100000003, 00000000003),
    ('4444444444444444', 100000004, 00000000004),
    ('4555555555555555', 100000005, 00000000005),
    ('4111111111112222', 100000001, 00000000001);

-- ============================================================
-- Transactions (TRANSACT)
-- ============================================================
-- 15 sample transactions for pagination testing (10 per page = 2 pages)
-- and view/detail testing. IDs are left-padded to 16 characters.
--
-- Also set the sequence to start after the highest seeded ID.
-- ============================================================
INSERT INTO transaction (transaction_id, card_number, type_code, category_code, source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, origination_ts, processing_ts)
VALUES
    ('0000000000000001', '4111111111111111', '01', 5001, 'ONLINE',   'Monthly Subscription Service',       -14.99,   100000001, 'StreamFlix',        'Los Angeles',  '90001', '2024-01-01 10:00:00', '2024-01-01 10:00:05'),
    ('0000000000000002', '4111111111111111', '01', 5411, 'POS',      'Grocery Purchase',                   -87.32,   100000002, 'FreshMart',         'New York',     '10002', '2024-01-02 14:30:00', '2024-01-02 14:30:10'),
    ('0000000000000003', '4222222222222222', '01', 5812, 'POS',      'Restaurant Dinner',                  -62.50,   100000003, 'Golden Dragon',     'San Francisco','94102', '2024-01-03 19:15:00', '2024-01-03 19:15:08'),
    ('0000000000000004', '4222222222222222', '02', 5812, 'ONLINE',   'Restaurant Refund',                   62.50,   100000003, 'Golden Dragon',     'San Francisco','94102', '2024-01-04 09:00:00', '2024-01-04 09:00:03'),
    ('0000000000000005', '4333333333333333', '01', 5311, 'POS',      'Department Store Purchase',          -245.00,   100000004, 'MegaStore',         'Dallas',       '75201', '2024-01-05 11:20:00', '2024-01-05 11:20:15'),
    ('0000000000000006', '4333333333333333', '01', 5541, 'POS',      'Gas Station Fill-up',                -55.80,   100000005, 'QuickFuel',         'Dallas',       '75202', '2024-01-06 07:45:00', '2024-01-06 07:45:05'),
    ('0000000000000007', '4555555555555555', '01', 5999, 'ONLINE',   'Electronics Purchase',               -899.99,  100000006, 'TechZone',          'Chicago',      '60602', '2024-01-07 16:00:00', '2024-01-07 16:00:12'),
    ('0000000000000008', '4555555555555555', '01', 5122, 'POS',      'Pharmacy Purchase',                  -23.45,   100000007, 'HealthPharm',       'Chicago',      '60603', '2024-01-08 12:10:00', '2024-01-08 12:10:04'),
    ('0000000000000009', '4111111111112222', '01', 4111, 'POS',      'Train Ticket',                       -35.00,   100000008, 'CityRail',          'New York',     '10003', '2024-01-09 08:00:00', '2024-01-09 08:00:02'),
    ('0000000000000010', '4111111111112222', '01', 5814, 'ONLINE',   'Food Delivery Order',                -28.75,   100000009, 'QuickEats',         'New York',     '10004', '2024-01-10 20:30:00', '2024-01-10 20:30:07'),
    ('0000000000000011', '4111111111111111', '01', 5200, 'POS',      'Home Improvement Materials',         -156.80,  100000010, 'BuildRight',        'New York',     '10005', '2024-01-11 14:00:00', '2024-01-11 14:00:09'),
    ('0000000000000012', '4222222222222222', '01', 5651, 'POS',      'Clothing Purchase',                  -189.99,  100000011, 'FashionPlus',       'San Francisco','94103', '2024-01-12 15:45:00', '2024-01-12 15:45:11'),
    ('0000000000000013', '4333333333333333', '01', 5912, 'ONLINE',   'Online Pharmacy Refill',             -42.00,   100000012, 'PharmaDirect',      'Dallas',       '75203', '2024-01-13 10:30:00', '2024-01-13 10:30:06'),
    ('0000000000000014', '4555555555555555', '01', 5732, 'POS',      'Electronics Repair Service',         -75.00,   100000013, 'FixIt Electronics', 'Chicago',      '60604', '2024-01-14 11:00:00', '2024-01-14 11:00:08'),
    ('0000000000000015', '4111111111111111', '01', 5411, 'POS',      'Weekly Grocery Shopping',            -112.45,  100000014, 'FreshMart',         'New York',     '10002', '2024-01-15 09:00:00', '2024-01-15 09:00:04');

-- Reset sequence to start after highest seeded transaction ID
SELECT setval('transaction_id_seq', 15);
