"""
Airflow DAG: CardDemo Daily Transaction Backup

Converted from Control-M folder DAILY-TransactionBackup.
Original dependency chain (INCOND/OUTCOND):
    CLOSEFIL -> TRANBKP -> WAITSTEP -> OPENFIL

Schedule: Daily
Retries : up to 5 (mirrors MAXRERUN=5 in Control-M)
Timeout : 23:00 same-day deadline (dagrun_timeout)
"""

from __future__ import annotations

from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.empty import EmptyOperator
from airflow.utils.trigger_rule import TriggerRule

# ---------------------------------------------------------------------------
# Shared alert helpers
# ---------------------------------------------------------------------------

_ALERT_EMAIL = ["carddemo-ops@example.com"]


def _on_failure_callback(context: dict) -> None:
    """Log task failure details for downstream alerting integrations."""
    task_instance = context.get("task_instance")
    dag_id = context.get("dag").dag_id if context.get("dag") else "unknown"
    task_id = task_instance.task_id if task_instance else "unknown"
    execution_date = context.get("execution_date", "unknown")
    exception = context.get("exception", "N/A")
    print(
        f"ALERT  [FAILURE] dag={dag_id} task={task_id} "
        f"execution_date={execution_date} exception={exception}"
    )


def _on_retry_callback(context: dict) -> None:
    """Log retry attempts."""
    task_instance = context.get("task_instance")
    dag_id = context.get("dag").dag_id if context.get("dag") else "unknown"
    task_id = task_instance.task_id if task_instance else "unknown"
    try_number = task_instance.try_number if task_instance else "?"
    print(
        f"ALERT  [RETRY] dag={dag_id} task={task_id} try={try_number}"
    )


# ---------------------------------------------------------------------------
# Default arguments (applied to every task in this DAG)
# ---------------------------------------------------------------------------

default_args = {
    "owner": "carddemo-ops",
    "depends_on_past": False,
    "email": _ALERT_EMAIL,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 5,
    "retry_delay": timedelta(minutes=2),
    "retry_exponential_backoff": True,
    "max_retry_delay": timedelta(minutes=30),
    "on_failure_callback": _on_failure_callback,
    "on_retry_callback": _on_retry_callback,
    "execution_timeout": timedelta(hours=4),
}

# ---------------------------------------------------------------------------
# DAG definition
# ---------------------------------------------------------------------------

with DAG(
    dag_id="carddemo_daily_transaction_backup",
    description=(
        "Daily transaction backup pipeline converted from "
        "Control-M folder DAILY-TransactionBackup. "
        "Chain: CLOSEFIL -> TRANBKP -> WAITSTEP -> OPENFIL"
    ),
    default_args=default_args,
    schedule_interval="@daily",
    start_date=datetime(2025, 9, 9),
    catchup=False,
    dagrun_timeout=timedelta(hours=23),
    tags=["carddemo", "daily", "transaction-backup"],
    max_active_runs=1,
) as dag:

    start = EmptyOperator(task_id="start")

    # -- CLOSEFIL: close VSAM files before backup -------------------------
    close_files = BashOperator(
        task_id="closefil",
        bash_command=(
            "echo 'Executing CLOSEFIL – closing VSAM files for backup' && "
            "{{ var.value.get('carddemo_closefil_cmd', 'echo CLOSEFIL placeholder') }}"
        ),
        doc="Close all VSAM files to prepare for the daily transaction backup.",
    )

    # -- TRANBKP: back up transaction data --------------------------------
    transaction_backup = BashOperator(
        task_id="tranbkp",
        bash_command=(
            "echo 'Executing TRANBKP – backing up transaction data' && "
            "{{ var.value.get('carddemo_tranbkp_cmd', 'echo TRANBKP placeholder') }}"
        ),
        doc="Back up the daily transaction data to the designated archive.",
    )

    # -- WAITSTEP: quiesce / wait for I/O to settle -----------------------
    wait_step = BashOperator(
        task_id="waitstep",
        bash_command=(
            "echo 'Executing WAITSTEP – waiting for I/O quiesce' && "
            "{{ var.value.get('carddemo_waitstep_cmd', 'sleep 10 && echo WAITSTEP complete') }}"
        ),
        doc="Wait for all pending I/O operations to complete before reopening files.",
    )

    # -- OPENFIL: reopen VSAM files after backup --------------------------
    open_files = BashOperator(
        task_id="openfil",
        bash_command=(
            "echo 'Executing OPENFIL – reopening VSAM files' && "
            "{{ var.value.get('carddemo_openfil_cmd', 'echo OPENFIL placeholder') }}"
        ),
        doc="Reopen VSAM files after the backup completes successfully.",
    )

    end = EmptyOperator(
        task_id="end",
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    # -- Dependency chain (mirrors Control-M INCOND/OUTCOND) ---------------
    start >> close_files >> transaction_backup >> wait_step >> open_files >> end
