"""
Transaction category balance data loader - migrated from TCATBALF.jcl.

This module replaces the JCL job TCATBALF which:
1. Deletes existing TCATBALF VSAM KSDS cluster
2. Defines new TCATBALF VSAM KSDS cluster (KEYS(17 0), RECORDSIZE(50 50))
3. Copies data from AWS.M2.CARDDEMO.TCATBALF.PS to VSAM file using REPRO
"""

from decimal import Decimal

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.transaction_category_balance import TransactionCategoryBalance


class TransactionCategoryBalanceLoader(BaseLoader[TransactionCategoryBalance]):
    """
    Loader for Transaction Category Balance data.
    
    Migrated from TCATBALF.jcl which loads transaction category balance data
    from a flat file into the TCATBALF VSAM KSDS file.
    
    Record layout from CVTRA01Y.cpy:
    - Position 0-10: TRANCAT-ACCT-ID (11 digits)
    - Position 11-12: TRANCAT-TYPE-CD (2 chars)
    - Position 13-16: TRANCAT-CD (4 digits)
    - Position 17-27: TRAN-CAT-BAL (11 digits, implied 2 decimals)
    """

    @property
    def job_name(self) -> str:
        return "TCATBALF"

    @property
    def record_length(self) -> int:
        return 50

    @property
    def model_class(self) -> type[TransactionCategoryBalance]:
        return TransactionCategoryBalance

    def parse_record(self, line: str) -> TransactionCategoryBalance | None:
        """Parse a single transaction category balance record from the flat file."""
        line = line.ljust(self.record_length)
        
        acct_id_str = line[0:11].strip()
        if not acct_id_str:
            return None

        try:
            acct_id = int(acct_id_str)
        except ValueError:
            self.logger.warning(f"Invalid account ID: {acct_id_str}")
            return None

        type_cd = line[11:13].strip()
        if not type_cd:
            return None

        cat_cd_str = line[13:17].strip()
        try:
            cat_cd = int(cat_cd_str) if cat_cd_str else 0
        except ValueError:
            self.logger.warning(f"Invalid category code: {cat_cd_str}")
            return None

        return TransactionCategoryBalance(
            trancat_acct_id=acct_id,
            trancat_type_cd=type_cd,
            trancat_cd=cat_cd,
            tran_cat_bal=self._parse_decimal(line[17:28]),
        )

    def _parse_decimal(self, value: str, decimal_places: int = 2) -> Decimal:
        """Parse a COBOL decimal field with implied decimal places."""
        try:
            clean_value = value.strip()
            if not clean_value:
                return Decimal("0.00")
            
            int_value = int(clean_value)
            return Decimal(int_value) / Decimal(10 ** decimal_places)
        except ValueError:
            return Decimal("0.00")
