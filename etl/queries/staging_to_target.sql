-- ============================================================================
-- CardDemo: Parameterized JOIN Queries
-- Staging-to-Target Extraction with Signed Overpunch Decoding
-- ============================================================================
-- Bind parameters:
--   $1  = load_batch_id   (BIGINT)  – filter by ETL batch
--   $2  = date_range_start (DATE)   – optional lower-bound filter
--   $3  = date_range_end   (DATE)   – optional upper-bound filter
--   $4  = account_id       (BIGINT) – optional account filter
-- ============================================================================

SET search_path TO carddemo;

-- ============================================================================
-- Helper: Signed overpunch decode function
-- ASCII signed overpunch convention for the last character of a
-- PIC S9(n)V99 DISPLAY field.
-- Positive: { 0  A 1  B 2  C 3  D 4  E 5  F 6  G 7  H 8  I 9
-- Negative: } 0  J 1  K 2  L 3  M 4  N 5  O 6  P 7  Q 8  R 9
-- ============================================================================
CREATE OR REPLACE FUNCTION carddemo.decode_signed_overpunch(raw_val TEXT)
RETURNS NUMERIC
LANGUAGE plpgsql IMMUTABLE STRICT
AS $$
DECLARE
    last_char   CHAR(1);
    prefix      TEXT;
    digit       INTEGER;
    sign_mult   INTEGER;
    raw_integer NUMERIC;
BEGIN
    IF raw_val IS NULL OR LENGTH(TRIM(raw_val)) = 0 THEN
        RETURN NULL;
    END IF;

    last_char := RIGHT(TRIM(raw_val), 1);
    prefix    := LEFT(TRIM(raw_val), LENGTH(TRIM(raw_val)) - 1);

    -- Positive overpunch: {=0, A=1 .. I=9
    CASE last_char
        WHEN '{' THEN digit := 0; sign_mult :=  1;
        WHEN 'A' THEN digit := 1; sign_mult :=  1;
        WHEN 'B' THEN digit := 2; sign_mult :=  1;
        WHEN 'C' THEN digit := 3; sign_mult :=  1;
        WHEN 'D' THEN digit := 4; sign_mult :=  1;
        WHEN 'E' THEN digit := 5; sign_mult :=  1;
        WHEN 'F' THEN digit := 6; sign_mult :=  1;
        WHEN 'G' THEN digit := 7; sign_mult :=  1;
        WHEN 'H' THEN digit := 8; sign_mult :=  1;
        WHEN 'I' THEN digit := 9; sign_mult :=  1;
        -- Negative overpunch: }=0, J=1 .. R=9
        WHEN '}' THEN digit := 0; sign_mult := -1;
        WHEN 'J' THEN digit := 1; sign_mult := -1;
        WHEN 'K' THEN digit := 2; sign_mult := -1;
        WHEN 'L' THEN digit := 3; sign_mult := -1;
        WHEN 'M' THEN digit := 4; sign_mult := -1;
        WHEN 'N' THEN digit := 5; sign_mult := -1;
        WHEN 'O' THEN digit := 6; sign_mult := -1;
        WHEN 'P' THEN digit := 7; sign_mult := -1;
        WHEN 'Q' THEN digit := 8; sign_mult := -1;
        WHEN 'R' THEN digit := 9; sign_mult := -1;
        ELSE
            -- If last char is a plain digit, treat as unsigned positive
            IF last_char BETWEEN '0' AND '9' THEN
                RETURN (prefix || last_char)::NUMERIC;
            END IF;
            RAISE EXCEPTION 'Invalid overpunch character: %', last_char;
    END CASE;

    raw_integer := (prefix || digit::TEXT)::NUMERIC;
    RETURN raw_integer * sign_mult;
END;
$$;

COMMENT ON FUNCTION carddemo.decode_signed_overpunch(TEXT) IS
    'Decodes ASCII signed overpunch in the trailing byte of a zoned decimal field. '
    'Returns the raw integer value (caller must apply V99 decimal shift).';

-- ============================================================================
-- 1. Transaction Type  (trantype.txt → transaction_type)
--    Copybook: CVTRA03Y.cpy  RECLN=60
--    Layout:   TRAN-TYPE PIC X(02) @0 | TRAN-TYPE-DESC PIC X(50) @2 | FILLER(8)
-- ============================================================================
INSERT INTO carddemo.transaction_type (
    tran_type_cd,
    tran_type_desc
)
SELECT
    SUBSTRING(s.raw_record FROM 1 FOR 2)                     AS tran_type_cd,
    RTRIM(SUBSTRING(s.raw_record FROM 3 FOR 50))             AS tran_type_desc
