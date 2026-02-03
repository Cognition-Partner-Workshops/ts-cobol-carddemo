"""
Card data loader - migrated from CARDFILE.jcl.

This module replaces the JCL job CARDFILE which:
1. Closes CARDDAT and CARDAIX files in CICS region
2. Deletes existing CARDDATA VSAM KSDS cluster and AIX
3. Defines new CARDDATA VSAM KSDS cluster (KEYS(16 0), RECORDSIZE(150 150))
4. Copies data from AWS.M2.CARDDEMO.CARDDATA.PS to VSAM file using REPRO
5. Defines alternate index on ACCT-ID (KEYS(11 16))
6. Defines path for alternate index
7. Builds alternate index
8. Opens CARDDAT and CARDAIX files in CICS region
"""

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.card import Card


class CardLoader(BaseLoader[Card]):
    """
    Loader for Card data.
    
    Migrated from CARDFILE.jcl which loads card master data
    from a flat file into the CARDDATA VSAM KSDS file.
    
    Record layout from CVACT02Y.cpy:
    - Position 0-15: CARD-NUM (16 chars)
    - Position 16-26: CARD-ACCT-ID (11 digits)
    - Position 27-29: CARD-CVV-CD (3 chars)
    - Position 30-79: CARD-EMBOSSED-NAME (50 chars)
    - Position 80-89: CARD-EXPIRAION-DATE (10 chars)
    - Position 90: CARD-ACTIVE-STATUS (1 char)
    """

    @property
    def job_name(self) -> str:
        return "CARDFILE"

    @property
    def record_length(self) -> int:
        return 150

    @property
    def model_class(self) -> type[Card]:
        return Card

    def parse_record(self, line: str) -> Card | None:
        """Parse a single card record from the flat file."""
        line = line.ljust(self.record_length)
        
        card_num = line[0:16].strip()
        if not card_num:
            return None

        acct_id_str = line[16:27].strip()
        try:
            acct_id = int(acct_id_str) if acct_id_str else None
        except ValueError:
            self.logger.warning(f"Invalid account ID for card {card_num}: {acct_id_str}")
            acct_id = None

        return Card(
            card_num=card_num,
            card_acct_id=acct_id,
            card_cvv_cd=line[27:30].strip() or None,
            card_embossed_name=line[30:80].strip() or None,
            card_expiraion_date=line[80:90].strip() or None,
            card_active_status=line[90:91].strip() or None,
        )
