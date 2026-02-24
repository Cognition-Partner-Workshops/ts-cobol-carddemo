-- ============================================================
-- Flyway Migration V1: Create Transaction ID Sequence
-- ============================================================
-- Replaces the legacy browse-to-end ID generation pattern:
--   STARTBR TRANSACT with HIGH-VALUES → READPREV → +1
--
-- PostgreSQL sequences are atomic and thread-safe, eliminating
-- the race condition risk documented in BRE Section 10.1.
--
-- Business Rule: BR-AT-13 (Auto-Increment Transaction ID)
-- ============================================================

CREATE SEQUENCE transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE;

COMMENT ON SEQUENCE transaction_id_seq IS
    'Thread-safe transaction ID generator. Replaces legacy COTRN02C browse-to-end pattern (STARTBR HIGH-VALUES → READPREV → +1). BR-AT-13.';
