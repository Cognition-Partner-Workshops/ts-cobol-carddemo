"""DailyTransaction model - migrated from COBOL copybook CVTRA06Y.cpy (350 bytes)."""

from decimal import Decimal

from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class DailyTransaction(Base):
    """
    Daily transaction entity representing the DALYTRAN PS file.
    
    Migrated from COBOL copybook CVTRA06Y.cpy.
    Record length: 350 bytes
    Primary key: DALYTRAN-ID (16 characters)
    Used for batch processing input.
    """
    __tablename__ = "dalytran"

    dalytran_id: Mapped[str] = mapped_column(String(16), primary_key=True)
    dalytran_type_cd: Mapped[str] = mapped_column(String(2), nullable=True)
    dalytran_cat_cd: Mapped[int] = mapped_column(nullable=True)
    dalytran_source: Mapped[str] = mapped_column(String(10), nullable=True)
    dalytran_desc: Mapped[str] = mapped_column(String(100), nullable=True)
    dalytran_amt: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=True)
    dalytran_merchant_id: Mapped[str] = mapped_column(String(9), nullable=True)
    dalytran_merchant_name: Mapped[str] = mapped_column(String(50), nullable=True)
    dalytran_merchant_city: Mapped[str] = mapped_column(String(50), nullable=True)
    dalytran_merchant_zip: Mapped[str] = mapped_column(String(10), nullable=True)
    dalytran_card_num: Mapped[str] = mapped_column(String(16), nullable=True)
    dalytran_orig_ts: Mapped[str] = mapped_column(String(26), nullable=True)
    dalytran_proc_ts: Mapped[str] = mapped_column(String(26), nullable=True)

    def __repr__(self) -> str:
        return f"DailyTransaction(dalytran_id={self.dalytran_id}, amt={self.dalytran_amt})"
