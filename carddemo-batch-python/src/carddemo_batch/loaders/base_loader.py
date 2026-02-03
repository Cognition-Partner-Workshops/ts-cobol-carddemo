"""Base loader class for all data loaders."""

import logging
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any, Generic, TypeVar

from sqlalchemy.orm import Session

from carddemo_batch.models.base import Base
from carddemo_batch.utils.logging_config import log_step_end, log_step_start

T = TypeVar("T", bound=Base)


class BaseLoader(ABC, Generic[T]):
    """
    Base class for data loaders.
    
    This class provides the common functionality for loading data from
    fixed-width flat files (equivalent to PS datasets) into the database
    (equivalent to VSAM KSDS files).
    
    The pattern mirrors the JCL job structure:
    1. Delete existing data (IDCAMS DELETE)
    2. Define/create table structure (IDCAMS DEFINE CLUSTER)
    3. Load data from flat file (IDCAMS REPRO)
    """

    def __init__(self, session: Session, logger: logging.Logger | None = None):
        self.session = session
        self.logger = logger or logging.getLogger(self.__class__.__name__)
        self.records_read = 0
        self.records_loaded = 0
        self.records_rejected = 0

    @property
    @abstractmethod
    def job_name(self) -> str:
        """Return the equivalent JCL job name."""
        pass

    @property
    @abstractmethod
    def record_length(self) -> int:
        """Return the expected record length in bytes."""
        pass

    @property
    @abstractmethod
    def model_class(self) -> type[T]:
        """Return the SQLAlchemy model class."""
        pass

    @abstractmethod
    def parse_record(self, line: str) -> T | None:
        """
        Parse a single record from the flat file.
        
        Args:
            line: Raw line from the data file
            
        Returns:
            Model instance or None if record should be skipped
        """
        pass

    def delete_existing_data(self) -> int:
        """
        Delete existing data from the table.
        
        Equivalent to JCL IDCAMS DELETE command.
        Returns the number of records deleted.
        """
        log_step_start(self.logger, "DELETE")
        count = self.session.query(self.model_class).delete()
        self.session.commit()
        self.logger.info(f"Deleted {count} existing records")
        log_step_end(self.logger, "DELETE", 0)
        return count

    def load_from_file(self, file_path: Path, truncate_first: bool = True) -> int:
        """
        Load data from a flat file into the database.
        
        Equivalent to JCL IDCAMS REPRO command.
        
        Args:
            file_path: Path to the input data file
            truncate_first: Whether to delete existing data first
            
        Returns:
            Number of records loaded
        """
        if truncate_first:
            self.delete_existing_data()

        log_step_start(self.logger, "REPRO")
        
        self.records_read = 0
        self.records_loaded = 0
        self.records_rejected = 0

        with open(file_path, "r", encoding="utf-8") as f:
            for line in f:
                self.records_read += 1
                
                if len(line.rstrip("\n\r")) == 0:
                    continue

                try:
                    record = self.parse_record(line.rstrip("\n\r"))
                    if record is not None:
                        self.session.add(record)
                        self.records_loaded += 1
                        
                        if self.records_loaded % 100 == 0:
                            self.session.commit()
                            self.logger.debug(f"Committed {self.records_loaded} records")
                except Exception as e:
                    self.records_rejected += 1
                    self.logger.warning(f"Record {self.records_read} rejected: {e}")

        self.session.commit()
        
        self.logger.info(f"Records read: {self.records_read}")
        self.logger.info(f"Records loaded: {self.records_loaded}")
        self.logger.info(f"Records rejected: {self.records_rejected}")
        
        log_step_end(self.logger, "REPRO", 0 if self.records_rejected == 0 else 4)
        
        return self.records_loaded

    def get_statistics(self) -> dict[str, Any]:
        """Return loading statistics."""
        return {
            "job_name": self.job_name,
            "records_read": self.records_read,
            "records_loaded": self.records_loaded,
            "records_rejected": self.records_rejected,
        }
