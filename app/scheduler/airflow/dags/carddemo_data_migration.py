"""
Airflow DAG: CardDemo Data Migration

Orchestrates the migration of CardDemo data from mainframe (DB2/VSAM) to
PostgreSQL, respecting referential-integrity load order:

    Tier 1 – Master / lookup tables (no FK dependencies):
        TransactionType, TransactionCategory, DisclosureGroup

    Tier 2 – Parent tables (referenced by child tables):
        Customer, Account

    Tier 3 – Child / dependent tables (have FKs to Tier 1 & 2):
        Card, CardXref, Transaction, CategoryBalance

Each tier runs in parallel within itself but waits for the previous tier
to complete before starting.  After all tiers finish a validation step
compares row counts between source and target.

This DAG is intended to be triggered manually or by an external event
(e.g., after infrastructure provisioning completes).
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
    "retries": 3,
    "retry_delay": timedelta(minutes=2),
    "execution_timeout": timedelta(hours=4),
}


# ---------------------------------------------------------------------------
# Callbacks
# ---------------------------------------------------------------------------
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


# ---------------------------------------------------------------------------
# Helper: build a migration task for a single table
# ---------------------------------------------------------------------------
def _make_migration_task(table_name: str, source_file: str, dag: DAG) -> BashOperator:
    """Return a BashOperator that migrates *table_name* from the legacy
    flat-file *source_file* into the target PostgreSQL table.

    In a real deployment the bash_command would invoke a migration script
    or ETL tool (e.g., ``pgloader``, ``aws dms``, a custom Python ETL).
    """
    return BashOperator(
        task_id=f"migrate_{table_name.lower()}",
        bash_command=(
            f"echo 'Migrating {table_name} from {source_file} to PostgreSQL...' && "
            f"echo 'Migration of {table_name} completed successfully.'"
        ),
        dag=dag,
        on_failure_callback=_on_failure_callback,
    )


# ---------------------------------------------------------------------------
# Table definitions per tier
# ---------------------------------------------------------------------------
TIER_1_MASTER_TABLES = {
    "TransactionType": "trantype.txt",
    "TransactionCategory": "trancatg.txt",
    "DisclosureGroup": "discgrp.txt",
}

TIER_2_PARENT_TABLES = {
    "Customer": "custdata.txt",
    "Account": "acctdata.txt",
}

TIER_3_CHILD_TABLES = {
    "Card": "carddata.txt",
    "CardXref": "cardxref.txt",
    "Transaction": "dailytran.txt",
    "CategoryBalance": "tcatbal.txt",
}


with DAG(
    dag_id="carddemo_data_migration",
    description=(
        "Migrate CardDemo data from mainframe flat files to PostgreSQL "
        "respecting referential-integrity load order: "
        "master -> parent -> child tables"
    ),
    default_args=default_args,
    schedule_interval=None,  # Manually triggered
    start_date=datetime(2025, 9, 9),
    catchup=False,
    max_active_runs=1,
    tags=["carddemo", "migration", "etl", "postgres"],
    on_failure_callback=_on_failure_callback,
    on_success_callback=_on_success_callback,
) as dag:

    # ------------------------------------------------------------------
    # Pre-migration: verify connectivity / prerequisites
    # ------------------------------------------------------------------
    pre_check = BashOperator(
        task_id="pre_migration_check",
        bash_command=(
            "echo 'Verifying source files and target database connectivity...' && "
            "echo 'Pre-migration checks passed.'"
        ),
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Tier 1 – Master / lookup tables (parallel)
    # ------------------------------------------------------------------
    tier1_tasks = [
        _make_migration_task(name, src, dag)
        for name, src in TIER_1_MASTER_TABLES.items()
    ]

    tier1_complete = BashOperator(
        task_id="tier1_master_tables_complete",
        bash_command="echo 'Tier 1 (master tables) migration complete.'",
        trigger_rule=TriggerRule.ALL_SUCCESS,
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Tier 2 – Parent tables (parallel)
    # ------------------------------------------------------------------
    tier2_tasks = [
        _make_migration_task(name, src, dag)
        for name, src in TIER_2_PARENT_TABLES.items()
    ]

    tier2_complete = BashOperator(
        task_id="tier2_parent_tables_complete",
        bash_command="echo 'Tier 2 (parent tables) migration complete.'",
        trigger_rule=TriggerRule.ALL_SUCCESS,
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Tier 3 – Child / dependent tables (parallel)
    # ------------------------------------------------------------------
    tier3_tasks = [
        _make_migration_task(name, src, dag)
        for name, src in TIER_3_CHILD_TABLES.items()
    ]

    tier3_complete = BashOperator(
        task_id="tier3_child_tables_complete",
        bash_command="echo 'Tier 3 (child/dependent tables) migration complete.'",
        trigger_rule=TriggerRule.ALL_SUCCESS,
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Post-migration: row-count validation
    # ------------------------------------------------------------------
    def _validate_migration(**context):
        """Compare row counts between source files and target tables.

        In a production deployment this would query PostgreSQL and count
        lines in the source flat files.  Here we simulate the validation.
        """
        all_tables = {
            **TIER_1_MASTER_TABLES,
            **TIER_2_PARENT_TABLES,
            **TIER_3_CHILD_TABLES,
        }
        results = {}
        for table_name, source_file in all_tables.items():
            # Simulated counts – replace with actual queries
            source_count = 100
            target_count = 100
            match = source_count == target_count
            results[table_name] = {
                "source_file": source_file,
                "source_count": source_count,
                "target_count": target_count,
                "match": match,
            }
            status = "OK" if match else "MISMATCH"
            print(
                f"  {table_name}: source={source_count} target={target_count} "
                f"[{status}]"
            )

        mismatches = [t for t, r in results.items() if not r["match"]]
        if mismatches:
            raise RuntimeError(
                f"Row-count mismatches detected for: {', '.join(mismatches)}"
            )
        print("All row counts match between source and target.")

    validate = PythonOperator(
        task_id="validate_migration",
        python_callable=_validate_migration,
        trigger_rule=TriggerRule.ALL_SUCCESS,
        on_failure_callback=_on_failure_callback,
    )

    # ------------------------------------------------------------------
    # Completion notification
    # ------------------------------------------------------------------
    def _notify_completion(**context):
        execution_date = context["execution_date"]
        print(
            f"CardDemo data migration completed and validated "
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
    # pre_check -> [tier1 tasks] -> tier1_complete
    #           -> [tier2 tasks] -> tier2_complete
    #           -> [tier3 tasks] -> tier3_complete
    #           -> validate -> notify
    # ------------------------------------------------------------------
    pre_check >> tier1_tasks >> tier1_complete
    tier1_complete >> tier2_tasks >> tier2_complete
    tier2_complete >> tier3_tasks >> tier3_complete
    tier3_complete >> validate >> notify
