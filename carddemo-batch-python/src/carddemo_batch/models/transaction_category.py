"""TransactionCategory model - migrated from COBOL copybook CVTRA04Y.cpy (60 bytes)."""

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class TransactionCategory(Base):
    """
    Transaction category reference entity representing the TRANCATG VSAM file.
    
    Migrated from COBOL copybook CVTRA04Y.cpy.
    Record length: 60 bytes
    Composite key: TRAN-TYPE-CD (2 chars) + TRAN-CAT-CD (4 digits)
    """
    __tablename__ = "trancatg"

    tran_type_cd: Mapped[str] = mapped_column(String(2), primary_key=True)
    tran_cat_cd: Mapped[int] = mapped_column(primary_key=True)
    tran_cat_type_desc: Mapped[str] = mapped_column(String(50), nullable=True)

    def __repr__(self) -> str:
        return f"TransactionCategory(type={self.tran_type_cd}, cat={self.tran_cat_cd})"
