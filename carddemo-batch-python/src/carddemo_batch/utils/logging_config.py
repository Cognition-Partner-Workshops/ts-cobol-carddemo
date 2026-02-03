"""Logging configuration for CardDemo batch processing."""

import logging
import sys
from datetime import datetime
from pathlib import Path


def setup_logging(
    log_level: str = "INFO",
    log_file: str | None = None,
    job_name: str = "carddemo"
) -> logging.Logger:
    """
    Set up logging for batch job execution.
    
    This mirrors the mainframe SYSPRINT DD output functionality,
    providing both console and file logging.
    
    Args:
        log_level: Logging level (DEBUG, INFO, WARNING, ERROR)
        log_file: Optional path to log file
        job_name: Name of the batch job for log identification
    
    Returns:
        Configured logger instance
    """
    logger = logging.getLogger(job_name)
    logger.setLevel(getattr(logging, log_level.upper()))
    
    if logger.handlers:
        return logger
    
    formatter = logging.Formatter(
        fmt="%(asctime)s | %(name)s | %(levelname)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )
    
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)
    
    if log_file:
        log_path = Path(log_file)
        log_path.parent.mkdir(parents=True, exist_ok=True)
        file_handler = logging.FileHandler(log_file)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
    
    return logger


def log_job_start(logger: logging.Logger, job_name: str) -> None:
    """Log the start of a batch job (equivalent to JCL job start)."""
    logger.info("=" * 60)
    logger.info(f"JOB {job_name} STARTED AT {datetime.now().isoformat()}")
    logger.info("=" * 60)


def log_job_end(logger: logging.Logger, job_name: str, return_code: int = 0) -> None:
    """Log the end of a batch job (equivalent to JCL job completion)."""
    logger.info("=" * 60)
    logger.info(f"JOB {job_name} ENDED AT {datetime.now().isoformat()}")
    logger.info(f"RETURN CODE: {return_code}")
    logger.info("=" * 60)


def log_step_start(logger: logging.Logger, step_name: str) -> None:
    """Log the start of a job step (equivalent to JCL EXEC step)."""
    logger.info("-" * 40)
    logger.info(f"STEP {step_name} STARTED")
    logger.info("-" * 40)


def log_step_end(logger: logging.Logger, step_name: str, return_code: int = 0) -> None:
    """Log the end of a job step."""
    logger.info(f"STEP {step_name} ENDED - RC={return_code}")
