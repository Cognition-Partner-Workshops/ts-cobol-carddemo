CREATE TABLE customers (
    cust_id           BIGINT       NOT NULL,
    first_name        VARCHAR(25)  NOT NULL,
    middle_name       VARCHAR(25),
    last_name         VARCHAR(25)  NOT NULL,
    addr_line_1       VARCHAR(50),
    addr_line_2       VARCHAR(50),
    addr_line_3       VARCHAR(50),
    addr_state_cd     VARCHAR(2),
    addr_country_cd   VARCHAR(3),
    addr_zip          VARCHAR(10),
    phone_num_1       VARCHAR(15),
    phone_num_2       VARCHAR(15),
    ssn               BIGINT,
    govt_issued_id    VARCHAR(20),
    dob               VARCHAR(10),
    eft_account_id    VARCHAR(10),
    pri_card_holder   VARCHAR(1),
    fico_credit_score INT,
    PRIMARY KEY (cust_id)
);

CREATE TABLE accounts (
    acct_id              BIGINT        NOT NULL,
    active_status        VARCHAR(1)    DEFAULT 'Y',
    curr_bal             DECIMAL(12,2) DEFAULT 0,
    credit_limit         DECIMAL(12,2) DEFAULT 0,
    cash_credit_limit    DECIMAL(12,2) DEFAULT 0,
    open_date            VARCHAR(10),
    expiration_date      VARCHAR(10),
    reissue_date         VARCHAR(10),
    curr_cyc_credit      DECIMAL(12,2) DEFAULT 0,
    curr_cyc_debit       DECIMAL(12,2) DEFAULT 0,
    addr_zip             VARCHAR(10),
    group_id             VARCHAR(10),
    PRIMARY KEY (acct_id)
);

CREATE TABLE cards (
    card_num          VARCHAR(16)  NOT NULL,
    acct_id           BIGINT       NOT NULL,
    cvv_cd            INT,
    embossed_name     VARCHAR(50),
    expiration_date   VARCHAR(10),
    active_status     VARCHAR(1)   DEFAULT 'Y',
    PRIMARY KEY (card_num),
    CONSTRAINT fk_cards_acct FOREIGN KEY (acct_id) REFERENCES accounts(acct_id)
);

CREATE TABLE card_account_xref (
    card_num  VARCHAR(16) NOT NULL,
    cust_id   BIGINT      NOT NULL,
    acct_id   BIGINT      NOT NULL,
    PRIMARY KEY (card_num),
    CONSTRAINT fk_xref_cust FOREIGN KEY (cust_id) REFERENCES customers(cust_id),
    CONSTRAINT fk_xref_acct FOREIGN KEY (acct_id) REFERENCES accounts(acct_id)
);

CREATE TABLE transactions (
    tran_id          VARCHAR(16)   NOT NULL,
    type_cd          VARCHAR(2),
    cat_cd           INT,
    source           VARCHAR(10),
    description      VARCHAR(100),
    amount           DECIMAL(11,2),
    merchant_id      BIGINT,
    merchant_name    VARCHAR(50),
    merchant_city    VARCHAR(50),
    merchant_zip     VARCHAR(10),
    card_num         VARCHAR(16),
    orig_ts          VARCHAR(26),
    proc_ts          VARCHAR(26),
    PRIMARY KEY (tran_id)
);

CREATE INDEX idx_transactions_card_num ON transactions(card_num);
CREATE INDEX idx_transactions_acct ON transactions(card_num, orig_ts);

CREATE TABLE users (
    user_id    VARCHAR(8)  NOT NULL,
    first_name VARCHAR(20),
    last_name  VARCHAR(20),
    password   VARCHAR(255),
    user_type  VARCHAR(1)  DEFAULT 'U',
    PRIMARY KEY (user_id)
);

CREATE TABLE transaction_types (
    type_cd     VARCHAR(2)  NOT NULL,
    type_desc   VARCHAR(50),
    PRIMARY KEY (type_cd)
);

CREATE TABLE transaction_categories (
    type_cd     VARCHAR(2)  NOT NULL,
    cat_cd      INT         NOT NULL,
    cat_desc    VARCHAR(50),
    PRIMARY KEY (type_cd, cat_cd),
    CONSTRAINT fk_trcat_type FOREIGN KEY (type_cd) REFERENCES transaction_types(type_cd)
);

CREATE TABLE disclosure_groups (
    acct_group_id   VARCHAR(10) NOT NULL,
    tran_type_cd    VARCHAR(2)  NOT NULL,
    tran_cat_cd     INT         NOT NULL,
    int_rate        DECIMAL(6,2),
    PRIMARY KEY (acct_group_id, tran_type_cd, tran_cat_cd)
);

CREATE TABLE transaction_category_balances (
    acct_id      BIGINT      NOT NULL,
    type_cd      VARCHAR(2)  NOT NULL,
    cat_cd       INT         NOT NULL,
    balance      DECIMAL(11,2) DEFAULT 0,
    PRIMARY KEY (acct_id, type_cd, cat_cd),
    CONSTRAINT fk_tcatbal_acct FOREIGN KEY (acct_id) REFERENCES accounts(acct_id)
);

CREATE TABLE authorization_summary (
    auth_id          BIGINT       NOT NULL AUTO_INCREMENT,
    card_num         VARCHAR(16)  NOT NULL,
    acct_id          BIGINT       NOT NULL,
    total_auth_amt   DECIMAL(11,2) DEFAULT 0,
    auth_count       INT           DEFAULT 0,
    last_auth_date   VARCHAR(10),
    PRIMARY KEY (auth_id)
);

CREATE TABLE authorization_details (
    auth_detail_id   BIGINT        NOT NULL AUTO_INCREMENT,
    auth_id          BIGINT        NOT NULL,
    auth_date        VARCHAR(10),
    auth_time        VARCHAR(8),
    auth_amount      DECIMAL(11,2),
    auth_status      VARCHAR(1),
    merchant_id      BIGINT,
    merchant_name    VARCHAR(50),
    PRIMARY KEY (auth_detail_id),
    CONSTRAINT fk_authdet_summary FOREIGN KEY (auth_id) REFERENCES authorization_summary(auth_id)
);

CREATE INDEX idx_auth_summary_card ON authorization_summary(card_num);

CREATE TABLE auth_fraud (
    fraud_id         BIGINT       NOT NULL AUTO_INCREMENT,
    card_num         VARCHAR(16)  NOT NULL,
    acct_id          BIGINT       NOT NULL,
    fraud_date       VARCHAR(10),
    fraud_amount     DECIMAL(11,2),
    fraud_reason     VARCHAR(100),
    PRIMARY KEY (fraud_id)
);

CREATE INDEX idx_auth_fraud_card ON auth_fraud(card_num);
