"""
User security data loader - migrated from DUSRSECJ.jcl.

This module replaces the JCL job DUSRSECJ which:
1. Pre-deletes existing USRSEC.PS file
2. Creates USRSEC.PS from in-stream data (IEBGENER)
3. Deletes existing USRSEC VSAM KSDS cluster
4. Defines new USRSEC VSAM KSDS cluster (KEYS(8 0), RECORDSIZE(80 80))
5. Copies data from USRSEC.PS to VSAM file using REPRO
"""

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.user_security import UserSecurity


class UserSecurityLoader(BaseLoader[UserSecurity]):
    """
    Loader for User Security data.
    
    Migrated from DUSRSECJ.jcl which loads user security data
    from a flat file into the USRSEC VSAM KSDS file.
    
    Record layout from CSUSR01Y.cpy:
    - Position 0-7: SEC-USR-ID (8 chars)
    - Position 8-27: SEC-USR-FNAME (20 chars)
    - Position 28-47: SEC-USR-LNAME (20 chars)
    - Position 48-55: SEC-USR-PWD (8 chars)
    - Position 56: SEC-USR-TYPE (1 char: 'A'=Admin, 'U'=User)
    
    Default users from JCL in-stream data:
    - ADMIN001-005: Administrator accounts
    - USER0001-0005: Regular user accounts
    """

    @property
    def job_name(self) -> str:
        return "DUSRSECJ"

    @property
    def record_length(self) -> int:
        return 80

    @property
    def model_class(self) -> type[UserSecurity]:
        return UserSecurity

    def parse_record(self, line: str) -> UserSecurity | None:
        """Parse a single user security record from the flat file."""
        line = line.ljust(self.record_length)
        
        usr_id = line[0:8].strip()
        if not usr_id:
            return None

        return UserSecurity(
            sec_usr_id=usr_id,
            sec_usr_fname=line[8:28].strip() or None,
            sec_usr_lname=line[28:48].strip() or None,
            sec_usr_pwd=line[48:56].strip() or None,
            sec_usr_type=line[56:57].strip() or None,
        )

    def load_default_users(self) -> int:
        """
        Load the default users as defined in the original JCL in-stream data.
        
        This mirrors the SYSUT1 DD * section of DUSRSECJ.jcl.
        """
        default_users = [
            ("ADMIN001", "MARGARET", "GOLD", "PASSWORD", "A"),
            ("ADMIN002", "RUSSELL", "RUSSELL", "PASSWORD", "A"),
            ("ADMIN003", "RAYMOND", "WHITMORE", "PASSWORD", "A"),
            ("ADMIN004", "EMMANUEL", "CASGRAIN", "PASSWORD", "A"),
            ("ADMIN005", "GRANVILLE", "LACHAPELLE", "PASSWORD", "A"),
            ("USER0001", "LAWRENCE", "THOMAS", "PASSWORD", "U"),
            ("USER0002", "AJITH", "KUMAR", "PASSWORD", "U"),
            ("USER0003", "LAURITZ", "ALME", "PASSWORD", "U"),
            ("USER0004", "AVERARDO", "MAZZI", "PASSWORD", "U"),
            ("USER0005", "LEE", "TING", "PASSWORD", "U"),
        ]

        self.delete_existing_data()
        
        for usr_id, fname, lname, pwd, usr_type in default_users:
            user = UserSecurity(
                sec_usr_id=usr_id,
                sec_usr_fname=fname,
                sec_usr_lname=lname,
                sec_usr_pwd=pwd,
                sec_usr_type=usr_type,
            )
            self.session.add(user)
            self.records_loaded += 1

        self.session.commit()
        self.logger.info(f"Loaded {self.records_loaded} default users")
        return self.records_loaded
