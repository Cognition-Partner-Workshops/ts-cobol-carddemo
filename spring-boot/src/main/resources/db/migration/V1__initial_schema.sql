-- CardDemo Initial Schema
-- This migration establishes the foundational tables for the CardDemo application,
-- mirroring the data structures from the original COBOL/CICS mainframe system.

CREATE TABLE IF NOT EXISTS customer (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name      VARCHAR(25)  NOT NULL,
    middle_name     VARCHAR(25),
    last_name       VARCHAR(25)  NOT NULL,
    address_line_1  VARCHAR(50)  NOT NULL,
    address_line_2  VARCHAR(50),
    city            VARCHAR(30)  NOT NULL,
    state           VARCHAR(2)   NOT NULL,
    zip_code        VARCHAR(10)  NOT NULL,
    country_code    VARCHAR(3)   NOT NULL DEFAULT 'US',
    phone_number    VARCHAR(15),
    ssn             VARCHAR(11)  NOT NULL,
    fico_score      SMALLINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_ssn UNIQUE (ssn)
);

CREATE TABLE IF NOT EXISTS account (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id         BIGINT          NOT NULL,
    account_status      VARCHAR(1)      NOT NULL DEFAULT 'A',
    credit_limit        DECIMAL(12, 2)  NOT NULL,
    current_balance     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    cash_credit_limit   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    open_date           DATE            NOT NULL,
    expiration_date     DATE            NOT NULL,
    reissue_date        DATE,
    group_id            VARCHAR(10),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_account_status CHECK (account_status IN ('A', 'C', 'S'))
);

CREATE TABLE IF NOT EXISTS card (
    id              BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id      BIGINT      NOT NULL,
    card_number     VARCHAR(16) NOT NULL,
    card_status     VARCHAR(1)  NOT NULL DEFAULT 'A',
    embossed_name   VARCHAR(50) NOT NULL,
    cvv_code        VARCHAR(4)  NOT NULL,
    issued_date     DATE        NOT NULL,
    expiry_date     DATE        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT uq_card_number UNIQUE (card_number),
    CONSTRAINT chk_card_status CHECK (card_status IN ('A', 'C', 'L'))
);

CREATE TABLE IF NOT EXISTS transaction_record (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_number         VARCHAR(16)     NOT NULL,
    transaction_type    VARCHAR(2)      NOT NULL,
    transaction_category VARCHAR(25),
    transaction_source  VARCHAR(10)     NOT NULL,
    amount              DECIMAL(12, 2)  NOT NULL,
    merchant_id         VARCHAR(20),
    merchant_name       VARCHAR(50),
    merchant_city       VARCHAR(30),
    merchant_zip        VARCHAR(10),
    timestamp           TIMESTAMP       NOT NULL,
    original_currency   VARCHAR(3)      NOT NULL DEFAULT 'USD',
    description         VARCHAR(100),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_card FOREIGN KEY (card_number) REFERENCES card (card_number)
);

CREATE TABLE IF NOT EXISTS app_user (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     VARCHAR(8)   NOT NULL,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(25)  NOT NULL,
    last_name   VARCHAR(25)  NOT NULL,
    user_type   VARCHAR(1)   NOT NULL DEFAULT 'U',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_user_id UNIQUE (user_id),
    CONSTRAINT chk_user_type CHECK (user_type IN ('A', 'U'))
);

CREATE INDEX idx_account_customer_id ON account (customer_id);
CREATE INDEX idx_card_account_id ON card (account_id);
CREATE INDEX idx_transaction_card_number ON transaction_record (card_number);
CREATE INDEX idx_transaction_timestamp ON transaction_record (timestamp);
CREATE INDEX idx_customer_last_name ON customer (last_name);
