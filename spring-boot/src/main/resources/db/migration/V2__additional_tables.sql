-- V2: Additional tables from COBOL data structures
-- transaction_type, transaction_category, category_balance, discount_group, card_xref

CREATE TABLE IF NOT EXISTS transaction_type (
    type_cd     VARCHAR(2)   NOT NULL PRIMARY KEY,
    type_desc   VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS transaction_category (
    cat_cd      VARCHAR(4)   NOT NULL PRIMARY KEY,
    cat_type_cd VARCHAR(2)   NOT NULL,
    cat_desc    VARCHAR(50)  NOT NULL,
    CONSTRAINT fk_category_type FOREIGN KEY (cat_type_cd) REFERENCES transaction_type (type_cd)
);

CREATE TABLE IF NOT EXISTS category_balance (
    acct_id     BIGINT         NOT NULL,
    cat_cd      VARCHAR(4)     NOT NULL,
    balance     DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_category_balance PRIMARY KEY (acct_id, cat_cd),
    CONSTRAINT fk_catbal_account FOREIGN KEY (acct_id) REFERENCES account (id),
    CONSTRAINT fk_catbal_category FOREIGN KEY (cat_cd) REFERENCES transaction_category (cat_cd)
);

CREATE TABLE IF NOT EXISTS discount_group (
    group_cd      VARCHAR(10)    NOT NULL PRIMARY KEY,
    interest_rate DECIMAL(5, 4)  NOT NULL
);

CREATE TABLE IF NOT EXISTS card_xref (
    card_number VARCHAR(16) NOT NULL PRIMARY KEY,
    acct_id     BIGINT      NOT NULL,
    cust_id     BIGINT      NOT NULL,
    CONSTRAINT fk_xref_card FOREIGN KEY (card_number) REFERENCES card (card_number),
    CONSTRAINT fk_xref_account FOREIGN KEY (acct_id) REFERENCES account (id),
    CONSTRAINT fk_xref_customer FOREIGN KEY (cust_id) REFERENCES customer (id)
);

CREATE INDEX idx_card_xref_acct ON card_xref (acct_id);
CREATE INDEX idx_card_xref_cust ON card_xref (cust_id);
CREATE INDEX idx_category_balance_cat ON category_balance (cat_cd);
