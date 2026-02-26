-- ============================================================================
-- CardDemo ETL: Pre-Load Validation SQL
-- Informatica-style pre-session SQL executed before staging-to-target load.
-- Validates staged raw records for structural integrity, type coercion,
-- null checks, and referential readiness.
-- ============================================================================
-- Bind: :batch_id = batch_id (BIGINT)
-- ============================================================================

SET search_path TO carddemo;

-- ============================================================================
-- Update batch status
-- ============================================================================
UPDATE carddemo.etl_batch_log
SET    status = 'VALIDATING'
WHERE  batch_id = :batch_id;

-- ============================================================================
-- 1. TRANSACTION TYPE validation  (trantype.txt, RECLN=60)
-- ============================================================================
-- Reject: record too short, missing type code, empty description
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'transaction_type',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 52 THEN
            'Record length < 52 (expected at least 52 for RECLN 60)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 2)) = '' THEN
            'NULL/empty tran_type_cd (PK field cannot be blank)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 3 FOR 50)) = '' THEN
            'NULL/empty tran_type_desc (required field)'
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_tran_type s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 52
      OR RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 2)) = ''
      OR RTRIM(SUBSTRING(s.raw_record FROM 3 FOR 50)) = ''
  );

-- ============================================================================
-- 2. TRANSACTION CATEGORY validation  (trancatg.txt, RECLN=60)
-- ============================================================================
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'transaction_category',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 56 THEN
            'Record length < 56 (expected at least 56 for RECLN 60)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 2)) = '' THEN
            'NULL/empty tran_type_cd (composite PK part 1)'
        WHEN SUBSTRING(s.raw_record FROM 3 FOR 4) !~ '^\d{4}$' THEN
            'tran_cat_cd is not a valid 4-digit number: "'
            || SUBSTRING(s.raw_record FROM 3 FOR 4) || '"'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 7 FOR 50)) = '' THEN
            'NULL/empty tran_cat_type_desc (required field)'
        -- Referential: tran_type_cd must exist in transaction_type
        WHEN NOT EXISTS (
            SELECT 1 FROM carddemo.transaction_type t
            WHERE t.tran_type_cd = SUBSTRING(s.raw_record FROM 1 FOR 2)
        ) AND NOT EXISTS (
            SELECT 1 FROM carddemo.stg_tran_type st
            WHERE st.load_batch_id = :batch_id
              AND SUBSTRING(st.raw_record FROM 1 FOR 2) =
                  SUBSTRING(s.raw_record FROM 1 FOR 2)
        ) THEN
            'Referential integrity: tran_type_cd "'
            || SUBSTRING(s.raw_record FROM 1 FOR 2)
            || '" not found in transaction_type or current batch'
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_tran_category s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 56
      OR RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 2)) = ''
      OR SUBSTRING(s.raw_record FROM 3 FOR 4) !~ '^\d{4}$'
      OR RTRIM(SUBSTRING(s.raw_record FROM 7 FOR 50)) = ''
      OR (
          NOT EXISTS (
              SELECT 1 FROM carddemo.transaction_type t
              WHERE t.tran_type_cd = SUBSTRING(s.raw_record FROM 1 FOR 2)
          )
          AND NOT EXISTS (
              SELECT 1 FROM carddemo.stg_tran_type st
              WHERE st.load_batch_id = :batch_id
                AND SUBSTRING(st.raw_record FROM 1 FOR 2) =
                    SUBSTRING(s.raw_record FROM 1 FOR 2)
          )
      )
  );

