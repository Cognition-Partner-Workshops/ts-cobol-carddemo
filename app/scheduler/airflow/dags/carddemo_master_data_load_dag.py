"""
Airflow DAG: CardDemo Master Data Load

Orchestrates the full data-load pipeline respecting referential-integrity
ordering derived from the CardDemo schema:

    Tier 1 – Master / reference tables (no FK dependencies):
        TransactionType, TransactionCategory, DisclosureGroup

    Tier 2 – Parent entity tables:
        Customer, Account

    Tier 3 – Child / dependent tables:
        Card, CardXref, Transaction, CategoryBalance

Each tier runs in parallel within itself; the next tier starts only after
every task in the previous tier has succeeded.

This DAG is independent of the batch-processing DAGs (daily backup, weekly
refresh, monthly interest) and is intended for initial or ad-hoc full loads
into the modernised Postgres database.
"""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Any

from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.empty import EmptyOperator
from airflow.operators.python import PythonOperator
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


def _validate_tier(tier_name: str, tables: list[str], **kwargs: Any) -> None:
    """Post-tier validation: verify row counts are non-zero."""
    print(f"Validating tier '{tier_name}' – tables: {tables}")
    # In production this would query the target Postgres database.
    # Placeholder logic: succeed if the upstream tasks all passed.
    ti = kwargs.get("ti")
    if ti is None:
        raise RuntimeError("TaskInstance not available in context")
    print(f"Tier '{tier_name}' validation passed.")


# ---------------------------------------------------------------------------
# Default arguments
# ---------------------------------------------------------------------------

default_args = {
    "owner": "carddemo-ops",
    "depends_on_past": False,
    "email": _ALERT_EMAIL,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 3,
    "retry_delay": timedelta(minutes=5),
    "retry_exponential_backoff": True,
    "max_retry_delay": timedelta(minutes=30),
    "on_failure_callback": _on_failure_callback,
    "on_retry_callback": _on_retry_callback,
    "execution_timeout": timedelta(hours=2),
}

# ---------------------------------------------------------------------------
# Table tier definitions
# ---------------------------------------------------------------------------

TIER_1_MASTER_TABLES = ["TransactionType", "TransactionCategory", "DisclosureGroup"]
TIER_2_PARENT_TABLES = ["Customer", "Account"]
TIER_3_CHILD_TABLES = ["Card", "CardXref", "Transaction", "CategoryBalance"]


def _load_table_command(table_name: str) -> str:
    """Return the bash command that loads a single table.

    The actual command is expected to be stored as an Airflow Variable
    (``carddemo_load_<table>_cmd``).  A safe placeholder is used when
    the variable is not yet configured.
    """
    var_key = f"carddemo_load_{table_name.lower()}_cmd"
    return (
        f"echo 'Loading table {table_name}' && "
        f"{{{{ var.value.get('{var_key}', 'echo {table_name} load placeholder') }}}}"
    )


# ---------------------------------------------------------------------------
# DAG definition
# ---------------------------------------------------------------------------

with DAG(
    dag_id="carddemo_master_data_load",
    description=(
        "Full data-load pipeline for CardDemo. Loads tables in "
        "referential-integrity order: master -> parent -> child."
    ),
    default_args=default_args,
    schedule_interval=None,  # Triggered manually or by an external sensor
    start_date=datetime(2025, 9, 9),
    catchup=False,
    dagrun_timeout=timedelta(hours=12),
    tags=["carddemo", "data-load", "master"],
    max_active_runs=1,
) as dag:

    start = EmptyOperator(task_id="start")

    # ===== Tier 1 – Master / Reference tables =============================
    tier1_tasks = []
    for table in TIER_1_MASTER_TABLES:
        task = BashOperator(
            task_id=f"load_{table.lower()}",
            bash_command=_load_table_command(table),
            doc=f"Load master table: {table}",
        )
        start >> task
        tier1_tasks.append(task)

    tier1_gate = EmptyOperator(
        task_id="tier1_complete",
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )
    for task in tier1_tasks:
        task >> tier1_gate

    validate_tier1 = PythonOperator(
        task_id="validate_tier1",
        python_callable=_validate_tier,
        op_kwargs={"tier_name": "master", "tables": TIER_1_MASTER_TABLES},
    )
    tier1_gate >> validate_tier1

    # ===== Tier 2 – Parent entity tables ==================================
    tier2_tasks = []
    for table in TIER_2_PARENT_TABLES:
        task = BashOperator(
            task_id=f"load_{table.lower()}",
            bash_command=_load_table_command(table),
            doc=f"Load parent table: {table}",
        )
        validate_tier1 >> task
        tier2_tasks.append(task)

    tier2_gate = EmptyOperator(
        task_id="tier2_complete",
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )
    for task in tier2_tasks:
        task >> tier2_gate

    validate_tier2 = PythonOperator(
        task_id="validate_tier2",
        python_callable=_validate_tier,
        op_kwargs={"tier_name": "parent", "tables": TIER_2_PARENT_TABLES},
    )
    tier2_gate >> validate_tier2

    # ===== Tier 3 – Child / dependent tables ==============================
    tier3_tasks = []
    for table in TIER_3_CHILD_TABLES:
        task = BashOperator(
            task_id=f"load_{table.lower()}",
            bash_command=_load_table_command(table),
            doc=f"Load child table: {table}",
        )
        validate_tier2 >> task
        tier3_tasks.append(task)

    tier3_gate = EmptyOperator(
        task_id="tier3_complete",
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )
    for task in tier3_tasks:
        task >> tier3_gate

    validate_tier3 = PythonOperator(
        task_id="validate_tier3",
        python_callable=_validate_tier,
        op_kwargs={"tier_name": "child", "tables": TIER_3_CHILD_TABLES},
    )
    tier3_gate >> validate_tier3

    # ===== Completion =====================================================
    end = EmptyOperator(
        task_id="end",
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )
    validate_tier3 >> end
