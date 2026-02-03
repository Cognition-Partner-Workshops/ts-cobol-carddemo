"""
Transaction data loader - migrated from TRANFILE.jcl.

This module replaces the JCL job TRANFILE which:
1. Closes TRANSACT and CXACAIX files in CICS region
2. Deletes existing TRANSACT VSAM KSDS cluster and AIX
3. Defines new TRANSACT VSAM KSDS cluster (KEYS(16 0), RECORDSIZE(350 350))
4. Copies data from AWS.M2.CARDDEMO.DALYTRAN.PS.INIT to VSAM file using REPRO
5. Defines alternate index on processed timestamp (KEYS(26 304))
6. Defines path for alternate index
7. Builds alternate index
8. Opens TRANSACT and CXACAIX files in CICS region
"""

from decimal import Decimal

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.transaction import Transaction


class TransactionLoader(BaseLoader[Transaction]):
    """
    Loader for Transaction data.
    
    Migrated from TRANFILE.jcl which loads transaction master data
    from a flat file into the TRANSACT VSAM KSDS file.
    
    Record layout from CVTRA05Y.cpy:
    - Position 0-15: TRAN-ID (16 chars)
    - Position 16-17: TRAN-TYPE-CD (2 chars)
    - Position 18-21: TRAN-CAT-CD (4 digits)
    - Position 22-31: TRAN-SOURCE (10 chars)
    - Position 32-131: TRAN-DESC (100 chars)
    - Position 132-143: TRAN-AMT (12 digits, implied 2 decimals)
    - Position 144-152: TRAN-MERCHANT-ID (9 chars)
    - Position 153-202: TRAN-MERCHANT-NAME (50 chars)
    - Position 203-252: TRAN-MERCHANT-CITY (50 chars)
    - Position 253-262: TRAN-MERCHANT-ZIP (10 chars)
    - Position 263-278: TRAN-CARD-NUM (16 chars)
    - Position 279-304: TRAN-ORIG-TS (26 chars)
    - Position 305-330: TRAN-PROC-TS (26 chars)
    """

    @property
    def job_name(self) -> str:
        return "TRANFILE"

    @property
    def record_length(self) -> int:
        return 350

    @property
    def model_class(self) -> type[Transaction]:
        return Transaction

    def parse_record(self, line: str) -> Transaction | None:
        """Parse a single transaction record from the flat file."""
        line = line.ljust(self.record_length)
        
        tran_id = line[0:16].strip()
        if not tran_id:
            return None

        cat_cd_str = line[18:22].strip()
        try:
            cat_cd = int(cat_cd_str) if cat_cd_str else None
        except ValueError:
            cat_cd = None

        return Transaction(
            tran_id=tran_id,
            tran_type_cd=line[16:18].strip() or None,
            tran_cat_cd=cat_cd,
            tran_source=line[22:32].strip() or None,
            tran_desc=line[32:132].strip() or None,
            tran_amt=self._parse_decimal(line[132:144]),
            tran_merchant_id=line[144:153].strip() or None,
            tran_merchant_name=line[153:203].strip() or None,
            tran_merchant_city=line[203:253].strip() or None,
            tran_merchant_zip=line[253:263].strip() or None,
            tran_card_num=line[263:279].strip() or None,
            tran_orig_ts=line[279:305].strip() or None,
            tran_proc_ts=line[305:331].strip() or None,
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