-- ============================================================================
-- 3. CUSTOMER validation  (custdata.txt, RECLN=500)
-- ============================================================================
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'customer',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 332 THEN
            'Record length < 332 (expected 500 for RECLN 500)'
        WHEN SUBSTRING(s.raw_record FROM 1 FOR 9) !~ '^\d{9}$' THEN
            'cust_id is not a valid 9-digit number: "'
            || SUBSTRING(s.raw_record FROM 1 FOR 9) || '"'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 10 FOR 25)) = '' THEN
            'NULL/empty cust_first_name (required)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 60 FOR 25)) = '' THEN
            'NULL/empty cust_last_name (required)'
        WHEN SUBSTRING(s.raw_record FROM 280 FOR 9) !~ '^\d{9}$' THEN
            'cust_ssn is not a valid 9-digit number: "'
            || SUBSTRING(s.raw_record FROM 280 FOR 9) || '"'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 309 FOR 10)) != ''
             AND RTRIM(SUBSTRING(s.raw_record FROM 309 FOR 10)) !~ '^\d{4}-\d{2}-\d{2}$' THEN
            'cust_dob invalid date format: "'
            || RTRIM(SUBSTRING(s.raw_record FROM 309 FOR 10)) || '"'
        WHEN SUBSTRING(s.raw_record FROM 330 FOR 3) ~ '^\d{3}$'
             AND SUBSTRING(s.raw_record FROM 330 FOR 3)::INTEGER NOT BETWEEN 0 AND 999 THEN
            'cust_fico_credit_score out of range: '
            || SUBSTRING(s.raw_record FROM 330 FOR 3)
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_customer s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 332
      OR SUBSTRING(s.raw_record FROM 1 FOR 9) !~ '^\d{9}$'
      OR RTRIM(SUBSTRING(s.raw_record FROM 10 FOR 25)) = ''
      OR RTRIM(SUBSTRING(s.raw_record FROM 60 FOR 25)) = ''
      OR SUBSTRING(s.raw_record FROM 280 FOR 9) !~ '^\d{9}$'
      OR (
          RTRIM(SUBSTRING(s.raw_record FROM 309 FOR 10)) != ''
          AND RTRIM(SUBSTRING(s.raw_record FROM 309 FOR 10)) !~ '^\d{4}-\d{2}-\d{2}$'
      )
  );

-- ============================================================================
-- 4. ACCOUNT validation  (acctdata.txt, RECLN=300)
-- ============================================================================
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'account',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 122 THEN
            'Record length < 122 (expected 300 for RECLN 300)'
        WHEN SUBSTRING(s.raw_record FROM 1 FOR 11) !~ '^\d{11}$' THEN
            'acct_id is not a valid 11-digit number: "'
            || SUBSTRING(s.raw_record FROM 1 FOR 11) || '"'
        WHEN UPPER(SUBSTRING(s.raw_record FROM 12 FOR 1)) NOT IN ('Y', 'N') THEN
            'acct_active_status invalid value: "'
            || SUBSTRING(s.raw_record FROM 12 FOR 1) || '" (expected Y/N)'
        -- Validate signed overpunch: last char of 12-char field must be valid
        WHEN RIGHT(SUBSTRING(s.raw_record FROM 13 FOR 12), 1)
             !~ '[{ABCDEFGHI}JKLMNOPQR0-9]' THEN
            'acct_curr_bal: invalid overpunch char in last position: "'
            || RIGHT(SUBSTRING(s.raw_record FROM 13 FOR 12), 1) || '"'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 49 FOR 10)) != ''
             AND RTRIM(SUBSTRING(s.raw_record FROM 49 FOR 10)) !~ '^\d{4}-\d{2}-\d{2}$' THEN
            'acct_open_date invalid date format: "'
            || RTRIM(SUBSTRING(s.raw_record FROM 49 FOR 10)) || '"'
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_account s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 122
      OR SUBSTRING(s.raw_record FROM 1 FOR 11) !~ '^\d{11}$'
      OR UPPER(SUBSTRING(s.raw_record FROM 12 FOR 1)) NOT IN ('Y', 'N')
      OR RIGHT(SUBSTRING(s.raw_record FROM 13 FOR 12), 1)
         !~ '[{ABCDEFGHI}JKLMNOPQR0-9]'
      OR (
          RTRIM(SUBSTRING(s.raw_record FROM 49 FOR 10)) != ''
          AND RTRIM(SUBSTRING(s.raw_record FROM 49 FOR 10)) !~ '^\d{4}-\d{2}-\d{2}$'
      )
  );

