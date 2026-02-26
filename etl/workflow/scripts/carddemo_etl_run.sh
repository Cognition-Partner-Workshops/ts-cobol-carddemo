#!/bin/bash
# ============================================================================
# CardDemo ETL Workflow Orchestrator
# Informatica-style shell script (modeled after Informatica-Demo patterns)
# Stages flat-file data, validates, transforms, and loads into Postgres.
#
# Usage:
#   ./carddemo_etl_run.sh [options]
#
# Options:
#   -d <data_dir>       Path to ASCII data directory  (required)
#   -c <connection>     Postgres connection string     (required)
#   -s <sql_dir>        Path to ETL SQL directory      (default: ../sql)
#   -q <query_file>     Path to staging_to_target.sql  (default: ../../queries/staging_to_target.sql)
#   -f <date_from>      Filter: transaction start date (optional, YYYY-MM-DD)
#   -t <date_to>        Filter: transaction end date   (optional, YYYY-MM-DD)
#   -a <account_id>     Filter: specific account ID    (optional)
#   -m <mail_to>        Notification email addresses   (optional)
#
# Exit codes:
#   0  = SUCCESS
#   1  = CONFIGURATION ERROR
#   2  = STAGING FAILURE
#   3  = VALIDATION FAILURE (rejections exceed threshold)
#   4  = LOAD FAILURE
#   5  = POST-LOAD VERIFICATION FAILURE
# ============================================================================

set -euo pipefail

# ============================================================================
# Defaults
# ============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="${SCRIPT_DIR}/../sql"
QUERY_FILE="${SCRIPT_DIR}/../../queries/staging_to_target.sql"
LOG_DIR="${SCRIPT_DIR}/../logs"
DATE_FROM=""
DATE_TO=""
ACCOUNT_ID=""
MAIL_TO=""
DATA_DIR=""
PG_CONN=""
REJECTION_THRESHOLD=10    # percentage: fail if rejected/staged > threshold
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# ============================================================================
# Functions
# ============================================================================
log_info()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] INFO  $*" | tee -a "$LOGFILE"; }
log_warn()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] WARN  $*" | tee -a "$LOGFILE"; }
log_error() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR $*" | tee -a "$LOGFILE"; }

send_notification() {
    local subject="$1"
    local body="$2"
    if [[ -n "$MAIL_TO" ]] && command -v mailx &>/dev/null; then
        echo "$body" | mailx -s "$subject" "$MAIL_TO"
    fi
}

cleanup() {
    local exit_code=$?
    if [[ $exit_code -ne 0 ]]; then
        log_error "ETL workflow failed with exit code $exit_code"
        send_notification \
            "CardDemo ETL FAILED (batch ${BATCH_ID:-unknown})" \
            "ETL run failed at $(date). Check log: $LOGFILE"
    fi
}
trap cleanup EXIT

run_sql() {
    local sql_file="$1"
    shift
    psql "$PG_CONN" -v ON_ERROR_STOP=1 -f "$sql_file" "$@" 2>&1 | tee -a "$LOGFILE"
}

run_sql_cmd() {
    psql "$PG_CONN" -v ON_ERROR_STOP=1 -t -A -c "$1" 2>&1
}

# ============================================================================
# Parse arguments
# ============================================================================
while getopts "d:c:s:q:f:t:a:m:" opt; do
    case $opt in
        d) DATA_DIR="$OPTARG" ;;
        c) PG_CONN="$OPTARG" ;;
        s) SQL_DIR="$OPTARG" ;;
        q) QUERY_FILE="$OPTARG" ;;
        f) DATE_FROM="$OPTARG" ;;
        t) DATE_TO="$OPTARG" ;;
        a) ACCOUNT_ID="$OPTARG" ;;
        m) MAIL_TO="$OPTARG" ;;
        *) echo "Usage: $0 -d <data_dir> -c <pg_conn> [-s sql_dir] [-q query_file] [-f date_from] [-t date_to] [-a acct_id] [-m email]" >&2; exit 1 ;;
    esac
done

# ============================================================================
# Validate inputs
# ============================================================================
if [[ -z "$DATA_DIR" ]]; then
    echo "ERROR: -d <data_dir> is required" >&2
    exit 1
