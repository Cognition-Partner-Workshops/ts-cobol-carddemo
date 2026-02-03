"""
Card cross-reference data loader - migrated from XREFFILE.jcl.

This module replaces the JCL job XREFFILE which:
1. Deletes existing CARDXREF VSAM KSDS cluster and AIX
2. Defines new CARDXREF VSAM KSDS cluster (KEYS(16 0), RECORDSIZE(50 50))
3. Copies data from AWS.M2.CARDDEMO.CARDXREF.PS to VSAM file using REPRO
4. Defines alternate index on ACCT-ID (KEYS(11 25))
5. Defines path for alternate index
6. Builds alternate index
"""

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.card_xref import CardXref


class CardXrefLoader(BaseLoader[CardXref]):
    """
    Loader for Card cross-reference data.
    
    Migrated from XREFFILE.jcl which loads card-to-account-to-customer
    cross-reference data from a flat file into the CARDXREF VSAM KSDS file.
    
    Record layout from CVACT03Y.cpy:
    - Position 0-15: XREF-CARD-NUM (16 chars)
    - Position 16-24: XREF-CUST-ID (9 digits)
    - Position 25-35: XREF-ACCT-ID (11 digits)
    """

    @property
    def job_name(self) -> str:
        return "XREFFILE"

    @property
    def record_length(self) -> int:
        return 50

    @property
    def model_class(self) -> type[CardXref]:
        return CardXref

    def parse_record(self, line: str) -> CardXref | None:
        """Parse a single card cross-reference record from the flat file."""
        line = line.ljust(self.record_length)
        
        card_num = line[0:16].strip()
        if not card_num:
            return None

        cust_id_str = line[16:25].strip()
        try:
            cust_id = int(cust_id_str) if cust_id_str else None
        except ValueError:
            self.logger.warning(f"Invalid customer ID for card {card_num}: {cust_id_str}")
            cust_id = None

        acct_id_str = line[25:36].strip()
        try:
            acct_id = int(acct_id_str) if acct_id_str else None
        except ValueError:
            self.logger.warning(f"Invalid account ID for card {card_num}: {acct_id_str}")
            acct_id = None

        return CardXref(
            xref_card_num=card_num,
            xref_cust_id=cust_id,
            xref_acct_id=acct_id,
        )