FROM carddemo.stg_tran_type s
WHERE s.load_batch_id = $1
ON CONFLICT (tran_type_cd) DO UPDATE
    SET tran_type_desc = EXCLUDED.tran_type_desc,
        updated_ts     = now();

-- ============================================================================
-- 2. Transaction Category  (trancatg.txt → transaction_category)
--    Copybook: CVTRA04Y.cpy  RECLN=60
--    Layout:   TRAN-TYPE-CD PIC X(02) @0 | TRAN-CAT-CD PIC 9(04) @2
--              TRAN-CAT-TYPE-DESC PIC X(50) @6 | FILLER(4)
-- ============================================================================
INSERT INTO carddemo.transaction_category (
    tran_type_cd,
    tran_cat_cd,
    tran_cat_type_desc
)
SELECT
    SUBSTRING(s.raw_record FROM 1 FOR 2)                     AS tran_type_cd,
    SUBSTRING(s.raw_record FROM 3 FOR 4)::SMALLINT           AS tran_cat_cd,
    RTRIM(SUBSTRING(s.raw_record FROM 7 FOR 50))             AS tran_cat_type_desc
FROM carddemo.stg_tran_category s
WHERE s.load_batch_id = $1
ON CONFLICT (tran_type_cd, tran_cat_cd) DO UPDATE
    SET tran_cat_type_desc = EXCLUDED.tran_cat_type_desc,
        updated_ts         = now();

-- ============================================================================
-- 3. Customer  (custdata.txt → customer)
--    Copybook: CVCUS01Y.cpy  RECLN=500
--    Layout:   CUST-ID PIC 9(09) @0 | CUST-FIRST-NAME PIC X(25) @9
--              CUST-MIDDLE-NAME PIC X(25) @34 | CUST-LAST-NAME PIC X(25) @59
--              CUST-ADDR-LINE-1 PIC X(50) @84  | CUST-ADDR-LINE-2 PIC X(50) @134
--              CUST-ADDR-LINE-3 PIC X(50) @184 | CUST-ADDR-STATE-CD PIC X(02) @234
--              CUST-ADDR-COUNTRY-CD PIC X(03) @236 | CUST-ADDR-ZIP PIC X(10) @239
--              CUST-PHONE-NUM-1 PIC X(15) @249 | CUST-PHONE-NUM-2 PIC X(15) @264
--              CUST-SSN PIC 9(09) @279 | CUST-GOVT-ISSUED-ID PIC X(20) @288
--              CUST-DOB PIC X(10) @308 | CUST-EFT-ACCOUNT-ID PIC X(10) @318
--              CUST-PRI-CARD-HOLDER-IND PIC X(01) @328
--              CUST-FICO-CREDIT-SCORE PIC 9(03) @329 | FILLER(168)
-- ============================================================================
INSERT INTO carddemo.customer (
    cust_id, cust_first_name, cust_middle_name, cust_last_name,
    cust_addr_line_1, cust_addr_line_2, cust_addr_line_3,
    cust_addr_state_cd, cust_addr_country_cd, cust_addr_zip,
    cust_phone_num_1, cust_phone_num_2,
    cust_ssn, cust_govt_issued_id, cust_dob,
    cust_eft_account_id, cust_pri_card_holder_ind, cust_fico_credit_score
)
SELECT
    SUBSTRING(s.raw_record FROM 1 FOR 9)::BIGINT                    AS cust_id,
    RTRIM(SUBSTRING(s.raw_record FROM 10 FOR 25))                   AS cust_first_name,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 35 FOR 25)), '')       AS cust_middle_name,
    RTRIM(SUBSTRING(s.raw_record FROM 60 FOR 25))                   AS cust_last_name,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 85 FOR 50)), '')       AS cust_addr_line_1,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 135 FOR 50)), '')      AS cust_addr_line_2,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 185 FOR 50)), '')      AS cust_addr_line_3,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 235 FOR 2)), '')       AS cust_addr_state_cd,
    COALESCE(
        NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 237 FOR 3)), ''),
        'USA'
    )                                                                AS cust_addr_country_cd,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 240 FOR 10)), '')      AS cust_addr_zip,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 250 FOR 15)), '')      AS cust_phone_num_1,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 265 FOR 15)), '')      AS cust_phone_num_2,
    -- SSN: 9 digits → XXX-XX-XXXX format
    OVERLAY(
        OVERLAY(
            SUBSTRING(s.raw_record FROM 280 FOR 9)
            PLACING '-' FROM 4 FOR 0
        ) PLACING '-' FROM 7 FOR 0
    )                                                                AS cust_ssn,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 289 FOR 20)), '')      AS cust_govt_issued_id,
    CASE
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 309 FOR 10)) ~ '^\d{4}-\d{2}-\d{2}$'
        THEN SUBSTRING(s.raw_record FROM 309 FOR 10)::DATE
        ELSE NULL
    END                                                              AS cust_dob,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 319 FOR 10)), '')      AS cust_eft_account_id,
    CASE UPPER(SUBSTRING(s.raw_record FROM 329 FOR 1))
        WHEN 'Y' THEN TRUE
        WHEN 'N' THEN FALSE
        ELSE NULL
    END                                                              AS cust_pri_card_holder_ind,
    CASE
        WHEN SUBSTRING(s.raw_record FROM 330 FOR 3) ~ '^\d{3}$'
        THEN SUBSTRING(s.raw_record FROM 330 FOR 3)::SMALLINT
        ELSE NULL
    END                                                              AS cust_fico_credit_score