fi
if [[ -z "$PG_CONN" ]]; then
    echo "ERROR: -c <connection_string> is required" >&2
    exit 1
fi
if [[ ! -d "$DATA_DIR" ]]; then
    echo "ERROR: Data directory does not exist: $DATA_DIR" >&2
    exit 1
fi

mkdir -p "$LOG_DIR"
LOGFILE="${LOG_DIR}/carddemo_etl_${TIMESTAMP}.log"

log_info "============================================================"
log_info "CardDemo ETL Workflow Started"
log_info "  Data directory : $DATA_DIR"
log_info "  SQL directory  : $SQL_DIR"
log_info "  Date filter    : ${DATE_FROM:-none} to ${DATE_TO:-none}"
log_info "  Account filter : ${ACCOUNT_ID:-none}"
log_info "  Log file       : $LOGFILE"
log_info "============================================================"

# ============================================================================
# STEP 0: Verify source files exist
# ============================================================================
log_info "STEP 0: Verifying source data files..."

REQUIRED_FILES=(
    "trantype.txt"
    "trancatg.txt"
    "custdata.txt"
    "acctdata.txt"
    "carddata.txt"
    "cardxref.txt"
    "discgrp.txt"
    "tcatbal.txt"
    "dailytran.txt"
)

for fname in "${REQUIRED_FILES[@]}"; do
    fpath="${DATA_DIR}/${fname}"
    if [[ ! -f "$fpath" ]]; then
        log_error "Required source file not found: $fpath"
        send_notification \
            "CardDemo ETL ABORTED: Missing file" \
            "File not found: $fpath"
        exit 1
    fi
    line_count=$(wc -l < "$fpath")
    log_info "  Found $fname ($line_count records)"
done

# ============================================================================
# STEP 1: Create batch log entry
# ============================================================================
log_info "STEP 1: Creating ETL batch log entry..."

