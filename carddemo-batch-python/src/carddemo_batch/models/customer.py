"""Customer model - migrated from COBOL copybook CVCUS01Y.cpy (500 bytes)."""

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class Customer(Base):
    """
    Customer entity representing the CUSTDATA VSAM file.
    
    Migrated from COBOL copybook CVCUS01Y.cpy.
    Record length: 500 bytes
    Primary key: CUST-ID (9 digits)
    """
    __tablename__ = "custdata"

    cust_id: Mapped[int] = mapped_column(primary_key=True)
    cust_first_name: Mapped[str] = mapped_column(String(25), nullable=True)
    cust_middle_name: Mapped[str] = mapped_column(String(25), nullable=True)
    cust_last_name: Mapped[str] = mapped_column(String(25), nullable=True)
    cust_addr_line_1: Mapped[str] = mapped_column(String(50), nullable=True)
    cust_addr_line_2: Mapped[str] = mapped_column(String(50), nullable=True)
    cust_addr_line_3: Mapped[str] = mapped_column(String(50), nullable=True)
    cust_addr_state_cd: Mapped[str] = mapped_column(String(2), nullable=True)
    cust_addr_country_cd: Mapped[str] = mapped_column(String(3), nullable=True)
    cust_addr_zip: Mapped[str] = mapped_column(String(10), nullable=True)
    cust_phone_num_1: Mapped[str] = mapped_column(String(15), nullable=True)
    cust_phone_num_2: Mapped[str] = mapped_column(String(15), nullable=True)
    cust_ssn: Mapped[int] = mapped_column(nullable=True)
    cust_govt_issued_id: Mapped[str] = mapped_column(String(20), nullable=True)
    cust_dob_yyyy_mm_dd: Mapped[str] = mapped_column(String(10), nullable=True)
    cust_eft_account_id: Mapped[str] = mapped_column(String(10), nullable=True)
    cust_pri_card_holder_ind: Mapped[str] = mapped_column(String(1), nullable=True)
    cust_fico_credit_score: Mapped[int] = mapped_column(nullable=True)

    def __repr__(self) -> str:
        return f"Customer(cust_id={self.cust_id}, name={self.cust_first_name} {self.cust_last_name})"
