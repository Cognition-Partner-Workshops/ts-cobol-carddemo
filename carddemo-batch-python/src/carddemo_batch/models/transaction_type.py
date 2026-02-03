"""TransactionType model - migrated from COBOL copybook CVTRA03Y.cpy (60 bytes)."""

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class TransactionType(Base):
    """
    Transaction type reference entity representing the TRANTYPE VSAM file.
    
    Migrated from COBOL copybook CVTRA03Y.cpy.
    Record length: 60 bytes
    Primary key: TRAN-TYPE (2 characters)
    """
    __tablename__ = "trantype"

    tran_type: Mapped[str] = mapped_column(String(2), primary_key=True)
    tran_type_desc: Mapped[str] = mapped_column(String(50), nullable=True)

    def __repr__(self) -> str:
        return f"TransactionType(type={self.tran_type}, desc={self.tran_type_desc})"