FROM carddemo.stg_customer s
WHERE s.load_batch_id = $1
ON CONFLICT (cust_id) DO UPDATE
    SET cust_first_name          = EXCLUDED.cust_first_name,
        cust_middle_name         = EXCLUDED.cust_middle_name,
        cust_last_name           = EXCLUDED.cust_last_name,
        cust_addr_line_1         = EXCLUDED.cust_addr_line_1,
        cust_addr_line_2         = EXCLUDED.cust_addr_line_2,
        cust_addr_line_3         = EXCLUDED.cust_addr_line_3,
        cust_addr_state_cd       = EXCLUDED.cust_addr_state_cd,
        cust_addr_country_cd     = EXCLUDED.cust_addr_country_cd,
        cust_addr_zip            = EXCLUDED.cust_addr_zip,
        cust_phone_num_1         = EXCLUDED.cust_phone_num_1,
        cust_phone_num_2         = EXCLUDED.cust_phone_num_2,
        cust_ssn                 = EXCLUDED.cust_ssn,
        cust_govt_issued_id      = EXCLUDED.cust_govt_issued_id,
        cust_dob                 = EXCLUDED.cust_dob,
        cust_eft_account_id      = EXCLUDED.cust_eft_account_id,
        cust_pri_card_holder_ind = EXCLUDED.cust_pri_card_holder_ind,
        cust_fico_credit_score   = EXCLUDED.cust_fico_credit_score,
        updated_ts               = now();