-- ============================================================================
-- 5. CARD validation  (carddata.txt, RECLN=150)
-- ============================================================================
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'card',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 91 THEN
            'Record length < 91 (expected 150 for RECLN 150)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16)) = '' THEN
            'NULL/empty card_num (PK field)'
        WHEN SUBSTRING(s.raw_record FROM 17 FOR 11) !~ '^\d{11}$' THEN
            'card_acct_id is not a valid 11-digit number: "'
            || SUBSTRING(s.raw_record FROM 17 FOR 11) || '"'
        WHEN SUBSTRING(s.raw_record FROM 28 FOR 3) !~ '^\d{3}$' THEN
            'card_cvv_cd is not a valid 3-digit number: "'
            || SUBSTRING(s.raw_record FROM 28 FOR 3) || '"'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 31 FOR 50)) = '' THEN
            'NULL/empty card_embossed_name (required)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 81 FOR 10)) != ''
             AND RTRIM(SUBSTRING(s.raw_record FROM 81 FOR 10)) !~ '^\d{4}-\d{2}-\d{2}$' THEN
            'card_expiration_date invalid date format: "'
            || RTRIM(SUBSTRING(s.raw_record FROM 81 FOR 10)) || '"'
        -- Referential: card_acct_id must exist
        WHEN NOT EXISTS (
            SELECT 1 FROM carddemo.account a
            WHERE a.acct_id = SUBSTRING(s.raw_record FROM 17 FOR 11)::BIGINT
        ) AND NOT EXISTS (
            SELECT 1 FROM carddemo.stg_account sa
            WHERE sa.load_batch_id = :batch_id
              AND SUBSTRING(sa.raw_record FROM 1 FOR 11) =
                  SUBSTRING(s.raw_record FROM 17 FOR 11)
        ) THEN
            'Referential integrity: card_acct_id '
            || SUBSTRING(s.raw_record FROM 17 FOR 11)
            || ' not found in account table or current batch'
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_card s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 91
      OR RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16)) = ''
      OR SUBSTRING(s.raw_record FROM 17 FOR 11) !~ '^\d{11}$'
      OR SUBSTRING(s.raw_record FROM 28 FOR 3) !~ '^\d{3}$'
      OR RTRIM(SUBSTRING(s.raw_record FROM 31 FOR 50)) = ''
      OR (
          RTRIM(SUBSTRING(s.raw_record FROM 81 FOR 10)) != ''
          AND RTRIM(SUBSTRING(s.raw_record FROM 81 FOR 10)) !~ '^\d{4}-\d{2}-\d{2}$'
      )
      OR (
          NOT EXISTS (
              SELECT 1 FROM carddemo.account a
              WHERE a.acct_id = SUBSTRING(s.raw_record FROM 17 FOR 11)::BIGINT
          )
          AND NOT EXISTS (
              SELECT 1 FROM carddemo.stg_account sa
              WHERE sa.load_batch_id = :batch_id
                AND SUBSTRING(sa.raw_record FROM 1 FOR 11) =
                    SUBSTRING(s.raw_record FROM 17 FOR 11)
          )
      )
  );

