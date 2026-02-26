#!/bin/bash
# ============================================================================
# CardDemo ETL: Maintenance Script
# Informatica-style maintenance (modeled after Informatica-Demo patterns)
# Handles archival of old ETL logs, purging stale staging data, and
# vacuuming target tables.
#
# Usage:
#   ./carddemo_maintenance.sh -c <pg_connection> [-r <days>] [-m <email>]
#
# Options:
#   -c <connection>   Postgres connection string  (required)
#   -r <days>         Retention period in days     (default: 90)
#   -m <mail_to>      Notification email           (optional)
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/../logs"
RETENTION_DAYS=90
PG_CONN=""
MAIL_TO=""
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

log_info()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] INFO  $*" | tee -a "$LOGFILE"; }
log_error() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR $*" | tee -a "$LOGFILE"; }

while getopts "c:r:m:" opt; do
    case $opt in
        c) PG_CONN="$OPTARG" ;;
        r) RETENTION_DAYS="$OPTARG" ;;
        m) MAIL_TO="$OPTARG" ;;
        *) echo "Usage: $0 -c <pg_conn> [-r <days>] [-m <email>]" >&2; exit 1 ;;
    esac
done

if [[ -z "$PG_CONN" ]]; then
    echo "ERROR: -c <connection_string> is required" >&2
    exit 1
fi

mkdir -p "$LOG_DIR"
LOGFILE="${LOG_DIR}/carddemo_maintenance_${TIMESTAMP}.log"

run_sql_cmd() {
    psql "$PG_CONN" -v ON_ERROR_STOP=1 -t -A -c "$1" 2>&1
}

log_info "============================================================"
log_info "CardDemo Maintenance Started"
log_info "  Retention: $RETENTION_DAYS days"
log_info "============================================================"

# -----------------------------------------------------------------------
# 1. Purge old ETL rejection records
# -----------------------------------------------------------------------
log_info "Purging ETL rejection records older than $RETENTION_DAYS days..."
deleted=$(run_sql_cmd "
    DELETE FROM carddemo.etl_rejected_records
    WHERE rejected_ts < now() - interval '${RETENTION_DAYS} days'
    RETURNING rejection_id;
" | wc -l)
log_info "  Purged $deleted rejection records"

# -----------------------------------------------------------------------
# 2. Purge old ETL batch log entries
# -----------------------------------------------------------------------
log_info "Purging ETL batch log entries older than $RETENTION_DAYS days..."
deleted=$(run_sql_cmd "
    DELETE FROM carddemo.etl_batch_log
    WHERE started_ts < now() - interval '${RETENTION_DAYS} days'
      AND status IN ('COMPLETED', 'FAILED')
    RETURNING batch_id;
" | wc -l)
log_info "  Purged $deleted batch log entries"

# -----------------------------------------------------------------------
# 3. Truncate all staging tables (safety net)
# -----------------------------------------------------------------------
log_info "Truncating staging tables..."
for tbl in stg_tran_type stg_tran_category stg_customer stg_account \
           stg_card stg_card_xref stg_disclosure_group stg_tran_cat_bal \
           stg_transaction; do
    run_sql_cmd "TRUNCATE TABLE carddemo.${tbl};" >> "$LOGFILE" 2>&1
done
log_info "  Staging tables truncated"

# -----------------------------------------------------------------------
# 4. VACUUM ANALYZE target tables
# -----------------------------------------------------------------------
log_info "Running VACUUM ANALYZE on target tables..."
for tbl in transaction_type transaction_category customer account card \
           card_xref disclosure_group tran_cat_balance transaction; do
    run_sql_cmd "VACUUM ANALYZE carddemo.${tbl};" >> "$LOGFILE" 2>&1
done
log_info "  VACUUM ANALYZE complete"

# -----------------------------------------------------------------------
# 5. Archive old log files
# -----------------------------------------------------------------------
log_info "Archiving log files older than $RETENTION_DAYS days..."
if [[ -d "$LOG_DIR" ]]; then
    find "$LOG_DIR" -name "*.log" -type f -mtime +"$RETENTION_DAYS" -exec rm -f {} \;
fi
log_info "  Old logs purged"

# -----------------------------------------------------------------------
# Done
# -----------------------------------------------------------------------
log_info "============================================================"
log_info "CardDemo Maintenance COMPLETED"
log_info "============================================================"

if [[ -n "$MAIL_TO" ]] && command -v mailx &>/dev/null; then
    echo "Maintenance completed at $(date). Log: $LOGFILE" | \
        mailx -s "CardDemo Maintenance Complete" "$MAIL_TO"
fi

exit 0