-- ============================================================================
-- 4. Account  (acctdata.txt → account)
--    Copybook: CVACT01Y.cpy  RECLN=300
--    Layout:   ACCT-ID PIC 9(11) @0 | ACCT-ACTIVE-STATUS PIC X(01) @11
--              ACCT-CURR-BAL PIC S9(10)V99 @12 (12 chars, overpunch)
--              ACCT-CREDIT-LIMIT PIC S9(10)V99 @24 (12 chars, overpunch)
--              ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 @36 (12 chars, overpunch)
--              ACCT-OPEN-DATE PIC X(10) @48 | ACCT-EXPIRAION-DATE PIC X(10) @58
--              ACCT-REISSUE-DATE PIC X(10) @68
--              ACCT-CURR-CYC-CREDIT PIC S9(10)V99 @78 (12 chars, overpunch)
--              ACCT-CURR-CYC-DEBIT PIC S9(10)V99 @90 (12 chars, overpunch)
--              ACCT-ADDR-ZIP PIC X(10) @102 | ACCT-GROUP-ID PIC X(10) @112
--              FILLER(178)
-- ============================================================================
INSERT INTO carddemo.account (
    acct_id, acct_active_status,
    acct_curr_bal, acct_credit_limit, acct_cash_credit_limit,
    acct_open_date, acct_expiration_date, acct_reissue_date,
    acct_curr_cyc_credit, acct_curr_cyc_debit,
    acct_addr_zip, acct_group_id
)
SELECT
    SUBSTRING(s.raw_record FROM 1 FOR 11)::BIGINT                   AS acct_id,
    CASE UPPER(SUBSTRING(s.raw_record FROM 12 FOR 1))
        WHEN 'Y' THEN TRUE
        WHEN 'N' THEN FALSE
        ELSE TRUE
    END                                                              AS acct_active_status,
    -- Signed overpunch decode + V99 decimal shift (divide by 100)
    carddemo.decode_signed_overpunch(
        SUBSTRING(s.raw_record FROM 13 FOR 12)
    ) / 100.0                                                        AS acct_curr_bal,
    carddemo.decode_signed_overpunch(
        SUBSTRING(s.raw_record FROM 25 FOR 12)
    ) / 100.0                                                        AS acct_credit_limit,
    carddemo.decode_signed_overpunch(
        SUBSTRING(s.raw_record FROM 37 FOR 12)
    ) / 100.0                                                        AS acct_cash_credit_limit,
    CASE
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 49 FOR 10)) ~ '^\d{4}-\d{2}-\d{2}$'
        THEN SUBSTRING(s.raw_record FROM 49 FOR 10)::DATE
        ELSE NULL
    END                                                              AS acct_open_date,
    CASE
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 59 FOR 10)) ~ '^\d{4}-\d{2}-\d{2}$'
        THEN SUBSTRING(s.raw_record FROM 59 FOR 10)::DATE
        ELSE NULL
    END                                                              AS acct_expiration_date,
    CASE
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 69 FOR 10)) ~ '^\d{4}-\d{2}-\d{2}$'
        THEN SUBSTRING(s.raw_record FROM 69 FOR 10)::DATE
        ELSE NULL
    END                                                              AS acct_reissue_date,
    carddemo.decode_signed_overpunch(
        SUBSTRING(s.raw_record FROM 79 FOR 12)
    ) / 100.0                                                        AS acct_curr_cyc_credit,
    carddemo.decode_signed_overpunch(
        SUBSTRING(s.raw_record FROM 91 FOR 12)
    ) / 100.0                                                        AS acct_curr_cyc_debit,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 103 FOR 10)), '')      AS acct_addr_zip,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 113 FOR 10)), '')      AS acct_group_id
FROM carddemo.stg_account s
WHERE s.load_batch_id = $1
    AND ($4::BIGINT IS NULL
         OR SUBSTRING(s.raw_record FROM 1 FOR 11)::BIGINT = $4)
ON CONFLICT (acct_id) DO UPDATE
    SET acct_active_status     = EXCLUDED.acct_active_status,
        acct_curr_bal          = EXCLUDED.acct_curr_bal,
        acct_credit_limit      = EXCLUDED.acct_credit_limit,
        acct_cash_credit_limit = EXCLUDED.acct_cash_credit_limit,
        acct_open_date         = EXCLUDED.acct_open_date,
        acct_expiration_date   = EXCLUDED.acct_expiration_date,
        acct_reissue_date      = EXCLUDED.acct_reissue_date,
        acct_curr_cyc_credit   = EXCLUDED.acct_curr_cyc_credit,
        acct_curr_cyc_debit    = EXCLUDED.acct_curr_cyc_debit,
        acct_addr_zip          = EXCLUDED.acct_addr_zip,
        acct_group_id          = EXCLUDED.acct_group_id,
        updated_ts             = now();

-- ============================================================================
-- 5. Card  (carddata.txt → card)
--    Copybook: CVACT02Y.cpy  RECLN=150
--    Layout:   CARD-NUM PIC X(16) @0 | CARD-ACCT-ID PIC 9(11) @16
--              CARD-CVV-CD PIC 9(03) @27 | CARD-EMBOSSED-NAME PIC X(50) @30
--              CARD-EXPIRAION-DATE PIC X(10) @80
--              CARD-ACTIVE-STATUS PIC X(01) @90 | FILLER(59)
-- ============================================================================
INSERT INTO carddemo.card (
    card_num, card_acct_id, card_cvv_cd,
    card_embossed_name, card_expiration_date, card_active_status
)
SELECT
    RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16))                    AS card_num,
    SUBSTRING(s.raw_record FROM 17 FOR 11)::BIGINT                  AS card_acct_id,
    LPAD(SUBSTRING(s.raw_record FROM 28 FOR 3), 3, '0')            AS card_cvv_cd,
    RTRIM(SUBSTRING(s.raw_record FROM 31 FOR 50))                   AS card_embossed_name,
    CASE
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 81 FOR 10)) ~ '^\d{4}-\d{2}-\d{2}$'
        THEN SUBSTRING(s.raw_record FROM 81 FOR 10)::DATE
        ELSE NULL
    END                                                              AS card_expiration_date,
    CASE UPPER(SUBSTRING(s.raw_record FROM 91 FOR 1))
        WHEN 'Y' THEN TRUE
        WHEN 'N' THEN FALSE
        ELSE TRUE
    END                                                              AS card_active_status