-- ============================================================================
-- 6. CARD XREF validation  (cardxref.txt, RECLN=50)
-- ============================================================================
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'card_xref',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 36 THEN
            'Record length < 36 (expected 50 for RECLN 50)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16)) = '' THEN
            'NULL/empty xref_card_num (PK field)'
        WHEN SUBSTRING(s.raw_record FROM 17 FOR 9) !~ '^\d{9}$' THEN
            'xref_cust_id is not a valid 9-digit number: "'
            || SUBSTRING(s.raw_record FROM 17 FOR 9) || '"'
        WHEN SUBSTRING(s.raw_record FROM 26 FOR 11) !~ '^\d{11}$' THEN
            'xref_acct_id is not a valid 11-digit number: "'
            || SUBSTRING(s.raw_record FROM 26 FOR 11) || '"'
        -- Referential: card_num must exist
        WHEN NOT EXISTS (
            SELECT 1 FROM carddemo.card c
            WHERE c.card_num = RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16))
        ) AND NOT EXISTS (
            SELECT 1 FROM carddemo.stg_card sc
            WHERE sc.load_batch_id = :batch_id
              AND RTRIM(SUBSTRING(sc.raw_record FROM 1 FOR 16)) =
                  RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16))
        ) THEN
            'Referential integrity: xref_card_num "'
            || RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16))
            || '" not found in card table or current batch'
        -- Referential: cust_id must exist
        WHEN NOT EXISTS (
            SELECT 1 FROM carddemo.customer cu
            WHERE cu.cust_id = SUBSTRING(s.raw_record FROM 17 FOR 9)::BIGINT
        ) AND NOT EXISTS (
            SELECT 1 FROM carddemo.stg_customer sc
            WHERE sc.load_batch_id = :batch_id
              AND SUBSTRING(sc.raw_record FROM 1 FOR 9) =
                  SUBSTRING(s.raw_record FROM 17 FOR 9)
        ) THEN
            'Referential integrity: xref_cust_id '
            || SUBSTRING(s.raw_record FROM 17 FOR 9)
            || ' not found in customer table or current batch'
        -- Referential: acct_id must exist
        WHEN NOT EXISTS (
            SELECT 1 FROM carddemo.account a
            WHERE a.acct_id = SUBSTRING(s.raw_record FROM 26 FOR 11)::BIGINT
        ) AND NOT EXISTS (
            SELECT 1 FROM carddemo.stg_account sa
            WHERE sa.load_batch_id = :batch_id
              AND SUBSTRING(sa.raw_record FROM 1 FOR 11) =
                  SUBSTRING(s.raw_record FROM 26 FOR 11)
        ) THEN
            'Referential integrity: xref_acct_id '
            || SUBSTRING(s.raw_record FROM 26 FOR 11)
            || ' not found in account table or current batch'
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_card_xref s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 36
      OR RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16)) = ''
      OR SUBSTRING(s.raw_record FROM 17 FOR 9) !~ '^\d{9}$'
      OR SUBSTRING(s.raw_record FROM 26 FOR 11) !~ '^\d{11}$'
      OR (
          NOT EXISTS (
              SELECT 1 FROM carddemo.card c
              WHERE c.card_num = RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16))
          )
          AND NOT EXISTS (
              SELECT 1 FROM carddemo.stg_card sc
              WHERE sc.load_batch_id = :batch_id
                AND RTRIM(SUBSTRING(sc.raw_record FROM 1 FOR 16)) =
                    RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16))
          )
      )
      OR (
          NOT EXISTS (
              SELECT 1 FROM carddemo.customer cu
              WHERE cu.cust_id = SUBSTRING(s.raw_record FROM 17 FOR 9)::BIGINT
          )
          AND NOT EXISTS (
              SELECT 1 FROM carddemo.stg_customer sc
              WHERE sc.load_batch_id = :batch_id
                AND SUBSTRING(sc.raw_record FROM 1 FOR 9) =
                    SUBSTRING(s.raw_record FROM 17 FOR 9)
          )
      )
      OR (
          NOT EXISTS (
              SELECT 1 FROM carddemo.account a
              WHERE a.acct_id = SUBSTRING(s.raw_record FROM 26 FOR 11)::BIGINT
          )
          AND NOT EXISTS (
              SELECT 1 FROM carddemo.stg_account sa
              WHERE sa.load_batch_id = :batch_id
                AND SUBSTRING(sa.raw_record FROM 1 FOR 11) =
                    SUBSTRING(s.raw_record FROM 26 FOR 11)
          )
      )
  );

