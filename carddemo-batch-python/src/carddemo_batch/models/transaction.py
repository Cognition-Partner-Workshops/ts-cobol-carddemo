"""Transaction model - migrated from COBOL copybook CVTRA05Y.cpy (350 bytes)."""

from decimal import Decimal

from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class Transaction(Base):
    """
    Transaction entity representing the TRANSACT VSAM file.
    
    Migrated from COBOL copybook CVTRA05Y.cpy.
    Record length: 350 bytes
    Primary key: TRAN-ID (16 characters)
    """
    __tablename__ = "transact"

    tran_id: Mapped[str] = mapped_column(String(16), primary_key=True)
    tran_type_cd: Mapped[str] = mapped_column(String(2), nullable=True)
    tran_cat_cd: Mapped[int] = mapped_column(nullable=True)
    tran_source: Mapped[str] = mapped_column(String(10), nullable=True)
    tran_desc: Mapped[str] = mapped_column(String(100), nullable=True)
    tran_amt: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=True)
    tran_merchant_id: Mapped[str] = mapped_column(String(9), nullable=True)
    tran_merchant_name: Mapped[str] = mapped_column(String(50), nullable=True)
    tran_merchant_city: Mapped[str] = mapped_column(String(50), nullable=True)
    tran_merchant_zip: Mapped[str] = mapped_column(String(10), nullable=True)
    tran_card_num: Mapped[str] = mapped_column(String(16), nullable=True)
    tran_orig_ts: Mapped[str] = mapped_column(String(26), nullable=True)
    tran_proc_ts: Mapped[str] = mapped_column(String(26), nullable=True)

    def __repr__(self) -> str:
        return f"Transaction(tran_id={self.tran_id}, amt={self.tran_amt})"
