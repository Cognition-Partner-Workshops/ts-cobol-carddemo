"""Account model - migrated from COBOL copybook CVACT01Y.cpy (300 bytes)."""

from decimal import Decimal

from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class Account(Base):
    """
    Account entity representing the ACCTDATA VSAM file.
    
    Migrated from COBOL copybook CVACT01Y.cpy.
    Record length: 300 bytes
    Primary key: ACCT-ID (11 digits)
    """
    __tablename__ = "acctdata"

    acct_id: Mapped[int] = mapped_column(primary_key=True)
    acct_active_status: Mapped[str] = mapped_column(String(1), nullable=True)
    acct_curr_bal: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=True)
    acct_credit_limit: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=True)
    acct_cash_credit_limit: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=True)
    acct_open_date: Mapped[str] = mapped_column(String(10), nullable=True)
    acct_expiraion_date: Mapped[str] = mapped_column(String(10), nullable=True)
    acct_reissue_date: Mapped[str] = mapped_column(String(10), nullable=True)
    acct_curr_cyc_credit: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=True)
    acct_curr_cyc_debit: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=True)
    acct_addr_zip: Mapped[str] = mapped_column(String(10), nullable=True)
    acct_group_id: Mapped[str] = mapped_column(String(10), nullable=True)

    def __repr__(self) -> str:
        return f"Account(acct_id={self.acct_id}, status={self.acct_active_status})"
