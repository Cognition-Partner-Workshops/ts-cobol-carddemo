"""UserSecurity model - migrated from COBOL copybook CSUSR01Y.cpy (80 bytes)."""

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from carddemo_batch.models.base import Base


class UserSecurity(Base):
    """
    User security entity representing the USRSEC VSAM file.
    
    Migrated from COBOL copybook CSUSR01Y.cpy.
    Record length: 80 bytes
    Primary key: SEC-USR-ID (8 characters)
    """
    __tablename__ = "usrsec"

    sec_usr_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    sec_usr_fname: Mapped[str] = mapped_column(String(20), nullable=True)
    sec_usr_lname: Mapped[str] = mapped_column(String(20), nullable=True)
    sec_usr_pwd: Mapped[str] = mapped_column(String(8), nullable=True)
    sec_usr_type: Mapped[str] = mapped_column(String(1), nullable=True)

    USER_TYPE_ADMIN = "A"
    USER_TYPE_USER = "U"

    def is_admin(self) -> bool:
        """Check if user is an administrator."""
        return self.sec_usr_type == self.USER_TYPE_ADMIN

    def is_user(self) -> bool:
        """Check if user is a regular user."""
        return self.sec_usr_type == self.USER_TYPE_USER

    def __repr__(self) -> str:
        return f"UserSecurity(usr_id={self.sec_usr_id}, type={self.sec_usr_type})"
