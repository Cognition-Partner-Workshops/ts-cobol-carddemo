-- ============================================================
-- Flyway Migration V3: Create Account Table
-- ============================================================
-- Replaces: ACCTDAT VSAM KSDS file
-- Copybook: CVACT01Y.cpy (ACCOUNT-RECORD, 300 bytes)
-- FILLER:   178 bytes excluded (padding only, no business data)
--
-- 12 data fields mapped from COBOL PIC clauses to PostgreSQL types.
-- All monetary fields use NUMERIC for precision (never float/double).
-- ============================================================

CREATE TABLE account (
    -- PK: ACCT-ID 9(11) — 11-digit numeric account identifier
    account_id          NUMERIC(11, 0)  NOT NULL,

    -- ACCT-ACTIVE-STATUS X(01) — 'Y' = active, 'N' = inactive
    active_status       VARCHAR(1)      NOT NULL,

    -- ACCT-CURR-BAL S9(10)V99 — signed, 10 integer + 2 decimal digits
    current_balance     NUMERIC(12, 2)  NOT NULL,

    -- ACCT-CREDIT-LIMIT S9(10)V99
    credit_limit        NUMERIC(12, 2)  NOT NULL,

    -- ACCT-CASH-CREDIT-LIMIT S9(10)V99
    cash_credit_limit   NUMERIC(12, 2)  NOT NULL,

    -- ACCT-OPEN-DATE X(10) — YYYY-MM-DD format
    open_date           DATE            NOT NULL,

    -- ACCT-EXPIRAION-DATE X(10) — note: legacy typo preserved in column comment
    expiration_date     DATE,

    -- ACCT-REISSUE-DATE X(10)
    reissue_date        DATE,

    -- ACCT-CURR-CYC-CREDIT S9(10)V99
    current_cycle_credit NUMERIC(12, 2) NOT NULL,

    -- ACCT-CURR-CYC-DEBIT S9(10)V99
    current_cycle_debit  NUMERIC(12, 2) NOT NULL,

    -- ACCT-ADDR-ZIP X(10)
    address_zip         VARCHAR(10),

    -- ACCT-GROUP-ID X(10)
    group_id            VARCHAR(10),

    -- Constraints
    CONSTRAINT account_pkey PRIMARY KEY (account_id)
);

-- Table comment
COMMENT ON TABLE account IS
    'Account master data. Replaces ACCTDAT VSAM KSDS (CVACT01Y.cpy, 300 bytes). 12 fields mapped; 178-byte FILLER excluded.';

COMMENT ON COLUMN account.account_id IS 'ACCT-ID 9(11) — Primary key, 11-digit numeric';
COMMENT ON COLUMN account.current_balance IS 'ACCT-CURR-BAL S9(10)V99 — Signed decimal, use BigDecimal in Java';
COMMENT ON COLUMN account.credit_limit IS 'ACCT-CREDIT-LIMIT S9(10)V99 — Signed decimal';
COMMENT ON COLUMN account.expiration_date IS 'ACCT-EXPIRAION-DATE X(10) — Note: legacy COBOL field has typo in name';
