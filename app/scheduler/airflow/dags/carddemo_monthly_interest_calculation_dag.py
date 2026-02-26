"""
Airflow DAG: CardDemo Monthly Interest Calculation

Converted from Control-M folder MONTHLY-InterestCalculation.
Original dependency chain:
    CLOSEFIL -> INTCALC -> COMBTRAN -> WAITSTEP -> OPENFIL

Schedule: 1st day of every month
Retries : up to 5 (mirrors MAXRERUN=5)
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
    "retry_delay": timedelta(minutes=3),
    "retry_exponential_backoff": True,
    "max_retry_delay": timedelta(minutes=30),
    "on_failure_callback": _on_failure_callback,
    "on_retry_callback": _on_retry_callback,
    "execution_timeout": timedelta(hours=6),
}

# ---------------------------------------------------------------------------
# DAG definition
# ---------------------------------------------------------------------------

with DAG(
    dag_id="carddemo_monthly_interest_calculation",
    description=(
        "Monthly interest-calculation pipeline converted from "
        "Control-M folder MONTHLY-InterestCalculation. "
        "Chain: CLOSEFIL -> INTCALC -> COMBTRAN -> WAITSTEP -> OPENFIL"
    ),
    default_args=default_args,
    schedule_interval="0 1 1 * *",  # 1st of every month at 01:00 UTC
    start_date=datetime(2025, 9, 9),
    catchup=False,
    dagrun_timeout=timedelta(hours=23),
    tags=["carddemo", "monthly", "interest-calculation"],
    max_active_runs=1,
) as dag:

    start = EmptyOperator(task_id="start")

    # -- CLOSEFIL: close VSAM files before interest calculation ------------
    close_files = BashOperator(
        task_id="closefil",
        bash_command=(
            "echo 'Executing CLOSEFIL – closing VSAM files for interest calc' && "
            "{{ var.value.get('carddemo_closefil_cmd', 'echo CLOSEFIL placeholder') }}"
        ),
        doc="Close VSAM files to prepare for monthly interest calculation.",
    )

    # -- INTCALC: run interest calculation batch ---------------------------
    interest_calc = BashOperator(
        task_id="intcalc",
        bash_command=(
            "echo 'Executing INTCALC – computing monthly interest' && "
            "{{ var.value.get('carddemo_intcalc_cmd', 'echo INTCALC placeholder') }}"
        ),
        doc="Calculate monthly interest for all active accounts.",
    )

    # -- COMBTRAN: combine interest transactions with master file ----------
    combine_transactions = BashOperator(
        task_id="combtran",
        bash_command=(
            "echo 'Executing COMBTRAN – merging interest transactions' && "
            "{{ var.value.get('carddemo_combtran_cmd', 'echo COMBTRAN placeholder') }}"
        ),
        doc="Merge computed interest transactions into the main transaction file.",
    )

    # -- WAITSTEP: quiesce / wait for I/O to settle -----------------------
    wait_step = BashOperator(
        task_id="waitstep",
        bash_command=(
            "echo 'Executing WAITSTEP – waiting for I/O quiesce' && "
            "{{ var.value.get('carddemo_waitstep_cmd', 'sleep 10 && echo WAITSTEP complete') }}"
        ),
        doc="Wait for all pending I/O to finish before reopening files.",
    )

    # -- OPENFIL: reopen VSAM files after interest calculation -------------
    open_files = BashOperator(
        task_id="openfil",
        bash_command=(
            "echo 'Executing OPENFIL – reopening VSAM files' && "
            "{{ var.value.get('carddemo_openfil_cmd', 'echo OPENFIL placeholder') }}"
        ),
        doc="Reopen VSAM files after monthly interest calculation completes.",
    )

    end = EmptyOperator(
        task_id="end",
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    # -- Dependency chain (mirrors Control-M INCOND/OUTCOND) ---------------
    (
        start
        >> close_files
        >> interest_calc
        >> combine_transactions
        >> wait_step
        >> open_files
        >> end
    )