FROM carddemo.stg_card s
WHERE s.load_batch_id = $1
    AND ($4::BIGINT IS NULL
         OR SUBSTRING(s.raw_record FROM 17 FOR 11)::BIGINT = $4)
ON CONFLICT (card_num) DO UPDATE
    SET card_acct_id        = EXCLUDED.card_acct_id,
        card_cvv_cd         = EXCLUDED.card_cvv_cd,
        card_embossed_name  = EXCLUDED.card_embossed_name,
        card_expiration_date= EXCLUDED.card_expiration_date,
        card_active_status  = EXCLUDED.card_active_status,
        updated_ts          = now();

-- ============================================================================
-- 6. Card Cross-Reference  (cardxref.txt → card_xref)
--    Copybook: CVACT03Y.cpy  RECLN=50
--    Layout:   XREF-CARD-NUM PIC X(16) @0 | XREF-CUST-ID PIC 9(09) @16
--              XREF-ACCT-ID PIC 9(11) @25 | FILLER(14)
-- ============================================================================
INSERT INTO carddemo.card_xref (
    xref_card_num, xref_cust_id, xref_acct_id
)
SELECT
    RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16))                    AS xref_card_num,
    SUBSTRING(s.raw_record FROM 17 FOR 9)::BIGINT                   AS xref_cust_id,
    SUBSTRING(s.raw_record FROM 26 FOR 11)::BIGINT                  AS xref_acct_id
FROM carddemo.stg_card_xref s
WHERE s.load_batch_id = $1
    AND ($4::BIGINT IS NULL
         OR SUBSTRING(s.raw_record FROM 26 FOR 11)::BIGINT = $4)
ON CONFLICT (xref_card_num) DO UPDATE
    SET xref_cust_id = EXCLUDED.xref_cust_id,
        xref_acct_id = EXCLUDED.xref_acct_id,
        updated_ts   = now();

-- ============================================================================
-- 7. Disclosure Group  (discgrp.txt → disclosure_group)
--    Copybook: CVTRA02Y.cpy  RECLN=50
--    Layout:   DIS-ACCT-GROUP-ID PIC X(10) @0 | DIS-TRAN-TYPE-CD PIC X(02) @10
--              DIS-TRAN-CAT-CD PIC 9(04) @12
--              DIS-INT-RATE PIC S9(04)V99 @16 (6 chars, overpunch)
--              FILLER(28)
-- ============================================================================
INSERT INTO carddemo.disclosure_group (
    dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate
)
SELECT
    RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 10))                    AS dis_acct_group_id,
    SUBSTRING(s.raw_record FROM 11 FOR 2)                           AS dis_tran_type_cd,
    SUBSTRING(s.raw_record FROM 13 FOR 4)::SMALLINT                 AS dis_tran_cat_cd,
    carddemo.decode_signed_overpunch(
        SUBSTRING(s.raw_record FROM 17 FOR 6)
    ) / 100.0                                                        AS dis_int_rate
FROM carddemo.stg_disclosure_group s
WHERE s.load_batch_id = $1
ON CONFLICT (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd) DO UPDATE
    SET dis_int_rate = EXCLUDED.dis_int_rate,
        updated_ts   = now();