-- ============================================================================
-- 7. DISCLOSURE GROUP validation  (discgrp.txt, RECLN=50)
-- ============================================================================
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'disclosure_group',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 22 THEN
            'Record length < 22 (expected 50 for RECLN 50)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 10)) = '' THEN
            'NULL/empty dis_acct_group_id (composite PK part 1)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 11 FOR 2)) = '' THEN
            'NULL/empty dis_tran_type_cd (composite PK part 2)'
        WHEN SUBSTRING(s.raw_record FROM 13 FOR 4) !~ '^\d{4}$' THEN
            'dis_tran_cat_cd is not a valid 4-digit number: "'
            || SUBSTRING(s.raw_record FROM 13 FOR 4) || '"'
        WHEN RIGHT(SUBSTRING(s.raw_record FROM 17 FOR 6), 1)
             !~ '[{ABCDEFGHI}JKLMNOPQR0-9]' THEN
            'dis_int_rate: invalid overpunch char: "'
            || RIGHT(SUBSTRING(s.raw_record FROM 17 FOR 6), 1) || '"'
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_disclosure_group s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 22
      OR RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 10)) = ''
      OR RTRIM(SUBSTRING(s.raw_record FROM 11 FOR 2)) = ''
      OR SUBSTRING(s.raw_record FROM 13 FOR 4) !~ '^\d{4}$'
      OR RIGHT(SUBSTRING(s.raw_record FROM 17 FOR 6), 1)
         !~ '[{ABCDEFGHI}JKLMNOPQR0-9]'
  );

-- ============================================================================
-- 8. TRANSACTION CATEGORY BALANCE validation  (tcatbal.txt, RECLN=50)
-- ============================================================================
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'tran_cat_balance',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 28 THEN
            'Record length < 28 (expected 50 for RECLN 50)'
        WHEN SUBSTRING(s.raw_record FROM 1 FOR 11) !~ '^\d{11}$' THEN
            'trancat_acct_id is not a valid 11-digit number: "'
            || SUBSTRING(s.raw_record FROM 1 FOR 11) || '"'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 12 FOR 2)) = '' THEN
            'NULL/empty trancat_type_cd'
        WHEN SUBSTRING(s.raw_record FROM 14 FOR 4) !~ '^\d{4}$' THEN
            'trancat_cd is not a valid 4-digit number: "'
            || SUBSTRING(s.raw_record FROM 14 FOR 4) || '"'
        WHEN RIGHT(SUBSTRING(s.raw_record FROM 18 FOR 11), 1)
             !~ '[{ABCDEFGHI}JKLMNOPQR0-9]' THEN
            'tran_cat_bal: invalid overpunch char: "'
            || RIGHT(SUBSTRING(s.raw_record FROM 18 FOR 11), 1) || '"'
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_tran_cat_bal s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 28
      OR SUBSTRING(s.raw_record FROM 1 FOR 11) !~ '^\d{11}$'
      OR RTRIM(SUBSTRING(s.raw_record FROM 12 FOR 2)) = ''
      OR SUBSTRING(s.raw_record FROM 14 FOR 4) !~ '^\d{4}$'
      OR RIGHT(SUBSTRING(s.raw_record FROM 18 FOR 11), 1)
         !~ '[{ABCDEFGHI}JKLMNOPQR0-9]'
  );

