"""DisclosureGroup model - migrated from COBOL copybook CVTRA02Y.cpy (50 bytes)."""

from decimal import Decimal

from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class DisclosureGroup(Base):
    """
    Disclosure group entity representing the DISCGRP VSAM file.
    
    Migrated from COBOL copybook CVTRA02Y.cpy.
    Record length: 50 bytes
    Composite key: DIS-ACCT-GROUP-ID (10 chars) + DIS-TRAN-TYPE-CD (2 chars) + DIS-TRAN-CAT-CD (4 digits)
    Contains interest rate information for account groups.
    """
    __tablename__ = "discgrp"

    dis_acct_group_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    dis_tran_type_cd: Mapped[str] = mapped_column(String(2), primary_key=True)
    dis_tran_cat_cd: Mapped[int] = mapped_column(primary_key=True)
    dis_int_rate: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=True)

    def __repr__(self) -> str:
        return f"DisclosureGroup(group={self.dis_acct_group_id}, type={self.dis_tran_type_cd}, rate={self.dis_int_rate})"
