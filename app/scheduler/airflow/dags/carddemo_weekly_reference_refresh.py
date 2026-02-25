"""
Airflow DAG: CardDemo Weekly Reference Data Refresh

Converted from Control-M folders:
  - WEEKLY-TransactionTypesDBRefresh (FOLDER + SMART_FOLDER)
  - WEEKLY-DisclosureGroupsRefresh   (SMART_FOLDER)

Original Control-M dependency chains:
  MNTTRDB2 (maintain transaction types in DB2)
    |
    +---> DisclosureGroupsRefresh: CLOSEFIL -> DISCGRP -> WAITSTEP -> OPENFIL
    +---> TransactionTypesDBRefresh: TRANEXTR

Both smart folders consume the MNTTRDB2 output condition, running in parallel
after the DB2 maintenance job finishes.

This DAG runs every Saturday (matching Control-M DAYS="SA").
"""

from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.python import PythonOperator
from airflow.utils.trigger_rule import TriggerRule

# ---------------------------------------------------------------------------
# Default arguments
# ---------------------------------------------------------------------------
default_args = {
    "owner": "carddemo",
    "depends_on_past": False,
    "email": ["carddemo-ops@example.com"],
    "email_on_failure": True,
    "email_on_retry": True,
    "retries": 5,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(hours=23),
    "sla": timedelta(hours=20),
}


def _on_failure_callback(context):
    """Log detailed failure information for alerting integrations."""
    dag_id = context.get("dag").dag_id
    task_id = context.get("task_instance").task_id
    execution_date = context.get("execution_date")
    exception = context.get("exception")
    log_url = context.get("task_instance").log_url
    print(
        f"FAILURE ALERT | dag={dag_id} task={task_id} "
        f"execution_date={execution_date} exception={exception} "
        f"log_url={log_url}"
    )


def _on_success_callback(context):
    """Log successful DAG completion."""
    dag_id = context.get("dag").dag_id
    execution_date = context.get("execution_date")
    print(f"SUCCESS | dag={dag_id} execution_date={execution_date}")


with DAG(
    dag_id="carddemo_weekly_reference_refresh",
    description=(
        "Weekly reference-data refresh: transaction types DB2 maintenance, "
        "disclosure groups refresh, and transaction extraction "
        "(from Control-M WEEKLY folders)"
    ),
    default_args=default_args,
    schedule_interval="0 0 * * 6",  # Every Saturday
    start_date=datetime(2025, 9, 9),
    catchup=False,
    max_active_runs=1,
    tags=["carddemo", "weekly", "reference", "db2", "disclosure"],
    on_failure_callback=_on_failure_callback,
    on_success_callback=_on_success_callback,
) as dag:

    # ======================================================================
    # Trigger job: MNTTRDB2 – Maintain Transaction Types in DB2
    # (FOLDER: WEEKLY-TransactionTypesDBRefresh)
    # ======================================================================
    mnttrdb2 = BashOperator(
        task_id="mnttrdb2",
        bash_command=(
            "echo 'Maintaining transaction types in DB2...' && "
            "echo 'MNTTRDB2 completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ======================================================================
    # Branch A: DisclosureGroupsRefresh SMART_FOLDER
    # Chain: CLOSEFIL -> DISCGRP -> WAITSTEP -> OPENFIL
    # ======================================================================
    discgrp_close_files = BashOperator(
        task_id="discgrp_closefil",
        bash_command=(
            "echo 'Closing files for Disclosure Groups refresh...' && "
            "echo 'CLOSEFIL (DisclosureGroups) completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    discgrp_refresh = BashOperator(
        task_id="discgrp_refresh",
        bash_command=(
            "echo 'Refreshing Disclosure Groups reference data...' && "
            "echo 'DISCGRP completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    discgrp_wait = BashOperator(
        task_id="discgrp_waitstep",
        bash_command=(
            "echo 'Waiting for Disclosure Groups dependent processes...' && "
            "sleep 5 && "
            "echo 'WAITSTEP (DisclosureGroups) completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    discgrp_open_files = BashOperator(
        task_id="discgrp_openfil",
        bash_command=(
            "echo 'Reopening files after Disclosure Groups refresh...' && "
            "echo 'OPENFIL (DisclosureGroups) completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ======================================================================
    # Branch B: TransactionTypesDBRefresh SMART_FOLDER
    # Single job: TRANEXTR
    # ======================================================================
    tranextr = BashOperator(
        task_id="tranextr",
        bash_command=(
            "echo 'Extracting refreshed transaction types...' && "
            "echo 'TRANEXTR completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ======================================================================
    # Join: Wait for both branches before notifying
    # ======================================================================
    def _notify_completion(**context):
        execution_date = context["execution_date"]
        print(
            f"Weekly reference-data refresh completed successfully "
            f"for {execution_date}."
        )

    notify = PythonOperator(
        task_id="notify_completion",
        python_callable=_notify_completion,
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    # ------------------------------------------------------------------
    # Dependency graph
    #
    #                           +-> discgrp_closefil -> discgrp_refresh
    #                           |      -> discgrp_waitstep -> discgrp_openfil -+
    #  mnttrdb2 --+             |                                              +--> notify
    #             |             +-> tranextr -----------------------------------+
    #             +-------------+
    # ------------------------------------------------------------------
    mnttrdb2 >> discgrp_close_files >> discgrp_refresh >> discgrp_wait >> discgrp_open_files
    mnttrdb2 >> tranextr
    [discgrp_open_files, tranextr] >> notify
