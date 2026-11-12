-- VSAM KSDS clusters read and written by INTCALC, one table per cluster.
-- Key columns follow the copybook keys; text keys compare in byte order (COLLATE "C")
-- like VSAM.  FILLER is kept so the unloaded records are byte-identical.
CREATE SCHEMA IF NOT EXISTS interest;

-- AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS (CVACT01Y)
CREATE TABLE IF NOT EXISTS interest.account (
    acct_id              BIGINT PRIMARY KEY,
    active_status        VARCHAR(1)  NOT NULL,
    curr_bal             NUMERIC(12,2) NOT NULL,
    credit_limit         NUMERIC(12,2) NOT NULL,
    cash_credit_limit    NUMERIC(12,2) NOT NULL,
    open_date            VARCHAR(10) NOT NULL,
    expiration_date      VARCHAR(10) NOT NULL,
    reissue_date         VARCHAR(10) NOT NULL,
    curr_cyc_credit      NUMERIC(12,2) NOT NULL,
    curr_cyc_debit       NUMERIC(12,2) NOT NULL,
    addr_zip             VARCHAR(10) NOT NULL,
    group_id             VARCHAR(10) NOT NULL,
    filler               VARCHAR(178) NOT NULL
);

-- AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS (CVACT03Y) and its AIX/PATH on XREF-ACCT-ID
CREATE TABLE IF NOT EXISTS interest.card_xref (
    card_num             VARCHAR(16) COLLATE "C" PRIMARY KEY,
    cust_id              BIGINT NOT NULL,
    acct_id              BIGINT NOT NULL,
    filler               VARCHAR(14) NOT NULL
);
CREATE INDEX IF NOT EXISTS card_xref_acct_aix ON interest.card_xref (acct_id, card_num);

-- AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS (CVTRA01Y)
CREATE TABLE IF NOT EXISTS interest.tran_cat_bal (
    acct_id              BIGINT NOT NULL,
    type_cd              VARCHAR(2) COLLATE "C" NOT NULL,
    cat_cd               INTEGER NOT NULL,
    balance              NUMERIC(11,2) NOT NULL,
    filler               VARCHAR(22) NOT NULL,
    PRIMARY KEY (acct_id, type_cd, cat_cd)
);

-- AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS (CVTRA02Y)
CREATE TABLE IF NOT EXISTS interest.disclosure_group (
    group_id             VARCHAR(10) COLLATE "C" NOT NULL,
    type_cd              VARCHAR(2) COLLATE "C" NOT NULL,
    cat_cd               INTEGER NOT NULL,
    int_rate             NUMERIC(6,2) NOT NULL,
    filler               VARCHAR(28) NOT NULL,
    PRIMARY KEY (group_id, type_cd, cat_cd)
);

-- AWS.M2.CARDDEMO.SYSTRAN(+1) (CVTRA05Y), sequential: seq is the write order
CREATE TABLE IF NOT EXISTS interest.systran (
    seq                  INTEGER PRIMARY KEY,
    tran_id              VARCHAR(16) NOT NULL,
    type_cd              VARCHAR(2)  NOT NULL,
    cat_cd               INTEGER NOT NULL,
    source               VARCHAR(10) NOT NULL,
    description          VARCHAR(100) NOT NULL,
    amount               NUMERIC(11,2) NOT NULL,
    merchant_id          BIGINT NOT NULL,
    merchant_name        VARCHAR(50) NOT NULL,
    merchant_city        VARCHAR(50) NOT NULL,
    merchant_zip         VARCHAR(10) NOT NULL,
    card_num             VARCHAR(16) NOT NULL,
    orig_ts              VARCHAR(26) NOT NULL,
    proc_ts              VARCHAR(26) NOT NULL
);
