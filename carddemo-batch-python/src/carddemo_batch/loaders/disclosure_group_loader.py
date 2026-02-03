"""
Disclosure group data loader - migrated from DISCGRP.jcl.

This module replaces the JCL job DISCGRP which:
1. Deletes existing DISCGRP VSAM KSDS cluster
2. Defines new DISCGRP VSAM KSDS cluster (KEYS(16 0), RECORDSIZE(50 50))
3. Copies data from AWS.M2.CARDDEMO.DISCGRP.PS to VSAM file using REPRO
"""

from decimal import Decimal

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.disclosure_group import DisclosureGroup


class DisclosureGroupLoader(BaseLoader[DisclosureGroup]):
    """
    Loader for Disclosure Group data.
    
    Migrated from DISCGRP.jcl which loads disclosure group (interest rate) data
    from a flat file into the DISCGRP VSAM KSDS file.
    
    Record layout from CVTRA02Y.cpy:
    - Position 0-9: DIS-ACCT-GROUP-ID (10 chars)
    - Position 10-11: DIS-TRAN-TYPE-CD (2 chars)
    - Position 12-15: DIS-TRAN-CAT-CD (4 digits)
    - Position 16-21: DIS-INT-RATE (6 digits, implied 2 decimals)
    """

    @property
    def job_name(self) -> str:
        return "DISCGRP"

    @property
    def record_length(self) -> int:
        return 50

    @property
    def model_class(self) -> type[DisclosureGroup]:
        return DisclosureGroup

    def parse_record(self, line: str) -> DisclosureGroup | None:
        """Parse a single disclosure group record from the flat file."""
        line = line.ljust(self.record_length)
        
        group_id = line[0:10].strip()
        if not group_id:
            return None

        type_cd = line[10:12].strip()
        if not type_cd:
            return None

        cat_cd_str = line[12:16].strip()
        try:
            cat_cd = int(cat_cd_str) if cat_cd_str else 0
        except ValueError:
            self.logger.warning(f"Invalid category code: {cat_cd_str}")
            return None

        return DisclosureGroup(
            dis_acct_group_id=group_id,
            dis_tran_type_cd=type_cd,
            dis_tran_cat_cd=cat_cd,
            dis_int_rate=self._parse_decimal(line[16:22]),
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
