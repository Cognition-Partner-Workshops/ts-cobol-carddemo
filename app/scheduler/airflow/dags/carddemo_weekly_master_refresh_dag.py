"""
Airflow DAG: CardDemo Weekly Master-Data Refresh

Converted from Control-M:
  * FOLDER  WEEKLY-TransactionTypesDBRefresh  (MNTTRDB2)
  * SMART_FOLDER WEEKLY-DisclosureGroupsRefresh
        CLOSEFIL -> DISCGRP -> WAITSTEP -> OPENFIL
  * SMART_FOLDER WEEKLY-TransactionTypesDBRefresh
        TRANEXTR

Both SMART_FOLDERs depend on MNTTRDB2 completing successfully (INCOND).
Schedule: every Saturday (mirrors DAYS="SA" in Control-M).
"""

from __future__ import annotations

from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.empty import EmptyOperator
from airflow.utils.trigger_rule import TriggerRule

# ---------------------------------------------------------------------------
# Alert helpers
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
# Default arguments
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
    dag_id="carddemo_weekly_master_refresh",
    description=(
        "Weekly master-data refresh pipeline. "
        "Runs MNTTRDB2 first, then fans out to "
        "DisclosureGroupsRefresh and TransactionTypesDBRefresh branches."
    ),
    default_args=default_args,
    schedule_interval="0 2 * * 6",  # Saturday at 02:00 UTC
    start_date=datetime(2025, 9, 9),
    catchup=False,
    dagrun_timeout=timedelta(hours=23),
    tags=["carddemo", "weekly", "master-refresh"],
    max_active_runs=1,
) as dag:

    start = EmptyOperator(task_id="start")

    # -----------------------------------------------------------------------
    # MNTTRDB2 – Maintain / refresh Transaction-Types DB2 table
    # This is the root job that both SMART_FOLDERs depend on.
    # -----------------------------------------------------------------------
    mnttrdb2 = BashOperator(
        task_id="mnttrdb2",
        bash_command=(
            "echo 'Executing MNTTRDB2 – refreshing Transaction-Types in DB2' && "
            "{{ var.value.get('carddemo_mnttrdb2_cmd', 'echo MNTTRDB2 placeholder') }}"
        ),
        doc="Refresh the Transaction-Types reference table in DB2.",
    )

    # ===== Branch 1: Disclosure Groups Refresh =============================
    # Control-M chain: CLOSEFIL -> DISCGRP -> WAITSTEP -> OPENFIL

    discgrp_close_files = BashOperator(
        task_id="discgrp_closefil",
        bash_command=(
            "echo 'Executing DisclosureGroupsRefresh CLOSEFIL' && "
            "{{ var.value.get('carddemo_closefil_cmd', 'echo CLOSEFIL placeholder') }}"
        ),
        doc="Close VSAM files before refreshing Disclosure Groups.",
    )

    discgrp_refresh = BashOperator(
        task_id="discgrp_refresh",
        bash_command=(
            "echo 'Executing DISCGRP – refreshing Disclosure Groups' && "
            "{{ var.value.get('carddemo_discgrp_cmd', 'echo DISCGRP placeholder') }}"
        ),
        doc="Refresh the Disclosure Groups reference data.",
    )

    discgrp_wait_step = BashOperator(
        task_id="discgrp_waitstep",
        bash_command=(
            "echo 'Executing DisclosureGroupsRefresh WAITSTEP' && "
            "{{ var.value.get('carddemo_waitstep_cmd', 'sleep 10 && echo WAITSTEP complete') }}"
        ),
        doc="Wait for I/O quiesce after Disclosure Groups refresh.",
    )

    discgrp_open_files = BashOperator(
        task_id="discgrp_openfil",
        bash_command=(
            "echo 'Executing DisclosureGroupsRefresh OPENFIL' && "
            "{{ var.value.get('carddemo_openfil_cmd', 'echo OPENFIL placeholder') }}"
        ),
        doc="Reopen VSAM files after Disclosure Groups refresh.",
    )

    # ===== Branch 2: Transaction Types DB Refresh ==========================
    # Control-M: single job TRANEXTR depends on MNTTRDB2

    tranextr = BashOperator(
        task_id="tranextr",
        bash_command=(
            "echo 'Executing TRANEXTR – extracting transaction types' && "
            "{{ var.value.get('carddemo_tranextr_cmd', 'echo TRANEXTR placeholder') }}"
        ),
        doc="Extract refreshed transaction-type data for downstream use.",
    )

    # ===== Convergence =====================================================

    end = EmptyOperator(
        task_id="end",
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    # -- Dependency graph ---------------------------------------------------
    # MNTTRDB2 is the root; two parallel branches fan out from it.
    start >> mnttrdb2

    # Branch 1 – Disclosure Groups
    mnttrdb2 >> discgrp_close_files >> discgrp_refresh >> discgrp_wait_step >> discgrp_open_files >> end

    # Branch 2 – Transaction Types extract
    mnttrdb2 >> tranextr >> end
