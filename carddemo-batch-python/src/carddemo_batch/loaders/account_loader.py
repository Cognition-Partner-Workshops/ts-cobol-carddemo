"""
Account data loader - migrated from ACCTFILE.jcl.

This module replaces the JCL job ACCTFILE which:
1. Deletes existing ACCTDATA VSAM KSDS cluster
2. Defines new ACCTDATA VSAM KSDS cluster (KEYS(11 0), RECORDSIZE(300 300))
3. Copies data from AWS.M2.CARDDEMO.ACCTDATA.PS to VSAM file using REPRO
"""

from decimal import Decimal

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.account import Account


class AccountLoader(BaseLoader[Account]):
    """
    Loader for Account data.
    
    Migrated from ACCTFILE.jcl which loads account master data
    from a flat file into the ACCTDATA VSAM KSDS file.
    
    Record layout from CVACT01Y.cpy:
    - Position 0-10: ACCT-ID (11 digits)
    - Position 11: ACCT-ACTIVE-STATUS (1 char)
    - Position 12-23: ACCT-CURR-BAL (12 digits, implied 2 decimals)
    - Position 24-35: ACCT-CREDIT-LIMIT (12 digits, implied 2 decimals)
    - Position 36-47: ACCT-CASH-CREDIT-LIMIT (12 digits, implied 2 decimals)
    - Position 48-57: ACCT-OPEN-DATE (10 chars)
    - Position 58-67: ACCT-EXPIRAION-DATE (10 chars)
    - Position 68-77: ACCT-REISSUE-DATE (10 chars)
    - Position 78-89: ACCT-CURR-CYC-CREDIT (12 digits, implied 2 decimals)
    - Position 90-101: ACCT-CURR-CYC-DEBIT (12 digits, implied 2 decimals)
    - Position 102-111: ACCT-ADDR-ZIP (10 chars)
    - Position 112-121: ACCT-GROUP-ID (10 chars)
    """

    @property
    def job_name(self) -> str:
        return "ACCTFILE"

    @property
    def record_length(self) -> int:
        return 300

    @property
    def model_class(self) -> type[Account]:
        return Account

    def parse_record(self, line: str) -> Account | None:
        """Parse a single account record from the flat file."""
        line = line.ljust(self.record_length)
        
        acct_id_str = line[0:11].strip()
        if not acct_id_str:
            return None
        
        try:
            acct_id = int(acct_id_str)
        except ValueError:
            self.logger.warning(f"Invalid account ID: {acct_id_str}")
            return None

        return Account(
            acct_id=acct_id,
            acct_active_status=line[11:12].strip() or None,
            acct_curr_bal=self._parse_decimal(line[12:24]),
            acct_credit_limit=self._parse_decimal(line[24:36]),
            acct_cash_credit_limit=self._parse_decimal(line[36:48]),
            acct_open_date=line[48:58].strip() or None,
            acct_expiraion_date=line[58:68].strip() or None,
            acct_reissue_date=line[68:78].strip() or None,
            acct_curr_cyc_credit=self._parse_decimal(line[78:90]),
            acct_curr_cyc_debit=self._parse_decimal(line[90:102]),
            acct_addr_zip=line[102:112].strip() or None,
            acct_group_id=line[112:122].strip() or None,
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
