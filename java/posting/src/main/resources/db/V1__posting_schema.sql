-- PostgreSQL home of the VSAM KSDS clusters the posting chain reads and updates.
-- Alphanumeric columns hold the COBOL PIC X value without trailing spaces; the record
-- codec pads them back to the copybook width. Keys use the "C" collation so ORDER BY
-- matches the byte order of a KSDS (and of the SORT step in TRANREPT).

-- AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS (CVACT01Y)
create table account (
    acct_id            bigint primary key,
    active_status      varchar(1)  not null,
    curr_bal           numeric(12, 2) not null,
    credit_limit       numeric(12, 2) not null,
    cash_credit_limit  numeric(12, 2) not null,
    open_date          varchar(10) not null,
    expiration_date    varchar(10) not null,
    reissue_date       varchar(10) not null,
    curr_cyc_credit    numeric(12, 2) not null,
    curr_cyc_debit     numeric(12, 2) not null,
    addr_zip           varchar(10) not null,
    group_id           varchar(10) not null
);

-- AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS (CVACT03Y)
create table card_xref (
    card_num  varchar(16) collate "C" primary key,
    cust_id   bigint not null,
    acct_id   bigint not null
);

-- AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS (CVTRA01Y)
create table tran_cat_balance (
    acct_id  bigint not null,
    type_cd  varchar(2) collate "C" not null,
    cat_cd   integer not null,
    balance  numeric(11, 2) not null,
    primary key (acct_id, type_cd, cat_cd)
);

-- AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS (CVTRA03Y)
create table tran_type (
    type_cd      varchar(2) collate "C" primary key,
    description  varchar(50) not null
);

-- AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS (CVTRA04Y)
create table tran_category (
    type_cd      varchar(2) collate "C" not null,
    cat_cd       integer not null,
    description  varchar(50) not null,
    primary key (type_cd, cat_cd)
);

-- AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS (CVTRA05Y)
create table transaction (
    tran_id        varchar(16) collate "C" primary key,
    type_cd        varchar(2) not null,
    cat_cd         integer not null,
    source         varchar(10) not null,
    description    varchar(100) not null,
    amount         numeric(11, 2) not null,
    merchant_id    varchar(9) not null,
    merchant_name  varchar(50) not null,
    merchant_city  varchar(50) not null,
    merchant_zip   varchar(10) not null,
    card_num       varchar(16) collate "C" not null,
    orig_ts        varchar(26) not null,
    proc_ts        varchar(26) not null
);