-- ============================================================================
-- 8. Transaction Category Balance  (tcatbal.txt → tran_cat_balance)
--    Copybook: CVTRA01Y.cpy  RECLN=50
--    Layout:   TRANCAT-ACCT-ID PIC 9(11) @0 | TRANCAT-TYPE-CD PIC X(02) @11
--              TRANCAT-CD PIC 9(04) @13
--              TRAN-CAT-BAL PIC S9(09)V99 @17 (11 chars, overpunch)
--              FILLER(22)
-- ============================================================================
INSERT INTO carddemo.tran_cat_balance (
    trancat_acct_id, trancat_type_cd, trancat_cd, tran_cat_bal
)
SELECT
    SUBSTRING(s.raw_record FROM 1 FOR 11)::BIGINT                   AS trancat_acct_id,
    SUBSTRING(s.raw_record FROM 12 FOR 2)                           AS trancat_type_cd,
    SUBSTRING(s.raw_record FROM 14 FOR 4)::SMALLINT                 AS trancat_cd,
    carddemo.decode_signed_overpunch(
        SUBSTRING(s.raw_record FROM 18 FOR 11)
    ) / 100.0                                                        AS tran_cat_bal
FROM carddemo.stg_tran_cat_bal s
WHERE s.load_batch_id = $1
    AND ($4::BIGINT IS NULL
         OR SUBSTRING(s.raw_record FROM 1 FOR 11)::BIGINT = $4)
ON CONFLICT (trancat_acct_id, trancat_type_cd, trancat_cd) DO UPDATE
    SET tran_cat_bal = EXCLUDED.tran_cat_bal,
        updated_ts   = now();

-- ============================================================================
-- 9. Transaction  (dailytran.txt → transaction)
--    Copybook: CVTRA05Y.cpy / CVTRA06Y.cpy  RECLN=350
--    Layout:   TRAN-ID PIC X(16) @0 | TRAN-TYPE-CD PIC X(02) @16
--              TRAN-CAT-CD PIC 9(04) @18 | TRAN-SOURCE PIC X(10) @22
--              TRAN-DESC PIC X(100) @32
--              TRAN-AMT PIC S9(09)V99 @132 (11 chars, overpunch)
--              TRAN-MERCHANT-ID PIC 9(09) @143
--              TRAN-MERCHANT-NAME PIC X(50) @152
--              TRAN-MERCHANT-CITY PIC X(50) @202
--              TRAN-MERCHANT-ZIP PIC X(10) @252
--              TRAN-CARD-NUM PIC X(16) @262
--              TRAN-ORIG-TS PIC X(26) @278
--              TRAN-PROC-TS PIC X(26) @304 | FILLER(20)
-- ============================================================================
INSERT INTO carddemo.transaction (
    tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc,
    tran_amt, tran_merchant_id, tran_merchant_name,
    tran_merchant_city, tran_merchant_zip,
    tran_card_num, tran_orig_ts, tran_proc_ts
)
SELECT
    RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16))                    AS tran_id,
    SUBSTRING(s.raw_record FROM 17 FOR 2)                           AS tran_type_cd,
    SUBSTRING(s.raw_record FROM 19 FOR 4)::SMALLINT                 AS tran_cat_cd,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 23 FOR 10)), '')       AS tran_source,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 33 FOR 100)), '')      AS tran_desc,
    carddemo.decode_signed_overpunch(
        SUBSTRING(s.raw_record FROM 133 FOR 11)
    ) / 100.0                                                        AS tran_amt,
    CASE
        WHEN SUBSTRING(s.raw_record FROM 144 FOR 9) ~ '^\d+$'
        THEN SUBSTRING(s.raw_record FROM 144 FOR 9)::BIGINT
        ELSE NULL
    END                                                              AS tran_merchant_id,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 153 FOR 50)), '')      AS tran_merchant_name,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 203 FOR 50)), '')      AS tran_merchant_city,
    NULLIF(RTRIM(SUBSTRING(s.raw_record FROM 253 FOR 10)), '')      AS tran_merchant_zip,
    RTRIM(SUBSTRING(s.raw_record FROM 263 FOR 16))                  AS tran_card_num,
    CASE
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 279 FOR 26)) ~ '^\d{4}-\d{2}-\d{2}'
        THEN SUBSTRING(s.raw_record FROM 279 FOR 26)::TIMESTAMP
        ELSE NULL
    END                                                              AS tran_orig_ts,
    CASE
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 305 FOR 26)) ~ '^\d{4}-\d{2}-\d{2}'
        THEN SUBSTRING(s.raw_record FROM 305 FOR 26)::TIMESTAMP
        ELSE NULL
    END                                                              AS tran_proc_ts
