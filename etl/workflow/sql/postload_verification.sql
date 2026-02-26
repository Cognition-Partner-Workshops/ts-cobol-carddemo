-- ============================================================================
-- CardDemo ETL: Post-Load Verification SQL
-- Informatica-style post-session SQL executed after staging-to-target load.
-- Verifies row counts, referential integrity, and data quality.
-- ============================================================================
-- Bind: $1 = batch_id (BIGINT)
-- ============================================================================

SET search_path TO carddemo;

-- ============================================================================
-- 1. Record count reconciliation
--    Compare staged records vs. (loaded + rejected) per entity.
-- ============================================================================

-- Transaction Type
UPDATE carddemo.etl_batch_log
SET    records_validated = sub.cnt
FROM (
    SELECT COUNT(*) AS cnt
    FROM   carddemo.stg_tran_type
    WHERE  load_batch_id = $1
) sub
WHERE  batch_id = $1
  AND  entity_name = 'transaction_type';

-- Customer
UPDATE carddemo.etl_batch_log
SET    records_validated = sub.cnt
FROM (
    SELECT COUNT(*) AS cnt
    FROM   carddemo.stg_customer
    WHERE  load_batch_id = $1
) sub
WHERE  batch_id = $1
  AND  entity_name = 'customer';

-- Account
UPDATE carddemo.etl_batch_log
SET    records_validated = sub.cnt
FROM (
    SELECT COUNT(*) AS cnt
    FROM   carddemo.stg_account
    WHERE  load_batch_id = $1
) sub
WHERE  batch_id = $1
  AND  entity_name = 'account';

-- ============================================================================
-- 2. Referential integrity post-check
--    Detect orphaned records that slipped through.
-- ============================================================================

-- Cards without a valid account
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, raw_record, rejection_reason)
SELECT
    $1,
    'card (post-load orphan)',
    c.card_num || '|' || c.card_acct_id::TEXT,
    'Post-load orphan: card_acct_id ' || c.card_acct_id
    || ' does not exist in account table'
FROM carddemo.card c
LEFT JOIN carddemo.account a ON a.acct_id = c.card_acct_id
WHERE a.acct_id IS NULL;

-- Card xrefs without a valid card
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, raw_record, rejection_reason)
SELECT
    $1,
    'card_xref (post-load orphan)',
    cx.xref_card_num || '|' || cx.xref_cust_id::TEXT || '|' || cx.xref_acct_id::TEXT,
    'Post-load orphan: xref_card_num "' || cx.xref_card_num
    || '" does not exist in card table'
FROM carddemo.card_xref cx
LEFT JOIN carddemo.card c ON c.card_num = cx.xref_card_num
WHERE c.card_num IS NULL;

-- Card xrefs without a valid customer
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, raw_record, rejection_reason)
SELECT
    $1,
    'card_xref (post-load orphan)',
    cx.xref_card_num || '|' || cx.xref_cust_id::TEXT || '|' || cx.xref_acct_id::TEXT,
    'Post-load orphan: xref_cust_id ' || cx.xref_cust_id
    || ' does not exist in customer table'
FROM carddemo.card_xref cx
LEFT JOIN carddemo.customer cu ON cu.cust_id = cx.xref_cust_id
WHERE cu.cust_id IS NULL;

-- Transactions without a valid card
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, raw_record, rejection_reason)
SELECT
    $1,
    'transaction (post-load orphan)',
    t.tran_id || '|' || t.tran_card_num,
    'Post-load orphan: tran_card_num "' || t.tran_card_num
    || '" does not exist in card table'
FROM carddemo.transaction t
LEFT JOIN carddemo.card c ON c.card_num = t.tran_card_num
WHERE c.card_num IS NULL;

-- Transactions with invalid type/category
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, raw_record, rejection_reason)
SELECT
    $1,
    'transaction (post-load orphan)',
    t.tran_id || '|' || t.tran_type_cd || '|' || t.tran_cat_cd::TEXT,
    'Post-load orphan: tran_type_cd/tran_cat_cd ('
    || t.tran_type_cd || '/' || t.tran_cat_cd
    || ') does not exist in transaction_category'
FROM carddemo.transaction t
LEFT JOIN carddemo.transaction_category tc
    ON tc.tran_type_cd = t.tran_type_cd
   AND tc.tran_cat_cd  = t.tran_cat_cd
WHERE tc.tran_type_cd IS NULL;

-- Tran category balance without a valid account
INSERT INTO carddemo.etl_rejected_records
    (batch_id, entity_name, raw_record, rejection_reason)
SELECT
    $1,
    'tran_cat_balance (post-load orphan)',
    tcb.trancat_acct_id::TEXT || '|' || tcb.trancat_type_cd || '|' || tcb.trancat_cd::TEXT,
    'Post-load orphan: trancat_acct_id ' || tcb.trancat_acct_id
    || ' does not exist in account table'
FROM carddemo.tran_cat_balance tcb
LEFT JOIN carddemo.account a ON a.acct_id = tcb.trancat_acct_id
WHERE a.acct_id IS NULL;

-- ============================================================================
-- 3. Data quality checks
-- ============================================================================

-- Accounts with credit limit = 0 (suspicious)
DO $$
DECLARE
    v_cnt INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_cnt
    FROM carddemo.account
    WHERE acct_credit_limit = 0 AND acct_active_status = TRUE;

    IF v_cnt > 0 THEN
        RAISE NOTICE 'DATA QUALITY WARNING: % active accounts have credit_limit = 0', v_cnt;
    END IF;
END;
$$;

-- Customers with FICO score outside typical range (already constrained, but log)
DO $$
DECLARE
    v_cnt INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_cnt
    FROM carddemo.customer
    WHERE cust_fico_credit_score IS NOT NULL
      AND cust_fico_credit_score < 300;

    IF v_cnt > 0 THEN
        RAISE NOTICE 'DATA QUALITY WARNING: % customers have FICO score < 300', v_cnt;
    END IF;
END;
$$;

-- Accounts where expiration date is before open date
DO $$
DECLARE
    v_cnt INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_cnt
    FROM carddemo.account
    WHERE acct_expiration_date IS NOT NULL
      AND acct_open_date IS NOT NULL
      AND acct_expiration_date < acct_open_date;

    IF v_cnt > 0 THEN
        RAISE NOTICE 'DATA QUALITY WARNING: % accounts have expiration_date before open_date', v_cnt;
    END IF;
END;
$$;

-- ============================================================================
-- 4. Final batch status update
-- ============================================================================
UPDATE carddemo.etl_batch_log
SET    status       = 'COMPLETED',
       completed_ts = now(),
       records_rejected = (
           SELECT COUNT(*)
           FROM   carddemo.etl_rejected_records r
           WHERE  r.batch_id = $1
       )
WHERE  batch_id = $1;

-- ============================================================================
-- 5. Summary report
-- ============================================================================
SELECT
    entity_name,
    records_staged,
    records_validated,
    records_inserted,
    records_updated,
    records_rejected,
    status,
    started_ts,
    completed_ts,
    completed_ts - started_ts AS duration
FROM carddemo.etl_batch_log
WHERE batch_id = $1
ORDER BY entity_name;
