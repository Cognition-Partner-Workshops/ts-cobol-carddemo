"""CardXref model - migrated from COBOL copybook CVACT03Y.cpy (50 bytes)."""

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class CardXref(Base):
    """
    Card cross-reference entity representing the CARDXREF VSAM file.
    
    Migrated from COBOL copybook CVACT03Y.cpy.
    Record length: 50 bytes
    Primary key: XREF-CARD-NUM (16 characters)
    Links card to customer and account.
    """
    __tablename__ = "cardxref"

    xref_card_num: Mapped[str] = mapped_column(String(16), primary_key=True)
    xref_cust_id: Mapped[int] = mapped_column(nullable=True)
    xref_acct_id: Mapped[int] = mapped_column(nullable=True)

    def __repr__(self) -> str:
        return f"CardXref(card={self.xref_card_num}, cust={self.xref_cust_id}, acct={self.xref_acct_id})"
