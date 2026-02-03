"""
Transaction type data loader - migrated from TRANTYPE.jcl.

This module replaces the JCL job TRANTYPE which:
1. Deletes existing TRANTYPE VSAM KSDS cluster
2. Defines new TRANTYPE VSAM KSDS cluster (KEYS(2 0), RECORDSIZE(60 60))
3. Copies data from AWS.M2.CARDDEMO.TRANTYPE.PS to VSAM file using REPRO
"""

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.transaction_type import TransactionType


class TransactionTypeLoader(BaseLoader[TransactionType]):
    """
    Loader for Transaction Type reference data.
    
    Migrated from TRANTYPE.jcl which loads transaction type reference data
    from a flat file into the TRANTYPE VSAM KSDS file.
    
    Record layout from CVTRA03Y.cpy:
    - Position 0-1: TRAN-TYPE (2 chars)
    - Position 2-51: TRAN-TYPE-DESC (50 chars)
    """

    @property
    def job_name(self) -> str:
        return "TRANTYPE"

    @property
    def record_length(self) -> int:
        return 60

    @property
    def model_class(self) -> type[TransactionType]:
        return TransactionType

    def parse_record(self, line: str) -> TransactionType | None:
        """Parse a single transaction type record from the flat file."""
        line = line.ljust(self.record_length)
        
        tran_type = line[0:2].strip()
        if not tran_type:
            return None

        return TransactionType(
            tran_type=tran_type,
            tran_type_desc=line[2:52].strip() or None,
        )
