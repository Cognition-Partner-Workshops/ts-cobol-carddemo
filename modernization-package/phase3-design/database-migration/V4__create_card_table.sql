-- ============================================================
-- Flyway Migration V4: Create Card Table
-- ============================================================
-- Replaces: CARDDAT VSAM KSDS file
-- Copybook: CVACT02Y.cpy (CARD-RECORD, 150 bytes)
-- FILLER:   59 bytes excluded (padding only, no business data)
--
-- 6 data fields mapped from COBOL PIC clauses to PostgreSQL types.
-- Foreign key to account table (card belongs to account).
-- ============================================================

CREATE TABLE card (
    -- PK: CARD-NUM X(16) — 16-character card number
    card_number         VARCHAR(16)     NOT NULL,

    -- FK: CARD-ACCT-ID 9(11) → account.account_id
    account_id          NUMERIC(11, 0)  NOT NULL,

    -- CARD-CVV-CD 9(03) — 3-digit CVV code
    cvv_code            NUMERIC(3, 0)   NOT NULL,

    -- CARD-EMBOSSED-NAME X(50) — name printed on card
    embossed_name       VARCHAR(50)     NOT NULL,

    -- CARD-EXPIRAION-DATE X(10) — note: legacy typo preserved in column comment
    expiration_date     DATE            NOT NULL,

    -- CARD-ACTIVE-STATUS X(01) — 'Y' = active, 'N' = inactive
    active_status       VARCHAR(1)      NOT NULL,

    -- Constraints
    CONSTRAINT card_pkey PRIMARY KEY (card_number),
    CONSTRAINT fk_card_account FOREIGN KEY (account_id)
        REFERENCES account (account_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

-- Index on account_id for FK lookups and "cards by account" queries
CREATE INDEX idx_card_account_id ON card (account_id);

-- Table comment
COMMENT ON TABLE card IS
    'Card data. Replaces CARDDAT VSAM KSDS (CVACT02Y.cpy, 150 bytes). 6 fields mapped; 59-byte FILLER excluded.';

COMMENT ON COLUMN card.card_number IS 'CARD-NUM X(16) — Primary key, 16-character card number';
COMMENT ON COLUMN card.account_id IS 'CARD-ACCT-ID 9(11) — FK to account.account_id';
COMMENT ON COLUMN card.expiration_date IS 'CARD-EXPIRAION-DATE X(10) — Note: legacy COBOL field has typo in name';