BATCH_ID=$(run_sql_cmd "
    INSERT INTO carddemo.etl_batch_log (entity_name, source_file, status)
    VALUES ('ALL', '${DATA_DIR}', 'STARTED')
    RETURNING batch_id;
")
log_info "  Batch ID: $BATCH_ID"

# ============================================================================
# STEP 2: Stage raw records (COPY from flat files into staging tables)
# ============================================================================
log_info "STEP 2: Staging raw records from flat files..."

stage_file() {
    local table_name="$1"
    local file_name="$2"
    local file_path="${DATA_DIR}/${file_name}"
    local line_num=0
    local staged=0

    log_info "  Staging $file_name -> $table_name..."

    # Truncate staging table for this load
    run_sql_cmd "TRUNCATE TABLE carddemo.${table_name};" >> "$LOGFILE" 2>&1

    # Read each line and insert as raw_record with line number
    while IFS= read -r line || [[ -n "$line" ]]; do
        line_num=$((line_num + 1))
        # Skip empty lines
        if [[ -z "$line" ]]; then
            continue
        fi
        staged=$((staged + 1))
    done < "$file_path"

    # Bulk load using COPY with a helper approach:
    # Create a temp file with batch_id and line numbers prepended
    local tmp_file
    tmp_file=$(mktemp /tmp/carddemo_stage_XXXXXX.csv)

    local ln=0
    while IFS= read -r line || [[ -n "$line" ]]; do
        ln=$((ln + 1))
        if [[ -n "$line" ]]; then
            # Escape any backslashes and tabs for COPY
            printf '%s\t%s\t%s\t%s\n' \
                "$line" \
                "$BATCH_ID" \
                "$file_name" \
                "$ln" >> "$tmp_file"
        fi
    done < "$file_path"

    psql "$PG_CONN" -c "\COPY carddemo.${table_name} (raw_record, load_batch_id, source_file, source_line_num) FROM '${tmp_file}' WITH (FORMAT text, DELIMITER E'\t')" >> "$LOGFILE" 2>&1

    rm -f "$tmp_file"

    local actual_count
    actual_count=$(run_sql_cmd "SELECT COUNT(*) FROM carddemo.${table_name} WHERE load_batch_id = ${BATCH_ID};")
    log_info "    Staged $actual_count records from $file_name"
}

stage_file "stg_tran_type"          "trantype.txt"
stage_file "stg_tran_category"      "trancatg.txt"
stage_file "stg_customer"           "custdata.txt"
stage_file "stg_account"            "acctdata.txt"
stage_file "stg_card"               "carddata.txt"
stage_file "stg_card_xref"          "cardxref.txt"
stage_file "stg_disclosure_group"   "discgrp.txt"
stage_file "stg_tran_cat_bal"       "tcatbal.txt"
stage_file "stg_transaction"        "dailytran.txt"

# Update batch with staged counts
run_sql_cmd "
    UPDATE carddemo.etl_batch_log
    SET records_staged = (
        SELECT SUM(cnt) FROM (
            SELECT COUNT(*) AS cnt FROM carddemo.stg_tran_type       WHERE load_batch_id = ${BATCH_ID}
            UNION ALL SELECT COUNT(*) FROM carddemo.stg_tran_category WHERE load_batch_id = ${BATCH_ID}
            UNION ALL SELECT COUNT(*) FROM carddemo.stg_customer      WHERE load_batch_id = ${BATCH_ID}
            UNION ALL SELECT COUNT(*) FROM carddemo.stg_account       WHERE load_batch_id = ${BATCH_ID}
            UNION ALL SELECT COUNT(*) FROM carddemo.stg_card          WHERE load_batch_id = ${BATCH_ID}
            UNION ALL SELECT COUNT(*) FROM carddemo.stg_card_xref     WHERE load_batch_id = ${BATCH_ID}
            UNION ALL SELECT COUNT(*) FROM carddemo.stg_disclosure_group WHERE load_batch_id = ${BATCH_ID}
            UNION ALL SELECT COUNT(*) FROM carddemo.stg_tran_cat_bal  WHERE load_batch_id = ${BATCH_ID}
            UNION ALL SELECT COUNT(*) FROM carddemo.stg_transaction   WHERE load_batch_id = ${BATCH_ID}
        ) t
    )
    WHERE batch_id = ${BATCH_ID};
" >> "$LOGFILE" 2>&1

log_info "STEP 2 complete."

# ============================================================================
# STEP 3: Pre-load validation
# ============================================================================
log_info "STEP 3: Running pre-load validation..."

psql "$PG_CONN" -v ON_ERROR_STOP=1 \
    -v "batch_id=${BATCH_ID}" \
    -f "${SQL_DIR}/preload_validation.sql" >> "$LOGFILE" 2>&1

REJECTED_COUNT=$(run_sql_cmd "
    SELECT COUNT(*)
    FROM carddemo.etl_rejected_records
    WHERE batch_id = ${BATCH_ID};
")
STAGED_COUNT=$(run_sql_cmd "
    SELECT COALESCE(records_staged, 0)
    FROM carddemo.etl_batch_log
    WHERE batch_id = ${BATCH_ID};
")

log_info "  Validation complete: $REJECTED_COUNT rejections out of $STAGED_COUNT staged"

if [[ "$STAGED_COUNT" -gt 0 ]]; then
    REJECTION_PCT=$(( (REJECTED_COUNT * 100) / STAGED_COUNT ))
    if [[ $REJECTION_PCT -gt $REJECTION_THRESHOLD ]]; then
        log_error "Rejection rate ${REJECTION_PCT}% exceeds threshold ${REJECTION_THRESHOLD}%"
        run_sql_cmd "
            UPDATE carddemo.etl_batch_log
            SET status = 'FAILED',
                error_message = 'Rejection rate ${REJECTION_PCT}% exceeds threshold',
                completed_ts = now()
            WHERE batch_id = ${BATCH_ID};
        " >> "$LOGFILE" 2>&1
        send_notification \
            "CardDemo ETL FAILED: High rejection rate (batch $BATCH_ID)" \
            "Rejection rate: ${REJECTION_PCT}%. Threshold: ${REJECTION_THRESHOLD}%."
        exit 3
    fi
fi

log_info "STEP 3 complete."

# ============================================================================
# STEP 4: Load staging → target (transform & insert)
# ============================================================================
log_info "STEP 4: Loading data from staging to target tables..."

run_sql_cmd "
    UPDATE carddemo.etl_batch_log
    SET status = 'LOADING'
    WHERE batch_id = ${BATCH_ID};
" >> "$LOGFILE" 2>&1

# Build bind parameter values
PARAM_DATE_FROM="${DATE_FROM:-}"
PARAM_DATE_TO="${DATE_TO:-}"
PARAM_ACCOUNT_ID="${ACCOUNT_ID:-}"

# Execute the staging-to-target queries with bind parameters
# The query file uses $1=batch_id, $2=date_from, $3=date_to, $4=account_id
psql "$PG_CONN" -v ON_ERROR_STOP=1 \
    -c "\\set batch_id ${BATCH_ID}" \
    -f "${QUERY_FILE}" >> "$LOGFILE" 2>&1 || {
        log_error "Load step failed"
        run_sql_cmd "
            UPDATE carddemo.etl_batch_log
            SET status = 'FAILED',
                error_message = 'Load step failed - see log',
                completed_ts = now()
            WHERE batch_id = ${BATCH_ID};
        " >> "$LOGFILE" 2>&1
        exit 4
    }

# Update loaded counts
for entity_table in "transaction_type" "transaction_category" "customer" "account" \
                    "card" "card_xref" "disclosure_group" "tran_cat_balance" "transaction"; do
    count=$(run_sql_cmd "SELECT COUNT(*) FROM carddemo.${entity_table};")
    log_info "  Target table carddemo.${entity_table}: $count total rows"
done

log_info "STEP 4 complete."

# ============================================================================
# STEP 5: Post-load verification
# ============================================================================
log_info "STEP 5: Running post-load verification..."

psql "$PG_CONN" -v ON_ERROR_STOP=1 \
    -v "batch_id=${BATCH_ID}" \
    -f "${SQL_DIR}/postload_verification.sql" >> "$LOGFILE" 2>&1

FINAL_REJECTED=$(run_sql_cmd "
    SELECT COUNT(*)
    FROM carddemo.etl_rejected_records
    WHERE batch_id = ${BATCH_ID};
")

log_info "  Post-load verification complete. Total rejections: $FINAL_REJECTED"
log_info "STEP 5 complete."

# ============================================================================
# STEP 6: Cleanup staging tables
# ============================================================================
log_info "STEP 6: Cleaning up staging tables..."

for stg_table in "stg_tran_type" "stg_tran_category" "stg_customer" "stg_account" \
                 "stg_card" "stg_card_xref" "stg_disclosure_group" "stg_tran_cat_bal" \
                 "stg_transaction"; do
    run_sql_cmd "DELETE FROM carddemo.${stg_table} WHERE load_batch_id = ${BATCH_ID};" >> "$LOGFILE" 2>&1
done

log_info "STEP 6 complete."

# ============================================================================
# STEP 7: Archive source files
# ============================================================================
log_info "STEP 7: Archiving source files..."

ARCHIVE_DIR="${DATA_DIR}/archive/${TIMESTAMP}"
mkdir -p "$ARCHIVE_DIR"

for fname in "${REQUIRED_FILES[@]}"; do
    cp "${DATA_DIR}/${fname}" "${ARCHIVE_DIR}/${fname}"
done

log_info "  Archived to: $ARCHIVE_DIR"
log_info "STEP 7 complete."

# ============================================================================
# DONE
# ============================================================================
run_sql_cmd "
    UPDATE carddemo.etl_batch_log
    SET status = 'COMPLETED', completed_ts = now()
    WHERE batch_id = ${BATCH_ID};
" >> "$LOGFILE" 2>&1

log_info "============================================================"
log_info "CardDemo ETL Workflow COMPLETED SUCCESSFULLY"
log_info "  Batch ID      : $BATCH_ID"
log_info "  Staged records: $STAGED_COUNT"
log_info "  Rejected      : $FINAL_REJECTED"
log_info "  Log file      : $LOGFILE"
log_info "============================================================"

send_notification \
    "CardDemo ETL COMPLETED (batch $BATCH_ID)" \
    "ETL run completed successfully. Staged: $STAGED_COUNT, Rejected: $FINAL_REJECTED. Log: $LOGFILE"

exit 0
