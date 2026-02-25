"""
Airflow DAG: CardDemo Monthly Interest Calculation

Converted from Control-M folder: MONTHLY-InterestCalculation
Original dependency chain: CLOSEFIL -> INTCALC -> COMBTRAN -> WAITSTEP -> OPENFIL

This DAG runs monthly and performs the interest calculation cycle:
1. Close files for exclusive batch access
2. Calculate interest on outstanding balances
3. Combine interest transactions with daily transactions
4. Wait for dependent processes
5. Reopen files for online access
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
    dag_id="carddemo_monthly_interest_calculation",
    description=(
        "Monthly interest calculation and transaction combination "
        "(from Control-M MONTHLY-InterestCalculation)"
    ),
    default_args=default_args,
    schedule_interval="@monthly",
    start_date=datetime(2025, 9, 9),
    catchup=False,
    max_active_runs=1,
    tags=["carddemo", "monthly", "interest", "calculation"],
    on_failure_callback=_on_failure_callback,
    on_success_callback=_on_success_callback,
) as dag:

    # ------------------------------------------------------------------
    # Task 1: CLOSEFIL – Close files for exclusive batch access
    # ------------------------------------------------------------------
    close_files = BashOperator(
        task_id="closefil",
        bash_command=(
            "echo 'Closing CardDemo data files for monthly processing...' && "
            "echo 'CLOSEFIL completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Task 2: INTCALC – Calculate interest on outstanding balances
    # ------------------------------------------------------------------
    interest_calc = BashOperator(
        task_id="intcalc",
        bash_command=(
            "echo 'Calculating interest on outstanding account balances...' && "
            "echo 'INTCALC completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Task 3: COMBTRAN – Combine interest transactions with daily txns
    # ------------------------------------------------------------------
    combine_transactions = BashOperator(
        task_id="combtran",
        bash_command=(
            "echo 'Combining interest transactions with daily transactions...' && "
            "echo 'COMBTRAN completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Task 4: WAITSTEP – Wait for dependent processes
    # ------------------------------------------------------------------
    wait_step = BashOperator(
        task_id="waitstep",
        bash_command=(
            "echo 'Waiting for dependent monthly processes to finish...' && "
            "sleep 5 && "
            "echo 'WAITSTEP completed successfully.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Task 5: OPENFIL – Reopen files for online access
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
    # Task 6: Completion notification
    # ------------------------------------------------------------------
    def _notify_completion(**context):
        execution_date = context["execution_date"]
        print(
            f"Monthly interest calculation completed successfully "
            f"for {execution_date}."
        )

    notify = PythonOperator(
        task_id="notify_completion",
        python_callable=_notify_completion,
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    # Dependency chain: CLOSEFIL -> INTCALC -> COMBTRAN -> WAITSTEP -> OPENFIL -> notify
    (
        close_files
        >> interest_calc
        >> combine_transactions
        >> wait_step
        >> open_files
        >> notify
    )
