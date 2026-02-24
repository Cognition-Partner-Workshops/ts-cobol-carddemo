-- ============================================================
-- Flyway Migration V2: Create Customer Table
-- ============================================================
-- Replaces: CUSTDAT VSAM KSDS file
-- Copybook: CVCUS01Y.cpy (CUSTOMER-RECORD, 500 bytes)
-- FILLER:   168 bytes excluded (padding only, no business data)
--
-- 18 data fields mapped from COBOL PIC clauses to PostgreSQL types.
-- ============================================================

CREATE TABLE customer (
    -- PK: CUST-ID 9(09) — 9-digit numeric customer identifier
    customer_id         NUMERIC(9, 0)   NOT NULL,

    -- CUST-FIRST-NAME X(25)
    first_name          VARCHAR(25)     NOT NULL,

    -- CUST-MIDDLE-NAME X(25)
    middle_name         VARCHAR(25),

    -- CUST-LAST-NAME X(25)
    last_name           VARCHAR(25)     NOT NULL,

    -- CUST-ADDR-LINE-1 X(50)
    address_line_1      VARCHAR(50),

    -- CUST-ADDR-LINE-2 X(50)
    address_line_2      VARCHAR(50),

    -- CUST-ADDR-LINE-3 X(50)
    address_line_3      VARCHAR(50),

    -- CUST-ADDR-STATE-CD X(02)
    state_code          VARCHAR(2),

    -- CUST-ADDR-COUNTRY-CD X(03)
    country_code        VARCHAR(3),

    -- CUST-ADDR-ZIP X(10)
    address_zip         VARCHAR(10),

    -- CUST-PHONE-NUM-1 X(15)
    phone_number_1      VARCHAR(15),

    -- CUST-PHONE-NUM-2 X(15)
    phone_number_2      VARCHAR(15),

    -- CUST-SSN 9(09) — unique Social Security Number
    ssn                 NUMERIC(9, 0),

    -- CUST-GOVT-ISSUED-ID X(20)
    government_issued_id VARCHAR(20),

    -- CUST-DOB-YYYY-MM-DD X(10)
    date_of_birth       DATE,

    -- CUST-EFT-ACCOUNT-ID X(10)
    eft_account_id      VARCHAR(10),

    -- CUST-PRI-CARD-HOLDER-IND X(01)
    primary_card_holder_ind VARCHAR(1),

    -- CUST-FICO-CREDIT-SCORE 9(03)
    fico_credit_score   NUMERIC(3, 0),

    -- Constraints
    CONSTRAINT customer_pkey PRIMARY KEY (customer_id)
);

-- Unique index on SSN
CREATE UNIQUE INDEX idx_customer_ssn ON customer (ssn)
    WHERE ssn IS NOT NULL;

-- Table and column comments for documentation
COMMENT ON TABLE customer IS
    'Customer master data. Replaces CUSTDAT VSAM KSDS (CVCUS01Y.cpy, 500 bytes). 18 fields mapped; 168-byte FILLER excluded.';

COMMENT ON COLUMN customer.customer_id IS 'CUST-ID 9(09) — Primary key, 9-digit numeric';
COMMENT ON COLUMN customer.ssn IS 'CUST-SSN 9(09) — Unique Social Security Number';
COMMENT ON COLUMN customer.fico_credit_score IS 'CUST-FICO-CREDIT-SCORE 9(03) — 3-digit FICO score';
