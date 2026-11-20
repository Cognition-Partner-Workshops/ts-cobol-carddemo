-- VSAM KSDS / GDG datasets of the interest chain as PostgreSQL tables.
-- Alphanumeric fields keep their fixed-width (space-padded) value so keys compare
-- exactly like the COBOL PIC X fields they replace.
DROP SCHEMA IF EXISTS interest CASCADE;
CREATE SCHEMA interest;

-- AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS (CVACT01Y)
CREATE TABLE interest.account (
    acct_id            bigint        PRIMARY KEY,
    active_status      varchar(1)    NOT NULL,
    curr_bal           numeric(12,2) NOT NULL,
    credit_limit       numeric(12,2) NOT NULL,
    cash_credit_limit  numeric(12,2) NOT NULL,
    open_date          varchar(10)   NOT NULL,
    expiration_date    varchar(10)   NOT NULL,
    reissue_date       varchar(10)   NOT NULL,
    curr_cyc_credit    numeric(12,2) NOT NULL,
    curr_cyc_debit     numeric(12,2) NOT NULL,
    addr_zip           varchar(10)   NOT NULL,
    group_id           varchar(10)   NOT NULL,
    filler             varchar(178)  NOT NULL,
    raw_record         varchar(300)  NOT NULL,
    rewritten          boolean       NOT NULL DEFAULT false
);

-- AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS + AIX on acct id (CVACT03Y)
CREATE TABLE interest.card_xref (
    card_num  varchar(16) PRIMARY KEY,
    -- position in the cluster: the order duplicates come back on the account alternate key
    load_seq  integer     NOT NULL,
    cust_id   bigint      NOT NULL,
    acct_id   bigint      NOT NULL,
    filler    varchar(14) NOT NULL
);
CREATE INDEX card_xref_acct_id ON interest.card_xref (acct_id, load_seq);

-- AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS (CVTRA02Y)
CREATE TABLE interest.disclosure_group (
    acct_group_id  varchar(10)  NOT NULL,
    tran_type_cd   varchar(2)   NOT NULL,
    tran_cat_cd    integer      NOT NULL,
    int_rate       numeric(6,2) NOT NULL,
    filler         varchar(28)  NOT NULL,
    PRIMARY KEY (acct_group_id, tran_type_cd, tran_cat_cd)
);

-- AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS (CVTRA01Y)
CREATE TABLE interest.tran_cat_balance (
    acct_id       bigint        NOT NULL,
    tran_type_cd  varchar(2)    NOT NULL,
    tran_cat_cd   integer       NOT NULL,
    tran_cat_bal  numeric(11,2) NOT NULL,
    filler        varchar(22)   NOT NULL,
    raw_record    varchar(50)   NOT NULL,
    PRIMARY KEY (acct_id, tran_type_cd, tran_cat_cd)
);

-- AWS.M2.CARDDEMO.SYSTRAN(+1) (CVTRA05Y), in write order
CREATE TABLE interest.systran (
    write_seq        bigint        PRIMARY KEY,
    tran_id          varchar(16)   NOT NULL,
    tran_type_cd     varchar(2)    NOT NULL,
    tran_cat_cd      integer       NOT NULL,
    tran_source      varchar(10)   NOT NULL,
    tran_desc        varchar(100)  NOT NULL,
    tran_amt         numeric(11,2) NOT NULL,
    merchant_id      bigint        NOT NULL,
    merchant_name    varchar(50)   NOT NULL,
    merchant_city    varchar(50)   NOT NULL,
    merchant_zip     varchar(10)   NOT NULL,
    card_num         varchar(16)   NOT NULL,
    orig_ts          varchar(26)   NOT NULL,
    proc_ts          varchar(26)   NOT NULL
);