-- ============================================================================
-- 9. TRANSACTION validation  (dailytran.txt, RECLN=350)
-- ============================================================================
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, source_line_num, raw_record, rejection_reason)
SELECT
    s.load_batch_id,
    'transaction',
    s.source_line_num,
    s.raw_record,
    CASE
        WHEN LENGTH(s.raw_record) < 330 THEN
            'Record length < 330 (expected 350 for RECLN 350)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16)) = '' THEN
            'NULL/empty tran_id (PK field)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 17 FOR 2)) = '' THEN
            'NULL/empty tran_type_cd (required)'
        WHEN SUBSTRING(s.raw_record FROM 19 FOR 4) !~ '^\d{4}$' THEN
            'tran_cat_cd is not a valid 4-digit number: "'
            || SUBSTRING(s.raw_record FROM 19 FOR 4) || '"'
        WHEN RIGHT(SUBSTRING(s.raw_record FROM 133 FOR 11), 1)
             !~ '[{ABCDEFGHI}JKLMNOPQR0-9]' THEN
            'tran_amt: invalid overpunch char: "'
            || RIGHT(SUBSTRING(s.raw_record FROM 133 FOR 11), 1) || '"'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 263 FOR 16)) = '' THEN
            'NULL/empty tran_card_num (required FK)'
        WHEN RTRIM(SUBSTRING(s.raw_record FROM 279 FOR 26)) != ''
             AND RTRIM(SUBSTRING(s.raw_record FROM 279 FOR 26))
                 !~ '^\d{4}-\d{2}-\d{2}' THEN
            'tran_orig_ts invalid timestamp format: "'
            || RTRIM(SUBSTRING(s.raw_record FROM 279 FOR 26)) || '"'
        -- Referential: tran_type_cd
        WHEN NOT EXISTS (
            SELECT 1 FROM carddemo.transaction_type t
            WHERE t.tran_type_cd = SUBSTRING(s.raw_record FROM 17 FOR 2)
        ) AND NOT EXISTS (
            SELECT 1 FROM carddemo.stg_tran_type st
            WHERE st.load_batch_id = :batch_id
              AND SUBSTRING(st.raw_record FROM 1 FOR 2) =
                  SUBSTRING(s.raw_record FROM 17 FOR 2)
        ) THEN
            'Referential integrity: tran_type_cd "'
            || SUBSTRING(s.raw_record FROM 17 FOR 2)
            || '" not found in transaction_type'
        -- Referential: card_num must exist
        WHEN NOT EXISTS (
            SELECT 1 FROM carddemo.card c
            WHERE c.card_num = RTRIM(SUBSTRING(s.raw_record FROM 263 FOR 16))
        ) AND NOT EXISTS (
            SELECT 1 FROM carddemo.stg_card sc
            WHERE sc.load_batch_id = :batch_id
              AND RTRIM(SUBSTRING(sc.raw_record FROM 1 FOR 16)) =
                  RTRIM(SUBSTRING(s.raw_record FROM 263 FOR 16))
        ) THEN
            'Referential integrity: tran_card_num "'
            || RTRIM(SUBSTRING(s.raw_record FROM 263 FOR 16))
            || '" not found in card table or current batch'
        ELSE 'Unknown validation failure'
    END
FROM carddemo.stg_transaction s
WHERE s.load_batch_id = :batch_id
  AND (
      LENGTH(s.raw_record) < 330
      OR RTRIM(SUBSTRING(s.raw_record FROM 1 FOR 16)) = ''
      OR RTRIM(SUBSTRING(s.raw_record FROM 17 FOR 2)) = ''
      OR SUBSTRING(s.raw_record FROM 19 FOR 4) !~ '^\d{4}$'
      OR RIGHT(SUBSTRING(s.raw_record FROM 133 FOR 11), 1)
         !~ '[{ABCDEFGHI}JKLMNOPQR0-9]'
      OR RTRIM(SUBSTRING(s.raw_record FROM 263 FOR 16)) = ''
      OR (
          RTRIM(SUBSTRING(s.raw_record FROM 279 FOR 26)) != ''
          AND RTRIM(SUBSTRING(s.raw_record FROM 279 FOR 26))
              !~ '^\d{4}-\d{2}-\d{2}'
      )
      OR (
          NOT EXISTS (
              SELECT 1 FROM carddemo.transaction_type t
              WHERE t.tran_type_cd = SUBSTRING(s.raw_record FROM 17 FOR 2)
          )
          AND NOT EXISTS (
              SELECT 1 FROM carddemo.stg_tran_type st
              WHERE st.load_batch_id = :batch_id
                AND SUBSTRING(st.raw_record FROM 1 FOR 2) =
                    SUBSTRING(s.raw_record FROM 17 FOR 2)
          )
      )
      OR (
          NOT EXISTS (
              SELECT 1 FROM carddemo.card c
              WHERE c.card_num = RTRIM(SUBSTRING(s.raw_record FROM 263 FOR 16))
          )
          AND NOT EXISTS (
              SELECT 1 FROM carddemo.stg_card sc
              WHERE sc.load_batch_id = :batch_id
                AND RTRIM(SUBSTRING(sc.raw_record FROM 1 FOR 16)) =
                    RTRIM(SUBSTRING(s.raw_record FROM 263 FOR 16))
          )
      )
  );

-- ============================================================================
-- Summary: count rejected records per entity for this batch
-- ============================================================================
UPDATE carddemo.etl_batch_log
SET    records_rejected = (
           SELECT COUNT(*)
           FROM   carddemo.etl_rejected_records r
           WHERE  r.batch_id = :batch_id
       )
WHERE  batch_id = :batch_id;
