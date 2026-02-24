-- ============================================================
-- Flyway Migration V5: Create Card Cross-Reference Table
-- ============================================================
-- Replaces: CCXREF VSAM KSDS + CXACAIX Alternate Index (AIX)
-- Copybook: CVACT03Y.cpy (CARD-XREF-RECORD, 50 bytes)
-- FILLER:   14 bytes excluded (padding only, no business data)
--
-- This is the critical cross-reference table that enables the
-- bidirectional Account ID ↔ Card Number resolution used in
-- the Add Transaction (CT02) validation Phase 1.
--
-- Business Rules: BR-AT-04 (Must Exist), BR-AT-05 (Resolution)
--
-- VSAM Access Patterns Replaced:
--   Path A: EXEC CICS READ DATASET(CXACAIX) RIDFLD(XREF-ACCT-ID)
--           → SELECT ... WHERE account_id = ? (uses idx_xref_account_id)
--   Path B: EXEC CICS READ DATASET(CCXREF) RIDFLD(XREF-CARD-NUM)
--           → SELECT ... WHERE card_number = ? (uses PK index)
-- ============================================================

CREATE TABLE card_cross_reference (
    -- PK: XREF-CARD-NUM X(16) → also FK to card.card_number
    -- Replaces CCXREF KSDS primary key
    card_number         VARCHAR(16)     NOT NULL,

    -- FK: XREF-CUST-ID 9(09) → customer.customer_id
    customer_id         NUMERIC(9, 0)   NOT NULL,

    -- FK: XREF-ACCT-ID 9(11) → account.account_id
    -- This column is indexed to replace the CXACAIX Alternate Index
    account_id          NUMERIC(11, 0)  NOT NULL,

    -- Constraints
    CONSTRAINT card_cross_reference_pkey PRIMARY KEY (card_number),
    CONSTRAINT fk_xref_card FOREIGN KEY (card_number)
        REFERENCES card (card_number)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_xref_account FOREIGN KEY (account_id)
        REFERENCES account (account_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_xref_customer FOREIGN KEY (customer_id)
        REFERENCES customer (customer_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

-- ============================================================
-- CRITICAL INDEX: Replaces CXACAIX Alternate Index
-- ============================================================
-- This index is the PostgreSQL equivalent of the VSAM Alternate
-- Index (AIX) defined as CXACAIX on the CCXREF base cluster.
--
-- Legacy: EXEC CICS READ DATASET(CXACAIX) RIDFLD(XREF-ACCT-ID)
-- Modern: SELECT * FROM card_cross_reference WHERE account_id = ?
--
-- Without this index, Account ID → Card Number resolution would
-- require a full table scan instead of an indexed lookup.
-- ============================================================
CREATE INDEX idx_xref_account_id ON card_cross_reference (account_id);

-- Index on customer_id for customer-related lookups
CREATE INDEX idx_xref_customer_id ON card_cross_reference (customer_id);

-- Table comment
COMMENT ON TABLE card_cross_reference IS
    'Card cross-reference bridging Card, Account, and Customer. Replaces both CCXREF VSAM KSDS and CXACAIX Alternate Index (CVACT03Y.cpy, 50 bytes). BR-AT-04, BR-AT-05.';

COMMENT ON COLUMN card_cross_reference.card_number IS 'XREF-CARD-NUM X(16) — PK + FK to card. Replaces CCXREF KSDS primary key.';
COMMENT ON COLUMN card_cross_reference.account_id IS 'XREF-ACCT-ID 9(11) — FK to account. Indexed to replace CXACAIX AIX.';
COMMENT ON COLUMN card_cross_reference.customer_id IS 'XREF-CUST-ID 9(09) — FK to customer.';
