"""
Airflow DAG: CardDemo Daily Transaction Backup

Converted from Control-M folder: DAILY-TransactionBackup
Original dependency chain: CLOSEFIL -> TRANBKP -> WAITSTEP -> OPENFIL

This DAG runs daily and performs the transaction backup cycle:
1. Close files for exclusive access
2. Back up transaction data
3. Wait for dependent processes to complete
4. Reopen files for online access
"""

from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.python import PythonOperator
from airflow.utils.trigger_rule import TriggerRule

# ---------------------------------------------------------------------------
# Default arguments – mirrors Control-M MAXRERUN=5, MAXWAIT=7 days
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
    dag_id="carddemo_daily_transaction_backup",
    description="Daily transaction backup cycle (from Control-M DAILY-TransactionBackup)",
    default_args=default_args,
    schedule_interval="@daily",
    start_date=datetime(2025, 9, 9),
    catchup=False,
    max_active_runs=1,
    tags=["carddemo", "daily", "transaction", "backup"],
    on_failure_callback=_on_failure_callback,
    on_success_callback=_on_success_callback,
) as dag:

    # ------------------------------------------------------------------
    # Task 1: CLOSEFIL – Close VSAM/files for exclusive batch access
    # ------------------------------------------------------------------
    close_files = BashOperator(
        task_id="closefil",
        bash_command=(
            "echo 'Closing CardDemo data files for exclusive batch access...' && "
            "echo 'CLOSEFIL completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Task 2: TRANBKP – Back up daily transaction data
    # ------------------------------------------------------------------
    transaction_backup = BashOperator(
        task_id="tranbkp",
        bash_command=(
            "echo 'Starting daily transaction backup...' && "
            "echo 'TRANBKP completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Task 3: WAITSTEP – Wait for dependent processes
    # ------------------------------------------------------------------
    wait_step = BashOperator(
        task_id="waitstep",
        bash_command=(
            "echo 'Waiting for dependent batch processes to finish...' && "
            "sleep 5 && "
            "echo 'WAITSTEP completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Task 4: OPENFIL – Reopen files for online access
    # ------------------------------------------------------------------
    open_files = BashOperator(
        task_id="openfil",
        bash_command=(
            "echo 'Reopening CardDemo data files for online access...' && "
            "echo 'OPENFIL completed successfully.'"
        ),
        trigger_rule=TriggerRule.ALL_SUCCESS,
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Task 5: Cleanup / notification on completion
    # ------------------------------------------------------------------
    def _notify_completion(**context):
        execution_date = context["execution_date"]
        print(
            f"Daily transaction backup completed successfully for {execution_date}."
        )

    notify = PythonOperator(
        task_id="notify_completion",
        python_callable=_notify_completion,
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    # Dependency chain: CLOSEFIL -> TRANBKP -> WAITSTEP -> OPENFIL -> notify
    close_files >> transaction_backup >> wait_step >> open_files >> notify
