"""TransactionCategoryBalance model - migrated from COBOL copybook CVTRA01Y.cpy (50 bytes)."""

from decimal import Decimal

from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class TransactionCategoryBalance(Base):
    """
    Transaction category balance entity representing the TCATBALF VSAM file.
    
    Migrated from COBOL copybook CVTRA01Y.cpy.
    Record length: 50 bytes
    Composite key: TRANCAT-ACCT-ID (11 digits) + TRANCAT-TYPE-CD (2 chars) + TRANCAT-CD (4 digits)
    """
    __tablename__ = "tcatbalf"

    trancat_acct_id: Mapped[int] = mapped_column(primary_key=True)
    trancat_type_cd: Mapped[str] = mapped_column(String(2), primary_key=True)
    trancat_cd: Mapped[int] = mapped_column(primary_key=True)
    tran_cat_bal: Mapped[Decimal] = mapped_column(Numeric(11, 2), nullable=True)

    def __repr__(self) -> str:
        return f"TransactionCategoryBalance(acct={self.trancat_acct_id}, type={self.trancat_type_cd}, cat={self.trancat_cd})"
