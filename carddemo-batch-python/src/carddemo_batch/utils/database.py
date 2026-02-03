"""Database utilities for CardDemo batch processing."""

import os
from typing import Generator

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

_engine: Engine | None = None
_SessionLocal: sessionmaker | None = None


def get_engine() -> Engine:
    """Get or create the database engine."""
    global _engine
    if _engine is None:
        database_url = os.environ.get(
            "DATABASE_URL",
            "postgresql://carddemo:carddemo@localhost:5432/carddemo"
        )
        _engine = create_engine(database_url, echo=False)
    return _engine


def get_session() -> Generator[Session, None, None]:
    """Get a database session."""
    global _SessionLocal
    if _SessionLocal is None:
        _SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=get_engine())
    
    session = _SessionLocal()
    try:
        yield session
    finally:
        session.close()


def init_database() -> None:
    """Initialize the database schema."""
    from carddemo_batch.models.base import Base
    Base.metadata.create_all(bind=get_engine())
