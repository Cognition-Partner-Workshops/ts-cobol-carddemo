"""
Command-line interface for CardDemo batch data loading.

This module provides a CLI that mirrors the JCL job submission process,
allowing users to run individual data loading jobs or all jobs at once.

Usage:
    carddemo-load --job ACCTFILE --input /path/to/acctdata.txt
    carddemo-load --job all --data-dir /path/to/data/
    carddemo-load --job DUSRSECJ --default-users
"""

import sys
from pathlib import Path

import click

from carddemo_batch.utils.database import get_engine, init_database
from carddemo_batch.utils.logging_config import (
    log_job_end,
    log_job_start,
    setup_logging,
)


@click.group()
@click.option("--verbose", "-v", is_flag=True, help="Enable verbose output")
@click.option("--log-file", type=click.Path(), help="Path to log file")
@click.pass_context
def cli(ctx: click.Context, verbose: bool, log_file: str | None) -> None:
    """CardDemo Batch Data Loader - JCL to Python Migration."""
    ctx.ensure_object(dict)
    log_level = "DEBUG" if verbose else "INFO"
    ctx.obj["logger"] = setup_logging(log_level, log_file, "carddemo-batch")
    ctx.obj["verbose"] = verbose


@cli.command()
@click.option("--database-url", envvar="DATABASE_URL", help="Database connection URL")
@click.pass_context
def init_db(ctx: click.Context, database_url: str | None) -> None:
    """Initialize the database schema."""
    logger = ctx.obj["logger"]
    log_job_start(logger, "INIT_DB")
    
    try:
        init_database()
        logger.info("Database schema initialized successfully")
        log_job_end(logger, "INIT_DB", 0)
    except Exception as e:
        logger.error(f"Failed to initialize database: {e}")
        log_job_end(logger, "INIT_DB", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_accounts(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load account data (equivalent to ACCTFILE.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.account_loader import AccountLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "ACCTFILE")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = AccountLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} account records")
            log_job_end(logger, "ACCTFILE", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "ACCTFILE", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_customers(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load customer data (equivalent to CUSTFILE.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.customer_loader import CustomerLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "CUSTFILE")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = CustomerLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} customer records")
            log_job_end(logger, "CUSTFILE", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "CUSTFILE", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_cards(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load card data (equivalent to CARDFILE.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.card_loader import CardLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "CARDFILE")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = CardLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} card records")
            log_job_end(logger, "CARDFILE", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "CARDFILE", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_xref(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load card cross-reference data (equivalent to XREFFILE.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.card_xref_loader import CardXrefLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "XREFFILE")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = CardXrefLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} cross-reference records")
            log_job_end(logger, "XREFFILE", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "XREFFILE", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_transactions(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load transaction data (equivalent to TRANFILE.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.transaction_loader import TransactionLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "TRANFILE")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = TransactionLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} transaction records")
            log_job_end(logger, "TRANFILE", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "TRANFILE", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True),
              help="Path to input data file")
@click.option("--default-users", is_flag=True, help="Load default users from JCL in-stream data")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_users(ctx: click.Context, input_file: str | None, default_users: bool,
               no_truncate: bool) -> None:
    """Load user security data (equivalent to DUSRSECJ.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.user_security_loader import UserSecurityLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "DUSRSECJ")
    
    if not input_file and not default_users:
        logger.error("Either --input or --default-users must be specified")
        log_job_end(logger, "DUSRSECJ", 12)
        sys.exit(12)
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = UserSecurityLoader(session, logger)
            if default_users:
                count = loader.load_default_users()
            else:
                count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} user records")
            log_job_end(logger, "DUSRSECJ", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "DUSRSECJ", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_tran_types(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load transaction type data (equivalent to TRANTYPE.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.transaction_type_loader import TransactionTypeLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "TRANTYPE")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = TransactionTypeLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} transaction type records")
            log_job_end(logger, "TRANTYPE", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "TRANTYPE", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_tran_categories(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load transaction category data (equivalent to TRANCATG.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.transaction_category_loader import TransactionCategoryLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "TRANCATG")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = TransactionCategoryLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} transaction category records")
            log_job_end(logger, "TRANCATG", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "TRANCATG", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_tran_cat_balances(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load transaction category balance data (equivalent to TCATBALF.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.transaction_category_balance_loader import (
        TransactionCategoryBalanceLoader,
    )
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "TCATBALF")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = TransactionCategoryBalanceLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} transaction category balance records")
            log_job_end(logger, "TCATBALF", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "TCATBALF", 12)
        sys.exit(12)


@cli.command()
@click.option("--input", "-i", "input_file", type=click.Path(exists=True), required=True,
              help="Path to input data file")
@click.option("--no-truncate", is_flag=True, help="Don't truncate existing data")
@click.pass_context
def load_disclosure_groups(ctx: click.Context, input_file: str, no_truncate: bool) -> None:
    """Load disclosure group data (equivalent to DISCGRP.jcl)."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders.disclosure_group_loader import DisclosureGroupLoader
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "DISCGRP")
    
    try:
        engine = get_engine()
        with Session(engine) as session:
            loader = DisclosureGroupLoader(session, logger)
            count = loader.load_from_file(Path(input_file), truncate_first=not no_truncate)
            logger.info(f"Loaded {count} disclosure group records")
            log_job_end(logger, "DISCGRP", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "DISCGRP", 12)
        sys.exit(12)


@cli.command()
@click.option("--data-dir", "-d", type=click.Path(exists=True), required=True,
              help="Path to directory containing data files")
@click.pass_context
def load_all(ctx: click.Context, data_dir: str) -> None:
    """Load all data files from a directory."""
    from sqlalchemy.orm import Session
    from carddemo_batch.loaders import (
        AccountLoader,
        CardLoader,
        CardXrefLoader,
        CustomerLoader,
        DisclosureGroupLoader,
        TransactionCategoryBalanceLoader,
        TransactionCategoryLoader,
        TransactionLoader,
        TransactionTypeLoader,
    )
    
    logger = ctx.obj["logger"]
    log_job_start(logger, "LOAD_ALL")
    
    data_path = Path(data_dir)
    
    file_mappings = [
        ("acctdata.txt", AccountLoader, "ACCTFILE"),
        ("custdata.txt", CustomerLoader, "CUSTFILE"),
        ("carddata.txt", CardLoader, "CARDFILE"),
        ("cardxref.txt", CardXrefLoader, "XREFFILE"),
        ("dailytran.txt", TransactionLoader, "TRANFILE"),
        ("trantype.txt", TransactionTypeLoader, "TRANTYPE"),
        ("trancatg.txt", TransactionCategoryLoader, "TRANCATG"),
        ("tcatbal.txt", TransactionCategoryBalanceLoader, "TCATBALF"),
        ("discgrp.txt", DisclosureGroupLoader, "DISCGRP"),
    ]
    
    total_loaded = 0
    failed_jobs = []
    
    try:
        engine = get_engine()
        init_database()
        
        with Session(engine) as session:
            for filename, loader_class, job_name in file_mappings:
                file_path = data_path / filename
                if file_path.exists():
                    logger.info(f"Processing {job_name}: {file_path}")
                    try:
                        loader = loader_class(session, logger)
                        count = loader.load_from_file(file_path)
                        total_loaded += count
                        logger.info(f"{job_name} completed: {count} records")
                    except Exception as e:
                        logger.error(f"{job_name} failed: {e}")
                        failed_jobs.append(job_name)
                else:
                    logger.warning(f"File not found: {file_path}")
        
        if failed_jobs:
            logger.error(f"Failed jobs: {', '.join(failed_jobs)}")
            log_job_end(logger, "LOAD_ALL", 8)
            sys.exit(8)
        else:
            logger.info(f"Total records loaded: {total_loaded}")
            log_job_end(logger, "LOAD_ALL", 0)
    except Exception as e:
        logger.error(f"Job failed: {e}")
        log_job_end(logger, "LOAD_ALL", 12)
        sys.exit(12)


def main() -> None:
    """Main entry point."""
    cli(obj={})


if __name__ == "__main__":
    main()
