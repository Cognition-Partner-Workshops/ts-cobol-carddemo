"""
Transaction category data loader - migrated from TRANCATG.jcl.

This module replaces the JCL job TRANCATG which:
1. Deletes existing TRANCATG VSAM KSDS cluster
2. Defines new TRANCATG VSAM KSDS cluster (KEYS(6 0), RECORDSIZE(60 60))
3. Copies data from AWS.M2.CARDDEMO.TRANCATG.PS to VSAM file using REPRO
"""

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.transaction_category import TransactionCategory


class TransactionCategoryLoader(BaseLoader[TransactionCategory]):
    """
    Loader for Transaction Category reference data.
    
    Migrated from TRANCATG.jcl which loads transaction category reference data
    from a flat file into the TRANCATG VSAM KSDS file.
    
    Record layout from CVTRA04Y.cpy:
    - Position 0-1: TRAN-TYPE-CD (2 chars)
    - Position 2-5: TRAN-CAT-CD (4 digits)
    - Position 6-55: TRAN-CAT-TYPE-DESC (50 chars)
    """

    @property
    def job_name(self) -> str:
        return "TRANCATG"

    @property
    def record_length(self) -> int:
        return 60

    @property
    def model_class(self) -> type[TransactionCategory]:
        return TransactionCategory

    def parse_record(self, line: str) -> TransactionCategory | None:
        """Parse a single transaction category record from the flat file."""
        line = line.ljust(self.record_length)
        
        tran_type_cd = line[0:2].strip()
        if not tran_type_cd:
            return None

        cat_cd_str = line[2:6].strip()
        try:
            cat_cd = int(cat_cd_str) if cat_cd_str else 0
        except ValueError:
            self.logger.warning(f"Invalid category code: {cat_cd_str}")
            return None

        return TransactionCategory(
            tran_type_cd=tran_type_cd,
            tran_cat_cd=cat_cd,
            tran_cat_type_desc=line[6:56].strip() or None,
        )
