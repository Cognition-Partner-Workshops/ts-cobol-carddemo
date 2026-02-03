"""Card model - migrated from COBOL copybook CVACT02Y.cpy (150 bytes)."""

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class Card(Base):
    """
    Card entity representing the CARDDATA VSAM file.
    
    Migrated from COBOL copybook CVACT02Y.cpy.
    Record length: 150 bytes
    Primary key: CARD-NUM (16 characters)
    """
    __tablename__ = "carddata"

    card_num: Mapped[str] = mapped_column(String(16), primary_key=True)
    card_acct_id: Mapped[int] = mapped_column(nullable=True)
    card_cvv_cd: Mapped[str] = mapped_column(String(3), nullable=True)
    card_embossed_name: Mapped[str] = mapped_column(String(50), nullable=True)
    card_expiraion_date: Mapped[str] = mapped_column(String(10), nullable=True)
    card_active_status: Mapped[str] = mapped_column(String(1), nullable=True)

    def __repr__(self) -> str:
        return f"Card(card_num={self.card_num}, acct_id={self.card_acct_id})"