FROM carddemo.stg_transaction s
WHERE s.load_batch_id = $1
    AND ($2::DATE IS NULL
         OR CASE
                WHEN RTRIM(SUBSTRING(s.raw_record FROM 279 FOR 26)) ~ '^\d{4}-\d{2}-\d{2}'
                THEN SUBSTRING(s.raw_record FROM 279 FOR 10)::DATE
                ELSE NULL
            END >= $2)
    AND ($3::DATE IS NULL
         OR CASE
                WHEN RTRIM(SUBSTRING(s.raw_record FROM 279 FOR 26)) ~ '^\d{4}-\d{2}-\d{2}'
                THEN SUBSTRING(s.raw_record FROM 279 FOR 10)::DATE
                ELSE NULL
            END <= $3)
ON CONFLICT (tran_id) DO UPDATE
    SET tran_type_cd       = EXCLUDED.tran_type_cd,
        tran_cat_cd        = EXCLUDED.tran_cat_cd,
        tran_source        = EXCLUDED.tran_source,
        tran_desc          = EXCLUDED.tran_desc,
        tran_amt           = EXCLUDED.tran_amt,
        tran_merchant_id   = EXCLUDED.tran_merchant_id,
        tran_merchant_name = EXCLUDED.tran_merchant_name,
        tran_merchant_city = EXCLUDED.tran_merchant_city,
        tran_merchant_zip  = EXCLUDED.tran_merchant_zip,
        tran_card_num      = EXCLUDED.tran_card_num,
        tran_orig_ts       = EXCLUDED.tran_orig_ts,
        tran_proc_ts       = EXCLUDED.tran_proc_ts,
        updated_ts         = now();

-- ============================================================================
-- Analytical queries (parameterized)
-- ============================================================================

-- 9a. Account summary with customer info, filtered by account ID
--     Bind: $4 = account_id
SELECT
    a.acct_id,
    a.acct_active_status,
    a.acct_curr_bal,
    a.acct_credit_limit,
    a.acct_open_date,
    a.acct_expiration_date,
    c.cust_id,
    c.cust_first_name || ' ' || c.cust_last_name   AS cust_full_name,
    c.cust_addr_state_cd,
    cx.xref_card_num
FROM carddemo.account       a
JOIN carddemo.card_xref     cx ON cx.xref_acct_id = a.acct_id
JOIN carddemo.customer      c  ON c.cust_id       = cx.xref_cust_id
WHERE ($4::BIGINT IS NULL OR a.acct_id = $4);

-- 9b. Transaction detail with lookups, filtered by date range and account
--     Bind: $2 = date_range_start, $3 = date_range_end, $4 = account_id
SELECT
    t.tran_id,
    t.tran_orig_ts,
    tt.tran_type_desc,
    tc.tran_cat_type_desc,
    t.tran_source,
    t.tran_amt,
    t.tran_merchant_name,
    t.tran_card_num,
    cx.xref_acct_id          AS acct_id,
    cx.xref_cust_id          AS cust_id
FROM carddemo.transaction             t
JOIN carddemo.transaction_type        tt ON tt.tran_type_cd = t.tran_type_cd
JOIN carddemo.transaction_category    tc ON tc.tran_type_cd = t.tran_type_cd
                                        AND tc.tran_cat_cd  = t.tran_cat_cd
JOIN carddemo.card_xref               cx ON cx.xref_card_num = t.tran_card_num
WHERE ($2::DATE IS NULL OR t.tran_orig_ts::DATE >= $2)
  AND ($3::DATE IS NULL OR t.tran_orig_ts::DATE <= $3)
  AND ($4::BIGINT IS NULL OR cx.xref_acct_id = $4)
ORDER BY t.tran_orig_ts;

-- 9c. Disclosure group interest rates for an account's group
--     Bind: $4 = account_id
SELECT
    dg.dis_acct_group_id,
    tt.tran_type_desc,
    tc.tran_cat_type_desc,
    dg.dis_int_rate
FROM carddemo.disclosure_group        dg
JOIN carddemo.transaction_type        tt ON tt.tran_type_cd = dg.dis_tran_type_cd
JOIN carddemo.transaction_category    tc ON tc.tran_type_cd = dg.dis_tran_type_cd
                                        AND tc.tran_cat_cd  = dg.dis_tran_cat_cd
WHERE dg.dis_acct_group_id = (
    SELECT a.acct_group_id
    FROM carddemo.account a
    WHERE a.acct_id = $4
)
ORDER BY dg.dis_tran_type_cd, dg.dis_tran_cat_cd;
